package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.data.model.AgentThreadSummaryDto
import ma.etherealmber.atlascommerce.ui.screens.agent.clearSubmittedAgentDraft
import ma.etherealmber.atlascommerce.ui.screens.agent.newestAgentThreadsFirst
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentConversationStateTest {

    @Test
    fun sentDraftIsClearedWhenComposerHasWhitespaceAroundSubmittedText() {
        assertEquals("", clearSubmittedAgentDraft("  bonjour Atlas  ", "bonjour Atlas"))
    }

    @Test
    fun newerDraftIsPreservedWhenItDiffersFromSubmittedText() {
        assertEquals("question suivante", clearSubmittedAgentDraft("question suivante", "question précédente"))
    }

    @Test
    fun latestUpdatedConversationIsFirstAndEqualTimestampsKeepApiOrder() {
        val threads = listOf(
            AgentThreadSummaryDto(id = "older", updatedAt = "2026-10-25T10:00:00Z"),
            AgentThreadSummaryDto(id = "newer-a", updatedAt = "2026-10-27T12:00:00Z"),
            AgentThreadSummaryDto(id = "newer-b", updatedAt = "2026-10-27T12:00:00Z"),
            AgentThreadSummaryDto(id = "unknown"),
        )

        assertEquals(
            listOf("newer-a", "newer-b", "older", "unknown"),
            newestAgentThreadsFirst(threads).map { it.id },
        )
    }
}
