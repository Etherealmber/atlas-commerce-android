package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

private data class AtlasWelcomeSlide(
    val title: Triple<String, String, String>,
    val subtitle: Triple<String, String, String>,
)

private val welcomeSlides = listOf(
    AtlasWelcomeSlide(
        title = Triple("Vivez votre espace de travail", "Hang out on your workspace", "عِش مساحة عملك"),
        subtitle = Triple(
            "Les ventes et les stocks évoluent en temps réel avec votre équipe — sans réunions inutiles.",
            "Realtime stock and sales activity with your team — no unnecessary meetings.",
            "تابع حركة المبيعات والمخزون مباشرةً مع فريقك — دون اجتماعات غير ضرورية.",
        ),
    ),
    AtlasWelcomeSlide(
        title = Triple("Restez proches, en temps réel", "Humanise connection", "تواصل إنساني مباشر"),
        subtitle = Triple(
            "De petites attentions pour rester proches : une présence partagée entre tous vos points de vente.",
            "Stay close through simple, subtle moments — real-time presence across all your outlets.",
            "ابقوا على تواصل بلحظات بسيطة ولطيفة — حضور مباشر في جميع فروعك.",
        ),
    ),
    AtlasWelcomeSlide(
        title = Triple("Créez votre propre espace", "Build your own space", "أنشئ مساحتك الخاصة"),
        subtitle = Triple(
            "À votre image : des cartes personnalisées pour le stock, les ventes et l'encaissement rapide.",
            "It's yours. Custom layout cards for inventory, sales, and rapid billing.",
            "مساحتك كما تحب: بطاقات مخصصة للمخزون والمبيعات والفوترة السريعة.",
        ),
    ),
    AtlasWelcomeSlide(
        title = Triple("Des rapports qui restent", "Reports that last", "تقارير تبقى معك"),
        subtitle = Triple(
            "Chaque journée se résume sans effort. Votre récapitulatif quotidien, sans saisie manuelle.",
            "Each business day is summarised in an effortless daily recap. Zero manual effort.",
            "ملخص يومي لكل يوم عمل، بكل سهولة ومن دون أي إدخال يدوي.",
        ),
    ),
)

private val AtlasCyan = Color(0xFF62D9F5)
private val AtlasBlue = Color(0xFF6AA8FF)
private val AtlasPurple = Color(0xFFAA8BFF)
private val AtlasMagenta = Color(0xFFF17CCB)
private val AtlasMint = Color(0xFF69E0C2)
private val AtlasAmber = Color(0xFFFFC66D)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AtlasWelcomeOnboardingScreen(
    isLoggedIn: Boolean = false,
    onComplete: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { welcomeSlides.size })
    val animationsEnabled = LocalAtlasAnimationsEnabled.current
    val language = LocalAtlasLanguage.current
    val scope = rememberCoroutineScope()
    var showTutorial by rememberSaveable { mutableStateOf(false) }
    var tutorialStep by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(showTutorial, pagerState.currentPage, pagerState.isScrollInProgress, animationsEnabled) {
        if (!showTutorial && animationsEnabled && !pagerState.isScrollInProgress && pagerState.currentPage < welcomeSlides.lastIndex) {
            delay(8_000)
            if (!showTutorial && !pagerState.isScrollInProgress && pagerState.currentPage < welcomeSlides.lastIndex) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    val escapeLabel = if (isLoggedIn) {
        atlasText("Passer", "Skip", "تخطي")
    } else {
        atlasText("Se connecter", "Log in", "تسجيل الدخول")
    }
    val indicatorCount = if (showTutorial) 3 else welcomeSlides.size
    val indicatorPage = if (showTutorial) tutorialStep else pagerState.currentPage

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AtlasMark(size = 28.dp)
                Spacer(Modifier.width(8.dp))
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(indicatorCount) { page ->
                        val selected = page == indicatorPage
                        val dotWidth by animateDpAsState(
                            if (selected) 26.dp else 7.dp,
                            spring(dampingRatio = 0.78f, stiffness = 500f),
                            label = "onboarding_dot_width_$showTutorial-$page",
                        )
                        val dotColor by animateColorAsState(
                            when {
                                selected -> AtlasCyan
                                page < indicatorPage -> AtlasPurple.copy(alpha = 0.72f)
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            tween(260),
                            label = "onboarding_dot_color_$showTutorial-$page",
                        )
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .width(dotWidth)
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .then(
                                    if (showTutorial) Modifier
                                    else Modifier.clickable { scope.launch { pagerState.animateScrollToPage(page) } },
                                )
                                .semantics { contentDescription = "${page + 1} / $indicatorCount" },
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.heightIn(min = 44.dp).clickable(onClick = onComplete),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Text(
                        escapeLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }

            if (showTutorial) {
                WelcomeTutorialContent(
                    step = tutorialStep,
                    animationsEnabled = animationsEnabled,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (tutorialStep > 0) {
                        TextButton(onClick = { tutorialStep = (tutorialStep - 1).coerceAtLeast(0) }) {
                            Text(atlasText("Retour", "Back", "السابق"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = {
                            if (tutorialStep < 2) tutorialStep += 1 else onComplete()
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            if (tutorialStep < 2) atlasText("Suivant", "Next", "التالي")
                            else atlasText("Terminer", "Finish tour", "إنهاء الجولة"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        atlasText("Passer le tutoriel", "Skip tutorial", "تخطي الجولة"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    key = { it },
                    beyondViewportPageCount = 1,
                ) { page ->
                    WelcomeSlideContent(page, language)
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (pagerState.currentPage > 0) {
                        TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                            Text(atlasText("Précédent", "Back", "السابق"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = {
                            if (pagerState.currentPage < welcomeSlides.lastIndex) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                tutorialStep = 0
                                showTutorial = true
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            if (pagerState.currentPage == welcomeSlides.lastIndex) atlasText("Découvrir l'application", "Take a quick tour", "جولة سريعة")
                            else atlasText("Continuer", "Continue", "متابعة"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Text(
                text = atlasText(
                    "En rejoignant Atlas, vous acceptez nos conditions d'utilisation et notre politique de confidentialité.",
                    "By joining Atlas, you acknowledge our Terms of Service and Privacy Policy.",
                    "بانضمامك إلى Atlas، فإنك توافق على شروط الخدمة وسياسة الخصوصية.",
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WelcomeTutorialContent(
    step: Int,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val safeStep = step.coerceIn(0, 2)
    val titles = listOf(
        atlasText("Votre aperçu du jour", "Your daily snapshot", "ملخص يومك"),
        atlasText("Gardez le stock en vue", "Keep stock in view", "تابع المخزون بسهولة"),
        atlasText("Vendez en quelques gestes", "Sell in a few taps", "أتمم البيع بخطوات بسيطة"),
    )
    val descriptions = listOf(
        atlasText("Repérez d'un coup d'œil les ventes et les alertes importantes.", "Spot sales and important alerts at a glance.", "اطّلع بسرعة على المبيعات والتنبيهات المهمة."),
        atlasText("Consultez les articles et leurs niveaux de stock avant de réapprovisionner.", "Check products and stock levels before you restock.", "راجع المنتجات ومستويات مخزونها قبل إعادة التزويد."),
        atlasText("Prévisualisez une facture et le total avant de confirmer une vraie vente.", "Review a receipt and total before confirming a real sale.", "راجع الإيصال والإجمالي قبل تأكيد عملية بيع حقيقية."),
    )

    Column(
        modifier.padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().weight(1f).heightIn(min = 230.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (animationsEnabled) {
                AnimatedContent(targetState = safeStep, label = "tutorial_demo_step") { visibleStep ->
                    TutorialDemoCard(visibleStep, Modifier.fillMaxWidth())
                }
            } else {
                TutorialDemoCard(safeStep, Modifier.fillMaxWidth())
            }
        }
        Surface(
            shape = CircleShape,
            color = AtlasCyan.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, AtlasCyan.copy(alpha = 0.3f)),
        ) {
            Text(
                atlasText("APERÇU · DONNÉES FICTIVES", "DEMO · SAMPLE DATA", "عرض · بيانات تجريبية"),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            titles[safeStep],
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            descriptions[safeStep],
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun TutorialDemoCard(step: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(horizontal = 8.dp).widthIn(max = 420.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AtlasMark(size = 22.dp)
                Spacer(Modifier.width(8.dp))
                Text("Atlas Commerce", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = when (step) {
                        0 -> Icons.Outlined.AutoAwesome
                        1 -> Icons.Outlined.Inventory2
                        else -> Icons.Outlined.ReceiptLong
                    },
                    contentDescription = null,
                    tint = AtlasCyan,
                    modifier = Modifier.size(20.dp),
                )
            }
            when (step) {
                0 -> {
                    Text(atlasText("Aujourd'hui", "Today", "اليوم"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        TutorialMetricCard(atlasText("Ventes", "Sales", "المبيعات"), "12 480 DH", AtlasCyan, Modifier.weight(1f))
                        TutorialMetricCard(atlasText("Alertes stock", "Low stock", "تنبيه المخزون"), "4", AtlasAmber, Modifier.weight(1f))
                    }
                    TutorialInfoRow(atlasText("Factures du jour", "Today's invoices", "فواتير اليوم"), "8")
                }
                1 -> {
                    Text(atlasText("Stock récent", "Recent stock", "المخزون مؤخراً"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TutorialProductRow(atlasText("Café en grains", "Coffee beans", "حبوب القهوة"), atlasText("12 unités", "12 units", "12 وحدة"), AtlasCyan)
                    TutorialProductRow(atlasText("Gobelets papier", "Paper cups", "أكواب ورقية"), atlasText("4 unités · bas", "4 units · low", "4 وحدات · منخفض"), AtlasAmber)
                    TutorialProductRow(atlasText("Eau pétillante", "Sparkling water", "مياه غازية"), atlasText("28 unités", "28 units", "28 وحدة"), AtlasPurple)
                }
                else -> {
                    Text(atlasText("Aperçu du ticket", "Receipt preview", "معاينة الإيصال"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TutorialInfoRow(atlasText("2 × Mug Atlas", "2 × Atlas mug", "2 × كوب Atlas"), "180 DH")
                    TutorialInfoRow(atlasText("Sous-total", "Subtotal", "المجموع الفرعي"), "180 DH")
                    Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            atlasText("Aperçu uniquement · aucune vente enregistrée", "Preview only · no sale is recorded", "معاينة فقط · لم يتم تسجيل أي بيع"),
                            modifier = Modifier.fillMaxWidth().padding(11.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialMetricCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun TutorialInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TutorialProductRow(name: String, stock: String, accent: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = 0.14f)) {
            Icon(Icons.Outlined.Inventory2, null, Modifier.padding(8.dp).size(18.dp), tint = accent)
        }
        Spacer(Modifier.width(10.dp))
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        Text(stock, style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WelcomeSlideContent(page: Int, language: AppLanguage) {
    val slide = welcomeSlides[page]
    Column(
        Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 215.dp, max = 330.dp),
            contentAlignment = Alignment.Center,
        ) {
            WelcomeIllustration(page)
        }
        Spacer(Modifier.height(8.dp))
        AtlasEntrance(delayMillis = 70, offsetY = 14.dp, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = chooseLanguage(slide.title, language),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 27.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(9.dp))
        AtlasEntrance(delayMillis = 150, offsetY = 10.dp, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = chooseLanguage(slide.subtitle, language),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 23.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(14.dp))
    }
}

private fun chooseLanguage(copy: Triple<String, String, String>, language: AppLanguage) = when (language) {
    AppLanguage.FRENCH -> copy.first
    AppLanguage.ENGLISH -> copy.second
    AppLanguage.ARABIC -> copy.third
}

@Composable
private fun WelcomeIllustration(page: Int) {
    val motion = LocalAtlasAnimationsEnabled.current
    val transition = rememberInfiniteTransition(label = "onboarding_illustration_$page")
    val bob by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "illustration_bob_$page",
    )
    val drop by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "illustration_drop_$page",
    )
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.51f)
            drawCircle(AtlasBlue.copy(alpha = 0.055f), size.minDimension * 0.38f, center)
            drawCircle(AtlasPurple.copy(alpha = 0.06f), size.minDimension * 0.28f, center)
            drawCircle(
                color = AtlasCyan.copy(alpha = 0.18f),
                radius = size.minDimension * 0.37f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            for (index in 0..7) {
                val angle = index * (Math.PI / 4.0)
                val radius = size.minDimension * 0.43f
                drawCircle(
                    color = listOf(AtlasCyan, AtlasPurple, AtlasMagenta, AtlasBlue)[index % 4].copy(alpha = 0.76f),
                    radius = if (index % 2 == 0) 3.dp.toPx() else 2.dp.toPx(),
                    center = Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * radius,
                        center.y + kotlin.math.sin(angle).toFloat() * radius,
                    ),
                )
            }
        }
        when (page) {
            0 -> WorkspaceIllustration(bob, drop, motion)
            1 -> ConnectionIllustration(bob, motion)
            2 -> DashboardIllustration(bob, motion)
            else -> RecapIllustration(bob, motion)
        }
    }
}

@Composable
private fun WorkspaceIllustration(bob: Float, drop: Float, motion: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.offset(y = 12.dp).size(width = 154.dp, height = 210.dp),
            shape = RoundedCornerShape(27.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AtlasMark(size = 17.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Atlas", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Outlined.AutoAwesome, null, tint = AtlasCyan, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(AtlasMint))
                }
                Surface(shape = RoundedCornerShape(13.dp), color = AtlasBlue.copy(alpha = 0.12f)) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text(atlasText("AUJOURD’HUI", "TODAY", "اليوم"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("12 480 DH", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
                repeat(2) { index ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(23.dp).clip(RoundedCornerShape(8.dp)).background(if (index == 0) AtlasPurple.copy(.2f) else AtlasCyan.copy(.2f)))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Box(Modifier.width(67.dp).height(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .16f)))
                            Spacer(Modifier.height(5.dp))
                            Box(Modifier.width(43.dp).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .09f)))
                        }
                    }
                }
            }
        }
        WidgetDropCard(
            icon = Icons.Outlined.ReceiptLong,
            label = atlasText("Facture", "Invoice", "فاتورة"),
            accent = AtlasPurple,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp, top = 42.dp).graphicsLayer {
                translationY = if (motion) -24f + drop * 22f + bob * 1.2f else 0f
                rotationZ = -7f
            },
        )
        WidgetDropCard(
            icon = Icons.Outlined.NotificationsActive,
            label = atlasText("Alerte stock", "Stock alert", "تنبيه المخزون"),
            accent = AtlasCyan,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 0.dp, top = 72.dp).graphicsLayer {
                val staggeredDrop = ((drop - 0.2f) / 0.8f).coerceIn(0f, 1f)
                translationY = if (motion) -24f + staggeredDrop * 22f - bob else 0f
                rotationZ = 6f
            },
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).graphicsLayer {
                translationY = if (motion) bob else 0f
                rotationZ = -2f
            },
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, AtlasMagenta.copy(alpha = .65f)),
            shadowElevation = 8.dp,
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ReceiptLong, null, tint = AtlasMagenta, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(atlasText("Ticket de caisse", "POS receipt", "إيصال نقطة البيع"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }

    }
}

@Composable
private fun WidgetDropCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = .62f)),
        shadowElevation = 7.dp,
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun ConnectionIllustration(bob: Float, motion: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val orbitColor = MaterialTheme.colorScheme.outlineVariant
        Canvas(Modifier.size(245.dp)) {
            drawCircle(orbitColor.copy(alpha = .8f), radius = size.minDimension * .36f, style = Stroke(1.2.dp.toPx()))
            drawCircle(AtlasPurple.copy(alpha = .45f), radius = size.minDimension * .45f, style = Stroke(1.dp.toPx()))
        }
        Surface(
            Modifier.size(116.dp).graphicsLayer { translationY = if (motion) bob else 0f},
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, AtlasCyan.copy(alpha = .7f)),
            shadowElevation = 14.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                AtlasMark(size = 58.dp)
                Box(Modifier.align(Alignment.BottomEnd).padding(end = 11.dp, bottom = 9.dp).size(13.dp).clip(CircleShape).background(AtlasMint))
            }
        }
        IllustratedAvatarBadge(0, AtlasCyan, Modifier.align(Alignment.TopStart).padding(start = 44.dp, top = 26.dp).graphicsLayer { scaleX = if (motion) 1f + bob / 45f else 1f; scaleY = if (motion) 1f + bob / 45f else 1f })
        IllustratedAvatarBadge(1, AtlasMagenta, Modifier.align(Alignment.TopEnd).padding(end = 48.dp, top = 34.dp).graphicsLayer { scaleX = if (motion) 1f - bob / 45f else 1f; scaleY = if (motion) 1f - bob / 45f else 1f })
        IllustratedAvatarBadge(2, AtlasAmber, Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 34.dp).graphicsLayer { translationY = if (motion) bob / 2f else 0f })
        IllustratedAvatarBadge(3, AtlasPurple, Modifier.align(Alignment.BottomEnd).padding(end = 44.dp, bottom = 26.dp).graphicsLayer { translationY = if (motion) -bob else 0f })
        Surface(
            Modifier.align(Alignment.TopCenter).padding(top = 1.dp),
            shape = RoundedCornerShape(20.dp),
            color = AtlasMint.copy(alpha = .18f),
            border = BorderStroke(1.dp, AtlasMint.copy(alpha = .4f)),
        ) {
            Text(atlasText("●  EN DIRECT", "●  LIVE", "●  مباشر"), Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = AtlasMint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IllustratedAvatarBadge(variant: Int, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(56.dp),
        shape = CircleShape,
        color = accent.copy(alpha = .16f),
        border = BorderStroke(1.5.dp, accent.copy(alpha = .86f)),
        shadowElevation = 6.dp,
    ) {
        Canvas(Modifier.fillMaxSize().padding(3.dp)) {
            val w = size.width
            val h = size.height
            val skin = when (variant) {
                1 -> Color(0xFFD99B72)
                2 -> Color(0xFF8C5D42)
                else -> Color(0xFFF0BD98)
            }
            val hair = when (variant) {
                1 -> Color(0xFF33251F)
                2 -> Color(0xFF211B19)
                3 -> Color(0xFF704A35)
                else -> Color(0xFF60402D)
            }
            val shirt = if (variant % 2 == 0) Color(0xFFF7F5F1) else Color(0xFFEAEFF5)

            drawCircle(accent.copy(alpha = .12f), radius = size.minDimension * .49f)
            val jacket = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * .25f, h * .59f)
                lineTo(w * .39f, h * .55f)
                lineTo(w * .5f, h * .68f)
                lineTo(w * .61f, h * .55f)
                lineTo(w * .75f, h * .59f)
                lineTo(w * .98f, h * 1.02f)
                lineTo(w * .02f, h * 1.02f)
                close()
            }
            drawPath(jacket, accent.copy(alpha = .86f))
            drawRoundRect(
                color = skin,
                topLeft = Offset(w * .42f, h * .51f),
                size = androidx.compose.ui.geometry.Size(w * .16f, h * .22f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .05f),
            )
            val shirtFront = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * .38f, h * .59f)
                lineTo(w * .5f, h * .72f)
                lineTo(w * .62f, h * .59f)
                lineTo(w * .59f, h * .93f)
                lineTo(w * .41f, h * .93f)
                close()
            }
            drawPath(shirtFront, shirt)
            val faceTop = if (variant == 3) .20f else .23f
            drawOval(
                color = skin,
                topLeft = Offset(w * .29f, h * faceTop),
                size = androidx.compose.ui.geometry.Size(w * .42f, h * .42f),
            )
            drawOval(
                color = hair,
                topLeft = Offset(w * .27f, h * .13f),
                size = androidx.compose.ui.geometry.Size(w * .46f, h * .26f),
            )
            drawRoundRect(
                color = hair,
                topLeft = Offset(w * .25f, h * .24f),
                size = androidx.compose.ui.geometry.Size(w * .10f, h * .20f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .05f),
            )
            drawRoundRect(
                color = hair,
                topLeft = Offset(w * .65f, h * .23f),
                size = androidx.compose.ui.geometry.Size(w * .10f, h * .21f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .05f),
            )
            drawCircle(Color(0xFF362A26), radius = w * .018f, center = Offset(w * .41f, h * .42f))
            drawCircle(Color(0xFF362A26), radius = w * .018f, center = Offset(w * .59f, h * .42f))
            val tie = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * .47f, h * .70f)
                lineTo(w * .53f, h * .70f)
                lineTo(w * .55f, h * .86f)
                lineTo(w * .5f, h * .95f)
                lineTo(w * .45f, h * .86f)
                close()
            }
            drawPath(tie, AtlasAmber)
        }
    }
}

@Composable
private fun DashboardIllustration(bob: Float, motion: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            Modifier.size(width = 266.dp, height = 220.dp).graphicsLayer { translationY = if (motion) bob * .4f else 0f },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 10.dp,
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(atlasText("MON TABLEAU DE BORD", "MY DASHBOARD", "لوحة تحكمي"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Outlined.AutoAwesome, null, tint = AtlasPurple, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(11.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    DashboardTile(atlasText("Ventes", "Sales", "المبيعات"), "8.4k", AtlasCyan, Modifier.weight(1f))
                    DashboardTile(atlasText("Stock", "Stock", "المخزون"), "94%", AtlasPurple, Modifier.weight(1f))
                }
                Spacer(Modifier.height(9.dp))
                Surface(
                    Modifier.fillMaxWidth().weight(.72f),
                    shape = RoundedCornerShape(14.dp),
                    color = AtlasMagenta.copy(alpha = .1f),
                    border = BorderStroke(1.dp, AtlasMagenta.copy(alpha = .28f)),
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(atlasText("ENCAISSEMENT RAPIDE", "QUICK BILLING", "فوترة سريعة"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.width(47.dp).height(24.dp).clip(RoundedCornerShape(8.dp)).background(AtlasMagenta.copy(alpha = .24f)), contentAlignment = Alignment.Center) {
                            Text(atlasText("+ Nouveau", "+ New", "+ جديد"), style = MaterialTheme.typography.labelSmall, color = AtlasMagenta, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Surface(
            Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 45.dp).graphicsLayer { translationX = if (motion) bob * 1.3f else 0f },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, AtlasCyan.copy(alpha = .55f)),
        ) {
            Icon(Icons.Outlined.Inventory2, null, Modifier.padding(10.dp).size(22.dp), tint = AtlasCyan)
        }
        Surface(
            Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 28.dp).graphicsLayer { translationY = if (motion) -bob else 0f },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, AtlasPurple.copy(alpha = .6f)),
        ) {
            Icon(Icons.Outlined.AutoAwesome, null, Modifier.padding(10.dp).size(20.dp), tint = AtlasPurple)
        }
    }
}

@Composable
private fun DashboardTile(title: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(15.dp),
        color = color.copy(alpha = .12f),
        border = BorderStroke(1.dp, color.copy(alpha = .35f)),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(value, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                listOf(.35f, .58f, .42f, .86f, .64f, .98f).forEachIndexed { index, height ->
                    Box(Modifier.weight(1f).height((19 * height).dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(color.copy(alpha = .35f + (index * .08f))))
                }
            }
        }
    }
}

@Composable
private fun RecapIllustration(bob: Float, motion: Boolean) {
    var metricIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(motion) {
        if (motion) {
            while (true) {
                delay(1_650)
                metricIndex = (metricIndex + 1) % 3
            }
        } else metricIndex = 0
    }
    val metrics = listOf(
        atlasText("Revenus · 12 480 DH", "Revenue · 12 480 DH", "الإيرادات · 12 480 درهم"),
        atlasText("Ventes · 38 aujourd’hui", "Sales · 38 today", "المبيعات · 38 اليوم"),
        atlasText("Meilleur produit · +24%", "Top item · +24%", "المنتج الأفضل · +24٪"),
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            Modifier.align(Alignment.CenterStart).padding(start = 36.dp).size(width = 132.dp, height = 194.dp).graphicsLayer { translationY = if (motion) bob * .5f else 0f },
            shape = RoundedCornerShape(25.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 9.dp,
        ) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(15.dp)).background(AtlasPurple.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Surface(Modifier.size(42.dp), shape = CircleShape, color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, AtlasPurple.copy(alpha = .55f))) {
                        Box(contentAlignment = Alignment.Center) { AtlasMark(size = 25.dp) }
                    }
                }
                Spacer(Modifier.height(11.dp))
                Text(atlasText("RÉCAP DU JOUR", "DAILY RECAP", "ملخص اليوم"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Canvas(Modifier.fillMaxWidth().height(56.dp)) {
                    val bars = listOf(.36f, .55f, .43f, .82f, .62f, .98f)
                    val gap = 5.dp.toPx()
                    val barWidth = (size.width - gap * (bars.size - 1)) / bars.size
                    bars.forEachIndexed { index, fraction ->
                        val barHeight = size.height * fraction
                        drawRoundRect(
                            color = listOf(AtlasCyan, AtlasBlue, AtlasPurple, AtlasMagenta)[index % 4].copy(alpha = .82f),
                            topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        )
                    }
                }
            }
        }
        Surface(
            Modifier.align(Alignment.TopEnd).padding(end = 22.dp, top = 45.dp).graphicsLayer { translationY = if (motion) -bob else 0f },
            shape = RoundedCornerShape(17.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, AtlasCyan.copy(alpha = .56f)),
            shadowElevation = 8.dp,
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = AtlasCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                AnimatedContent(metricIndex, label = "daily_recap_metric") { index ->
                    Text(metrics[index], style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Surface(
            Modifier.align(Alignment.BottomEnd).padding(end = 38.dp, bottom = 38.dp).graphicsLayer { translationX = if (motion) bob else 0f },
            shape = RoundedCornerShape(15.dp),
            color = AtlasMagenta.copy(alpha = .16f),
            border = BorderStroke(1.dp, AtlasMagenta.copy(alpha = .48f)),
        ) {
            Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ReceiptLong, null, tint = AtlasMagenta, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(atlasText("RÉSUMÉ DU JOUR", "DAILY SUMMARY", "ملخص يومي"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Surface(
            Modifier.align(Alignment.CenterEnd).padding(end = 5.dp).size(47.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, AtlasAmber.copy(alpha = .72f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = AtlasAmber, modifier = Modifier.size(23.dp))
            }
        }
    }
}
