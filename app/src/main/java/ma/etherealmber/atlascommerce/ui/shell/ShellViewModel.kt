package ma.etherealmber.atlascommerce.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.core.update.RoleNotificationDispatcher
import ma.etherealmber.atlascommerce.data.CartStore
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import javax.inject.Inject

@HiltViewModel
class ShellViewModel @Inject constructor(
    cartStore: CartStore,
    private val commerceRepository: CommerceRepository,
    private val sessionManager: SessionManager,
    private val preferences: AtlasPreferences,
    private val notificationDispatcher: RoleNotificationDispatcher,
    serverConfig: ServerConfig,
) : ViewModel() {

    val cartCount: StateFlow<Int> = cartStore.lines
        .map { lines -> lines.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val serverBaseUrl: StateFlow<String?> = serverConfig.selection
        .map { it?.baseUrl }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val roleNotificationsEnabled: StateFlow<Boolean> = preferences.roleNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager.state
                .map { st -> (st as? SessionState.Active)?.user?.scopeIdentity }
                .distinctUntilChanged()
                .collect { key ->
                    pollJob?.cancel()
                    _notificationCount.value = 0
                    if (key != null) {
                        val user = (sessionManager.state.value as? SessionState.Active)?.user
                        if (user != null) {
                            notificationDispatcher.onSessionChanged(
                                "${user.serverUrl}|${user.username}",
                                user.role,
                            )
                            startRolePolling()
                        }
                    }
                }
        }
    }

    private fun startRolePolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                refreshNotificationsBadge()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun refreshNotificationsBadge() {
        viewModelScope.launch {
            val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: run {
                _notificationCount.value = 0
                return@launch
            }
            val role = activeUser.role
            val username = activeUser.username
            val roleLabel = Roles.label(role)

            val rawAlerts = (commerceRepository.alerts() as? ApiResult.Success)?.data.orEmpty()
            val rawActions = (commerceRepository.mobileActions("all") as? ApiResult.Success)?.data.orEmpty()

            val roleAlerts = Roles.filterAlertsForRole(role, rawAlerts)
            val roleActions = rawActions.filter { Roles.isActionVisibleForUser(role, username, it) }
            val actionablePending = roleActions.filter { it.isPending }

            _notificationCount.value = roleAlerts.size + actionablePending.size

            if (!roleNotificationsEnabled.value) return@launch

            // Dispatch OS notifications strictly relevant to the current user's role:
            // 1. Incoming pending items NOT created by this user (e.g. Admin receives staff product request,
            //    or Stock/Sales receives a role-targeted notification).
            actionablePending
                .filterNot { it.createdBy.equals(username, ignoreCase = true) }
                .take(3)
                .forEach { action ->
                    notificationDispatcher.dispatchIfNew(
                        dedupKey = "pending:${action.id}:${action.status}",
                        title = action.title.ifBlank { "Nouvelle notification Atlas" },
                        body = action.detail?.takeIf { it.isNotBlank() }
                            ?: "Action en attente dans votre espace $roleLabel",
                        roleBadge = roleLabel,
                        urgent = action.isUrgent || action.type == "new_product",
                    )
                }

            // 2. Notify staff member when an Admin approves or rejects their own submitted request!
            if (!Roles.isAdmin(role)) {
                roleActions
                    .filter {
                        it.createdBy.equals(username, ignoreCase = true) &&
                            (it.isCompleted || it.isRejectedOrCancelled)
                    }
                    .take(2)
                    .forEach { action ->
                        val approved = action.isCompleted
                        notificationDispatcher.dispatchIfNew(
                            dedupKey = "resolved:${action.id}:${action.status}",
                            title = if (approved) {
                                "Demande validée par l'Admin ✓"
                            } else {
                                "Demande refusée par l'Admin"
                            },
                            body = action.title,
                            roleBadge = roleLabel,
                            urgent = false,
                        )
                    }
            }
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 25_000L
    }
}
