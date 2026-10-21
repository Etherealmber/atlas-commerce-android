package ma.etherealmber.atlascommerce.ui.screens.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ProbeReport
import ma.etherealmber.atlascommerce.core.net.ServerProbe
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import javax.inject.Inject

@HiltViewModel
class ServerSetupViewModel @Inject constructor(
    private val serverConfig: ServerConfig,
    private val serverProbe: ServerProbe,
) : ViewModel() {

    data class UiState(
        val input: String = "",
        val current: String? = null,
        val testing: Boolean = false,
        val saving: Boolean = false,
        val report: ProbeReport? = null,
        val error: String? = null,
    ) {
        val normalizedInput: String? get() = ServerUrl.normalize(input)
        val canContinue: Boolean get() = !testing && !saving && !input.isBlank()
    }

    private val _state = MutableStateFlow(
        UiState(
            input = serverConfig.selection.value?.baseUrl ?: ServerUrl.PRODUCTION,
            current = serverConfig.selection.value?.baseUrl,
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onInputChange(value: String) {
        _state.update { it.copy(input = value, report = null, error = null) }
    }

    fun choosePreset(url: String) {
        _state.update { it.copy(input = url, report = null, error = null) }
        test()
    }

    fun test() {
        if (_state.value.testing) return
        val raw = _state.value.input.trim()
        if (raw.isBlank()) {
            _state.update { it.copy(error = "Saisissez une adresse de serveur.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(testing = true, error = null) }
            val report = serverProbe.probe(raw)
            _state.update { it.copy(testing = false, report = report) }
        }
    }

    /**
     * Saves the address and lets the root composable move on to the login screen.
     * The probe is not mandatory: a firewall may block `/api/health` while the
     * rest of the API still works, so we only validate the address format.
     */
    fun continueToLogin(onDone: () -> Unit) {
        if (_state.value.saving) return
        val raw = _state.value.input.trim()
        if (raw.isBlank()) {
            _state.update { it.copy(error = "Saisissez une adresse de serveur.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            val result = serverConfig.select(raw)
            result.fold(
                onSuccess = { selection ->
                    _state.update { it.copy(saving = false, current = selection.baseUrl) }
                    onDone()
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            saving = false,
                            error = throwable.message ?: "Adresse de serveur invalide.",
                        )
                    }
                },
            )
        }
    }
}
