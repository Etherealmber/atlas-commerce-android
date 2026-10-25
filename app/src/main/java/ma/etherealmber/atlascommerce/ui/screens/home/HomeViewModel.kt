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
import ma.etherealmber.atlascommerce.data.model.CompanySettingsDto
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
        val settings: CompanySettingsDto? = null,
        val currency: String = "MAD",
        val finance: FinanceMonthlyDto? = null,
        val selectedFinancePeriod: String? = null,
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

    fun refresh() {
        load(initial = false)
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
            val settingsJob = async { commerceRepository.settings() }
            val financeJob = if (isAdmin) async { commerceRepository.financeMonthly(requestedFinancePeriod) } else null
            val dashboardResult = dashboardJob.await()
            val settingsResult = settingsJob.await()
            val financeResult = financeJob?.await()
            val finance = financeResult?.successOrNull()
            val payrollResult = if (finance != null && finance.period.isNotBlank()) {
                commerceRepository.employeePayroll(period = finance.period, pageSize = 20)
            } else {
                null
            }
            val payroll = payrollResult?.successOrNull()

            val failure = dashboardResult.failureOrNull() ?: settingsResult.failureOrNull()
            val financeFailure = financeResult?.failureOrNull() ?: payrollResult?.failureOrNull()
            val dashboard = dashboardResult.successOrNull()
            val settings = settingsResult.successOrNull()

            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope
            if (!stillSameScope) return@launch
            _state.update { current ->
                val financeIsCurrent = current.selectedFinancePeriod == requestedFinancePeriod
                current.copy(
                    loading = false,
                    refreshing = false,
                    dashboard = dashboard ?: current.dashboard,
                    settings = settings ?: current.settings,
                    currency = (if (financeIsCurrent) finance?.currency?.takeIf { it.isNotBlank() } else null)
                        ?: dashboard?.activeCurrency
                        ?: settings?.currency?.takeIf { it.isNotBlank() }
                        ?: current.currency,
                    selectedFinancePeriod = when {
                        !isAdmin -> null
                        financeIsCurrent -> finance?.period ?: requestedFinancePeriod
                        else -> current.selectedFinancePeriod
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

            if (dashboardResult is ApiResult.Success) {
                // Keep the stored profile in sync when it changed on the web app.
                sessionManager.refreshProfile()
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
