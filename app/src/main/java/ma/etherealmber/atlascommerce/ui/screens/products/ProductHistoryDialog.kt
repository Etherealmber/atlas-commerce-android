package ma.etherealmber.atlascommerce.ui.screens.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasQuantity
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

/** Full-page product history destination. Actual movement data needs a server history endpoint. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ProductHistoryScreen(
    product: ProductDto,
    currency: String,
    serverBaseUrl: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
) {
    val sharedArtworkModifier = if (LocalAtlasAnimationsEnabled.current) {
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
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn(initialScale = 0.82f, animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f)) +
                            slideInVertically(initialOffsetY = { (it * 0.16f).toInt() }),
                        label = "product-hero-image-${product.id}",
                    ) {
                        ProductArtwork(
                            imageUrl = product.imageUrl,
                            serverBaseUrl = serverBaseUrl,
                            label = product.name,
                            modifier = Modifier.size(76.dp).then(sharedArtworkModifier),
                        )
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
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtlasChip(
                        text = atlasText("Stock ${atlasQuantity(product.stock)}", "Stock ${atlasQuantity(product.stock)}", "المخزون ${atlasQuantity(product.stock)}"),
                        tone = when {
                            product.isOutOfStock -> AtlasTone.DANGER
                            product.isLowStock -> AtlasTone.WARNING
                            else -> AtlasTone.POSITIVE
                        },
                    )
                    if (product.price > 0.0) AtlasChip(text = atlasMoney(product.price, currency))
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
                    AtlasChip(
                        text = atlasText("Non disponible", "Unavailable", "غير متاح"),
                        tone = AtlasTone.NEUTRAL,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    atlasText(
                        "L'historique des mouvements n'est pas encore disponible sur ce serveur. Le stock actuel reste affiché ci-dessus.",
                        "Movement history isn't available from this server yet. Current stock is shown above.",
                        "سجل الحركات غير متاح على هذا الخادم بعد. يظل المخزون الحالي ظاهراً أعلاه.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
