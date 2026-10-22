package ma.etherealmber.atlascommerce.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasPageTransition
import ma.etherealmber.atlascommerce.ui.components.AtlasTopBar
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentScreen
import ma.etherealmber.atlascommerce.ui.screens.home.HomeScreen
import ma.etherealmber.atlascommerce.ui.screens.products.ProductsScreen
import ma.etherealmber.atlascommerce.ui.screens.sale.SaleScreen
import ma.etherealmber.atlascommerce.ui.screens.scan.ScanScreen
import ma.etherealmber.atlascommerce.ui.screens.settings.SettingsScreen

enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Accueil", Icons.Outlined.Home),
    PRODUCTS("Produits", Icons.Outlined.Inventory2),
    SCAN("Scan", Icons.Outlined.QrCodeScanner),
    SALE("Vente", Icons.Outlined.ShoppingCart),
    AGENT("Agent", Icons.Outlined.AutoAwesome),
    SETTINGS("Réglages", Icons.Outlined.Settings),
    ;

    companion object {
        /**
         * Settings (`SETTINGS`) is intentionally EXCLUDED from the Bottom Navigation Bar:
         * access Settings cleanly by tapping the user's Profile Avatar in the Top Bar.
         */
        val BottomBarTabs = listOf(HOME, PRODUCTS, SCAN, SALE, AGENT)
    }
}

@Composable
fun MainShell(
    user: SessionUser,
    snackbarHostState: SnackbarHostState,
    defaultStartTab: String = MainTab.HOME.name,
    externalTabRequest: String? = null,
    onExternalTabHandled: () -> Unit = {},
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

    val resolvedInitial = MainTab.BottomBarTabs
        .firstOrNull { it.name == defaultStartTab }
        ?.name ?: MainTab.HOME.name

    var tabName by rememberSaveable { mutableStateOf(resolvedInitial) }
    var lastBottomTabName by rememberSaveable { mutableStateOf(resolvedInitial) }
    val tab = MainTab.entries.firstOrNull { it.name == tabName } ?: MainTab.HOME
    val scope = rememberCoroutineScope()

    LaunchedEffect(externalTabRequest) {
        if (!externalTabRequest.isNullOrBlank()) {
            tabName = externalTabRequest
            if (externalTabRequest != MainTab.SETTINGS.name) {
                lastBottomTabName = externalTabRequest
            }
            onExternalTabHandled()
        }
    }

    LaunchedEffect(tab) {
        shellViewModel.refreshNotificationsBadge()
    }

    val onToast: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val switchTab: (MainTab) -> Unit = { target ->
        if (target != MainTab.SETTINGS) {
            lastBottomTabName = target.name
        }
        tabName = target.name
    }

    BackHandler(enabled = tab != MainTab.HOME) {
        if (tab == MainTab.SETTINGS) {
            tabName = lastBottomTabName
        } else {
            switchTab(MainTab.HOME)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AtlasTopBar(
                title = tab.label,
                subtitle = "${user.displayName} · ${Roles.label(user.role)}",
                onBack = if (tab == MainTab.SETTINGS) {
                    { tabName = lastBottomTabName }
                } else {
                    null
                },
                actions = {
                    IconButton(onClick = { onOpenQrDevice(null) }) {
                        Icon(
                            imageVector = Icons.Outlined.Devices,
                            contentDescription = "Scanner un poste PC (QR)",
                        )
                    }
                    IconButton(onClick = onOpenNotifications) {
                        if (notificationCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(if (notificationCount > 99) "99+" else notificationCount.toString())
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    AtlasAvatar(
                        avatarUrl = user.avatarUrl,
                        label = user.displayName,
                        size = 34.dp,
                        serverBaseUrl = serverBaseUrl,
                        borderColor = if (tab == MainTab.SETTINGS) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            null
                        },
                        onClick = {
                            if (tab == MainTab.SETTINGS) {
                                tabName = lastBottomTabName
                            } else {
                                switchTab(MainTab.SETTINGS)
                            }
                        },
                    )
                    Spacer(Modifier.width(10.dp))
                },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                MainTab.BottomBarTabs.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { switchTab(item) },
                        icon = {
                            if (item == MainTab.SALE && cartCount > 0) {
                                BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                                    Icon(item.icon, contentDescription = item.label)
                                }
                            } else {
                                Icon(item.icon, contentDescription = item.label)
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
        AtlasPageTransition(
            targetState = tab,
            indexSelector = { it.ordinal },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) { currentTab ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentTab) {
                    MainTab.HOME -> HomeScreen(
                        user = user,
                        onOpenProducts = { switchTab(MainTab.PRODUCTS) },
                        onOpenSale = { switchTab(MainTab.SALE) },
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
                        isActive = tab == MainTab.SCAN && currentTab == MainTab.SCAN,
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
