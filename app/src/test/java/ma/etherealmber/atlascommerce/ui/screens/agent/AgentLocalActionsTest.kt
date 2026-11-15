package ma.etherealmber.atlascommerce.ui.screens.agent

import ma.etherealmber.atlascommerce.core.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgentLocalActionsTest {
    @Test
    fun `explicit theme request from tablet feedback is handled locally`() {
        assertEquals(
            AgentLocalAction.SetTheme(ThemeMode.DARK),
            detectExplicitAgentLocalAction("change theme to dark"),
        )
    }

    @Test
    fun `theme request without a target opens the in-agent chooser`() {
        assertEquals(
            AgentLocalAction.ChooseTheme,
            detectExplicitAgentLocalAction("change the theme"),
        )
    }

    @Test
    fun `broad settings request opens the in-agent settings panel`() {
        assertEquals(
            AgentLocalAction.ShowSettingsPanel,
            detectExplicitAgentLocalAction("Hey, I want to change the app appearance"),
        )
        assertEquals(
            AgentLocalAction.ShowSettingsPanel,
            detectExplicitAgentLocalAction("change the app appaerence"),
        )
        assertEquals(
            AgentLocalAction.ShowSettingsPanel,
            detectExplicitAgentLocalAction("show all settings"),
        )
        assertEquals(AgentLocalAction.ShowSettingsPanel, detectExplicitAgentLocalAction("settings"))
    }

    @Test
    fun `theme-only request does not open unrelated appearance settings`() {
        assertEquals(
            AgentLocalAction.ChooseTheme,
            detectExplicitAgentLocalAction("change the app theme"),
        )
    }

    @Test
    fun `issue tracker setup questions open verified work status`() {
        assertEquals(
            AgentLocalAction.ShowWorkStatus,
            detectExplicitAgentLocalAction("Is the issue tracker set up or not?"),
        )
    }

    @Test
    fun `tracker status only states what server availability confirms`() {
        assertEquals(IssueTrackerSetupState.AVAILABLE, issueTrackerSetupState("available"))
        assertEquals(IssueTrackerSetupState.NOT_CONFIGURED, issueTrackerSetupState("not_configured"))
        assertEquals(IssueTrackerSetupState.UNKNOWN, issueTrackerSetupState("unavailable"))
        assertEquals(IssueTrackerSetupState.UNKNOWN, issueTrackerSetupState(null))
    }

    @Test
    fun `question about current theme does not change app preferences`() {
        assertNull(detectExplicitAgentLocalAction("What theme is the app using?"))
    }
}
