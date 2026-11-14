package ma.etherealmber.atlascommerce.data.repo

import ma.etherealmber.atlascommerce.core.AppLanguage

internal const val AGENT_MAX_QUESTION_LENGTH = 1800
internal const val AGENT_MAX_PROMPT_LENGTH = 2450

/** Keep the accepted question and language instruction, then fit useful live data in the same budget. */
internal fun composeAgentPrompt(question: String, snapshot: String, language: AppLanguage): String {
    val guidance = buildString {
        appendLine("[REPORT] Live data is not a custom date-range report. Do not invent figures; use the server report when authorized.")
        appendLine("[MOBILE] Use role-authorized APIs and supported routes only: Home, Products, Scan, Sales, Reports/Admin, Employees/Admin, Notifications, Settings, Profile, Data storage. No Projects route/API exists; never invent project links.")
        append("[INSIGHTS] For improvement requests, give up to three practical suggestions grounded only in permitted live data, and offer a supported page when useful.")
    }
    val fixedPrefix = "${language.agentContextInstruction}\n${language.agentQuestionLabel}: "
    val questionBudget = (AGENT_MAX_PROMPT_LENGTH - fixedPrefix.length - 1 - guidance.length - 1)
        .coerceAtLeast(0)
    val boundedQuestion = question.take(minOf(AGENT_MAX_QUESTION_LENGTH, questionBudget))
    val prefix = "$fixedPrefix$boundedQuestion\n$guidance\n"
    val snapshotBudget = (AGENT_MAX_PROMPT_LENGTH - prefix.length).coerceAtLeast(0)
    return prefix + snapshot.take(snapshotBudget)
}
