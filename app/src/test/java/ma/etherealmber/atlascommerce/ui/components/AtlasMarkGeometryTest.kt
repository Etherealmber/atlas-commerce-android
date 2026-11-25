package ma.etherealmber.atlascommerce.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtlasMarkGeometryTest {
    @Test
    fun markHasFifteenRaysAndOneHorizontalBeam() {
        assertEquals(15, AtlasMarkGeometry.ticks.size)
        assertEquals(126f, AtlasMarkGeometry.beam.start.y, 0.001f)
        assertEquals(126f, AtlasMarkGeometry.beam.end.y, 0.001f)
        assertEquals(126f, AtlasMarkGeometry.beam.start.x, 0.001f)
        assertEquals(251f, AtlasMarkGeometry.beam.end.x, 0.001f)
    }

    @Test
    fun opticalStrokeUsesSmallSizeCompensation() {
        assertEquals(14f, AtlasMarkGeometry.opticalStrokeUnits(20f), 0f)
        assertEquals(13f, AtlasMarkGeometry.opticalStrokeUnits(32f), 0f)
        assertEquals(9f, AtlasMarkGeometry.opticalStrokeUnits(52f), 0f)
    }

    @Test
    fun wordmarkContainsFiveOutlinedLetterPaths() {
        assertEquals(5, AtlasWordmarkPaths.glyphs.size)
        assertTrue(AtlasWordmarkPaths.glyphs.all { it.startsWith("M") && it.contains("Z") })
    }
}
