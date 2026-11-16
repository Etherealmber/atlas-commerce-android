package ma.etherealmber.atlascommerce.core.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.model.MobilePreferencesDto
import ma.etherealmber.atlascommerce.data.model.MobilePreferencesPutRequest
import ma.etherealmber.atlascommerce.data.model.hasValidContractPayload
import ma.etherealmber.atlascommerce.data.model.isSuccessfulWriteAfter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.di.ApplicationScope
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed interface MobilePreferenceSyncState {
    data object Idle : MobilePreferenceSyncState
    data object Loading : MobilePreferenceSyncState
    data class Synced(val response: MobilePreferencesDto) : MobilePreferenceSyncState
    data class Conflict(val currentRevision: Long?, val message: String) : MobilePreferenceSyncState
    data class Cooldown(val retryAfterSeconds: Long, val message: String) : MobilePreferenceSyncState
    data class Offline(val message: String) : MobilePreferenceSyncState
    data class Error(val message: String) : MobilePreferenceSyncState
}

/** Session-bound, allowlist-only client for the unwrapped mobile preferences resource. */
@Singleton
class MobilePreferencesSyncRepository @Inject constructor(
    private val apiProvider: ApiProvider,
    private val sessionManager: SessionManager,
    private val preferences: AtlasPreferences,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<MobilePreferenceSyncState>(MobilePreferenceSyncState.Idle)
    private val mutableLastSuccessfulSyncAt = MutableStateFlow<String?>(null)
    private val syncMutex = Mutex()
    val state: StateFlow<MobilePreferenceSyncState> = mutableState.asStateFlow()
    val lastSuccessfulSyncAt: StateFlow<String?> = mutableLastSuccessfulSyncAt.asStateFlow()

    init {
        applicationScope.launch {
            var previousScope: String? = null
            var hasObservedSession = false
            sessionManager.state.collect { session ->
                if (session is SessionState.Loading) return@collect
                val nextScope = (session as? SessionState.Active)?.user?.scopeIdentity
                if (!hasObservedSession || previousScope != nextScope) {
                    cancelScheduledWork()
                    hasObservedSession = true
                    previousScope = nextScope
                    mutableState.value = MobilePreferenceSyncState.Idle
                    mutableLastSuccessfulSyncAt.value = null
                    if (nextScope != null) launch {
                        val user = activeUser()
                        if (user?.scopeIdentity == nextScope) {
                            val preferenceScope = "${user.serverUrl.trim().lowercase()}|${user.username.trim().lowercase()}"
                            mutableLastSuccessfulSyncAt.value = preferences.mobilePreferenceSyncMetadata(preferenceScope)?.lastSyncedAt
                            refreshStatus()
                        }
                    }
                }
            }
        }
    }

    suspend fun refreshStatus(): MobilePreferenceSyncState = perform("manual", write = false)

    suspend fun syncNow(): MobilePreferenceSyncState = perform("manual", write = true)

    suspend fun runScheduled(expectedScopeHash: String): MobilePreferenceSyncState {
        val user = activeUser() ?: return MobilePreferenceSyncState.Error("No active account.")
        if (mobileScopeHash(user.scopeIdentity) != expectedScopeHash) {
            cancelScheduledWork()
            return MobilePreferenceSyncState.Error("The active account changed; scheduled sync was cancelled.")
        }
        return perform("scheduled", write = true)
    }

    private suspend fun perform(syncType: String, write: Boolean): MobilePreferenceSyncState =
        syncMutex.withLock { performLocked(syncType, write) }

    private suspend fun performLocked(syncType: String, write: Boolean): MobilePreferenceSyncState {
        val user = activeUser() ?: return publish(MobilePreferenceSyncState.Error("Sign in to sync preferences."))
        val scope = user.scopeIdentity
        val preferenceScope = "${user.serverUrl.trim().lowercase()}|${user.username.trim().lowercase()}"
        val api = apiProvider.apiForSession(user.serverUrl)
            ?: return publish(MobilePreferenceSyncState.Error("The active server changed; try again."))
        mutableState.value = MobilePreferenceSyncState.Loading
        try {
            val remote = api.getMobilePreferences()
            if (!remote.hasValidContractPayload()) {
                return publish(MobilePreferenceSyncState.Error("The server returned incomplete or invalid preferences; nothing was applied."))
            }
            if (!isStillActive(scope)) return MobilePreferenceSyncState.Idle
            if (!preferences.hasAtLeastKnownMobilePreferenceRevision(remote.revision, preferenceScope)) {
                return publish(MobilePreferenceSyncState.Conflict(null, "The server response is older than this account's last accepted revision."))
            }
            if (!preferences.recordMobilePreferenceRevision(remote.revision, preferenceScope)) {
                return if (isStillActive(scope)) publish(MobilePreferenceSyncState.Error("The active account changed before its revision could be recorded.")) else MobilePreferenceSyncState.Idle
            }
            if (!isStillActive(scope)) return MobilePreferenceSyncState.Idle
            runCatching {
                preferences.recordMobilePreferenceMetadata(
                    remote.revision, remote.serverTime, remote.syncFrequency, remote.lastSyncedAt, remote.nextSyncAt, preferenceScope,
                )
            }
            mutableLastSuccessfulSyncAt.value = remote.lastSyncedAt
            val frequencyDirty = preferences.mobilePreferenceFrequencyDirty(preferenceScope)
            if (!frequencyDirty) scheduleFromServer(remote.serverTime, remote.nextSyncAt, scope)
            else if (!write) cancelScheduledWork()
            if (remote.lastSyncedAt != null) {
                if (!preferences.applyRemoteMobilePreferenceSettings(remote.settings, preferenceScope)) {
                    return publish(MobilePreferenceSyncState.Error("Could not safely merge the validated server settings for this account."))
                }
            }
            if (!write) {
                if (!frequencyDirty) {
                    preferences.acceptServerMobilePreferenceFrequency(remote.syncFrequency, preferenceScope)
                }
                return publish(MobilePreferenceSyncState.Synced(remote))
            }
            if (syncType == "scheduled" && frequencyDirty) {
                cancelScheduledWork()
                return publish(MobilePreferenceSyncState.Synced(remote))
            }
            if (syncType == "scheduled" && !frequencyDirty) {
                preferences.acceptServerMobilePreferenceFrequency(remote.syncFrequency, preferenceScope)
            }
            if (syncType == "scheduled") {
                val serverDelay = serverRelativeDelayMillis(remote.serverTime, remote.nextSyncAt)
                if (serverDelay == null || serverDelay > 0L) {
                    if (serverDelay != null) enqueue(scope, serverDelay)
                    return publish(MobilePreferenceSyncState.Synced(remote))
                }
            }

            val frequency = preferences.mobilePreferenceFrequency()
            val settings = preferences.mobilePreferenceSettings()
            val request = MobilePreferencesPutRequest(
                revision = remote.revision,
                syncType = syncType,
                settings = settings,
                syncFrequency = frequency,
            )
            if (encodedBodyBytes(request) > 4096) {
                return publish(MobilePreferenceSyncState.Error("The preference request exceeds the server's 4 KB limit."))
            }
            val saved = api.putMobilePreferences(request)
            if (!saved.isSuccessfulWriteAfter(remote.revision)) {
                return publish(MobilePreferenceSyncState.Error("The server returned incomplete, stale, or invalid preferences; nothing was applied."))
            }
            if (!isStillActive(scope)) return MobilePreferenceSyncState.Idle
            if (!preferences.recordMobilePreferenceRevision(saved.revision, preferenceScope)) {
                return if (isStillActive(scope)) publish(MobilePreferenceSyncState.Error("The active account changed before the successful revision could be saved.")) else MobilePreferenceSyncState.Idle
            }
            if (!preferences.applySuccessfulMobilePreferenceSettings(saved.settings, settings, preferenceScope)) {
                return publish(MobilePreferenceSyncState.Error("Preferences could not be safely saved on this device."))
            }
            runCatching {
                preferences.recordMobilePreferenceMetadata(
                    saved.revision, saved.serverTime, saved.syncFrequency, saved.lastSyncedAt, saved.nextSyncAt, preferenceScope,
                )
            }
            mutableLastSuccessfulSyncAt.value = saved.lastSyncedAt
            val frequencyAccepted = preferences.acceptSuccessfulMobilePreferenceFrequency(
                serverFrequency = saved.syncFrequency,
                submittedFrequency = frequency,
                expectedScope = preferenceScope,
            )
            if (frequencyAccepted) scheduleFromServer(saved.serverTime, saved.nextSyncAt, scope)
            else cancelScheduledWork()
            return publish(MobilePreferenceSyncState.Synced(saved))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (http: HttpException) {
            if (!isStillActive(scope)) return MobilePreferenceSyncState.Idle
            return publish(handleHttpFailure(http, scope))
        } catch (io: IOException) {
            return publish(MobilePreferenceSyncState.Offline(io.localizedMessage ?: "You're offline. Connect and try again."))
        } catch (error: Exception) {
            return publish(MobilePreferenceSyncState.Error(error.localizedMessage ?: "Preference sync failed."))
        }
    }

    private fun handleHttpFailure(error: HttpException, scope: String): MobilePreferenceSyncState {
        val response = error.response()
        val raw = runCatching { response?.errorBody()?.string() }.getOrNull().orEmpty()
        val payload = parseMobilePreferenceError(raw, response?.headers()?.get("Retry-After"))
        val code = payload.code.orEmpty()
        val message = payload.message?.takeIf { it.isNotBlank() } ?: "Preference sync failed (HTTP ${error.code()})."
        return when {
            error.code() == 409 && code == "CONFLICT" -> MobilePreferenceSyncState.Conflict(
                currentRevision = payload.revision,
                message = "Preferences changed on another device. Refresh the server copy, then sync again.",
            )
            error.code() == 409 && code == "NOT_DUE" -> {
                scheduleFromServer(payload.serverTime, payload.nextSyncAt, scope)
                MobilePreferenceSyncState.Error("Scheduled sync is not due yet; it has been rescheduled.")
            }
            error.code() == 429 -> {
                val seconds = payload.retryAfterSeconds ?: 300L
                scheduleAfterServerDelay(payload.serverTime, seconds, scope)
                MobilePreferenceSyncState.Cooldown(seconds, message)
            }
            else -> MobilePreferenceSyncState.Error(message)
        }
    }

    private fun scheduleFromServer(serverTime: String?, nextSyncAt: String?, scope: String) {
        val delay = serverRelativeDelayMillis(serverTime, nextSyncAt) ?: return cancelScheduledWork()
        enqueue(scope, delay)
    }

    private fun scheduleAfterServerDelay(serverTime: String?, seconds: Long, scope: String) {
        val serverNow = serverTime?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return
        val target = runCatching { serverNow.plusSeconds(seconds.coerceIn(1, 86_400)) }.getOrNull() ?: return
        val delay = Duration.between(serverNow, target).toMillis().coerceAtLeast(1_000L)
        enqueue(scope, delay)
    }

    private fun enqueue(scope: String, delayMillis: Long) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val request = OneTimeWorkRequestBuilder<MobilePreferenceSyncWorker>()
            .setInputData(workDataOf(MobilePreferenceSyncWorker.KEY_SCOPE_HASH to mobileScopeHash(scope)))
            .setInitialDelay(delayMillis.coerceAtLeast(1_000L), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(MobilePreferenceSyncWorker.WORK_TAG)
            .addTag("${MobilePreferenceSyncWorker.WORK_TAG}:${mobileScopeHash(scope)}")
            .build()
        workManager.cancelAllWorkByTag(MobilePreferenceSyncWorker.WORK_TAG)
        workManager.enqueueUniqueWork(
            MobilePreferenceSyncWorker.uniqueName(scope),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelScheduledWork() {
        WorkManager.getInstance(context.applicationContext).cancelAllWorkByTag(MobilePreferenceSyncWorker.WORK_TAG)
    }

    private fun activeUser() = (sessionManager.state.value as? SessionState.Active)?.user
    private fun isStillActive(scope: String) = activeUser()?.scopeIdentity == scope
    private fun publish(value: MobilePreferenceSyncState): MobilePreferenceSyncState {
        mutableState.value = value
        return value
    }
}

internal data class MobilePreferenceErrorPayload(
    val code: String?,
    val message: String?,
    val revision: Long?,
    val serverTime: String?,
    val nextSyncAt: String?,
    val retryAfterSeconds: Long?,
)

internal fun parseMobilePreferenceError(raw: String, retryAfterHeader: String? = null): MobilePreferenceErrorPayload {
    val json = MobilePreferenceJson.decodeObject(raw)
    val bodyRetry = (json?.get("retry_after_seconds") as? Number)?.toLong()?.takeIf { it in 1L..86_400L }
    val headerRetry = retryAfterHeader?.trim()?.toLongOrNull()?.takeIf { it in 1L..86_400L }
    val revision = (json?.get("revision") as? Number)?.toLong()?.takeIf { it >= 0 }
    return MobilePreferenceErrorPayload(
        code = (json?.get("code") as? String)?.takeIf { it.isNotBlank() },
        message = (json?.get("error") as? String)?.takeIf { it.isNotBlank() }
            ?: (json?.get("message") as? String)?.takeIf { it.isNotBlank() },
        revision = revision,
        serverTime = (json?.get("server_time") as? String)?.takeIf { it.isNotBlank() },
        nextSyncAt = (json?.get("next_sync_at") as? String)?.takeIf { it.isNotBlank() },
        retryAfterSeconds = bodyRetry ?: headerRetry,
    )
}

internal fun serverRelativeDelayMillis(serverTime: String?, nextSyncAt: String?): Long? {
    val now = serverTime?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    val next = nextSyncAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    return runCatching { Duration.between(now, next).toMillis().coerceAtLeast(0L) }.getOrNull()
}

internal fun mobileScopeHash(scope: String): String = MessageDigest.getInstance("SHA-256")
    .digest(scope.toByteArray(Charsets.UTF_8))
    .take(12)
    .joinToString("") { "%02x".format(it.toInt() and 0xff) }

private fun encodedBodyBytes(request: MobilePreferencesPutRequest): Int {
    val settings = JSONObject(request.settings)
    val json = JSONObject()
        .put("schema_version", request.schemaVersion)
        .put("revision", request.revision)
        .put("sync_type", request.syncType)
        .put("settings", settings)
        .put("sync_frequency", request.syncFrequency)
    return json.toString().toByteArray(Charsets.UTF_8).size
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MobilePreferenceWorkerEntryPoint {
    fun mobilePreferencesSyncRepository(): MobilePreferencesSyncRepository
}

class MobilePreferenceSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val scopeHash = inputData.getString(KEY_SCOPE_HASH) ?: return Result.failure()
        val repository = EntryPointAccessors.fromApplication(
            applicationContext,
            MobilePreferenceWorkerEntryPoint::class.java,
        ).mobilePreferencesSyncRepository()
        return when (repository.runScheduled(scopeHash)) {
            is MobilePreferenceSyncState.Synced, is MobilePreferenceSyncState.Cooldown -> Result.success()
            is MobilePreferenceSyncState.Offline -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        const val KEY_SCOPE_HASH = "mobile_preference_scope_hash"
        const val WORK_TAG = "atlas-mobile-preferences-sync"
        fun uniqueName(scope: String) = "$WORK_TAG:${mobileScopeHash(scope)}"
    }
}
