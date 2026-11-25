package ma.etherealmber.atlascommerce.ui.i18n

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.ThemeMode
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
import ma.etherealmber.atlascommerce.ui.theme.LocalAtlasDarkTheme
import ma.etherealmber.atlascommerce.ui.theme.LocalAtlasThemeMode
import ma.etherealmber.atlascommerce.ui.theme.LocalSetAtlasThemeMode

internal data class AtlasToolbarPillMotion(val scale: Float, val background: Color)
internal val AtlasToolbarPillMotionKey = SemanticsPropertyKey<AtlasToolbarPillMotion>("AtlasToolbarPillMotion")

@Composable
fun AtlasLanguageToolbarPill(
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val language = LocalAtlasLanguage.current
    val setLanguage = LocalSetAtlasLanguage.current
    val followsSystem = LocalAtlasUsesSystemLanguage.current
    val useSystemLanguage = LocalUseSystemAtlasLanguage.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val dark = LocalAtlasDarkTheme.current
    val compact = widthDp <= 480
    val visualHeight = if (compact) 28.dp else 32.dp
    val languageContentDescription = atlasText("Langue", "Language", "اللغة")
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(if (pressed) 120 else 150),
        label = "language-pill-press-scale",
    )
    val background by animateColorAsState(
        targetValue = if (pressed) {
            if (dark) Color(0xFF2F2F35) else Color(0x2971717A)
        } else Color.Transparent,
        animationSpec = tween(if (pressed) 120 else 150),
        label = "language-pill-press-background",
    )
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .defaultMinSize(minWidth = if (compact) 48.dp else 68.dp, minHeight = 48.dp)
                .semantics {
                    contentDescription = languageContentDescription
                    this[AtlasToolbarPillMotionKey] = AtlasToolbarPillMotion(scale, background)
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = { expanded = true },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier
                    .height(visualHeight)
                    .widthIn(min = if (compact) 28.dp else 68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(background)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .padding(horizontal = if (compact) 6.dp else 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (pressed && dark) Color(0xFFFAFAFA) else if (dark) Color(0xFFD4D4D8) else contentColor,
                )
                if (!compact) {
                    Spacer(Modifier.size(5.dp))
                    Text(
                        text = language.tag.uppercase(),
                        color = if (pressed && dark) Color(0xFFFAFAFA) else if (dark) Color(0xFFD4D4D8) else contentColor,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        atlasText(
                            "Suivre la langue du téléphone",
                            "Follow phone language (${AppLanguage.fromSystemLocale().nativeLabel})",
                            "اتباع لغة الهاتف (${AppLanguage.fromSystemLocale().nativeLabel})",
                        ),
                    )
                },
                onClick = {
                    expanded = false
                    useSystemLanguage()
                },
                leadingIcon = if (followsSystem) {
                    { Text("✓", color = MaterialTheme.colorScheme.primary) }
                } else null,
            )
            AppLanguage.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.nativeLabel) },
                    onClick = {
                        expanded = false
                        setLanguage(option)
                    },
                    leadingIcon = if (option == language) {
                        { Text("✓", color = MaterialTheme.colorScheme.primary) }
                    } else null,
                )
            }
        }
    }
}

@Composable
fun AtlasThemeToolbarControl(modifier: Modifier = Modifier) {
    val mode = LocalAtlasThemeMode.current
    val setMode = LocalSetAtlasThemeMode.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val dark = LocalAtlasDarkTheme.current
    val height = if (widthDp <= 480) 28.dp else 32.dp
    val gap = if (widthDp <= 360) 4.dp else 6.dp
    val options = if (widthDp <= 360 && mode != ThemeMode.SYSTEM) {
        listOf(ThemeMode.LIGHT, ThemeMode.DARK)
    } else ThemeMode.entries
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
        options.forEach { option ->
            val selected = option == mode
            val icon = when (option) {
                ThemeMode.SYSTEM -> Icons.Outlined.Computer
                ThemeMode.LIGHT -> Icons.Outlined.Sun
                ThemeMode.DARK -> Icons.Outlined.Moon
            }
            val label = when (option) {
                ThemeMode.SYSTEM -> atlasText("Système", "System", "النظام")
                ThemeMode.LIGHT -> atlasText("Clair", "Light", "فاتح")
                ThemeMode.DARK -> atlasText("Sombre", "Dark", "داكن")
            }
            ToolbarThemeButton(
                mode = option,
                label = label,
                icon = icon,
                visualHeight = height,
                selected = selected,
                dark = dark,
                onClick = { setMode(option) },
            )
        }
    }
}

@Composable
private fun ToolbarThemeButton(
    mode: ThemeMode,
    label: String,
    icon: ImageVector,
    visualHeight: Dp,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
) {
    val source = remember(mode) { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(if (pressed) 120 else 150),
        label = "theme-pill-press-scale-${mode.storageKey}",
    )
    val pressedColor = if (dark) Color(0xFF2F2F35) else Color(0x2971717A)
    val background by animateColorAsState(
        targetValue = when {
            pressed -> pressedColor
            selected -> if (dark) Color(0xFF27272A) else Color(0xFFF4F4F5)
            else -> Color.Transparent
        },
        animationSpec = tween(if (pressed) 120 else 150),
        label = "theme-pill-press-background-${mode.storageKey}",
    )
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics {
                contentDescription = label
                this[AtlasToolbarPillMotionKey] = AtlasToolbarPillMotion(scale, background)
            }
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = visualHeight, height = visualHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(background)
                .graphicsLayer { scaleX = scale; scaleY = scale },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when {
                    pressed && dark -> Color(0xFFFAFAFA)
                    selected -> MaterialTheme.colorScheme.onSurface
                    dark -> Color(0xFFD4D4D8)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
