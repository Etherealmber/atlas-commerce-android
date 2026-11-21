package ma.etherealmber.atlascommerce.core.update

import com.squareup.moshi.JsonClass
import ma.etherealmber.atlascommerce.BuildConfig
import java.io.File
import java.text.NumberFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class UpdateDownloadSource(val key: String, val label: String) {
    AUTO("auto", "Auto"),
    CDN1("cdn1", "Atlas API"),
    GITHUB("github", "GitHub"),
    ;

    companion object {
        fun fromKey(raw: String?): UpdateDownloadSource =
            entries.firstOrNull { it.key.equals(raw?.trim(), ignoreCase = true) } ?: AUTO
    }
}

object UpdateDefaults {
    val CDN1_LATEST_JSON_URL: String =
        "${BuildConfig.CDN1_BASE_URL.trimEnd('/')}/latest.json"
    val CDN1_LATEST_APK_URL: String =
        "${BuildConfig.CDN1_BASE_URL.trimEnd('/')}/latest.apk"
    val GITHUB_LATEST_JSON_URL: String =
        "${BuildConfig.PROJECT_REPO.trimEnd('/')}/releases/latest/download/latest.json"
    val GITHUB_LATEST_APK_URL: String =
        BuildConfig.DEFAULT_DOWNLOAD_URL
    val GITHUB_RELEASES_API_URL: String =
        "${BuildConfig.PROJECT_REPO.trimEnd('/').replace("https://github.com/", "https://api.github.com/repos/")}/releases?per_page=8"
}

enum class MirrorTestTarget { CDN1, GITHUB }

data class MirrorPingResult(
    val testing: Boolean = false,
    val currentTest: MirrorTestTarget? = null,
    val cdn1Ms: Long? = null,
    val cdn1Online: Boolean? = null,
    val githubMs: Long? = null,
    val githubOnline: Boolean? = null,
) {
    val fastestLabel: String?
        get() = when {
            cdn1Online == true && githubOnline == true && cdn1Ms != null && githubMs != null ->
                if (cdn1Ms <= githubMs) "Atlas API recommandé (${cdn1Ms} ms)" else "GitHub recommandé (${githubMs} ms)"
            cdn1Online == true && cdn1Ms != null -> "Atlas API actif (${cdn1Ms} ms)"
            githubOnline == true && githubMs != null -> "GitHub actif (${githubMs} ms)"
            else -> null
        }
}

/** Tests mirrors in order and publishes the first result before starting the second request. */
internal suspend fun pingMirrorsSequentially(
    cdnTarget: String,
    githubTarget: String,
    measure: suspend (String) -> Pair<Boolean, Long?>,
    publish: (MirrorPingResult) -> Unit,
) {
    publish(MirrorPingResult(testing = true, currentTest = MirrorTestTarget.CDN1))
    val (cdnOnline, cdnMs) = measure(cdnTarget)
    val cdnResult = MirrorPingResult(
        testing = true,
        currentTest = MirrorTestTarget.GITHUB,
        cdn1Ms = cdnMs,
        cdn1Online = cdnOnline,
    )
    publish(cdnResult)

    val (githubOnline, githubMs) = measure(githubTarget)
    publish(
        cdnResult.copy(
            testing = false,
            currentTest = null,
            githubMs = githubMs,
            githubOnline = githubOnline,
        ),
    )
}

@JsonClass(generateAdapter = true)
data class ReleaseChangelogEntry(
    val tagName: String,
    val title: String,
    val publishedAt: String? = null,
    val isLatest: Boolean = false,
    val isPrerelease: Boolean = false,
    val notes: String,
    val htmlUrl: String? = null,
    val authorLogin: String? = null,
    val authorUrl: String? = null,
    val targetCommitish: String? = null,
)

data class GitHubLimitNotice(
    val retryAtEpochSeconds: Long,
    val operation: GitHubLimitedOperation,
) {
    fun remainingSeconds(nowEpochSeconds: Long = System.currentTimeMillis() / 1_000L): Int =
        (retryAtEpochSeconds - nowEpochSeconds)
            .coerceAtLeast(0L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
}

enum class GitHubLimitedOperation { RELEASE_HISTORY, UPDATE_CHECK, SOURCE_TEST }

/** Uses only response headers/body from the request that was already made; never probes /rate_limit. */
internal fun githubRateLimitRetryAfterSeconds(
    statusCode: Int,
    retryAfterHeader: String?,
    resetEpochHeader: String?,
    remainingHeader: String?,
    responseBody: String?,
    nowEpochSeconds: Long,
): Int? {
    val looksRateLimited = statusCode == 429 || (
        statusCode == 403 && (
            remainingHeader?.trim() == "0" ||
                responseBody.orEmpty().contains("rate limit", ignoreCase = true) ||
                responseBody.orEmpty().contains("secondary limit", ignoreCase = true)
            )
        )
    if (!looksRateLimited) return null
    val retryAfter = retryAfterHeader?.trim()?.let { value ->
        value.toLongOrNull()?.takeIf { it > 0L }
            ?: runCatching {
                (ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond() - nowEpochSeconds)
                    .takeIf { it > 0L }
            }.getOrNull()
    }
    val resetDelay = resetEpochHeader?.trim()?.toLongOrNull()?.minus(nowEpochSeconds)?.takeIf { it > 0L }
    return (retryAfter ?: resetDelay ?: 60L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(1)
}

internal fun formatReleaseNotesForDisplay(rawBody: String, tagName: String): String =
    rawBody.trim().ifBlank { "• Mise à jour $tagName — optimisations et correctifs." }

internal fun monotonicElapsedRealtimeMs(): Long = System.nanoTime() / 1_000_000L

internal fun updateCheckCooldownRemainingMs(
    lastCheckElapsedMs: Long,
    nowElapsedMs: Long,
    minimumIntervalMs: Long,
): Long {
    if (lastCheckElapsedMs <= 0L || nowElapsedMs < lastCheckElapsedMs || minimumIntervalMs <= 0L) return 0L
    return (minimumIntervalMs - (nowElapsedMs - lastCheckElapsedMs)).coerceAtLeast(0L)
}

internal object ManualApiRefreshRateLimiter {
    private data class RefreshWindow(
        val nextAllowedAtMs: Long,
        val cooldownMs: Long,
    )

    private val refreshByKey = LinkedHashMap<String, RefreshWindow>()

    @Synchronized
    fun acquire(key: String, nowElapsedMs: Long, minimumIntervalMs: Long): Long? {
        if (key.isBlank() || minimumIntervalMs <= 0L) return null
        val previous = refreshByKey[key]
        if (previous != null && nowElapsedMs < previous.nextAllowedAtMs) {
            val increasedCooldown = (previous.cooldownMs * 2L).coerceAtMost(MAX_ADAPTIVE_COOLDOWN_MS)
            refreshByKey[key] = RefreshWindow(
                nextAllowedAtMs = nowElapsedMs + increasedCooldown,
                cooldownMs = increasedCooldown,
            )
            return increasedCooldown.coerceAtMost(Int.MAX_VALUE.toLong())
        }
        val baseCooldown = minimumIntervalMs.coerceAtMost(MAX_ADAPTIVE_COOLDOWN_MS)
        refreshByKey[key] = RefreshWindow(nowElapsedMs + baseCooldown, baseCooldown)
        while (refreshByKey.size > 64) refreshByKey.remove(refreshByKey.keys.first())
        return null
    }

    @Synchronized
    fun resetForTests() = refreshByKey.clear()

    private const val MAX_ADAPTIVE_COOLDOWN_MS = 30_000L
}

internal object UpdateCheckRateLimiter {
    private var lastCheckElapsedMs = 0L

    @Synchronized
    fun resetForTests() {
        lastCheckElapsedMs = 0L
    }

    @Synchronized
    fun acquire(nowElapsedMs: Long): Long? {
        val remaining = updateCheckCooldownRemainingMs(
            lastCheckElapsedMs = lastCheckElapsedMs,
            nowElapsedMs = nowElapsedMs,
            minimumIntervalMs = MIN_INTERVAL_MS,
        )
        if (remaining > 0L) return remaining
        lastCheckElapsedMs = nowElapsedMs
        return null
    }

    private const val MIN_INTERVAL_MS = 60_000L
}

internal fun minimumVersionRequiresUpdate(
    installedVersionCode: Int,
    minimumSupportedVersionCode: Int,
    availableVersionCode: Int,
): Boolean = minimumSupportedVersionCode > 0 &&
    installedVersionCode < minimumSupportedVersionCode &&
    availableVersionCode > installedVersionCode &&
    availableVersionCode >= minimumSupportedVersionCode

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val mandatory: Boolean,
    val notes: String? = null,
    val cdn1Url: String? = null,
    val githubUrl: String? = null,
    val sha256: String? = null,
    val publishedAt: String? = null,
) {
    val displayVersion: String
        get() {
            val cleaned = versionName.trim().removePrefix("v").removePrefix("V")
            return if (cleaned.isNotEmpty()) "v$cleaned" else "build $versionCode"
        }

    /**
     * Minimal, concise summary shown in update banners while full release history
     * is available in the Paramètres → Nouveautés des versions dialog.
     */
    val effectiveNotes: String
        get() {
            val raw = notes?.trim().orEmpty()
            val bareVersionRegex = Regex(
                "^[vV]?\\d+\\.\\d+(?:\\.\\d+)?(?:\\s*\\(build\\s*\\d+\\))?$",
                RegexOption.IGNORE_CASE,
            )
            if (raw.isEmpty() || bareVersionRegex.matches(raw)) {
                return "Mise à jour Atlas Commerce $displayVersion — optimisations et correctifs."
            }
            return raw
        }

    /** Returns ordered download URLs honoring user preference and automatic mirror failover. */
    fun orderedDownloadUrls(
        preferred: UpdateDownloadSource,
        ping: MirrorPingResult = MirrorPingResult(),
    ): List<String> {
        val cdn = cdn1Url?.takeIf { it.isNotBlank() }
        val gh = githubUrl?.takeIf { it.isNotBlank() }
        val rawOrder = when (preferred) {
            UpdateDownloadSource.CDN1 -> listOfNotNull(cdn, apkUrl, gh)
            UpdateDownloadSource.GITHUB -> listOfNotNull(gh, cdn, apkUrl)
            UpdateDownloadSource.AUTO -> {
                val measuredAvailable = buildList {
                    if (cdn != null && ping.cdn1Online == true && ping.cdn1Ms != null) {
                        add(cdn to ping.cdn1Ms)
                    }
                    if (gh != null && ping.githubOnline == true && ping.githubMs != null) {
                        add(gh to ping.githubMs)
                    }
                }.sortedBy { it.second }.map { it.first }
                measuredAvailable + listOfNotNull(apkUrl, cdn, gh)
            }
        }
        return rawOrder.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }
}

sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    data class UpToDate(val checkedAtMillis: Long) : UpdateState

    data class Available(
        val info: UpdateInfo,
        val dismissedBanner: Boolean = false,
    ) : UpdateState

    data class Downloading(
        val info: UpdateInfo,
        val bytesRead: Long,
        val totalBytes: Long,
        val activeSourceLabel: String? = null,
        val hiddenInForeground: Boolean = false,
        val activeConnections: Int = 1,
        val speedBytesPerSec: Long = 0L,
    ) : UpdateState {
        val progressFraction: Float?
            get() = if (totalBytes > 0L) {
                (bytesRead.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
            } else {
                null
            }

        val percent: Int?
            get() = progressFraction?.let { (it * 100f).toInt().coerceIn(0, 100) }

        val statusTitle: String
            get() {
                val via = activeSourceLabel?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                return "Téléchargement de la mise à jour$via en cours…"
            }

        val statusMessage: String
            get() = if (activeConnections > 1) {
                "Téléchargement accéléré actif ($activeConnections connexions parallèles). Appuyez sur Masquer pour réduire en mini-bulle."
            } else {
                "Appuyez sur Masquer pour réduire la barre en mini-bulle à côté du profil, ou suivez dans Paramètres."
            }

        val progressSummary: String
            get() = UpdateFormatter.progressSummary(
                bytesRead = bytesRead,
                totalBytes = totalBytes,
                speedBytesPerSec = speedBytesPerSec,
                activeConnections = activeConnections,
            )
    }

    data class ReadyToInstall(
        val info: UpdateInfo,
        val apkFile: File,
        val awaitingInstallPermission: Boolean = false,
        val dismissedBanner: Boolean = false,
    ) : UpdateState

    data class Error(
        val message: String,
        val info: UpdateInfo? = null,
        /** True only for a transport failure during the APK download itself. */
        val recoverableNetwork: Boolean = false,
        /** Remaining manual-check cooldown in seconds, when the check was rate-limited. */
        val retryAfterSeconds: Int? = null,
    ) : UpdateState
}

object UpdateFormatter {

    fun dataSize(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        if (safe < 1_024L) return "$safe o"
        if (safe < 1_048_576L) {
            val kb = safe / 1_024.0
            val fmt = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
                minimumFractionDigits = 0
                maximumFractionDigits = 0
            }
            return "${fmt.format(kb)} Ko"
        }
        val mb = safe / 1_048_576L.toDouble()
        val fmt = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }
        return "${fmt.format(mb)} Mo"
    }

    fun speed(bytesPerSec: Long): String? {
        if (bytesPerSec <= 0L) return null
        return "${dataSize(bytesPerSec)}/s"
    }

    fun progressSummary(
        bytesRead: Long,
        totalBytes: Long,
        speedBytesPerSec: Long = 0L,
        activeConnections: Int = 1,
    ): String {
        if (bytesRead <= 0L && totalBytes <= 0L) {
            return "Connexion au serveur de mise à jour…"
        }
        val extras = buildList {
            speed(speedBytesPerSec)?.let { add(it) }
            if (activeConnections > 1) add("$activeConnections flux")
        }.joinToString(" · ")
        val suffix = if (extras.isNotEmpty()) " · $extras" else ""

        if (totalBytes > 0L) {
            val pct = ((bytesRead.toDouble() / totalBytes.toDouble()) * 100.0)
                .toInt()
                .coerceIn(0, 100)
            return "$pct % · ${dataSize(bytesRead)} / ${dataSize(totalBytes)}$suffix"
        }
        return "${dataSize(bytesRead)} téléchargés$suffix"
    }
}
