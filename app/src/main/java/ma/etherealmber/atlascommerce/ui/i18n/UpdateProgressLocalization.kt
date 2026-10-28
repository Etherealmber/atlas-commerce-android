package ma.etherealmber.atlascommerce.ui.i18n

import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.update.UpdateState
import java.text.NumberFormat
import java.util.Locale

/** Locale-aware display only; transfer calculations remain in UpdateState. */
fun localizedUpdateSize(bytes: Long, language: AppLanguage): String {
    val safe = bytes.coerceAtLeast(0L)
    val locale = when (language) {
        AppLanguage.FRENCH -> Locale.FRANCE
        AppLanguage.ENGLISH -> Locale.UK
        AppLanguage.ARABIC -> Locale.forLanguageTag("ar-MA")
    }
    val unit = when {
        safe < 1024 -> when (language) { AppLanguage.FRENCH -> "o"; AppLanguage.ENGLISH -> "B"; AppLanguage.ARABIC -> "بايت" }
        safe < 1048576 -> when (language) { AppLanguage.FRENCH -> "Ko"; AppLanguage.ENGLISH -> "KB"; AppLanguage.ARABIC -> "ك.ب" }
        else -> when (language) { AppLanguage.FRENCH -> "Mo"; AppLanguage.ENGLISH -> "MB"; AppLanguage.ARABIC -> "م.ب" }
    }
    val value = when { safe < 1024 -> safe.toDouble(); safe < 1048576 -> safe / 1024.0; else -> safe / 1048576.0 }
    val decimals = if (safe >= 1048576) 1 else 0
    val formatted = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }.format(value)
    return "$formatted $unit"
}

fun localizedUpdateSummary(state: UpdateState.Downloading, language: AppLanguage): String {
    if (state.bytesRead <= 0L && state.totalBytes <= 0L) return when (language) {
        AppLanguage.FRENCH -> "Connexion au serveur de mise à jour…"
        AppLanguage.ENGLISH -> "Connecting to the update server…"
        AppLanguage.ARABIC -> "جارٍ الاتصال بخادم التحديث…"
    }
    val extras = buildList {
        if (state.speedBytesPerSec > 0) add(when (language) {
            AppLanguage.FRENCH -> "${localizedUpdateSize(state.speedBytesPerSec, language)}/s"
            AppLanguage.ENGLISH -> "${localizedUpdateSize(state.speedBytesPerSec, language)}/s"
            AppLanguage.ARABIC -> "${localizedUpdateSize(state.speedBytesPerSec, language)}/ث"
        })
        if (state.activeConnections > 1) add(when (language) {
            AppLanguage.FRENCH -> "${state.activeConnections} flux"
            AppLanguage.ENGLISH -> "${state.activeConnections} streams"
            AppLanguage.ARABIC -> "${state.activeConnections} اتصالات"
        })
    }.joinToString(" · ")
    val suffix = if (extras.isNotEmpty()) " · $extras" else ""
    val current = localizedUpdateSize(state.bytesRead, language)
    return if (state.totalBytes > 0) {
        "${state.percent ?: 0} % · $current / ${localizedUpdateSize(state.totalBytes, language)}$suffix"
    } else when (language) {
        AppLanguage.FRENCH -> "$current téléchargés$suffix"
        AppLanguage.ENGLISH -> "$current downloaded$suffix"
        AppLanguage.ARABIC -> "تم تنزيل $current$suffix"
    }
}
