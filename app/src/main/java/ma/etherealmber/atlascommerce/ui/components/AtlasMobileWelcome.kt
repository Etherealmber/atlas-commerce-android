package ma.etherealmber.atlascommerce.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.ThemeMode
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.theme.LocalAtlasDarkTheme

private enum class WelcomeStep(val id: String) {
    HELLO("hello"), THEME("theme"), FEATURES("features"), SHOTS("shots"), READY("ready"),
}

private data class WelcomeShot(val title: String, val caption: String, val imageName: String)
private data class WelcomeFeature(val title: String, val body: String, val icon: ImageVector)

private val welcomeShots = listOf(
    WelcomeShot("Accueil", "Trésorerie, stock et alertes en un coup d’œil.", "phone-home-demo"),
    WelcomeShot("Produits", "Catalogue, seuils bas et réapprovisionnement.", "phone-products-demo"),
    WelcomeShot("Scan", "Code-barres → fiche produit en une seconde.", "android-scan-demo"),
    WelcomeShot("Balance", "Recettes, dépenses et caisse au même endroit.", "phone-balance-demo"),
)

private fun welcomeFeatures(role: String): List<WelcomeFeature> = buildList {
    if (Roles.canAdjustStock(role) || Roles.normalize(role) == Roles.STOCK) {
        add(WelcomeFeature("Scanner", "Visez un code-barres : la fiche produit, le stock et l’historique s’ouvrent.", Icons.Outlined.QrCodeScanner))
    }
    add(WelcomeFeature("Produits & stock", "Cherchez, filtrez les ruptures et proposez un ajustement au bureau.", Icons.Outlined.Inventory2))
    if (Roles.canCreateDocuments(role)) {
        add(WelcomeFeature("Vente rapide", "Un ticket, un paiement : la caisse et le stock se mettent à jour.", Icons.Outlined.ShoppingCart))
    }
    add(WelcomeFeature("Atlas Agent", "« Rapport du jour », « Stock bas ? » : l’agent prépare, vous confirmez.", Icons.Outlined.Bot))
}

private data class WelcomePalette(
    val background: Color,
    val card: Color,
    val foreground: Color,
    val muted: Color,
    val line: Color,
    val soft: Color,
    val primary: Color,
    val onPrimary: Color,
)

private val welcomeLight = WelcomePalette(
    Color(0xFFF4F5F7), Color(0xFFFFFFFF), Color(0xFF18181B), Color(0xFF71717A),
    Color(0xFFE4E4E7), Color(0xFFF4F4F5), Color(0xFF18181B), Color(0xFFFAFAFA),
)
private val welcomeDark = WelcomePalette(
    Color(0xFF111113), Color(0xFF1C1C1F), Color(0xFFF4F4F5), Color(0xFFA1A1AA),
    Color(0xFF2A2A2E), Color(0xFF27272A), Color(0xFFF4F4F5), Color(0xFF111113),
)

/** First-login, device-local five-step welcome. Tour completion is handled separately. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AtlasMobileWelcomeScreen(
    isLoggedIn: Boolean = true,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeSelected: (ThemeMode) -> Unit = {},
    onComplete: () -> Unit,
    firstName: String = "",
    role: String = "",
    initialStep: Int = 0,
    onStepChanged: (Int) -> Unit = {},
    onCompleteWithTour: () -> Unit = onComplete,
) {
    @Suppress("UNUSED_VARIABLE")
    val compatibilityLoggedIn = isLoggedIn
    var stepIndex by rememberSaveable(initialStep) { mutableIntStateOf(initialStep.coerceIn(0, 4)) }
    val currentStep = WelcomeStep.entries[stepIndex]
    val animationsEnabled = LocalAtlasAnimationsEnabled.current
    val dark = LocalAtlasDarkTheme.current
    val colors = if (dark) welcomeDark else welcomeLight
    val focusRequester = remember { FocusRequester() }
    val pagerState = rememberPagerState(pageCount = { welcomeShots.size })
    var selectedTheme by remember(themeMode) { mutableIntStateOf(themeMode.ordinal) }
    val selectedThemeMode = ThemeMode.entries.getOrElse(selectedTheme) { ThemeMode.SYSTEM }
    val themeLabel = when (selectedThemeMode) {
        ThemeMode.LIGHT -> "Clair"
        ThemeMode.DARK -> "Sombre"
        ThemeMode.SYSTEM -> "Système"
    }
    val featureItems = remember(role) { welcomeFeatures(role) }
    val forwardLabel = when (currentStep) {
        WelcomeStep.HELLO -> "Commencer"
        WelcomeStep.READY -> "Ouvrir Atlas"
        else -> "Continuer"
    }

    LaunchedEffect(stepIndex) {
        onStepChanged(stepIndex)
        androidx.compose.runtime.withFrameNanos { }
        runCatching { focusRequester.requestFocus() }
    }
    BackHandler(enabled = true) {
        if (stepIndex > 0) stepIndex = (stepIndex - 1).coerceAtLeast(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .pointerInput(stepIndex) {
                if (stepIndex != WelcomeStep.SHOTS.ordinal) {
                    var distance = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { distance = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            distance += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            if (distance < -60f) stepIndex = (stepIndex + 1).coerceAtMost(4)
                            else if (distance > 60f) stepIndex = (stepIndex - 1).coerceAtLeast(0)
                        },
                    )
                }
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AtlasAnimatedBrand(
                height = 28.dp,
                tint = if (dark) Color.White else Color(0xFF0F1115),
                markOnly = true,
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.semantics {
                    progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(stepIndex + 1f, 1f..5f, stepIndex)
                },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WelcomeStep.entries.forEachIndexed { index, _ ->
                    val active = index == stepIndex
                    Box(
                        Modifier
                            .width(if (active) 22.dp else 7.dp)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    active -> colors.foreground
                                    index < stepIndex -> colors.muted
                                    else -> colors.line
                                },
                            ),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(
                modifier = Modifier.heightIn(min = 36.dp).clickable { onComplete() },
                shape = CircleShape,
                color = colors.card,
                border = BorderStroke(1.dp, colors.line),
            ) {
                Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text("Passer", color = colors.foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = stepIndex,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    if (!animationsEnabled) EnterTransition.None togetherWith ExitTransition.None
                    else {
                        val direction = if (targetState >= initialState) 1 else -1
                        (slideInHorizontally(tween(220)) { direction * it / 30 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(220)) { -direction * it / 30 } + fadeOut(tween(220)))
                    }
                },
                label = "atlas-mobile-welcome-step",
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        when (WelcomeStep.entries[page]) {
                            WelcomeStep.HELLO -> {
                                Box(
                                    Modifier.size(62.dp).clip(RoundedCornerShape(20.dp)).background(colors.soft),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AtlasAnimatedBrand(
                                        height = 34.dp,
                                        tint = if (dark) Color.White else Color(0xFF0F1115),
                                        markOnly = true,
                                        animateWhenVisible = true,
                                    )
                                }
                                WelcomeHeading(
                                    text = if (firstName.isBlank()) "Bonjour" else "Bonjour, ${firstName.trim().substringBefore(' ')}",
                                    modifier = Modifier.focusRequester(focusRequester),
                                    colors = colors,
                                )
                                Text(
                                    "Atlas Commerce dans votre poche : scanner, vendre et suivre votre stock, où que vous soyez.",
                                    color = colors.muted,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    listOf("Synchronisé avec le bureau", "Fonctionne hors ligne", "v0.9.3").forEach { label ->
                                        Surface(shape = CircleShape, color = colors.soft, border = BorderStroke(1.dp, colors.line)) {
                                            Text(label, modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = colors.foreground, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                        }
                                    }
                                }
                            }
                            WelcomeStep.THEME -> {
                                WelcomeKicker("Apparence", colors)
                                WelcomeHeading("Choisissez votre thème", Modifier.focusRequester(focusRequester), colors)
                                Text("Modifiable à tout moment dans Profil → Réglages.", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    listOf(
                                        Triple(ThemeMode.LIGHT, "Clair", "Surfaces blanches, idéal en journée") to Icons.Outlined.Sun,
                                        Triple(ThemeMode.DARK, "Sombre", "Charbon neutre, repose les yeux") to Icons.Outlined.Moon,
                                        Triple(ThemeMode.SYSTEM, "Système", "Suit le réglage du téléphone") to Icons.Outlined.Computer,
                                    ).forEach { (choice, icon) ->
                                        val (mode, label, hint) = choice
                                        WelcomeThemeChoice(
                                            mode = mode,
                                            label = label,
                                            hint = hint,
                                            icon = icon,
                                            selected = themeMode == mode,
                                            colors = colors,
                                            onClick = {
                                                selectedTheme = mode.ordinal
                                                onThemeSelected(mode)
                                            },
                                        )
                                    }
                                }
                            }
                            WelcomeStep.FEATURES -> {
                                WelcomeKicker("Sur votre téléphone", colors)
                                WelcomeHeading("L’essentiel, au bout du pouce", Modifier.focusRequester(focusRequester), colors)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    featureItems.forEach { feature ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = colors.card,
                                            border = BorderStroke(1.dp, colors.line),
                                        ) {
                                            Row(
                                                Modifier.fillMaxWidth().padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(11.dp),
                                            ) {
                                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.soft), contentAlignment = Alignment.Center) {
                                                    Icon(feature.icon, contentDescription = null, tint = colors.foreground, modifier = Modifier.size(20.dp))
                                                }
                                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(feature.title, color = colors.foreground, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                                    Text(feature.body, color = colors.muted, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                                Text("Factures, devis, paie et rapports complets restent sur ordinateur.", color = colors.muted, style = MaterialTheme.typography.bodySmall)
                            }
                            WelcomeStep.SHOTS -> {
                                WelcomeKicker("Captures réelles", colors)
                                WelcomeHeading("Voici à quoi ça ressemble", Modifier.focusRequester(focusRequester), colors)
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 380.dp, max = 520.dp),
                                    pageSpacing = 0.dp,
                                    contentPadding = PaddingValues(horizontal = 0.dp),
                                ) { imagePage ->
                                    val shot = welcomeShots[imagePage]
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(themedShot(shot.imageName, dark)),
                                            contentDescription = "Capture réelle : ${shot.title} sur mobile",
                                            modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.card),
                                            contentScale = ContentScale.Fit,
                                        )
                                        Text(shot.title, color = colors.foreground, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                        Text(shot.caption, color = colors.muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    welcomeShots.indices.forEach { index ->
                                        Box(
                                            Modifier.padding(horizontal = 3.dp).size(width = if (pagerState.currentPage == index) 18.dp else 6.dp, height = 6.dp)
                                                .clip(CircleShape).background(if (pagerState.currentPage == index) colors.foreground else colors.line),
                                        )
                                    }
                                }
                            }
                            WelcomeStep.READY -> {
                                Box(Modifier.size(62.dp).clip(CircleShape).background(colors.soft), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.foreground, modifier = Modifier.size(30.dp))
                                }
                                WelcomeHeading("Tout est prêt", Modifier.focusRequester(focusRequester), colors)
                                Text(
                                    buildAnnotatedString {
                                        append("Thème : ")
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.foreground)) { append(themeLabel) }
                                        append(". Vos choix sont enregistrés sur ce téléphone.")
                                    },
                                    color = colors.muted,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Surface(shape = RoundedCornerShape(18.dp), color = colors.card, border = BorderStroke(1.dp, colors.line)) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.soft), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.Compass, contentDescription = null, tint = colors.foreground, modifier = Modifier.size(20.dp))
                                        }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Visite guidée mobile", color = colors.foreground, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "Elle démarre à l’ouverture : accueil, barre du bas, scan, menu rapide, Atlas Agent et profil. C’est une simple démonstration : elle n’ajoute rien à votre base de données et vous pouvez la passer à tout moment.",
                                                color = colors.muted,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    modifier = Modifier.align(Alignment.CenterHorizontally).clickable { onComplete() },
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    border = BorderStroke(1.dp, colors.line),
                                ) {
                                    Text("Ouvrir sans la visite", modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = colors.muted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (stepIndex > 0) {
                Surface(
                    modifier = Modifier.size(52.dp).clickable { stepIndex = (stepIndex - 1).coerceAtLeast(0) },
                    shape = CircleShape,
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.line),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Étape précédente", tint = colors.foreground)
                    }
                }
            }
            Surface(
                modifier = Modifier.weight(1f).height(52.dp).clickable {
                    if (stepIndex < 4) stepIndex += 1 else onCompleteWithTour()
                },
                shape = RoundedCornerShape(16.dp),
                color = colors.primary,
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(forwardLabel, color = colors.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeading(text: String, modifier: Modifier, colors: WelcomePalette) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().focusable().semantics { heading() },
        color = colors.foreground,
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 34.sp),
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun WelcomeKicker(text: String, colors: WelcomePalette) {
    Text(text.uppercase(), color = colors.muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
}

@Composable
private fun WelcomeThemeChoice(
    mode: ThemeMode,
    label: String,
    hint: String,
    icon: ImageVector,
    selected: Boolean,
    colors: WelcomePalette,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = colors.card,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.foreground else colors.line),
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Icon(icon, contentDescription = null, tint = colors.foreground, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = colors.foreground, fontWeight = FontWeight.SemiBold)
                Text(hint, color = colors.muted, style = MaterialTheme.typography.bodySmall)
            }
            Box(Modifier.size(22.dp).clip(CircleShape).background(if (selected) colors.foreground else Color.Transparent), contentAlignment = Alignment.Center) {
                if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = colors.card, modifier = Modifier.size(14.dp))
                else Surface(Modifier.fillMaxSize(), shape = CircleShape, color = Color.Transparent, border = BorderStroke(1.dp, colors.line)) {}
            }
        }
    }
}
