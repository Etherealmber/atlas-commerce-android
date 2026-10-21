package ma.etherealmber.atlascommerce.ui.screens.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ApiRunner
import ma.etherealmber.atlascommerce.data.CartStore
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val commerceRepository: CommerceRepository,
    private val cartStore: CartStore,
) : ViewModel() {

    data class UiState(
        val query: String = "",
        val products: List<ProductDto> = emptyList(),
        val loading: Boolean = true,
        val refreshing: Boolean = false,
        val lowStockOnly: Boolean = false,
        val offline: Boolean = false,
        val error: String? = null,
        val currency: String = "MAD",
    ) {
        val visibleProducts: List<ProductDto>
            get() = if (lowStockOnly) products.filter { it.isLowStock || it.isOutOfStock } else products

        val lowStockCount: Int
            get() = products.count { it.isLowStock || it.isOutOfStock }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var searchJob: Job? = null

    init {
        fetchState(query = "", initial = true)
        loadCurrency()
    }

    private fun loadCurrency() {
        viewModelScope.launch {
            val result = commerceRepository.settings()
            if (result is ApiResult.Success) {
                val currency = result.data.currency?.takeIf { it.isNotBlank() }
                if (currency != null) _state.update { it.copy(currency = currency) }
            }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            fetchState(query = value, initial = false)
        }
    }

    fun refresh() {
        fetchState(query = _state.value.query, initial = false)
    }

    fun toggleLowStockOnly() {
        _state.update { it.copy(lowStockOnly = !it.lowStockOnly) }
    }

    fun addToCart(product: ProductDto, quantity: Double = 1.0) {
        if (quantity <= 0.0) return
        cartStore.add(product, quantity)
        _messages.tryEmit("${product.name} · ajouté au panier")
    }

    private fun fetchState(query: String, initial: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = initial && it.products.isEmpty(),
                    refreshing = !initial,
                    error = null,
                )
            }
            when (val result = commerceRepository.products(query = query, limit = 300)) {
                is ApiResult.Failure -> {
                    val offline = result.code == ApiRunner.CODE_NETWORK
                    val cached = if (offline) commerceRepository.cachedProducts().filter { matches(it, query) } else emptyList()
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            offline = offline,
                            products = if (offline && it.products.isEmpty()) cached else it.products,
                            error = if (offline) null else result.message,
                        )
                    }
                    if (offline) _messages.tryEmit("Hors ligne — catalogue enregistré sur l'appareil.")
                }

                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        offline = false,
                        error = null,
                        products = result.data,
                    )
                }
            }
        }
    }

    private fun matches(product: ProductDto, query: String): Boolean {
        if (query.isBlank()) return true
        val needle = query.trim().lowercase()
        return listOfNotNull(product.name, product.sku, product.barcode, product.category)
            .any { it.lowercase().contains(needle) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 280L
    }
}
