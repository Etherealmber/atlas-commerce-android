package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

private data class AtlasSkeletonStyle(
    val base: Color,
    val highlight: Color,
    val phase: State<Float>,
)

private val LocalAtlasSkeletonStyle = compositionLocalOf<AtlasSkeletonStyle?> { null }

/** Provides one coordinated shimmer phase to a screen's loading placeholders. */
@Composable
fun AtlasShimmer(content: @Composable () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val base = if (dark) Color(0xFF424242) else Color(0xFFE0E0E0) // Gray 800 / 300
    val highlight = if (dark) Color(0xFF616161) else Color(0xFFF5F5F5) // Gray 700 / 100
    val phase: State<Float> = if (LocalAtlasAnimationsEnabled.current) {
        val transition = rememberInfiniteTransition(label = "atlas_skeleton_shimmer")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_450, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "atlas_skeleton_phase",
        )
    } else {
        remember { mutableFloatStateOf(0.52f) }
    }
    CompositionLocalProvider(
        LocalAtlasSkeletonStyle provides AtlasSkeletonStyle(base, highlight, phase),
        content = content,
    )
}

@Composable
fun AtlasSkeletonBlock(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(10.dp),
) {
    val provided = LocalAtlasSkeletonStyle.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val base = provided?.base ?: if (dark) Color(0xFF424242) else Color(0xFFE0E0E0)
    val highlight = provided?.highlight ?: if (dark) Color(0xFF616161) else Color(0xFFF5F5F5)
    val phaseState = provided?.phase
    Box(
        modifier = modifier
            .clip(shape)
            .drawWithCache {
                val phase = phaseState?.value ?: 0.52f
                val bandWidth = size.width * 1.35f
                val startX = -bandWidth + (size.width + bandWidth) * phase
                val brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to base,
                        0.36f to base,
                        0.5f to highlight,
                        0.64f to base,
                        1f to base,
                    ),
                    start = Offset(startX, 0f),
                    end = Offset(startX + bandWidth, size.height),
                )
                onDrawBehind { drawRect(brush) }
            },
    )
}

@Composable
fun AtlasSkeletonList(
    modifier: Modifier = Modifier,
    rows: Int = 5,
    avatarSize: androidx.compose.ui.unit.Dp = 44.dp,
) {
    AtlasShimmer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(rows.coerceAtLeast(1)) { index ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AtlasSkeletonBlock(Modifier.size(avatarSize), CircleShape)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index % 2 == 0) 0.56f else 0.7f).height(15.dp))
                            AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index % 3 == 0) 0.9f else 0.72f).height(11.dp))
                        }
                    }
                    AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index % 2 == 0) 0.45f else 0.62f).height(10.dp))
                }
            }
        }
    }
}

@Composable
fun AtlasDashboardSkeleton(modifier: Modifier = Modifier) {
    AtlasShimmer {
        Column(
            modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AtlasSkeletonBlock(Modifier.fillMaxWidth(0.48f).height(24.dp))
            AtlasSkeletonBlock(Modifier.fillMaxWidth(0.78f).height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AtlasSkeletonBlock(Modifier.size(34.dp), CircleShape)
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.64f).height(12.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.82f).height(22.dp))
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AtlasSkeletonBlock(Modifier.fillMaxWidth(0.45f).height(18.dp))
                AtlasSkeletonBlock(Modifier.fillMaxWidth().height(150.dp), RoundedCornerShape(16.dp))
            }
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(2) {
                        AtlasSkeletonBlock(
                            Modifier.weight(1f).height(74.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AtlasCatalogSkeleton(modifier: Modifier = Modifier, rows: Int = 5) {
    AtlasShimmer {
        Column(
            modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            repeat(rows.coerceAtLeast(1)) { index ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AtlasSkeletonBlock(Modifier.size(54.dp), RoundedCornerShape(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index % 2 == 0) 0.7f else 0.52f).height(15.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.86f).height(11.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.38f).height(13.dp))
                    }
                    AtlasSkeletonBlock(Modifier.size(34.dp), CircleShape)
                }
            }
        }
    }
}

@Composable
fun AtlasFinanceSkeleton(modifier: Modifier = Modifier, scrollable: Boolean = false) {
    AtlasShimmer {
        val scrollModifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
        Column(
            modifier.fillMaxWidth().then(scrollModifier).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AtlasSkeletonBlock(Modifier.fillMaxWidth(0.5f).height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(2) {
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.68f).height(12.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.88f).height(22.dp))
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AtlasSkeletonBlock(Modifier.fillMaxWidth(0.58f).height(16.dp))
                AtlasSkeletonBlock(Modifier.fillMaxWidth().height(112.dp), RoundedCornerShape(14.dp))
            }
            repeat(4) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AtlasSkeletonBlock(Modifier.size(36.dp), CircleShape)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.55f).height(13.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.8f).height(10.dp))
                    }
                    AtlasSkeletonBlock(Modifier.size(width = 64.dp, height = 17.dp))
                }
            }
        }
    }
}

@Composable
fun AtlasPayrollSkeleton(modifier: Modifier = Modifier) {
    AtlasShimmer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtlasSkeletonBlock(Modifier.size(44.dp), RoundedCornerShape(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.62f).height(17.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.94f).height(11.dp))
                    }
                    AtlasSkeletonBlock(Modifier.size(28.dp), CircleShape)
                    AtlasSkeletonBlock(Modifier.size(28.dp), CircleShape)
                }
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AtlasSkeletonBlock(Modifier.size(22.dp), CircleShape)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.48f).height(16.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.76f).height(11.dp))
                    }
                    AtlasSkeletonBlock(Modifier.size(22.dp), CircleShape)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { index ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index == 0) 0.72f else 0.58f).height(10.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.42f).height(19.dp))
                    }
                }
            }
            AtlasSkeletonBlock(Modifier.fillMaxWidth().height(46.dp), RoundedCornerShape(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtlasSkeletonBlock(Modifier.size(width = 58.dp, height = 30.dp), RoundedCornerShape(99.dp))
                AtlasSkeletonBlock(Modifier.size(width = 74.dp, height = 30.dp), RoundedCornerShape(99.dp))
                AtlasSkeletonBlock(Modifier.size(width = 66.dp, height = 30.dp), RoundedCornerShape(99.dp))
            }
            PayrollSkeletonRows(rows = 4)
        }
    }
}

@Composable
fun AtlasPayrollRowsSkeleton(modifier: Modifier = Modifier, rows: Int = 4) {
    AtlasShimmer {
        PayrollSkeletonRows(modifier = modifier, rows = rows)
    }
}

@Composable
private fun PayrollSkeletonRows(modifier: Modifier = Modifier, rows: Int) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        repeat(rows.coerceAtLeast(1)) { index ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index % 2 == 0) 0.54f else 0.68f).height(14.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.78f).height(10.dp))
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        AtlasSkeletonBlock(Modifier.size(width = 70.dp, height = 14.dp))
                        AtlasSkeletonBlock(Modifier.size(width = 56.dp, height = 18.dp), RoundedCornerShape(99.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AtlasDeviceListSkeleton(modifier: Modifier = Modifier, rows: Int = 2) =
    AtlasSkeletonList(modifier = modifier, rows = rows, avatarSize = 40.dp)

@Composable
fun AtlasChatSkeleton(modifier: Modifier = Modifier) {
    AtlasShimmer {
        Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) { index ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (index % 2 == 0) Arrangement.Start else Arrangement.End,
                ) {
                    Column(
                        Modifier.fillMaxWidth(if (index % 2 == 0) 0.78f else 0.66f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.48f).height(12.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth().height(if (index == 1) 34.dp else 22.dp))
                    }
                }
            }
        }
    }
}
