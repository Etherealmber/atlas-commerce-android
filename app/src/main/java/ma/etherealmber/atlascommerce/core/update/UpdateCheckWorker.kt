package ma.etherealmber.atlascommerce.core.update

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ma.etherealmber.atlascommerce.AtlasApp
import ma.etherealmber.atlascommerce.BuildConfig
import ma.etherealmber.atlascommerce.MainActivity
import ma.etherealmber.atlascommerce.R
import ma.etherealmber.atlascommerce.data.model.AndroidUpdateDto
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Best-effort periodic public-release check; business/payroll data is never fetched here. */
class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val latest = MANIFEST_URLS.firstNotNullOfOrNull(::fetchManifest)
                ?: return@withContext Result.retry()
            if (latest.versionCode <= BuildConfig.VERSION_CODE) return@withContext Result.success()

            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) ||
                !NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
            ) return@withContext Result.success()

            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (latest.versionCode <= prefs.getInt(KEY_LAST_NOTIFIED_VERSION, 0)) return@withContext Result.success()

            val titleVersion = latest.versionName?.takeIf { it.isNotBlank() } ?: "build ${latest.versionCode}"
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AtlasApp.EXTRA_OPEN_SETTINGS, true)
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(applicationContext, AtlasApp.UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentTitle("Mise à jour Atlas Commerce disponible")
                .setContentText("Version $titleVersion — appuyez pour consulter les nouveautés.")
                .setStyle(NotificationCompat.BigTextStyle().bigText(latest.notes?.takeIf { it.isNotBlank() }
                    ?: "La version $titleVersion est disponible. Ouvrez Paramètres pour vérifier et télécharger."))
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
            prefs.edit().putInt(KEY_LAST_NOTIFIED_VERSION, latest.versionCode).apply()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun fetchManifest(url: String): AndroidUpdateDto? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .header("User-Agent", "Atlas-Android-Background-Updater")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            adapter.fromJson(body)?.takeIf { it.versionCode > 0 && (it.success || it.ok) }
        }
    }.getOrNull()

    companion object {
        const val UNIQUE_WORK_NAME = "atlas-periodic-update-check"
        const val NOTIFICATION_ID = 4104
        const val REQUEST_CODE = 4107
        const val PREFS = "atlas_background_update_check"
        const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version_code"

        val MANIFEST_URLS = listOf(
            "${BuildConfig.CDN1_BASE_URL.trimEnd('/')}/latest.json",
            "${BuildConfig.PROJECT_REPO.trimEnd('/')}/releases/latest/download/latest.json",
        )
        val client = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        val adapter = Moshi.Builder().build().adapter(AndroidUpdateDto::class.java)
    }
}

object UpdateCheckWorkScheduler {
    fun setEnabled(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            workManager.cancelUniqueWork(UpdateCheckWorker.UNIQUE_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
            .setInitialDelay(2, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UpdateCheckWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
