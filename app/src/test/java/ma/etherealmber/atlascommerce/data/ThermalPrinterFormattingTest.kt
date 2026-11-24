package ma.etherealmber.atlascommerce.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalPrinterFormattingTest {
    @Test
    fun `Bluetooth connect permission becomes runtime permission on Android 12`() {
        assertFalse(ThermalPrinter.requiresBluetoothConnectPermission(30))
        assertTrue(ThermalPrinter.requiresBluetoothConnectPermission(31))
        assertTrue(ThermalPrinter.requiresBluetoothConnectPermission(35))
    }

    @Test
    fun `long names preserve quantity and amount within 58mm width`() {
        assertReceiptRowFits(width = 32)
    }

    @Test
    fun `long names preserve quantity and amount within 80mm width`() {
        assertReceiptRowFits(width = 48)
    }

    @Test
    fun `discount and VAT-free VR totals fit 58mm receipt`() {
        assertDiscountAndVrReceiptFits(width = 32)
    }

    @Test
    fun `discount and VAT-free VR totals fit 80mm receipt`() {
        assertDiscountAndVrReceiptFits(width = 48)
    }

    @Test
    fun `zero server total is printed explicitly at both widths`() {
        listOf(32, 48).forEach { width ->
            val receipt = ThermalPrinter.formatReceiptBody(
                client = "Client",
                lines = listOf(ReceiptLine("Sample", 1.0, 0.0)),
                total = 0.0,
                currency = "MAD",
                columns = width,
                vatFreeVr = true,
            )
            assertTrue(receipt.contains("TOTAL"))
            assertTrue(receipt.contains("0.00 MAD"))
            assertTrue(receipt.lines().all { it.length <= width })
        }
    }

    @Test
    fun `very long client name wraps within 58mm and 80mm receipt widths`() {
        listOf(32, 48).forEach { width ->
            val client = "Clientèle chantier - Société Atlas ".repeat(8)
            val receipt = ThermalPrinter.formatReceiptBody(
                client = client,
                lines = listOf(ReceiptLine("Marbre", 2.0, 10.0)),
                total = 20.0,
                currency = "MAD",
                columns = width,
            )
            assertTrue(receipt.contains("Client: "))
            assertTrue(receipt.contains("Clientèle"))
            assertTrue(receipt.lines().all { it.length <= width })
        }
    }

    @Test
    fun `missing or non-finite server total is refused before formatting`() {
        listOf<Double?>(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { total ->
            val error = runCatching { ThermalPrinter.requireVerifiedTotal(total) }.exceptionOrNull()
            assertTrue("total=$total must be rejected", error is IllegalArgumentException)
        }
        assertEquals(0.0, ThermalPrinter.requireVerifiedTotal(0.0), 0.0)
        assertFalse(ThermalPrinter.requireVerifiedTotal(12.5).isNaN())
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

    private fun assertDiscountAndVrReceiptFits(width: Int) {
        val receipt = ThermalPrinter.formatReceiptBody(
            client = "VR customer",
            lines = listOf(ReceiptLine("Premium marble", 2.0, 50.0)),
            total = 87.5,
            currency = "MAD",
            columns = width,
            discount = 12.5,
            vatFreeVr = true,
        )
        assertTrue(receipt.contains("DISCOUNT"))
        assertTrue(receipt.contains("12.50 MAD"))
        assertTrue(receipt.replace("\n", " ").contains("VAT: 0.00 MAD (VR - VAT exempt)"))
        assertTrue(receipt.contains("TOTAL"))
        assertTrue(receipt.contains("87.50 MAD"))
        assertTrue(receipt.lines().all { it.length <= width })
    }
}
