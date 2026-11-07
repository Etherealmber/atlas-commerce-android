package ma.etherealmber.atlascommerce.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.data.model.AssistChatResponseDto
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FilterInputStream

class AssistSseFrameParserTest {

    @Test
    fun `parses direct json data frames and ignores unknown fields and events`() {
        val frames = mutableListOf<AssistSseFrame>()
        val parser = AssistSseFrameParser { frames.add(it); Unit }

        parser.accept("event: token")
        parser.accept("data: {")
        parser.accept("data: \"t\":\"Bonjour vous\"")
        parser.accept("data: }")
        parser.accept("")
        parser.accept("event: tool")
        parser.accept("data: {\"name\":\"inventory.read\",\"ok\":true}")
        parser.accept("")
        parser.accept("event: future_extension")
        parser.accept("data: {\"future\":true}")
        parser.accept("")
        parser.accept(": keep-alive")
        parser.accept("id: 9")
        parser.accept("retry: 1000")
        parser.accept("event: done")
        parser.accept("data: {\"reply\":\"Bonjour vous\"}")
        parser.accept("")
        parser.accept("event: error")
        parser.accept("data: {\"code\":\"QUOTA\",\"error\":\"Quota reached\"}")
        parser.accept("")
        parser.finish()

        assertEquals(listOf("token", "tool", "future_extension", "done", "error"), frames.map { it.event })
        assertEquals("{\n\"t\":\"Bonjour vous\"\n}", frames[0].data)
        assertEquals("{\"name\":\"inventory.read\",\"ok\":true}", frames[1].data)
        assertEquals("{\"reply\":\"Bonjour vous\"}", frames[3].data)
        assertEquals("{\"code\":\"QUOTA\",\"error\":\"Quota reached\"}", frames[4].data)
    }

    @Test
    fun `stream errors and missing done are failures rather than completed replies`() {
        val final = AssistChatResponseDto(reply = "authoritative")
        assertEquals(ApiResult.Success(final), finishAssistSse(final, null))
        assertEquals(
            "QUOTA",
            (finishAssistSse(final, ApiResult.Failure("QUOTA", "Quota reached")) as ApiResult.Failure).code,
        )
        assertEquals(
            "STREAM_INCOMPLETE",
            (finishAssistSse(null, null) as ApiResult.Failure).code,
        )
    }

    @Test
    fun `utf8 characters split across socket reads survive decoding and eof drops unfinished frame`() {
        val wire = "event: token\ndata: {\"t\":\"été 東京 مرحبا\"}\n\n" +
            "event: done\ndata: {\"reply\":\"must not be dispatched without frame terminator\"}"
        val bytes = wire.toByteArray(Charsets.UTF_8)
        val oneByteReads = object : FilterInputStream(ByteArrayInputStream(bytes)) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                super.read(buffer, offset, minOf(length, 1))
        }
        val frames = mutableListOf<AssistSseFrame>()
        val parser = AssistSseFrameParser { frames.add(it); Unit }
        oneByteReads.bufferedReader(Charsets.UTF_8).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                parser.accept(line)
            }
            parser.finish()
        }

        assertEquals(1, frames.size)
        assertEquals("token", frames.single().event)
        assertTrue(frames.single().data.contains("été 東京 مرحبا"))
    }
}
