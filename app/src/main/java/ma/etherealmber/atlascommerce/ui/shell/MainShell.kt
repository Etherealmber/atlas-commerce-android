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
import androidx.compose.material.icons.outlined.Person
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
import ma.etherealmber.atlascommerce.ui.screens.settings.ProfileScreen
import ma.etherealmber.atlascommerce.ui.screens.settings.SettingsScreen

enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Accueil", Icons.Outlined.Home),
    PRODUCTS("Produits", Icons.Outlined.Inventory2),
    SCAN("Scan", Icons.Outlined.QrCodeScanner),
    SALE("Vente", Icons.Outlined.ShoppingCart),
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
        val BottomBarTabs = listOf(HOME, PRODUCTS, SCAN, SALE, AGENT)

        /** Filters bottom navigation items strictly according to the signed-in user's role. */
        fun bottomTabsForRole(role: String?): List<MainTab> =
            if (Roles.canCreateDocuments(role)) {
                BottomBarTabs
            } else {
                listOf(HOME, PRODUCTS, SCAN, AGENT)
            }
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

    val roleTabs = MainTab.bottomTabsForRole(user.role)
    val resolvedInitial = roleTabs
        .firstOrNull { it.name == defaultStartTab }
        ?.name ?: MainTab.HOME.name

    var tabName by rememberSaveable(user.scopeIdentity) { mutableStateOf(resolvedInitial) }
    var lastBottomTabName by rememberSaveable(user.scopeIdentity) { mutableStateOf(resolvedInitial) }
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
            tabName = resolved
            if (resolved != MainTab.PROFILE.name && resolved != MainTab.SETTINGS.name) {
                lastBottomTabName = resolved
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
        if (target == MainTab.SALE && !Roles.canCreateDocuments(user.role)) {
            onToast("Accès Vente réservé aux rôles Administrateur et Secrétaire.")
        } else {
            if (target != MainTab.PROFILE && target != MainTab.SETTINGS) {
                lastBottomTabName = target.name
            }
            tabName = target.name
        }
    }

    BackHandler(enabled = tab != MainTab.HOME) {
        when (tab) {
            MainTab.SETTINGS -> tabName = MainTab.PROFILE.name
            MainTab.PROFILE -> tabName = lastBottomTabName
            else -> switchTab(MainTab.HOME)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AtlasTopBar(
                title = tab.label,
                subtitle = "${user.displayName} · ${Roles.label(user.role)}",
                onBack = when (tab) {
                    MainTab.SETTINGS -> ({ tabName = MainTab.PROFILE.name })
                    MainTab.PROFILE -> ({ tabName = lastBottomTabName })
                    else -> null
                },
                actions = {
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
                            Spacer(Modifier.width(4.dp))
                        }

                        else -> {
                            if (Roles.isAdmin(user.role) || Roles.canCreateDocuments(user.role)) {
                                IconButton(onClick = { onOpenQrDevice(null) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Devices,
                                        contentDescription = "Scanner un poste PC (QR)",
                                    )
                                }
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
                                            contentDescription = "Info Box & Notifications",
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = "Info Box & Notifications",
                                    )
                                }
                            }
                            Spacer(Modifier.width(4.dp))
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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                roleTabs.forEach { item ->
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

                    MainTab.PROFILE -> ProfileScreen(
                        user = user,
                        onOpenSettings = { switchTab(MainTab.SETTINGS) },
                        onSignOut = onSignOut,
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
