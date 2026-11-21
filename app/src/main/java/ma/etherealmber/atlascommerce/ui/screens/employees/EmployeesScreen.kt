package ma.etherealmber.atlascommerce.ui.screens.employees

import ma.etherealmber.atlascommerce.ui.components.rememberApiRefreshAction

import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoneyCompact
import ma.etherealmber.atlascommerce.ui.i18n.atlasShortDate
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.PayrollRowDto
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingDockPadding
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingTopBarPadding
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasFloatingTopBar
import ma.etherealmber.atlascommerce.ui.components.rememberAtlasFloatingTopBarScrollConnection
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasPayrollRowsSkeleton
import ma.etherealmber.atlascommerce.ui.components.AtlasPayrollSkeleton
import ma.etherealmber.atlascommerce.ui.components.AtlasPayrollSummarySkeleton
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.screens.home.HomeViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Admin-only employee directory and read-only payroll view backed by the server-authorized worker APIs.
 */
@Composable
fun EmployeesScreen(
    user: SessionUser,
    wideLayout: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val dockPadding = LocalAtlasFloatingDockPadding.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val directoryViewModel: EmployeeDirectoryViewModel = hiltViewModel()
    val directoryState by directoryViewModel.state.collectAsStateWithLifecycle()
    val refreshEmployees = rememberApiRefreshAction("employees-payroll", 30_000L, viewModel::refreshFinance)
    val isAdmin = Roles.isAdmin(user.role)
    val language = LocalAtlasLanguage.current
    val locale = when (language) { AppLanguage.FRENCH -> Locale.FRANCE; AppLanguage.ENGLISH -> Locale.UK; AppLanguage.ARABIC -> Locale.forLanguageTag("ar-MA") }
    var query by rememberSaveable(user.scopeIdentity) { mutableStateOf("") }
    var directoryQuery by rememberSaveable(user.scopeIdentity) { mutableStateOf("") }
    var editingEmployee by remember { mutableStateOf<ma.etherealmber.atlascommerce.data.model.EmployeeDto?>(null) }
    var confirmEmployeeDelete by remember { mutableStateOf(false) }
    var confirmEmployeeResign by remember { mutableStateOf(false) }
    var formerEmployeeReason by remember(user.scopeIdentity) { mutableStateOf("") }
    var paymentFilter by rememberSaveable(user.scopeIdentity) { mutableStateOf("Tous") }
    var payrollInfoOpen by remember { mutableStateOf(false) }

    val currentMonth = remember(state.serverCurrentFinancePeriod) {
        state.serverCurrentFinancePeriod?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
    }
    val selectedPeriod = state.selectedFinancePeriod ?: state.serverCurrentFinancePeriod
    val selectedMonth = remember(selectedPeriod) {
        selectedPeriod?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
    }
    val formattedMonth = remember(selectedMonth, locale) {
        selectedMonth?.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val monthLabel = formattedMonth
        ?: atlasText("Chargement du mois…", "Loading month…", "جارٍ تحميل الشهر…")
    val payroll = state.payroll
    val visibleEmployees = remember(directoryState.employees, directoryQuery) {
        val needle = directoryQuery.trim()
        directoryState.employees.filter { employee ->
            needle.isBlank() || listOf(employee.displayName, employee.name, employee.employeeNumber, employee.roleTitle, employee.department)
                .any { it.contains(needle, ignoreCase = true) }
        }
    }
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

    if ((state.financeLoading || state.loading) && state.finance == null && state.payroll == null) {
        AtlasPayrollSkeleton(
            modifier = Modifier.fillMaxSize(),
            payrollRows = visibleRows,
            hasPayrollSnapshot = payroll != null,
            hasCachedNotice = state.financeFromCache,
            showReturnToCurrentMonth = selectedMonth != null && currentMonth != null && selectedMonth != currentMonth,
            hasMoreRows = payroll?.hasMore == true,
            bottomPadding = 24.dp + dockPadding,
        )
        return
    }

    if (payrollInfoOpen) {
        AlertDialog(
            onDismissRequest = { payrollInfoOpen = false },
            title = { Text(atlasText("À propos des chiffres", "About these figures", "حول هذه الأرقام")) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        atlasText(
                            "Le nombre d'employés correspond aux fiches de paie, pas au nombre de personnes; la période de paie ou la date d'échéance d'une fiche peut différer du mois choisi. Le mois sélectionné filtre les fiches. Les totaux viennent du serveur et sont en lecture seule; les chiffres du mois en cours peuvent évoluer jusqu'à sa clôture. Les mois futurs ne sont pas disponibles.",
                            "The Employees count represents payroll records, not unique people; a record's pay period or due date may differ from the selected month. The selected month filters the records. Totals come from the server and are read-only; current-month figures can change until month-end. Future months aren't available.",
                            "يمثل عدد الموظفين سجلات الرواتب وليس عدد الأشخاص؛ وقد تختلف فترة الراتب أو تاريخ استحقاق السجل عن الشهر المحدد. يحدد الشهر المختار السجلات. تأتي الإجماليات من الخادم وهي للقراءة فقط؛ وقد تتغير أرقام الشهر الحالي حتى نهايته. الأشهر المستقبلية غير متاحة.",
                        ),
                    )
                    payroll?.takeIf { it.asOf.isNotBlank() }?.let { data ->
                        val timezoneSuffix = data.timezone.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()
                        Text(
                            atlasText(
                                "Les statuts sont évalués au ${atlasShortDate(data.asOf)}$timezoneSuffix.",
                                "Statuses are evaluated as of ${atlasShortDate(data.asOf)}$timezoneSuffix.",
                                "تُقيّم الحالات حتى ${atlasShortDate(data.asOf)}$timezoneSuffix.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { payrollInfoOpen = false }) {
                    Text(atlasText("Fermer", "Close", "إغلاق"))
                }
            },
        )
    }

    if (directoryState.detailsLoading && directoryState.selectedEmployee == null) {
        AlertDialog(
            onDismissRequest = directoryViewModel::clearDetails,
            title = { Text(atlasText("Chargement de l’employé…", "Loading employee…", "جارٍ تحميل الموظف…")) },
            text = { CircularProgressIndicator() },
            confirmButton = { TextButton(onClick = directoryViewModel::clearDetails) { Text(atlasText("Fermer", "Close", "إغلاق")) } },
        )
    }
    if (!directoryState.detailsLoading && directoryState.selectedEmployee == null && !directoryState.actionError.isNullOrBlank() && editingEmployee == null) {
        AlertDialog(
            onDismissRequest = directoryViewModel::clearDetails,
            title = { Text(atlasText("Détails indisponibles", "Details unavailable", "التفاصيل غير متاحة")) },
            text = { Text(atlasNotice(directoryState.actionError.orEmpty(), language)) },
            confirmButton = { TextButton(onClick = directoryViewModel::clearDetails) { Text(atlasText("Fermer", "Close", "إغلاق")) } },
        )
    }
    directoryState.selectedEmployee?.let { employee ->
        EmployeeDetailsDialog(
            employee = employee,
            loading = directoryState.detailsLoading,
            deleting = directoryState.deleting,
            saving = directoryState.saving,
            error = directoryState.actionError,
            onDismiss = directoryViewModel::clearDetails,
            onEdit = {
                editingEmployee = employee
                directoryViewModel.clearDetails()
            },
            onDelete = { confirmEmployeeDelete = true },
            onMarkFormer = { confirmEmployeeResign = true },
        )
    }
    editingEmployee?.let { employee ->
        EmployeeEditDialog(
            employee = employee,
            saving = directoryState.saving,
            error = directoryState.actionError,
            onDismiss = { editingEmployee = null },
            onSave = { request ->
                directoryViewModel.updateEmployee(employee.id, request) { saved ->
                    if (saved) editingEmployee = null
                }
            },
        )
    }
    if (confirmEmployeeResign) {
        val employee = directoryState.selectedEmployee
        AlertDialog(
            onDismissRequest = { if (!directoryState.saving) confirmEmployeeResign = false },
            title = { Text(atlasText("Marquer comme ancien employé ?", "Mark as former employee?", "تحديد الموظف كسابق؟")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(atlasText(
                        "Cette action arrête les obligations de paie futures. Elle ne verse pas de solde de départ; enregistrez tout paiement final séparément. Vous pourrez ensuite supprimer la fiche.",
                        "This stops future payroll obligations. It does not pay a final settlement; record any final payment separately. You can then delete the record.",
                        "يوقف هذا الإجراء التزامات الرواتب المستقبلية. لا يتم دفع مستحقات نهائية؛ سجّل أي دفعة نهائية بشكل منفصل. بعد ذلك يمكنك حذف الملف.",
                    ))
                    OutlinedTextField(
                        value = formerEmployeeReason,
                        onValueChange = { formerEmployeeReason = it },
                        label = { Text(atlasText("Motif de départ", "Reason for leaving", "سبب المغادرة")) },
                        placeholder = { Text(atlasText("Ex. fin de contrat", "For example, contract ended", "مثال: انتهاء العقد")) },
                        singleLine = true,
                        enabled = !directoryState.saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    directoryState.actionError?.takeIf(String::isNotBlank)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !directoryState.saving && employee?.active != 0,
                    onClick = {
                        employee?.let { target ->
                            directoryViewModel.markEmployeeFormer(target.id, formerEmployeeReason) { success ->
                                if (success) confirmEmployeeResign = false
                            }
                        }
                    },
                ) {
                    if (directoryState.saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text(atlasText("Confirmer", "Confirm", "تأكيد"))
                }
            },
            dismissButton = { TextButton(onClick = { confirmEmployeeResign = false }, enabled = !directoryState.saving) { Text(atlasText("Annuler", "Cancel", "إلغاء")) } },
        )
    }
    if (confirmEmployeeDelete) {
        val employee = directoryState.selectedEmployee
        AlertDialog(
            onDismissRequest = { if (!directoryState.deleting) confirmEmployeeDelete = false },
            title = { Text(atlasText("Supprimer l’employé ?", "Delete employee?", "حذف الموظف؟")) },
            text = {
                Text(atlasText(
                    "La fiche sera supprimée définitivement. Son historique de paiements est conservé. Seuls les employés ayant quitté l’entreprise peuvent être supprimés.",
                    "This permanently removes the employee record. Their payment history is retained. Only former employees can be deleted.",
                    "سيتم حذف ملف الموظف نهائياً مع الاحتفاظ بسجل المدفوعات. يمكن حذف الموظفين السابقين فقط.",
                ))
            },
            confirmButton = {
                TextButton(
                    enabled = !directoryState.deleting && employee?.active == 0,
                    onClick = {
                        employee?.let { directoryViewModel.deleteEmployee(it.id) }
                        confirmEmployeeDelete = false
                    },
                ) { Text(if (directoryState.deleting) atlasText("Suppression…", "Deleting…", "جارٍ الحذف…") else atlasText("Supprimer", "Delete", "حذف")) }
            },
            dismissButton = { TextButton(onClick = { confirmEmployeeDelete = false }) { Text(atlasText("Annuler", "Cancel", "إلغاء")) } },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp + LocalAtlasFloatingTopBarPadding.current, bottom = 24.dp + dockPadding),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                atlasText("Suivi de la paie"),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            IconButton(
                                onClick = { payrollInfoOpen = true },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Outlined.Info,
                                        contentDescription = atlasText("À propos des chiffres de paie", "About payroll figures", "حول أرقام الرواتب"),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            atlasText("Consultation seule · échéances et versements du serveur Atlas"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = refreshEmployees, enabled = !state.financeLoading) {
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
                        onClick = { selectedMonth?.minusMonths(1)?.toString()?.let(viewModel::selectFinancePeriod) },
                        enabled = selectedMonth != null && !state.financeLoading,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = atlasText("Mois précédent"))
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            monthLabel,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            when {
                                selectedMonth == null || currentMonth == null -> atlasText("Synchronisation…", "Syncing…", "جارٍ المزامنة…")
                                selectedMonth == currentMonth -> atlasText("Mois actuel · provisoire", "Current month · provisional", "الشهر الحالي · مؤقت")
                                else -> atlasText("Période de paie sélectionnée", "Selected payroll period", "فترة الرواتب المحددة")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = { selectedMonth?.plusMonths(1)?.toString()?.let(viewModel::selectFinancePeriod) },
                        enabled = !state.financeLoading && selectedMonth != null && currentMonth != null && selectedMonth < currentMonth,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = atlasText("Mois suivant"))
                    }
                }
                if (selectedMonth != null && currentMonth != null && selectedMonth != currentMonth) {
                    TextButton(
                        onClick = { currentMonth?.toString()?.let(viewModel::selectFinancePeriod) },
                        enabled = !state.financeLoading,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(atlasText("Revenir au mois en cours", "Return to current month", "العودة إلى الشهر الحالي"))
                    }
                }

            }
        }

        item(key = "employee_directory_header") {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(atlasText("Équipe", "Employee directory", "دليل الموظفين"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            atlasText("${directoryState.employees.size} fiche(s) · appuyez pour les détails et l’historique", "${directoryState.employees.size} records · tap for details and payment history", "${directoryState.employees.size} ملف · اضغط للتفاصيل وسجل المدفوعات"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = directoryViewModel::load, enabled = !directoryState.loading) {
                        if (directoryState.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser l’équipe", "Refresh employee directory", "تحديث دليل الموظفين"))
                    }
                }
                AtlasTextField(
                    value = directoryQuery,
                    onValueChange = { directoryQuery = it },
                    label = atlasText("Rechercher dans l’équipe", "Search employees", "ابحث عن موظف"),
                    placeholder = atlasText("Nom, numéro, poste ou service", "Name, number, role or department", "الاسم أو الرقم أو المنصب أو القسم"),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (directoryState.loading && directoryState.employees.isEmpty()) {
            item(key = "employee_directory_loading") {
                AtlasPayrollRowsSkeleton(rows = 2, wideLayout = wideLayout)
            }
        } else if (!directoryState.error.isNullOrBlank() && directoryState.employees.isEmpty()) {
            item(key = "employee_directory_error") {
                AtlasErrorCard(title = atlasText("Équipe indisponible", "Directory unavailable", "الدليل غير متاح"), message = atlasNotice(directoryState.error.orEmpty(), language), onRetry = directoryViewModel::load)
            }
        } else if (visibleEmployees.isEmpty()) {
            item(key = "employee_directory_empty") {
                AtlasEmptyState(
                    icon = Icons.Outlined.Groups,
                    title = atlasText("Aucun employé trouvé", "No employees found", "لم يتم العثور على موظفين"),
                    message = atlasText("Aucune fiche d’employé ne correspond à cette recherche.", "No employee record matches this search.", "لا يوجد ملف موظف يطابق هذا البحث."),
                )
            }
        } else {
            items(visibleEmployees, key = { "directory-${it.id}" }) { employee ->
                EmployeeDirectoryRow(employee = employee, onClick = { directoryViewModel.showDetails(employee.id) })
            }
        }

        if (state.financeFromCache) {
            item(key = "employees_cached_notice") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(
                            when {
                                state.financeOffline -> atlasText("Pas de réseau — données de paie enregistrées affichées.", "No internet connection — showing saved payroll data.", "لا يوجد اتصال بالإنترنت — يتم عرض بيانات الرواتب المحفوظة.")
                                state.financeLoading -> atlasText("Données enregistrées affichées pendant l'actualisation.", "Showing saved payroll while refreshing.", "يتم عرض بيانات الرواتب المحفوظة أثناء التحديث.")
                                else -> atlasText("Serveur indisponible — dernières données de paie enregistrées affichées.", "Server unavailable — showing the latest saved payroll data.", "الخادم غير متاح — يتم عرض أحدث بيانات الرواتب المحفوظة.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }


        state.financeError?.let { error ->
            item(key = "employees_error") {
                AtlasErrorCard(title = atlasText("Données indisponibles"), message = atlasNotice(error, language), onRetry = refreshEmployees)
            }
        }

        item(key = "employee_summary") {
            if (payroll == null && state.financeLoading) {
                AtlasPayrollSummarySkeleton()
            } else {
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
                AtlasPayrollRowsSkeleton(
                    rows = 4,
                    wideLayout = wideLayout,
                    includeHeader = wideLayout,
                )
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
            if (wideLayout) {
                item(key = "employee_table_header") { EmployeePayrollTableHeader() }
            }
            items(visibleRows, key = { "${it.employeeId}-${it.payPeriod.from}" }) { row ->
                EmployeePayrollCard(
                    row = row,
                    asOf = payroll?.asOf.orEmpty(),
                    currency = payroll?.currency ?: "MAD",
                    wideLayout = wideLayout,
                )
            }
        }

        if (payroll?.hasMore == true) {
            if (state.payrollLoadingMore) {
                item(key = "employee_more_loading") {
                    AtlasPayrollRowsSkeleton(rows = 2, wideLayout = wideLayout)
                }
            }
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
private fun EmployeePayrollTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(atlasText("Employé", "Employee", "الموظف"), Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(atlasText("Période", "Period", "الفترة"), Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(atlasText("Montant", "Amount", "المبلغ"), Modifier.weight(0.8f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(atlasText("Statut", "Status", "الحالة"), Modifier.weight(0.65f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmployeePayrollCard(row: PayrollRowDto, asOf: String, currency: String, wideLayout: Boolean) {
    val status = payrollStatus(row, asOf)
    val employeeName = row.displayName.ifBlank { atlasText("Employé #${row.employeeId}", "Employee #${row.employeeId}", "موظف رقم ${row.employeeId}") }
    val fromDate = atlasShortDate(row.payPeriod.from).takeIf { it != "—" } ?: "—"
    val toDate = atlasShortDate(row.payPeriod.to).takeIf { it != "—" } ?: "—"
    val periodLabel = atlasText("$fromDate → $toDate", "$fromDate → $toDate", "$fromDate → $toDate")
    val statusLabel = when (status) {
        "Payé" -> atlasText("Payé", "Paid", "مدفوع")
        "À venir" -> atlasText("À venir", "Upcoming", "قادم")
        "À payer" -> atlasText("À payer", "To pay", "مستحق الدفع")
        else -> status
    }
    val tone = when (status) {
        "Payé" -> AtlasTone.POSITIVE
        "À venir" -> AtlasTone.INFO
        else -> AtlasTone.WARNING
    }
    if (wideLayout) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1.5f)) {
                    Text(employeeName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("#${row.employeeId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1.2f)) {
                    Text(periodLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    row.dueDate?.takeIf(String::isNotBlank)?.let { dueDate ->
                        Text(
                            atlasText("Échéance ${atlasShortDate(dueDate)}", "Due ${atlasShortDate(dueDate)}", "الاستحقاق ${atlasShortDate(dueDate)}"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    row.lastPayment?.let { payment ->
                        Text(
                            atlasText("Dernier versement ${atlasMoney(payment.amount, currency)} · ${atlasShortDate(payment.paidAt)}", "Last payment ${atlasMoney(payment.amount, currency)} · ${atlasShortDate(payment.paidAt)}", "آخر دفعة ${atlasMoney(payment.amount, currency)} · ${atlasShortDate(payment.paidAt)}"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(atlasMoney(row.amountDue, currency), Modifier.weight(0.8f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.weight(0.65f), contentAlignment = Alignment.CenterEnd) {
                    AtlasChip(text = statusLabel, tone = tone)
                }
            }
        }
    } else {
        AtlasCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(employeeName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    val dueDateLabel = row.dueDate?.takeIf(String::isNotBlank)?.let {
                        atlasText(" · Échéance ${atlasShortDate(it)}", " · Due ${atlasShortDate(it)}", " · الاستحقاق ${atlasShortDate(it)}")
                    }.orEmpty()
                    Text(
                        text = atlasText("Période $periodLabel$dueDateLabel", "Period $periodLabel$dueDateLabel", "الفترة $periodLabel$dueDateLabel"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(atlasMoney(row.amountDue, currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    AtlasChip(text = statusLabel, tone = tone)
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
}

@Composable
private fun EmployeeDirectoryRow(employee: ma.etherealmber.atlascommerce.data.model.EmployeeDto, onClick: () -> Unit) {
    val displayName = employee.displayName.ifBlank { employee.name.ifBlank { "#${employee.id}" } }
    AtlasCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val detail = listOf(employee.employeeNumber, employee.roleTitle, employee.department)
                    .filter(String::isNotBlank)
                    .joinToString(" · ")
                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (employee.email.isNotBlank() || employee.phone.isNotBlank()) {
                    Text(listOf(employee.email, employee.phone).filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AtlasChip(
                text = if (employee.active == 0) atlasText("Ancien", "Former", "سابق") else atlasText("Actif", "Active", "نشط"),
                tone = if (employee.active == 0) AtlasTone.NEUTRAL else AtlasTone.POSITIVE,
            )
        }
    }
}

@Composable
private fun EmployeeDetailsDialog(
    employee: ma.etherealmber.atlascommerce.data.model.EmployeeDto,
    loading: Boolean,
    deleting: Boolean,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkFormer: () -> Unit,
) {
    val displayName = employee.displayName.ifBlank { employee.name.ifBlank { "#${employee.id}" } }
    var headerExpanded by remember(employee.id) { mutableStateOf(true) }
    val scrollConnection = ma.etherealmber.atlascommerce.ui.components.rememberAtlasFloatingTopBarScrollConnection(
        expanded = headerExpanded,
        onExpandedChange = { headerExpanded = it },
    )
    Dialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().nestedScroll(scrollConnection)) {
                AtlasFloatingTopBar(
                    title = displayName,
                    subtitle = atlasText("Détails et historique de paie", "Details and payroll history", "التفاصيل وسجل الرواتب"),
                    onBack = onDismiss,
                    expanded = headerExpanded,
                    actions = {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Outlined.Edit, contentDescription = atlasText("Modifier l’employé", "Edit employee", "تعديل الموظف"))
                        }
                        if (employee.active == 0) {
                            IconButton(onClick = onDelete, enabled = !deleting) {
                                if (deleting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Outlined.Delete, contentDescription = atlasText("Supprimer l’employé", "Delete employee", "حذف الموظف"), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "employee_profile") {
                        AtlasCard {
                            listOfNotNull(
                                employee.employeeNumber.takeIf(String::isNotBlank)?.let { "${atlasText("Numéro", "Number", "الرقم")}: $it" },
                                employee.roleTitle.takeIf(String::isNotBlank)?.let { "${atlasText("Poste", "Role", "المنصب")}: $it" },
                                employee.department.takeIf(String::isNotBlank)?.let { "${atlasText("Service", "Department", "القسم")}: $it" },
                                employee.email.takeIf(String::isNotBlank)?.let { "Email: $it" },
                                employee.phone.takeIf(String::isNotBlank)?.let { "${atlasText("Téléphone", "Phone", "الهاتف")}: $it" },
                                employee.joinedDate.takeIf(String::isNotBlank)?.let { "${atlasText("Date d’entrée", "Joined", "تاريخ الانضمام")}: ${atlasShortDate(it)}" },
                                "${atlasText("Salaire prévu", "Pay rate", "الأجر")}: ${atlasMoney(employee.payAmount, "MAD")}",
                                atlasText("Statut : ${if (employee.active == 0) "Ancien" else "Actif"}", "Status: ${if (employee.active == 0) "Former" else "Active"}", "الحالة: ${if (employee.active == 0) "سابق" else "نشط"}"),
                            ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            if (employee.active != 0) {
                                AtlasDivider(Modifier.padding(vertical = 4.dp))
                                Text(
                                    atlasText("Marquez d’abord l’employé comme ancien. Cela n’enregistre aucun paiement final.", "Mark the employee as former first. This does not record a final payment.", "حدّد الموظف كسابق أولاً. لا يسجّل هذا أي دفعة نهائية."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(onClick = onMarkFormer, enabled = !saving) {
                                    if (saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    else Text(atlasText("Marquer comme ancien", "Mark as former", "تحديد كسابق"))
                                }
                            }
                            if (!error.isNullOrBlank()) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    item(key = "employee_payments_title") {
                        Text(atlasText("Historique des paiements", "Payment history", "سجل المدفوعات"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    if (loading) {
                        item(key = "employee_payments_loading") { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) }
                    } else if (employee.payments.isEmpty()) {
                        item(key = "employee_payments_empty") {
                            AtlasEmptyState(
                                icon = Icons.Outlined.Groups,
                                title = atlasText("Aucun paiement enregistré", "No payments recorded", "لا توجد مدفوعات مسجلة"),
                                message = atlasText("Aucun historique de paiement n’est disponible pour cet employé.", "No payment history is available for this employee.", "لا يتوفر سجل مدفوعات لهذا الموظف."),
                            )
                        }
                    } else {
                        items(employee.payments, key = { "employee-payment-${it.id}" }) { payment ->
                            val paymentDate = if (payment.paidAt.isNotBlank()) atlasShortDate(payment.paidAt) else null
                            AtlasCard {
                                Text(atlasMoney(payment.amount, "MAD"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    listOf(payment.periodLabel, paymentDate, payment.note)
                                        .filterNotNull().filter(String::isNotBlank).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeEditDialog(
    employee: ma.etherealmber.atlascommerce.data.model.EmployeeDto,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (ma.etherealmber.atlascommerce.data.model.EmployeeUpdateRequest) -> Unit,
) {
    var firstName by remember(employee.id) { mutableStateOf(employee.firstName.ifBlank { employee.name }) }
    var lastName by remember(employee.id) { mutableStateOf(employee.lastName) }
    var phone by remember(employee.id) { mutableStateOf(employee.phone) }
    var email by remember(employee.id) { mutableStateOf(employee.email) }
    var roleTitle by remember(employee.id) { mutableStateOf(employee.roleTitle) }
    var department by remember(employee.id) { mutableStateOf(employee.department) }
    var category by remember(employee.id) { mutableStateOf(employee.category) }
    var employmentStatus by remember(employee.id) { mutableStateOf(employee.employmentStatus) }
    var joinedDate by remember(employee.id) { mutableStateOf(employee.joinedDate) }
    var payAmount by remember(employee.id) { mutableStateOf(employee.payAmount.toString()) }
    val parsedPay = payAmount.toDoubleOrNull()
    val fullName = listOf(firstName.trim(), lastName.trim()).filter(String::isNotBlank).joinToString(" ")
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(atlasText("Modifier l’employé", "Edit employee", "تعديل الموظف")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(firstName, { firstName = it }, label = { Text(atlasText("Prénom", "First name", "الاسم الأول")) }, singleLine = true)
                OutlinedTextField(lastName, { lastName = it }, label = { Text(atlasText("Nom", "Last name", "اسم العائلة")) }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text(atlasText("Téléphone", "Phone", "الهاتف")) }, singleLine = true)
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true)
                OutlinedTextField(roleTitle, { roleTitle = it }, label = { Text(atlasText("Poste", "Role", "المنصب")) }, singleLine = true)
                OutlinedTextField(department, { department = it }, label = { Text(atlasText("Service", "Department", "القسم")) }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text(atlasText("Catégorie", "Category", "الفئة")) }, singleLine = true)
                OutlinedTextField(employmentStatus, { employmentStatus = it }, label = { Text(atlasText("Statut professionnel", "Employment status", "حالة العمل")) }, singleLine = true)
                OutlinedTextField(joinedDate, { joinedDate = it }, label = { Text(atlasText("Date d’entrée (AAAA-MM-JJ)", "Join date (YYYY-MM-DD)", "تاريخ الانضمام (YYYY-MM-DD)")) }, singleLine = true)
                OutlinedTextField(payAmount, { payAmount = it }, label = { Text(atlasText("Salaire prévu (MAD)", "Pay rate (MAD)", "الأجر (درهم)")) }, singleLine = true)
                if (!error.isNullOrBlank()) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && fullName.isNotBlank() && parsedPay != null && parsedPay >= 0,
                onClick = {
                    val amount = parsedPay ?: return@TextButton
                    onSave(ma.etherealmber.atlascommerce.data.model.EmployeeUpdateRequest(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        name = fullName,
                        phone = phone.trim(),
                        email = email.trim(),
                        roleTitle = roleTitle.trim(),
                        department = department.trim(),
                        category = category.trim(),
                        employmentStatus = employmentStatus.trim(),
                        joinedDate = joinedDate.trim(),
                        payAmount = amount,
                    ))
                },
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text(atlasText("Enregistrer", "Save", "حفظ"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text(atlasText("Annuler", "Cancel", "إلغاء")) } },
    )
}

private fun payrollStatus(row: PayrollRowDto, asOf: String): String = when {
    row.status.equals("paid", ignoreCase = true) && row.dueDate?.let { it > asOf } == true -> "À venir"
    row.status.equals("paid", ignoreCase = true) -> "Payé"
    else -> "À payer"
}
