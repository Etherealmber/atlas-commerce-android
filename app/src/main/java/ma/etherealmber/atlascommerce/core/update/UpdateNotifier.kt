package ma.etherealmber.atlascommerce.core.update

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.localizedUpdateSize
import ma.etherealmber.atlascommerce.ui.i18n.localizedUpdateSummary
import ma.etherealmber.atlascommerce.ui.i18n.notificationText
import ma.etherealmber.atlascommerce.AtlasApp
import ma.etherealmber.atlascommerce.MainActivity
import ma.etherealmber.atlascommerce.R
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over system download notifications so [AppUpdateManager] keeps full
 * pure-JVM unit test coverage while driving visible Android 8–15 notifications in
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
    preferences: AtlasPreferences,
) : UpdateNotifier {

    // Updates never block the main thread while DataStore is loading.
    private val language = preferences.appLanguage.stateIn(
        CoroutineScope(SupervisorJob() + Dispatchers.IO), SharingStarted.Eagerly, AppLanguage.FRENCH,
    )

    @Volatile
    private var lastNotifyRealtimeMs: Long = 0L

    override fun showStarting(info: UpdateInfo) {
        lastNotifyRealtimeMs = 0L
        val lang = language.value
        postNotification(
            NotificationCompat.Builder(context, AtlasApp.UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentTitle(notificationText(lang, "Mise à jour Atlas Commerce ${info.displayVersion}", "Atlas Commerce update ${info.displayVersion}", "تحديث Atlas Commerce ${info.displayVersion}"))
                .setContentText(notificationText(lang, "Téléchargement démarré — suivez l’avancement ici ou dans Paramètres.", "Download started — follow progress here or in Settings.", "بدأ التنزيل — تابع التقدم هنا أو في الإعدادات."))
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        notificationText(lang, "Téléchargement démarré en arrière-plan. Appuyez pour voir la progression dans Paramètres.", "Background download started. Tap to view progress in Settings.", "بدأ التنزيل في الخلفية. اضغط لعرض التقدم في الإعدادات."),
                    ),
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, true)
                .setContentIntent(openSettingsPendingIntent()),
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
        val lang = language.value
        val summary = localizedUpdateSummary(UpdateState.Downloading(info, bytesRead, totalBytes), lang)

        postNotification(
            NotificationCompat.Builder(context, AtlasApp.UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentTitle(notificationText(lang, "Téléchargement ${info.displayVersion}", "Downloading ${info.displayVersion}", "تنزيل ${info.displayVersion}"))
                .setContentText(notificationText(lang, "$summary · Ouvrir Paramètres", "$summary · Open Settings", "$summary · فتح الإعدادات"))
                .setSubText(if (!indeterminate) "$percent %" else null)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, percent, indeterminate)
                .setContentIntent(openSettingsPendingIntent()),
        )
    }

    override fun showCompleted(info: UpdateInfo, apkFile: File) {
        lastNotifyRealtimeMs = 0L
        val lang = language.value
        val sizeLabel = localizedUpdateSize(apkFile.length(), lang)
        postNotification(
            NotificationCompat.Builder(context, AtlasApp.UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentTitle(notificationText(lang, "Mise à jour ${info.displayVersion} téléchargée", "Update ${info.displayVersion} downloaded", "تم تنزيل التحديث ${info.displayVersion}"))
                .setContentText(notificationText(lang, "Paquet prêt ($sizeLabel) — appuyez pour installer.", "Package ready ($sizeLabel) — tap to install.", "الحزمة جاهزة ($sizeLabel) — اضغط للتثبيت."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setOngoing(false)
                .setOnlyAlertOnce(false)
                .setAutoCancel(true)
                .setProgress(0, 0, false)
                .setContentIntent(openSettingsPendingIntent()),
        )
    }

    override fun showError(info: UpdateInfo?, message: String) {
        lastNotifyRealtimeMs = 0L
        val lang = language.value
        val suffix = info?.displayVersion?.let { " $it" }.orEmpty()
        postNotification(
            NotificationCompat.Builder(context, AtlasApp.UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentTitle(notificationText(lang, "Échec du téléchargement$suffix", "Download failed$suffix", "فشل التنزيل$suffix"))
                .setContentText(atlasNotice(message, lang))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setOngoing(false)
                .setOnlyAlertOnce(false)
                .setAutoCancel(true)
                .setProgress(0, 0, false)
                .setContentIntent(openSettingsPendingIntent()),
        )
    }

    override fun cancel() {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }

    // Permission is checked immediately before posting; SecurityException is also caught below
    // to cover a permission change between the check and the notification service call.
    @SuppressLint("MissingPermission")
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
                    "Téléchargements & mises à jour",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Progression visible des téléchargements et installation des mises à jour Atlas Commerce"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                    enableVibration(false)
                },
            )
        }
    }

    private fun openSettingsPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AtlasApp.EXTRA_OPEN_SETTINGS, true)
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

    // The runtime permission gate is checked before deduplication and the notification call is
    // guarded by runCatching to handle a permission revoked between check and post.
    @SuppressLint("MissingPermission")
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
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Demandes produits Admin, alertes de stock et notifications ciblées par rôle"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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
