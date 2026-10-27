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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BugReport
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import ma.etherealmber.atlascommerce.ui.components.AtlasPageTransition
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.components.AtlasTopBar
import ma.etherealmber.atlascommerce.ui.components.UpdateMiniProgressBall
import ma.etherealmber.atlascommerce.ui.components.ShakeGestureEffect
import ma.etherealmber.atlascommerce.ui.root.AppViewModel
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentScreen
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
            if (Roles.isAdmin(role)) BottomBarTabs else listOf(HOME, PRODUCTS, SCAN, BALANCE)
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
    MainTab.AGENT -> atlasText("Atlas Agent", "Atlas Agent", "مساعد Atlas")
    MainTab.PROFILE -> atlasText("Mon profil", "My profile", "ملفي الشخصي")
    MainTab.SETTINGS -> atlasText("Paramètres", "Settings", "الإعدادات")
}

private val BackStackTabs = setOf(MainTab.AGENT, MainTab.SALE, MainTab.PROFILE, MainTab.SETTINGS)

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
    onOpenDeveloperTools: () -> Unit = {},
    onSignOut: () -> Unit,
    onStartUpdateDownload: () -> Unit = {},
    shellViewModel: ShellViewModel = hiltViewModel(),
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val cartCount by shellViewModel.cartCount.collectAsStateWithLifecycle()
    val shakeToReportEnabled by appViewModel.shakeToReportEnabled.collectAsStateWithLifecycle()
    val notificationCount by shellViewModel.notificationCount.collectAsStateWithLifecycle()
    val serverBaseUrl by shellViewModel.serverBaseUrl.collectAsStateWithLifecycle()

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
        else -> rawTab
    }
    val scope = rememberCoroutineScope()
    var showProblemReportDialog by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var showShakeReportPrompt by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }

    val shakeDisabledMessage = atlasText(
        "Raccourci secouer pour signaler désactivé.",
        "Shake-to-report shortcut disabled.",
        "تم تعطيل اختصار الهز للإبلاغ.",
    )
    ShakeGestureEffect(
        enabled = isForeground && shakeToReportEnabled && !showProblemReportDialog && !showShakeReportPrompt,
        onShake = { showShakeReportPrompt = true },
    )

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
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val switchTab: (MainTab) -> Unit = { target ->
        if (target == MainTab.SALE && !Roles.canCreateDocuments(user.role)) {
            onToast("Accès Vente réservé aux rôles Administrateur et Secrétaire.")
        } else {
            if (roleTabs.contains(target)) setLastBottomTabName(target.name)
            setTabName(target.name)
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AtlasTopBar(
                title = tab.localizedLabel(),
                titleLeading = if (tab == MainTab.AGENT) {
                    { AtlasAgentMascot(Modifier.size(28.dp)) }
                } else null,
                onBack = when (tab) {
                    MainTab.SETTINGS -> ({ setTabName(MainTab.PROFILE.name) })
                    MainTab.PROFILE, MainTab.AGENT, MainTab.SALE -> ({ setTabName(lastBottomTabName) })
                    else -> null
                },
                actions = {
                    val hasHiddenDownload =
                        (updateState as? UpdateState.Downloading)?.let { it.hiddenInForeground && !it.info.mandatory } == true
                    if (tab !in BackStackTabs) UpdateMiniProgressBall(
                        state = updateState,
                        onClick = onShowUpdateProgress,
                    )
                    if (hasHiddenDownload && tab !in BackStackTabs) {
                        Spacer(Modifier.width(6.dp))
                    }
                    if (tab in BackStackTabs) IconButton(onClick = { showProblemReportDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.BugReport,
                            contentDescription = atlasText("Signaler un problème", "Report a problem", "الإبلاغ عن مشكلة"),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    when (tab) {
                        MainTab.AGENT -> {
                            AtlasAvatar(
                                avatarUrl = user.avatarUrl,
                                label = user.username,
                                size = 34.dp,
                                serverBaseUrl = serverBaseUrl,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                onClick = { switchTab(MainTab.PROFILE) },
                            )
                            Spacer(Modifier.width(8.dp))
                        }

                        MainTab.SALE, MainTab.SETTINGS -> Unit

                        MainTab.PROFILE -> {
                            IconButton(onClick = { switchTab(MainTab.SETTINGS) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Paramètres",
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
                                    contentDescription = "Ouvrir Atlas Agent",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            AtlasAvatar(
                                avatarUrl = user.avatarUrl,
                                label = user.username,
                                size = 34.dp,
                                serverBaseUrl = serverBaseUrl,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                onClick = { switchTab(MainTab.PROFILE) },
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
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
        AtlasPageTransition(
            targetState = tab,
            indexSelector = { it.ordinal },
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
                        isActive = isForeground && tab == MainTab.AGENT && currentTab == MainTab.AGENT,
                    )

                    MainTab.PROFILE -> ProfileScreen(
                        user = user,
                        onOpenSettings = { switchTab(MainTab.SETTINGS) },
                        onSignOut = onSignOut,
                        onToast = onToast,
                    )

                    MainTab.BALANCE -> BalanceScreen(onToast = onToast)

                    MainTab.SETTINGS -> SettingsScreen(
                        user = user,
                        onChangeServer = onChangeServer,
                        onOpenQrDevice = { onOpenQrDevice(null) },
                        onOpenLicense = onOpenLicense,
                        onOpenSecurity = onOpenSecurity,
                        onOpenAbout = onOpenAbout,
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
    // Keep the navigation pill fully opaque for legible controls; only the surrounding
    // system/navigation background is transparent so page content can extend behind it.
    val barColor = MaterialTheme.colorScheme.surfaceContainerLow
    val motionEnabled = LocalAtlasAnimationsEnabled.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .height(84.dp),
    ) {
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(26.dp),
            color = barColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = null,
            tonalElevation = 0.dp,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leftTabs.forEach { item ->
                    FloatingBottomBarItem(
                        item = item,
                        selected = selected == item,
                        motionEnabled = motionEnabled,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(item) },
                    )
                }
                if (centerTab != null) Spacer(Modifier.width(64.dp))
                rightTabs.forEach { item ->
                    FloatingBottomBarItem(
                        item = item,
                        selected = selected == item,
                        motionEnabled = motionEnabled,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(item) },
                    )
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
                    .size(56.dp)
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = androidx.compose.animation.core.tween(if (motionEnabled) 180 else 0),
        label = "nav-tint-${item.name}",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 44.dp else 0.dp,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.78f, stiffness = 480f) else tween(0),
        label = "nav-indicator-${item.name}",
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
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(30.dp)) {
            Surface(
                modifier = Modifier.width(indicatorWidth).height(28.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {}
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
                        contentDescription = "Menu rapide (Notifications et Vente)",
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
                            text = "Notifications",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Boîte d'alertes & activité",
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
                            Text("Vente", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Nouvelle vente / caisse", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    leadingIcon = { Icon(Icons.Outlined.ShoppingCart, contentDescription = null) },
                    onClick = { expanded = false; onOpenSale() },
                )
            }
        }
    }
}

