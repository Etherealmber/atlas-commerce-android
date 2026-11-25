package ma.etherealmber.atlascommerce.ui.screens.scan

import ma.etherealmber.atlascommerce.ui.theme.AtlasIllustrationPalette as ArtPalette
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
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
import ma.etherealmber.atlascommerce.ui.components.AtlasShimmer
import ma.etherealmber.atlascommerce.ui.components.AtlasSkeletonBlock
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasPulseHalo
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.BarcodeCameraView
import ma.etherealmber.atlascommerce.ui.components.CameraVisualStyle
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingTopBarPadding
import ma.etherealmber.atlascommerce.ui.components.ProductCreateDialog
import ma.etherealmber.atlascommerce.ui.components.ScanFeedback
import ma.etherealmber.atlascommerce.ui.screens.products.productStockDisplay
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoneyCompact
import ma.etherealmber.atlascommerce.ui.i18n.atlasQuantity
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
    createProductRequest: Int = 0,
    onCreateProductRequestHandled: () -> Unit = {},
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val queuedInventoryOperations by viewModel.inventoryQueueEntries.collectAsStateWithLifecycle()
    val inventoryQueueReadError by viewModel.inventoryQueueReadError.collectAsStateWithLifecycle()
    val inventoryQueueReplaying by viewModel.inventoryQueueReplaying.collectAsStateWithLifecycle()
    val autoOpenCamera by viewModel.autoOpenCamera.collectAsStateWithLifecycle()
    val scanHaptic by viewModel.scanHapticEnabled.collectAsStateWithLifecycle()
    val scanSound by viewModel.scanSoundEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val cameraPermissionDeniedMessage = atlasText(
        "Caméra refusée — utilisez la saisie manuelle.",
        "Camera permission denied — use manual entry instead.",
        "تم رفض إذن الكاميرا — استخدم الإدخال اليدوي بدلًا من ذلك.",
    )
    var showCreateDialog by remember { mutableStateOf(false) }
    var showScannedProductDialog by remember { mutableStateOf(false) }
    var showStockAdjustDialog by remember { mutableStateOf(false) }
    var showDiscardHeldConfirmation by remember { mutableStateOf(false) }
    var typedHeldDiscardCount by remember { mutableStateOf("") }

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
            onToast(cameraPermissionDeniedMessage)
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
    LaunchedEffect(state.product?.id, state.lastCode) {
        if (state.product != null) showScannedProductDialog = true
    }
    LaunchedEffect(state.notFound, state.lastCode, state.pendingApproval) {
        if (state.notFound && !state.pendingApproval && !state.lastCode.isNullOrBlank()) {
            showCreateDialog = true
        }
    }

    LaunchedEffect(createProductRequest) {
        if (createProductRequest > 0) {
            showCreateDialog = true
            onCreateProductRequestHandled()
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
    val heldWrites = queuedInventoryOperations.filter { it.status == "HELD" }
    val terminalWrites = queuedInventoryOperations.filter { it.status in setOf("REJECTED", "CONFLICT", "FAILED") }

    Column(modifier = Modifier.fillMaxSize()) {
        if (heldWrites.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "${heldWrites.size} writes held — this server needs migration 0020",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        enabled = !inventoryQueueReplaying,
                        onClick = {
                            viewModel.exportHeldInventoryWrites { payloads ->
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Held inventory writes", payloads))
                                onToast("Held write payloads copied as JSON.")
                            }
                        },
                    ) { Text(atlasText("Copier les écritures", "Copy held writes", "نسخ العمليات المتوقفة")) }
                    TextButton(
                        enabled = !inventoryQueueReplaying,
                        onClick = { typedHeldDiscardCount = ""; showDiscardHeldConfirmation = true },
                    ) { Text(atlasText("Supprimer les écritures", "Discard held writes", "حذف العمليات المتوقفة")) }
                }
            }
        }
        terminalWrites.forEach { blocked ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${blocked.productName} · ${blocked.locationName}: ${blocked.failureMessage.orEmpty()}",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                when (blocked.status) {
                    "CONFLICT" -> TextButton(
                        enabled = !inventoryQueueReplaying,
                        onClick = { viewModel.recountAndResendInventoryOperation(blocked.operationId) },
                    ) { Text(atlasText("Recompter et renvoyer", "Recount and resend", "إعادة الجرد والإرسال")) }
                    "REJECTED" -> TextButton(
                        enabled = !inventoryQueueReplaying,
                        onClick = { viewModel.dismissRejectedInventoryOperation(blocked.operationId) },
                    ) { Text(atlasText("Ignorer", "Dismiss", "تجاهل")) }
                    else -> TextButton(
                        enabled = !inventoryQueueReplaying,
                        onClick = { viewModel.dismissFailedInventoryOperation(blocked.operationId) },
                    ) { Text(atlasText("Écarter", "Dismiss", "تجاهل")) }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(if (hardwareActive) ArtPalette.scanIdleBackdrop else MaterialTheme.colorScheme.surfaceContainerLow),
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
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Videocam,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = atlasText("Caméra en veille", "Camera is paused", "الكاميرا متوقفة مؤقتًا"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = atlasText("La caméra ne s'ouvre plus automatiquement. Appuyez ci-dessous pour scanner un code-barres, ou saisissez le code manuellement.", "The camera will no longer open automatically. Tap below to scan a barcode, or enter the code manually.", "لن تفتح الكاميرا تلقائيًا بعد الآن. اضغط أدناه لمسح الرمز الشريطي أو أدخل الرمز يدويًا."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    AtlasButton(
                        text = atlasText("Démarrer la caméra", "Start camera", "بدء الكاميرا"),
                        onClick = startCameraExplicitly,
                        icon = Icons.Outlined.Videocam,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        start = 10.dp,
                        top = 10.dp + LocalAtlasFloatingTopBarPadding.current,
                        end = 10.dp,
                        bottom = 10.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (state.hasActiveScan) {
                    Surface(
                        onClick = { viewModel.cancelCurrentScan() },
                        color = ArtPalette.scanCancelOverlay,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = atlasText("Annuler le scan", "Cancel scan", "إلغاء المسح"),
                                tint = ArtPalette.white,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = atlasText("Annuler", "Cancel", "إلغاء"),
                                style = MaterialTheme.typography.labelLarge,
                                color = ArtPalette.white,
                            )
                        }
                    }
                }
                if (hardwareActive) {
                    Surface(
                        color = ArtPalette.scanControlOverlay,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        IconButton(onClick = { viewModel.setCameraEnabled(false) }) {
                            Icon(
                                imageVector = Icons.Outlined.VideocamOff,
                                contentDescription = atlasText("Fermer la caméra", "Close camera", "إغلاق الكاميرا"),
                                tint = ArtPalette.white,
                            )
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
                if (state.neededContextLoading && state.neededItems.isEmpty() && state.openRequests.isEmpty()) {
                    Text(
                        text = atlasText("Articles nécessaires & demandes ouvertes", "Needed items & open requests", "الأصناف المطلوبة والطلبات المفتوحة"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    AtlasShimmer {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(3) { AtlasSkeletonBlock(Modifier.size(width = 120.dp, height = 32.dp), RoundedCornerShape(99.dp)) }
                        }
                    }
                } else if (state.neededItems.isNotEmpty() || state.openRequests.isNotEmpty()) {
                    Text(
                        text = atlasText("Articles nécessaires & demandes ouvertes", "Needed items & open requests", "الأصناف المطلوبة والطلبات المفتوحة"),
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
                                        text = atlasText("Demande : ${req.name ?: req.code}", "Request: ${req.name ?: req.code}", "الطلب: ${req.name ?: req.code}"),
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
                                        text = atlasText("${item.name} · manque ${atlasQuantity(item.neededQuantity)}", "${item.name} · short by ${atlasQuantity(item.neededQuantity)}", "${item.name} · النقص ${atlasQuantity(item.neededQuantity)}"),
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

    val scannedProduct = state.product
    val canEditScannedStock = Roles.canRequestStockAdjustment(user.role) && scannedProduct?.trackInventory == true
    LaunchedEffect(showStockAdjustDialog, scannedProduct?.id) {
        if (showStockAdjustDialog && scannedProduct != null && Roles.canAdjustStock(user.role)) {
            viewModel.loadInventoryContext(scannedProduct.id)
        }
    }
    if (showScannedProductDialog && scannedProduct != null) {
        AlertDialog(
            onDismissRequest = { showScannedProductDialog = false },
            title = {
                Text(
                    text = scannedProduct.displayName?.takeIf { it.isNotBlank() } ?: scannedProduct.name,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        atlasText(
                            "Ce produit existe déjà. Voulez-vous modifier son stock ?",
                            "This product already exists. Would you like to edit its stock?",
                            "هذا المنتج موجود بالفعل. هل تريد تعديل مخزونه؟",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        atlasText(
                            "Code : ${state.lastCode.orEmpty()}",
                            "Code: ${state.lastCode.orEmpty()}",
                            "الرمز: ${state.lastCode.orEmpty()}",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (scannedProduct.trackInventory) {
                        Text(
                            atlasText(
                                "Stock : ${productStockDisplay(scannedProduct.stock, scannedProduct.unit, scannedProduct.secondaryUnit, scannedProduct.secondaryUnitFactor)}",
                                "Stock: ${productStockDisplay(scannedProduct.stock, scannedProduct.unit, scannedProduct.secondaryUnit, scannedProduct.secondaryUnitFactor)}",
                                "المخزون: ${productStockDisplay(scannedProduct.stock, scannedProduct.unit, scannedProduct.secondaryUnit, scannedProduct.secondaryUnitFactor)}",
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        atlasText(
                            "Prix : ${atlasMoneyCompact(scannedProduct.price)}",
                            "Price: ${atlasMoneyCompact(scannedProduct.price)}",
                            "السعر: ${atlasMoneyCompact(scannedProduct.price)}",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (state.added) {
                        Text(
                            atlasText("Ajouté au panier.", "Added to cart.", "تمت الإضافة إلى السلة."),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (canEditScannedStock) {
                        TextButton(
                            onClick = {
                                showScannedProductDialog = false
                                showStockAdjustDialog = true
                            },
                        ) {
                            Text(atlasText("Modifier le stock", "Edit stock", "تعديل المخزون"))
                        }
                    }
                    TextButton(
                        onClick = {
                            showScannedProductDialog = false
                            viewModel.continueScanning()
                            startCameraExplicitly()
                        },
                        enabled = isActive,
                    ) {
                        Text(atlasText("Aucun changement", "No changes", "لا تغيير"))
                    }
                }
            },
            dismissButton = {
                if (canSell && !state.added) {
                    TextButton(
                        onClick = {
                            viewModel.addToCart()
                            showScannedProductDialog = false
                        },
                    ) {
                        Text(atlasText("Ajouter", "Add", "إضافة"))
                    }
                } else {
                    TextButton(onClick = { showScannedProductDialog = false }) {
                        Text(atlasText("Aucun changement", "No changes", "لا تغيير"))
                    }
                }
            },
        )
    }

    if (showStockAdjustDialog && scannedProduct != null) {
        var rawStockChange by remember(scannedProduct.id) { mutableStateOf("") }
        var operationKind by remember(scannedProduct.id) { mutableStateOf("receive") }
        var selectedLocationId by remember(scannedProduct.id) { mutableStateOf<Long?>(null) }
        var locationMenuOpen by remember { mutableStateOf(false) }
        var stockReason by remember(scannedProduct.id) { mutableStateOf("") }
        val parsedStockChange = rawStockChange.replace(',', '.').toDoubleOrNull()
        val directAdjustment = Roles.canAdjustStock(user.role)
        val selectedLocation = state.inventoryLocations.firstOrNull { it.id == selectedLocationId }
        val observedPlacement = state.productPlacements.firstOrNull { it.locationId == selectedLocationId }
        val countNeedsZeroOrMore = operationKind == "count"
        val quantityValid = parsedStockChange != null && parsedStockChange.isFinite() && (
            if (countNeedsZeroOrMore) parsedStockChange >= 0.0 else parsedStockChange > 0.0
        )
        LaunchedEffect(state.inventoryLocations, selectedLocationId) {
            if (selectedLocationId == null) {
                selectedLocationId = state.inventoryLocations.firstOrNull { it.isDefault }?.id
                    ?: state.inventoryLocations.firstOrNull()?.id
            }
        }
        AlertDialog(
            onDismissRequest = { showStockAdjustDialog = false },
            title = {
                Text(
                    if (directAdjustment) atlasText("Opération d'entrepôt", "Warehouse operation", "عملية المستودع")
                    else atlasText("Demander un changement de stock", "Request a stock change", "طلب تغيير المخزون"),
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(scannedProduct.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        atlasText(
                            "Stock global : ${productStockDisplay(scannedProduct.stock, scannedProduct.unit, scannedProduct.secondaryUnit, scannedProduct.secondaryUnitFactor)}",
                            "Global stock: ${productStockDisplay(scannedProduct.stock, scannedProduct.unit, scannedProduct.secondaryUnit, scannedProduct.secondaryUnitFactor)}",
                            "المخزون الإجمالي: ${productStockDisplay(scannedProduct.stock, scannedProduct.unit, scannedProduct.secondaryUnit, scannedProduct.secondaryUnitFactor)}",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (directAdjustment) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(selected = operationKind == "receive", onClick = { operationKind = "receive" }, label = { Text(atlasText("Réception", "Receive", "استلام")) })
                            FilterChip(selected = operationKind == "issue", onClick = { operationKind = "issue" }, label = { Text(atlasText("Sortie", "Issue", "صرف")) })
                            FilterChip(selected = operationKind == "count", onClick = { operationKind = "count" }, label = { Text(atlasText("Comptage", "Count", "جرد")) })
                        }
                        androidx.compose.foundation.layout.Box {
                            OutlinedButton(onClick = { locationMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedLocation?.name ?: atlasText("Choisir un emplacement", "Choose a location", "اختر موقعاً"))
                            }
                            DropdownMenu(expanded = locationMenuOpen, onDismissRequest = { locationMenuOpen = false }) {
                                state.inventoryLocations.forEach { location ->
                                    DropdownMenuItem(
                                        text = { Text("${location.name} · ${location.code}") },
                                        onClick = { selectedLocationId = location.id; locationMenuOpen = false },
                                    )
                                }
                            }
                        }
                        if (state.inventoryContextLoading) {
                            Text(atlasText("Chargement des emplacements…", "Loading warehouse locations…", "جار تحميل المواقع…"), style = MaterialTheme.typography.bodySmall)
                        }
                        if (operationKind == "count") {
                            Text(
                                observedPlacement?.let { placement ->
                                    atlasText(
                                        "Valeur observée ${AtlasFormat.quantity(placement.quantity)} · version ${placement.revision ?: "?"}",
                                        "Observed ${AtlasFormat.quantity(placement.quantity)} · revision ${placement.revision ?: "?"}",
                                        "الكمية المرصودة ${AtlasFormat.quantity(placement.quantity)} · الإصدار ${placement.revision ?: "?"}",
                                    )
                                } ?: atlasText("Aucune valeur de base en cache pour cet emplacement.", "No cached baseline for this location.", "لا توجد قيمة أساسية محفوظة لهذا الموقع."),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        state.inventoryContextError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    } else {
                        Text(
                            atlasText("La demande sera appliquée après approbation.", "The request will be applied after approval.", "سيتم تطبيق الطلب بعد الموافقة."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = operationKind == "receive", onClick = { operationKind = "receive" }, label = { Text(atlasText("Entrée", "Add", "إضافة")) })
                            FilterChip(selected = operationKind == "issue", onClick = { operationKind = "issue" }, label = { Text(atlasText("Sortie", "Remove", "إزالة")) })
                        }
                    }
                    OutlinedTextField(
                        value = rawStockChange,
                        onValueChange = { value -> rawStockChange = value.filter { it.isDigit() || it == '.' || it == ',' } },
                        label = { Text(if (operationKind == "count") atlasText("Quantité comptée", "Counted quantity", "الكمية المحصاة") else atlasText("Quantité", "Quantity", "الكمية")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = stockReason,
                        onValueChange = { stockReason = it },
                        label = { Text(atlasText("Motif (facultatif)", "Note (optional)", "ملاحظة (اختيارية)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    inventoryQueueReadError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    val pendingCount = queuedInventoryOperations.count { it.status == "QUEUED" }
                    if (pendingCount > 0) {
                        Text(atlasText("$pendingCount opération(s) en attente de synchronisation.", "$pendingCount operation(s) waiting to sync.", "$pendingCount عملية بانتظار المزامنة."), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        parsedStockChange?.let { amount ->
                            if (directAdjustment) {
                                selectedLocationId?.let { locationId -> viewModel.queueInventoryOperation(scannedProduct, operationKind, locationId, amount, stockReason) }
                            } else {
                                viewModel.adjustScannedStock(scannedProduct, amount, operationKind == "issue", stockReason)
                            }
                        }
                        showStockAdjustDialog = false
                    },
                    enabled = quantityValid && (!directAdjustment || (selectedLocation != null && !state.inventoryContextLoading && inventoryQueueReadError == null)) &&
                        (operationKind != "count" || observedPlacement?.revision != null),
                ) {
                    Text(if (directAdjustment) atlasText("Mettre en file", "Queue operation", "إضافة إلى قائمة الانتظار") else atlasText("Envoyer", "Submit", "إرسال"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStockAdjustDialog = false }) {
                    Text(atlasText("Annuler", "Cancel", "إلغاء"))
                }
            },
        )
    }

    if (showDiscardHeldConfirmation) {
        val heldCount = queuedInventoryOperations.count { it.status == "HELD" }
        AlertDialog(
            onDismissRequest = { showDiscardHeldConfirmation = false },
            title = { Text(atlasText("Confirmer la suppression", "Confirm discard", "تأكيد الحذف")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type $heldCount to permanently discard all held writes.")
                    OutlinedTextField(
                        value = typedHeldDiscardCount,
                        onValueChange = { typedHeldDiscardCount = it.filter { character -> character.isDigit() }.take(6) },
                        label = { Text(atlasText("Nombre d'écritures", "Number of writes", "عدد العمليات")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !inventoryQueueReplaying && heldCount > 0 && typedHeldDiscardCount.toIntOrNull() == heldCount,
                    onClick = {
                        val exactCount = typedHeldDiscardCount.toIntOrNull() ?: return@TextButton
                        viewModel.discardHeldInventoryWrites(exactCount)
                        showDiscardHeldConfirmation = false
                    },
                ) { Text(atlasText("Supprimer définitivement", "Discard permanently", "حذف نهائي")) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardHeldConfirmation = false }) {
                    Text(atlasText("Annuler", "Cancel", "إلغاء"))
                }
            },
        )
    }

    if (showCreateDialog) {
        ProductCreateDialog(
            canCreateDirectly = isAdmin,
            initialBarcode = state.lastCode.orEmpty(),
            requesterName = user.displayName.ifBlank { user.username },
            imageServerBaseUrl = user.serverUrl,
            submitting = state.creating,
            onDismiss = {
                showCreateDialog = false
            },
            onSubmitDirect = { req ->
                showCreateDialog = false
                viewModel.createProductFromScan(req)
            },
            onSubmitRequest = { code, name, sku, unit, secondaryUnit, secondaryUnitFactor, qty, cat, note, photoUrl ->
                showCreateDialog = false
                viewModel.submitRequestFromScan(code, name, sku, unit, secondaryUnit, secondaryUnitFactor, qty, cat, note, photoUrl)
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
                                    text = atlasText("QR code de sécurité PC détecté", "PC sign-in QR detected", "تم اكتشاف رمز QR لتسجيل دخول الكمبيوتر"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = atlasText("Cet onglet scanne les articles du stock. Ouvrez le scanner d'appareil PC ou annulez.", "This scanner is for inventory barcodes. Open the PC device scanner or cancel.", "هذا الماسح مخصص لرموز المخزون. افتح ماسح أجهزة الكمبيوتر أو ألغِ العملية."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AtlasButton(
                                text = atlasText("Scanner poste PC", "Open PC device scanner", "فتح ماسح أجهزة الكمبيوتر"),
                                onClick = onOpenQrDevice,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(onClick = onCancelScan) {
                                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(atlasText("Annuler", "Cancel", "إلغاء"))
                            }
                        }
                    }
                }

                state.lookingUp -> {
                    AtlasCard(padding = 12.dp) {
                        AtlasShimmer {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                AtlasSkeletonBlock(Modifier.size(42.dp), RoundedCornerShape(14.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AtlasSkeletonBlock(Modifier.fillMaxWidth(0.8f).height(14.dp))
                                    AtlasSkeletonBlock(Modifier.fillMaxWidth(0.56f).height(11.dp))
                                }
                                OutlinedButton(onClick = onCancelScan) {
                                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(atlasText("Annuler", "Cancel", "إلغاء"))
                                }
                            }
                        }
                    }
                }

                product != null -> {
                    val stockLabel = atlasText(" · Stock ", " · Stock ", " · المخزون ")
                    val shortLabel = atlasText(" (manque ", " (short ", " (النقص ")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = buildString {
                                    append(atlasMoney(product.price))
                                    append(stockLabel)
                                    append(atlasQuantity(product.stock))
                                    if (product.neededQuantity > 0.0) {
                                        append(shortLabel)
                                        append(atlasQuantity(product.neededQuantity))
                                        append(")")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.added) {
                            AtlasChip(text = atlasText("Ajouté", "Added", "تمت الإضافة"), tone = AtlasTone.POSITIVE, icon = Icons.Outlined.CheckCircle)
                        } else {
                            AtlasChip(
                                text = if (product.isLowStock) atlasText("À réappro.", "Restock", "إعادة التخزين") else atlasText("En stock", "In stock", "متوفر"),
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
                                text = if (state.added) atlasText("Ajouter +", "Add +", "إضافة +") else atlasText("Ajouter au panier", "Add to cart", "إضافة إلى السلة"),
                                onClick = onAdd,
                                modifier = Modifier.weight(1f),
                                icon = Icons.Outlined.ShoppingCart,
                            )
                            TextButton(onClick = onOpenSale) { Text(atlasText("Panier", "Cart", "السلة")) }
                        } else {
                            TextButton(
                                onClick = onOpenProducts,
                                modifier = Modifier.weight(1f),
                            ) { Text(atlasText("Voir fiche stock", "View stock item", "عرض تفاصيل المخزون")) }
                        }
                        OutlinedButton(onClick = onCancelScan) {
                            Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(atlasText("Annuler", "Cancel", "إلغاء"))
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
                                    atlasText("Code ${state.lastCode} transmis à l'Administrateur", "Code ${state.lastCode} sent to the administrator", "تم إرسال الرمز ${state.lastCode} إلى المسؤول")
                                } else {
                                    atlasText("Code ${state.lastCode} introuvable", "Code ${state.lastCode} not found", "لم يتم العثور على الرمز ${state.lastCode}")
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (state.pendingApproval) {
                                    atlasText("La demande est en attente de validation Admin. Vous pouvez continuer ou annuler ce scan.", "The request is waiting for administrator approval. You can continue or cancel this scan.", "الطلب بانتظار موافقة المسؤول. يمكنك المتابعة أو إلغاء هذا المسح.")
                                } else if (isAdmin) {
                                    atlasText("En tant qu'Admin, vous pouvez créer cet article directement ou annuler ce scan.", "As an administrator, you can create this item directly or cancel this scan.", "بصفتك مسؤولًا، يمكنك إنشاء هذا الصنف مباشرة أو إلغاء المسح.")
                                } else {
                                    atlasText("Cet article sera envoyé immédiatement à l'Administrateur pour création, ou appuyez sur Annuler.", "This item will be sent to the administrator for creation, or tap Cancel.", "سيُرسل هذا الصنف إلى المسؤول لإنشائه، أو اضغط إلغاء.")
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
                                text = if (isAdmin) atlasText("Créer le produit", "Create product", "إنشاء منتج") else atlasText("Envoyer à l'Admin", "Send to admin", "إرسال إلى المسؤول"),
                                onClick = onOpenCreateDialog,
                                icon = if (isAdmin) Icons.Outlined.Add else Icons.Outlined.Send,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            TextButton(
                                onClick = onOpenProducts,
                                modifier = Modifier.weight(1f),
                            ) { Text(atlasText("Voir catalogue", "View catalog", "عرض الكتالوج")) }
                        }
                        OutlinedButton(onClick = onCancelScan) {
                            Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(atlasText("Annuler", "Cancel", "إلغاء"))
                        }
                    }
                }

                state.error != null -> {
                    AtlasErrorCard(
                        title = atlasText("Recherche impossible", "Search failed", "تعذّر البحث"),
                        message = atlasNotice(state.error.orEmpty(), LocalAtlasLanguage.current),
                        onRetry = onRetry,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onCancelScan,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(atlasText("Annuler", "Cancel", "إلغاء"))
                    }
                }

                else -> {
                    Text(
                        text = atlasText("Scanner un article du stock", "Scan an inventory item", "مسح صنف من المخزون"),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = atlasText("Scannez un code-barres EAN / SKU pour vérifier ou vendre, ou annulez à tout moment si vous ne souhaitez pas enregistrer l'objet."),
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
            label = atlasText("Saisie manuelle", "Manual entry", "إدخال يدوي"),
            placeholder = atlasText("Code-barres / SKU", "Barcode / SKU", "الرمز الشريطي / SKU"),
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
            Text(atlasText("Chercher", "Search", "بحث"))
        }
        if (code.isNotBlank() || hasActiveScan) {
            IconButton(
                onClick = {
                    code = ""
                    if (hasActiveScan) onClearScan()
                },
            ) {
                Icon(Icons.Outlined.Close, contentDescription = atlasText("Effacer et annuler", "Clear and cancel", "مسح وإلغاء"))
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
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = atlasText("Accès caméra requis", "Camera access required", "يلزم الوصول إلى الكاميرا"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = atlasText("La caméra sert uniquement à lire les codes-barres et se coupe automatiquement dès que vous quittez l'onglet Scan.", "The camera is used only to read barcodes and turns off automatically when you leave the Scan tab.", "تُستخدم الكاميرا لقراءة الرموز الشريطية فقط، وتتوقف تلقائيًا عند مغادرة علامة تبويب المسح."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        AtlasButton(text = atlasText("Autoriser la caméra", "Allow camera", "السماح بالكاميرا"), onClick = onRequest)
        if (askedOnce) {
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onOpenSettings) {
                Text(atlasText("Ouvrir les réglages", "Open settings", "فتح الإعدادات"), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
