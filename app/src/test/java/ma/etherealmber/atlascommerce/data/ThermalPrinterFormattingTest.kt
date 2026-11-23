package ma.etherealmber.atlascommerce.data

import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalPrinterFormattingTest {
    @Test
    fun `long names preserve quantity and amount within 58mm width`() {
        assertReceiptRowFits(width = 32)
    }

    @Test
    fun `long names preserve quantity and amount within 80mm width`() {
        assertReceiptRowFits(width = 48)
    }

    private fun assertReceiptRowFits(width: Int) {
        val row = ThermalPrinter.formatItemRow(
            name = "Very long marble product name ".repeat(4),
            quantity = 123.456,
            amountValue = 1_234_567.89,
            currency = "MAD",
            columns = width,
        )

        assertTrue(row.lines().all { it.length <= width })
        assertTrue(row.contains("x123.456"))
        assertTrue(row.contains("1234567.89 MAD"))
    }
}
