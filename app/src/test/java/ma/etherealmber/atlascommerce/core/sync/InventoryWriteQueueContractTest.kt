package ma.etherealmber.atlascommerce.core.sync

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.runBlocking
import ma.etherealmber.atlascommerce.data.model.InventoryLocationPlacementDto
import ma.etherealmber.atlascommerce.data.model.InventoryOfflineOperationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Rule
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class InventoryWriteQueueContractTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `operation UUID is generated once and reused in header and body after retry`() = runBlocking {
        val generated = AtomicInteger()
        val operationId = "123e4567-e89b-42d3-a456-426614174000"
        val entry = InventoryQueueEntryFactory {
            generated.incrementAndGet()
            operationId
        }.movement(3L, 42, "Marble", "Warehouse", "receive", 5.0, "", OCCURRED_AT)
        val sent = mutableListOf<Pair<String, String>>()
        val gateway = InventoryOperationGateway(InventoryHttpPost { headerKey, body ->
            sent += headerKey to body.operationId
            InventoryHttpResult(statusCode = 503)
        })

        // A retry reuses the persisted queue entry; it does not construct a new operation.
        gateway.submit(entry)
        gateway.submit(entry)

        assertEquals(1, generated.get())
        assertEquals(listOf(operationId to operationId, operationId to operationId), sent)
        val json = wireJson(entry.toRequest())
        assertTrue(json.contains("\"operation_id\":\"$operationId\""))
        assertTrue(json.contains("\"location_id\":3"))
    }

    @Test
    fun `each queued item is sent as its own request and never as a batch`() = runBlocking {
        val factory = InventoryQueueEntryFactory { UUIDS.incrementAndGet().toString() }
        val sent = mutableListOf<Pair<String, Any>>()
        val gateway = InventoryOperationGateway(InventoryHttpPost { key, body ->
            sent += key to body
            InventoryHttpResult(statusCode = 200, responseOperationId = body.operationId)
        })
        val first = factory.movement(1L, 10, "A", "One", "receive", 1.0, "", OCCURRED_AT)
        val second = factory.movement(2L, 11, "B", "Two", "issue", 2.0, "", OCCURRED_AT)

        gateway.submit(first)
        gateway.submit(second)

        assertEquals(2, sent.size)
        assertEquals(listOf(first.operationId, second.operationId), sent.map { it.first })
        assertEquals(listOf(1L, 2L), sent.map { (it.second as ma.etherealmber.atlascommerce.data.model.InventoryOfflineOperationRequest).locationId })
    }

    @Test
    fun `location code string is rejected before the fake HTTP transport is called`() = runBlocking {
        var requests = 0
        val gateway = InventoryOperationGateway(InventoryHttpPost { _, _ ->
            requests++
            InventoryHttpResult(statusCode = 200)
        })
        val error = runCatching {
            InventoryQueueEntryFactory().movement("MAG", 42, "Marble", "Warehouse", "receive", 1.0, "", OCCURRED_AT)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(0, requests)
    }

    @Test
    fun `count request copies observed quantity and revision from placements response`() {
        val placementResponse = InventoryLocationPlacementDto(
            locationId = 3,
            code = "MAG",
            name = "Warehouse",
            quantity = 15.0,
            revision = 8,
        )
        val entry = InventoryQueueEntryFactory { "count-operation" }.count(
            locationId = 3,
            productId = 42,
            productName = "Marble",
            locationName = "Warehouse",
            countedQuantity = 17.0,
            placement = placementResponse,
            note = "Cycle count",
            occurredAt = OCCURRED_AT,
        )

        assertEquals(15.0, entry.toRequest().observedQuantity!!, 0.0)
        assertEquals(8L, entry.toRequest().observedRevision)
        assertEquals(17.0, entry.toRequest().countedQuantity!!, 0.0)
        val json = wireJson(entry.toRequest())
        assertTrue(json.contains("\"observed_revision\":8"))
        assertTrue(json.contains("\"counted_quantity\":17.0"))
    }

    @Test
    fun `count conflict and idempotency key reuse are terminal visible conflicts and are sent once`() = runBlocking {
        listOf("COUNT_CONFLICT", "IDEMPOTENCY_KEY_REUSED").forEach { code ->
            var requests = 0
            val entry = InventoryQueueEntryFactory { "conflict-$code" }
                .movement(3L, 42, "Marble", "Warehouse", "receive", 5.0, "", OCCURRED_AT)
            val gateway = InventoryOperationGateway(InventoryHttpPost { _, _ ->
                requests++
                InventoryHttpResult(statusCode = 409, errorCode = code, errorMessage = code)
            })
            val exchange = InventoryReplayScope.submitAndDecideIfAllowed(
                "account-a", "account-a", 3L, entry, gateway,
            )!!
            val (result, decision) = exchange

            assertEquals(code, result.errorCode)
            assertEquals(
                InventoryHttpDecision.Terminal(InventoryTerminalStatus.CONFLICT),
                decision,
            )
            assertTrue("$code must surface as a conflict", InventoryQueueConflictAction.canRetry("CONFLICT", replaying = false))
            assertEquals("terminal conflict must not retry automatically", 1, requests)
        }
    }

    @Test
    fun `rejected write does not stall later FIFO work`() {
        val rejected = InventoryQueueEntryFactory { "rejected-write" }
            .movement(3L, 42, "Marble", "Warehouse", "receive", 5.0, "", OCCURRED_AT)
            .copy(status = InventoryQueueStatus.REJECTED.name, failureMessage = "Quantité invalide")
        val following = InventoryQueueEntryFactory { "following-write" }
            .movement(3L, 43, "Granite", "Warehouse", "receive", 2.0, "", OCCURRED_AT)
        assertEquals(
            InventoryHttpDecision.Terminal(InventoryTerminalStatus.REJECTED),
            InventoryRetryPolicy.decide(422, "QTY_INVALID", 0),
        )
        assertEquals(following, InventoryQueueReplayOrder.nextReplayable(listOf(rejected, following)))
        assertNull(InventoryQueueReplayOrder.nextReplayable(listOf(rejected)))
    }

    @Test
    fun `conflict write offers recount and resend but rejected write does not`() {
        assertEquals(
            InventoryHttpDecision.Terminal(InventoryTerminalStatus.CONFLICT),
            InventoryRetryPolicy.decide(409, "COUNT_CONFLICT", 0),
        )
        assertTrue(InventoryQueueConflictAction.canRetry("CONFLICT", replaying = false))
        assertFalse(InventoryQueueConflictAction.canRetry("REJECTED", replaying = false))
    }

    @Test
    fun `rejected and conflict statuses and French messages survive process death`() {
        val directory = temporaryFolder.newFolder("terminal-process-death")
        val rejected = InventoryQueueEntryFactory { "rejected-persisted" }
            .movement(3L, 42, "Marble", "Warehouse", "receive", 5.0, "", OCCURRED_AT)
            .copy(status = InventoryQueueStatus.REJECTED.name, failureCode = "QTY_INVALID", failureMessage = "Quantité insuffisante — veuillez vérifier le stock.")
        val conflict = InventoryQueueEntryFactory { "conflict-persisted" }
            .count(3L, 43, "Granite", "Warehouse", 7.0,
                InventoryLocationPlacementDto(locationId = 3, quantity = 5.0, revision = 8), "", OCCURRED_AT)
            .copy(status = InventoryQueueStatus.CONFLICT.name, failureCode = "COUNT_CONFLICT", failureMessage = "Le stock a changé.")

        fileStore(directory).save("server-account", listOf(rejected, conflict))
        val restored = fileStore(directory).load("server-account")

        assertEquals(listOf("REJECTED", "CONFLICT"), restored.map { it.status })
        assertEquals(rejected.failureMessage, restored.first().failureMessage)
        assertEquals(conflict.failureMessage, restored.last().failureMessage)
    }

    @Test
    fun `held export contains one JSON payload per held write with operation ids`() {
        val entries = listOf(
            InventoryQueueEntryFactory { "held-one" }.movement(3L, 42, "A", "Warehouse", "receive", 1.0, "", OCCURRED_AT)
                .copy(status = InventoryQueueStatus.HELD.name),
            InventoryQueueEntryFactory { "held-two" }.movement(3L, 43, "B", "Warehouse", "issue", 2.0, "", OCCURRED_AT)
                .copy(status = InventoryQueueStatus.HELD.name),
            InventoryQueueEntryFactory { "still-queued" }.movement(3L, 44, "C", "Warehouse", "receive", 3.0, "", OCCURRED_AT),
        )
        val adapter = Moshi.Builder().build().adapter<List<InventoryOfflineOperationRequest>>(
            Types.newParameterizedType(List::class.java, InventoryOfflineOperationRequest::class.java),
        )

        val payload = InventoryHeldWriteActions.exportJson(entries, adapter::toJson)
        val decoded = adapter.fromJson(payload).orEmpty()

        assertEquals(2, decoded.size)
        assertEquals(listOf("held-one", "held-two"), decoded.map { it.operationId })
    }

    @Test
    fun `held discard requires exact count and held actions are unavailable during replay`() {
        val gate = InventoryQueueReplayGate()
        assertTrue(InventoryHeldWriteActions.canExport(3, replaying = false))
        assertFalse(InventoryHeldWriteActions.canDiscard(3, typedCount = 2, replaying = false))
        assertTrue(InventoryHeldWriteActions.canDiscard(3, typedCount = 3, replaying = false))
        assertFalse(InventoryHeldWriteActions.canDiscard(3, typedCount = 3, replaying = true))

        assertTrue(gate.begin())
        assertTrue(gate.isActive())
        assertFalse(InventoryHeldWriteActions.canExport(3, replaying = gate.isActive()))
        assertFalse(InventoryHeldWriteActions.canDiscard(3, typedCount = 3, replaying = gate.isActive()))
        assertFalse(InventoryQueueConflictAction.canRetry("CONFLICT", replaying = gate.isActive()))
        gate.end()
        assertTrue(InventoryHeldWriteActions.canExport(3, replaying = gate.isActive()))
    }

    @Test
    fun `404 unsupported route is held and other 4xx statuses are terminal`() {
        assertEquals(
            InventoryHttpDecision.HoldUnsupportedServer,
            InventoryRetryPolicy.decide(404, "HTTP_404", rateLimitAttempts = 0),
        )
        assertTrue(InventoryRetryPolicy.MIGRATION_REQUIRED_MESSAGE.contains("needs migration 0020"))
        assertTrue(InventoryRetryPolicy.MIGRATION_REQUIRED_MESSAGE.contains("held on this device"))
        assertEquals(
            InventoryHttpDecision.HoldUnsupportedServer,
            InventoryRetryPolicy.decide(404, "NOT_FOUND", rateLimitAttempts = 0),
        )
        assertTrue(
            InventoryRetryPolicy.decide(404, "LOCATION_NOT_FOUND", 0) is InventoryHttpDecision.Terminal,
        )
        listOf(400, 401, 403, 409, 422).forEach { code ->
            val decision = InventoryRetryPolicy.decide(code, "VALIDATION_ERROR", rateLimitAttempts = 0)
            assertEquals(
                "HTTP $code must be a visible rejection",
                InventoryHttpDecision.Terminal(InventoryTerminalStatus.REJECTED),
                decision,
            )
        }
        listOf("QTY_INVALID", "LOCATION_NOT_FOUND", "PRODUCT_NOT_FOUND").forEach { errorCode ->
            assertEquals(
                InventoryHttpDecision.Terminal(InventoryTerminalStatus.REJECTED),
                InventoryRetryPolicy.decide(if (errorCode == "QTY_INVALID") 422 else 404, errorCode, 0),
            )
        }
    }

    @Test
    fun `429 honors retry after reuses key and parks after five rate limit responses`() = runBlocking {
        val retry = InventoryRetryPolicy.decide(429, "RATE_LIMITED", 0, "120")
        assertEquals(InventoryHttpDecision.RetryAfter(seconds = 120, attempt = 1), retry)
        assertEquals(
            InventoryHttpDecision.RetryAfter(seconds = InventoryRetryPolicy.MAX_RETRY_AFTER_SECONDS, attempt = 1),
            InventoryRetryPolicy.decide(429, "RATE_LIMITED", 0, "999999999"),
        )
        assertEquals(
            InventoryHttpDecision.ParkRateLimited,
            InventoryRetryPolicy.decide(429, "RATE_LIMITED", InventoryRetryPolicy.MAX_RATE_LIMIT_ATTEMPTS - 1, "1"),
        )
        val operationId = "123e4567-e89b-42d3-a456-426614174001"
        val entry = InventoryQueueEntryFactory { operationId }
            .movement(3L, 42, "Marble", "Warehouse", "receive", 5.0, "", OCCURRED_AT)
        val retriedIds = mutableListOf<Pair<String, String>>()
        val gateway = InventoryOperationGateway(InventoryHttpPost { key, body ->
            retriedIds += key to body.operationId
            InventoryHttpResult(statusCode = 429, errorCode = "RATE_LIMITED", retryAfter = "120")
        })
        repeat(InventoryRetryPolicy.MAX_RATE_LIMIT_ATTEMPTS) { attempt ->
            val (_, decision) = gateway.submitAndDecide(entry, attempt)
            if (attempt < InventoryRetryPolicy.MAX_RATE_LIMIT_ATTEMPTS - 1) {
                assertTrue(decision is InventoryHttpDecision.RetryAfter)
            } else {
                assertEquals(InventoryHttpDecision.ParkRateLimited, decision)
            }
        }
        assertEquals(List(InventoryRetryPolicy.MAX_RATE_LIMIT_ATTEMPTS) { operationId to operationId }, retriedIds)
        assertEquals(30L, InventoryRetryPolicy.retryAfterSeconds("not-a-date", Instant.EPOCH))
        val now = Instant.parse("2026-11-01T00:00:00Z")
        val header = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(now.plusSeconds(120), ZoneOffset.UTC))
        assertEquals(120L, InventoryRetryPolicy.retryAfterSeconds(header, now))
    }

    @Test
    fun `5xx stays retryable while preserving the operation key`() {
        assertEquals(InventoryHttpDecision.Retry, InventoryRetryPolicy.decide(503, "UPSTREAM", 0))
        assertEquals(InventoryHttpDecision.Retry, InventoryRetryPolicy.decide(0, null, 0))
    }

    @Test
    fun `account switch after enqueue under account A refuses HTTP replay under account B`() = runBlocking {
        val entry = InventoryQueueEntryFactory { "account-a-op" }
            .movement(3L, 42, "Marble", "Warehouse", "receive", 5.0, "", OCCURRED_AT)
        val accountStore = fileStore(temporaryFolder.newFolder("account-switch"))
        accountStore.save("account-a", listOf(entry))
        val loadedForA = accountStore.load("account-a").single()
        var requests = 0
        val gateway = InventoryOperationGateway(InventoryHttpPost { _, _ ->
            requests++
            InventoryHttpResult(statusCode = 200)
        })

        val accountSwitch = InventoryReplayScope.submitAndDecideIfAllowed("account-a", "account-b", 3, loadedForA, gateway)
        val locationSwitch = InventoryReplayScope.submitAndDecideIfAllowed("account-a", "account-a", 4, loadedForA, gateway)

        assertNull(accountSwitch)
        assertNull(locationSwitch)
        assertEquals(0, requests)
        assertEquals("account-a-op", accountStore.load("account-a").single().operationId)
    }

    @Test
    fun `operation key survives process death reload and resumes unchanged`() = runBlocking {
        val directory = temporaryFolder.newFolder("outbox")
        val queued = InventoryQueueEntryFactory { "stable-operation-uuid" }
            .movement(3L, 42, "Marble", "Warehouse", "receive", 5.0, "", OCCURRED_AT)

        fileStore(directory).save("server-account", listOf(queued))
        val reloaded = fileStore(directory).load("server-account").single()

        assertEquals(queued.operationId, reloaded.operationId)
        assertEquals(queued.toRequest(), reloaded.toRequest())
        var resumedHeader: String? = null
        var resumedBodyId: String? = null
        InventoryOperationGateway(InventoryHttpPost { headerKey, body ->
            resumedHeader = headerKey
            resumedBodyId = body.operationId
            InventoryHttpResult(statusCode = 503)
        }).submit(reloaded)
        assertEquals(queued.operationId, resumedHeader)
        assertEquals(queued.operationId, resumedBodyId)
        assertNotNull(directory.listFiles()?.singleOrNull())
        assertFalse(directory.listFiles().orEmpty().any { it.name.endsWith(".tmp") })
    }

    private fun wireJson(body: InventoryOfflineOperationRequest): String =
        Moshi.Builder().build().adapter(InventoryOfflineOperationRequest::class.java).toJson(body)

    private fun fileStore(directory: File): InventoryQueueFileStore {
        val moshi = Moshi.Builder().build()
        val listType = Types.newParameterizedType(List::class.java, InventoryQueueEntry::class.java)
        val adapter = moshi.adapter<List<InventoryQueueEntry>>(listType)
        return InventoryQueueFileStore(
            directory = directory,
            serialize = { adapter.toJson(it) },
            deserialize = { adapter.fromJson(it) },
            encrypt = { "encrypted:$it" },
            decrypt = { stored -> stored.takeIf { it.startsWith("encrypted:") }?.removePrefix("encrypted:") },
        )
    }

    companion object {
        private const val OCCURRED_AT = "2026-10-21T09:30:00Z"
        private val UUIDS = AtomicInteger()
    }
}
