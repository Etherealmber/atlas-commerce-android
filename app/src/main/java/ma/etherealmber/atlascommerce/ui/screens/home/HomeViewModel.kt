package ma.etherealmber.atlascommerce.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ApiRunner
import ma.etherealmber.atlascommerce.core.net.failureOrNull
import ma.etherealmber.atlascommerce.core.net.successOrNull
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.model.DashboardDto
import ma.etherealmber.atlascommerce.data.model.FinanceMonthlyDto
import ma.etherealmber.atlascommerce.data.model.PayrollPageDto
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val commerceRepository: CommerceRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val refreshing: Boolean = false,
        val dashboard: DashboardDto? = null,
        val currency: String = "MAD",
        val finance: FinanceMonthlyDto? = null,
        val selectedFinancePeriod: String? = null,
        val serverCurrentFinancePeriod: String? = null,
        val payroll: PayrollPageDto? = null,
        val financeLoading: Boolean = false,
        val financeError: String? = null,
        val payrollLoadingMore: Boolean = false,
        val reportGenerating: Boolean = false,
        val error: String? = null,
        val offline: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        // Reload whenever the signed-in account changes so one device can be
        // shared between staff without leaking the previous user's figures.
        viewModelScope.launch {
            sessionManager.state
                .map { (it as? SessionState.Active)?.user?.scopeIdentity }
                .distinctUntilChanged()
                .collect { identity ->
                    _state.value = UiState()
                    if (identity != null) load(initial = true)
                }
        }
    }

    /** Refresh only the dashboard endpoint; finance has its own refresh action. */
    fun refresh() {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        val scopeIdentity = activeUser.scopeIdentity
        _state.update { it.copy(loading = it.dashboard == null, refreshing = true, error = null, offline = false) }
        viewModelScope.launch {
            val result = commerceRepository.dashboard()
            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == scopeIdentity
            if (!stillSameScope) return@launch
            val dashboard = result.successOrNull()
            val failure = result.failureOrNull()
            _state.update { current ->
                current.copy(
                    loading = false,
                    refreshing = false,
                    dashboard = dashboard ?: current.dashboard,
                    currency = dashboard?.activeCurrency ?: current.currency,
                    error = if (dashboard == null && failure != null && failure.code != ApiRunner.CODE_NETWORK) failure.message else null,
                    offline = failure?.code == ApiRunner.CODE_NETWORK,
                )
            }
        }
    }

    /** Refresh only the finance/payroll data shared by the Home summary and Employees page. */
    fun refreshFinance() {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(activeUser.role)) return
        val requestedScope = activeUser.scopeIdentity
        val requestedPeriod = _state.value.selectedFinancePeriod
        _state.update { it.copy(financeLoading = true, financeError = null) }
        viewModelScope.launch {
            val currentFinanceResult = commerceRepository.financeMonthly()
            val currentFinance = currentFinanceResult.successOrNull()
            val serverPeriod = currentFinance?.period
                ?.takeIf { it.matches(Regex("\\d{4}-(0[1-9]|1[0-2])")) }
            val targetPeriod = requestedPeriod ?: serverPeriod
            val financeResult = when {
                targetPeriod == null || targetPeriod == serverPeriod -> currentFinanceResult
                else -> commerceRepository.financeMonthly(targetPeriod)
            }
            val finance = financeResult.successOrNull()
            val payrollResult = finance?.period?.takeIf { it.isNotBlank() }?.let { period ->
                commerceRepository.employeePayroll(period = period, pageSize = 20)
            }
            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope
            val selectionStillCurrent = _state.value.selectedFinancePeriod == requestedPeriod
            if (!stillSameScope || !selectionStillCurrent) return@launch

            val payroll = payrollResult?.successOrNull()
            val failure = financeResult.failureOrNull() ?: payrollResult?.failureOrNull()
            _state.update { current ->
                current.copy(
                    financeLoading = false,
                    serverCurrentFinancePeriod = serverPeriod ?: current.serverCurrentFinancePeriod,
                    selectedFinancePeriod = if (requestedPeriod == null) null else finance?.period ?: requestedPeriod,
                    finance = finance ?: current.finance,
                    payroll = payroll ?: current.payroll,
                    currency = finance?.currency?.takeIf { it.isNotBlank() } ?: current.currency,
                    financeError = failure?.let {
                        if (it.code == "FIN_FORBIDDEN") "Réservé aux administrateurs (owner/admin)." else it.message
                    },
                )
            }
        }
    }

    fun selectFinancePeriod(period: String) {
        if (!period.matches(Regex("\\d{4}-(0[1-9]|1[0-2])"))) return
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(activeUser.role)) return
        val current = _state.value
        if (current.selectedFinancePeriod == period && current.finance?.period == period) return

        val requestedScope = activeUser.scopeIdentity
        _state.update {
            it.copy(
                selectedFinancePeriod = period,
                finance = null,
                payroll = null,
                financeLoading = true,
                financeError = null,
                payrollLoadingMore = false,
            )
        }
        viewModelScope.launch {
            val financeResult = commerceRepository.financeMonthly(period)
            val finance = financeResult.successOrNull()
            val payrollResult = if (finance != null && finance.period.isNotBlank()) {
                commerceRepository.employeePayroll(period = finance.period, pageSize = 20)
            } else {
                null
            }
            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope
            if (!stillSameScope || _state.value.selectedFinancePeriod != period) return@launch

            val failure = financeResult.failureOrNull() ?: payrollResult?.failureOrNull()
            _state.update { state ->
                state.copy(
                    selectedFinancePeriod = finance?.period ?: period,
                    finance = finance,
                    payroll = payrollResult?.successOrNull(),
                    currency = finance?.currency?.takeIf { it.isNotBlank() } ?: state.currency,
                    financeLoading = false,
                    financeError = failure?.let {
                        if (it.code == "FIN_FORBIDDEN") "Réservé aux administrateurs (owner/admin)." else it.message
                    },
                )
            }
        }
    }

    fun loadMorePayroll() {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(activeUser.role)) return
        val current = _state.value.payroll ?: return
        if (!current.hasMore || _state.value.payrollLoadingMore) return
        val scopeIdentity = activeUser.scopeIdentity
        viewModelScope.launch {
            _state.update { it.copy(payrollLoadingMore = true, financeError = null) }
            val result = commerceRepository.employeePayroll(
                period = current.period,
                page = current.page + 1,
                pageSize = current.pageSize,
            )
            val stillSameUser = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == scopeIdentity
            if (!stillSameUser) return@launch
            _state.update { state ->
                val nextPage = result.successOrNull()
                state.copy(
                    payrollLoadingMore = false,
                    payroll = if (nextPage != null) {
                        nextPage.copy(rows = state.payroll?.rows.orEmpty() + nextPage.rows)
                    } else {
                        state.payroll
                    },
                    financeError = result.failureOrNull()?.let { failure ->
                        if (failure.code == "FIN_FORBIDDEN") "Réservé aux administrateurs (owner/admin)." else failure.message
                    },
                )
            }
        }
    }

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return@launch
            val requestedScope = activeUser.scopeIdentity
            val isAdmin = Roles.isAdmin(activeUser.role)
            val requestedFinancePeriod = _state.value.selectedFinancePeriod
            val knownServerCurrentPeriod = _state.value.serverCurrentFinancePeriod
            _state.update {
                it.copy(
                    loading = initial && it.dashboard == null,
                    refreshing = !initial,
                    financeLoading = isAdmin,
                    financeError = null,
                    error = null,
                )
            }

            val dashboardJob = async { commerceRepository.dashboard() }
            val serverCurrentFinanceJob = if (isAdmin) async { commerceRepository.financeMonthly() } else null
            val financeJob = when {
                !isAdmin -> null
                requestedFinancePeriod == null || requestedFinancePeriod == knownServerCurrentPeriod -> serverCurrentFinanceJob
                else -> async { commerceRepository.financeMonthly(requestedFinancePeriod) }
            }
            val dashboardResult = dashboardJob.await()
            val serverCurrentFinance = serverCurrentFinanceJob?.await()?.successOrNull()
            val financeResult = financeJob?.await()
            val finance = financeResult?.successOrNull()
            val payrollResult = if (finance != null && finance.period.isNotBlank()) {
                commerceRepository.employeePayroll(period = finance.period, pageSize = 20)
            } else {
                null
            }
            val payroll = payrollResult?.successOrNull()

            val failure = dashboardResult.failureOrNull()
            val financeFailure = financeResult?.failureOrNull() ?: payrollResult?.failureOrNull()
            val dashboard = dashboardResult.successOrNull()

            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope
            if (!stillSameScope) return@launch
            _state.update { current ->
                val financeIsCurrent = current.selectedFinancePeriod == requestedFinancePeriod
                current.copy(
                    loading = false,
                    refreshing = false,
                    dashboard = dashboard ?: current.dashboard,
                    currency = (if (financeIsCurrent) finance?.currency?.takeIf { it.isNotBlank() } else null)
                        ?: dashboard?.activeCurrency
                        ?: current.currency,
                    selectedFinancePeriod = when {
                        !isAdmin -> null
                        financeIsCurrent -> finance?.period ?: requestedFinancePeriod
                        else -> current.selectedFinancePeriod
                    },
                    serverCurrentFinancePeriod = if (!isAdmin) null else {
                        serverCurrentFinance?.period
                            ?.takeIf { it.matches(Regex("\\d{4}-(0[1-9]|1[0-2])")) }
                            ?: current.serverCurrentFinancePeriod
                    },
                    finance = when {
                        !isAdmin -> null
                        financeIsCurrent -> finance
                        else -> current.finance
                    },
                    payroll = when {
                        !isAdmin -> null
                        financeIsCurrent -> payroll
                        else -> current.payroll
                    },
                    financeLoading = if (!isAdmin || financeIsCurrent) false else current.financeLoading,
                    financeError = if (!isAdmin) null else if (financeIsCurrent) financeFailure?.let { failed ->
                        if (failed.code == "FIN_FORBIDDEN") {
                            "Réservé aux administrateurs (owner/admin)."
                        } else {
                            failed.message
                        }
                    } else current.financeError,
                    error = if (dashboard == null && failure != null && failure.code != ApiRunner.CODE_NETWORK) {
                        failure.message
                    } else {
                        null
                    },
                    offline = failure?.code == ApiRunner.CODE_NETWORK,
                )
            }

        }
    }

    fun downloadFinanceReport(cacheDirectory: File, period: String, onResult: (ApiResult<File>) -> Unit) {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user
        if (activeUser == null || !Roles.isAdmin(activeUser.role)) {
            onResult(ApiResult.Failure("FIN_FORBIDDEN", "Réservé aux administrateurs (owner/admin)."))
            return
        }
        val requestedScope = activeUser.scopeIdentity
        if (!period.matches(Regex("\\d{4}-(0[1-9]|1[0-2])"))) {
            onResult(ApiResult.Failure("BAD_PERIOD", "Période invalide — format attendu AAAA-MM."))
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(reportGenerating = true) }
            val directory = File(
                File(cacheDirectory, "finance"),
                scopeCacheKey(requestedScope),
            ).apply { mkdirs() }
            ma.etherealmber.atlascommerce.data.FinanceReportCache.pruneAccount(directory)
            val destination = File(directory, "rapport-mensuel-$period.pdf")
            val result = commerceRepository.downloadFinanceReport(period, destination)
            _state.update { it.copy(reportGenerating = false) }
            val currentScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity
            if (currentScope != requestedScope) {
                (result as? ApiResult.Success)?.data?.delete()
                onResult(ApiResult.Failure("SESSION_CHANGED", "Le compte actif a changé. Relancez le téléchargement avec le bon compte."))
            } else {
                onResult(result)
            }
        }
    }

    fun createCombinedReportUrl(
        period: String,
        from: String?,
        to: String?,
        sections: List<String>,
        onResult: (ApiResult<String>) -> Unit,
    ) {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user
        if (activeUser == null || !Roles.isAdmin(activeUser.role)) {
            onResult(ApiResult.Failure("FIN_FORBIDDEN", "Réservé aux administrateurs (owner/admin)."))
            return
        }
        val requestedScope = activeUser.scopeIdentity
        viewModelScope.launch {
            _state.update { it.copy(reportGenerating = true) }
            val result = commerceRepository.createCombinedReportUrl(period, from, to, sections)
            _state.update { it.copy(reportGenerating = false) }
            val currentScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity
            if (currentScope != requestedScope) {
                onResult(ApiResult.Failure("SESSION_CHANGED", "Le compte actif a changé. Relancez l'ouverture avec le bon compte."))
            } else {
                onResult(result)
            }
        }
    }

    private fun scopeCacheKey(scopeIdentity: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(scopeIdentity.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
