package ma.etherealmber.atlascommerce.ui.theme

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

val ElectricIndigo = Color(0xFF0A0A0A)
val CyanAccent = Color(0xFF475569)

private val Brand = Color(0xFF0A0A0A)
private val BrandSoft = Color(0xFF171717)
private val Saffron = Color(0xFFF59E0B)
private val Danger = Color(0xFFEF4444)
private val Ink = Color(0xFF0F172A)
private val InkSoft = Color(0xFF475569)
private val Line = Color(0xFFE8EAEF)
private val Paper = Color(0xFFF5F2EB)
private val Surface = Color(0xFFFFFFFF)
private val Elevated = Color(0xFFFAF8F4)

val DarkBg = Color(0xFF0A0A0C)
val DarkBgMid = Color(0xFF121214)
val DarkBgTop = Color(0xFF1A1A1E)
val DarkSurface = Color(0xFF141414)
val DarkElevated = Color(0xFF1A1A1A)
private val DarkOnBg = Color(0xFFF4F4F5)
val DarkMuted = Color(0xFFA1A1AA)
val DarkOutline = Color(0xFF27272A)

val DarkGlassFillTop = DarkSurface
val DarkGlassFillBottom = DarkBg
val DarkGlassPanel = DarkElevated
val DarkGlassElevated = DarkElevated
val DarkGlassStroke = DarkOutline
val DarkGlassStrokeAccent = Color(0xFF3F3F46)
val DarkGlassChip = Color(0xFF27272A)

val LightGlassFillTop = Surface
val LightGlassFillBottom = Paper
val LightGlassPanelTop = Surface
val LightGlassPanelMid = Elevated
val LightGlassStrokeTop = Line
val LightGlassStrokeBottom = Line

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
    screenPadding = 16.dp,
    cardMargin = 12.dp,
    cardPadding = 16.dp,
    sectionGap = 16.dp,
    itemGap = 10.dp,
    barHorizontal = 16.dp,
    barVertical = 10.dp,
    iconSize = 24.dp,
)

val LocalAtlasDensity = staticCompositionLocalOf { CompactDensity }

private fun lightColors(compact: Boolean) = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF1F5F9),
    onPrimaryContainer = Brand,
    secondary = BrandSoft,
    onSecondary = Color.White,
    tertiary = Saffron,
    onTertiary = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Elevated,
    onSurfaceVariant = InkSoft,
    outline = Line,
    error = Danger,
    onError = Color.White,
)

private fun darkColors(compact: Boolean) = darkColorScheme(
    primary = Color(0xFFFAFAFA),
    onPrimary = Brand,
    primaryContainer = DarkElevated,
    onPrimaryContainer = DarkOnBg,
    secondary = Color(0xFFA1A1AA),
    onSecondary = DarkBg,
    tertiary = Saffron,
    onTertiary = Brand,
    background = DarkBg,
    onBackground = DarkOnBg,
    surface = DarkSurface,
    onSurface = DarkOnBg,
    surfaceVariant = DarkElevated,
    onSurfaceVariant = DarkMuted,
    outline = DarkOutline,
    error = Danger,
    onError = Color.White,
)

private fun atlasShapes(compact: Boolean) = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

private fun atlasTypography(compact: Boolean): Typography {
    val base = Typography()
    val titleSize = if (compact) 18.sp else 20.sp
    val bodySize = if (compact) 14.sp else 15.sp
    return base.copy(
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = titleSize),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = bodySize),
        bodyMedium = base.bodyMedium.copy(fontSize = 13.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun AtlasTheme(
    darkTheme: Boolean = false,
    compact: Boolean = true,
    content: @Composable () -> Unit,
) {
    val density = if (compact) CompactDensity else ComfortDensity
    CompositionLocalProvider(LocalAtlasDensity provides density) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColors(compact) else lightColors(compact),
            shapes = atlasShapes(compact),
            typography = atlasTypography(compact),
            content = content,
        )
    }
}
