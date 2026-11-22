package ma.etherealmber.atlascommerce.ui.icons

import org.junit.Assert.assertEquals
import org.junit.Test

class AtlasIconsParityTest {
    @Test
    fun appNavigationAndCommerceSymbolsUseLucideWebMobileNames() {
        assertEquals("House", AtlasIcons.Outlined.Home.name)
        assertEquals("Package", AtlasIcons.Outlined.Inventory2.name)
        assertEquals("ScanLine", AtlasIcons.Outlined.QrCodeScanner.name)
        assertEquals("Wallet", AtlasIcons.Outlined.AccountBalanceWallet.name)
        assertEquals("Users", AtlasIcons.Outlined.Groups.name)
        assertEquals("ShoppingCart", AtlasIcons.Outlined.ShoppingCart.name)
        assertEquals("MonitorSmartphone", AtlasIcons.Outlined.Devices.name)
        assertEquals("ArrowLeft", AtlasIcons.AutoMirrored.Outlined.ArrowBack.name)
        assertEquals("ArrowRight", AtlasIcons.AutoMirrored.Outlined.ArrowForward.name)
    }
}
