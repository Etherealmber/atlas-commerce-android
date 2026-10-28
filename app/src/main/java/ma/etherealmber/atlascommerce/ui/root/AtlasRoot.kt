package ma.etherealmber.atlascommerce.ui.root

import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import android.Manifest
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.util.Locale
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.net.VerifyLink
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.core.update.UpdateState
import ma.etherealmber.atlascommerce.ui.components.AppUpdateBanner
import ma.etherealmber.atlascommerce.ui.components.AtlasBootSplashScreen
import ma.etherealmber.atlascommerce.ui.components.AtlasPageTransition
import ma.etherealmber.atlascommerce.ui.components.AtlasWelcomeOnboardingScreen
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.components.MandatoryUpdateScreen
import ma.etherealmber.atlascommerce.ui.components.UpdateMiniProgressBall
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.LocalSetAtlasLanguage
import ma.etherealmber.atlascommerce.ui.screens.devices.QrDeviceScreen
import ma.etherealmber.atlascommerce.ui.screens.settings.DeveloperDiagnosticsScreen
import ma.etherealmber.atlascommerce.ui.screens.legal.AboutScreen
import ma.etherealmber.atlascommerce.ui.screens.legal.LicenseScreen
import ma.etherealmber.atlascommerce.ui.screens.legal.SecurityScreen
import ma.etherealmber.atlascommerce.ui.screens.login.LoginScreen
import ma.etherealmber.atlascommerce.ui.screens.snake.SnakeScreen
import ma.etherealmber.atlascommerce.ui.screens.snake.rememberInternetAvailable
import ma.etherealmber.atlascommerce.ui.screens.notifications.NotificationsScreen
import ma.etherealmber.atlascommerce.ui.screens.server.ServerSetupScreen
import ma.etherealmber.atlascommerce.ui.shell.MainShell
import ma.etherealmber.atlascommerce.ui.shell.MainTab
import ma.etherealmber.atlascommerce.ui.theme.AtlasTheme
import ma.etherealmber.atlascommerce.ui.theme.LocalAtlasDarkTheme
import ma.etherealmber.atlascommerce.ui.util.findActivity

private object Overlay {
    const val SNAKE = "snake"
    const val SERVER = "server"
    const val QR_DEVICE = "qr_device"
    const val NOTIFICATIONS = "notifications"
    const val LICENSE = "license"
    const val SECURITY = "security"
    const val ABOUT = "about"
    const val DEVELOPER = "developer"
}

private enum class RootStage(val rank: Int) {
    SPLASH(0),
    ONBOARDING(1),
    SERVER_SETUP(2),
    MANDATORY_UPDATE(3),
    LOGIN(4),
    SHELL(5),
}

/**
 * Composition root of the app.
 */
@Composable
fun AtlasRoot(
    deepLinkQrToken: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    deepLinkVerify: VerifyLink? = null,
    onVerifyLinkConsumed: () -> Unit = {},
    deepLinkServerOrigin: String? = null,
    openNotificationsRequested: Boolean = false,
    onOpenNotificationsConsumed: () -> Unit = {},
    openSettingsRequested: Boolean = false,
    onOpenSettingsConsumed: () -> Unit = {},
    viewModel: AppViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val densityMode by viewModel.densityMode.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val languageRestartNotice by viewModel.languageRestartNotice.collectAsStateWithLifecycle()
    val languageSaveFailed by viewModel.languageSaveFailed.collectAsStateWithLifecycle()
    val updateServer by viewModel.server.collectAsStateWithLifecycle()
    val appearanceLoaded by viewModel.appearanceLoaded.collectAsStateWithLifecycle()
    val animationsEnabled by viewModel.animationsEnabled.collectAsStateWithLifecycle()
    var appearanceTimedOut by remember { mutableStateOf(false) }

    LaunchedEffect(appearanceLoaded) {
        if (!appearanceLoaded) {
            kotlinx.coroutines.delay(2_000L)
            appearanceTimedOut = true
        }
    }

    val baseConfiguration = LocalConfiguration.current
    val localizedConfiguration = remember(baseConfiguration, appLanguage) {
        Configuration(baseConfiguration).apply {
            setLocale(Locale.forLanguageTag(appLanguage.tag))
        }
    }

    CompositionLocalProvider(
        LocalAtlasAnimationsEnabled provides animationsEnabled,
        LocalAtlasLanguage provides appLanguage,
        LocalSetAtlasLanguage provides viewModel::setAppLanguage,
    ) {
        CompositionLocalProvider(
            LocalConfiguration provides localizedConfiguration,
            LocalLayoutDirection provides if (appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            AtlasTheme(themeMode = themeMode, densityMode = densityMode) {
                if (languageSaveFailed) {
                    AlertDialog(
                        onDismissRequest = viewModel::dismissLanguageSaveFailed,
                        title = { Text(atlasText("Enregistrement impossible", "Could not save", "تعذّر الحفظ")) },
                        text = { Text(atlasText("La langue n’a pas été enregistrée. Réessayez.", "The language was not saved. Try again.", "لم تُحفظ اللغة. حاول مجدداً.")) },
                        confirmButton = { TextButton(onClick = viewModel::dismissLanguageSaveFailed) { Text(atlasText("Fermer", "Close", "إغلاق")) } },
                    )
                }
                languageRestartNotice?.let { pending ->
                    AlertDialog(
                        onDismissRequest = viewModel::dismissLanguageRestartNotice,
                        title = { Text(atlasText("Redémarrage nécessaire", "Restart required", "يلزم إعادة التشغيل")) },
                        text = { Text(atlasText("Langue enregistrée : ${pending.nativeLabel}. Fermez complètement l’application, puis rouvrez-la pour appliquer le changement.", "Language saved: ${pending.nativeLabel}. Close the app completely, then reopen it to apply the change.", "تم حفظ اللغة: ${pending.nativeLabel}. أغلق التطبيق تماماً ثم أعد فتحه لتطبيق التغيير.")) },
                        confirmButton = { TextButton(onClick = viewModel::dismissLanguageRestartNotice) { Text(atlasText("Compris", "OK", "حسناً")) } },
                    )
                }
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
                if (!appearanceLoaded && !appearanceTimedOut) {
                    AtlasBootSplashScreen(holdUntilReady = true)
                } else {
                    AtlasGate(
                        deepLinkQrToken = deepLinkQrToken,
                        onDeepLinkConsumed = onDeepLinkConsumed,
                        deepLinkVerify = deepLinkVerify,
                        onVerifyLinkConsumed = onVerifyLinkConsumed,
                        deepLinkServerOrigin = deepLinkServerOrigin,
                        openNotificationsRequested = openNotificationsRequested,
                        onOpenNotificationsConsumed = onOpenNotificationsConsumed,
                        openSettingsRequested = openSettingsRequested,
                        onOpenSettingsConsumed = onOpenSettingsConsumed,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun AtlasGate(
    deepLinkQrToken: String?,
    onDeepLinkConsumed: () -> Unit,
    deepLinkVerify: VerifyLink?,
    onVerifyLinkConsumed: () -> Unit,
    deepLinkServerOrigin: String?,
    openNotificationsRequested: Boolean,
    onOpenNotificationsConsumed: () -> Unit,
    openSettingsRequested: Boolean,
    onOpenSettingsConsumed: () -> Unit,
    viewModel: AppViewModel,
) {
    val context = LocalContext.current
    val atlasLanguage = LocalAtlasLanguage.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val session by viewModel.session.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val serverLoaded by viewModel.serverLoaded.collectAsStateWithLifecycle()
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val defaultStartTab by viewModel.defaultStartTab.collectAsStateWithLifecycle()
    val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsStateWithLifecycle()
    val passkeyRegistrationMessage by viewModel.passkeyRegistrationMessage.collectAsStateWithLifecycle()
    val waitingForPasskeyProvider by viewModel.waitingForPasskeyProvider.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var bootSplashFinished by rememberSaveable { mutableStateOf(false) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }
    val internetAvailable = rememberInternetAvailable()
    var offlineOfferShown by remember { mutableStateOf(false) }
    var showOfflineOffer by remember { mutableStateOf(false) }
    var showReconnectPrompt by remember { mutableStateOf(false) }
    var wasOffline by remember { mutableStateOf(false) }
    LaunchedEffect(internetAvailable) {
        when (internetAvailable) {
            false -> wasOffline = true
            true -> {
                if (wasOffline && overlay == Overlay.SNAKE) showReconnectPrompt = true
                wasOffline = false
                offlineOfferShown = false
                showOfflineOffer = false
            }
            null -> Unit
        }
    }
    var pendingQrToken by rememberSaveable { mutableStateOf<String?>(null) }
    var requestedTab by rememberSaveable { mutableStateOf<String?>(null) }
    var notificationPermissionPrompted by rememberSaveable { mutableStateOf(false) }
    var passkeySetupOfferVisible by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.repostUpdateNotification()
        }
    }

    LaunchedEffect(session) {
        if (session is SessionState.Active &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationPermissionPrompted &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionPrompted = true
            kotlinx.coroutines.delay(700)
            runCatching { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }

    val requestNotificationPermissionIfNeeded: () -> Unit = {
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
    }

    val startDownloadWithNotificationPermission: () -> Unit = {
        requestNotificationPermissionIfNeeded()
        viewModel.startUpdateDownload()
    }
    val startSettingsDownloadWithNotificationPermission: () -> Unit = {
        requestNotificationPermissionIfNeeded()
        viewModel.startUpdateDownload(fromSettings = true)
    }

    LaunchedEffect(autoCheckUpdates, server?.baseUrl) {
        ma.etherealmber.atlascommerce.core.update.UpdateCheckWorkScheduler.setEnabled(
            context,
            autoCheckUpdates,
            server?.baseUrl,
        )
    }

    val triggerInstall: () -> Unit = {
        viewModel.installDownloadedUpdate(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onAppResumed(context)
                Lifecycle.Event.ON_STOP -> viewModel.onAppStopped()
                Lifecycle.Event.ON_DESTROY -> viewModel.cancelPasskeyRegistration()
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

    LaunchedEffect(openNotificationsRequested, session) {
        if (openNotificationsRequested && session is SessionState.Active) {
            overlay = Overlay.NOTIFICATIONS
            onOpenNotificationsConsumed()
        }
    }

    LaunchedEffect(openSettingsRequested, session) {
        if (openSettingsRequested && session is SessionState.Active) {
            overlay = null
            requestedTab = MainTab.SETTINGS.name
            onOpenSettingsConsumed()
        }
    }

    val showToast: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(atlasNotice(message, atlasLanguage)) }
    }

    LaunchedEffect(deepLinkVerify) {
        deepLinkVerify?.let { link ->
            val originHint = deepLinkServerOrigin
            onVerifyLinkConsumed()
            viewModel.verifyEmailLink(link, originHint) { msg -> showToast(msg) }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.notices.collect { message -> showToast(message) }
    }
    LaunchedEffect(viewModel) {
        viewModel.passkeySetupOffers.collect { passkeySetupOfferVisible = true }
    }
    LaunchedEffect(session) {
        if (session is SessionState.Active) {
            requestNotificationPermissionIfNeeded()
        }
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

    val loadingBackend = !serverLoaded || session is SessionState.Loading
    val activeUser = (session as? SessionState.Active)?.user
    var savedShellTab by rememberSaveable(activeUser?.scopeIdentity) { mutableStateOf<String?>(null) }
    var savedLastBottomTab by rememberSaveable(activeUser?.scopeIdentity) { mutableStateOf<String?>(null) }

    val baseStage = when {
        !bootSplashFinished || loadingBackend -> RootStage.SPLASH
        !onboardingDone -> RootStage.ONBOARDING
        mandatoryBlocking -> RootStage.MANDATORY_UPDATE
        // Server setup is a pre-login step. Never bounce an active session back
        // to server selection after authentication.
        server == null && session !is SessionState.Active -> RootStage.SERVER_SETUP
        session is SessionState.SignedOut -> RootStage.LOGIN
        activeUser != null -> RootStage.SHELL
        else -> RootStage.SPLASH
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            AtlasPageTransition(
                targetState = baseStage,
                modifier = Modifier.fillMaxSize(),
                indexSelector = { it.rank },
            ) { currentStage ->
                when (currentStage) {
                    RootStage.SPLASH -> AtlasBootSplashScreen(
                        onFinished = { bootSplashFinished = true },
                        holdUntilReady = loadingBackend,
                    )

                    RootStage.ONBOARDING -> AtlasWelcomeOnboardingScreen(
                        onComplete = {
                            requestNotificationPermissionIfNeeded()
                            viewModel.completeOnboarding()
                        },
                    )

                    RootStage.SERVER_SETUP -> ServerSetupScreen()

                    RootStage.MANDATORY_UPDATE -> MandatoryUpdateScreen(
                        state = updateState,
                        onDownload = startDownloadWithNotificationPermission,
                        onInstall = triggerInstall,
                        onChangeServer = { overlay = Overlay.SERVER },
                    )

                    RootStage.LOGIN -> LoginScreen(
                        onChangeServer = { overlay = Overlay.SERVER },
                        onOpenQrDevice = {
                            pendingQrToken = null
                            overlay = Overlay.QR_DEVICE
                        },
                    )

                    RootStage.SHELL -> {
                        if (activeUser != null) {
                            MainShell(
                                user = activeUser,
                                snackbarHostState = snackbarHostState,
                                defaultStartTab = defaultStartTab,
                                activeTabOverride = savedShellTab,
                                onActiveTabChange = { savedShellTab = it },
                                lastBottomTabOverride = savedLastBottomTab,
                                onLastBottomTabChange = { savedLastBottomTab = it },
                                isForeground = overlay == null,
                                externalTabRequest = requestedTab,
                                onExternalTabHandled = { requestedTab = null },
                                updateState = updateState,
                                onShowUpdateProgress = viewModel::showUpdateDownloadBanner,
                                onChangeServer = { overlay = Overlay.SERVER },
                                onOpenQrDevice = { token ->
                                    pendingQrToken = token
                                    overlay = Overlay.QR_DEVICE
                                },
                                onOpenNotifications = {
                                    requestNotificationPermissionIfNeeded()
                                    overlay = Overlay.NOTIFICATIONS
                                },
                                onOpenLicense = { overlay = Overlay.LICENSE },
                                onOpenSecurity = { overlay = Overlay.SECURITY },
                                onOpenAbout = { overlay = Overlay.ABOUT },
                                onOpenSnake = { overlay = Overlay.SNAKE },
                                onOpenDeveloperTools = { overlay = Overlay.DEVELOPER },
                                onSignOut = viewModel::signOut,
                                onStartUpdateDownload = startSettingsDownloadWithNotificationPermission,
                            )
                        } else {
                            AtlasBootSplashScreen(holdUntilReady = true)
                        }
                    }
                }
            }

            // Full-screen overlay host layered above MainShell so opening License, Security,
            // About, Devices (QR), Server Setup, or Notifications NEVER destroys MainShell's
            // active tab (e.g. Paramètres) or scroll position when tapping Retour / Back!
            var lastOverlay by remember { mutableStateOf<String?>(null) }
            if (overlay != null) {
                lastOverlay = overlay
            }
            AnimatedVisibility(
                visible = bootSplashFinished && onboardingDone && !loadingBackend && overlay != null,
                enter = slideInHorizontally(initialOffsetX = { it / 6 }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it / 6 }) + fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (overlay ?: lastOverlay) {
                        Overlay.SNAKE -> SnakeScreen(onClose = { showReconnectPrompt = false; overlay = null }, pausedByPrompt = showReconnectPrompt, isActive = overlay == Overlay.SNAKE)
                        Overlay.SERVER -> ServerSetupScreen(onClose = { overlay = null })

                        Overlay.QR_DEVICE -> QrDeviceScreen(
                            currentUser = activeUser,
                            initialToken = pendingQrToken,
                            onClose = {
                                overlay = null
                                pendingQrToken = null
                            },
                            onToast = showToast,
                        )

                        Overlay.NOTIFICATIONS -> NotificationsScreen(
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

                        Overlay.LICENSE -> LicenseScreen(onBack = { overlay = null })

                        Overlay.SECURITY -> SecurityScreen(onBack = { overlay = null })

                        Overlay.ABOUT -> AboutScreen(onBack = { overlay = null })

                        Overlay.DEVELOPER -> DeveloperDiagnosticsScreen(
                            onBack = { overlay = null },
                            onToast = showToast,
                            appViewModel = viewModel,
                        )
                    }
                }
            }

            // Registered AFTER MainShell so system Back always dismisses any open overlay first
            // and returns directly to the exact underlying tab (such as Paramètres).
            BackHandler(enabled = overlay != null) {
                showReconnectPrompt = false
                overlay = null
                pendingQrToken = null
            }

            if (bootSplashFinished && onboardingDone && serverLoaded && !mandatoryBlocking) {
                if ((baseStage != RootStage.SHELL || overlay != null) &&
                    !(baseStage == RootStage.SHELL && savedShellTab == MainTab.SETTINGS.name)
                ) {
                    UpdateMiniProgressBall(
                        state = updateState,
                        onClick = viewModel::showUpdateDownloadBanner,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 12.dp, end = 14.dp),
                    )
                }
                if (!(baseStage == RootStage.SHELL && savedShellTab == MainTab.SETTINGS.name)) AppUpdateBanner(
                    state = updateState,
                    onDownload = startDownloadWithNotificationPermission,
                    onInstall = triggerInstall,
                    onDismiss = viewModel::dismissUpdateBanner,
                    onHideDownload = {
                        requestNotificationPermissionIfNeeded()
                        viewModel.hideUpdateDownloadBanner()
                        showToast(
                            "Barre réduite à côté du profil — progression aussi visible dans Paramètres et Notifications.",
                        )
                    },
                    onOpenSettings = if (activeUser != null) {
                        {
                            overlay = null
                            requestedTab = MainTab.SETTINGS.name
                        }
                    } else {
                        null
                    },
                    belowTopBar = baseStage == RootStage.SHELL || overlay != null,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp),
            )

            LaunchedEffect(internetAvailable, baseStage, overlay, waitingForPasskeyProvider, passkeyRegistrationMessage) {
                if (internetAvailable == false && !offlineOfferShown && overlay == null && !waitingForPasskeyProvider && passkeyRegistrationMessage == null &&
                    (baseStage == RootStage.LOGIN || baseStage == RootStage.SHELL)) {
                    offlineOfferShown = true
                    showOfflineOffer = true
                }
            }
            if (showOfflineOffer) {
                AlertDialog(
                    onDismissRequest = { showOfflineOffer = false },
                    title = { Text(atlasText("Pas de connexion Internet", "No internet connection", "لا يوجد اتصال بالإنترنت")) },
                    text = { Text(atlasText("Envie de jouer à Snake Atlas hors ligne ?", "Want to play Atlas Snake offline?", "هل تريد لعب ثعبان Atlas دون اتصال؟")) },
                    confirmButton = { TextButton(onClick = { showOfflineOffer = false; overlay = Overlay.SNAKE }) { Text(atlasText("Jouer", "Play", "العب")) } },
                    dismissButton = { TextButton(onClick = { showOfflineOffer = false }) { Text(atlasText("Pas maintenant", "Not now", "ليس الآن")) } },
                )
            }
            if (showReconnectPrompt && overlay == Overlay.SNAKE) {
                AlertDialog(
                    onDismissRequest = { showReconnectPrompt = false },
                    title = { Text(atlasText("Connexion rétablie", "Connection restored", "تم استعادة الاتصال")) },
                    text = { Text(atlasText("Voulez-vous continuer à jouer ?", "Would you like to keep playing?", "هل تريد متابعة اللعب؟")) },
                    confirmButton = { TextButton(onClick = { showReconnectPrompt = false }) { Text(atlasText("Continuer", "Keep playing", "متابعة اللعب")) } },
                    dismissButton = { TextButton(onClick = { showReconnectPrompt = false; overlay = null }) { Text(atlasText("Retour à Atlas", "Return to Atlas", "العودة إلى Atlas")) } },
                )
            }

            if (passkeySetupOfferVisible && session is SessionState.Active) {
                AlertDialog(
                    onDismissRequest = { passkeySetupOfferVisible = false },
                    title = { Text(atlasText("Créer une passkey ?")) },
                    text = {
                        Text(atlasText("Ajoutez une connexion rapide et protégée par le gestionnaire de mots de passe de cet appareil. Vous pourrez aussi la créer plus tard dans Paramètres > Sécurité. Cette étape est facultative."))
                    },
                    confirmButton = {
                        Button(onClick = {
                            passkeySetupOfferVisible = false
                            viewModel.registerAndroidPasskey(context) { }
                        }) { Text(atlasText("Créer une passkey")) }
                    },
                    dismissButton = {
                        TextButton(onClick = { passkeySetupOfferVisible = false }) { Text(atlasText("Plus tard")) }
                    },
                )
            }

            if (waitingForPasskeyProvider) {
                AlertDialog(
                    onDismissRequest = viewModel::cancelPasskeyRegistration,
                    title = { Text(atlasText("Passkey Android", "Android passkey", "مفتاح مرور Android")) },
                    text = { Text(atlasText("Terminez la création dans le gestionnaire Android. Si aucune fenêtre ne s'affiche, vous pouvez annuler et réessayer.", "Complete setup in Android's credential manager. If no prompt appears, you can cancel and try again.", "أكمل الإعداد في مدير بيانات الاعتماد في Android. إذا لم تظهر نافذة، يمكنك الإلغاء والمحاولة مجدداً.")) },
                    confirmButton = {
                        TextButton(onClick = viewModel::cancelPasskeyRegistration) { Text(atlasText("Annuler", "Cancel", "إلغاء")) }
                    },
                )
            }

            passkeyRegistrationMessage?.let { message ->
                val providerSetupSuggested = message.contains("aucun fournisseur", ignoreCase = true) ||
                    message.contains("verrouillage d'écran", ignoreCase = true) ||
                    message.contains("activées", ignoreCase = true) ||
                    message.contains("fournisseur Android", ignoreCase = true)
                AlertDialog(
                    onDismissRequest = viewModel::dismissPasskeyRegistrationMessage,
                    title = { Text(atlasText("Passkey Android")) },
                    text = { Text(atlasNotice(message, atlasLanguage)) },
                    confirmButton = {
                        TextButton(onClick = viewModel::dismissPasskeyRegistrationMessage) { Text(atlasText("OK")) }
                    },
                    dismissButton = {
                        if (providerSetupSuggested) {
                            TextButton(onClick = {
                                viewModel.dismissPasskeyRegistrationMessage()
                                runCatching { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
                            }) { Text(atlasText("Paramètres Android")) }
                        }
                    },
                )
            }
        }
    }
}
