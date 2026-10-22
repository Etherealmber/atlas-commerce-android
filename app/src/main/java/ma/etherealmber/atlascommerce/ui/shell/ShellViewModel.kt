package ma.etherealmber.atlascommerce.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.data.CartStore
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import javax.inject.Inject

@HiltViewModel
class ShellViewModel @Inject constructor(
    cartStore: CartStore,
    private val commerceRepository: CommerceRepository,
    serverConfig: ServerConfig,
) : ViewModel() {

    val cartCount: StateFlow<Int> = cartStore.lines
        .map { lines -> lines.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val serverBaseUrl: StateFlow<String?> = serverConfig.selection
        .map { it?.baseUrl }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()

    init {
        refreshNotificationsBadge()
    }

    fun refreshNotificationsBadge() {
        viewModelScope.launch {
            val alerts = (commerceRepository.alerts() as? ApiResult.Success)?.data.orEmpty()
            val stats = (commerceRepository.mobileActionStats() as? ApiResult.Success)?.data
            _notificationCount.value = alerts.size + (stats?.pending ?: 0)
        }
    }
}
