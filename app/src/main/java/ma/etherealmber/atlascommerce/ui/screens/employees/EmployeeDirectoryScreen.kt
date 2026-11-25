package ma.etherealmber.atlascommerce.ui.screens.employees

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.style.TextOverflow
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
import ma.etherealmber.atlascommerce.ui.components.AtlasFinanceBarChart
import ma.etherealmber.atlascommerce.ui.components.AtlasFinanceBarEntry
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasPaginationControls
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingTopBarPadding
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingDockPadding
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
    var page by rememberSaveable(user.scopeIdentity) { mutableStateOf(1) }
    var editTarget by remember { mutableStateOf<EmployeeDto?>(null) }
    var resignTarget by remember { mutableStateOf<EmployeeDto?>(null) }
    var resignReason by rememberSaveable { mutableStateOf("") }
    var payTarget by remember { mutableStateOf<EmployeeDto?>(null) }
    var advanceTarget by remember { mutableStateOf<EmployeeDto?>(null) }
    var situationTarget by remember { mutableStateOf<EmployeeDto?>(null) }
    var payrollActionNote by rememberSaveable { mutableStateOf("") }
    var detailEmployeeId by remember { mutableStateOf<Long?>(null) }
    val employee = state.selectedEmployee
    val detailActive = detailEmployeeId != null || employee != null || state.detailsLoading
    LaunchedEffect(detailActive) { onDetailsVisibilityChanged(detailActive) }
    androidx.activity.compose.BackHandler(enabled = detailActive) {
        if (!state.saving) {
            detailEmployeeId = null
            viewModel.clearDetails()
            onDetailsVisibilityChanged(false)
        }
    }
    val canAdminPayroll = Roles.isAdmin(user.role)
    val visible = state.employees.filter { row ->
        (showFormer == (row.active == 0)) &&
            (query.isBlank() || listOf(row.displayName, row.employeeNumber, row.roleTitle, row.department, row.phone)
                .any { it.contains(query.trim(), ignoreCase = true) })
    }
    val pageCount = ((visible.size + 9) / 10).coerceAtLeast(1)
    LaunchedEffect(pageCount) { if (page > pageCount) page = pageCount }
    val pageEmployees = visible.drop((page - 1) * 10).take(10)
    if (!canEdit) {
        AtlasErrorCard(title = atlasText("Accès réservé", "Restricted access", "الوصول مقيد"), message = atlasText("Le répertoire des employés est réservé aux administrateurs et secrétaires."))
        return
    }
    AnimatedContent(
        targetState = detailActive,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally(tween(280)) { it } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(tween(220)) { -it / 6 } + fadeOut(tween(150)))
            } else {
                (slideInHorizontally(tween(240)) { -it / 6 } + fadeIn(tween(170))) togetherWith
                    (slideOutHorizontally(tween(260)) { it } + fadeOut(tween(150)))
            }
        },
        label = "employee-directory-history-transition",
    ) { showingDetails ->
    if (showingDetails) {
        EmployeeProfilePage(
            employee = employee,
            loading = state.detailsLoading,
            error = state.actionError,
            saving = state.saving,
            canEdit = canEdit,
            canMarkFormer = Roles.isAdmin(user.role),
            onClose = {
                detailEmployeeId = null
                viewModel.clearDetails()
                onDetailsVisibilityChanged(false)
            },
            onRefresh = { detailEmployeeId?.let(viewModel::showDetails) },
            onEdit = { employee?.let { editTarget = it } },
            onMarkFormer = {
                resignTarget = employee
                resignReason = ""
            },
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp + LocalAtlasFloatingTopBarPadding.current, bottom = 28.dp + LocalAtlasFloatingDockPadding.current),
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
                        FilterChip(selected = !showFormer, onClick = { showFormer = false; page = 1 }, label = { Text(atlasText("En poste", "Current", "الحاليون")) })
                        FilterChip(selected = showFormer, onClick = { showFormer = true; page = 1 }, label = { Text(atlasText("Anciens", "Former", "السابقون")) })
                    }
                    AtlasTextField(value = query, onValueChange = { query = it; page = 1 }, label = atlasText("Rechercher", "Search", "بحث"), placeholder = atlasText("Nom, poste, numéro", "Name, role or number", "الاسم أو المنصب أو الرقم"), modifier = Modifier.fillMaxWidth())
                }
            }
            if (!state.error.isNullOrBlank() && state.employees.isEmpty()) item {
                AtlasErrorCard(title = atlasText("Équipe indisponible", "Directory unavailable", "الدليل غير متاح"), message = state.error.orEmpty(), onRetry = viewModel::load)
            }
            if (!state.loading && visible.isEmpty()) item {
                AtlasEmptyState(icon = Icons.Outlined.Groups, title = atlasText("Aucun employé", "No employees", "لا يوجد موظفون"), message = atlasText("Aucune fiche ne correspond à cette vue.", "No employee records match this view.", "لا توجد ملفات مطابقة لهذا العرض."))
            }
            items(pageEmployees, key = { "employee-${it.id}" }) { row ->
                EmployeeDirectoryCard(
                    employee = row,
                    canPay = canAdminPayroll,
                    canMarkFormer = canAdminPayroll,
                    onEdit = { editTarget = row },
                    onDetails = {
                        detailEmployeeId = row.id
                        onDetailsVisibilityChanged(true)
                        viewModel.showDetails(row.id)
                    },
                    onSituation = {
                        situationTarget = row
                        viewModel.loadSituation(row.id)
                    },
                    onPay = {
                        payTarget = row
                        payrollActionNote = ""
                    },
                    onAdvance = {
                        advanceTarget = row
                        payrollActionNote = ""
                    },
                    onMarkFormer = {
                        resignTarget = row
                        resignReason = ""
                    },
                )
            }
            if (visible.size > 10) item(key = "employees-pagination") {
                AtlasPaginationControls(
                    totalItems = visible.size,
                    currentPage = page.coerceAtMost(pageCount),
                    onPageSelected = { page = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
                    pageSize = 10,
                )
            }
        }
    }
    }
    editTarget?.let { target ->
        EmployeeProfileEditDialog(target, saving = state.saving, error = state.actionError, onClose = { editTarget = null }) { request ->
            viewModel.updateEmployee(target.id, request) { ok -> if (ok) editTarget = null }
        }
    }
    situationTarget?.let { target ->
        val detailed = state.situationEmployee?.takeIf { it.id == target.id }
        EmployeeSituationDialog(
            employee = detailed?.copy(
                suggestedPay = target.suggestedPay,
                grossAccrued = target.grossAccrued,
                canAdvance = target.canAdvance,
                maxAdvance = target.maxAdvance,
                advancesInPeriod = target.advancesInPeriod,
                advanceNeedsProof = target.advanceNeedsProof,
            ) ?: target,
            loading = state.situationLoading,
            error = state.actionError,
            onDismiss = {
                situationTarget = null
                viewModel.clearSituation()
            },
        )
    }
    payTarget?.let { target ->
        EmployeePayrollActionDialog(
            employee = target,
            advance = false,
            saving = state.saving,
            error = state.actionError,
            note = payrollActionNote,
            onNoteChange = { payrollActionNote = it },
            onDismiss = { if (!state.saving) payTarget = null },
            onConfirm = { note ->
                viewModel.recordEmployeePay(target.id, note) { success ->
                    if (success) {
                        payTarget = null
                        payrollActionNote = ""
                    }
                }
            },
        )
    }
    advanceTarget?.let { target ->
        EmployeePayrollActionDialog(
            employee = target,
            advance = true,
            saving = state.saving,
            error = state.actionError,
            note = payrollActionNote,
            onNoteChange = { payrollActionNote = it },
            onDismiss = { if (!state.saving) advanceTarget = null },
            onConfirm = { note ->
                viewModel.recordEmployeeAdvance(target.id, note) { success ->
                    if (success) {
                        advanceTarget = null
                        payrollActionNote = ""
                    }
                }
            },
        )
    }
    resignTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!state.saving) resignTarget = null },
            title = { Text(atlasText("Démissionner", "Mark as former", "تحديد كسابق")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(atlasText(
                        "Cette action arrête les obligations de paie futures. Elle n’enregistre aucun paiement final.",
                        "This stops future payroll obligations. It does not record a final payment.",
                        "يوقف هذا الإجراء التزامات الرواتب المستقبلية ولا يسجل دفعة نهائية.",
                    ))
                    OutlinedTextField(
                        value = resignReason,
                        onValueChange = { resignReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(atlasText("Motif de départ", "Reason for leaving", "سبب المغادرة")) },
                        singleLine = true,
                        enabled = !state.saving,
                    )
                    state.actionError?.takeIf(String::isNotBlank)?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.markEmployeeFormer(target.id, resignReason) { success ->
                            if (success) {
                                resignTarget = null
                                resignReason = ""
                            }
                        }
                    },
                    enabled = !state.saving,
                ) {
                    if (state.saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text(atlasText("Confirmer", "Confirm", "تأكيد"))
                }
            },
            dismissButton = {
                TextButton(onClick = { resignTarget = null }, enabled = !state.saving) {
                    Text(atlasText("Annuler", "Cancel", "إلغاء"))
                }
            },
        )
    }
}

@Composable
private fun EmployeeDirectoryCard(
    employee: EmployeeDto,
    canPay: Boolean,
    canMarkFormer: Boolean,
    onEdit: () -> Unit,
    onDetails: () -> Unit,
    onSituation: () -> Unit,
    onPay: () -> Unit,
    onAdvance: () -> Unit,
    onMarkFormer: () -> Unit,
) {
    val displayName = employee.displayName.ifBlank { employee.name.ifBlank { "#${employee.id}" } }
    val initials = displayName.trim().split(Regex("\\s+")).filter(String::isNotBlank).take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    val subtitle = listOf(employee.roleTitle, employee.employeeNumber).filter(String::isNotBlank).joinToString(" · ")
    val cycle = if (employee.payCycleType == "monthly") {
        atlasText("Mensuel", "Monthly", "شهري")
    } else {
        atlasText("Tous les ${employee.payCycleDays} jours", "Every ${employee.payCycleDays} days", "كل ${employee.payCycleDays} يوم")
    }
    var actionsExpanded by rememberSaveable(employee.id) { mutableStateOf(false) }

    AtlasCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(initials.ifBlank { "?" }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle.ifBlank { atlasText("Poste non renseigné", "Role not specified", "المنصب غير محدد") },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AtlasChip(
                text = if (employee.active == 0) atlasText("Ancien", "Former", "سابق") else atlasText("Actif", "Active", "نشط"),
                tone = if (employee.active == 0) AtlasTone.NEUTRAL else AtlasTone.POSITIVE,
            )
        }
        AtlasDivider(Modifier.padding(vertical = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmployeeMetric(
                label = atlasText("Salaire", "Salary", "الراتب"),
                value = atlasMoney(employee.payAmount, "MAD"),
                modifier = Modifier.weight(1f),
            )
            EmployeeMetric(label = atlasText("Cycle", "Cycle", "الدورة"), value = cycle, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmployeeMetric(
                label = atlasText("Dernier paiement", "Last payment", "آخر دفعة"),
                value = employee.lastPaidAt?.takeIf(String::isNotBlank)?.let { atlasShortDate(it) }
                    ?: atlasText("Jamais", "Never", "أبداً"),
                modifier = Modifier.weight(1f),
            )
            EmployeeMetric(
                label = atlasText("Prochaine échéance", "Next due", "الاستحقاق التالي"),
                value = employee.nextDue?.takeIf(String::isNotBlank)?.let { atlasShortDate(it) } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }
        AtlasDivider(Modifier.padding(vertical = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(atlasText("Modifier", "Edit", "تعديل"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = onDetails, modifier = Modifier.weight(1.5f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(atlasText("Détails & historique", "Details & history", "التفاصيل والسجل"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        AtlasDivider(Modifier.padding(vertical = 8.dp))
        TextButton(onClick = { actionsExpanded = !actionsExpanded }, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Text(
                atlasText(
                    if (actionsExpanded) "▾ Actions de la fiche" else "▸ Actions de la fiche",
                    if (actionsExpanded) "▾ Record actions" else "▸ Record actions",
                    if (actionsExpanded) "▾ إجراءات الملف" else "▸ إجراءات الملف",
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (actionsExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onDetails, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text(atlasText("Fiche", "Profile", "الملف"), maxLines = 1)
                }
                OutlinedButton(onClick = onSituation, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text(atlasText("Situation", "Situation", "الوضعية"), maxLines = 1)
                }
                if (canPay && employee.active != 0 && employee.payAmount > 0) {
                    OutlinedButton(onClick = onPay, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text(atlasText("Marquer payé", "Mark paid", "تسجيل الدفع"), maxLines = 1)
                    }
                }
                if (canPay && employee.active != 0 && employee.canAdvance) {
                    OutlinedButton(onClick = onAdvance, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text(atlasText("Acompte", "Advance", "تسبيق"), maxLines = 1)
                    }
                }
                if (canMarkFormer && employee.active != 0) {
                    OutlinedButton(onClick = onMarkFormer, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Text(atlasText("Démissionner", "Mark former", "تحديد كسابق"), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Text(
                            listOf(employee.employeeNumber, employee.roleTitle, employee.department).filter(String::isNotBlank).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
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
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val left = listOf(
                                atlasText("Contrat", "Contract", "العقد") to employee.contractType,
                                atlasText("Heures par semaine", "Weekly hours", "ساعات أسبوعياً") to employee.weeklyHours?.let { atlasText("$it h", "$it h", "$it س") }.orEmpty(),
                                atlasText("Date d’entrée", "Start date", "تاريخ البدء") to employee.joinedDate.takeIf(String::isNotBlank)?.let { atlasShortDate(it) }.orEmpty(),
                                atlasText("Salaire du cycle", "Cycle salary", "راتب الدورة") to atlasMoney(employee.payAmount, "MAD"),
                                atlasText("Cycle de paie", "Pay cycle", "دورة الدفع") to if (employee.payCycleType == "monthly") atlasText("Mensuel (30 jours)", "Monthly (30 days)", "شهري (30 يوماً)") else atlasText("Tous les ${employee.payCycleDays} jours", "Every ${employee.payCycleDays} days", "كل ${employee.payCycleDays} يوم"),
                                atlasText("Salaire journalier", "Daily pay", "الأجر اليومي") to atlasMoney(employee.dailyRate, "MAD"),
                                atlasText("Prochain paiement", "Next payment", "الدفعة التالية") to if (employee.active == 0) atlasText("Ancien employé", "Former employee", "موظف سابق") else employee.nextDue?.let { atlasShortDate(it) }.orEmpty(),
                                atlasText("Dernier paiement", "Last payment", "آخر دفعة") to employee.lastPaidAt?.let { atlasShortDate(it) }.orEmpty(),
                            )
                            val right = listOf(
                                atlasText("Téléphone", "Phone", "الهاتف") to employee.phone,
                                "Email" to employee.email,
                                atlasText("Nationalité", "Nationality", "الجنسية") to employee.nationality,
                                atlasText("Date de naissance", "Date of birth", "تاريخ الميلاد") to employee.dateOfBirth.takeIf(String::isNotBlank)?.let { atlasShortDate(it) }.orEmpty(),
                                atlasText("CIN / passeport", "National ID / passport", "رقم البطاقة / جواز السفر") to employee.nationalId,
                                atlasText("Identifiant fiscal", "Tax ID", "المعرف الضريبي") to employee.taxId,
                                atlasText("CNSS / assurance sociale", "CNSS / social insurance", "الضمان الاجتماعي") to employee.socialInsurance,
                                atlasText("Assurance maladie", "Health insurance", "التأمين الصحي") to employee.healthInsurance,
                                "RIB" to employee.rib,
                            )
                            if (maxWidth >= 600.dp) {
                                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    Column(Modifier.weight(1f)) { left.forEach { (label, value) -> EmployeeProfileInfo(label, value) } }
                                    Column(Modifier.weight(1f)) { right.forEach { (label, value) -> EmployeeProfileInfo(label, value) } }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    (left + right).forEach { (label, value) -> EmployeeProfileInfo(label, value) }
                                }
                            }
                        }
                        if (employee.grossAccrued != null || employee.suggestedPay != null || employee.advancesInPeriod > 0) {
                            AtlasDivider(Modifier.padding(vertical = 8.dp))
                            Text(atlasText("Situation du cycle", "Current cycle", "وضع الدورة"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            employee.grossAccrued?.let { EmployeeProfileInfo(atlasText("Accumulation brute", "Gross accrued", "المستحق الإجمالي"), atlasMoney(it, "MAD")) }
                            employee.suggestedPay?.let { EmployeeProfileInfo(atlasText("Reste suggéré à payer", "Suggested amount due", "المبلغ المقترح للدفع"), atlasMoney(it, "MAD")) }
                            if (employee.advancesInPeriod > 0) EmployeeProfileInfo(atlasText("Acomptes du cycle", "Advances this cycle", "التسبيقات في الدورة"), atlasMoney(employee.advancesInPeriod, "MAD"))
                        }
                        if (canMarkFormer && employee.active != 0) {
                            TextButton(onClick = onMarkFormer, enabled = !saving) {
                                if (saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                else Text(atlasText("Démissionner", "Mark as former", "تحديد كسابق"))
                            }
                        }
                    }
                }
                if (employee.payments.isNotEmpty()) item {
                    AtlasFinanceBarChart(
                        title = atlasText("Paiements récents", "Recent payments", "الدفعات الأخيرة"),
                        entries = employee.payments.take(6).reversed().mapIndexed { index, payment ->
                            AtlasFinanceBarEntry(
                                label = payment.periodLabel.ifBlank { payment.paidAt.takeIf(String::isNotBlank)?.let { atlasShortDate(it) } ?: "#${index + 1}" },
                                amount = payment.amount,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        currency = "MAD",
                    )
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
private fun EmployeeProfileInfo(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value.takeIf(String::isNotBlank) ?: atlasText("Non renseigné", "Not provided", "غير متوفر"),
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmployeeSituationDialog(employee: EmployeeDto, loading: Boolean, error: String?, onDismiss: () -> Unit) {
    val name = employee.displayName.ifBlank { employee.name }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(atlasText("Situation de paie", "Payroll situation", "وضعية الأجور")) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(atlasText("Salaire du cycle : ${atlasMoney(employee.payAmount, "MAD")} · ${if (employee.payCycleType == "monthly") "mensuel" else "${employee.payCycleDays} jours"}", "Cycle salary: ${atlasMoney(employee.payAmount, "MAD")} · ${if (employee.payCycleType == "monthly") "monthly" else "${employee.payCycleDays} days"}", "راتب الدورة: ${atlasMoney(employee.payAmount, "MAD")}"), style = MaterialTheme.typography.bodySmall)
                employee.suggestedPay?.let { Text(atlasText("Montant suggéré à payer : ${atlasMoney(it, "MAD")}", "Suggested amount to pay: ${atlasMoney(it, "MAD")}", "المبلغ المقترح للدفع: ${atlasMoney(it, "MAD")}"), fontWeight = FontWeight.SemiBold) }
                if (employee.advancesInPeriod > 0) Text(atlasText("Acomptes du cycle : ${atlasMoney(employee.advancesInPeriod, "MAD")}", "Advances this cycle: ${atlasMoney(employee.advancesInPeriod, "MAD")}", "التسبيقات في الدورة: ${atlasMoney(employee.advancesInPeriod, "MAD")}"))
                Text(atlasText("Historique récent", "Recent history", "السجل الأخير"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                if (!error.isNullOrBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (!loading && employee.payments.isEmpty()) Text(atlasText("Aucun paiement enregistré.", "No payments recorded.", "لا توجد دفعات مسجلة."), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                employee.payments.take(6).forEach { payment ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(payment.periodLabel.ifBlank { atlasShortDate(payment.paidAt) }, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (payment.note.isNotBlank()) Text(payment.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text(atlasMoney(payment.amount, "MAD"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(atlasText("Fermer", "Close", "إغلاق")) } },
    )
}

@Composable
private fun EmployeePayrollActionDialog(
    employee: EmployeeDto,
    advance: Boolean,
    saving: Boolean,
    error: String?,
    note: String,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val name = employee.displayName.ifBlank { employee.name }
    val requiresProof = advance && employee.advanceNeedsProof
    val canConfirm = !saving && (!requiresProof || note.trim().length >= 8)
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (advance) atlasText("Enregistrer un acompte", "Record an advance", "تسجيل تسبيق") else atlasText("Marquer le salaire payé", "Record salary payment", "تسجيل دفع الراتب")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (advance) {
                    Text(atlasText("Plafond possible : ${atlasMoney(employee.maxAdvance, "MAD")} · déjà avancé : ${atlasMoney(employee.advancesInPeriod, "MAD")}", "Available advance cap: ${atlasMoney(employee.maxAdvance, "MAD")} · already advanced: ${atlasMoney(employee.advancesInPeriod, "MAD")}", "الحد المتاح للتسبيق: ${atlasMoney(employee.maxAdvance, "MAD")}"))
                    Text(atlasText("Le montant proposé est calculé par le serveur selon le cycle et les règles de paie.", "The server calculates the proposed amount from the cycle and payroll rules.", "يحسب الخادم المبلغ المقترح وفق الدورة وقواعد الأجور."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(atlasText("Le paiement enregistrera le montant restant du cycle, après déduction des acomptes éventuels.", "This records the remaining cycle amount after any advances.", "يسجل هذا المبلغ المتبقي من الدورة بعد خصم أي تسبيقات."), style = MaterialTheme.typography.bodySmall)
                    employee.suggestedPay?.let { Text(atlasText("Montant suggéré : ${atlasMoney(it, "MAD")}", "Suggested amount: ${atlasMoney(it, "MAD")}", "المبلغ المقترح: ${atlasMoney(it, "MAD")}"), fontWeight = FontWeight.SemiBold) }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (requiresProof) atlasText("Note / justificatif (8 caractères min.)", "Note / proof (at least 8 characters)", "ملاحظة / إثبات (8 أحرف على الأقل)") else atlasText("Note (facultative)", "Note (optional)", "ملاحظة (اختيارية)")) },
                    enabled = !saving,
                    minLines = 1,
                    maxLines = 3,
                )
                if (!error.isNullOrBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note) }, enabled = canConfirm) {
                if (saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text(if (advance) atlasText("Enregistrer l’acompte", "Record advance", "تسجيل التسبيق") else atlasText("Confirmer le paiement", "Confirm payment", "تأكيد الدفع"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text(atlasText("Annuler", "Cancel", "إلغاء")) } },
    )
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
