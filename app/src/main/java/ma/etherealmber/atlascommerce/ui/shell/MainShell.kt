package ma.etherealmber.atlascommerce.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.core.update.UpdateState
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasPageTransition
import ma.etherealmber.atlascommerce.ui.components.AtlasTopBar
import ma.etherealmber.atlascommerce.ui.components.UpdateMiniProgressBall
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentScreen
import ma.etherealmber.atlascommerce.ui.screens.balance.BalanceScreen
import ma.etherealmber.atlascommerce.ui.screens.home.HomeScreen
import ma.etherealmber.atlascommerce.ui.screens.products.ProductsScreen
import ma.etherealmber.atlascommerce.ui.screens.sale.SaleScreen
import ma.etherealmber.atlascommerce.ui.screens.scan.ScanScreen
import ma.etherealmber.atlascommerce.ui.screens.settings.ProfileScreen
import ma.etherealmber.atlascommerce.ui.screens.settings.SettingsScreen

enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Accueil", Icons.Outlined.Home),
    PRODUCTS("Produits", Icons.Outlined.Inventory2),
    SCAN("Scan", Icons.Outlined.QrCodeScanner),
    SALE("Vente", Icons.Outlined.ShoppingCart),
    BALANCE("Balance", Icons.Outlined.AccountBalanceWallet),
    AGENT("Agent", Icons.Outlined.AutoAwesome),
    PROFILE("Mon profil", Icons.Outlined.Person),
    SETTINGS("Paramètres", Icons.Outlined.Settings),
    ;

    companion object {
        /**
         * Neither `PROFILE` nor `SETTINGS` appears in the Bottom Navigation Bar:
         * tapping the Top Bar Avatar opens `PROFILE` (Account + Customizations ONLY),
         * and the top-right Settings icon inside `PROFILE` opens `SETTINGS`.
         */
        val BottomBarTabs = listOf(HOME, PRODUCTS, SCAN, BALANCE, AGENT)

        /** Balance is always in navigation; financial figures remain server-authorized. */
        fun bottomTabsForRole(@Suppress("UNUSED_PARAMETER") role: String?): List<MainTab> = BottomBarTabs
    }
}

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
    onSignOut: () -> Unit,
    onStartUpdateDownload: () -> Unit = {},
    shellViewModel: ShellViewModel = hiltViewModel(),
) {
    val cartCount by shellViewModel.cartCount.collectAsStateWithLifecycle()
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
    val lastBottomTabName = lastBottomTabOverride ?: internalLastBottomTabName
    val setLastBottomTabName: (String) -> Unit = { next ->
        internalLastBottomTabName = next
        onLastBottomTabChange?.invoke(next)
    }

    val rawTab = MainTab.entries.firstOrNull { it.name == tabName } ?: MainTab.HOME
    val tab = if (rawTab == MainTab.SALE && !Roles.canCreateDocuments(user.role)) {
        MainTab.HOME
    } else {
        rawTab
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(externalTabRequest) {
        if (!externalTabRequest.isNullOrBlank()) {
            val targetAllowed = externalTabRequest != MainTab.SALE.name || Roles.canCreateDocuments(user.role)
            val resolved = if (targetAllowed) externalTabRequest else MainTab.PRODUCTS.name
            setTabName(resolved)
            if (resolved != MainTab.PROFILE.name && resolved != MainTab.SETTINGS.name) {
                setLastBottomTabName(resolved)
            }
            onExternalTabHandled()
        }
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
            if (target != MainTab.PROFILE && target != MainTab.SETTINGS) {
                setLastBottomTabName(target.name)
            }
            setTabName(target.name)
        }
    }

    BackHandler(enabled = isForeground && tab != MainTab.HOME) {
        when (tab) {
            MainTab.SETTINGS -> setTabName(MainTab.PROFILE.name)
            MainTab.PROFILE -> setTabName(lastBottomTabName)
            else -> switchTab(MainTab.HOME)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AtlasTopBar(
                title = tab.label,
                subtitle = "${user.displayName} · ${Roles.label(user.role)}",
                onBack = when (tab) {
                    MainTab.SETTINGS -> ({ setTabName(MainTab.PROFILE.name) })
                    MainTab.PROFILE -> ({ setTabName(lastBottomTabName) })
                    else -> null
                },
                actions = {
                    val hasHiddenDownload =
                        (updateState as? UpdateState.Downloading)?.let { it.hiddenInForeground && !it.info.mandatory } == true
                    if (tab != MainTab.SETTINGS) UpdateMiniProgressBall(
                        state = updateState,
                        onClick = onShowUpdateProgress,
                    )
                    if (hasHiddenDownload && tab != MainTab.SETTINGS) {
                        Spacer(Modifier.width(6.dp))
                    }
                    when (tab) {
                        MainTab.PROFILE -> {
                            IconButton(onClick = { switchTab(MainTab.SETTINGS) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Paramètres",
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                        }

                        MainTab.SETTINGS -> {
                            Spacer(Modifier.width(8.dp))
                        }

                        else -> {
                            TopBarQuickActionsMenu(
                                notificationCount = notificationCount,
                                showSale = Roles.canCreateDocuments(user.role),
                                onOpenNotifications = onOpenNotifications,
                                onOpenSale = { switchTab(MainTab.SALE) },
                            )
                            Spacer(Modifier.width(6.dp))
                            AtlasAvatar(
                                avatarUrl = user.avatarUrl,
                                label = user.displayName,
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
        bottomBar = {
            if (!useRail) NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 5.dp,
            ) {
                roleTabs.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { switchTab(item) },
                        icon = {
                            if (item == MainTab.SALE && cartCount > 0) {
                                BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                                    AnimatedTabIcon(item.icon, item.label, selected = tab == item)
                                }
                            } else {
                                AnimatedTabIcon(item.icon, item.label, selected = tab == item)
                            }
                        },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        if (useRail) {
            NavigationRail(modifier = Modifier.align(Alignment.CenterStart), containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                roleTabs.forEach { item ->
                    NavigationRailItem(
                        selected = tab == item,
                        onClick = { switchTab(item) },
                        icon = {
                            if (item == MainTab.SALE && cartCount > 0) {
                                BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) { AnimatedTabIcon(item.icon, item.label, selected = tab == item) }
                            } else AnimatedTabIcon(item.icon, item.label, selected = tab == item)
                        },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
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
                .padding(start = if (useRail) 80.dp else 0.dp),
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

                    MainTab.AGENT -> AgentScreen(
                        onToast = onToast,
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

