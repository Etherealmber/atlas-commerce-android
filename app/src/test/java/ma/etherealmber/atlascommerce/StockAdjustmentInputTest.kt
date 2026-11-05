package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.ui.screens.products.parseStockAdjustmentQuantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StockAdjustmentInputTest {
    @Test
    fun acceptsPositiveWholeAndLocalizedDecimalQuantities() {
        assertEquals(3.0, parseStockAdjustmentQuantity("3")!!, 0.0)
        assertEquals(1.5, parseStockAdjustmentQuantity("1,5")!!, 0.0001)
        assertEquals(0.25, parseStockAdjustmentQuantity(" 0.25 ")!!, 0.0001)
    }

    @Test
    fun rejectsEmptyZeroNegativeMalformedAndNonFiniteQuantities() {
        listOf("", "0", "0,0", "-2", "1.2.3", "1,2,3", "1e309", "NaN").forEach {
            assertNull("Expected '$it' to be rejected", parseStockAdjustmentQuantity(it))
        }
    }
}
