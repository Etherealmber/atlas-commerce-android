package ma.etherealmber.atlascommerce.ui.screens.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.model.EmployeeDto
import ma.etherealmber.atlascommerce.data.model.EmployeeUpdateRequest
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import javax.inject.Inject

@HiltViewModel
class EmployeeDirectoryViewModel @Inject constructor(
    private val repository: CommerceRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    data class UiState(
        val scopeIdentity: String? = null,
        val employees: List<EmployeeDto> = emptyList(),
        val selectedEmployee: EmployeeDto? = null,
        val loading: Boolean = false,
        val detailsLoading: Boolean = false,
        val saving: Boolean = false,
        val deleting: Boolean = false,
        val error: String? = null,
        val actionError: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var detailsJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager.state
                .map { (it as? SessionState.Active)?.user }
                .distinctUntilChanged { a, b -> a?.scopeIdentity == b?.scopeIdentity }
                .collect { user ->
                    detailsJob?.cancel()
                    detailsJob = null
                    _state.value = UiState(scopeIdentity = user?.scopeIdentity)
                    if (user != null && Roles.isAdmin(user.role)) load()
                }
        }
    }

    fun load() {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(user.role)) return
        val scope = user.scopeIdentity
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.employees(status = "all")) {
                is ApiResult.Success -> if (activeScope() == scope) {
                    _state.update { it.copy(loading = false, employees = result.data, error = null) }
                }
                is ApiResult.Failure -> if (activeScope() == scope) {
                    _state.update { it.copy(loading = false, error = result.message) }
                }
            }
        }
    }

    fun showDetails(id: Long) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(user.role)) return
        val scope = user.scopeIdentity
        detailsJob?.cancel()
        _state.update { it.copy(detailsLoading = true, actionError = null) }
        detailsJob = viewModelScope.launch {
            when (val result = repository.employee(id)) {
                is ApiResult.Success -> if (activeScope() == scope) {
                    _state.update { it.copy(detailsLoading = false, selectedEmployee = result.data, actionError = null) }
                }
                is ApiResult.Failure -> if (activeScope() == scope) {
                    _state.update { it.copy(detailsLoading = false, actionError = result.message) }
                }
            }
        }
    }

    fun clearDetails() {
        detailsJob?.cancel()
        detailsJob = null
        _state.update { it.copy(selectedEmployee = null, detailsLoading = false, actionError = null) }
    }

    fun updateEmployee(id: Long, request: EmployeeUpdateRequest, onComplete: (Boolean) -> Unit = {}) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(user.role) || _state.value.saving) return
        val scope = user.scopeIdentity
        _state.update { it.copy(saving = true, actionError = null) }
        viewModelScope.launch {
            when (val result = repository.updateEmployee(id, request)) {
                is ApiResult.Success -> if (activeScope() == scope) {
                    _state.update { it.copy(saving = false, selectedEmployee = result.data, actionError = null) }
                    load()
                    onComplete(true)
                } else onComplete(false)
                is ApiResult.Failure -> if (activeScope() == scope) {
                    _state.update { it.copy(saving = false, actionError = result.message) }
                    onComplete(false)
                } else onComplete(false)
            }
        }
    }

    fun markEmployeeFormer(id: Long, onComplete: (Boolean) -> Unit = {}) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(user.role) || _state.value.saving) return
        val scope = user.scopeIdentity
        _state.update { it.copy(saving = true, actionError = null) }
        viewModelScope.launch {
            when (val result = repository.markEmployeeFormer(id)) {
                is ApiResult.Success -> if (activeScope() == scope) {
                    _state.update { it.copy(saving = false, selectedEmployee = result.data, actionError = null) }
                    load()
                    onComplete(true)
                } else onComplete(false)
                is ApiResult.Failure -> if (activeScope() == scope) {
                    _state.update { it.copy(saving = false, actionError = result.message) }
                    onComplete(false)
                } else onComplete(false)
            }
        }
    }

    fun deleteEmployee(id: Long) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(user.role) || _state.value.deleting) return
        val target = _state.value.selectedEmployee?.takeIf { it.id == id } ?: return
        if (target.active != 0) {
            _state.update { it.copy(actionError = "Démissionnez d’abord l’employé avant une suppression définitive.") }
            return
        }
        val scope = user.scopeIdentity
        _state.update { it.copy(deleting = true, actionError = null) }
        viewModelScope.launch {
            when (val result = repository.deleteEmployee(id)) {
                is ApiResult.Success -> if (activeScope() == scope) {
                    _state.update { it.copy(deleting = false, selectedEmployee = null, actionError = null) }
                    load()
                }
                is ApiResult.Failure -> if (activeScope() == scope) {
                    _state.update { it.copy(deleting = false, actionError = result.message) }
                }
            }
        }
    }

    private fun activeScope(): String? =
        (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity
}
