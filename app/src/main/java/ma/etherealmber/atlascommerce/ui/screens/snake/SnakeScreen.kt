package ma.etherealmber.atlascommerce.ui.screens.snake

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import java.time.LocalDate
import kotlin.math.abs

private enum class SnakePage { MENU, PLAY, SETTINGS, DAILY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnakeScreen(onClose: () -> Unit, pausedByPrompt: Boolean, isActive: Boolean) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var foreground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("atlas_snake_progress", Context.MODE_PRIVATE)
    }
    val today = remember { LocalDate.now() }
    val todayKey = remember(today) { today.toString() }
    val dailyTarget = remember(today) { 8 + today.dayOfYear % 5 }
    var bestScore by remember(preferences) { mutableIntStateOf(preferences.getInt("best_score", 0)) }
    var bestCrowns by remember(preferences) { mutableIntStateOf(preferences.getInt("best_crowns", 0)) }
    var dailyBest by remember(preferences, todayKey) {
        mutableIntStateOf(if (preferences.getString("daily_date", null) == todayKey) preferences.getInt("daily_best", 0) else 0)
    }
    var speedMillis by remember(preferences) { mutableIntStateOf(preferences.getInt("speed_ms", 170)) }
    var wrapWalls by remember(preferences) { mutableStateOf(preferences.getBoolean("wrap_walls", false)) }

    var page by remember { mutableStateOf(SnakePage.MENU) }
    var dailyMode by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var game by remember { mutableStateOf(SnakeState(wrapWalls = wrapWalls)) }
    val boardFocusRequester = remember { FocusRequester() }

    fun startGame(daily: Boolean) {
        dailyMode = daily
        game = SnakeState(wrapWalls = wrapWalls, targetScore = if (daily) dailyTarget else null)
        running = true
        page = SnakePage.PLAY
    }

    LaunchedEffect(game.score, game.crowns, dailyMode) {
        if (game.score > bestScore) {
            bestScore = game.score
            preferences.edit().putInt("best_score", bestScore).apply()
        }
        if (game.crowns > bestCrowns) {
            bestCrowns = game.crowns
            preferences.edit().putInt("best_crowns", bestCrowns).apply()
        }
        if (dailyMode && game.score > dailyBest) {
            dailyBest = game.score
            preferences.edit().putString("daily_date", todayKey).putInt("daily_best", dailyBest).apply()
        }
    }
    LaunchedEffect(page, isActive, foreground) {
        if (page == SnakePage.PLAY && isActive && foreground) boardFocusRequester.requestFocus()
    }
    LaunchedEffect(running, pausedByPrompt, game.over, foreground, isActive, speedMillis) {
        while (running && !pausedByPrompt && !game.over && foreground && isActive) {
            delay(speedMillis.toLong())
            game = game.step()
        }
    }
    LaunchedEffect(game.over) { if (game.over) running = false }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(when (page) {
                    SnakePage.MENU, SnakePage.PLAY -> atlasText("Atlas Snake", "Atlas Snake", "ثعبان Atlas")
                    SnakePage.SETTINGS -> atlasText("Paramètres", "Settings", "الإعدادات")
                    SnakePage.DAILY -> atlasText("Défi du jour", "Daily challenge", "تحدي اليوم")
                })
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (page == SnakePage.MENU) onClose() else {
                        running = false
                        page = SnakePage.MENU
                    }
                }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = atlasText("Retour", "Back", "رجوع"))
                }
            },
        )
    }) { padding ->
        when (page) {
            SnakePage.PLAY -> GameplayPage(
                modifier = Modifier.fillMaxSize().padding(padding),
                game = game,
                bestScore = bestScore,
                bestCrowns = bestCrowns,
                dailyMode = dailyMode,
                dailyTarget = dailyTarget,
                running = running,
                pausedByPrompt = pausedByPrompt,
                boardFocusRequester = boardFocusRequester,
                onDirection = { direction -> if (running && !pausedByPrompt) game = game.turn(direction) },
                onToggleRun = {
                    if (!pausedByPrompt) {
                        if (game.over) startGame(dailyMode) else running = !running
                    }
                },
                onRestart = { if (!pausedByPrompt) startGame(dailyMode) },
                onSettings = { running = false; page = SnakePage.SETTINGS },
            )
            SnakePage.MENU -> MenuPage(
                modifier = Modifier.fillMaxSize().padding(padding),
                bestScore = bestScore,
                bestCrowns = bestCrowns,
                onPlay = { startGame(false) },
                onSettings = { page = SnakePage.SETTINGS },
                onDaily = { page = SnakePage.DAILY },
            )
            SnakePage.SETTINGS -> SettingsPage(
                modifier = Modifier.fillMaxSize().padding(padding),
                speedMillis = speedMillis,
                wrapWalls = wrapWalls,
                onSpeed = { speedMillis = it; preferences.edit().putInt("speed_ms", it).apply() },
                onWrapWalls = {
                    wrapWalls = it
                    preferences.edit().putBoolean("wrap_walls", it).apply()
                    game = game.copy(wrapWalls = it)
                },
            )
            SnakePage.DAILY -> DailyPage(
                modifier = Modifier.fillMaxSize().padding(padding),
                target = dailyTarget,
                dailyBest = dailyBest,
                onStart = { startGame(true) },
            )
        }
    }
}

@Composable
private fun MenuPage(
    modifier: Modifier,
    bestScore: Int,
    bestCrowns: Int,
    onPlay: () -> Unit,
    onSettings: () -> Unit,
    onDaily: () -> Unit,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            atlasText("Le serpent Atlas", "Atlas Snake", "ثعبان Atlas"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            atlasText("Ramassez des pommes. Attrapez la couronne dorée !", "Collect apples. Catch the golden crown!", "اجمع التفاح والتقط التاج الذهبي!"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
        ) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    StatBadge(icon = { AppleGlyph(Modifier.size(22.dp)) }, label = atlasText("Record", "Best", "الأفضل"), value = bestScore)
                    StatBadge(icon = { CrownGlyph(Modifier.size(24.dp)) }, label = atlasText("Couronnes", "Crowns", "التيجان"), value = bestCrowns)
                }
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val previewSide = (maxWidth - 12.dp).coerceAtMost(340.dp)
                    SnakeBoard(
                        game = SnakeState(
                            body = listOf(Cell(5, 11), Cell(4, 11), Cell(3, 11), Cell(2, 11)),
                            direction = Direction.RIGHT,
                            queued = Direction.RIGHT,
                            food = Cell(13, 8),
                            crown = Cell(9, 5),
                        ),
                        modifier = Modifier.size(previewSide),
                        showEyes = true,
                    )
                }
            }
        }
        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).height(54.dp), shape = RoundedCornerShape(18.dp)) {
            Text(atlasText("Jouer", "Play", "العب"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Row(Modifier.fillMaxWidth().widthIn(max = 420.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp)) {
                Text(atlasText("Paramètres", "Settings", "الإعدادات"))
            }
            OutlinedButton(onClick = onDaily, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp)) {
                Text(atlasText("Défi du jour", "Daily challenge", "تحدي اليوم"))
            }
        }
    }
}

@Composable
private fun GameplayPage(
    modifier: Modifier,
    game: SnakeState,
    bestScore: Int,
    bestCrowns: Int,
    dailyMode: Boolean,
    dailyTarget: Int,
    running: Boolean,
    pausedByPrompt: Boolean,
    boardFocusRequester: FocusRequester,
    onDirection: (Direction) -> Unit,
    onToggleRun: () -> Unit,
    onRestart: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(modifier.padding(horizontal = 14.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            StatBadge(icon = { AppleGlyph(Modifier.size(20.dp)) }, label = atlasText("Pommes", "Apples", "التفاح"), value = game.score)
            StatBadge(icon = { CrownGlyph(Modifier.size(22.dp)) }, label = atlasText("Couronnes", "Crowns", "التيجان"), value = game.crowns)
            StatBadge(icon = { AppleGlyph(Modifier.size(17.dp)) }, label = atlasText("Record", "Best", "الأفضل"), value = bestScore)
        }
        if (dailyMode) {
            Text(
                atlasText("Défi : $dailyTarget pommes", "Daily goal: $dailyTarget apples", "الهدف اليومي: $dailyTarget تفاحات"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val boardSide = minOf(maxWidth, maxHeight, 500.dp)
            SnakeBoard(
                game = game,
                modifier = Modifier.size(boardSide)
                    .focusRequester(boardFocusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val direction = when (event.key) {
                            Key.DirectionUp, Key.W -> Direction.UP
                            Key.DirectionDown, Key.S -> Direction.DOWN
                            Key.DirectionLeft, Key.A -> Direction.LEFT
                            Key.DirectionRight, Key.D -> Direction.RIGHT
                            else -> null
                        }
                        if (direction != null) {
                            onDirection(direction)
                            true
                        } else if (event.key == Key.Enter || event.key == Key.Spacebar) {
                            onToggleRun()
                            true
                        } else false
                    }
                    .focusable()
                    .pointerInput(game.over, running) {
                        var drag = Offset.Zero
                        detectDragGestures(onDragStart = { drag = Offset.Zero }, onDragEnd = {
                            if (drag.getDistance() > 18f && !game.over) {
                                val direction = if (abs(drag.x) > abs(drag.y)) {
                                    if (drag.x > 0) Direction.RIGHT else Direction.LEFT
                                } else if (drag.y > 0) Direction.DOWN else Direction.UP
                                onDirection(direction)
                            }
                        }) { change, amount -> change.consume(); drag += amount }
                    },
                showEyes = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            DirectionPad(onDirection = onDirection, enabled = running && !pausedByPrompt && !game.over)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onToggleRun, enabled = !pausedByPrompt, modifier = Modifier.widthIn(min = 112.dp)) {
                    Text(when {
                        game.over -> atlasText("Rejouer", "Play again", "العب مجدداً")
                        running -> atlasText("Pause", "Pause", "إيقاف مؤقت")
                        else -> atlasText("Continuer", "Resume", "متابعة")
                    })
                }
                OutlinedButton(onClick = onRestart, enabled = !pausedByPrompt, modifier = Modifier.widthIn(min = 112.dp)) {
                    Text(atlasText("Recommencer", "Restart", "إعادة البدء"))
                }
                TextButton(onClick = onSettings, enabled = !running) {
                    Text(atlasText("Paramètres", "Settings", "الإعدادات"))
                }
            }
        }
        val message = when {
            game.over && game.won && dailyMode -> atlasText("Défi relevé !", "Challenge complete!", "اكتمل التحدي!")
            game.over && game.won -> atlasText("Plateau rempli !", "Board filled!", "امتلأت اللوحة!")
            game.over -> atlasText("Partie terminée", "Game over", "انتهت اللعبة")
            pausedByPrompt -> atlasText("Partie en pause", "Game paused", "اللعبة متوقفة مؤقتاً")
            !running -> atlasText("Balayez le plateau ou utilisez les flèches", "Swipe the board or use the arrows", "اسحب على اللوحة أو استخدم الأسهم")
            else -> atlasText("Touchez une direction pour tourner", "Tap a direction to turn", "اضغط اتجاهاً للانعطاف")
        }
        Text(message, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DirectionPad(onDirection: (Direction) -> Unit, enabled: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DirectionButton("↑", enabled) { onDirection(Direction.UP) }
        Row {
            DirectionButton("←", enabled) { onDirection(Direction.LEFT) }
            Spacer(Modifier.size(38.dp))
            DirectionButton("→", enabled) { onDirection(Direction.RIGHT) }
        }
        DirectionButton("↓", enabled) { onDirection(Direction.DOWN) }
    }
}

@Composable
private fun DirectionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(38.dp), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(12.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsPage(
    modifier: Modifier,
    speedMillis: Int,
    wrapWalls: Boolean,
    onSpeed: (Int) -> Unit,
    onWrapWalls: (Boolean) -> Unit,
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(atlasText("Ajustez votre partie", "Tune your game", "اضبط لعبتك"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(atlasText("Vitesse", "Speed", "السرعة"), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(220 to atlasText("Lente", "Slow", "بطيئة"), 170 to atlasText("Classique", "Classic", "كلاسيكية"), 125 to atlasText("Rapide", "Fast", "سريعة")).forEach { (speed, title) ->
                if (speedMillis == speed) Button(onClick = { onSpeed(speed) }, modifier = Modifier.weight(1f)) { Text(title) }
                else OutlinedButton(onClick = { onSpeed(speed) }, modifier = Modifier.weight(1f)) { Text(title) }
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(atlasText("Traverser les bords", "Wrap at edges", "العبور عبر الحواف"), style = MaterialTheme.typography.titleMedium)
                Text(
                    atlasText("Désactivé : toucher un mur termine la partie.", "Off: hitting a wall ends the run.", "عند الإيقاف، يؤدي لمس الجدار إلى إنهاء اللعبة."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = wrapWalls, onCheckedChange = onWrapWalls)
        }
        Text(
            atlasText("Les pommes allongent le serpent. Une couronne apparaît après cinq pommes et donne deux segments bonus.", "Apples grow the snake. A crown appears after five apples and awards two bonus segments.", "التفاح يطيل الثعبان. يظهر التاج بعد خمس تفاحات ويمنح قطعتين إضافيتين."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DailyPage(modifier: Modifier, target: Int, dailyBest: Int, onStart: () -> Unit) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        CrownGlyph(Modifier.size(64.dp))
        Text(atlasText("Défi du jour", "Daily challenge", "تحدي اليوم"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            atlasText("Ramassez $target pommes en une seule partie. Évitez les murs et votre propre queue.", "Collect $target apples in one run. Avoid the walls and your own tail.", "اجمع $target تفاحات في جولة واحدة. تجنب الجدران وذيلك."),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            atlasText("Meilleur score aujourd’hui : $dailyBest", "Today's best: $dailyBest", "أفضل نتيجة اليوم: $dailyBest"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
            Text(atlasText("Commencer le défi", "Start challenge", "ابدأ التحدي"))
        }
    }
}

@Composable
private fun StatBadge(icon: @Composable () -> Unit, label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            icon()
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SnakeBoard(game: SnakeState, modifier: Modifier = Modifier, showEyes: Boolean) {
    Box(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFBDE9A7))
            .border(2.dp, Color(0xFF438847), RoundedCornerShape(18.dp)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.width / BOARD_SIZE
            val light = Color(0xFFBDE9A7)
            val dark = Color(0xFFA8DC91)
            for (y in 0 until BOARD_SIZE) for (x in 0 until BOARD_SIZE) {
                drawRect(if ((x + y) % 2 == 0) light else dark, Offset(x * cell, y * cell), Size(cell, cell))
            }
            game.food.takeIf { it.x in 0 until BOARD_SIZE && it.y in 0 until BOARD_SIZE }?.let { drawApple(it, cell) }
            game.crown?.let { drawCrownAt(it, cell) }
            game.body.forEachIndexed { index, point ->
                val inset = cell * 0.075f
                val topLeft = Offset(point.x * cell + inset, point.y * cell + inset)
                val segmentSize = Size(cell - inset * 2, cell - inset * 2)
                drawRoundRect(Color(0xFF126D91).copy(alpha = 0.22f), Offset(topLeft.x, topLeft.y + cell * 0.055f), segmentSize, CornerRadius(cell * 0.33f))
                drawRoundRect(
                    color = if (index == 0) Color(0xFF21A8C9) else if (index % 2 == 0) Color(0xFF188FB2) else Color(0xFF1683A8),
                    topLeft = topLeft,
                    size = segmentSize,
                    cornerRadius = CornerRadius(cell * 0.34f),
                )
                if (index == 0 && showEyes) drawSnakeEyes(point, cell, game.direction)
            }
        }
        if (game.over) {
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 5.dp,
            ) {
                Text(
                    when {
                        game.won && game.targetScore != null -> atlasText("Défi réussi !", "Challenge complete!", "اكتمل التحدي!")
                        game.won -> atlasText("Bravo !", "Great run!", "أحسنت!")
                        else -> atlasText("Partie terminée", "Game over", "انتهت اللعبة")
                    },
                    Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawApple(cellPoint: Cell, cell: Float) {
    val center = Offset((cellPoint.x + 0.5f) * cell, (cellPoint.y + 0.56f) * cell)
    drawCircle(Color(0xFFB71C32), cell * 0.31f, center)
    drawCircle(Color(0xFFE83C4E), cell * 0.245f, Offset(center.x - cell * 0.055f, center.y - cell * 0.045f))
    drawLine(Color(0xFF684126), Offset(center.x, center.y - cell * 0.22f), Offset(center.x + cell * 0.025f, center.y - cell * 0.4f), strokeWidth = cell * 0.075f)
    val leaf = Path().apply {
        moveTo(center.x + cell * 0.02f, center.y - cell * 0.29f)
        quadraticTo(center.x + cell * 0.25f, center.y - cell * 0.46f, center.x + cell * 0.21f, center.y - cell * 0.24f)
        quadraticTo(center.x + cell * 0.1f, center.y - cell * 0.18f, center.x + cell * 0.02f, center.y - cell * 0.29f)
    }
    drawPath(leaf, Color(0xFF2E8B45))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrownAt(point: Cell, cell: Float) {
    val left = (point.x + 0.17f) * cell
    val top = (point.y + 0.22f) * cell
    val right = (point.x + 0.83f) * cell
    val bottom = (point.y + 0.77f) * cell
    val crown = Path().apply {
        moveTo(left, top + cell * 0.13f)
        lineTo(left + cell * 0.06f, bottom - cell * 0.12f)
        lineTo(right - cell * 0.06f, bottom - cell * 0.12f)
        lineTo(right, top + cell * 0.13f)
        lineTo((point.x + 0.66f) * cell, top + cell * 0.31f)
        lineTo((point.x + 0.5f) * cell, top)
        lineTo((point.x + 0.34f) * cell, top + cell * 0.31f)
        close()
    }
    drawPath(crown, Color(0xFFFFC928))
    drawRoundRect(Color(0xFFE7A914), Offset(left + cell * 0.04f, bottom - cell * 0.13f), Size(right - left - cell * 0.08f, cell * 0.13f), CornerRadius(cell * 0.05f))
    drawCircle(Color.White.copy(alpha = 0.8f), cell * 0.045f, Offset((point.x + 0.5f) * cell, (point.y + 0.38f) * cell))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSnakeEyes(head: Cell, cell: Float, direction: Direction) {
    val (eyeA, eyeB, pupilOffset) = when (direction) {
        Direction.RIGHT -> Triple(Offset(0.68f, 0.34f), Offset(0.68f, 0.66f), Offset(0.035f, 0f))
        Direction.LEFT -> Triple(Offset(0.32f, 0.34f), Offset(0.32f, 0.66f), Offset(-0.035f, 0f))
        Direction.UP -> Triple(Offset(0.34f, 0.32f), Offset(0.66f, 0.32f), Offset(0f, -0.035f))
        Direction.DOWN -> Triple(Offset(0.34f, 0.68f), Offset(0.66f, 0.68f), Offset(0f, 0.035f))
    }
    listOf(eyeA, eyeB).forEach { eye ->
        val center = Offset((head.x + eye.x) * cell, (head.y + eye.y) * cell)
        drawCircle(Color.White, cell * 0.105f, center)
        drawCircle(Color(0xFF173540), cell * 0.052f, Offset(center.x + pupilOffset.x * cell, center.y + pupilOffset.y * cell))
    }
}

@Composable
private fun AppleGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width * 0.5f, size.height * 0.58f)
        drawCircle(Color(0xFFE83C4E), size.minDimension * 0.36f, center)
        drawLine(Color(0xFF684126), Offset(center.x, center.y - size.height * 0.24f), Offset(center.x + size.width * 0.04f, center.y - size.height * 0.46f), strokeWidth = size.minDimension * 0.08f)
        val leaf = Path().apply {
            moveTo(center.x + size.width * 0.02f, center.y - size.height * 0.32f)
            quadraticTo(center.x + size.width * 0.38f, center.y - size.height * 0.52f, center.x + size.width * 0.25f, center.y - size.height * 0.25f)
            close()
        }
        drawPath(leaf, Color(0xFF388E3C))
    }
}

@Composable
private fun CrownGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val left = size.width * 0.12f
        val right = size.width * 0.88f
        val top = size.height * 0.18f
        val bottom = size.height * 0.82f
        val crown = Path().apply {
            moveTo(left, top + size.height * 0.18f)
            lineTo(left + size.width * 0.08f, bottom - size.height * 0.12f)
            lineTo(right - size.width * 0.08f, bottom - size.height * 0.12f)
            lineTo(right, top + size.height * 0.18f)
            lineTo(size.width * 0.68f, top + size.height * 0.34f)
            lineTo(size.width * 0.5f, top)
            lineTo(size.width * 0.32f, top + size.height * 0.34f)
            close()
        }
        drawPath(crown, Color(0xFFFFC928))
        drawRoundRect(Color(0xFFE7A914), Offset(left + size.width * 0.05f, bottom - size.height * 0.12f), Size(right - left - size.width * 0.1f, size.height * 0.14f), CornerRadius(size.minDimension * 0.08f))
    }
}
