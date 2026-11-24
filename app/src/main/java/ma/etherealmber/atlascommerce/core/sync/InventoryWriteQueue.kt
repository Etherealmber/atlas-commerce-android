package ma.etherealmber.atlascommerce.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.net.TokenHolder
import ma.etherealmber.atlascommerce.core.security.CryptoStore
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.model.InventoryLocationDto
import ma.etherealmber.atlascommerce.data.model.InventoryLocationPlacementDto
import ma.etherealmber.atlascommerce.data.model.InventoryOfflineOperationRequest
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

internal enum class InventoryQueueStatus { QUEUED, CONFLICT, REJECTED, HELD, FAILED }

@JsonClass(generateAdapter = true)
data class InventoryQueueEntry(
    val operationId: String,
    val kind: String,
    val productId: Long,
    val productName: String,
    val locationId: Long,
    val locationName: String,
    val quantityDelta: Double? = null,
    val countedQuantity: Double? = null,
    val observedQuantity: Double? = null,
    val observedRevision: Long? = null,
    val note: String = "",
    val clientOccurredAt: String,
    val status: String = InventoryQueueStatus.QUEUED.name,
    val failureCode: String? = null,
    val failureMessage: String? = null,
    val rateLimitAttempts: Int = 0,
    val retryNotBeforeEpochMillis: Long? = null,
) {
    fun toRequest() = InventoryOfflineOperationRequest(
        operationId = operationId,
        kind = kind,
        productId = productId,
        locationId = locationId,
        quantityDelta = quantityDelta,
        countedQuantity = countedQuantity,
        observedQuantity = observedQuantity,
        observedRevision = observedRevision,
        note = note,
        clientOccurredAt = clientOccurredAt,
    )
}

@JsonClass(generateAdapter = true)
data class CachedInventoryContext(
    val locations: List<InventoryLocationDto>,
    val placements: List<InventoryLocationPlacementDto>,
)

/** Encrypted durable outbox, isolated per server/account, with WorkManager-backed FIFO replay. */
@Singleton
class InventoryWriteQueue @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi,
    private val crypto: CryptoStore,
    private val sessionManager: SessionManager,
    @ma.etherealmber.atlascommerce.di.ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val directory = File(context.noBackupFilesDir, "atlas-inventory-outbox").apply { mkdirs() }
    private val mutex = Mutex()
    private val replayGate = InventoryQueueReplayGate()
    private val activeEntries = MutableStateFlow<List<InventoryQueueEntry>>(emptyList())
    private val activeReadError = MutableStateFlow<String?>(null)
    private val activeReplay = MutableStateFlow(false)
    val entries: StateFlow<List<InventoryQueueEntry>> = activeEntries.asStateFlow()
    val readError: StateFlow<String?> = activeReadError.asStateFlow()
    val isReplaying: StateFlow<Boolean> = activeReplay.asStateFlow()
    private val listAdapter by lazy {
        moshi.adapter<List<InventoryQueueEntry>>(
            Types.newParameterizedType(List::class.java, InventoryQueueEntry::class.java),
        )
    }
    private val contextAdapter by lazy { moshi.adapter(CachedInventoryContext::class.java) }
    private val requestListAdapter by lazy {
        moshi.adapter<List<InventoryOfflineOperationRequest>>(
            Types.newParameterizedType(List::class.java, InventoryOfflineOperationRequest::class.java),
        )
    }
    private val entryFactory = InventoryQueueEntryFactory()
    private val fileStore by lazy {
        InventoryQueueFileStore(
            directory = directory,
            serialize = { listAdapter.toJson(it) },
            deserialize = { listAdapter.fromJson(it) },
            encrypt = { crypto.encryptAtRest(it) },
            decrypt = { crypto.decryptAtRest(it) },
        )
    }

    init {
        applicationScope.launch {
            var lastKey: String? = null
            sessionManager.state.collect { session ->
                val user = (session as? SessionState.Active)?.user
                val key = user?.let { accountKey(it.serverUrl, it.username) }
                if (key != lastKey) {
                    lastKey = key
                    if (key == null) {
                        if (accountForActiveUser() == null) {
                            activeEntries.value = emptyList()
                            activeReadError.value = null
                        }
                    } else {
                        val loaded = try {
                            load(key)
                        } catch (_: Exception) {
                            if (accountForActiveUser() == key) {
                                activeEntries.value = emptyList()
                                activeReadError.value = "Saved inventory queue could not be read. Existing operations were preserved; do not clear app data."
                            }
                            return@collect
                        }
                        if (accountForActiveUser() != key) return@collect
                        activeEntries.value = loaded
                        activeReadError.value = null
                        InventoryQueueReplayOrder.nextReplayable(loaded)?.let { first ->
                            schedule(key, first.locationId, retryDelaySeconds(first))
                        }
                    }
                }
            }
        }
    }

    suspend fun cacheInventoryContext(
        expectedScopeIdentity: String,
        productId: Long,
        locations: List<InventoryLocationDto>,
        placements: List<InventoryLocationPlacementDto>,
    ) = withContext(Dispatchers.IO) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return@withContext
        if (user.scopeIdentity != expectedScopeIdentity) return@withContext
        val key = accountKey(user.serverUrl, user.username)
        val file = File(directory, "context_${key}_$productId.json")
        val encrypted = crypto.encryptAtRest(contextAdapter.toJson(CachedInventoryContext(locations, placements)))
        runCatching { file.writeText(encrypted) }
    }

    suspend fun cachedInventoryContext(expectedScopeIdentity: String, productId: Long): Pair<List<InventoryLocationDto>, List<InventoryLocationPlacementDto>>? =
        withContext(Dispatchers.IO) {
            val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return@withContext null
            if (user.scopeIdentity != expectedScopeIdentity) return@withContext null
            val key = accountKey(user.serverUrl, user.username)
            val encrypted = runCatching { File(directory, "context_${key}_$productId.json").readText() }.getOrNull()
                ?: return@withContext null
            val plain = crypto.decryptAtRest(encrypted) ?: return@withContext null
            val saved = runCatching { contextAdapter.fromJson(plain) }.getOrNull() ?: return@withContext null
            saved.locations to saved.placements
        }

    suspend fun enqueueMovement(
        expectedScopeIdentity: String,
        productId: Long,
        productName: String,
        locationId: Long,
        locationName: String,
        kind: String,
        quantity: Double,
        note: String,
    ): InventoryQueueEntry {
        val user = (sessionManager.state.value as? SessionState.Active)?.user
            ?: error("Sign in before queueing an inventory operation.")
        require(user.scopeIdentity == expectedScopeIdentity) { "The active account changed; retry this operation." }
        val account = accountKey(user.serverUrl, user.username)
        val entry = entryFactory.movement(locationId, productId, productName, locationName, kind, quantity, note)
        return enqueue(account, entry)
    }

    suspend fun enqueueCount(
        expectedScopeIdentity: String,
        productId: Long,
        productName: String,
        locationId: Long,
        locationName: String,
        countedQuantity: Double,
        observedPlacement: InventoryLocationPlacementDto,
        note: String,
    ): InventoryQueueEntry {
        val user = (sessionManager.state.value as? SessionState.Active)?.user
            ?: error("Sign in before queueing an inventory operation.")
        require(user.scopeIdentity == expectedScopeIdentity) { "The active account changed; retry this operation." }
        val account = accountKey(user.serverUrl, user.username)
        val entry = entryFactory.count(
            locationId, productId, productName, locationName, countedQuantity, observedPlacement, note,
        )
        return enqueue(account, entry)
    }

    suspend fun discardBlockedOperation(operationId: String): Boolean = mutex.withLock {
        if (replayGate.isActive()) return@withLock false
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return@withLock false
        val key = accountKey(user.serverUrl, user.username)
        val current = load(key)
        publishIfActive(key, current)
        val index = current.indexOfFirst { it.operationId == operationId && it.status != InventoryQueueStatus.QUEUED.name }
        if (index < 0) return@withLock false
        val remaining = current.toMutableList().also { it.removeAt(index) }
        save(key, remaining)
        publishIfActive(key, remaining)
        InventoryQueueReplayOrder.nextReplayable(remaining)?.let { schedule(key, it.locationId) }
        true
    }

    suspend fun exportHeldWrites(): String? = mutex.withLock {
        if (replayGate.isActive()) return@withLock null
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return@withLock null
        val key = accountKey(user.serverUrl, user.username)
        val current = load(key)
        publishIfActive(key, current)
        val heldCount = current.count { it.status == InventoryQueueStatus.HELD.name }
        if (!InventoryHeldWriteActions.canExport(heldCount, replayGate.isActive())) return@withLock null
        InventoryHeldWriteActions.exportJson(current, requestListAdapter::toJson)
    }

    suspend fun discardHeldWrites(typedCount: Int): Boolean = mutex.withLock {
        val currentReplay = replayGate.isActive()
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return@withLock false
        val key = accountKey(user.serverUrl, user.username)
        val current = load(key)
        val heldCount = current.count { it.status == InventoryQueueStatus.HELD.name }
        if (!InventoryHeldWriteActions.canDiscard(heldCount, typedCount, currentReplay)) return@withLock false
        val remaining = current.filterNot { it.status == InventoryQueueStatus.HELD.name }
        save(key, remaining)
        publishIfActive(key, remaining)
        InventoryQueueReplayOrder.nextReplayable(remaining)?.let { schedule(key, it.locationId) }
        true
    }

    suspend fun retryConflict(operationId: String, freshPlacement: InventoryLocationPlacementDto?): Boolean = mutex.withLock {
        if (replayGate.isActive()) return@withLock false
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return@withLock false
        val key = accountKey(user.serverUrl, user.username)
        val current = load(key)
        val index = current.indexOfFirst {
            it.operationId == operationId && InventoryQueueConflictAction.canRetry(it.status, replayGate.isActive())
        }
        if (index < 0) return@withLock false
        val old = current[index]
        val updated = if (old.kind == "count") {
            val placement = freshPlacement?.takeIf { it.locationId == old.locationId && it.revision != null }
                ?: return@withLock false
            old.copy(
                operationId = java.util.UUID.randomUUID().toString(),
                observedQuantity = placement.quantity,
                observedRevision = placement.revision,
                clientOccurredAt = java.time.Instant.now().toString(),
                status = InventoryQueueStatus.QUEUED.name,
                failureCode = null,
                failureMessage = null,
                rateLimitAttempts = 0,
                retryNotBeforeEpochMillis = null,
            )
        } else {
            old.copy(
                operationId = java.util.UUID.randomUUID().toString(),
                status = InventoryQueueStatus.QUEUED.name,
                failureCode = null,
                failureMessage = null,
                rateLimitAttempts = 0,
                retryNotBeforeEpochMillis = null,
            )
        }
        val next = current.toMutableList().also { it[index] = updated }
        save(key, next)
        publishIfActive(key, next)
        schedule(key, updated.locationId)
        true
    }

    internal suspend fun beginReplay(account: String): Boolean = mutex.withLock {
        if (accountForActiveUser() != account || !replayGate.begin()) return@withLock false
        activeReplay.value = true
        true
    }

    internal suspend fun endReplay() = mutex.withLock {
        replayGate.end()
        activeReplay.value = false
    }

    internal suspend fun holdQueuedWrites(account: String, code: String, message: String) = mutex.withLock {
        val current = load(account)
        val next = current.map { entry ->
            if (entry.status == InventoryQueueStatus.QUEUED.name) {
                entry.copy(status = InventoryQueueStatus.HELD.name, failureCode = code, failureMessage = message)
            } else entry
        }
        save(account, next)
        publishIfActive(account, next)
    }

    private suspend fun enqueue(account: String, entry: InventoryQueueEntry): InventoryQueueEntry = mutex.withLock {
        val current = load(account)
        require(current.none {
            it.status in setOf(InventoryQueueStatus.CONFLICT.name, InventoryQueueStatus.HELD.name, InventoryQueueStatus.FAILED.name)
        }) {
            "Resolve the earlier blocked inventory operation before adding another."
        }
        require(current.size < 500) { "The offline inventory queue is full; reconnect and sync it before adding more." }
        val next = current + entry
        save(account, next)
        publishIfActive(account, next)
        schedule(account, entry.locationId)
        entry
    }

    internal suspend fun nextQueued(account: String, scheduledLocationId: Long): InventoryQueueEntry? = mutex.withLock {
        val items = load(account)
        publishIfActive(account, items)
        val first = InventoryQueueReplayOrder.nextReplayable(items) ?: return@withLock null
        val delaySeconds = retryDelaySeconds(first)
        if (first.locationId != scheduledLocationId || delaySeconds > 0L) {
            schedule(account, first.locationId, delaySeconds)
            return@withLock null
        }
        first
    }

    internal suspend fun markRateLimitedRetry(account: String, operationId: String, delaySeconds: Long) = mutex.withLock {
        val current = load(account)
        val item = current.firstOrNull { it.operationId == operationId } ?: return@withLock
        val retryAt = System.currentTimeMillis() + delaySeconds.coerceAtLeast(0L) * 1_000L
        val next = current.map {
            if (it.operationId == operationId) it.copy(
                rateLimitAttempts = it.rateLimitAttempts + 1,
                retryNotBeforeEpochMillis = retryAt,
            ) else it
        }
        save(account, next)
        publishIfActive(account, next)
        schedule(account, item.locationId, delaySeconds)
    }

    private fun retryDelaySeconds(entry: InventoryQueueEntry): Long {
        val remainingMillis = (entry.retryNotBeforeEpochMillis ?: return 0L) - System.currentTimeMillis()
        return if (remainingMillis <= 0L) 0L else (remainingMillis + 999L) / 1_000L
    }

    internal suspend fun markApplied(account: String, operationId: String) = mutex.withLock {
        val next = load(account).filterNot { it.operationId == operationId }
        save(account, next)
        publishIfActive(account, next)
    }

    internal suspend fun markBlocked(
        account: String,
        operationId: String,
        status: InventoryQueueStatus,
        code: String,
        message: String,
    ) = mutex.withLock {
        val next = load(account).map { entry ->
            if (entry.operationId == operationId) entry.copy(status = status.name, failureCode = code, failureMessage = message) else entry
        }
        save(account, next)
        publishIfActive(account, next)
    }

    internal fun accountForActiveUser(): String? = (sessionManager.state.value as? SessionState.Active)?.user?.let {
        accountKey(it.serverUrl, it.username)
    }

    internal fun activeUser() = (sessionManager.state.value as? SessionState.Active)?.user

    private fun publishIfActive(account: String, items: List<InventoryQueueEntry>) {
        if (accountForActiveUser() == account) {
            activeEntries.value = items
            activeReadError.value = null
        }
    }

    private suspend fun load(account: String): List<InventoryQueueEntry> = withContext(Dispatchers.IO) {
        fileStore.load(account)
    }

    private suspend fun save(account: String, items: List<InventoryQueueEntry>) = withContext(Dispatchers.IO) {
        fileStore.save(account, items)
    }

    private fun schedule(account: String, locationId: Long, initialDelaySeconds: Long = 0L) {
        val request = OneTimeWorkRequestBuilder<InventoryWriteWorker>()
            .setInputData(workDataOf(
                InventoryWriteWorker.INPUT_ACCOUNT to account,
                InventoryWriteWorker.INPUT_LOCATION_ID to locationId,
            ))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
            .setInitialDelay(initialDelaySeconds.coerceAtLeast(0L), TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("inventory-outbox-$account", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private fun accountKey(serverUrl: String, username: String): String {
        val raw = "${serverUrl.trim().lowercase()}|${username.trim().lowercase()}"
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).take(12)
            .joinToString("") { "%02x".format(it) }
    }
}

class InventoryWriteWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val account = inputData.getString(INPUT_ACCOUNT) ?: return Result.failure()
        val locationId = inputData.getLong(INPUT_LOCATION_ID, 0L).takeIf { it > 0 } ?: return Result.failure()
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, WorkerEntryPoint::class.java)
        val queue = entryPoint.inventoryWriteQueue()
        val activeUser = queue.activeUser() ?: return Result.success()
        if (queue.accountForActiveUser() != account) return Result.success()
        val api = entryPoint.apiProvider().apiForSession(activeUser.serverUrl) ?: return Result.retry()
        val bearer = entryPoint.tokenHolder().token?.takeIf(String::isNotBlank) ?: return Result.retry()
        val gateway = InventoryOperationGateway { idempotencyKey, body ->
            val response = api.submitInventoryOfflineOperation(idempotencyKey, "Bearer $bearer", body)
            if (response.isSuccessful) {
                InventoryHttpResult(
                    statusCode = response.code(),
                    responseOperationId = response.body()?.takeIf { it.success }?.data?.operationId,
                    errorCode = response.body()?.code,
                    errorMessage = response.body()?.error ?: response.body()?.message,
                )
            } else {
                val raw = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
                val json = runCatching { JSONObject(raw) }.getOrNull()
                InventoryHttpResult(
                    statusCode = response.code(),
                    errorCode = json?.optString("code")?.takeIf(String::isNotBlank) ?: "HTTP_${response.code()}",
                    errorMessage = json?.optString("error")?.takeIf(String::isNotBlank)
                        ?: json?.optString("message")?.takeIf(String::isNotBlank),
                    retryAfter = response.headers()["Retry-After"],
                )
            }
        }

        if (!queue.beginReplay(account)) return Result.success()
        return try {
            while (true) {
                if (queue.accountForActiveUser() != account) return Result.success()
                if (entryPoint.tokenHolder().token != bearer) return Result.retry()
                val item = try {
                    queue.nextQueued(account, locationId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    return Result.retry()
                } ?: return Result.success()
                try {
                    val exchange = InventoryReplayScope.submitAndDecideIfAllowed(
                        account, queue.accountForActiveUser(), locationId, item, gateway,
                    ) ?: return Result.success()
                    val (result, decision) = exchange
                    if (result.statusCode in 200..299) {
                        if (result.responseOperationId == item.operationId) {
                            queue.markApplied(account, item.operationId)
                            continue
                        }
                        queue.markBlocked(account, item.operationId, InventoryQueueStatus.FAILED,
                            result.errorCode ?: "INVALID_RESPONSE", result.errorMessage ?: "The server returned an incomplete inventory result.")
                        return Result.success()
                    }
                    when (decision) {
                        InventoryHttpDecision.Retry -> return Result.retry()
                        is InventoryHttpDecision.RetryAfter -> {
                            queue.markRateLimitedRetry(account, item.operationId, decision.seconds)
                            return Result.success()
                        }
                        InventoryHttpDecision.ParkRateLimited -> {
                            queue.markBlocked(account, item.operationId, InventoryQueueStatus.FAILED,
                                "RATE_LIMITED", InventoryRetryPolicy.RATE_LIMIT_PARKED_MESSAGE)
                            return Result.success()
                        }
                        InventoryHttpDecision.HoldUnsupportedServer -> {
                            queue.holdQueuedWrites(
                                account, "WORKER_MIGRATION_REQUIRED", InventoryRetryPolicy.MIGRATION_REQUIRED_MESSAGE,
                            )
                            return Result.success()
                        }
                        is InventoryHttpDecision.Terminal -> {
                            val message = result.errorMessage ?: "Inventory operation needs review (${result.statusCode})."
                            queue.markBlocked(
                                account, item.operationId,
                                if (decision.status == InventoryTerminalStatus.CONFLICT) InventoryQueueStatus.CONFLICT
                                else InventoryQueueStatus.REJECTED,
                                result.errorCode ?: "HTTP_${result.statusCode}", message,
                            )
                            if (decision.status == InventoryTerminalStatus.REJECTED) continue
                            return Result.success()
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: IOException) {
                    return Result.retry()
                } catch (_: Throwable) {
                    return Result.retry()
                }
            }
            Result.success()
        } finally {
            queue.endReplay()
        }
    }

    companion object {
        const val INPUT_ACCOUNT = "inventory_account"
        const val INPUT_LOCATION_ID = "inventory_location_id"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun inventoryWriteQueue(): InventoryWriteQueue
    fun apiProvider(): ApiProvider
    fun tokenHolder(): TokenHolder
}
