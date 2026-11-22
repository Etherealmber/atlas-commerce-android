package ma.etherealmber.atlascommerce.ui.screens.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductStockPresentationTest {
    @Test
    fun secondaryStockIsDerivedFromTheSinglePrimaryBalance() {
        assertEquals(9.54, convertedSecondaryStock(quantity = 6.0, factor = 1.59)!!, 1e-9)
    }

    @Test
    fun absentOrInvalidFactorDoesNotProduceSecondaryStock() {
        assertNull(convertedSecondaryStock(quantity = 6.0, factor = null))
        assertNull(convertedSecondaryStock(quantity = 6.0, factor = 0.0))
        assertNull(convertedSecondaryStock(quantity = 6.0, factor = Double.NaN))
    }
}
