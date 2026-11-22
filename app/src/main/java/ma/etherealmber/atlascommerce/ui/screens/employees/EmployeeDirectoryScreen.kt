package ma.etherealmber.atlascommerce.ui.screens.employees

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.EmployeeDto
import ma.etherealmber.atlascommerce.data.model.EmployeeUpdateRequest
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasFloatingTopBar
import ma.etherealmber.atlascommerce.ui.components.rememberAtlasFloatingTopBarScrollConnection
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasShortDate
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

@Composable
fun EmployeeDirectoryScreen(
    user: SessionUser,
    onDetailsVisibilityChanged: (Boolean) -> Unit = {},
    viewModel: EmployeeDirectoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val canEdit = Roles.isAdmin(user.role) || Roles.normalize(user.role) == Roles.SECRETARY
    var query by rememberSaveable(user.scopeIdentity) { mutableStateOf("") }
    var showFormer by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<EmployeeDto?>(null) }
    var detailEmployeeId by remember { mutableStateOf<Long?>(null) }
    val employee = state.selectedEmployee
    val detailActive = detailEmployeeId != null || employee != null || state.detailsLoading || !state.actionError.isNullOrBlank()
    LaunchedEffect(detailActive) { onDetailsVisibilityChanged(detailActive) }
    androidx.activity.compose.BackHandler(enabled = detailActive) {
        if (!state.saving) { detailEmployeeId = null; viewModel.clearDetails() }
    }
    val visible = state.employees.filter { row ->
        (showFormer == (row.active == 0)) &&
            (query.isBlank() || listOf(row.displayName, row.employeeNumber, row.roleTitle, row.department, row.phone)
                .any { it.contains(query.trim(), ignoreCase = true) })
    }
    if (!canEdit) {
        AtlasErrorCard(title = atlasText("Accès réservé", "Restricted access", "الوصول مقيد"), message = atlasText("Le répertoire des employés est réservé aux administrateurs et secrétaires."))
        return
    }
    if (detailActive) {
        EmployeeProfilePage(
            employee = employee,
            loading = state.detailsLoading,
            error = state.actionError,
            saving = state.saving,
            canEdit = canEdit,
            canMarkFormer = Roles.isAdmin(user.role),
            onClose = { detailEmployeeId = null; viewModel.clearDetails() },
            onRefresh = { detailEmployeeId?.let(viewModel::showDetails) },
            onEdit = { employee?.let { editTarget = it } },
            onMarkFormer = { employee?.let { viewModel.markEmployeeFormer(it.id, "Départ") } },
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                AtlasCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(atlasText("Équipe", "Employees", "الموظفون"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(atlasText("${visible.size} employé(s)", "${visible.size} employees", "${visible.size} موظف"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = viewModel::load, enabled = !state.loading) {
                            if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser", "Refresh", "تحديث"))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !showFormer, onClick = { showFormer = false }, label = { Text(atlasText("En poste", "Current", "الحاليون")) })
                        FilterChip(selected = showFormer, onClick = { showFormer = true }, label = { Text(atlasText("Anciens", "Former", "السابقون")) })
                    }
                    AtlasTextField(value = query, onValueChange = { query = it }, label = atlasText("Rechercher", "Search", "بحث"), placeholder = atlasText("Nom, poste, numéro", "Name, role or number", "الاسم أو المنصب أو الرقم"), modifier = Modifier.fillMaxWidth())
                }
            }
            if (!state.error.isNullOrBlank() && state.employees.isEmpty()) item {
                AtlasErrorCard(title = atlasText("Équipe indisponible", "Directory unavailable", "الدليل غير متاح"), message = state.error.orEmpty(), onRetry = viewModel::load)
            }
            if (!state.loading && visible.isEmpty()) item {
                AtlasEmptyState(icon = Icons.Outlined.Groups, title = atlasText("Aucun employé", "No employees", "لا يوجد موظفون"), message = atlasText("Aucune fiche ne correspond à cette vue.", "No employee records match this view.", "لا توجد ملفات مطابقة لهذا العرض."))
            }
            items(visible, key = { "employee-${it.id}" }) { row ->
                val nextPayment = row.nextDue?.takeIf(String::isNotBlank)?.let { atlasShortDate(it) }
                AtlasCard(onClick = { detailEmployeeId = row.id; viewModel.showDetails(row.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(row.displayName.ifBlank { row.name }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(listOf(row.employeeNumber, row.roleTitle, row.department).filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (row.active == 0) atlasText("Ancien employé", "Former employee", "موظف سابق")
                                else atlasText("Prochain paiement : ${nextPayment ?: "—"} · ${atlasMoney(row.payAmount, "MAD")}", "Next payment: ${nextPayment ?: "—"} · ${atlasMoney(row.payAmount, "MAD")}", "الدفعة التالية: ${nextPayment ?: "—"} · ${atlasMoney(row.payAmount, "MAD")}"),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (row.payOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AtlasChip(text = if (row.active == 0) atlasText("Ancien", "Former", "سابق") else atlasText("Actif", "Active", "نشط"), tone = if (row.active == 0) AtlasTone.NEUTRAL else AtlasTone.POSITIVE)
                    }
                }
            }
        }
    }
    editTarget?.let { target ->
        EmployeeProfileEditDialog(target, saving = state.saving, error = state.actionError, onClose = { editTarget = null }) { request ->
            viewModel.updateEmployee(target.id, request) { ok -> if (ok) editTarget = null }
        }
    }
}

@Composable
private fun EmployeeProfilePage(
    employee: EmployeeDto?,
    loading: Boolean,
    error: String?,
    saving: Boolean,
    canEdit: Boolean,
    canMarkFormer: Boolean,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onMarkFormer: () -> Unit,
) {
    val title = employee?.displayName?.ifBlank { employee.name } ?: atlasText("Employé", "Employee", "موظف")
    var headerExpanded by remember(employee?.id) { mutableStateOf(true) }
    val scrollConnection = ma.etherealmber.atlascommerce.ui.components.rememberAtlasFloatingTopBarScrollConnection(
        expanded = headerExpanded,
        onExpandedChange = { headerExpanded = it },
    )
    Box(Modifier.fillMaxSize().nestedScroll(scrollConnection)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(104.dp)) }
            if (loading && employee == null) item {
                AtlasCard { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp); Text(atlasText("Chargement du profil et des paiements…", "Loading profile and payments…", "جارٍ تحميل الملف والمدفوعات…")) }
            }
            if (employee == null && !error.isNullOrBlank()) item {
                AtlasErrorCard(title = atlasText("Détails indisponibles", "Details unavailable", "التفاصيل غير متاحة"), message = error, onRetry = onRefresh)
            }
            if (employee != null) {
                item {
                    // A full-width profile banner remains part of the scrolling content, not the pinned toolbar.
                    AtlasCard {
                        Text(atlasText("EMPLOYEE PROFILE", "EMPLOYEE PROFILE", "ملف الموظف"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(listOf(employee.employeeNumber, employee.roleTitle, employee.department).filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AtlasChip(text = if (employee.active == 0) atlasText("Ancien", "Former", "سابق") else atlasText("Actif", "Active", "نشط"), tone = if (employee.active == 0) AtlasTone.NEUTRAL else AtlasTone.POSITIVE)
                            Spacer(Modifier.weight(1f))
                            Text(atlasMoney(employee.payAmount, "MAD"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    AtlasCard {
                        Text(atlasText("Profil et cycle de paie", "Profile and pay cycle", "الملف ودورة الدفع"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        listOfNotNull(
                            employee.phone.takeIf(String::isNotBlank)?.let { "${atlasText("Téléphone", "Phone", "الهاتف")}: $it" },
                            employee.email.takeIf(String::isNotBlank)?.let { "Email: $it" },
                            employee.joinedDate.takeIf(String::isNotBlank)?.let { "${atlasText("Date d’entrée", "Start date", "تاريخ البدء")}: ${atlasShortDate(it)}" },
                            employee.lastPaidAt?.takeIf(String::isNotBlank)?.let { "${atlasText("Dernier paiement", "Last payment", "آخر دفعة")}: ${atlasShortDate(it)}" },
                            "${atlasText("Cycle de paie", "Pay cycle", "دورة الدفع")}: ${if (employee.payCycleType == "monthly") atlasText("Mensuel (30 jours)", "Monthly (30 days)", "شهري (30 يوماً)") else atlasText("Tous les ${employee.payCycleDays} jours", "Every ${employee.payCycleDays} days", "كل ${employee.payCycleDays} يوم")}",
                            "${atlasText("Montant journalier", "Daily pay", "الأجر اليومي")}: ${atlasMoney(employee.dailyRate, "MAD")}",
                            if (employee.active == 0) atlasText("Statut : ancien employé", "Status: former employee", "الحالة: موظف سابق") else "${atlasText("Prochain paiement", "Next payment", "الدفعة التالية")}: ${employee.nextDue?.let { atlasShortDate(it) } ?: "—"}",
                        ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        if (canMarkFormer && employee.active != 0) {
                            TextButton(onClick = onMarkFormer, enabled = !saving) {
                                if (saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                else Text(atlasText("Marquer comme ancien", "Mark as former", "تحديد كسابق"))
                            }
                        }
                    }
                }
                item { Text(atlasText("Historique des paiements", "Payment history", "سجل المدفوعات"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                if (employee.payments.isEmpty()) item {
                    AtlasEmptyState(icon = Icons.Outlined.History, title = atlasText("Aucun paiement enregistré", "No payments recorded", "لا توجد مدفوعات مسجلة"), message = atlasText("L’historique apparaîtra ici.", "Payment history will appear here.", "سيظهر سجل المدفوعات هنا."))
                }
                items(employee.payments, key = { "payment-${it.id}" }) { payment ->
                    AtlasCard {
                        Text(atlasMoney(payment.amount, "MAD"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(listOf(payment.periodLabel, payment.paidAt.takeIf(String::isNotBlank)?.let { atlasShortDate(it) }, payment.note).filterNotNull().filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        AtlasFloatingTopBar(
            title = title,
            subtitle = atlasText("Détails et historique de paie", "Details and payroll history", "التفاصيل وسجل الرواتب"),
            onBack = { if (!saving) onClose() },
            expanded = headerExpanded,
            onCollapsedClick = { headerExpanded = true },
            modifier = Modifier.align(Alignment.TopCenter).zIndex(1f),
            actions = {
                IconButton(onClick = onRefresh, enabled = !loading && employee != null) {
                    if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser l’historique", "Refresh history", "تحديث السجل"))
                }
                if (canEdit && employee != null) IconButton(onClick = onEdit, enabled = !saving) {
                    Icon(Icons.Outlined.Edit, contentDescription = atlasText("Modifier l’employé", "Edit employee", "تعديل الموظف"))
                }
            },
        )
    }
}

@Composable
private fun EmployeeProfileEditDialog(employee: EmployeeDto, saving: Boolean, error: String?, onClose: () -> Unit, onSave: (EmployeeUpdateRequest) -> Unit) {
    var first by remember(employee.id) { mutableStateOf(employee.firstName.ifBlank { employee.name }) }
    var last by remember(employee.id) { mutableStateOf(employee.lastName) }
    var joined by remember(employee.id) { mutableStateOf(employee.joinedDate) }
    var pay by remember(employee.id) { mutableStateOf(employee.payAmount.toString()) }
    var cycle by remember(employee.id) { mutableStateOf(employee.payCycleType) }
    var days by remember(employee.id) { mutableStateOf(employee.payCycleDays.toString()) }
    AlertDialog(
        onDismissRequest = { if (!saving) onClose() },
        title = { Text(atlasText("Modifier employé et paie", "Edit employee and pay", "تعديل الموظف والأجر")) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(first, { first = it }, label = { Text(atlasText("Prénom", "First name", "الاسم الأول")) }, singleLine = true)
                OutlinedTextField(last, { last = it }, label = { Text(atlasText("Nom", "Last name", "اسم العائلة")) }, singleLine = true)
                OutlinedTextField(joined, { joined = it }, label = { Text(atlasText("Date de début (AAAA-MM-JJ)", "Start date (YYYY-MM-DD)", "تاريخ البدء")) }, singleLine = true)
                OutlinedTextField(pay, { pay = it }, label = { Text(atlasText("Montant par cycle", "Pay per cycle", "الأجر لكل دورة")) }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = cycle == "monthly", onClick = { cycle = "monthly" }, label = { Text(atlasText("Mensuel", "Monthly", "شهري")) })
                    FilterChip(selected = cycle == "days", onClick = { cycle = "days" }, label = { Text(atlasText("Jours", "Days", "أيام")) })
                }
                if (cycle == "days") OutlinedTextField(days, { days = it }, label = { Text(atlasText("Nombre de jours par cycle", "Days per cycle", "أيام الدورة")) }, singleLine = true)
                if (!error.isNullOrBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = !saving && first.isNotBlank() && pay.toDoubleOrNull()?.let { it >= 0 } == true && (cycle == "monthly" || (days.toIntOrNull() ?: 0) in 1..365), onClick = {
                val fullName = listOf(first.trim(), last.trim()).filter(String::isNotBlank).joinToString(" ")
                onSave(EmployeeUpdateRequest(firstName = first.trim(), lastName = last.trim(), name = fullName, phone = employee.phone, email = employee.email, roleTitle = employee.roleTitle, department = employee.department, category = employee.category, employmentStatus = employee.employmentStatus, joinedDate = joined.trim(), payAmount = pay.toDouble(), payCycleType = cycle, payCycleDays = if (cycle == "monthly") 30 else days.toInt()))
            }) { Text(if (saving) atlasText("Enregistrement…", "Saving…", "جار الحفظ…") else atlasText("Enregistrer", "Save", "حفظ")) }
        },
        dismissButton = { TextButton(onClick = onClose, enabled = !saving) { Text(atlasText("Annuler", "Cancel", "إلغاء")) } },
    )
}
