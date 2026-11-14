package ma.etherealmber.atlascommerce.data.repo

import ma.etherealmber.atlascommerce.core.AppLanguage

internal const val AGENT_MAX_QUESTION_LENGTH = 1800
private const val AGENT_MAX_PROMPT_LENGTH = 2450

/** Preserve the question and language instruction before adding bounded optional context. */
internal fun composeAgentPrompt(question: String, snapshot: String, language: AppLanguage): String {
    val prefix = buildString {
        appendLine(language.agentContextInstruction)
        appendLine("${language.agentQuestionLabel}: $question")
        appendLine("[REPORT SCOPE] Live data is not a custom date-range report. Do not invent figures; direct the user to the server-generated report if authorized.")
        appendLine("[MOBILE] Use role-authorized APIs and only these routes: Home, Products, Scan, Sales, Reports/Admin, Employees/Admin, Notifications, Settings, Profile, Data storage. There is no Projects route/API; do not invent project links. Explain this and offer a supported route.")
        appendLine("[INSIGHTS] For an improvement request, use the supplied permitted live context to give up to three specific, practical suggestions; cite only available facts and offer a supported page when it helps.")
    }
    return prefix + snapshot.take((AGENT_MAX_PROMPT_LENGTH - prefix.length).coerceAtLeast(0))
}
