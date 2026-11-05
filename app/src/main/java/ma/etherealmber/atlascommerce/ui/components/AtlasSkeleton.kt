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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

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
fun AtlasDashboardSkeleton(
    modifier: Modifier = Modifier,
    isAdmin: Boolean = false,
    showFinancialHero: Boolean = false,
    offline: Boolean = false,
) {
    AtlasShimmer {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // These are the stable Home sections. Data-dependent chart, alert and invoice
            // placeholders are intentionally omitted until the server tells us they exist.
            item(key = "home_greeting_skeleton") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.58f).height(28.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.78f).height(13.dp))
                    }
                    AtlasSkeletonBlock(Modifier.size(48.dp), CircleShape)
                }
            }
            if (offline) item(key = "home_offline_banner") { AtlasOfflineBanner() }
            item(key = "home_hero_skeleton") {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    AtlasSkeletonBlock(Modifier.fillMaxWidth(0.38f).height(10.dp))
                    AtlasSkeletonBlock(Modifier.fillMaxWidth(0.62f).height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AtlasSkeletonBlock(Modifier.weight(if (showFinancialHero) 1f else 1.2f).height(25.dp), RoundedCornerShape(99.dp))
                        AtlasSkeletonBlock(Modifier.weight(1f).height(25.dp), RoundedCornerShape(99.dp))
                    }
                }
            }
            item(key = "home_primary_actions_skeleton") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtlasSkeletonBlock(Modifier.weight(1f).height(48.dp), RoundedCornerShape(14.dp))
                    AtlasSkeletonBlock(Modifier.weight(1f).height(48.dp), RoundedCornerShape(14.dp))
                }
            }
            item(key = "home_notifications_skeleton") {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AtlasSkeletonBlock(Modifier.size(22.dp), CircleShape)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.55f).height(13.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.92f).height(10.dp))
                    }
                    AtlasSkeletonBlock(Modifier.size(width = 58.dp, height = 24.dp), RoundedCornerShape(99.dp))
                }
            }
            item(key = "home_primary_stats_skeleton") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(2) { index ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surface).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            AtlasSkeletonBlock(Modifier.size(25.dp), CircleShape)
                            AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index == 0) 0.5f else 0.68f).height(10.dp))
                            AtlasSkeletonBlock(Modifier.fillMaxWidth(0.58f).height(19.dp))
                        }
                    }
                }
            }
            if (showFinancialHero) {
                item(key = "home_financial_stats_skeleton") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(2) { index ->
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surface).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                AtlasSkeletonBlock(Modifier.size(25.dp), CircleShape)
                                AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index == 0) 0.64f else 0.76f).height(10.dp))
                                AtlasSkeletonBlock(Modifier.fillMaxWidth(0.7f).height(19.dp))
                            }
                        }
                    }
                }
            }
            if (isAdmin) {
                item(key = "home_finance_card_skeleton") {
                    AtlasFinanceSkeleton(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun AtlasCatalogSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 5,
    showRoleBanner: Boolean = false,
    showCartButton: Boolean = true,
    bottomPadding: Dp = 24.dp,
) {
    AtlasShimmer {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = bottomPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showRoleBanner) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "catalog_role_banner_skeleton") {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.56f).height(14.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.76f).height(11.dp))
                    }
                }
            }
            items((1..rows.coerceAtLeast(1)).toList(), key = { "product_card_skeleton_$it" }) { index ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AtlasSkeletonBlock(Modifier.size(64.dp), RoundedCornerShape(14.dp))
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index % 2 == 0) 0.76f else 0.58f).height(15.dp))
                            AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index % 3 == 0) 0.68f else 0.45f).height(11.dp))
                        }
                        if (showCartButton) AtlasSkeletonBlock(Modifier.size(36.dp), CircleShape)
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AtlasSkeletonBlock(Modifier.weight(1f).height(28.dp), RoundedCornerShape(99.dp))
                        AtlasSkeletonBlock(Modifier.weight(0.8f).height(28.dp), RoundedCornerShape(99.dp))
                        AtlasSkeletonBlock(Modifier.weight(0.9f).height(28.dp), RoundedCornerShape(99.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AtlasProductRequestsSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 6,
    showActions: Boolean = false,
    bottomPadding: Dp = 24.dp,
) {
    AtlasShimmer {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = bottomPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items((1..rows.coerceAtLeast(1)).toList(), key = { "product_request_skeleton_$it" }) { index ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AtlasSkeletonBlock(Modifier.size(56.dp), RoundedCornerShape(14.dp))
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index % 2 == 0) 0.72f else 0.54f).height(14.dp))
                            AtlasSkeletonBlock(Modifier.fillMaxWidth(0.82f).height(10.dp))
                            if (index % 3 == 0) AtlasSkeletonBlock(Modifier.fillMaxWidth(0.48f).height(10.dp))
                        }
                        AtlasSkeletonBlock(Modifier.size(width = 76.dp, height = 24.dp), RoundedCornerShape(99.dp))
                    }
                    if (showActions) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AtlasSkeletonBlock(Modifier.size(width = 104.dp, height = 32.dp), RoundedCornerShape(99.dp))
                            AtlasSkeletonBlock(Modifier.size(width = 76.dp, height = 32.dp), RoundedCornerShape(99.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AtlasStockRequestsSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 4,
    showActions: Boolean = false,
    bottomPadding: Dp = 24.dp,
) {
    AtlasShimmer {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = bottomPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items((1..rows.coerceAtLeast(1)).toList(), key = { "stock_request_skeleton_$it" }) { index ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index % 2 == 0) 0.7f else 0.5f).height(14.dp))
                            AtlasSkeletonBlock(Modifier.fillMaxWidth(0.38f).height(10.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        AtlasSkeletonBlock(Modifier.size(width = 76.dp, height = 24.dp), RoundedCornerShape(99.dp))
                    }
                    if (index % 2 == 0) AtlasSkeletonBlock(Modifier.fillMaxWidth(0.76f).height(10.dp))
                    AtlasSkeletonBlock(Modifier.fillMaxWidth(0.52f).height(10.dp))
                    if (showActions) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AtlasSkeletonBlock(Modifier.size(width = 96.dp, height = 32.dp), RoundedCornerShape(99.dp))
                            AtlasSkeletonBlock(Modifier.size(width = 72.dp, height = 32.dp), RoundedCornerShape(99.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AtlasFinanceSkeleton(modifier: Modifier = Modifier) {
    AtlasShimmer {
        AtlasCard(modifier = modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    AtlasSkeletonBlock(Modifier.fillMaxWidth(0.54f).height(15.dp))
                    AtlasSkeletonBlock(Modifier.fillMaxWidth(0.72f).height(10.dp))
                }
                AtlasSkeletonBlock(Modifier.size(22.dp), CircleShape)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    AtlasSkeletonBlock(Modifier.fillMaxWidth(0.38f).height(11.dp))
                    AtlasSkeletonBlock(Modifier.fillMaxWidth(0.86f).height(10.dp))
                }
                AtlasSkeletonBlock(Modifier.size(width = 100.dp, height = 36.dp), RoundedCornerShape(10.dp))
            }
            Spacer(Modifier.height(10.dp))
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)))
            Spacer(Modifier.height(10.dp))
            AtlasSkeletonBlock(Modifier.fillMaxWidth(0.36f).height(15.dp))
            AtlasSkeletonBlock(Modifier.fillMaxWidth(0.92f).height(10.dp))
            AtlasSkeletonBlock(Modifier.fillMaxWidth().height(42.dp), RoundedCornerShape(12.dp))
            AtlasSkeletonBlock(Modifier.fillMaxWidth().height(42.dp), RoundedCornerShape(12.dp))
            AtlasSkeletonBlock(Modifier.fillMaxWidth(0.94f).height(10.dp))
        }
    }
}

@Composable
fun AtlasPayrollSkeleton(modifier: Modifier = Modifier) {
    AtlasShimmer {
        Column(
            modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Mirrors the read-only payroll header and month navigation card.
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtlasSkeletonBlock(Modifier.size(44.dp), RoundedCornerShape(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.62f).height(16.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.9f).height(10.dp))
                    }
                    AtlasSkeletonBlock(Modifier.size(38.dp), CircleShape)
                    AtlasSkeletonBlock(Modifier.size(38.dp), CircleShape)
                }
                AtlasSkeletonBlock(Modifier.fillMaxWidth(0.88f).height(10.dp))
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AtlasSkeletonBlock(Modifier.size(36.dp), CircleShape)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.48f).height(15.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(0.76f).height(10.dp))
                    }
                    AtlasSkeletonBlock(Modifier.size(36.dp), CircleShape)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index == 0) 0.82f else 0.7f).height(9.dp))
                        AtlasSkeletonBlock(Modifier.fillMaxWidth(if (index == 2) 0.72f else 0.46f).height(17.dp))
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                AtlasSkeletonBlock(Modifier.fillMaxWidth().height(48.dp), RoundedCornerShape(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtlasSkeletonBlock(Modifier.size(width = 54.dp, height = 30.dp), RoundedCornerShape(99.dp))
                    AtlasSkeletonBlock(Modifier.size(width = 74.dp, height = 30.dp), RoundedCornerShape(99.dp))
                    AtlasSkeletonBlock(Modifier.size(width = 68.dp, height = 30.dp), RoundedCornerShape(99.dp))
                    AtlasSkeletonBlock(Modifier.size(width = 60.dp, height = 30.dp), RoundedCornerShape(99.dp))
                }
            }
            AtlasPayrollLoadingIndicator(Modifier.fillMaxWidth().padding(vertical = 18.dp))
        }
    }
}

@Composable
fun AtlasPayrollLoadingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            atlasText("Chargement des versements…", "Loading payroll entries…", "جارٍ تحميل بيانات الرواتب…"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
