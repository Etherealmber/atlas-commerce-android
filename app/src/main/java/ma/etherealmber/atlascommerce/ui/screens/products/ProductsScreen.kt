package ma.etherealmber.atlascommerce.ui.screens.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.format.AtlasFormat
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.ProductCreationRequestDto
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasLoading
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val density = Atlas.density
    var selected by remember { mutableStateOf<ProductDto?>(null) }
    var adjustTarget by remember { mutableStateOf<ProductDto?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingFulfillRequest by remember { mutableStateOf<ProductCreationRequestDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message -> onToast(message) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = density.screenPadding, vertical = density.itemGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AtlasTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    label = "Rechercher un produit",
                    placeholder = "Nom, SKU ou code-barres",
                    leadingIcon = Icons.Outlined.Search,
                    imeAction = ImeAction.Search,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Nouveau produit")
                }
                IconButton(onClick = onOpenScan) {
                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scanner un code-barres")
                }
                IconButton(onClick = viewModel::refresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser")
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = state.filter == ProductsViewModel.FilterMode.ALL,
                    onClick = { viewModel.setFilter(ProductsViewModel.FilterMode.ALL) },
                    label = { Text("Tous") },
                )
                FilterChip(
                    selected = state.filter == ProductsViewModel.FilterMode.NEEDED,
                    onClick = { viewModel.setFilter(ProductsViewModel.FilterMode.NEEDED) },
                    label = { Text("À réappro. (${state.lowStockCount})") },
                )
                FilterChip(
                    selected = state.filter == ProductsViewModel.FilterMode.REQUESTS,
                    onClick = { viewModel.setFilter(ProductsViewModel.FilterMode.REQUESTS) },
                    label = { Text("Demandes (${state.openRequests.size})") },
                )
            }
        }

        if (state.offline) {
            AtlasOfflineBanner(modifier = Modifier.padding(horizontal = density.screenPadding))
            Spacer(Modifier.height(8.dp))
        }

        when {
            state.loading -> AtlasLoading(message = "Chargement du catalogue…")

            !state.error.isNullOrBlank() -> {
                Column(Modifier.padding(horizontal = density.screenPadding)) {
                    AtlasErrorCard(message = state.error.orEmpty(), onRetry = viewModel::refresh)
                }
            }

            state.filter == ProductsViewModel.FilterMode.REQUESTS -> {
                if (state.openRequests.isEmpty()) {
                    AtlasEmptyState(
                        title = "Aucune demande en attente",
                        message = "Les codes inconnus scannés sur le terrain apparaissent ici avec les champs requis.",
                        icon = Icons.Outlined.Inventory2,
                        actionLabel = "Nouveau produit",
                        onAction = { showCreateDialog = true },
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = density.screenPadding,
                            end = density.screenPadding,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(density.itemGap),
                    ) {
                        items(state.openRequests, key = { it.id }) { req ->
                            ProductRequestRow(
                                request = req,
                                canCreate = Roles.canMutateStock(user.role),
                                onFulfill = { pendingFulfillRequest = req },
                                onDismiss = { viewModel.dismissProductRequest(req.id) },
                            )
                        }
                    }
                }
            }

            state.visibleProducts.isEmpty() -> AtlasEmptyState(
                title = if (state.query.isBlank()) "Aucun produit" else "Aucun résultat",
                message = if (state.query.isBlank()) {
                    "Ajoutez un produit directement avec le bouton + ou scannez son code-barres."
                } else {
                    "Essayez un autre nom, SKU ou code-barres."
                },
                icon = Icons.Outlined.Inventory2,
                actionLabel = "Nouveau produit",
                onAction = { showCreateDialog = true },
            )

            else -> {
                val canSell = Roles.canCreateDocuments(user.role)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = density.screenPadding,
                        end = density.screenPadding,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(density.itemGap),
                ) {
                    if (!canSell) {
                        item {
                            AtlasCard {
                                Text(
                                    text = "Consultation & gestion stock",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "Votre rôle permet de gérer le stock et les ajouts, pas d'émettre de facture.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    items(state.visibleProducts, key = { it.id }) { product ->
                        ProductRow(
                            product = product,
                            currency = state.currency,
                            onOpen = { selected = product },
                            onAdd = {
                                if (canSell) {
                                    viewModel.addToCart(product)
                                } else {
                                    onToast("Votre rôle ne permet pas de créer une vente.")
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ProductCreateDialog(
            canCreateDirectly = Roles.canMutateStock(user.role),
            initialBarcode = state.query.trim().takeIf { it.all(Char::isDigit) && it.length >= 6 }.orEmpty(),
            submitting = state.submitting,
            onDismiss = { showCreateDialog = false },
            onSubmitDirect = { req ->
                showCreateDialog = false
                viewModel.createProduct(req)
            },
            onSubmitRequest = { code, name, sku, unit, qty, cat, note ->
                showCreateDialog = false
                viewModel.submitProductRequest(code, name, sku, unit, qty, cat, note)
            },
        )
    }

    val fulfill = pendingFulfillRequest
    if (fulfill != null) {
        ProductCreateDialog(
            canCreateDirectly = Roles.canMutateStock(user.role),
            initialBarcode = fulfill.code,
            initialName = fulfill.name.orEmpty(),
            initialSku = fulfill.sku.orEmpty(),
            initialUnit = fulfill.unit ?: "unité",
            initialQuantity = fulfill.quantity?.let { AtlasFormat.quantity(it) } ?: "1",
            initialCategory = fulfill.category.orEmpty(),
            submitting = state.submitting,
            onDismiss = { pendingFulfillRequest = null },
            onSubmitDirect = { req ->
                pendingFulfillRequest = null
                viewModel.createProduct(req, fulfillingRequestId = fulfill.id)
            },
            onSubmitRequest = { code, name, sku, unit, qty, cat, note ->
                pendingFulfillRequest = null
                viewModel.submitProductRequest(code, name, sku, unit, qty, cat, note)
            },
        )
    }

    val product = selected
    if (product != null) {
        ProductDetailDialog(
            product = product,
            currency = state.currency,
            canSell = Roles.canCreateDocuments(user.role),
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
    if (target != null) {
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
private fun ProductRequestRow(
    request: ProductCreationRequestDto,
    canCreate: Boolean,
    onFulfill: () -> Unit,
    onDismiss: () -> Unit,
) {
    AtlasCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = request.name?.takeIf { it.isNotBlank() } ?: "Code ${request.code}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = listOfNotNull(
                        "Code ${request.code}",
                        request.sku?.takeIf { it.isNotBlank() }?.let { "SKU $it" },
                        request.quantity?.let { "${AtlasFormat.quantity(it)} ${request.unit ?: "unité"}" },
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
            AtlasChip(text = "En attente", tone = AtlasTone.WARNING)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canCreate) {
                TextButton(onClick = onFulfill) { Text("Créer au stock") }
            }
            TextButton(onClick = onDismiss) { Text("Écarter") }
        }
    }
}

@Composable
private fun ProductRow(
    product: ProductDto,
    currency: String,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
) {
    AtlasCard(onClick = onOpen, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (product.subtitle.isNotBlank()) {
                    Text(
                        text = product.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AtlasChip(
                        text = "Stock ${AtlasFormat.quantity(product.stock)}",
                        tone = when {
                            product.isOutOfStock -> AtlasTone.DANGER
                            product.isLowStock -> AtlasTone.WARNING
                            else -> AtlasTone.POSITIVE
                        },
                    )
                    if (product.neededQuantity > 0.0) {
                        AtlasChip(
                            text = "Manque ${AtlasFormat.quantity(product.neededQuantity)}",
                            tone = if (product.isOutOfStock) AtlasTone.DANGER else AtlasTone.WARNING,
                        )
                    }
                    AtlasChip(text = AtlasFormat.money(product.price, currency))
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onAdd) {
                Icon(
                    imageVector = Icons.Outlined.AddShoppingCart,
                    contentDescription = "Ajouter au panier",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ProductDetailDialog(
    product: ProductDto,
    currency: String,
    canSell: Boolean,
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
                    AtlasChip(text = "Prix ${AtlasFormat.money(product.price, currency)}")
                    AtlasChip(
                        text = "Stock ${AtlasFormat.quantity(product.stock)}",
                        tone = if (product.isLowStock) AtlasTone.WARNING else AtlasTone.POSITIVE,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Quantité", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { if (quantity > 1.0) quantity -= 1.0 },
                        enabled = quantity > 1.0,
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Réduire")
                    }
                    Text(
                        text = AtlasFormat.quantity(quantity),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(44.dp),
                    )
                    IconButton(onClick = { quantity += 1.0 }) {
                        Icon(Icons.Outlined.AddShoppingCart, contentDescription = "Augmenter")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Total ${AtlasFormat.money(product.price * quantity, currency)} · sans TVA (ticket VR)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!canSell) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Votre rôle ne permet pas de créer une vente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(quantity) }, enabled = canSell) {
                Text("Ajouter au panier")
            }
        },
        dismissButton = {
            Row {
                if (product.trackInventory) {
                    TextButton(onClick = onAdjustStock) { Text("Stock") }
                }
                if (canSell) {
                    TextButton(onClick = onOpenSale) { Text("Voir le panier") }
                }
                TextButton(onClick = onDismiss) { Text("Fermer") }
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
        title = { Text("Ajuster le stock") },
        text = {
            Column {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Stock actuel : ${AtlasFormat.quantity(product.stock)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isOut,
                        onClick = { isOut = false },
                        label = { Text("Entrée") },
                    )
                    FilterChip(
                        selected = isOut,
                        onClick = { isOut = true },
                        label = { Text("Sortie") },
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = rawQuantity,
                    onValueChange = { input ->
                        rawQuantity = input.filter { it.isDigit() || it == '.' || it == ',' }
                    },
                    label = { Text("Quantité") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motif (facultatif)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(quantity, isOut, reason) },
                enabled = quantity > 0.0,
            ) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}
