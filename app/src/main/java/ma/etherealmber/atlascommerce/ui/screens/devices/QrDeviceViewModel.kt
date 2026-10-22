package ma.etherealmber.atlascommerce.ui.screens.devices

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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.model.QrSessionStatusDto
import ma.etherealmber.atlascommerce.data.model.TrustedDeviceDto
import ma.etherealmber.atlascommerce.data.repo.AuthRepository
import javax.inject.Inject

@HiltViewModel
class QrDeviceViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val serverConfig: ServerConfig,
) : ViewModel() {

    enum class Step {
        IDLE,
        WAITING_PC_APPROVAL,
        CONFIRM_PC_BIND,
        COMPLETED,
    }

    data class UiState(
        val step: Step = Step.IDLE,
        val activeToken: String = "",
        val manualInput: String = "",
        val loading: Boolean = false,
        val statusDto: QrSessionStatusDto? = null,
        val statusMessage: String? = null,
        val error: String? = null,
        val devices: List<TrustedDeviceDto> = emptyList(),
        val devicesLoading: Boolean = false,
        val revokingId: String? = null,
        val cameraEnabled: Boolean = true,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val serverUrl: String? get() = serverConfig.selection.value?.baseUrl

    private var pollJob: Job? = null

    init {
        if (sessionManager.state.value is SessionState.Active) {
            loadDevices()
        }
    }

    fun setCameraEnabled(enabled: Boolean) {
        _state.update { it.copy(cameraEnabled = enabled) }
    }

    fun onManualInputChange(value: String) {
        _state.update { it.copy(manualInput = value, error = null) }
    }

    fun consumeInitialToken(token: String?) {
        val clean = QrTokenParser.extractQrToken(token) ?: return
        if (clean != _state.value.activeToken) {
            handleRawScan(clean)
        }
    }

    fun submitManual() {
        val raw = _state.value.manualInput.trim()
        if (raw.isBlank()) return
        handleRawScan(raw)
    }

    fun handleRawScan(raw: String) {
        if (_state.value.loading || _state.value.step != Step.IDLE) return

        if (QrTokenParser.looksLikeProductBarcode(raw)) {
            _state.update {
                it.copy(
                    error = "Ceci est un code-barres produit ($raw). " +
                        "Cet écran sert uniquement aux QR codes de sécurité PC — utilisez l'onglet Scan pour le stock.",
                )
            }
            return
        }

        val token = QrTokenParser.extractQrToken(raw)
        if (token == null) {
            _state.update {
                it.copy(
                    error = "QR code non reconnu. Scannez le QR affiché sur votre ordinateur Atlas Commerce.",
                )
            }
            return
        }

        val signedIn = sessionManager.state.value is SessionState.Active
        if (signedIn) {
            previewPcBind(token)
        } else {
            claimPhoneLogin(token)
        }
    }

    private fun previewPcBind(token: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    activeToken = token,
                    error = null,
                    statusMessage = "Vérification de la demande du poste PC…",
                )
            }
            when (
                val res = authRepository.qrPhoneBind(
                    token = token,
                    fingerprint = sessionManager.fingerprint,
                    userAgent = sessionManager.deviceUserAgent(),
                    confirm = false,
                )
            ) {
                is ApiResult.Failure -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            step = Step.IDLE,
                            activeToken = "",
                            error = res.message,
                        )
                    }
                }

                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            step = Step.CONFIRM_PC_BIND,
                            statusDto = res.data,
                            statusMessage = res.data.message ?: "Confirmez-vous la connexion de ce poste PC ?",
                        )
                    }
                }
            }
        }
    }

    fun confirmPcAuthorization() {
        val token = _state.value.activeToken
        if (token.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (
                val res = authRepository.qrPhoneBind(
                    token = token,
                    fingerprint = sessionManager.fingerprint,
                    userAgent = sessionManager.deviceUserAgent(),
                    confirm = true,
                )
            ) {
                is ApiResult.Failure -> {
                    _state.update { it.copy(loading = false, error = res.message) }
                }

                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            step = Step.COMPLETED,
                            statusDto = res.data,
                            statusMessage = "Poste PC autorisé avec succès.",
                        )
                    }
                    _messages.tryEmit("Poste PC connecté à votre session Atlas.")
                    loadDevices()
                }
            }
        }
    }

    fun denyPcAuthorization() {
        val token = _state.value.activeToken
        if (token.isBlank()) {
            resetScan()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            authRepository.qrDeny(token)
            _state.update {
                it.copy(
                    loading = false,
                    step = Step.IDLE,
                    activeToken = "",
                    statusDto = null,
                    statusMessage = null,
                )
            }
            _messages.tryEmit("Demande de connexion PC refusée.")
        }
    }

    private fun claimPhoneLogin(token: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    activeToken = token,
                    error = null,
                    statusMessage = "Association avec la session ouverte sur l'ordinateur…",
                )
            }
            val claim = authRepository.qrClaim(
                token = token,
                fingerprint = sessionManager.fingerprint,
                userAgent = sessionManager.deviceUserAgent(),
            )
            if (claim is ApiResult.Failure) {
                _state.update {
                    it.copy(
                        loading = false,
                        step = Step.IDLE,
                        activeToken = "",
                        error = claim.message,
                    )
                }
                return@launch
            }

            val initialStatus = (claim as ApiResult.Success).data
            _state.update {
                it.copy(
                    loading = false,
                    step = Step.WAITING_PC_APPROVAL,
                    statusDto = initialStatus,
                    statusMessage = "Validez maintenant la connexion sur l'écran de votre ordinateur…",
                )
            }

            if (initialStatus.status.equals("approved", ignoreCase = true)) {
                finalizePhoneLogin(token)
                return@launch
            }

            var attempts = 0
            while (isActive && attempts < 45) {
                delay(2_000L)
                attempts++
                when (val poll = authRepository.qrStatus(token)) {
                    is ApiResult.Success -> {
                        val s = poll.data
                        _state.update { it.copy(statusDto = s) }
                        when (s.status.lowercase()) {
                            "approved" -> {
                                finalizePhoneLogin(token)
                                return@launch
                            }
                            "denied", "expired", "consumed" -> {
                                _state.update {
                                    it.copy(
                                        step = Step.IDLE,
                                        activeToken = "",
                                        error = "Session QR ${s.status} — scannez un nouveau QR code.",
                                    )
                                }
                                return@launch
                            }
                        }
                    }

                    is ApiResult.Failure -> Unit
                }
            }
            _state.update {
                it.copy(
                    step = Step.IDLE,
                    activeToken = "",
                    error = "Délai d'approbation dépassé — régénérez le QR sur le PC.",
                )
            }
        }
    }

    private suspend fun finalizePhoneLogin(token: String) {
        _state.update { it.copy(loading = true, statusMessage = "Ouverture de votre session Atlas…") }
        when (val done = authRepository.qrComplete(token, sessionManager.fingerprint)) {
            is ApiResult.Failure -> {
                _state.update {
                    it.copy(
                        loading = false,
                        step = Step.IDLE,
                        activeToken = "",
                        error = done.message,
                    )
                }
            }

            is ApiResult.Success -> {
                when (val saved = sessionManager.completeQrLogin(done.data)) {
                    is ApiResult.Failure -> _state.update {
                        it.copy(
                            loading = false,
                            step = Step.IDLE,
                            activeToken = "",
                            error = saved.message,
                        )
                    }

                    is ApiResult.Success -> {
                        _state.update {
                            it.copy(
                                loading = false,
                                step = Step.COMPLETED,
                                statusMessage = "Session mobile connectée !",
                            )
                        }
                        _messages.tryEmit("Connecté à Atlas Commerce.")
                    }
                }
            }
        }
    }

    fun resetScan() {
        pollJob?.cancel()
        _state.update {
            it.copy(
                step = Step.IDLE,
                activeToken = "",
                manualInput = "",
                statusDto = null,
                statusMessage = null,
                error = null,
                loading = false,
            )
        }
    }

    fun loadDevices() {
        viewModelScope.launch {
            _state.update { it.copy(devicesLoading = true) }
            when (val res = authRepository.devices()) {
                is ApiResult.Failure -> _state.update { it.copy(devicesLoading = false) }
                is ApiResult.Success -> _state.update {
                    it.copy(devicesLoading = false, devices = res.data)
                }
            }
        }
    }

    fun revokeDevice(device: TrustedDeviceDto) {
        val targetId = device.revokeId
        if (targetId.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(revokingId = targetId) }
            when (val res = authRepository.revokeDevice(targetId)) {
                is ApiResult.Failure -> {
                    _state.update { it.copy(revokingId = null) }
                    _messages.tryEmit(res.message)
                }

                is ApiResult.Success -> {
                    _state.update { it.copy(revokingId = null) }
                    _messages.tryEmit("${device.friendlyLabel} révoqué.")
                    if (device.current) {
                        sessionManager.signOut("Cet appareil a été déconnecté.")
                    } else {
                        loadDevices()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
