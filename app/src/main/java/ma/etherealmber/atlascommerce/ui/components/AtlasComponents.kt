package ma.etherealmber.atlascommerce.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import ma.etherealmber.atlascommerce.ui.theme.AtlasPalette

/** Semantic tone shared by chips, tiles and banners. */
enum class AtlasTone { NEUTRAL, POSITIVE, WARNING, DANGER, INFO }

@Composable
fun toneForeground(tone: AtlasTone): Color {
    val palette = Atlas.palette
    return when (tone) {
        AtlasTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        AtlasTone.POSITIVE -> palette.positive
        AtlasTone.WARNING -> palette.warning
        AtlasTone.DANGER -> palette.danger
        AtlasTone.INFO -> palette.info
    }
}

@Composable
fun toneBackground(tone: AtlasTone): Color {
    val palette = Atlas.palette
    return when (tone) {
        AtlasTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
        AtlasTone.POSITIVE -> palette.positiveSoft
        AtlasTone.WARNING -> palette.warningSoft
        AtlasTone.DANGER -> palette.dangerSoft
        AtlasTone.INFO -> palette.infoSoft
    }
}

/**
 * The Atlas "A" mark, drawn with Canvas so it stays crisp at any size and never
 * depends on an asset pipeline.
 */
@Composable
fun AtlasMark(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    palette: AtlasPalette = Atlas.palette,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(
            color = palette.markFace,
            cornerRadius = CornerRadius(w * 0.24f, w * 0.24f),
            size = Size(w, h),
        )
        val glyph = Path().apply {
            moveTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.80f, h * 0.80f)
            lineTo(w * 0.655f, h * 0.80f)
            lineTo(w * 0.605f, h * 0.665f)
            lineTo(w * 0.395f, h * 0.665f)
            lineTo(w * 0.345f, h * 0.80f)
            lineTo(w * 0.20f, h * 0.80f)
            close()
        }
        drawPath(glyph, color = palette.markInk)
        // Inner counter of the "A", punched out in the face colour.
        val counter = Path().apply {
            moveTo(w * 0.50f, h * 0.375f)
            lineTo(w * 0.565f, h * 0.545f)
            lineTo(w * 0.435f, h * 0.545f)
            close()
        }
        drawPath(counter, color = palette.markFace)
    }
}

@Composable
fun AtlasCard(
    modifier: Modifier = Modifier,
    padding: Dp = Atlas.density.cardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = BorderStroke(1.dp, Atlas.palette.cardBorder)
    val shape = MaterialTheme.shapes.large
    val color = MaterialTheme.colorScheme.surface
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = color,
            border = border,
        ) {
            Column(Modifier.padding(padding), content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = color,
            border = border,
        ) {
            Column(Modifier.padding(padding), content = content)
        }
    }
}

@Composable
fun AtlasSectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun AtlasStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    tone: AtlasTone = AtlasTone.NEUTRAL,
    icon: ImageVector? = null,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Atlas.palette.cardBorder),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = toneForeground(tone),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!hint.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = toneForeground(tone),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun AtlasChip(
    text: String,
    tone: AtlasTone = AtlasTone.NEUTRAL,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = toneBackground(tone),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = toneForeground(tone),
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = toneForeground(tone),
                maxLines = 1,
            )
        }
    }
}

@Composable
fun AtlasButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AtlasOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AtlasTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    onDone: (() -> Unit)? = null,
) {
    var revealed by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val placeholderContent: (@Composable () -> Unit)? = if (placeholder == null) {
        null
    } else {
        { Text(text = placeholder) }
    }
    val supportingContent: (@Composable () -> Unit)? = if (supportingText == null) {
        null
    } else {
        { Text(text = supportingText) }
    }
    val leadingContent: (@Composable () -> Unit)? = if (leadingIcon == null) {
        null
    } else {
        { Icon(imageVector = leadingIcon, contentDescription = null) }
    }
    val trailingContent: (@Composable () -> Unit)? = if (!isPassword) {
        null
    } else {
        {
            IconButton(onClick = { revealed = !revealed }) {
                Icon(
                    imageVector = if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (revealed) "Masquer" else "Afficher",
                )
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholderContent,
        leadingIcon = leadingContent,
        trailingIcon = trailingContent,
        supportingText = supportingContent,
        visualTransformation = if (isPassword && !revealed) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                onDone?.invoke()
            },
        ),
        singleLine = singleLine,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtlasTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Retour")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
fun AtlasServerChip(host: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Icon(Icons.Outlined.Dns, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = host,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AtlasLoading(message: String = "Chargement…", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(strokeWidth = 3.dp)
        Spacer(Modifier.height(14.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AtlasErrorCard(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Connexion impossible",
    onRetry: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = toneBackground(AtlasTone.DANGER),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = toneForeground(AtlasTone.DANGER),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = toneForeground(AtlasTone.DANGER),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (onRetry != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onRetry) { Text("Réessayer") }
            }
        }
    }
}

@Composable
fun AtlasOfflineBanner(message: String = "Mode hors ligne — données enregistrées sur l'appareil.", modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = toneBackground(AtlasTone.WARNING),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = toneForeground(AtlasTone.WARNING),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = toneForeground(AtlasTone.WARNING),
            )
        }
    }
}

@Composable
fun AtlasEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inventory2,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun AtlasInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun AtlasDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
    )
}

/** Small label/value line used in the legal screens. */
@Composable
fun AtlasBullet(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------------------------------------------------------------------------
// Motion, Transitions & Scan Haptic/Audio Feedback
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Native GFM Markdown Parser & Compose Renderer (Tables, Lists, Code, Inline)
// ---------------------------------------------------------------------------

/**
 * Pure AST representation of GitHub-Flavored Markdown returned by Atlas Agent IA
 * (headings, pipe tables with column alignment, numbered/bullet/task lists,
 * blockquotes, fenced code blocks, dividers, and rich inline spans).
 */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class BulletList(val items: List<ListItem>) : MarkdownBlock
    data class NumberedList(val items: List<NumberedItem>) : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class Table(
        val headers: List<String>,
        val alignments: List<ColumnAlign>,
        val rows: List<List<String>>,
    ) : MarkdownBlock

    data object Divider : MarkdownBlock

    enum class ColumnAlign { LEFT, CENTER, RIGHT }
    data class ListItem(val text: String, val checked: Boolean? = null)
    data class NumberedItem(val number: Int, val text: String)
}

object AtlasMarkdownParser {

    private val headingRegex = Regex("^(#{1,6})\\s+(.+)$")
    private val hrRegex = Regex("^\\s*(?:[-*_]\\s*){3,}$")
    private val bulletRegex = Regex("^\\s*[-*+•]\\s+(?:(\\[[ xX]])\\s+)?(.+)$")
    private val numberedRegex = Regex("^\\s*(\\d+)[.)]\\s+(.+)$")
    private val tableSepCellRegex = Regex("^:?-{2,}:?$")

    fun parse(raw: String): List<MarkdownBlock> {
        if (raw.isBlank()) return emptyList()
        val lines = raw.replace("\r\n", "\n").lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // Skip blank lines between blocks
            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // 1. Fenced code block: ```lang ... ```
            if (trimmed.startsWith("```")) {
                val lang = trimmed.removePrefix("```").trim()
                val buf = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    buf += lines[i]
                    i++
                }
                if (i < lines.size) i++ // consume closing ```
                blocks += MarkdownBlock.CodeBlock(lang, buf.joinToString("\n"))
                continue
            }

            // 2. Pipe table: header row followed by |---|---| separator row
            if (looksLikeTableRow(trimmed) &&
                i + 1 < lines.size &&
                isTableSeparatorRow(lines[i + 1].trim())
            ) {
                val headers = splitTableCells(trimmed)
                val rawAligns = splitTableCells(lines[i + 1].trim()).map(::parseAlignment)
                val colCount = maxOf(headers.size, rawAligns.size)
                val normalizedHeaders = (0 until colCount).map { idx -> headers.getOrElse(idx) { "" } }
                val normalizedAligns = (0 until colCount).map { idx ->
                    rawAligns.getOrElse(idx) { MarkdownBlock.ColumnAlign.LEFT }
                }
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && looksLikeTableRow(lines[i].trim())) {
                    val cells = splitTableCells(lines[i].trim())
                    rows += (0 until colCount).map { idx -> cells.getOrElse(idx) { "" } }
                    i++
                }
                blocks += MarkdownBlock.Table(
                    headers = normalizedHeaders,
                    alignments = normalizedAligns,
                    rows = rows,
                )
                continue
            }

            // 3. Horizontal rule (---, ***, ___)
            if (hrRegex.matches(trimmed)) {
                blocks += MarkdownBlock.Divider
                i++
                continue
            }

            // 4. Heading (# .. ######)
            val headingMatch = headingRegex.matchEntire(trimmed)
            if (headingMatch != null) {
                blocks += MarkdownBlock.Heading(
                    level = headingMatch.groupValues[1].length,
                    text = headingMatch.groupValues[2].trim(),
                )
                i++
                continue
            }

            // 5. Blockquote (> ...)
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines += lines[i].trim().removePrefix(">").trimStart()
                    i++
                }
                blocks += MarkdownBlock.Blockquote(quoteLines.joinToString("\n"))
                continue
            }

            // 6. Bullet / task list (- ..., * ..., • ..., - [x] ...)
            if (bulletRegex.matches(line)) {
                val items = mutableListOf<MarkdownBlock.ListItem>()
                while (i < lines.size) {
                    val m = bulletRegex.matchEntire(lines[i]) ?: break
                    val box = m.groupValues[1]
                    val checked = when {
                        box.isEmpty() -> null
                        box.equals("[x]", ignoreCase = true) -> true
                        else -> false
                    }
                    val itemText = StringBuilder(m.groupValues[2].trim())
                    i++
                    while (i < lines.size &&
                        lines[i].startsWith("  ") &&
                        !bulletRegex.matches(lines[i]) &&
                        !numberedRegex.matches(lines[i]) &&
                        lines[i].isNotBlank()
                    ) {
                        itemText.append(' ').append(lines[i].trim())
                        i++
                    }
                    items += MarkdownBlock.ListItem(itemText.toString(), checked)
                }
                blocks += MarkdownBlock.BulletList(items)
                continue
            }

            // 7. Numbered list (1. ..., 2. ...)
            if (numberedRegex.matches(line)) {
                val items = mutableListOf<MarkdownBlock.NumberedItem>()
                while (i < lines.size) {
                    val m = numberedRegex.matchEntire(lines[i]) ?: break
                    val num = m.groupValues[1].toIntOrNull() ?: (items.size + 1)
                    val itemText = StringBuilder(m.groupValues[2].trim())
                    i++
                    while (i < lines.size &&
                        lines[i].startsWith("  ") &&
                        !numberedRegex.matches(lines[i]) &&
                        !bulletRegex.matches(lines[i]) &&
                        lines[i].isNotBlank()
                    ) {
                        itemText.append(' ').append(lines[i].trim())
                        i++
                    }
                    items += MarkdownBlock.NumberedItem(num, itemText.toString())
                }
                blocks += MarkdownBlock.NumberedList(items)
                continue
            }

            // 8. Paragraph (coalesce contiguous plain text lines until next block boundary)
            val paraLines = mutableListOf<String>()
            while (i < lines.size) {
                val cur = lines[i]
                val curTrimmed = cur.trim()
                if (curTrimmed.isEmpty() ||
                    curTrimmed.startsWith("```") ||
                    curTrimmed.startsWith(">") ||
                    hrRegex.matches(curTrimmed) ||
                    headingRegex.matches(curTrimmed) ||
                    bulletRegex.matches(cur) ||
                    numberedRegex.matches(cur) ||
                    (looksLikeTableRow(curTrimmed) && i + 1 < lines.size && isTableSeparatorRow(lines[i + 1].trim()))
                ) {
                    break
                }
                paraLines += curTrimmed
                i++
            }
            if (paraLines.isNotEmpty()) {
                blocks += MarkdownBlock.Paragraph(paraLines.joinToString("\n"))
            } else {
                i++
            }
        }

        return blocks
    }

    internal fun looksLikeTableRow(trimmed: String): Boolean {
        if (!trimmed.contains('|')) return false
        val cells = splitTableCells(trimmed)
        return cells.size >= 2
    }

    internal fun isTableSeparatorRow(trimmed: String): Boolean {
        if (!trimmed.contains('|') || !trimmed.contains('-')) return false
        val cells = splitTableCells(trimmed)
        return cells.size >= 2 && cells.all { cell -> tableSepCellRegex.matches(cell) }
    }

    internal fun splitTableCells(row: String): List<String> {
        val stripped = row.trim()
            .removePrefix("|")
            .let { if (it.endsWith("|") && !it.endsWith("\\|")) it.dropLast(1) else it }
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var k = 0
        while (k < stripped.length) {
            val ch = stripped[k]
            if (ch == '\\' && k + 1 < stripped.length && stripped[k + 1] == '|') {
                cur.append('|')
                k += 2
            } else if (ch == '|') {
                result += cur.toString().trim()
                cur.clear()
                k++
            } else {
                cur.append(ch)
                k++
            }
        }
        result += cur.toString().trim()
        return result
    }

    private fun parseAlignment(spec: String): MarkdownBlock.ColumnAlign {
        val left = spec.startsWith(":")
        val right = spec.endsWith(":")
        return when {
            left && right -> MarkdownBlock.ColumnAlign.CENTER
            right -> MarkdownBlock.ColumnAlign.RIGHT
            else -> MarkdownBlock.ColumnAlign.LEFT
        }
    }

    /** Strips markdown syntax markers for natural table column width estimation. */
    fun stripInlineMarkers(text: String): String =
        text.replace(Regex("\\*\\*\\*(.+?)\\*\\*\\*"), "$1")
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
            .replace(Regex("__(.+?)__"), "$1")
            .replace(Regex("~~(.+?)~~"), "$1")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            .trim()
}

/**
 * Converts inline Markdown (`**bold**`, `*italic*`, `` `code` ``, `~~strike~~`, `[link](url)`)
 * into a native Compose [AnnotatedString].
 */
fun buildInlineMarkdownAnnotatedString(
    raw: String,
    codeBackground: Color,
    codeColor: Color,
    linkColor: Color,
): AnnotatedString = buildAnnotatedString {
    var i = 0
    val n = raw.length
    while (i < n) {
        // 1. Inline code: `...`
        if (raw[i] == '`') {
            val end = raw.indexOf('`', i + 1)
            if (end > i) {
                val start = length
                append(" ${raw.substring(i + 1, end)} ")
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                        color = codeColor,
                        fontWeight = FontWeight.Medium,
                    ),
                    start,
                    length,
                )
                i = end + 1
                continue
            }
        }

        // 2. Bold + Italic: ***...***
        if (raw.startsWith("***", i)) {
            val end = raw.indexOf("***", i + 3)
            if (end > i) {
                val start = length
                append(
                    buildInlineMarkdownAnnotatedString(
                        raw.substring(i + 3, end),
                        codeBackground,
                        codeColor,
                        linkColor,
                    ),
                )
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                    start,
                    length,
                )
                i = end + 3
                continue
            }
        }

        // 3. Bold: **...** or __...__
        if (raw.startsWith("**", i) || raw.startsWith("__", i)) {
            val delim = raw.substring(i, i + 2)
            val end = raw.indexOf(delim, i + 2)
            if (end > i) {
                val start = length
                append(
                    buildInlineMarkdownAnnotatedString(
                        raw.substring(i + 2, end),
                        codeBackground,
                        codeColor,
                        linkColor,
                    ),
                )
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                i = end + 2
                continue
            }
        }

        // 4. Strikethrough: ~~...~~
        if (raw.startsWith("~~", i)) {
            val end = raw.indexOf("~~", i + 2)
            if (end > i) {
                val start = length
                append(raw.substring(i + 2, end))
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, length)
                i = end + 2
                continue
            }
        }

        // 5. Link: [label](https://...)
        if (raw[i] == '[') {
            val closeBracket = raw.indexOf(']', i + 1)
            if (closeBracket > i && closeBracket + 1 < n && raw[closeBracket + 1] == '(') {
                val closeParen = raw.indexOf(')', closeBracket + 2)
                if (closeParen > closeBracket) {
                    val label = raw.substring(i + 1, closeBracket)
                    val url = raw.substring(closeBracket + 2, closeParen)
                    val start = length
                    append(label)
                    addLink(
                        LinkAnnotation.Url(
                            url = url,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Medium,
                                ),
                            ),
                        ),
                        start,
                        length,
                    )
                    i = closeParen + 1
                    continue
                }
            }
        }

        // 6. Single-asterisk italic: *...*
        if (raw[i] == '*' && (i + 1 < n && raw[i + 1] != ' ')) {
            val end = raw.indexOf('*', i + 1)
            if (end > i && raw[end - 1] != ' ') {
                val start = length
                append(raw.substring(i + 1, end))
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                i = end + 1
                continue
            }
        }

        append(raw[i])
        i++
    }
}

@Composable
fun AtlasMarkdown(
    markdown: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { AtlasMarkdownParser.parse(markdown) }
    val context = LocalContext.current
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val codeFg = MaterialTheme.colorScheme.primary
    val linkFg = MaterialTheme.colorScheme.primary

    SelectionContainer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Heading -> {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        }
                        Text(
                            text = rememberInline(block.text, codeBg, codeFg, linkFg),
                            style = style,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                        )
                    }

                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = rememberInline(block.text, codeBg, codeFg, linkFg),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                        )
                    }

                    is MarkdownBlock.BulletList -> {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            block.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    if (item.checked != null) {
                                        Icon(
                                            imageVector = if (item.checked) {
                                                Icons.Outlined.CheckBox
                                            } else {
                                                Icons.Outlined.CheckBoxOutlineBlank
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(16.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    } else {
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 8.dp),
                                        )
                                    }
                                    Text(
                                        text = rememberInline(item.text, codeBg, codeFg, linkFg),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    is MarkdownBlock.NumberedList -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            block.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Text(
                                        text = "${item.number}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(24.dp),
                                    )
                                    Text(
                                        text = rememberInline(item.text, codeBg, codeFg, linkFg),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    is MarkdownBlock.Blockquote -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = rememberInline(block.text, codeBg, codeFg, linkFg),
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = textColor,
                            )
                        }
                    }

                    is MarkdownBlock.CodeBlock -> {
                        CodeBlockCard(
                            language = block.language,
                            code = block.code,
                            context = context,
                        )
                    }

                    is MarkdownBlock.Table -> {
                        MarkdownTableCard(
                            table = block,
                            textColor = textColor,
                            codeBg = codeBg,
                            codeFg = codeFg,
                            linkFg = linkFg,
                        )
                    }

                    MarkdownBlock.Divider -> {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberInline(
    raw: String,
    codeBg: Color,
    codeFg: Color,
    linkFg: Color,
): AnnotatedString = remember(raw, codeBg, codeFg, linkFg) {
    buildInlineMarkdownAnnotatedString(raw, codeBg, codeFg, linkFg)
}

@Composable
private fun MarkdownTableCard(
    table: MarkdownBlock.Table,
    textColor: Color,
    codeBg: Color,
    codeFg: Color,
    linkFg: Color,
) {
    val colCount = table.headers.size
    if (colCount == 0) return

    val weights = remember(table) {
        (0 until colCount).map { col ->
            val maxChars = maxOf(
                AtlasMarkdownParser.stripInlineMarkers(table.headers[col]).length,
                table.rows.maxOfOrNull { row ->
                    AtlasMarkdownParser.stripInlineMarkers(row.getOrElse(col) { "" }).length
                } ?: 0,
            )
            maxChars.coerceIn(8, 38).toFloat()
        }
    }
    val minScrollWidth = remember(weights) {
        weights.sumOf { w -> (w * 6.2f).toInt().coerceIn(88, 220) }.dp
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val resolvedWidth = maxOf(maxWidth, minScrollWidth)
        val scrollState = rememberScrollState()

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Atlas.palette.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
        ) {
            Column(modifier = Modifier.width(resolvedWidth)) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    table.headers.forEachIndexed { colIdx, header ->
                        Text(
                            text = rememberInline(header, codeBg, codeFg, linkFg),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = table.alignments[colIdx].toComposeAlign(),
                            modifier = Modifier
                                .weight(weights[colIdx])
                                .padding(horizontal = 6.dp),
                        )
                    }
                }

                // Data Rows
                table.rows.forEachIndexed { rowIdx, row ->
                    HorizontalDivider(color = Atlas.palette.cardBorder.copy(alpha = 0.65f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (rowIdx % 2 == 1) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        row.forEachIndexed { colIdx, cell ->
                            Text(
                                text = rememberInline(cell, codeBg, codeFg, linkFg),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor,
                                textAlign = table.alignments[colIdx].toComposeAlign(),
                                modifier = Modifier
                                    .weight(weights[colIdx])
                                    .padding(horizontal = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun MarkdownBlock.ColumnAlign.toComposeAlign(): TextAlign = when (this) {
    MarkdownBlock.ColumnAlign.LEFT -> TextAlign.Start
    MarkdownBlock.ColumnAlign.CENTER -> TextAlign.Center
    MarkdownBlock.ColumnAlign.RIGHT -> TextAlign.End
}

@Composable
private fun CodeBlockCard(
    language: String,
    code: String,
    context: Context,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Atlas.palette.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = language.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("code", code))
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Copier", style = MaterialTheme.typography.labelSmall)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp),
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
