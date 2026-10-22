package ma.etherealmber.atlascommerce.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.format.AtlasFormat
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.DashboardDto
import ma.etherealmber.atlascommerce.data.model.InvoiceDto
import ma.etherealmber.atlascommerce.data.model.MonthlyPointDto
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasLoading
import ma.etherealmber.atlascommerce.ui.components.AtlasOfflineBanner
import ma.etherealmber.atlascommerce.ui.components.AtlasOutlinedButton
import ma.etherealmber.atlascommerce.ui.components.AtlasSectionHeader
import ma.etherealmber.atlascommerce.ui.components.AtlasStatTile
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.StatusChip
import ma.etherealmber.atlascommerce.ui.theme.Atlas

@Composable
fun HomeScreen(
    user: SessionUser,
    onOpenProducts: () -> Unit,
    onOpenSale: () -> Unit,
    onOpenScan: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onToast: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dashboard = state.dashboard

    LaunchedEffect(state.offline) {
        if (state.offline) onToast("Hors ligne — dernières données affichées.")
    }

    when {
        state.loading && dashboard == null -> AtlasLoading(message = "Chargement du tableau de bord…")
        dashboard == null -> HomeErrorContent(
            user = user,
            message = state.error ?: "Vérifiez l'adresse du serveur et votre connexion.",
            offline = state.offline,
            onRetry = viewModel::refresh,
            onOpenProducts = onOpenProducts,
            onOpenSale = onOpenSale,
            onOpenScan = onOpenScan,
        )

        else -> HomeContent(
            user = user,
            state = state,
            dashboard = dashboard,
            onRefresh = viewModel::refresh,
            onOpenProducts = onOpenProducts,
            onOpenSale = onOpenSale,
            onOpenScan = onOpenScan,
            onOpenNotifications = onOpenNotifications,
        )
    }
}

@Composable
private fun HomeErrorContent(
    user: SessionUser,
    message: String,
    offline: Boolean,
    onRetry: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenSale: () -> Unit,
    onOpenScan: () -> Unit,
) {
    val density = Atlas.density
    val canSell = Roles.canCreateDocuments(user.role)
    LazyColumn(
        contentPadding = PaddingValues(
            start = density.screenPadding,
            end = density.screenPadding,
            top = density.cardMargin,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(density.itemGap),
    ) {
        if (offline) {
            item { AtlasOfflineBanner() }
        }
        item {
            AtlasErrorCard(
                title = "Tableau de bord indisponible",
                message = message,
                onRetry = onRetry,
            )
        }
        item {
            AtlasCard {
                Text("Que faire maintenant ?", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Vous pouvez consulter le catalogue hors ligne ou scanner un article en attendant le retour de la connexion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtlasOutlinedButton(
                        text = "Produits",
                        onClick = onOpenProducts,
                        icon = Icons.Outlined.Inventory2,
                    )
                    if (canSell) {
                        AtlasOutlinedButton(
                            text = "Vente",
                            onClick = onOpenSale,
                            icon = Icons.Outlined.PointOfSale,
                        )
                    } else {
                        AtlasOutlinedButton(
                            text = "Scanner",
                            onClick = onOpenScan,
                            icon = Icons.Outlined.QrCodeScanner,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    user: SessionUser,
    state: HomeViewModel.UiState,
    dashboard: DashboardDto,
    onRefresh: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenSale: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val density = Atlas.density
    val canViewFinancials = Roles.canViewFinancials(user.role)
    val canSell = Roles.canCreateDocuments(user.role)
    val isAdmin = Roles.isAdmin(user.role)
    val invoices = dashboard.recentInvoices.take(5)

    LazyColumn(
        contentPadding = PaddingValues(
            start = density.screenPadding,
            end = density.screenPadding,
            top = density.cardMargin,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(density.itemGap),
    ) {
        if (state.offline) {
            item { AtlasOfflineBanner() }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.settings?.companyName?.takeIf { it.isNotBlank() }
                        ?.let { "Espace ${Roles.label(user.role)} · $it" }
                        ?: "Espace ${Roles.label(user.role)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (state.refreshing) {
                    Text(
                        text = "Actualisation…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser")
                    }
                }
            }
        }

        // Hero adapted strictly to role: Treasury for Admin/Secretary, Inventory Operations for Stock/User!
        item {
            AtlasEntrance(delayMillis = 0) {
                if (canViewFinancials) {
                    BalanceHero(
                        balance = dashboard.currentBalance,
                        stockValue = dashboard.stockValue,
                        receivable = dashboard.unpaidSales.total,
                        currency = state.currency,
                    )
                } else {
                    OperationsHero(
                        roleLabel = Roles.label(user.role),
                        productCount = dashboard.productCount,
                        lowStockCount = dashboard.lowStockCount,
                    )
                }
            }
        }

        // Primary role-gated action bar
        item {
            AtlasEntrance(delayMillis = 45) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (canSell) {
                        AtlasButton(
                            text = "Nouvelle vente",
                            onClick = onOpenSale,
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.PointOfSale,
                        )
                    } else {
                        AtlasButton(
                            text = "Scanner un article",
                            onClick = onOpenScan,
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.QrCodeScanner,
                        )
                    }
                    AtlasOutlinedButton(
                        text = "Produits",
                        onClick = onOpenProducts,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Inventory2,
                    )
                }
            }
        }

        // Role Info Box quick card
        item {
            AtlasEntrance(delayMillis = 75) {
                AtlasCard(onClick = onOpenNotifications, padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAdmin) {
                                Icons.Outlined.AdminPanelSettings
                            } else {
                                Icons.Outlined.NotificationsActive
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (isAdmin) {
                                    "Info Box Admin · Demandes & notifications"
                                } else {
                                    "Info Box · Notifications ${Roles.label(user.role)}"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = Roles.scopeSummary(user.role),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AtlasChip(
                            text = "Ouvrir",
                            tone = AtlasTone.INFO,
                        )
                    }
                }
            }
        }

        item {
            AtlasEntrance(delayMillis = 105) {
                Row(horizontalArrangement = Arrangement.spacedBy(density.itemGap)) {
                    AtlasStatTile(
                        label = "Produits",
                        value = AtlasFormat.integer(dashboard.productCount.toDouble()),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Inventory2,
                    )
                    if (canViewFinancials) {
                        AtlasStatTile(
                            label = "Valeur du stock",
                            value = AtlasFormat.moneyCompact(dashboard.stockValue, state.currency),
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.TrendingUp,
                        )
                    } else {
                        AtlasStatTile(
                            label = "Stock faible",
                            value = AtlasFormat.integer(dashboard.lowStockCount.toDouble()),
                            modifier = Modifier.weight(1f),
                            hint = if (dashboard.lowStockCount > 0) "À vérifier" else "Optimal",
                            tone = if (dashboard.lowStockCount > 0) AtlasTone.WARNING else AtlasTone.POSITIVE,
                            icon = Icons.Outlined.Warning,
                        )
                    }
                }
            }
        }

        if (canViewFinancials) {
            item {
                AtlasEntrance(delayMillis = 135) {
                    Row(horizontalArrangement = Arrangement.spacedBy(density.itemGap)) {
                        AtlasStatTile(
                            label = "Stock faible",
                            value = AtlasFormat.integer(dashboard.lowStockCount.toDouble()),
                            modifier = Modifier.weight(1f),
                            hint = if (dashboard.lowStockCount > 0) "À réapprovisionner" else "Tout est bon",
                            tone = if (dashboard.lowStockCount > 0) AtlasTone.WARNING else AtlasTone.POSITIVE,
                            icon = Icons.Outlined.Warning,
                        )
                        AtlasStatTile(
                            label = "Créances clients",
                            value = AtlasFormat.moneyCompact(dashboard.unpaidSales.total, state.currency),
                            modifier = Modifier.weight(1f),
                            hint = "${dashboard.unpaidSales.count} facture(s)",
                            tone = if (dashboard.unpaidSales.overdue > 0) AtlasTone.DANGER else AtlasTone.INFO,
                            icon = Icons.Outlined.ReceiptLong,
                        )
                    }
                }
            }
        }

        if (dashboard.lowStockCount > 0) {
            item {
                AtlasCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AtlasChip(text = "Alerte stock", tone = AtlasTone.WARNING, icon = Icons.Outlined.Warning)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onOpenProducts) { Text("Voir") }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${dashboard.lowStockCount} produit(s) sont au niveau ou sous le seuil d'alerte.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (canViewFinancials && dashboard.monthlySeries.size >= 2) {
            item {
                AtlasCard {
                    AtlasSectionHeader(
                        title = "Tendance",
                        subtitle = "Ventes et dépenses des derniers mois",
                    )
                    Spacer(Modifier.height(10.dp))
                    MonthlyChart(dashboard.monthlySeries.takeLast(6))
                }
            }
        }

        if (canViewFinancials) {
            item {
                AtlasCard {
                    AtlasSectionHeader(
                        title = "Derniers documents",
                        subtitle = if (invoices.isEmpty()) null else "${invoices.size} récent(s)",
                    )
                    Spacer(Modifier.height(6.dp))
                    if (invoices.isEmpty()) {
                        AtlasEmptyState(
                            title = "Aucun document",
                            message = "Les ventes et factures apparaîtront ici.",
                            icon = Icons.Outlined.ReceiptLong,
                            actionLabel = "Créer une vente",
                            onAction = onOpenSale,
                        )
                    } else {
                        invoices.forEach { invoice ->
                            InvoiceRow(invoice = invoice, currency = state.currency)
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = onOpenProducts) { Text("Voir le catalogue") }
                    }
                }
            }
        }

        item {
            AtlasCard {
                Text(
                    text = "Atlas Commerce ${AppDetails.VERSION_NAME}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Affichage et permissions synchronisés dynamiquement avec votre rôle (${Roles.label(user.role)}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BalanceHero(
    balance: Double,
    stockValue: Double,
    receivable: Double,
    currency: String,
) {
    val palette = Atlas.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(palette.heroStart, palette.heroEnd)))
            .padding(18.dp),
    ) {
        Text(
            text = "SOLDE DE TRÉSORERIE",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.68f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = AtlasFormat.money(balance, currency),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroPill(text = "Stock ${AtlasFormat.moneyCompact(stockValue, currency)}")
            HeroPill(text = "Créances ${AtlasFormat.moneyCompact(receivable, currency)}")
        }
    }
}

@Composable
private fun OperationsHero(
    roleLabel: String,
    productCount: Int,
    lowStockCount: Int,
) {
    val palette = Atlas.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(palette.heroStart, palette.heroEnd)))
            .padding(18.dp),
    ) {
        Text(
            text = "PILOTAGE CATALOGUE & STOCK · ${roleLabel.uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${AtlasFormat.integer(productCount.toDouble())} articles actifs",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroPill(
                text = if (lowStockCount > 0) {
                    "$lowStockCount article(s) en alerte stock"
                } else {
                    "Aucune rupture critique"
                },
            )
            HeroPill(text = "Demandes → Admin")
        }
    }
}

@Composable
private fun HeroPill(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthlyChart(points: List<MonthlyPointDto>) {
    val salesColor = MaterialTheme.colorScheme.primary
    val expenseColor = Atlas.palette.danger.copy(alpha = 0.45f)
    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
        ) {
            if (points.isEmpty()) return@Canvas
            val maxValue = points.maxOf { maxOf(it.sales, it.expenses) }.coerceAtLeast(1.0)
            val slot = size.width / points.size
            val barWidth = (slot * 0.26f).coerceAtLeast(2f)
            val gap = 2.dp.toPx()
            points.forEachIndexed { index, point ->
                val center = slot * index + slot / 2f
                val salesHeight = ((point.sales / maxValue).toFloat() * size.height).coerceAtLeast(0f)
                val expenseHeight = ((point.expenses / maxValue).toFloat() * size.height).coerceAtLeast(0f)
                val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(center - barWidth - gap / 2f, size.height - expenseHeight),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = salesColor,
                    topLeft = Offset(center + gap / 2f, size.height - salesHeight),
                    size = Size(barWidth, salesHeight),
                    cornerRadius = radius,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { point ->
                Text(
                    text = point.label.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot(color = salesColor, label = "Ventes")
            LegendDot(color = expenseColor, label = "Dépenses")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = color, shape = RoundedCornerShape(3.dp)) {
            Spacer(Modifier.size(8.dp))
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InvoiceRow(invoice: InvoiceDto, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = invoice.number?.takeIf { it.isNotBlank() } ?: "Document #${invoice.id}",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    invoice.clientName?.takeIf { it.isNotBlank() },
                    AtlasFormat.shortDate(invoice.issueDate).takeIf { it != "—" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = AtlasFormat.money(invoice.total, currency),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            StatusChip(status = invoice.status)
        }
    }
}
