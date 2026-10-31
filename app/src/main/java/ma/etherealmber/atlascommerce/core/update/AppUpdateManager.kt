package ma.etherealmber.atlascommerce.core.update

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ma.etherealmber.atlascommerce.BuildConfig
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.data.model.AndroidUpdateDto
import ma.etherealmber.atlascommerce.data.model.GitHubReleaseDto
import ma.etherealmber.atlascommerce.di.ApplicationScope
import ma.etherealmber.atlascommerce.di.ProbeClient
import ma.etherealmber.atlascommerce.di.UpdateClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

internal enum class DownloadNetworkTransport { WIFI, CELLULAR, OTHER }

private fun detectDownloadNetworkTransport(context: Context): DownloadNetworkTransport = runCatching {
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
        ?: return@runCatching DownloadNetworkTransport.OTHER
    val network = connectivity.activeNetwork
        ?: return@runCatching DownloadNetworkTransport.OTHER
    val capabilities = connectivity.getNetworkCapabilities(network)
        ?: return@runCatching DownloadNetworkTransport.OTHER
    when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> DownloadNetworkTransport.WIFI
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> DownloadNetworkTransport.CELLULAR
        else -> DownloadNetworkTransport.OTHER
    }
}.getOrDefault(DownloadNetworkTransport.OTHER)

/**
 * Application-scoped manager for checking, pinging (`CDN 1` vs `GitHub`),
 * downloading across parallel HTTP `Range` instances (with automatic single-stream
 * fallback, mirror failover & SHA-256 verification), loading release changelogs,
 * and installing native APK updates.
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
    private val parallelDownloadProvider: suspend () -> Boolean = { true },
    private val followLatestReleaseProvider: suspend () -> Boolean = { true },
    private val cdn1LatestJsonUrl: String? = null,
    private val githubLatestJsonUrl: String? = null,
    private val releasesApiUrl: String? = null,
    private val networkTransportProvider: () -> DownloadNetworkTransport = { DownloadNetworkTransport.OTHER },
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
        parallelDownloadProvider = {
            try {
                preferences.parallelUpdateDownload.first()
            } catch (_: Throwable) {
                true
            }
        },
        followLatestReleaseProvider = {
            try {
                preferences.followLatestRelease.first()
            } catch (_: Throwable) {
                true
            }
        },
        cdn1LatestJsonUrl = UpdateDefaults.CDN1_LATEST_JSON_URL,
        githubLatestJsonUrl = UpdateDefaults.GITHUB_LATEST_JSON_URL,
        releasesApiUrl = UpdateDefaults.GITHUB_RELEASES_API_URL,
        networkTransportProvider = { detectDownloadNetworkTransport(context) },
    )

    private val lock = Any()
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _mirrorPing = MutableStateFlow(MirrorPingResult())
    val mirrorPing: StateFlow<MirrorPingResult> = _mirrorPing.asStateFlow()

    private val _changelog = MutableStateFlow<List<ReleaseChangelogEntry>>(defaultOfflineChangelog())
    val changelog: StateFlow<List<ReleaseChangelogEntry>> = _changelog.asStateFlow()

    private val _changelogLoading = MutableStateFlow(false)
    val changelogLoading: StateFlow<Boolean> = _changelogLoading.asStateFlow()

    @Volatile
    private var downloadJob: Job? = null

    @Volatile
    private var checkJob: Job? = null

    @Volatile
    private var pingJob: Job? = null

    @Volatile
    private var changelogJob: Job? = null

    @Volatile
    private var lastAutoCheckMs: Long = 0L

    @Volatile
    private var hideActiveDownloadInForeground: Boolean = false

    /** Settings already shows its own download state, so avoid a duplicate OS alert while foregrounded. */
    @Volatile
    private var settingsStartedDownload: Boolean = false

    @Volatile
    private var appInForeground: Boolean = true

    @Volatile
    private var deferredSettingsResultNotification: Boolean = false

    private val updateAdapter by lazy { moshi.adapter(AndroidUpdateDto::class.java) }
    private val releasesListAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, GitHubReleaseDto::class.java)
        moshi.adapter<List<GitHubReleaseDto>>(type)
    }

    init {
        // Cache files are app-private and a completed APK remains after Android's installer
        // returns. On the next process launch, remove packages already installed and any
        // interrupted .part transfer; retain only a newer, still-usable downloaded package.
        scope.launch(ioDispatcher) { cleanupObsoleteUpdateFiles() }
    }

    internal fun cleanupObsoleteUpdateFiles() = synchronized(lock) {
        if (isDownloadingLocked()) return@synchronized
        val directory = runCatching { updatesDirProvider() }.getOrNull() ?: return@synchronized
        val installedApkPattern = Regex("^atlas-commerce-v(\\d+)\\.apk$")
        directory.listFiles().orEmpty().forEach { file ->
            val installedVersion = installedApkPattern.matchEntire(file.name)?.groupValues?.getOrNull(1)?.toIntOrNull()
            val isInterruptedDownload = file.name.endsWith(".apk.part", ignoreCase = true)
            if (isInterruptedDownload || (installedVersion != null && installedVersion <= currentVersionCode)) {
                runCatching { file.delete() }
            }
        }
    }

    val isDownloading: Boolean
        get() = synchronized(lock) {
            _state.value is UpdateState.Downloading || downloadJob?.isActive == true
        }

    /** Clear the cached update package only when no transfer is in progress. */
    fun clearCachedUpdateFiles(): Boolean = synchronized(lock) {
        if (isDownloadingLocked()) return@synchronized false
        val deleted = runCatching { updatesDirProvider().deleteRecursively() }.getOrDefault(false)
        if (!deleted) return@synchronized false
        when (val current = _state.value) {
            is UpdateState.ReadyToInstall -> {
                _state.value = if (current.info.mandatory) {
                    UpdateState.Available(info = current.info, dismissedBanner = false)
                } else {
                    UpdateState.Idle
                }
            }
            is UpdateState.Available -> if (!current.info.mandatory) _state.value = UpdateState.Idle
            is UpdateState.Error -> if (current.info?.mandatory != true) _state.value = UpdateState.Idle
            else -> Unit
        }
        notifier.cancel()
        settingsStartedDownload = false
        appInForeground = true
        deferredSettingsResultNotification = false
        true
    }

    private fun defaultOfflineChangelog(): List<ReleaseChangelogEntry> = listOf(
        ReleaseChangelogEntry(
            tagName = "v$currentVersionName",
            title = "Atlas Commerce v$currentVersionName (build $currentVersionCode)",
            isLatest = true,
            notes = buildString {
                appendLine("• Téléchargement multi-connexions adapté à la source : CDN 1 et GitHub Releases.")
                appendLine("• Suivi direct de la dernière version publiée sur GitHub Releases & CDN 1.")
                appendLine("• Journal complet des versions intégré dans Paramètres et notes JSON allégées.")
                append("• Licence MIT © 2026 Etherealmber & navigation persistante entre sous-écrans.")
            },
            htmlUrl = "${BuildConfig.PROJECT_REPO.trimEnd('/')}/releases",
        ),
    )

    /**
     * Loads full release notes directly inside Settings (`Paramètres`) from GitHub Releases,
     * falling back gracefully to built-in version highlights when offline.
     */
    fun refreshChangelog() {
        val apiUrl = releasesApiUrl?.takeIf { it.isNotBlank() } ?: return
        if (changelogJob?.isActive == true) return
        _changelogLoading.value = true
        changelogJob = scope.launch(ioDispatcher) {
            try {
                val request = Request.Builder()
                    .url(apiUrl)
                    .get()
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Atlas-Android-Updater")
                    .build()
                val parsed = probeClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) emptyList()
                    else {
                        val raw = resp.body?.string().orEmpty()
                        releasesListAdapter.fromJson(raw).orEmpty()
                    }
                }
                val entries = parsed
                    .filter { !it.draft && it.tagName.isNotBlank() }
                    .mapIndexed { idx, rel ->
                        val cleanedBody = formatReleaseNotesForDisplay(rel.body.orEmpty(), rel.tagName)
                        ReleaseChangelogEntry(
                            tagName = rel.tagName.trim(),
                            title = rel.name?.trim()?.takeIf { it.isNotEmpty() }
                                ?: "Atlas Commerce ${rel.tagName.trim()}",
                            publishedAt = rel.publishedAt?.take(10),
                            isLatest = idx == 0,
                            notes = cleanedBody,
                            htmlUrl = rel.htmlUrl,
                        )
                    }
                if (entries.isNotEmpty()) {
                    _changelog.value = entries
                }
            } catch (c: CancellationException) {
                throw c
            } catch (_: Throwable) {
                // Keep existing / built-in changelog entries silently when offline
            } finally {
                _changelogLoading.value = false
            }
        }
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
        val startNs = System.nanoTime()
        val headReq = Request.Builder()
            .url(url)
            .head()
            .header("User-Agent", "Atlas-Android-Updater")
            .build()
        return try {
            probeClient.newCall(headReq).execute().use { resp ->
                val elapsed = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
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
     * Checks `GET /api/app/android-update`, `CDN 1/latest.json`, and
     * `GitHub releases/latest/download/latest.json` without overriding any active download.
     */
    fun checkForUpdate(manual: Boolean = false, mandatoryOnly: Boolean = false) {
        synchronized(lock) {
            if (_state.value is UpdateState.Downloading || downloadJob?.isActive == true) {
                return
            }
            if (!manual && !mandatoryOnly && _state.value is UpdateState.ReadyToInstall) {
                return
            }
            if (checkJob?.isActive == true) {
                return
            }
            if (manual) {
                _state.value = UpdateState.Checking
            }
            checkJob = scope.launch(ioDispatcher) {
                performCheck(manual, mandatoryOnly)
            }
        }
    }

    private fun fetchManifestJson(url: String?): AndroidUpdateDto? {
        val clean = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            val req = Request.Builder()
                .url(clean)
                .get()
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "Atlas-Android-Updater")
                .build()
            probeClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null
                else resp.body?.string()?.let { updateAdapter.fromJson(it) }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun performCheck(manual: Boolean, mandatoryOnly: Boolean) {
        val baseUrl = ServerUrl.normalize(baseUrlProvider()) ?: return
        if (ServerUrl.isCleartext(baseUrl) && !allowCleartext) {
            synchronized(lock) {
                if (!isDownloadingLocked() && manual) {
                    _state.value = UpdateState.Error("Le serveur configuré n'utilise pas HTTPS.")
                }
            }
            return
        }

        val followLatest = runCatching { followLatestReleaseProvider() }.getOrDefault(true)

        val checkUrl = (ServerUrl.withSlash(baseUrl) + "api/app/android-update")
            .toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("versionCode", currentVersionCode.toString())
            ?.addQueryParameter("versionName", currentVersionName)
            ?.build()
            ?: return

        var primaryError: String? = null

        val (primaryDto, cdnManifestDto, ghManifestDto) = coroutineScope {
            val primaryDeferred = async {
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
                            runCatching { updateAdapter.fromJson(rawBody) }.getOrNull()
                        } else {
                            primaryError = "Impossible de vérifier les mises à jour (${response.code})."
                            null
                        }
                    }
                } catch (c: CancellationException) {
                    throw c
                } catch (_: IOException) {
                    primaryError = "Impossible de joindre le serveur pour vérifier les mises à jour."
                    null
                } catch (t: Throwable) {
                    primaryError = t.message?.takeIf { it.isNotBlank() } ?: "Erreur lors de la vérification."
                    null
                }
            }
            val cdnDeferred = async { fetchManifestJson(cdn1LatestJsonUrl) }
            val ghDeferred = async { fetchManifestJson(githubLatestJsonUrl) }
            Triple(primaryDeferred.await(), cdnDeferred.await(), ghDeferred.await())
        }

        val candidates = listOfNotNull(primaryDto, cdnManifestDto, ghManifestDto)
            .filter { it.success || it.ok || it.versionCode > 0 }

        val dto = if (followLatest) {
            // Treat whichever manifest was pushed most recently as the active latest release
            candidates.maxWithOrNull(
                compareBy<AndroidUpdateDto> { it.publishedAt?.trim().orEmpty() }
                    .thenBy { it.versionCode },
            )
        } else {
            candidates.maxByOrNull { it.versionCode }
        }

        if (dto == null) {
            synchronized(lock) {
                if (!isDownloadingLocked() && manual) {
                    _state.value = UpdateState.Error(
                        primaryDto?.error ?: primaryError ?: "Réponse de mise à jour invalide.",
                    )
                }
            }
            return
        }

        val resolvedPrimaryUrl = resolveApkUrl(baseUrl, dto.resolvedPrimaryUrl)
        val resolvedCdn1 = dto.resolvedCdn1Url?.let { resolveApkUrl(baseUrl, it) }
            ?: resolvedPrimaryUrl?.takeIf { it.contains("cdn1", ignoreCase = true) }
        val resolvedGithub = dto.resolvedGithubUrl?.let { resolveApkUrl(baseUrl, it) }
            ?: if (cdn1LatestJsonUrl != null || githubLatestJsonUrl != null) {
                UpdateDefaults.GITHUB_LATEST_APK_URL
            } else {
                null
            }

        val primaryUrl = (resolvedPrimaryUrl ?: resolvedCdn1 ?: resolvedGithub)
            ?.takeIf { it.isNotBlank() }

        val remoteCleanName = dto.versionName?.trim()?.removePrefix("v")?.removePrefix("V").orEmpty()
        val localCleanName = currentVersionName.trim().removePrefix("v").removePrefix("V")
        val differsFromInstalled = dto.versionCode > 0 && (
            dto.versionCode != currentVersionCode ||
                (remoteCleanName.isNotEmpty() && !remoteCleanName.equals(localCleanName, ignoreCase = true))
            )
        val policyMinimumVersionCode = maxOf(
            dto.resolvedMinimumSupportedVersionCode,
            primaryDto?.resolvedMinimumSupportedVersionCode ?: 0,
        )
        val backendRequiresUpdate = minimumVersionRequiresUpdate(
            installedVersionCode = currentVersionCode,
            minimumSupportedVersionCode = policyMinimumVersionCode,
            availableVersionCode = dto.versionCode,
        )
        val manifestRequiresUpdate = dto.mandatory || primaryDto?.mandatory == true
        if (policyMinimumVersionCode > 0) {
            AtlasLogBuffer.log(
                "UPDATE",
                "Minimum-version policy: current=$currentVersionCode minimum=$policyMinimumVersionCode release=${dto.versionCode} forced=$backendRequiresUpdate",
                if (policyMinimumVersionCode > dto.versionCode) "WARN" else "INFO",
            )
        }
        if (policyMinimumVersionCode > dto.versionCode && dto.versionCode > 0) {
            AtlasLogBuffer.log("UPDATE", "Ignored invalid minimum-version policy above available release", "WARN")
        }

        val shouldOfferUpdate = primaryUrl != null && if (mandatoryOnly) {
            (backendRequiresUpdate || manifestRequiresUpdate) && dto.versionCode > currentVersionCode
        } else if (followLatest) {
            dto.versionCode > currentVersionCode || differsFromInstalled
        } else {
            dto.versionCode > currentVersionCode
        }

        synchronized(lock) {
            if (isDownloadingLocked()) return

            if (!shouldOfferUpdate || primaryUrl == null) {
                _state.value = UpdateState.UpToDate(System.currentTimeMillis())
                return
            }

            val info = UpdateInfo(
                versionCode = dto.versionCode,
                versionName = dto.versionName?.takeIf { it.isNotBlank() } ?: dto.versionCode.toString(),
                apkUrl = primaryUrl,
                mandatory = dto.mandatory || primaryDto?.mandatory == true || backendRequiresUpdate,
                notes = dto.notes?.trim()?.takeIf { it.isNotBlank() },
                cdn1Url = resolvedCdn1,
                githubUrl = resolvedGithub,
                sha256 = dto.sha256?.trim()?.lowercase()?.takeIf { it.length == 64 },
                publishedAt = dto.publishedAt?.trim()?.takeIf { it.isNotBlank() },
            )

            val existingReady = _state.value as? UpdateState.ReadyToInstall
            if (existingReady != null &&
                existingReady.info.versionCode == info.versionCode &&
                (info.sha256 == null || existingReady.info.sha256 == info.sha256) &&
                existingReady.apkFile.exists() &&
                existingReady.apkFile.length() > 0L
            ) {
                _state.value = existingReady.copy(info = info)
                return
            }

            val targetFile = targetApkFile(info.versionCode)
            if (!manual && targetFile.exists() && targetFile.length() > MIN_VALID_APK_BYTES) {
                val cachedMatches = info.sha256 == null ||
                    runCatching { computeSha256Hex(targetFile) }.getOrNull().equals(info.sha256, ignoreCase = true)
                if (cachedMatches) {
                    _state.value = UpdateState.ReadyToInstall(info = info, apkFile = targetFile)
                    return
                }
            }

            _state.value = UpdateState.Available(info = info, dismissedBanner = false)
        }
    }

    /**
     * Starts downloading the available update APK if no download is currently running.
     */
    fun startDownload(overrideInfo: UpdateInfo? = null, fromSettings: Boolean = false) {
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
            settingsStartedDownload = fromSettings && !infoToDownload.mandatory
            appInForeground = true
            deferredSettingsResultNotification = false
            hideActiveDownloadInForeground = settingsStartedDownload
            _state.value = UpdateState.Downloading(
                info = infoToDownload,
                bytesRead = 0L,
                totalBytes = -1L,
                hiddenInForeground = hideActiveDownloadInForeground,
            )
            notifyStarting(infoToDownload)
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
        val useParallel = runCatching { parallelDownloadProvider() }.getOrDefault(true)
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
                downloadFromUrl(
                    info = info,
                    url = candidateUrl,
                    sourceLabel = sourceLabel,
                    partFile = partFile,
                    allowParallel = useParallel,
                )

                // Verify SHA-256 checksum when supplied by manifest
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
                notifyCompleted(info, finalFile)
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
        notifyError(info, message)
    }

    /**
     * Parallel HTTP `Range` downloader. Only the CDN/R2 mirror adapts to the active network:
     * 6 ranges on cellular, 16 on Wi-Fi, and 4 when the transport is unknown. GitHub remains at
     * 4 connections on every network because that mirror is more restrictive. Standard R2 has a monthly Class B read allowance, not a
     * per-download concurrency cap. If a server ignores Range, reuse its full response.
     */
    private suspend fun downloadFromUrl(
        info: UpdateInfo,
        url: String,
        sourceLabel: String?,
        partFile: File,
        allowParallel: Boolean,
    ) {
        if (!allowParallel) {
            streamSingleUrl(info, url, sourceLabel, partFile)
            return
        }

        val probeRequest = Request.Builder()
            .url(url)
            .get()
            .header("Range", "bytes=0-0")
            .header("Accept-Encoding", "identity")
            .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
            .build()

        val probeResponse = downloadClient.newCall(probeRequest).execute()
        val resolvedUrl = probeResponse.request.url.toString()

        if (probeResponse.code == 206) {
            val totalBytes = parseTotalBytesFromContentRange(probeResponse.header("Content-Range"))
            probeResponse.close()

            if (totalBytes != null && totalBytes >= MIN_PARALLEL_BYTES) {
                downloadParallelRanges(
                    info = info,
                    url = resolvedUrl,
                    sourceLabel = sourceLabel,
                    partFile = partFile,
                    totalBytes = totalBytes,
                    connections = connectionsForSource(sourceLabel),
                )
                return
            }

            streamSingleUrl(info, resolvedUrl, sourceLabel, partFile)
            return
        }

        // Server ignored `Range` and returned `200 OK` with full stream — reuse immediately!
        probeResponse.use { fullResponse ->
            if (!fullResponse.isSuccessful) {
                throw IOException("Le miroir ${sourceLabel ?: "de téléchargement"} a répondu ${fullResponse.code}.")
            }
            streamOpenResponse(info, sourceLabel, partFile, fullResponse)
        }
    }

    /** Keep GitHub fixed; adapt parallelism only for the first-party CDN/R2 mirror. */
    internal fun connectionsForSource(
        sourceLabel: String?,
        transport: DownloadNetworkTransport = networkTransportProvider(),
    ): Int = when {
        sourceLabel.equals("GitHub", ignoreCase = true) -> GITHUB_PARALLEL_CONNECTIONS
        sourceLabel.equals("CDN 1", ignoreCase = true) -> when (transport) {
            DownloadNetworkTransport.WIFI -> R2_CDN_WIFI_CONNECTIONS
            DownloadNetworkTransport.CELLULAR -> R2_CDN_CELLULAR_CONNECTIONS
            DownloadNetworkTransport.OTHER -> R2_CDN_OTHER_CONNECTIONS
        }
        else -> R2_CDN_OTHER_CONNECTIONS
    }

    internal fun parseTotalBytesFromContentRange(headerValue: String?): Long? {
        if (headerValue.isNullOrBlank()) return null
        val slashIdx = headerValue.lastIndexOf('/')
        if (slashIdx < 0 || slashIdx + 1 >= headerValue.length) return null
        return headerValue.substring(slashIdx + 1).trim().toLongOrNull()?.takeIf { it > 0L }
    }

    private suspend fun downloadParallelRanges(
        info: UpdateInfo,
        url: String,
        sourceLabel: String?,
        partFile: File,
        totalBytes: Long,
        connections: Int,
    ) = coroutineScope {
        val segmentCount = connections.coerceIn(2, MAX_PARALLEL_CONNECTIONS)
        val totalReadAtomic = AtomicLong(0L)
        val lastUiPct = AtomicInteger(-1)
        val lastNotifyPct = AtomicInteger(-1)
        val startNs = System.nanoTime()
        val lastUiEmitNs = AtomicLong(startNs)
        val lastNotifyNs = AtomicLong(startNs)

        _state.value = UpdateState.Downloading(
            info = info,
            bytesRead = 0L,
            totalBytes = totalBytes,
            activeSourceLabel = sourceLabel,
            hiddenInForeground = hideActiveDownloadInForeground,
            activeConnections = segmentCount,
            speedBytesPerSec = 0L,
        )
        notifyProgress(info, 0L, totalBytes)

        RandomAccessFile(partFile, "rw").use { raf ->
            raf.setLength(totalBytes)
            val channel = raf.channel
            val chunkSize = totalBytes / segmentCount

            val tasks = (0 until segmentCount).map { index ->
                val startByte = index * chunkSize
                val endByte = if (index == segmentCount - 1) totalBytes - 1L else (startByte + chunkSize - 1L)
                async(ioDispatcher) {
                    downloadSingleSegmentWithRetry(
                        channel = channel,
                        url = url,
                        sourceLabel = sourceLabel,
                        startByte = startByte,
                        endByte = endByte,
                    ) { delta ->
                        val accumulated = totalReadAtomic.addAndGet(delta)
                        val nowNs = System.nanoTime()
                        val elapsedNs = (nowNs - startNs).coerceAtLeast(1_000_000L)
                        val speedBps = (accumulated * 1_000_000_000L) / elapsedNs
                        val pct = ((accumulated.toDouble() / totalBytes.toDouble()) * 100.0)
                            .toInt()
                            .coerceIn(0, 100)
                        val done = accumulated >= totalBytes

                        val prevUiNs = lastUiEmitNs.get()
                        val prevPct = lastUiPct.get()
                        if (done || pct > prevPct || nowNs - prevUiNs >= UI_PROGRESS_MIN_INTERVAL_NS) {
                            lastUiPct.set(pct)
                            lastUiEmitNs.set(nowNs)
                            _state.value = UpdateState.Downloading(
                                info = info,
                                bytesRead = accumulated,
                                totalBytes = totalBytes,
                                activeSourceLabel = sourceLabel,
                                hiddenInForeground = hideActiveDownloadInForeground,
                                activeConnections = segmentCount,
                                speedBytesPerSec = speedBps,
                            )
                        }

                        val prevNotifyNs = lastNotifyNs.get()
                        val prevNotifyPct = lastNotifyPct.get()
                        if (done || (pct > prevNotifyPct && (prevNotifyPct < 0 || nowNs - prevNotifyNs >= NOTIFY_PROGRESS_MIN_INTERVAL_NS))) {
                            lastNotifyPct.set(pct)
                            lastNotifyNs.set(nowNs)
                            notifyProgress(info, accumulated, totalBytes)
                        }
                    }
                }
            }
            tasks.awaitAll()
            channel.force(true)
        }

        val finalBytes = totalReadAtomic.get()
        if (finalBytes < totalBytes) {
            throw IOException(
                "Téléchargement parallèle incomplet (${UpdateFormatter.dataSize(finalBytes)} / ${UpdateFormatter.dataSize(totalBytes)}).",
            )
        }
    }

    private fun downloadSingleSegmentWithRetry(
        channel: FileChannel,
        url: String,
        sourceLabel: String?,
        startByte: Long,
        endByte: Long,
        onBytesWritten: (Long) -> Unit,
    ) {
        var cursor = startByte
        var attempt = 0
        val expectedTotal = endByte - startByte + 1L

        while (cursor <= endByte) {
            attempt++
            try {
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .header("Range", "bytes=$cursor-$endByte")
                    .header("Accept-Encoding", "identity")
                    .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                    .build()

                downloadClient.newCall(req).execute().use { resp ->
                    if (resp.code != 206) {
                        throw IOException(
                            "Le serveur ${sourceLabel ?: ""} n'a pas honoré la plage HTTP (${resp.code}).".trim(),
                        )
                    }
                    val body = resp.body ?: throw IOException("Segment vide reçu.")
                    val buffer = ByteArray(STREAM_BUFFER_BYTES)
                    body.byteStream().use { input ->
                        while (cursor <= endByte) {
                            val maxToRead = minOf(buffer.size.toLong(), endByte - cursor + 1L).toInt()
                            val count = input.read(buffer, 0, maxToRead)
                            if (count == -1) break
                            var writtenInSlice = 0
                            while (writtenInSlice < count) {
                                val bb = ByteBuffer.wrap(buffer, writtenInSlice, count - writtenInSlice)
                                writtenInSlice += channel.write(bb, cursor + writtenInSlice)
                            }
                            cursor += count
                            onBytesWritten(count.toLong())
                        }
                    }
                }
                if (cursor <= endByte && attempt > MAX_SEGMENT_RETRIES) {
                    throw IOException("Segment interrompu ($cursor / $expectedTotal octets).")
                }
            } catch (t: Throwable) {
                if (attempt > MAX_SEGMENT_RETRIES) throw t
            }
        }
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
            streamOpenResponse(info, sourceLabel, partFile, response)
        }
    }

    private fun streamOpenResponse(
        info: UpdateInfo,
        sourceLabel: String?,
        partFile: File,
        response: Response,
    ) {
        val body = response.body ?: throw IOException("Aucun fichier reçu du serveur.")
        val totalBytes = body.contentLength()
        val startNs = System.nanoTime()

        _state.value = UpdateState.Downloading(
            info = info,
            bytesRead = 0L,
            totalBytes = totalBytes,
            activeSourceLabel = sourceLabel,
            hiddenInForeground = hideActiveDownloadInForeground,
            activeConnections = 1,
            speedBytesPerSec = 0L,
        )
        notifyProgress(info, 0L, totalBytes)

        var totalRead = 0L
        var lastUiPct = -1
        var lastNotifyPct = -1
        var lastUiEmitNs = startNs
        var lastNotifyNs = startNs
        body.byteStream().use { input ->
            FileOutputStream(partFile).use { output ->
                val buffer = ByteArray(STREAM_BUFFER_BYTES)
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    output.write(buffer, 0, count)
                    totalRead += count

                    val nowNs = System.nanoTime()
                    val elapsedNs = (nowNs - startNs).coerceAtLeast(1_000_000L)
                    val speedBps = (totalRead * 1_000_000_000L) / elapsedNs
                    val currentPct = if (totalBytes > 0L) {
                        ((totalRead.toDouble() / totalBytes.toDouble()) * 100.0).toInt().coerceIn(0, 100)
                    } else {
                        -1
                    }
                    val isCompleteChunk = totalBytes > 0L && totalRead >= totalBytes

                    if (isCompleteChunk || currentPct != lastUiPct || nowNs - lastUiEmitNs >= UI_PROGRESS_MIN_INTERVAL_NS) {
                        lastUiPct = currentPct
                        lastUiEmitNs = nowNs
                        _state.value = UpdateState.Downloading(
                            info = info,
                            bytesRead = totalRead,
                            totalBytes = totalBytes,
                            activeSourceLabel = sourceLabel,
                            hiddenInForeground = hideActiveDownloadInForeground,
                            activeConnections = 1,
                            speedBytesPerSec = speedBps,
                        )
                    }
                    if (isCompleteChunk ||
                        (currentPct != lastNotifyPct && (lastNotifyPct < 0 || nowNs - lastNotifyNs >= NOTIFY_PROGRESS_MIN_INTERVAL_NS)) ||
                        (totalBytes <= 0L && nowNs - lastNotifyNs >= NOTIFY_PROGRESS_MIN_INTERVAL_NS)
                    ) {
                        lastNotifyPct = currentPct
                        lastNotifyNs = nowNs
                        notifyProgress(info, totalRead, totalBytes)
                    }
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
     * Hides an optional update or install-ready banner for this session. Never
     * cancels an active download or dismisses a mandatory update.
     */
    fun dismissBanner() {
        synchronized(lock) {
            when (val current = _state.value) {
                is UpdateState.Available -> {
                    if (!current.info.mandatory) {
                        _state.value = current.copy(dismissedBanner = true)
                    }
                }
                is UpdateState.ReadyToInstall -> {
                    if (!current.info.mandatory) {
                        _state.value = current.copy(dismissedBanner = true)
                    }
                }
                else -> Unit
            }
        }
    }

    /**
     * Hides the in-app foreground download banner into the Top Bar mini progress ball
     * while keeping the background APK stream, OS notification, and Settings card active.
     */
    fun hideDownloadBanner() {
        synchronized(lock) {
            val current = _state.value
            if (current is UpdateState.Downloading && !current.info.mandatory) {
                hideActiveDownloadInForeground = true
                _state.value = current.copy(hiddenInForeground = true)
                notifyProgress(current.info, current.bytesRead, current.totalBytes)
            }
        }
    }

    /** Reopens the update banner/installer from its persistent Top Bar icon. */
    fun showDownloadBanner() {
        synchronized(lock) {
            when (val current = _state.value) {
                is UpdateState.Downloading -> if (!current.info.mandatory) {
                    hideActiveDownloadInForeground = false
                    _state.value = current.copy(hiddenInForeground = false)
                }
                is UpdateState.Available -> if (!current.info.mandatory && current.dismissedBanner) {
                    _state.value = current.copy(dismissedBanner = false)
                }
                is UpdateState.ReadyToInstall -> if (!current.info.mandatory && current.dismissedBanner) {
                    _state.value = current.copy(dismissedBanner = false)
                }
                else -> Unit
            }
        }
    }

    fun toggleDownloadBanner() {
        synchronized(lock) {
            val current = _state.value
            if (current is UpdateState.Downloading && !current.info.mandatory) {
                val nextHidden = !current.hiddenInForeground
                hideActiveDownloadInForeground = nextHidden
                _state.value = current.copy(hiddenInForeground = nextHidden)
            }
        }
    }

    /**
     * Immediately pushes the active update notification as soon as Android
     * notification permission is granted so the user sees it right away.
     */
    fun repostActiveNotification() {
        if (settingsStartedDownload && appInForeground) return
        when (val current = _state.value) {
            is UpdateState.Downloading -> {
                if (current.bytesRead <= 0L && current.totalBytes <= 0L) {
                    notifier.showStarting(current.info)
                } else {
                    notifyProgress(current.info, current.bytesRead, current.totalBytes)
                }
            }
            is UpdateState.ReadyToInstall -> notifier.showCompleted(current.info, current.apkFile)
            is UpdateState.Error -> notifier.showError(current.info, current.message)
            else -> Unit
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

    fun onAppResumed(context: Context, autoCheck: Boolean = true) {
        appInForeground = true
        if (settingsStartedDownload) {
            notifier.cancel()
            deferredSettingsResultNotification = false
        }
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
            // Mandatory compatibility checks always run; the preference controls only
            // optional update discovery, never the server's minimum-version policy.
            checkForUpdate(manual = false, mandatoryOnly = !autoCheck)
        }
    }

    fun onAppStopped() {
        lastAutoCheckMs = 0L
        appInForeground = false
        if (settingsStartedDownload) {
            val current = _state.value
            if (current is UpdateState.Downloading || deferredSettingsResultNotification) {
                deferredSettingsResultNotification = false
                repostActiveNotification()
            }
        }
    }

    private fun shouldSuppressSettingsNotification(): Boolean =
        settingsStartedDownload && appInForeground

    private fun notifyStarting(info: UpdateInfo) {
        if (!shouldSuppressSettingsNotification()) notifier.showStarting(info)
    }

    private fun notifyProgress(info: UpdateInfo, bytesRead: Long, totalBytes: Long) {
        if (!shouldSuppressSettingsNotification()) notifier.showProgress(info, bytesRead, totalBytes)
    }

    private fun notifyCompleted(info: UpdateInfo, apkFile: File) {
        if (shouldSuppressSettingsNotification()) {
            deferredSettingsResultNotification = true
        } else {
            notifier.showCompleted(info, apkFile)
        }
    }

    private fun notifyError(info: UpdateInfo?, message: String) {
        if (shouldSuppressSettingsNotification()) {
            deferredSettingsResultNotification = true
        } else {
            notifier.showError(info, message)
        }
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
        const val R2_CDN_OTHER_CONNECTIONS = 4
        const val R2_CDN_CELLULAR_CONNECTIONS = 6
        const val R2_CDN_WIFI_CONNECTIONS = 16
        const val GITHUB_PARALLEL_CONNECTIONS = 4
        const val MAX_PARALLEL_CONNECTIONS = 16
        const val MAX_SEGMENT_RETRIES = 2
        const val MIN_PARALLEL_BYTES = 16 * 1024L
        const val STREAM_BUFFER_BYTES = 32 * 1024
        const val MIN_VALID_APK_BYTES = 1_024L
        const val RESUME_RECHECK_DEBOUNCE_MS = 5_000L
        const val UI_PROGRESS_MIN_INTERVAL_NS = 80_000_000L
        const val NOTIFY_PROGRESS_MIN_INTERVAL_NS = 200_000_000L
    }
}
