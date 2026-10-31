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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import ma.etherealmber.atlascommerce.ui.theme.AtlasAuthTheme

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
fun AtlasWelcomeOnboardingScreen(onComplete: () -> Unit) {
    AtlasAuthTheme {
        val pagerState = rememberPagerState(pageCount = { welcomeSlides.size })
        val animationsEnabled = LocalAtlasAnimationsEnabled.current
        val language = LocalAtlasLanguage.current
        val scope = rememberCoroutineScope()

        LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress, animationsEnabled) {
            if (animationsEnabled && !pagerState.isScrollInProgress && pagerState.currentPage < welcomeSlides.lastIndex) {
                delay(8_000)
                if (!pagerState.isScrollInProgress && pagerState.currentPage < welcomeSlides.lastIndex) {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                    AtlasMark(Modifier.align(Alignment.CenterStart), size = 28.dp)
                    Row(
                        Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        welcomeSlides.indices.forEach { page ->
                            val selected = page == pagerState.currentPage
                            val dotWidth by animateDpAsState(
                                if (selected) 26.dp else 7.dp,
                                spring(dampingRatio = 0.78f, stiffness = 500f),
                                label = "onboarding_dot_width_$page",
                            )
                            val dotColor by animateColorAsState(
                                when {
                                    selected -> AtlasCyan
                                    page < pagerState.currentPage -> AtlasPurple.copy(alpha = 0.72f)
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                },
                                tween(260),
                                label = "onboarding_dot_color_$page",
                            )
                            Box(
                                Modifier
                                    .width(dotWidth)
                                    .height(7.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .clickable { scope.launch { pagerState.animateScrollToPage(page) } }
                                    .semantics { contentDescription = "${page + 1} / ${welcomeSlides.size}" },
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.CenterEnd).clickable(onClick = onComplete),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Text(
                            atlasText("Se connecter", "Sign in", "تسجيل الدخول"),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }

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
                            } else onComplete()
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            if (pagerState.currentPage == welcomeSlides.lastIndex) atlasText("Commencer", "Get started", "ابدأ")
                            else atlasText("Continuer", "Continue", "متابعة"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Text(
                    text = atlasText(
                        "En rejoignant Atlas, vous acceptez nos conditions d'utilisation et notre politique de confidentialité.",
                        "By joining Atlas, you acknowledge our Terms of Service and Privacy Policy.",
                        "بانضمامك إلى Atlas، فإنك توافق على شروط الخدمة وسياسة الخصوصية.",
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
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
        Spacer(Modifier.height(9.dp))
        Text(
            text = chooseLanguage(slide.subtitle, language),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9CA3AF),
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
        )
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
        Surface(
            Modifier.align(Alignment.Center).offset(y = (-31).dp).size(47.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, AtlasCyan.copy(alpha = .6f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = AtlasCyan, modifier = Modifier.size(24.dp))
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
        AvatarBadge("A", AtlasCyan, Modifier.align(Alignment.TopStart).padding(start = 44.dp, top = 26.dp).graphicsLayer { scaleX = if (motion) 1f + bob / 60f else 1f; scaleY = if (motion) 1f + bob / 60f else 1f })
        AvatarBadge("M", AtlasMagenta, Modifier.align(Alignment.TopEnd).padding(end = 48.dp, top = 34.dp).graphicsLayer { scaleX = if (motion) 1f - bob / 60f else 1f; scaleY = if (motion) 1f - bob / 60f else 1f })
        AvatarBadge("S", AtlasAmber, Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 34.dp))
        AvatarBadge("Y", AtlasPurple, Modifier.align(Alignment.BottomEnd).padding(end = 44.dp, bottom = 26.dp).graphicsLayer { translationY = if (motion) -bob else 0f })
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
private fun AvatarBadge(letter: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(53.dp),
        shape = CircleShape,
        color = color.copy(alpha = .19f),
        border = BorderStroke(1.5.dp, color.copy(alpha = .82f)),
        shadowElevation = 5.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(letter, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
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
