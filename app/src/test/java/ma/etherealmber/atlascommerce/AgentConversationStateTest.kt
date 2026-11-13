package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.data.model.AgentThreadSummaryDto
import ma.etherealmber.atlascommerce.data.model.AssistProposalDto
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentNavigationDestination
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentViewModel
import ma.etherealmber.atlascommerce.ui.screens.agent.detectExplicitAgentNavigationRequest
import ma.etherealmber.atlascommerce.ui.screens.agent.mobileNavigationSuggestion
import ma.etherealmber.atlascommerce.ui.screens.agent.clearSubmittedAgentDraft
import ma.etherealmber.atlascommerce.ui.screens.agent.detectAgentNavigationSuggestion
import ma.etherealmber.atlascommerce.ui.screens.agent.firstSelectableAgentThread
import ma.etherealmber.atlascommerce.ui.screens.agent.newestAgentThreadsFirst
import ma.etherealmber.atlascommerce.ui.screens.agent.shouldShowAgentChatSkeleton
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
    fun explicitNaturalLanguageSettingsCommandIsRecognizedLocally() {
        val action = detectExplicitAgentNavigationRequest("Can you put me inside the settings?")

        assertEquals(AgentNavigationDestination.SETTINGS, action?.destination)
    }

    @Test
    fun openInvoiceQuestionDoesNotBecomeNavigation() {
        assertEquals(null, detectExplicitAgentNavigationRequest("How many open invoices are unpaid?"))
        assertEquals(
            AgentNavigationDestination.SETTINGS,
            detectExplicitAgentNavigationRequest("Can you put me inside the settings?")?.destination,
        )
    }

    @Test
    fun mobileUiNavigationMapsOnlyAllowlistedNativeRoutes() {
        val action = mobileNavigationSuggestion(
            AssistProposalDto(type = "mobile_ui", action = "NAVIGATE", path = "/m/settings"),
        )

        assertEquals(AgentNavigationDestination.SETTINGS, action?.destination)
        assertEquals(
            null,
            mobileNavigationSuggestion(
                AssistProposalDto(type = "mobile_ui", action = "NAVIGATE", path = "https://evil.example/"),
            ),
        )
        assertEquals(
            null,
            mobileNavigationSuggestion(
                AssistProposalDto(type = "mobile_ui", action = "NAVIGATE", path = "/m/unknown"),
            ),
        )
    }

    @Test
    fun agentLoaderDoesNotReplaceExistingConversationMessages() {
        val loadingEmptyState = AgentViewModel.UiState(loadingContext = true)
        val loadingThreadState = AgentViewModel.UiState(loadingThread = true)
        val loadedConversationRefreshingContext = AgentViewModel.UiState(
            loadingContext = true,
            messages = listOf(AgentViewModel.ChatMessage(fromUser = true, text = "Keep showing this")),
        )

        assertEquals(true, shouldShowAgentChatSkeleton(loadingEmptyState))
        assertEquals(true, shouldShowAgentChatSkeleton(loadingThreadState))
        assertEquals(false, shouldShowAgentChatSkeleton(loadedConversationRefreshingContext))
        assertEquals(false, shouldShowAgentChatSkeleton(AgentViewModel.UiState()))
    }

    @Test
    fun latestRemoteThreadWithBlankIdIsSkippedForAutomaticLoading() {
        val threads = listOf(
            AgentThreadSummaryDto(id = "", updatedAt = "2026-10-28T12:00:00Z"),
            AgentThreadSummaryDto(id = "valid", updatedAt = "2026-10-27T12:00:00Z"),
        )

        assertEquals("valid", firstSelectableAgentThread(newestAgentThreadsFirst(threads))?.id)
        assertEquals(null, firstSelectableAgentThread(listOf(AgentThreadSummaryDto(id = ""))))
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
