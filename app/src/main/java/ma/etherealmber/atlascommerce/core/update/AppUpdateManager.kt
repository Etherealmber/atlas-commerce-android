package ma.etherealmber.atlascommerce.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ma.etherealmber.atlascommerce.BuildConfig
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.data.model.AndroidUpdateDto
import ma.etherealmber.atlascommerce.di.ApplicationScope
import ma.etherealmber.atlascommerce.di.ProbeClient
import ma.etherealmber.atlascommerce.di.UpdateClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-scoped manager for checking, downloading and installing native APK
 * updates served by `GET /api/app/android-update`.
 *
 * State invariants:
 * - While a download is active ([UpdateState.Downloading]), background or manual
 *   update checks, banner dismissals, and duplicate button taps NEVER overwrite,
 *   cancel, or reset the active download state.
 * - Downloads stream to `.apk.part` first and verify byte completeness before
 *   exposing `.apk`, preventing corrupt half-downloaded APK installs.
 * - Live progress is published simultaneously to [state] (for in-app UI across
 *   every screen) and [UpdateNotifier] (for the Android system status bar).
 */
@Singleton
class AppUpdateManager internal constructor(
    private val updatesDirProvider: () -> File,
    private val baseUrlProvider: () -> String,
    private val currentVersionCode: Int,
    private val currentVersionName: String,
    private val probeClient: OkHttpClient,
    private val downloadClient: OkHttpClient,
    private val moshi: Moshi,
    private val notifier: UpdateNotifier,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val allowCleartext: Boolean = BuildConfig.ALLOW_CLEARTEXT,
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        serverConfig: ServerConfig,
        @ProbeClient probeClient: OkHttpClient,
        @UpdateClient downloadClient: OkHttpClient,
        moshi: Moshi,
        notifier: UpdateNotifier,
        @ApplicationScope scope: CoroutineScope,
    ) : this(
        updatesDirProvider = { File(context.cacheDir, "updates").apply { mkdirs() } },
        baseUrlProvider = { serverConfig.baseUrl() },
        currentVersionCode = BuildConfig.VERSION_CODE,
        currentVersionName = BuildConfig.VERSION_NAME,
        probeClient = probeClient,
        downloadClient = downloadClient,
        moshi = moshi,
        notifier = notifier,
        scope = scope,
    )

    private val lock = Any()
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    @Volatile
    private var downloadJob: Job? = null

    @Volatile
    private var checkJob: Job? = null

    @Volatile
    private var lastAutoCheckMs: Long = 0L

    private val updateAdapter by lazy { moshi.adapter(AndroidUpdateDto::class.java) }

    val isDownloading: Boolean
        get() = synchronized(lock) {
            _state.value is UpdateState.Downloading || downloadJob?.isActive == true
        }

    /**
     * Checks `GET /api/app/android-update` against the active server.
     *
     * Never overrides an in-progress download, and automatic background checks
     * never discard an APK that has already finished downloading.
     */
    fun checkForUpdate(manual: Boolean = false) {
        synchronized(lock) {
            if (_state.value is UpdateState.Downloading || downloadJob?.isActive == true) {
                return
            }
            if (!manual && _state.value is UpdateState.ReadyToInstall) {
                return
            }
            if (checkJob?.isActive == true) {
                return
            }
            if (manual) {
                _state.value = UpdateState.Checking
            }
            checkJob = scope.launch(ioDispatcher) {
                performCheck(manual)
            }
        }
    }

    private suspend fun performCheck(manual: Boolean) {
        val baseUrl = ServerUrl.normalize(baseUrlProvider()) ?: return
        if (ServerUrl.isCleartext(baseUrl) && !allowCleartext) {
            synchronized(lock) {
                if (!isDownloadingLocked() && manual) {
                    _state.value = UpdateState.Error("Le serveur configuré n'utilise pas HTTPS.")
                }
            }
            return
        }

        val checkUrl = (ServerUrl.withSlash(baseUrl) + "api/app/android-update")
            .toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("versionCode", currentVersionCode.toString())
            ?.addQueryParameter("versionName", currentVersionName)
            ?.build()
            ?: return

        val request = Request.Builder()
            .url(checkUrl)
            .get()
            .header("Accept", "application/json")
            .header("X-Atlas-Client", "android")
            .build()

        try {
            probeClient.newCall(request).execute().use { response ->
                val rawBody = runCatching { response.body?.string() }.getOrNull().orEmpty()
                if (!response.isSuccessful) {
                    synchronized(lock) {
                        if (!isDownloadingLocked() && manual) {
                            _state.value = UpdateState.Error(
                                "Impossible de vérifier les mises à jour (${response.code}).",
                            )
                        }
                    }
                    return
                }

                val dto = runCatching { updateAdapter.fromJson(rawBody) }.getOrNull()
                if (dto == null || (!dto.success && !dto.ok)) {
                    synchronized(lock) {
                        if (!isDownloadingLocked() && manual) {
                            _state.value = UpdateState.Error(
                                dto?.error ?: "Réponse de mise à jour invalide.",
                            )
                        }
                    }
                    return
                }

                val resolvedApkUrl = resolveApkUrl(baseUrl, dto.apkUrl)
                val hasNewerVersion = dto.updateAvailable &&
                    dto.versionCode > currentVersionCode &&
                    !resolvedApkUrl.isNullOrBlank()

                synchronized(lock) {
                    // Double-check under lock: if a download began while the HTTP check was
                    // in flight, NEVER overwrite the active download state.
                    if (isDownloadingLocked()) return

                    if (!hasNewerVersion) {
                        _state.value = UpdateState.UpToDate(System.currentTimeMillis())
                        return
                    }

                    val info = UpdateInfo(
                        versionCode = dto.versionCode,
                        versionName = dto.versionName?.takeIf { it.isNotBlank() } ?: dto.versionCode.toString(),
                        apkUrl = resolvedApkUrl!!,
                        mandatory = dto.mandatory,
                        notes = dto.notes?.trim()?.takeIf { it.isNotBlank() },
                    )

                    val existingReady = _state.value as? UpdateState.ReadyToInstall
                    if (existingReady != null &&
                        existingReady.info.versionCode == info.versionCode &&
                        existingReady.apkFile.exists() &&
                        existingReady.apkFile.length() > 0L
                    ) {
                        _state.value = existingReady.copy(info = info)
                        return
                    }

                    val targetFile = targetApkFile(info.versionCode)
                    if (!manual && targetFile.exists() && targetFile.length() > MIN_VALID_APK_BYTES) {
                        _state.value = UpdateState.ReadyToInstall(info = info, apkFile = targetFile)
                        return
                    }

                    _state.value = UpdateState.Available(info = info, dismissedBanner = false)
                }
            }
        } catch (c: CancellationException) {
            throw c
        } catch (_: IOException) {
            synchronized(lock) {
                if (!isDownloadingLocked() && manual) {
                    _state.value = UpdateState.Error(
                        "Impossible de joindre le serveur pour vérifier les mises à jour.",
                    )
                }
            }
        } catch (t: Throwable) {
            synchronized(lock) {
                if (!isDownloadingLocked() && manual) {
                    _state.value = UpdateState.Error(
                        t.message?.takeIf { it.isNotBlank() } ?: "Erreur lors de la vérification.",
                    )
                }
            }
        }
    }

    /**
     * Starts downloading the available update APK if no download is currently
     * running. Transitions synchronously to [UpdateState.Downloading] so the UI
     * reflects the download immediately and duplicate taps cannot race.
     */
    fun startDownload(overrideInfo: UpdateInfo? = null) {
        val infoToDownload: UpdateInfo
        synchronized(lock) {
            if (isDownloadingLocked()) {
                return
            }
            val candidate = overrideInfo ?: when (val current = _state.value) {
                is UpdateState.Available -> current.info
                is UpdateState.Error -> current.info
                is UpdateState.ReadyToInstall -> current.info
                else -> null
            } ?: return

            infoToDownload = candidate
            _state.value = UpdateState.Downloading(
                info = infoToDownload,
                bytesRead = 0L,
                totalBytes = -1L,
            )
            notifier.showStarting(infoToDownload)
            downloadJob = scope.launch(ioDispatcher) {
                executeDownload(infoToDownload)
            }
        }
    }

    private suspend fun executeDownload(info: UpdateInfo) {
        val dir = updatesDirProvider().apply { mkdirs() }
        val partFile = File(dir, "atlas-commerce-v${info.versionCode}.apk.part")
        val finalFile = targetApkFile(info.versionCode)

        runCatching { partFile.delete() }

        val request = Request.Builder()
            .url(info.apkUrl)
            .get()
            .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
            .build()

        try {
            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Le serveur de téléchargement a répondu ${response.code}.")
                }
                val body = response.body ?: throw IOException("Aucun fichier reçu du serveur.")
                val totalBytes = body.contentLength()

                _state.value = UpdateState.Downloading(
                    info = info,
                    bytesRead = 0L,
                    totalBytes = totalBytes,
                )
                notifier.showProgress(info, 0L, totalBytes)

                var totalRead = 0L
                body.byteStream().use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buffer = ByteArray(STREAM_BUFFER_BYTES)
                        while (true) {
                            val count = input.read(buffer)
                            if (count == -1) break
                            output.write(buffer, 0, count)
                            totalRead += count
                            _state.value = UpdateState.Downloading(
                                info = info,
                                bytesRead = totalRead,
                                totalBytes = totalBytes,
                            )
                            notifier.showProgress(info, totalRead, totalBytes)
                        }
                        output.flush()
                    }
                }

                if (totalRead <= 0L) {
                    throw IOException("Le fichier téléchargé est vide.")
                }
                if (totalBytes > 0L && totalRead < totalBytes) {
                    throw IOException("Téléchargement interrompu avant la fin (${UpdateFormatter.dataSize(totalRead)} / ${UpdateFormatter.dataSize(totalBytes)}).")
                }

                withContext(ioDispatcher) {
                    Files.move(
                        partFile.toPath(),
                        finalFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                }

                // Clean up older cached APKs once the latest package is safely saved.
                runCatching {
                    dir.listFiles()?.forEach { sibling ->
                        if (sibling != finalFile) sibling.delete()
                    }
                }

                synchronized(lock) {
                    _state.value = UpdateState.ReadyToInstall(
                        info = info,
                        apkFile = finalFile,
                        awaitingInstallPermission = false,
                    )
                }
                notifier.showCompleted(info, finalFile)
            }
        } catch (c: CancellationException) {
            runCatching { partFile.delete() }
            throw c
        } catch (t: Throwable) {
            runCatching { partFile.delete() }
            val message = when (t) {
                is IOException -> t.message?.takeIf { it.isNotBlank() }
                    ?: "Connexion interrompue pendant le téléchargement. Réessayez."
                else -> t.message?.takeIf { it.isNotBlank() }
                    ?: "Échec du téléchargement de la mise à jour."
            }
            synchronized(lock) {
                _state.value = UpdateState.Error(message = message, info = info)
            }
            notifier.showError(info, message)
        }
    }

    /**
     * Hides the optional update banner for this session. Never dismisses an
     * active download or a mandatory update.
     */
    fun dismissBanner() {
        synchronized(lock) {
            val current = _state.value
            if (current is UpdateState.Available && !current.info.mandatory) {
                _state.value = current.copy(dismissedBanner = true)
            }
        }
    }

    /**
     * Launches the Android package installer for a downloaded APK, or redirects to
     * Android's "Install unknown apps" screen if the user has not granted it yet.
     */
    fun installApk(context: Context): Boolean {
        val current = _state.value as? UpdateState.ReadyToInstall ?: return false
        if (!current.apkFile.exists() || current.apkFile.length() <= 0L) {
            synchronized(lock) {
                _state.value = UpdateState.Error(
                    message = "Le fichier d'installation n'est plus en cache. Relancez le téléchargement.",
                    info = current.info,
                )
            }
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            synchronized(lock) {
                val latest = _state.value as? UpdateState.ReadyToInstall
                if (latest != null) {
                    _state.value = latest.copy(awaitingInstallPermission = true)
                }
            }
            return runCatching {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settingsIntent)
                true
            }.getOrDefault(false)
        }

        synchronized(lock) {
            val latest = _state.value as? UpdateState.ReadyToInstall
            if (latest != null && latest.awaitingInstallPermission) {
                _state.value = latest.copy(awaitingInstallPermission = false)
            }
        }

        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                current.apkFile,
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        }.getOrElse { error ->
            synchronized(lock) {
                _state.value = UpdateState.Error(
                    message = error.message ?: "Impossible d'ouvrir l'installateur Android.",
                    info = current.info,
                )
            }
            false
        }
    }

    /**
     * Called when the activity resumes: if the user just granted "Install unknown
     * apps" in Android 14 Settings, automatically proceed to install without
     * re-downloading.
     */
    fun onAppResumed(context: Context) {
        val current = _state.value
        if (current is UpdateState.ReadyToInstall &&
            current.awaitingInstallPermission &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls())
        ) {
            installApk(context)
            return
        }
        // Check for updates every time the user opens or returns to the app,
        // without overriding any active download or already-downloaded APK.
        val now = System.currentTimeMillis()
        if (!isDownloading && now - lastAutoCheckMs >= RESUME_RECHECK_DEBOUNCE_MS) {
            lastAutoCheckMs = now
            checkForUpdate(manual = false)
        }
    }

    fun onAppStopped() {
        // Ensure returning later re-runs a fresh server check and surfaces any available update.
        lastAutoCheckMs = 0L
    }

    private fun isDownloadingLocked(): Boolean =
        _state.value is UpdateState.Downloading || downloadJob?.isActive == true

    private fun targetApkFile(versionCode: Int): File =
        File(updatesDirProvider(), "atlas-commerce-v$versionCode.apk")

    private fun resolveApkUrl(baseUrl: String, rawApkUrl: String?): String? {
        val trimmed = rawApkUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true)
        ) {
            return trimmed
        }
        val base = ServerUrl.withSlash(baseUrl)
        return base + trimmed.trimStart('/')
    }

    private companion object {
        const val STREAM_BUFFER_BYTES = 16 * 1024
        const val MIN_VALID_APK_BYTES = 1_024L
        const val RESUME_RECHECK_DEBOUNCE_MS = 5_000L
    }
}
