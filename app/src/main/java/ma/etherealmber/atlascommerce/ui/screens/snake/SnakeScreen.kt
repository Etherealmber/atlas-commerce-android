package ma.etherealmber.atlascommerce.ui.screens.snake

import ma.etherealmber.atlascommerce.ui.theme.AtlasIllustrationPalette as ArtPalette
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import ma.etherealmber.atlascommerce.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasFloatingTopBar
import ma.etherealmber.atlascommerce.ui.components.rememberAtlasFloatingTopBarScrollConnection
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
    val scoreViewModel: SnakePersonalBestViewModel = hiltViewModel()
    val personalBestState by scoreViewModel.state.collectAsStateWithLifecycle()
    val bestScore = personalBestState.personalBest
    val preferences = remember(context) {
        context.getSharedPreferences("atlas_snake_progress", Context.MODE_PRIVATE)
    }
    val today = remember { LocalDate.now() }
    val todayKey = remember(today) { today.toString() }
    val dailyTarget = remember(today) { 8 + today.dayOfYear % 5 }
    var bestCrowns by remember(preferences) { mutableIntStateOf(preferences.getInt("best_crowns", 0)) }
    var dailyBest by remember(preferences, todayKey) {
        mutableIntStateOf(if (preferences.getString("daily_date", null) == todayKey) preferences.getInt("daily_best", 0) else 0)
    }
    // v2 avoids carrying forward the old, noticeably faster tick on existing installs.
    var speedMillis by remember(preferences) { mutableIntStateOf(preferences.getInt("snake_speed_ms_v2", 300)) }
    var wrapWalls by remember(preferences) { mutableStateOf(preferences.getBoolean("wrap_walls", false)) }

    var page by remember { mutableStateOf(SnakePage.MENU) }
    var dailyMode by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var game by remember { mutableStateOf(SnakeState(wrapWalls = wrapWalls)) }
    var crownToastVisible by remember { mutableStateOf(false) }
    var observedCrowns by remember { mutableIntStateOf(0) }
    val boardFocusRequester = remember { FocusRequester() }

    fun startGame(daily: Boolean) {
        dailyMode = daily
        game = SnakeState(wrapWalls = wrapWalls, targetScore = if (daily) dailyTarget else null)
        running = true
        page = SnakePage.PLAY
    }

    LaunchedEffect(game.score, game.crowns, dailyMode, bestScore) {
        if (game.score > bestScore) {
            scoreViewModel.recordScore(game.score)
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
    LaunchedEffect(game.crowns) {
        when {
            game.crowns > observedCrowns -> {
                observedCrowns = game.crowns
                crownToastVisible = true
                delay(1_350)
                crownToastVisible = false
            }
            game.crowns < observedCrowns -> {
                observedCrowns = game.crowns
                crownToastVisible = false
            }
        }
    }

    var headerExpanded by remember(page) { mutableStateOf(true) }
    val headerContentInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 100.dp
    val headerScrollConnection = rememberAtlasFloatingTopBarScrollConnection(
        expanded = headerExpanded,
        onExpandedChange = { headerExpanded = it },
    )

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).nestedScroll(headerScrollConnection)) {
        when (page) {
            SnakePage.PLAY -> GameplayPage(
                modifier = Modifier.fillMaxSize(),
                headerInset = headerContentInset,
                game = game,
                bestScore = bestScore,
                bestCrowns = bestCrowns,
                crownToastVisible = crownToastVisible,
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
                modifier = Modifier.fillMaxSize(),
                headerInset = headerContentInset,
                bestScore = bestScore,
                bestCrowns = bestCrowns,
                syncStatus = personalBestState.syncStatus,
                onPlay = { startGame(false) },
                onSettings = { page = SnakePage.SETTINGS },
                onDaily = { page = SnakePage.DAILY },
            )
            SnakePage.SETTINGS -> SettingsPage(
                modifier = Modifier.fillMaxSize(),
                headerInset = headerContentInset,
                speedMillis = speedMillis,
                wrapWalls = wrapWalls,
                onSpeed = { speedMillis = it; preferences.edit().putInt("snake_speed_ms_v2", it).apply() },
                onWrapWalls = {
                    wrapWalls = it
                    preferences.edit().putBoolean("wrap_walls", it).apply()
                    game = game.copy(wrapWalls = it)
                },
            )
            SnakePage.DAILY -> DailyPage(
                modifier = Modifier.fillMaxSize(),
                headerInset = headerContentInset,
                target = dailyTarget,
                dailyBest = dailyBest,
                onStart = { startGame(true) },
            )
        }
        AtlasFloatingTopBar(
            title = when (page) {
                SnakePage.MENU, SnakePage.PLAY -> atlasText("Atlas Snake", "Atlas Snake", "ثعبان Atlas")
                SnakePage.SETTINGS -> atlasText("Paramètres", "Settings", "الإعدادات")
                SnakePage.DAILY -> atlasText("Défi du jour", "Daily challenge", "تحدي اليوم")
            },
            onBack = {
                if (page == SnakePage.MENU) onClose() else {
                    running = false
                    page = SnakePage.MENU
                }
            },
            expanded = headerExpanded,
            onCollapsedClick = { headerExpanded = true },
            modifier = Modifier.align(Alignment.TopCenter).zIndex(1f),
        )
    }
}

@Composable
private fun SnakeSyncStatusText(status: SnakeScoreSyncStatus) {
    val message = when (status) {
        SnakeScoreSyncStatus.GUEST -> atlasText("Enregistré sur cet appareil", "Saved on this device", "محفوظ على هذا الجهاز")
        SnakeScoreSyncStatus.CHECKING -> atlasText("Vérification du record…", "Checking personal best…", "جارٍ التحقق من أفضل نتيجة…")
        SnakeScoreSyncStatus.PENDING -> atlasText("Synchronisation du record…", "Syncing personal best…", "جارٍ مزامنة أفضل نتيجة…")
        SnakeScoreSyncStatus.SYNCED -> atlasText("Record synchronisé", "Personal best synced", "تمت مزامنة أفضل نتيجة")
        SnakeScoreSyncStatus.UNAVAILABLE -> atlasText("Synchronisation indisponible · record conservé ici", "Sync unavailable · best kept on this device", "المزامنة غير متاحة · النتيجة محفوظة على هذا الجهاز")
    }
    Text(
        text = message,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun MenuPage(
    modifier: Modifier,
    headerInset: Dp,
    bestScore: Int,
    bestCrowns: Int,
    syncStatus: SnakeScoreSyncStatus,
    onPlay: () -> Unit,
    onSettings: () -> Unit,
    onDaily: () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= 760.dp) {
            Row(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f).widthIn(max = 480.dp).fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(Modifier.height(headerInset))
                    Text(
                        atlasText("Le serpent Atlas", "Atlas Snake", "ثعبان Atlas"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        atlasText("Ramassez les logos Atlas et le logo Etherealmber !", "Collect Atlas logos and the Etherealmber logo!", "اجمع شعارات Atlas وشعار Etherealmber!"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp,
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            SnakeScoreLine(
                                icon = { PersonalBestCrown(Modifier.size(24.dp)) },
                                label = atlasText("Record perso", "Personal best", "أفضل نتيجة شخصية"),
                                value = bestScore,
                            )
                            SnakeScoreLine(
                                icon = { EtherealmberLogoGlyph(Modifier.size(22.dp), size = 22.dp) },
                                label = atlasText("Etherealmber", "Etherealmber", "Etherealmber"),
                                value = bestCrowns,
                            )
                        }
                    }
                    SnakeSyncStatusText(syncStatus)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                        Text(atlasText("Jouer", "Play", "العب"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                            Text(atlasText("Paramètres", "Settings", "الإعدادات"))
                        }
                        OutlinedButton(onClick = onDaily, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                            Text(atlasText("Défi du jour", "Daily challenge", "تحدي اليوم"))
                        }
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                        val previewSide = minOf(maxWidth, maxHeight, 620.dp)
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
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(headerInset))
                Text(
                    atlasText("Le serpent Atlas", "Atlas Snake", "ثعبان Atlas"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    atlasText("Ramassez les logos Atlas et le logo Etherealmber !", "Collect Atlas logos and the Etherealmber logo!", "اجمع شعارات Atlas وشعار Etherealmber!"),
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
                            StatBadge(
                                icon = { PersonalBestCrown(Modifier.size(24.dp)) },
                                label = atlasText("Record perso", "Personal best", "أفضل نتيجة شخصية"),
                                value = bestScore,
                            )
                            StatBadge(
                                icon = { EtherealmberLogoGlyph(Modifier.size(22.dp), size = 22.dp) },
                                label = atlasText("Etherealmber", "Etherealmber", "Etherealmber"),
                                value = bestCrowns,
                            )
                        }
                        SnakeSyncStatusText(syncStatus)
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
    }
}

@Composable
private fun GameplayPage(
    modifier: Modifier,
    headerInset: Dp,
    game: SnakeState,
    bestScore: Int,
    bestCrowns: Int,
    crownToastVisible: Boolean,
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
    val statusMessage = when {
        game.over && game.won && dailyMode -> atlasText("Défi relevé !", "Challenge complete!", "اكتمل التحدي!")
        game.over && game.won -> atlasText("Plateau rempli !", "Board filled!", "امتلأت اللوحة!")
        game.over -> atlasText("Partie terminée", "Game over", "انتهت اللعبة")
        pausedByPrompt -> atlasText("Partie en pause", "Game paused", "اللعبة متوقفة مؤقتاً")
        !running -> atlasText("Balayez, utilisez le clavier ou les actions TalkBack", "Swipe, use the keyboard, or TalkBack actions", "اسحب أو استخدم لوحة المفاتيح أو إجراءات TalkBack")
        else -> atlasText("Balayez pour tourner · actions TalkBack disponibles", "Swipe to turn · TalkBack actions available", "اسحب للانعطاف · إجراءات TalkBack متاحة")
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= 760.dp) {
            Row(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.width(280.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(headerInset))
                    Text(
                        atlasText("Atlas Snake", "Atlas Snake", "ثعبان Atlas"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Button(
                        onClick = onToggleRun,
                        enabled = !pausedByPrompt,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Text(
                            when {
                                game.over -> atlasText("Rejouer", "Play again", "العب مجدداً")
                                running -> atlasText("Pause", "Pause", "إيقاف مؤقت")
                                else -> atlasText("Continuer", "Resume", "متابعة")
                            },
                            maxLines = 1,
                        )
                    }
                    OutlinedButton(
                        onClick = onRestart,
                        enabled = !pausedByPrompt,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Text(atlasText("Recommencer", "Restart", "إعادة البدء"), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onSettings,
                        enabled = !running,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(atlasText("Paramètres", "Settings", "الإعدادات"), maxLines = 1)
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SnakeScoreLine(
                                icon = { AtlasLogoGlyph(Modifier.size(22.dp)) },
                                label = atlasText("Score", "Score", "النقاط"),
                                value = game.score,
                            )
                            SnakeScoreLine(
                                icon = { EtherealmberLogoGlyph(Modifier.size(22.dp), size = 22.dp) },
                                label = atlasText("Etherealmber", "Etherealmber", "Etherealmber"),
                                value = game.crowns,
                            )
                            HorizontalDivider()
                            SnakeScoreLine(
                                icon = { PersonalBestCrown(Modifier.size(22.dp)) },
                                label = atlasText("Record perso", "Personal best", "أفضل نتيجة شخصية"),
                                value = bestScore,
                            )
                        }
                    }
                    if (dailyMode) {
                        Text(
                            atlasText("Défi : $dailyTarget logos", "Daily goal: $dailyTarget Atlas logos", "الهدف اليومي: $dailyTarget شعاراً"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                SnakeBoardViewport(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    maxBoardSide = 720.dp,
                    game = game,
                    crownToastVisible = crownToastVisible,
                    running = running,
                    boardFocusRequester = boardFocusRequester,
                    onDirection = onDirection,
                    onToggleRun = onToggleRun,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(headerInset))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onToggleRun,
                        enabled = !pausedByPrompt,
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    ) {
                        Text(
                            when {
                                game.over -> atlasText("Rejouer", "Play again", "العب مجدداً")
                                running -> atlasText("Pause", "Pause", "إيقاف مؤقت")
                                else -> atlasText("Continuer", "Resume", "متابعة")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                    OutlinedButton(
                        onClick = onRestart,
                        enabled = !pausedByPrompt,
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    ) {
                        Text(atlasText("Recommencer", "Restart", "إعادة البدء"), style = MaterialTheme.typography.labelMedium, maxLines = 2)
                    }
                    TextButton(
                        onClick = onSettings,
                        enabled = !running,
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(atlasText("Paramètres", "Settings", "الإعدادات"), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatBadge(icon = { AtlasLogoGlyph(Modifier.size(22.dp)) }, label = atlasText("Atlas", "Atlas", "Atlas"), value = game.score)
                    StatBadge(
                        icon = { EtherealmberLogoGlyph(Modifier.size(22.dp), size = 22.dp) },
                        label = atlasText("Etherealmber", "Etherealmber", "Etherealmber"),
                        value = game.crowns,
                    )
                    StatBadge(
                        icon = { PersonalBestCrown(Modifier.size(22.dp)) },
                        label = atlasText("Record perso", "Personal best", "أفضل نتيجة شخصية"),
                        value = bestScore,
                    )
                }
                if (dailyMode) {
                    Text(
                        atlasText("Défi : $dailyTarget logos", "Daily goal: $dailyTarget Atlas logos", "الهدف اليومي: $dailyTarget شعاراً"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                SnakeBoardViewport(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 500.dp),
                    maxBoardSide = 500.dp,
                    game = game,
                    crownToastVisible = crownToastVisible,
                    running = running,
                    boardFocusRequester = boardFocusRequester,
                    onDirection = onDirection,
                    onToggleRun = onToggleRun,
                )
                Text(
                    statusMessage,
                    Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SnakeBoardViewport(
    modifier: Modifier,
    maxBoardSide: Dp,
    game: SnakeState,
    crownToastVisible: Boolean,
    running: Boolean,
    boardFocusRequester: FocusRequester,
    onDirection: (Direction) -> Unit,
    onToggleRun: () -> Unit,
) {
    val boardDescription = atlasText(
        "Plateau du jeu Snake. Utilisez les actions TalkBack pour diriger le serpent.",
        "Snake game board. Use TalkBack actions to steer the snake.",
        "لوحة لعبة الثعبان. استخدم إجراءات TalkBack لتوجيه الثعبان.",
    )
    val moveUpLabel = atlasText("Monter", "Move up", "تحرك للأعلى")
    val moveDownLabel = atlasText("Descendre", "Move down", "تحرك للأسفل")
    val moveLeftLabel = atlasText("Aller à gauche", "Move left", "تحرك إلى اليسار")
    val moveRightLabel = atlasText("Aller à droite", "Move right", "تحرك إلى اليمين")
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val boardSide = minOf(maxWidth, maxHeight, maxBoardSide)
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
                }
                .semantics {
                    contentDescription = boardDescription
                    customActions = listOf(
                        CustomAccessibilityAction(moveUpLabel) { onDirection(Direction.UP); true },
                        CustomAccessibilityAction(moveDownLabel) { onDirection(Direction.DOWN); true },
                        CustomAccessibilityAction(moveLeftLabel) { onDirection(Direction.LEFT); true },
                        CustomAccessibilityAction(moveRightLabel) { onDirection(Direction.RIGHT); true },
                    )
                },
            showEyes = true,
        )
        CrownCollectedToast(
            visible = crownToastVisible,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
        )
    }
}

@Composable
private fun SnakeScoreLine(icon: @Composable () -> Unit, label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon()
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CrownCollectedToast(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(initialScale = 0.88f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.58f)),
            shadowElevation = 6.dp,
        ) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                EtherealmberLogoGlyph(Modifier.size(19.dp), size = 19.dp)
                Spacer(Modifier.width(7.dp))
                Text(
                    atlasText("Etherealmber +2", "Etherealmber +2", "Etherealmber +٢"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SettingsPage(
    modifier: Modifier,
    headerInset: Dp,
    speedMillis: Int,
    wrapWalls: Boolean,
    onSpeed: (Int) -> Unit,
    onWrapWalls: (Boolean) -> Unit,
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Spacer(Modifier.height(headerInset))
        Text(atlasText("Ajustez votre partie", "Tune your game", "اضبط لعبتك"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(atlasText("Vitesse", "Speed", "السرعة"), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(380 to atlasText("Lente", "Slow", "بطيئة"), 300 to atlasText("Classique", "Classic", "كلاسيكية"), 220 to atlasText("Rapide", "Fast", "سريعة")).forEach { (speed, title) ->
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
            atlasText("Les logos Atlas allongent le serpent. Le logo Etherealmber apparaît après cinq logos et donne deux segments bonus.", "Atlas logos grow the snake. The Etherealmber logo appears after five logos and awards two bonus segments.", "شعارات Atlas تطيل الثعبان. يظهر شعار Etherealmber بعد خمسة شعارات ويمنح قطعتين إضافيتين."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DailyPage(modifier: Modifier, headerInset: Dp, target: Int, dailyBest: Int, onStart: () -> Unit) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(headerInset))
        EtherealmberLogoGlyph(Modifier.size(64.dp), size = 64.dp)
        Text(atlasText("Défi du jour", "Daily challenge", "تحدي اليوم"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            atlasText("Ramassez $target logos Atlas en une seule partie. Évitez les murs et votre propre queue.", "Collect $target Atlas logos in one run. Avoid walls and your own tail.", "اجمع $target من شعارات Atlas في جولة واحدة. تجنب الجدران وذيلك."),
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
private fun PersonalBestCrown(modifier: Modifier = Modifier) {
    val description = atlasText(
        "Couronne du record personnel",
        "Personal-best crown",
        "تاج أفضل نتيجة شخصية",
    )
    Text(
        text = "👑",
        modifier = modifier.semantics {
            contentDescription = description
        },
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
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
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceContainerLow)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(18.dp)),
    ) {
        val cellSize = maxWidth / BOARD_SIZE
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.width / BOARD_SIZE
            val light = colors.surfaceContainerLow
            val dark = colors.surfaceContainerHigh
            for (y in 0 until BOARD_SIZE) for (x in 0 until BOARD_SIZE) {
                drawRect(if ((x + y) % 2 == 0) light else dark, Offset(x * cell, y * cell), Size(cell, cell))
            }
            game.body.forEachIndexed { index, point ->
                val inset = cell * 0.075f
                val topLeft = Offset(point.x * cell + inset, point.y * cell + inset)
                val segmentSize = Size(cell - inset * 2, cell - inset * 2)
                drawRoundRect(colors.onSurface.copy(alpha = 0.12f), Offset(topLeft.x, topLeft.y + cell * 0.055f), segmentSize, CornerRadius(cell * 0.33f))
                drawRoundRect(
                    color = if (index == 0) colors.tertiary else if (index % 2 == 0) colors.primary else colors.primary.copy(alpha = 0.82f),
                    topLeft = topLeft,
                    size = segmentSize,
                    cornerRadius = CornerRadius(cell * 0.34f),
                )
                if (index == 0 && showEyes) drawSnakeEyes(point, cell, game.direction)
            }
        }
        if (game.food.x in 0 until BOARD_SIZE && game.food.y in 0 until BOARD_SIZE) {
            val tokenSize = cellSize * 0.84f
            Box(
                modifier = Modifier
                    .offset(
                        x = cellSize * game.food.x + (cellSize - tokenSize) / 2,
                        y = cellSize * game.food.y + (cellSize - tokenSize) / 2,
                    )
                    .size(tokenSize)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(colors.secondaryContainer)
                    .border(1.dp, colors.outlineVariant, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = atlasText("Logo Atlas à collecter", "Atlas logo to collect", "شعار Atlas للجمع"),
                    tint = colors.onSecondaryContainer,
                    modifier = Modifier.fillMaxSize().padding(tokenSize * 0.14f),
                )
            }
        }
        game.crown?.takeIf { it.x in 0 until BOARD_SIZE && it.y in 0 until BOARD_SIZE }?.let { point ->
            val tokenSize = cellSize * 0.84f
            EtherealmberLogoGlyph(
                modifier = Modifier.offset(
                    x = cellSize * point.x + (cellSize - tokenSize) / 2,
                    y = cellSize * point.y + (cellSize - tokenSize) / 2,
                ).size(tokenSize),
                size = tokenSize,
                contentDescription = atlasText(
                    "Logo Etherealmber à collecter",
                    "Etherealmber logo to collect",
                    "شعار Etherealmber للجمع",
                ),
            )
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSnakeEyes(head: Cell, cell: Float, direction: Direction) {
    val (eyeA, eyeB, pupilOffset) = when (direction) {
        Direction.RIGHT -> Triple(Offset(0.68f, 0.34f), Offset(0.68f, 0.66f), Offset(0.035f, 0f))
        Direction.LEFT -> Triple(Offset(0.32f, 0.34f), Offset(0.32f, 0.66f), Offset(-0.035f, 0f))
        Direction.UP -> Triple(Offset(0.34f, 0.32f), Offset(0.66f, 0.32f), Offset(0f, -0.035f))
        Direction.DOWN -> Triple(Offset(0.34f, 0.68f), Offset(0.66f, 0.68f), Offset(0f, 0.035f))
    }
    listOf(eyeA, eyeB).forEach { eye ->
        val center = Offset((head.x + eye.x) * cell, (head.y + eye.y) * cell)
        drawCircle(ArtPalette.white, cell * 0.105f, center)
        drawCircle(ArtPalette.snakePupil, cell * 0.052f, Offset(center.x + pupilOffset.x * cell, center.y + pupilOffset.y * cell))
    }
}

@Composable
private fun AtlasLogoGlyph(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(colors.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = colors.onSecondaryContainer,
            modifier = Modifier.fillMaxSize().padding(2.dp),
        )
    }
}

@Composable
private fun EtherealmberLogoGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    contentDescription: String? = null,
) {
    AtlasAvatar(
        avatarUrl = AppDetails.GITHUB_AVATAR_URL,
        label = AppDetails.GITHUB_HANDLE,
        modifier = modifier,
        size = size,
        borderColor = MaterialTheme.colorScheme.tertiary,
        clickContentDescription = contentDescription ?: atlasText(
            "Logo Etherealmber",
            "Etherealmber logo",
            "شعار Etherealmber",
        ),
    )
}
