package ma.etherealmber.atlascommerce.ui.screens.employees

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
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.format.AtlasFormat
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.PayrollRowDto
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isAdmin = Roles.isAdmin(user.role)
    var query by rememberSaveable(user.scopeIdentity) { mutableStateOf("") }
    var paymentFilter by rememberSaveable(user.scopeIdentity) { mutableStateOf("Tous") }

    val timezone = state.finance?.timezone?.takeIf { it.isNotBlank() } ?: "Africa/Casablanca"
    val currentMonth = remember(timezone) {
        runCatching { YearMonth.now(ZoneId.of(timezone)) }.getOrElse { YearMonth.now() }
    }
    val period = state.selectedFinancePeriod ?: state.finance?.period ?: currentMonth.toString()
    val selectedMonth = remember(period) { runCatching { YearMonth.parse(period) }.getOrDefault(currentMonth) }
    val monthLabel = remember(selectedMonth) {
        selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString() }
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
            title = "Accès réservé",
            message = "La liste des employés et les informations de paie sont réservées aux rôles administrateur. Aucune donnée n'a été demandée au serveur.",
        )
        return
    }

    if (state.loading && state.finance == null && state.payroll == null) {
        AtlasLoading(message = "Chargement des données employés…")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
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
                        Text("Suivi de la paie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Consultation seule · échéances et versements du serveur Atlas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::refresh, enabled = !state.refreshing && !state.financeLoading) {
                        if (state.refreshing || state.financeLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser les employés")
                        }
                    }
                }
                AtlasDivider(Modifier.padding(vertical = 10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.selectFinancePeriod(selectedMonth.minusMonths(1).toString()) },
                        enabled = !state.financeLoading,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Mois précédent")
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(monthLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (selectedMonth == currentMonth) "Mois en cours · données provisoires" else "Période de paie sélectionnée",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.selectFinancePeriod(selectedMonth.plusMonths(1).toString()) },
                        enabled = !state.financeLoading && selectedMonth < currentMonth,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Mois suivant")
                    }
                }
            }
        }

        state.financeError?.let { error ->
            item(key = "employees_error") {
                AtlasErrorCard(title = "Données indisponibles", message = error, onRetry = viewModel::refresh)
            }
        }

        item(key = "employee_summary") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard(
                    label = "Employés concernés",
                    value = payroll?.total?.toString() ?: "—",
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    label = "À payer",
                    value = payroll?.totals?.unpaidCount?.toString() ?: "—",
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    label = "Montant dû",
                    value = payroll?.let { AtlasFormat.moneyCompact(it.totals.amountDue, it.currency) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "employee_filters") {
            AtlasCard {
                AtlasTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "Rechercher un employé",
                    placeholder = "Nom ou identifiant",
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
                            label = { Text(option) },
                        )
                    }
                }
            }
        }

        if (state.financeLoading && payroll == null) {
            item(key = "employee_loading") {
                AtlasCard { AtlasLoading(message = "Chargement de la paie…") }
            }
        } else if (visibleRows.isEmpty()) {
            item(key = "employee_empty") {
                AtlasEmptyState(
                    icon = Icons.Outlined.Groups,
                    title = if (payroll?.rows.isNullOrEmpty()) "Aucun versement pour cette période" else "Aucun résultat",
                    message = if (payroll?.rows.isNullOrEmpty()) {
                        "Le serveur n'a renvoyé aucun versement à afficher pour $monthLabel."
                    } else {
                        "Modifiez la recherche ou le filtre de paiement."
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
                        "${payroll.rows.size} sur ${payroll.total} affichés",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = viewModel::loadMorePayroll, enabled = !state.payrollLoadingMore) {
                        if (state.payrollLoadingMore) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Afficher plus")
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
    AtlasCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.displayName.ifBlank { "Employé #${row.employeeId}" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = buildString {
                        append("Période ${row.payPeriod.from.ifBlank { "—" }} → ${row.payPeriod.to.ifBlank { "—" }}")
                        row.dueDate?.takeIf(String::isNotBlank)?.let { append(" · Échéance $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(AtlasFormat.money(row.amountDue, currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                AtlasChip(
                    text = status,
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
                "Dernier versement · ${AtlasFormat.money(payment.amount, currency)} · ${payment.paidAt}",
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
