package ma.etherealmber.atlascommerce.core.update

import java.io.File
import java.text.NumberFormat
import java.util.Locale

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val mandatory: Boolean,
    val notes: String? = null,
) {
    val displayVersion: String
        get() {
            val cleaned = versionName.trim().removePrefix("v").removePrefix("V")
            return if (cleaned.isNotEmpty()) "v$cleaned" else "build $versionCode"
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
            get() = "Téléchargement de la mise à jour ${info.displayVersion}…"

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
        val mb = safe / 1_048_576.0
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
