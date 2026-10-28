package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.data.model.AgentThreadSummaryDto
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentNavigationDestination
import ma.etherealmber.atlascommerce.ui.screens.agent.clearSubmittedAgentDraft
import ma.etherealmber.atlascommerce.ui.screens.agent.detectAgentNavigationSuggestion
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
    fun locationQuestionOffersTheMatchingProductsArea() {
        val action = detectAgentNavigationSuggestion("Where can I find this product in the inventory?")

        assertEquals(AgentNavigationDestination.PRODUCTS, action?.destination)
    }

    @Test
    fun customDateReportPreservesTheRequestedMonthRange() {
        val action = detectAgentNavigationSuggestion("Open the report for October–December 2026")

        assertEquals(AgentNavigationDestination.BALANCE_REPORT, action?.destination)
        assertEquals("2026-10-01", action?.fromDate)
        assertEquals("2026-12-31", action?.toDate)
    }

    @Test
    fun unrelatedPromptDoesNotCreateANavigationAction() {
        assertEquals(null, detectAgentNavigationSuggestion("Summarize today's low stock items"))
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
