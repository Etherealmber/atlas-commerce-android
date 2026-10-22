package ma.etherealmber.atlascommerce.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.data.model.AlertItemDto
import ma.etherealmber.atlascommerce.data.model.MobileActionDto
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val commerceRepository: CommerceRepository,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val alerts: List<AlertItemDto> = emptyList(),
        val actions: List<MobileActionDto> = emptyList(),
        val pendingOnly: Boolean = false,
        val busyId: Long? = null,
        val error: String? = null,
    ) {
        val visibleActions: List<MobileActionDto>
            get() = if (pendingOnly) {
                actions.filter { it.status.equals("pending", ignoreCase = true) }
            } else {
                actions
            }

        val pendingCount: Int
            get() = actions.count { it.status.equals("pending", ignoreCase = true) }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        refresh()
    }

    fun togglePendingOnly(only: Boolean) {
        _state.update { it.copy(pendingOnly = only) }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val alertsRes = commerceRepository.alerts()
            val actionsRes = commerceRepository.mobileActions("all")

            val alerts = (alertsRes as? ApiResult.Success)?.data.orEmpty()
            val actions = (actionsRes as? ApiResult.Success)?.data.orEmpty()
            val err = if (alertsRes is ApiResult.Failure && actionsRes is ApiResult.Failure) {
                alertsRes.message
            } else {
                null
            }
            _state.update {
                it.copy(
                    loading = false,
                    alerts = alerts,
                    actions = actions,
                    error = err,
                )
            }
        }
    }

    fun completeAction(action: MobileActionDto) {
        viewModelScope.launch {
            _state.update { it.copy(busyId = action.id) }
            when (val res = commerceRepository.completeMobileAction(action.id, applyStock = true)) {
                is ApiResult.Failure -> {
                    _state.update { it.copy(busyId = null) }
                    _messages.tryEmit(res.message)
                }

                is ApiResult.Success -> {
                    _state.update { it.copy(busyId = null) }
                    _messages.tryEmit("Action « ${action.title} » validée.")
                    refresh()
                }
            }
        }
    }

    fun rejectAction(action: MobileActionDto) {
        viewModelScope.launch {
            _state.update { it.copy(busyId = action.id) }
            when (val res = commerceRepository.rejectMobileAction(action.id, "Ignorée depuis mobile")) {
                is ApiResult.Failure -> {
                    _state.update { it.copy(busyId = null) }
                    _messages.tryEmit(res.message)
                }

                is ApiResult.Success -> {
                    _state.update { it.copy(busyId = null) }
                    _messages.tryEmit("Action « ${action.title} » écartée.")
                    refresh()
                }
            }
        }
    }
}
