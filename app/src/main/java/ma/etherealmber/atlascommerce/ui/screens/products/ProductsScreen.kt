package ma.etherealmber.atlascommerce.ui.screens.products

import ma.etherealmber.atlascommerce.ui.i18n.atlasRoleLabel
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasQuantity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.ProductCreationRequestDto
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingDockPadding
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasCatalogSkeleton
import ma.etherealmber.atlascommerce.ui.components.AtlasOfflineBanner
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.ProductCreateDialog
import ma.etherealmber.atlascommerce.ui.theme.Atlas

@Composable
fun ProductsScreen(
    user: SessionUser,
    onOpenScan: () -> Unit,
    onOpenSale: () -> Unit,
    onToast: (String) -> Unit,
    viewModel: ProductsViewModel = hiltViewModel(),
) {
    val dockPadding = LocalAtlasFloatingDockPadding.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val density = Atlas.density
    var selected by remember { mutableStateOf<ProductDto?>(null) }
    var historyTarget by remember { mutableStateOf<ProductDto?>(null) }
    var adjustTarget by remember { mutableStateOf<ProductDto?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingFulfillRequest by remember { mutableStateOf<ProductCreationRequestDto?>(null) }

    val isAdmin = Roles.canCreateProductDirectly(user.role)
    val canSell = Roles.canCreateDocuments(user.role)
    val canAdjustStock = Roles.canAdjustStock(user.role)

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message -> onToast(message) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = density.screenPadding, vertical = density.itemGap),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 460.dp
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AtlasTextField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            label = atlasText("Rechercher un produit"),
                            placeholder = atlasText("Nom, SKU ou code-barres"),
                            leadingIcon = Icons.Outlined.Search,
                            imeAction = ImeAction.Search,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showCreateDialog = true }) {
                                Icon(if (isAdmin) Icons.Outlined.Add else Icons.Outlined.Send, contentDescription = if (isAdmin) atlasText("Créer un produit") else atlasText("Demander un produit à l'Admin"))
                            }
                            IconButton(onClick = onOpenScan) { Icon(Icons.Outlined.QrCodeScanner, contentDescription = atlasText("Scanner un code-barres")) }
                            IconButton(onClick = viewModel::refresh) { Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser")) }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AtlasTextField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            label = atlasText("Rechercher un produit"),
                            placeholder = atlasText("Nom, SKU ou code-barres"),
                            leadingIcon = Icons.Outlined.Search,
                            imeAction = ImeAction.Search,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(if (isAdmin) Icons.Outlined.Add else Icons.Outlined.Send, contentDescription = if (isAdmin) atlasText("Créer un produit") else atlasText("Demander un produit à l'Admin"))
                        }
                        IconButton(onClick = onOpenScan) { Icon(Icons.Outlined.QrCodeScanner, contentDescription = atlasText("Scanner un code-barres")) }
                        IconButton(onClick = viewModel::refresh) { Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser")) }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = state.filter == ProductsViewModel.FilterMode.ALL,
                    onClick = { viewModel.setFilter(ProductsViewModel.FilterMode.ALL) },
                    label = { Text(atlasText("Tous")) },
                )
                FilterChip(
                    selected = state.filter == ProductsViewModel.FilterMode.NEEDED,
                    onClick = { viewModel.setFilter(ProductsViewModel.FilterMode.NEEDED) },
                    label = { Text(atlasText("À réappro. (${state.lowStockCount})", "Low stock (${state.lowStockCount})", "مخزون منخفض (${state.lowStockCount})")) },
                )
                FilterChip(
                    selected = state.filter == ProductsViewModel.FilterMode.REQUESTS,
                    onClick = { viewModel.setFilter(ProductsViewModel.FilterMode.REQUESTS) },
                    label = {
                        Text(
                            if (isAdmin) {
                                atlasText("À valider (${state.openRequests.size})", "To approve (${state.openRequests.size})", "بانتظار الموافقة (${state.openRequests.size})")
                            } else {
                                atlasText("Mes demandes (${state.openRequests.size})", "My requests (${state.openRequests.size})", "طلباتي (${state.openRequests.size})")
                            },
                        )
                    },
                )
            }
            if (state.filter != ProductsViewModel.FilterMode.REQUESTS && !state.loading) {
                Text(
                    atlasText(
                        "${state.visibleProducts.size} résultat${if (state.visibleProducts.size == 1) "" else "s"} · catalogue en direct",
                        "${state.visibleProducts.size} results · live catalog",
                        "${state.visibleProducts.size} نتيجة · الكتالوج المباشر",
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.offline) {
            AtlasOfflineBanner(modifier = Modifier.padding(horizontal = density.screenPadding))
            Spacer(Modifier.height(8.dp))
        }

        when {
            state.loading -> AtlasCatalogSkeleton(Modifier.fillMaxSize(), rows = 6)

            !state.error.isNullOrBlank() -> {
                Column(Modifier.padding(horizontal = density.screenPadding)) {
                    AtlasErrorCard(message = atlasNotice(state.error.orEmpty(), LocalAtlasLanguage.current), onRetry = viewModel::refresh)
                }
            }

            state.filter == ProductsViewModel.FilterMode.REQUESTS -> {
                if (state.openRequests.isEmpty()) {
                    AtlasEmptyState(
                        title = atlasText("Aucune demande en attente"),
                        message = if (isAdmin) {
                            atlasText("Les demandes d'ajout envoyées par l'équipe apparaissent ici pour création immédiate.")
                        } else {
                            atlasText("Vos fiches produits transmises à l'Administrateur apparaissent ici.")
                        },
                        icon = Icons.Outlined.Inventory2,
                        actionLabel = if (isAdmin) atlasText("Créer un produit") else atlasText("Demander à l'Admin"),
                        onAction = { showCreateDialog = true },
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 320.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = density.screenPadding, end = density.screenPadding, bottom = 24.dp + dockPadding),
                        horizontalArrangement = Arrangement.spacedBy(density.itemGap),
                        verticalArrangement = Arrangement.spacedBy(density.itemGap),
                    ) {
                        gridItemsIndexed(state.openRequests, key = { _, req -> req.id }) { index, req ->
                            StaggeredCatalogEntry(index = index) {
                                ProductRequestRow(
                                    request = req,
                                    isAdmin = isAdmin,
                                    serverBaseUrl = user.serverUrl,
                                    onFulfill = { pendingFulfillRequest = req },
                                    onDismiss = { viewModel.dismissProductRequest(req.id) },
                                )
                            }
                        }
                    }
                }
            }

            state.visibleProducts.isEmpty() -> AtlasEmptyState(
                title = if (state.query.isBlank()) atlasText("Aucun produit") else atlasText("Aucun résultat"),
                message = if (state.query.isBlank()) {
                    if (isAdmin) {
                        atlasText("Créez un produit directement avec le bouton + ou scannez son code-barres.")
                    } else {
                        atlasText("Envoyez une demande de création à l'Administrateur avec le bouton d'envoi.")
                    }
                } else {
                    atlasText("Essayez un autre nom, SKU ou code-barres.")
                },
                icon = Icons.Outlined.Inventory2,
                actionLabel = if (isAdmin) atlasText("Créer un produit") else atlasText("Demander à l'Admin"),
                onAction = { showCreateDialog = true },
            )

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 320.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = density.screenPadding, end = density.screenPadding, bottom = 24.dp + dockPadding),
                    horizontalArrangement = Arrangement.spacedBy(density.itemGap),
                    verticalArrangement = Arrangement.spacedBy(density.itemGap),
                ) {
                    if (!isAdmin) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            AtlasCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AtlasChip(text = atlasRoleLabel(user.role), tone = AtlasTone.INFO)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = atlasText("Nouveaux produits soumis à validation Admin"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = Roles.scopeSummary(user.role),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    gridItemsIndexed(state.visibleProducts, key = { _, product -> product.id }) { index, product ->
                        StaggeredCatalogEntry(index = index) {
                            ProductRow(
                                product = product,
                                currency = state.currency,
                                serverBaseUrl = user.serverUrl,
                                canSell = canSell,
                                onOpen = {
                                    if (canSell) historyTarget = product else selected = product
                                },
                                onAdd = {
                                    if (canSell) {
                                        viewModel.addToCart(product)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ProductCreateDialog(
            canCreateDirectly = isAdmin,
            initialBarcode = state.query.trim().takeIf { it.all(Char::isDigit) && it.length >= 6 }.orEmpty(),
            imageServerBaseUrl = user.serverUrl,
            submitting = state.submitting,
            onDismiss = { showCreateDialog = false },
            onSubmitDirect = { req ->
                showCreateDialog = false
                viewModel.createProduct(req)
            },
            onSubmitRequest = { code, name, sku, unit, qty, cat, note, photoUrl ->
                showCreateDialog = false
                viewModel.submitProductRequest(code, name, sku, unit, qty, cat, note, photoUrl)
            },
        )
    }

    val fulfill = pendingFulfillRequest
    if (fulfill != null && isAdmin) {
        ProductCreateDialog(
            canCreateDirectly = true,
            initialBarcode = fulfill.code,
            initialName = fulfill.name.orEmpty(),
            initialSku = fulfill.sku.orEmpty(),
            initialUnit = fulfill.unit ?: "unité",
            initialQuantity = fulfill.quantity?.let { atlasQuantity(it) } ?: "1",
            initialCategory = fulfill.category.orEmpty(),
            initialImageUrl = fulfill.photoUrl,
            imageServerBaseUrl = user.serverUrl,
            submitting = state.submitting,
            onDismiss = { pendingFulfillRequest = null },
            onSubmitDirect = { req ->
                pendingFulfillRequest = null
                viewModel.createProduct(req, fulfillingRequestId = fulfill.id)
            },
            onSubmitRequest = { code, name, sku, unit, qty, cat, note, photoUrl ->
                pendingFulfillRequest = null
                viewModel.submitProductRequest(code, name, sku, unit, qty, cat, note, photoUrl)
            },
        )
    }

    val historyProduct = historyTarget
    if (historyProduct != null && canSell) {
        ProductHistoryDialog(
            product = historyProduct,
            currency = state.currency,
            serverBaseUrl = user.serverUrl,
            onDismiss = { historyTarget = null },
        )
    }

    val product = selected
    if (product != null) {
        ProductDetailDialog(
            product = product,
            currency = state.currency,
            canSell = canSell,
            canAdjustStock = canAdjustStock,
            onDismiss = { selected = null },
            onAdd = { quantity ->
                viewModel.addToCart(product, quantity)
                selected = null
            },
            onOpenSale = {
                selected = null
                onOpenSale()
            },
            onAdjustStock = {
                adjustTarget = product
                selected = null
            },
        )
    }

    val target = adjustTarget
    if (target != null && canAdjustStock) {
        StockAdjustDialog(
            product = target,
            onDismiss = { adjustTarget = null },
            onSubmit = { quantity, out, reason ->
                viewModel.adjustStock(target, quantity, out, reason)
                adjustTarget = null
            },
        )
    }
}

@Composable
private fun StaggeredCatalogEntry(
    index: Int,
    content: @Composable () -> Unit,
) {
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    val delayMillis = if (index in 1..8) index * 80 else 0
    AnimatedVisibility(
        visible = true,
        enter = if (motionEnabled) {
            fadeIn(animationSpec = tween(durationMillis = 280, delayMillis = delayMillis)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 280, delayMillis = delayMillis),
                    initialOffsetY = { height -> (height * 0.2f).roundToInt() },
                )
        } else EnterTransition.None,
    ) {
        content()
    }
}

@Composable
private fun ProductRequestRow(
    request: ProductCreationRequestDto,
    isAdmin: Boolean,
    serverBaseUrl: String,
    onFulfill: () -> Unit,
    onDismiss: () -> Unit,
) {
    AtlasCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProductArtwork(
                imageUrl = request.photoUrl,
                serverBaseUrl = serverBaseUrl,
                label = request.name ?: request.code,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = request.name?.takeIf { it.isNotBlank() } ?: atlasText("Code ${request.code}", "Code ${request.code}", "الرمز ${request.code}"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = listOfNotNull(
                        atlasText("Code ${request.code}", "Code ${request.code}", "الرمز ${request.code}"),
                        request.sku?.takeIf { it.isNotBlank() }?.let { "SKU $it" },
                        request.quantity?.let {
                            "${atlasQuantity(it)} ${request.unit ?: atlasText("unité", "unit", "وحدة")}"
                        },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!request.note.isNullOrBlank()) {
                    Text(
                        text = request.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AtlasChip(
                text = if (isAdmin) atlasText("À valider") else atlasText("Chez l'Admin"),
                tone = AtlasTone.WARNING,
            )
        }
        if (isAdmin) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onFulfill) { Text(atlasText("Créer au stock")) }
                TextButton(onClick = onDismiss) { Text(atlasText("Refuser")) }
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: ProductDto,
    currency: String,
    serverBaseUrl: String,
    canSell: Boolean,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
) {
    AtlasCard(onClick = onOpen, padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductArtwork(
                    imageUrl = product.imageUrl,
                    serverBaseUrl = serverBaseUrl,
                    label = product.name,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (product.subtitle.isNotBlank()) {
                        Text(product.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (canSell) {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Outlined.AddShoppingCart, contentDescription = atlasText("Ajouter au panier"), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AtlasChip(
                    text = atlasText("Stock ${atlasQuantity(product.stock)}", "Stock ${atlasQuantity(product.stock)}", "المخزون ${atlasQuantity(product.stock)}"),
                    tone = when {
                        product.isOutOfStock -> AtlasTone.DANGER
                        product.isLowStock -> AtlasTone.WARNING
                        else -> AtlasTone.POSITIVE
                    },
                )
                if (product.neededQuantity > 0.0) {
                    AtlasChip(text = atlasText("Manque ${atlasQuantity(product.neededQuantity)}", "Short by ${atlasQuantity(product.neededQuantity)}", "النقص ${atlasQuantity(product.neededQuantity)}"), tone = if (product.isOutOfStock) AtlasTone.DANGER else AtlasTone.WARNING)
                }
                AtlasChip(text = atlasMoney(product.price, currency))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductDetailDialog(
    product: ProductDto,
    currency: String,
    canSell: Boolean,
    canAdjustStock: Boolean,
    onDismiss: () -> Unit,
    onAdd: (Double) -> Unit,
    onOpenSale: () -> Unit,
    onAdjustStock: () -> Unit,
) {
    var quantity by remember(product.id) { mutableStateOf(1.0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = product.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column {
                if (product.subtitle.isNotBlank()) {
                    Text(
                        text = product.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AtlasChip(text = atlasText("Prix ${atlasMoney(product.price, currency)}", "Price ${atlasMoney(product.price, currency)}", "السعر ${atlasMoney(product.price, currency)}"))
                    AtlasChip(
                        text = atlasText("Stock ${atlasQuantity(product.stock)}", "Stock ${atlasQuantity(product.stock)}", "المخزون ${atlasQuantity(product.stock)}"),
                        tone = if (product.isLowStock) AtlasTone.WARNING else AtlasTone.POSITIVE,
                    )
                }
                if (canSell) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(atlasText("Quantité"), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = { if (quantity > 1.0) quantity -= 1.0 },
                            enabled = quantity > 1.0,
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = atlasText("Réduire"))
                        }
                        Text(
                            text = atlasQuantity(quantity),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(44.dp),
                        )
                        IconButton(onClick = { quantity += 1.0 }) {
                            Icon(Icons.Outlined.AddShoppingCart, contentDescription = atlasText("Augmenter"))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = atlasText(
                            "Total ${atlasMoney(product.price * quantity, currency)} · sans TVA (ticket VR)",
                            "Total ${atlasMoney(product.price * quantity, currency)} · no VAT (VR receipt)",
                            "الإجمالي ${atlasMoney(product.price * quantity, currency)} · بدون ضريبة (إيصال VR)",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (canSell) {
                TextButton(onClick = { onAdd(quantity) }) {
                    Text(atlasText("Ajouter au panier"))
                }
            }
        },
        dismissButton = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (product.trackInventory && canAdjustStock) {
                    TextButton(onClick = onAdjustStock) { Text(atlasText("Ajuster stock"), maxLines = 1, softWrap = false) }
                }
                if (canSell) {
                    TextButton(onClick = onOpenSale) { Text(atlasText("Voir le panier"), maxLines = 1, softWrap = false) }
                }
                TextButton(onClick = onDismiss) { Text(atlasText("Fermer"), maxLines = 1, softWrap = false) }
            }
        },
    )
}

@Composable
private fun StockAdjustDialog(
    product: ProductDto,
    onDismiss: () -> Unit,
    onSubmit: (quantity: Double, out: Boolean, reason: String) -> Unit,
) {
    var rawQuantity by remember(product.id) { mutableStateOf("") }
    var isOut by remember(product.id) { mutableStateOf(true) }
    var reason by remember(product.id) { mutableStateOf("") }
    val quantity = rawQuantity.replace(',', '.').toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(atlasText("Ajuster le stock")) },
        text = {
            Column {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = atlasText(
                        "Stock actuel : ${atlasQuantity(product.stock)}",
                        "Current stock: ${atlasQuantity(product.stock)}",
                        "المخزون الحالي: ${atlasQuantity(product.stock)}",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isOut,
                        onClick = { isOut = false },
                        label = { Text(atlasText("Entrée")) },
                    )
                    FilterChip(
                        selected = isOut,
                        onClick = { isOut = true },
                        label = { Text(atlasText("Sortie")) },
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = rawQuantity,
                    onValueChange = { input ->
                        rawQuantity = input.filter { it.isDigit() || it == '.' || it == ',' }
                    },
                    label = { Text(atlasText("Quantité")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(atlasText("Motif (facultatif)")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(quantity, isOut, reason) },
                enabled = quantity > 0.0,
            ) { Text(atlasText("Valider")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(atlasText("Annuler")) }
        },
    )
}
