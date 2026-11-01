package ma.etherealmber.atlascommerce.ui.screens.products

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
import ma.etherealmber.atlascommerce.data.model.ProductMovementDto
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import javax.inject.Inject

@HiltViewModel
class ProductHistoryViewModel @Inject constructor(
    private val commerceRepository: CommerceRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    data class UiState(
        val productId: Long = 0L,
        val scopeIdentity: String? = null,
        val movements: List<ProductMovementDto> = emptyList(),
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val canLoadMore: Boolean = false,
        val error: String? = null,
        val imageSaving: Boolean = false,
        val imageError: String? = null,
        val updatedImageUrl: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var historyJob: Job? = null
    private var moreJob: Job? = null
    private var imageJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager.state
                .map { (it as? SessionState.Active)?.user?.scopeIdentity }
                .distinctUntilChanged()
                .collect { scope ->
                    historyJob?.cancel()
                    moreJob?.cancel()
                    imageJob?.cancel()
                    _state.value = UiState(scopeIdentity = scope)
                }
        }
    }

    fun loadHistory(productId: Long, refresh: Boolean = false) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        val scope = user.scopeIdentity
        val current = _state.value
        if (!refresh && current.productId == productId && current.scopeIdentity == scope &&
            (current.loading || current.movements.isNotEmpty())
        ) return

        historyJob?.cancel()
        moreJob?.cancel()
        _state.value = UiState(productId = productId, scopeIdentity = scope, loading = true)
        historyJob = viewModelScope.launch {
            when (val result = commerceRepository.productMovements(productId = productId, limit = PAGE_SIZE, offset = 0)) {
                is ApiResult.Failure -> {
                    if (activeScope() == scope) {
                        _state.update { it.copy(loading = false, error = result.message) }
                    }
                }
                is ApiResult.Success -> {
                    if (activeScope() == scope) {
                        _state.update {
                            it.copy(
                                loading = false,
                                movements = result.data,
                                canLoadMore = result.data.size >= PAGE_SIZE,
                                error = null,
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMoreHistory() {
        val current = _state.value
        if (!current.canLoadMore || current.loading || current.loadingMore || current.productId <= 0L) return
        val scope = current.scopeIdentity ?: return
        val offset = current.movements.size
        moreJob?.cancel()
        _state.update { it.copy(loadingMore = true) }
        moreJob = viewModelScope.launch {
            when (val result = commerceRepository.productMovements(current.productId, PAGE_SIZE, offset)) {
                is ApiResult.Failure -> {
                    if (activeScope() == scope) _state.update { it.copy(loadingMore = false, error = result.message) }
                }
                is ApiResult.Success -> {
                    if (activeScope() == scope && _state.value.productId == current.productId) {
                        _state.update {
                            it.copy(
                                loadingMore = false,
                                movements = it.movements + result.data,
                                canLoadMore = result.data.size >= PAGE_SIZE,
                                error = null,
                            )
                        }
                    }
                }
            }
        }
    }

    fun setPrimaryImage(productId: Long, dataUrl: String, altText: String) {
        val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        val scope = user.scopeIdentity
        if (!Roles.isAdmin(user.role) && Roles.normalize(user.role) != Roles.SECRETARY) {
            _state.update { it.copy(imageError = "Seuls les Administrateurs et Secrétaires peuvent modifier l'image.") }
            return
        }
        imageJob?.cancel()
        _state.update { it.copy(imageSaving = true, imageError = null) }
        imageJob = viewModelScope.launch {
            when (val result = commerceRepository.addPrimaryProductImage(productId, dataUrl, altText)) {
                is ApiResult.Failure -> {
                    if (activeScope() == scope) _state.update { it.copy(imageSaving = false, imageError = result.message) }
                }
                is ApiResult.Success -> {
                    if (activeScope() == scope && result.data.url.isNotBlank()) {
                        _state.update {
                            it.copy(imageSaving = false, imageError = null, updatedImageUrl = result.data.url)
                        }
                    } else if (activeScope() == scope) {
                        _state.update { it.copy(imageSaving = false, imageError = "Le serveur n'a pas retourné l'image mise à jour.") }
                    }
                }
            }
        }
    }

    private fun activeScope(): String? =
        (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity

    private companion object {
        const val PAGE_SIZE = 50
    }
}
