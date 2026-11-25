package ma.etherealmber.atlascommerce.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import ma.etherealmber.atlascommerce.data.model.DailyMetricsDto
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
        val dailyMetrics: DailyMetricsDto? = null,
        val metricsLoading: Boolean = true,
        val metricsError: String? = null,
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

    /** Refresh the three home data sources together; no request is made per card. */
    fun refresh() {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        val scopeIdentity = activeUser.scopeIdentity
        val cacheKey = homeCacheKey(activeUser.scopeIdentity, activeUser.role)
        val selectedPeriod = _state.value.selectedFinancePeriod
        val isAdmin = Roles.isAdmin(activeUser.role)
        _state.update { it.copy(
            loading = it.dashboard == null,
            refreshing = true,
            financeLoading = isAdmin,
            error = null,
            offline = false,
        ) }
        viewModelScope.launch {
            val dashboardJob = async { commerceRepository.dashboard(cacheKey, forceRefresh = true) }
            val metricsJob = async { commerceRepository.dailyMetrics(cacheKey, forceRefresh = true) }
            val financeJob = if (isAdmin) async {
                commerceRepository.financeMonthly(selectedPeriod, cacheKey, forceRefresh = true)
            } else null
            val dashboardResult = dashboardJob.await()
            val metricsResult = metricsJob.await()
            val financeResult = financeJob?.await()
            val sameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == scopeIdentity
            if (!sameScope) return@launch
            val dashboard = dashboardResult.successOrNull()
            val metrics = metricsResult.successOrNull()
            val finance = financeResult?.successOrNull()
            val dashboardFailure = dashboardResult.failureOrNull()
            val financeFailure = financeResult?.failureOrNull()
            val deviceOnline = hasDeviceInternetCapability(applicationContext)
            _state.update { current -> current.copy(
                loading = false,
                refreshing = false,
                financeLoading = false,
                dashboard = dashboard ?: current.dashboard,
                dailyMetrics = metrics ?: current.dailyMetrics,
                metricsLoading = false,
                metricsError = metricsResult.failureOrNull()?.message?.takeIf { metrics == null && current.dailyMetrics == null },
                finance = finance ?: current.finance,
                serverCurrentFinancePeriod = finance?.period ?: current.serverCurrentFinancePeriod,
                currency = finance?.currency?.takeIf(String::isNotBlank) ?: dashboard?.activeCurrency ?: current.currency,
                financeError = financeFailure?.message?.takeIf { finance == null && current.finance == null },
                error = if (dashboard == null && dashboardFailure != null &&
                    (dashboardFailure.code != ApiRunner.CODE_NETWORK || deviceOnline)
                ) dashboardFailure.message else null,
                offline = dashboardFailure?.code == ApiRunner.CODE_NETWORK && !deviceOnline,
            ) }
        }
    }

    /** Refresh the selected monthly summary with one endpoint call. */
    fun refreshFinance() {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(activeUser.role)) return
        val requestedScope = activeUser.scopeIdentity
        val homeCacheKey = homeCacheKey(activeUser.scopeIdentity, activeUser.role)
        val cacheScope = offlineCache.scopeFor(activeUser.serverUrl, activeUser.username)
        val previous = _state.value
        val requestedPeriod = previous.selectedFinancePeriod
        _state.update { it.copy(financeLoading = true, financeError = null) }
        viewModelScope.launch {
            val financeResult = commerceRepository.financeMonthly(requestedPeriod, homeCacheKey, forceRefresh = true)
            val finance = financeResult.successOrNull()
            val payroll = previous.payroll?.takeIf { it.period == finance?.period }
            val sameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope
            if (!sameScope || _state.value.selectedFinancePeriod != requestedPeriod) return@launch
            if (finance != null && payroll != null) {
                offlineCache.saveFinanceBundle(finance, payroll, isLatest = requestedPeriod == null, scope = cacheScope)
            }
            val failure = financeResult.failureOrNull()
            val hasFallback = previous.finance != null
            val deviceOffline = !hasDeviceInternetCapability(applicationContext)
            _state.update { current -> current.copy(
                financeLoading = false,
                serverCurrentFinancePeriod = finance?.period ?: current.serverCurrentFinancePeriod,
                selectedFinancePeriod = if (requestedPeriod == null) null else finance?.period ?: requestedPeriod,
                finance = finance ?: current.finance,
                payroll = payroll ?: current.payroll,
                currency = finance?.currency?.takeIf(String::isNotBlank) ?: current.currency,
                financeError = failure?.let {
                    if (it.code == "FIN_FORBIDDEN") "Réservé aux administrateurs (owner/admin)."
                    else if ((it.code == ApiRunner.CODE_NETWORK || it.httpStatus?.let { status -> status in 500..599 } == true) && hasFallback) null
                    else it.message
                },
                financeFromCache = finance == null && current.financeFromCache,
                financeOffline = failure?.code == ApiRunner.CODE_NETWORK && deviceOffline && hasFallback,
            ) }
        }
    }

    fun selectFinancePeriod(period: String) {
        if (!period.matches(Regex("\\d{4}-(0[1-9]|1[0-2])"))) return
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(activeUser.role)) return
        val current = _state.value
        if (current.selectedFinancePeriod == period && current.finance?.period == period) return

        val requestedScope = activeUser.scopeIdentity
        val homeCacheKey = homeCacheKey(activeUser.scopeIdentity, activeUser.role)
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

            val financeResult = commerceRepository.financeMonthly(period, homeCacheKey, forceRefresh = true)
            val finance = financeResult.successOrNull()
            val payroll = _state.value.payroll?.takeIf { it.period == finance?.period }
            val sameRequest = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope &&
                _state.value.selectedFinancePeriod == period
            if (!sameRequest) return@launch

            if (finance != null && payroll != null) {
                offlineCache.saveFinanceBundle(finance, payroll, isLatest = false, scope = cacheScope)
            }
            if ((sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != requestedScope ||
                _state.value.selectedFinancePeriod != period
            ) return@launch

            val failure = financeResult.failureOrNull()
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

    /** Payroll detail stays lazy so opening Home remains capped at its three summary requests. */
    fun loadMorePayroll() {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(activeUser.role) || _state.value.payrollLoadingMore) return
        val current = _state.value.payroll
        if (current != null && !current.hasMore) return
        val period = _state.value.finance?.period ?: current?.period ?: return
        val requestedScope = activeUser.scopeIdentity
        val cacheScope = offlineCache.scopeFor(activeUser.serverUrl, activeUser.username)
        val page = current?.page?.plus(1) ?: 1
        val pageSize = current?.pageSize ?: 20
        viewModelScope.launch {
            _state.update { it.copy(payrollLoadingMore = true, financeError = null) }
            val result = commerceRepository.employeePayroll(period = period, page = page, pageSize = pageSize)
            val sameUser = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope
            if (!sameUser || _state.value.finance?.period != period) return@launch
            val displayedPayroll = _state.value.payroll?.takeIf { it.period == period }
            val nextPage = result.successOrNull()
            val mergedPayroll = nextPage?.copy(rows = displayedPayroll?.rows.orEmpty() + nextPage.rows)
            val failure = result.failureOrNull()
            val hasFallback = displayedPayroll != null
            val deviceOnline = hasDeviceInternetCapability(applicationContext)
            _state.update { state -> state.copy(
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
            ) }
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
            val homeCacheKey = homeCacheKey(activeUser.scopeIdentity, activeUser.role)
            val cacheScope = offlineCache.scopeFor(activeUser.serverUrl, activeUser.username)
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

            val dashboardJob = async { commerceRepository.dashboard(homeCacheKey) }
            val metricsJob = async { commerceRepository.dailyMetrics(homeCacheKey) }
            val financeJob = if (isAdmin) async { commerceRepository.financeMonthly(requestedFinancePeriod, homeCacheKey) } else null
            val dashboardResult = dashboardJob.await()
            val metricsResult = metricsJob.await()
            val financeResult = financeJob?.await()
            val finance = financeResult?.successOrNull()
            val payroll = cachedBundle?.payroll?.takeIf { it.period == finance?.period }

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
                    isLatest = requestedFinancePeriod == null,
                    scope = cacheScope,
                )
            }
            if ((sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != requestedScope) return@launch

            val failure = dashboardResult.failureOrNull()
            val financeFailure = financeResult?.failureOrNull()
            val dashboard = dashboardResult.successOrNull()
            val dailyMetrics = metricsResult.successOrNull()
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
                    dailyMetrics = dailyMetrics ?: current.dailyMetrics,
                    metricsLoading = false,
                    metricsError = metricsResult.failureOrNull()?.message?.takeIf { dailyMetrics == null && current.dailyMetrics == null },
                    currency = (if (financeIsCurrent) displayedFinance?.currency?.takeIf { it.isNotBlank() } else null)
                        ?: dashboard?.activeCurrency
                        ?: current.currency,
                    selectedFinancePeriod = when {
                        !isAdmin -> null
                        financeIsCurrent -> finance?.period ?: cachedForFinancePeriod?.finance?.period ?: requestedFinancePeriod
                        else -> current.selectedFinancePeriod
                    },
                    serverCurrentFinancePeriod = if (!isAdmin) null else {
                        finance?.period?.takeIf { it.matches(Regex("\\d{4}-(0[1-9]|1[0-2])")) }
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
                    financeFromCache = if (!isAdmin) false else if (finance != null) false else financeStillCached,
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
            val directory = withContext(Dispatchers.IO) {
                val reportDirectory = financeReportDirectory(cacheDirectory, requestedScope)
                reportDirectory.mkdirs()
                ma.etherealmber.atlascommerce.data.FinanceReportCache.pruneAccount(reportDirectory)
                reportDirectory
            }
            val destination = File(directory, "rapport-mensuel-$period-${System.currentTimeMillis()}.pdf")
            val result = commerceRepository.downloadFinanceReport(period, destination)
            if (result is ApiResult.Success) {
                withContext(Dispatchers.IO) {
                    ma.etherealmber.atlascommerce.data.FinanceReportCache.pruneAccount(directory)
                }
            }
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

    fun loadFinanceReportHistory(cacheDirectory: File, onResult: (ApiResult<List<File>>) -> Unit) {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user
        if (activeUser == null || !Roles.isAdmin(activeUser.role)) {
            onResult(ApiResult.Failure("FIN_FORBIDDEN", "Réservé aux administrateurs (owner/admin)."))
            return
        }
        val requestedScope = activeUser.scopeIdentity
        viewModelScope.launch {
            val result: ApiResult<List<File>> = try {
                ApiResult.Success(
                    withContext(Dispatchers.IO) {
                        ma.etherealmber.atlascommerce.data.FinanceReportCache.listAccount(
                            financeReportDirectory(cacheDirectory, requestedScope),
                        )
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ApiResult.Failure("REPORT_HISTORY", "Impossible de lire l'historique des rapports.")
            }
            val currentScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity
            if (currentScope != requestedScope) {
                onResult(ApiResult.Failure("SESSION_CHANGED", "Le compte actif a changé. Rouvrez l'historique avec le bon compte."))
            } else {
                onResult(result)
            }
        }
    }

    private fun homeCacheKey(scopeIdentity: String, role: String): String =
        "$scopeIdentity:${role.trim().lowercase()}"

    private fun financeReportDirectory(cacheDirectory: File, scopeIdentity: String): File =
        File(File(cacheDirectory, "finance"), scopeCacheKey(scopeIdentity))

    private fun scopeCacheKey(scopeIdentity: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(scopeIdentity.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
