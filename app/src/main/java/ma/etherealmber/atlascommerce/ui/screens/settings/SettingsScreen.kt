package ma.etherealmber.atlascommerce.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalTime
import java.time.ZoneId
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.DensityMode
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.ThemeMode
import ma.etherealmber.atlascommerce.core.update.UpdateState
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.SavedAccountEntry
import ma.etherealmber.atlascommerce.core.security.DeviceUnlockAuthenticator
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasOutlinedButton
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasSkeletonList
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.SettingsUpdateCard
import ma.etherealmber.atlascommerce.ui.i18n.localizeReleaseNotes
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.root.AppViewModel
import ma.etherealmber.atlascommerce.ui.screens.legal.EtherealmberAuthorCard
import ma.etherealmber.atlascommerce.ui.screens.legal.ProblemReportDialog
import ma.etherealmber.atlascommerce.ui.shell.MainTab
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import ma.etherealmber.atlascommerce.ui.util.findActivity
import ma.etherealmber.atlascommerce.ui.util.openExternal

@Composable
private fun AccountManagerRow(
    avatarUrl: String?,
    displayName: String,
    username: String,
    role: String,
    serverBaseUrl: String?,
    active: Boolean,
    subtitle: String? = null,
    onClick: (() -> Unit)?,
    onForget: (() -> Unit)? = null,
) {
    AtlasCard(onClick = onClick, padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AtlasAvatar(avatarUrl = avatarUrl, label = displayName, size = 42.dp, serverBaseUrl = serverBaseUrl)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("@$username · $role", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (active) {
                AtlasChip(text = atlasText("Actif", "Active", "نشط"), tone = AtlasTone.POSITIVE)
            } else if (onForget != null) {
                IconButton(onClick = onForget) {
                    Icon(Icons.Outlined.Close, contentDescription = atlasText("Oublier ce compte", "Forget this account", "نسيان هذا الحساب"))
                }
            }
        }
    }
}

/**
 * Dedicated **Profile & Personalization** screen (`MainTab.PROFILE`).
 *
 * Uses native `Column + verticalScroll(rememberScrollState())` instead of variable-height
 * `LazyColumn` items so fast upward flings from the bottom never snap or skip back to index 0.
 * Displays ONLY account details + app customizations, with direct access to [SettingsScreen]
 * via the top-right Settings button.
 */
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    user: SessionUser,
    onOpenSettings: () -> Unit,
    onOpenQrDevice: () -> Unit,
    onSignOut: () -> Unit,
    onToast: (String) -> Unit,
    isForeground: Boolean = true,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val density = Atlas.density
    val scrollState = rememberScrollState()

    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
    val appLanguage by appViewModel.appLanguage.collectAsStateWithLifecycle()
    val appLanguageFollowsSystem by appViewModel.appLanguageFollowsSystem.collectAsStateWithLifecycle()
    val densityMode by appViewModel.densityMode.collectAsStateWithLifecycle()
    val animationsEnabled by appViewModel.animationsEnabled.collectAsStateWithLifecycle()
    val autoOpenCamera by appViewModel.autoOpenCamera.collectAsStateWithLifecycle()
    val scanHapticEnabled by appViewModel.scanHapticEnabled.collectAsStateWithLifecycle()
    val scanSoundEnabled by appViewModel.scanSoundEnabled.collectAsStateWithLifecycle()
    val autoAddCartOnScan by appViewModel.autoAddCartOnScan.collectAsStateWithLifecycle()
    val defaultStartTab by appViewModel.defaultStartTab.collectAsStateWithLifecycle()
    val savedClientName by appViewModel.defaultClientName.collectAsStateWithLifecycle()
    val confirmBeforeSale by appViewModel.confirmBeforeSale.collectAsStateWithLifecycle()
    val roleNotificationsEnabled by appViewModel.roleNotificationsEnabled.collectAsStateWithLifecycle()
    val serverSelection by appViewModel.server.collectAsStateWithLifecycle()
    val savedAccounts by appViewModel.savedAccounts.collectAsStateWithLifecycle()
    val uploadingAvatar by appViewModel.uploadingAvatar.collectAsStateWithLifecycle()
    val savingProfile by appViewModel.savingProfile.collectAsStateWithLifecycle()

    var confirmSignOut by remember { mutableStateOf(false) }
    var showThemeSwitcher by remember { mutableStateOf(false) }
    var addAccountAfterSignOut by remember { mutableStateOf(false) }
    var showAccountManager by remember { mutableStateOf(false) }
    var reopenAccountManagerOnReturn by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var draftDisplayName by remember(user.displayName) { mutableStateOf(user.displayName) }
    var draftEmail by remember(user.email) { mutableStateOf(user.email) }
    var draftPhone by remember(user.phone) { mutableStateOf(user.phone) }
    var draftClientName by remember(savedClientName) { mutableStateOf(savedClientName) }

    LaunchedEffect(isForeground, user.scopeIdentity) {
        if (isForeground && reopenAccountManagerOnReturn) {
            reopenAccountManagerOnReturn = false
            showAccountManager = true
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            appViewModel.uploadAvatarFromUri(context, uri, onToast)
        }
    }

    val currentSavedAccount = savedAccounts.firstOrNull {
        it.username.equals(user.username, ignoreCase = true)
    }
    val otherSavedAccounts = savedAccounts.filterNot {
        it.username.equals(user.username, ignoreCase = true)
    }
    val configureDeviceUnlockMessage = atlasText(
        "Configurez la biométrie ou un verrouillage d'écran sécurisé pour activer la connexion rapide.",
        "Set up biometrics or a secure screen lock to enable quick sign-in.",
        "أعد إعداد القياسات الحيوية أو قفل شاشة آمن لتفعيل تسجيل الدخول السريع.",
    )
    val localUnlockUnavailableMessage = atlasText(
        "Le déverrouillage local est indisponible. Utilisez votre mot de passe et configurez un verrouillage sécurisé.",
        "Local device unlock is unavailable. Use your password and set up a secure device lock.",
        "إلغاء القفل المحلي غير متاح. استخدم كلمة المرور وأعد إعداد قفل آمن.",
    )
    val savedAccountUnlockUnavailableMessage = atlasText(
        "Le déverrouillage local est indisponible. Utilisez le mot de passe et configurez la biométrie ou un verrouillage sécurisé.",
        "Local device unlock is unavailable. Use your password and set up biometrics or a secure screen lock.",
        "إلغاء القفل المحلي غير متاح. استخدم كلمة المرور وأعد إعداد القياسات الحيوية أو قفل شاشة آمن.",
    )
    val deviceUnlockAuthTitle = atlasText("Activer le déverrouillage Atlas", "Enable Atlas device unlock", "تفعيل إلغاء قفل Atlas")
    val deviceUnlockAuthSubtitle = atlasText("Confirmez avec la biométrie ou le code de votre appareil.", "Confirm with biometrics or your device PIN/password.", "أكد هويتك بالبصمة أو رمز جهازك.")
    val unlockSavedAccountTitle = atlasText("Déverrouiller Atlas", "Unlock Atlas", "إلغاء قفل Atlas")
    val updateCurrentDeviceUnlock: (Boolean) -> Unit = { enabled ->
        val account = currentSavedAccount
        if (account != null && !enabled) {
            appViewModel.setSavedAccountDeviceUnlock(account.serverUrl, account.username, false)
        } else if (account != null) {
            val activity = context.findActivity() as? FragmentActivity
            if (activity == null || !DeviceUnlockAuthenticator.isAvailable(activity)) {
                appViewModel.setSavedAccountDeviceUnlock(account.serverUrl, account.username, false)
                onToast(configureDeviceUnlockMessage)
            } else {
                DeviceUnlockAuthenticator.authenticate(
                    activity,
                    deviceUnlockAuthTitle,
                    deviceUnlockAuthSubtitle,
                ) { outcome ->
                    when (outcome) {
                        DeviceUnlockAuthenticator.Outcome.AUTHENTICATED -> appViewModel.setSavedAccountDeviceUnlock(account.serverUrl, account.username, true)
                        DeviceUnlockAuthenticator.Outcome.CANCELED -> Unit
                        DeviceUnlockAuthenticator.Outcome.UNAVAILABLE,
                        DeviceUnlockAuthenticator.Outcome.FAILED -> {
                            appViewModel.setSavedAccountDeviceUnlock(account.serverUrl, account.username, false)
                            onToast(localUnlockUnavailableMessage)
                        }
                    }
                }
            }
        }
    }
    val unlockAndSwitchSavedAccount: (SavedAccountEntry) -> Unit = { account ->
        val activity = context.findActivity()
        val fragmentActivity = activity as? FragmentActivity
        if (!account.canUseDeviceUnlock) {
            Unit
        } else if (fragmentActivity == null || !DeviceUnlockAuthenticator.isAvailable(fragmentActivity)) {
            appViewModel.setSavedAccountDeviceUnlock(account.serverUrl, account.username, false)
            onToast(savedAccountUnlockUnavailableMessage)
        } else {
            DeviceUnlockAuthenticator.authenticate(
                fragmentActivity,
                unlockSavedAccountTitle,
                deviceUnlockAuthSubtitle,
            ) { outcome ->
                when (outcome) {
                    DeviceUnlockAuthenticator.Outcome.AUTHENTICATED -> appViewModel.switchSavedAccount(account, onToast)
                    DeviceUnlockAuthenticator.Outcome.CANCELED -> Unit
                    DeviceUnlockAuthenticator.Outcome.UNAVAILABLE,
                    DeviceUnlockAuthenticator.Outcome.FAILED -> {
                        appViewModel.setSavedAccountDeviceUnlock(account.serverUrl, account.username, false)
                        onToast(savedAccountUnlockUnavailableMessage)
                    }
                }
            }
        }
    }
    val profileGreeting = if (LocalTime.now(ZoneId.systemDefault()).hour in 5..17) {
        atlasText("Bonjour", "Good morning", "صباح الخير")
    } else {
        atlasText("Bonsoir", "Good evening", "مساء الخير")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = density.screenPadding,
                end = density.screenPadding,
                top = density.cardMargin,
                bottom = 32.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(density.itemGap),
    ) {
        // 1. Account & Profile Header Card (tap directly on avatar to change photo)
        AtlasEntrance(delayMillis = 0) {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(68.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AtlasAvatar(
                            avatarUrl = user.avatarUrl,
                            label = user.username,
                            size = 64.dp,
                            serverBaseUrl = serverSelection?.baseUrl,
                            clickContentDescription = atlasText(
                                "Changer la photo de profil de ${user.username}",
                                "Change ${user.username}'s profile photo",
                                "تغيير صورة الملف الشخصي لـ ${user.username}",
                            ),
                            onClick = if (!uploadingAvatar) ({ photoPicker.launch("image/*") }) else null,
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                if (uploadingAvatar) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.8.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.PhotoCamera,
                                        contentDescription = atlasText("Changer la photo de profil", "Change profile photo", "تغيير صورة الملف الشخصي"),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = profileGreeting,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            draftDisplayName = user.displayName
                            draftEmail = user.email
                            draftPhone = user.phone
                            showEditProfile = true
                        },
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = atlasText("Modifier le profil", "Edit profile", "تعديل الملف الشخصي"))
                    }
                    IconButton(onClick = { showAccountManager = true }) {
                        Icon(
                            Icons.Outlined.SwapHoriz,
                            contentDescription = atlasText("Changer de compte", "Switch account", "تبديل الحساب"),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                AtlasOutlinedButton(
                    text = atlasText("Appareils liés", "Linked devices", "الأجهزة المرتبطة"),
                    onClick = onOpenQrDevice,
                    icon = Icons.Outlined.Devices,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                AtlasOutlinedButton(
                    text = atlasText("Se déconnecter de cette session", "Sign out of this session", "تسجيل الخروج من هذه الجلسة"),
                    onClick = { confirmSignOut = true },
                    icon = Icons.Outlined.Logout,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        currentSavedAccount?.let { account ->
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            atlasText("Déverrouillage de l'appareil", "Device unlock", "إلغاء قفل الجهاز"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (account.canUseDeviceUnlock) {
                                atlasText("Biométrie ou code de l'appareil activé.", "Biometrics or device PIN/password is enabled.", "القياسات الحيوية أو رمز الجهاز مفعّل.")
                            } else {
                                atlasText("Désactivé — le mot de passe reste nécessaire.", "Off — your account password is still required.", "متوقف — لا تزال كلمة مرور الحساب مطلوبة.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = account.canUseDeviceUnlock,
                        onCheckedChange = updateCurrentDeviceUnlock,
                    )
                }
            }
        }

        // 2. Appearance, Navigation & Motion Customizations
        AtlasCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesomeMotion,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = atlasText("Apparence, navigation & animations"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            val roleAllowedTabs = MainTab.bottomTabsForRole(user.role)
            val selectedLaunchTab = roleAllowedTabs.firstOrNull { it.name == defaultStartTab }
                ?: roleAllowedTabs.firstOrNull { it == MainTab.HOME }
                ?: roleAllowedTabs.first()
            Spacer(Modifier.height(10.dp))
            ThemeSettingRow(
                value = when (themeMode) {
                    ThemeMode.SYSTEM -> atlasText("Système", "System", "النظام")
                    ThemeMode.LIGHT -> atlasText("Clair", "Light", "فاتح")
                    ThemeMode.DARK -> atlasText("Sombre", "Dark", "داكن")
                },
                onClick = { showThemeSwitcher = true },
            )
            if (showThemeSwitcher) {
                ThemeSwitcherDialog(
                    currentMode = themeMode,
                    animationsEnabled = animationsEnabled,
                    onDismiss = { showThemeSwitcher = false },
                    onApply = { appViewModel.setThemeMode(it) },
                )
            }
            SettingsChoiceRow(
                icon = Icons.Outlined.Language,
                title = atlasText("Langue", "Language", "اللغة"),
                value = if (appLanguageFollowsSystem) {
                    atlasText("Système · ${AppLanguage.fromSystemLocale().nativeLabel}", "System · ${AppLanguage.fromSystemLocale().nativeLabel}", "النظام · ${AppLanguage.fromSystemLocale().nativeLabel}")
                } else appLanguage.nativeLabel,
                selectedKey = if (appLanguageFollowsSystem) "SYSTEM" else appLanguage.name,
                options = listOf("SYSTEM" to atlasText("Système · ${AppLanguage.fromSystemLocale().nativeLabel}", "System · ${AppLanguage.fromSystemLocale().nativeLabel}", "النظام · ${AppLanguage.fromSystemLocale().nativeLabel}")) +
                    AppLanguage.entries.map { it.name to it.nativeLabel },
                onSelected = { key ->
                    if (key == "SYSTEM") appViewModel.followSystemAppLanguage()
                    else AppLanguage.entries.firstOrNull { it.name == key }?.let(appViewModel::setAppLanguage)
                },
            )
            SettingsChoiceRow(
                icon = Icons.Outlined.Tune,
                title = atlasText("Densité d'affichage", "Display density", "كثافة العرض"),
                value = when (densityMode) {
                    DensityMode.COMPACT -> atlasText("Compact", "Compact", "مضغوط")
                    DensityMode.COMFORT -> atlasText("Confort", "Comfort", "مريح")
                },
                selectedKey = densityMode.name,
                options = DensityMode.entries.map { mode ->
                    mode.name to when (mode) {
                        DensityMode.COMPACT -> atlasText("Compact", "Compact", "مضغوط")
                        DensityMode.COMFORT -> atlasText("Confort", "Comfort", "مريح")
                    }
                },
                onSelected = { key -> DensityMode.entries.firstOrNull { it.name == key }?.let(appViewModel::setDensityMode) },
            )
            SettingsChoiceRow(
                icon = Icons.Outlined.Home,
                title = atlasText("Onglet d'ouverture au lancement", "Tab to open at launch", "علامة التبويب عند التشغيل"),
                value = settingsTabLabel(selectedLaunchTab),
                selectedKey = selectedLaunchTab.name,
                options = roleAllowedTabs.map { it.name to settingsTabLabel(it) },
                onSelected = { key ->
                    if (roleAllowedTabs.any { it.name == key }) appViewModel.setDefaultStartTab(key)
                },
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsToggleRow(
                title = atlasText("Animations fluides", "Smooth animations", "حركات سلسة"),
                subtitle = atlasText("Transitions entre les pages, apparitions d'accueil et effets de mouvement.", "Page transitions, home screen entrances, and motion effects.", "انتقالات الصفحات وحركات ظهور الشاشة الرئيسية وتأثيرات الحركة."),
                checked = animationsEnabled,
                onCheckedChange = appViewModel::setAnimationsEnabled,
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsToggleRow(
                title = atlasText("Notifications système par rôle", "Role-based system notifications", "إشعارات النظام حسب الدور"),
                subtitle = atlasText(
                    "Affiche les notifications Android filtrées selon votre rôle (${profileRoleLabel(user.role)}).",
                    "Shows Android notifications filtered for your role (${profileRoleLabel(user.role)}).",
                    "يعرض إشعارات Android المصفّاة حسب دورك (${profileRoleLabel(user.role)}).",
                ),
                checked = roleNotificationsEnabled,
                onCheckedChange = appViewModel::setRoleNotificationsEnabled,
            )
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    }.onFailure { onToast("Ouvrez Paramètres Android > Applications > Atlas Commerce > Notifications.") }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(atlasText("Autorisation et canaux de notification Android"))
            }
        }

        // 3. Camera & Barcode Customizations
        AtlasCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = atlasText("Caméra & lecteur code-barres"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            SettingsToggleRow(
                title = atlasText("Ouvrir la caméra automatiquement", "Open camera automatically", "فتح الكاميرا تلقائياً"),
                subtitle = atlasText("Désactivé par défaut — la caméra reste fermée en entrant dans Scan ou QR PC.", "Off by default — the camera stays closed when opening Scan or PC QR.", "معطّلة افتراضياً — تظل الكاميرا مغلقة عند فتح المسح أو QR الكمبيوتر."),
                checked = autoOpenCamera,
                onCheckedChange = appViewModel::setAutoOpenCamera,
            )
            AtlasDivider(Modifier.padding(vertical = 6.dp))
            SettingsToggleRow(
                title = atlasText("Vibration au scan", "Vibrate on scan", "الاهتزاز عند المسح"),
                subtitle = atlasText("Retour haptique dès qu'un code-barres ou QR est détecté.", "Vibrate when a barcode or QR code is detected.", "اهتزاز عند اكتشاف باركود أو رمز QR."),
                checked = scanHapticEnabled,
                onCheckedChange = appViewModel::setScanHapticEnabled,
            )
            AtlasDivider(Modifier.padding(vertical = 6.dp))
            SettingsToggleRow(
                title = atlasText("Signal sonore au scan", "Scan sound", "صوت المسح"),
                subtitle = atlasText("Bip court de confirmation lors de la lecture d'un code.", "Play a short confirmation beep when a code is read.", "إصدار نغمة تأكيد قصيرة عند قراءة الرمز."),
                checked = scanSoundEnabled,
                onCheckedChange = appViewModel::setScanSoundEnabled,
            )
            AtlasDivider(Modifier.padding(vertical = 6.dp))
            SettingsToggleRow(
                title = atlasText("Ajout automatique au panier", "Automatically add to cart", "الإضافة التلقائية إلى السلة"),
                subtitle = atlasText("Ajoute immédiatement 1 unité au ticket VR lorsqu'un article est scanné.", "Immediately add one unit to the VR receipt when an item is scanned.", "إضافة وحدة واحدة فوراً إلى إيصال VR عند مسح المنتج."),
                checked = autoAddCartOnScan,
                onCheckedChange = appViewModel::setAutoAddCartOnScan,
            )
        }

        // 4. Cashier / Quick Sale Customizations (Role-gated)
        if (Roles.canCreateDocuments(user.role)) {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.PointOfSale,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = atlasText("Vente rapide & caisse"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AtlasTextField(
                        value = draftClientName,
                        onValueChange = { draftClientName = it },
                        label = atlasText("Client pré-rempli par défaut"),
                        placeholder = atlasText("Ex. Client comptoir"),
                        leadingIcon = Icons.Outlined.Person,
                        imeAction = ImeAction.Done,
                        onDone = {
                            appViewModel.setDefaultClientName(draftClientName)
                            onToast("Client par défaut enregistré.")
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    AtlasOutlinedButton(
                        text = atlasText("OK"),
                        onClick = {
                            appViewModel.setDefaultClientName(draftClientName)
                            onToast("Client par défaut enregistré.")
                        },
                    )
                }
                AtlasDivider(Modifier.padding(vertical = 8.dp))
                SettingsToggleRow(
                    title = atlasText("Confirmer avant création du ticket"),
                    subtitle = atlasText("Affiche un récapitulatif du montant avant d'envoyer la vente au serveur."),
                    checked = confirmBeforeSale,
                    onCheckedChange = appViewModel::setConfirmBeforeSale,
                )
            }
        }

    }

    if (showAccountManager) {
        Dialog(
            onDismissRequest = { showAccountManager = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { showAccountManager = false }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = atlasText("Retour", "Back", "رجوع"))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                atlasText("Comptes et appareils", "Accounts & devices", "الحسابات والأجهزة"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                atlasText("Ce serveur uniquement", "This server only", "هذا الخادم فقط"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    AtlasDivider()
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            atlasText("Compte actif", "Active account", "الحساب النشط"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        AccountManagerRow(
                            avatarUrl = user.avatarUrl,
                            displayName = user.displayName.ifBlank { user.username },
                            username = user.username,
                            role = profileRoleLabel(user.role),
                            serverBaseUrl = serverSelection?.baseUrl,
                            active = true,
                            onClick = null,
                        )

                        Text(
                            atlasText("Autres comptes enregistrés", "Other saved accounts", "حسابات أخرى محفوظة"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        if (otherSavedAccounts.isEmpty()) {
                            Text(
                                atlasText(
                                    "Aucun autre compte n'est enregistré sur ce serveur.",
                                    "No other accounts are saved on this server.",
                                    "لا توجد حسابات أخرى محفوظة على هذا الخادم.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            otherSavedAccounts.forEach { account ->
                                AccountManagerRow(
                                    avatarUrl = account.avatarUrl,
                                    displayName = account.displayName.ifBlank { account.username },
                                    username = account.username,
                                    role = profileRoleLabel(account.role),
                                    serverBaseUrl = serverSelection?.baseUrl,
                                    active = false,
                                    subtitle = if (account.canUseDeviceUnlock) {
                                        atlasText("Déverrouillage de l'appareil activé", "Device unlock enabled", "إلغاء قفل الجهاز مفعّل")
                                    } else {
                                        atlasText("Mot de passe requis", "Password required", "كلمة المرور مطلوبة")
                                    },
                                    onClick = {
                                        if (account.canUseDeviceUnlock) {
                                            unlockAndSwitchSavedAccount(account)
                                        } else {
                                            showAccountManager = false
                                            addAccountAfterSignOut = true
                                            confirmSignOut = true
                                        }
                                    },
                                    onForget = { appViewModel.forgetSavedAccount(account, onToast) },
                                )
                            }
                        }

                        AtlasDivider(Modifier.padding(vertical = 2.dp))
                        AtlasCard(onClick = {
                            showAccountManager = false
                            reopenAccountManagerOnReturn = true
                            onOpenQrDevice()
                        }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(atlasText("Lier un autre appareil", "Link another device", "ربط جهاز آخر"), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        atlasText("Ouvrir la gestion sécurisée des appareils", "Open secure device management", "فتح إدارة الأجهزة الآمنة"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        AtlasButton(
                            text = atlasText("Ajouter ou connecter un autre compte", "Add or sign in to another account", "إضافة حساب آخر أو تسجيل الدخول إليه"),
                            onClick = {
                                showAccountManager = false
                                addAccountAfterSignOut = true
                                confirmSignOut = true
                            },
                            icon = Icons.Outlined.Person,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showEditProfile) {
        val profileUpdatedToast = atlasText("Profil mis à jour.", "Profile updated.", "تم تحديث الملف الشخصي.")
        val emptyDisplayNameToast = atlasText(
            "Le nom affiché ne peut pas être vide.",
            "Display name cannot be empty.",
            "لا يمكن أن يكون الاسم المعروض فارغاً.",
        )
        Dialog(
            onDismissRequest = { if (!savingProfile) showEditProfile = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { if (!savingProfile) showEditProfile = false }, enabled = !savingProfile) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = atlasText("Retour", "Back", "رجوع"))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(atlasText("Modifier le profil", "Edit profile", "تعديل الملف الشخصي"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(atlasText("Informations du compte", "Account information", "معلومات الحساب"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    AtlasDivider()
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        AtlasCard(padding = 16.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AtlasAvatar(
                                    avatarUrl = user.avatarUrl,
                                    label = user.displayName.ifBlank { user.username },
                                    size = 64.dp,
                                    serverBaseUrl = serverSelection?.baseUrl,
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(user.displayName.ifBlank { user.username }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { if (!uploadingAvatar) photoPicker.launch("image/*") }, enabled = !uploadingAvatar) {
                                    if (uploadingAvatar) {
                                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Outlined.PhotoCamera, contentDescription = atlasText("Changer la photo de profil", "Change profile photo", "تغيير صورة الملف الشخصي"))
                                    }
                                }
                            }
                        }
                        AtlasTextField(
                            value = draftDisplayName,
                            onValueChange = { draftDisplayName = it },
                            label = atlasText("Nom affiché *", "Display name *", "الاسم المعروض *"),
                            placeholder = atlasText("Ex. Nom complet", "Full name", "الاسم الكامل"),
                            supportingText = atlasText("Nom affiché dans votre espace de travail", "Name shown in your workspace", "الاسم الظاهر في مساحة العمل"),
                            leadingIcon = Icons.Outlined.Person,
                            imeAction = ImeAction.Next,
                        )
                        AtlasTextField(
                            value = draftEmail,
                            onValueChange = { draftEmail = it },
                            label = atlasText("Adresse e-mail", "Email address", "البريد الإلكتروني"),
                            placeholder = "contact@example.com",
                            leadingIcon = Icons.Outlined.Email,
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        )
                        AtlasTextField(
                            value = draftPhone,
                            onValueChange = { draftPhone = it },
                            label = atlasText("Numéro de téléphone", "Phone number", "رقم الهاتف"),
                            placeholder = "+212 …",
                            leadingIcon = Icons.Outlined.Phone,
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done,
                        )
                    }
                    AtlasDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TextButton(onClick = { showEditProfile = false }, enabled = !savingProfile, modifier = Modifier.weight(1f)) {
                            Text(atlasText("Annuler", "Cancel", "إلغاء"))
                        }
                        AtlasButton(
                            text = atlasText("Enregistrer", "Save", "حفظ"),
                            loading = savingProfile,
                            onClick = {
                                appViewModel.updateProfileDetails(draftDisplayName, draftEmail, draftPhone) { msg, success ->
                                    val toastMessage = when {
                                        success -> profileUpdatedToast
                                        msg == "Le nom affiché ne peut pas être vide." -> emptyDisplayNameToast
                                        else -> msg
                                    }
                                    onToast(toastMessage)
                                    if (success) showEditProfile = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false; addAccountAfterSignOut = false },
            title = {
                Text(
                    if (addAccountAfterSignOut) atlasText("Changer de compte ?", "Switch accounts?", "تبديل الحساب؟")
                    else atlasText("Se déconnecter ?", "Sign out?", "تسجيل الخروج؟"),
                )
            },
            text = {
                Text(
                    if (addAccountAfterSignOut) {
                        atlasText(
                            "La session actuelle sera fermée. Vous pourrez ensuite choisir un compte enregistré ou vous connecter avec un autre compte. Les données du serveur ne sont pas supprimées.",
                            "Your current session will end. Then you can choose a saved account or sign in with another account. Server data will not be deleted.",
                            "ستنتهي جلستك الحالية. بعد ذلك يمكنك اختيار حساب محفوظ أو تسجيل الدخول بحساب آخر. لن تُحذف بيانات الخادم.",
                        )
                    } else {
                        atlasText(
                            "La session active, le panier et les données temporaires seront effacés. Aucune donnée métier n'est supprimée sur le serveur.",
                            "The active session, cart and temporary data will be cleared. No business data will be deleted from the server.",
                            "سيتم مسح الجلسة النشطة والسلة والبيانات المؤقتة. لن تُحذف أي بيانات تجارية على الخادم.",
                        )
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSignOut = false
                        addAccountAfterSignOut = false
                        onSignOut()
                    },
                ) { Text(if (addAccountAfterSignOut) atlasText("Continuer", "Continue", "متابعة") else atlasText("Se déconnecter", "Sign out", "تسجيل الخروج")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false; addAccountAfterSignOut = false }) {
                    Text(atlasText("Annuler", "Cancel", "إلغاء"))
                }
            },
        )
    }
}

/**
 * Dedicated **System & Security Settings** screen (`MainTab.SETTINGS`), opened via the top-right
 * Settings button inside [ProfileScreen].
 *
 * Includes saved-server management, OTA APK updates, isolated offline cache, and legal information.
 * Account/device management lives in Profile; Atlas Agent quota lives in the Agent experience.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSettingRow(
    value: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(atlasText("Thème", "Theme", "المظهر"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            }
            Text(atlasText("Personnaliser", "Customize", "تخصيص"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ThemeSwitcherDialog(
    currentMode: ThemeMode,
    animationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onApply: (ThemeMode) -> Unit,
) {
    var selectedMode by remember(currentMode) { mutableStateOf(currentMode) }
    var applyAfterClose by remember { mutableStateOf(false) }
    var hasEntered by remember { mutableStateOf(false) }
    val visibility = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        visibility.targetState = true
    }
    LaunchedEffect(visibility.currentState, visibility.targetState, visibility.isIdle, hasEntered) {
        if (visibility.currentState) hasEntered = true
        if (hasEntered && visibility.isIdle && !visibility.currentState && !visibility.targetState) {
            if (applyAfterClose) onApply(selectedMode)
            onDismiss()
        }
    }
    fun close(apply: Boolean) {
        if (visibility.targetState) {
            applyAfterClose = apply
            visibility.targetState = false
        }
    }

    val enter = if (animationsEnabled) {
        fadeIn(animationSpec = tween(240)) + scaleIn(
            initialScale = 0.92f,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        )
    } else EnterTransition.None
    val exit = if (animationsEnabled) {
        fadeOut(animationSpec = tween(150)) + scaleOut(
            targetScale = 0.97f,
            animationSpec = tween(150),
        )
    } else ExitTransition.None
    val configuration = LocalConfiguration.current
    val compactLayout = configuration.screenWidthDp < 420
    val dialogMaxHeight = (configuration.screenHeightDp.dp - 32.dp).coerceAtLeast(300.dp)

    Dialog(
        onDismissRequest = { close(apply = false) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AnimatedVisibility(visibleState = visibility, enter = enter, exit = exit) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(0.94f)
                    .heightIn(max = dialogMaxHeight),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 20.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = dialogMaxHeight)
                        .verticalScroll(rememberScrollState())
                        .padding(if (compactLayout) 16.dp else 22.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compactLayout) 12.dp else 16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            atlasText("Choisir un thème", "Choose a theme", "اختر المظهر"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            atlasText(
                                "Choisissez une ambiance. Le thème ne changera qu'après la fermeture de cette fenêtre.",
                                "Pick a look. Your theme changes only after this dialog closes.",
                                "اختر المظهر. لن يتغير التطبيق إلا بعد إغلاق هذه النافذة.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (compactLayout) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                ThemeModeCompactOption(
                                    mode = mode,
                                    selected = selectedMode == mode,
                                    enabled = visibility.targetState,
                                    onClick = { selectedMode = mode },
                                )
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                ThemePreviewCard(
                                    mode = mode,
                                    selected = selectedMode == mode,
                                    enabled = visibility.targetState,
                                    animationsEnabled = animationsEnabled,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedMode = mode },
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { close(apply = false) }, enabled = visibility.targetState) {
                            Text(atlasText("Annuler", "Cancel", "إلغاء"))
                        }
                        Spacer(Modifier.width(8.dp))
                        AtlasButton(
                            text = atlasText("Appliquer le thème", "Apply theme", "تطبيق المظهر"),
                            onClick = { close(apply = true) },
                            enabled = visibility.targetState,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeCompactOption(
    mode: ThemeMode,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val title = when (mode) {
        ThemeMode.SYSTEM -> atlasText("Système", "System", "النظام")
        ThemeMode.LIGHT -> atlasText("Clair", "Light", "فاتح")
        ThemeMode.DARK -> atlasText("Sombre", "Dark", "داكن")
    }
    val description = when (mode) {
        ThemeMode.SYSTEM -> atlasText("Selon l'appareil", "Follows device", "حسب الجهاز")
        ThemeMode.LIGHT -> atlasText("Clair et net", "Bright and clear", "فاتح وواضح")
        ThemeMode.DARK -> atlasText("Confort nocturne", "Night comfort", "مريح ليلاً")
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        when (mode) {
                            ThemeMode.SYSTEM -> "◐"
                            ThemeMode.LIGHT -> "☼"
                            ThemeMode.DARK -> "☾"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        }
    }
}

@Composable
private fun ThemePreviewCard(
    mode: ThemeMode,
    selected: Boolean,
    enabled: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val dark = mode == ThemeMode.DARK
    val previewBackground = if (dark) Color(0xFF191A1E) else Color(0xFFF4F1EA)
    val previewSurface = if (dark) Color(0xFF282A30) else Color.White
    val previewInk = if (dark) Color(0xFFF4F4F5) else Color(0xFF202124)
    val previewMuted = if (dark) Color(0xFFA9ABB2) else Color(0xFF77736D)
    val selectedBorder = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderColor by animateColorAsState(
        targetValue = selectedBorder,
        animationSpec = if (animationsEnabled) tween(260) else snap(),
        label = "theme-option-border",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = if (animationsEnabled) spring(dampingRatio = 0.82f, stiffness = 420f) else snap(),
        label = "theme-option-border-width",
    )

    Surface(
        modifier = modifier
            .then(if (animationsEnabled) Modifier.animateContentSize(spring(dampingRatio = 0.82f, stiffness = 480f)) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = previewBackground,
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else previewSurface,
                    modifier = Modifier.size(26.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "◐"
                                ThemeMode.LIGHT -> "☼"
                                ThemeMode.DARK -> "☾"
                            },
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else previewInk,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    when (mode) {
                        ThemeMode.SYSTEM -> atlasText("Système", "System", "النظام")
                        ThemeMode.LIGHT -> atlasText("Clair", "Light", "فاتح")
                        ThemeMode.DARK -> atlasText("Sombre", "Dark", "داكن")
                    },
                    color = previewInk,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = previewSurface,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Column(Modifier.padding(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = if (mode == ThemeMode.SYSTEM) Color(0xFFB88A44) else MaterialTheme.colorScheme.primary, modifier = Modifier.size(8.dp)) {}
                        Spacer(Modifier.width(5.dp))
                        Surface(shape = RoundedCornerShape(99.dp), color = previewMuted.copy(alpha = 0.48f), modifier = Modifier.fillMaxWidth(0.58f).height(4.dp)) {}
                    }
                    Surface(shape = RoundedCornerShape(99.dp), color = previewMuted.copy(alpha = 0.25f), modifier = Modifier.fillMaxWidth(0.82f).height(4.dp)) {}
                    if (mode == ThemeMode.SYSTEM) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFFE8E3D9), modifier = Modifier.weight(1f).height(8.dp)) {}
                            Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFF34363C), modifier = Modifier.weight(1f).height(8.dp)) {}
                        }
                    }
                }
            }
            Text(
                when (mode) {
                    ThemeMode.SYSTEM -> atlasText("Selon l'appareil", "Follows device", "حسب الجهاز")
                    ThemeMode.LIGHT -> atlasText("Clair et net", "Bright and clear", "فاتح وواضح")
                    ThemeMode.DARK -> atlasText("Confort nocturne", "Night comfort", "مريح ليلاً")
                },
                style = MaterialTheme.typography.labelSmall,
                color = previewMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SettingsChoiceRow(
    icon: ImageVector,
    title: String,
    value: String,
    selectedKey: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    var pickerOpen by remember(title) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { pickerOpen = true },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            }
            Text(atlasText("Modifier", "Change", "تغيير"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
    if (pickerOpen) {
        val maxHeight = (LocalConfiguration.current.screenHeightDp.dp - 32.dp).coerceAtLeast(280.dp)
        Dialog(
            onDismissRequest = { pickerOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(0.92f).heightIn(max = maxHeight),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 18.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    options.forEach { (key, label) ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onSelected(key)
                                pickerOpen = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (key == selectedKey) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, if (key == selectedKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = key == selectedKey,
                                    onClick = {
                                        onSelected(key)
                                        pickerOpen = false
                                    },
                                )
                                Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { pickerOpen = false }) {
                            Text(atlasText("Fermer", "Close", "إغلاق"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    user: SessionUser,
    onChangeServer: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSnake: () -> Unit = {},
    onOpenDeveloperTools: () -> Unit = {},
    onSignOut: () -> Unit,
    onToast: (String) -> Unit,
    onStartUpdateDownload: () -> Unit = {},
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val density = Atlas.density
    val scrollState = rememberScrollState()

    val autoCheckUpdates by appViewModel.autoCheckUpdates.collectAsStateWithLifecycle()
    val automaticCrashReportsEnabled by appViewModel.automaticCrashReportsEnabled.collectAsStateWithLifecycle()
    val automaticCrashReportsAvailable by appViewModel.automaticCrashReportsAvailable.collectAsStateWithLifecycle()
    val crashReportsAvailabilityChecked by appViewModel.crashReportsAvailabilityChecked.collectAsStateWithLifecycle()
    val parallelUpdateDownload by appViewModel.parallelUpdateDownload.collectAsStateWithLifecycle()
    val followLatestRelease by appViewModel.followLatestRelease.collectAsStateWithLifecycle()
    val serverSelection by appViewModel.server.collectAsStateWithLifecycle()
    val appLanguage by appViewModel.appLanguage.collectAsStateWithLifecycle()
    val savedServers by appViewModel.savedServers.collectAsStateWithLifecycle()
    val updateState by appViewModel.updateState.collectAsStateWithLifecycle()
    val updateDownloadSource by appViewModel.updateDownloadSource.collectAsStateWithLifecycle()
    val localStorageSummary by appViewModel.localStorageSummary.collectAsStateWithLifecycle()
    val shakeToReportEnabled by appViewModel.shakeToReportEnabled.collectAsStateWithLifecycle()
    val mirrorPing by appViewModel.mirrorPing.collectAsStateWithLifecycle()
    val releaseChangelog by appViewModel.releaseChangelog.collectAsStateWithLifecycle()
    val releaseChangelogLoading by appViewModel.releaseChangelogLoading.collectAsStateWithLifecycle()
    val animationsEnabled by appViewModel.animationsEnabled.collectAsStateWithLifecycle()
    val developerModeEnabled by appViewModel.developerModeEnabled.collectAsStateWithLifecycle()
    var showProblemReportDialog by remember { mutableStateOf(false) }
    var showReleaseHistoryDialog by remember { mutableStateOf(false) }
    var confirmClearUpdatePackage by remember { mutableStateOf(false) }
    var buildTapCount by remember { mutableIntStateOf(0) }
    var lastBuildTapAt by remember { mutableLongStateOf(0L) }

    val onBuildVersionTap: () -> Unit = {
        val now = System.currentTimeMillis()
        if (now - lastBuildTapAt > 2_000L) buildTapCount = 0
        lastBuildTapAt = now
        buildTapCount += 1
        if (buildTapCount >= 7) {
            buildTapCount = 0
            val enabled = !developerModeEnabled
            appViewModel.setDeveloperModeEnabled(enabled)
            if (enabled) {
                onToast("Mode développeur activé. Ouverture des outils développeur.")
                onOpenDeveloperTools()
            } else {
                onToast("Mode développeur désactivé.")
            }
        }
    }

    val shareAppMessage = atlasText(
        "Découvrez Atlas Commerce : https://stock.afmarbre.com/download",
        "Try Atlas Commerce: https://stock.afmarbre.com/download",
        "جرّب Atlas Commerce: https://stock.afmarbre.com/download",
    )
    val shareChooserTitle = atlasText("Partager Atlas Commerce", "Share Atlas Commerce", "مشاركة Atlas Commerce")
    val shareFailedMessage = atlasText("Impossible d'ouvrir le partage.", "Could not open sharing.", "تعذّر فتح المشاركة.")
    val shareApp: () -> Unit = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Atlas Commerce")
            putExtra(Intent.EXTRA_TEXT, shareAppMessage)
        }
        runCatching {
            context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
        }.onFailure {
            onToast(shareFailedMessage)
        }
    }

    if (showReleaseHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showReleaseHistoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(atlasText("Historique des versions"), modifier = Modifier.weight(1f))
                    if (releaseChangelogLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = appViewModel::refreshReleaseChangelog) {
                            Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser l'historique"))
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (releaseChangelog.isEmpty() && releaseChangelogLoading) {
                        AtlasSkeletonList(Modifier.fillMaxWidth(), rows = 3, avatarSize = 32.dp)
                    } else if (releaseChangelog.isEmpty()) {
                        Text(
                            atlasText("Aucune version publiée n'est disponible pour le moment."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    releaseChangelog.forEach { entry ->
                        AtlasCard(padding = 12.dp) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                if (entry.isLatest) AtlasChip(text = atlasText("Dernière"), tone = AtlasTone.POSITIVE)
                                entry.publishedAt?.let { AtlasChip(text = it, tone = AtlasTone.NEUTRAL) }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = localizeReleaseNotes(entry.notes, appLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReleaseHistoryDialog = false }) { Text(atlasText("Fermer")) }
            },
        )
    }

    if (confirmClearUpdatePackage) {
        val updatePackageClearedMessage = atlasText("Fichier de mise à jour supprimé.", "Downloaded update removed.", "تم حذف ملف التحديث.")
        val updatePackageBusyMessage = atlasText("Impossible de supprimer le fichier. Si un téléchargement est actif, attendez sa fin puis réessayez.", "Could not remove the downloaded update. If a download is active, wait for it to finish and try again.", "تعذّر حذف الملف. إذا كان هناك تنزيل نشط، انتظر حتى يكتمل ثم أعد المحاولة.")
        AlertDialog(
            onDismissRequest = { confirmClearUpdatePackage = false },
            title = { Text(atlasText("Supprimer le fichier téléchargé ?", "Delete downloaded update?", "حذف ملف التحديث المُنزّل؟")) },
            text = {
                Text(
                    atlasText(
                        "Cela supprime uniquement l'installateur conservé sur cet appareil, pas l'application actuelle. Atlas le supprime aussi automatiquement après une mise à jour réussie, au prochain démarrage.",
                        "This only removes the installer stored on this device, not the app you're using. Atlas also removes it automatically after a successful update, on the next launch.",
                        "سيؤدي هذا إلى حذف ملف التثبيت المحفوظ على هذا الجهاز فقط، وليس التطبيق الحالي. كما يحذفه Atlas تلقائياً بعد نجاح التحديث عند التشغيل التالي.",
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    appViewModel.clearCachedUpdateFiles { cleared ->
                        confirmClearUpdatePackage = false
                        onToast(if (cleared) updatePackageClearedMessage else updatePackageBusyMessage)
                    }
                }) {
                    Text(atlasText("Supprimer", "Delete", "حذف"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearUpdatePackage = false }) {
                    Text(atlasText("Annuler", "Cancel", "إلغاء"))
                }
            },
        )
    }

    if (showProblemReportDialog) {
        ProblemReportDialog(
            screenOrigin = atlasText("Paramètres", "Settings", "الإعدادات"),
            onDismiss = { showProblemReportDialog = false },
            onToast = onToast,
            appViewModel = appViewModel,
        )
    }

    LaunchedEffect(user.scopeIdentity, serverSelection?.baseUrl) {
        appViewModel.refreshCachedProductCount()
        appViewModel.refreshLocalStorageSummary()
        appViewModel.refreshReleaseChangelog()
    }
    LaunchedEffect(updateState) {
        if (updateState is UpdateState.ReadyToInstall) {
            appViewModel.refreshLocalStorageSummary()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .animateContentSize(spring(dampingRatio = 0.9f, stiffness = 420f))
            .padding(
                start = density.screenPadding,
                end = density.screenPadding,
                top = density.cardMargin,
                bottom = 32.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(density.itemGap),
    ) {
        // Updates, accelerated downloads, and release history
        AtlasEntrance(delayMillis = 90, offsetY = 8.dp) {
            SettingsUpdateCard(
                state = updateState,
                selectedSource = updateDownloadSource,
                mirrorPing = mirrorPing,
                onSelectSource = appViewModel::setUpdateDownloadSource,
                onPingMirrors = appViewModel::pingUpdateMirrors,
                onCheck = { appViewModel.checkForUpdate(manual = true) },
                onDownload = onStartUpdateDownload,
                onInstall = {
                    if (!appViewModel.installDownloadedUpdate(context)) {
                        onToast("Impossible d'ouvrir l'installateur sur cet appareil.")
                    }
                },
            )
        }

        AtlasCard {
            SettingsToggleRow(
                title = atlasText("Recherche automatique des mises à jour", "Check for updates automatically", "التحقق تلقائياً من التحديثات"),
                subtitle = atlasText(
                    "Vérifie à l'ouverture et environ toutes les 12 h en arrière-plan, selon les règles batterie d'Android.",
                    "Checks at launch and about every 12 hours in the background, subject to Android battery rules.",
                    "يتحقق عند التشغيل وكل 12 ساعة تقريباً في الخلفية، وفقاً لقيود بطارية Android.",
                ),
                checked = autoCheckUpdates,
                onCheckedChange = appViewModel::setAutoCheckUpdates,
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsToggleRow(
                title = atlasText("Téléchargement accéléré multi-connexions", "Multi-connection accelerated downloads", "تنزيلات سريعة متعددة الاتصالات"),
                subtitle = atlasText("Adapte le nombre de connexions au réseau (CDN : 6 sur mobile, 16 en Wi-Fi ; GitHub reste limité à 4).", "Adapts connections to the network (CDN: 6 on mobile, 16 on Wi-Fi; GitHub stays capped at 4).", "يكيّف عدد الاتصالات حسب الشبكة (CDN: 6 على بيانات الهاتف و16 على Wi-Fi؛ يظل GitHub محدوداً بـ4)."),
                checked = parallelUpdateDownload,
                onCheckedChange = appViewModel::setParallelUpdateDownload,
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsToggleRow(
                title = atlasText("Toujours suivre la dernière version publiée", "Always follow the latest published release", "اتباع أحدث إصدار منشور دائماً"),
                subtitle = atlasText("Considère toute version publiée en dernier sur GitHub Releases ou CDN 1 comme la version active.", "Treat the latest version published on GitHub Releases or CDN 1 as the active release.", "اعتبار أحدث إصدار منشور على GitHub Releases أو CDN 1 هو الإصدار النشط."),
                checked = followLatestRelease,
                onCheckedChange = appViewModel::setFollowLatestRelease,
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            val downloadedUpdateBytes = localStorageSummary.updateDownloadBytes
            AtlasOutlinedButton(
                text = if (downloadedUpdateBytes > 0L) {
                    atlasText("Supprimer le fichier de mise à jour · ${Formatter.formatShortFileSize(context, downloadedUpdateBytes)}", "Delete downloaded update · ${Formatter.formatShortFileSize(context, downloadedUpdateBytes)}", "حذف ملف التحديث · ${Formatter.formatShortFileSize(context, downloadedUpdateBytes)}")
                } else {
                    atlasText("Aucun fichier de mise à jour téléchargé", "No downloaded update file", "لا يوجد ملف تحديث مُنزّل")
                },
                onClick = { confirmClearUpdatePackage = true },
                enabled = downloadedUpdateBytes > 0L && updateState !is UpdateState.Downloading,
                icon = Icons.Outlined.DeleteOutline,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                atlasText(
                    "Atlas supprime automatiquement l'installateur après l'installation réussie de la mise à jour et le prochain démarrage. Vous pouvez aussi le supprimer ici.",
                    "Atlas automatically removes the installer after the update installs successfully and Atlas is opened again. You can also remove it here.",
                    "يحذف Atlas ملف التثبيت تلقائياً بعد تثبيت التحديث بنجاح وفتح Atlas مجدداً. ويمكنك حذفه من هنا أيضاً.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f).clickable { showReleaseHistoryDialog = true }) {
                    Text(
                        text = atlasText("Nouveautés des versions"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = atlasText("Ouvrir l'historique complet des changements publiés"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showReleaseHistoryDialog = true }) {
                    Text(atlasText("Voir"))
                }
                IconButton(
                    onClick = appViewModel::refreshReleaseChangelog,
                    enabled = !releaseChangelogLoading,
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser l'historique des versions"))
                }
            }
        }

        // 4. Active Server & Saved Servers Management
        AtlasCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = atlasText("Serveur & espaces enregistrés"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = serverSelection?.baseUrl?.let { ServerUrl.display(it) } ?: atlasText("Aucun serveur configuré"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = atlasText("Chaque serveur dispose de son propre stockage chiffré isolé. Changer de serveur ferme automatiquement la session en cours."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val otherServers = savedServers.filterNot {
                it.url.equals(serverSelection?.baseUrl, ignoreCase = true)
            }
            if (otherServers.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    otherServers.forEach { entry ->
                        AtlasCard(
                            onClick = { appViewModel.switchSavedServer(entry, onToast) },
                            padding = 10.dp,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = entry.label.ifBlank { ServerUrl.display(entry.url) },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = ServerUrl.display(entry.url),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = { appViewModel.forgetSavedServer(entry, onToast) },
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = atlasText("Supprimer ce serveur"),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            AtlasOutlinedButton(
                text = atlasText("Changer ou ajouter un serveur"),
                onClick = onChangeServer,
                icon = Icons.Outlined.SwapHoriz,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Updates and optional diagnostics stay in Settings; local data management lives in Profile.
        AtlasCard {
            when {
                !crashReportsAvailabilityChecked -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            atlasText("Vérification de Crashlytics…", "Checking Crashlytics…", "جارٍ التحقق من Crashlytics…"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                automaticCrashReportsAvailable -> {
                    val crashSettingFailureMessage = atlasText(
                        "Impossible de modifier le réglage Crashlytics.",
                        "Could not update the Crashlytics setting.",
                        "تعذر تحديث إعداد Crashlytics.",
                    )
                    val crashSettingEnabledMessage = atlasText(
                        "Rapports de plantage activés.",
                        "Crash reports enabled.",
                        "تم تفعيل تقارير الأعطال.",
                    )
                    val crashSettingDisabledMessage = atlasText(
                        "Envoi automatique désactivé.",
                        "Automatic reporting disabled.",
                        "تم تعطيل الإرسال التلقائي.",
                    )
                    SettingsToggleRow(
                        title = atlasText("Rapports de plantage automatiques (Firebase)", "Automatic crash reports (Firebase)", "تقارير الأعطال التلقائية (Firebase)"),
                        subtitle = if (automaticCrashReportsEnabled) {
                            atlasText(
                                "Firebase Crashlytics est configuré. L'envoi automatique est activé selon votre choix; aucune donnée Analytics n'est collectée.",
                                "Firebase Crashlytics is configured. Automatic reporting is enabled by your choice; no Analytics data is collected.",
                                "تم إعداد Firebase Crashlytics. الإرسال التلقائي مفعّل باختيارك، ولا يتم جمع بيانات Analytics.",
                            )
                        } else {
                            atlasText(
                                "Firebase Crashlytics est configuré et prêt. Les rapports restent désactivés jusqu'à votre accord; aucune donnée Analytics n'est collectée.",
                                "Firebase Crashlytics is configured and ready. Reports stay off until you opt in; no Analytics data is collected.",
                                "تم إعداد Firebase Crashlytics وهو جاهز. تظل التقارير متوقفة حتى توافق؛ ولا يتم جمع بيانات Analytics.",
                            )
                        },
                        checked = automaticCrashReportsEnabled,
                        onCheckedChange = { enabled ->
                            appViewModel.setAutomaticCrashReportsEnabled(enabled) { succeeded ->
                                onToast(
                                    when {
                                        !succeeded -> crashSettingFailureMessage
                                        enabled -> crashSettingEnabledMessage
                                        else -> crashSettingDisabledMessage
                                    },
                                )
                            }
                        },
                    )
                }
                else -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    atlasText("Rapports de plantage automatiques", "Automatic crash reports", "تقارير الأعطال التلقائية"),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    atlasText(
                                        "Indisponibles dans cette version : Firebase Crashlytics n'est pas configuré. Cette installation ne peut pas envoyer de rapports.",
                                        "Unavailable in this build: Firebase Crashlytics is not configured. This installation cannot send reports.",
                                        "غير متاحة في هذا الإصدار: لم يتم إعداد Firebase Crashlytics. لا يمكن لهذا التثبيت إرسال التقارير.",
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsRow(
                icon = Icons.Outlined.AutoAwesomeMotion,
                title = atlasText("Revoir l'écran de bienvenue", "Replay the welcome screen", "إعادة عرض شاشة الترحيب"),
                subtitle = atlasText("Relancer la présentation animée d'Atlas Commerce", "Replay the Atlas Commerce introduction", "إعادة عرض مقدمة Atlas Commerce"),
                onClick = { appViewModel.replayOnboarding() },
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsRow(
                icon = Icons.Outlined.Share,
                title = atlasText("Partager Atlas Commerce", "Share Atlas Commerce", "مشاركة Atlas Commerce"),
                subtitle = atlasText(
                    "Invitez vos amis à télécharger l'application.",
                    "Invite your friends to download the app.",
                    "ادعُ أصدقاءك لتنزيل التطبيق.",
                ),
                onClick = shareApp,
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
        }

        AnimatedVisibility(
            visible = developerModeEnabled,
            enter = if (animationsEnabled) fadeIn() + expandVertically() else EnterTransition.None,
            exit = if (animationsEnabled) fadeOut() + shrinkVertically() else ExitTransition.None,
        ) {
            AtlasCard(padding = 4.dp) {
                SettingsRow(
                    icon = Icons.Outlined.BugReport,
                    title = atlasText("Outils développeur", "Developer tools", "أدوات المطوّر"),
                    subtitle = atlasText("État local résumé · journaux masqués", "Summary status · logs hidden", "حالة موجزة · السجلات مخفية"),
                    onClick = onOpenDeveloperTools,
                )
            }
        }

        AtlasCard(padding = 4.dp) {
            SettingsRow(
                icon = Icons.Outlined.SportsEsports,
                title = atlasText("Snake Atlas", "Atlas Snake", "ثعبان Atlas"),
                subtitle = atlasText("Un jeu disponible hors ligne", "A game you can play offline", "لعبة متاحة دون اتصال"),
                onClick = onOpenSnake,
            )
        }

        // 6. Security, License & About
        AtlasCard(padding = 4.dp) {
            SettingsRow(
                icon = Icons.Outlined.Security,
                title = atlasText("Sécurité & confidentialité", "Security & privacy", "الأمان والخصوصية"),
                subtitle = atlasText("Isolation multi-comptes, sessions, chiffrement", "Account isolation, sessions, encryption", "عزل الحسابات والجلسات والتشفير"),
                onClick = onOpenSecurity,
            )
            AtlasDivider(Modifier.padding(horizontal = 12.dp))
            SettingsRow(
                icon = Icons.Outlined.Description,
                title = atlasText("Licence MIT", "MIT license", "ترخيص MIT"),
                subtitle = atlasText("Sources et composants tiers", "Sources and third-party components", "المصادر والمكونات الخارجية"),
                onClick = onOpenLicense,
            )
            AtlasDivider(Modifier.padding(horizontal = 12.dp))
            SettingsRow(
                icon = Icons.Outlined.Info,
                title = atlasText("À propos", "About", "حول التطبيق"),
                subtitle = "${AppDetails.NAME} ${AppDetails.VERSION_NAME}",
                onClick = onOpenAbout,
            )
        }

        // 7. Author Identity (@Etherealmber rounded GitHub PFP + MIT) & Open Source Links
        EtherealmberAuthorCard(onToast = onToast)

        AtlasCard {
            Text(atlasText("Liens du projet"), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            SettingsRow(
                icon = Icons.Outlined.Person,
                title = atlasText(
                    "Profil GitHub @${AppDetails.GITHUB_HANDLE}",
                    "GitHub profile @${AppDetails.GITHUB_HANDLE}",
                    "ملف GitHub @${AppDetails.GITHUB_HANDLE}",
                ),
                subtitle = "github.com/${AppDetails.GITHUB_HANDLE}",
                onClick = {
                    if (!openExternal(context, AppDetails.GITHUB_PROFILE_URL)) {
                        onToast("Aucun navigateur disponible.")
                    }
                },
            )
            SettingsRow(
                icon = Icons.Outlined.Language,
                title = "Projet web Atlas Commerce",
                subtitle = "Etherealmber/atlas-commerce",
                onClick = {
                    if (!openExternal(context, AppDetails.WEB_REPO)) {
                        onToast("Aucun navigateur disponible.")
                    }
                },
            )
            SettingsRow(
                icon = Icons.Outlined.OpenInNew,
                title = "Code source Android",
                subtitle = "Etherealmber/atlas-commerce-android",
                onClick = {
                    if (!openExternal(context, AppDetails.PROJECT_REPO)) {
                        onToast("Aucun navigateur disponible.")
                    }
                },
            )
            SettingsRow(
                icon = Icons.Outlined.BugReport,
                title = atlasText("Signaler un problème", "Report a problem", "الإبلاغ عن مشكلة"),
                subtitle = atlasText(
                    "Un résumé technique privé sera joint uniquement si vous envoyez le rapport. Les captures (3 max) se partagent séparément et ne sont pas envoyées avec le formulaire.",
                    "A private technical summary is attached only if you send the report. Screenshots (up to 3) are shared separately, not sent with the form.",
                    "لن يُرفق ملخص تقني خاص إلا عند إرسال البلاغ. تُشارك لقطات الشاشة (حتى 3) بشكل منفصل ولا تُرسل مع النموذج.",
                ),
                onClick = { showProblemReportDialog = true },
            )
        }

        // Keep the accidental shake shortcut well below the frequently used settings.
        AtlasCard {
            SettingsToggleRow(
                title = atlasText("Signaler un problème en secouant", "Shake to report a problem", "هز الهاتف للإبلاغ عن مشكلة"),
                subtitle = atlasText(
                    "Affiche une confirmation uniquement. Aucun rapport ni diagnostic n'est envoyé sans votre accord.",
                    "Only opens a confirmation. No report or diagnostics are sent without your approval.",
                    "يعرض طلب تأكيد فقط. لا يُرسل أي تقرير أو بيانات تشخيصية دون موافقتك.",
                ),
                checked = shakeToReportEnabled,
                onCheckedChange = appViewModel::setShakeToReportEnabled,
            )
        }

        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                text = atlasText(
                    "${AppDetails.NAME} ${AppDetails.VERSION_NAME} (build ${AppDetails.VERSION_CODE})",
                    "${AppDetails.NAME} ${AppDetails.VERSION_NAME} (build ${AppDetails.VERSION_CODE})",
                    "${AppDetails.NAME} ${AppDetails.VERSION_NAME} (البنية ${AppDetails.VERSION_CODE})",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onBuildVersionTap)
                    .padding(vertical = 8.dp),
            )
            Text(
                text = atlasText(
                    "© ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER} · Licence ${AppDetails.LICENSE_NAME}",
                    "© ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER} · License ${AppDetails.LICENSE_NAME}",
                    "© ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER} · الترخيص ${AppDetails.LICENSE_NAME}",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun profileRoleLabel(role: String): String = when (Roles.normalize(role)) {
    Roles.OWNER -> atlasText("Compte principal", "Primary account", "الحساب الرئيسي")
    Roles.ADMIN -> atlasText("Administrateur", "Administrator", "مدير")
    Roles.SECRETARY -> atlasText("Secrétariat", "Sales secretary", "السكرتارية")
    Roles.STOCK -> atlasText("Stock", "Stock", "المخزون")
    else -> atlasText("Opérateur", "Operator", "مشغّل")
}

@Composable
private fun settingsTabLabel(tab: MainTab): String = when (tab) {
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

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = 0.9f, stiffness = 520f))
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = atlasText(title),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = atlasText(subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(atlasText(title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = atlasText(subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
