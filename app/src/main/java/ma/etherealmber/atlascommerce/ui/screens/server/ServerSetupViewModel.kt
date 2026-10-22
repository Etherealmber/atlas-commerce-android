package ma.etherealmber.atlascommerce.ui.screens.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ProbeReport
import ma.etherealmber.atlascommerce.core.net.ServerProbe
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.SavedServerEntry
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.session.SessionManager
import javax.inject.Inject

@HiltViewModel
class ServerSetupViewModel @Inject constructor(
    private val serverConfig: ServerConfig,
    private val serverProbe: ServerProbe,
    private val sessionManager: SessionManager,
) : ViewModel() {

    data class UiState(
        val input: String = "",
        val label: String = "",
        val rememberServer: Boolean = true,
        val current: String? = null,
        val testing: Boolean = false,
        val saving: Boolean = false,
        val report: ProbeReport? = null,
        val error: String? = null,
    ) {
        val normalizedInput: String? get() = ServerUrl.normalize(input)
        val canContinue: Boolean get() = !testing && !saving && !input.isBlank()
    }

    private var probeJob: Job? = null

    val savedServers: StateFlow<List<SavedServerEntry>> = serverConfig.savedServers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _state = MutableStateFlow(
        UiState(
            input = serverConfig.selection.value?.baseUrl.orEmpty(),
            current = serverConfig.selection.value?.baseUrl,
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onInputChange(value: String) {
        probeJob?.cancel()
        _state.update { it.copy(input = value, report = null, testing = false, error = null) }
    }

    fun onLabelChange(value: String) {
        _state.update { it.copy(label = value) }
    }

    fun onRememberServerChange(value: Boolean) {
        _state.update { it.copy(rememberServer = value) }
    }

    fun choosePreset(url: String, presetLabel: String = "") {
        _state.update {
            it.copy(
                input = url,
                label = presetLabel.ifBlank { it.label },
                report = null,
                error = null,
            )
        }
        test()
    }

    fun forgetServer(entry: SavedServerEntry) {
        viewModelScope.launch {
            serverConfig.forgetSavedServer(entry.url)
            if (_state.value.input.equals(entry.url, ignoreCase = true)) {
                _state.update { it.copy(input = "", label = "", report = null, error = null) }
            }
        }
    }

    fun test() {
        if (_state.value.testing) return
        val raw = _state.value.input.trim()
        if (raw.isBlank()) {
            _state.update { it.copy(error = "Saisissez une adresse de serveur.") }
            return
        }
        probeJob?.cancel()
        probeJob = viewModelScope.launch {
            _state.update { it.copy(testing = true, error = null) }
            val report = serverProbe.probe(raw)
            if (_state.value.input.trim() == raw) {
                _state.update { it.copy(testing = false, report = report) }
            }
        }
    }

    /**
     * Validates and switches server, signing out any session from a different server
     * first so zero user or company data can ever cross servers.
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
            val previousUrl = serverConfig.selection.value?.baseUrl
            val targetNormalized = ServerUrl.normalize(raw)
            if (previousUrl != null && !previousUrl.equals(targetNormalized, ignoreCase = true)) {
                sessionManager.signOut()
            }
            val result = serverConfig.select(
                rawUrl = raw,
                label = _state.value.label,
                rememberServer = _state.value.rememberServer,
            )
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
