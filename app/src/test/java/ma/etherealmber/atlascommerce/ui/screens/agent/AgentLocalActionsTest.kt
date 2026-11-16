package ma.etherealmber.atlascommerce.ui.screens.agent

import ma.etherealmber.atlascommerce.core.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentLocalActionsTest {
    @Test
    fun `specific theme requests produce an inline confirmation instead of applying`() {
        assertEquals(
            AgentLocalAction.ChooseTheme(ThemeMode.DARK),
            detectExplicitAgentLocalAction("change theme to dark"),
        )
    }

    @Test
    fun `theme request without a target opens the in-agent chooser`() {
        assertEquals(
            AgentLocalAction.ChooseTheme(),
            detectExplicitAgentLocalAction("change the theme"),
        )
    }

    @Test
    fun `appearance requests offer inline theme choices instead of opening settings`() {
        assertEquals(
            AgentLocalAction.ChooseTheme(),
            detectExplicitAgentLocalAction("Hey, I want to change the app appearance"),
        )
        assertEquals(
            AgentLocalAction.ChooseTheme(),
            detectExplicitAgentLocalAction("Can I change the app appearance?"),
        )
        assertEquals(AgentLocalAction.ChooseTheme(), detectExplicitAgentLocalAction("app appearance"))
        assertEquals(
            AgentLocalAction.ChooseTheme(),
            detectExplicitAgentLocalAction("change the app appaerence"),
        )
        assertEquals(AgentLocalAction.ShowSettingsPanel, detectExplicitAgentLocalAction("show all settings"))
        assertEquals(AgentLocalAction.ShowSettingsPanel, detectExplicitAgentLocalAction("settings"))
    }

    @Test
    fun `theme-only request does not open unrelated appearance settings`() {
        assertEquals(
            AgentLocalAction.ChooseTheme(),
            detectExplicitAgentLocalAction("change the app theme"),
        )
    }

    @Test
    fun `saved theme choice prompts restore their inline controls`() {
        assertTrue(isAgentThemeChoicePrompt("Which theme would you like? Choose one below."))
        assertTrue(isAgentThemeChoicePrompt("Quel thème souhaitez-vous ? Choisissez une option ci-dessous."))
        assertFalse(isAgentThemeChoicePrompt("Done. The dark theme is on."))
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
