package ma.etherealmber.atlascommerce

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import ma.etherealmber.atlascommerce.push.AtlasFirebaseMessagingService

@HiltAndroidApp
class AtlasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                AtlasFirebaseMessagingService.CHANNEL_ID,
                "Atlas Agent",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Approbations et alertes assistant IA"
            },
        )
    }
}
