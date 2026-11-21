package ma.etherealmber.atlascommerce.data.repo

import ma.etherealmber.atlascommerce.core.AppLanguage
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal const val AGENT_MAX_QUESTION_LENGTH = 1800
internal const val AGENT_MAX_PROMPT_LENGTH = 2450

/** Supplies a trustworthy device-local clock and clear behavior in all supported languages. */
internal fun localizedAgentClockContext(now: ZonedDateTime, language: AppLanguage): String {
    val timestamp = now.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
    return when (language) {
        AppLanguage.FRENCH -> "Heure actuelle de l'appareil : $timestamp. Utilise cette heure pour les questions de date ou d'heure. Pour une autre ville, convertis cet instant avec son fuseau horaire connu; sinon, demande la ville ou la région au lieu de deviner. Ne dis pas que l'heure actuelle est inaccessible."
        AppLanguage.ENGLISH -> "Current device time: $timestamp. Use it for current date/time questions. For another city, convert this instant using its known time zone; otherwise ask for the city or region instead of guessing. Do not claim the current time is unavailable."
        AppLanguage.ARABIC -> "الوقت الحالي للجهاز: $timestamp. استخدمه للإجابة عن أسئلة التاريخ والوقت الحاليين. لمدينة أخرى، حوّل هذه اللحظة وفق منطقتها الزمنية المعروفة؛ وإلا فاسأل عن المدينة أو المنطقة بدلاً من التخمين. لا تقل إن الوقت الحالي غير متاح."
    }
}

/** Keep the accepted question and language instruction, then fit useful live data in the same budget. */
internal fun composeAgentPrompt(
    question: String,
    snapshot: String,
    language: AppLanguage,
    localTimeContext: String = "",
): String {
    val guidance = buildString {
        appendLine("[DATA] Supplied, role-authorized facts only; don't invent.")
        appendLine("[ROUTES] Implemented mobile routes only; no Projects.")
        appendLine("[STATUS] No tracker feed; never invent issue IDs/status.")
        appendLine("[RELEASE] Group feature/improvement/fix/issue; cite supplied refs.")
        if (localTimeContext.isNotBlank()) appendLine("[CLOCK] $localTimeContext")
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
