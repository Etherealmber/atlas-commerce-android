package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.ui.root.roundUpTo50MiB
import ma.etherealmber.atlascommerce.ui.shell.MainTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun launchTabMovesToFrontOnlyWhenAllowedForRole() {
        val adminTabs = MainTab.orderedBottomTabsForRole("admin", MainTab.EMPLOYEES.name)
        assertEquals(MainTab.EMPLOYEES, adminTabs.first())
        assertTrue(MainTab.bottomTabsForRole("admin").containsAll(adminTabs))

        val stockTabs = MainTab.orderedBottomTabsForRole("stock", MainTab.BALANCE.name)
        assertEquals(MainTab.HOME, stockTabs.first())
        assertEquals(setOf(MainTab.HOME, MainTab.SCAN, MainTab.PRODUCTS), stockTabs.toSet())
    }
}
