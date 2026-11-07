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
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.ui.i18n.notificationText
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
        val language = (application as? AtlasApp)?.activeLanguage?.value ?: AppLanguage.FRENCH
        val fallbackBody = notificationText(
            language,
            "Vous avez une nouvelle notification Atlas Commerce.",
            "You have a new Atlas Commerce notification.",
            "لديك إشعار جديد من Atlas Commerce.",
        )
        val content = resolvePushNotificationContent(
            notificationTitle = message.notification?.title,
            notificationBody = message.notification?.body,
            data = message.data,
            fallbackBody = fallbackBody,
        ) ?: return
        showNotification(content.first, content.second, message.data, message.messageId)
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        messageId: String?,
    ) {
        runCatching {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                for ((key, value) in data) putExtra(key, value)
                // Worker business pushes use `event` (for example stock_request_decided),
                // while older notification payloads use `type` / `action_id`.
                if (data["type"].orEmpty().isNotBlank() ||
                    data["event"].orEmpty().isNotBlank() ||
                    data.containsKey("action_id")
                ) {
                    putExtra(AtlasApp.EXTRA_OPEN_NOTIFICATIONS, true)
                }
            }
            val notificationId = messageId?.hashCode() ?: System.currentTimeMillis().toInt()
            val pending = PendingIntent.getActivity(
                this,
                notificationId,
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
            val channel = nm.getNotificationChannel(AtlasApp.ASSIST_CHANNEL_ID)
            if (channel == null) {
                AtlasLogBuffer.log("PUSH", "Incoming push could not be shown because its notification channel is missing", "WARN")
                return@runCatching
            }
            if (!nm.areNotificationsEnabled() || channel.importance == NotificationManager.IMPORTANCE_NONE) {
                AtlasLogBuffer.log(
                    tag = "PUSH",
                    message = "Incoming push could not be shown because app or assistant-channel notifications are disabled",
                    level = "WARN",
                )
                return@runCatching
            }
            nm.notify(notificationId, notification)
        }.onFailure { error ->
            AtlasLogBuffer.log(
                tag = "PUSH",
                message = "Could not display incoming push (${error.javaClass.simpleName})",
                level = "WARN",
            )
        }
    }
}

/** Resolve common server payload conventions without logging or exposing payload contents. */
internal fun resolvePushNotificationContent(
    notificationTitle: String?,
    notificationBody: String?,
    data: Map<String, String>,
    fallbackBody: String = "You have a new Atlas Commerce notification.",
): Pair<String, String>? {
    fun firstNonBlank(vararg values: String?): String? =
        values.firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotEmpty) }

    val dataByLowercaseKey = data.entries.associate { it.key.lowercase() to it.value }
    fun dataValue(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { dataByLowercaseKey[it.lowercase()]?.trim()?.takeIf(String::isNotEmpty) }

    val title = firstNonBlank(
        notificationTitle,
        dataValue("title", "notification_title", "notificationTitle", "subject"),
    ) ?: "Atlas Commerce"
    val body = firstNonBlank(
        notificationBody,
        dataValue("body", "message", "text", "content", "description", "notification_body", "notificationBody"),
    ) ?: if (data.isNotEmpty()) fallbackBody else return null

    return title to body
}
