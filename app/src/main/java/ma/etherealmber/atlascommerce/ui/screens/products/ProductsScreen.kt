package ma.etherealmber.atlascommerce.ui.screens.products

import ma.etherealmber.atlascommerce.ui.i18n.atlasRoleLabel
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasQuantity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
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
import ma.etherealmber.atlascommerce.data.model.StockAdjustmentRequestDto
import ma.etherealmber.atlascommerce.ui.components.ProductArtwork
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingDockPadding
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasCatalogSkeleton
import ma.etherealmber.atlascommerce.ui.components.AtlasProductRequestsSkeleton
import ma.etherealmber.atlascommerce.ui.components.AtlasStockRequestsSkeleton
import ma.etherealmber.atlascommerce.ui.components.AtlasRefreshStatusShimmer
import ma.etherealmber.atlascommerce.ui.components.AtlasOfflineBanner
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.ProductCreateDialog
import ma.etherealmber.atlascommerce.ui.theme.Atlas

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProductsScreen(
    user: SessionUser,
    onOpenScan: () -> Unit,
    onOpenSale: () -> Unit,
    onToast: (String) -> Unit,
    onHistoryPageVisibilityChanged: (Boolean) -> Unit = {},
    showPageTitle: Boolean = false,
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
    val canRequestStockAdjustment = Roles.canRequestStockAdjustment(user.role)
    val canChangeStock = canAdjustStock || canRequestStockAdjustment
    val canEditProductImage = Roles.isAdmin(user.role) || Roles.normalize(user.role) == Roles.SECRETARY

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message -> onToast(message) }
    }
    LaunchedEffect(historyTarget != null) {
        onHistoryPageVisibilityChanged(historyTarget != null)
    }
    BackHandler(enabled = historyTarget != null && canSell) {
        historyTarget = null
    }

    val motionEnabled = LocalAtlasAnimationsEnabled.current
    SharedTransitionLayout {
    val sharedTransitionScope = this
    AnimatedContent(
        targetState = historyTarget,
        modifier = Modifier.fillMaxSize(),
        contentKey = { destination -> destination?.id ?: "product-catalog" },
        transitionSpec = {
            if (!motionEnabled) {
                EnterTransition.None togetherWith ExitTransition.None
            } else if (targetState != null) {
                (slideInHorizontally(animationSpec = tween(320)) { it } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(animationSpec = tween(250)) { -it / 5 } + fadeOut(tween(180)))
            } else {
                (slideInHorizontally(animationSpec = tween(280)) { -it / 5 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(animationSpec = tween(320)) { it } + fadeOut(tween(180)))
            }
        },
        label = "product_catalog_push_navigation",
    ) { destination ->
        val animatedVisibilityScope = this
        if (destination != null && canSell) {
            ProductHistoryScreen(
                product = destination,
                currency = state.currency,
                serverBaseUrl = user.serverUrl,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                canEditImage = canEditProductImage,
                canAdjustStock = canAdjustStock,
                onOpenStockAdjust = { adjustTarget = destination },
                onBack = { historyTarget = null },
                onProductImageUpdated = { url ->
                    historyTarget = historyTarget?.takeIf { it.id == destination.id }?.copy(imageUrl = url)
                    viewModel.refresh()
                },
                onToast = onToast,
            )
        } else {
        Column(modifier = Modifier.fillMaxSize()) {
        if (showPageTitle) {
            Text(
                text = atlasText("Produits", "Products", "المنتجات"),
                modifier = Modifier.fillMaxWidth().padding(horizontal = density.screenPadding, vertical = density.itemGap),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
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
                        val count = if (state.loadingProductRequests && state.openRequests.isEmpty()) "" else " (${state.openRequests.size})"
                        Text(
                            if (isAdmin) {
                                atlasText("À valider$count", "To approve$count", "بانتظار الموافقة$count")
                            } else {
                                atlasText("Mes demandes$count", "My requests$count", "طلباتي$count")
                            },
                        )
                    },
                )
                FilterChip(
                    selected = state.filter == ProductsViewModel.FilterMode.STOCK_ADJUSTMENTS,
                    onClick = { viewModel.setFilter(ProductsViewModel.FilterMode.STOCK_ADJUSTMENTS) },
                    label = {
                        val pendingCount = state.stockAdjustmentRequests.count { it.status.equals("pending", ignoreCase = true) }
                        Text(
                            if (state.stockAdjustmentRequests.isEmpty()) atlasText("Demandes de stock", "Stock requests", "طلبات المخزون")
                            else atlasText(
                                "Stock (${pendingCount} en attente)",
                                "Stock (${pendingCount} pending)",
                                "المخزون (${pendingCount} معلّقة)",
                            ),
                        )
                    },
                )
            }
            if (state.filter != ProductsViewModel.FilterMode.REQUESTS &&
                state.filter != ProductsViewModel.FilterMode.STOCK_ADJUSTMENTS &&
                !state.loading && state.error.isNullOrBlank()
            ) {
                Text(
                    when {
                        state.checkingCachedCatalog -> atlasText(
                            "${state.visibleProducts.size} résultat${if (state.visibleProducts.size == 1) "" else "s"} · catalogue enregistré, vérification en cours",
                            "${state.visibleProducts.size} results · saved catalog, checking for updates",
                            "${state.visibleProducts.size} نتيجة · كتالوج محفوظ، جارٍ التحقق من التحديثات",
                        )
                        state.cachedFallback -> atlasText(
                            "${state.visibleProducts.size} résultat${if (state.visibleProducts.size == 1) "" else "s"} · catalogue enregistré",
                            "${state.visibleProducts.size} results · saved catalog",
                            "${state.visibleProducts.size} نتيجة · كتالوج محفوظ",
                        )
                        else -> atlasText(
                            "${state.visibleProducts.size} résultat${if (state.visibleProducts.size == 1) "" else "s"} · catalogue en direct",
                            "${state.visibleProducts.size} results · live catalog",
                            "${state.visibleProducts.size} نتيجة · الكتالوج المباشر",
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.offline) {
            AtlasOfflineBanner(modifier = Modifier.padding(horizontal = density.screenPadding))
            Spacer(Modifier.height(8.dp))
        } else if (state.checkingCachedCatalog) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = density.screenPadding),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    atlasText(
                        "Catalogue enregistré disponible — vérification des mises à jour en cours.",
                        "Saved catalog available — checking for live updates.",
                        "الكتالوج المحفوظ متاح — جارٍ التحقق من التحديثات المباشرة.",
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
        } else if (state.cachedFallback) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = density.screenPadding),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        if (state.refreshing) atlasText("Catalogue enregistré affiché pendant l'actualisation.", "Showing the saved catalog while refreshing.", "يتم عرض الكتالوج المحفوظ أثناء التحديث.")
                        else atlasText("Mise à jour en direct impossible — catalogue enregistré affiché.", "Live update failed — showing the saved catalog.", "تعذر التحديث المباشر — يتم عرض الكتالوج المحفوظ."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        val refreshLabel = when {
            state.filter == ProductsViewModel.FilterMode.STOCK_ADJUSTMENTS &&
                state.stockAdjustmentLoading && state.stockAdjustmentRequests.isNotEmpty() ->
                atlasText("Actualisation des demandes de stock…", "Refreshing stock requests…", "جارٍ تحديث طلبات المخزون…")
            state.filter == ProductsViewModel.FilterMode.REQUESTS &&
                state.loadingProductRequests && state.openRequests.isNotEmpty() ->
                atlasText("Actualisation des demandes produit…", "Refreshing product requests…", "جارٍ تحديث طلبات المنتجات…")
            state.filter != ProductsViewModel.FilterMode.REQUESTS &&
                state.filter != ProductsViewModel.FilterMode.STOCK_ADJUSTMENTS &&
                (state.checkingCachedCatalog || state.refreshing && state.visibleProducts.isNotEmpty()) ->
                atlasText("Vérification du catalogue…", "Refreshing catalog…", "جارٍ تحديث الكتالوج…")
            else -> null
        }
        refreshLabel?.let { label ->
            AtlasRefreshStatusShimmer(
                modifier = Modifier.fillMaxWidth().padding(horizontal = density.screenPadding),
                label = label,
            )
            Spacer(Modifier.height(8.dp))
        }

        when {
            state.filter == ProductsViewModel.FilterMode.STOCK_ADJUSTMENTS -> when {
                state.stockAdjustmentLoading && state.stockAdjustmentRequests.isEmpty() ->
                    AtlasStockRequestsSkeleton(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        rows = 4,
                        showActions = canAdjustStock,
                        bottomPadding = 24.dp + dockPadding,
                    )

                !state.stockAdjustmentError.isNullOrBlank() ->
                    Column(Modifier.padding(horizontal = density.screenPadding)) {
                        AtlasErrorCard(
                            message = atlasNotice(state.stockAdjustmentError.orEmpty(), LocalAtlasLanguage.current),
                            onRetry = viewModel::loadStockAdjustmentRequests,
                        )
                    }

                state.stockAdjustmentRequests.isEmpty() -> AtlasEmptyState(
                    title = atlasText("Aucune demande de stock", "No stock requests", "لا توجد طلبات مخزون"),
                    message = if (canAdjustStock) {
                        atlasText(
                            "Les demandes d'ajustement envoyées par l'équipe apparaîtront ici.",
                            "Stock adjustment requests from your team will appear here.",
                            "ستظهر هنا طلبات تعديل المخزون المرسلة من الفريق.",
                        )
                    } else {
                        atlasText(
                            "Vos demandes d'ajustement apparaîtront ici avec leur décision.",
                            "Your stock adjustment requests and their decisions will appear here.",
                            "ستظهر هنا طلبات تعديل المخزون والقرارات المتعلقة بها.",
                        )
                    },
                    icon = Icons.Outlined.Inventory2,
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 320.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = density.screenPadding, end = density.screenPadding, bottom = 24.dp + dockPadding),
                    horizontalArrangement = Arrangement.spacedBy(density.itemGap),
                    verticalArrangement = Arrangement.spacedBy(density.itemGap),
                ) {
                    gridItemsIndexed(state.stockAdjustmentRequests, key = { _, request -> request.id }) { _, request ->
                        StockAdjustmentRequestRow(
                            request = request,
                            canApprove = canAdjustStock,
                            deciding = request.id in state.decidingStockRequestIds,
                            onApprove = { viewModel.decideStockAdjustmentRequest(request.id, approve = true) },
                            onReject = { viewModel.decideStockAdjustmentRequest(request.id, approve = false) },
                        )
                    }
                }
            }

            state.filter == ProductsViewModel.FilterMode.REQUESTS &&
                (state.loadingProductRequests || state.loading) && state.openRequests.isEmpty() ->
                AtlasProductRequestsSkeleton(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    rows = 6,
                    showActions = isAdmin,
                    bottomPadding = 24.dp + dockPadding,
                )

            state.filter == ProductsViewModel.FilterMode.REQUESTS && !state.productRequestsError.isNullOrBlank() ->
                Column(Modifier.padding(horizontal = density.screenPadding)) {
                    AtlasErrorCard(
                        message = atlasNotice(state.productRequestsError.orEmpty(), LocalAtlasLanguage.current),
                        onRetry = viewModel::refresh,
                    )
                }

            (state.loading || state.refreshing || state.checkingCachedCatalog) && state.filter != ProductsViewModel.FilterMode.REQUESTS -> AtlasCatalogSkeleton(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                rows = 6,
                showRoleBanner = !isAdmin,
                showCartButton = canSell,
                bottomPadding = 24.dp + dockPadding,
                products = state.visibleProducts,
            )

            !state.error.isNullOrBlank() && state.filter != ProductsViewModel.FilterMode.REQUESTS -> {
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
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wideLayout = maxWidth >= 760.dp
                    LazyVerticalGrid(
                        columns = if (wideLayout) GridCells.Fixed(1) else GridCells.Adaptive(minSize = 320.dp),
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
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    canSell = canSell,
                                    wideLayout = wideLayout,
                                    onOpen = {
                                        if (canSell) historyTarget = product else selected = product
                                    },
                                    onAdd = {
                                        if (canSell) viewModel.addToCart(product)
                                    },
                                )
                            }
                        }
                    }
                }
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

    val product = selected
    if (product != null) {
        ProductDetailDialog(
            product = product,
            currency = state.currency,
            canSell = canSell,
            canDirectlyAdjustStock = canAdjustStock,
            canChangeStock = canChangeStock,
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
    if (target != null && canChangeStock) {
        StockAdjustDialog(
            product = target,
            directAdjustment = canAdjustStock,
            onDismiss = { adjustTarget = null },
            onSubmit = { quantity, out, reason ->
                viewModel.adjustStock(target, quantity, out, reason) { updated ->
                    if (historyTarget?.id == updated.id) historyTarget = updated
                }
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
private fun StockAdjustmentRequestRow(
    request: StockAdjustmentRequestDto,
    canApprove: Boolean,
    deciding: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val status = request.status.lowercase()
    val statusText = when (status) {
        "pending" -> atlasText("En attente", "Pending", "قيد الانتظار")
        "approved" -> atlasText("Approuvée", "Approved", "تمت الموافقة")
        "rejected" -> atlasText("Refusée", "Rejected", "مرفوضة")
        else -> atlasText("Échec", "Failed", "فشلت")
    }
    val directionText = when (request.direction.lowercase()) {
        "in" -> atlasText("Entrée", "Stock in", "إدخال للمخزون")
        "out" -> atlasText("Sortie", "Stock out", "إخراج من المخزون")
        else -> atlasText("Ajustement", "Adjustment", "تعديل")
    }
    val tone = when (status) {
        "pending" -> AtlasTone.WARNING
        "approved" -> AtlasTone.POSITIVE
        "rejected", "failed" -> AtlasTone.DANGER
        else -> AtlasTone.NEUTRAL
    }

    AtlasCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    request.productName.ifBlank {
                        atlasText("Produit #${request.productId}", "Product #${request.productId}", "المنتج #${request.productId}")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$directionText · ${atlasQuantity(request.quantity)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            AtlasChip(text = statusText, tone = tone)
        }
        if (!request.reason.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(request.reason.orEmpty(), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            atlasText(
                "Demandé par ${request.requestedBy}",
                "Requested by ${request.requestedBy}",
                "طلبه ${request.requestedBy}",
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (request.stockBefore != null && request.stockAfter != null) {
            Text(
                atlasText(
                    "Stock ${atlasQuantity(request.stockBefore)} → ${atlasQuantity(request.stockAfter)}",
                    "Stock ${atlasQuantity(request.stockBefore)} → ${atlasQuantity(request.stockAfter)}",
                    "المخزون ${atlasQuantity(request.stockBefore)} ← ${atlasQuantity(request.stockAfter)}",
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (canApprove && status == "pending") {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onApprove, enabled = !deciding) {
                    Text(atlasText("Approuver", "Approve", "موافقة"))
                }
                TextButton(onClick = onReject, enabled = !deciding) {
                    Text(atlasText("Refuser", "Reject", "رفض"))
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ProductRow(
    product: ProductDto,
    currency: String,
    serverBaseUrl: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    canSell: Boolean,
    wideLayout: Boolean,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
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
    AtlasCard(onClick = onOpen, padding = if (wideLayout) 16.dp else 14.dp) {
        if (wideLayout) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProductArtwork(
                    imageUrl = product.imageUrl,
                    serverBaseUrl = serverBaseUrl,
                    label = product.name,
                    modifier = Modifier.size(68.dp).then(sharedArtworkModifier),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (product.subtitle.isNotBlank()) {
                        Text(product.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Box(Modifier.weight(1f, fill = false)) {
                    ProductMetaChips(product, currency)
                }
                if (canSell) {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Outlined.AddShoppingCart, contentDescription = atlasText("Ajouter au panier"), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProductArtwork(
                        imageUrl = product.imageUrl,
                        serverBaseUrl = serverBaseUrl,
                        label = product.name,
                        modifier = Modifier.size(64.dp).then(sharedArtworkModifier),
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
                ProductMetaChips(product, currency)
            }
        }
    }
}

@Composable
private fun ProductMetaChips(product: ProductDto, currency: String) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AtlasChip(
            text = atlasText(
                "Stock ${productStockDisplay(product.stock, product.unit)}",
                "Stock ${productStockDisplay(product.stock, product.unit)}",
                "المخزون ${productStockDisplay(product.stock, product.unit)}",
            ),
            tone = productStockTone(product.stock, product.trackInventory),
        )
        if (product.neededQuantity > 0.0) {
            AtlasChip(
                text = atlasText("Manque ${atlasQuantity(product.neededQuantity)}", "Short by ${atlasQuantity(product.neededQuantity)}", "النقص ${atlasQuantity(product.neededQuantity)}"),
                tone = if (product.isOutOfStock) AtlasTone.DANGER else AtlasTone.WARNING,
            )
        }
        AtlasChip(text = atlasMoney(product.price, currency))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductDetailDialog(
    product: ProductDto,
    currency: String,
    canSell: Boolean,
    canDirectlyAdjustStock: Boolean,
    canChangeStock: Boolean,
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
                        text = atlasText(
                            "Stock ${productStockDisplay(product.stock, product.unit)}",
                            "Stock ${productStockDisplay(product.stock, product.unit)}",
                            "المخزون ${productStockDisplay(product.stock, product.unit)}",
                        ),
                        tone = productStockTone(product.stock, product.trackInventory),
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
                if (product.trackInventory && canChangeStock) {
                    TextButton(onClick = onAdjustStock) {
                        Text(
                            if (canDirectlyAdjustStock) atlasText("Modifier le stock", "Edit stock", "تعديل المخزون")
                            else atlasText("Demander un changement", "Request stock change", "طلب تغيير المخزون"),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
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
    directAdjustment: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (quantity: Double, out: Boolean, reason: String) -> Unit,
) {
    var rawQuantity by remember(product.id) { mutableStateOf("") }
    var isOut by remember(product.id) { mutableStateOf(true) }
    var reason by remember(product.id) { mutableStateOf("") }
    val quantity = parseStockAdjustmentQuantity(rawQuantity)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (directAdjustment) atlasText("Ajuster le stock", "Adjust stock", "تعديل المخزون")
                else atlasText("Demander un ajustement", "Request a stock adjustment", "طلب تعديل المخزون"),
            )
        },
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
                        "Stock actuel : ${productStockDisplay(product.stock, product.unit)}",
                        "Current stock: ${productStockDisplay(product.stock, product.unit)}",
                        "المخزون الحالي: ${productStockDisplay(product.stock, product.unit)}",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!directAdjustment) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        atlasText(
                            "Votre demande sera appliquée après approbation par un Admin ou un Secrétaire.",
                            "Your request will be applied after approval by an Admin or Secretary.",
                            "سيُطبّق طلبك بعد موافقة المسؤول أو السكرتير.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                onClick = { quantity?.let { onSubmit(it, isOut, reason) } },
                enabled = quantity != null,
            ) {
                Text(
                    if (directAdjustment) atlasText("Appliquer", "Apply", "تطبيق")
                    else atlasText("Envoyer la demande", "Submit request", "إرسال الطلب"),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(atlasText("Annuler")) }
        },
    )
}
