package ma.etherealmber.atlascommerce.ui.screens.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.format.AtlasFormat
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasPulseHalo
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.BarcodeCameraView
import ma.etherealmber.atlascommerce.ui.components.CameraVisualStyle
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.components.ProductCreateDialog
import ma.etherealmber.atlascommerce.ui.components.ScanFeedback
import ma.etherealmber.atlascommerce.ui.theme.Atlas

/**
 * Warehouse & POS barcode scanner (`STOCK_BARCODE`).
 *
 * Strictly closes CameraX hardware whenever [isActive] turns `false` (tab change,
 * top-level overlay, dialog open, camera toggle off, or Activity pause), and provides
 * an immediate « Annuler » control whenever a barcode/object has been scanned.
 */
@Composable
fun ScanScreen(
    user: SessionUser,
    isActive: Boolean = true,
    onOpenSale: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenQrDevice: (String?) -> Unit,
    onToast: (String) -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val autoOpenCamera by viewModel.autoOpenCamera.collectAsStateWithLifecycle()
    val scanHaptic by viewModel.scanHapticEnabled.collectAsStateWithLifecycle()
    val scanSound by viewModel.scanSoundEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    var showCreateDialog by remember { mutableStateOf(false) }

    val isAdmin = Roles.canCreateProductDirectly(user.role)
    val canSell = Roles.canCreateDocuments(user.role)

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var askedOnce by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { allowed ->
        granted = allowed
        askedOnce = true
        if (allowed) {
            viewModel.setCameraEnabled(true)
        } else {
            viewModel.setCameraEnabled(false)
            onToast("Caméra refusée — utilisez la saisie manuelle.")
        }
    }

    // NEVER auto-open camera on arrival unless user explicitly turned ON autoOpenCamera.
    // ALWAYS reset cameraEnabled = false immediately whenever leaving Scan!
    LaunchedEffect(isActive, autoOpenCamera) {
        if (!isActive) {
            viewModel.setCameraEnabled(false)
        } else if (autoOpenCamera && granted) {
            viewModel.setCameraEnabled(true)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message -> onToast(message) }
    }
    LaunchedEffect(Unit) {
        viewModel.scanFeedbackEvents.collect {
            ScanFeedback.trigger(view = view, haptic = scanHaptic, sound = scanSound)
        }
    }

    val startCameraExplicitly: () -> Unit = {
        val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        granted = allowed
        if (allowed) {
            viewModel.setCameraEnabled(true)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val hardwareActive = isActive && granted && state.cameraEnabled && !showCreateDialog

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0B1220)),
        ) {
            if (hardwareActive) {
                BarcodeCameraView(
                    active = true,
                    style = CameraVisualStyle.STOCK_BARCODE,
                    onDetected = viewModel::onBarcode,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (!granted && askedOnce) {
                CameraPermissionPane(
                    askedOnce = true,
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                        context.startActivity(intent)
                    },
                )
            } else {
                // Default idle state: camera stays completely OFF until explicitly started!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AtlasPulseHalo {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Caméra en veille",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "La caméra ne s'ouvre plus automatiquement. Appuyez ci-dessous pour scanner un code-barres, ou saisissez le code manuellement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    AtlasButton(
                        text = "Démarrer la caméra",
                        onClick = startCameraExplicitly,
                        icon = Icons.Outlined.Videocam,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (state.hasActiveScan) {
                    Surface(
                        onClick = { viewModel.cancelCurrentScan() },
                        color = Color(0xDC7F1D1D),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Annuler le scan",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Annuler",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                    }
                }
                Surface(
                    color = Color(0xCC0F172A),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showCreateDialog = true }) {
                            Icon(
                                if (isAdmin) Icons.Outlined.Add else Icons.Outlined.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isAdmin) "Nouveau produit" else "Demander à l'Admin",
                                color = Color.White,
                            )
                        }
                        if (hardwareActive) {
                            IconButton(onClick = { viewModel.setCameraEnabled(false) }) {
                                Icon(
                                    imageVector = Icons.Outlined.VideocamOff,
                                    contentDescription = "Fermer la caméra",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Atlas.density.screenPadding, vertical = 12.dp),
            ) {
                // Needed items & open scan requests quick strip
                if (state.neededItems.isNotEmpty() || state.openRequests.isNotEmpty()) {
                    Text(
                        text = "Articles nécessaires & demandes ouvertes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyRow(
                        contentPadding = PaddingValues(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.openRequests, key = { "req-${it.id}" }) { req ->
                            AssistChip(
                                onClick = {
                                    viewModel.onBarcode(req.code)
                                },
                                label = {
                                    Text(
                                        text = "Demande : ${req.name ?: req.code}",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }
                        items(state.neededItems, key = { "need-${it.id}" }) { item ->
                            AssistChip(
                                onClick = {
                                    val code = item.barcode?.takeIf { it.isNotBlank() }
                                        ?: item.sku?.takeIf { it.isNotBlank() }
                                    if (code != null) viewModel.onBarcode(code) else onOpenProducts()
                                },
                                label = {
                                    Text(
                                        text = "${item.name} · manque ${AtlasFormat.quantity(item.neededQuantity)}",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                ScanResultPanel(
                    state = state,
                    canSell = canSell,
                    isAdmin = isAdmin,
                    onAdd = { viewModel.addToCart() },
                    onOpenSale = onOpenSale,
                    onOpenProducts = onOpenProducts,
                    onOpenCreateDialog = { showCreateDialog = true },
                    onOpenQrDevice = {
                        val token = state.detectedPcQrToken
                        viewModel.clearPcQrRedirect()
                        onOpenQrDevice(token)
                    },
                    onCancelScan = { viewModel.cancelCurrentScan() },
                    onRetry = viewModel::retry,
                )
                Spacer(Modifier.height(10.dp))
                ManualCodeField(
                    hasActiveScan = state.hasActiveScan,
                    onSubmit = viewModel::onBarcode,
                    onClearScan = { viewModel.cancelCurrentScan() },
                )
            }
        }
    }

    if (showCreateDialog) {
        ProductCreateDialog(
            canCreateDirectly = isAdmin,
            initialBarcode = state.lastCode.orEmpty(),
            submitting = state.creating,
            onDismiss = {
                showCreateDialog = false
            },
            onSubmitDirect = { req ->
                showCreateDialog = false
                viewModel.createProductFromScan(req)
            },
            onSubmitRequest = { code, name, sku, unit, qty, cat, note ->
                showCreateDialog = false
                viewModel.submitRequestFromScan(code, name, sku, unit, qty, cat, note)
            },
        )
    }
}

@Composable
private fun ScanResultPanel(
    state: ScanViewModel.UiState,
    canSell: Boolean,
    isAdmin: Boolean,
    onAdd: () -> Unit,
    onOpenSale: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenCreateDialog: () -> Unit,
    onOpenQrDevice: () -> Unit,
    onCancelScan: () -> Unit,
    onRetry: () -> Unit,
) {
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    val panelKey = when {
        state.detectedPcQrToken != null -> "qr"
        state.lookingUp -> "lookup"
        state.product != null -> "product-${state.product.id}-${state.added}"
        state.notFound -> "notfound-${state.lastCode}-${state.pendingApproval}"
        state.error != null -> "err"
        else -> "idle"
    }

    AnimatedContent(
        targetState = panelKey,
        transitionSpec = {
            if (!motionEnabled) {
                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            } else {
                (fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 6 }) togetherWith
                    fadeOut(tween(150))
            }
        },
        label = "scan_result_panel",
    ) { _ ->
        Column(modifier = Modifier.fillMaxWidth()) {
            val product = state.product
            when {
                state.detectedPcQrToken != null -> {
                    AtlasCard(padding = 10.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Computer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "QR code de sécurité PC détecté",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Cet onglet scanne les articles du stock. Ouvrez le scanner d'appareil PC ou annulez.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AtlasButton(
                                text = "Scanner poste PC",
                                onClick = onOpenQrDevice,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(onClick = onCancelScan) {
                                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Annuler")
                            }
                        }
                    }
                }

                state.lookingUp -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Recherche de ${state.lastCode}…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = onCancelScan) {
                            Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Annuler")
                        }
                    }
                }

                product != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = buildString {
                                    append(AtlasFormat.money(product.price))
                                    append(" · Stock ")
                                    append(AtlasFormat.quantity(product.stock))
                                    if (product.neededQuantity > 0.0) {
                                        append(" (manque ")
                                        append(AtlasFormat.quantity(product.neededQuantity))
                                        append(")")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.added) {
                            AtlasChip(text = "Ajouté", tone = AtlasTone.POSITIVE, icon = Icons.Outlined.CheckCircle)
                        } else {
                            AtlasChip(
                                text = if (product.isLowStock) "À réappro." else "En stock",
                                tone = if (product.isLowStock) AtlasTone.WARNING else AtlasTone.POSITIVE,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (canSell) {
                            AtlasButton(
                                text = if (state.added) "Ajouter +" else "Ajouter au panier",
                                onClick = onAdd,
                                modifier = Modifier.weight(1f),
                                icon = Icons.Outlined.ShoppingCart,
                            )
                            TextButton(onClick = onOpenSale) { Text("Panier") }
                        } else {
                            TextButton(
                                onClick = onOpenProducts,
                                modifier = Modifier.weight(1f),
                            ) { Text("Voir fiche stock") }
                        }
                        OutlinedButton(onClick = onCancelScan) {
                            Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Annuler")
                        }
                    }
                }

                state.notFound -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = Atlas.palette.warning,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (state.pendingApproval) {
                                    "Code ${state.lastCode} transmis à l'Administrateur"
                                } else {
                                    "Code ${state.lastCode} introuvable"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (state.pendingApproval) {
                                    "La demande est en attente de validation Admin. Vous pouvez continuer ou annuler ce scan."
                                } else if (isAdmin) {
                                    "En tant qu'Admin, vous pouvez créer cet article directement ou annuler ce scan."
                                } else {
                                    "Cet article sera envoyé immédiatement à l'Administrateur pour création, ou appuyez sur Annuler."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!state.pendingApproval) {
                            AtlasButton(
                                text = if (isAdmin) "Créer le produit" else "Envoyer à l'Admin",
                                onClick = onOpenCreateDialog,
                                icon = if (isAdmin) Icons.Outlined.Add else Icons.Outlined.Send,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            TextButton(
                                onClick = onOpenProducts,
                                modifier = Modifier.weight(1f),
                            ) { Text("Voir catalogue") }
                        }
                        OutlinedButton(onClick = onCancelScan) {
                            Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Annuler")
                        }
                    }
                }

                state.error != null -> {
                    AtlasErrorCard(
                        title = "Recherche impossible",
                        message = state.error.orEmpty(),
                        onRetry = onRetry,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onCancelScan,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Annuler")
                    }
                }

                else -> {
                    Text(
                        text = "Scanner un article du stock",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Scannez un code-barres EAN / SKU pour vérifier ou vendre, ou annulez à tout moment si vous ne souhaitez pas enregistrer l'objet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualCodeField(
    hasActiveScan: Boolean,
    onSubmit: (String) -> Unit,
    onClearScan: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        AtlasTextField(
            value = code,
            onValueChange = { code = it },
            label = "Saisie manuelle",
            placeholder = "Code-barres / SKU",
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search,
            onDone = {
                if (code.isNotBlank()) {
                    onSubmit(code)
                    code = ""
                }
            },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        TextButton(
            onClick = {
                if (code.isNotBlank()) {
                    onSubmit(code)
                    code = ""
                }
            },
            enabled = code.isNotBlank(),
        ) {
            Text("Chercher")
        }
        if (code.isNotBlank() || hasActiveScan) {
            IconButton(
                onClick = {
                    code = ""
                    if (hasActiveScan) onClearScan()
                },
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Effacer et annuler")
            }
        }
    }
}

@Composable
private fun CameraPermissionPane(
    askedOnce: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Accès caméra requis",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "La caméra sert uniquement à lire les codes-barres et se coupe automatiquement dès que vous quittez l'onglet Scan.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(14.dp))
        AtlasButton(text = "Autoriser la caméra", onClick = onRequest)
        if (askedOnce) {
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onOpenSettings) {
                Text("Ouvrir les réglages", color = Color.White)
            }
        }
    }
}
