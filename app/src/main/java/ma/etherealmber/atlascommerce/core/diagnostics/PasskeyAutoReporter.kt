package ma.etherealmber.atlascommerce.core.diagnostics

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.model.UserReportRequest
import ma.etherealmber.atlascommerce.data.repo.AuthRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class PasskeyFailureStage { LOGIN_CAPABILITY, LOGIN_OPTIONS, LOGIN_VERIFY, LOGIN_SESSION, LOGIN_PROVIDER, LOGIN_TIMEOUT, REGISTRATION_CAPABILITY, REGISTRATION_OPTIONS, REGISTRATION_VERIFY, REGISTRATION_NO_PROVIDER, REGISTRATION_PROVIDER, REGISTRATION_TIMEOUT }

/** Only fixed, non-secret metadata is included. Credential JSON and logs never leave the device here. */
internal fun passkeyAutomaticReport(stage: PasskeyFailureStage, status: Int?, sdk: Int): String = buildString {
    appendLine("Automatic Android passkey failure")
    appendLine("Stage: ${stage.name}")
    appendLine("HTTP status: ${status?.takeIf { it in 100..599 } ?: "not available"}")
    appendLine("Android SDK: $sdk")
    appendLine("App: ${AppDetails.VERSION_NAME} (${AppDetails.VERSION_CODE})")
    append("@atlas-bot please investigate the failure stage and suggest diagnostic steps; do not assume a root cause.")
}

/** Latest pending failure stays local until the same server has an authenticated session. */
@Singleton
class PasskeyAutoReporter @Inject constructor(
    @ApplicationContext context: Context,
    private val sessions: SessionManager,
    private val serverConfig: ServerConfig,
    private val authRepository: AuthRepository,
) {
    private val prefs = context.getSharedPreferences("atlas_passkey_auto_report", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    fun record(stage: PasskeyFailureStage, status: Int? = null, attemptServer: String?) {
        val server = ServerUrl.normalize(attemptServer) ?: return
        if (ServerUrl.normalize(serverConfig.selection.value?.baseUrl) != server) return
        prefs.edit().putString("id", UUID.randomUUID().toString())
            .putString("server", server).putString("stage", stage.name)
            .putInt("status", status?.takeIf { it in 100..599 } ?: 0).apply()
    }

    fun clear() { prefs.edit().clear().apply() }

    suspend fun flush() = mutex.withLock {
        val id = prefs.getString("id", null) ?: return@withLock
        val server = prefs.getString("server", null) ?: return@withLock
        val stage = prefs.getString("stage", null)?.let { value ->
            PasskeyFailureStage.entries.firstOrNull { it.name == value }
        } ?: return@withLock
        val current = ServerUrl.normalize(serverConfig.selection.value?.baseUrl)
        val user = (sessions.state.value as? SessionState.Active)?.user ?: return@withLock
        if (current != server || ServerUrl.normalize(user.serverUrl) != server) return@withLock
        // Avoid repeatedly opening the same issue during a rapid series of retries.
        val signature = "$server|${stage.name}|${prefs.getInt("status", 0)}"
        val now = System.currentTimeMillis()
        val last = prefs.getLong("sent_at", 0)
        if (prefs.getString("sent_signature", null) == signature && now >= last && now - last < 300_000L) {
            if (prefs.getString("id", null) == id) prefs.edit().remove("id").apply()
            return@withLock
        }
        val status = prefs.getInt("status", 0).takeIf { it != 0 }
        val result = authRepository.submitUserReport(UserReportRequest(
            kind = "bug",
            message = passkeyAutomaticReport(stage, status, Build.VERSION.SDK_INT),
            page = "Android passkey",
            version = "${AppDetails.VERSION_NAME} (${AppDetails.VERSION_CODE})",
        ))
        if (result is ApiResult.Success) {
            val edit = prefs.edit().putString("sent_signature", signature).putLong("sent_at", now)
            if (prefs.getString("id", null) == id) edit.remove("id")
            edit.apply()
            AtlasLogBuffer.log("REPORT", "Automatic passkey report accepted; issueCreated=${!result.data.issueUrl.isNullOrBlank()}")
        } else {
            AtlasLogBuffer.log("REPORT", "Automatic passkey report could not be sent; retained locally", "WARN")
        }
    }
}
