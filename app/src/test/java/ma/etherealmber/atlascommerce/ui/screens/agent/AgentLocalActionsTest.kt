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
    fun `broad appearance request opens the in-agent appearance settings`() {
        assertEquals(
            AgentLocalAction.ShowAppearanceSettings,
            detectExplicitAgentLocalAction("Hey, I want to change the app appearance"),
        )
        assertEquals(
            AgentLocalAction.ShowAppearanceSettings,
            detectExplicitAgentLocalAction("change the app appaerence"),
        )
    }

    @Test
    fun `theme-only request does not open unrelated appearance settings`() {
        assertEquals(
            AgentLocalAction.ChooseTheme,
            detectExplicitAgentLocalAction("change the app theme"),
        )
    }

    @Test
    fun `question about current theme does not change app preferences`() {
        assertNull(detectExplicitAgentLocalAction("What theme is the app using?"))
    }
}
