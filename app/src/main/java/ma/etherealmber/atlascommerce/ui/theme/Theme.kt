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
// Atlas neutral scale — the supplied palette defines BOTH light and dark modes.
// Keep all surface, text, border, and chart color decisions centralized here.
// ---------------------------------------------------------------------------

object AtlasNeutral {
    // Light: white/paper at the base, descending into charcoal and near-black.
    val lightBase = Color(0xFFFFFFFF)
    val light50 = Color(0xFFFAFAFA)
    val light100 = Color(0xFFF5F5F5)
    val light200 = Color(0xFFE5E5E5)
    val light300 = Color(0xFFD4D4D4)
    val light400 = Color(0xFFA3A3A3)
    val light500 = Color(0xFF737373)
    val light600 = Color(0xFF525252)
    val light700 = Color(0xFF404040)
    val light800 = Color(0xFF262626)
    val light900 = Color(0xFF171717)
    val light950 = Color(0xFF0A0A0A)

    // Dark: black base with the same neutral ramp reversed for surfaces and text.
    val darkBase = Color(0xFF000000)
    val dark50 = Color(0xFF0A0A0A)
    val dark100 = Color(0xFF171717)
    val dark200 = Color(0xFF262626)
    val dark300 = Color(0xFF373737)
    val dark400 = Color(0xFF525252)
    val dark500 = Color(0xFF8A8A8A)
    val dark600 = Color(0xFFA3A3A3)
    val dark700 = Color(0xFFD4D4D4)
    val dark800 = Color(0xFFE5E5E5)
    val dark900 = Color(0xFFF5F5F5)
    val dark950 = Color(0xFFFAFAFA)
}

val Brand = AtlasNeutral.light950
val BrandSoft = AtlasNeutral.light900
val Saffron = Color(0xFFF59E0B)
val Paper = AtlasNeutral.light100
val Surface = AtlasNeutral.lightBase
val Elevated = AtlasNeutral.light200
val Ink = AtlasNeutral.light900
val InkSoft = AtlasNeutral.light600
val Line = AtlasNeutral.light300
val LineSoft = AtlasNeutral.light200
val LightPrimaryContainer = AtlasNeutral.light200
val LightSecondaryContainer = AtlasNeutral.light200
val LightSurfaceDim = AtlasNeutral.light300
val LightSurfaceBright = AtlasNeutral.lightBase
val LightContainerLowest = AtlasNeutral.lightBase
val LightContainerLow = AtlasNeutral.light50
val LightContainer = AtlasNeutral.light100
val LightContainerHigh = AtlasNeutral.light200
val LightContainerHighest = AtlasNeutral.light300

val DarkBg = AtlasNeutral.darkBase
val DarkSurface = AtlasNeutral.dark50
val DarkSurfaceLow = AtlasNeutral.dark50
val DarkSurfaceVariant = AtlasNeutral.dark100
val DarkSurfaceBright = AtlasNeutral.dark400
val DarkElevated = AtlasNeutral.dark200
val DarkOnBg = AtlasNeutral.dark950
val DarkMuted = AtlasNeutral.dark700
val DarkOutline = AtlasNeutral.dark400
val DarkOutlineSoft = AtlasNeutral.dark300
val DarkPrimary = AtlasNeutral.dark950
val DarkSecondary = AtlasNeutral.dark800
val DarkError = Color(0xFFF87171)
val DarkErrorOn = DarkBg
val DarkContainer = AtlasNeutral.dark100
val DarkContainerHigh = AtlasNeutral.dark200
val DarkContainerHighest = AtlasNeutral.dark300

/** Fixed artwork and camera-accent colors; kept here so UI files contain no scattered hex palette. */
object AtlasIllustrationPalette {
    const val avatarFallbackHex = "#1D4ED8"
    val white = Color(0xFFFFFFFF)
    val scanIdleBackdrop = Color(0xFF0B1220)
    val scanCancelOverlay = Color(0xDC7F1D1D)
    val scanControlOverlay = Color(0xCC0F172A)
    val scanControlOverlaySoft = Color(0xAA0F172A)
    val cameraSecurityBackdrop = Color(0xFF0B1026)
    val cameraSecurityMuted = Color(0xFF94A3B8)
    val cameraCyan = Color(0xFF38BDF8)
    val cameraIndigo = Color(0xFF818CF8)
    val onboardingCyan = Color(0xFF62D9F5)
    val onboardingBlue = Color(0xFF6AA8FF)
    val onboardingPurple = Color(0xFFAA8BFF)
    val onboardingMagenta = Color(0xFFF17CCB)
    val onboardingMint = Color(0xFF69E0C2)
    val onboardingAmber = Color(0xFFFFC66D)
    val skinLight = Color(0xFFF0BD98)
    val skinMedium = Color(0xFFD99B72)
    val skinDark = Color(0xFF8C5D42)
    val hairDark = Color(0xFF33251F)
    val hairBlack = Color(0xFF211B19)
    val hairBrown = Color(0xFF704A35)
    val hairChestnut = Color(0xFF60402D)
    val shirtLight = Color(0xFFF7F5F1)
    val shirtBlue = Color(0xFFEAEFF5)
    val eyeDark = Color(0xFF362A26)
    val snakePupil = Color(0xFF173540)
}

@Immutable
data class AtlasThemePreviewPalette(
    val background: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val outline: Color,
)

fun atlasThemePreviewPalette(dark: Boolean): AtlasThemePreviewPalette = if (dark) {
    AtlasThemePreviewPalette(DarkBg, DarkSurface, DarkOnBg, DarkMuted, DarkPrimary, DarkElevated, DarkOnBg, DarkOutlineSoft)
} else {
    AtlasThemePreviewPalette(Paper, Surface, Ink, InkSoft, Brand, Elevated, Brand, LineSoft)
}

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
    val heroText: Color,
    val heroMutedText: Color,
    val heroPill: Color,
    val chartSales: Color,
    val chartIncome: Color,
    val chartExpense: Color,
    val chartReceivable: Color,
    val chartPayable: Color,
)

val LightPalette = AtlasPalette(
    positive = Color(0xFF15803D),
    positiveSoft = Color(0xFFDCFCE7),
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
    heroStart = Color(0xFFE9E9E9),
    heroEnd = Color(0xFFD2D2D2),
    heroText = Ink,
    heroMutedText = InkSoft,
    heroPill = Color.White.copy(alpha = 0.72f),
    chartSales = Color(0xFF2563EB),
    chartIncome = Color(0xFF15803D),
    chartExpense = Color(0xFFDC2626),
    chartReceivable = Color(0xFF7C3AED),
    chartPayable = Color(0xFFD97706),
)

val DarkPalette = AtlasPalette(
    positive = Color(0xFF86EFAC),
    positiveSoft = Color(0xFF163322),
    warning = Color(0xFFFBBF24),
    warningSoft = Color(0xFF3A2A0B),
    danger = DarkError,
    dangerSoft = Color(0xFF3A1414),
    info = Color(0xFF9BC8DF),
    infoSoft = Color(0xFF193543),
    markFace = Color(0xFFFAFAFA),
    markInk = Brand,
    cardBorder = DarkOutline,
    mutedText = DarkMuted,
    heroStart = DarkSurface,
    heroEnd = DarkBg,
    heroText = DarkOnBg,
    heroMutedText = DarkMuted,
    heroPill = Color.White.copy(alpha = 0.10f),
    chartSales = Color(0xFF60A5FA),
    chartIncome = Color(0xFF86EFAC),
    chartExpense = Color(0xFFFCA5A5),
    chartReceivable = Color(0xFFC4B5FD),
    chartPayable = Color(0xFFFCD34D),
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
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Brand,
    secondary = BrandSoft,
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryContainer,
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
    outlineVariant = LineSoft,
    error = LightPalette.danger,
    onError = Color.White,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightContainerLowest,
    surfaceContainerLow = LightContainerLow,
    surfaceContainer = LightContainer,
    surfaceContainerHigh = LightContainerHigh,
    surfaceContainerHighest = LightContainerHighest,
)

private fun darkColors() = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBg,
    primaryContainer = DarkElevated,
    onPrimaryContainer = DarkOnBg,
    secondary = DarkSecondary,
    onSecondary = DarkBg,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnBg,
    tertiary = Saffron,
    onTertiary = Brand,
    background = DarkBg,
    onBackground = DarkOnBg,
    surface = DarkSurface,
    onSurface = DarkOnBg,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkOutline,
    outlineVariant = DarkOutlineSoft,
    error = DarkPalette.danger,
    onError = DarkErrorOn,
    surfaceDim = DarkBg,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkBg,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkContainer,
    surfaceContainerHigh = DarkContainerHigh,
    surfaceContainerHighest = DarkContainerHighest,
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
        heroText = animateColorAsState(paletteTarget.heroText, colorSpec, label = "palette-hero-text").value,
        heroMutedText = animateColorAsState(paletteTarget.heroMutedText, colorSpec, label = "palette-hero-muted").value,
        heroPill = animateColorAsState(paletteTarget.heroPill, colorSpec, label = "palette-hero-pill").value,
        chartSales = animateColorAsState(paletteTarget.chartSales, colorSpec, label = "palette-chart-sales").value,
        chartIncome = animateColorAsState(paletteTarget.chartIncome, colorSpec, label = "palette-chart-income").value,
        chartExpense = animateColorAsState(paletteTarget.chartExpense, colorSpec, label = "palette-chart-expense").value,
        chartReceivable = animateColorAsState(paletteTarget.chartReceivable, colorSpec, label = "palette-chart-receivable").value,
        chartPayable = animateColorAsState(paletteTarget.chartPayable, colorSpec, label = "palette-chart-payable").value,
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

/** Charcoal Atlas theme shared by pre-authentication screens. */
@Composable
fun AtlasAuthTheme(content: @Composable () -> Unit) {
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    val colors = darkColors()
    val authPalette = DarkPalette
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
