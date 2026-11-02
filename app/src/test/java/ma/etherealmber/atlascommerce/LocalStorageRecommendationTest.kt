package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.ui.root.roundUpTo50MiB
import ma.etherealmber.atlascommerce.ui.shell.MainTab
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalStorageRecommendationTest {
    private val mib = 1024L * 1024L

    @Test
    fun storageRecommendationRoundsUpToNextFiftyMib() {
        assertEquals(100L * mib, roundUpTo50MiB(69L * mib))
        assertEquals(150L * mib, roundUpTo50MiB(140L * mib))
        assertEquals(200L * mib, roundUpTo50MiB(169L * mib))
        assertEquals(150L * mib, roundUpTo50MiB(150L * mib))
    }

    @Test
    fun launchTabSelectsTheScreenWithoutReorderingTheDock() {
        val adminDock = listOf(MainTab.HOME, MainTab.PRODUCTS, MainTab.SCAN, MainTab.BALANCE, MainTab.EMPLOYEES)
        assertEquals(adminDock, MainTab.bottomTabsForRole("admin"))
        assertEquals(MainTab.SCAN, MainTab.initialTabForRole("admin", MainTab.SCAN.name))
        assertEquals(MainTab.BALANCE, MainTab.initialTabForRole("admin", MainTab.BALANCE.name))
        assertEquals(MainTab.EMPLOYEES, MainTab.initialTabForRole("admin", MainTab.EMPLOYEES.name))

        val stockDock = listOf(MainTab.HOME, MainTab.SCAN, MainTab.PRODUCTS)
        assertEquals(stockDock, MainTab.bottomTabsForRole("stock"))
        assertEquals(MainTab.HOME, MainTab.initialTabForRole("stock", MainTab.BALANCE.name))
    }
}
