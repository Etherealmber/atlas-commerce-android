package ma.etherealmber.atlascommerce.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as MaterialSurface
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.etherealmber.atlascommerce.core.DensityMode
import ma.etherealmber.atlascommerce.core.ThemeMode
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled

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
    positive = Color(0xFFA16207),
    positiveSoft = Color(0xFFFEF3C7),
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
    positive = Color(0xFFFBBF24),
    positiveSoft = Color(0xFF3A2A0B),
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
    val target = if (dark) darkColors() else lightColors()
    val paletteTarget = if (dark) DarkPalette else LightPalette
    val duration = if (LocalAtlasAnimationsEnabled.current) 520 else 0
    val colorSpec = tween<Color>(durationMillis = duration, easing = FastOutSlowInEasing)
    val colors = target.copy(
        primary = animateColorAsState(target.primary, colorSpec, label = "theme-primary").value,
        onPrimary = animateColorAsState(target.onPrimary, colorSpec, label = "theme-on-primary").value,
        primaryContainer = animateColorAsState(target.primaryContainer, colorSpec, label = "theme-primary-container").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, colorSpec, label = "theme-on-primary-container").value,
        secondary = animateColorAsState(target.secondary, colorSpec, label = "theme-secondary").value,
        onSecondary = animateColorAsState(target.onSecondary, colorSpec, label = "theme-on-secondary").value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, colorSpec, label = "theme-secondary-container").value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, colorSpec, label = "theme-on-secondary-container").value,
        tertiary = animateColorAsState(target.tertiary, colorSpec, label = "theme-tertiary").value,
        onTertiary = animateColorAsState(target.onTertiary, colorSpec, label = "theme-on-tertiary").value,
        background = animateColorAsState(target.background, colorSpec, label = "theme-background").value,
        onBackground = animateColorAsState(target.onBackground, colorSpec, label = "theme-on-background").value,
        surface = animateColorAsState(target.surface, colorSpec, label = "theme-surface").value,
        onSurface = animateColorAsState(target.onSurface, colorSpec, label = "theme-on-surface").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, colorSpec, label = "theme-surface-variant").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, colorSpec, label = "theme-on-surface-variant").value,
        surfaceTint = animateColorAsState(target.surfaceTint, colorSpec, label = "theme-surface-tint").value,
        surfaceDim = animateColorAsState(target.surfaceDim, colorSpec, label = "theme-surface-dim").value,
        surfaceBright = animateColorAsState(target.surfaceBright, colorSpec, label = "theme-surface-bright").value,
        surfaceContainerLowest = animateColorAsState(target.surfaceContainerLowest, colorSpec, label = "theme-container-lowest").value,
        surfaceContainerLow = animateColorAsState(target.surfaceContainerLow, colorSpec, label = "theme-container-low").value,
        surfaceContainer = animateColorAsState(target.surfaceContainer, colorSpec, label = "theme-container").value,
        surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, colorSpec, label = "theme-container-high").value,
        surfaceContainerHighest = animateColorAsState(target.surfaceContainerHighest, colorSpec, label = "theme-container-highest").value,
        inverseSurface = animateColorAsState(target.inverseSurface, colorSpec, label = "theme-inverse-surface").value,
        inverseOnSurface = animateColorAsState(target.inverseOnSurface, colorSpec, label = "theme-inverse-on-surface").value,
        inversePrimary = animateColorAsState(target.inversePrimary, colorSpec, label = "theme-inverse-primary").value,
        outline = animateColorAsState(target.outline, colorSpec, label = "theme-outline").value,
        outlineVariant = animateColorAsState(target.outlineVariant, colorSpec, label = "theme-outline-variant").value,
        error = animateColorAsState(target.error, colorSpec, label = "theme-error").value,
        onError = animateColorAsState(target.onError, colorSpec, label = "theme-on-error").value,
        errorContainer = animateColorAsState(target.errorContainer, colorSpec, label = "theme-error-container").value,
        onErrorContainer = animateColorAsState(target.onErrorContainer, colorSpec, label = "theme-on-error-container").value,
        scrim = animateColorAsState(target.scrim, colorSpec, label = "theme-scrim").value,
    )
    val palette = paletteTarget.copy(
        positive = animateColorAsState(paletteTarget.positive, colorSpec, label = "palette-positive").value,
        positiveSoft = animateColorAsState(paletteTarget.positiveSoft, colorSpec, label = "palette-positive-soft").value,
        warning = animateColorAsState(paletteTarget.warning, colorSpec, label = "palette-warning").value,
        warningSoft = animateColorAsState(paletteTarget.warningSoft, colorSpec, label = "palette-warning-soft").value,
        danger = animateColorAsState(paletteTarget.danger, colorSpec, label = "palette-danger").value,
        dangerSoft = animateColorAsState(paletteTarget.dangerSoft, colorSpec, label = "palette-danger-soft").value,
        info = animateColorAsState(paletteTarget.info, colorSpec, label = "palette-info").value,
        infoSoft = animateColorAsState(paletteTarget.infoSoft, colorSpec, label = "palette-info-soft").value,
        markFace = animateColorAsState(paletteTarget.markFace, colorSpec, label = "palette-mark-face").value,
        markInk = animateColorAsState(paletteTarget.markInk, colorSpec, label = "palette-mark-ink").value,
        cardBorder = animateColorAsState(paletteTarget.cardBorder, colorSpec, label = "palette-card-border").value,
        mutedText = animateColorAsState(paletteTarget.mutedText, colorSpec, label = "palette-muted").value,
        heroStart = animateColorAsState(paletteTarget.heroStart, colorSpec, label = "palette-hero-start").value,
        heroEnd = animateColorAsState(paletteTarget.heroEnd, colorSpec, label = "palette-hero-end").value,
    )
    CompositionLocalProvider(
        LocalAtlasPalette provides palette,
        LocalAtlasDarkTheme provides dark,
        LocalAtlasDensity provides if (compact) CompactDensity else ComfortDensity,
    ) {
        MaterialTheme(
            colorScheme = colors,
            shapes = atlasShapes(),
            typography = atlasTypography(compact),
            content = content,
        )
    }
}

/** Dark zinc-and-saffron Atlas theme shared by the pre-authentication screens. */
@Composable
fun AtlasAuthTheme(content: @Composable () -> Unit) {
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    val colors = darkColorScheme(
        primary = Color(0xFFF4F4F5),
        onPrimary = Color(0xFF0A0A0C),
        primaryContainer = Color(0xFF2A2A30),
        onPrimaryContainer = Color(0xFFF4F4F5),
        secondary = Color(0xFFD4D4D8),
        onSecondary = Color(0xFF18181B),
        tertiary = Saffron,
        onTertiary = Color(0xFF171717),
        background = Color(0xFF09090B),
        onBackground = Color(0xFFF4F4F5),
        surface = Color(0xFF17171B),
        onSurface = Color(0xFFF4F4F5),
        surfaceVariant = Color(0xFF222227),
        onSurfaceVariant = Color(0xFFB4B4BC),
        outline = Color(0xFF66666F),
        outlineVariant = Color(0xFF3A3A41),
        error = Color(0xFFFCA5A5),
        onError = Color(0xFF450A0A),
    )
    val authPalette = DarkPalette.copy(
        positive = Color(0xFFFBBF24),
        positiveSoft = Color(0xFF3A2A0B),
        cardBorder = Color(0xFF3A3A41),
        mutedText = Color(0xFFB4B4BC),
        heroStart = Color(0xFF09090B),
        heroEnd = Color(0xFF26262C),
    )
    CompositionLocalProvider(LocalAtlasPalette provides authPalette) {
        MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes) {
            // Establish matching LocalContentColor as well as MaterialTheme colors;
            // auth routes are composed beneath a root Surface using the app theme.
            MaterialSurface(
                modifier = Modifier.fillMaxSize(),
                color = colors.background,
                contentColor = colors.onBackground,
                content = content,
            )
        }
    }
}

/** Convenience accessors used across screens. */
object Atlas {
    val palette: AtlasPalette
        @Composable get() = LocalAtlasPalette.current

    val density: AtlasDensity
        @Composable get() = LocalAtlasDensity.current
}
