package ma.etherealmber.atlascommerce.ui.screens.agent

import java.text.Normalizer
import java.util.Locale
import ma.etherealmber.atlascommerce.core.ThemeMode

/** Small native actions that are safe to execute directly from an explicit user request. */
internal sealed interface AgentLocalAction {
    data object CheckForUpdate : AgentLocalAction
    data class SetTheme(val mode: ThemeMode) : AgentLocalAction
}

/**
 * Recognize only direct commands. Questions, explanations, assistant suggestions, and casual
 * discussion are not permission to perform a local action.
 */
internal fun detectExplicitAgentLocalAction(prompt: String): AgentLocalAction? {
    val normalized = Normalizer.normalize(prompt, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .trim()
    val directRequest = Regex(
        "^(?:(?:please|kindly|can you|could you|would you|s'il te plait|s'il vous plait|merci de)\\s+)?" +
            "(?:check\\b|look\\b|search\\b|verify\\b|verifie\\b|verifier\\b|recherche\\b|chercher\\b|" +
            "change\\b|set\\b|switch\\b|use\\b|turn\\b|enable\\b|make\\b|put\\b|mets\\b|passe\\b|active\\b|" +
            "تحقق|غير|بدل|ارجع)",
    ).containsMatchIn(normalized)
    if (!directRequest) return null

    val negatesAction = listOf(
        "don't", "do not", "never", "not ", "ne pas", "ne change pas", "ne mets pas", "n'active pas",
        "لا تتحقق", "لا تغير", "لا تغيّر", "لا تبدل",
    ).any(normalized::contains)
    if (negatesAction) return null

    val asksToCheckUpdates = listOf(
        "check for updates", "check updates", "check for an update", "check if there is an update",
        "look for updates", "see if there is an update", "verifie les mises a jour", "verifier les mises a jour",
        "recherche les mises a jour", "chercher les mises a jour", "تحقق من التحديث", "تحقق من التحديثات",
    ).any(normalized::contains)
    if (asksToCheckUpdates) return AgentLocalAction.CheckForUpdate

    val asksToChangeTheme = listOf(
        "change theme", "change the theme", "set theme", "set the theme", "switch theme",
        "switch to dark", "switch to light", "switch to system", "use dark mode", "use light mode",
        "use system theme", "use the system theme", "use the light theme", "use the dark theme",
        "turn on dark mode", "turn on light mode",
        "enable dark mode", "enable light mode", "make it dark", "make it light",
        "put it in dark mode", "put it in light mode", "change le theme", "mets le theme",
        "passe en mode sombre", "passe en mode clair", "active le mode sombre", "active le mode clair",
        "ارجع للوضع الداكن", "غير المظهر",
    ).any(normalized::contains)
    if (!asksToChangeTheme) return null

    // Ignore the old mode in corrective phrases such as "switch to light from dark".
    val commandPart = when {
        " from " in normalized -> normalized.substringAfterLast(" from ")
            .takeIf { " to " in it }?.substringAfterLast(" to ") ?: normalized.substringBeforeLast(" from ")
        " depuis " in normalized -> normalized.substringBeforeLast(" depuis ")
        else -> normalized.substringBefore(" instead of ").substringBefore(" au lieu de ")
    }
    val targetSeparator = listOf(" to ", " en mode ", " mode ").firstOrNull(commandPart::contains)
    val targetPhrase = targetSeparator?.let { commandPart.substringAfterLast(it) } ?: commandPart
    val mode = when {
        listOf("system", "automatic", "auto", "systeme", "automatique", "تلقائي").any(targetPhrase::contains) -> ThemeMode.SYSTEM
        listOf("dark", "sombre", "noir", "داكن", "مظلم").any(targetPhrase::contains) -> ThemeMode.DARK
        listOf("light", "clair", "blanc", "فاتح").any(targetPhrase::contains) -> ThemeMode.LIGHT
        else -> return null
    }
    return AgentLocalAction.SetTheme(mode)
}
