package ma.etherealmber.atlascommerce.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabletNavigationLayoutTest {
    @Test
    fun `sidebar width matches the shell breakpoints`() {
        assertEquals(0, TabletNavigationLayout.sidebarWidthDp(windowWidthDp = 599, collapsed = false))
        assertEquals(76, TabletNavigationLayout.sidebarWidthDp(windowWidthDp = 600, collapsed = false))
        assertEquals(76, TabletNavigationLayout.sidebarWidthDp(windowWidthDp = 839, collapsed = false))
        assertEquals(264, TabletNavigationLayout.sidebarWidthDp(windowWidthDp = 840, collapsed = false))
        assertEquals(76, TabletNavigationLayout.sidebarWidthDp(windowWidthDp = 1200, collapsed = true))
    }

    @Test
    fun `floating headers are phone only`() {
        assertTrue(TabletNavigationLayout.shouldShowFloatingShellTopBar(isTablet = false))
        assertFalse(TabletNavigationLayout.shouldShowFloatingShellTopBar(isTablet = true))
        assertTrue(TabletNavigationLayout.shouldShowFloatingPageTopBar(windowWidthDp = 599))
        assertFalse(TabletNavigationLayout.shouldShowFloatingPageTopBar(windowWidthDp = 600))
    }

    @Test
    fun `notification enable prompt is suppressed after dismissal or permission grant`() {
        assertTrue(shouldShowNotificationEnablePrompt(notificationsEnabled = false, dismissed = false))
        assertFalse(shouldShowNotificationEnablePrompt(notificationsEnabled = false, dismissed = true))
        assertFalse(shouldShowNotificationEnablePrompt(notificationsEnabled = true, dismissed = false))
    }
}
