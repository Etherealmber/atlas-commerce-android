package ma.etherealmber.atlascommerce.ui.screens.employees

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.EmployeeDto
import ma.etherealmber.atlascommerce.data.model.EmployeeUpdateRequest
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
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
fun EmployeeDirectoryScreen(user: SessionUser, viewModel: EmployeeDirectoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = LocalAtlasLanguage.current
    val canEdit = Roles.isAdmin(user.role) || Roles.normalize(user.role) == Roles.SECRETARY
    var query by rememberSaveable(user.scopeIdentity) { mutableStateOf("") }
    var showFormer by rememberSaveable(user.scopeIdentity) { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<EmployeeDto?>(null) }
    val visible = state.employees.filter { employee ->
        (showFormer == (employee.active == 0)) &&
            (query.isBlank() || listOf(employee.displayName, employee.employeeNumber, employee.roleTitle, employee.department, employee.phone)
                .any { it.contains(query.trim(), ignoreCase = true) })
    }
    if (!canEdit) {
        AtlasErrorCard(title = atlasText("Accès réservé", "Restricted access", "الوصول مقيد"), message = atlasText("Le répertoire des employés est réservé aux administrateurs et secrétaires."))
        return
    }
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
                        else Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser"))
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
        items(visible, key = { "employee-${it.id}" }) { employee ->
            val nextPayment = employee.nextDue?.takeIf(String::isNotBlank)?.let { atlasShortDate(it) }
            AtlasCard(onClick = { viewModel.showDetails(employee.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(employee.displayName.ifBlank { employee.name }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(listOf(employee.employeeNumber, employee.roleTitle, employee.department).filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (employee.active == 0) atlasText("Ancien employé", "Former employee", "موظف سابق")
                            else atlasText("Prochain paiement : ${nextPayment ?: "—"} · ${atlasMoney(employee.payAmount, "MAD")}", "Next payment: ${nextPayment ?: "—"} · ${atlasMoney(employee.payAmount, "MAD")}", "الدفعة التالية: ${nextPayment ?: "—"} · ${atlasMoney(employee.payAmount, "MAD")}"),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (employee.payOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AtlasChip(text = if (employee.active == 0) atlasText("Ancien", "Former", "سابق") else atlasText("Actif", "Active", "نشط"), tone = if (employee.active == 0) AtlasTone.NEUTRAL else AtlasTone.POSITIVE)
                }
            }
        }
    }
    if (state.detailsLoading && state.selectedEmployee == null) AlertDialog(
        onDismissRequest = viewModel::clearDetails,
        title = { Text(atlasText("Chargement…", "Loading…", "جار التحميل…")) },
        text = { CircularProgressIndicator() }, confirmButton = { TextButton(onClick = viewModel::clearDetails) { Text(atlasText("Fermer", "Close", "إغلاق")) } },
    )
    state.selectedEmployee?.let { employee ->
        EmployeeProfileDialog(employee = employee, canEdit = canEdit, canMarkFormer = Roles.isAdmin(user.role), onClose = viewModel::clearDetails, onEdit = { editTarget = employee; viewModel.clearDetails() }, onMarkFormer = { viewModel.markEmployeeFormer(employee.id, "Départ") })
    }
    editTarget?.let { employee ->
        EmployeeProfileEditDialog(employee, saving = state.saving, error = state.actionError, onClose = { editTarget = null }) { request ->
            viewModel.updateEmployee(employee.id, request) { ok -> if (ok) editTarget = null }
        }
    }
}

@Composable
private fun EmployeeProfileDialog(employee: EmployeeDto, canEdit: Boolean, canMarkFormer: Boolean, onClose: () -> Unit, onEdit: () -> Unit, onMarkFormer: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, contentDescription = atlasText("Retour", "Back", "رجوع")) }
                        Column(Modifier.weight(1f)) {
                            Text(employee.displayName.ifBlank { employee.name }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(atlasText("Profil et paiements", "Profile and payments", "الملف والمدفوعات"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (canEdit) IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = atlasText("Modifier", "Edit", "تعديل")) }
                    }
                }
                item {
                    AtlasCard {
                        listOfNotNull(
                            employee.joinedDate.takeIf(String::isNotBlank)?.let { "${atlasText("Date d’entrée", "Start date", "تاريخ البدء")}: ${atlasShortDate(it)}" },
                            employee.lastPaidAt?.takeIf(String::isNotBlank)?.let { "${atlasText("Dernier paiement", "Last payment", "آخر دفعة")}: ${atlasShortDate(it)}" },
                            "${atlasText("Cycle de paie", "Pay cycle", "دورة الدفع")}: ${if (employee.payCycleType == "monthly") atlasText("Mensuel (30 jours)", "Monthly (30 days)", "شهري (30 يوماً)") else atlasText("Tous les ${employee.payCycleDays} jours", "Every ${employee.payCycleDays} days", "كل ${employee.payCycleDays} يوم")}",
                            "${atlasText("Montant journalier", "Daily pay", "الأجر اليومي")}: ${atlasMoney(employee.dailyRate, "MAD")}",
                            if (employee.active == 0) atlasText("Statut : ancien employé", "Status: former employee", "الحالة: موظف سابق") else "${atlasText("Prochain paiement", "Next payment", "الدفعة التالية")}: ${employee.nextDue?.let { atlasShortDate(it) } ?: "—"}",
                        ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        if (canMarkFormer && employee.active != 0) {
                            TextButton(onClick = onMarkFormer) { Text(atlasText("Marquer comme ancien", "Mark as former", "تحديد كسابق")) }
                        }
                    }
                }
                item { Text(atlasText("Historique des paiements", "Payment history", "سجل المدفوعات"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                if (employee.payments.isEmpty()) item { AtlasEmptyState(icon = Icons.Outlined.History, title = atlasText("Aucun paiement enregistré", "No payments recorded", "لا توجد مدفوعات مسجلة"), message = atlasText("L’historique apparaîtra ici.", "Payment history will appear here.", "سيظهر سجل المدفوعات هنا.")) }
                items(employee.payments, key = { "payment-${it.id}" }) { payment ->
                    AtlasCard {
                        Text(atlasMoney(payment.amount, "MAD"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(listOf(payment.periodLabel, payment.paidAt.takeIf(String::isNotBlank)?.let { atlasShortDate(it) }, payment.note).filterNotNull().filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
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
