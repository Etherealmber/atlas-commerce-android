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
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.TrustedDeviceDto
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasPulseHalo
import ma.etherealmber.atlascommerce.ui.components.BarcodeCameraView
import ma.etherealmber.atlascommerce.ui.components.CameraVisualStyle
import ma.etherealmber.atlascommerce.ui.components.ScanFeedback

private val SecurityBgTop = Color(0xFF070B19)
private val SecurityBgBottom = Color(0xFF0F172A)
private val SecurityCardBg = Color(0xFF111C38)
private val SecurityCardBorder = Color(0xFF283B6B)
private val SecurityCyan = Color(0xFF38BDF8)
private val SecurityEmerald = Color(0xFF34D399)
private val SecurityMuted = Color(0xFF94A3B8)

/**
 * Dedicated QR Device Pairing & Connected Sessions screen.
 *
 * Designed with a midnight-indigo security cockpit aesthetic, square cyan/emerald QR reticle,
 * `FORMAT_QR_CODE`-only decoder, and explicit account verification card so operators NEVER
 * confuse pairing a workstation with scanning inventory barcodes.
 */
@Composable
fun QrDeviceScreen(
    currentUser: SessionUser?,
    initialToken: String? = null,
    onClose: () -> Unit,
    onToast: (String) -> Unit,
    viewModel: QrDeviceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val autoOpenCamera by viewModel.autoOpenCamera.collectAsStateWithLifecycle()
    val scanHaptic by viewModel.scanHapticEnabled.collectAsStateWithLifecycle()
    val scanSound by viewModel.scanSoundEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current

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
        if (!allowed) onToast("Caméra refusée — collez le lien ou code QR ci-dessous.")
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

    LaunchedEffect(initialToken) {
        viewModel.consumeInitialToken(initialToken)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg -> onToast(msg) }
    }

    LaunchedEffect(Unit) {
        viewModel.scanFeedbackEvents.collect {
            ScanFeedback.trigger(view = view, haptic = scanHaptic, sound = scanSound)
        }
    }

    // Auto-close after pre-login QR authentication succeeds
    LaunchedEffect(currentUser, state.step) {
        if (currentUser != null && state.step == QrDeviceViewModel.Step.COMPLETED && state.statusDto == null) {
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
            .background(Brush.verticalGradient(listOf(SecurityBgTop, SecurityBgBottom)))
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
                    contentDescription = "Retour",
                    tint = Color.White,
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SecurityCyan.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = SecurityCyan,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (currentUser != null) "Scanner un poste PC" else "Connexion mobile par QR",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "APPAIRAGE SÉCURISÉ · HORS CATALOGUE STOCK",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecurityCyan,
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
                    contentDescription = if (state.cameraEnabled) "Couper la caméra" else "Activer la caméra",
                    tint = if (state.cameraEnabled) SecurityEmerald else SecurityMuted,
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
                                borderColor = SecurityCyan,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = currentUser.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "@${currentUser.username} · ${Roles.label(currentUser.role)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecurityMuted,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(SecurityEmerald.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "Prêt à autoriser",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecurityEmerald,
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
                        message = state.statusMessage,
                        loading = state.loading,
                        onApprove = viewModel::confirmPcAuthorization,
                        onDeny = viewModel::denyPcAuthorization,
                    )

                    QrDeviceViewModel.Step.WAITING_PC_APPROVAL -> WaitingPcCard(
                        statusMessage = state.statusMessage,
                        accountHint = state.statusDto?.displayName ?: state.statusDto?.username,
                        avatarUrl = state.statusDto?.avatarUrl,
                        serverUrl = viewModel.serverUrl,
                        onCancel = viewModel::resetScan,
                    )

                    QrDeviceViewModel.Step.COMPLETED -> CompletedPairingCard(
                        message = state.statusMessage ?: "Appareil synchronisé avec succès.",
                        onScanAnother = viewModel::resetScan,
                        onDone = onClose,
                    )

                    QrDeviceViewModel.Step.IDLE -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .aspectRatio(1f)
                                .align(Alignment.CenterHorizontally),
                        ) {
                            if (shouldRunCamera) {
                                BarcodeCameraView(
                                    active = true,
                                    style = CameraVisualStyle.QR_DEVICE_SECURITY,
                                    onDetected = viewModel::handleRawScan,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(SecurityCardBg)
                                        .border(1.dp, SecurityCardBorder, RoundedCornerShape(24.dp))
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AtlasPulseHalo {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(SecurityCyan.copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.QrCode2,
                                                    contentDescription = null,
                                                    tint = SecurityCyan,
                                                    modifier = Modifier.size(30.dp),
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = "Lecteur QR en veille",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "Appuyez ci-dessous pour démarrer la caméra QR, ou collez directement le lien ci-dessous.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SecurityMuted,
                                            textAlign = TextAlign.Center,
                                        )
                                        Spacer(Modifier.height(14.dp))
                                        Button(
                                            onClick = startQrCameraExplicitly,
                                            colors = ButtonDefaults.buttonColors(containerColor = SecurityCyan),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Videocam,
                                                contentDescription = null,
                                                tint = SecurityBgTop,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("Démarrer la caméra QR", color = SecurityBgTop)
                                        }
                                    }
                                }
                            }
                        }

                        SecurityCard {
                            Text(
                                text = if (currentUser != null) {
                                    "Sur l'ordinateur, ouvrez l'écran de connexion Atlas Commerce puis cliquez sur « Connexion par QR »."
                                } else {
                                    "Sur un ordinateur déjà connecté, ouvrez Paramètres → Appareils → « Connecter un téléphone par QR »."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = SecurityMuted,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = state.manualInput,
                                    onValueChange = viewModel::onManualInputChange,
                                    label = { Text("Lien ou jeton QR", color = SecurityMuted) },
                                    placeholder = { Text("https://.../login-qr?t=…", color = SecurityMuted) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Link, contentDescription = null, tint = SecurityCyan)
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = SecurityCyan,
                                        unfocusedBorderColor = SecurityCardBorder,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                                Button(
                                    onClick = viewModel::submitManual,
                                    enabled = state.manualInput.isNotBlank() && !state.loading,
                                    colors = ButtonDefaults.buttonColors(containerColor = SecurityCyan),
                                ) {
                                    Text("Lier", color = SecurityBgTop, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (!state.error.isNullOrBlank()) {
                item {
                    Surface(
                        color = Color(0xFF3B121E),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = state.error.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFCA5A5),
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
                            tint = SecurityCyan,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Appareils connectés (${state.devices.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = viewModel::loadDevices) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Actualiser les appareils",
                                tint = SecurityMuted,
                            )
                        }
                    }
                }

                if (state.devicesLoading && state.devices.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SecurityCyan, modifier = Modifier.size(22.dp))
                        }
                    }
                } else if (state.devices.isEmpty()) {
                    item {
                        SecurityCard {
                            Text(
                                text = "Aucun autre appareil enregistré sur ce compte.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecurityMuted,
                            )
                        }
                    }
                } else {
                    items(state.devices, key = { it.revokeId.ifBlank { it.hashCode().toString() } }) { device ->
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

@Composable
private fun SecurityCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SecurityCardBg)
            .border(1.dp, SecurityCardBorder, RoundedCornerShape(18.dp))
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
                tint = SecurityCyan,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Autoriser ce poste PC ?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = message ?: "Un navigateur demande l'accès à votre compte Atlas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecurityMuted,
                )
            }
            if (user != null) {
                AtlasAvatar(
                    avatarUrl = user.avatarUrl,
                    label = user.displayName,
                    size = 40.dp,
                    serverBaseUrl = serverUrl,
                    borderColor = SecurityEmerald,
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
                Icon(Icons.Outlined.Close, contentDescription = null, tint = Color(0xFFFCA5A5))
                Spacer(Modifier.width(6.dp))
                Text("Refuser", color = Color(0xFFFCA5A5))
            }
            Button(
                onClick = onApprove,
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = SecurityEmerald),
                modifier = Modifier.weight(1f),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SecurityBgTop)
                } else {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SecurityBgTop)
                    Spacer(Modifier.width(6.dp))
                    Text("Autoriser le PC", color = SecurityBgTop, fontWeight = FontWeight.Bold)
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
                    borderColor = SecurityCyan,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = accountHint,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                CircularProgressIndicator(color = SecurityCyan, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = statusMessage ?: "En attente de confirmation sur l'ordinateur…",
                style = MaterialTheme.typography.bodyMedium,
                color = SecurityMuted,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onCancel) {
                Text("Annuler", color = Color.White)
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
                tint = SecurityEmerald,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onScanAnother, modifier = Modifier.weight(1f)) {
                Text("Autre poste", color = Color.White)
            }
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = SecurityCyan),
                modifier = Modifier.weight(1f),
            ) {
                Text("Terminé", color = SecurityBgTop, fontWeight = FontWeight.Bold)
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
                        text = device.friendlyLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (device.current) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(SecurityEmerald.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "Cet appareil",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecurityEmerald,
                            )
                        }
                    }
                }
                val meta = listOfNotNull(
                    device.lastIp?.takeIf { it.isNotBlank() }?.let { "IP $it" },
                    device.lastSeen?.takeIf { it.isNotBlank() },
                    device.fingerprint.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = SecurityMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onRevoke, enabled = !revoking) {
                if (revoking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SecurityCyan)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Révoquer cet appareil",
                        tint = Color(0xFFFCA5A5),
                    )
                }
            }
        }
    }
}
