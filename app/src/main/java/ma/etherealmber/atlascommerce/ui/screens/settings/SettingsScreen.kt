package ma.etherealmber.atlascommerce.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.ChevronRight
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
import ma.etherealmber.atlascommerce.ui.shell.MainTab
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import ma.etherealmber.atlascommerce.ui.util.openExternal

@OptIn(ExperimentalLayoutApi::class)
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
    val autoCheckUpdates by appViewModel.autoCheckUpdates.collectAsStateWithLifecycle()
    val roleNotificationsEnabled by appViewModel.roleNotificationsEnabled.collectAsStateWithLifecycle()
    val serverSelection by appViewModel.server.collectAsStateWithLifecycle()
    val updateState by appViewModel.updateState.collectAsStateWithLifecycle()
    val updateDownloadSource by appViewModel.updateDownloadSource.collectAsStateWithLifecycle()
    val mirrorPing by appViewModel.mirrorPing.collectAsStateWithLifecycle()
    val devices by appViewModel.devices.collectAsStateWithLifecycle()
    val devicesLoading by appViewModel.devicesLoading.collectAsStateWithLifecycle()
    val uploadingAvatar by appViewModel.uploadingAvatar.collectAsStateWithLifecycle()
    val savingProfile by appViewModel.savingProfile.collectAsStateWithLifecycle()
    val cachedProductCount by appViewModel.cachedProductCount.collectAsStateWithLifecycle()

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

    LaunchedEffect(Unit) {
        appViewModel.loadDevices()
        appViewModel.refreshCachedProductCount()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = density.screenPadding,
            end = density.screenPadding,
            top = density.cardMargin,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(density.itemGap),
    ) {
        // 1. Profil & Compte
        item {
            AtlasEntrance(delayMillis = 0) {
                AtlasCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AtlasAvatar(
                            avatarUrl = user.avatarUrl,
                            label = user.displayName,
                            size = 58.dp,
                            serverBaseUrl = serverSelection?.baseUrl,
                            onClick = { photoPicker.launch("image/*") },
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "@${user.username}",
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AtlasOutlinedButton(
                            text = if (uploadingAvatar) "Envoi…" else "Changer la photo",
                            onClick = { photoPicker.launch("image/*") },
                            icon = Icons.Outlined.PhotoCamera,
                            enabled = !uploadingAvatar,
                            modifier = Modifier.weight(1f),
                        )
                        AtlasOutlinedButton(
                            text = "Déconnexion",
                            onClick = { confirmSignOut = true },
                            icon = Icons.Outlined.Logout,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // 2. Apparence, Navigation & Animations
        item {
            AtlasEntrance(delayMillis = 35) {
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
                    AtlasDivider(Modifier.padding(vertical = 8.dp))
                    SettingsRow(
                        icon = Icons.Outlined.AutoAwesomeMotion,
                        title = "Revoir l'écran de bienvenue",
                        subtitle = "Relancer la présentation animée d'Atlas Commerce",
                        onClick = { appViewModel.replayOnboarding() },
                    )
                }
            }
        }

        // 3. Caméra & Scan
        item {
            AtlasEntrance(delayMillis = 70) {
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
            }
        }

        // 4. Vente rapide (Caisse) — visible uniquement aux rôles habilités Ventes/Facturation
        if (Roles.canCreateDocuments(user.role)) {
            item {
                AtlasEntrance(delayMillis = 105) {
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
        }

        // 5. Appareils connectés & QR PC
        item {
            AtlasEntrance(delayMillis = 140) {
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
                                text = "Appareils connectés (${devices.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Autorisez un ordinateur par QR ou révoquez une session.",
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
            }
        }

        // 6. Cache hors ligne & Mises à jour
        item {
            AtlasEntrance(delayMillis = 175) {
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
                                text = "$cachedProductCount article(s) mémorisé(s) sur cet appareil",
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
                }
            }
        }

        item {
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

        // 7. Serveur
        item {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Serveur", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = ServerUrl.display(serverSelection?.baseUrl ?: AppDetails.DEFAULT_API_BASE),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Vos données restent sur votre serveur Atlas. Changer de serveur vous déconnecte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                AtlasOutlinedButton(
                    text = "Changer de serveur",
                    onClick = onChangeServer,
                    icon = Icons.Outlined.SwapHoriz,
                )
            }
        }

        // 8. Mentions légales & liens
        item {
            AtlasCard(padding = 4.dp) {
                SettingsRow(
                    icon = Icons.Outlined.Security,
                    title = "Sécurité & confidentialité",
                    subtitle = "Sessions, appareil, données",
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
        }

        item {
            AtlasCard {
                Text("Liens", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
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
                    icon = Icons.Outlined.OpenInNew,
                    title = "Signaler un problème",
                    subtitle = "Issues GitHub",
                    onClick = {
                        if (!openExternal(context, AppDetails.ISSUES_URL)) {
                            onToast("Aucun navigateur disponible.")
                        }
                    },
                )
            }
        }

        item {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "${AppDetails.NAME} ${AppDetails.VERSION_NAME} (build ${AppDetails.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "© ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER} · Licence ${AppDetails.LICENSE_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        placeholder = "Ex. MY Marbre",
                        imeAction = ImeAction.Next,
                    )
                    AtlasTextField(
                        value = draftEmail,
                        onValueChange = { draftEmail = it },
                        label = "Adresse e-mail",
                        placeholder = "contact@exemple.ma",
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
                    "Le jeton de session sera effacé de cet appareil et le panier vidé. " +
                        "Aucune donnée de vente n'est supprimée sur le serveur.",
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
