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
    }
    return prefix + snapshot.take((AGENT_MAX_PROMPT_LENGTH - prefix.length).coerceAtLeast(0))
}
