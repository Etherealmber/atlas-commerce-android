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
        assertEquals("Bot", AtlasIcons.Outlined.Bot.name)
        assertEquals("Grid2x2", AtlasIcons.Outlined.GridView.name)
        assertEquals("Grid2x2", AtlasIcons.Outlined.AutoAwesomeMotion.name)
        assertEquals("CircleDollarSign", AtlasIcons.Outlined.CircleDollarSign.name)
        assertEquals("HandCoins", AtlasIcons.Outlined.HandCoins.name)
        assertEquals("CreditCard", AtlasIcons.Outlined.CreditCard.name)
        assertEquals("Check", AtlasIcons.Outlined.Check.name)
        assertEquals("Plus", AtlasIcons.Outlined.Add.name)
        assertEquals("Download", AtlasIcons.Outlined.FileDownload.name)
        assertEquals("Receipt", AtlasIcons.Outlined.PointOfSale.name)
        assertEquals("Receipt", AtlasIcons.Outlined.ReceiptLong.name)
        assertEquals("Bell", AtlasIcons.Outlined.NotificationsActive.name)
        assertEquals("PenLine", AtlasIcons.Outlined.Edit.name)
        assertEquals("Globe", AtlasIcons.Outlined.Language.name)
        assertEquals("ArrowDownLeft", AtlasIcons.Outlined.ArrowDownward.name)
        assertEquals("ArrowUpRight", AtlasIcons.Outlined.ArrowUpward.name)
        assertEquals("ArrowLeft", AtlasIcons.AutoMirrored.Outlined.ArrowBack.name)
        assertEquals("ArrowRight", AtlasIcons.AutoMirrored.Outlined.ArrowForward.name)
        assertEquals("Send", AtlasIcons.AutoMirrored.Outlined.Send.name)
    }
}
