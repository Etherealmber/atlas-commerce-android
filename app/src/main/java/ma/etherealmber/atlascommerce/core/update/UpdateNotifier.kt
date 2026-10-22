package ma.etherealmber.atlascommerce.core.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ma.etherealmber.atlascommerce.AtlasApp
import ma.etherealmber.atlascommerce.MainActivity
import ma.etherealmber.atlascommerce.R
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over system download notifications so [AppUpdateManager] keeps full
 * pure-JVM unit test coverage while driving real Android 8–15 notifications in
 * production.
 */
interface UpdateNotifier {
    fun showStarting(info: UpdateInfo)
    fun showProgress(info: UpdateInfo, bytesRead: Long, totalBytes: Long)
    fun showCompleted(info: UpdateInfo, apkFile: File)
    fun showError(info: UpdateInfo?, message: String)
    fun cancel()
}

@Singleton
class AndroidUpdateNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : UpdateNotifier {

    @Volatile
    private var lastNotifyRealtimeMs: Long = 0L

    override fun showStarting(info: UpdateInfo) {
        lastNotifyRealtimeMs = 0L
        postNotification(
            NotificationCompat.Builder(context, AtlasApp.UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentTitle("Mise à jour Atlas Commerce ${info.displayVersion}")
                .setContentText("Préparation du téléchargement…")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, true)
                .setContentIntent(openAppPendingIntent()),
        )
    }

    override fun showProgress(info: UpdateInfo, bytesRead: Long, totalBytes: Long) {
        val now = SystemClock.elapsedRealtime()
        val isComplete = totalBytes > 0L && bytesRead >= totalBytes
        if (!isComplete && now - lastNotifyRealtimeMs < MIN_NOTIFY_INTERVAL_MS) {
            return
        }
        lastNotifyRealtimeMs = now

        val indeterminate = totalBytes <= 0L
        val percent = if (!indeterminate) {
            ((bytesRead.toDouble() / totalBytes.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val summary = UpdateFormatter.progressSummary(bytesRead, totalBytes)

        postNotification(
            NotificationCompat.Builder(context, AtlasApp.UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentTitle("Téléchargement ${info.displayVersion}")
                .setContentText(summary)
                .setSubText(if (!indeterminate) "$percent %" else null)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, percent, indeterminate)
                .setContentIntent(openAppPendingIntent()),
        )
    }

    override fun showCompleted(info: UpdateInfo, apkFile: File) {
        lastNotifyRealtimeMs = 0L
        val sizeLabel = UpdateFormatter.dataSize(apkFile.length())
        postNotification(
            NotificationCompat.Builder(context, AtlasApp.UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentTitle("Mise à jour ${info.displayVersion} téléchargée")
                .setContentText("Paquet prêt ($sizeLabel) — appuyez pour installer.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setOngoing(false)
                .setAutoCancel(true)
                .setProgress(0, 0, false)
                .setContentIntent(openAppPendingIntent()),
        )
    }

    override fun showError(info: UpdateInfo?, message: String) {
        lastNotifyRealtimeMs = 0L
        val suffix = info?.displayVersion?.let { " $it" }.orEmpty()
        postNotification(
            NotificationCompat.Builder(context, AtlasApp.UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentTitle("Échec du téléchargement$suffix")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setOngoing(false)
                .setAutoCancel(true)
                .setProgress(0, 0, false)
                .setContentIntent(openAppPendingIntent()),
        )
    }

    override fun cancel() {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }

    private fun postNotification(builder: NotificationCompat.Builder) {
        if (!canPostNotifications()) return
        runCatching {
            ensureChannel()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(AtlasApp.UPDATE_CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    AtlasApp.UPDATE_CHANNEL_ID,
                    "Mises à jour de l'application",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Progression du téléchargement et installation des mises à jour Atlas Commerce"
                    setShowBadge(false)
                },
            )
        }
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val NOTIFICATION_ID = 4105
        private const val REQUEST_CODE_OPEN_APP = 4106
        // Android 14 drops notifications if an app posts faster than ~5 updates/sec.
        private const val MIN_NOTIFY_INTERVAL_MS = 250L
    }
}

/**
 * Delivers native Android OS system notifications strictly scoped to the signed-in
 * user's role (Admin product-creation requests, targeted role messages, request approvals,
 * and critical role-filtered stock/sales alerts).
 */
@Singleton
class RoleNotificationDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val deliveredKeys = LinkedHashSet<String>()
    private var activeUserKey: String = ""

    fun onSessionChanged(username: String, role: String) {
        val key = "${username.lowercase()}:${role.lowercase()}"
        if (key != activeUserKey) {
            synchronized(deliveredKeys) {
                deliveredKeys.clear()
                activeUserKey = key
            }
        }
    }

    fun dispatchIfNew(
        dedupKey: String,
        title: String,
        body: String,
        roleBadge: String,
        urgent: Boolean = false,
    ) {
        if (!canPostNotifications()) return
        val fullKey = "$activeUserKey|$dedupKey"
        val isNew = synchronized(deliveredKeys) {
            if (deliveredKeys.size > 400) {
                val trimCount = 150
                val iter = deliveredKeys.iterator()
                repeat(trimCount) { if (iter.hasNext()) { iter.next(); iter.remove() } }
            }
            deliveredKeys.add(fullKey)
        }
        if (!isNew) return

        runCatching {
            ensureRoleChannel()
            val notifId = ROLE_NOTIFICATION_BASE_ID + (fullKey.hashCode() and 0x0FFF)
            val notification = NotificationCompat.Builder(context, AtlasApp.ALERTS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentTitle(title)
                .setContentText(body)
                .setSubText(roleBadge)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(
                    if (urgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT,
                )
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(openNotificationsPendingIntent(notifId))
                .build()
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun ensureRoleChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(AtlasApp.ALERTS_CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    AtlasApp.ALERTS_CHANNEL_ID,
                    "Notifications & alertes métier",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Demandes produits Admin, alertes de stock et notifications ciblées par rôle"
                    setShowBadge(true)
                },
            )
        }
    }

    private fun openNotificationsPendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AtlasApp.EXTRA_OPEN_NOTIFICATIONS, true)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val ROLE_NOTIFICATION_BASE_ID = 5200
    }
}
