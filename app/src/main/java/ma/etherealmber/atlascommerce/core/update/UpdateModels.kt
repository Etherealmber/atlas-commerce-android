package ma.etherealmber.atlascommerce.core.update

import java.io.File
import java.text.NumberFormat
import java.util.Locale

enum class UpdateDownloadSource(val key: String, val label: String) {
    AUTO("auto", "Auto"),
    CDN1("cdn1", "CDN 1"),
    GITHUB("github", "GitHub"),
    ;

    companion object {
        fun fromKey(raw: String?): UpdateDownloadSource =
            entries.firstOrNull { it.key.equals(raw?.trim(), ignoreCase = true) } ?: AUTO
    }
}

object UpdateDefaults {
    const val CDN1_LATEST_JSON_URL = "https://cdn1.afmarbre.com/app-updates/android/latest.json"
    const val CDN1_LATEST_APK_URL = "https://cdn1.afmarbre.com/app-updates/android/latest.apk"
    const val GITHUB_LATEST_APK_URL =
        "https://github.com/Etherealmber/atlas-commerce-android/releases/latest/download/atlas-commerce.apk"
}

data class MirrorPingResult(
    val testing: Boolean = false,
    val cdn1Ms: Long? = null,
    val cdn1Online: Boolean? = null,
    val githubMs: Long? = null,
    val githubOnline: Boolean? = null,
) {
    val fastestLabel: String?
        get() = when {
            cdn1Online == true && githubOnline == true && cdn1Ms != null && githubMs != null ->
                if (cdn1Ms <= githubMs) "CDN 1 recommandé (${cdn1Ms} ms)" else "GitHub recommandé (${githubMs} ms)"
            cdn1Online == true && cdn1Ms != null -> "CDN 1 actif (${cdn1Ms} ms)"
            githubOnline == true && githubMs != null -> "GitHub actif (${githubMs} ms)"
            else -> null
        }
}

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val mandatory: Boolean,
    val notes: String? = null,
    val cdn1Url: String? = null,
    val githubUrl: String? = null,
    val sha256: String? = null,
) {
    val displayVersion: String
        get() {
            val cleaned = versionName.trim().removePrefix("v").removePrefix("V")
            return if (cleaned.isNotEmpty()) "v$cleaned" else "build $versionCode"
        }

    /**
     * Always provides meaningful, human-readable release notes even if a manifest
     * omitted notes or only supplied a bare version number like `"1.0.8"`.
     */
    val effectiveNotes: String
        get() {
            val raw = notes?.trim().orEmpty()
            val bareVersionRegex = Regex("^[vV]?\\d+\\.\\d+(?:\\.\\d+)?(?:\\s*\\(build\\s*\\d+\\))?$", RegexOption.IGNORE_CASE)
            if (raw.isEmpty() || bareVersionRegex.matches(raw)) {
                return buildString {
                    appendLine("• Mise à niveau vers Atlas Commerce $displayVersion (build $versionCode).")
                    appendLine("• Améliorations de fluidité, stabilité et sécurité des échanges.")
                    append("• Téléchargement redondant haute vitesse via CDN 1 et GitHub.")
                }
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
                val ghFaster = ping.githubOnline == true &&
                    ping.githubMs != null &&
                    (ping.cdn1Online != true || ping.cdn1Ms == null || ping.githubMs < ping.cdn1Ms)
                if (ghFaster) {
                    listOfNotNull(gh, cdn, apkUrl)
                } else {
                    listOfNotNull(cdn, apkUrl, gh)
                }
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
            get() = "Téléchargement en cours — veuillez patienter sans fermer l'application."

        val progressSummary: String
            get() = UpdateFormatter.progressSummary(bytesRead, totalBytes)
    }

    data class ReadyToInstall(
        val info: UpdateInfo,
        val apkFile: File,
        val awaitingInstallPermission: Boolean = false,
    ) : UpdateState

    data class Error(
        val message: String,
        val info: UpdateInfo? = null,
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

    fun progressSummary(bytesRead: Long, totalBytes: Long): String {
        if (bytesRead <= 0L && totalBytes <= 0L) {
            return "Connexion au serveur de mise à jour…"
        }
        if (totalBytes > 0L) {
            val pct = ((bytesRead.toDouble() / totalBytes.toDouble()) * 100.0)
                .toInt()
                .coerceIn(0, 100)
            return "$pct % · ${dataSize(bytesRead)} / ${dataSize(totalBytes)}"
        }
        return "${dataSize(bytesRead)} téléchargés"
    }
}
