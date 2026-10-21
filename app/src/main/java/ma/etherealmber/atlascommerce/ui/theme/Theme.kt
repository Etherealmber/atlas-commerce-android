package ma.etherealmber.atlascommerce.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.etherealmber.atlascommerce.core.DensityMode
import ma.etherealmber.atlascommerce.core.ThemeMode

// ---------------------------------------------------------------------------
// Brand palette — zinc / paper / saffron, matching the Atlas web app.
// ---------------------------------------------------------------------------

val Brand = Color(0xFF0A0A0A)
val BrandSoft = Color(0xFF171717)
val Saffron = Color(0xFFF59E0B)
val Paper = Color(0xFFF5F2EB)
val Surface = Color(0xFFFFFFFF)
val Elevated = Color(0xFFFAF8F4)
val Ink = Color(0xFF0F172A)
val InkSoft = Color(0xFF475569)
val Line = Color(0xFFE8EAEF)

val DarkBg = Color(0xFF0A0A0C)
val DarkSurface = Color(0xFF141414)
val DarkElevated = Color(0xFF1A1A1A)
val DarkOnBg = Color(0xFFF4F4F5)
val DarkMuted = Color(0xFFA1A1AA)
val DarkOutline = Color(0xFF27272A)

@Immutable
data class AtlasPalette(
    val positive: Color,
    val positiveSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val markFace: Color,
    val markInk: Color,
    val cardBorder: Color,
    val mutedText: Color,
    val heroStart: Color,
    val heroEnd: Color,
)

val LightPalette = AtlasPalette(
    positive = Color(0xFF0F7B4F),
    positiveSoft = Color(0xFFE6F6EC),
    warning = Color(0xFFB45309),
    warningSoft = Color(0xFFFEF3C7),
    danger = Color(0xFFB91C1C),
    dangerSoft = Color(0xFFFEE2E2),
    info = Color(0xFF1D4ED8),
    infoSoft = Color(0xFFE0EAFF),
    markFace = Brand,
    markInk = Color(0xFFFAFAFA),
    cardBorder = Line,
    mutedText = InkSoft,
    heroStart = Color(0xFF171717),
    heroEnd = Color(0xFF2B2B2B),
)

val DarkPalette = AtlasPalette(
    positive = Color(0xFF34D399),
    positiveSoft = Color(0xFF10362A),
    warning = Color(0xFFFBBF24),
    warningSoft = Color(0xFF3A2A0B),
    danger = Color(0xFFF87171),
    dangerSoft = Color(0xFF3A1414),
    info = Color(0xFF93C5FD),
    infoSoft = Color(0xFF16233F),
    markFace = Color(0xFFFAFAFA),
    markInk = Brand,
    cardBorder = DarkOutline,
    mutedText = DarkMuted,
    heroStart = Color(0xFF1F1F23),
    heroEnd = Color(0xFF0E0E11),
)

val LocalAtlasPalette = staticCompositionLocalOf { LightPalette }

/** Resolved (not requested) dark flag, used by the system bars. */
val LocalAtlasDarkTheme = staticCompositionLocalOf { false }

@Immutable
data class AtlasDensity(
    val screenPadding: Dp,
    val cardMargin: Dp,
    val cardPadding: Dp,
    val sectionGap: Dp,
    val itemGap: Dp,
    val barHorizontal: Dp,
    val barVertical: Dp,
    val iconSize: Dp,
)

val CompactDensity = AtlasDensity(
    screenPadding = 12.dp,
    cardMargin = 8.dp,
    cardPadding = 12.dp,
    sectionGap = 12.dp,
    itemGap = 8.dp,
    barHorizontal = 12.dp,
    barVertical = 8.dp,
    iconSize = 22.dp,
)

val ComfortDensity = AtlasDensity(
    screenPadding = 18.dp,
    cardMargin = 12.dp,
    cardPadding = 16.dp,
    sectionGap = 18.dp,
    itemGap = 10.dp,
    barHorizontal = 16.dp,
    barVertical = 10.dp,
    iconSize = 24.dp,
)

val LocalAtlasDensity = staticCompositionLocalOf { CompactDensity }

private fun lightColors() = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF1F5F9),
    onPrimaryContainer = Brand,
    secondary = BrandSoft,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9E1),
    onSecondaryContainer = Brand,
    tertiary = Saffron,
    onTertiary = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Elevated,
    onSurfaceVariant = InkSoft,
    outline = Line,
    outlineVariant = Color(0xFFDDE1E8),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    surfaceContainer = Surface,
    surfaceContainerHigh = Elevated,
)

private fun darkColors() = darkColorScheme(
    primary = Color(0xFFFAFAFA),
    onPrimary = Brand,
    primaryContainer = DarkElevated,
    onPrimaryContainer = DarkOnBg,
    secondary = DarkMuted,
    onSecondary = DarkBg,
    secondaryContainer = DarkElevated,
    onSecondaryContainer = DarkOnBg,
    tertiary = Saffron,
    onTertiary = Brand,
    background = DarkBg,
    onBackground = DarkOnBg,
    surface = DarkSurface,
    onSurface = DarkOnBg,
    surfaceVariant = DarkElevated,
    onSurfaceVariant = DarkMuted,
    outline = DarkOutline,
    outlineVariant = Color(0xFF3F3F46),
    error = Color(0xFFF87171),
    onError = Brand,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkElevated,
)

private fun atlasShapes() = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private fun atlasTypography(compact: Boolean): Typography {
    val base = Typography()
    val titleSize = if (compact) 19.sp else 22.sp
    val bodySize = if (compact) 14.sp else 16.sp
    return base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = titleSize),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = bodySize),
        bodyMedium = base.bodyMedium.copy(fontSize = 13.sp),
        bodySmall = base.bodySmall.copy(fontSize = 12.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun AtlasTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    densityMode: DensityMode = DensityMode.COMPACT,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val compact = densityMode == DensityMode.COMPACT
    CompositionLocalProvider(
        LocalAtlasPalette provides if (dark) DarkPalette else LightPalette,
        LocalAtlasDarkTheme provides dark,
        LocalAtlasDensity provides if (compact) CompactDensity else ComfortDensity,
    ) {
        MaterialTheme(
            colorScheme = if (dark) darkColors() else lightColors(),
            shapes = atlasShapes(),
            typography = atlasTypography(compact),
            content = content,
        )
    }
}

/** Convenience accessors used across screens. */
object Atlas {
    val palette: AtlasPalette
        @Composable get() = LocalAtlasPalette.current

    val density: AtlasDensity
        @Composable get() = LocalAtlasDensity.current
}
