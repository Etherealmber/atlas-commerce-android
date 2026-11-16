package ma.etherealmber.atlascommerce.ui.shell

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Adds a no-ripple multi-tap gesture while retaining an optional double-tap and long-press.
 * A double-tap action is deferred briefly so a third tap wins and only fires the triple action.
 */
@Composable
internal fun Modifier.atlasTapSequenceGestures(
    onDoubleTap: (() -> Unit)? = null,
    onTripleTap: () -> Unit,
    onLongPress: (() -> Unit)? = null,
): Modifier {
    val scope = rememberCoroutineScope()
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentTripleTap by rememberUpdatedState(onTripleTap)
    val currentLongPress by rememberUpdatedState(onLongPress)
    var tapCount by remember { mutableIntStateOf(0) }
    var pendingAction by remember { mutableStateOf<Job?>(null) }

    val hasDoubleTapAction = onDoubleTap != null
    return pointerInput(hasDoubleTapAction) {
        fun registerTap(count: Int) {
            if (count == 2 && tapCount == 2) {
                // The previous double-tap timer can still be pending when another,
                // separate double-tap arrives just outside the platform double-tap window.
                pendingAction?.cancel()
                pendingAction = null
                tapCount = 0
                currentDoubleTap?.invoke()
            }
            tapCount += count
            if (tapCount >= 3) {
                pendingAction?.cancel()
                pendingAction = null
                tapCount = 0
                currentTripleTap()
                return
            }
            pendingAction?.cancel()
            pendingAction = scope.launch {
                // detectTapGestures waits up to the platform double-tap timeout before
                // reporting an isolated tap after a possible second/third tap.
                delay(viewConfiguration.doubleTapTimeoutMillis * 2L + 80L)
                val completedCount = tapCount
                tapCount = 0
                if (completedCount == 2) currentDoubleTap?.invoke()
            }
        }

        detectTapGestures(
            onTap = { registerTap(1) },
            onDoubleTap = if (hasDoubleTapAction) ({ registerTap(2) }) else null,
            onLongPress = {
                pendingAction?.cancel()
                pendingAction = null
                tapCount = 0
                currentLongPress?.invoke()
            },
        )
    }
}
