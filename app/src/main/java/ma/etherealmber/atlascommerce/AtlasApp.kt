package ma.etherealmber.atlascommerce

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AtlasApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer.installCrashBreadcrumbHandler(this)
        ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer.log(
            tag = "AtlasApp",
            message = "Application started (${ma.etherealmber.atlascommerce.core.AppDetails.VERSION_NAME} build ${ma.etherealmber.atlascommerce.core.AppDetails.VERSION_CODE})",
        )
        ensureNotificationChannels()
    }

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            // Clean up legacy low-importance channel IDs so upgraded installs display
            // notifications visibly in both the status bar and main notification shade.
            nm.deleteNotificationChannel("atlas_updates")
            nm.deleteNotificationChannel("atlas_role_alerts")

            nm.createNotificationChannel(
                NotificationChannel(
                    UPDATE_CHANNEL_ID,
                    "Téléchargements & mises à jour",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Progression visible des téléchargements et installation des mises à jour Atlas Commerce"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                    enableVibration(false)
                },
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    ALERTS_CHANNEL_ID,
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

    companion object {
        const val UPDATE_CHANNEL_ID = "atlas_updates_v2"
        const val ALERTS_CHANNEL_ID = "atlas_role_alerts_v2"
        const val EXTRA_OPEN_NOTIFICATIONS = "atlas_open_notifications"
        const val EXTRA_OPEN_SETTINGS = "atlas_open_settings"
    }
}
