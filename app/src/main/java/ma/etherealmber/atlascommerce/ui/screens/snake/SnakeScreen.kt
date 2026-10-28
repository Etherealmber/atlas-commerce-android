package ma.etherealmber.atlascommerce.ui.screens.snake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import ma.etherealmber.atlascommerce.R
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnakeScreen(onClose: () -> Unit, pausedByPrompt: Boolean, isActive: Boolean) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var foreground by remember(lifecycleOwner) { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ -> foreground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var game by remember { mutableStateOf(SnakeState()) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(running, pausedByPrompt, game.over, foreground, isActive) {
        while (running && !pausedByPrompt && !game.over && foreground && isActive) {
            delay(240)
            game = game.step()
        }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(atlasText("Snake Atlas", "Atlas Snake", "ثعبان Atlas")) },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = atlasText("Retour", "Back", "رجوع")) } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(atlasText("Mangez les logos Atlas !", "Collect the Atlas logos!", "اجمع شعارات Atlas!"), style = MaterialTheme.typography.titleMedium)
            Text(atlasText("Score : ${game.score}", "Score: ${game.score}", "النتيجة: ${game.score}"), style = MaterialTheme.typography.titleMedium)
            val snakeColor = MaterialTheme.colorScheme.primary
            val boardColor = MaterialTheme.colorScheme.surfaceVariant
            BoxWithConstraints(Modifier.widthIn(max = 440.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(boardColor)
                .pointerInput(Unit) {
                    var drag = Offset.Zero
                    detectDragGestures(onDragStart = { drag = Offset.Zero }, onDragEnd = {
                        if (drag.getDistance() > 18f) {
                            val direction = if (abs(drag.x) > abs(drag.y)) {
                                if (drag.x > 0) Direction.RIGHT else Direction.LEFT
                            } else if (drag.y > 0) Direction.DOWN else Direction.UP
                            game = game.turn(direction)
                        }
                    }) { change, amount -> change.consume(); drag += amount }
                }) {
                val cell = maxWidth / BOARD_SIZE
                Canvas(Modifier.fillMaxSize()) {
                    val side = size.width / BOARD_SIZE
                    game.body.forEachIndexed { index, point ->
                        drawRoundRect(color = if (index == 0) snakeColor else snakeColor.copy(alpha = 0.75f),
                            topLeft = Offset(point.x * side + 1f, point.y * side + 1f), size = Size(side - 2f, side - 2f), cornerRadius = CornerRadius(side * 0.24f))
                    }
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).absoluteOffset(
                        x = cell * game.food.x + cell * 0.09f,
                        y = cell * game.food.y + cell * 0.09f,
                    ).size(cell * 0.82f),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = atlasText("Logo Atlas à collecter", "Atlas logo to collect", "شعار Atlas للجمع"),
                        modifier = Modifier.padding(cell * 0.16f),
                    )
                }
            }
            if (game.over) Text(if (game.won) atlasText("Bravo, plateau rempli !", "You filled the board!", "أحسنت، امتلأت اللوحة!") else atlasText("Partie terminée", "Game over", "انتهت اللعبة"), color = MaterialTheme.colorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { if (game.over) { game = SnakeState(); running = true } else running = !running }, enabled = !pausedByPrompt) {
                    Text(if (game.over) atlasText("Rejouer", "Play again", "العب مجدداً") else if (running) atlasText("Pause", "Pause", "إيقاف مؤقت") else atlasText("Jouer", "Play", "العب"))
                }
                OutlinedButton(onClick = { game = SnakeState(); running = false }) { Text(atlasText("Recommencer", "Restart", "إعادة البدء")) }
            }
            Text(atlasText("Balayez ou utilisez les flèches.", "Swipe or use the arrows.", "اسحب أو استخدم الأسهم."), style = MaterialTheme.typography.bodySmall)
            FilledTonalIconButton(
                onClick = { game = game.turn(Direction.UP) },
                modifier = Modifier.size(64.dp),
                enabled = !pausedByPrompt && !game.over,
            ) { Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = atlasText("Haut", "Up", "أعلى"), modifier = Modifier.size(36.dp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(
                    onClick = { game = game.turn(Direction.LEFT) },
                    modifier = Modifier.size(64.dp),
                    enabled = !pausedByPrompt && !game.over,
                ) { Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = atlasText("Gauche", "Left", "يسار"), modifier = Modifier.size(36.dp)) }
                FilledTonalIconButton(
                    onClick = { game = game.turn(Direction.DOWN) },
                    modifier = Modifier.size(64.dp),
                    enabled = !pausedByPrompt && !game.over,
                ) { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = atlasText("Bas", "Down", "أسفل"), modifier = Modifier.size(36.dp)) }
                FilledTonalIconButton(
                    onClick = { game = game.turn(Direction.RIGHT) },
                    modifier = Modifier.size(64.dp),
                    enabled = !pausedByPrompt && !game.over,
                ) { Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = atlasText("Droite", "Right", "يمين"), modifier = Modifier.size(36.dp)) }
            }
        }
    }
}
