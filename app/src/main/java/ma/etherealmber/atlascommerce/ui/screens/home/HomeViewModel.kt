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
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ApiRunner
import ma.etherealmber.atlascommerce.core.net.failureOrNull
import ma.etherealmber.atlascommerce.core.net.successOrNull
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.model.CompanySettingsDto
import ma.etherealmber.atlascommerce.data.model.DashboardDto
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
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

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = initial && it.dashboard == null,
                    refreshing = !initial,
                    error = null,
                )
            }

            val dashboardJob = async { commerceRepository.dashboard() }
            val settingsJob = async { commerceRepository.settings() }
            val dashboardResult = dashboardJob.await()
            val settingsResult = settingsJob.await()

            val failure = dashboardResult.failureOrNull() ?: settingsResult.failureOrNull()
            val dashboard = dashboardResult.successOrNull()
            val settings = settingsResult.successOrNull()

            _state.update { current ->
                current.copy(
                    loading = false,
                    refreshing = false,
                    dashboard = dashboard ?: current.dashboard,
                    settings = settings ?: current.settings,
                    currency = dashboard?.activeCurrency
                        ?: settings?.currency?.takeIf { it.isNotBlank() }
                        ?: current.currency,
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
}
