package ma.etherealmber.atlascommerce.ui.screens.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.data.model.ClientDto
import ma.etherealmber.atlascommerce.data.model.InvoiceDto
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import javax.inject.Inject

@HiltViewModel
class ClientsViewModel @Inject constructor(private val repository: CommerceRepository) : ViewModel() {
    data class UiState(
        val clients: List<ClientDto> = emptyList(),
        val invoices: List<InvoiceDto> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val currency: String = "MAD",
    )
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val clientsCall = async { repository.clients() }
            val invoicesCall = async { repository.invoices("all") }
            val clients = clientsCall.await()
            val invoices = invoicesCall.await()
            val clientList = (clients as? ApiResult.Success)?.data
            val invoiceList = (invoices as? ApiResult.Success)?.data
            _state.update { it.copy(
                clients = clientList ?: it.clients,
                invoices = invoiceList ?: it.invoices,
                loading = false,
                error = if (clientList == null) (clients as? ApiResult.Failure)?.message ?: "Clients indisponibles" else null,
            ) }
        }
    }
}
