package ma.etherealmber.atlascommerce.ui.screens.employees

import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoneyCompact
import ma.etherealmber.atlascommerce.ui.i18n.atlasShortDate
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.PayrollRowDto
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingDockPadding
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasLoading
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.screens.home.HomeViewModel
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Read-only employee/payroll view backed by the server-authorized payroll endpoint.
 * Employee CRUD is intentionally not simulated in the client because no such API exists here.
 */
@Composable
fun EmployeesScreen(
    user: SessionUser,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val dockPadding = LocalAtlasFloatingDockPadding.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isAdmin = Roles.isAdmin(user.role)
    val language = LocalAtlasLanguage.current
    val locale = when (language) { AppLanguage.FRENCH -> Locale.FRANCE; AppLanguage.ENGLISH -> Locale.UK; AppLanguage.ARABIC -> Locale.forLanguageTag("ar-MA") }
    var query by rememberSaveable(user.scopeIdentity) { mutableStateOf("") }
    var paymentFilter by rememberSaveable(user.scopeIdentity) { mutableStateOf("Tous") }

    val timezone = state.finance?.timezone?.takeIf { it.isNotBlank() } ?: "Africa/Casablanca"
    val currentMonth = remember(timezone) {
        runCatching { YearMonth.now(ZoneId.of(timezone)) }.getOrElse { YearMonth.now() }
    }
    val period = state.selectedFinancePeriod ?: state.finance?.period ?: currentMonth.toString()
    val selectedMonth = remember(period) { runCatching { YearMonth.parse(period) }.getOrDefault(currentMonth) }
    val monthLabel = remember(selectedMonth, locale) {
        selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val payroll = state.payroll
    val visibleRows = remember(payroll?.rows, query, paymentFilter) {
        payroll?.rows.orEmpty().filter { row ->
            val matchesName = query.isBlank() || row.displayName.contains(query.trim(), ignoreCase = true) || row.employeeId.toString().contains(query.trim())
            val status = payrollStatus(row, payroll?.asOf.orEmpty())
            val matchesStatus = paymentFilter == "Tous" || paymentFilter == status
            matchesName && matchesStatus
        }
    }

    if (!isAdmin) {
        AtlasErrorCard(
            title = atlasText("Accès réservé"),
            message = atlasText("La liste des employés et les informations de paie sont réservées aux rôles administrateur. Aucune donnée n'a été demandée au serveur."),
        )
        return
    }

    if (state.loading && state.finance == null && state.payroll == null) {
        AtlasLoading(message = atlasText("Chargement des données employés…", "Loading employee data…", "جارٍ تحميل بيانات الموظفين…"))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp + dockPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "employees_intro") {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(atlasText("Suivi de la paie"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            atlasText("Consultation seule · échéances et versements du serveur Atlas"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::refresh, enabled = !state.refreshing && !state.financeLoading) {
                        if (state.refreshing || state.financeLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser les employés"))
                        }
                    }
                }
                AtlasDivider(Modifier.padding(vertical = 10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.selectFinancePeriod(selectedMonth.minusMonths(1).toString()) },
                        enabled = !state.financeLoading,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = atlasText("Mois précédent"))
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(monthLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (selectedMonth == currentMonth) atlasText("Mois en cours · données provisoires") else atlasText("Période de paie sélectionnée", "Selected payroll period", "فترة الرواتب المحددة"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.selectFinancePeriod(selectedMonth.plusMonths(1).toString()) },
                        enabled = !state.financeLoading && selectedMonth < currentMonth,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = atlasText("Mois suivant"))
                    }
                }
                if (selectedMonth != currentMonth) {
                    TextButton(
                        onClick = { viewModel.selectFinancePeriod(currentMonth.toString()) },
                        enabled = !state.financeLoading,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(atlasText("Revenir au mois en cours", "Return to current month", "العودة إلى الشهر الحالي"))
                    }
                }
                Text(
                    text = atlasText(
                        "Utilisez les flèches pour changer de mois. Le mois choisi filtre les fiches de paie et les totaux ci-dessous. Les données viennent du serveur, en lecture seule; celles du mois en cours peuvent évoluer jusqu'à sa clôture. Les périodes futures ne sont pas disponibles. Les mois antérieurs sont demandés au serveur; une période sans données affiche une liste vide.",
                        "Use the arrows to change months. The selected month filters the payroll records and totals below. Data comes from the server and is read-only; current-month figures may change until month-end. Future months aren't available. Earlier months are requested from the server; a period with no data shows an empty list.",
                        "استخدم الأسهم لتغيير الشهر. يحدد الشهر المختار سجلات الرواتب والإجماليات أدناه. البيانات من الخادم ومتاحة للقراءة فقط؛ وقد تتغير أرقام الشهر الحالي حتى نهايته. الأشهر المستقبلية غير متاحة. تُطلب الأشهر السابقة من الخادم، وتظهر قائمة فارغة للفترات التي لا تحتوي على بيانات.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.financeError?.let { error ->
            item(key = "employees_error") {
                AtlasErrorCard(title = atlasText("Données indisponibles"), message = atlasNotice(error, language), onRetry = viewModel::refresh)
            }
        }

        item(key = "employee_summary") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard(
                    label = atlasText("Employés concernés", "Employees", "الموظفون"),
                    value = payroll?.total?.toString() ?: "—",
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    label = atlasText("À payer", "To pay", "مستحق الدفع"),
                    value = payroll?.totals?.unpaidCount?.toString() ?: "—",
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    label = atlasText("Montant dû", "Amount due", "المبلغ المستحق"),
                    value = payroll?.let { atlasMoneyCompact(it.totals.amountDue, it.currency) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "payroll_explanation") {
            AtlasCard {
                Text(
                    atlasText("Comprendre les chiffres", "How to read these figures", "كيفية فهم الأرقام"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    atlasText(
                        "Employés correspond au nombre de fiches de paie de la période. « À payer » et « Montant dû » reprennent les totaux fournis par le serveur. La période de paie et l'échéance de chaque fiche peuvent différer du mois sélectionné. Le statut de chaque fiche est évalué selon son état et son échéance à la date de référence indiquée.",
                        "Employees is the number of payroll records for this period. To pay and Amount due use totals returned by the server. A record's pay period and due date may differ from the selected month. Each record's status is evaluated using its state and due date as of the reference date shown.",
                        "يمثل عدد الموظفين عدد سجلات الرواتب لهذه الفترة. يعرض «مستحق الدفع» و«المبلغ المستحق» الإجماليات التي يرسلها الخادم. قد تختلف فترة الدفع وتاريخ الاستحقاق لكل سجل عن الشهر المحدد. تُحدد حالة كل سجل بناءً على حالته وتاريخ استحقاقه في التاريخ المرجعي المعروض.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                payroll?.takeIf { it.asOf.isNotBlank() }?.let { payrollData ->
                    Spacer(Modifier.height(6.dp))
                    val asOf = payrollData.asOf
                    val payrollTimezone = payrollData.timezone.takeIf(String::isNotBlank) ?: timezone
                    Text(
                        atlasText(
                            "Statuts évalués au ${atlasShortDate(asOf)} · fuseau $payrollTimezone",
                            "Statuses evaluated as of ${atlasShortDate(asOf)} · timezone $payrollTimezone",
                            "الحالات محسوبة حتى ${atlasShortDate(asOf)} · المنطقة الزمنية $payrollTimezone",
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item(key = "employee_filters") {
            AtlasCard {
                AtlasTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = atlasText("Rechercher un employé"),
                    placeholder = atlasText("Nom ou identifiant"),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Tous", "À payer", "À venir", "Payé").forEach { option ->
                        FilterChip(
                            selected = paymentFilter == option,
                            onClick = { paymentFilter = option },
                            label = { Text(atlasText(option, when (option) { "Tous" -> "All"; "À payer" -> "To pay"; else -> option }, when (option) { "Tous" -> "الكل"; "À payer" -> "مستحق الدفع"; "À venir" -> "قادم"; else -> "مدفوع" })) },
                        )
                    }
                }
            }
        }

        if (state.financeLoading && payroll == null) {
            item(key = "employee_loading") {
                AtlasCard { AtlasLoading(message = atlasText("Chargement de la paie…", "Loading payroll…", "جارٍ تحميل الرواتب…")) }
            }
        } else if (visibleRows.isEmpty()) {
            item(key = "employee_empty") {
                AtlasEmptyState(
                    icon = Icons.Outlined.Groups,
                    title = if (payroll?.rows.isNullOrEmpty()) {
                        atlasText("Aucun versement pour cette période", "No payments for this period", "لا توجد دفعات لهذه الفترة")
                    } else {
                        atlasText("Aucun résultat")
                    },
                    message = if (payroll?.rows.isNullOrEmpty()) {
                        atlasText(
                            "Le serveur n'a renvoyé aucun versement à afficher pour $monthLabel.",
                            "The server returned no payments to show for $monthLabel.",
                            "لم يُرجع الخادم أي دفعات لعرضها عن $monthLabel.",
                        )
                    } else {
                        atlasText("Modifiez la recherche ou le filtre de paiement.", "Change the search or payment filter.", "غيّر البحث أو عامل تصفية الدفعات.")
                    },
                )
            }
        } else {
            items(visibleRows, key = { "${it.employeeId}-${it.payPeriod.from}" }) { row ->
                EmployeePayrollCard(row = row, asOf = payroll?.asOf.orEmpty(), currency = payroll?.currency ?: "MAD")
            }
        }

        if (payroll?.hasMore == true) {
            item(key = "employee_more") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        atlasText("${payroll.rows.size} sur ${payroll.total} affichés", "${payroll.rows.size} of ${payroll.total} shown", "${payroll.rows.size} من أصل ${payroll.total} معروض"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = viewModel::loadMorePayroll, enabled = !state.payrollLoadingMore) {
                        if (state.payrollLoadingMore) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text(atlasText("Afficher plus"))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    AtlasCard(modifier = modifier, padding = 10.dp) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EmployeePayrollCard(row: PayrollRowDto, asOf: String, currency: String) {
    val status = payrollStatus(row, asOf)
    val fromDate = atlasShortDate(row.payPeriod.from).takeIf { it != "—" } ?: "—"
    val toDate = atlasShortDate(row.payPeriod.to).takeIf { it != "—" } ?: "—"
    val periodLabel = atlasText("Période $fromDate → $toDate", "Period $fromDate → $toDate", "الفترة $fromDate → $toDate")
    val dueDateLabel = row.dueDate?.takeIf(String::isNotBlank)?.let {
        atlasText(" · Échéance ${atlasShortDate(it)}", " · Due ${atlasShortDate(it)}", " · الاستحقاق ${atlasShortDate(it)}")
    }.orEmpty()
    AtlasCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    row.displayName.ifBlank { atlasText("Employé #${row.employeeId}", "Employee #${row.employeeId}", "موظف رقم ${row.employeeId}") },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = periodLabel + dueDateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(atlasMoney(row.amountDue, currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                AtlasChip(
                    text = when (status) {
                        "Payé" -> atlasText("Payé", "Paid", "مدفوع")
                        "À venir" -> atlasText("À venir", "Upcoming", "قادم")
                        "À payer" -> atlasText("À payer", "To pay", "مستحق الدفع")
                        else -> status
                    },
                    tone = when (status) {
                        "Payé" -> AtlasTone.POSITIVE
                        "À venir" -> AtlasTone.INFO
                        else -> AtlasTone.WARNING
                    },
                )
            }
        }
        row.lastPayment?.let { payment ->
            AtlasDivider(Modifier.padding(vertical = 8.dp))
            Text(
                atlasText(
                    "Dernier versement · ${atlasMoney(payment.amount, currency)} · ${atlasShortDate(payment.paidAt)}",
                    "Last payment · ${atlasMoney(payment.amount, currency)} · ${atlasShortDate(payment.paidAt)}",
                    "آخر دفعة · ${atlasMoney(payment.amount, currency)} · ${atlasShortDate(payment.paidAt)}",
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun payrollStatus(row: PayrollRowDto, asOf: String): String = when {
    row.status.equals("paid", ignoreCase = true) && row.dueDate?.let { it > asOf } == true -> "À venir"
    row.status.equals("paid", ignoreCase = true) -> "Payé"
    else -> "À payer"
}
