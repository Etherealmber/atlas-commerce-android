package ma.etherealmber.atlascommerce.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Global toggle allowing users to enable/reduce UI motion in Réglages. */
val LocalAtlasAnimationsEnabled = compositionLocalOf { true }

/**
 * Staggered smooth entrance for landing screens, cards, and sections.
 * When reduced motion is chosen, content renders immediately with zero delay.
 */
@Composable
fun AtlasEntrance(
    delayMillis: Int = 0,
    offsetY: Dp = 14.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val enabled = LocalAtlasAnimationsEnabled.current
    if (!enabled) {
        Box(modifier = modifier) { content() }
        return
    }

    var visible by remember { mutableStateOf(false) }
    val pxOffset = with(LocalDensity.current) { offsetY.roundToPx() }

    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(durationMillis = 240, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f),
                initialOffsetY = { pxOffset },
            ),
        exit = fadeOut(tween(150)),
    ) {
        content()
    }
}

/**
 * Directional page transition across bottom tabs and top-level screens/overlays.
 */
@Composable
fun <S> AtlasPageTransition(
    targetState: S,
    indexSelector: (S) -> Int = { 0 },
    modifier: Modifier = Modifier,
    label: String = "atlas_page",
    content: @Composable (S) -> Unit,
) {
    val enabled = LocalAtlasAnimationsEnabled.current
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        label = label,
        transitionSpec = {
            if (!enabled) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val initialOrder = indexSelector(initialState)
                val targetOrder = indexSelector(targetState)
                when {
                    // Overlay / Settings pushed vertically
                    targetOrder >= 100 && initialOrder < 100 -> {
                        (
                            fadeIn(tween(230, easing = FastOutSlowInEasing)) +
                                slideInVertically(tween(250, easing = FastOutSlowInEasing)) { it / 12 } +
                                scaleIn(tween(240, easing = FastOutSlowInEasing), initialScale = 0.97f)
                            ) togetherWith (
                            fadeOut(tween(180)) +
                                scaleOut(tween(200), targetScale = 0.98f)
                            )
                    }

                    // Overlay popped back down to main content
                    initialOrder >= 100 && targetOrder < 100 -> {
                        (
                            fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                                scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.98f)
                            ) togetherWith (
                            fadeOut(tween(190)) +
                                slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it / 12 }
                            )
                    }

                    // Lateral tab switch right
                    targetOrder > initialOrder -> horizontalSlideTransform(forward = true)

                    // Lateral tab switch left
                    targetOrder < initialOrder -> horizontalSlideTransform(forward = false)

                    else -> fadeIn(tween(200)) togetherWith fadeOut(tween(160))
                }
            }
        },
    ) { state ->
        content(state)
    }
}

private fun horizontalSlideTransform(forward: Boolean): ContentTransform {
    val direction = if (forward) 1 else -1
    return (
        fadeIn(tween(220, easing = FastOutSlowInEasing)) +
            slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { fullWidth ->
                direction * (fullWidth / 10)
            }
        ) togetherWith (
        fadeOut(tween(170)) +
            slideOutHorizontally(tween(210, easing = FastOutSlowInEasing)) { fullWidth ->
                -direction * (fullWidth / 12)
            }
        )
}

/**
 * Ambient breathing ring behind splash emblem & tap-to-scan camera launch hero.
 */
@Composable
fun AtlasPulseHalo(
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 112.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val enabled = LocalAtlasAnimationsEnabled.current
    if (!enabled) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) { content() }
        return
    }

    val infinite = rememberInfiniteTransition(label = "pulse_halo")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse_progress",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val baseRadius = this.size.minDimension * 0.34f
            val outerRadius = baseRadius + (this.size.minDimension * 0.16f * progress)
            drawCircle(
                color = color.copy(alpha = (1f - progress) * 0.32f),
                radius = outerRadius,
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(
                color = color.copy(alpha = 0.10f),
                radius = baseRadius * 1.05f,
            )
        }
        content()
    }
}

/**
 * Animated 3-dot wave shown while Atlas Agent IA is composing a reply.
 */
@Composable
fun AtlasTypingDots(
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    val enabled = LocalAtlasAnimationsEnabled.current
    val infinite = rememberInfiniteTransition(label = "typing_dots")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "typing_phase",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val distance = kotlin.math.abs(phase - index).let { d -> minOf(d, 3f - d) }
            val scale = if (enabled) (1.28f - (distance * 0.34f)).coerceIn(0.75f, 1.28f) else 1f
            val alpha = if (enabled) (1f - (distance * 0.42f)).coerceIn(0.35f, 1f) else 0.8f
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .background(color, CircleShape),
            )
        }
    }
}

/** Optional haptic + short acoustic feedback on barcode/QR scan confirmation. */
object ScanFeedback {
    fun trigger(view: View?, haptic: Boolean, sound: Boolean) {
        if (haptic) {
            runCatching {
                view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
        }
        if (sound) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55)
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 90)
            }
        }
    }
}
