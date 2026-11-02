package ma.etherealmber.atlascommerce.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import ma.etherealmber.atlascommerce.core.net.hasDeviceInternetCapability
import ma.etherealmber.atlascommerce.core.net.failureOrNull
import ma.etherealmber.atlascommerce.core.net.successOrNull
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.OfflineCache
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
    private val offlineCache: OfflineCache,
    @ApplicationContext private val applicationContext: Context,
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
        val financeFromCache: Boolean = false,
        val financeOffline: Boolean = false,
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
            val deviceOnline = hasDeviceInternetCapability(applicationContext)
            _state.update { current ->
                current.copy(
                    loading = false,
                    refreshing = false,
                    dashboard = dashboard ?: current.dashboard,
                    currency = dashboard?.activeCurrency ?: current.currency,
                    error = if (dashboard == null && failure != null &&
                        (failure.code != ApiRunner.CODE_NETWORK || deviceOnline)
                    ) failure.message else null,
                    offline = failure?.code == ApiRunner.CODE_NETWORK && !deviceOnline,
                )
            }
        }
    }

    /** Refresh only the finance/payroll data shared by the Home summary and Employees page. */
    fun refreshFinance() {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(activeUser.role)) return
        val requestedScope = activeUser.scopeIdentity
        val cacheScope = offlineCache.scopeFor(activeUser.serverUrl, activeUser.username)
        val previous = _state.value
        val requestedPeriod = previous.selectedFinancePeriod
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
            val payroll = payrollResult?.successOrNull()
            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope
            val selectionStillCurrent = _state.value.selectedFinancePeriod == requestedPeriod
            if (!stillSameScope || !selectionStillCurrent) return@launch

            if (finance != null && payroll != null) {
                offlineCache.saveFinanceBundle(
                    finance,
                    payroll,
                    isLatest = serverPeriod == finance.period,
                    scope = cacheScope,
                )
            }
            if ((sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != requestedScope ||
                _state.value.selectedFinancePeriod != requestedPeriod
            ) return@launch

            val failure = financeResult.failureOrNull() ?: payrollResult?.failureOrNull()
            val hasFallback = previous.finance != null || previous.payroll != null
            val deviceOffline = !hasDeviceInternetCapability(applicationContext)
            _state.update { current ->
                current.copy(
                    financeLoading = false,
                    serverCurrentFinancePeriod = serverPeriod ?: current.serverCurrentFinancePeriod,
                    selectedFinancePeriod = if (requestedPeriod == null) null else finance?.period ?: requestedPeriod,
                    finance = finance ?: current.finance,
                    payroll = payroll ?: current.payroll,
                    currency = finance?.currency?.takeIf { it.isNotBlank() } ?: current.currency,
                    financeError = failure?.let {
                        if (it.code == "FIN_FORBIDDEN") "Réservé aux administrateurs (owner/admin)."
                        else if ((it.code == ApiRunner.CODE_NETWORK || it.httpStatus?.let { status -> status in 500..599 } == true) && hasFallback) null
                        else it.message
                    },
                    financeFromCache = if (finance != null && payroll != null) false else current.financeFromCache ||
                        ((failure?.code == ApiRunner.CODE_NETWORK || failure?.httpStatus?.let { it in 500..599 } == true) && hasFallback),
                    financeOffline = failure?.code == ApiRunner.CODE_NETWORK && deviceOffline && hasFallback,
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
        val cacheScope = offlineCache.scopeFor(activeUser.serverUrl, activeUser.username)
        _state.update {
            it.copy(
                selectedFinancePeriod = period,
                finance = null,
                payroll = null,
                financeLoading = true,
                financeError = null,
                financeFromCache = false,
                financeOffline = false,
                payrollLoadingMore = false,
            )
        }
        viewModelScope.launch {
            val cached = offlineCache.readFinanceBundle(period, scope = cacheScope)
            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope
            if (!stillSameScope || _state.value.selectedFinancePeriod != period) return@launch
            if (cached != null) {
                _state.update { state ->
                    state.copy(
                        finance = cached.finance,
                        payroll = cached.payroll,
                        currency = cached.finance.currency.ifBlank { state.currency },
                        financeFromCache = true,
                        financeOffline = !hasDeviceInternetCapability(applicationContext),
                    )
                }
            }

            val financeResult = commerceRepository.financeMonthly(period)
            val finance = financeResult.successOrNull()
            val payrollResult = if (finance != null && finance.period.isNotBlank()) {
                commerceRepository.employeePayroll(period = finance.period, pageSize = 20)
            } else null
            val payroll = payrollResult?.successOrNull()
            val sameRequest = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope &&
                _state.value.selectedFinancePeriod == period
            if (!sameRequest) return@launch

            if (finance != null && payroll != null) {
                offlineCache.saveFinanceBundle(finance, payroll, isLatest = false, scope = cacheScope)
            }
            if ((sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != requestedScope ||
                _state.value.selectedFinancePeriod != period
            ) return@launch

            val failure = financeResult.failureOrNull() ?: payrollResult?.failureOrNull()
            val fallbackFinance = finance ?: cached?.finance
            val fallbackPayroll = payroll ?: cached?.payroll
            val hasFallback = fallbackFinance != null || fallbackPayroll != null
            val deviceOffline = !hasDeviceInternetCapability(applicationContext)
            _state.update { state ->
                state.copy(
                    selectedFinancePeriod = fallbackFinance?.period ?: period,
                    finance = fallbackFinance,
                    payroll = fallbackPayroll,
                    currency = fallbackFinance?.currency?.takeIf { it.isNotBlank() } ?: state.currency,
                    financeLoading = false,
                    financeError = failure?.let {
                        if (it.code == "FIN_FORBIDDEN") "Réservé aux administrateurs (owner/admin)."
                        else if ((it.code == ApiRunner.CODE_NETWORK || it.httpStatus?.let { status -> status in 500..599 } == true) && hasFallback) null
                        else it.message
                    },
                    financeFromCache = if (finance != null && payroll != null) false else cached != null,
                    financeOffline = failure?.code == ApiRunner.CODE_NETWORK && deviceOffline && hasFallback,
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
        val cacheScope = offlineCache.scopeFor(activeUser.serverUrl, activeUser.username)
        viewModelScope.launch {
            _state.update { it.copy(payrollLoadingMore = true, financeError = null) }
            val result = commerceRepository.employeePayroll(
                period = current.period,
                page = current.page + 1,
                pageSize = current.pageSize,
            )
            val stillSameUser = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == scopeIdentity
            if (!stillSameUser) return@launch
            val displayedPayroll = _state.value.payroll
            if (displayedPayroll == null || displayedPayroll.period != current.period) return@launch
            val nextPage = result.successOrNull()
            val mergedPayroll = nextPage?.copy(rows = displayedPayroll.rows + nextPage.rows)
            val failure = result.failureOrNull()
            val hasFallback = _state.value.payroll != null
            val deviceOnline = hasDeviceInternetCapability(applicationContext)
            _state.update { state ->
                state.copy(
                    payrollLoadingMore = false,
                    payroll = mergedPayroll ?: state.payroll,
                    financeError = failure?.let {
                        if (it.code == "FIN_FORBIDDEN") "Réservé aux administrateurs (owner/admin)."
                        else if ((it.code == ApiRunner.CODE_NETWORK || it.httpStatus?.let { status -> status in 500..599 } == true) && hasFallback) null
                        else it.message
                    },
                    financeFromCache = if (nextPage != null) false else state.financeFromCache ||
                        ((failure?.code == ApiRunner.CODE_NETWORK || failure?.httpStatus?.let { it in 500..599 } == true) && hasFallback),
                    financeOffline = failure?.code == ApiRunner.CODE_NETWORK && !deviceOnline && hasFallback,
                )
            }
            val finance = _state.value.finance
            if (finance != null && mergedPayroll != null) {
                offlineCache.saveFinanceBundle(
                    finance,
                    mergedPayroll,
                    isLatest = _state.value.serverCurrentFinancePeriod == finance.period,
                    scope = cacheScope,
                )
            }
        }
    }

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return@launch
            val requestedScope = activeUser.scopeIdentity
            val cacheScope = offlineCache.scopeFor(activeUser.serverUrl, activeUser.username)
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

            // Render the encrypted, account-scoped snapshot first. Network requests continue
            // afterwards and replace it when newer server data arrives.
            val cachedBundle = if (isAdmin) offlineCache.readFinanceBundle(requestedFinancePeriod, scope = cacheScope) else null
            if (cachedBundle != null &&
                (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope
            ) {
                _state.update { current ->
                    current.copy(
                        finance = cachedBundle.finance,
                        payroll = cachedBundle.payroll,
                        serverCurrentFinancePeriod = current.serverCurrentFinancePeriod ?: cachedBundle.finance.period,
                        currency = cachedBundle.finance.currency.ifBlank { current.currency },
                        financeFromCache = true,
                        financeOffline = !hasDeviceInternetCapability(applicationContext),
                    )
                }
            }

            val dashboardJob = async { commerceRepository.dashboard() }
            val serverCurrentFinanceJob = if (isAdmin) async { commerceRepository.financeMonthly() } else null
            val financeJob = when {
                !isAdmin -> null
                requestedFinancePeriod == null || requestedFinancePeriod == knownServerCurrentPeriod -> serverCurrentFinanceJob
                else -> async { commerceRepository.financeMonthly(requestedFinancePeriod) }
            }
            val dashboardResult = dashboardJob.await()
            val serverCurrentFinanceResult = serverCurrentFinanceJob?.await()
            val serverCurrentFinance = serverCurrentFinanceResult?.successOrNull()
            val financeResult = financeJob?.await()
            val finance = financeResult?.successOrNull()
            val payrollResult = if (finance != null && finance.period.isNotBlank()) {
                commerceRepository.employeePayroll(period = finance.period, pageSize = 20)
            } else null
            val payroll = payrollResult?.successOrNull()

            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope
            if (!stillSameScope) return@launch
            val financeForCache = finance ?: cachedBundle?.finance
            val cachedForFinancePeriod = cachedBundle?.takeIf { bundle ->
                financeForCache == null || bundle.finance.period == financeForCache.period
            }
            if (finance != null && payroll != null) {
                offlineCache.saveFinanceBundle(
                    finance,
                    payroll,
                    isLatest = serverCurrentFinance?.period == finance.period,
                    scope = cacheScope,
                )
            }
            if ((sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != requestedScope) return@launch

            val failure = dashboardResult.failureOrNull()
            val financeFailure = financeResult?.failureOrNull() ?: payrollResult?.failureOrNull()
            val dashboard = dashboardResult.successOrNull()
            val financeFallback = finance ?: cachedForFinancePeriod?.finance
            val payrollFallback = payroll ?: cachedForFinancePeriod?.payroll
            val hasFinanceFallback = financeFallback != null || payrollFallback != null
            val deviceOnline = hasDeviceInternetCapability(applicationContext)

            _state.update { current ->
                val financeIsCurrent = current.selectedFinancePeriod == requestedFinancePeriod
                val displayedFinance = when {
                    !isAdmin -> null
                    financeIsCurrent -> financeFallback ?: current.finance
                    else -> current.finance
                }
                val displayedPayroll = when {
                    !isAdmin -> null
                    financeIsCurrent -> payrollFallback ?: current.payroll?.takeIf { it.period == displayedFinance?.period }
                    else -> current.payroll?.takeIf { it.period == current.finance?.period }
                }
                val financeStillCached = financeIsCurrent && (finance == null || payroll == null) &&
                    (cachedForFinancePeriod != null || current.financeFromCache)
                val networkFailureWithFallback = financeFailure?.code == ApiRunner.CODE_NETWORK && hasFinanceFallback
                current.copy(
                    loading = false,
                    refreshing = false,
                    dashboard = dashboard ?: current.dashboard,
                    currency = (if (financeIsCurrent) displayedFinance?.currency?.takeIf { it.isNotBlank() } else null)
                        ?: dashboard?.activeCurrency
                        ?: current.currency,
                    selectedFinancePeriod = when {
                        !isAdmin -> null
                        financeIsCurrent -> finance?.period ?: cachedForFinancePeriod?.finance?.period ?: requestedFinancePeriod
                        else -> current.selectedFinancePeriod
                    },
                    serverCurrentFinancePeriod = if (!isAdmin) null else {
                        serverCurrentFinance?.period
                            ?.takeIf { it.matches(Regex("\\d{4}-(0[1-9]|1[0-2])")) }
                            ?: current.serverCurrentFinancePeriod
                    },
                    finance = displayedFinance,
                    payroll = displayedPayroll,
                    financeLoading = if (!isAdmin || financeIsCurrent) false else current.financeLoading,
                    financeError = if (!isAdmin) null else if (financeIsCurrent) financeFailure?.let { failed ->
                        when {
                            failed.code == "FIN_FORBIDDEN" -> "Réservé aux administrateurs (owner/admin)."
                            (failed.code == ApiRunner.CODE_NETWORK || failed.httpStatus?.let { it in 500..599 } == true) &&
                                (hasFinanceFallback || current.finance != null || current.payroll != null) -> null
                            else -> failed.message
                        }
                    } else current.financeError,
                    financeFromCache = if (!isAdmin) false else if (finance != null && payroll != null) false else financeStillCached,
                    financeOffline = financeIsCurrent && networkFailureWithFallback && !deviceOnline,
                    error = if (dashboard == null && failure != null &&
                        (failure.code != ApiRunner.CODE_NETWORK || deviceOnline)
                    ) failure.message else null,
                    offline = failure?.code == ApiRunner.CODE_NETWORK && !deviceOnline,
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
