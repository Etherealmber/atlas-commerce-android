package ma.etherealmber.atlascommerce.ui.screens.agent

import java.text.Normalizer
import java.util.Locale
import ma.etherealmber.atlascommerce.core.ThemeMode

/** Small native actions that are safe to execute directly from an explicit user request. */
internal enum class IssueTrackerSetupState { AVAILABLE, NOT_CONFIGURED, UNKNOWN }

internal fun issueTrackerSetupState(availability: String?): IssueTrackerSetupState = when (availability?.trim()?.lowercase(Locale.ROOT)) {
    "available" -> IssueTrackerSetupState.AVAILABLE
    "unconfigured", "not_configured", "not configured", "not-configured" -> IssueTrackerSetupState.NOT_CONFIGURED
    else -> IssueTrackerSetupState.UNKNOWN
}

internal sealed interface AgentLocalAction {
    data object CheckForUpdate : AgentLocalAction
    data object ShowWorkStatus : AgentLocalAction
    data class ChooseTheme(val requestedMode: ThemeMode? = null) : AgentLocalAction
    data object ShowSettingsPanel : AgentLocalAction
    data object ConfigureDeviceUnlock : AgentLocalAction
}

/**
 * Recognize safe native requests locally. Appearance requests offer in-chat choices; no
 * preference changes until the user explicitly selects one of those options.
 */
internal fun detectExplicitAgentLocalAction(prompt: String): AgentLocalAction? {
    val normalized = Normalizer.normalize(prompt, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .trim()
    val trackerInquiry = normalized.removePrefix("hey, ").removePrefix("hey ").removePrefix("hi, ").removePrefix("hi ")
    val asksForTrackerSetupStatus =
        listOf("issue tracker", "github issue", "github tracker", "reported issues").any(trackerInquiry::contains) &&
            listOf("configured", "set up", "setup", "connected", "status", "available", "working").any(trackerInquiry::contains) &&
            listOf("is ", "are ", "where ", "what is ", "show ", "check ", "tell me ", "do we have ", "i want to know ", "can you tell me ", "issue tracker status", "issue tracker setup", "github tracker status", "github issue status")
                .any(trackerInquiry::startsWith)
    if (asksForTrackerSetupStatus) return AgentLocalAction.ShowWorkStatus
    val standaloneSettingsRequest = trackerInquiry.trim().trimEnd('.', '!', '?')
    if (standaloneSettingsRequest in setOf("appearance", "app appearance", "the app appearance", "appearance settings")) {
        return AgentLocalAction.ChooseTheme()
    }
    if (standaloneSettingsRequest in setOf("settings", "app settings", "all settings", "all app settings")) {
        return AgentLocalAction.ShowSettingsPanel
    }

    val asksAboutAppearance = listOf(
        "app appearance", "the app appearance", "my app appearance", "app's appearance",
        "change appearance", "change the appearance", "app appaerence", "the app appaerence",
        "appearance settings", "customize appearance", "customize the app appearance",
        "change how the app looks", "change the app look", "look and feel",
    ).any(normalized::contains)
    val asksHowToChangeAppearance = asksAboutAppearance && listOf(
        "can i", "can i change", "how do i", "how can i", "is it possible", "i want to", "i want ",
        "i'd like to", "could you", "change", "customize", "show", "open",
    ).any(normalized::contains)
    if (asksHowToChangeAppearance) return AgentLocalAction.ChooseTheme()


    val directRequest = Regex(
        "^(?:(?:hey|hi|hello|bonjour|salut)[,!]?\\s+)?" +
            "(?:(?:please|kindly|can you|can u|could you|would you|i want to|i want you to|i would like to|i'd like to|" +
            "s'il te plait|s'il vous plait|je veux|j'aimerais|merci de)\\s+)?" +
            "(?:theme\\s+change\\b|theme\\s+settings\\b|check\\b|look\\b|search\\b|verify\\b|verifie\\b|verifier\\b|recherche\\b|chercher\\b|" +
            "show\\b|open\\b|view\\b|configure\\b|change\\b|set\\b|switch\\b|use\\b|turn\\b|enable\\b|make\\b|put\\b|" +
            "mets\\b|passe\\b|active\\b|تحقق|غير|بدل|ارجع|أظهر|اظهر|افتح|فعّل|فعل)",
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

    val asksForDeviceUnlock = listOf(
        "device unlock", "biometric sign-in", "biometric sign in", "fingerprint sign-in", "fingerprint sign in",
        "unlock with biometrics", "فتح القفل بالبصمة", "بصمة الجهاز",
    ).any(normalized::contains)
    if (asksForDeviceUnlock) return AgentLocalAction.ConfigureDeviceUnlock

    val asksForWorkStatus = listOf(
        "my work status", "work status", "my work", "my requests", "my reported issues", "my reports",
        "mes demandes", "mon activité", "mes signalements", "حالة أعمالي", "طلباتي",
    ).any(normalized::contains)
    if (asksForWorkStatus) return AgentLocalAction.ShowWorkStatus

    val asksForAppearanceSettings = listOf(
        "show settings", "show me settings", "show all settings", "show me all the settings", "show all those settings",
        "open settings", "open app settings", "app settings", "all app settings", "all the app settings",
        "change app settings", "change the app settings", "customize settings",
    ).any(normalized::contains)
    if (asksForAppearanceSettings) return AgentLocalAction.ShowSettingsPanel

    val asksToChangeTheme = listOf(
        "change theme", "change my theme", "change the theme", "change app theme", "change the app theme", "theme change",
        "set theme", "set the theme", "switch theme", "theme settings",
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
        else -> return AgentLocalAction.ChooseTheme()
    }
    return AgentLocalAction.ChooseTheme(mode)
}

/** Requests in this category are intentionally unsupported by Atlas Agent. */
internal fun isForbiddenAgentRequest(prompt: String): Boolean {
    val normalized = Normalizer.normalize(prompt, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
    val destructive = Regex("\\b(delete|remove|clear|erase|wipe|destroy|supprimer|effacer|vider|detruire)\\b|حذف|امسح|احذف|افرغ")
    val storage = Regex("\\b(cache|cached|local data|data storage|storage|donnees locales|stockage|memoire)\\b|الذاكرة|التخزين|البيانات")
    val access = Regex("\\b(access|open|view|inspect|read|browse|clean|clear|purge|manage|navigate|go to|show me|accesser|ouvrir|voir|consulter|nettoyer)\\b|ادخل|افتح|اعرض|اقرأ|امسح")
    val signOut = Regex("\\b(sign out|signout|log out|logout|log off|disconnect|deconnecter|deconnexion)\\b|تسجيل الخروج|اخرج")
    val server = Regex("\\b(server|serveur|serveurs|servers)\\b|الخادم|السيرفر")
    return destructive.containsMatchIn(normalized) ||
        (storage.containsMatchIn(normalized) && access.containsMatchIn(normalized)) ||
        signOut.containsMatchIn(normalized) ||
        (destructive.containsMatchIn(normalized) && server.containsMatchIn(normalized))
}

internal fun isForbiddenAgentProposal(proposal: ma.etherealmber.atlascommerce.data.model.AssistProposalDto): Boolean {
    val description = listOfNotNull(
        proposal.title, proposal.summary, proposal.action, proposal.kind, proposal.type, proposal.path,
    ).joinToString(" ").replace('_', ' ').replace('-', ' ')
    val normalized = Normalizer.normalize(description, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
    return isForbiddenAgentRequest(normalized) ||
        Regex("\\b(server|servers|serveur|serveurs|logout|sign out|log out|cache|storage|local data)\\b|الذاكرة|التخزين|الخادم|السيرفر")
            .containsMatchIn(normalized)
}
