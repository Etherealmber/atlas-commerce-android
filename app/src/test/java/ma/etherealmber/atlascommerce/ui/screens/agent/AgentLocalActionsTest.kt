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
    fun `question about theme does not change app preferences`() {
        assertNull(detectExplicitAgentLocalAction("Would you change the theme to dark?"))
    }
}
