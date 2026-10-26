package ma.etherealmber.atlascommerce.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import org.json.JSONObject
import retrofit2.HttpException
import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.net.TokenHolder
import ma.etherealmber.atlascommerce.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v1.2 FCM bridge — registers this device's push token with the Atlas worker
 * (`POST /api/assist/device-token`, Bearer session) so server-side pushes
 * (assist approvals, finance reminders, stock alerts) reach Android.
 *
 * Graceful degradation: when `google-services.json` is absent (Firebase not
 * configured for this checkout), every entry point is a no-op — the app still
 * builds and runs normally, push simply stays inactive.
 */
@Singleton
class PushTokenRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiProvider: ApiProvider,
    private val tokenHolder: TokenHolder,
    @ApplicationScope private val scope: CoroutineScope,
) {

    fun firebaseConfigured(): Boolean =
        runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)

    /** Called whenever a session becomes active (restore, password login, QR pairing). */
    fun onSessionActive() {
        if (!firebaseConfigured()) {
            AtlasLogBuffer.log("PUSH", "FCM is not configured in this app build; push registration skipped", "WARN")
            return
        }
        scope.launch { registerWithRetry() }
    }

    /** Called by [AtlasFirebaseMessagingService] when FCM rotates the token. */
    fun onTokenRefreshed(token: String) {
        if (!firebaseConfigured() || !tokenHolder.hasSession()) return
        scope.launch { postWithRetry(token) }
    }

    suspend fun register(): Boolean {
        if (!firebaseConfigured() || !tokenHolder.hasSession()) return false
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (error: Exception) {
            AtlasLogBuffer.log("PUSH", "FCM token fetch failed: ${error.javaClass.simpleName}", "ERROR")
            return false
        }
        if (token.isBlank()) {
            AtlasLogBuffer.log("PUSH", "FCM returned an empty registration token", "ERROR")
            return false
        }
        return post(token)
    }

    private suspend fun registerWithRetry() {
        for ((attempt, pauseMs) in RETRY_DELAYS_MS.withIndex()) {
            if (pauseMs > 0) delay(pauseMs)
            if (!tokenHolder.hasSession()) return
            if (register()) return
            if (attempt < RETRY_DELAYS_MS.lastIndex) {
                AtlasLogBuffer.log("PUSH", "Will retry FCM token registration (${attempt + 1}/${RETRY_DELAYS_MS.lastIndex})")
            }
        }
    }

    private suspend fun postWithRetry(token: String) {
        for ((attempt, pauseMs) in RETRY_DELAYS_MS.withIndex()) {
            if (pauseMs > 0) delay(pauseMs)
            if (!tokenHolder.hasSession()) return
            if (post(token)) return
            if (attempt < RETRY_DELAYS_MS.lastIndex) {
                AtlasLogBuffer.log("PUSH", "Will retry refreshed FCM token registration (${attempt + 1}/${RETRY_DELAYS_MS.lastIndex})")
            }
        }
    }

    private suspend fun post(token: String): Boolean {
        if (!tokenHolder.hasSession()) return false
        return try {
            val result = apiProvider.api().registerDeviceToken(
                DeviceTokenRequest(token = token, platform = "android"),
            )
            if (result.success && result.data?.registered == true) {
                AtlasLogBuffer.log("PUSH", "FCM device token accepted by selected Worker")
                true
            } else {
                val detail = AtlasLogBuffer.sanitize(result.code ?: result.error ?: result.message.orEmpty()).take(160)
                AtlasLogBuffer.log("PUSH", "Worker did not confirm FCM token registration: ${detail.ifBlank { "empty response" }}", "ERROR")
                false
            }
        } catch (error: HttpException) {
            val body = runCatching { error.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
            val payload = runCatching { JSONObject(body) }.getOrNull()
            val detail = AtlasLogBuffer.sanitize(
                payload?.optString("code")?.takeIf { it.isNotBlank() }
                    ?: payload?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: payload?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: "no server error detail",
            ).take(160)
            AtlasLogBuffer.log("PUSH", "FCM token registration failed: HTTP ${error.code()}; $detail", "ERROR")
            false
        } catch (error: Exception) {
            AtlasLogBuffer.log("PUSH", "FCM token registration failed: ${error.javaClass.simpleName}", "ERROR")
            false
        }
    }

    private companion object {
        val RETRY_DELAYS_MS = longArrayOf(0L, 60_000L, 5 * 60_000L, 20 * 60_000L)
    }
}
