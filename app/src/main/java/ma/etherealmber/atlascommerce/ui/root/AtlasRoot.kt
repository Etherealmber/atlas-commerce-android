package ma.etherealmber.atlascommerce.ui.root

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.core.update.UpdateState
import ma.etherealmber.atlascommerce.ui.components.AppUpdateBanner
import ma.etherealmber.atlascommerce.ui.components.AtlasMark
import ma.etherealmber.atlascommerce.ui.components.MandatoryUpdateScreen
import ma.etherealmber.atlascommerce.ui.screens.devices.QrDeviceScreen
import ma.etherealmber.atlascommerce.ui.screens.legal.AboutScreen
import ma.etherealmber.atlascommerce.ui.screens.legal.LicenseScreen
import ma.etherealmber.atlascommerce.ui.screens.legal.SecurityScreen
import ma.etherealmber.atlascommerce.ui.screens.login.LoginScreen
import ma.etherealmber.atlascommerce.ui.screens.notifications.NotificationsScreen
import ma.etherealmber.atlascommerce.ui.screens.server.ServerSetupScreen
import ma.etherealmber.atlascommerce.ui.shell.MainShell
import ma.etherealmber.atlascommerce.ui.shell.MainTab
import ma.etherealmber.atlascommerce.ui.theme.AtlasTheme
import ma.etherealmber.atlascommerce.ui.theme.LocalAtlasDarkTheme
import ma.etherealmber.atlascommerce.ui.util.findActivity

private object Overlay {
    const val SERVER = "server"
    const val QR_DEVICE = "qr_device"
    const val NOTIFICATIONS = "notifications"
    const val LICENSE = "license"
    const val SECURITY = "security"
    const val ABOUT = "about"
}

/**
 * Composition root of the app.
 */
@Composable
fun AtlasRoot(
    deepLinkQrToken: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    viewModel: AppViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val densityMode by viewModel.densityMode.collectAsStateWithLifecycle()
    val appearanceLoaded by viewModel.appearanceLoaded.collectAsStateWithLifecycle()

    AtlasTheme(themeMode = themeMode, densityMode = densityMode) {
        val dark = LocalAtlasDarkTheme.current
        val activity = LocalContext.current.findActivity()
        val view = LocalView.current
        SideEffect {
            val window = activity?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
        if (!appearanceLoaded) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
        } else {
            AtlasGate(
                deepLinkQrToken = deepLinkQrToken,
                onDeepLinkConsumed = onDeepLinkConsumed,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun AtlasGate(
    deepLinkQrToken: String?,
    onDeepLinkConsumed: () -> Unit,
    viewModel: AppViewModel,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val session by viewModel.session.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val serverLoaded by viewModel.serverLoaded.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingQrToken by rememberSaveable { mutableStateOf<String?>(null) }
    var requestedTab by rememberSaveable { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    val startDownloadWithNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            runCatching {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        viewModel.startUpdateDownload()
    }

    val triggerInstall: () -> Unit = {
        viewModel.installDownloadedUpdate(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onAppResumed(context)
                Lifecycle.Event.ON_STOP -> viewModel.onAppStopped()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(deepLinkQrToken) {
        if (!deepLinkQrToken.isNullOrBlank()) {
            pendingQrToken = deepLinkQrToken
            overlay = Overlay.QR_DEVICE
            onDeepLinkConsumed()
        }
    }

    val showToast: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(viewModel) {
        viewModel.notices.collect { message -> showToast(message) }
    }
    LaunchedEffect(session) {
        if (session is SessionState.SignedOut && overlay == Overlay.NOTIFICATIONS) {
            overlay = null
        }
    }

    val mandatoryBlocking = when (val u = updateState) {
        is UpdateState.Available -> u.info.mandatory
        is UpdateState.Downloading -> u.info.mandatory
        is UpdateState.ReadyToInstall -> u.info.mandatory
        is UpdateState.Error -> u.info?.mandatory == true
        else -> false
    }

    BackHandler(enabled = overlay != null) {
        overlay = null
        pendingQrToken = null
    }

    val activeUser = (session as? SessionState.Active)?.user

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                !serverLoaded || session is SessionState.Loading -> SplashPane()

                overlay == Overlay.SERVER ->
                    ServerSetupScreen(onClose = { overlay = null })

                overlay == Overlay.QR_DEVICE ->
                    QrDeviceScreen(
                        currentUser = activeUser,
                        initialToken = pendingQrToken,
                        onClose = {
                            overlay = null
                            pendingQrToken = null
                        },
                        onToast = showToast,
                    )

                overlay == Overlay.NOTIFICATIONS ->
                    NotificationsScreen(
                        onBack = { overlay = null },
                        onNavigateHref = { href ->
                            overlay = null
                            val h = href.orEmpty().lowercase()
                            requestedTab = when {
                                h.contains("product") || h.contains("stock") -> MainTab.PRODUCTS.name
                                h.contains("invoice") || h.contains("sale") -> MainTab.SALE.name
                                h.contains("scan") -> MainTab.SCAN.name
                                h.contains("assist") || h.contains("agent") -> MainTab.AGENT.name
                                else -> MainTab.HOME.name
                            }
                        },
                        onToast = showToast,
                    )

                overlay == Overlay.LICENSE -> LicenseScreen(onBack = { overlay = null })

                overlay == Overlay.SECURITY -> SecurityScreen(onBack = { overlay = null })

                overlay == Overlay.ABOUT -> AboutScreen(onBack = { overlay = null })

                server == null -> ServerSetupScreen()

                mandatoryBlocking -> MandatoryUpdateScreen(
                    state = updateState,
                    onDownload = startDownloadWithNotificationPermission,
                    onInstall = triggerInstall,
                    onChangeServer = { overlay = Overlay.SERVER },
                )

                session is SessionState.SignedOut -> LoginScreen(
                    onChangeServer = { overlay = Overlay.SERVER },
                    onOpenQrDevice = {
                        pendingQrToken = null
                        overlay = Overlay.QR_DEVICE
                    },
                    onOpenSecurity = { overlay = Overlay.SECURITY },
                    onOpenLicense = { overlay = Overlay.LICENSE },
                    onCheckUpdates = {
                        viewModel.checkForUpdate(manual = true)
                        showToast("Vérification des mises à jour…")
                    },
                )

                activeUser != null -> {
                    MainShell(
                        user = activeUser,
                        snackbarHostState = snackbarHostState,
                        externalTabRequest = requestedTab,
                        onExternalTabHandled = { requestedTab = null },
                        onChangeServer = { overlay = Overlay.SERVER },
                        onOpenQrDevice = { token ->
                            pendingQrToken = token
                            overlay = Overlay.QR_DEVICE
                        },
                        onOpenNotifications = { overlay = Overlay.NOTIFICATIONS },
                        onOpenLicense = { overlay = Overlay.LICENSE },
                        onOpenSecurity = { overlay = Overlay.SECURITY },
                        onOpenAbout = { overlay = Overlay.ABOUT },
                        onSignOut = viewModel::signOut,
                        onStartUpdateDownload = startDownloadWithNotificationPermission,
                    )
                }

                else -> SplashPane()
            }

            if (serverLoaded && !mandatoryBlocking) {
                AppUpdateBanner(
                    state = updateState,
                    onDownload = startDownloadWithNotificationPermission,
                    onInstall = triggerInstall,
                    onDismiss = viewModel::dismissUpdateBanner,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp),
            )
        }
    }
}

@Composable
private fun SplashPane() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        AtlasMark(size = 72.dp)
        Spacer(Modifier.height(18.dp))
        Text(
            text = AppDetails.NAME,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = AppDetails.TAGLINE,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(22.dp))
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
    }
}
