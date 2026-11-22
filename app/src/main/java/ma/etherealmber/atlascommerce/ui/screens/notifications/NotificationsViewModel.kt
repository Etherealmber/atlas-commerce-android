package ma.etherealmber.atlascommerce.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.RoleTarget
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.model.AlertItemDto
import ma.etherealmber.atlascommerce.data.model.MobileActionDto
import ma.etherealmber.atlascommerce.data.model.ProductRequestBody
import ma.etherealmber.atlascommerce.data.model.CreateProductRequest
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val commerceRepository: CommerceRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val role: String = Roles.USER,
        val username: String = "",
        val alerts: List<AlertItemDto> = emptyList(),
        val actions: List<MobileActionDto> = emptyList(),
        val pendingOnly: Boolean = false,
        val sentNotificationCount: Int = 0,
        val busyId: Long? = null,
        val sendingNotification: Boolean = false,
        val productActionDetails: MobileActionDto? = null,
        val loadingProductActionId: Long? = null,
        val error: String? = null,
    ) {
        val isAdmin: Boolean get() = Roles.isAdmin(role)
        val roleLabel: String get() = Roles.label(role)
        val roleScopeSummary: String get() = Roles.scopeSummary(role)
        val availableTargets: List<RoleTarget> get() = Roles.sendableTargets(role)

        val visibleActions: List<MobileActionDto>
            get() = if (pendingOnly) {
                actions.filter { it.isPending }
            } else {
                actions
            }

        val pendingCount: Int
            get() = actions.count { it.isPending }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var productActionDetailsGeneration = 0L

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            sessionManager.state
                .map { st -> (st as? SessionState.Active)?.user?.scopeIdentity }
                .distinctUntilChanged()
                .collect { key ->
                    refreshJob?.cancel()
                    productActionDetailsGeneration += 1
                    _state.value = UiState()
                    if (key != null) {
                        refresh()
                    }
                }
        }
    }

    fun togglePendingOnly(only: Boolean) {
        _state.update { it.copy(pendingOnly = only) }
    }

    fun refresh() {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        val requestedScope = activeUser.scopeIdentity
        val role = activeUser.role
        val username = activeUser.username
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    role = role,
                    username = username,
                    error = null,
                )
            }
            val alertsRes = commerceRepository.alerts()
            val actionsRes = commerceRepository.mobileActions("all")
            val currentScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity
            if (currentScope != requestedScope) return@launch

            val rawAlerts = (alertsRes as? ApiResult.Success)?.data.orEmpty()
            val rawActions = (actionsRes as? ApiResult.Success)?.data.orEmpty()
            val filteredAlerts = Roles.filterAlertsForRole(role, rawAlerts)
            val filteredActions = rawActions.filter {
                Roles.isActionVisibleForUser(role = role, username = username, action = it)
            }
            val err = if (alertsRes is ApiResult.Failure && actionsRes is ApiResult.Failure) alertsRes.message else null
            _state.update {
                it.copy(
                    loading = false,
                    role = role,
                    username = username,
                    alerts = filteredAlerts,
                    actions = filteredActions,
                    sentNotificationCount = filteredActions.count { action ->
                        action.createdBy.equals(username, ignoreCase = true) &&
                            commerceRepository.parseActionMeta(action)?.category.equals("role_notification", ignoreCase = true)
                    },
                    error = err,
                )
            }
        }
    }

    fun sendNotificationToRole(
        target: RoleTarget,
        title: String,
        message: String,
        urgent: Boolean,
        onSent: () -> Unit,
    ) {
        if (title.trim().isEmpty()) {
            _messages.tryEmit("Veuillez saisir un objet pour la notification.")
            return
        }
        viewModelScope.launch {
            val activeUser = (sessionManager.state.value as? SessionState.Active)?.user
            _state.update { it.copy(sendingNotification = true) }
            when (
                val res = commerceRepository.sendRoleNotification(
                    senderUsername = activeUser?.username ?: "mobile",
                    senderRole = activeUser?.role,
                    target = target,
                    title = title,
                    message = message,
                    urgent = urgent,
                )
            ) {
                is ApiResult.Failure -> {
                    _state.update { it.copy(sendingNotification = false) }
                    _messages.tryEmit(res.message)
                }

                is ApiResult.Success -> {
                    _state.update { it.copy(sendingNotification = false) }
                    _messages.tryEmit("Notification envoyée à : ${target.label}.")
                    onSent()
                    refresh()
                }
            }
        }
    }

    fun productRequestBody(action: MobileActionDto): ProductRequestBody? =
        commerceRepository.parseActionMeta(action)?.createProductBody

    fun loadProductActionDetails(action: MobileActionDto) {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        if (!Roles.isAdmin(activeUser.role) || !action.type.equals("new_product", ignoreCase = true)) return
        val requestedScope = activeUser.scopeIdentity
        val generation = ++productActionDetailsGeneration
        _state.update { it.copy(loadingProductActionId = action.id, productActionDetails = null) }
        viewModelScope.launch {
            when (val result = commerceRepository.mobileAction(action.id)) {
                is ApiResult.Failure -> {
                    if (generation == productActionDetailsGeneration &&
                        (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope
                    ) {
                        _state.update { it.copy(loadingProductActionId = null) }
                        _messages.tryEmit(result.message)
                    }
                }
                is ApiResult.Success -> {
                    if (generation != productActionDetailsGeneration ||
                        (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != requestedScope
                    ) return@launch
                    _state.update {
                        it.copy(
                            loadingProductActionId = null,
                            productActionDetails = result.data,
                        )
                    }
                }
            }
        }
    }

    fun clearProductActionDetails() {
        productActionDetailsGeneration += 1
        _state.update { it.copy(loadingProductActionId = null, productActionDetails = null) }
    }

    fun completeAction(
        action: MobileActionDto,
        productRequest: CreateProductRequest? = null,
        onResult: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val activeUser = (sessionManager.state.value as? SessionState.Active)?.user
            val isAdmin = Roles.isAdmin(activeUser?.role)

            // STRICT: Only an Admin can validate a product creation request (`new_product`)!
            if (action.type.equals("new_product", ignoreCase = true) && !isAdmin) {
                _messages.tryEmit("Seul un Administrateur peut valider et créer ce produit.")
                onResult(false)
                return@launch
            }

            _state.update { it.copy(busyId = action.id) }
            if (isAdmin && action.type.equals("new_product", ignoreCase = true)) {
                when (
                    val res = commerceRepository.approveActionAsAdmin(
                        action = action,
                        adminUsername = activeUser?.username ?: "admin",
                        productRequest = productRequest,
                    )
                ) {
                    is ApiResult.Failure -> {
                        _state.update { it.copy(busyId = null) }
                        _messages.tryEmit(res.message)
                        onResult(false)
                    }

                    is ApiResult.Success -> {
                        _state.update { it.copy(busyId = null) }
                        onResult(true)
                        val created = res.data
                        _messages.tryEmit(
                            if (created != null) {
                                "Produit « ${created.name} » créé et demande validée."
                            } else {
                                "Demande « ${action.title} » validée."
                            },
                        )
                        refresh()
                    }
                }
                return@launch
            }

            when (
                val res = commerceRepository.completeMobileAction(
                    id = action.id,
                    applyStock = true,
                    completedBy = activeUser?.username,
                )
            ) {
                is ApiResult.Failure -> {
                    _state.update { it.copy(busyId = null) }
                    _messages.tryEmit(res.message)
                }

                is ApiResult.Success -> {
                    _state.update { it.copy(busyId = null) }
                    _messages.tryEmit("Notification « ${action.title} » marquée comme traitée.")
                    refresh()
                }
            }
        }
    }

    fun rejectAction(action: MobileActionDto) {
        viewModelScope.launch {
            val activeUser = (sessionManager.state.value as? SessionState.Active)?.user
            if (!Roles.canCreateDocuments(activeUser?.role)) {
                _messages.tryEmit("Seul un Administrateur ou responsable peut refuser cette demande.")
                return@launch
            }
            _state.update { it.copy(busyId = action.id) }
            when (val res = commerceRepository.rejectMobileAction(action.id, "Refusée depuis mobile")) {
                is ApiResult.Failure -> {
                    _state.update { it.copy(busyId = null) }
                    _messages.tryEmit(res.message)
                }

                is ApiResult.Success -> {
                    _state.update { it.copy(busyId = null) }
                    _messages.tryEmit("Demande « ${action.title} » refusée.")
                    refresh()
                }
            }
        }
    }
}
