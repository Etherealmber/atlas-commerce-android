package ma.etherealmber.atlascommerce.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
        if (!firebaseConfigured()) return
        scope.launch { runCatching { register() } }
    }

    /** Called by [AtlasFirebaseMessagingService] when FCM rotates the token. */
    fun onTokenRefreshed(token: String) {
        if (!firebaseConfigured() || !tokenHolder.hasSession()) return
        scope.launch { runCatching { post(token) } }
    }

    suspend fun register() {
        if (!firebaseConfigured() || !tokenHolder.hasSession()) return
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        if (token.isNullOrBlank()) return
        post(token)
    }

    private suspend fun post(token: String) {
        if (!tokenHolder.hasSession()) return
        runCatching {
            apiProvider.api().registerDeviceToken(DeviceTokenRequest(token = token, platform = "android"))
        }
    }
}
