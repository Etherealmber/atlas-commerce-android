package ma.etherealmber.atlascommerce.ui.screens.devices

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.TrustedDeviceDto
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasDeviceListSkeleton
import ma.etherealmber.atlascommerce.ui.components.AtlasPulseHalo
import ma.etherealmber.atlascommerce.ui.components.BarcodeCameraView
import ma.etherealmber.atlascommerce.ui.components.CameraVisualStyle
import ma.etherealmber.atlascommerce.ui.components.ScanFeedback
import ma.etherealmber.atlascommerce.ui.i18n.atlasRoleLabel
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasDateTime
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.theme.Atlas


/**
 * Dedicated QR Device Pairing & Connected Sessions screen.
 *
 * Uses the Atlas surface and saffron palette, with a distinct QR reticle,
 * `FORMAT_QR_CODE`-only decoder, and explicit account verification card so operators NEVER
 * confuse pairing a workstation with scanning inventory barcodes.
 */
@Composable
fun QrDeviceScreen(
    currentUser: SessionUser?,
    mode: QrDeviceMode,
    initialToken: String? = null,
    onClose: () -> Unit,
    onToast: (String) -> Unit,
    viewModel: QrDeviceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val otherDevices = remember(state.devices, viewModel) { state.devices.filterNot(viewModel::isCurrentDevice) }
    val autoOpenCamera by viewModel.autoOpenCamera.collectAsStateWithLifecycle()
    val scanHaptic by viewModel.scanHapticEnabled.collectAsStateWithLifecycle()
    val scanSound by viewModel.scanSoundEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val language = LocalAtlasLanguage.current
    val statusMessage = state.statusMessage?.let { atlasNotice(it, language) }
    val errorMessage = state.error?.let { atlasNotice(it, language) }
    var deviceDetailsRevealed by remember(currentUser?.scopeIdentity) { mutableStateOf(false) }
    val cameraPermissionDeniedMessage = if (mode == QrDeviceMode.SIGN_IN_PHONE) {
        atlasText(
            "Caméra refusée — collez le lien ou code QR ci-dessous.",
            "Camera permission denied — paste a QR link or token below.",
            "تم رفض إذن الكاميرا — الصق رابط QR أو الرمز أدناه.",
        )
    } else {
        atlasText(
            "Autorisez l'accès à la caméra pour scanner le QR affiché sur l'ordinateur.",
            "Allow camera access to scan the QR displayed on the computer.",
            "اسمح باستخدام الكاميرا لمسح رمز QR المعروض على الكمبيوتر.",
        )
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { allowed ->
        hasCameraPermission = allowed
        viewModel.setCameraEnabled(allowed)
        if (!allowed) onToast(cameraPermissionDeniedMessage)
    }

    val startQrCameraExplicitly: () -> Unit = {
        val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        hasCameraPermission = allowed
        if (allowed) {
            viewModel.setCameraEnabled(true)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // NEVER auto-open camera on screen entry unless user turned ON autoOpenCamera!
    LaunchedEffect(autoOpenCamera) {
        if (autoOpenCamera && hasCameraPermission) {
            viewModel.setCameraEnabled(true)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setCameraEnabled(false)
        }
    }

    LaunchedEffect(initialToken, mode) {
        viewModel.resetScan()
        viewModel.consumeInitialToken(initialToken, mode)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg -> onToast(atlasNotice(msg, language)) }
    }

    LaunchedEffect(Unit) {
        viewModel.scanFeedbackEvents.collect {
            ScanFeedback.trigger(view = view, haptic = scanHaptic, sound = scanSound)
        }
    }

    // A phone sign-in completes this route; PC authorization stays open to show its result.
    LaunchedEffect(mode, state.step) {
        if (mode == QrDeviceMode.SIGN_IN_PHONE &&
            state.step == QrDeviceViewModel.Step.COMPLETED && state.statusDto == null
        ) {
            onClose()
        }
    }

    val shouldRunCamera = hasCameraPermission &&
        state.cameraEnabled &&
        state.step == QrDeviceViewModel.Step.IDLE &&
        !state.loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Distinct Security Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = atlasText("Retour", "Back", "رجوع"),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (mode == QrDeviceMode.AUTHORIZE_PC) atlasText("Autoriser un PC", "Authorize a PC", "السماح لجهاز كمبيوتر") else atlasText("Connexion du téléphone par QR", "Sign in on this phone with QR", "تسجيل الدخول على هذا الهاتف عبر QR"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = atlasText("APPAIRAGE SÉCURISÉ · HORS CATALOGUE STOCK", "SECURE PAIRING · NOT AN INVENTORY SCAN", "اقتران آمن · ليس مسحًا للمخزون"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(
                onClick = {
                    if (state.cameraEnabled) {
                        viewModel.setCameraEnabled(false)
                    } else {
                        startQrCameraExplicitly()
                    }
                },
            ) {
                Icon(
                    imageVector = if (state.cameraEnabled) Icons.Outlined.Videocam else Icons.Outlined.VideocamOff,
                    contentDescription = if (state.cameraEnabled) atlasText("Couper la caméra", "Turn camera off", "إيقاف الكاميرا") else atlasText("Activer la caméra", "Turn camera on", "تشغيل الكاميرا"),
                    tint = if (state.cameraEnabled) Atlas.palette.positive else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Active Signed-In Identity Pill
            if (currentUser != null) {
                item {
                    SecurityCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AtlasAvatar(
                                avatarUrl = currentUser.avatarUrl,
                                label = currentUser.displayName,
                                size = 44.dp,
                                serverBaseUrl = viewModel.serverUrl,
                                borderColor = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = currentUser.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = atlasText(
                                    "@${currentUser.username} · ${atlasRoleLabel(currentUser.role)}",
                                    "@${currentUser.username} · ${atlasRoleLabel(currentUser.role)}",
                                    "@${currentUser.username} · ${atlasRoleLabel(currentUser.role)}",
                                ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Atlas.palette.positive.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = atlasText("Prêt à autoriser", "Ready to approve", "جاهز للموافقة"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Atlas.palette.positive,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            // Step content: Square QR Scanner OR Confirmation / Polling Card
            item {
                when (state.step) {
                    QrDeviceViewModel.Step.CONFIRM_PC_BIND -> PcApprovalCard(
                        user = currentUser,
                        serverUrl = viewModel.serverUrl,
                        message = statusMessage,
                        loading = state.loading,
                        onApprove = viewModel::confirmPcAuthorization,
                        onDeny = viewModel::denyPcAuthorization,
                    )

                    QrDeviceViewModel.Step.WAITING_PC_APPROVAL -> WaitingPcCard(
                        statusMessage = statusMessage,
                        accountHint = state.statusDto?.displayName ?: state.statusDto?.username,
                        avatarUrl = state.statusDto?.avatarUrl,
                        serverUrl = viewModel.serverUrl,
                        onCancel = viewModel::resetScan,
                    )

                    QrDeviceViewModel.Step.COMPLETED -> CompletedPairingCard(
                        message = statusMessage ?: atlasText("Appareil synchronisé avec succès.", "Device connected successfully.", "تم ربط الجهاز بنجاح."),
                        onScanAnother = viewModel::resetScan,
                        onDone = onClose,
                    )

                    QrDeviceViewModel.Step.IDLE -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .then(if (shouldRunCamera) Modifier.aspectRatio(1f) else Modifier)
                                .align(Alignment.CenterHorizontally),
                        ) {
                            if (shouldRunCamera) {
                                BarcodeCameraView(
                                    active = true,
                                    style = CameraVisualStyle.QR_DEVICE_SECURITY,
                                    onDetected = { raw -> viewModel.handleRawScan(raw, mode) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AtlasPulseHalo {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.QrCode2,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(30.dp),
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = atlasText("Lecteur QR en veille", "QR scanner is paused", "ماسح QR متوقف مؤقتًا"),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = if (mode == QrDeviceMode.AUTHORIZE_PC) {
                                                atlasText("Démarrez la caméra et scannez le QR de connexion affiché sur le PC.", "Start the camera and scan the sign-in QR shown on the PC.", "شغّل الكاميرا وامسح رمز تسجيل الدخول المعروض على الكمبيوتر.")
                                            } else {
                                                atlasText("Démarrez la caméra pour scanner le QR et connecter ce téléphone.", "Start the camera to scan the QR and sign in on this phone.", "شغّل الكاميرا لمسح الرمز وتسجيل الدخول على هذا الهاتف.")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                        )
                                        Spacer(Modifier.height(14.dp))
                                        Button(
                                            onClick = startQrCameraExplicitly,
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Videocam,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = atlasText("Démarrer la caméra QR", "Start QR camera", "بدء كاميرا QR"),
                                                modifier = Modifier.weight(1f),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SecurityCard {
                            Text(
                                text = if (mode == QrDeviceMode.AUTHORIZE_PC) {
                                    atlasText("Sur le PC, choisissez « Connexion par QR » à l'écran de connexion. Le compte déjà ouvert sur ce téléphone sera autorisé.", "On the PC, choose “Sign in with QR” on its sign-in screen. The account already signed in on this phone will be authorized.", "على الكمبيوتر، اختر «تسجيل الدخول عبر QR». سيتم اعتماد الحساب المسجل على هذا الهاتف.")
                                } else {
                                    atlasText("Sur un ordinateur déjà connecté au compte, ouvrez Paramètres → Appareils → « Connecter un téléphone par QR ».", "On a computer already signed in to the account, open Settings → Devices → “Connect a phone with QR”.", "على كمبيوتر مسجل الدخول إلى الحساب، افتح الإعدادات ← الأجهزة ← «ربط هاتف عبر QR».")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (mode == QrDeviceMode.SIGN_IN_PHONE) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedTextField(
                                        value = state.manualInput,
                                        onValueChange = viewModel::onManualInputChange,
                                        label = { Text(atlasText("Lien ou jeton QR", "QR link or token", "رابط أو رمز QR"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        placeholder = { Text("https://.../login-qr?t=…", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Button(
                                        onClick = { viewModel.submitManual(mode) },
                                        enabled = state.manualInput.isNotBlank() && !state.loading,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    ) {
                                        Text(atlasText("Lier", "Link", "ربط"), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!state.error.isNullOrBlank()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }

            // Connected devices section whenever signed in
            if (currentUser != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Devices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = atlasText("Autres appareils", "Other devices", "أجهزة أخرى"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = viewModel::loadDevices, enabled = !state.devicesLoading) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = atlasText("Actualiser les appareils", "Refresh devices", "تحديث الأجهزة"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    TextButton(
                        onClick = { deviceDetailsRevealed = !deviceDetailsRevealed },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (deviceDetailsRevealed) {
                                atlasText("Masquer les appareils", "Hide devices", "إخفاء الأجهزة")
                            } else {
                                atlasText("Afficher les appareils", "Show devices", "إظهار الأجهزة")
                            },
                        )
                    }
                }

                if (deviceDetailsRevealed) {
                    item {
                        Text(
                            atlasText(
                                "Autres appareils (${otherDevices.size})",
                                "Other devices (${otherDevices.size})",
                                "أجهزة أخرى (${otherDevices.size})",
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.devicesLoading && otherDevices.isEmpty()) {
                        item { AtlasDeviceListSkeleton(Modifier.fillMaxWidth(), rows = 2) }
                    } else if (otherDevices.isEmpty()) {
                        item {
                            SecurityCard {
                                Text(
                                    text = atlasText("Aucun autre appareil enregistré sur ce compte.", "No other devices are registered to this account.", "لا توجد أجهزة أخرى مسجّلة لهذا الحساب."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(otherDevices, key = { it.revokeId.ifBlank { it.hashCode().toString() } }) { device ->
                            SecurityDeviceRow(
                                device = device,
                                revoking = state.revokingId == device.revokeId,
                                onRevoke = { viewModel.revokeDevice(device) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
private fun PcApprovalCard(
    user: SessionUser?,
    serverUrl: String?,
    message: String?,
    loading: Boolean,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    SecurityCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Computer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = atlasText("Autoriser ce poste PC ?", "Approve this PC?", "هل توافق على هذا الكمبيوتر؟"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = message ?: atlasText(
                        "Un navigateur demande l'accès à votre compte Atlas. L'approbation est liée à cette session QR à usage unique.",
                        "A browser is requesting access to your Atlas account. Approval is bound to this one-time QR session.",
                        "يطلب متصفح الوصول إلى حساب Atlas. ترتبط الموافقة بجلسة QR هذه لمرة واحدة.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (user != null) {
                AtlasAvatar(
                    avatarUrl = user.avatarUrl,
                    label = user.displayName,
                    size = 40.dp,
                    serverBaseUrl = serverUrl,
                    borderColor = Atlas.palette.positive,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDeny,
                enabled = !loading,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
                Text(atlasText("Refuser", "Deny", "رفض"), color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = onApprove,
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = Atlas.palette.positive),
                modifier = Modifier.weight(1f),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text(atlasText("Autoriser le PC", "Approve this PC", "الموافقة على هذا الكمبيوتر"), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WaitingPcCard(
    statusMessage: String?,
    accountHint: String?,
    avatarUrl: String?,
    serverUrl: String?,
    onCancel: () -> Unit,
) {
    SecurityCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!accountHint.isNullOrBlank()) {
                AtlasAvatar(
                    avatarUrl = avatarUrl,
                    label = accountHint,
                    size = 56.dp,
                    serverBaseUrl = serverUrl,
                    borderColor = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = accountHint,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = statusMessage ?: atlasText(
                    "En attente de confirmation sur l'ordinateur…",
                    "Waiting for confirmation on the computer…",
                    "بانتظار التأكيد على الكمبيوتر…",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onCancel) {
                Text(atlasText("Annuler", "Cancel", "إلغاء"), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun CompletedPairingCard(
    message: String,
    onScanAnother: () -> Unit,
    onDone: () -> Unit,
) {
    SecurityCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Atlas.palette.positive,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onScanAnother, modifier = Modifier.weight(1f)) {
                Text(atlasText("Autre poste", "Another computer", "كمبيوتر آخر"), color = MaterialTheme.colorScheme.onSurface)
            }
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
            ) {
                Text(atlasText("Terminé", "Done", "تم"), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SecurityDeviceRow(
    device: TrustedDeviceDto,
    revoking: Boolean,
    onRevoke: () -> Unit,
) {
    SecurityCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = atlasText(device.friendlyLabel),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (device.current) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Atlas.palette.positive.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = atlasText("Cet appareil", "This device", "هذا الجهاز"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Atlas.palette.positive,
                            )
                        }
                    }
                }
                val lastSeen = device.lastSeen?.takeIf(String::isNotBlank)
                val localizedLastSeen = if (lastSeen != null) atlasDateTime(lastSeen) else null
                val meta = listOfNotNull(
                    device.lastIp?.takeIf { it.isNotBlank() }?.let { "IP $it" },
                    localizedLastSeen,
                    device.fingerprint.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onRevoke, enabled = !revoking) {
                if (revoking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = atlasText("Révoquer cet appareil", "Revoke this device", "إلغاء صلاحية هذا الجهاز"),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
