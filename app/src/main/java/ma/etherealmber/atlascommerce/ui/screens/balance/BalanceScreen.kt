package ma.etherealmber.atlascommerce.ui.screens.balance

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
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.data.model.CreateFinanceEntryRequest
import ma.etherealmber.atlascommerce.data.model.FinanceOutstandingDocumentDto
import ma.etherealmber.atlascommerce.data.model.FinanceStatementRowDto
import ma.etherealmber.atlascommerce.ui.components.AtlasLoading
import ma.etherealmber.atlascommerce.ui.components.CombinedReportWebViewDialog
import ma.etherealmber.atlascommerce.ui.components.FinancePdfViewerDialog
import java.io.File
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private enum class BalanceSection(val label: String) {
    MOVEMENTS("Mouvements"), CLIENTS("Clients"), REPORTS("Rapports")
}

@Composable
fun BalanceScreen(
    onToast: (String) -> Unit,
    viewModel: BalanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var section by remember { mutableStateOf(BalanceSection.MOVEMENTS) }
    var pdfViewerFile by remember { mutableStateOf<File?>(null) }
    var pendingPdfFile by remember { mutableStateOf<File?>(null) }
    var combinedUrl by remember { mutableStateOf<String?>(null) }
    var confirmCombined by remember { mutableStateOf(false) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var entryKind by remember { mutableStateOf(EntryKind.EXPENSE) }
    var movementFilter by remember { mutableStateOf("Toutes les écritures") }
    var movementSearch by remember { mutableStateOf("") }

    val savePdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val file = pendingPdfFile
        if (uri != null && file != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.use { out -> file.inputStream().buffered().use { it.copyTo(out) } }
                    ?: error("Impossible d'écrire le PDF sélectionné.")
            }.onSuccess { onToast("Copie PDF enregistrée.") }
                .onFailure { onToast("Enregistrement impossible : ${it.message.orEmpty()}") }
        }
        pendingPdfFile = null
    }

    if (confirmCombined) {
        AlertDialog(
            onDismissRequest = { confirmCombined = false },
            title = { Text("Ouvrir le rapport combiné ?") },
            text = { Text("Le rapport sera consulté dans Atlas via un ticket HMAC temporaire. Ne partagez pas le lien ni l'écran pendant sa validité.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCombined = false
                    state.selectedPeriod?.let { period ->
                        viewModel.createCombinedReportUrl(period) { result ->
                            when (result) {
                                is ApiResult.Success -> combinedUrl = result.data
                                is ApiResult.Failure -> onToast(result.message)
                            }
                        }
                    } ?: onToast("Sélectionnez un mois avant d'ouvrir le rapport.")
                }) { Text("Continuer") }
            },
            dismissButton = { TextButton(onClick = { confirmCombined = false }) { Text("Annuler") } },
        )
    }

    combinedUrl?.let { url ->
        CombinedReportWebViewDialog(url = url, onDismiss = { combinedUrl = null })
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
                    context.startActivity(Intent.createChooser(intent, "Partager le rapport PDF"))
                }.onFailure { onToast("Impossible de partager le PDF : ${it.message.orEmpty()}") }
            },
            onSave = {
                pendingPdfFile = file
                savePdfLauncher.launch(file.name)
            },
            onDelete = {
                file.delete()
                pdfViewerFile = null
                onToast("Rapport temporaire supprimé.")
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
                            onToast("Écriture enregistrée dans la balance.")
                        }
                        is ApiResult.Failure -> onToast(result.message)
                    }
                }
            },
        )
    }

    if (!state.isAdmin) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Les soldes et relevés financiers sont réservés aux rôles owner/admin.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    if (state.loading && state.finance == null) {
        AtlasLoading(message = "Chargement des soldes et mouvements…")
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Balance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Soldes, créances et mouvements réconciliés", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MonthSelector(
                selectedPeriod = state.selectedPeriod,
                onSelect = viewModel::selectPeriod,
            )
            IconButton(onClick = viewModel::refresh, enabled = !state.refreshing) {
                if (state.refreshing) CircularProgressIndicator(Modifier.width(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser les soldes")
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
                        Text(kind.quickLabel)
                    }
                } else {
                    OutlinedButton(onClick = open) {
                        Icon(kind.icon, contentDescription = null, modifier = Modifier.width(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(kind.quickLabel)
                    }
                }
            }
        }
        TabRow(selectedTabIndex = section.ordinal) {
            BalanceSection.entries.forEach { tab ->
                Tab(
                    selected = section == tab,
                    onClick = { section = tab },
                    text = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        if (state.error != null) {
            Text(state.error.orEmpty(), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxContentWidth = if (maxWidth >= 900.dp) 1180.dp else 820.dp
            LazyColumn(
                modifier = Modifier.widthIn(max = maxContentWidth).fillMaxSize().align(Alignment.TopCenter),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = if (maxWidth >= 600.dp) 24.dp else 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (section) {
                    BalanceSection.MOVEMENTS -> {
                        val finance = state.finance
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
                                val outflows = listOf(
                                    "Dépenses" to finance.expenses,
                                    "Fournisseurs" to finance.supplierPaid,
                                    "Paie" to finance.payrollPaid,
                                    "Remboursements" to finance.refunds,
                                )
                                val totals = state.statement?.totals
                                if (maxWidth < 600.dp) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        FinanceFlowCard("Sorties par type", outflows, finance.currency)
                                        FinanceFlowCard("Entrées et sorties", listOf("Entrées" to (totals?.credits ?: 0.0), "Sorties" to (totals?.debits ?: 0.0)), finance.currency)
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        FinanceFlowCard("Sorties par type", outflows, finance.currency, Modifier.weight(1f))
                                        FinanceFlowCard("Entrées et sorties", listOf("Entrées" to (totals?.credits ?: 0.0), "Sorties" to (totals?.debits ?: 0.0)), finance.currency, Modifier.weight(1f))
                                    }
                                }
                            }
                            item(key = "reconcile") {
                                ReconciliationNotice(
                                    balanced = state.statement?.reconciliation?.balanced,
                                    status = state.statement?.status.orEmpty(),
                                    asOf = state.statement?.asOf.orEmpty(),
                                )
                            }
                        }
                        item(key = "movement-filters") {
                            if (maxWidth < 480.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    EntryDropdown("Filtrer", movementFilter, listOf("Toutes les écritures", "Entrées", "Sorties", "Dépenses")) { movementFilter = it }
                                    OutlinedTextField(
                                        value = movementSearch,
                                        onValueChange = { movementSearch = it },
                                        label = { Text("Rechercher") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f)) {
                                        EntryDropdown("Filtrer", movementFilter, listOf("Toutes les écritures", "Entrées", "Sorties", "Dépenses")) { movementFilter = it }
                                    }
                                    OutlinedTextField(
                                        value = movementSearch,
                                        onValueChange = { movementSearch = it },
                                        label = { Text("Rechercher") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.4f),
                                    )
                                }
                            }
                        }
                        item(key = "movement-heading") {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Transactions récentes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("Derniers mouvements de trésorerie · ${state.statement?.pageSize ?: 50} par page", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            item(key = "empty-movements") { EmptyPanel(if (state.rows.isEmpty()) "Aucun mouvement pour cette période." else "Aucun mouvement ne correspond à ce filtre.") }
                        } else {
                            items(filteredRows, key = { "${it.source}-${it.id}" }) { row ->
                                StatementRowCard(row = row, currency = state.statement?.currency ?: "MAD")
                            }
                        }
                        if (state.statement?.hasMore == true) {
                            item(key = "load-more") {
                                OutlinedButton(onClick = viewModel::loadMore, enabled = !state.loadingMore, modifier = Modifier.fillMaxWidth()) {
                                    if (state.loadingMore) CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp)
                                    else Text("Charger les mouvements suivants")
                                }
                            }
                        }
                    }
                    BalanceSection.CLIENTS -> {
                        val bucket = state.finance?.receivables
                        item(key = "clients-title") {
                            Text("Créances clients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        item(key = "clients-total") {
                            SummaryAmountCard(
                                title = "À recevoir",
                                amount = bucket?.totalOutstanding ?: 0.0,
                                currency = state.finance?.currency ?: "MAD",
                                subtitle = "${bucket?.count ?: 0} document(s) · ${formatMoney(bucket?.totalOverdue ?: 0.0, state.finance?.currency ?: "MAD")} en retard",
                            )
                        }
                        val receivableDocuments = bucket?.documents.orEmpty()
                        if (receivableDocuments.isEmpty()) item(key = "no-receivables") { EmptyPanel("Aucune créance client ouverte pour cette période.") }
                        else items(receivableDocuments, key = { "receivable-${it.id}" }) { document ->
                            ReceivableCard(document, state.finance?.currency ?: "MAD")
                        }
                        if (bucket?.truncated == true) item(key = "truncated") { Text("Liste partielle : le serveur limite les documents affichés.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }

                        val payables = state.finance?.payables
                        item(key = "suppliers-title") { Text("Dettes fournisseurs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                        item(key = "suppliers-total") {
                            SummaryAmountCard(
                                title = "À payer",
                                amount = payables?.totalOutstanding ?: 0.0,
                                currency = state.finance?.currency ?: "MAD",
                                subtitle = "${payables?.count ?: 0} document(s) · ${formatMoney(payables?.totalOverdue ?: 0.0, state.finance?.currency ?: "MAD")} en retard",
                            )
                        }
                        val payableDocuments = payables?.documents.orEmpty()
                        if (payableDocuments.isEmpty()) item(key = "no-payables") { EmptyPanel("Aucune dette fournisseur ouverte pour cette période.") }
                        else items(payableDocuments, key = { "payable-${it.id}" }) { document ->
                            ReceivableCard(document, state.finance?.currency ?: "MAD")
                        }
                        item(key = "payroll-due") {
                            SummaryAmountCard(
                                title = "Paie encore due",
                                amount = state.finance?.payroll?.totalDue ?: 0.0,
                                currency = state.finance?.currency ?: "MAD",
                                subtitle = "${state.finance?.payroll?.employees ?: 0} employé(s) concernés",
                                icon = Icons.Outlined.AccountBalanceWallet,
                            )
                        }
                    }
                    BalanceSection.REPORTS -> {
                        item(key = "report-heading") {
                            Text("Rapports financiers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        item(key = "month-pdf") {
                            ReportActionCard(
                                title = "Relevé mensuel PDF",
                                description = "Rapport officiel du serveur · ${state.selectedPeriod ?: "—"} · téléchargé en cache privé et conservé temporairement.",
                                icon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                                actionLabel = if (state.reportGenerating) "Préparation…" else "Télécharger le PDF",
                                enabled = !state.reportGenerating && state.selectedPeriod != null,
                            ) {
                                state.selectedPeriod?.let { period ->
                                    viewModel.downloadReport(context.cacheDir, period) { result ->
                                        when (result) {
                                            is ApiResult.Success -> {
                                                pdfViewerFile = result.data
                                                onToast("PDF téléchargé et ouvert dans Atlas.")
                                            }
                                            is ApiResult.Failure -> onToast(result.message)
                                        }
                                    }
                                }
                            }
                        }
                        item(key = "combined-report") {
                            ReportActionCard(
                                title = "Pack de rapports combiné",
                                description = "Pack pour ${state.selectedPeriod ?: "le mois sélectionné"} : balance, stock, créances clients et fournisseurs, et paie. Ouvert dans le lecteur intégré.",
                                icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null) },
                                actionLabel = if (state.reportGenerating) "Préparation…" else "Ouvrir dans Atlas",
                                enabled = !state.reportGenerating,
                            ) { confirmCombined = true }
                        }
                        item(key = "report-note") {
                            Text("Les chiffres proviennent des API financières du serveur sélectionné. Aucun solde n'est calculé ou codé en dur dans l'application.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(selectedPeriod: String?, onSelect: (String) -> Unit) {
    val now = remember { YearMonth.now() }
    val periods = remember(now) { (0..23).map { now.minusMonths(it.toLong()) } }
    var expanded by remember { mutableStateOf(false) }
    val current = selectedPeriod?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
    Box {
        OutlinedButton(onClick = { expanded = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
            Text(current?.let(::monthLabel) ?: "Mois", style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Outlined.ExpandMore, contentDescription = "Choisir un mois")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            periods.forEach { month ->
                DropdownMenuItem(
                    text = { Text(monthLabel(month)) },
                    onClick = { expanded = false; onSelect(month.toString()) },
                )
            }
        }
    }
}

private fun monthLabel(month: YearMonth): String {
    val name = month.month.getDisplayName(TextStyle.FULL, Locale.FRENCH)
    return "${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString() }} ${month.year}"
}

@Composable
private fun BalanceSummary(opening: Double, closing: Double, income: Double, outflows: Double, currency: String, compact: Boolean) {
    val cards = listOf(
        Triple("Solde d'ouverture", opening, Icons.Outlined.AccountBalanceWallet),
        Triple("Solde actuel", closing, Icons.Outlined.AccountBalanceWallet),
        Triple("Entrées", income, Icons.Outlined.ArrowDownward),
        Triple("Sorties payées", outflows, Icons.Outlined.ArrowUpward),
    )
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cards.chunked(2).forEach { pair -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (title, value, icon) -> SummaryAmountCard(title, value, currency, icon = icon, modifier = Modifier.weight(1f)) }
            } }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            cards.forEach { (title, value, icon) -> SummaryAmountCard(title, value, currency, icon = icon, modifier = Modifier.weight(1f)) }
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
                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(if (balanced == true) "Relevé réconcilié" else "Réconciliation indisponible", style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.SemiBold)
                Text("${if (status == "open") "Mois en cours" else "Période clôturée"} · arrêté au $asOf", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = color)
        }
    }
}

@Composable
private fun StatementRowCard(row: FinanceStatementRowDto, currency: String) {
    val kind = when (row.type) {
        "sale" -> "Encaissement"
        "refund" -> "Remboursement"
        "expense" -> "Dépense"
        "payroll" -> "Paie"
        "supplier" -> "Fournisseur"
        else -> row.type.ifBlank { "Mouvement" }
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.label.ifBlank { kind }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("$kind · ${row.ref} · ${row.ts}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                val amount = if (row.credit > 0.0) "+${formatMoney(row.credit, currency)}" else "−${formatMoney(row.debit, currency)}"
                Text(amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (row.credit > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Text("Solde ${formatMoney(row.runningBalance, currency)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReceivableCard(document: FinanceOutstandingDocumentDto, currency: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(document.party.ifBlank { "Client" }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(document.state.replaceFirstChar { it.uppercase(Locale.FRENCH) }, style = MaterialTheme.typography.labelMedium, color = if (document.state == "overdue") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            Text("${document.number} · échéance ${document.dueDate ?: "non définie"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(vertical = 3.dp))
            Text("Restant dû  ${formatMoney(document.remaining, currency)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("Payé ${formatMoney(document.paid, currency)} / Total ${formatMoney(document.total, currency)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val categories: List<String>,
) {
    EXPENSE("expense", "Dépense", "+ Dépense", Icons.Outlined.ArrowUpward, listOf("Loyer", "Transport", "Fournitures", "Salaires", "Taxes", "Autre dépense")),
    INCOME("income", "Recette", "+ Recette", Icons.Outlined.ArrowDownward, listOf("Vente divers", "Prestation de service", "Remboursement", "Subvention", "Intérêts", "Autre recette")),
    OWNER_CONTRIB("owner_contrib", "Apport propriétaire", "+ Apport", Icons.Outlined.AccountBalanceWallet, listOf("Apport en capital", "Apport en compte courant", "Autre apport")),
    OWNER_DRAW("owner_draw", "Prélèvement propriétaire", "+ Prélèvement", Icons.Outlined.ArrowUpward, listOf("Prélèvement personnel", "Dividende", "Remboursement associé", "Autre prélèvement")),
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle écriture — ${kind.label}", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    if (step == 0) "Étape 1/2 · Type et libellé" else "Étape 2/2 · Montant et règlement",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (step == 0) {
                    EntryDropdown("Type", kind.label, EntryKind.entries.map { it.label }) { label ->
                        EntryKind.entries.firstOrNull { it.label == label }?.let { selected ->
                            kind = selected
                            category = selected.categories.first()
                        }
                    }
                    EntryDropdown("Catégorie", category, kind.categories) { category = it }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Libellé *") },
                        placeholder = { Text("Ex. Loyer entrepôt octobre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (description.isBlank()) Text("La description est obligatoire.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                } else {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                        label = { Text("Montant *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it.take(10) },
                        label = { Text("Date (AAAA-MM-JJ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = !validDate,
                    )
                    EntryDropdown(
                        "Mode de paiement",
                        when (payment) { "bank" -> "Banque / virement"; "card" -> "Carte"; "cheque" -> "Chèque"; else -> "Espèces (caisse)" },
                        listOf("Espèces (caisse)", "Banque / virement", "Carte", "Chèque"),
                    ) { label -> payment = when (label) { "Banque / virement" -> "bank"; "Carte" -> "card"; "Chèque" -> "cheque"; else -> "cash" } }
                    OutlinedTextField(
                        value = vendor,
                        onValueChange = { vendor = it },
                        label = { Text("Bénéficiaire / fournisseur (facultatif)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (facultatif)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    if (parsedAmount == null || parsedAmount <= 0.0) Text("Saisissez un montant supérieur à zéro.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    if (!validDate) Text("La date doit être une date valide au format AAAA-MM-JJ.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            if (step == 0) {
                Button(onClick = { if (description.isNotBlank()) step = 1 }, enabled = description.isNotBlank() && !saving) { Text("Continuer") }
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
                    else Text("Enregistrer")
                }
            }
        },
        dismissButton = {
            Row {
                if (step == 1) TextButton(onClick = { step = 0 }, enabled = !saving) { Text("Retour") }
                TextButton(onClick = onDismiss, enabled = !saving) { Text("Annuler") }
            }
        },
    )
}

@Composable
private fun EntryDropdown(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Outlined.ExpandMore, contentDescription = "Choisir $label")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { expanded = false; onSelect(option) }) }
            }
        }
    }
}

private fun formatMoney(value: Double, currency: String): String =
    "${NumberFormat.getNumberInstance(Locale.FRANCE).apply { maximumFractionDigits = 2; minimumFractionDigits = 0 }.format(value)} $currency"
