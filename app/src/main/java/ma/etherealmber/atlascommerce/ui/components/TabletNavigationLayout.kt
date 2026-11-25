package ma.etherealmber.atlascommerce.ui.components

/** Shared tablet breakpoint and sidebar widths used by the shell and root-level page overlays. */
internal object TabletNavigationLayout {
    const val RAIL_BREAKPOINT_DP = 600
    const val EXPANDED_BREAKPOINT_DP = 840
    const val RAIL_WIDTH_DP = 76
    const val EXPANDED_WIDTH_DP = 264

    fun sidebarWidthDp(windowWidthDp: Int, collapsed: Boolean): Int = when {
        windowWidthDp < RAIL_BREAKPOINT_DP -> 0
        windowWidthDp >= EXPANDED_BREAKPOINT_DP && !collapsed -> EXPANDED_WIDTH_DP
        else -> RAIL_WIDTH_DP
    }

    fun shouldShowShellTopBar(
        isTablet: Boolean,
        isForeground: Boolean,
        tabletDestinationHasTopBar: Boolean,
        profileDetailActive: Boolean,
        nestedPageActive: Boolean,
    ): Boolean = isForeground && !profileDetailActive && !nestedPageActive &&
        (!isTablet || tabletDestinationHasTopBar)

    fun shouldShowFloatingPageTopBar(windowWidthDp: Int): Boolean =
        windowWidthDp < RAIL_BREAKPOINT_DP
}

/** Keep a dismissed prompt hidden until permission state is rechecked on Activity resume. */
internal fun shouldShowNotificationEnablePrompt(notificationsEnabled: Boolean, dismissed: Boolean): Boolean =
    !notificationsEnabled && !dismissed
