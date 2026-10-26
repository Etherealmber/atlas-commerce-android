package ma.etherealmber.atlascommerce.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import ma.etherealmber.atlascommerce.AtlasApp
import ma.etherealmber.atlascommerce.MainActivity
import ma.etherealmber.atlascommerce.R
import javax.inject.Inject

/**
 * v1.2 FCM bridge — receives worker pushes sent over FCM HTTP v1
 * (`android.notification.channel_id = "atlas_assist"`).
 *
 * Background messages carrying a `notification` block are shown by the system
 * tray automatically; this service covers foreground display, data-only
 * messages, and token rotation (re-registration with the worker).
 */
@AndroidEntryPoint
class AtlasFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var registrar: PushTokenRegistrar

    override fun onNewToken(token: String) {
        registrar.onTokenRefreshed(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Atlas Commerce"
        val body = message.notification?.body ?: message.data["body"]
        if (body.isNullOrBlank()) return
        showNotification(title, body, message.data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        runCatching {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                for ((key, value) in data) putExtra(key, value)
                if (data["type"].orEmpty().isNotBlank() || data.containsKey("action_id")) {
                    putExtra(AtlasApp.EXTRA_OPEN_NOTIFICATIONS, true)
                }
            }
            val pending = PendingIntent.getActivity(
                this,
                title.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(this, AtlasApp.ASSIST_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            val nm = getSystemService(NotificationManager::class.java) ?: return
            nm.notify(title.hashCode(), notification)
        }
    }
}
