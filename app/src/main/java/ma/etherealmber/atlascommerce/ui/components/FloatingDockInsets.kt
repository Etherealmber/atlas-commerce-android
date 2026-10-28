package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Extra scroll-end space needed only when the floating bottom dock overlays the page. */
val LocalAtlasFloatingDockPadding = staticCompositionLocalOf { 0.dp }
