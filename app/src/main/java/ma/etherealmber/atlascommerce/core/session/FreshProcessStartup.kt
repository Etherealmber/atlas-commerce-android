package ma.etherealmber.atlascommerce.core.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.data.model.MeDto

internal enum class StartupResumeResult {
    PREPARED,
    NO_SESSION,
    RESTORE_DISABLED,
    NO_SELECTED_SERVER,
    INVALID_SESSION_ORIGIN,
    DIFFERENT_ORIGIN,
    UNDECRYPTABLE_TOKEN,
}

internal class StartupResumeRequest(
    val serverUrl: String,
    val bearerToken: String,
) {
    override fun toString(): String = "StartupResumeRequest(serverUrl=$serverUrl, bearerToken=[REDACTED])"
}

internal data class StartupResumePlan(
    val result: StartupResumeResult,
    val request: StartupResumeRequest? = null,
)

/** A narrow policy seam: only a valid saved session on the currently selected origin may be sent. */
internal object StartupResumePreparation {
    const val NETWORK_TIMEOUT_MS = 5_000L

    /** Snapshot and validate local data before attempting to restore the session. */
    fun createRequest(
        previousSession: AtlasPreferences.StoredSession?,
        selectedServerUrl: String?,
        restoreDisabled: Boolean,
        decryptToken: (String) -> String?,
    ): StartupResumePlan {
        if (restoreDisabled) return StartupResumePlan(StartupResumeResult.RESTORE_DISABLED)
        val stored = previousSession ?: return StartupResumePlan(StartupResumeResult.NO_SESSION)
        val selectedServer = ServerUrl.normalize(selectedServerUrl)
            ?: return StartupResumePlan(StartupResumeResult.NO_SELECTED_SERVER)
        val selectedOrigin = ServerUrl.origin(selectedServer)
            ?: return StartupResumePlan(StartupResumeResult.NO_SELECTED_SERVER)
        val storedOrigin = ServerUrl.origin(stored.serverUrl)
            ?: return StartupResumePlan(StartupResumeResult.INVALID_SESSION_ORIGIN)
        if (storedOrigin != selectedOrigin) {
            return StartupResumePlan(StartupResumeResult.DIFFERENT_ORIGIN)
        }

        val token = try {
            decryptToken(stored.tokenCipher)?.takeIf(String::isNotBlank)
        } catch (_: Throwable) {
            null
        } ?: return StartupResumePlan(StartupResumeResult.UNDECRYPTABLE_TOKEN)

        return StartupResumePlan(
            result = StartupResumeResult.PREPARED,
            request = StartupResumeRequest(selectedServer, token),
        )
    }

    /** A session is restored only after the selected server confirms it through `/api/auth/me`. */
    suspend fun verify(
        request: StartupResumeRequest,
        timeoutMillis: Long = NETWORK_TIMEOUT_MS,
        validate: suspend (serverUrl: String, bearerToken: String) -> ApiResult<MeDto>,
    ): StartupSessionVerification {
        val response = try {
            withTimeoutOrNull(timeoutMillis) {
                validate(request.serverUrl, request.bearerToken)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            return StartupSessionVerification.Unavailable
        } ?: return StartupSessionVerification.Unavailable

        return when (response) {
            is ApiResult.Success -> if (response.data.ok) {
                StartupSessionVerification.Valid(response.data)
            } else {
                StartupSessionVerification.Rejected
            }
            is ApiResult.Failure -> if (
                response.httpStatus == 401 || response.httpStatus == 403 ||
                    response.code == "SESSION_EXPIRED" || response.code.contains("REVOK", ignoreCase = true)
            ) {
                StartupSessionVerification.Rejected
            } else {
                StartupSessionVerification.Unavailable
            }
        }
    }
}

internal sealed interface StartupSessionVerification {
    data class Valid(val profile: MeDto) : StartupSessionVerification
    data object Rejected : StartupSessionVerification
    data object Unavailable : StartupSessionVerification
}

internal enum class FreshProcessSessionOutcome {
    RESTORED,
    NO_SESSION,
    REJECTED,
    UNAVAILABLE,
}

/** Restores only server-validated sessions; explicit sign-out or rejected tokens stay signed out. */
internal class FreshProcessStartup(
    private val restorePreviousSession: suspend () -> FreshProcessSessionOutcome,
    private val clearStoredSession: suspend () -> Unit,
    private val purgeActiveState: () -> Unit,
    private val publishSignedOut: () -> Unit,
    private val releaseStartupGate: () -> Unit,
    private val onFailure: (Throwable) -> Unit = {},
) {
    suspend fun run(): FreshProcessSessionOutcome {
        var outcome = FreshProcessSessionOutcome.UNAVAILABLE
        try {
            outcome = restorePreviousSession()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            runCatching { onFailure(error) }
            outcome = FreshProcessSessionOutcome.UNAVAILABLE
        } finally {
            withContext(NonCancellable) {
                if (outcome != FreshProcessSessionOutcome.RESTORED) {
                    if (outcome != FreshProcessSessionOutcome.UNAVAILABLE) {
                        runCatching { clearStoredSession() }
                    }
                    runCatching { purgeActiveState() }
                    try {
                        publishSignedOut()
                    } finally {
                        releaseStartupGate()
                    }
                } else {
                    releaseStartupGate()
                }
            }
        }
        return outcome
    }
}
