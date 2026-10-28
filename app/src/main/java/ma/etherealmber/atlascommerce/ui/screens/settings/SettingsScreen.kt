package ma.etherealmber.atlascommerce.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.TrustedDeviceDto
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasInfoRow
import ma.etherealmber.atlascommerce.ui.components.AtlasOutlinedButton
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.SettingsUpdateCard
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.root.AppViewModel
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentQuotaCard
import ma.etherealmber.atlascommerce.ui.screens.legal.EtherealmberAuthorCard
import ma.etherealmber.atlascommerce.ui.screens.legal.ProblemReportDialog
import ma.etherealmber.atlascommerce.ui.shell.MainTab
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import ma.etherealmber.atlascommerce.ui.util.openExternal

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
    onSignOut: () -> Unit,
    onToast: (String) -> Unit,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val density = Atlas.density
    val scrollState = rememberScrollState()

    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
    val appLanguage by appViewModel.appLanguage.collectAsStateWithLifecycle()
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
    var showEditProfile by remember { mutableStateOf(false) }
    var draftDisplayName by remember(user.displayName) { mutableStateOf(user.displayName) }
    var draftEmail by remember(user.email) { mutableStateOf(user.email) }
    var draftClientName by remember(savedClientName) { mutableStateOf(savedClientName) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            appViewModel.uploadAvatarFromUri(context, uri, onToast)
        }
    }

    val otherSavedAccounts = savedAccounts.filterNot {
        it.username.equals(user.username, ignoreCase = true)
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
                        modifier = Modifier
                            .size(68.dp)
                            .clickable(enabled = !uploadingAvatar) {
                                photoPicker.launch("image/*")
                            },
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
                            onClick = {
                                if (!uploadingAvatar) {
                                    photoPicker.launch("image/*")
                                }
                            },
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
                            text = atlasText(
                                "@${user.username} · Appuyez sur la photo pour modifier",
                                "@${user.username} · Tap the photo to edit",
                                "@${user.username} · اضغط على الصورة للتعديل",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            draftDisplayName = user.displayName
                            draftEmail = user.email
                            showEditProfile = true
                        },
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = atlasText("Modifier le profil", "Edit profile", "تعديل الملف الشخصي"))
                    }
                }
                if (user.email.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    AtlasInfoRow(label = atlasText("E-mail", "Email", "البريد الإلكتروني"), value = user.email)
                }
                Spacer(Modifier.height(10.dp))
                AtlasOutlinedButton(
                    text = atlasText("Se déconnecter de cette session", "Sign out of this session", "تسجيل الخروج من هذه الجلسة"),
                    onClick = { confirmSignOut = true },
                    icon = Icons.Outlined.Logout,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Quick-switch between other accounts saved specifically on THIS server (100% isolated)
        if (otherSavedAccounts.isNotEmpty()) {
            AtlasCard {
                Text(
                    text = atlasText("Autres comptes mémorisés sur ce serveur", "Other accounts saved on this server", "حسابات أخرى محفوظة على هذا الخادم"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = atlasText(
                        "Bascule instantanée — les paniers, caches et discussions restent strictement séparés.",
                        "Switch instantly — carts, caches, and conversations stay separate.",
                        "التبديل فوري — تبقى السلال وذاكرات التخزين والمحادثات منفصلة.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    otherSavedAccounts.forEach { acc ->
                        AtlasCard(
                            onClick = { appViewModel.switchSavedAccount(acc, onToast) },
                            padding = 10.dp,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AtlasAvatar(
                                    avatarUrl = acc.avatarUrl,
                                    label = acc.displayName.ifBlank { acc.username },
                                    size = 36.dp,
                                    serverBaseUrl = serverSelection?.baseUrl,
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = acc.displayName.ifBlank { acc.username },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = atlasText(
                                "@${acc.username} · ${profileRoleLabel(acc.role)}",
                                "@${acc.username} · ${profileRoleLabel(acc.role)}",
                                "@${acc.username} · ${profileRoleLabel(acc.role)}",
                            ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = { appViewModel.forgetSavedAccount(acc, onToast) },
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = atlasText("Oublier ce compte"),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
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
            Spacer(Modifier.height(10.dp))
            Text(
                text = atlasText("Thème"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { appViewModel.setThemeMode(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> atlasText("Système", "System", "النظام")
                                    ThemeMode.LIGHT -> atlasText("Clair", "Light", "فاتح")
                                    ThemeMode.DARK -> atlasText("Sombre", "Dark", "داكن")
                                },
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = atlasText("Langue", "Language", "اللغة"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = appLanguage == language,
                        onClick = { appViewModel.setAppLanguage(language) },
                        label = { Text(language.nativeLabel) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = atlasText("Densité d'affichage"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DensityMode.entries.forEach { mode ->
                    FilterChip(
                        selected = densityMode == mode,
                        onClick = { appViewModel.setDensityMode(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    DensityMode.COMPACT -> atlasText("Compact", "Compact", "مضغوط")
                                    DensityMode.COMFORT -> atlasText("Confort", "Comfort", "مريح")
                                },
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = atlasText("Onglet d'ouverture au lancement"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MainTab.bottomTabsForRole(user.role).forEach { item ->
                    FilterChip(
                        selected = defaultStartTab == item.name,
                        onClick = { appViewModel.setDefaultStartTab(item.name) },
                        label = { Text(settingsTabLabel(item)) },
                    )
                }
            }
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

    if (showEditProfile) {
        AlertDialog(
            onDismissRequest = { if (!savingProfile) showEditProfile = false },
            title = { Text(atlasText("Modifier mon profil")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtlasTextField(
                        value = draftDisplayName,
                        onValueChange = { draftDisplayName = it },
                        label = atlasText("Nom affiché *"),
                        placeholder = atlasText("Ex. Nom complet"),
                        imeAction = ImeAction.Next,
                    )
                    AtlasTextField(
                        value = draftEmail,
                        onValueChange = { draftEmail = it },
                        label = atlasText("Adresse e-mail"),
                        placeholder = "contact@exemple.com",
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    )
                }
            },
            confirmButton = {
                AtlasButton(
                    text = "Enregistrer",
                    loading = savingProfile,
                    onClick = {
                        appViewModel.updateProfileDetails(draftDisplayName, draftEmail) { msg ->
                            onToast(msg)
                            showEditProfile = false
                        }
                    },
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditProfile = false },
                    enabled = !savingProfile,
                ) {
                    Text(atlasText("Annuler"))
                }
            },
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(atlasText("Se déconnecter ?")) },
            text = {
                Text(
                    atlasText(
                        "La session active, le panier et les données temporaires seront effacés. Aucune donnée métier n'est supprimée sur le serveur.",
                        "The active session, cart and temporary data will be cleared. No business data will be deleted from the server.",
                        "سيتم مسح الجلسة النشطة والسلة والبيانات المؤقتة. لن تُحذف أي بيانات تجارية من الخادم.",
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSignOut = false
                        onSignOut()
                    },
                ) { Text(atlasText("Se déconnecter")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text(atlasText("Annuler")) }
            },
        )
    }
}

/**
 * Dedicated **System & Security Settings** screen (`MainTab.SETTINGS`), opened via the top-right
 * Settings button inside [ProfileScreen].
 *
 * Includes QR PC pairing & connected devices, live Atlas Agent IA quota bars & reset countdown,
 * saved servers management, OTA APK updates, isolated offline cache, and legal information.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    user: SessionUser,
    onChangeServer: () -> Unit,
    onOpenQrDevice: () -> Unit = {},
    onOpenLicense: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAbout: () -> Unit,
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
    val parallelUpdateDownload by appViewModel.parallelUpdateDownload.collectAsStateWithLifecycle()
    val followLatestRelease by appViewModel.followLatestRelease.collectAsStateWithLifecycle()
    val serverSelection by appViewModel.server.collectAsStateWithLifecycle()
    val savedServers by appViewModel.savedServers.collectAsStateWithLifecycle()
    val agentPolicy by appViewModel.agentPolicy.collectAsStateWithLifecycle()
    val updateState by appViewModel.updateState.collectAsStateWithLifecycle()
    val updateDownloadSource by appViewModel.updateDownloadSource.collectAsStateWithLifecycle()
    val appLanguage by appViewModel.appLanguage.collectAsStateWithLifecycle()
    val shakeToReportEnabled by appViewModel.shakeToReportEnabled.collectAsStateWithLifecycle()
    val mirrorPing by appViewModel.mirrorPing.collectAsStateWithLifecycle()
    val releaseChangelog by appViewModel.releaseChangelog.collectAsStateWithLifecycle()
    val releaseChangelogLoading by appViewModel.releaseChangelogLoading.collectAsStateWithLifecycle()
    val devices by appViewModel.devices.collectAsStateWithLifecycle()
    val devicesLoading by appViewModel.devicesLoading.collectAsStateWithLifecycle()
    val cachedProductCount by appViewModel.cachedProductCount.collectAsStateWithLifecycle()
    val animationsEnabled by appViewModel.animationsEnabled.collectAsStateWithLifecycle()
    val developerModeEnabled by appViewModel.developerModeEnabled.collectAsStateWithLifecycle()
    val automaticCrashReportsEnabled by appViewModel.automaticCrashReportsEnabled.collectAsStateWithLifecycle()
    val automaticCrashReportsAvailable by appViewModel.automaticCrashReportsAvailable.collectAsStateWithLifecycle()
    val crashReportsUnavailableToast = atlasText(
        "Crashlytics n'est pas configuré pour cette version.",
        "Crashlytics is not configured for this build.",
        "لم يتم إعداد Crashlytics لهذا الإصدار.",
    )
    val crashReportsEnabledToast = atlasText(
        "Rapports de plantage activés.",
        "Crash reports enabled.",
        "تم تفعيل تقارير الأعطال.",
    )
    val crashReportsDisabledToast = atlasText(
        "Envoi automatique désactivé.",
        "Automatic reporting disabled.",
        "تم تعطيل الإرسال التلقائي.",
    )
    val androidPasskeysAvailable by appViewModel.androidPasskeysAvailable.collectAsStateWithLifecycle()
    val registeringPasskey by appViewModel.registeringPasskey.collectAsStateWithLifecycle()
    var showProblemReportDialog by remember { mutableStateOf(false) }
    var showReleaseHistoryDialog by remember { mutableStateOf(false) }
    var showClearReportsDialog by remember { mutableStateOf(false) }
    var showClearLocalDataDialog by remember { mutableStateOf(false) }
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

    if (showClearReportsDialog) {
        AlertDialog(
            onDismissRequest = { showClearReportsDialog = false },
            title = { Text(atlasText("Supprimer les PDF temporaires ?")) },
            text = { Text(atlasText("Les rapports téléchargés dans le cache privé de cette application seront supprimés de cet appareil. Les copies déjà enregistrées ou partagées ailleurs ne seront pas touchées.")) },
            confirmButton = {
                TextButton(onClick = {
                    showClearReportsDialog = false
                    appViewModel.clearFinanceReportCache(onToast)
                }) { Text(atlasText("Supprimer")) }
            },
            dismissButton = { TextButton(onClick = { showClearReportsDialog = false }) { Text(atlasText("Annuler")) } },
        )
    }

    if (showClearLocalDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearLocalDataDialog = false },
            title = { Text(atlasText("Effacer les données locales ?")) },
            text = {
                Text(atlasText("Cette action déconnecte ce compte et supprime les sessions mémorisées, serveurs enregistrés, préférences, cache hors ligne, journaux et PDF temporaires de cet appareil. Elle ne supprime aucune vente, facture ni donnée d'entreprise sur le serveur."))
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearLocalDataDialog = false
                    appViewModel.clearAllLocalAppData(onToast)
                }) { Text(atlasText("Effacer de cet appareil")) }
            },
            dismissButton = { TextButton(onClick = { showClearLocalDataDialog = false }) { Text(atlasText("Annuler")) } },
        )
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
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
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
                                text = entry.notes,
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

    if (showProblemReportDialog) {
        ProblemReportDialog(
            screenOrigin = atlasText("Paramètres", "Settings", "الإعدادات"),
            onDismiss = { showProblemReportDialog = false },
            onToast = onToast,
            appViewModel = appViewModel,
        )
    }

    LaunchedEffect(user.scopeIdentity, serverSelection?.baseUrl) {
        appViewModel.refreshPasskeyCapability()
        appViewModel.loadDevices()
        appViewModel.refreshAgentPolicy()
        appViewModel.refreshCachedProductCount()
        appViewModel.refreshReleaseChangelog()
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
        AtlasCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(atlasText("Langue de l'application", "App language", "لغة التطبيق"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = appLanguage == language,
                        onClick = { appViewModel.setAppLanguage(language) },
                        label = { Text(language.nativeLabel) },
                    )
                }
            }
        }
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

        // 1. Connected Devices & QR PC Pairing
        AtlasCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Devices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = atlasText("Appareils connectés & QR PC (${devices.size})", "Connected devices & PC QR (${devices.size})", "الأجهزة المتصلة وQR الكمبيوتر (${devices.size})"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = atlasText("Autorisez un ordinateur par QR ou révoquez une session à distance.", "Approve a computer with QR or revoke a session remotely.", "وافق على كمبيوتر عبر QR أو ألغِ جلسة عن بُعد."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = appViewModel::loadDevices, enabled = !devicesLoading) {
                    Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser les appareils", "Refresh devices", "تحديث الأجهزة"))
                }
            }
            Spacer(Modifier.height(10.dp))
            AtlasButton(
                text = atlasText("Scanner un poste PC (QR)", "Scan a PC sign-in QR", "امسح رمز تسجيل الدخول للكمبيوتر"),
                onClick = onOpenQrDevice,
                icon = Icons.Outlined.QrCode2,
                modifier = Modifier.fillMaxWidth(),
            )
            if (devices.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                devices.forEachIndexed { idx, device ->
                    if (idx > 0) AtlasDivider(Modifier.padding(vertical = 6.dp))
                    ConnectedDeviceRow(
                        device = device,
                        onRevoke = { appViewModel.revokeDevice(device, onToast) },
                    )
                }
            }
        }

        // 2. Live Atlas Agent IA Quota Bars & Limit Countdown
        AtlasEntrance(delayMillis = 45, offsetY = 8.dp) {
            AgentQuotaCard(
                policy = agentPolicy,
                onRefresh = appViewModel::refreshAgentPolicy,
            )
        }

        // 3. Updates, Fast Parallel Download, Latest Release Tracking & Changelog
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
                title = atlasText("Téléchargement accéléré multi-connexions", "Multi-connection accelerated downloads", "تنزيلات سريعة متعددة الاتصالات"),
                subtitle = atlasText("Télécharge les mises à jour via 4 connexions HTTP Range parallèles haute vitesse.", "Downloads updates using four high-speed parallel HTTP Range connections.", "تنزيل التحديثات عبر أربعة اتصالات HTTP Range متوازية وعالية السرعة."),
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

        // 5. Offline Cache & System Options
        AtlasCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = atlasText("Catalogue hors ligne & système"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = atlasText(
                            "$cachedProductCount article(s) isolé(s) pour @${user.username}",
                            "$cachedProductCount cached item(s) for @${user.username}",
                            "$cachedProductCount عنصر محفوظ لـ @${user.username}",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AtlasOutlinedButton(
                    text = atlasText("Vider"),
                    onClick = { appViewModel.clearOfflineCache(onToast) },
                    icon = Icons.Outlined.DeleteOutline,
                    enabled = cachedProductCount > 0,
                )
            }
            AtlasDivider(Modifier.padding(vertical = 8.dp))
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
            if (automaticCrashReportsAvailable) {
                SettingsToggleRow(
                    title = atlasText("Rapports de plantage automatiques (Firebase)", "Automatic crash reports (Firebase)", "تقارير الأعطال التلقائية (Firebase)"),
                    subtitle = atlasText(
                        "Option désactivée par défaut. Les rapports sont envoyés à Firebase Crashlytics, pas à Atlas Bot.",
                        "Off by default. Crash reports go to Firebase Crashlytics, not Atlas Bot.",
                        "معطلة افتراضياً. تُرسل تقارير الأعطال إلى Firebase Crashlytics وليس Atlas Bot.",
                    ),
                    checked = automaticCrashReportsEnabled,
                    onCheckedChange = { enabled ->
                        appViewModel.setAutomaticCrashReportsEnabled(enabled) { succeeded ->
                            onToast(
                                when {
                                    !succeeded -> crashReportsUnavailableToast
                                    enabled -> crashReportsEnabledToast
                                    else -> crashReportsDisabledToast
                                },
                            )
                        }
                    },
                )
                AtlasDivider(Modifier.padding(vertical = 8.dp))
            }
            SettingsRow(
                icon = Icons.Outlined.AutoAwesomeMotion,
                title = atlasText("Revoir l'écran de bienvenue", "Replay the welcome screen", "إعادة عرض شاشة الترحيب"),
                subtitle = atlasText("Relancer la présentation animée d'Atlas Commerce", "Replay the Atlas Commerce introduction", "إعادة عرض مقدمة Atlas Commerce"),
                onClick = { appViewModel.replayOnboarding() },
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsRow(
                icon = Icons.Outlined.DeleteOutline,
                title = atlasText("Supprimer les PDF temporaires", "Delete temporary PDFs", "حذف ملفات PDF المؤقتة"),
                subtitle = atlasText("Les rapports privés sont purgés automatiquement après 7 jours.", "Private reports are automatically deleted after 7 days.", "تُحذف التقارير الخاصة تلقائياً بعد 7 أيام."),
                onClick = { showClearReportsDialog = true },
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsRow(
                icon = Icons.Outlined.DeleteOutline,
                title = atlasText("Effacer toutes les données locales", "Clear all local data", "مسح جميع البيانات المحلية"),
                subtitle = atlasText("Déconnecte le compte et efface les données stockées sur cet appareil uniquement.", "Signs out and clears data stored on this device only.", "تسجيل الخروج ومسح البيانات المخزنة على هذا الجهاز فقط."),
                onClick = { showClearLocalDataDialog = true },
            )
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

        AtlasEntrance(delayMillis = 30, offsetY = 8.dp) {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(atlasText("Passkey Android"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (androidPasskeysAvailable) {
                                atlasText(
                                    "Le serveur les autorise. La création exige aussi un gestionnaire Android compatible et un verrouillage d'écran.",
                                    "The server allows passkeys. Creating one also requires a compatible Android credential manager and a screen lock.",
                                    "يسمح الخادم بمفاتيح المرور. يتطلب إنشاؤها أيضاً مدير بيانات اعتماد متوافقاً مع Android وقفل شاشة.",
                                )
                            } else {
                                atlasText(
                                    "Configuration Android non confirmée par le serveur sélectionné. L'inscription reste masquée tant que le serveur ne l'annonce pas comme disponible.",
                                    "Android support has not been confirmed by the selected server. Registration stays hidden until the server reports it as available.",
                                    "لم يؤكد الخادم المحدد دعم Android. يظل التسجيل مخفياً حتى يعلن الخادم توفره.",
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (androidPasskeysAvailable) {
                    AtlasOutlinedButton(
                        text = if (registeringPasskey) atlasText("Enregistrement en cours…") else atlasText("Enregistrer une passkey", "Register passkey", "تسجيل مفتاح مرور"),
                        onClick = { appViewModel.registerAndroidPasskey(context) { } },
                        icon = Icons.Outlined.Fingerprint,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !registeringPasskey,
                    )
                    TextButton(
                        onClick = {
                            val credentialSettings = Intent("android.settings.PASSWORDS_AND_ACCOUNTS_SETTINGS")
                            val securitySettings = Intent(Settings.ACTION_SECURITY_SETTINGS)
                            val openedCredentialSettings = runCatching { context.startActivity(credentialSettings) }.isSuccess
                            if (!openedCredentialSettings) {
                                runCatching { context.startActivity(securitySettings) }
                                    .onFailure { onToast("Ouvrez Paramètres Android > Mots de passe et clés d'accès, puis choisissez un fournisseur.") }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(atlasText("Configurer le fournisseur Android"))
                    }
                } else {
                    TextButton(
                        onClick = appViewModel::refreshPasskeyCapability,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(atlasText("Vérifier à nouveau"))
                    }
                }
            }
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
private fun ConnectedDeviceRow(
    device: TrustedDeviceDto,
    onRevoke: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = device.friendlyLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (device.current) {
                    Spacer(Modifier.width(6.dp))
                    AtlasChip(text = atlasText("Cet appareil", "This device", "هذا الجهاز"), tone = AtlasTone.POSITIVE)
                }
            }
            val meta = listOfNotNull(
                device.lastIp?.takeIf { it.isNotBlank() },
                device.lastSeen?.takeIf { it.isNotBlank() }?.take(16)?.replace('T', ' '),
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onRevoke) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = atlasText("Révoquer cet appareil", "Revoke this device", "إلغاء صلاحية هذا الجهاز"),
                tint = MaterialTheme.colorScheme.error,
            )
        }
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
