package ma.etherealmber.atlascommerce

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AtlasApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannels()
    }

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(
                    UPDATE_CHANNEL_ID,
                    "Mises à jour de l'application",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Progression du téléchargement et installation des mises à jour Atlas Commerce"
                    setShowBadge(false)
                },
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    ALERTS_CHANNEL_ID,
                    "Notifications & alertes métier",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Demandes produits Admin, alertes de stock et notifications ciblées par rôle"
                    setShowBadge(true)
                },
            )
        }
    }

    companion object {
        const val UPDATE_CHANNEL_ID = "atlas_updates"
        const val ALERTS_CHANNEL_ID = "atlas_role_alerts"
        const val EXTRA_OPEN_NOTIFICATIONS = "atlas_open_notifications"
    }
}
