package ma.etherealmber.atlascommerce.ui.screens.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.security.DeviceIdentityUnavailableException
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.model.QrSessionStatusDto
import ma.etherealmber.atlascommerce.data.model.TrustedDeviceDto
import ma.etherealmber.atlascommerce.data.repo.AuthRepository
import javax.inject.Inject

enum class QrDeviceMode { AUTHORIZE_PC, SIGN_IN_PHONE }

@HiltViewModel
class QrDeviceViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val serverConfig: ServerConfig,
    preferences: AtlasPreferences,
) : ViewModel() {

    enum class Step {
        IDLE,
        WAITING_PC_APPROVAL,
        CONFIRM_PC_BIND,
        DESKTOP_V2_CONSENT,
        COMPLETED,
    }

    data class DesktopV2Approval(
        val requestId: String,
        val origin: String,
        val username: String,
        val deviceLabel: String,
        val pairingCode: String,
        val consentNonce: String,
        val expiresAt: String,
        val consentExpiresAt: String,
        val sessionIdentity: String,
        val bearerSnapshot: String,
        val serverSnapshot: String,
        val comparisonConfirmed: Boolean = false,
    )

    data class UiState(
        val step: Step = Step.IDLE,
        val desktopV2: DesktopV2Approval? = null,
        val activeToken: String = "",
        val manualInput: String = "",
        val loading: Boolean = false,
        val statusDto: QrSessionStatusDto? = null,
        val statusMessage: String? = null,
        val error: String? = null,
        val devices: List<TrustedDeviceDto> = emptyList(),
        val devicesLoading: Boolean = false,
        val devicesLoadError: String? = null,
        val revokingId: String? = null,
        /**
         * Camera is OFF (`false`) on entering Scan Device unless the user starts it explicitly.
         */
        val cameraEnabled: Boolean = false,
    )

    val autoOpenCamera: StateFlow<Boolean> = preferences.autoOpenCamera
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val scanHapticEnabled: StateFlow<Boolean> = preferences.scanHapticEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val scanSoundEnabled: StateFlow<Boolean> = preferences.scanSoundEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _scanFeedbackEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val scanFeedbackEvents: SharedFlow<Unit> = _scanFeedbackEvents.asSharedFlow()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val serverUrl: String? get() = serverConfig.selection.value?.baseUrl

    private fun deviceFingerprintOrNull(): String? = try {
        sessionManager.fingerprint
    } catch (_: DeviceIdentityUnavailableException) {
        _state.update {
            it.copy(
                loading = false,
                step = Step.IDLE,
                activeToken = "",
                error = "L'identité sécurisée de cet appareil est temporairement indisponible. Réessayez dans quelques instants.",
            )
        }
        null
    }

    private var pollJob: Job? = null
    private var desktopDecisionGate = SingleUseDecisionGate()
    private var desktopPrepareJob: Job? = null
    private var initializedRoute: Pair<String?, QrDeviceMode>? = null
    private var devicesLoadGeneration = 0L

    init {
        viewModelScope.launch {
            serverConfig.selection.map { it?.baseUrl }.distinctUntilChanged().collect { selected ->
                val pending = _state.value.desktopV2
                if (pending != null && ServerUrl.normalize(selected) != ServerUrl.normalize(pending.serverSnapshot)) {
                    _state.update {
                        it.copy(
                            step = Step.IDLE, desktopV2 = null, loading = false,
                            error = "Le serveur a changé. La demande d'approbation a été abandonnée.",
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            sessionManager.state
                .map { (it as? SessionState.Active)?.user?.scopeIdentity }
                .distinctUntilChanged()
                .collect { scopeIdentity ->
                    devicesLoadGeneration += 1
                    val pendingV2 = _state.value.desktopV2
                    if (pendingV2 != null && pendingV2.sessionIdentity != scopeIdentity) {
                        _state.update {
                            it.copy(
                                step = Step.IDLE, desktopV2 = null, loading = false,
                                error = "Le compte a changé. La demande d'approbation a été abandonnée.",
                            )
                        }
                    }
                    _state.update { it.copy(devices = emptyList(), devicesLoading = false, devicesLoadError = null, revokingId = null) }
                    if (scopeIdentity != null) loadDevices()
                }
        }
    }

    fun setCameraEnabled(enabled: Boolean) {
        _state.update { it.copy(cameraEnabled = enabled) }
    }

    fun onManualInputChange(value: String) {
        _state.update { it.copy(manualInput = value, error = null) }
    }

    /** Idempotent for a retained ViewModel; a new ViewModel after process death consumes again. */
    fun clearInitializedRoute() {
        initializedRoute = null
    }

    fun initializeRoute(token: String?, mode: QrDeviceMode) {
        val route = token to mode
        if (initializedRoute == route) return
        initializedRoute = route
        resetScan()
        consumeInitialToken(token, mode)
    }

    fun consumeInitialToken(token: String?, mode: QrDeviceMode) {
        if (token != null && runCatching { java.net.URI(token).rawPath == "/login/desktop-phone" }.getOrDefault(false)) {
            handleRawScan(token, mode)
            return
        }
        val clean = QrTokenParser.extractQrToken(token) ?: return
        if (clean != _state.value.activeToken) {
            handleRawScan(clean, mode)
        }
    }

    fun submitManual(mode: QrDeviceMode) {
        if (mode != QrDeviceMode.SIGN_IN_PHONE) return
        val raw = _state.value.manualInput.trim()
        if (raw.isBlank()) return
        handleRawScan(raw, mode)
    }

    fun handleRawScan(raw: String, mode: QrDeviceMode) {
        if (_state.value.loading || _state.value.step != Step.IDLE) return

        val signedIn = sessionManager.state.value is SessionState.Active
        if (mode == QrDeviceMode.AUTHORIZE_PC && !signedIn) {
            _state.update { it.copy(error = "Connectez-vous sur ce téléphone avant d'autoriser un ordinateur.") }
            return
        }
        if (mode == QrDeviceMode.SIGN_IN_PHONE && signedIn) {
            _state.update { it.copy(error = "Déconnectez-vous avant d'utiliser le QR de connexion du téléphone.") }
            return
        }

        if (QrTokenParser.looksLikeProductBarcode(raw)) {
            _state.update {
                it.copy(
                    error = "Ceci est un code-barres produit ($raw). " +
                        "Cet écran sert uniquement aux QR codes de sécurité PC — utilisez l'onglet Scan pour le stock.",
                )
            }
            return
        }

        val desktopPath = runCatching { java.net.URI(raw).rawPath == "/login/desktop-phone" }.getOrDefault(false)
        if (desktopPath) {
            val qr = DesktopPhoneQrParser.parse(raw, serverConfig.selection.value?.baseUrl)
            if (qr == null) {
                _state.update { it.copy(error = "QR d'approbation desktop invalide ou provenant d'un autre serveur. Vérifiez le serveur HTTPS sélectionné.") }
                return
            }
            if (mode != QrDeviceMode.AUTHORIZE_PC) {
                _state.update { it.copy(error = "Ce QR approuve une connexion desktop ; utilisez le mode Autoriser un PC.") }
                return
            }
            _scanFeedbackEvents.tryEmit(Unit)
            _state.update { it.copy(cameraEnabled = false) }
            prepareDesktopV2(qr)
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

        _scanFeedbackEvents.tryEmit(Unit)
        _state.update { it.copy(cameraEnabled = false) }

        when (mode) {
            QrDeviceMode.AUTHORIZE_PC -> previewPcBind(token)
            QrDeviceMode.SIGN_IN_PHONE -> claimPhoneLogin(token)
        }
    }

    private fun prepareDesktopV2(qr: DesktopPhoneQr) {
        val active = sessionManager.state.value as? SessionState.Active
        val selected = serverConfig.selection.value?.baseUrl
        val bearer = sessionManager.authToken
        if (active == null || bearer.isNullOrBlank() || selected == null ||
            ServerUrl.normalize(active.user.serverUrl) != ServerUrl.normalize(selected)
        ) {
            _state.update { it.copy(error = "Une session Android valide sur le serveur HTTPS sélectionné est nécessaire.") }
            return
        }
        val identity = active.user.scopeIdentity
        val expectedUsername = active.user.username
        desktopPrepareJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, statusMessage = "Vérification de la demande desktop v2…") }
            when (val result = authRepository.desktopPhonePrepare(qr.origin, qr.requestId, bearer)) {
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, step = Step.IDLE, statusMessage = null, error = result.message)
                }
                is ApiResult.Success -> {
                    val data = result.data
                    val stillSameSession = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == identity &&
                        sessionManager.authToken == bearer
                    val stillSameServer = ServerUrl.normalize(serverConfig.selection.value?.baseUrl) == ServerUrl.normalize(selected)
                    val validData = data.requestId == qr.requestId && data.origin == qr.origin &&
                        data.username.equals(expectedUsername, ignoreCase = true) &&
                        data.deviceLabel.isNotBlank() && data.pairingCode.isNotBlank() &&
                        data.consentNonce.matches(Regex("[A-Fa-f0-9]{64}")) &&
                        DesktopPhoneApprovalRules.isValidWindow(data.expiresAt, data.consentExpiresAt, System.currentTimeMillis())
                    if (!stillSameSession || !stillSameServer || !validData) {
                        _state.update {
                            it.copy(
                                loading = false, step = Step.IDLE,
                                error = if (!stillSameSession) "Le compte a changé pendant la préparation. Scannez un nouveau QR."
                                else if (!stillSameServer) "Le serveur a changé pendant la préparation. Scannez un nouveau QR."
                                else "Réponse de préparation invalide ou expirée. Aucune décision n'a été envoyée.",
                            )
                        }
                        return@launch
                    }
                    desktopDecisionGate = SingleUseDecisionGate()
                    _state.update {
                        it.copy(
                            loading = false, step = Step.DESKTOP_V2_CONSENT,
                            desktopV2 = DesktopV2Approval(
                                requestId = data.requestId, origin = data.origin,
                                username = data.username, deviceLabel = data.deviceLabel,
                                pairingCode = data.pairingCode, consentNonce = data.consentNonce,
                                expiresAt = data.expiresAt, consentExpiresAt = data.consentExpiresAt,
                                sessionIdentity = identity, bearerSnapshot = bearer,
                                serverSnapshot = selected,
                            ),
                            statusMessage = null,
                        )
                    }
                }
            }
            desktopPrepareJob = null
        }
    }

    fun confirmDesktopCodeComparison(confirmed: Boolean) {
        _state.update { state -> state.copy(desktopV2 = state.desktopV2?.copy(comparisonConfirmed = confirmed)) }
    }

    /** Local cancellation deliberately sends no request; the server-side consent nonce expires. */
    fun cancelDesktopApproval() {
        if (_state.value.desktopV2 == null && desktopPrepareJob == null) return
        desktopPrepareJob?.cancel()
        desktopPrepareJob = null
        _state.update { it.copy(step = Step.IDLE, desktopV2 = null, loading = false, error = null, statusMessage = null) }
    }

    fun decideDesktopApproval(approve: Boolean) {
        val pending = _state.value.desktopV2 ?: return
        if (_state.value.loading) return
        if (!pending.comparisonConfirmed) return
        if (!desktopDecisionGate.begin(pending.comparisonConfirmed, pending.expiresAt, pending.consentExpiresAt)) {
            cancelDesktopApproval()
            _state.update { it.copy(error = "La demande ou le consentement a expiré ou a déjà été utilisé. Scannez un nouveau QR.") }
            return
        }
        val current = sessionManager.state.value as? SessionState.Active
        val server = serverConfig.selection.value?.baseUrl
        if (current?.user?.scopeIdentity != pending.sessionIdentity || sessionManager.authToken != pending.bearerSnapshot) {
            cancelDesktopApproval()
            _state.update { it.copy(error = "Le compte a changé. L'approbation a été abandonnée.") }
            return
        }
        if (ServerUrl.normalize(server) != ServerUrl.normalize(pending.serverSnapshot) ||
            ServerUrl.normalize(server) != ServerUrl.normalize(pending.origin)
        ) {
            cancelDesktopApproval()
            _state.update { it.copy(error = "Le serveur a changé. Aucun bearer n'a été envoyé à une autre origine.") }
            return
        }
        if (!DesktopPhoneApprovalRules.isValidWindow(pending.expiresAt, pending.consentExpiresAt, System.currentTimeMillis())) {
            cancelDesktopApproval()
            _state.update { it.copy(error = "La demande ou le consentement a expiré. Scannez un nouveau QR.") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.desktopPhoneDecision(
                origin = pending.origin, requestId = pending.requestId,
                nonce = pending.consentNonce, decision = if (approve) "approve" else "deny",
                bearerToken = pending.bearerSnapshot,
            )) {
                is ApiResult.Failure -> {
                    // A lost response can follow a committed one-time decision; never replay it.
                    _state.update {
                        it.copy(
                            loading = false, step = Step.IDLE, desktopV2 = null,
                            error = "Décision non confirmée (${result.code}). La demande a été effacée pour éviter un rejeu. Scannez un nouveau QR.",
                        )
                    }
                }
                is ApiResult.Success -> {
                    if (!DesktopPhoneApprovalRules.matchesResponse(approve, result.data.status)) {
                        _state.update { it.copy(loading = false, step = Step.IDLE, desktopV2 = null, error = "Réponse inattendue ; la demande a été effacée. Scannez un nouveau QR.") }
                    } else {
                        _state.update {
                            it.copy(
                                loading = false, step = Step.COMPLETED, desktopV2 = null,
                                statusMessage = if (approve) "Connexion desktop approuvée. Aucun jeton n'a été émis au téléphone."
                                else "Demande desktop refusée.",
                            )
                        }
                    }
                }
            }
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
            val fingerprint = deviceFingerprintOrNull() ?: return@launch
            when (
                val res = authRepository.qrPhoneBind(
                    token = token,
                    fingerprint = fingerprint,
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
            val fingerprint = deviceFingerprintOrNull() ?: return@launch
            when (
                val res = authRepository.qrPhoneBind(
                    token = token,
                    fingerprint = fingerprint,
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
            val fingerprint = deviceFingerprintOrNull() ?: return@launch
            val claim = authRepository.qrClaim(
                token = token,
                fingerprint = fingerprint,
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
        val fingerprint = deviceFingerprintOrNull() ?: return
        _state.update { it.copy(loading = true, statusMessage = "Ouverture de votre session Atlas…") }
        when (val done = authRepository.qrComplete(token, fingerprint)) {
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
                desktopV2 = null,
                activeToken = "",
                manualInput = "",
                statusDto = null,
                statusMessage = null,
                error = null,
                loading = false,
            )
        }
    }

    fun isCurrentDevice(device: TrustedDeviceDto): Boolean {
        val fingerprint = deviceFingerprintOrNull() ?: return device.current
        val shortFingerprint = device.fingerprint.removeSuffix("…").removeSuffix("...")
        return device.current || device.full.equals(fingerprint, ignoreCase = true) ||
            (shortFingerprint.isNotBlank() && fingerprint.startsWith(shortFingerprint, ignoreCase = true))
    }

    fun loadDevices() {
        val requestedScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity
        if (requestedScope == null) {
            devicesLoadGeneration += 1
            _state.update { it.copy(devices = emptyList(), devicesLoading = false, devicesLoadError = null) }
            return
        }
        val generation = ++devicesLoadGeneration
        viewModelScope.launch {
            if (generation != devicesLoadGeneration || (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != requestedScope) return@launch
            _state.update { it.copy(devicesLoading = true, devicesLoadError = null) }
            val result = authRepository.devices()
            if (generation != devicesLoadGeneration || (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != requestedScope) return@launch
            when (result) {
                is ApiResult.Failure -> _state.update { it.copy(devicesLoading = false, devicesLoadError = result.message) }
                is ApiResult.Success -> _state.update { it.copy(devicesLoading = false, devicesLoadError = null, devices = result.data) }
            }
        }
    }

    fun revokeDevice(device: TrustedDeviceDto) {
        val targetId = device.revokeId
        val requestedScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity
        if (targetId.isBlank() || requestedScope == null) return
        viewModelScope.launch {
            if ((sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != requestedScope) return@launch
            _state.update { it.copy(revokingId = targetId) }
            when (val res = authRepository.revokeDevice(targetId)) {
                is ApiResult.Failure -> {
                    if ((sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity == requestedScope) {
                        _state.update { it.copy(revokingId = null) }
                        _messages.tryEmit(res.message)
                    }
                }

                is ApiResult.Success -> {
                    if ((sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != requestedScope) return@launch
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
