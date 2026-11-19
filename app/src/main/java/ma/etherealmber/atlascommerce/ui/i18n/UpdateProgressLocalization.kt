package ma.etherealmber.atlascommerce.ui.i18n

import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.update.GitHubLimitedOperation
import ma.etherealmber.atlascommerce.core.update.UpdateState
import java.text.NumberFormat
import java.util.Locale

fun localizedGitHubLimitNotice(seconds: Int, operation: GitHubLimitedOperation, language: AppLanguage): String {
    val wait = when {
        seconds >= 60 -> {
            val minutes = (seconds + 59) / 60
            when (language) {
                AppLanguage.FRENCH -> "$minutes min"
                AppLanguage.ENGLISH -> "$minutes min"
                AppLanguage.ARABIC -> "$minutes د"
            }
        }
        else -> when (language) {
            AppLanguage.FRENCH -> "$seconds s"
            AppLanguage.ENGLISH -> "$seconds sec"
            AppLanguage.ARABIC -> "$seconds ث"
        }
    }
    return when (operation) {
        GitHubLimitedOperation.RELEASE_HISTORY -> when (language) {
            AppLanguage.FRENCH -> "Limite temporaire de GitHub atteinte. Les notes de version disponibles localement restent affichées et les mises à jour utilisent Atlas API. Réessayez GitHub dans $wait."
            AppLanguage.ENGLISH -> "GitHub’s temporary request limit was reached. Locally available release notes remain visible and updates use Atlas API. Try GitHub again in $wait."
            AppLanguage.ARABIC -> "تم بلوغ حد GitHub المؤقت. تظل ملاحظات الإصدار المتاحة محلياً ظاهرة وتستخدم التحديثات Atlas API. أعد المحاولة بعد $wait."
        }
        GitHubLimitedOperation.UPDATE_CHECK -> when (language) {
            AppLanguage.FRENCH -> "Limite temporaire de GitHub atteinte. Atlas API est utilisé pour les mises à jour. Réessayez GitHub dans $wait."
            AppLanguage.ENGLISH -> "GitHub’s temporary request limit was reached. Atlas API is being used for updates. Try GitHub again in $wait."
            AppLanguage.ARABIC -> "تم بلوغ حد GitHub المؤقت. تُستخدم Atlas API للتحديثات. أعد المحاولة بعد $wait."
        }
        GitHubLimitedOperation.SOURCE_TEST -> when (language) {
            AppLanguage.FRENCH -> "GitHub est temporairement limité. Atlas API reste utilisé pour les mises à jour. Réessayez GitHub dans $wait."
            AppLanguage.ENGLISH -> "GitHub is temporarily rate-limited. Atlas API remains available for updates. Try GitHub again in $wait."
            AppLanguage.ARABIC -> "تم تقييد GitHub مؤقتاً. تظل Atlas API متاحة للتحديثات. أعد المحاولة بعد $wait."
        }
    }
}

fun localizedApiRefreshWait(seconds: Int, language: AppLanguage): String = when (language) {
    AppLanguage.FRENCH -> "Trop de demandes d’actualisation. Réessayez dans $seconds s."
    AppLanguage.ENGLISH -> "Too many refresh requests. Please slow down and try again in $seconds sec."
    AppLanguage.ARABIC -> "طلبات التحديث كثيرة. يرجى التمهل والمحاولة بعد $seconds ث."
}

fun localizedUpdateCheckCooldown(seconds: Int, language: AppLanguage): String = when (language) {
    AppLanguage.FRENCH -> if (seconds > 0) "La vérification est limitée. Réessayez dans $seconds s." else "Vous pouvez vérifier les mises à jour maintenant."
    AppLanguage.ENGLISH -> if (seconds > 0) "Update checks are rate-limited. Try again in $seconds sec." else "You can check for updates now."
    AppLanguage.ARABIC -> if (seconds > 0) "تم تقييد التحقق من التحديثات. أعد المحاولة بعد $seconds ث." else "يمكنك التحقق من التحديثات الآن."
}

fun localizedUpdateCooldownButton(seconds: Int, language: AppLanguage): String = when (language) {
    AppLanguage.FRENCH -> "Réessayez dans $seconds s"
    AppLanguage.ENGLISH -> "Try again in $seconds sec"
    AppLanguage.ARABIC -> "أعد المحاولة بعد $seconds ث"
}

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
