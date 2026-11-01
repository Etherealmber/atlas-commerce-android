package ma.etherealmber.atlascommerce.ui.screens.products

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.model.ProductMovementDto
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasSkeletonList
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasQuantity
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

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
    onOpenStockAdjust: () -> Unit,
    onBack: () -> Unit,
    onProductImageUpdated: (String) -> Unit,
    onToast: (String) -> Unit,
    viewModel: ProductHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var displayedImageUrl by remember(product.id) { mutableStateOf(product.imageUrl) }
    var loadedStock by remember(product.id) { mutableStateOf(product.stock) }
    val imageUpdatedMessage = atlasText("Photo du produit mise à jour.", "Product image updated.", "تم تحديث صورة المنتج.")
    val imageReadErrorMessage = atlasText("Impossible de lire cette image.", "Could not read this image.", "تعذرت قراءة هذه الصورة.")

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
                runCatching { productImageDataUrl(context, uri) }
                    .onSuccess { dataUrl -> viewModel.setPrimaryImage(product.id, dataUrl, product.name) }
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

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 18.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = atlasText("Retour", "Back", "رجوع"))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    atlasText("Historique du produit", "Product history", "سجل المنتج"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    atlasText("Détails et mouvements de stock", "Product details and stock movements", "تفاصيل المنتج وحركات المخزون"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AtlasDivider()
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
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
                if (!state.imageError.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.imageError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtlasChip(
                        text = atlasText(
                            "Stock ${productStockDisplay(product.stock, product.unit)}",
                            "Stock ${productStockDisplay(product.stock, product.unit)}",
                            "المخزون ${productStockDisplay(product.stock, product.unit)}",
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
                    IconButton(onClick = { viewModel.loadHistory(product.id, refresh = true) }, enabled = !state.loading) {
                        Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser l'historique", "Refresh history", "تحديث السجل"))
                    }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    state.loading && state.movements.isEmpty() -> AtlasSkeletonList(Modifier.fillMaxWidth(), rows = 3, avatarSize = 28.dp)
                    !state.error.isNullOrBlank() && state.movements.isEmpty() -> AtlasErrorCard(
                        message = state.error.orEmpty(),
                        onRetry = { viewModel.loadHistory(product.id, refresh = true) },
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
        val details = listOfNotNull(movement.reason, movement.reference, movement.createdBy)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (details.isNotBlank()) {
            Text(details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private suspend fun productImageDataUrl(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: throw IOException("Impossible de lire cette image.")
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Format d'image illisible.")

    var sample = 1
    while (max(bounds.outWidth / sample, bounds.outHeight / sample) > 1800) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    var bitmap: Bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: throw IOException("Impossible de décoder cette image.")
    val scale = minOf(1f, 1600f / max(bitmap.width, bitmap.height))
    if (scale < 1f) {
        val resized = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1),
            true,
        )
        if (resized !== bitmap) bitmap.recycle()
        bitmap = resized
    }

    var bytes: ByteArray? = null
    for (quality in listOf(88, 80, 72, 64)) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        bytes = stream.toByteArray()
        if (bytes.size <= 3_500_000) break
    }
    bitmap.recycle()
    val encoded = bytes?.takeIf { it.size <= 3_500_000 } ?: throw IOException("Image trop lourde après compression (maximum 3,5 Mo).")
    "data:image/jpeg;base64,${Base64.encodeToString(encoded, Base64.NO_WRAP)}"
}
