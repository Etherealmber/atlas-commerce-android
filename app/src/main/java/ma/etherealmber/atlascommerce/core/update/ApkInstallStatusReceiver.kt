package ma.etherealmber.atlascommerce.core.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build

/** Handles the system's confirmation hand-off for a PackageInstaller session. */
class ApkInstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE) !=
            PackageInstaller.STATUS_PENDING_USER_ACTION
        ) {
            return
        }

        val confirmationIntent = getConfirmationIntent(intent) ?: return

        confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(confirmationIntent) }
    }

    @Suppress("DEPRECATION")
    private fun getConfirmationIntent(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }

    companion object {
        const val ACTION_INSTALL_STATUS = "ma.etherealmber.atlascommerce.UPDATE_INSTALL_STATUS"
    }
}
