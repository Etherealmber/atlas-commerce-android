package ma.etherealmber.atlascommerce.ui.screens.home

import ma.etherealmber.atlascommerce.ui.components.rememberApiRefreshAction

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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import java.util.Locale
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.DashboardDto
import ma.etherealmber.atlascommerce.data.model.FinanceMonthlyDto
import ma.etherealmber.atlascommerce.data.model.FinanceOutstandingDocumentDto
import ma.etherealmber.atlascommerce.data.model.InvoiceDto
import ma.etherealmber.atlascommerce.data.model.PayrollPageDto
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingDockPadding
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingTopBarPadding
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasInfoRow
import ma.etherealmber.atlascommerce.ui.components.AtlasDashboardSkeleton
import ma.etherealmber.atlascommerce.ui.components.AtlasFinanceSkeleton
import ma.etherealmber.atlascommerce.ui.components.AtlasOfflineBanner
import ma.etherealmber.atlascommerce.ui.components.AtlasOutlinedButton
import ma.etherealmber.atlascommerce.ui.components.AtlasSectionHeader
import ma.etherealmber.atlascommerce.ui.components.AtlasStatTile
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.FinancePdfViewerDialog
import ma.etherealmber.atlascommerce.ui.components.FinanceReportHistoryDialog
import ma.etherealmber.atlascommerce.ui.components.FinanceReportActionsDialog
import ma.etherealmber.atlascommerce.ui.components.FinanceReportSelection
import ma.etherealmber.atlascommerce.ui.components.FinanceReportSelectionDialog
import ma.etherealmber.atlascommerce.ui.components.NativeFinanceReportDialog
import ma.etherealmber.atlascommerce.ui.components.financeMonthLabel as sharedFinanceMonthLabel
import ma.etherealmber.atlascommerce.ui.components.financeReportTitle as sharedFinanceReportTitle
import ma.etherealmber.atlascommerce.ui.components.StatusChip
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.i18n.atlasLocale
import ma.etherealmber.atlascommerce.ui.i18n.atlasShortDate
import ma.etherealmber.atlascommerce.ui.theme.Atlas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: SessionUser,
    onOpenProducts: () -> Unit,
    onOpenProductsAlerts: () -> Unit = onOpenProducts,
    onOpenSale: () -> Unit,
    onOpenScan: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onToast: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshDashboard = rememberApiRefreshAction("home-dashboard", 30_000L, viewModel::refresh)
    val refreshHomeFinance = rememberApiRefreshAction("home-finance", 30_000L, viewModel::refreshFinance)
    val dashboard = state.dashboard
    val isAdmin = Roles.isAdmin(user.role)
    val context = LocalContext.current
    val atlasLanguage = LocalAtlasLanguage.current
    val shareReportChooserTitle = atlasText("Partager le rapport PDF", "Share PDF report", "مشاركة تقرير PDF")
    var pendingReportFile by remember { mutableStateOf<java.io.File?>(null) }
    var showReportActions by remember { mutableStateOf(false) }
    var pdfViewerFile by remember { mutableStateOf<java.io.File?>(null) }
    var showNativeFinanceReport by remember { mutableStateOf(false) }
    var reportSelection by remember { mutableStateOf(FinanceReportSelection()) }
    var showReportSelection by remember { mutableStateOf(false) }
    var showFinanceReportHistory by remember { mutableStateOf(false) }
    var financeReportHistory by remember(user.scopeIdentity) { mutableStateOf<List<java.io.File>>(emptyList()) }
    var financeReportHistoryLoading by remember(user.scopeIdentity) { mutableStateOf(false) }

    val activeScope = rememberUpdatedState(user.scopeIdentity)
    var latestFinanceHistoryRequest by remember { mutableStateOf(0L) }
    val refreshFinanceReportHistory = {
        val requestScope = user.scopeIdentity
        val requestId = ++latestFinanceHistoryRequest
        financeReportHistoryLoading = true
        viewModel.loadFinanceReportHistory(context.cacheDir) { result ->
            if (!isCurrentFinanceReportHistoryRequest(
                    requestId = requestId,
                    latestRequestId = latestFinanceHistoryRequest,
                    requestedScope = requestScope,
                    activeScope = activeScope.value,
                )
            ) {
                return@loadFinanceReportHistory
            }
            financeReportHistoryLoading = false
            when (result) {
                is ApiResult.Failure -> {
                    financeReportHistory = emptyList()
                    onToast(atlasNotice(result.message, atlasLanguage))
                }
                is ApiResult.Success -> financeReportHistory = result.data
            }
        }
    }
    val gatedRefreshFinanceReportHistory = rememberApiRefreshAction(
        "finance-report-history", 5_000L, refreshFinanceReportHistory,
    )

    LaunchedEffect(user.scopeIdentity) {
        showFinanceReportHistory = false
        financeReportHistoryLoading = false
        pendingReportFile = null
        showReportActions = false
        pdfViewerFile = null
        showNativeFinanceReport = false
    }

    if (showReportSelection) {
        FinanceReportSelectionDialog(
            initial = reportSelection,
            onDismiss = { showReportSelection = false },
            onGenerate = { selection ->
                reportSelection = selection
                showReportSelection = false
                showNativeFinanceReport = true
            },
        )
    }

    if (showNativeFinanceReport) {
        NativeFinanceReportDialog(
            selection = reportSelection,
            onDismiss = { showNativeFinanceReport = false },
            fallbackCurrency = state.currency,
        )
    }
    if (showFinanceReportHistory) {
        FinanceReportHistoryDialog(
            reports = financeReportHistory,
            loading = financeReportHistoryLoading,
            onDismiss = { showFinanceReportHistory = false },
            onRefresh = gatedRefreshFinanceReportHistory,
            onSelect = { file ->
                showFinanceReportHistory = false
                pendingReportFile = file
                showReportActions = true
            },
        )
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
            }.onSuccess { onToast("Copie PDF enregistrée.") }
                .onFailure { onToast("Impossible d'enregistrer le PDF.") }
        }
        pendingReportFile = null
    }

    pendingReportFile?.takeIf { showReportActions }?.let { reportFile ->
        FinanceReportActionsDialog(
            fileName = financeReportTitle(reportFile),
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
                    context.startActivity(Intent.createChooser(shareIntent, shareReportChooserTitle))
                }.onFailure { onToast("Impossible de partager le PDF.") }
                pendingReportFile = null
            },
            onSave = {
                showReportActions = false
                saveReportLauncher.launch(financeReportExportFileName(reportFile))
            },
            onDelete = {
                showReportActions = false
                reportFile.delete()
                financeReportHistory = financeReportHistory.filterNot { it.absolutePath == reportFile.absolutePath }
                pendingReportFile = null
                onToast("Rapport temporaire supprimé.")
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
                    context.startActivity(Intent.createChooser(shareIntent, shareReportChooserTitle))
                }.onFailure { onToast("Impossible de partager le PDF.") }
            },
            onSave = { pendingReportFile = reportFile; saveReportLauncher.launch(financeReportExportFileName(reportFile)) },
            onDelete = {
                reportFile.delete()
                financeReportHistory = financeReportHistory.filterNot { it.absolutePath == reportFile.absolutePath }
                pdfViewerFile = null
                onToast("Rapport temporaire supprimé.")
            },
        )
    }

    LaunchedEffect(state.offline) {
        if (state.offline) onToast("Hors ligne — dernières données affichées.")
    }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = refreshDashboard,
        modifier = Modifier.fillMaxSize(),
    ) {
    when {
        state.loading && dashboard == null -> AtlasDashboardSkeleton(
            modifier = Modifier.fillMaxSize(),
            isAdmin = isAdmin,
            showFinancialHero = isAdmin && state.finance != null,
            offline = state.offline,
            contentPadding = PaddingValues(
                start = Atlas.density.screenPadding,
                end = Atlas.density.screenPadding,
                top = Atlas.density.cardMargin + LocalAtlasFloatingTopBarPadding.current,
                bottom = 48.dp + LocalAtlasFloatingDockPadding.current,
            ),
            itemGap = Atlas.density.itemGap,
        )
        dashboard == null -> HomeErrorContent(
            user = user,
            message = atlasNotice(state.error ?: "Vérifiez l'adresse du serveur et votre connexion.", LocalAtlasLanguage.current),
            offline = state.offline,
            onRetry = refreshDashboard,
            onOpenProducts = onOpenProducts,
            onOpenSale = onOpenSale,
            onOpenScan = onOpenScan,
        )

        else -> HomeContent(
            user = user,
            state = state,
            dashboard = dashboard,
            onRefresh = refreshDashboard,
            onRefreshFinance = refreshHomeFinance,
            onOpenProducts = onOpenProducts,
            onOpenProductsAlerts = onOpenProductsAlerts,
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
            onOpenFinanceReportHistory = {
                showFinanceReportHistory = true
                gatedRefreshFinanceReportHistory()
            },
        )
    }
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
    val dockPadding = LocalAtlasFloatingDockPadding.current
    val canSell = Roles.canCreateDocuments(user.role)
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = density.cardMargin + LocalAtlasFloatingTopBarPadding.current,
            bottom = 28.dp + dockPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
    onRefreshFinance: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenProductsAlerts: () -> Unit,
    onOpenSale: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLoadMorePayroll: () -> Unit,
    onSelectFinancePeriod: (String) -> Unit,
    onExportFinanceReport: (String) -> Unit,
    onOpenCombinedReport: () -> Unit,
    onOpenFinanceReportHistory: () -> Unit,
) {
    val density = Atlas.density
    val dockPadding = LocalAtlasFloatingDockPadding.current
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
            start = 16.dp,
            end = 16.dp,
            top = density.cardMargin + LocalAtlasFloatingTopBarPadding.current,
            // Keep the final cards scrollable clear of the floating dock while intermediate content passes behind it.
            bottom = 48.dp + dockPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "dashboard_greeting") {
            AtlasEntrance(delayMillis = 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = atlasText("Voici l'essentiel de votre activité.", "Here is a snapshot of your business.", "إليك لمحة عن نشاطك التجاري."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.refreshing,
                        modifier = Modifier.size(48.dp),
                    ) {
                        if (state.refreshing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = atlasText("Actualiser", "Refresh", "تحديث"),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        if (state.offline) {
            item { AtlasOfflineBanner() }
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

        item(key = "home_kpi_carousel") {
            DailyKpiCarousel(
                metrics = state.dailyMetrics,
                loading = state.metricsLoading,
                error = state.metricsError,
                onRetry = onRefresh,
                currency = state.currency,
            )
        }
        item(key = "home_money_flow") {
            if (state.metricsLoading && state.dailyMetrics == null) HomeMetricsChartSkeleton(188.dp)
            else MoneyFlowChart(state.dailyMetrics, state.currency)
        }
        item(key = "home_stock_health") {
            if (state.metricsLoading && state.dailyMetrics == null) HomeMetricsChartSkeleton(198.dp)
            else StockHealthChart(state.dailyMetrics, onOpenProductsAlerts)
        }

        // Primary role-gated action bar
        item {
            AtlasEntrance(delayMillis = 45) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (canSell) {
                        AtlasButton(
                            text = atlasText("Nouvelle vente", "New sale", "بيع جديد"),
                            onClick = onOpenSale,
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.PointOfSale,
                        )
                    } else {
                        AtlasButton(
                            text = atlasText("Scanner", "Scan", "مسح ضوئي"),
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
                        value = homeInteger(dashboard.productCount.toDouble()),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Inventory2,
                    )
                    if (canDisplayFinance) {
                        AtlasStatTile(
                            label = atlasText("Valeur du stock", "Inventory value", "قيمة المخزون"),
                            value = homeMoneyCompact(dashboard.stockValue, state.currency),
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.TrendingUp,
                        )
                    } else {
                        AtlasStatTile(
                            label = atlasText("Stock faible", "Low stock", "مخزون منخفض"),
                            value = homeInteger(dashboard.lowStockCount.toDouble()),
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
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(density.itemGap),
                    ) {
                        AtlasStatTile(
                            label = atlasText("Stock faible", "Low stock", "مخزون منخفض"),
                            value = homeInteger(dashboard.lowStockCount.toDouble()),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            hint = if (dashboard.lowStockCount > 0) atlasText("À réapprovisionner", "Reorder needed", "يلزم إعادة الطلب") else atlasText("Tout est bon", "All good", "كل شيء على ما يرام"),
                            tone = if (dashboard.lowStockCount > 0) AtlasTone.WARNING else AtlasTone.POSITIVE,
                            icon = Icons.Outlined.Warning,
                        )
                        AtlasStatTile(
                            label = atlasText("Créances clients", "Customer receivables", "مستحقات العملاء"),
                            value = homeMoneyCompact(dashboard.unpaidSales.total, state.currency),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            hint = atlasText("${dashboard.unpaidSales.count} facture(s)", "${dashboard.unpaidSales.count} invoice(s)", "${dashboard.unpaidSales.count} فاتورة"),
                            tone = if (dashboard.unpaidSales.overdue > 0) AtlasTone.DANGER else AtlasTone.INFO,
                            icon = Icons.Outlined.ReceiptLong,
                        )
                    }
                }
            }
        }

        if (dashboard.monthlySeries.isNotEmpty()) {
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
            item(key = "home_finance") {
                if (state.financeLoading && state.finance == null) {
                    AtlasFinanceSkeleton(Modifier.fillMaxWidth())
                } else {
                    FinanceAdminCard(
                        summary = state.finance,
                        payroll = state.payroll,
                        loading = state.financeLoading,
                        error = state.financeError,
                        reportGenerating = state.reportGenerating,
                        payrollLoadingMore = state.payrollLoadingMore,
                        selectedPeriod = state.selectedFinancePeriod,
                        onRefresh = onRefreshFinance,
                        onSelectPeriod = onSelectFinancePeriod,
                        onLoadMorePayroll = onLoadMorePayroll,
                        onExportReport = onExportFinanceReport,
                        onOpenCombinedReport = onOpenCombinedReport,
                        onOpenReportHistory = onOpenFinanceReportHistory,
                    )
                }
            }
        }

        if (dashboard.lowStockCount > 0) {
            item {
                AtlasCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AtlasChip(text = atlasText("Alerte stock", "Low-stock alert", "تنبيه انخفاض المخزون"), tone = AtlasTone.WARNING, icon = Icons.Outlined.Warning)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onOpenProductsAlerts, modifier = Modifier.height(48.dp)) { Text(atlasText("Voir", "View", "عرض")) }
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
    onOpenReportHistory: () -> Unit,
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
            if (loading) {
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
        summary?.let { FinanceMovementFlowChart(it) }
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
                atlasText("Télécharger le PDF · ${financeMonthLabel(activePeriod, atlasLocale())}", "Download PDF · ${financeMonthLabel(activePeriod, atlasLocale())}", "تنزيل PDF · ${financeMonthLabel(activePeriod, atlasLocale())}")
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
        Spacer(Modifier.height(6.dp))
        AtlasOutlinedButton(
            text = atlasText("Historique des rapports", "Report history", "سجل التقارير"),
            onClick = onOpenReportHistory,
            enabled = !reportGenerating,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Outlined.History,
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
                    value = homeMoneyCompact(summary.closingBalance, summary.currency),
                    modifier = Modifier.weight(1f),
                    hint = atlasText("Ouverture ${homeMoneyCompact(summary.openingBalance, summary.currency)}", "Opening ${homeMoneyCompact(summary.openingBalance, summary.currency)}", "الافتتاحي ${homeMoneyCompact(summary.openingBalance, summary.currency)}"),
                    tone = AtlasTone.POSITIVE,
                )
                AtlasStatTile(
                    label = atlasText("Paie à verser"),
                    value = homeMoneyCompact(summary.payroll.totalDue, summary.currency),
                    modifier = Modifier.weight(1f),
                    hint = atlasText("${summary.payroll.employees} employé(s)", "${summary.payroll.employees} employees", "${summary.payroll.employees} موظف"),
                    tone = if (summary.payroll.totalDue > 0.0) AtlasTone.WARNING else AtlasTone.POSITIVE,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtlasStatTile(
                    label = atlasText("Clients à recevoir"),
                    value = homeMoneyCompact(summary.receivables.totalOutstanding, summary.currency),
                    modifier = Modifier.weight(1f),
                    hint = atlasText("${summary.receivables.count} facture(s) · ${homeMoneyCompact(summary.receivables.totalOverdue, summary.currency)} en retard", "${summary.receivables.count} invoices · ${homeMoneyCompact(summary.receivables.totalOverdue, summary.currency)} overdue", "${summary.receivables.count} فاتورة · ${homeMoneyCompact(summary.receivables.totalOverdue, summary.currency)} متأخر"),
                    tone = if (summary.receivables.totalOverdue > 0.0) AtlasTone.DANGER else AtlasTone.INFO,
                )
                AtlasStatTile(
                    label = atlasText("Fournisseurs à payer"),
                    value = homeMoneyCompact(summary.payables.totalOutstanding, summary.currency),
                    modifier = Modifier.weight(1f),
                    hint = atlasText("${summary.payables.count} facture(s) · ${homeMoneyCompact(summary.payables.totalOverdue, summary.currency)} en retard", "${summary.payables.count} invoices · ${homeMoneyCompact(summary.payables.totalOverdue, summary.currency)} overdue", "${summary.payables.count} فاتورة · ${homeMoneyCompact(summary.payables.totalOverdue, summary.currency)} متأخر"),
                    tone = if (summary.payables.totalOverdue > 0.0) AtlasTone.DANGER else AtlasTone.WARNING,
                )
            }
            Spacer(Modifier.height(10.dp))
            AtlasDivider()
            AtlasInfoRow(atlasText("Recettes encaissées", "Receipts collected", "الإيرادات المحصّلة"), homeMoney(summary.salesReceived, summary.currency))
            AtlasInfoRow(atlasText("Dépenses (remboursements inclus)", "Expenses (including refunds)", "المصروفات (بما فيها المبالغ المستردة)"), homeMoney(summary.expenses, summary.currency))
            AtlasInfoRow(atlasText("Remboursements", "Refunds", "المبالغ المستردة"), homeMoney(summary.refunds, summary.currency))
            AtlasInfoRow(atlasText("Salaires versés ce mois", "Salaries paid this month", "الرواتب المدفوعة هذا الشهر"), homeMoney(summary.payrollPaid, summary.currency))
            AtlasInfoRow(atlasText("Fournisseurs réglés ce mois", "Suppliers paid this month", "مدفوعات الموردين هذا الشهر"), homeMoney(summary.supplierPaid, summary.currency))

            FinanceObligationsList(
                title = atlasText("Clients à encaisser", "Customer payments to collect", "مستحقات العملاء للتحصيل"),
                documents = summary.receivables.documents,
                currency = summary.currency,
            )
            FinanceObligationsList(
                title = atlasText("Factures fournisseurs à régler", "Supplier invoices to pay", "فواتير الموردين المستحقة"),
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
                payroll == null && summary.payroll.totalDue > 0.0 -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        atlasText("${homeMoney(summary.payroll.totalDue, summary.currency)} dus pour ${summary.payroll.employees} employé(s).", "${homeMoney(summary.payroll.totalDue, summary.currency)} due for ${summary.payroll.employees} employee(s).", "${homeMoney(summary.payroll.totalDue, summary.currency)} مستحقة لـ ${summary.payroll.employees} موظف."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onLoadMorePayroll, enabled = !payrollLoadingMore, modifier = Modifier.height(48.dp)) {
                        if (payrollLoadingMore) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text(atlasText("Afficher les détails", "Load payroll details", "عرض تفاصيل الرواتب"))
                    }
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
                                    append(atlasText("Échéance ${atlasShortDate(employee.dueDate)}", "Due ${atlasShortDate(employee.dueDate)}", "الاستحقاق ${atlasShortDate(employee.dueDate)}"))
                                    employee.lastPayment?.let {
                                        append(atlasText(" · Dernier versement ${atlasShortDate(it.paidAt)}", " · Last payment ${atlasShortDate(it.paidAt)}", " · آخر دفعة ${atlasShortDate(it.paidAt)}"))
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(homeMoney(employee.amountDue, payroll.currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val isFutureDue = employee.status.equals("paid", ignoreCase = true) &&
                                employee.dueDate?.let { it > payroll.asOf } == true
                            val statusLabel = when {
                                isFutureDue -> atlasText("À venir", "Upcoming", "قادم")
                                employee.status.equals("paid", ignoreCase = true) -> atlasText("Payé", "Paid", "مدفوع")
                                else -> atlasText("À payer", "To pay", "مستحق الدفع")
                            }
                            AtlasChip(
                                text = statusLabel,
                                tone = when {
                                    isFutureDue -> AtlasTone.INFO
                                    employee.status.equals("paid", ignoreCase = true) -> AtlasTone.POSITIVE
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
                            "Mois en cours · arrêté au ${atlasShortDate(date)} · source ${summary.reconciliation.source}",
                            "Current month · as of ${atlasShortDate(date)} · source ${summary.reconciliation.source}",
                            "الشهر الحالي · حتى ${atlasShortDate(date)} · المصدر ${summary.reconciliation.source}",
                        )
                    } else {
                        atlasText(
                            "Période ${financeMonthLabel(activePeriod, atlasLocale())} · données au ${atlasShortDate(date)} · source ${summary.reconciliation.source}",
                            "Period ${financeMonthLabel(activePeriod, atlasLocale())} · data as of ${atlasShortDate(date)} · source ${summary.reconciliation.source}",
                            "الفترة ${financeMonthLabel(activePeriod, atlasLocale())} · البيانات حتى ${atlasShortDate(date)} · المصدر ${summary.reconciliation.source}",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinancePeriodPicker(
    period: String,
    timezone: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentPeriod = financeCurrentPeriod(timezone)
    TextButton(onClick = { expanded = true }, modifier = Modifier.height(48.dp)) {
        Text(financeMonthLabel(period, atlasLocale()))
        Text("  ▾")
    }
    if (expanded) {
        val currentMonth = YearMonth.parse(currentPeriod)
        val options = (0L..35L).map { currentMonth.minusMonths(it).toString() }
        ModalBottomSheet(onDismissRequest = { expanded = false }) {
            Text(
                atlasText("Choisir un mois", "Choose a month", "اختر شهراً"),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            LazyColumn {
                items(options) { option ->
                    TextButton(
                        onClick = { expanded = false; onSelect(option) },
                        modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp),
                    ) {
                        Text(financeMonthLabel(option, atlasLocale()), Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

internal fun isCurrentFinanceReportHistoryRequest(
    requestId: Long,
    latestRequestId: Long,
    requestedScope: String,
    activeScope: String,
): Boolean = requestId == latestRequestId && requestedScope == activeScope

private fun financeCurrentPeriod(timezone: String): String = runCatching {
    YearMonth.now(ZoneId.of(timezone)).toString()
}.getOrElse {
    YearMonth.now(ZoneId.of("Africa/Casablanca")).toString()
}

private fun financeMonthLabel(period: String, locale: Locale): String =
    sharedFinanceMonthLabel(period, locale)

@Composable
private fun financeReportTitle(file: java.io.File): String = sharedFinanceReportTitle(file)

private fun financeReportExportFileName(file: java.io.File): String {
    val period = Regex("^rapport-mensuel-(\\d{4}-(?:0[1-9]|1[0-2]))(?:-\\d+)?$")
        .find(file.nameWithoutExtension)?.groupValues?.getOrNull(1)
    return period?.let { "atlas-releve-$it.pdf" } ?: file.name
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
                    text = listOfNotNull(document.number.takeIf { it.isNotBlank() }, document.dueDate?.let { date -> atlasShortDate(date) }).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(homeMoney(document.remaining, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
            color = palette.heroMutedText.copy(alpha = 0.82f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = homeMoney(balance, currency),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = palette.heroText,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroPill(text = atlasText("Stock ${homeMoneyCompact(stockValue, currency)}", "Inventory ${homeMoneyCompact(stockValue, currency)}", "المخزون ${homeMoneyCompact(stockValue, currency)}"), modifier = Modifier.weight(1f))
            HeroPill(text = atlasText("Créances ${homeMoneyCompact(receivable, currency)}", "Receivables ${homeMoneyCompact(receivable, currency)}", "المستحقات ${homeMoneyCompact(receivable, currency)}"), modifier = Modifier.weight(1f))
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
            color = palette.heroMutedText.copy(alpha = 0.82f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = atlasText(
                "${homeInteger(productCount.toDouble())} articles actifs",
                "${homeInteger(productCount.toDouble())} active products",
                "${homeInteger(productCount.toDouble())} منتج نشط",
            ),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = palette.heroText,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroPill(
                text = if (lowStockCount > 0) {
                    atlasText("$lowStockCount article(s) en alerte stock", "$lowStockCount items low on stock", "$lowStockCount منتج منخفض المخزون")
                } else {
                    atlasText("Aucune rupture critique", "No critical stock shortages", "لا يوجد نقص حرج في المخزون")
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeroPill(text: String, modifier: Modifier = Modifier) {
    val palette = Atlas.palette
    Surface(
        modifier = modifier,
        color = palette.heroPill,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = palette.heroText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                    atlasShortDate(invoice.issueDate).takeIf { it != "—" },
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
                text = homeMoney(invoice.total, currency),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            StatusChip(status = invoice.status)
        }
    }
}
