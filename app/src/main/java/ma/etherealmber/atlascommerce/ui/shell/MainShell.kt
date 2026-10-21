package ma.etherealmber.atlascommerce.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
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
import ma.etherealmber.atlascommerce.ui.components.AtlasTopBar
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
    SETTINGS("Réglages", Icons.Outlined.Settings),
}

@Composable
fun MainShell(
    user: SessionUser,
    snackbarHostState: SnackbarHostState,
    onChangeServer: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAbout: () -> Unit,
    onSignOut: () -> Unit,
    shellViewModel: ShellViewModel = hiltViewModel(),
) {
    val cartCount by shellViewModel.cartCount.collectAsStateWithLifecycle()
    var tabName by rememberSaveable { mutableStateOf(MainTab.HOME.name) }
    val tab = MainTab.entries.firstOrNull { it.name == tabName } ?: MainTab.HOME
    val scope = rememberCoroutineScope()

    val onToast: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val switchTab: (MainTab) -> Unit = { target -> tabName = target.name }

    // Back from a secondary tab returns to Accueil instead of closing the app.
    BackHandler(enabled = tab != MainTab.HOME) { switchTab(MainTab.HOME) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AtlasTopBar(
                title = tab.label,
                subtitle = "${user.displayName} · ${Roles.label(user.role)}",
                actions = {
                    IconButton(onClick = onChangeServer) {
                        Icon(Icons.Outlined.Dns, contentDescription = "Changer de serveur")
                    }
                    IconButton(onClick = { switchTab(MainTab.SETTINGS) }) {
                        Icon(Icons.Outlined.Person, contentDescription = "Mon compte")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                MainTab.entries.forEach { item ->
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
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (tab) {
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
                    onOpenSale = { switchTab(MainTab.SALE) },
                    onOpenProducts = { switchTab(MainTab.PRODUCTS) },
                    onToast = onToast,
                )

                MainTab.SALE -> SaleScreen(
                    user = user,
                    onOpenProducts = { switchTab(MainTab.PRODUCTS) },
                    onToast = onToast,
                )

                MainTab.SETTINGS -> SettingsScreen(
                    user = user,
                    onChangeServer = onChangeServer,
                    onOpenLicense = onOpenLicense,
                    onOpenSecurity = onOpenSecurity,
                    onOpenAbout = onOpenAbout,
                    onSignOut = onSignOut,
                    onToast = onToast,
                )
            }
        }
    }
}
