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
 * Recognize only direct update-check and theme-change requests. This deliberately does not
 * interpret an assistant suggestion or a casual discussion of dark mode as permission to act.
 */
internal fun detectExplicitAgentLocalAction(prompt: String): AgentLocalAction? {
    val normalized = Normalizer.normalize(prompt, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
    val negatesAction = listOf(
        "don't", "do not", "never", "not ", "ne pas", "ne change pas", "ne mets pas", "n'active pas",
        "لا تتحقق", "لا تغير", "لا تغيّر", "لا تبدل",
    ).any(normalized::contains)
    if (negatesAction) return null

    val asksToCheckUpdates = listOf(
        "check for updates", "check updates", "check for an update", "check if there is an update",
        "look for updates", "see if there is an update", "is there an update", "any app update",
        "verifie les mises a jour", "verifier les mises a jour", "recherche les mises a jour",
        "chercher les mises a jour", "y a-t-il une mise a jour", "une mise a jour disponible",
        "تحقق من التحديث", "تحقق من التحديثات", "هل يوجد تحديث",
    ).any(normalized::contains)
    if (asksToCheckUpdates) return AgentLocalAction.CheckForUpdate

    val asksToChangeTheme = listOf(
        "change theme", "change the theme", "set theme", "set the theme", "switch theme",
        "switch to dark", "switch to light", "switch to system", "use dark mode", "use light mode",
        "use system theme", "turn on dark mode", "turn on light mode", "enable dark mode",
        "enable light mode", "make it dark", "make it light", "put it in dark mode", "put it in light mode",
        "change le theme", "mets le theme", "passe en mode sombre", "passe en mode clair",
        "active le mode sombre", "active le mode clair", "ارجع للوضع الداكن", "غير المظهر",
    ).any(normalized::contains)
    if (!asksToChangeTheme) return null

    val mode = when {
        listOf("system", "automatic", "auto", "système", "systeme", "automatique", "تلقائي").any(normalized::contains) -> ThemeMode.SYSTEM
        listOf("dark", "sombre", "noir", "داكن", "مظلم").any(normalized::contains) -> ThemeMode.DARK
        listOf("light", "clair", "blanc", "فاتح").any(normalized::contains) -> ThemeMode.LIGHT
        else -> return null
    }
    return AgentLocalAction.SetTheme(mode)
}
