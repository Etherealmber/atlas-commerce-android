package ma.etherealmber.atlascommerce.ui.screens.scan

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import dagger.hilt.android.lifecycle.HiltViewModel
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
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.CartStore
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val commerceRepository: CommerceRepository,
    private val cartStore: CartStore,
    private val sessionManager: SessionManager,
) : ViewModel() {

    data class UiState(
        val lookingUp: Boolean = false,
        val lastCode: String? = null,
        val product: ProductDto? = null,
        val notFound: Boolean = false,
        val added: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var lastHandledCode: String? = null
    private var lastHandledAt: Long = 0L

    init {
        viewModelScope.launch {
            sessionManager.state
                .map { (it as? SessionState.Active)?.user?.username }
                .distinctUntilChanged()
                .collect { username ->
                    if (username != null) reset()
                }
        }
    }

    /** Called by the camera analyser; duplicate reads are throttled. */
    fun onBarcode(raw: String) {
        val code = raw.trim()
        if (code.isBlank()) return
        val now = SystemClock.elapsedRealtime()
        if (code == lastHandledCode && now - lastHandledAt < THROTTLE_MS) return
        lastHandledCode = code
        lastHandledAt = now

        _state.update {
            it.copy(
                lastCode = code,
                lookingUp = true,
                product = null,
                notFound = false,
                added = false,
                error = null,
            )
        }

        // Scanning B right after A must not let A's (slower) answer win.
        lookupJob?.cancel()
        lookupJob = viewModelScope.launch {
            when (val result = commerceRepository.scan(code)) {
                is ApiResult.Failure -> _state.update {
                    when (result.code) {
                        // Not a product yet: the UI offers to create it instead.
                        "NOT_FOUND" -> it.copy(lookingUp = false, notFound = true, error = null)
                        else -> it.copy(lookingUp = false, error = result.message)
                    }
                }

                is ApiResult.Success -> {
                    val product = result.data.displayName?.takeIf { it.isNotBlank() }
                        ?.let { merged -> result.data.copy(name = merged) }
                        ?: result.data
                    _state.update {
                        it.copy(
                            lookingUp = false,
                            product = product,
                            notFound = false,
                            error = null,
                        )
                    }
                }
            }
        }
    }

    fun addToCart(quantity: Double = 1.0) {
        val product = _state.value.product ?: return
        cartStore.add(product, quantity)
        _state.update { it.copy(added = true) }
        _messages.tryEmit("${product.name} · ajouté au panier")
    }

    fun retry() {
        val code = _state.value.lastCode ?: return
        lastHandledCode = null
        onBarcode(code)
    }

    fun reset() {
        lastHandledCode = null
        _state.value = UiState()
    }

    private var lookupJob: Job? = null

    private companion object {
        const val THROTTLE_MS = 2_500L
    }
}
