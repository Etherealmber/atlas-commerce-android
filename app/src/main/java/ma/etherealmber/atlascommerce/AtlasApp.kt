package ma.etherealmber.atlascommerce

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.ui.i18n.notificationText

@HiltAndroidApp
class AtlasApp : Application() {

    private val startupIoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer.installCrashBreadcrumbHandler(this)
        ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer.log(
            tag = "AtlasApp",
            message = "Application started (${ma.etherealmber.atlascommerce.core.AppDetails.VERSION_NAME} build ${ma.etherealmber.atlascommerce.core.AppDetails.VERSION_CODE})",
        )
        // These involve disk/Firebase initialization. Keep them off the launch thread so a slow
        // emulator or a large cached-report directory cannot hold Android on the splash screen.
        startupIoScope.launch {
            ma.etherealmber.atlascommerce.core.diagnostics.CrashReportConsent.applyStoredConsent(this@AtlasApp)
            runCatching {
                ma.etherealmber.atlascommerce.data.FinanceReportCache.pruneExpired(java.io.File(cacheDir, "finance"))
            }.onFailure { error ->
                ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer.log(
                    tag = "AtlasApp",
                    message = "Deferred finance cache cleanup failed: ${error.javaClass.simpleName}",
                    level = "WARN",
                )
            }
        }
        ensureNotificationChannels(AppLanguage.FRENCH, cleanupLegacy = true)
        startupIoScope.launch {
            AtlasPreferences(this@AtlasApp).appLanguage.collect { language ->
                ensureNotificationChannels(language, cleanupLegacy = false)
            }
        }
    }

    private fun ensureNotificationChannels(language: AppLanguage, cleanupLegacy: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            // Clean up legacy low-importance channel IDs so upgraded installs display
            // notifications visibly in both the status bar and main notification shade.
            if (cleanupLegacy) {
                nm.deleteNotificationChannel("atlas_updates")
                nm.deleteNotificationChannel("atlas_role_alerts")
            }

            nm.createNotificationChannel(
                NotificationChannel(
                    UPDATE_CHANNEL_ID,
                    notificationText(language, "Téléchargements & mises à jour", "Downloads & updates", "التنزيلات والتحديثات"),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = notificationText(language, "Progression visible des téléchargements et installation des mises à jour Atlas Commerce", "Download progress and Atlas Commerce update installation", "تقدم التنزيل وتثبيت تحديثات Atlas Commerce")
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                    enableVibration(false)
                },
            )
            // v1.2 FCM bridge — the worker targets channel_id "atlas_assist" on
            // every HTTP v1 push; without this channel Android drops them.
            nm.createNotificationChannel(
                NotificationChannel(
                    ASSIST_CHANNEL_ID,
                    notificationText(language, "Assistant & notifications serveur", "Assistant & server notifications", "المساعد وإشعارات الخادم"),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = notificationText(language, "Notifications push envoyées par le serveur Atlas (agent, paie, stock)", "Push notifications sent by the Atlas server (assistant, payroll, stock)", "إشعارات يرسلها خادم Atlas (المساعد والرواتب والمخزون)")
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                },
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    ALERTS_CHANNEL_ID,
                    notificationText(language, "Notifications & alertes métier", "Business notifications & alerts", "إشعارات وتنبيهات العمل"),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = notificationText(language, "Demandes produits Admin, alertes de stock et notifications ciblées par rôle", "Administrator product requests, stock alerts, and role-based notifications", "طلبات المنتجات للمسؤول وتنبيهات المخزون والإشعارات حسب الدور")
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                },
            )
        }
    }

    companion object {
        const val UPDATE_CHANNEL_ID = "atlas_updates_v2"
        const val ASSIST_CHANNEL_ID = "atlas_assist"
        const val ALERTS_CHANNEL_ID = "atlas_role_alerts_v2"
        const val EXTRA_OPEN_NOTIFICATIONS = "atlas_open_notifications"
        const val EXTRA_OPEN_SETTINGS = "atlas_open_settings"
        const val ACTION_ASSIST_NOTIFICATION = "ATLAS_ASSIST_APPROVAL"
    }
}
