package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Extra scroll-end space needed only when the floating bottom dock overlays the page. */
val LocalAtlasFloatingDockPadding = staticCompositionLocalOf { 0.dp }

/** Initial scroll-content inset beneath the shell's floating top bar; it scrolls with page content. */
val LocalAtlasFloatingTopBarPadding = staticCompositionLocalOf { 0.dp }

/** Whether floating top bars should show their full controls or the compact centered title. */
val LocalAtlasFloatingTopBarExpanded = staticCompositionLocalOf { true }

/** Collapse a page header on upward scrolling and restore it on downward scrolling. */
@Composable
fun rememberAtlasFloatingTopBarScrollConnection(
    expanded: Boolean,
    threshold: Dp = 12.dp,
    onExpandedChange: (Boolean) -> Unit,
): NestedScrollConnection {
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    val scrollDelta = remember { mutableFloatStateOf(0f) }
    val currentExpanded = rememberUpdatedState(expanded)
    val currentOnExpandedChange = rememberUpdatedState(onExpandedChange)

    return remember(thresholdPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val verticalDelta = if (consumed.y != 0f) consumed.y else available.y
                if (verticalDelta == 0f) return Offset.Zero
                scrollDelta.floatValue += verticalDelta
                when {
                    scrollDelta.floatValue <= -thresholdPx -> {
                        if (currentExpanded.value) currentOnExpandedChange.value(false)
                        scrollDelta.floatValue = 0f
                    }
                    scrollDelta.floatValue >= thresholdPx -> {
                        if (!currentExpanded.value) currentOnExpandedChange.value(true)
                        scrollDelta.floatValue = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }
}
