package ma.etherealmber.atlascommerce.ui.components

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private val AtlasIntroEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
internal val LocalSystemAnimatorEnabled = compositionLocalOf { true }

internal data class AtlasBrandAnimationSnapshot(
    val tickTrim: List<Float>,
    val tickAlpha: List<Float>,
    val beamTrim: Float,
    val beamScale: Float,
    val wordmarkClip: Float,
    val glyphAlpha: List<Float>,
    val introComplete: Boolean,
    val tapGeneration: Int,
)

internal val AtlasBrandAnimationSnapshotKey = SemanticsPropertyKey<AtlasBrandAnimationSnapshot>("AtlasBrandAnimationSnapshot")

/** Follows the Android animator-duration setting live, not just the value at process start. */
@Composable
internal fun rememberSystemAnimatorEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember(context) {
        mutableStateOf(readAnimatorEnabled(context.contentResolver))
    }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = readAnimatorEnabled(context.contentResolver)
            }
        }
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        context.contentResolver.registerContentObserver(uri, false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return enabled
}

private fun readAnimatorEnabled(resolver: android.content.ContentResolver): Boolean =
    runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f }
        .getOrDefault(true)

/**
 * Animated Atlas lockup and standalone mark. Intro and touch motion are deliberately finite; the
 * idle wave only runs after the lockup intro and is omitted for a standalone mark.
 */
@Composable
fun AtlasAnimatedBrand(
    modifier: Modifier = Modifier,
    height: Dp = 30.dp,
    tint: Color,
    markOnly: Boolean = false,
    onClick: () -> Unit = {},
    morphKey: Any? = null,
    animateWhenVisible: Boolean = false,
    contentDescription: String = "Atlas Commerce",
) {
    val motionEnabled = LocalAtlasAnimationsEnabled.current && LocalSystemAnimatorEnabled.current
    val tickTrim = List(AtlasMarkGeometry.TICK_COUNT) {
        remember(motionEnabled) { Animatable(if (motionEnabled) 0f else 1f) }
    }
    val tickAlpha = List(AtlasMarkGeometry.TICK_COUNT) {
        remember(motionEnabled) { Animatable(1f) }
    }
    val beamTrim = remember(motionEnabled) { Animatable(if (motionEnabled) 0f else 1f) }
    val beamScale = remember { Animatable(1f) }
    val wordmarkClip = remember(motionEnabled) { Animatable(if (motionEnabled) 0f else 1f) }
    val glyphAlpha = List(AtlasWordmarkPaths.glyphs.size) {
        remember(motionEnabled) { Animatable(if (motionEnabled) 0f else 1f) }
    }
    val glyphShift = List(AtlasWordmarkPaths.glyphs.size) {
        remember(motionEnabled) { Animatable(if (motionEnabled) 0.35f else 0f) }
    }
    var introComplete by remember(motionEnabled) { mutableStateOf(!motionEnabled) }
    var tapGeneration by remember { mutableStateOf(0) }
    var tapActive by remember { mutableStateOf(false) }
    var firstMorph by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val viewportHeightPx = with(density) { screenHeightDp.dp.toPx() }
    var visibleEnough by remember(animateWhenVisible) { mutableStateOf(!animateWhenVisible) }
    val visibilityModifier = if (animateWhenVisible) {
        Modifier.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            val visibleHeight = (min(bounds.bottom, viewportHeightPx) - max(bounds.top, 0f)).coerceAtLeast(0f)
            if (bounds.height > 0f && visibleHeight / bounds.height >= 0.35f) visibleEnough = true
        }
    } else Modifier

    LaunchedEffect(motionEnabled, visibleEnough) {
        if (!visibleEnough) return@LaunchedEffect
        if (!motionEnabled) {
            tickTrim.forEach { it.snapTo(1f) }
            tickAlpha.forEach { it.snapTo(1f) }
            beamTrim.snapTo(1f)
            wordmarkClip.snapTo(1f)
            glyphAlpha.forEach { it.snapTo(1f) }
            glyphShift.forEach { it.snapTo(0f) }
            introComplete = true
            return@LaunchedEffect
        }
        introComplete = false
        coroutineScope {
            tickTrim.forEachIndexed { index, value ->
                launch {
                    delay(index * 32L)
                    value.animateTo(1f, tween(420, easing = AtlasIntroEasing))
                }
            }
            launch {
                delay(420)
                beamTrim.animateTo(1f, tween(520, easing = AtlasIntroEasing))
            }
            if (!markOnly) {
                launch {
                    delay(700)
                    wordmarkClip.animateTo(1f, tween(600, easing = AtlasIntroEasing))
                }
                glyphAlpha.forEachIndexed { index, value ->
                    launch {
                        delay(720L + index * 48L)
                        coroutineScope {
                            launch { value.animateTo(1f, tween(500, easing = AtlasIntroEasing)) }
                            launch { glyphShift[index].animateTo(0f, tween(500, easing = AtlasIntroEasing)) }
                        }
                    }
                }
            }
        }
        introComplete = true
    }

    LaunchedEffect(introComplete, tapActive, motionEnabled, markOnly) {
        if (!introComplete || tapActive || !motionEnabled || markOnly) return@LaunchedEffect
        coroutineScope {
            tickAlpha.forEachIndexed { index, alpha ->
                launch {
                    delay(index * 160L)
                    while (true) {
                        alpha.animateTo(0.32f, tween(1_200, easing = EaseInOut))
                        alpha.animateTo(1f, tween(1_200, easing = EaseInOut))
                    }
                }
            }
        }
    }

    LaunchedEffect(tapGeneration) {
        if (tapGeneration == 0 || !introComplete || !motionEnabled) return@LaunchedEffect
        tapActive = true
        coroutineScope {
            tickAlpha.forEachIndexed { index, alpha ->
                launch {
                    delay(index * 26L)
                    alpha.animateTo(0.18f, tween(248, easing = EaseInOut))
                    alpha.animateTo(1f, tween(372, easing = EaseInOut))
                }
            }
        }
        tapActive = false
    }

    LaunchedEffect(morphKey) {
        if (firstMorph) {
            firstMorph = false
            return@LaunchedEffect
        }
        if (morphKey == null || !introComplete || !motionEnabled) return@LaunchedEffect
        coroutineScope {
            launch {
                beamScale.snapTo(1f)
                beamScale.animateTo(0.08f, tween(235, easing = AtlasIntroEasing))
                beamScale.animateTo(1f, tween(325, easing = AtlasIntroEasing))
            }
            tickAlpha.forEachIndexed { index, alpha ->
                launch {
                    delay(120L + index * 22L)
                    alpha.animateTo(0.18f, tween(224, easing = EaseInOut))
                    alpha.animateTo(1f, tween(336, easing = EaseInOut))
                }
            }
        }
    }

    val tapSource = remember { MutableInteractionSource() }
    val glyphPaths = if (markOnly) emptyList() else rememberWordmarkPaths()
    val canvasModifier = if (markOnly) {
        modifier.size(height)
    } else {
        modifier.height(height).aspectRatio(AtlasWordmarkPaths.VIEWPORT_WIDTH / AtlasWordmarkPaths.VIEWPORT_HEIGHT)
    }
    Canvas(
        modifier = canvasModifier
            .then(visibilityModifier)
            .clipToBounds()
            .semantics {
                this.contentDescription = contentDescription
                this[AtlasBrandAnimationSnapshotKey] = AtlasBrandAnimationSnapshot(
                    tickTrim = tickTrim.map { it.value },
                    tickAlpha = tickAlpha.map { it.value },
                    beamTrim = beamTrim.value,
                    beamScale = beamScale.value,
                    wordmarkClip = wordmarkClip.value,
                    glyphAlpha = glyphAlpha.map { it.value },
                    introComplete = introComplete,
                    tapGeneration = tapGeneration,
                )
            }
            .clickable(
                interactionSource = tapSource,
                indication = null,
                onClick = {
                    if (introComplete) {
                        if (motionEnabled) tapGeneration++
                        onClick()
                    }
                },
            ),
    ) {
        val factor = size.height / AtlasWordmarkPaths.VIEWPORT_HEIGHT
        val markScale = if (markOnly) size.minDimension / AtlasMarkGeometry.VIEWPORT_SIZE else factor
        val strokeWidth = (if (markOnly) 10f else 12f) * markScale
        val baseX = if (markOnly) (size.width - AtlasMarkGeometry.VIEWPORT_SIZE * markScale) / 2f else 0f
        val baseY = if (markOnly) (size.height - AtlasMarkGeometry.VIEWPORT_SIZE * markScale) / 2f else 0f
        AtlasMarkGeometry.ticks.forEachIndexed { index, tick ->
            val progress = tickTrim[index].value.coerceIn(0f, 1f)
            val dx = tick.end.x - tick.start.x
            val dy = tick.end.y - tick.start.y
            drawLine(
                color = tint.copy(alpha = tickAlpha[index].value.coerceIn(0f, 1f)),
                start = Offset(baseX + tick.start.x * markScale, baseY + tick.start.y * markScale),
                end = Offset(baseX + (tick.start.x + dx * progress) * markScale, baseY + (tick.start.y + dy * progress) * markScale),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Butt,
            )
        }
        val beam = AtlasMarkGeometry.beam
        val beamEndX = beam.start.x + (beam.end.x - beam.start.x) * beamTrim.value.coerceIn(0f, 1f)
        withTransform({ scale(beamScale.value, 1f, pivot = Offset(baseX + beam.start.x * markScale, baseY + beam.start.y * markScale)) }) {
            drawLine(
                color = tint,
                start = Offset(baseX + beam.start.x * markScale, baseY + beam.start.y * markScale),
                end = Offset(baseX + beamEndX * markScale, baseY + beam.end.y * markScale),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Butt,
            )
        }
        if (!markOnly) {
            val wordmarkStart = 305f * factor
            clipRect(
                left = wordmarkStart,
                right = wordmarkStart + (AtlasWordmarkPaths.VIEWPORT_WIDTH - 305f) * wordmarkClip.value.coerceIn(0f, 1f),
            ) {
                glyphPaths.forEachIndexed { index, path ->
                    val shift = glyphShift[index].value
                    val glyphWidths = floatArrayOf(135f, 75f, 24f, 108f, 87f)
                    withTransform({
                        translate(left = -glyphWidths[index] * shift * factor, top = 0f)
                        scale(factor, factor, pivot = Offset.Zero)
                    }) {
                        drawPath(path, tint.copy(alpha = glyphAlpha[index].value.coerceIn(0f, 1f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberWordmarkPaths(): List<Path> = remember {
    AtlasWordmarkPaths.glyphs.map(::parseBrandPath)
}

private fun parseBrandPath(data: String): Path {
    val path = Path()
    val tokens = Regex("[MLZ]|-?\\d+(?:\\.\\d+)?").findAll(data).map { it.value }.toList()
    var command = ' '
    var index = 0
    while (index < tokens.size) {
        val token = tokens[index]
        if (token.length == 1 && token[0].isLetter()) {
            command = token[0]
            index++
            when (command) {
                'Z' -> path.close()
                'M', 'L' -> {
                    val x = tokens[index++].toFloat()
                    val y = tokens[index++].toFloat()
                    if (command == 'M') path.moveTo(x, y) else path.lineTo(x, y)
                    if (command == 'M') command = 'L'
                }
            }
        } else {
            val x = token.toFloat()
            val y = tokens[index + 1].toFloat()
            path.lineTo(x, y)
            index += 2
        }
    }
    return path
}
