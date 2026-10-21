package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.core.format.AtlasFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtlasFormatTest {

    @Test
    fun `money always carries the currency code`() {
        val formatted = AtlasFormat.money(1250.5, "MAD")
        assertTrue(formatted.endsWith(" MAD"))
        assertTrue(formatted.contains("1"))
        assertTrue(formatted.contains("250"))
    }

    @Test
    fun `missing currency falls back to MAD`() {
        assertTrue(AtlasFormat.money(10.0, null).endsWith(" MAD"))
        assertTrue(AtlasFormat.money(10.0, "  ").endsWith(" MAD"))
    }

    @Test
    fun `whole quantities have no decimals`() {
        assertEquals("2", AtlasFormat.quantity(2.0))
        assertEquals("0", AtlasFormat.quantity(0.0))
    }

    @Test
    fun `fractional quantities keep two digits`() {
        assertEquals("1,5", AtlasFormat.quantity(1.5))
    }

    @Test
    fun `non finite amounts are displayed as zero`() {
        assertTrue(AtlasFormat.money(Double.NaN, "MAD").startsWith("0"))
    }

    @Test
    fun `dates accept ISO and plain dates`() {
        assertTrue(AtlasFormat.date("2026-10-21T10:30:00Z").contains("2026"))
        assertTrue(AtlasFormat.date("2026-10-21").contains("2026"))
        assertEquals("—", AtlasFormat.date(null))
    }
}
