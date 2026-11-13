package ma.etherealmber.atlascommerce.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentContextNeedsTest {
    @Test
    fun settingsNavigationDoesNotFetchBusinessContext() {
        assertTrue(agentContextNeeds("Can you put me inside the settings?").isEmpty)
    }

    @Test
    fun genericWritingDoesNotFetchBusinessContext() {
        assertTrue(agentContextNeeds("Rewrite this email in a warmer tone").isEmpty)
    }

    @Test
    fun stockQuestionSelectsCatalogueWithoutUnrelatedFinanceAndClientReads() {
        val needs = agentContextNeeds("Which products are out of stock?")

        assertTrue(needs.products)
        assertFalse(needs.finance)
        assertFalse(needs.clients)
        assertFalse(needs.actions)
        assertFalse(needs.alerts)
    }

    @Test
    fun clientQuestionSelectsOnlyClientData() {
        val needs = agentContextNeeds("Find this customer and show the contact details")

        assertTrue(needs.clients)
        assertFalse(needs.products)
        assertFalse(needs.finance)
    }

    @Test
    fun payrollAndSalaryQuestionsSelectFinanceContext() {
        assertTrue(agentContextNeeds("How much payroll is due?").finance)
        assertTrue(agentContextNeeds("Combien de salaires restent à payer ?").finance)
        assertTrue(agentContextNeeds("كم أجور الموظفين؟").finance)
    }

    @Test
    fun businessOverviewSelectsDashboard() {
        val needs = agentContextNeeds("Give me an overview of the company")

        assertTrue(needs.dashboard)
        assertFalse(needs.products)
        assertFalse(needs.clients)
        assertFalse(needs.actions)
    }

    @Test
    fun nonBusinessAndUnclassifiedQuestionsDoNotTriggerExpensiveBroadSnapshot() {
        assertEquals(AgentContextNeeds(), agentContextNeeds("Tell me a short joke"))
        assertEquals(AgentContextNeeds(), agentContextNeeds("How are you?"))
    }

    @Test
    fun generalBusinessQuestionUsesCompactDashboardContext() {
        val needs = agentContextNeeds("How is my business doing?")

        assertTrue(needs.dashboard)
        assertFalse(needs.products)
        assertFalse(needs.clients)
        assertFalse(needs.finance)
        assertFalse(needs.actions)
        assertFalse(needs.alerts)
    }
}
