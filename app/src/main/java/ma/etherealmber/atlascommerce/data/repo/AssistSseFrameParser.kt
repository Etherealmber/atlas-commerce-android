package ma.etherealmber.atlascommerce.data.repo

import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.data.model.AssistChatResponseDto

/** One complete Server-Sent Event frame, with multiple `data:` lines joined per SSE rules. */
internal data class AssistSseFrame(val event: String, val data: String)

/**
 * Line-level SSE framing. The UTF-8 reader belongs to the caller so multibyte characters split
 * across socket reads are decoded before reaching this parser.
 */
internal class AssistSseFrameParser(
    private val onFrame: (AssistSseFrame) -> Unit,
) {
    private var eventName = ""
    private val dataLines = mutableListOf<String>()

    fun accept(line: String) {
        if (line.isEmpty()) {
            dispatch()
            return
        }
        if (line.startsWith(':')) return // SSE comment / keep-alive

        val colon = line.indexOf(':')
        val field = if (colon < 0) line else line.substring(0, colon)
        var value = if (colon < 0) "" else line.substring(colon + 1)
        if (value.startsWith(' ')) value = value.substring(1)

        when (field) {
            "event" -> eventName = value
            "data" -> dataLines += value
            // id, retry, and future extension fields are intentionally ignored.
        }
    }

    /** SSE discards an unfinished frame at EOF; only a blank line commits a frame. */
    fun finish() {
        eventName = ""
        dataLines.clear()
    }

    private fun dispatch() {
        if (dataLines.isNotEmpty()) onFrame(AssistSseFrame(eventName, dataLines.joinToString("\n")))
        eventName = ""
        dataLines.clear()
    }
}

internal fun finishAssistSse(
    completed: AssistChatResponseDto?,
    failure: ApiResult.Failure?,
): ApiResult<AssistChatResponseDto> = when {
    failure != null -> failure
    completed != null -> ApiResult.Success(completed)
    else -> ApiResult.Failure("STREAM_INCOMPLETE", "Le flux Atlas Agent s'est interrompu sans réponse complète.")
}
