package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Extra scroll-end space needed only when the floating bottom dock overlays the page. */
val LocalAtlasFloatingDockPadding = staticCompositionLocalOf { 0.dp }

/** Initial scroll-content inset beneath the shell's floating top bar; it scrolls with page content. */
val LocalAtlasFloatingTopBarPadding = staticCompositionLocalOf { 0.dp }

/** Whether floating top bars should show their full controls or the compact centered title. */
val LocalAtlasFloatingTopBarExpanded = staticCompositionLocalOf { true }
