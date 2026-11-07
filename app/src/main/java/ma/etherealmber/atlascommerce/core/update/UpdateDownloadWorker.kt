package ma.etherealmber.atlascommerce.core.update

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import ma.etherealmber.atlascommerce.AtlasApp
import ma.etherealmber.atlascommerce.R
import ma.etherealmber.atlascommerce.ui.i18n.notificationText
import java.util.concurrent.TimeUnit

/**
 * Durable, user-started APK download. WorkManager keeps the transfer alive when the
 * Activity is dismissed and can recreate it after the app process is reclaimed.
 */
class UpdateDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val info = inputData.toUpdateInfo() ?: return Result.failure()
        val manager = EntryPointAccessors.fromApplication(
            applicationContext,
            UpdateDownloadWorkerEntryPoint::class.java,
        ).appUpdateManager()

        setForeground(createForegroundInfo(info))
        val retryable = manager.runDownloadFromWorker(info)
        return if (retryable && runAttemptCount < MAX_NETWORK_RETRIES) Result.retry() else Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        createForegroundInfo(inputData.toUpdateInfo())

    private fun createForegroundInfo(info: UpdateInfo?): ForegroundInfo {
        val version = info?.displayVersion?.let { " $it" }.orEmpty()
        val language = (applicationContext as AtlasApp).activeLanguage.value
        val notification = NotificationCompat.Builder(applicationContext, AtlasApp.UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle(notificationText(language, "Téléchargement Atlas Commerce$version", "Downloading Atlas Commerce$version", "جارٍ تنزيل Atlas Commerce$version"))
            .setContentText(notificationText(language, "Le téléchargement continue en arrière-plan.", "The update download continues in the background.", "يستمر تنزيل التحديث في الخلفية."))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, true)
            .setContentIntent(UpdateNotificationActions.openSettingsPendingIntent(applicationContext))
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(AndroidUpdateNotifier.NOTIFICATION_ID, notification)
        }
    }

    private fun Data.toUpdateInfo(): UpdateInfo? {
        val versionCode = getInt(KEY_VERSION_CODE, 0)
        val versionName = getString(KEY_VERSION_NAME).orEmpty()
        val apkUrl = getString(KEY_APK_URL).orEmpty()
        if (versionCode <= 0 || versionName.isBlank() || apkUrl.isBlank()) return null
        return UpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            mandatory = getBoolean(KEY_MANDATORY, false),
            cdn1Url = getString(KEY_CDN_URL),
            githubUrl = getString(KEY_GITHUB_URL),
            sha256 = getString(KEY_SHA256),
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "atlas-update-apk-download"
        private const val FOREGROUND_NOTIFICATION_ID = 4108
        private const val MAX_NETWORK_RETRIES = 3
        private const val KEY_VERSION_CODE = "version_code"
        private const val KEY_VERSION_NAME = "version_name"
        private const val KEY_APK_URL = "apk_url"
        private const val KEY_MANDATORY = "mandatory"
        private const val KEY_CDN_URL = "cdn_url"
        private const val KEY_GITHUB_URL = "github_url"
        private const val KEY_SHA256 = "sha256"

        fun enqueue(context: Context, info: UpdateInfo) {
            val request = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
                .setInputData(
                    workDataOf(
                        KEY_VERSION_CODE to info.versionCode,
                        KEY_VERSION_NAME to info.versionName,
                        KEY_APK_URL to info.apkUrl,
                        KEY_MANDATORY to info.mandatory,
                        KEY_CDN_URL to info.cdn1Url,
                        KEY_GITHUB_URL to info.githubUrl,
                        KEY_SHA256 to info.sha256,
                    ),
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UpdateDownloadWorkerEntryPoint {
    fun appUpdateManager(): AppUpdateManager
}
