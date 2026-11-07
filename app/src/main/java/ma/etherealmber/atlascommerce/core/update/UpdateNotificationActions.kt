package ma.etherealmber.atlascommerce.core.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ma.etherealmber.atlascommerce.MainActivity

/** Intent contract shared by update notifications and the app entry activity. */
object UpdateNotificationActions {
    const val EXTRA_DOWNLOAD_UPDATE = "atlas.update.download"
    const val EXTRA_INSTALL_UPDATE = "atlas.update.install"

    private const val EXTRA_VERSION_CODE = "atlas.update.version_code"
    private const val EXTRA_VERSION_NAME = "atlas.update.version_name"
    private const val EXTRA_APK_URL = "atlas.update.apk_url"
    private const val EXTRA_MANDATORY = "atlas.update.mandatory"
    private const val EXTRA_CDN_URL = "atlas.update.cdn_url"
    private const val EXTRA_GITHUB_URL = "atlas.update.github_url"
    private const val EXTRA_SHA256 = "atlas.update.sha256"

    fun downloadPendingIntent(context: Context, info: UpdateInfo): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_DOWNLOAD_BASE + info.versionCode,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DOWNLOAD_UPDATE, true)
            putUpdateInfo(info)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun installPendingIntent(context: Context, info: UpdateInfo): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_INSTALL,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_INSTALL_UPDATE, true)
            putUpdateInfo(info)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun openSettingsPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN_SETTINGS,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ma.etherealmber.atlascommerce.AtlasApp.EXTRA_OPEN_SETTINGS, true)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun Intent.putUpdateInfo(info: UpdateInfo) {
        putExtra(EXTRA_VERSION_CODE, info.versionCode)
        putExtra(EXTRA_VERSION_NAME, info.versionName)
        putExtra(EXTRA_APK_URL, info.apkUrl)
        putExtra(EXTRA_MANDATORY, info.mandatory)
        putExtra(EXTRA_CDN_URL, info.cdn1Url)
        putExtra(EXTRA_GITHUB_URL, info.githubUrl)
        putExtra(EXTRA_SHA256, info.sha256)
    }

    fun downloadInfoFrom(intent: Intent?): UpdateInfo? {
        if (intent?.getBooleanExtra(EXTRA_DOWNLOAD_UPDATE, false) != true) return null
        return readUpdateInfo(intent)
    }

    fun installInfoFrom(intent: Intent?): UpdateInfo? {
        if (intent?.getBooleanExtra(EXTRA_INSTALL_UPDATE, false) != true) return null
        return readUpdateInfo(intent)
    }

    private fun readUpdateInfo(intent: Intent): UpdateInfo? {
        val versionCode = intent.getIntExtra(EXTRA_VERSION_CODE, 0)
        val versionName = intent.getStringExtra(EXTRA_VERSION_NAME).orEmpty()
        val apkUrl = intent.getStringExtra(EXTRA_APK_URL).orEmpty()
        if (versionCode <= 0 || versionName.isBlank() || apkUrl.isBlank()) return null
        return UpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            mandatory = intent.getBooleanExtra(EXTRA_MANDATORY, false),
            cdn1Url = intent.getStringExtra(EXTRA_CDN_URL),
            githubUrl = intent.getStringExtra(EXTRA_GITHUB_URL),
            sha256 = intent.getStringExtra(EXTRA_SHA256),
        )
    }

    fun installRequested(intent: Intent?): Boolean =
        intent?.getBooleanExtra(EXTRA_INSTALL_UPDATE, false) == true

    private const val REQUEST_DOWNLOAD_BASE = 42_100
    private const val REQUEST_INSTALL = 42_099
    private const val REQUEST_OPEN_SETTINGS = 42_098
}
