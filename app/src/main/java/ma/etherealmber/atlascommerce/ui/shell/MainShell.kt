package ma.etherealmber.atlascommerce.ui.shell

import ma.etherealmber.atlascommerce.ui.components.rememberApiRefreshAction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.core.update.UpdateState
import ma.etherealmber.atlascommerce.ui.screens.settings.DataCacheManagerDialog
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasSurpriseDialog
import ma.etherealmber.atlascommerce.ui.components.nextSurprise
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingDockPadding
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingTopBarPadding
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingTopBarExpanded
import ma.etherealmber.atlascommerce.ui.theme.LocalAtlasDarkTheme
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
import ma.etherealmber.atlascommerce.ui.screens.employees.EmployeeDirectoryScreen
import ma.etherealmber.atlascommerce.ui.screens.clients.ClientsScreen
import ma.etherealmber.atlascommerce.ui.screens.products.ProductsScreen
import ma.etherealmber.atlascommerce.ui.screens.sale.SaleScreen
import ma.etherealmber.atlascommerce.ui.screens.scan.ScanScreen
import ma.etherealmber.atlascommerce.ui.screens.notifications.NotificationsScreen
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
    AGENT("Atlas Agent", Icons.Outlined.Bot),
    PROFILE("Mon profil", Icons.Outlined.Person),
    SETTINGS("Paramètres", Icons.Outlined.Settings),
    NOTIFICATIONS("Notifications", Icons.Outlined.Notifications),
    CLIENTS("Clients", Icons.Outlined.Groups),
    ;

    companion object {
        /** Profile, settings, sales, and Atlas Agent are pushed screens with a back action. */
        val BottomBarTabs = listOf(HOME, PRODUCTS, SCAN, BALANCE, EMPLOYEES)

        /** Financial and employee data remain guarded by server-authorized roles. */
        fun bottomTabsForRole(role: String?): List<MainTab> =
            when {
                Roles.isAdmin(role) -> BottomBarTabs
                Roles.normalize(role) == Roles.STOCK -> listOf(HOME, SCAN, PRODUCTS)
                Roles.normalize(role) == Roles.SECRETARY -> listOf(HOME, PRODUCTS, SCAN, BALANCE, EMPLOYEES)
                else -> listOf(HOME, PRODUCTS, SCAN, BALANCE)
            }

        /** Resolve the selected launch tab without changing the dock's stable visual order. */
        fun initialTabForRole(role: String?, preferredTabName: String?): MainTab {
            val allowedTabs = bottomTabsForRole(role)
            return allowedTabs.firstOrNull { it.name == preferredTabName }
                ?: allowedTabs.firstOrNull { it == HOME }
                ?: allowedTabs.first()
        }


    }
}

private data class ShellPageTarget(val tab: MainTab, val isDataStorage: Boolean = false)

@Composable
internal fun MainTab.localizedLabel(): String = when (this) {
    MainTab.HOME -> atlasText("Accueil", "Home", "الرئيسية")
    MainTab.PRODUCTS -> atlasText("Produits", "Products", "المنتجات")
    MainTab.SCAN -> atlasText("Scan", "Scan", "المسح")
    MainTab.SALE -> atlasText("Vente", "Sales", "المبيعات")
    MainTab.BALANCE -> atlasText("Balance", "Balance", "الرصيد")
    MainTab.EMPLOYEES -> atlasText("Employés", "Employees", "الموظفون")
    MainTab.AGENT -> atlasText("Atlas Agent", "Atlas Agent", "مساعد Atlas")
    MainTab.PROFILE -> atlasText("Mon profil", "My profile", "ملفي الشخصي")
    MainTab.SETTINGS -> atlasText("Paramètres", "Settings", "الإعدادات")
    MainTab.NOTIFICATIONS -> atlasText("Notifications", "Notifications", "الإشعارات")
    MainTab.CLIENTS -> atlasText("Clients", "Clients", "العملاء")
}

private val BackStackTabs = setOf(MainTab.AGENT, MainTab.SALE, MainTab.PROFILE, MainTab.SETTINGS, MainTab.NOTIFICATIONS, MainTab.CLIENTS)
private val FloatingDockTabs = setOf(MainTab.HOME, MainTab.PRODUCTS, MainTab.SCAN, MainTab.BALANCE, MainTab.EMPLOYEES)
private val FloatingDockOverlayTabs = setOf(MainTab.SALE)

@Composable
fun MainShell(
    user: SessionUser,
    defaultStartTab: String = MainTab.HOME.name,
    activeTabOverride: String? = null,
    onActiveTabChange: ((String) -> Unit)? = null,
    lastBottomTabOverride: String? = null,
    onLastBottomTabChange: ((String) -> Unit)? = null,
    sidebarCollapsed: Boolean = false,
    onSidebarCollapsedChange: (Boolean) -> Unit = {},
    isForeground: Boolean = true,
    externalTabRequest: String? = null,
    onExternalTabHandled: () -> Unit = {},
    updateState: UpdateState = UpdateState.Idle,
    onShowUpdateProgress: () -> Unit = {},
    onChangeServer: () -> Unit,
    onOpenQrDevice: (String?) -> Unit,
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
    val context = LocalContext.current
    val toastMessage: (String) -> Unit = { message -> android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show() }
    val cartCount by shellViewModel.cartCount.collectAsStateWithLifecycle()
    val shakeToReportEnabled by appViewModel.shakeToReportEnabled.collectAsStateWithLifecycle()
    val notificationCount by shellViewModel.notificationCount.collectAsStateWithLifecycle()
    val serverBaseUrl by shellViewModel.serverBaseUrl.collectAsStateWithLifecycle()
    val agentPolicy by appViewModel.agentPolicy.collectAsStateWithLifecycle()

    val roleTabs = MainTab.bottomTabsForRole(user.role)
    // The launch preference selects the initial screen; it must not reorder the dock.
    // The scan action occupies a fixed center slot, so reordering it to the edge squeezed
    // all remaining tabs into one half of the dock on compact Android screens.
    val resolvedInitial = MainTab.initialTabForRole(user.role, defaultStartTab).name

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
        rawTab == MainTab.EMPLOYEES && !(Roles.isAdmin(user.role) || Roles.normalize(user.role) == Roles.SECRETARY) -> MainTab.HOME
        rawTab == MainTab.CLIENTS && !Roles.canCreateDocuments(user.role) -> MainTab.HOME
        rawTab == MainTab.BALANCE && Roles.normalize(user.role) == Roles.STOCK -> MainTab.HOME
        else -> rawTab
    }
    var surpriseIndex by rememberSaveable(user.scopeIdentity) { mutableStateOf(-1) }
    var showAvatarSurprise by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var showAgentSurprise by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var agentHistoryRequested by remember { mutableStateOf(false) }
    val agentViewModel: AgentViewModel? = if (tab == MainTab.AGENT) hiltViewModel() else null
    val refreshAgentContext = rememberApiRefreshAction(
        key = "agent-context",
        cooldownMs = 5_000L,
        onRefresh = { agentViewModel?.refreshContext() },
    )
    val refreshAgentPolicy = rememberApiRefreshAction("agent-policy", 5_000L, appViewModel::refreshAgentPolicy)
    val agentState = agentViewModel?.state?.collectAsStateWithLifecycle()?.value
    var showProblemReportDialog by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var showShakeReportPrompt by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var showAgentQuotaDialog by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var showDataCacheManagerDialog by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var releaseHistorySubpageActive by remember(user.scopeIdentity) { mutableStateOf(false) }
    var showSidebarSignOutConfirmation by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var productHistoryActive by remember(user.scopeIdentity) { mutableStateOf(false) }
    var pendingAgentReportRequest by remember(user.scopeIdentity) { mutableStateOf<AgentNavigationSuggestion?>(null) }
    LaunchedEffect(tab) {
        if (tab != MainTab.AGENT) showAgentQuotaDialog = false
        if (tab != MainTab.PRODUCTS) productHistoryActive = false
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
                        toastMessage(shakeDisabledMessage)
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
            onToast = toastMessage,
        )
    }

    LaunchedEffect(externalTabRequest) {
        if (!externalTabRequest.isNullOrBlank()) {
            val targetAllowed = when (externalTabRequest) {
                MainTab.SALE.name -> Roles.canCreateDocuments(user.role)
                MainTab.EMPLOYEES.name -> Roles.isAdmin(user.role) || Roles.normalize(user.role) == Roles.SECRETARY
                MainTab.CLIENTS.name -> Roles.canCreateDocuments(user.role)
                MainTab.BALANCE.name -> Roles.normalize(user.role) != Roles.STOCK
                else -> true
            }
            val resolved = if (targetAllowed) externalTabRequest else MainTab.HOME.name
            showDataCacheManagerDialog = false
            if (resolved != MainTab.SETTINGS.name) releaseHistorySubpageActive = false
            setTabName(resolved)
            if (roleTabs.any { it.name == resolved }) setLastBottomTabName(resolved)
            onExternalTabHandled()
        }
    }

    LaunchedEffect(tab) {
        if (tab != MainTab.SETTINGS) releaseHistorySubpageActive = false
        AtlasLogBuffer.log("NAVIGATION", "Main tab selected: ${tab.name}")
    }

    LaunchedEffect(tab, isForeground) {
        if (isForeground) {
            shellViewModel.refreshNotificationsBadge()
        }
    }

    val onToast: (String) -> Unit = { message ->
        toastMessage(atlasNotice(message, atlasLanguage))
    }
    val switchTab: (MainTab) -> Unit = { target ->
        showDataCacheManagerDialog = false
        if (target != MainTab.SETTINGS) releaseHistorySubpageActive = false
        if (target == MainTab.BALANCE && Roles.normalize(user.role) == Roles.STOCK) {
            setTabName(MainTab.HOME.name)
        } else if (target == MainTab.SALE && !Roles.canCreateDocuments(user.role)) {
            onToast("Accès Vente réservé aux rôles Administrateur et Secrétaire.")
        } else {
            if (roleTabs.contains(target)) setLastBottomTabName(target.name)
            setTabName(target.name)
        }
    }
    val openDataCacheManager: () -> Unit = {
        releaseHistorySubpageActive = false
        showDataCacheManagerDialog = true
    }
    val closeDataCacheManager: () -> Unit = {
        showDataCacheManagerDialog = false
        releaseHistorySubpageActive = false
    }
    val openNotificationsForLayout: () -> Unit = {
        switchTab(MainTab.NOTIFICATIONS)
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
            AgentNavigationDestination.NOTIFICATIONS -> openNotificationsForLayout()
            AgentNavigationDestination.SETTINGS -> switchTab(MainTab.SETTINGS)
            AgentNavigationDestination.PROFILE -> switchTab(MainTab.PROFILE)
            AgentNavigationDestination.DATA_STORAGE -> openDataCacheManager()
        }
    }

    BackHandler(enabled = isForeground && tab != MainTab.HOME && !productHistoryActive && !releaseHistorySubpageActive) {
        AtlasLogBuffer.log("NAVIGATION", "System Back handled by shell on tab=${tab.name}")
        showDataCacheManagerDialog = false
        when (tab) {
            MainTab.SETTINGS -> setTabName(MainTab.PROFILE.name)
            MainTab.PROFILE, MainTab.AGENT, MainTab.SALE, MainTab.NOTIFICATIONS, MainTab.CLIENTS -> setTabName(lastBottomTabName)
            else -> switchTab(MainTab.HOME)
        }
    }
    BackHandler(enabled = isForeground && tab == MainTab.HOME) {
        AtlasLogBuffer.log("NAVIGATION", "System Back signs out from the dashboard to the sign-in screen")
        onSignOut()
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val shellMaxHeight = maxHeight
    val useRail = maxWidth >= 600.dp
    val useDesktopSidebar = maxWidth >= 1000.dp
    val pageTarget = ShellPageTarget(tab = tab, isDataStorage = useRail && showDataCacheManagerDialog)
    // A full drawer consumes too much of a medium-width window (especially split-screen).
    // Keep those windows on a compact rail; allow an expanded drawer from 840dp upward.
    val sidebarCanExpand = maxWidth >= 840.dp
    val sidebarExpanded = sidebarCanExpand && !sidebarCollapsed
    val sidebarWidth by animateDpAsState(
        targetValue = if (!useRail) 0.dp else if (sidebarExpanded) 264.dp else 76.dp,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 360f),
        label = "atlas-sidebar-width",
    )
    if (showDataCacheManagerDialog && !useRail) {
        DataCacheManagerDialog(
            onDismiss = closeDataCacheManager,
            onToast = toastMessage,
            appViewModel = appViewModel,
        )
    }

    val showShellTopBar = isForeground && (
        (!useRail && !productHistoryActive && !releaseHistorySubpageActive) ||
            (useRail && tab in setOf(MainTab.AGENT, MainTab.SALE, MainTab.NOTIFICATIONS, MainTab.CLIENTS) && !productHistoryActive)
        )
    var shellTopBarVisible by remember { mutableStateOf(false) }
    LaunchedEffect(showShellTopBar) {
        shellTopBarVisible = showShellTopBar
    }
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    val showBottomNavigation = !useRail && (tab in FloatingDockTabs || tab in FloatingDockOverlayTabs) &&
        !productHistoryActive && !releaseHistorySubpageActive
    val dockSelectedTab = roleTabs.firstOrNull { it.name == lastBottomTabName }
        ?: roleTabs.firstOrNull()
        ?: MainTab.HOME
    val floatingTopBarExpanded = remember(tab) { mutableStateOf(true) }
    val collapsedHeaderDescription = atlasText("Développer l’en-tête", "Expand header", "توسيع الرأس")
    val scrollDelta = remember(tab) { mutableFloatStateOf(0f) }
    LaunchedEffect(tab, productHistoryActive, releaseHistorySubpageActive, isForeground) {
        floatingTopBarExpanded.value = true
        scrollDelta.floatValue = 0f
    }
    val scrollDirectionThresholdPx = with(LocalDensity.current) { 12.dp.toPx() }
    val floatingBarScrollConnection = remember(tab, useRail, scrollDirectionThresholdPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val verticalDelta = if (consumed.y != 0f) consumed.y else available.y
                if ((useRail && tab != MainTab.AGENT) || verticalDelta == 0f) return Offset.Zero
                scrollDelta.floatValue += verticalDelta
                when {
                    scrollDelta.floatValue <= -scrollDirectionThresholdPx -> {
                        floatingTopBarExpanded.value = false
                        scrollDelta.floatValue = 0f
                    }
                    scrollDelta.floatValue >= scrollDirectionThresholdPx -> {
                        floatingTopBarExpanded.value = true
                        scrollDelta.floatValue = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }
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
            AnimatedVisibility(
                visible = shellTopBarVisible,
                modifier = Modifier.graphicsLayer { clip = false },
                enter = if (motionEnabled) {
                    fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                            initialOffsetY = { height -> -height / 4 },
                        ) + scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.98f)
                } else EnterTransition.None,
                exit = if (motionEnabled) {
                    fadeOut(tween(150, easing = FastOutSlowInEasing)) +
                        slideOutVertically(
                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                            targetOffsetY = { height -> -height / 5 },
                        ) + scaleOut(tween(150, easing = FastOutSlowInEasing), targetScale = 0.98f)
                } else ExitTransition.None,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(
                            start = if (useRail) sidebarWidth + 20.dp else 20.dp,
                            end = 20.dp,
                            top = 18.dp,
                            bottom = 18.dp,
                        )
                        .graphicsLayer { clip = false },
                ) {
                    Surface(
                        modifier = Modifier
                            .graphicsLayer { clip = false }
                            .then(
                                if (floatingTopBarExpanded.value) {
                                    Modifier.fillMaxWidth()
                                } else {
                                    Modifier
                                        .align(Alignment.Center)
                                        .clickable(role = Role.Button) {
                                            scrollDelta.floatValue = 0f
                                            floatingTopBarExpanded.value = true
                                        }
                                        .semantics {
                                            contentDescription = collapsedHeaderDescription
                                        }
                                },
                            ),
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                        shadowElevation = 8.dp,
                    ) {
                        AnimatedContent(
                            targetState = floatingTopBarExpanded.value,
                            transitionSpec = {
                                val contentTransform = if (motionEnabled) {
                                    (fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                                        scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.94f)) togetherWith
                                        (fadeOut(tween(130, easing = FastOutSlowInEasing)) +
                                            scaleOut(tween(150, easing = FastOutSlowInEasing), targetScale = 0.96f))
                                } else {
                                    EnterTransition.None togetherWith ExitTransition.None
                                }
                                contentTransform.using(if (motionEnabled) SizeTransform(clip = false) else null)
                            },
                            label = "floating-top-bar-content",
                        ) { expanded ->
                            if (expanded) {
                                AtlasTopBar(
                                    title = if (!useRail || tab in setOf(MainTab.AGENT, MainTab.SALE, MainTab.NOTIFICATIONS, MainTab.CLIENTS)) tab.localizedLabel() else "",
                                    subtitle = null,
                                    windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal),
                                    containerColor = Color.Transparent,
                                    titleLeading = if (tab == MainTab.AGENT) {
                                        {
                                            AtlasAgentSurpriseMascot(
                                                modifier = Modifier.size(30.dp),
                                                isTyping = agentState?.input?.isNotBlank() == true,
                                                isThinking = agentState?.sending == true,
                                                onSurprise = {
                                                    surpriseIndex = nextSurprise(surpriseIndex, 5)
                                                    showAgentSurprise = true
                                                },
                                            )
                                        }
                                    } else null,
                                    onBack = if (useRail) null else when (tab) {
                                        MainTab.SETTINGS -> ({ setTabName(MainTab.PROFILE.name) })
                                        MainTab.PROFILE, MainTab.AGENT, MainTab.SALE, MainTab.NOTIFICATIONS, MainTab.CLIENTS -> ({ setTabName(lastBottomTabName) })
                                        else -> null
                                    },
                                    actions = {
                                        val openAgentDescription = atlasText(
                                            "Ouvrir Atlas Agent",
                                            "Open Atlas Agent",
                                            "فتح Atlas Agent",
                                        )
                                        val hasHiddenUpdate = when (val state = updateState) {
                                            is UpdateState.Downloading -> state.hiddenInForeground && !state.info.mandatory
                                            is UpdateState.Available -> state.dismissedBanner && !state.info.mandatory
                                            is UpdateState.ReadyToInstall -> state.dismissedBanner && !state.info.mandatory
                                            else -> false
                                        }
                                        if (isForeground && !useRail && tab != MainTab.AGENT) {
                                            UpdateMiniProgressBall(
                                                state = updateState,
                                                onClick = onShowUpdateProgress,
                                            )
                                        }
                                        if (hasHiddenUpdate && isForeground) Spacer(Modifier.width(6.dp))
                                        if (!useRail && tab !in BackStackTabs) {
                                            IconButton(
                                                onClick = { switchTab(MainTab.AGENT) },
                                                modifier = Modifier.semantics {
                                                    contentDescription = openAgentDescription
                                                },
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.AutoAwesome,
                                                    contentDescription = openAgentDescription,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp),
                                                )
                                            }
                                        }
                                        if (tab == MainTab.AGENT) {
                                            IconButton(onClick = { agentHistoryRequested = true }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.History,
                                                    contentDescription = atlasText("Historique", "Conversation history", "سجل المحادثات"),
                                                )
                                            }
                                            IconButton(onClick = { agentViewModel?.startNewConversation() }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.AddComment,
                                                    contentDescription = atlasText("Nouvelle discussion", "New conversation", "محادثة جديدة"),
                                                )
                                            }
                                            IconButton(onClick = refreshAgentContext) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Refresh,
                                                    contentDescription = atlasText("Actualiser le contexte", "Refresh context", "تحديث السياق"),
                                                )
                                            }
                                            IconButton(onClick = {
                                                showAgentQuotaDialog = true
                                                refreshAgentPolicy()
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.HourglassBottom,
                                                    contentDescription = atlasText("Quota IA", "AI quota", "حصة الذكاء الاصطناعي"),
                                                )
                                            }
                                        }
                                        when (tab) {
                                            MainTab.AGENT, MainTab.SALE -> Unit

                                            MainTab.SETTINGS -> {
                                                if (!useRail && isForeground) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                        tonalElevation = 1.dp,
                                                    ) {
                                                        IconButton(onClick = openDataCacheManager, modifier = Modifier.size(40.dp)) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Inventory2,
                                                                contentDescription = atlasText("Données et cache", "Data & cache manager", "البيانات والذاكرة المؤقتة"),
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            MainTab.PROFILE -> {
                                                // On tablets these destinations already live in the sidebar.
                                                if (!useRail) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                        tonalElevation = 1.dp,
                                                    ) {
                                                        IconButton(onClick = { switchTab(MainTab.SETTINGS) }, modifier = Modifier.size(40.dp)) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Settings,
                                                                contentDescription = atlasText("Paramètres", "Settings", "الإعدادات"),
                                                            )
                                                        }
                                                    }
                                                    Spacer(Modifier.width(4.dp))
                                                    if (isForeground) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                            tonalElevation = 1.dp,
                                                        ) {
                                                            IconButton(onClick = openDataCacheManager, modifier = Modifier.size(40.dp)) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.Inventory2,
                                                                    contentDescription = atlasText("Données et cache", "Data & cache manager", "البيانات والذاكرة المؤقتة"),
                                                                )
                                                            }
                                                        }
                                                        Spacer(Modifier.width(4.dp))
                                                    }
                                                }
                                            }

                                            else -> {
                                                // Tablet navigation is in the sidebar; keep its notifications,
                                                // sale, and account actions out of the top-bar dropdown.
                                                if (!useRail) {
                                                    TopBarQuickActionsMenu(
                                                        notificationCount = notificationCount,
                                                        showSale = Roles.canCreateDocuments(user.role),
                                                        showClients = Roles.canCreateDocuments(user.role),
                                                        onOpenNotifications = openNotificationsForLayout,
                                                        onOpenSale = { switchTab(MainTab.SALE) },
                                                        onOpenClients = { switchTab(MainTab.CLIENTS) },
                                                    )
                                                    Spacer(Modifier.width(6.dp))
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
                                        }
                                    },
                                )
                            } else {
                                val compactUpdateVisible = when (updateState) {
                                    is UpdateState.Available,
                                    is UpdateState.Downloading,
                                    is UpdateState.ReadyToInstall -> true
                                    is UpdateState.Error -> updateState.info != null
                                    else -> false
                                }
                                val showCompactUpdate = isForeground && compactUpdateVisible
                                val showCompactAccessories = tab == MainTab.AGENT || showCompactUpdate
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (showCompactAccessories) {
                                        Box(Modifier.width(38.dp), contentAlignment = Alignment.CenterEnd) {
                                            if (tab == MainTab.AGENT) {
                                                AtlasAgentSurpriseMascot(
                                                    modifier = Modifier.size(28.dp),
                                                    isTyping = agentState?.input?.isNotBlank() == true,
                                                    isThinking = agentState?.sending == true,
                                                    onSurprise = {
                                                        surpriseIndex = nextSurprise(surpriseIndex, 5)
                                                        showAgentSurprise = true
                                                    },
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = tab.localizedLabel(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                    if (showCompactAccessories) {
                                        Spacer(Modifier.width(4.dp))
                                        Box(Modifier.width(38.dp), contentAlignment = Alignment.CenterStart) {
                                            if (showCompactUpdate) {
                                                UpdateMiniProgressBall(
                                                    state = updateState,
                                                    onClick = onShowUpdateProgress,
                                                    forceVisible = true,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val safeTopInset = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
        Box(
            Modifier
                .fillMaxSize()
                .nestedScroll(floatingBarScrollConnection)
                .padding(
                    start = padding.calculateLeftPadding(layoutDirection),
                    top = safeTopInset,
                    end = padding.calculateRightPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding(),
                )
                .background(MaterialTheme.colorScheme.background),
        ) {
        val showRail = useRail
        val sidebarStartInset = if (layoutDirection == LayoutDirection.Ltr) {
            padding.calculateLeftPadding(layoutDirection)
        } else {
            padding.calculateRightPadding(layoutDirection)
        }
        val sidebarEdgeOffset = if (layoutDirection == LayoutDirection.Ltr) -sidebarStartInset else sidebarStartInset
        val sidebarTopInset = safeTopInset
        val sidebarBottomInset = padding.calculateBottomPadding()
        val sidebarContentTopLift = if (useRail && showShellTopBar) sidebarTopInset else 0.dp
        val sidebarHeight = (shellMaxHeight - sidebarBottomInset).coerceAtLeast(0.dp)
        val sidebarBackgroundHeight = shellMaxHeight
        if (showRail) {
            // Extend the rail surface behind the top bar. On tablet destinations with
            // a top bar, lift the contents into its transparent sidebar region as well.
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = sidebarEdgeOffset, y = -sidebarTopInset)
                    .width(sidebarWidth + sidebarStartInset)
                    .requiredHeight(sidebarBackgroundHeight),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {}
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = sidebarEdgeOffset, y = -sidebarContentTopLift)
                    .width(sidebarWidth + sidebarStartInset)
                    .height(sidebarHeight + sidebarContentTopLift),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                AtlasSidebarContent(
                    user = user,
                    serverBaseUrl = serverBaseUrl,
                    roleTabs = roleTabs,
                    selectedTab = tab,
                    cartCount = cartCount,
                    notificationCount = notificationCount,
                    updateState = updateState,
                    expanded = sidebarExpanded,
                    onOpenDataStorageManager = openDataCacheManager,
                    dataStorageSelected = showDataCacheManagerDialog,
                    canCollapse = sidebarCanExpand,
                    onToggleCollapse = { onSidebarCollapsedChange(!sidebarCollapsed) },
                    onNavigate = switchTab,
                    onOpenNotifications = openNotificationsForLayout,
                    onRequestSignOut = { showSidebarSignOutConfirmation = true },
                    onAvatarSurprise = {
                        surpriseIndex = nextSurprise(surpriseIndex, 5)
                        showAvatarSurprise = true
                    },
                    showBrand = true,
                )
            }
            VerticalDivider(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = sidebarEdgeOffset)
                    .padding(start = sidebarWidth - 1.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        val navigationInset = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
        CompositionLocalProvider(
            LocalAtlasFloatingDockPadding provides if (showBottomNavigation) 116.dp + navigationInset else 0.dp,
            LocalAtlasFloatingTopBarPadding provides if (showShellTopBar && shellTopBarVisible) 76.dp else 0.dp,
            LocalAtlasFloatingTopBarExpanded provides floatingTopBarExpanded.value,
        ) {
        AtlasPageTransition(
            targetState = pageTarget,
            indexSelector = { it.tab.ordinal },
            tabletMode = useRail,
            // The dock floats over the page so content can scroll beneath it. Main scrollable
            // screens add end content-padding so their final items can still scroll clear of it.
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (showRail) sidebarWidth else 0.dp),
        ) { currentPage ->
            val currentTab = currentPage.tab
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (useRail) Modifier.background(MaterialTheme.colorScheme.background) else Modifier)
                    .then(if (useRail) Modifier.zIndex(if (currentPage == pageTarget) 1f else 0f) else Modifier),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(Modifier.widthIn(max = if (useRail) 1440.dp else 840.dp).fillMaxSize()) {
                    if (useRail && !currentPage.isDataStorage && currentTab != MainTab.NOTIFICATIONS && currentTab != MainTab.AGENT && !releaseHistorySubpageActive) {
                        Text(
                            currentTab.localizedLabel(),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    if (useRail && currentPage.isDataStorage) {
                        DataCacheManagerDialog(
                            onDismiss = closeDataCacheManager,
                            onToast = toastMessage,
                            appViewModel = appViewModel,
                            tabletSidebarWidth = 0.dp,
                            isForeground = isForeground,
                        )
                    } else {
                        Box(Modifier.fillMaxSize().weight(1f)) {
                when (currentTab) {
                    MainTab.HOME -> HomeScreen(
                        user = user,
                        onOpenProducts = { switchTab(MainTab.PRODUCTS) },
                        onOpenSale = { switchTab(MainTab.SALE) },
                        onOpenScan = { switchTab(MainTab.SCAN) },
                        onOpenNotifications = openNotificationsForLayout,
                        onToast = onToast,
                    )

                    MainTab.PRODUCTS -> ProductsScreen(
                        user = user,
                        onOpenScan = { switchTab(MainTab.SCAN) },
                        onOpenSale = { switchTab(MainTab.SALE) },
                        onToast = onToast,
                        onHistoryPageVisibilityChanged = { productHistoryActive = it },
                        showPageTitle = false,
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

                    MainTab.EMPLOYEES -> EmployeeDirectoryScreen(user = user)

                    MainTab.CLIENTS -> ClientsScreen()

                    MainTab.AGENT -> AgentScreen(
                        user = user,
                        appViewModel = appViewModel,
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
                        onOpenQrDevice = { onOpenQrDevice(null) },
                        onSignOut = onSignOut,
                        onToast = onToast,
                        isForeground = isForeground,
                    )

                    MainTab.BALANCE -> BalanceScreen(
                        onToast = onToast,
                        requestedReport = pendingAgentReportRequest,
                        onReportRequestHandled = { pendingAgentReportRequest = null },
                    )

                    MainTab.NOTIFICATIONS -> NotificationsScreen(
                        onNavigateHref = { href ->
                            val route = href.orEmpty().lowercase()
                            switchTab(when {
                                route.contains("product") || route.contains("stock") -> MainTab.PRODUCTS
                                route.contains("invoice") || route.contains("sale") -> MainTab.SALE
                                route.contains("scan") -> MainTab.SCAN
                                route.contains("assist") || route.contains("agent") -> MainTab.AGENT
                                else -> MainTab.HOME
                            })
                        },
                        onToast = onToast,
                    )

                    MainTab.SETTINGS -> SettingsScreen(
                        user = user,
                        onChangeServer = onChangeServer,
                        onOpenLicense = onOpenLicense,
                        onOpenSecurity = onOpenSecurity,
                        onOpenAbout = onOpenAbout,
                        onOpenSnake = onOpenSnake,
                        onOpenDeveloperTools = onOpenDeveloperTools,
                        onSignOut = onSignOut,
                        onStartUpdateDownload = onStartUpdateDownload,
                        onReleaseHistoryVisibilityChanged = { releaseHistorySubpageActive = it },
                        onToast = onToast,
                    )
                }
                    }
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
                TextButton(onClick = refreshAgentPolicy) {
                    Text(atlasText("Actualiser", "Refresh", "تحديث"))
                }
            },
        )
    }
    if (!useRail) {
        AnimatedVisibility(
            visible = showBottomNavigation,
            modifier = Modifier.align(Alignment.BottomCenter).width(floatingBarWidth).graphicsLayer { clip = false },
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
                        targetOffsetY = { height -> height },
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
                selected = dockSelectedTab,
                onSelect = switchTab,
            )
        }
    }
    if (showSidebarSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = { showSidebarSignOutConfirmation = false },
            title = { Text(atlasText("Se déconnecter ?", "Sign out?", "تسجيل الخروج؟")) },
            text = {
                Text(atlasText(
                    "Vous devrez vous reconnecter pour accéder à votre espace Atlas.",
                    "You'll need to sign in again to access your Atlas workspace.",
                    "ستحتاج إلى تسجيل الدخول مجدداً للوصول إلى مساحة Atlas.",
                ))
            },
            confirmButton = {
                TextButton(onClick = {
                    showSidebarSignOutConfirmation = false
                    onSignOut()
                }) { Text(atlasText("Se déconnecter", "Sign out", "تسجيل الخروج")) }
            },
            dismissButton = {
                TextButton(onClick = { showSidebarSignOutConfirmation = false }) {
                    Text(atlasText("Annuler", "Cancel", "إلغاء"))
                }
            },
        )
    }
    }
}


@Composable
private fun AtlasAgentSurpriseMascot(
    modifier: Modifier = Modifier,
    isTyping: Boolean,
    isThinking: Boolean,
    onSurprise: () -> Unit,
) {
    val description = atlasText(
        "Mascotte Atlas Agent. Appuyez trois fois ou maintenez pour afficher une surprise",
        "Atlas Agent mascot. Triple-tap or long-press for a surprise",
        "تميمة Atlas Agent. انقر ثلاث مرات أو اضغط مطولاً لعرض مفاجأة",
    )
    val actionLabel = atlasText("Afficher une surprise", "Show a surprise", "إظهار مفاجأة")
    AtlasAgentMascot(
        modifier = modifier
            .atlasTapSequenceGestures(onTripleTap = onSurprise, onLongPress = onSurprise)
            .semantics {
                contentDescription = description
                customActions = listOf(CustomAccessibilityAction(actionLabel) {
                    onSurprise()
                    true
                })
            },
        isTyping = isTyping,
        isThinking = isThinking,
    )
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
    val barColor = MaterialTheme.colorScheme.surface
    val motionEnabled = LocalAtlasAnimationsEnabled.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 18.dp)
            .height(100.dp)
            .graphicsLayer { clip = false },
    ) {
        val compactWidth = maxWidth < 360.dp
        val dockWidth = if (maxWidth >= 680.dp) 640.dp else maxWidth
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).width(dockWidth).height(64.dp),
            shape = RoundedCornerShape(26.dp),
            color = barColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
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
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp,
                    focusedElevation = 7.dp,
                    hoveredElevation = 7.dp,
                ),
            ) {
                Icon(centerTab.icon, contentDescription = centerTab.localizedLabel(), modifier = Modifier.size(25.dp))
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(if (compactWidth) 52.dp else 64.dp)
                    .height(64.dp)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.height(28.dp))
                Spacer(Modifier.height(2.dp))
                Text(
                    text = centerTab.localizedLabel(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compactWidth) 10.sp else MaterialTheme.typography.labelSmall.fontSize),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
    val interactionSource = remember(item) { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && motionEnabled) 0.9f else 1f,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.66f, stiffness = 620f) else tween(0),
        label = "nav-icon-press-${item.name}",
    )
    val pillScale by animateFloatAsState(
        targetValue = if (selected && motionEnabled) 1f else 0.94f,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.76f, stiffness = 480f) else tween(0),
        label = "nav-pill-scale-${item.name}",
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .semantics { this.selected = selected }
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (compactWidth) 1.dp else 2.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        val pillWidth by animateDpAsState(
            targetValue = if (selected) maxWidth else 0.dp,
            animationSpec = if (motionEnabled) spring(dampingRatio = 0.78f, stiffness = 440f) else tween(0),
            label = "nav-selected-pill-${item.name}",
        )
        val pillHeight by animateDpAsState(
            targetValue = if (selected) 48.dp else 0.dp,
            animationSpec = if (motionEnabled) spring(dampingRatio = 0.78f, stiffness = 480f) else tween(0),
            label = "nav-selected-pill-height-${item.name}",
        )
        Surface(
            modifier = Modifier
                .width(pillWidth)
                .height(pillHeight)
                .graphicsLayer { scaleX = pillScale; scaleY = pillScale },
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
                            scaleX = (if (motionEnabled) iconScale else 1f) * pressScale
                            scaleY = (if (motionEnabled) iconScale else 1f) * pressScale
                        },
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.localizedLabel(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compactWidth) 10.sp else MaterialTheme.typography.labelSmall.fontSize),
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
    showClients: Boolean,
    onOpenNotifications: () -> Unit,
    onOpenSale: () -> Unit,
    onOpenClients: () -> Unit,
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
            if (showClients) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(atlasText("Clients", "Clients", "العملاء"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(atlasText("Crédits et soldes", "Credits and balances", "الائتمان والأرصدة"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    leadingIcon = { Icon(Icons.Outlined.Groups, contentDescription = null) },
                    onClick = { expanded = false; onOpenClients() },
                )
            }
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

