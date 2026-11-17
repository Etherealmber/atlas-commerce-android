package ma.etherealmber.atlascommerce.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtlasThemePaletteTest {
    @Test
    fun earlierAtlasPaletteAndThemeElementGradientsAreRestored() {
        assertEquals(Color.White, AtlasNeutral.lightBase)
        assertEquals(Color(0xFFF2F2F2), Paper)
        assertEquals(Color(0xFF06141B), DarkBg)
        assertEquals(Color(0xFF11212D), DarkSurface)
        assertEquals(Color(0xFF0B1B25), DarkSurfaceLow)
        assertNotEquals(DarkBg, DarkSurface)
        assertNotEquals(DarkSurface, DarkSurfaceVariant)
        assertTrue(DarkOnBg != DarkBg)
        assertEquals(DarkSurface, DarkPalette.heroStart)
        assertEquals(DarkBg, DarkPalette.heroEnd)
        assertEquals(Color(0xFFE9E9E9), LightPalette.heroStart)
        assertEquals(Color(0xFFD2D2D2), LightPalette.heroEnd)
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
