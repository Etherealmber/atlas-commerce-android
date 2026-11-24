package ma.etherealmber.atlascommerce.core.sync

import ma.etherealmber.atlascommerce.data.model.InventoryLocationPlacementDto
import ma.etherealmber.atlascommerce.data.model.InventoryOfflineOperationRequest
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Transport-neutral result so queue protocol behavior can be tested without Android/WorkManager. */
internal data class InventoryHttpResult(
    val statusCode: Int,
    val responseOperationId: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val retryAfter: String? = null,
)

internal fun interface InventoryHttpPost {
    suspend fun post(idempotencyKey: String, body: InventoryOfflineOperationRequest): InventoryHttpResult
}

internal class InventoryOperationGateway(private val post: InventoryHttpPost) {
    suspend fun submit(entry: InventoryQueueEntry): InventoryHttpResult =
        post.post(entry.operationId, entry.toRequest())

    suspend fun submitAndDecide(
        entry: InventoryQueueEntry,
        rateLimitAttempts: Int,
    ): Pair<InventoryHttpResult, InventoryHttpDecision> {
        val result = submit(entry)
        return result to InventoryRetryPolicy.decide(
            result.statusCode, result.errorCode, rateLimitAttempts, result.retryAfter,
        )
    }
}

internal object InventoryReplayScope {
    fun permits(
        queuedAccount: String,
        activeAccount: String?,
        scheduledLocationId: Long,
        entry: InventoryQueueEntry,
    ): Boolean = queuedAccount == activeAccount &&
        scheduledLocationId > 0 && entry.locationId == scheduledLocationId

    suspend fun submitAndDecideIfAllowed(
        queuedAccount: String,
        activeAccount: String?,
        scheduledLocationId: Long,
        entry: InventoryQueueEntry,
        gateway: InventoryOperationGateway,
    ): Pair<InventoryHttpResult, InventoryHttpDecision>? = if (permits(queuedAccount, activeAccount, scheduledLocationId, entry)) {
        gateway.submitAndDecide(entry, entry.rateLimitAttempts)
    } else {
        null
    }
}

internal sealed interface InventoryHttpDecision {
    data object Retry : InventoryHttpDecision
    data class RetryAfter(val seconds: Long, val attempt: Int) : InventoryHttpDecision
    data object ParkRateLimited : InventoryHttpDecision
    data object HoldUnsupportedServer : InventoryHttpDecision
    data class Terminal(val conflict: Boolean) : InventoryHttpDecision
}

internal object InventoryRetryPolicy {
    const val MAX_RATE_LIMIT_ATTEMPTS = 5
    const val DEFAULT_RETRY_AFTER_SECONDS = 30L
    const val MAX_RETRY_AFTER_SECONDS = 6 * 60 * 60L
    const val MIGRATION_REQUIRED_MESSAGE = "This server does not support offline operations (needs migration 0020). The write is held on this device."
    const val RATE_LIMIT_PARKED_MESSAGE = "The server kept rate-limiting this operation. It is parked; retry after reviewing the queue."

    fun decide(
        statusCode: Int,
        errorCode: String?,
        rateLimitAttempts: Int,
        retryAfter: String? = null,
    ): InventoryHttpDecision = when {
        statusCode == 404 && errorCode !in setOf("PRODUCT_NOT_FOUND", "LOCATION_NOT_FOUND") -> InventoryHttpDecision.HoldUnsupportedServer
        statusCode == 429 && rateLimitAttempts + 1 >= MAX_RATE_LIMIT_ATTEMPTS ->
            InventoryHttpDecision.ParkRateLimited
        statusCode == 429 -> InventoryHttpDecision.RetryAfter(
            seconds = retryAfterSeconds(retryAfter),
            attempt = rateLimitAttempts + 1,
        )
        statusCode >= 500 || statusCode <= 0 -> InventoryHttpDecision.Retry
        statusCode == 409 -> InventoryHttpDecision.Terminal(
            conflict = errorCode == "COUNT_CONFLICT" || errorCode == "IDEMPOTENCY_KEY_REUSED",
        )
        statusCode in 400..499 -> InventoryHttpDecision.Terminal(conflict = false)
        else -> InventoryHttpDecision.Terminal(conflict = false)
    }

    fun retryAfterSeconds(header: String?, now: Instant = Instant.now()): Long = parseRetryAfter(header, now)

    private fun parseRetryAfter(header: String?, now: Instant = Instant.now()): Long {
        val seconds = header?.trim()?.toLongOrNull()
        val parsed = when {
            seconds != null -> seconds.coerceAtLeast(0)
            !header.isNullOrBlank() -> runCatching {
                ZonedDateTime.parse(header.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant().epochSecond - now.epochSecond
            }.getOrNull()?.coerceAtLeast(0)
            else -> null
        } ?: DEFAULT_RETRY_AFTER_SECONDS
        return parsed.coerceAtMost(MAX_RETRY_AFTER_SECONDS)
    }
}

/** Entry construction boundary: accept numeric location IDs only, never a location code. */
internal class InventoryQueueEntryFactory(private val operationId: () -> String = { UUID.randomUUID().toString() }) {
    fun requireLocationId(value: Any?): Long = when (value) {
        is Long -> value.takeIf { it > 0 }
        is Int -> value.toLong().takeIf { it > 0 }
        else -> null
    } ?: throw IllegalArgumentException("location_id must be a positive integer ID.")

    fun movement(
        locationId: Any?,
        productId: Long,
        productName: String,
        locationName: String,
        kind: String,
        quantity: Double,
        note: String,
        occurredAt: String = Instant.now().toString(),
    ): InventoryQueueEntry {
        val id = requireLocationId(locationId)
        require(productId > 0 && kind in setOf("receive", "issue") && quantity.isFinite() && quantity > 0.0)
        return InventoryQueueEntry(
            operationId = operationId(), kind = kind, productId = productId, productName = productName,
            locationId = id, locationName = locationName, quantityDelta = quantity,
            note = note.trim().take(200), clientOccurredAt = occurredAt,
        )
    }

    fun count(
        locationId: Any?,
        productId: Long,
        productName: String,
        locationName: String,
        countedQuantity: Double,
        placement: InventoryLocationPlacementDto,
        note: String,
        occurredAt: String = Instant.now().toString(),
    ): InventoryQueueEntry {
        val id = requireLocationId(locationId)
        require(id == placement.locationId) { "The placement does not belong to the selected location." }
        require(productId > 0 && countedQuantity.isFinite() && countedQuantity >= 0.0)
        require(placement.quantity.isFinite())
        val observedRevision = requireNotNull(placement.revision) {
            "A location revision is required for offline cycle counts."
        }
        return InventoryQueueEntry(
            operationId = operationId(), kind = "count", productId = productId, productName = productName,
            locationId = id, locationName = locationName, countedQuantity = countedQuantity,
            observedQuantity = placement.quantity, observedRevision = observedRevision,
            note = note.trim().take(200), clientOccurredAt = occurredAt,
        )
    }
}

/** Small file-backed seam shared by the Android queue and JVM persistence tests. */
internal class InventoryQueueFileStore(
    private val directory: File,
    private val serialize: (List<InventoryQueueEntry>) -> String,
    private val deserialize: (String) -> List<InventoryQueueEntry>?,
    private val encrypt: (String) -> String,
    private val decrypt: (String) -> String?,
) {
    fun load(accountKey: String): List<InventoryQueueEntry> {
        val file = File(directory, "queue_$accountKey.json")
        if (!file.exists()) return emptyList()
        val plain = decrypt(file.readText()) ?: throw IOException("Could not decrypt an existing inventory queue file.")
        return deserialize(plain) ?: throw IOException("Could not deserialize an existing inventory queue file.")
    }

    fun save(accountKey: String, entries: List<InventoryQueueEntry>) {
        val file = File(directory, "queue_$accountKey.json")
        if (entries.isEmpty()) {
            runCatching { file.delete() }
            return
        }
        directory.mkdirs()
        val encrypted = encrypt(serialize(entries))
        val temp = File(directory, "queue_$accountKey.tmp")
        temp.writeText(encrypted)
        try {
            java.nio.file.Files.move(
                temp.toPath(), file.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: Throwable) {
            if (!temp.renameTo(file)) {
                file.writeText(encrypted)
                temp.delete()
            }
        }
    }
}
