package ma.etherealmber.atlascommerce.ui.screens.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.BarcodeCameraView
import ma.etherealmber.atlascommerce.ui.components.CameraVisualStyle
import ma.etherealmber.atlascommerce.ui.components.ProductCreateDialog
import ma.etherealmber.atlascommerce.ui.theme.Atlas

/**
 * Warehouse & POS barcode scanner (`STOCK_BARCODE`).
 *
 * Strictly closes CameraX hardware whenever [isActive] turns `false` (tab change,
 * top-level overlay, dialog open, camera toggle off, or Activity pause).
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
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }

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
        if (!allowed) onToast("Caméra refusée — utilisez la saisie manuelle.")
    }

    LaunchedEffect(isActive) {
        if (isActive && !granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message -> onToast(message) }
    }

    val hardwareActive = isActive && granted && state.cameraEnabled && !showCreateDialog

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
        ) {
            if (granted) {
                BarcodeCameraView(
                    active = hardwareActive,
                    style = CameraVisualStyle.STOCK_BARCODE,
                    onDetected = viewModel::onBarcode,
                    modifier = Modifier.fillMaxSize(),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        color = Color(0xAA0F172A),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showCreateDialog = true }) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Nouveau produit", color = Color.White)
                            }
                            IconButton(onClick = { viewModel.setCameraEnabled(!state.cameraEnabled) }) {
                                Icon(
                                    imageVector = if (state.cameraEnabled) {
                                        Icons.Outlined.Videocam
                                    } else {
                                        Icons.Outlined.VideocamOff
                                    },
                                    contentDescription = if (state.cameraEnabled) {
                                        "Mettre la caméra en pause"
                                    } else {
                                        "Réactiver la caméra"
                                    },
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }
            } else {
                CameraPermissionPane(
                    askedOnce = askedOnce,
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                        context.startActivity(intent)
                    },
                )
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
                    canSell = Roles.canCreateDocuments(user.role),
                    canMutateStock = Roles.canMutateStock(user.role),
                    onAdd = { viewModel.addToCart() },
                    onOpenSale = onOpenSale,
                    onOpenProducts = onOpenProducts,
                    onOpenCreateDialog = { showCreateDialog = true },
                    onOpenQrDevice = {
                        val token = state.detectedPcQrToken
                        viewModel.clearPcQrRedirect()
                        onOpenQrDevice(token)
                    },
                    onRetry = viewModel::retry,
                )
                Spacer(Modifier.height(10.dp))
                ManualCodeField(onSubmit = viewModel::onBarcode)
            }
        }
    }

    if (showCreateDialog) {
        ProductCreateDialog(
            canCreateDirectly = Roles.canMutateStock(user.role),
            initialBarcode = state.lastCode.orEmpty(),
            submitting = state.creating,
            onDismiss = { showCreateDialog = false },
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
    canMutateStock: Boolean,
    onAdd: () -> Unit,
    onOpenSale: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenCreateDialog: () -> Unit,
    onOpenQrDevice: () -> Unit,
    onRetry: () -> Unit,
) {
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
                            text = "Cet onglet scanne les articles du stock. Ouvrez le scanner d'appareil PC pour autoriser l'ordinateur.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                AtlasButton(
                    text = "Ouvrir Scanner un poste PC",
                    onClick = onOpenQrDevice,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        state.lookingUp -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Recherche de ${state.lastCode}…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canSell) {
                    AtlasButton(
                        text = if (state.added) "Ajouter encore" else "Ajouter au panier",
                        onClick = onAdd,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.ShoppingCart,
                    )
                    TextButton(onClick = onOpenSale) { Text("Voir le panier") }
                } else {
                    TextButton(onClick = onOpenProducts) { Text("Voir fiche stock") }
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
                            "Code ${state.lastCode} en attente de validation"
                        } else {
                            "Code ${state.lastCode} introuvable"
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Requis pour l'ajout : Nom *, Unité *, Quantité *" +
                            if (canMutateStock) ", SKU *, Prix *." else ".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtlasButton(
                    text = if (canMutateStock) "Créer le produit" else "Demander l'ajout",
                    onClick = onOpenCreateDialog,
                    icon = Icons.Outlined.Add,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRetry) { Text("Réessayer") }
                TextButton(onClick = onOpenProducts) { Text("Catalogue") }
            }
        }

        state.error != null -> AtlasErrorCard(
            title = "Recherche impossible",
            message = state.error.orEmpty(),
            onRetry = onRetry,
        )

        else -> {
            Text(
                text = "Scanner un article du stock",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Scannez un code-barres EAN / SKU pour vendre ou vérifier le stock, " +
                    "ou appuyez sur « + Nouveau produit » pour enregistrer un article manquant.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ManualCodeField(onSubmit: (String) -> Unit) {
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
                if (code.isNotBlank()) onSubmit(code)
            },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        TextButton(
            onClick = { if (code.isNotBlank()) onSubmit(code) },
            enabled = code.isNotBlank(),
        ) {
            Text("Chercher")
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
