package ma.etherealmber.atlascommerce.core.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences

internal enum class StartupResumeResult {
    PREPARED,
    NO_SESSION,
    RESTORE_DISABLED,
    NO_SELECTED_SERVER,
    INVALID_SESSION_ORIGIN,
    DIFFERENT_ORIGIN,
    UNDECRYPTABLE_TOKEN,
    SERVER_REJECTED,
    NETWORK_FAILED,
    TIMED_OUT,
}

/** A narrow policy seam: only a valid saved session on the currently selected origin may be sent. */
internal object StartupResumePreparation {
    const val NETWORK_TIMEOUT_MS = 1_500L

    suspend fun prepare(
        previousSession: AtlasPreferences.StoredSession?,
        selectedServerUrl: String?,
        restoreDisabled: Boolean,
        decryptToken: (String) -> String?,
        timeoutMillis: Long = NETWORK_TIMEOUT_MS,
        notifyServer: suspend (serverUrl: String, bearerToken: String) -> ApiResult<Unit>,
    ): StartupResumeResult {
        if (restoreDisabled) return StartupResumeResult.RESTORE_DISABLED
        val stored = previousSession ?: return StartupResumeResult.NO_SESSION
        val selectedServer = ServerUrl.normalize(selectedServerUrl)
            ?: return StartupResumeResult.NO_SELECTED_SERVER
        val selectedOrigin = ServerUrl.origin(selectedServer)
            ?: return StartupResumeResult.NO_SELECTED_SERVER
        val storedOrigin = ServerUrl.origin(stored.serverUrl)
            ?: return StartupResumeResult.INVALID_SESSION_ORIGIN
        if (storedOrigin != selectedOrigin) return StartupResumeResult.DIFFERENT_ORIGIN

        val token = try {
            decryptToken(stored.tokenCipher)?.takeIf(String::isNotBlank)
        } catch (_: Throwable) {
            null
        } ?: return StartupResumeResult.UNDECRYPTABLE_TOKEN

        val response = try {
            withTimeoutOrNull(timeoutMillis) {
                notifyServer(selectedServer, token)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            return StartupResumeResult.NETWORK_FAILED
        } ?: return StartupResumeResult.TIMED_OUT

        return when (response) {
            is ApiResult.Success -> StartupResumeResult.PREPARED
            is ApiResult.Failure -> StartupResumeResult.SERVER_REJECTED
        }
    }
}

/** Guarantees cold-start cleanup and SignedOut publication, even when preparation fails/cancels. */
internal class FreshProcessStartup(
    private val preparePreviousSession: suspend () -> Unit,
    private val clearStoredSession: suspend () -> Unit,
    private val purgeActiveState: () -> Unit,
    private val publishSignedOut: () -> Unit,
    private val releaseStartupGate: () -> Unit,
    private val onFailure: (Throwable) -> Unit = {},
) {
    suspend fun run() {
        try {
            preparePreviousSession()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            runCatching { onFailure(error) }
        } finally {
            withContext(NonCancellable) {
                runCatching { clearStoredSession() }
                runCatching { purgeActiveState() }
                try {
                    publishSignedOut()
                } finally {
                    releaseStartupGate()
                }
            }
        }
    }
}
