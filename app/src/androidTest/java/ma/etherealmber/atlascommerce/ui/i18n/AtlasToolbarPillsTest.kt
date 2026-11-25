package ma.etherealmber.atlascommerce.ui.i18n

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import ma.etherealmber.atlascommerce.ui.theme.AtlasTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AtlasToolbarPillsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun languagePillPressedStateReturnsToNeutralAfterRelease() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AtlasTheme {
                AtlasLanguageToolbarPill(modifier = Modifier.testTag("language-pill"))
            }
        }
        val pill = compose.onNodeWithContentDescription("Langue")
        pill.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(140L)
        val pressed = pill.fetchSemanticsNode().config[AtlasToolbarPillMotionKey]
        assertEquals(0.94f, pressed.scale, 0.001f)
        assertNotEquals(androidx.compose.ui.graphics.Color.Transparent, pressed.background)

        pill.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(200L)
        val released = pill.fetchSemanticsNode().config[AtlasToolbarPillMotionKey]
        assertEquals(1f, released.scale, 0.001f)
        assertEquals(androidx.compose.ui.graphics.Color.Transparent, released.background)
    }

    @Test
    fun themeOptionsStayInsideNarrowToolbarAtRequestedWidths() {
        listOf(320, 360, 390, 412).forEach { widthDp ->
            compose.setContent {
                val configuration = Configuration().apply {
                    screenWidthDp = widthDp
                    screenHeightDp = 800
                }
                CompositionLocalProvider(LocalConfiguration provides configuration) {
                    AtlasTheme {
                        Box(Modifier.width(widthDp.dp).height(56.dp).testTag("toolbar")) {
                            Row(Modifier.testTag("actions")) {
                                AtlasThemeToolbarControl()
                                AtlasLanguageToolbarPill()
                            }
                        }
                    }
                }
            }
            val bar = compose.onNodeWithTag("toolbar").fetchSemanticsNode().boundsInRoot
            val light = compose.onNodeWithContentDescription("Clair").fetchSemanticsNode().boundsInRoot
            val dark = compose.onNodeWithContentDescription("Sombre").fetchSemanticsNode().boundsInRoot
            val system = compose.onNodeWithContentDescription("Système").fetchSemanticsNode().boundsInRoot
            val language = compose.onNodeWithContentDescription("Langue").fetchSemanticsNode().boundsInRoot
            compose.runOnIdle {
                listOf(light, dark, system, language).forEach { bounds ->
                    assertTrue(bounds.left >= bar.left)
                    assertTrue(bounds.right <= bar.right)
                    assertTrue(bounds.top >= bar.top)
                    assertTrue(bounds.bottom <= bar.bottom)
                }
                assertTrue(!light.overlaps(dark))
                assertTrue(!dark.overlaps(system))
                assertTrue(!system.overlaps(language))
            }
        }
    }
}
