package ma.etherealmber.atlascommerce.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.model.FinanceMonthlyDto
import ma.etherealmber.atlascommerce.data.model.PayrollPageDto
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class NativeFinanceReportViewModel @Inject constructor(
    private val repository: CommerceRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    data class UiState(
        val loading: Boolean = false,
        val error: String? = null,
        val financeMonths: List<FinanceMonthlyDto> = emptyList(),
        val products: List<ProductDto> = emptyList(),
        val payroll: PayrollPageDto? = null,
        val sectionErrors: Map<String, String> = emptyMap(),
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()
    private var requestId = 0L

    fun load(selection: FinanceReportSelection) {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user
        if (activeUser == null || !Roles.isAdmin(activeUser.role)) {
            _state.value = UiState(error = "Réservé aux administrateurs (owner/admin).")
            return
        }
        val months = runCatching { financeReportMonths(selection) }.getOrElse {
            _state.value = UiState(error = it.message ?: "Période de rapport invalide.")
            return
        }
        if (months.size > MAX_MONTHS) {
            _state.value = UiState(error = "Pour un rapport natif, choisissez une période de 24 mois maximum.")
            return
        }

        val request = ++requestId
        val scopeIdentity = activeUser.scopeIdentity
        val sections = selection.sectionKeys.toSet()
        val needsFinance = sections.any { it != FinanceReportSection.STOCK.key }
        val needsProducts = FinanceReportSection.STOCK.key in sections
        val needsPayroll = FinanceReportSection.EMPLOYEES.key in sections
        _state.value = UiState(loading = true)
        viewModelScope.launch {
            val financeSemaphore = Semaphore(4)
            val financeRequests = if (needsFinance) {
                months.map { period ->
                    async { period to financeSemaphore.withPermit { repository.financeMonthly(period.toString()) } }
                }
            } else emptyList()
            val productsRequest = if (needsProducts) async { repository.products(limit = 300) } else null
            val payrollRequest = if (needsPayroll) {
                async { repository.employeePayroll(months.last().toString(), pageSize = 50) }
            } else null
            val financeResults = financeRequests.awaitAll()
            val products = productsRequest?.await()
            val payroll = payrollRequest?.await()
            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == scopeIdentity
            if (request != requestId || !stillSameScope) return@launch

            val successfulFinance = financeResults.mapNotNull { (_, result) -> (result as? ApiResult.Success)?.data }
            val errors = buildMap<String, String> {
                financeResults.firstNotNullOfOrNull { (_, result) -> (result as? ApiResult.Failure)?.message }
                    ?.let { put("finance", it) }
                (products as? ApiResult.Failure)?.let { put("stock", it.message) }
                (payroll as? ApiResult.Failure)?.let { put("employees", it.message) }
            }
            _state.value = UiState(
                loading = false,
                error = if (successfulFinance.isEmpty() && needsFinance && products !is ApiResult.Success) errors.values.firstOrNull() else null,
                financeMonths = successfulFinance,
                products = (products as? ApiResult.Success)?.data.orEmpty(),
                payroll = (payroll as? ApiResult.Success)?.data,
                sectionErrors = errors,
            )
        }
    }

    companion object {
        private const val MAX_MONTHS = 24
    }
}

private fun financeReportMonths(selection: FinanceReportSelection): List<YearMonth> {
    val today = YearMonth.now()
    val first = when (selection.period) {
        "month" -> today
        "3m" -> today.minusMonths(2)
        "6m" -> today.minusMonths(5)
        "12m" -> today.minusMonths(11)
        "year" -> today.withMonth(1)
        "custom" -> YearMonth.from(LocalDate.parse(selection.from ?: error("Date de début manquante.")))
        else -> error("Période de rapport invalide.")
    }
    val last = if (selection.period == "custom") {
        YearMonth.from(LocalDate.parse(selection.to ?: error("Date de fin manquante.")))
    } else today
    require(!last.isBefore(first)) { "La date de fin doit suivre la date de début." }
    val monthSpan = java.time.temporal.ChronoUnit.MONTHS.between(first, last)
    require(monthSpan < 24L) { "Pour un rapport natif, choisissez une période de 24 mois maximum." }
    return (0..monthSpan.toInt()).map { first.plusMonths(it.toLong()) }
}

@Composable
fun NativeFinanceReportDialog(
    selection: FinanceReportSelection,
    onDismiss: () -> Unit,
    fallbackCurrency: String = "MAD",
    viewModel: NativeFinanceReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val sections = remember(selection.sectionKeys) { selection.selectedSections() }
    val tabKeys = remember(sections) { listOf("overview") + sections.map { it.key } }
    var activeKey by remember(selection) { mutableStateOf("overview") }
    LaunchedEffect(selection) { viewModel.load(selection) }
    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = atlasText("Fermer", "Close", "إغلاق"))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(atlasText("Rapport financier", "Finance report", "التقرير المالي"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(selection.localizedLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.load(selection) }, enabled = !state.loading) {
                        Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser", "Refresh", "تحديث"))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tabKeys.forEach { key ->
                        val label = when (key) {
                            "overview" -> atlasText("Vue d’ensemble", "Overview", "نظرة عامة")
                            else -> sections.firstOrNull { it.key == key }?.let { it.localizedLabel() }.orEmpty()
                        }
                        FilterChip(selected = activeKey == key, onClick = { activeKey = key }, label = { Text(label, maxLines = 1) })
                    }
                }
                when {
                    state.loading && state.financeMonths.isEmpty() && state.products.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text(atlasText("Chargement des données financières…", "Loading finance data…", "جارٍ تحميل البيانات المالية…"))
                            }
                        }
                    }
                    state.error != null && state.financeMonths.isEmpty() && state.products.isEmpty() -> {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (selection.period == "custom") item {
                            ReportNote(atlasText(
                                "Les totaux sont chargés par mois complet; l'API mobile ne fournit pas de filtre journalier pour cette période personnalisée.",
                                "Totals are loaded by whole month; the mobile API does not provide day-level filtering for this custom range.",
                                "يتم تحميل الإجماليات حسب الشهر الكامل؛ لا توفر واجهة الهاتف تصفية يومية لهذه الفترة المخصصة.",
                            ))
                        }
                        state.sectionErrors.values.distinct().forEach { message ->
                            item { ReportNote(atlasText("Certaines données n'ont pas pu être chargées : $message", "Some data could not be loaded: $message", "تعذر تحميل بعض البيانات: $message")) }
                        }
                        if (state.loading) item { CircularProgressIndicator(Modifier.size(24.dp)) }
                        when (activeKey) {
                            "overview" -> item {
                                NativeReportOverview(
                                    financeMonths = state.financeMonths,
                                    products = state.products,
                                    payroll = state.payroll,
                                    sections = sections,
                                    errors = state.sectionErrors,
                                    fallbackCurrency = fallbackCurrency,
                                )
                            }
                            FinanceReportSection.BALANCE.key -> item { NativeBalanceSection(state.financeMonths, state.sectionErrors["finance"]) }
                            FinanceReportSection.STOCK.key -> item { NativeStockSection(state.products, state.sectionErrors["stock"], fallbackCurrency) }
                            FinanceReportSection.RECEIVABLES.key -> item { NativeOutstandingSection(state.financeMonths.lastOrNull()?.receivables, "Créances clients", "Customer receivables", "مستحقات العملاء", state.sectionErrors["finance"], state.financeMonths.lastOrNull()?.currency ?: fallbackCurrency) }
                            FinanceReportSection.PAYABLES.key -> item { NativeOutstandingSection(state.financeMonths.lastOrNull()?.payables, "Fournisseurs", "Suppliers", "الموردون", state.sectionErrors["finance"], state.financeMonths.lastOrNull()?.currency ?: fallbackCurrency) }
                            FinanceReportSection.EMPLOYEES.key -> item { NativePayrollSection(state.payroll, state.financeMonths.lastOrNull(), state.sectionErrors["employees"]) }
                            FinanceReportSection.AGING.key -> item { NativeAgingSection(state.financeMonths.lastOrNull()) }
                            FinanceReportSection.TVA.key -> item { NativeUnavailableSection("TVA / VAT", state.sectionErrors["finance"]) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeReportOverview(
    financeMonths: List<FinanceMonthlyDto>,
    products: List<ProductDto>,
    payroll: PayrollPageDto?,
    sections: List<FinanceReportSection>,
    errors: Map<String, String>,
    fallbackCurrency: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(atlasText("Données réelles du compte, affichées dans Atlas", "Live account data, shown inside Atlas", "بيانات الحساب الفعلية معروضة داخل Atlas"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        sections.forEach { section ->
            when (section) {
                FinanceReportSection.BALANCE -> NativeBalanceSection(financeMonths, errors["finance"])
                FinanceReportSection.STOCK -> NativeStockSection(products, errors["stock"], fallbackCurrency)
                FinanceReportSection.RECEIVABLES -> NativeOutstandingSection(financeMonths.lastOrNull()?.receivables, "Créances clients", "Customer receivables", "مستحقات العملاء", errors["finance"], financeMonths.lastOrNull()?.currency ?: fallbackCurrency)
                FinanceReportSection.PAYABLES -> NativeOutstandingSection(financeMonths.lastOrNull()?.payables, "Fournisseurs", "Suppliers", "الموردون", errors["finance"], financeMonths.lastOrNull()?.currency ?: fallbackCurrency)
                FinanceReportSection.EMPLOYEES -> NativePayrollSection(payroll, financeMonths.lastOrNull(), errors["employees"])
                FinanceReportSection.AGING -> NativeAgingSection(financeMonths.lastOrNull())
                FinanceReportSection.TVA -> NativeUnavailableSection("TVA / VAT", errors["finance"])
            }
        }
    }
}

@Composable
private fun NativeBalanceSection(months: List<FinanceMonthlyDto>, error: String?) {
    ReportSectionCard(atlasText("Balance", "Balance", "الرصيد")) {
        if (months.isEmpty()) ReportError(error) else {
            months.forEach { finance ->
                Text(finance.period, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                ReportMetric(atlasText("Solde de clôture", "Closing balance", "الرصيد الختامي"), finance.closingBalance.formattedMoney(finance.currency))
                ReportMetric(atlasText("Solde d’ouverture", "Opening balance", "الرصيد الافتتاحي"), finance.openingBalance.formattedMoney(finance.currency))
                ReportMetric(atlasText("Ventes encaissées", "Sales received", "المبيعات المحصلة"), finance.salesReceived.formattedMoney(finance.currency))
                ReportMetric(atlasText("Dépenses", "Expenses", "المصروفات"), finance.expenses.formattedMoney(finance.currency))
                ReportMetric(atlasText("Remboursements", "Refunds", "المبالغ المستردة"), finance.refunds.formattedMoney(finance.currency))
                if (months.last() != finance) androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun NativeStockSection(products: List<ProductDto>, error: String?, currency: String) {
    ReportSectionCard(atlasText("Stock", "Inventory", "المخزون")) {
        if (products.isEmpty() && error != null) ReportError(error) else {
            val tracked = products.filter { it.trackInventory && it.isActive }
            val low = tracked.count { it.isLowStock }
            val value = tracked.sumOf { it.stock * it.price }
            ReportMetric(atlasText("Articles suivis", "Tracked items", "الأصناف المتتبعة"), tracked.size.toString())
            ReportMetric(atlasText("Valeur au prix de vente", "Retail-price value", "القيمة بسعر البيع"), value.formattedMoney(currency))
            ReportMetric(atlasText("Stock faible / épuisé", "Low / out of stock", "مخزون منخفض / نافد"), low.toString())
            tracked.sortedWith(compareBy<ProductDto> { !it.isOutOfStock }.thenBy { !it.isLowStock }.thenBy { it.stock })
                .take(100).forEach { product ->
                    ReportMetric(
                        product.displayName?.takeIf(String::isNotBlank) ?: product.name,
                        "${product.stock.toString().trimEndZeroes()} ${product.unit.orEmpty()} · ${product.price.formattedMoney(currency)}",
                    )
                }
            if (products.size >= 300) ReportNote(atlasText("Affichage limité aux 300 premiers articles chargés.", "Showing the first 300 loaded products.", "يقتصر العرض على أول 300 صنف تم تحميله."))
        }
    }
}

@Composable
private fun NativeOutstandingSection(
    bucket: ma.etherealmber.atlascommerce.data.model.FinanceOutstandingBucketDto?,
    frenchTitle: String,
    englishTitle: String,
    arabicTitle: String,
    error: String?,
    currency: String,
) {
    ReportSectionCard(atlasText(frenchTitle, englishTitle, arabicTitle)) {
        if (bucket == null) ReportError(error) else {
            ReportMetric(atlasText("À recevoir / payer", "Outstanding", "المستحق"), bucket.totalOutstanding.formattedMoney(currency))
            ReportMetric(atlasText("En retard", "Overdue", "متأخر"), bucket.totalOverdue.formattedMoney(currency))
            ReportMetric(atlasText("Documents", "Documents", "المستندات"), bucket.count.toString())
            bucket.documents.take(100).forEach { document ->
                ReportMetric(
                    "${document.number} · ${document.party}",
                    "${document.remaining.formattedMoney(currency)} · ${document.state}${document.dueDate?.let { " · $it" }.orEmpty()}",
                )
            }
            if (bucket.truncated) ReportNote(atlasText("La liste est limitée par le serveur.", "The server limited this list.", "قائمة المستندات محدودة من الخادم."))
        }
    }
}

@Composable
private fun NativePayrollSection(payroll: PayrollPageDto?, finance: FinanceMonthlyDto?, error: String?) {
    ReportSectionCard(atlasText("Employés et paie", "Employees and payroll", "الموظفون والرواتب")) {
        if (payroll == null && finance == null) ReportError(error) else {
            val currency = payroll?.currency ?: finance?.currency ?: "MAD"
            ReportMetric(atlasText("Montant dû", "Amount due", "المبلغ المستحق"), (payroll?.totals?.amountDue ?: finance?.payroll?.totalDue ?: 0.0).formattedMoney(currency))
            ReportMetric(atlasText("Employés en attente", "Employees pending", "الموظفون بانتظار الدفع"), (payroll?.totals?.unpaidCount ?: finance?.payroll?.employees ?: 0).toString())
            payroll?.rows.orEmpty().forEach { row ->
                ReportMetric(row.displayName, "${row.amountDue.formattedMoney(currency)} · ${row.status}")
            }
            if (payroll?.hasMore == true) ReportNote(atlasText("Seuls les 50 premiers résultats sont affichés.", "Showing the first 50 results.", "يتم عرض أول 50 نتيجة فقط."))
        }
    }
}

@Composable
private fun NativeAgingSection(finance: FinanceMonthlyDto?) {
    ReportSectionCard(atlasText("Balance âgée", "Aging", "أعمار الديون")) {
        if (finance == null) ReportError(null) else {
            val documents = finance.receivables.documents.map { it to "${atlasText("Client", "Customer", "عميل")} · ${finance.currency}" } +
                finance.payables.documents.map { it to "${atlasText("Fournisseur", "Supplier", "مورد")} · ${finance.currency}" }
            val overdue = documents.filter { (document, _) ->
                document.dueDate?.let { due -> runCatching { LocalDate.parse(due).isBefore(LocalDate.now()) }.getOrDefault(false) } == true
            }
            val notOverdue = documents.filterNot { it in overdue }
            ReportMetric(atlasText("Documents en retard", "Overdue documents", "المستندات المتأخرة"), overdue.size.toString())
            ReportMetric(atlasText("Documents non échus / sans échéance", "Not due / no due date", "غير مستحقة / بلا تاريخ استحقاق"), notOverdue.size.toString())
            (overdue + notOverdue).take(100).forEach { (document, kind) ->
                ReportMetric("${document.number} · ${document.party}", "${kind.substringBefore(" · ")} · ${document.remaining.formattedMoney(finance.currency)} · ${document.dueDate ?: atlasText("Sans échéance", "No due date", "بلا تاريخ استحقاق")}")
            }
        }
    }
}

@Composable
private fun NativeUnavailableSection(title: String, error: String?) {
    ReportSectionCard(title) {
        ReportNote(
            error ?: atlasText(
                "Le serveur ne fournit pas actuellement les données détaillées de TVA à l'application mobile. Aucun montant n'est estimé.",
                "The server does not currently provide detailed VAT data to the mobile app. No amounts are estimated.",
                "لا يوفّر الخادم حالياً بيانات ضريبة القيمة المضافة التفصيلية للتطبيق. لم يتم تقدير أي مبالغ.",
            ),
        )
    }
}

@Composable
private fun ReportSectionCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ReportMetric(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReportNote(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Text(text, Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun ReportError(error: String?) {
    Text(error ?: atlasText("Aucune donnée disponible.", "No data available.", "لا توجد بيانات متاحة."), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun FinanceReportSelection.localizedLabel(): String = when (period) {
    "month" -> atlasText("Mois en cours", "Current month", "الشهر الحالي")
    "3m" -> atlasText("3 derniers mois", "Last 3 months", "آخر 3 أشهر")
    "6m" -> atlasText("6 derniers mois", "Last 6 months", "آخر 6 أشهر")
    "12m" -> atlasText("12 derniers mois", "Last 12 months", "آخر 12 شهراً")
    "year" -> atlasText("Année en cours", "Current year", "السنة الحالية")
    "custom" -> if (from != null && to != null) "$from → $to" else atlasText("Période personnalisée", "Custom period", "فترة مخصصة")
    else -> period
}

@Composable
private fun FinanceReportSection.localizedLabel(): String = when (this) {
    FinanceReportSection.BALANCE -> atlasText("Balance", "Balance", "الرصيد")
    FinanceReportSection.STOCK -> atlasText("Stock", "Inventory", "المخزون")
    FinanceReportSection.RECEIVABLES -> atlasText("Créances clients", "Customer receivables", "مستحقات العملاء")
    FinanceReportSection.PAYABLES -> atlasText("Fournisseurs", "Suppliers", "الموردون")
    FinanceReportSection.EMPLOYEES -> atlasText("Employés et paie", "Employees and payroll", "الموظفون والرواتب")
    FinanceReportSection.AGING -> atlasText("Balance âgée", "Aging", "أعمار الديون")
    FinanceReportSection.TVA -> atlasText("TVA", "VAT", "ضريبة القيمة المضافة")
}

@Composable
private fun Double.formattedMoney(currency: String = "MAD"): String = atlasMoney(this, currency)
private fun Double.trimEndZeroes(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
