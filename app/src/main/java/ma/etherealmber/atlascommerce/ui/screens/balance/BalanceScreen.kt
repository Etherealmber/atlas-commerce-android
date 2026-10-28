package ma.etherealmber.atlascommerce.ui.screens.balance

import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.data.model.CreateFinanceEntryRequest
import ma.etherealmber.atlascommerce.data.model.FinancePeriodDto
import ma.etherealmber.atlascommerce.data.model.FinanceOutstandingDocumentDto
import ma.etherealmber.atlascommerce.data.model.FinanceStatementRowDto
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingDockPadding
import ma.etherealmber.atlascommerce.ui.components.AtlasLoading
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasLocale
import ma.etherealmber.atlascommerce.ui.i18n.atlasShortDate
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.components.CombinedReportWebViewDialog
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentNavigationSuggestion
import ma.etherealmber.atlascommerce.ui.components.FinancePdfViewerDialog
import ma.etherealmber.atlascommerce.ui.components.FinanceReportSelection
import ma.etherealmber.atlascommerce.ui.components.FinanceReportSelectionDialog
import java.io.File
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private enum class BalanceSection { MOVEMENTS, CLIENTS, REPORTS }

@Composable
fun BalanceScreen(
    onToast: (String) -> Unit,
    requestedReport: AgentNavigationSuggestion? = null,
    onReportRequestHandled: () -> Unit = {},
    viewModel: BalanceViewModel = hiltViewModel(),
) {
    val dockPadding = LocalAtlasFloatingDockPadding.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appLanguage = LocalAtlasLanguage.current
    val context = LocalContext.current
    val copyPdfSavedMessage = atlasText("Copie PDF enregistrée.", "PDF copy saved.", "تم حفظ نسخة PDF.")
    val savePdfFailedMessage = atlasText("Impossible d'enregistrer le PDF.", "Could not save the PDF.", "تعذّر حفظ PDF.")
    val sharePdfFailedMessage = atlasText("Impossible de partager le PDF.", "Could not share the PDF.", "تعذّرت مشاركة PDF.")
    val reportDeletedMessage = atlasText("Rapport temporaire supprimé.", "Temporary report deleted.", "تم حذف التقرير المؤقت.")
    val entrySavedMessage = atlasText("Écriture enregistrée dans la balance.", "Entry saved to the balance.", "تم حفظ القيد في الرصيد.")
    val reportDownloadedMessage = atlasText("PDF téléchargé et ouvert dans Atlas.", "PDF downloaded and opened in Atlas.", "تم تنزيل PDF وفتحه في Atlas.")
    val chooserTitle = atlasText("Partager le rapport PDF", "Share PDF report", "مشاركة تقرير PDF")
    var section by remember { mutableStateOf(BalanceSection.MOVEMENTS) }
    var pdfViewerFile by remember { mutableStateOf<File?>(null) }
    var pendingPdfFile by remember { mutableStateOf<File?>(null) }
    var combinedUrl by remember { mutableStateOf<String?>(null) }
    var reportSelection by remember { mutableStateOf(FinanceReportSelection()) }
    var showReportSelection by remember { mutableStateOf(false) }
    var confirmCombined by remember { mutableStateOf(false) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var entryKind by remember { mutableStateOf(EntryKind.EXPENSE) }
    var movementFilter by remember { mutableStateOf("Toutes les écritures") }
    var movementSearch by remember { mutableStateOf("") }

    LaunchedEffect(requestedReport) {
        val request = requestedReport ?: return@LaunchedEffect
        reportSelection = if (request.fromDate != null && request.toDate != null) {
            FinanceReportSelection(period = "custom", from = request.fromDate, to = request.toDate)
        } else {
            FinanceReportSelection()
        }
        section = BalanceSection.REPORTS
        showReportSelection = true
        onReportRequestHandled()
    }

    val savePdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val file = pendingPdfFile
        if (uri != null && file != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.use { out -> file.inputStream().buffered().use { it.copyTo(out) } }
                    ?: error("Impossible d'écrire le PDF sélectionné.")
            }.onSuccess { onToast(copyPdfSavedMessage) }
                .onFailure { onToast(savePdfFailedMessage) }
        }
        pendingPdfFile = null
    }

    if (showReportSelection) {
        FinanceReportSelectionDialog(
            initial = reportSelection,
            onDismiss = { showReportSelection = false },
            onGenerate = { selection ->
                reportSelection = selection
                showReportSelection = false
                confirmCombined = true
            },
        )
    }

    if (confirmCombined) {
        AlertDialog(
            onDismissRequest = { confirmCombined = false },
            title = { Text(atlasText("Ouvrir le rapport combiné ?", "Open the combined report?", "هل تريد فتح التقرير الموحّد؟")) },
            text = {
                Text(
                    atlasText(
                        "${localizedFinancePeriodLabel(reportSelection)} · ${reportSelection.selectedSections().joinToString { localizedReportSectionLabel(it, appLanguage) }}. Le lien HMAC est temporaire (30 min) et privé : ne le partagez pas.",
                        "${localizedFinancePeriodLabel(reportSelection)} · ${reportSelection.selectedSections().joinToString { localizedReportSectionLabel(it, appLanguage) }}. The HMAC link is private and expires in 30 minutes. Do not share it.",
                        "${localizedFinancePeriodLabel(reportSelection)} · ${reportSelection.selectedSections().joinToString { localizedReportSectionLabel(it, appLanguage) }}. رابط HMAC خاص ومؤقت لمدة 30 دقيقة. لا تشاركه.",
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmCombined = false
                    viewModel.createCombinedReportUrl(reportSelection.period, reportSelection.from, reportSelection.to, reportSelection.sectionKeys) { result ->
                        when (result) {
                            is ApiResult.Success -> combinedUrl = result.data
                            is ApiResult.Failure -> onToast(result.message)
                        }
                    }
                }) { Text(atlasText("Continuer")) }
            },
            dismissButton = { TextButton(onClick = { confirmCombined = false }) { Text(atlasText("Annuler")) } },
        )
    }

    combinedUrl?.let { url ->
        CombinedReportWebViewDialog(url = url, selectedSections = reportSelection.sectionKeys, onDismiss = { combinedUrl = null })
    }
    pdfViewerFile?.let { file ->
        FinancePdfViewerDialog(
            file = file,
            onDismiss = { pdfViewerFile = null },
            onShare = {
                runCatching {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newUri(context.contentResolver, file.name, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, chooserTitle))
                }.onFailure { onToast(sharePdfFailedMessage) }
            },
            onSave = {
                pendingPdfFile = file
                savePdfLauncher.launch(file.name)
            },
            onDelete = {
                file.delete()
                pdfViewerFile = null
                onToast(reportDeletedMessage)
            },
        )
    }

    if (showEntryDialog && state.isAdmin) {
        NewEntryDialog(
            initialKind = entryKind,
            saving = state.savingEntry,
            onDismiss = { if (!state.savingEntry) showEntryDialog = false },
            onSave = { request ->
                viewModel.createEntry(request) { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            showEntryDialog = false
                            onToast(entrySavedMessage)
                        }
                        is ApiResult.Failure -> onToast(result.message)
                    }
                }
            },
        )
    }

    if (!state.isAdmin) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(atlasText("Les soldes et relevés financiers sont réservés aux rôles owner/admin."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    if (state.loading && state.finance == null) {
        AtlasLoading(message = atlasText("Chargement des soldes et mouvements…", "Loading balances and transactions…", "جارٍ تحميل الأرصدة والحركات…"))
        return
    }

    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    atlasText("Trésorerie · créances · mouvements"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MonthSelector(selectedPeriod = state.selectedPeriod, onSelect = viewModel::selectPeriod)
                IconButton(onClick = viewModel::refresh, enabled = !state.refreshing) {
                    if (state.refreshing) CircularProgressIndicator(Modifier.width(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser les soldes"))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EntryKind.entries.forEach { kind ->
                val open = {
                    entryKind = kind
                    showEntryDialog = true
                }
                if (kind == EntryKind.EXPENSE) {
                    Button(onClick = open) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.width(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(atlasText(kind.quickLabel, kind.quickEnglish, kind.quickArabic))
                    }
                } else {
                    OutlinedButton(onClick = open) {
                        Icon(kind.icon, contentDescription = null, modifier = Modifier.width(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(atlasText(kind.quickLabel, kind.quickEnglish, kind.quickArabic))
                    }
                }
            }
        }
        TabRow(selectedTabIndex = section.ordinal) {
            BalanceSection.entries.forEach { tab ->
                Tab(
                    selected = section == tab,
                    onClick = { section = tab },
                    text = { Text(when (tab) { BalanceSection.MOVEMENTS -> atlasText("Mouvements", "Transactions", "الحركات"); BalanceSection.CLIENTS -> atlasText("Clients", "Customers", "العملاء"); BalanceSection.REPORTS -> atlasText("Rapports", "Reports", "التقارير") }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        if (state.error != null) {
            Text(atlasNotice(state.error.orEmpty(), appLanguage), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val maxContentWidth = if (maxWidth >= 900.dp) 1180.dp else 820.dp
            LazyColumn(
                modifier = Modifier.widthIn(max = maxContentWidth).fillMaxSize().align(Alignment.TopCenter),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = if (maxWidth >= 600.dp) 24.dp else 16.dp,
                    end = if (maxWidth >= 600.dp) 24.dp else 16.dp,
                    top = 12.dp,
                    bottom = 12.dp + dockPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (section) {
                    BalanceSection.MOVEMENTS -> {
                        val finance = state.finance
                        state.periodBreakdown?.let { periodData ->
                            item(key = "cash-bank-summary") {
                                CashBankDashboard(periodData, finance?.currency ?: periodData.currency)
                            }
                        } ?: state.periodBreakdownError?.let { detailError ->
                            item(key = "cash-bank-unavailable") {
                                Text(
                                    atlasText(
                                        "Ventilation caisse/banque indisponible sur ce serveur. Les soldes détaillés ne sont pas estimés. ${detailError.take(100)}",
                                        "Cash/bank breakdown is unavailable on this server. Detailed balances are not estimated. ${detailError.take(100)}",
                                        "تفصيل الصندوق والبنك غير متاح على هذا الخادم. لم يتم تقدير الأرصدة التفصيلية. ${detailError.take(100)}",
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (finance != null) {
                            item(key = "balance-summary") {
                                BalanceSummary(
                                    opening = finance.openingBalance,
                                    closing = finance.closingBalance,
                                    income = finance.salesReceived,
                                    outflows = finance.expenses + finance.payrollPaid + finance.supplierPaid,
                                    currency = finance.currency,
                                    compact = maxWidth < 600.dp,
                                )
                            }
                            item(key = "flow-panels") {
                                val breakdown = state.periodBreakdown
                                val outflows = if (breakdown != null) {
                                    breakdown.expenseCategories.map { it.category to it.total }.ifEmpty { listOf(atlasText("Aucune dépense", "No expenses", "لا توجد مصروفات") to 0.0) }
                                } else {
                                    listOf(atlasText("Dépenses") to finance.expenses, atlasText("Fournisseurs") to finance.supplierPaid, atlasText("Paie") to finance.payrollPaid, atlasText("Remboursements", "Refunds", "المبالغ المستردة") to finance.refunds)
                                }
                                val incomingTotal = breakdown?.inflows?.total ?: state.statement?.totals?.credits ?: 0.0
                                val outgoingTotal = breakdown?.outflows?.total ?: state.statement?.totals?.debits ?: 0.0
                                val categoryTitle = if (breakdown != null) atlasText("Dépenses par catégorie", "Expenses by category", "المصروفات حسب الفئة") else atlasText("Sorties par type", "Expenses by type", "المصروفات حسب النوع")
                                if (maxWidth < 600.dp) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        FinanceFlowCard(categoryTitle, outflows, finance.currency)
                                        FinanceFlowCard(atlasText("Entrées et sorties", "Income and expenses", "الإيرادات والمصروفات"), listOf(atlasText("Entrées") to incomingTotal, atlasText("Sorties") to outgoingTotal), finance.currency)
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        FinanceFlowCard(categoryTitle, outflows, finance.currency, Modifier.weight(1f))
                                        FinanceFlowCard(atlasText("Entrées et sorties", "Income and expenses", "الإيرادات والمصروفات"), listOf(atlasText("Entrées") to incomingTotal, atlasText("Sorties") to outgoingTotal), finance.currency, Modifier.weight(1f))
                                    }
                                }
                            }
                            item(key = "reconcile") {
                                ReconciliationNotice(
                                    balanced = state.periodBreakdown?.statement?.balanced ?: state.statement?.reconciliation?.balanced,
                                    status = state.periodBreakdown?.status ?: state.statement?.status.orEmpty(),
                                    asOf = state.periodBreakdown?.asOf ?: state.statement?.asOf.orEmpty(),
                                )
                            }
                        }
                        item(key = "movement-filters") {
                            if (maxWidth < 480.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    EntryDropdown(atlasText("Filtrer", "Filter", "تصفية"), movementFilter, listOf("Toutes les écritures", "Entrées", "Sorties", "Dépenses")) { movementFilter = it }
                                    OutlinedTextField(
                                        value = movementSearch,
                                        onValueChange = { movementSearch = it },
                                        label = { Text(atlasText("Rechercher")) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f)) {
                                        EntryDropdown(atlasText("Filtrer", "Filter", "تصفية"), movementFilter, listOf("Toutes les écritures", "Entrées", "Sorties", "Dépenses")) { movementFilter = it }
                                    }
                                    OutlinedTextField(
                                        value = movementSearch,
                                        onValueChange = { movementSearch = it },
                                        label = { Text(atlasText("Rechercher")) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.4f),
                                    )
                                }
                            }
                        }
                        item(key = "movement-heading") {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(atlasText("Transactions récentes"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    atlasText(
                                        "Derniers mouvements de trésorerie · ${state.statement?.pageSize ?: 50} par page",
                                        "Recent cash transactions · ${state.statement?.pageSize ?: 50} per page",
                                        "حركات الخزينة الأخيرة · ${state.statement?.pageSize ?: 50} لكل صفحة",
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        val filteredRows = state.rows.filter { row ->
                            val matchesFilter = when (movementFilter) {
                                "Entrées" -> row.credit > 0.0
                                "Sorties" -> row.debit > 0.0
                                "Dépenses" -> row.type.equals("expense", ignoreCase = true)
                                else -> true
                            }
                            val term = movementSearch.trim()
                            matchesFilter && (term.isBlank() || listOf(row.label, row.ref, row.type, row.ts).any { it.contains(term, ignoreCase = true) })
                        }
                        if (filteredRows.isEmpty() && !state.loading) {
                            item(key = "empty-movements") { EmptyPanel(if (state.rows.isEmpty()) atlasText("Aucun mouvement pour cette période.", "No transactions for this period.", "لا توجد حركات لهذه الفترة.") else atlasText("Aucun mouvement ne correspond à ce filtre.", "No transactions match this filter.", "لا توجد حركات تطابق عامل التصفية هذا.")) }
                        } else {
                            items(filteredRows, key = { "${it.source}-${it.id}" }) { row ->
                                StatementRowCard(row = row, currency = state.statement?.currency ?: "MAD")
                            }
                        }
                        if (state.statement?.hasMore == true) {
                            item(key = "load-more") {
                                OutlinedButton(onClick = viewModel::loadMore, enabled = !state.loadingMore, modifier = Modifier.fillMaxWidth()) {
                                    if (state.loadingMore) CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp)
                                    else Text(atlasText("Charger les mouvements suivants"))
                                }
                            }
                        }
                    }
                    BalanceSection.CLIENTS -> {
                        val bucket = state.finance?.receivables
                        item(key = "clients-total") {
                            SummaryAmountCard(
                                title = atlasText("À recevoir", "Receivables", "مستحقات القبض"),
                                amount = bucket?.totalOutstanding ?: 0.0,
                                currency = state.finance?.currency ?: "MAD",
                                subtitle = atlasText("${bucket?.count ?: 0} document(s) · ${formatMoney(bucket?.totalOverdue ?: 0.0, state.finance?.currency ?: "MAD")} en retard", "${bucket?.count ?: 0} document(s) · ${formatMoney(bucket?.totalOverdue ?: 0.0, state.finance?.currency ?: "MAD")} overdue", "${bucket?.count ?: 0} مستند · ${formatMoney(bucket?.totalOverdue ?: 0.0, state.finance?.currency ?: "MAD")} متأخر"),
                            )
                        }
                        val receivableDocuments = bucket?.documents.orEmpty()
                        if (receivableDocuments.isEmpty()) item(key = "no-receivables") { EmptyPanel(atlasText("Aucune créance client ouverte pour cette période.", "No open customer receivables for this period.", "لا توجد مستحقات مفتوحة على العملاء لهذه الفترة.")) }
                        else items(receivableDocuments, key = { "receivable-${it.id}" }) { document ->
                            ReceivableCard(document, state.finance?.currency ?: "MAD")
                        }
                        if (bucket?.truncated == true) item(key = "truncated") { Text(atlasText("Liste partielle : le serveur limite les documents affichés."), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }

                        val payables = state.finance?.payables
                        item(key = "suppliers-title") { Text(atlasText("Dettes fournisseurs"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                        item(key = "suppliers-total") {
                            SummaryAmountCard(
                                title = atlasText("À payer", "Payables", "مستحقات الدفع"),
                                amount = payables?.totalOutstanding ?: 0.0,
                                currency = state.finance?.currency ?: "MAD",
                                subtitle = atlasText("${payables?.count ?: 0} document(s) · ${formatMoney(payables?.totalOverdue ?: 0.0, state.finance?.currency ?: "MAD")} en retard", "${payables?.count ?: 0} document(s) · ${formatMoney(payables?.totalOverdue ?: 0.0, state.finance?.currency ?: "MAD")} overdue", "${payables?.count ?: 0} مستند · ${formatMoney(payables?.totalOverdue ?: 0.0, state.finance?.currency ?: "MAD")} متأخر"),
                            )
                        }
                        val payableDocuments = payables?.documents.orEmpty()
                        if (payableDocuments.isEmpty()) item(key = "no-payables") { EmptyPanel(atlasText("Aucune dette fournisseur ouverte pour cette période.", "No open supplier payables for this period.", "لا توجد مستحقات مفتوحة للموردين لهذه الفترة.")) }
                        else items(payableDocuments, key = { "payable-${it.id}" }) { document ->
                            ReceivableCard(document, state.finance?.currency ?: "MAD")
                        }
                        item(key = "payroll-due") {
                            SummaryAmountCard(
                                title = atlasText("Paie encore due", "Payroll still due", "رواتب لا تزال مستحقة"),
                                amount = state.finance?.payroll?.totalDue ?: 0.0,
                                currency = state.finance?.currency ?: "MAD",
                                subtitle = atlasText("${state.finance?.payroll?.employees ?: 0} employé(s) concernés", "${state.finance?.payroll?.employees ?: 0} employees", "${state.finance?.payroll?.employees ?: 0} موظف"),
                                icon = Icons.Outlined.AccountBalanceWallet,
                            )
                        }
                    }
                    BalanceSection.REPORTS -> {
                        item(key = "report-heading") {
                            Text(atlasText("Rapports financiers"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        item(key = "month-pdf") {
                            ReportActionCard(
                                title = atlasText("Relevé mensuel PDF", "Monthly statement PDF", "كشف شهري PDF"),
                                description = atlasText("Rapport officiel du serveur · ${state.selectedPeriod ?: "—"} · téléchargé en cache privé et conservé temporairement.", "Official server report · ${state.selectedPeriod ?: "—"} · downloaded to private cache and kept temporarily.", "تقرير رسمي من الخادم · ${state.selectedPeriod ?: "—"} · يُنزّل إلى ذاكرة خاصة ويُحفظ مؤقتاً."),
                                icon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                                actionLabel = if (state.reportGenerating) atlasText("Préparation…", "Preparing…", "جارٍ التحضير…") else atlasText("Télécharger le PDF", "Download PDF", "تنزيل PDF"),
                                enabled = !state.reportGenerating && state.selectedPeriod != null,
                            ) {
                                state.selectedPeriod?.let { period ->
                                    viewModel.downloadReport(context.cacheDir, period) { result ->
                                        when (result) {
                                            is ApiResult.Success -> {
                                                pdfViewerFile = result.data
                                                onToast(reportDownloadedMessage)
                                            }
                                            is ApiResult.Failure -> onToast(result.message)
                                        }
                                    }
                                }
                            }
                        }
                        item(key = "combined-report") {
                            ReportActionCard(
                                title = atlasText("Pack de rapports personnalisable", "Customizable report bundle", "حزمة تقارير قابلة للتخصيص"),
                                description = atlasText("Choisissez les rubriques et la période : mois, année en cours, derniers mois ou dates exactes.", "Choose sections and a period: month, current year, recent months, or exact dates.", "اختر الأقسام والفترة: شهر أو السنة الحالية أو الأشهر الأخيرة أو تواريخ محددة."),
                                icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null) },
                                actionLabel = if (state.reportGenerating) atlasText("Préparation…", "Preparing…", "جارٍ التحضير…") else atlasText("Ouvrir dans Atlas", "Open in Atlas", "فتح في Atlas"),
                                enabled = !state.reportGenerating,
                            ) { showReportSelection = true }
                        }
                        item(key = "report-note") {
                            Text(atlasText("Les chiffres proviennent des API financières du serveur sélectionné. Aucun solde n'est calculé ou codé en dur dans l'application.", "Figures come from the selected server’s financial APIs. No balance is calculated or hardcoded in the app.", "تأتي الأرقام من واجهات الخادم المالي المحدد. لا يحسب التطبيق أي رصيد ولا يضعه بشكل ثابت."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun localizedReportSectionLabel(section: ma.etherealmber.atlascommerce.ui.components.FinanceReportSection, language: AppLanguage): String = when (section) {
    ma.etherealmber.atlascommerce.ui.components.FinanceReportSection.BALANCE -> when (language) { AppLanguage.FRENCH -> "Balance"; AppLanguage.ENGLISH -> "Balance"; AppLanguage.ARABIC -> "الرصيد" }
    ma.etherealmber.atlascommerce.ui.components.FinanceReportSection.STOCK -> when (language) { AppLanguage.FRENCH -> "Stock"; AppLanguage.ENGLISH -> "Inventory"; AppLanguage.ARABIC -> "المخزون" }
    ma.etherealmber.atlascommerce.ui.components.FinanceReportSection.RECEIVABLES -> when (language) { AppLanguage.FRENCH -> "Créances clients"; AppLanguage.ENGLISH -> "Customer receivables"; AppLanguage.ARABIC -> "مستحقات العملاء" }
    ma.etherealmber.atlascommerce.ui.components.FinanceReportSection.PAYABLES -> when (language) { AppLanguage.FRENCH -> "Fournisseurs"; AppLanguage.ENGLISH -> "Suppliers"; AppLanguage.ARABIC -> "الموردون" }
    ma.etherealmber.atlascommerce.ui.components.FinanceReportSection.EMPLOYEES -> when (language) { AppLanguage.FRENCH -> "Employés & paie"; AppLanguage.ENGLISH -> "Employees & payroll"; AppLanguage.ARABIC -> "الموظفون والرواتب" }
    ma.etherealmber.atlascommerce.ui.components.FinanceReportSection.AGING -> when (language) { AppLanguage.FRENCH -> "Balance âgée"; AppLanguage.ENGLISH -> "Aging report"; AppLanguage.ARABIC -> "تقرير أعمار الديون" }
    ma.etherealmber.atlascommerce.ui.components.FinanceReportSection.TVA -> when (language) { AppLanguage.FRENCH -> "TVA"; AppLanguage.ENGLISH -> "VAT"; AppLanguage.ARABIC -> "ضريبة القيمة المضافة" }
}

@Composable
private fun localizedFinancePeriodLabel(selection: FinanceReportSelection): String = when (selection.period) {
    "month" -> atlasText("Mois en cours", "Current month", "الشهر الحالي")
    "3m" -> atlasText("3 derniers mois", "Last 3 months", "آخر 3 أشهر")
    "6m" -> atlasText("6 derniers mois", "Last 6 months", "آخر 6 أشهر")
    "12m" -> atlasText("12 derniers mois", "Last 12 months", "آخر 12 شهراً")
    "year" -> atlasText("Année en cours", "Current year", "السنة الحالية")
    "custom" -> if (selection.from != null && selection.to != null) "${atlasShortDate(selection.from)} → ${atlasShortDate(selection.to)}" else atlasText("Période personnalisée", "Custom period", "فترة مخصصة")
    else -> selection.period
}

@Composable
private fun MonthSelector(selectedPeriod: String?, onSelect: (String) -> Unit) {
    val locale = atlasLocale()
    val now = remember { YearMonth.now() }
    val periods = remember(now) { (0..23).map { now.minusMonths(it.toLong()) } }
    var expanded by remember { mutableStateOf(false) }
    val current = selectedPeriod?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
    Box {
        OutlinedButton(onClick = { expanded = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
            Text(current?.let { monthLabel(it, locale) } ?: atlasText("Mois"), style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Outlined.ExpandMore, contentDescription = atlasText("Choisir un mois"))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            periods.forEach { month ->
                DropdownMenuItem(
                    text = { Text(monthLabel(month, locale)) },
                    onClick = { expanded = false; onSelect(month.toString()) },
                )
            }
        }
    }
}

private fun monthLabel(month: YearMonth, locale: Locale): String {
    val name = month.month.getDisplayName(TextStyle.FULL, locale)
    return "${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }} ${month.year}"
}

@Composable
private fun BalanceClosingCard(closing: Double, currency: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(8.dp))
                Text(atlasText("SOLDE DE CLÔTURE", "CLOSING BALANCE", "الرصيد الختامي"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(formatMoney(closing, currency), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(atlasText("Après les mouvements comptabilisés"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f))
        }
    }
}

@Composable
private fun BalanceSummary(opening: Double, closing: Double, income: Double, outflows: Double, currency: String, compact: Boolean) {
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BalanceClosingCard(closing, currency, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryAmountCard(atlasText("Ouverture", "Opening", "الافتتاحي"), opening, currency, icon = Icons.Outlined.AccountBalanceWallet, modifier = Modifier.weight(1f))
                SummaryAmountCard(atlasText("Entrées", "Income", "الإيرادات"), income, currency, icon = Icons.Outlined.ArrowDownward, modifier = Modifier.weight(1f))
            }
            SummaryAmountCard(atlasText("Sorties payées", "Paid expenses", "المصروفات المدفوعة"), outflows, currency, icon = Icons.Outlined.ArrowUpward, modifier = Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BalanceClosingCard(closing, currency, Modifier.weight(1.25f))
            SummaryAmountCard(atlasText("Ouverture", "Opening", "الافتتاحي"), opening, currency, icon = Icons.Outlined.AccountBalanceWallet, modifier = Modifier.weight(1f))
            SummaryAmountCard(atlasText("Entrées", "Income", "الإيرادات"), income, currency, icon = Icons.Outlined.ArrowDownward, modifier = Modifier.weight(1f))
            SummaryAmountCard(atlasText("Sorties payées", "Paid expenses", "المصروفات المدفوعة"), outflows, currency, icon = Icons.Outlined.ArrowUpward, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryAmountCard(title: String, amount: Double, currency: String, subtitle: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                if (icon != null) Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatMoney(amount, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CashBankDashboard(data: FinancePeriodDto, currency: String) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 600.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CashAccountCard(atlasText("Caisse", "Cash", "النقد"), data.inflows.cash, data.outflows.cash, currency, Modifier.weight(1f))
                    CashAccountCard(atlasText("Banque", "Bank", "البنك"), data.inflows.bank, data.outflows.bank, currency, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OwnerMovementCard(atlasText("Apports", "Contributions", "المساهمات"), data.owner.contributions.total, data.owner.contributions.count, positive = true, currency = currency, modifier = Modifier.weight(1f))
                    OwnerMovementCard(atlasText("Prélèvements", "Withdrawals", "السحوبات"), data.owner.withdrawals.total, data.owner.withdrawals.count, positive = false, currency = currency, modifier = Modifier.weight(1f))
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CashAccountCard(atlasText("Caisse", "Cash", "النقد"), data.inflows.cash, data.outflows.cash, currency, Modifier.weight(1f))
                CashAccountCard(atlasText("Banque", "Bank", "البنك"), data.inflows.bank, data.outflows.bank, currency, Modifier.weight(1f))
                OwnerMovementCard(atlasText("Apports", "Contributions", "المساهمات"), data.owner.contributions.total, data.owner.contributions.count, positive = true, currency = currency, modifier = Modifier.weight(1f))
                OwnerMovementCard(atlasText("Prélèvements", "Withdrawals", "السحوبات"), data.owner.withdrawals.total, data.owner.withdrawals.count, positive = false, currency = currency, modifier = Modifier.weight(1f))
            }
        }
    }
    if (data.outflows.notRecorded > 0.0) {
        Text(
            atlasText(
                "Mode de paiement non enregistré (paie) : ${formatMoney(data.outflows.notRecorded, currency)} — non attribué à la caisse ni à la banque.",
                "Unrecorded payment method (payroll): ${formatMoney(data.outflows.notRecorded, currency)} — not assigned to cash or bank.",
                "طريقة الدفع غير مسجّلة (الرواتب): ${formatMoney(data.outflows.notRecorded, currency)} — غير منسوبة إلى الصندوق أو البنك.",
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (!data.statement.consistentWithBreakdown) {
        Text(
            atlasText("La ventilation diffère du relevé; vérifiez la cohérence des données serveur."),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun CashAccountCard(title: String, incoming: Double, outgoing: Double, currency: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.uppercase(atlasLocale()), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                atlasText("Entrées  +${formatMoney(incoming, currency)}", "Incoming  +${formatMoney(incoming, currency)}", "الوارد  +${formatMoney(incoming, currency)}"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(atlasText("Sorties  −${formatMoney(outgoing, currency)}", "Outgoing  −${formatMoney(outgoing, currency)}", "الصادر  −${formatMoney(outgoing, currency)}"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            val net = incoming - outgoing
            Text(
                atlasText(
                    "Net  ${if (net >= 0.0) "+" else "−"}${formatMoney(kotlin.math.abs(net), currency)}",
                    "Net  ${if (net >= 0.0) "+" else "−"}${formatMoney(kotlin.math.abs(net), currency)}",
                    "الصافي  ${if (net >= 0.0) "+" else "−"}${formatMoney(kotlin.math.abs(net), currency)}",
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (net >= 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun OwnerMovementCard(title: String, amount: Double, count: Int, positive: Boolean, currency: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title.uppercase(atlasLocale()), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${if (positive) "+" else "−"}${formatMoney(amount, currency)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text(
                atlasText(
                    "$count mouvement(s) · hors solde caisse/banque",
                    "$count transactions · excluded from cash/bank balances",
                    "$count حركة · غير مشمولة في أرصدة الصندوق والبنك",
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FinanceFlowCard(
    title: String,
    rows: List<Pair<String, Double>>,
    currency: String,
    modifier: Modifier = Modifier,
) {
    val largest = rows.maxOfOrNull { it.second.coerceAtLeast(0.0) } ?: 0.0
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            rows.forEach { (label, amount) ->
                val target = if (largest > 0.0) (amount / largest).toFloat().coerceIn(0f, 1f) else 0f
                val fraction by animateFloatAsState(targetValue = target, label = "flow-$label")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(atlasText(label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatMoney(amount, currency), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    }
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReconciliationNotice(balanced: Boolean?, status: String, asOf: String) {
    val color = if (balanced == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (balanced == true) atlasText("Relevé réconcilié") else atlasText("Réconciliation indisponible", "Reconciliation unavailable", "المطابقة غير متاحة"),
                    style = MaterialTheme.typography.titleSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (status == "open") {
                        atlasText("Mois en cours · arrêté au ${atlasShortDate(asOf)}", "Current month · as of ${atlasShortDate(asOf)}", "الشهر الحالي · حتى ${atlasShortDate(asOf)}")
                    } else {
                        atlasText("Période clôturée · arrêté au ${atlasShortDate(asOf)}", "Closed period · as of ${atlasShortDate(asOf)}", "فترة مغلقة · حتى ${atlasShortDate(asOf)}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = color)
        }
    }
}

@Composable
private fun StatementRowCard(row: FinanceStatementRowDto, currency: String) {
    val kind = when (row.type.lowercase()) {
        "sale" -> atlasText("Encaissement", "Payment received", "تحصيل")
        "refund" -> atlasText("Remboursement", "Refund", "استرداد")
        "expense" -> atlasText("Dépense", "Expense", "مصروف")
        "payroll" -> atlasText("Paie", "Payroll", "الرواتب")
        "supplier" -> atlasText("Fournisseur", "Supplier", "المورد")
        else -> atlasText("Mouvement", "Transaction", "حركة")
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (row.label.isNotBlank()) atlasText(row.label) else kind, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("$kind · ${row.ref} · ${atlasShortDate(row.ts)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                val amount = if (row.credit > 0.0) "+${formatMoney(row.credit, currency)}" else "−${formatMoney(row.debit, currency)}"
                Text(amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (row.credit > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Text(
                    atlasText("Solde ${formatMoney(row.runningBalance, currency)}", "Balance ${formatMoney(row.runningBalance, currency)}", "الرصيد ${formatMoney(row.runningBalance, currency)}"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReceivableCard(document: FinanceOutstandingDocumentDto, currency: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(document.party.ifBlank { atlasText("Client") }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(when (document.state.lowercase()) {
                    "overdue" -> atlasText("En retard", "Overdue", "متأخر")
                    "open", "pending" -> atlasText("En attente", "Pending", "قيد الانتظار")
                    "paid", "closed", "completed" -> atlasText("Payé", "Paid", "مدفوع")
                    else -> atlasText(document.state)
                }, style = MaterialTheme.typography.labelMedium, color = if (document.state == "overdue") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            Text(
                atlasText(
                    "${document.number} · échéance ${document.dueDate?.let { atlasShortDate(it) } ?: "non définie"}",
                    "${document.number} · due ${document.dueDate?.let { atlasShortDate(it) } ?: "not set"}",
                    "${document.number} · الاستحقاق ${document.dueDate?.let { atlasShortDate(it) } ?: "غير محدد"}",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(Modifier.padding(vertical = 3.dp))
            Text(atlasText("Restant dû  ${formatMoney(document.remaining, currency)}", "Remaining due  ${formatMoney(document.remaining, currency)}", "المتبقي المستحق  ${formatMoney(document.remaining, currency)}"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(atlasText("Payé ${formatMoney(document.paid, currency)} / Total ${formatMoney(document.total, currency)}", "Paid ${formatMoney(document.paid, currency)} / Total ${formatMoney(document.total, currency)}", "المدفوع ${formatMoney(document.paid, currency)} / الإجمالي ${formatMoney(document.total, currency)}"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReportActionCard(title: String, description: String, icon: @Composable () -> Unit, actionLabel: String, enabled: Boolean, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                if (actionLabel.endsWith("…")) CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp)
                else Text(actionLabel)
            }
        }
    }
}

@Composable
private fun EmptyPanel(message: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private enum class EntryKind(
    val apiType: String,
    val label: String,
    val quickLabel: String,
    val quickEnglish: String,
    val quickArabic: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val categories: List<String>,
) {
    EXPENSE("expense", "Dépense", "+ Dépense", "+ Expense", "+ مصروف", Icons.Outlined.ArrowUpward, listOf("Loyer", "Transport", "Fournitures", "Salaires", "Taxes", "Autre dépense")),
    INCOME("income", "Recette", "+ Recette", "+ Income", "+ إيراد", Icons.Outlined.ArrowDownward, listOf("Vente divers", "Prestation de service", "Remboursement", "Subvention", "Intérêts", "Autre recette")),
    OWNER_CONTRIB("owner_contrib", "Apport propriétaire", "+ Apport", "+ Contribution", "+ مساهمة", Icons.Outlined.AccountBalanceWallet, listOf("Apport en capital", "Apport en compte courant", "Autre apport")),
    OWNER_DRAW("owner_draw", "Prélèvement propriétaire", "+ Prélèvement", "+ Withdrawal", "+ سحب", Icons.Outlined.ArrowUpward, listOf("Prélèvement personnel", "Dividende", "Remboursement associé", "Autre prélèvement")),
}

@Composable
private fun NewEntryDialog(
    initialKind: EntryKind,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (CreateFinanceEntryRequest) -> Unit,
) {
    var kind by remember(initialKind) { mutableStateOf(initialKind) }
    var category by remember(initialKind) { mutableStateOf(initialKind.categories.first()) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var payment by remember { mutableStateOf("cash") }
    var vendor by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(0) }

    val parsedAmount = amount.replace(',', '.').toDoubleOrNull()
    val validDate = runCatching { java.time.LocalDate.parse(date).toString() == date }.getOrDefault(false)
    val canSave = !saving && description.isNotBlank() && parsedAmount != null && parsedAmount > 0.0 && validDate
    val localizedKindLabel = when (kind) {
        EntryKind.EXPENSE -> atlasText("Dépense", "Expense", "مصروف")
        EntryKind.INCOME -> atlasText("Recette", "Income", "إيراد")
        EntryKind.OWNER_CONTRIB -> atlasText("Apport propriétaire", "Owner contribution", "مساهمة المالك")
        EntryKind.OWNER_DRAW -> atlasText("Prélèvement propriétaire", "Owner withdrawal", "سحب المالك")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                atlasText("Nouvelle écriture — ${kind.label}", "New entry — $localizedKindLabel", "قيد جديد — $localizedKindLabel"),
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    if (step == 0) {
                        atlasText("Étape 1/2 · Type et libellé", "Step 1/2 · Type and description", "الخطوة 1/2 · النوع والوصف")
                    } else {
                        atlasText("Étape 2/2 · Montant et règlement", "Step 2/2 · Amount and payment", "الخطوة 2/2 · المبلغ والدفع")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (step == 0) {
                    EntryDropdown(atlasText("Type", "Type", "النوع"), atlasText(kind.label, when (kind) { EntryKind.EXPENSE -> "Expense"; EntryKind.INCOME -> "Income"; EntryKind.OWNER_CONTRIB -> "Owner contribution"; EntryKind.OWNER_DRAW -> "Owner withdrawal" }, when (kind) { EntryKind.EXPENSE -> "مصروف"; EntryKind.INCOME -> "إيراد"; EntryKind.OWNER_CONTRIB -> "مساهمة المالك"; EntryKind.OWNER_DRAW -> "سحب المالك" }), EntryKind.entries.map { it.label }) { label ->
                        EntryKind.entries.firstOrNull { it.label == label }?.let { selected ->
                            kind = selected
                            category = selected.categories.first()
                        }
                    }
                    EntryDropdown(atlasText("Catégorie"), category, kind.categories) { category = it }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(atlasText("Libellé *")) },
                        placeholder = { Text(atlasText("Ex. Loyer entrepôt octobre")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (description.isBlank()) Text(atlasText("La description est obligatoire."), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                } else {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                        label = { Text(atlasText("Montant *")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it.take(10) },
                        label = { Text(atlasText("Date (AAAA-MM-JJ)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = !validDate,
                    )
                    EntryDropdown(
                        atlasText("Mode de paiement", "Payment method", "طريقة الدفع"),
                        when (payment) { "bank" -> "Banque / virement"; "card" -> "Carte"; "cheque" -> "Chèque"; else -> "Espèces (caisse)" },
                        listOf("Espèces (caisse)", "Banque / virement", "Carte", "Chèque"),
                    ) { label -> payment = when (label) { "Banque / virement" -> "bank"; "Carte" -> "card"; "Chèque" -> "cheque"; else -> "cash" } }
                    OutlinedTextField(
                        value = vendor,
                        onValueChange = { vendor = it },
                        label = { Text(atlasText("Bénéficiaire / fournisseur (facultatif)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(atlasText("Notes (facultatif)")) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    if (parsedAmount == null || parsedAmount <= 0.0) Text(atlasText("Saisissez un montant supérieur à zéro."), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    if (!validDate) Text(atlasText("La date doit être une date valide au format AAAA-MM-JJ."), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            if (step == 0) {
                Button(onClick = { if (description.isNotBlank()) step = 1 }, enabled = description.isNotBlank() && !saving) { Text(atlasText("Continuer")) }
            } else {
                Button(
                    onClick = {
                        val value = parsedAmount ?: return@Button
                        onSave(
                            CreateFinanceEntryRequest(
                                category = category,
                                description = description.trim(),
                                amount = value,
                                paymentMethod = payment,
                                expenseDate = date,
                                vendorName = vendor.trim(),
                                notes = notes.trim(),
                                entryType = kind.apiType,
                            ),
                        )
                    },
                    enabled = canSave,
                ) {
                    if (saving) CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp)
                    else Text(atlasText("Enregistrer"))
                }
            }
        },
        dismissButton = {
            Row {
                if (step == 1) TextButton(onClick = { step = 0 }, enabled = !saving) { Text(atlasText("Retour")) }
                TextButton(onClick = onDismiss, enabled = !saving) { Text(atlasText("Annuler")) }
            }
        },
    )
}

@Composable
private fun EntryDropdown(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(atlasText(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(atlasText(selected), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = atlasText("Choisir $label", "Choose ${atlasText(label)}", "اختر ${atlasText(label)}"),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option -> DropdownMenuItem(text = { Text(atlasText(option)) }, onClick = { expanded = false; onSelect(option) }) }
            }
        }
    }
}

@Composable
private fun formatMoney(value: Double, currency: String): String = atlasMoney(value, currency)
