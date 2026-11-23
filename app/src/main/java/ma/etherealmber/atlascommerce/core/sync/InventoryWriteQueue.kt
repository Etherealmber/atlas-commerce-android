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
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

internal enum class InventoryQueueStatus { QUEUED, CONFLICT, FAILED }

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
    private val activeEntries = MutableStateFlow<List<InventoryQueueEntry>>(emptyList())
    val entries: StateFlow<List<InventoryQueueEntry>> = activeEntries.asStateFlow()
    private val listAdapter by lazy {
        moshi.adapter<List<InventoryQueueEntry>>(
            Types.newParameterizedType(List::class.java, InventoryQueueEntry::class.java),
        )
    }
    private val contextAdapter by lazy { moshi.adapter(CachedInventoryContext::class.java) }

    init {
        applicationScope.launch {
            var lastKey: String? = null
            sessionManager.state.collect { session ->
                val user = (session as? SessionState.Active)?.user
                val key = user?.let { accountKey(it.serverUrl, it.username) }
                if (key != lastKey) {
                    lastKey = key
                    activeEntries.value = if (key == null) emptyList() else load(key)
                    if (key != null && load(key).any { it.status == InventoryQueueStatus.QUEUED.name }) schedule(key)
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
        require(kind == "receive" || kind == "issue")
        require(productId > 0 && locationId > 0 && quantity.isFinite() && quantity > 0.0)
        val user = (sessionManager.state.value as? SessionState.Active)?.user
            ?: error("Sign in before queueing an inventory operation.")
        require(user.scopeIdentity == expectedScopeIdentity) { "The active account changed; retry this operation." }
        val account = accountKey(user.serverUrl, user.username)
        return enqueue(account,
            InventoryQueueEntry(
                operationId = UUID.randomUUID().toString(),
                kind = kind,
                productId = productId,
                productName = productName,
                locationId = locationId,
                locationName = locationName,
                quantityDelta = quantity,
                note = note.trim().take(200),
                clientOccurredAt = Instant.now().toString(),
            ),
        )
    }

    suspend fun enqueueCount(
        expectedScopeIdentity: String,
        productId: Long,
        productName: String,
        locationId: Long,
        locationName: String,
        countedQuantity: Double,
        observedQuantity: Double,
        observedRevision: Long?,
        note: String,
    ): InventoryQueueEntry {
        require(productId > 0 && locationId > 0)
        require(countedQuantity.isFinite() && countedQuantity >= 0.0)
        require(observedQuantity.isFinite() && observedQuantity >= 0.0)
        require(observedRevision != null) { "A location revision is required for offline cycle counts." }
        val user = (sessionManager.state.value as? SessionState.Active)?.user
            ?: error("Sign in before queueing an inventory operation.")
        require(user.scopeIdentity == expectedScopeIdentity) { "The active account changed; retry this operation." }
        val account = accountKey(user.serverUrl, user.username)
        return enqueue(account,
            InventoryQueueEntry(
                operationId = UUID.randomUUID().toString(),
                kind = "count",
                productId = productId,
                productName = productName,
                locationId = locationId,
                locationName = locationName,
                countedQuantity = countedQuantity,
                observedQuantity = observedQuantity,
                observedRevision = observedRevision,
                note = note.trim().take(200),
                clientOccurredAt = Instant.now().toString(),
            ),
        )
    }

    suspend fun discardBlockedOperation(operationId: String): Boolean = mutex.withLock {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return@withLock false
        val key = accountKey(user.serverUrl, user.username)
        val current = load(key)
        val index = current.indexOfFirst { it.operationId == operationId && it.status != InventoryQueueStatus.QUEUED.name }
        if (index < 0) return@withLock false
        val remaining = current.toMutableList().also { it.removeAt(index) }
        save(key, remaining)
        publishIfActive(key, remaining)
        if (remaining.firstOrNull()?.status == InventoryQueueStatus.QUEUED.name) schedule(key)
        true
    }

    private suspend fun enqueue(account: String, entry: InventoryQueueEntry): InventoryQueueEntry = mutex.withLock {
        val current = load(account)
        require(current.none { it.status != InventoryQueueStatus.QUEUED.name }) {
            "Resolve the earlier blocked inventory operation before adding another."
        }
        require(current.size < 500) { "The offline inventory queue is full; reconnect and sync it before adding more." }
        val next = current + entry
        save(account, next)
        publishIfActive(account, next)
        schedule(account)
        entry
    }

    internal suspend fun nextQueued(account: String): InventoryQueueEntry? = mutex.withLock {
        val items = load(account)
        if (items.any { it.status != InventoryQueueStatus.QUEUED.name }) null else items.firstOrNull()
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
        if (accountForActiveUser() == account) activeEntries.value = items
    }

    private suspend fun load(account: String): List<InventoryQueueEntry> = withContext(Dispatchers.IO) {
        val file = File(directory, "queue_$account.json")
        val encrypted = runCatching { file.takeIf(File::exists)?.readText() }.getOrNull() ?: return@withContext emptyList()
        val plain = crypto.decryptAtRest(encrypted) ?: return@withContext emptyList()
        runCatching { listAdapter.fromJson(plain).orEmpty() }.getOrDefault(emptyList())
    }

    private suspend fun save(account: String, items: List<InventoryQueueEntry>) = withContext(Dispatchers.IO) {
        val file = File(directory, "queue_$account.json")
        if (items.isEmpty()) {
            runCatching { file.delete() }
            return@withContext
        }
        val encrypted = crypto.encryptAtRest(listAdapter.toJson(items))
        val temp = File(directory, "queue_$account.tmp")
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

    private fun schedule(account: String) {
        val request = OneTimeWorkRequestBuilder<InventoryWriteWorker>()
            .setInputData(workDataOf(InventoryWriteWorker.INPUT_ACCOUNT to account))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
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
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, WorkerEntryPoint::class.java)
        val queue = entryPoint.inventoryWriteQueue()
        val activeUser = queue.activeUser() ?: return Result.success()
        if (queue.accountForActiveUser() != account) return Result.success()
        val api = entryPoint.apiProvider().apiForSession(activeUser.serverUrl) ?: return Result.retry()
        val bearer = entryPoint.tokenHolder().token?.takeIf(String::isNotBlank) ?: return Result.retry()

        while (true) {
            if (queue.accountForActiveUser() != account) return Result.success()
            if (entryPoint.tokenHolder().token != bearer) return Result.retry()
            val item = queue.nextQueued(account) ?: return Result.success()
            try {
                val response = api.submitInventoryOfflineOperation(item.operationId, "Bearer $bearer", item.toRequest())
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data?.operationId == item.operationId) {
                        queue.markApplied(account, item.operationId)
                        continue
                    }
                    queue.markBlocked(account, item.operationId, InventoryQueueStatus.FAILED,
                        body?.code ?: "INVALID_RESPONSE", body?.error ?: body?.message ?: "The server returned an incomplete inventory result.")
                    return Result.success()
                }
                if (response.code() >= 500 || response.code() == 429) return Result.retry()
                val raw = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
                val json = runCatching { JSONObject(raw) }.getOrNull()
                val code = json?.optString("code")?.takeIf(String::isNotBlank) ?: "HTTP_${response.code()}"
                // Keep queued writes intact when the Worker route has not been deployed yet.
                if (response.code() == 404 && code == "HTTP_404") return Result.retry()
                val message = json?.optString("error")?.takeIf(String::isNotBlank)
                    ?: json?.optString("message")?.takeIf(String::isNotBlank)
                    ?: "Inventory operation needs review (${response.code()})."
                queue.markBlocked(account, item.operationId,
                    if (response.code() == 409 && code == "COUNT_CONFLICT") InventoryQueueStatus.CONFLICT else InventoryQueueStatus.FAILED,
                    code, message)
                return Result.success()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                return Result.retry()
            } catch (_: Throwable) {
                return Result.retry()
            }
        }
    }

    companion object { const val INPUT_ACCOUNT = "inventory_account" }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun inventoryWriteQueue(): InventoryWriteQueue
    fun apiProvider(): ApiProvider
    fun tokenHolder(): TokenHolder
}
