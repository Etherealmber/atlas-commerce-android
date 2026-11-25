package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import ma.etherealmber.atlascommerce.ui.theme.AtlasTheme
import ma.etherealmber.atlascommerce.core.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AtlasAnimatedBrandTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun introDrawsTicksThenBeamAndFinishesTheLockup() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(
                LocalAtlasAnimationsEnabled provides true,
                LocalSystemAnimatorEnabled provides true,
            ) {
                AtlasAnimatedBrand(
                    modifier = Modifier.testTag("brand"),
                    height = 54.dp,
                    tint = Color.Black,
                    contentDescription = "Animated Atlas lockup",
                )
            }
        }

        val start = snapshot()
        assertTrue(start.tickTrim.all { it == 0f })
        assertEquals(0f, start.beamTrim, 0f)
        compose.mainClock.advanceTimeBy(900L)
        val middle = snapshot()
        assertTrue(middle.tickTrim.all { it == 1f })
        assertTrue(middle.beamTrim > 0.5f)
        compose.mainClock.advanceTimeBy(700L)
        val end = snapshot()
        assertTrue(end.introComplete)
        assertTrue(end.tickTrim.all { it == 1f })
        assertEquals(1f, end.beamTrim, 0f)
        assertEquals(1f, end.wordmarkClip, 0f)
        assertTrue(end.glyphAlpha.all { it == 1f })
    }

    @Test
    fun introIgnoresTapsAndLaterTapsRestartTheRipple() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(
                LocalAtlasAnimationsEnabled provides true,
                LocalSystemAnimatorEnabled provides true,
            ) {
                AtlasAnimatedBrand(height = 54.dp, tint = Color.Black, contentDescription = "Tap Atlas")
            }
        }
        compose.onNodeWithContentDescription("Tap Atlas").performClick()
        assertEquals(0, snapshot().tapGeneration)
        compose.mainClock.advanceTimeBy(1_600L)
        compose.onNodeWithContentDescription("Tap Atlas").performClick()
        compose.mainClock.advanceTimeBy(200L)
        compose.onNodeWithContentDescription("Tap Atlas").performClick()
        assertEquals(2, snapshot().tapGeneration)
    }

    @Test
    fun reducedMotionStartsOnTheFinalFrameAndDoesNotWave() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(
                LocalAtlasAnimationsEnabled provides false,
                LocalSystemAnimatorEnabled provides true,
            ) {
                AtlasAnimatedBrand(height = 54.dp, tint = Color.Black, contentDescription = "Still Atlas")
            }
        }
        val initial = snapshot("Still Atlas")
        assertTrue(initial.introComplete)
        assertTrue(initial.tickTrim.all { it == 1f })
        assertEquals(1f, initial.beamTrim, 0f)
        assertEquals(1f, initial.wordmarkClip, 0f)
        compose.mainClock.advanceTimeBy(5_000L)
        assertTrue(snapshot("Still Atlas").tickAlpha.all { it == 1f })
    }

    @Test
    fun tabletLockupBoundsDoNotChangeWhenThemeChanges() {
        compose.mainClock.autoAdvance = false
        val dark = mutableStateOf(false)
        compose.setContent {
            AtlasTheme(themeMode = if (dark.value) ThemeMode.DARK else ThemeMode.LIGHT) {
                Column {
                    AtlasAnimatedBrand(
                        modifier = Modifier.testTag("phone-lockup"),
                        height = 54.dp,
                        tint = if (dark.value) Color.White else Color(0xFF0F1115),
                    )
                    AtlasAnimatedBrand(
                        modifier = Modifier.testTag("tablet-lockup"),
                        height = 72.dp,
                        tint = if (dark.value) Color.White else Color(0xFF0F1115),
                    )
                }
            }
        }
        val lightPhone = compose.onNodeWithTag("phone-lockup").fetchSemanticsNode().boundsInRoot
        val lightTablet = compose.onNodeWithTag("tablet-lockup").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { dark.value = true }
        compose.waitForIdle()
        val darkPhone = compose.onNodeWithTag("phone-lockup").fetchSemanticsNode().boundsInRoot
        val darkTablet = compose.onNodeWithTag("tablet-lockup").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            val density = compose.density.density
            assertEquals(54f * density, lightPhone.height, 1f)
            assertEquals(72f * density, lightTablet.height, 1f)
            assertEquals(lightPhone, darkPhone)
            assertEquals(lightTablet, darkTablet)
        }
    }

    @Test
    fun railMorphRestoresTheFullLengthBeam() {
        compose.mainClock.autoAdvance = false
        val expanded = mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(
                LocalAtlasAnimationsEnabled provides true,
                LocalSystemAnimatorEnabled provides true,
            ) {
                AtlasAnimatedBrand(
                    modifier = Modifier.testTag("rail-mark"),
                    height = 30.dp,
                    tint = Color.Black,
                    markOnly = true,
                    morphKey = expanded.value,
                )
            }
        }
        compose.mainClock.advanceTimeBy(1_600L)
        compose.runOnIdle { expanded.value = true }
        compose.mainClock.advanceTimeBy(700L)
        assertEquals(1f, snapshot("Atlas Commerce").beamScale, 0.001f)
        assertEquals(125f, AtlasMarkGeometry.beam.end.x - AtlasMarkGeometry.beam.start.x, 0f)
    }

    private fun snapshot(description: String = "Animated Atlas lockup"): AtlasBrandAnimationSnapshot =
        compose.onNodeWithContentDescription(description)
            .fetchSemanticsNode().config[AtlasBrandAnimationSnapshotKey]

    private fun snapshot(): AtlasBrandAnimationSnapshot = snapshot("Animated Atlas lockup")
}
