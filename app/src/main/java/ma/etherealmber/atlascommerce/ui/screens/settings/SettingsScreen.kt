package ma.etherealmber.atlascommerce.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
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
import ma.etherealmber.atlascommerce.core.AppDetails
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
                            label = user.displayName,
                            size = 64.dp,
                            serverBaseUrl = serverSelection?.baseUrl,
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
                                        contentDescription = "Changer la photo de profil",
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
                            text = user.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "@${user.username} · Appuyez sur la photo pour modifier",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        AtlasChip(text = Roles.label(user.role), tone = AtlasTone.INFO)
                    }
                    IconButton(
                        onClick = {
                            draftDisplayName = user.displayName
                            draftEmail = user.email
                            showEditProfile = true
                        },
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Modifier le profil")
                    }
                }
                if (user.email.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    AtlasInfoRow(label = "E-mail", value = user.email)
                }
                Spacer(Modifier.height(10.dp))
                AtlasOutlinedButton(
                    text = "Se déconnecter de cette session",
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
                    text = "Autres comptes mémorisés sur ce serveur",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Bascule instantanée — les paniers, caches et discussions restent strictement séparés.",
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
                                        text = "@${acc.username} · ${Roles.label(acc.role)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = { appViewModel.forgetSavedAccount(acc, onToast) },
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Oublier ce compte",
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
                    text = "Apparence, navigation & animations",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Thème",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { appViewModel.setThemeMode(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Densité d'affichage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DensityMode.entries.forEach { mode ->
                    FilterChip(
                        selected = densityMode == mode,
                        onClick = { appViewModel.setDensityMode(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Onglet d'ouverture au lancement",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MainTab.BottomBarTabs.forEach { item ->
                    FilterChip(
                        selected = defaultStartTab == item.name,
                        onClick = { appViewModel.setDefaultStartTab(item.name) },
                        label = { Text(item.label) },
                    )
                }
            }
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsToggleRow(
                title = "Animations fluides",
                subtitle = "Transitions entre les pages, apparitions d'accueil et effets de mouvement.",
                checked = animationsEnabled,
                onCheckedChange = appViewModel::setAnimationsEnabled,
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsToggleRow(
                title = "Notifications système par rôle",
                subtitle = "Affiche les notifications Android filtrées selon votre rôle (${Roles.label(user.role)}).",
                checked = roleNotificationsEnabled,
                onCheckedChange = appViewModel::setRoleNotificationsEnabled,
            )
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
                    text = "Caméra & lecteur code-barres",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            SettingsToggleRow(
                title = "Ouvrir la caméra automatiquement",
                subtitle = "Désactivé par défaut — la caméra reste fermée en entrant dans Scan ou QR PC.",
                checked = autoOpenCamera,
                onCheckedChange = appViewModel::setAutoOpenCamera,
            )
            AtlasDivider(Modifier.padding(vertical = 6.dp))
            SettingsToggleRow(
                title = "Vibration au scan",
                subtitle = "Retour haptique dès qu'un code-barres ou QR est détecté.",
                checked = scanHapticEnabled,
                onCheckedChange = appViewModel::setScanHapticEnabled,
            )
            AtlasDivider(Modifier.padding(vertical = 6.dp))
            SettingsToggleRow(
                title = "Signal sonore au scan",
                subtitle = "Bip court de confirmation lors de la lecture d'un code.",
                checked = scanSoundEnabled,
                onCheckedChange = appViewModel::setScanSoundEnabled,
            )
            AtlasDivider(Modifier.padding(vertical = 6.dp))
            SettingsToggleRow(
                title = "Ajout automatique au panier",
                subtitle = "Ajoute immédiatement 1 unité au ticket VR lorsqu'un article est scanné.",
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
                        text = "Vente rapide & caisse",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AtlasTextField(
                        value = draftClientName,
                        onValueChange = { draftClientName = it },
                        label = "Client pré-rempli par défaut",
                        placeholder = "Ex. Client comptoir",
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
                        text = "OK",
                        onClick = {
                            appViewModel.setDefaultClientName(draftClientName)
                            onToast("Client par défaut enregistré.")
                        },
                    )
                }
                AtlasDivider(Modifier.padding(vertical = 8.dp))
                SettingsToggleRow(
                    title = "Confirmer avant création du ticket",
                    subtitle = "Affiche un récapitulatif du montant avant d'envoyer la vente au serveur.",
                    checked = confirmBeforeSale,
                    onCheckedChange = appViewModel::setConfirmBeforeSale,
                )
            }
        }
    }

    if (showEditProfile) {
        AlertDialog(
            onDismissRequest = { if (!savingProfile) showEditProfile = false },
            title = { Text("Modifier mon profil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtlasTextField(
                        value = draftDisplayName,
                        onValueChange = { draftDisplayName = it },
                        label = "Nom affiché *",
                        placeholder = "Ex. Nom complet",
                        imeAction = ImeAction.Next,
                    )
                    AtlasTextField(
                        value = draftEmail,
                        onValueChange = { draftEmail = it },
                        label = "Adresse e-mail",
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
                    Text("Annuler")
                }
            },
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Se déconnecter ?") },
            text = {
                Text(
                    "La session active, le panier et les données temporaires seront effacés. " +
                        "Aucune donnée métier n'est supprimée sur le serveur.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSignOut = false
                        onSignOut()
                    },
                ) { Text("Se déconnecter") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Annuler") }
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
@Composable
fun SettingsScreen(
    user: SessionUser,
    onChangeServer: () -> Unit,
    onOpenQrDevice: () -> Unit = {},
    onOpenLicense: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAbout: () -> Unit,
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
    val mirrorPing by appViewModel.mirrorPing.collectAsStateWithLifecycle()
    val releaseChangelog by appViewModel.releaseChangelog.collectAsStateWithLifecycle()
    val releaseChangelogLoading by appViewModel.releaseChangelogLoading.collectAsStateWithLifecycle()
    val devices by appViewModel.devices.collectAsStateWithLifecycle()
    val devicesLoading by appViewModel.devicesLoading.collectAsStateWithLifecycle()
    val cachedProductCount by appViewModel.cachedProductCount.collectAsStateWithLifecycle()
    val animationsEnabled by appViewModel.animationsEnabled.collectAsStateWithLifecycle()
    val developerModeEnabled by appViewModel.developerModeEnabled.collectAsStateWithLifecycle()
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
            appViewModel.setDeveloperModeEnabled(true)
            onToast("Mode développeur activé. Désactivez-le depuis ce panneau.")
        }
    }

    if (showClearReportsDialog) {
        AlertDialog(
            onDismissRequest = { showClearReportsDialog = false },
            title = { Text("Supprimer les PDF temporaires ?") },
            text = { Text("Les rapports téléchargés dans le cache privé de cette application seront supprimés de cet appareil. Les copies déjà enregistrées ou partagées ailleurs ne seront pas touchées.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearReportsDialog = false
                    appViewModel.clearFinanceReportCache(onToast)
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { showClearReportsDialog = false }) { Text("Annuler") } },
        )
    }

    if (showClearLocalDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearLocalDataDialog = false },
            title = { Text("Effacer les données locales ?") },
            text = {
                Text("Cette action déconnecte ce compte et supprime les sessions mémorisées, serveurs enregistrés, préférences, cache hors ligne, journaux et PDF temporaires de cet appareil. Elle ne supprime aucune vente, facture ni donnée d'entreprise sur le serveur.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearLocalDataDialog = false
                    appViewModel.clearAllLocalAppData(onToast)
                }) { Text("Effacer de cet appareil") }
            },
            dismissButton = { TextButton(onClick = { showClearLocalDataDialog = false }) { Text("Annuler") } },
        )
    }

    if (showReleaseHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showReleaseHistoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Historique des versions", modifier = Modifier.weight(1f))
                    if (releaseChangelogLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = appViewModel::refreshReleaseChangelog) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser l'historique")
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
                            "Aucune version publiée n'est disponible pour le moment.",
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
                                if (entry.isLatest) AtlasChip(text = "Dernière", tone = AtlasTone.POSITIVE)
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
                TextButton(onClick = { showReleaseHistoryDialog = false }) { Text("Fermer") }
            },
        )
    }

    if (showProblemReportDialog) {
        ProblemReportDialog(
            screenOrigin = "Paramètres",
            onDismiss = { showProblemReportDialog = false },
            onToast = onToast,
            appViewModel = appViewModel,
        )
    }

    LaunchedEffect(user.scopeIdentity) {
        appViewModel.loadDevices()
        appViewModel.refreshAgentPolicy()
        appViewModel.refreshCachedProductCount()
        appViewModel.refreshReleaseChangelog()
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
                        text = "Appareils connectés & QR PC (${devices.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Autorisez un ordinateur par QR ou révoquez une session à distance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = appViewModel::loadDevices, enabled = !devicesLoading) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser les appareils")
                }
            }
            Spacer(Modifier.height(10.dp))
            AtlasButton(
                text = "Scanner un poste PC (QR)",
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
        AgentQuotaCard(
            policy = agentPolicy,
            onRefresh = appViewModel::refreshAgentPolicy,
        )

        // 3. Updates, Fast Parallel Download, Latest Release Tracking & Changelog
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

        AtlasCard {
            SettingsToggleRow(
                title = "Téléchargement accéléré multi-connexions",
                subtitle = "Télécharge les mises à jour via 4 connexions HTTP Range parallèles haute vitesse.",
                checked = parallelUpdateDownload,
                onCheckedChange = appViewModel::setParallelUpdateDownload,
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsToggleRow(
                title = "Toujours suivre la dernière version publiée",
                subtitle = "Considère toute version publiée en dernier sur GitHub Releases ou CDN 1 comme la version active.",
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
                        text = "Nouveautés des versions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Ouvrir l'historique complet des changements publiés",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showReleaseHistoryDialog = true }) {
                    Text("Voir")
                }
                IconButton(
                    onClick = appViewModel::refreshReleaseChangelog,
                    enabled = !releaseChangelogLoading,
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser l'historique des versions")
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
                    text = "Serveur & espaces enregistrés",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = serverSelection?.baseUrl?.let { ServerUrl.display(it) } ?: "Aucun serveur configuré",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Chaque serveur dispose de son propre stockage chiffré isolé. Changer de serveur ferme automatiquement la session en cours.",
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
                                        contentDescription = "Supprimer ce serveur",
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
                text = "Changer ou ajouter un serveur",
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
                        text = "Catalogue hors ligne & système",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "$cachedProductCount article(s) isolé(s) pour @${user.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AtlasOutlinedButton(
                    text = "Vider",
                    onClick = { appViewModel.clearOfflineCache(onToast) },
                    icon = Icons.Outlined.DeleteOutline,
                    enabled = cachedProductCount > 0,
                )
            }
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsToggleRow(
                title = "Recherche automatique des mises à jour",
                subtitle = "Vérifie silencieusement les nouvelles versions APK à l'ouverture.",
                checked = autoCheckUpdates,
                onCheckedChange = appViewModel::setAutoCheckUpdates,
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsRow(
                icon = Icons.Outlined.AutoAwesomeMotion,
                title = "Revoir l'écran de bienvenue",
                subtitle = "Relancer la présentation animée d'Atlas Commerce",
                onClick = { appViewModel.replayOnboarding() },
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsRow(
                icon = Icons.Outlined.DeleteOutline,
                title = "Supprimer les PDF temporaires",
                subtitle = "Les rapports privés sont purgés automatiquement après 7 jours.",
                onClick = { showClearReportsDialog = true },
            )
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            SettingsRow(
                icon = Icons.Outlined.DeleteOutline,
                title = "Effacer toutes les données locales",
                subtitle = "Déconnecte le compte et efface les données stockées sur cet appareil uniquement.",
                onClick = { showClearLocalDataDialog = true },
            )
        }

        AnimatedVisibility(
            visible = developerModeEnabled,
            enter = if (animationsEnabled) fadeIn() + expandVertically() else EnterTransition.None,
            exit = if (animationsEnabled) fadeOut() + shrinkVertically() else ExitTransition.None,
        ) {
            AtlasCard {
                SettingsToggleRow(
                    title = "Mode développeur",
                    subtitle = "Actif. Affiche des détails de diagnostic non secrets.",
                    checked = true,
                    onCheckedChange = appViewModel::setDeveloperModeEnabled,
                )
                AtlasDivider(Modifier.padding(vertical = 8.dp))
                AtlasInfoRow("Serveur sélectionné", serverSelection?.let { ServerUrl.display(it.baseUrl) } ?: "Aucun")
                AtlasInfoRow("Version Android", "${AppDetails.VERSION_NAME} · build ${AppDetails.VERSION_CODE}")
                AtlasInfoRow("Identifiant appareil", appViewModel.deviceShortId)
                Text(
                    "Les jetons, mots de passe et clés d'accès ne sont jamais affichés ici.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 6. Security, License & About
        AtlasCard(padding = 4.dp) {
            SettingsRow(
                icon = Icons.Outlined.Security,
                title = "Sécurité & confidentialité",
                subtitle = "Isolation multi-comptes, sessions, chiffrement",
                onClick = onOpenSecurity,
            )
            AtlasDivider(Modifier.padding(horizontal = 12.dp))
            SettingsRow(
                icon = Icons.Outlined.Description,
                title = "Licence MIT",
                subtitle = "Sources et composants tiers",
                onClick = onOpenLicense,
            )
            AtlasDivider(Modifier.padding(horizontal = 12.dp))
            SettingsRow(
                icon = Icons.Outlined.Info,
                title = "À propos",
                subtitle = "${AppDetails.NAME} ${AppDetails.VERSION_NAME}",
                onClick = onOpenAbout,
            )
        }

        // 7. Author Identity (@Etherealmber rounded GitHub PFP + MIT) & Open Source Links
        EtherealmberAuthorCard(onToast = onToast)

        AtlasCard {
            Text("Liens du projet", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            SettingsRow(
                icon = Icons.Outlined.Person,
                title = "Profil GitHub @${AppDetails.GITHUB_HANDLE}",
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
                title = "Signaler un problème",
                subtitle = "Capturer les journaux, ID appareil & jusqu'à 3 images (≤ 1 Mo) avec @atlas-bot",
                onClick = { showProblemReportDialog = true },
            )
        }

        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "${AppDetails.NAME} ${AppDetails.VERSION_NAME} (build ${AppDetails.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onBuildVersionTap),
            )
            Text(
                text = "© ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER} · Licence ${AppDetails.LICENSE_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = subtitle,
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
                    AtlasChip(text = "Cet appareil", tone = AtlasTone.POSITIVE)
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
                contentDescription = "Révoquer cet appareil",
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
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
