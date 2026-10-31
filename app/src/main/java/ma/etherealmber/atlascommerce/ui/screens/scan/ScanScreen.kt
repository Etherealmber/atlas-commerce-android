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
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
                        text = atlasText("Caméra en veille", "Camera is paused", "الكاميرا متوقفة مؤقتًا"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = atlasText("La caméra ne s'ouvre plus automatiquement. Appuyez ci-dessous pour scanner un code-barres, ou saisissez le code manuellement.", "The camera will no longer open automatically. Tap below to scan a barcode, or enter the code manually.", "لن تفتح الكاميرا تلقائيًا بعد الآن. اضغط أدناه لمسح الرمز الشريطي أو أدخل الرمز يدويًا."),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
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
                                contentDescription = atlasText("Annuler le scan", "Cancel scan", "إلغاء المسح"),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = atlasText("Annuler", "Cancel", "إلغاء"),
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
                                text = if (isAdmin) atlasText("Nouveau produit", "New product", "منتج جديد") else atlasText("Demander à l'Admin", "Ask an admin", "طلب من المسؤول"),
                                color = Color.White,
                            )
                        }
                        if (hardwareActive) {
                            IconButton(onClick = { viewModel.setCameraEnabled(false) }) {
                                Icon(
                                    imageVector = Icons.Outlined.VideocamOff,
                                    contentDescription = atlasText("Fermer la caméra", "Close camera", "إغلاق الكاميرا"),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = atlasText("Recherche de ${state.lastCode}…", "Searching for ${state.lastCode}…", "جارٍ البحث عن ${state.lastCode}…"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = onCancelScan) {
                            Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(atlasText("Annuler", "Cancel", "إلغاء"))
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
            tint = Color.White,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = atlasText("Accès caméra requis", "Camera access required", "يلزم الوصول إلى الكاميرا"),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = atlasText("La caméra sert uniquement à lire les codes-barres et se coupe automatiquement dès que vous quittez l'onglet Scan.", "The camera is used only to read barcodes and turns off automatically when you leave the Scan tab.", "تُستخدم الكاميرا لقراءة الرموز الشريطية فقط، وتتوقف تلقائيًا عند مغادرة علامة تبويب المسح."),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(14.dp))
        AtlasButton(text = atlasText("Autoriser la caméra", "Allow camera", "السماح بالكاميرا"), onClick = onRequest)
        if (askedOnce) {
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onOpenSettings) {
                Text(atlasText("Ouvrir les réglages", "Open settings", "فتح الإعدادات"), color = Color.White)
            }
        }
    }
}
