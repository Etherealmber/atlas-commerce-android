package ma.etherealmber.atlascommerce.ui.screens.sale

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
import ma.etherealmber.atlascommerce.data.CartLine
import ma.etherealmber.atlascommerce.data.CartStore
import ma.etherealmber.atlascommerce.data.model.CreatedInvoiceDto
import ma.etherealmber.atlascommerce.data.model.QuickSaleItemDto
import ma.etherealmber.atlascommerce.data.model.QuickSaleRequest
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SaleViewModel @Inject constructor(
    private val commerceRepository: CommerceRepository,
    private val cartStore: CartStore,
) : ViewModel() {

    data class UiState(
        val clientName: String = "",
        val clientPhone: String = "",
        val note: String = "",
        val submitting: Boolean = false,
        val error: String? = null,
        val created: CreatedInvoiceDto? = null,
        val currency: String = "MAD",
    ) {
        val canSubmit: Boolean get() = !submitting && clientName.isNotBlank()
    }

    val lines: StateFlow<List<CartLine>> = cartStore.lines

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        loadCurrency()
    }

    private fun loadCurrency() {
        viewModelScope.launch {
            val result = commerceRepository.settings()
            if (result is ApiResult.Success) {
                val currency = result.data.currency?.takeIf { it.isNotBlank() } ?: return@launch
                _state.update { it.copy(currency = currency) }
            }
        }
    }

    fun onClientNameChange(value: String) {
        _state.update { it.copy(clientName = value, error = null) }
    }

    fun onClientPhoneChange(value: String) {
        _state.update { it.copy(clientPhone = value, error = null) }
    }

    fun onNoteChange(value: String) {
        _state.update { it.copy(note = value) }
    }

    fun increment(productId: Long) = cartStore.increment(productId)

    fun decrement(productId: Long) = cartStore.decrement(productId)

    fun remove(productId: Long) = cartStore.remove(productId)

    fun clearCart() {
        cartStore.clear()
        _state.update { it.copy(error = null) }
    }

    fun dismissCreated() {
        _state.update { it.copy(created = null) }
    }

    fun submit() {
        val current = _state.value
        if (current.submitting) return
        val snapshot = cartStore.snapshot()
        if (snapshot.isEmpty()) {
            _state.update { it.copy(error = "Le panier est vide.") }
            return
        }
        if (current.clientName.isBlank()) {
            _state.update { it.copy(error = "Indiquez le nom du client pour le ticket.") }
            return
        }

        val request = QuickSaleRequest(
            clientName = current.clientName.trim(),
            clientPhone = current.clientPhone.trim().takeIf { it.isNotBlank() },
            issueDate = LocalDate.now().toString(),
            notes = current.note.trim().takeIf { it.isNotBlank() }
                ?: "Vente rapide depuis l'application Android · sans TVA · à encaisser.",
            items = snapshot.map { line ->
                QuickSaleItemDto(
                    productId = line.product.id,
                    productName = line.product.name,
                    quantity = line.quantity,
                    unitPrice = line.product.price,
                    listPrice = line.product.price,
                    taxRate = 0.0,
                )
            },
        )

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            when (val result = commerceRepository.createQuickSale(request)) {
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.message)
                }

                is ApiResult.Success -> {
                    cartStore.clear()
                    _state.update {
                        it.copy(
                            submitting = false,
                            error = null,
                            created = result.data,
                            clientName = "",
                            clientPhone = "",
                            note = "",
                        )
                    }
                    _messages.tryEmit("Ticket créé — à encaisser depuis le web.")
                }
            }
        }
    }
}
