package ma.etherealmber.atlascommerce.ui.screens.home

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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
import ma.etherealmber.atlascommerce.ui.components.CombinedReportWebViewDialog
import ma.etherealmber.atlascommerce.ui.components.FinancePdfViewerDialog
import ma.etherealmber.atlascommerce.ui.components.FinanceReportActionsDialog
import ma.etherealmber.atlascommerce.ui.components.FinanceReportSelection
import ma.etherealmber.atlascommerce.ui.components.FinanceReportSelectionDialog
import ma.etherealmber.atlascommerce.ui.components.StatusChip
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
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
    var pendingReportFile by remember { mutableStateOf<java.io.File?>(null) }
    var showReportActions by remember { mutableStateOf(false) }
    var pdfViewerFile by remember { mutableStateOf<java.io.File?>(null) }
    var combinedReportUrl by remember { mutableStateOf<String?>(null) }
    var reportSelection by remember { mutableStateOf(FinanceReportSelection()) }
    var showReportSelection by remember { mutableStateOf(false) }
    var showCombinedReportConfirm by remember { mutableStateOf(false) }

    if (showReportSelection) {
        FinanceReportSelectionDialog(
            initial = reportSelection,
            onDismiss = { showReportSelection = false },
            onGenerate = { selection ->
                reportSelection = selection
                showReportSelection = false
                showCombinedReportConfirm = true
            },
        )
    }

    if (showCombinedReportConfirm) {
        AlertDialog(
            onDismissRequest = { showCombinedReportConfirm = false },
            title = { Text(atlasText("Ouvrir le pack de rapports ?")) },
            text = {
                Text(
                    atlasText(
                        "${reportSelection.shortLabel} · ${reportSelection.selectedSections().joinToString { it.label }}. Atlas ouvrira les rubriques choisies avec un ticket temporaire de 30 minutes. Ne partagez pas le lien ni l'écran.",
                        "${reportSelection.shortLabel} · ${reportSelection.selectedSections().joinToString { it.label }}. Atlas will open the selected sections using a temporary 30-minute access token. Do not share the link or screen.",
                        "${reportSelection.shortLabel} · ${reportSelection.selectedSections().joinToString { it.label }}. سيفتح Atlas الأقسام المحددة برمز وصول مؤقت لمدة 30 دقيقة. لا تشارك الرابط أو الشاشة.",
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCombinedReportConfirm = false
                    viewModel.createCombinedReportUrl(reportSelection.period, reportSelection.from, reportSelection.to, reportSelection.sectionKeys) { result ->
                        when (result) {
                            is ApiResult.Failure -> onToast(result.message)
                            is ApiResult.Success -> combinedReportUrl = result.data
                        }
                    }
                }) { Text(atlasText("Continuer")) }
            },
            dismissButton = { TextButton(onClick = { showCombinedReportConfirm = false }) { Text(atlasText("Annuler")) } },
        )
    }
    combinedReportUrl?.let { url ->
        CombinedReportWebViewDialog(url = url, selectedSections = reportSelection.sectionKeys, onDismiss = { combinedReportUrl = null })
    }
    val saveReportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { destinationUri ->
        val file = pendingReportFile
        if (destinationUri != null && file != null) {
            runCatching {
                context.contentResolver.openOutputStream(destinationUri, "wt")?.use { output ->
                    file.inputStream().buffered().use { input -> input.copyTo(output) }
                } ?: error("Impossible d'écrire le fichier choisi.")
            }.onSuccess { onToast(atlasText("Copie PDF enregistrée.")) }
                .onFailure { onToast(atlasText("Impossible d'enregistrer le PDF.")) }
        }
        pendingReportFile = null
    }

    pendingReportFile?.takeIf { showReportActions }?.let { reportFile ->
        FinanceReportActionsDialog(
            fileName = reportFile.name,
            onDismiss = { showReportActions = false },
            onRead = {
                showReportActions = false
                pdfViewerFile = reportFile
                pendingReportFile = null
            },
            onShare = {
                showReportActions = false
                runCatching {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", reportFile)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newUri(context.contentResolver, reportFile.name, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Partager le rapport PDF"))
                }.onFailure { onToast(atlasText("Impossible de partager le PDF.")) }
                pendingReportFile = null
            },
            onSave = {
                showReportActions = false
                saveReportLauncher.launch(reportFile.name)
            },
            onDelete = {
                showReportActions = false
                reportFile.delete()
                pendingReportFile = null
                onToast(atlasText("Rapport temporaire supprimé."))
            },
        )
    }
    pdfViewerFile?.let { reportFile ->
        FinancePdfViewerDialog(
            file = reportFile,
            onDismiss = { pdfViewerFile = null },
            onShare = {
                runCatching {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", reportFile)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newUri(context.contentResolver, reportFile.name, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Partager le rapport PDF"))
                }.onFailure { onToast(atlasText("Impossible de partager le PDF.")) }
            },
            onSave = { pendingReportFile = reportFile; saveReportLauncher.launch(reportFile.name) },
            onDelete = { reportFile.delete(); pdfViewerFile = null; onToast(atlasText("Rapport temporaire supprimé.")) },
        )
    }

    LaunchedEffect(state.offline) {
        if (state.offline) onToast(atlasText("Hors ligne — dernières données affichées."))
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
            onSelectFinancePeriod = viewModel::selectFinancePeriod,
            onExportFinanceReport = { period ->
                viewModel.downloadFinanceReport(context.cacheDir, period) { result ->
                    when (result) {
                        is ApiResult.Failure -> onToast(result.message)
                        is ApiResult.Success -> {
                            pendingReportFile = result.data
                            showReportActions = true
                        }
                    }
                }
            },
            onOpenCombinedReport = { showReportSelection = true },
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
                title = atlasText("Tableau de bord indisponible", "Dashboard unavailable", "لوحة المعلومات غير متاحة"),
                message = message,
                onRetry = onRetry,
            )
        }
        item {
            AtlasCard {
                Text(atlasText("Que faire maintenant ?", "What can I do now?", "ماذا يمكنني أن أفعل الآن؟"), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = atlasText(
                        "Vous pouvez consulter le catalogue hors ligne ou scanner un article en attendant le retour de la connexion.",
                        "Browse the catalog offline or scan an item while waiting for the connection to return.",
                        "يمكنك تصفح الكتالوج دون اتصال أو مسح منتج ضوئيًا حتى عودة الاتصال.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtlasOutlinedButton(
                        text = atlasText("Produits", "Products", "المنتجات"),
                        onClick = onOpenProducts,
                        icon = Icons.Outlined.Inventory2,
                    )
                    if (canSell) {
                        AtlasOutlinedButton(
                            text = atlasText("Vente", "Sales", "المبيعات"),
                            onClick = onOpenSale,
                            icon = Icons.Outlined.PointOfSale,
                        )
                    } else {
                        AtlasOutlinedButton(
                            text = atlasText("Scanner", "Scan", "مسح ضوئي"),
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
    onSelectFinancePeriod: (String) -> Unit,
    onExportFinanceReport: (String) -> Unit,
    onOpenCombinedReport: () -> Unit,
) {
    val density = Atlas.density
    val canViewInvoices = Roles.canCreateDocuments(user.role)
    val canSell = Roles.canCreateDocuments(user.role)
    val isAdmin = Roles.isAdmin(user.role)
    // New finance endpoints enforce owner/admin server-side; only reveal the older
    // dashboard's financial fields after that protected endpoint has authorized us.
    val canDisplayFinance = isAdmin && state.finance != null
    val invoices = dashboard.recentInvoices.take(5)
    val isDaytimeGreeting = LocalTime.now(ZoneId.systemDefault()).hour in 5..17
    val greeting = if (isDaytimeGreeting) {
        atlasText("Bonjour", "Good morning", "صباح الخير")
    } else {
        atlasText("Bonsoir", "Good evening", "مساء الخير")
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = density.screenPadding,
            end = density.screenPadding,
            top = density.cardMargin,
            // Keep the last report heading and actions fully scrollable above the floating dock.
            bottom = 48.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(density.itemGap),
    ) {
        item(key = "dashboard_greeting") {
            AtlasEntrance(delayMillis = 0) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = atlasText("Voici l'essentiel de votre activité.", "Here is a snapshot of your business.", "إليك لمحة عن نشاطك التجاري."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.offline) {
            item { AtlasOfflineBanner() }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = atlasText("Résumé du jour", "Today's overview", "ملخص اليوم"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (state.refreshing) {
                    Text(
                        text = atlasText("Actualisation…", "Refreshing…", "جارٍ التحديث…"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser", "Refresh", "تحديث"))
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
                        text = atlasText("Produits", "Products", "المنتجات"),
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
                                text = if (isAdmin) atlasText("Demandes & notifications", "Requests & notifications", "الطلبات والإشعارات") else atlasText("Notifications", "Notifications", "الإشعارات"),

                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = atlasText(
                                    "Retrouvez ici vos demandes et notifications récentes.",
                                    "Your recent requests and notifications appear here.",
                                    "ستظهر طلباتك وإشعاراتك الأخيرة هنا.",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AtlasChip(
                            text = atlasText("Ouvrir", "Open", "فتح"),
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
                        label = atlasText("Produits", "Products", "المنتجات"),
                        value = AtlasFormat.integer(dashboard.productCount.toDouble()),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Inventory2,
                    )
                    if (canDisplayFinance) {
                        AtlasStatTile(
                            label = atlasText("Valeur du stock", "Inventory value", "قيمة المخزون"),
                            value = AtlasFormat.moneyCompact(dashboard.stockValue, state.currency),
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.TrendingUp,
                        )
                    } else {
                        AtlasStatTile(
                            label = atlasText("Stock faible", "Low stock", "مخزون منخفض"),
                            value = AtlasFormat.integer(dashboard.lowStockCount.toDouble()),
                            modifier = Modifier.weight(1f),
                            hint = if (dashboard.lowStockCount > 0) atlasText("À vérifier", "Needs review", "يحتاج إلى مراجعة") else atlasText("Optimal", "Optimal", "مثالي"),
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
                            label = atlasText("Stock faible", "Low stock", "مخزون منخفض"),
                            value = AtlasFormat.integer(dashboard.lowStockCount.toDouble()),
                            modifier = Modifier.weight(1f),
                            hint = if (dashboard.lowStockCount > 0) atlasText("À réapprovisionner", "Reorder needed", "يلزم إعادة الطلب") else atlasText("Tout est bon", "All good", "كل شيء على ما يرام"),
                            tone = if (dashboard.lowStockCount > 0) AtlasTone.WARNING else AtlasTone.POSITIVE,
                            icon = Icons.Outlined.Warning,
                        )
                        AtlasStatTile(
                            label = atlasText("Créances clients", "Customer receivables", "مستحقات العملاء"),
                            value = AtlasFormat.moneyCompact(dashboard.unpaidSales.total, state.currency),
                            modifier = Modifier.weight(1f),
                            hint = atlasText("${dashboard.unpaidSales.count} facture(s)", "${dashboard.unpaidSales.count} invoice(s)", "${dashboard.unpaidSales.count} فاتورة"),
                            tone = if (dashboard.unpaidSales.overdue > 0) AtlasTone.DANGER else AtlasTone.INFO,
                            icon = Icons.Outlined.ReceiptLong,
                        )
                    }
                }
            }
        }

        if (canDisplayFinance && dashboard.monthlySeries.isNotEmpty()) {
            item(key = "monthly_sales_trend") {
                AtlasEntrance(delayMillis = 165) {
                    MonthlySalesTrendCard(
                        points = dashboard.monthlySeries,
                        currency = state.currency,
                    )
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
                    selectedPeriod = state.selectedFinancePeriod,
                    onRefresh = onRefresh,
                    onSelectPeriod = onSelectFinancePeriod,
                    onLoadMorePayroll = onLoadMorePayroll,
                    onExportReport = onExportFinanceReport,
                    onOpenCombinedReport = onOpenCombinedReport,
                )
            }
        }

        if (dashboard.lowStockCount > 0) {
            item {
                AtlasCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AtlasChip(text = atlasText("Alerte stock", "Low-stock alert", "تنبيه انخفاض المخزون"), tone = AtlasTone.WARNING, icon = Icons.Outlined.Warning)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onOpenProducts) { Text(atlasText("Voir", "View", "عرض")) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = atlasText(
                        "${dashboard.lowStockCount} produit(s) sont au niveau ou sous le seuil d'alerte.",
                        "${dashboard.lowStockCount} product(s) are at or below the low-stock threshold.",
                        "${dashboard.lowStockCount} منتج عند حد المخزون المنخفض أو دونه.",
                    ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (canViewInvoices) {
            item {
                AtlasCard {
                    AtlasSectionHeader(
                        title = atlasText("Derniers documents", "Recent documents", "المستندات الأخيرة"),
                        subtitle = if (invoices.isEmpty()) null else atlasText("${invoices.size} récent(s)", "${invoices.size} recent", "${invoices.size} حديث"),
                    )
                    Spacer(Modifier.height(6.dp))
                    if (invoices.isEmpty()) {
                        AtlasEmptyState(
                            title = atlasText("Aucun document", "No documents", "لا توجد مستندات"),
                            message = atlasText("Les ventes et factures apparaîtront ici.", "Sales and invoices will appear here.", "ستظهر المبيعات والفواتير هنا."),
                            icon = Icons.Outlined.ReceiptLong,
                            actionLabel = atlasText("Créer une vente", "Create a sale", "إنشاء عملية بيع"),
                            onAction = onOpenSale,
                        )
                    } else {
                        invoices.forEach { invoice ->
                            InvoiceRow(invoice = invoice, currency = state.currency)
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = onOpenProducts) { Text(atlasText("Voir le catalogue", "View catalog", "عرض الكتالوج")) }
                    }
                }
            }
        }

    }
}

@Composable
private fun MonthlySalesTrendCard(points: List<MonthlyPointDto>, currency: String) {
    val recent = points.takeLast(6)
    if (recent.isEmpty()) return
    val peak = recent.maxOfOrNull { it.sales.coerceAtLeast(0.0) }?.takeIf { it > 0.0 } ?: 1.0
    val palette = Atlas.palette

    AtlasCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(atlasText("Tendance des ventes", "Sales trend", "اتجاه المبيعات"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(atlasText("Données réelles · ${recent.size} périodes", "Live data · ${recent.size} periods", "بيانات مباشرة · ${recent.size} فترة"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(112.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            recent.forEach { point ->
                val value = point.sales.coerceAtLeast(0.0)
                val barFraction = (value / peak).toFloat().coerceIn(0.04f, 1f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = AtlasFormat.moneyCompact(value, currency),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(62.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.52f)
                                .fillMaxHeight(barFraction)
                                .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                                .background(Brush.verticalGradient(listOf(palette.heroStart, palette.heroEnd))),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = point.label.ifBlank { point.key },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private data class FinanceChartEntry(
    val label: String,
    val amount: Double,
    val color: Color,
)

@Composable
private fun FinanceMiniBarChart(
    title: String,
    entries: List<FinanceChartEntry>,
    currency: String,
) {
    if (entries.isEmpty()) return
    val peak = entries.maxOfOrNull { kotlin.math.abs(it.amount) }?.takeIf { it > 0.0 } ?: 1.0
    AtlasCard(padding = 12.dp) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        entries.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = AtlasFormat.moneyCompact(entry.amount, currency),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((kotlin.math.abs(entry.amount) / peak).toFloat().coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(entry.color),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FinanceAdminCharts(summary: FinanceMonthlyDto) {
    val palette = Atlas.palette
    FinanceMiniBarChart(
        title = atlasText("Trésorerie · ouverture et clôture", "Cash position · opening and closing", "النقد المتاح · الافتتاح والإغلاق"),
        currency = summary.currency,
        entries = listOf(
            FinanceChartEntry(atlasText("Solde d'ouverture", "Opening balance", "رصيد الافتتاح"), summary.openingBalance, MaterialTheme.colorScheme.primary),
            FinanceChartEntry(atlasText("Solde de clôture", "Closing balance", "رصيد الإغلاق"), summary.closingBalance, palette.positive),
        ),
    )
    FinanceMiniBarChart(
        title = atlasText("Factures ouvertes · clients et fournisseurs", "Open invoices · receivables and payables", "الفواتير المفتوحة · العملاء والموردون"),
        currency = summary.currency,
        entries = listOf(
            FinanceChartEntry(atlasText("Clients · encours", "Receivables · outstanding", "مستحقات العملاء"), summary.receivables.totalOutstanding, MaterialTheme.colorScheme.primary),
            FinanceChartEntry(atlasText("Clients · en retard", "Receivables · overdue", "مستحقات العملاء المتأخرة"), summary.receivables.totalOverdue, palette.danger),
            FinanceChartEntry(atlasText("Fournisseurs · encours", "Payables · outstanding", "مستحقات الموردين"), summary.payables.totalOutstanding, palette.warning),
            FinanceChartEntry(atlasText("Fournisseurs · en retard", "Payables · overdue", "مستحقات الموردين المتأخرة"), summary.payables.totalOverdue, palette.danger),
        ),
    )
}

@Composable
private fun FinanceAdminCard(
    summary: FinanceMonthlyDto?,
    payroll: PayrollPageDto?,
    loading: Boolean,
    error: String?,
    reportGenerating: Boolean,
    payrollLoadingMore: Boolean,
    selectedPeriod: String?,
    onRefresh: () -> Unit,
    onSelectPeriod: (String) -> Unit,
    onLoadMorePayroll: () -> Unit,
    onExportReport: (String) -> Unit,
    onOpenCombinedReport: () -> Unit,
) {
    val financeTimezone = summary?.timezone?.takeIf { it.isNotBlank() } ?: "Africa/Casablanca"
    val currentFinancePeriod = financeCurrentPeriod(financeTimezone)
    val activePeriod = selectedPeriod ?: summary?.period ?: currentFinancePeriod
    AtlasCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(atlasText("Solde & mouvements"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = summary?.timezone?.takeIf { it.isNotBlank() }?.let {
                        atlasText("Relevé mensuel · $it", "Monthly statement · $it", "الكشف الشهري · $it")
                    } ?: atlasText("Relevé mensuel", "Monthly statement", "الكشف الشهري"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (loading && summary == null) {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser les finances"))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(atlasText("Période du relevé"), style = MaterialTheme.typography.labelMedium)
                Text(
                    if (activePeriod == currentFinancePeriod) {
                        atlasText("Le mois en cours est disponible avant clôture.", "The current month is available before closing.", "الشهر الحالي متاح قبل الإغلاق.")
                    } else {
                        atlasText("Consultez un mois passé ou en cours.", "View a past or current month.", "اعرض شهراً سابقاً أو الشهر الحالي.")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FinancePeriodPicker(period = activePeriod, timezone = financeTimezone, onSelect = onSelectPeriod)
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(8.dp))
        AtlasDivider()
        Text(atlasText("Rapports PDF"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            atlasText("Relevé bancaire du mois sélectionné, y compris le mois en cours."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        AtlasOutlinedButton(
            text = if (reportGenerating) {
                atlasText("Préparation du relevé…", "Preparing statement…", "جارٍ إعداد الكشف…")
            } else {
                atlasText("Télécharger le PDF · ${financeMonthLabel(activePeriod)}", "Download PDF · ${financeMonthLabel(activePeriod)}", "تنزيل PDF · ${financeMonthLabel(activePeriod)}")
            },
            onClick = { onExportReport(activePeriod) },
            enabled = !reportGenerating && !loading,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Outlined.Description,
        )
        Spacer(Modifier.height(6.dp))
        AtlasOutlinedButton(
            text = atlasText("Pack rapports détaillés · ouvrir dans Atlas"),
            onClick = onOpenCombinedReport,
            enabled = !reportGenerating && !loading,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Outlined.ReceiptLong,
        )
        Text(
            atlasText("Choisissez les rubriques et la période : mois, année en cours, derniers mois ou dates personnalisées. Le PDF bancaire reste mensuel."),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (summary != null) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtlasStatTile(
                    label = atlasText("Solde clôture"),
                    value = AtlasFormat.moneyCompact(summary.closingBalance, summary.currency),
                    modifier = Modifier.weight(1f),
                    hint = "Ouverture ${AtlasFormat.moneyCompact(summary.openingBalance, summary.currency)}",
                    tone = AtlasTone.POSITIVE,
                )
                AtlasStatTile(
                    label = atlasText("Paie à verser"),
                    value = AtlasFormat.moneyCompact(summary.payroll.totalDue, summary.currency),
                    modifier = Modifier.weight(1f),
                    hint = "${summary.payroll.employees} employé(s)",
                    tone = if (summary.payroll.totalDue > 0.0) AtlasTone.WARNING else AtlasTone.POSITIVE,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtlasStatTile(
                    label = atlasText("Clients à recevoir"),
                    value = AtlasFormat.moneyCompact(summary.receivables.totalOutstanding, summary.currency),
                    modifier = Modifier.weight(1f),
                    hint = "${summary.receivables.count} facture(s) · ${AtlasFormat.moneyCompact(summary.receivables.totalOverdue, summary.currency)} en retard",
                    tone = if (summary.receivables.totalOverdue > 0.0) AtlasTone.DANGER else AtlasTone.INFO,
                )
                AtlasStatTile(
                    label = atlasText("Fournisseurs à payer"),
                    value = AtlasFormat.moneyCompact(summary.payables.totalOutstanding, summary.currency),
                    modifier = Modifier.weight(1f),
                    hint = "${summary.payables.count} facture(s) · ${AtlasFormat.moneyCompact(summary.payables.totalOverdue, summary.currency)} en retard",
                    tone = if (summary.payables.totalOverdue > 0.0) AtlasTone.DANGER else AtlasTone.WARNING,
                )
            }
            Spacer(Modifier.height(10.dp))
            FinanceAdminCharts(summary)
            Spacer(Modifier.height(10.dp))
            AtlasDivider()
            AtlasInfoRow("Recettes encaissées", AtlasFormat.money(summary.salesReceived, summary.currency))
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
            Text(atlasText("Équipe · paie à préparer"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            when {
                loading && payroll == null -> {
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                payroll == null && error != null -> {
                    Spacer(Modifier.height(4.dp))
                    Text(atlasText("La liste de paie n'a pas pu être chargée."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                payroll == null || payroll.rows.isEmpty() -> {
                    Spacer(Modifier.height(4.dp))
                    Text(atlasText("Aucun versement dû pour cette période."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> payroll.rows.forEach { employee ->
                    AtlasDivider(Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(employee.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                text = buildString {
                                    append(atlasText("Échéance ${AtlasFormat.shortDate(employee.dueDate)}", "Due ${AtlasFormat.shortDate(employee.dueDate)}", "الاستحقاق ${AtlasFormat.shortDate(employee.dueDate)}"))
                                    employee.lastPayment?.let {
                                        append(atlasText(" · Dernier versement ${AtlasFormat.shortDate(it.paidAt)}", " · Last payment ${AtlasFormat.shortDate(it.paidAt)}", " · آخر دفعة ${AtlasFormat.shortDate(it.paidAt)}"))
                                    }
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
                                isFutureDue -> atlasText("À venir", "Upcoming", "قادم")
                                employee.status.equals("paid", ignoreCase = true) -> atlasText("Payé", "Paid", "مدفوع")
                                else -> atlasText("À payer", "To pay", "مستحق الدفع")
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
                        atlasText("${payroll.rows.size} sur ${payroll.total} employés", "${payroll.rows.size} of ${payroll.total} employees", "${payroll.rows.size} من أصل ${payroll.total} موظف"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onLoadMorePayroll, enabled = !payrollLoadingMore) {
                        if (payrollLoadingMore) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(atlasText("Afficher plus"))
                        }
                    }
                }
            }
            summary.reconciliation.asOfDate?.let { date ->
                Spacer(Modifier.height(4.dp))
                val currentMonth = activePeriod == currentFinancePeriod
                Text(
                    text = if (currentMonth) {
                        atlasText(
                            "Mois en cours · arrêté au ${AtlasFormat.shortDate(date)} · source ${summary.reconciliation.source}",
                            "Current month · as of ${AtlasFormat.shortDate(date)} · source ${summary.reconciliation.source}",
                            "الشهر الحالي · حتى ${AtlasFormat.shortDate(date)} · المصدر ${summary.reconciliation.source}",
                        )
                    } else {
                        atlasText(
                            "Période ${financeMonthLabel(activePeriod)} · données au ${AtlasFormat.shortDate(date)} · source ${summary.reconciliation.source}",
                            "Period ${financeMonthLabel(activePeriod)} · data as of ${AtlasFormat.shortDate(date)} · source ${summary.reconciliation.source}",
                            "الفترة ${financeMonthLabel(activePeriod)} · البيانات حتى ${AtlasFormat.shortDate(date)} · المصدر ${summary.reconciliation.source}",
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (error == null && !loading) {
            Spacer(Modifier.height(6.dp))
            Text(atlasText("Aucune donnée financière reçue."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FinancePeriodPicker(
    period: String,
    timezone: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentPeriod = financeCurrentPeriod(timezone)
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(financeMonthLabel(period))
            Text("  ▾")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            val currentMonth = YearMonth.parse(currentPeriod)
            for (offset in 0L..35L) {
                val option = currentMonth.minusMonths(offset).toString()
                DropdownMenuItem(
                    text = { Text(financeMonthLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

private fun financeCurrentPeriod(timezone: String): String = runCatching {
    YearMonth.now(ZoneId.of(timezone)).toString()
}.getOrElse {
    YearMonth.now(ZoneId.of("Africa/Casablanca")).toString()
}

private fun financeMonthLabel(period: String): String = runCatching {
    YearMonth.parse(period)
        .format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.FRENCH))
        .replaceFirstChar { it.uppercase() }
}.getOrDefault(period)

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
        Text(atlasText("Aucun montant en attente."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        "overdue" -> atlasText("En retard", "Overdue", "متأخر")
                        "partial" -> atlasText("Partiel", "Partial", "جزئي")
                        else -> atlasText("À régler", "To pay", "مستحق الدفع")
                    },
                    tone = tone,
                )
            }
        }
    }
    if (documents.size > 5) {
        Text(atlasText("${documents.size - 5} autre(s) document(s) dans le rapport.", "${documents.size - 5} more document(s) in the report.", "${documents.size - 5} مستندات إضافية في التقرير."), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            text = atlasText("SOLDE DE TRÉSORERIE"),
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
            text = atlasText("CATALOGUE & STOCK"),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = atlasText(
                "${AtlasFormat.integer(productCount.toDouble())} articles actifs",
                "${AtlasFormat.integer(productCount.toDouble())} active products",
                "${AtlasFormat.integer(productCount.toDouble())} منتج نشط",
            ),
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
private fun InvoiceRow(invoice: InvoiceDto, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = invoice.number?.takeIf { it.isNotBlank() } ?: atlasText("Document #${invoice.id}", "Document #${invoice.id}", "مستند رقم ${invoice.id}"),
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
