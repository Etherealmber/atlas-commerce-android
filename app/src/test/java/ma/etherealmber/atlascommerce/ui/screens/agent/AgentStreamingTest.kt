package ma.etherealmber.atlascommerce.ui.screens.agent

import ma.etherealmber.atlascommerce.data.model.AssistChatResponseDto
import ma.etherealmber.atlascommerce.data.model.AssistProposalDto
import ma.etherealmber.atlascommerce.data.model.AssistToolTraceDto
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AgentStreamingTest {

    @Test
    fun `done reply replaces provisional tokens without duplication`() {
        assertEquals(
            "Bonjour à vous.",
            replaceProvisionalReply("Bonjour ", "Bonjour à vous."),
        )
    }

    @Test
    fun `done json decodes tool trace proposals and final response metadata`() {
        val payload = """
            {
              "reply":"Done reply",
              "tool_trace":[{"name":"inventory.read","ok":true}],
              "proposals":[{"type":"inventory_adjustment","product_id":1,"product_name":"Produit","sku":"P1","delta":2,"current_stock":20,"requires_approval":true}],
              "usage":{"prompt":20,"completion":8},
              "documents":[],
              "thread_id":"android-thread",
              "task":"general",
              "history_turns":0
            }
        """.trimIndent()
        val response = Moshi.Builder().build()
            .adapter(AssistChatResponseDto::class.java)
            .fromJson(payload)!!

        assertEquals("Done reply", response.reply)
        assertEquals("android-thread", response.threadId)
        assertEquals(20L, response.usage?.prompt)
        assertEquals("inventory_adjustment", response.proposals.single().type)
        assertEquals(1L, response.proposals.single().productId)
        assertEquals("inventory.read", response.toolTrace.single().name)
    }

    @Test
    fun `stream final metadata stays attached and proposals keep only server supplied ids`() {
        val finalResponse = AssistChatResponseDto(
            reply = "Stock ajusté sur proposition.",
            toolTrace = listOf(AssistToolTraceDto(name = "inventory.adjust.propose", ok = true)),
            proposals = listOf(
                AssistProposalDto(
                    type = "inventory_adjustment",
                    productId = 1,
                    productName = "Produit",
                    sku = "P1",
                    delta = 2.0,
                    currentStock = 20.0,
                    note = "Proposition assistant IA",
                    requiresApproval = true,
                ),
            ),
        )

        assertEquals("inventory.adjust.propose", finalResponse.toolTrace.single().name)
        assertEquals("Produit", finalResponse.proposals.single().productName)
        assertFalse(finalResponse.proposals.single().proposalId.orEmpty().isNotBlank())
    }
}
