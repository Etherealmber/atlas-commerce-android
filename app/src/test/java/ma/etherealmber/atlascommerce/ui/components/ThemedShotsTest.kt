package ma.etherealmber.atlascommerce.ui.components

import ma.etherealmber.atlascommerce.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemedShotsTest {
    @Test
    fun darkProductsCaptureUsesDarkResource() {
        assertEquals(R.drawable.phone_products_demo_dark, themedShot("phone-products-demo", dark = true))
    }

    @Test
    fun capturesWithoutDarkVariantFallBackToLight() {
        assertEquals(R.drawable.android_scan_demo, themedShot("android-scan-demo", dark = true))
    }

    @Test
    fun changingThemeSelectsTheOtherCapture() {
        assertEquals(R.drawable.phone_home_demo, themedShot("phone-home-demo", dark = false))
        assertEquals(R.drawable.phone_home_demo_dark, themedShot("phone-home-demo", dark = true))
    }
}
