package ma.etherealmber.atlascommerce.ui.screens.home

import android.content.ClipData
import android.content.Intent
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
import androidx.compose.material.icons.outlined.Description
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.format.AtlasFormat
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.DashboardDto
import ma.etherealmber.atlascommerce.data.model.FinanceMonthlyDto
import ma.etherealmber.atlascommerce.data.model.FinanceOutstandingDocumentDto
import ma.etherealmber.atlascommerce.data.model.InvoiceDto
import ma.etherealmber.atlascommerce.data.model.MonthlyPointDto
import ma.etherealmber.atlascommerce.data.model.PayrollPageDto
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasInfoRow
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
    val context = LocalContext.current

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
            onLoadMorePayroll = viewModel::loadMorePayroll,
            onExportFinanceReport = {
                viewModel.downloadFinanceReport(context.cacheDir) { result ->
                    when (result) {
                        is ApiResult.Failure -> onToast(result.message)
                        is ApiResult.Success -> runCatching {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                result.data,
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                clipData = ClipData.newUri(context.contentResolver, result.data.name, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Partager le rapport mensuel")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }.onFailure { onToast("PDF téléchargé, mais impossible de l'ouvrir : ${it.message.orEmpty()}") }
                    }
                }
            },
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
    onLoadMorePayroll: () -> Unit,
    onExportFinanceReport: () -> Unit,
) {
    val density = Atlas.density
    val canViewInvoices = Roles.canCreateDocuments(user.role)
    val canSell = Roles.canCreateDocuments(user.role)
    val isAdmin = Roles.isAdmin(user.role)
    // New finance endpoints enforce owner/admin server-side; only reveal the older
    // dashboard's financial fields after that protected endpoint has authorized us.
    val canDisplayFinance = isAdmin && state.finance != null
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

        // Treasury is restricted to Owner/Admin; other roles get operational stock data only.
        item {
            AtlasEntrance(delayMillis = 0) {
                if (canDisplayFinance) {
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
                    if (canDisplayFinance) {
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

        if (canDisplayFinance) {
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

        if (isAdmin) {
            item {
                FinanceAdminCard(
                    summary = state.finance,
                    payroll = state.payroll,
                    loading = state.financeLoading,
                    error = state.financeError,
                    reportGenerating = state.reportGenerating,
                    payrollLoadingMore = state.payrollLoadingMore,
                    onRefresh = onRefresh,
                    onLoadMorePayroll = onLoadMorePayroll,
                    onExportReport = onExportFinanceReport,
                )
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

        if (canDisplayFinance && dashboard.monthlySeries.size >= 2) {
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

        if (canViewInvoices) {
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
private fun FinanceAdminCard(
    summary: FinanceMonthlyDto?,
    payroll: PayrollPageDto?,
    loading: Boolean,
    error: String?,
    reportGenerating: Boolean,
    payrollLoadingMore: Boolean,
    onRefresh: () -> Unit,
    onLoadMorePayroll: () -> Unit,
    onExportReport: () -> Unit,
) {
    AtlasCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Finance & équipe", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = summary?.let { "Rapport ${it.period} · ${it.timezone}" } ?: "Données administrateur",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (loading && summary == null) {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser les finances")
                }
            }
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (summary != null) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtlasStatTile(
                    label = "Solde clôture",
                    value = AtlasFormat.moneyCompact(summary.closingBalance, summary.currency),
                    modifier = Modifier.weight(1f),
                    hint = "Ouverture ${AtlasFormat.moneyCompact(summary.openingBalance, summary.currency)}",
                    tone = AtlasTone.POSITIVE,
                )
                AtlasStatTile(
                    label = "Paie à verser",
                    value = AtlasFormat.moneyCompact(summary.payroll.totalDue, summary.currency),
                    modifier = Modifier.weight(1f),
                    hint = "${summary.payroll.employees} employé(s)",
                    tone = if (summary.payroll.totalDue > 0.0) AtlasTone.WARNING else AtlasTone.POSITIVE,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtlasStatTile(
                    label = "Clients à recevoir",
                    value = AtlasFormat.moneyCompact(summary.receivables.totalOutstanding, summary.currency),
                    modifier = Modifier.weight(1f),
                    hint = "${summary.receivables.count} facture(s) · ${AtlasFormat.moneyCompact(summary.receivables.totalOverdue, summary.currency)} en retard",
                    tone = if (summary.receivables.totalOverdue > 0.0) AtlasTone.DANGER else AtlasTone.INFO,
                )
                AtlasStatTile(
                    label = "Fournisseurs à payer",
                    value = AtlasFormat.moneyCompact(summary.payables.totalOutstanding, summary.currency),
                    modifier = Modifier.weight(1f),
                    hint = "${summary.payables.count} facture(s) · ${AtlasFormat.moneyCompact(summary.payables.totalOverdue, summary.currency)} en retard",
                    tone = if (summary.payables.totalOverdue > 0.0) AtlasTone.DANGER else AtlasTone.WARNING,
                )
            }
            Spacer(Modifier.height(10.dp))
            AtlasDivider()
            AtlasInfoRow("Ventes encaissées", AtlasFormat.money(summary.salesReceived, summary.currency))
            AtlasInfoRow("Dépenses (remboursements inclus)", AtlasFormat.money(summary.expenses, summary.currency))
            AtlasInfoRow("Remboursements", AtlasFormat.money(summary.refunds, summary.currency))
            AtlasInfoRow("Salaires versés ce mois", AtlasFormat.money(summary.payrollPaid, summary.currency))
            AtlasInfoRow("Fournisseurs réglés ce mois", AtlasFormat.money(summary.supplierPaid, summary.currency))

            FinanceObligationsList(
                title = "Clients à encaisser",
                documents = summary.receivables.documents,
                currency = summary.currency,
            )
            FinanceObligationsList(
                title = "Factures fournisseurs à régler",
                documents = summary.payables.documents,
                currency = summary.currency,
            )

            Spacer(Modifier.height(8.dp))
            Text("Équipe · paie à préparer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            when {
                loading && payroll == null -> {
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                payroll == null && error != null -> {
                    Spacer(Modifier.height(4.dp))
                    Text("La liste de paie n'a pas pu être chargée.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                payroll == null || payroll.rows.isEmpty() -> {
                    Spacer(Modifier.height(4.dp))
                    Text("Aucun versement dû pour cette période.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> payroll.rows.forEach { employee ->
                    AtlasDivider(Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(employee.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                text = buildString {
                                    append("Échéance ${AtlasFormat.shortDate(employee.dueDate)}")
                                    employee.lastPayment?.let { append(" · Dernier versement ${AtlasFormat.shortDate(it.paidAt)}") }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(AtlasFormat.money(employee.amountDue, payroll.currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val isFutureDue = employee.status.equals("paid", ignoreCase = true) &&
                                employee.dueDate?.let { it > payroll.asOf } == true
                            val statusLabel = when {
                                isFutureDue -> "À venir"
                                employee.status.equals("paid", ignoreCase = true) -> "Payé"
                                else -> "À payer"
                            }
                            AtlasChip(
                                text = statusLabel,
                                tone = when (statusLabel) {
                                    "Payé" -> AtlasTone.POSITIVE
                                    "À venir" -> AtlasTone.INFO
                                    else -> AtlasTone.WARNING
                                },
                            )
                        }
                    }
                }
            }
            if (payroll?.hasMore == true) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${payroll.rows.size} sur ${payroll.total} employés",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onLoadMorePayroll, enabled = !payrollLoadingMore) {
                        if (payrollLoadingMore) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Afficher plus")
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            AtlasOutlinedButton(
                text = if (reportGenerating) "Préparation du PDF…" else "Exporter le rapport PDF",
                onClick = onExportReport,
                enabled = !reportGenerating && !loading,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.Description,
            )
            summary.reconciliation.asOfDate?.let { date ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "Données au ${AtlasFormat.shortDate(date)} · source ${summary.reconciliation.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (error == null && !loading) {
            Spacer(Modifier.height(6.dp))
            Text("Aucune donnée financière reçue.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FinanceObligationsList(
    title: String,
    documents: List<FinanceOutstandingDocumentDto>,
    currency: String,
) {
    Spacer(Modifier.height(10.dp))
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    if (documents.isEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text("Aucun montant en attente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    documents.take(5).forEach { document ->
        AtlasDivider(Modifier.padding(vertical = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = document.party.ifBlank { document.number },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(document.number.takeIf { it.isNotBlank() }, document.dueDate?.let { date -> AtlasFormat.shortDate(date) }).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(AtlasFormat.money(document.remaining, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                val tone = when (document.state.lowercase()) {
                    "overdue" -> AtlasTone.DANGER
                    "partial" -> AtlasTone.WARNING
                    else -> AtlasTone.INFO
                }
                AtlasChip(
                    text = when (document.state.lowercase()) {
                        "overdue" -> "En retard"
                        "partial" -> "Partiel"
                        else -> "À régler"
                    },
                    tone = tone,
                )
            }
        }
    }
    if (documents.size > 5) {
        Text("${documents.size - 5} autre(s) document(s) dans le rapport.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
