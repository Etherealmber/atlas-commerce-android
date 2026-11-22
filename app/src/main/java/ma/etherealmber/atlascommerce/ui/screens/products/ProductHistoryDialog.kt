package ma.etherealmber.atlascommerce.ui.screens.products

import ma.etherealmber.atlascommerce.ui.components.rememberApiRefreshAction

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.model.ProductMovementDto
import ma.etherealmber.atlascommerce.ui.components.encodeProductImageDataUri
import ma.etherealmber.atlascommerce.ui.components.ProductArtwork
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasFloatingTopBar
import ma.etherealmber.atlascommerce.ui.components.rememberAtlasFloatingTopBarScrollConnection
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasSkeletonList
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasQuantity
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

/** Full-page product details and stock history, backed by the commerce movement endpoint. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ProductHistoryScreen(
    product: ProductDto,
    currency: String,
    serverBaseUrl: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    canEditImage: Boolean,
    canAdjustStock: Boolean,
    canDeleteProduct: Boolean,
    onOpenStockAdjust: () -> Unit,
    onBack: () -> Unit,
    onProductImageUpdated: (String) -> Unit,
    onProductDeleted: () -> Unit,
    onToast: (String) -> Unit,
    viewModel: ProductHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshProductHistory = rememberApiRefreshAction(
        key = "product-history-${product.id}",
        cooldownMs = 5_000L,
        onRefresh = { viewModel.loadHistory(product.id, refresh = true) },
    )
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var displayedImageUrl by remember(product.id) { mutableStateOf(product.imageUrl) }
    var loadedStock by remember(product.id) { mutableStateOf(product.stock) }
    var confirmDelete by remember(product.id) { mutableStateOf(false) }
    var deletePassword by remember(product.id) { mutableStateOf("") }
    val imageUpdatedMessage = atlasText("Photo du produit mise à jour.", "Product image updated.", "تم تحديث صورة المنتج.")
    val imageReadErrorMessage = atlasText("Impossible de lire cette image.", "Could not read this image.", "تعذرت قراءة هذه الصورة.")
    val productDeletedMessage = atlasText("Produit supprimé ou archivé.", "Product deleted or archived.", "تم حذف المنتج أو أرشفته.")

    LaunchedEffect(product.id, state.scopeIdentity) {
        viewModel.loadHistory(product.id)
    }
    LaunchedEffect(product.stock) {
        if (loadedStock != product.stock) {
            loadedStock = product.stock
            viewModel.loadHistory(product.id, refresh = true)
        }
    }
    LaunchedEffect(product.imageUrl) {
        displayedImageUrl = product.imageUrl
    }
    LaunchedEffect(state.updatedImageUrl) {
        val updatedUrl = state.updatedImageUrl
        if (!updatedUrl.isNullOrBlank()) {
            displayedImageUrl = updatedUrl
            onProductImageUpdated(updatedUrl)
            onToast(imageUpdatedMessage)
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    encodeProductImageDataUri(context, uri)
                        ?: error("Impossible d'encoder cette image.")
                }.onSuccess { dataUrl -> viewModel.setPrimaryImage(product.id, dataUrl, product.name) }
                    .onFailure {
                        onToast(imageReadErrorMessage)
                    }
            }
        }
    }

    val sharedArtworkModifier = if (motionEnabled) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                state = rememberSharedContentState(key = "product-artwork-${product.id}"),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }

    var headerExpanded by remember(product.id) { mutableStateOf(true) }
    val headerScrollConnection = rememberAtlasFloatingTopBarScrollConnection(
        expanded = headerExpanded,
        onExpandedChange = { headerExpanded = it },
    )

    Box(Modifier.fillMaxSize().nestedScroll(headerScrollConnection)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 100.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
            AtlasCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(76.dp)) {
                        ProductHeroArtwork(
                            imageUrl = displayedImageUrl,
                            serverBaseUrl = serverBaseUrl,
                            label = product.name,
                            modifier = Modifier.size(76.dp).then(sharedArtworkModifier),
                            motionEnabled = motionEnabled,
                            productId = product.id,
                        )
                        if (canEditImage) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shadowElevation = 3.dp,
                            ) {
                                IconButton(
                                    onClick = { if (!state.imageSaving) photoPicker.launch("image/*") },
                                    enabled = !state.imageSaving,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    if (state.imageSaving) {
                                        androidx.compose.material3.CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(
                                            Icons.Outlined.Edit,
                                            contentDescription = atlasText("Modifier la photo du produit", "Edit product image", "تعديل صورة المنتج"),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        ) {
                            Text(
                                atlasText("Fiche produit", "Product overview", "ملف المنتج"),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (product.subtitle.isNotBlank()) {
                            Text(
                                product.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                val creator = product.createdBy?.takeIf { it.isNotBlank() }
                val approver = product.approvedBy?.takeIf { it.isNotBlank() }
                val productAttribution = when {
                    creator != null && approver != null -> atlasText(
                        "Créé par $creator · Approuvé par $approver",
                        "Created by $creator · Approved by $approver",
                        "أنشأه $creator · اعتمده $approver",
                    )
                    creator != null -> atlasText("Créé par $creator", "Created by $creator", "أنشأه $creator")
                    approver != null -> atlasText("Approuvé par $approver", "Approved by $approver", "اعتمده $approver")
                    else -> null
                }
                if (productAttribution != null) {
                    Text(
                        productAttribution,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!state.imageError.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.imageError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtlasChip(
                        text = atlasText(
                            "Stock ${productStockDisplay(product.stock, product.unit, product.secondaryUnit, product.secondaryUnitFactor)}",
                            "Stock ${productStockDisplay(product.stock, product.unit, product.secondaryUnit, product.secondaryUnitFactor)}",
                            "المخزون ${productStockDisplay(product.stock, product.unit, product.secondaryUnit, product.secondaryUnitFactor)}",
                        ),
                        tone = productStockTone(product.stock, product.trackInventory),
                    )
                    if (product.price > 0.0) AtlasChip(text = atlasMoney(product.price, currency))
                }
                if (canAdjustStock && product.trackInventory) {
                    TextButton(onClick = onOpenStockAdjust) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(atlasText("Modifier le stock", "Edit stock", "تعديل المخزون"))
                    }
                }
            }

            AtlasCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        atlasText("Historique des mouvements", "Movement history", "سجل الحركات"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (!state.loading && state.error == null) {
                        AtlasChip(text = atlasQuantity(state.movements.size.toDouble()), tone = AtlasTone.NEUTRAL)
                    }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    state.loading && state.movements.isEmpty() -> AtlasSkeletonList(Modifier.fillMaxWidth(), rows = 3, avatarSize = 28.dp)
                    !state.error.isNullOrBlank() && state.movements.isEmpty() -> AtlasErrorCard(
                        message = state.error.orEmpty(),
                        onRetry = refreshProductHistory,
                    )
                    state.movements.isEmpty() -> Text(
                        atlasText("Aucun mouvement enregistré pour ce produit.", "No movements recorded for this product.", "لا توجد حركات مسجلة لهذا المنتج."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> {
                        state.movements.forEachIndexed { index, movement ->
                            if (index > 0) AtlasDivider(Modifier.padding(vertical = 8.dp))
                            ProductMovementRow(movement)
                        }
                        if (state.loadingMore) {
                            Spacer(Modifier.height(8.dp))
                            AtlasSkeletonList(Modifier.fillMaxWidth(), rows = 1, avatarSize = 28.dp)
                        } else if (state.canLoadMore) {
                            TextButton(
                                onClick = viewModel::loadMoreHistory,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(atlasText("Charger la suite", "Load more", "تحميل المزيد"))
                            }
                        }
                        if (!state.error.isNullOrBlank()) {
                            Text(state.error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        }
        AtlasFloatingTopBar(
            title = atlasText("Historique du produit", "Product history", "سجل المنتج"),
            subtitle = atlasText("Détails et mouvements de stock", "Product details and stock movements", "تفاصيل المنتج وحركات المخزون"),
            onBack = onBack,
            expanded = headerExpanded,
            onCollapsedClick = { headerExpanded = true },
            modifier = Modifier.align(Alignment.TopCenter).zIndex(1f),
            actions = {
                IconButton(onClick = refreshProductHistory, enabled = !state.loading) {
                    if (state.loading) androidx.compose.material3.CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser l'historique", "Refresh history", "تحديث السجل"))
                }
                if (canDeleteProduct) {
                    IconButton(onClick = { deletePassword = ""; confirmDelete = true }, enabled = !state.deletingProduct) {
                        Icon(Icons.Outlined.Delete, contentDescription = atlasText("Supprimer le produit", "Delete product", "حذف المنتج"), tint = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!state.deletingProduct) { confirmDelete = false; deletePassword = "" } },
            title = { Text(atlasText("Supprimer le produit ?", "Delete product?", "حذف المنتج؟")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(atlasText(
                        "Confirmez avec votre mot de passe administrateur. Le produit sera supprimé s’il n’a pas d’historique; sinon, le serveur l’archivera immédiatement et conservera les transactions. Aucun délai d’annulation n’est appliqué.",
                        "Confirm with your administrator password. The product is deleted if it has no history; otherwise, the server archives it immediately and preserves transactions. There is no undo countdown.",
                        "أكّد باستخدام كلمة مرور المسؤول. سيُحذف المنتج إذا لم يكن له سجل؛ وإلا فسيؤرشفه الخادم فوراً مع الاحتفاظ بالمعاملات. لا توجد مهلة للتراجع.",
                    ))
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        label = { Text(atlasText("Mot de passe administrateur", "Administrator password", "كلمة مرور المسؤول")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !state.deletingProduct,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.error?.takeIf(String::isNotBlank)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.deletingProduct && deletePassword.isNotBlank(),
                    onClick = {
                        viewModel.deleteProduct(product.id, deletePassword) { deleted, message ->
                            confirmDelete = false
                            deletePassword = ""
                            if (deleted) {
                                onToast(message ?: productDeletedMessage)
                                onProductDeleted()
                            } else if (!message.isNullOrBlank()) onToast(message)
                        }
                    },
                ) {
                    if (state.deletingProduct) androidx.compose.material3.CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text(atlasText("Confirmer la suppression", "Confirm deletion", "تأكيد الحذف"))
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }, enabled = !state.deletingProduct) { Text(atlasText("Annuler", "Cancel", "إلغاء")) } },
        )
    }
}

@Composable
private fun ProductHeroArtwork(
    imageUrl: String?,
    serverBaseUrl: String,
    label: String,
    modifier: Modifier,
    motionEnabled: Boolean,
    productId: Long,
) {
    AnimatedVisibility(
        visible = true,
        enter = if (motionEnabled) {
            fadeIn() + scaleIn(initialScale = 0.82f, animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f)) +
                slideInVertically(initialOffsetY = { (it * 0.16f).toInt() })
        } else EnterTransition.None,
        label = "product-hero-image-$productId",
    ) {
        ProductArtwork(
            imageUrl = imageUrl,
            serverBaseUrl = serverBaseUrl,
            label = label,
            modifier = modifier,
        )
    }
}

@Composable
private fun ProductMovementRow(movement: ProductMovementDto) {
    val movementLabel = when (movement.type.lowercase()) {
        "initial" -> atlasText("Stock initial", "Initial stock", "المخزون الأولي")
        "sale" -> atlasText("Vente", "Sale", "بيع")
        "purchase" -> atlasText("Achat", "Purchase", "شراء")
        "restock", "refund_restock" -> atlasText("Réapprovisionnement", "Restock", "إعادة التزويد")
        "in" -> atlasText("Entrée", "Stock in", "إدخال")
        "out" -> atlasText("Sortie", "Stock out", "إخراج")
        "adjustment" -> atlasText("Ajustement", "Adjustment", "تعديل")
        else -> if (movement.type.isBlank()) atlasText("Mouvement", "Movement", "حركة") else movement.type
    }
    val timestamp = movement.createdAt?.replace('T', ' ')?.take(16)?.ifBlank { "—" } ?: "—"
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(movementLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            atlasText(
                "Qté ${atlasQuantity(movement.quantity)} · ${atlasQuantity(movement.previousQuantity)} → ${atlasQuantity(movement.newQuantity)}",
                "Qty ${atlasQuantity(movement.quantity)} · ${atlasQuantity(movement.previousQuantity)} → ${atlasQuantity(movement.newQuantity)}",
                "الكمية ${atlasQuantity(movement.quantity)} · ${atlasQuantity(movement.previousQuantity)} ← ${atlasQuantity(movement.newQuantity)}",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val details = listOfNotNull(movement.reason, movement.reference)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (details.isNotBlank()) {
            Text(details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val actor = movement.createdBy?.takeIf { it.isNotBlank() }
        val approver = movement.approvedBy?.takeIf { it.isNotBlank() }
        val movementAttribution = when {
            actor != null && approver != null -> atlasText(
                "Créé par $actor · Approuvé par $approver",
                "Created by $actor · Approved by $approver",
                "أنشأه $actor · اعتمده $approver",
            )
            actor != null -> atlasText("Créé par $actor", "Created by $actor", "أنشأه $actor")
            approver != null -> atlasText("Approuvé par $approver", "Approved by $approver", "اعتمده $approver")
            else -> null
        }
        if (movementAttribution != null) {
            Text(
                movementAttribution,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
