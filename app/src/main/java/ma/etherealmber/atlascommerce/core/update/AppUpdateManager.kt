package ma.etherealmber.atlascommerce.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.FileProvider
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ma.etherealmber.atlascommerce.BuildConfig
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
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
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-scoped manager for checking, pinging (`CDN 1` vs `GitHub`),
 * downloading (with automatic mirror failover & SHA-256 verification), and
 * installing native APK updates.
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
    private val sourceProvider: suspend () -> UpdateDownloadSource = { UpdateDownloadSource.AUTO },
    private val cdn1LatestJsonUrl: String? = null,
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        serverConfig: ServerConfig,
        preferences: AtlasPreferences,
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
        sourceProvider = {
            val raw = try {
                preferences.updateDownloadSource.first()
            } catch (_: Throwable) {
                null
            }
            UpdateDownloadSource.fromKey(raw)
        },
        cdn1LatestJsonUrl = UpdateDefaults.CDN1_LATEST_JSON_URL,
    )

    private val lock = Any()
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _mirrorPing = MutableStateFlow(MirrorPingResult())
    val mirrorPing: StateFlow<MirrorPingResult> = _mirrorPing.asStateFlow()

    @Volatile
    private var downloadJob: Job? = null

    @Volatile
    private var checkJob: Job? = null

    @Volatile
    private var pingJob: Job? = null

    @Volatile
    private var lastAutoCheckMs: Long = 0L

    private val updateAdapter by lazy { moshi.adapter(AndroidUpdateDto::class.java) }

    val isDownloading: Boolean
        get() = synchronized(lock) {
            _state.value is UpdateState.Downloading || downloadJob?.isActive == true
        }

    /**
     * Measures real HTTPS latency in `ms` to both `CDN 1` and `GitHub Releases` in parallel.
     */
    fun pingMirrors() {
        if (pingJob?.isActive == true) return
        val activeInfo = when (val st = _state.value) {
            is UpdateState.Available -> st.info
            is UpdateState.Downloading -> st.info
            is UpdateState.ReadyToInstall -> st.info
            is UpdateState.Error -> st.info
            else -> null
        }
        val cdnTarget = activeInfo?.cdn1Url?.takeIf { it.isNotBlank() }
            ?: UpdateDefaults.CDN1_LATEST_APK_URL
        val ghTarget = activeInfo?.githubUrl?.takeIf { it.isNotBlank() }
            ?: UpdateDefaults.GITHUB_LATEST_APK_URL

        _mirrorPing.value = _mirrorPing.value.copy(testing = true)
        pingJob = scope.launch(ioDispatcher) {
            val report = coroutineScope {
                val cdnDeferred = async { measureEndpointMs(cdnTarget) }
                val ghDeferred = async { measureEndpointMs(ghTarget) }
                val (cdnOk, cdnMs) = cdnDeferred.await()
                val (ghOk, ghMs) = ghDeferred.await()
                MirrorPingResult(
                    testing = false,
                    cdn1Ms = cdnMs,
                    cdn1Online = cdnOk,
                    githubMs = ghMs,
                    githubOnline = ghOk,
                )
            }
            _mirrorPing.value = report
        }
    }

    internal fun measureEndpointMs(url: String): Pair<Boolean, Long?> {
        val start = SystemClock.elapsedRealtime()
        val headReq = Request.Builder()
            .url(url)
            .head()
            .header("User-Agent", "Atlas-Android-Updater")
            .build()
        return try {
            probeClient.newCall(headReq).execute().use { resp ->
                val elapsed = (SystemClock.elapsedRealtime() - start).coerceAtLeast(1L)
                if (resp.code in 200..399 || resp.code == 405) {
                    true to elapsed
                } else {
                    false to elapsed
                }
            }
        } catch (_: Throwable) {
            false to null
        }
    }

    /**
     * Checks `GET /api/app/android-update` (and optional `CDN 1` `latest.json` mirror)
     * without overriding any active download.
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

        var primaryDto: AndroidUpdateDto? = null
        var primaryError: String? = null

        try {
            val request = Request.Builder()
                .url(checkUrl)
                .get()
                .header("Accept", "application/json")
                .header("X-Atlas-Client", "android")
                .build()
            probeClient.newCall(request).execute().use { response ->
                val rawBody = runCatching { response.body?.string() }.getOrNull().orEmpty()
                if (response.isSuccessful) {
                    primaryDto = runCatching { updateAdapter.fromJson(rawBody) }.getOrNull()
                } else {
                    primaryError = "Impossible de vérifier les mises à jour (${response.code})."
                }
            }
        } catch (c: CancellationException) {
            throw c
        } catch (_: IOException) {
            primaryError = "Impossible de joindre le serveur pour vérifier les mises à jour."
        } catch (t: Throwable) {
            primaryError = t.message?.takeIf { it.isNotBlank() } ?: "Erreur lors de la vérification."
        }

        // Also check CDN 1 `latest.json` if configured and pick whichever manifest advertises the newest build
        var cdnManifestDto: AndroidUpdateDto? = null
        val cdnManifestUrl = cdn1LatestJsonUrl?.takeIf { it.isNotBlank() }
        if (cdnManifestUrl != null) {
            cdnManifestDto = try {
                val req = Request.Builder()
                    .url(cdnManifestUrl)
                    .get()
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache")
                    .build()
                probeClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) null
                    else resp.body?.string()?.let { updateAdapter.fromJson(it) }
                }
            } catch (_: Throwable) {
                null
            }
        }

        val dto = listOfNotNull(primaryDto, cdnManifestDto)
            .maxByOrNull { it.versionCode }

        if (dto == null || (!dto.success && !dto.ok && dto.versionCode <= 0)) {
            synchronized(lock) {
                if (!isDownloadingLocked() && manual) {
                    _state.value = UpdateState.Error(
                        dto?.error ?: primaryError ?: "Réponse de mise à jour invalide.",
                    )
                }
            }
            return
        }

        val resolvedPrimaryUrl = resolveApkUrl(baseUrl, dto.resolvedPrimaryUrl)
        val resolvedCdn1 = dto.resolvedCdn1Url?.let { resolveApkUrl(baseUrl, it) }
            ?: resolvedPrimaryUrl?.takeIf { it.contains("cdn1", ignoreCase = true) }
        val resolvedGithub = dto.resolvedGithubUrl?.let { resolveApkUrl(baseUrl, it) }
            ?: if (cdn1LatestJsonUrl != null) UpdateDefaults.GITHUB_LATEST_APK_URL else null

        val effectivePrimary = resolvedPrimaryUrl ?: resolvedCdn1 ?: resolvedGithub
        val isNewer = dto.versionCode > currentVersionCode && !effectivePrimary.isNullOrBlank() &&
            (dto.updateAvailable || dto.versionCode > currentVersionCode)

        synchronized(lock) {
            if (isDownloadingLocked()) return

            if (!isNewer) {
                _state.value = UpdateState.UpToDate(System.currentTimeMillis())
                return
            }

            val info = UpdateInfo(
                versionCode = dto.versionCode,
                versionName = dto.versionName?.takeIf { it.isNotBlank() } ?: dto.versionCode.toString(),
                apkUrl = effectivePrimary!!,
                mandatory = dto.mandatory,
                notes = dto.notes?.trim()?.takeIf { it.isNotBlank() },
                cdn1Url = resolvedCdn1,
                githubUrl = resolvedGithub,
                sha256 = dto.sha256?.trim()?.lowercase()?.takeIf { it.length == 64 },
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

    /**
     * Starts downloading the available update APK if no download is currently running.
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

        val preferred = runCatching { sourceProvider() }.getOrDefault(UpdateDownloadSource.AUTO)
        val candidateUrls = info.orderedDownloadUrls(preferred, _mirrorPing.value)
            .ifEmpty { listOf(info.apkUrl) }

        var lastError: Throwable? = null

        for (candidateUrl in candidateUrls) {
            runCatching { partFile.delete() }
            val sourceLabel = when {
                candidateUrl.contains("github.com", ignoreCase = true) ||
                    candidateUrl.contains("githubusercontent.com", ignoreCase = true) -> "GitHub"
                candidateUrl.contains("cdn1", ignoreCase = true) -> "CDN 1"
                else -> null
            }

            try {
                streamSingleUrl(
                    info = info,
                    url = candidateUrl,
                    sourceLabel = sourceLabel,
                    partFile = partFile,
                )

                // Optional SHA-256 verification when manifest supplies checksum
                val expectedSha = info.sha256
                if (!expectedSha.isNullOrBlank()) {
                    val actualSha = computeSha256Hex(partFile)
                    if (!actualSha.equals(expectedSha, ignoreCase = true)) {
                        throw IOException("Empreinte SHA-256 invalide depuis ${sourceLabel ?: "le serveur"}.")
                    }
                }

                withContext(ioDispatcher) {
                    Files.move(
                        partFile.toPath(),
                        finalFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                }

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
                return
            } catch (c: CancellationException) {
                runCatching { partFile.delete() }
                throw c
            } catch (t: Throwable) {
                runCatching { partFile.delete() }
                lastError = t
            }
        }

        val message = when (val t = lastError) {
            is IOException -> t.message?.takeIf { it.isNotBlank() }
                ?: "Connexion interrompue pendant le téléchargement. Réessayez."
            else -> t?.message?.takeIf { it.isNotBlank() }
                ?: "Échec du téléchargement de la mise à jour."
        }
        synchronized(lock) {
            _state.value = UpdateState.Error(message = message, info = info)
        }
        notifier.showError(info, message)
    }

    private fun streamSingleUrl(
        info: UpdateInfo,
        url: String,
        sourceLabel: String?,
        partFile: File,
    ) {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
            .build()

        downloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Le miroir ${sourceLabel ?: "de téléchargement"} a répondu ${response.code}.")
            }
            val body = response.body ?: throw IOException("Aucun fichier reçu du serveur.")
            val totalBytes = body.contentLength()

            _state.value = UpdateState.Downloading(
                info = info,
                bytesRead = 0L,
                totalBytes = totalBytes,
                activeSourceLabel = sourceLabel,
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
                            activeSourceLabel = sourceLabel,
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
                throw IOException(
                    "Téléchargement interrompu avant la fin (${UpdateFormatter.dataSize(totalRead)} / ${UpdateFormatter.dataSize(totalBytes)}).",
                )
            }
        }
    }

    private fun computeSha256Hex(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(STREAM_BUFFER_BYTES)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
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

    fun onAppResumed(context: Context) {
        val current = _state.value
        if (current is UpdateState.ReadyToInstall &&
            current.awaitingInstallPermission &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls())
        ) {
            installApk(context)
            return
        }
        val now = System.currentTimeMillis()
        if (!isDownloading && now - lastAutoCheckMs >= RESUME_RECHECK_DEBOUNCE_MS) {
            lastAutoCheckMs = now
            checkForUpdate(manual = false)
        }
    }

    fun onAppStopped() {
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
