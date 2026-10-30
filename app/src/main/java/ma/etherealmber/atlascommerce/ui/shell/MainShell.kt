package ma.etherealmber.atlascommerce.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.core.update.UpdateState
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasSurpriseDialog
import ma.etherealmber.atlascommerce.ui.components.nextSurprise
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingDockPadding
import ma.etherealmber.atlascommerce.ui.components.AtlasPageTransition
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.components.AtlasTopBar
import ma.etherealmber.atlascommerce.ui.components.UpdateMiniProgressBall
import ma.etherealmber.atlascommerce.ui.components.ShakeGestureEffect
import ma.etherealmber.atlascommerce.ui.root.AppViewModel
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentQuotaBarsContent
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentNavigationDestination
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentNavigationSuggestion
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentScreen
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentViewModel
import ma.etherealmber.atlascommerce.ui.screens.agent.localizedAgentText
import ma.etherealmber.atlascommerce.ui.screens.agent.AtlasAgentMascot
import ma.etherealmber.atlascommerce.ui.screens.legal.ProblemReportDialog
import ma.etherealmber.atlascommerce.ui.screens.balance.BalanceScreen
import ma.etherealmber.atlascommerce.ui.screens.home.HomeScreen
import ma.etherealmber.atlascommerce.ui.screens.employees.EmployeesScreen
import ma.etherealmber.atlascommerce.ui.screens.products.ProductsScreen
import ma.etherealmber.atlascommerce.ui.screens.sale.SaleScreen
import ma.etherealmber.atlascommerce.ui.screens.scan.ScanScreen
import ma.etherealmber.atlascommerce.ui.screens.settings.ProfileScreen
import ma.etherealmber.atlascommerce.ui.screens.settings.SettingsScreen
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Accueil", Icons.Outlined.Home),
    PRODUCTS("Produits", Icons.Outlined.Inventory2),
    SCAN("Scan", Icons.Outlined.QrCodeScanner),
    SALE("Vente", Icons.Outlined.ShoppingCart),
    BALANCE("Balance", Icons.Outlined.AccountBalanceWallet),
    EMPLOYEES("Employés", Icons.Outlined.Groups),
    AGENT("Atlas Agent", Icons.Outlined.AutoAwesome),
    PROFILE("Mon profil", Icons.Outlined.Person),
    SETTINGS("Paramètres", Icons.Outlined.Settings),
    ;

    companion object {
        /** Profile, settings, sales, and Atlas Agent are pushed screens with a back action. */
        val BottomBarTabs = listOf(HOME, PRODUCTS, SCAN, BALANCE, EMPLOYEES)

        /** Financial and employee data remain guarded by server-authorized roles. */
        fun bottomTabsForRole(role: String?): List<MainTab> =
            when {
                Roles.isAdmin(role) -> BottomBarTabs
                Roles.normalize(role) == Roles.STOCK -> listOf(HOME, SCAN, PRODUCTS)
                else -> listOf(HOME, PRODUCTS, SCAN, BALANCE)
            }
    }
}

@Composable
private fun MainTab.localizedLabel(): String = when (this) {
    MainTab.HOME -> atlasText("Accueil", "Home", "الرئيسية")
    MainTab.PRODUCTS -> atlasText("Produits", "Products", "المنتجات")
    MainTab.SCAN -> atlasText("Scan", "Scan", "المسح")
    MainTab.SALE -> atlasText("Vente", "Sales", "المبيعات")
    MainTab.BALANCE -> atlasText("Balance", "Balance", "الرصيد")
    MainTab.EMPLOYEES -> atlasText("Employés", "Employees", "الموظفون")
    MainTab.AGENT -> atlasText("Assistant Atlas", "Atlas Assistant", "مساعد Atlas")
    MainTab.PROFILE -> atlasText("Mon profil", "My profile", "ملفي الشخصي")
    MainTab.SETTINGS -> atlasText("Paramètres", "Settings", "الإعدادات")
}

private val BackStackTabs = setOf(MainTab.AGENT, MainTab.SALE, MainTab.PROFILE, MainTab.SETTINGS)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainShell(
    user: SessionUser,
    snackbarHostState: SnackbarHostState,
    defaultStartTab: String = MainTab.HOME.name,
    activeTabOverride: String? = null,
    onActiveTabChange: ((String) -> Unit)? = null,
    lastBottomTabOverride: String? = null,
    onLastBottomTabChange: ((String) -> Unit)? = null,
    isForeground: Boolean = true,
    externalTabRequest: String? = null,
    onExternalTabHandled: () -> Unit = {},
    updateState: UpdateState = UpdateState.Idle,
    onShowUpdateProgress: () -> Unit = {},
    onChangeServer: () -> Unit,
    onOpenQrDevice: (String?) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSnake: () -> Unit = {},
    onOpenDeveloperTools: () -> Unit = {},
    onSignOut: () -> Unit,
    onStartUpdateDownload: () -> Unit = {},
    shellViewModel: ShellViewModel = hiltViewModel(),
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val atlasLanguage = LocalAtlasLanguage.current
    val cartCount by shellViewModel.cartCount.collectAsStateWithLifecycle()
    val shakeToReportEnabled by appViewModel.shakeToReportEnabled.collectAsStateWithLifecycle()
    val notificationCount by shellViewModel.notificationCount.collectAsStateWithLifecycle()
    val serverBaseUrl by shellViewModel.serverBaseUrl.collectAsStateWithLifecycle()
    val agentPolicy by appViewModel.agentPolicy.collectAsStateWithLifecycle()

    val roleTabs = MainTab.bottomTabsForRole(user.role)
    val resolvedInitial = roleTabs
        .firstOrNull { it.name == defaultStartTab }
        ?.name ?: MainTab.HOME.name

    var internalTabName by rememberSaveable(user.scopeIdentity) { mutableStateOf(resolvedInitial) }
    var internalLastBottomTabName by rememberSaveable(user.scopeIdentity) { mutableStateOf(resolvedInitial) }

    val tabName = activeTabOverride ?: internalTabName
    val setTabName: (String) -> Unit = { next ->
        internalTabName = next
        onActiveTabChange?.invoke(next)
    }
    val storedLastBottomTabName = lastBottomTabOverride ?: internalLastBottomTabName
    val lastBottomTabName = storedLastBottomTabName.takeIf { saved -> roleTabs.any { it.name == saved } }
        ?: MainTab.HOME.name
    val setLastBottomTabName: (String) -> Unit = { next ->
        internalLastBottomTabName = next
        onLastBottomTabChange?.invoke(next)
    }

    val rawTab = MainTab.entries.firstOrNull { it.name == tabName } ?: MainTab.HOME
    val tab = when {
        rawTab == MainTab.SALE && !Roles.canCreateDocuments(user.role) -> MainTab.HOME
        rawTab == MainTab.EMPLOYEES && !Roles.isAdmin(user.role) -> MainTab.HOME
        rawTab == MainTab.BALANCE && Roles.normalize(user.role) == Roles.STOCK -> MainTab.HOME
        else -> rawTab
    }
    val scope = rememberCoroutineScope()
    var surpriseIndex by rememberSaveable(user.scopeIdentity) { mutableStateOf(-1) }
    var showAvatarSurprise by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var showAgentSurprise by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var agentHistoryRequested by remember { mutableStateOf(false) }
    val agentViewModel: AgentViewModel? = if (tab == MainTab.AGENT) hiltViewModel() else null
    val agentState = agentViewModel?.state?.collectAsStateWithLifecycle()?.value
    var showProblemReportDialog by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var showShakeReportPrompt by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var showAgentQuotaDialog by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var pendingAgentReportRequest by remember(user.scopeIdentity) { mutableStateOf<AgentNavigationSuggestion?>(null) }
    LaunchedEffect(tab) {
        if (tab != MainTab.AGENT) showAgentQuotaDialog = false
    }

    val shakeDisabledMessage = atlasText(
        "Raccourci secouer pour signaler désactivé.",
        "Shake-to-report shortcut disabled.",
        "تم تعطيل اختصار الهز للإبلاغ.",
    )
    ShakeGestureEffect(
        enabled = isForeground && shakeToReportEnabled && !showProblemReportDialog && !showShakeReportPrompt,
        onShake = { showShakeReportPrompt = true },
    )

    if (showAvatarSurprise || showAgentSurprise) {
        AtlasSurpriseDialog(surpriseIndex) { showAvatarSurprise = false; showAgentSurprise = false }
    }

    if (showShakeReportPrompt) {
        AlertDialog(
            onDismissRequest = { showShakeReportPrompt = false },
            title = { Text(atlasText("Signaler un problème ?", "Report a problem?", "هل تريد الإبلاغ عن مشكلة؟")) },
            text = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(atlasText(
                        "Le téléphone a été secoué. Rien ne sera envoyé sans votre confirmation dans le formulaire.",
                        "Your phone was shaken. Nothing will be sent unless you confirm in the report form.",
                        "تم هز الهاتف. لن يتم إرسال شيء حتى تؤكد ذلك في نموذج الإبلاغ.",
                    ))
                    TextButton(onClick = {
                        showShakeReportPrompt = false
                        appViewModel.setShakeToReportEnabled(false)
                        scope.launch { snackbarHostState.showSnackbar(shakeDisabledMessage) }
                    }) {
                        Text(atlasText("Désactiver ce raccourci", "Disable this shortcut", "تعطيل هذا الاختصار"))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showShakeReportPrompt = false
                    showProblemReportDialog = true
                }) { Text(atlasText("Signaler", "Report", "إبلاغ")) }
            },
            dismissButton = {
                TextButton(onClick = { showShakeReportPrompt = false }) {
                    Text(atlasText("Pas maintenant", "Not now", "ليس الآن"))
                }
            },
        )
    }

    if (showProblemReportDialog) {
        ProblemReportDialog(
            screenOrigin = tab.label,
            onDismiss = { showProblemReportDialog = false },
            onToast = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
        )
    }

    LaunchedEffect(externalTabRequest) {
        if (!externalTabRequest.isNullOrBlank()) {
            val targetAllowed = when (externalTabRequest) {
                MainTab.SALE.name -> Roles.canCreateDocuments(user.role)
                MainTab.EMPLOYEES.name -> Roles.isAdmin(user.role)
                MainTab.BALANCE.name -> Roles.normalize(user.role) != Roles.STOCK
                else -> true
            }
            val resolved = if (targetAllowed) externalTabRequest else MainTab.HOME.name
            setTabName(resolved)
            if (roleTabs.any { it.name == resolved }) setLastBottomTabName(resolved)
            onExternalTabHandled()
        }
    }

    LaunchedEffect(tab) {
        AtlasLogBuffer.log("NAVIGATION", "Main tab selected: ${tab.name}")
    }

    LaunchedEffect(tab, isForeground) {
        if (isForeground) {
            shellViewModel.refreshNotificationsBadge()
        }
    }

    val onToast: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(atlasNotice(message, atlasLanguage)) }
    }
    val switchTab: (MainTab) -> Unit = { target ->
        if (target == MainTab.BALANCE && Roles.normalize(user.role) == Roles.STOCK) {
            setTabName(MainTab.HOME.name)
        } else if (target == MainTab.SALE && !Roles.canCreateDocuments(user.role)) {
            onToast("Accès Vente réservé aux rôles Administrateur et Secrétaire.")
        } else {
            if (roleTabs.contains(target)) setLastBottomTabName(target.name)
            setTabName(target.name)
        }
    }
    val restrictedFinanceMessage = atlasText(
        "Les rapports financiers sont réservés aux administrateurs.",
        "Financial reports are restricted to administrators.",
        "التقارير المالية مخصصة للمسؤولين فقط.",
    )
    val restrictedEmployeesMessage = atlasText(
        "Les informations employés sont réservées aux administrateurs.",
        "Employee information is restricted to administrators.",
        "معلومات الموظفين مخصصة للمسؤولين فقط.",
    )
    val navigateFromAgent: (AgentNavigationSuggestion) -> Unit = { suggestion ->
        when (suggestion.destination) {
            AgentNavigationDestination.HOME -> switchTab(MainTab.HOME)
            AgentNavigationDestination.PRODUCTS -> switchTab(MainTab.PRODUCTS)
            AgentNavigationDestination.SALES -> switchTab(MainTab.SALE)
            AgentNavigationDestination.BALANCE_REPORT -> {
                if (!Roles.isAdmin(user.role)) {
                    onToast(restrictedFinanceMessage)
                } else {
                    pendingAgentReportRequest = suggestion
                    switchTab(MainTab.BALANCE)
                }
            }
            AgentNavigationDestination.EMPLOYEES -> {
                if (Roles.isAdmin(user.role)) switchTab(MainTab.EMPLOYEES)
                else onToast(restrictedEmployeesMessage)
            }
            AgentNavigationDestination.SCAN -> switchTab(MainTab.SCAN)
            AgentNavigationDestination.NOTIFICATIONS -> onOpenNotifications()
            AgentNavigationDestination.SETTINGS -> switchTab(MainTab.SETTINGS)
            AgentNavigationDestination.PROFILE -> switchTab(MainTab.PROFILE)
        }
    }

    BackHandler(enabled = isForeground && tab != MainTab.HOME) {
        AtlasLogBuffer.log("NAVIGATION", "System Back handled by shell on tab=${tab.name}")
        when (tab) {
            MainTab.SETTINGS -> setTabName(MainTab.PROFILE.name)
            MainTab.PROFILE, MainTab.AGENT, MainTab.SALE -> setTabName(lastBottomTabName)
            else -> switchTab(MainTab.HOME)
        }
    }
    BackHandler(enabled = isForeground && tab == MainTab.HOME) {
        AtlasLogBuffer.log("NAVIGATION", "System Back signs out from the dashboard to the sign-in screen")
        onSignOut()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    val floatingBarWidth by animateDpAsState(
        targetValue = maxWidth,
        animationSpec = if (motionEnabled) {
            spring(dampingRatio = 0.86f, stiffness = 360f)
        } else {
            tween(0)
        },
        label = "floating-bottom-bar-width",
    )
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            AtlasTopBar(
                title = tab.localizedLabel(),
                subtitle = agentState?.let { state ->
                    val thread = state.activeThreadTitle.takeIf(String::isNotBlank)?.let { localizedAgentText(it, atlasLanguage) }
                        ?: atlasText("Sans titre", "Untitled", "بلا عنوان")
                    val status = when {
                        state.policy?.isExhausted == true -> atlasText("Quota atteint", "Quota reached", "تم بلوغ الحصة")
                        state.status?.configured == true -> atlasText("Configuré", "Configured", "تم الإعداد")
                        state.status?.configured == false -> atlasText("En pause", "Paused", "متوقف مؤقتًا")
                        else -> atlasText("Vérification…", "Checking…", "جارٍ التحقق…")
                    }
                    listOf(thread, status).filter { it.isNotBlank() }.joinToString(" · ")
                },
                titleLeading = if (tab == MainTab.AGENT) {
                    {
                        AtlasAgentMascot(
                            modifier = Modifier.size(32.dp).combinedClickable(
                                onClick = {},
                                onLongClickLabel = atlasText("Afficher une surprise", "Show a surprise", "إظهار مفاجأة"),
                                onLongClick = { surpriseIndex = nextSurprise(surpriseIndex, 5); showAgentSurprise = true },
                            ),
                            isTyping = agentState?.input?.isNotBlank() == true,
                            isThinking = agentState?.sending == true,
                        )
                    }
                } else null,
                onBack = when (tab) {
                    MainTab.SETTINGS -> ({ setTabName(MainTab.PROFILE.name) })
                    MainTab.PROFILE, MainTab.AGENT, MainTab.SALE -> ({ setTabName(lastBottomTabName) })
                    else -> null
                },
                actions = {
                    val hasHiddenUpdate = when (val state = updateState) {
                        is UpdateState.Downloading -> state.hiddenInForeground && !state.info.mandatory
                        is UpdateState.Available -> state.dismissedBanner && !state.info.mandatory
                        is UpdateState.ReadyToInstall -> state.dismissedBanner && !state.info.mandatory
                        else -> false
                    }
                    if (isForeground) {
                        UpdateMiniProgressBall(
                            state = updateState,
                            onClick = onShowUpdateProgress,
                        )
                    }
                    if (hasHiddenUpdate && isForeground) Spacer(Modifier.width(6.dp))
                    when (tab) {
                        MainTab.AGENT -> {
                            IconButton(onClick = { agentHistoryRequested = true }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Outlined.History, contentDescription = atlasText("Historique des discussions", "Conversation history", "سجل المحادثات"))
                            }
                            IconButton(onClick = { agentViewModel?.startNewConversation() }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Outlined.AddComment, contentDescription = atlasText("Nouvelle discussion", "New conversation", "محادثة جديدة"))
                            }
                            IconButton(onClick = { agentViewModel?.refreshContext() }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser le contexte", "Refresh context", "تحديث السياق"))
                            }
                            IconButton(
                                onClick = {
                                    showAgentQuotaDialog = true
                                    appViewModel.refreshAgentPolicy()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HourglassBottom,
                                    contentDescription = atlasText("Afficher le quota IA", "Show AI quota", "عرض حصة الذكاء الاصطناعي"),
                                    tint = if (agentPolicy?.isExhausted == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                        }

                        MainTab.SALE, MainTab.SETTINGS -> Unit

                        MainTab.PROFILE -> {
                            IconButton(onClick = { switchTab(MainTab.SETTINGS) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = atlasText("Paramètres", "Settings", "الإعدادات"),
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                        }

                        else -> {
                            TopBarQuickActionsMenu(
                                notificationCount = notificationCount,
                                showSale = Roles.canCreateDocuments(user.role),
                                onOpenNotifications = onOpenNotifications,
                                onOpenSale = { switchTab(MainTab.SALE) },
                            )
                            Spacer(Modifier.width(6.dp))
                            IconButton(onClick = { switchTab(MainTab.AGENT) }) {
                                Icon(
                                    imageVector = MainTab.AGENT.icon,
                                    contentDescription = atlasText("Ouvrir Atlas Agent", "Open Atlas Agent", "فتح مساعد Atlas"),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            AtlasAvatar(
                                avatarUrl = user.avatarUrl,
                                label = user.displayName.ifBlank { user.username },
                                size = 34.dp,
                                serverBaseUrl = serverBaseUrl,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                clickContentDescription = atlasText(
                                    "Ouvrir le profil de ${user.displayName.ifBlank { user.username }}",
                                    "Open profile for ${user.displayName.ifBlank { user.username }}",
                                    "فتح الملف الشخصي لـ ${user.displayName.ifBlank { user.username }}",
                                ),
                                onClick = { switchTab(MainTab.PROFILE) },
                                onLongClick = { surpriseIndex = nextSurprise(surpriseIndex, 5); showAvatarSurprise = true },
                                longClickLabel = atlasText("Afficher une surprise", "Show a surprise", "إظهار مفاجأة"),
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
        val showRail = useRail && tab !in BackStackTabs
        if (showRail) {
            NavigationRail(modifier = Modifier.align(Alignment.CenterStart), containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                roleTabs.forEach { item ->
                    NavigationRailItem(
                        selected = tab == item,
                        onClick = { switchTab(item) },
                        icon = {
                            if (item == MainTab.SALE && cartCount > 0) {
                                BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) { AnimatedTabIcon(item.icon, item.localizedLabel(), selected = tab == item) }
                            } else AnimatedTabIcon(item.icon, item.localizedLabel(), selected = tab == item)
                        },
                        label = { Text(item.localizedLabel(), style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                    )
                }
            }
            VerticalDivider(modifier = Modifier.align(Alignment.CenterStart).padding(start = 79.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
        }
        val navigationInset = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
        CompositionLocalProvider(
            LocalAtlasFloatingDockPadding provides if (!useRail && tab !in BackStackTabs) 96.dp + navigationInset else 0.dp,
        ) {
        AtlasPageTransition(
            targetState = tab,
            indexSelector = { it.ordinal },
            // The dock floats over the page so content can scroll beneath it. Main scrollable
            // screens add end content-padding so their final items can still scroll clear of it.
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (showRail) 80.dp else 0.dp),
        ) { currentTab ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = if (useRail) 1200.dp else 840.dp).fillMaxSize()) {
                when (currentTab) {
                    MainTab.HOME -> HomeScreen(
                        user = user,
                        onOpenProducts = { switchTab(MainTab.PRODUCTS) },
                        onOpenSale = { switchTab(MainTab.SALE) },
                        onOpenScan = { switchTab(MainTab.SCAN) },
                        onOpenNotifications = onOpenNotifications,
                        onToast = onToast,
                    )

                    MainTab.PRODUCTS -> ProductsScreen(
                        user = user,
                        onOpenScan = { switchTab(MainTab.SCAN) },
                        onOpenSale = { switchTab(MainTab.SALE) },
                        onToast = onToast,
                    )

                    MainTab.SCAN -> ScanScreen(
                        user = user,
                        isActive = isForeground && tab == MainTab.SCAN && currentTab == MainTab.SCAN,
                        onOpenSale = { switchTab(MainTab.SALE) },
                        onOpenProducts = { switchTab(MainTab.PRODUCTS) },
                        onOpenQrDevice = onOpenQrDevice,
                        onToast = onToast,
                    )

                    MainTab.SALE -> SaleScreen(
                        user = user,
                        onOpenProducts = { switchTab(MainTab.PRODUCTS) },
                        onToast = onToast,
                    )

                    MainTab.EMPLOYEES -> EmployeesScreen(user = user)

                    MainTab.AGENT -> AgentScreen(
                        onToast = onToast,
                        onNavigate = navigateFromAgent,
                        historyRequested = agentHistoryRequested,
                        onHistoryHandled = { agentHistoryRequested = false },
                        viewModel = agentViewModel ?: hiltViewModel(),
                        isActive = isForeground && tab == MainTab.AGENT && currentTab == MainTab.AGENT,
                    )

                    MainTab.PROFILE -> ProfileScreen(
                        user = user,
                        onOpenSettings = { switchTab(MainTab.SETTINGS) },
                        onSignOut = onSignOut,
                        onToast = onToast,
                    )

                    MainTab.BALANCE -> BalanceScreen(
                        onToast = onToast,
                        requestedReport = pendingAgentReportRequest,
                        onReportRequestHandled = { pendingAgentReportRequest = null },
                    )

                    MainTab.SETTINGS -> SettingsScreen(
                        user = user,
                        onChangeServer = onChangeServer,
                        onOpenQrDevice = { onOpenQrDevice(null) },
                        onOpenLicense = onOpenLicense,
                        onOpenSecurity = onOpenSecurity,
                        onOpenAbout = onOpenAbout,
                        onOpenSnake = onOpenSnake,
                        onOpenDeveloperTools = onOpenDeveloperTools,
                        onSignOut = onSignOut,
                        onStartUpdateDownload = onStartUpdateDownload,
                        onToast = onToast,
                    )
                }
                }
            }
        }
        }
        }
    }
    if (showAgentQuotaDialog && tab == MainTab.AGENT) {
        AlertDialog(
            onDismissRequest = { showAgentQuotaDialog = false },
            title = { Text(atlasText("Utilisation de l'IA", "AI usage", "استخدام الذكاء الاصطناعي")) },
            text = {
                val currentPolicy = agentPolicy
                if (currentPolicy == null) {
                    Text(atlasText("Chargement du quota…", "Loading quota…", "جارٍ تحميل الحصة…"))
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        AgentQuotaBarsContent(policy = currentPolicy, compact = false)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAgentQuotaDialog = false }) {
                    Text(atlasText("Fermer", "Close", "إغلاق"))
                }
            },
            dismissButton = {
                TextButton(onClick = appViewModel::refreshAgentPolicy) {
                    Text(atlasText("Actualiser", "Refresh", "تحديث"))
                }
            },
        )
    }
    AnimatedVisibility(
        visible = !useRail && tab !in BackStackTabs,
        modifier = Modifier.align(Alignment.BottomCenter).width(floatingBarWidth),
        enter = if (motionEnabled) {
            fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                    initialOffsetY = { height -> height / 2 },
                ) +
                scaleIn(
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 460f),
                    initialScale = 0.94f,
                )
        } else {
            EnterTransition.None
        },
        exit = if (motionEnabled) {
            fadeOut(tween(150, easing = FastOutSlowInEasing)) +
                slideOutVertically(
                    animationSpec = tween(190, easing = FastOutSlowInEasing),
                    targetOffsetY = { height -> height / 3 },
                ) +
                scaleOut(
                    animationSpec = tween(170, easing = FastOutSlowInEasing),
                    targetScale = 0.97f,
                )
        } else {
            ExitTransition.None
        },
    ) {
        AtlasFloatingBottomBar(
            tabs = roleTabs,
            selected = tab,
            onSelect = switchTab,
        )
    }
    }
}

@Composable
private fun AtlasFloatingBottomBar(
    tabs: List<MainTab>,
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val centerTab = tabs.firstOrNull { it == MainTab.SCAN } ?: tabs.getOrNull(tabs.size / 2)
    val centerIndex = centerTab?.let(tabs::indexOf) ?: -1
    val leftTabs = if (centerIndex >= 0) tabs.take(centerIndex) else tabs
    val rightTabs = if (centerIndex >= 0) tabs.drop(centerIndex + 1) else emptyList()
    // Only the dock itself is opaque; the surrounding overlay stays transparent.
    val barColor = MaterialTheme.colorScheme.surfaceContainerLow
    val motionEnabled = LocalAtlasAnimationsEnabled.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .height(84.dp),
    ) {
        val compactWidth = maxWidth < 360.dp
        val dockWidth = if (maxWidth >= 680.dp) 640.dp else maxWidth
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).width(dockWidth).height(64.dp),
            shape = RoundedCornerShape(26.dp),
            color = barColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            tonalElevation = 0.dp,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = if (compactWidth) 2.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    leftTabs.forEach { item ->
                        FloatingBottomBarItem(item, selected == item, motionEnabled, compactWidth, Modifier.weight(1f)) { onSelect(item) }
                    }
                }
                if (centerTab != null) Spacer(Modifier.width(if (compactWidth) 52.dp else 64.dp))
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    rightTabs.forEach { item ->
                        FloatingBottomBarItem(item, selected == item, motionEnabled, compactWidth, Modifier.weight(1f)) { onSelect(item) }
                    }
                }
            }
        }

        if (centerTab != null) {
            val fabScale by animateFloatAsState(
                targetValue = if (!motionEnabled || selected == centerTab) 1f else 0.96f,
                animationSpec = if (motionEnabled) spring(dampingRatio = 0.68f, stiffness = 520f) else tween(0),
                label = "nav-fab-${centerTab.name}",
            )
            val fabContainerColor by animateColorAsState(
                targetValue = if (selected == centerTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                animationSpec = tween(if (motionEnabled) 220 else 0, easing = FastOutSlowInEasing),
                label = "nav-fab-color-${centerTab.name}",
            )
            val fabContentColor by animateColorAsState(
                targetValue = if (selected == centerTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                animationSpec = tween(if (motionEnabled) 220 else 0, easing = FastOutSlowInEasing),
                label = "nav-fab-content-color-${centerTab.name}",
            )
            FloatingActionButton(
                onClick = { onSelect(centerTab) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(if (compactWidth) 50.dp else 56.dp)
                    .graphicsLayer {
                        scaleX = if (motionEnabled) fabScale else 1f
                        scaleY = if (motionEnabled) fabScale else 1f
                    },
                shape = CircleShape,
                containerColor = fabContainerColor,
                contentColor = fabContentColor,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 7.dp,
                    focusedElevation = 5.dp,
                    hoveredElevation = 5.dp,
                ),
            ) {
                Icon(centerTab.icon, contentDescription = centerTab.localizedLabel(), modifier = Modifier.size(25.dp))
            }
            Text(
                text = centerTab.localizedLabel(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FloatingBottomBarItem(
    item: MainTab,
    selected: Boolean,
    motionEnabled: Boolean,
    compactWidth: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = androidx.compose.animation.core.tween(if (motionEnabled) 180 else 0),
        label = "nav-tint-${item.name}",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected && motionEnabled) 1.12f else 1f,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.62f, stiffness = 520f) else tween(0),
        label = "nav-icon-scale-${item.name}",
    )
    val iconOffset by animateDpAsState(
        targetValue = if (selected && motionEnabled) (-2).dp else 0.dp,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.72f, stiffness = 520f) else tween(0),
        label = "nav-icon-offset-${item.name}",
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (compactWidth) 1.dp else 2.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        val pillWidth by animateDpAsState(
            targetValue = if (selected) maxWidth else 0.dp,
            animationSpec = if (motionEnabled) spring(dampingRatio = 0.74f, stiffness = 430f) else tween(0),
            label = "nav-selected-pill-${item.name}",
        )
        val pillHeight by animateDpAsState(
            targetValue = if (selected) 48.dp else 0.dp,
            animationSpec = if (motionEnabled) spring(dampingRatio = 0.76f, stiffness = 460f) else tween(0),
            label = "nav-selected-pill-height-${item.name}",
        )
        Surface(
            modifier = Modifier.width(pillWidth).height(pillHeight),
            shape = RoundedCornerShape(17.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = if (selected) 1.dp else 0.dp,
        ) {}
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.height(28.dp)) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .offset(y = iconOffset)
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = if (motionEnabled) iconScale else 1f
                            scaleY = if (motionEnabled) iconScale else 1f
                        },
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.localizedLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AnimatedTabIcon(icon: ImageVector, label: String, selected: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
        label = "nav-icon-$label",
    )
    Icon(
        imageVector = icon,
        contentDescription = label,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    )
}

/**
 * Compact small-arrow dropdown trigger positioned directly next to the Top Bar
 * Profile Avatar, housing Notifications (with unread badge count) and QR Login
 * and the permitted Vente quick action. Device pairing stays in Settings/Scan.
 */
@Composable
private fun TopBarQuickActionsMenu(
    notificationCount: Int,
    showSale: Boolean,
    onOpenNotifications: () -> Unit,
    onOpenSale: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "topBarArrowRotation",
    )
    val badgeText = if (notificationCount > 99) "99+" else notificationCount.toString()

    Box(contentAlignment = Alignment.Center) {
        BadgedBox(
            badge = {
                if (notificationCount > 0) {
                    Badge {
                        Text(badgeText)
                    }
                }
            },
        ) {
            Surface(
                onClick = { expanded = !expanded },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = atlasText("Menu rapide (Notifications et Vente)"),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = arrowRotation },
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = atlasText("Notifications"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = atlasText("Boîte d'alertes & activité"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                    )
                },
                trailingIcon = if (notificationCount > 0) {
                    {
                        Badge {
                            Text(badgeText)
                        }
                    }
                } else {
                    null
                },
                onClick = {
                    expanded = false
                    onOpenNotifications()
                },
            )
            if (showSale) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(atlasText("Vente"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(atlasText("Nouvelle vente / caisse"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    leadingIcon = { Icon(Icons.Outlined.ShoppingCart, contentDescription = null) },
                    onClick = { expanded = false; onOpenSale() },
                )
            }
        }
    }
}

