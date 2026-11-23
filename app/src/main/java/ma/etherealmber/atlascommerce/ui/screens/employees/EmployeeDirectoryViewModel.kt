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
import java.util.UUID
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
        val situationEmployee: EmployeeDto? = null,
        val situationLoading: Boolean = false,
        val loading: Boolean = false,
        val detailsLoading: Boolean = false,
        val saving: Boolean = false,
        val deleting: Boolean = false,
        val error: String? = null,
        val actionError: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var directoryJob: Job? = null
    private var detailsJob: Job? = null
    private var actionJob: Job? = null
    private var deleteJob: Job? = null
    private var situationJob: Job? = null
    private var directoryGeneration: Long = 0L
    private var detailGeneration: Long = 0L
    private var situationGeneration: Long = 0L

    init {
        viewModelScope.launch {
            sessionManager.state
                .map { (it as? SessionState.Active)?.user }
                .distinctUntilChanged { a, b -> a?.scopeIdentity == b?.scopeIdentity && a?.role == b?.role }
                .collect { user ->
                    directoryJob?.cancel()
                    detailsJob?.cancel()
                    actionJob?.cancel()
                    deleteJob?.cancel()
                    situationJob?.cancel()
                    directoryJob = null
                    detailsJob = null
                    actionJob = null
                    deleteJob = null
                    situationJob = null
                    directoryGeneration += 1
                    detailGeneration += 1
                    situationGeneration += 1
                    _state.value = UiState(scopeIdentity = user?.scopeIdentity)
                    if (user != null && canAccessEmployees(user.role)) load()
                }
        }
    }

    fun load() {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!canAccessEmployees(user.role)) return
        val scope = user.scopeIdentity
        val generation = ++directoryGeneration
        directoryJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        directoryJob = viewModelScope.launch {
            when (val result = repository.employees(status = "all")) {
                is ApiResult.Success -> if (activeAdminScope() == scope && directoryGeneration == generation) {
                    _state.update { it.copy(loading = false, employees = result.data, error = null) }
                }
                is ApiResult.Failure -> if (activeAdminScope() == scope && directoryGeneration == generation) {
                    _state.update { it.copy(loading = false, error = result.message) }
                }
            }
        }
    }

    fun showDetails(id: Long) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!canAccessEmployees(user.role)) return
        val scope = user.scopeIdentity
        val generation = ++detailGeneration
        detailsJob?.cancel()
        _state.update { it.copy(detailsLoading = true, actionError = null) }
        detailsJob = viewModelScope.launch {
            when (val result = repository.employee(id)) {
                is ApiResult.Success -> if (activeAdminScope() == scope && detailGeneration == generation) {
                    _state.update { it.copy(detailsLoading = false, selectedEmployee = result.data, actionError = null) }
                }
                is ApiResult.Failure -> if (activeAdminScope() == scope && detailGeneration == generation) {
                    _state.update { it.copy(detailsLoading = false, actionError = result.message) }
                }
            }
        }
    }

    fun loadSituation(id: Long) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!canAccessEmployees(user.role)) return
        val scope = user.scopeIdentity
        val generation = ++situationGeneration
        situationJob?.cancel()
        _state.update { it.copy(situationEmployee = null, situationLoading = true, actionError = null) }
        situationJob = viewModelScope.launch {
            when (val result = repository.employee(id)) {
                is ApiResult.Success -> if (activeAdminScope() == scope && situationGeneration == generation) {
                    _state.update { it.copy(situationEmployee = result.data, situationLoading = false) }
                }
                is ApiResult.Failure -> if (activeAdminScope() == scope && situationGeneration == generation) {
                    _state.update { it.copy(situationLoading = false, actionError = result.message) }
                }
            }
        }
    }

    fun clearSituation() {
        situationGeneration += 1
        situationJob?.cancel()
        situationJob = null
        _state.update { it.copy(situationEmployee = null, situationLoading = false) }
    }

    fun clearDetails() {
        detailGeneration += 1
        detailsJob?.cancel()
        detailsJob = null
        _state.update { it.copy(selectedEmployee = null, detailsLoading = false, actionError = null) }
    }

    fun updateEmployee(id: Long, request: EmployeeUpdateRequest, onComplete: (Boolean) -> Unit = {}) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!canAccessEmployees(user.role) || _state.value.saving) return
        val scope = user.scopeIdentity
        val generation = detailGeneration
        _state.update { it.copy(saving = true, actionError = null) }
        actionJob = viewModelScope.launch {
            when (val result = repository.updateEmployee(id, request)) {
                is ApiResult.Success -> if (activeAdminScope() == scope) {
                    _state.update { current ->
                        current.copy(
                            saving = false,
                            selectedEmployee = if (detailGeneration == generation) result.data else current.selectedEmployee,
                            actionError = if (detailGeneration == generation) null else current.actionError,
                        )
                    }
                    load()
                    onComplete(true)
                } else onComplete(false)
                is ApiResult.Failure -> if (activeAdminScope() == scope) {
                    _state.update { current ->
                        current.copy(
                            saving = false,
                            actionError = if (detailGeneration == generation) result.message else current.actionError,
                        )
                    }
                    onComplete(false)
                } else onComplete(false)
            }
        }
    }

    fun markEmployeeFormer(id: Long, reason: String, onComplete: (Boolean) -> Unit = {}) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(user.role) || _state.value.saving) return
        val scope = user.scopeIdentity
        val generation = detailGeneration
        _state.update { it.copy(saving = true, actionError = null) }
        actionJob = viewModelScope.launch {
            when (val result = repository.markEmployeeFormer(id, reason.trim().ifBlank { "Départ" })) {
                is ApiResult.Success -> if (activeAdminScope() == scope) {
                    _state.update { current ->
                        current.copy(
                            saving = false,
                            selectedEmployee = if (detailGeneration == generation) result.data else current.selectedEmployee,
                            actionError = if (detailGeneration == generation) null else current.actionError,
                        )
                    }
                    load()
                    onComplete(true)
                } else onComplete(false)
                is ApiResult.Failure -> if (activeAdminScope() == scope) {
                    _state.update { current ->
                        current.copy(
                            saving = false,
                            actionError = if (detailGeneration == generation) result.message else current.actionError,
                        )
                    }
                    onComplete(false)
                } else onComplete(false)
            }
        }
    }

    fun recordEmployeePay(id: Long, note: String = "", onComplete: (Boolean) -> Unit = {}) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(user.role) || _state.value.saving) return
        val scope = user.scopeIdentity
        val generation = detailGeneration
        _state.update { it.copy(saving = true, actionError = null) }
        actionJob = viewModelScope.launch {
            when (val result = repository.payEmployee(id, UUID.randomUUID().toString(), note)) {
                is ApiResult.Success -> if (activeAdminScope() == scope) {
                    _state.update { current ->
                        current.copy(
                            saving = false,
                            selectedEmployee = if (detailGeneration == generation) result.data else current.selectedEmployee,
                            actionError = if (detailGeneration == generation) null else current.actionError,
                        )
                    }
                    load()
                    if (detailGeneration == generation && _state.value.selectedEmployee?.id == id) showDetails(id)
                    onComplete(true)
                } else onComplete(false)
                is ApiResult.Failure -> if (activeAdminScope() == scope) {
                    _state.update { current ->
                        current.copy(
                            saving = false,
                            actionError = if (detailGeneration == generation) result.message else current.actionError,
                        )
                    }
                    onComplete(false)
                } else onComplete(false)
            }
        }
    }

    fun recordEmployeeAdvance(id: Long, note: String = "", onComplete: (Boolean) -> Unit = {}) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(user.role) || _state.value.saving) return
        val scope = user.scopeIdentity
        val generation = detailGeneration
        _state.update { it.copy(saving = true, actionError = null) }
        actionJob = viewModelScope.launch {
            when (val result = repository.advanceEmployee(id, UUID.randomUUID().toString(), note)) {
                is ApiResult.Success -> if (activeAdminScope() == scope) {
                    _state.update { current ->
                        current.copy(
                            saving = false,
                            selectedEmployee = if (detailGeneration == generation) result.data else current.selectedEmployee,
                            actionError = if (detailGeneration == generation) null else current.actionError,
                        )
                    }
                    load()
                    if (detailGeneration == generation && _state.value.selectedEmployee?.id == id) showDetails(id)
                    onComplete(true)
                } else onComplete(false)
                is ApiResult.Failure -> if (activeAdminScope() == scope) {
                    _state.update { current ->
                        current.copy(
                            saving = false,
                            actionError = if (detailGeneration == generation) result.message else current.actionError,
                        )
                    }
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
        val generation = detailGeneration
        _state.update { it.copy(deleting = true, actionError = null) }
        deleteJob = viewModelScope.launch {
            when (val result = repository.deleteEmployee(id)) {
                is ApiResult.Success -> if (activeAdminScope() == scope) {
                    _state.update { current ->
                        current.copy(
                            deleting = false,
                            selectedEmployee = if (detailGeneration == generation) null else current.selectedEmployee,
                            actionError = if (detailGeneration == generation) null else current.actionError,
                        )
                    }
                    load()
                }
                is ApiResult.Failure -> if (activeAdminScope() == scope) {
                    _state.update { current ->
                        current.copy(
                            deleting = false,
                            actionError = if (detailGeneration == generation) result.message else current.actionError,
                        )
                    }
                }
            }
        }
    }

    private fun activeAdminScope(): String? =
        (sessionManager.state.value as? SessionState.Active)?.user
            ?.takeIf { canAccessEmployees(it.role) }
            ?.scopeIdentity

    private fun canAccessEmployees(role: String?): Boolean =
        Roles.isAdmin(role) || Roles.normalize(role) == Roles.SECRETARY
}
