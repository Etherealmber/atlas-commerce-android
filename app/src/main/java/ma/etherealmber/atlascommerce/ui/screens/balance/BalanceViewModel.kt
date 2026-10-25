package ma.etherealmber.atlascommerce.ui.screens.balance

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
import java.io.File
import java.security.MessageDigest
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.model.CreateFinanceEntryRequest
import ma.etherealmber.atlascommerce.data.model.FinanceEntryDto
import ma.etherealmber.atlascommerce.data.model.FinanceMonthlyDto
import ma.etherealmber.atlascommerce.data.model.FinancePeriodDto
import ma.etherealmber.atlascommerce.data.model.FinanceStatementDto
import ma.etherealmber.atlascommerce.data.model.FinanceStatementRowDto
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import javax.inject.Inject

@HiltViewModel
class BalanceViewModel @Inject constructor(
    private val repository: CommerceRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    data class UiState(
        val loading: Boolean = true,
        val refreshing: Boolean = false,
        val isAdmin: Boolean = false,
        val selectedPeriod: String? = null,
        val finance: FinanceMonthlyDto? = null,
        val periodBreakdown: FinancePeriodDto? = null,
        val periodBreakdownError: String? = null,
        val statement: FinanceStatementDto? = null,
        val rows: List<FinanceStatementRowDto> = emptyList(),
        val loadingMore: Boolean = false,
        val reportGenerating: Boolean = false,
        val savingEntry: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.state
                .map { (it as? SessionState.Active)?.user }
                .distinctUntilChanged { old, new -> old?.scopeIdentity == new?.scopeIdentity }
                .collect { user ->
                    _state.value = UiState(isAdmin = user?.let { Roles.isAdmin(it.role) } == true)
                    if (user != null && Roles.isAdmin(user.role)) load(period = null, initial = true)
                }
        }
    }

    fun refresh() {
        val current = _state.value
        if (!current.isAdmin) return
        load(current.selectedPeriod, initial = false)
    }

    fun selectPeriod(period: String) {
        if (!period.matches(Regex("\\d{4}-(0[1-9]|1[0-2])"))) return
        if (_state.value.selectedPeriod == period && _state.value.finance?.period == period) return
        load(period, initial = true)
    }

    fun createEntry(request: CreateFinanceEntryRequest, onResult: (ApiResult<FinanceEntryDto>) -> Unit) {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user
        if (activeUser == null || !Roles.isAdmin(activeUser.role)) {
            onResult(ApiResult.Failure("FIN_FORBIDDEN", "Réservé aux administrateurs (owner/admin)."))
            return
        }
        if (_state.value.savingEntry) return
        val scopeIdentity = activeUser.scopeIdentity
        viewModelScope.launch {
            _state.update { it.copy(savingEntry = true, error = null) }
            val result = repository.createFinanceEntry(request)
            val sameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == scopeIdentity
            _state.update { it.copy(savingEntry = false) }
            if (!sameScope) {
                onResult(ApiResult.Failure("SESSION_CHANGED", "Le compte actif a changé. Vérifiez l'écriture avant de la renvoyer."))
                return@launch
            }
            when (result) {
                is ApiResult.Success -> {
                    onResult(result)
                    load(_state.value.selectedPeriod, initial = false)
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(error = result.message) }
                    onResult(result)
                }
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        val active = sessionManager.state.value as? SessionState.Active ?: return
        val statement = current.statement ?: return
        if (!current.isAdmin || !statement.hasMore || current.loadingMore) return
        val scopeIdentity = active.user.scopeIdentity
        val period = statement.period
        val nextPage = statement.page + 1
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true, error = null) }
            val result = repository.financeStatement(period, nextPage, statement.pageSize)
            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == scopeIdentity
            if (!stillSameScope) return@launch
            when (result) {
                is ApiResult.Success -> _state.update { state ->
                    if (state.selectedPeriod != period) state else state.copy(
                        statement = result.data,
                        rows = state.rows + result.data.rows,
                        loadingMore = false,
                    )
                }
                is ApiResult.Failure -> _state.update { state ->
                    if (state.selectedPeriod != period) state else state.copy(loadingMore = false, error = result.message)
                }
            }
        }
    }

    fun downloadReport(cacheDirectory: File, period: String, onResult: (ApiResult<File>) -> Unit) {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user
        if (activeUser == null || !Roles.isAdmin(activeUser.role)) {
            onResult(ApiResult.Failure("FIN_FORBIDDEN", "Réservé aux administrateurs (owner/admin)."))
            return
        }
        val scopeIdentity = activeUser.scopeIdentity
        viewModelScope.launch {
            _state.update { it.copy(reportGenerating = true) }
            val directory = File(File(cacheDirectory, "finance"), scopeCacheKey(scopeIdentity)).apply { mkdirs() }
            ma.etherealmber.atlascommerce.data.FinanceReportCache.pruneAccount(directory)
            val file = File(directory, "rapport-mensuel-$period.pdf")
            val result = repository.downloadFinanceReport(period, file)
            _state.update { it.copy(reportGenerating = false) }
            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == scopeIdentity
            if (!stillSameScope) {
                (result as? ApiResult.Success)?.data?.delete()
                onResult(ApiResult.Failure("SESSION_CHANGED", "Le compte actif a changé. Relancez le téléchargement avec le bon compte."))
            } else onResult(result)
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
        val scopeIdentity = activeUser.scopeIdentity
        viewModelScope.launch {
            _state.update { it.copy(reportGenerating = true) }
            val result = repository.createCombinedReportUrl(period, from, to, sections)
            _state.update { it.copy(reportGenerating = false) }
            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == scopeIdentity
            if (!stillSameScope) onResult(ApiResult.Failure("SESSION_CHANGED", "Le compte actif a changé. Relancez l'ouverture avec le bon compte."))
            else onResult(result)
        }
    }

    private fun scopeCacheKey(scopeIdentity: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(scopeIdentity.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun load(period: String?, initial: Boolean) {
        val active = sessionManager.state.value as? SessionState.Active ?: return
        if (!Roles.isAdmin(active.user.role)) return
        val scopeIdentity = active.user.scopeIdentity
        _state.update {
            it.copy(
                loading = initial,
                refreshing = !initial,
                selectedPeriod = period ?: it.selectedPeriod,
                finance = if (initial) null else it.finance,
                periodBreakdown = if (initial) null else it.periodBreakdown,
                periodBreakdownError = null,
                statement = if (initial) null else it.statement,
                rows = if (initial) emptyList() else it.rows,
                loadingMore = false,
                error = null,
                isAdmin = true,
            )
        }
        viewModelScope.launch {
            val financeResult = repository.financeMonthly(period)
            val finance = (financeResult as? ApiResult.Success)?.data
            val periodKey = finance?.period?.takeIf { it.isNotBlank() }
            val statementDeferred = periodKey?.let { key -> async { repository.financeStatement(key, page = 1, pageSize = 50) } }
            val breakdownDeferred = periodKey?.let { key -> async { repository.financePeriod(key) } }
            val statementResult = statementDeferred?.await()
            val breakdownResult = breakdownDeferred?.await()
            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == scopeIdentity
            if (!stillSameScope) return@launch
            val selectionStillCurrent = if (period == null) _state.value.selectedPeriod == null else _state.value.selectedPeriod == period
            if (!selectionStillCurrent) return@launch
            val statement = (statementResult as? ApiResult.Success)?.data
            val breakdown = (breakdownResult as? ApiResult.Success)?.data
            val error = when {
                financeResult is ApiResult.Failure -> financeResult.message
                statementResult is ApiResult.Failure -> statementResult.message
                else -> null
            }
            _state.update { state ->
                state.copy(
                    loading = false,
                    refreshing = false,
                    selectedPeriod = finance?.period ?: period ?: state.selectedPeriod,
                    finance = finance,
                    periodBreakdown = breakdown,
                    periodBreakdownError = (breakdownResult as? ApiResult.Failure)?.message,
                    statement = statement,
                    rows = statement?.rows.orEmpty(),
                    error = error,
                )
            }
        }
    }
}
