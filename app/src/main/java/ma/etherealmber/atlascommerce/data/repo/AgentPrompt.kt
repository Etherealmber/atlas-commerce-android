package ma.etherealmber.atlascommerce.data.repo

import ma.etherealmber.atlascommerce.core.AppLanguage

internal const val AGENT_MAX_QUESTION_LENGTH = 1800
internal const val AGENT_MAX_PROMPT_LENGTH = 2450

/** Keep the accepted question and language instruction, then fit useful live data in the same budget. */
internal fun composeAgentPrompt(question: String, snapshot: String, language: AppLanguage): String {
    val guidance = buildString {
        appendLine("[DATA] Supplied, role-authorized facts only; don't invent.")
        appendLine("[ROUTES] Implemented mobile routes only; no Projects.")
        appendLine("[STATUS] No tracker feed; never invent issue IDs/status.")
        appendLine("[RELEASE] Group feature/improvement/fix/issue; cite supplied refs.")
        append("[IDEAS] Three max, data-grounded.")
    }
    val fixedPrefix = "${language.agentContextInstruction}\n${language.agentQuestionLabel}: "
    val questionBudget = (AGENT_MAX_PROMPT_LENGTH - fixedPrefix.length - 1 - guidance.length - 1)
        .coerceAtLeast(0)
    val boundedQuestion = question.take(minOf(AGENT_MAX_QUESTION_LENGTH, questionBudget))
    val prefix = "$fixedPrefix$boundedQuestion\n$guidance\n"
    val snapshotBudget = (AGENT_MAX_PROMPT_LENGTH - prefix.length).coerceAtLeast(0)
    return prefix + snapshot.take(snapshotBudget)
}
