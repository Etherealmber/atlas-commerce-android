package ma.etherealmber.atlascommerce.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtlasThemePaletteTest {
    @Test
    fun originalCharcoalThemeAndHeroGradientsAreRestored() {
        assertEquals(Color.White, AtlasNeutral.lightBase)
        assertEquals(Color(0xFFF5F2EB), Paper)
        assertEquals(Color(0xFF0A0A0C), DarkBg)
        assertEquals(Color(0xFF141414), DarkSurface)
        assertEquals(Color(0xFF1A1A1A), DarkElevated)
        assertNotEquals(DarkBg, DarkSurface)
        assertTrue(DarkOnBg != DarkBg)
        assertEquals(Color(0xFF1F1F23), DarkPalette.heroStart)
        assertEquals(Color(0xFF0E0E11), DarkPalette.heroEnd)
        assertEquals(Color(0xFF171717), LightPalette.heroStart)
        assertEquals(Color(0xFF2B2B2B), LightPalette.heroEnd)
    }

    @Test
    fun mascotUsesFriendlyThemeGradientsAndNeverPureBlackFaceDetails() {
        val dark = atlasAgentMascotColors(dark = true)
        val light = atlasAgentMascotColors(dark = false)

        assertNotEquals(dark.faceGradientStart, dark.faceGradientEnd)
        assertNotEquals(light.faceGradientStart, light.faceGradientEnd)
        assertNotEquals(Color.Black, dark.pupil)
        assertNotEquals(Color.Black, dark.smile)
        assertEquals(Color.White, light.eyeWhite)
    }
}
