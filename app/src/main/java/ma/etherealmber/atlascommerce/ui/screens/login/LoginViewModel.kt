package ma.etherealmber.atlascommerce.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.BuildConfig
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.security.AndroidPasskeyCredentials
import ma.etherealmber.atlascommerce.core.security.AndroidPasskeyOrigin
import ma.etherealmber.atlascommerce.core.security.DeviceIdentity
import ma.etherealmber.atlascommerce.core.persist.SavedAccountEntry
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.session.LoginOutcome
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.data.model.supportsAndroid
import ma.etherealmber.atlascommerce.data.repo.AuthRepository
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val serverConfig: ServerConfig,
    private val preferences: AtlasPreferences,
    private val authRepository: AuthRepository,
    private val deviceIdentity: DeviceIdentity,
    @ApplicationContext private val applicationContext: Context,
) : ViewModel() {

    enum class Step { CREDENTIALS, OTP }

    data class UiState(
        val username: String = "",
        val password: String = "",
        val otp: String = "",
        val rememberAccount: Boolean = true,
        val step: Step = Step.CREDENTIALS,
        val loading: Boolean = false,
        val passkeyAvailable: Boolean = false,
        val passkeyError: String? = null,
        val error: String? = null,
        val info: String? = null,
    ) {
        val canSubmit: Boolean
            get() = !loading && username.isNotBlank() && when (step) {
                Step.CREDENTIALS -> password.isNotBlank()
                Step.OTP -> otp.length >= 4
            }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val server = serverConfig.selection

    /** Saved accounts strictly scoped ONLY to the currently selected server URL. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val savedAccounts: StateFlow<List<SavedAccountEntry>> = serverConfig.selection
        .flatMapLatest { sel -> preferences.savedAccountsForServer(sel?.baseUrl) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            serverConfig.selection.collectLatest { selectedServer ->
                _state.update { it.copy(passkeyAvailable = false) }
                selectedServer?.baseUrl?.let { refreshPasskeyCapabilityFor(it) }
            }
        }
    }

    fun refreshPasskeyCapability() {
        val selectedBase = serverConfig.selection.value?.baseUrl ?: run {
            _state.update { it.copy(passkeyAvailable = false) }
            AtlasLogBuffer.log("PASSKEY", "Login capability refresh skipped: no selected server", "WARN")
            return
        }
        viewModelScope.launch { refreshPasskeyCapabilityFor(selectedBase) }
    }

    private suspend fun refreshPasskeyCapabilityFor(selectedBase: String) {
        val selectedOrigin = ServerUrl.origin(selectedBase)
        val result = authRepository.webAuthnCapabilities()
        if (!serverConfig.selection.value?.baseUrl.equals(selectedBase, ignoreCase = true)) return
        val capability = (result as? ApiResult.Success)?.data
        val installedOrigins = AndroidPasskeyOrigin.forInstalledApp(applicationContext)
        val android = capability?.android
        val advertisedOrigins = listOfNotNull(android?.origin, android?.originLegacy)
        val originConfigured = advertisedOrigins.isNotEmpty()
        val signingOriginMatch = advertisedOrigins.any(installedOrigins::contains)
        val selectedOriginAllowed = selectedOrigin != null && capability?.origins?.contains(selectedOrigin) == true
        val packageMatch = android?.packageName == BuildConfig.APPLICATION_ID
        val available = capability?.supportsAndroid(
            BuildConfig.APPLICATION_ID,
            installedOrigins,
            selectedOrigin,
        ) == true
        when (result) {
            is ApiResult.Success -> AtlasLogBuffer.log(
                "PASSKEY",
                "Login capability: enabled=${capability?.enabled}; androidEnabled=${android?.enabled}; packageMatch=$packageMatch; selectedOriginAllowed=$selectedOriginAllowed; androidOriginConfigured=$originConfigured; installedSigningOriginMatch=$signingOriginMatch; available=$available",
            )
            is ApiResult.Failure -> AtlasLogBuffer.log(
                "PASSKEY",
                "Login capability request failed: code=${result.code}; http=${result.httpStatus ?: "none"}; message=${result.message}",
                "ERROR",
            )
        }
        _state.update { it.copy(passkeyAvailable = available) }
    }

    fun onUsernameChange(value: String) {
        _state.update { it.copy(username = value, error = null, passkeyError = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null, passkeyError = null) }
    }

    fun onOtpChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _state.update { it.copy(otp = digits, error = null, passkeyError = null) }
    }

    fun onRememberAccountChange(value: Boolean) {
        _state.update { it.copy(rememberAccount = value) }
    }

    fun signInWithSavedAccount(entry: SavedAccountEntry) {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val res = sessionManager.switchToSavedAccount(entry)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, error = null) }
                is ApiResult.Failure -> _state.update {
                    it.copy(
                        loading = false,
                        username = entry.username,
                        error = res.message,
                    )
                }
            }
        }
    }

    fun forgetSavedAccount(entry: SavedAccountEntry) {
        viewModelScope.launch {
            sessionManager.forgetSavedAccount(entry.serverUrl, entry.username)
        }
    }

    fun signInWithPasskey(context: Context) {
        val current = _state.value
        if (current.loading || !current.passkeyAvailable || current.step != Step.CREDENTIALS) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, passkeyError = null, info = null) }
            AtlasLogBuffer.log("PASSKEY", "Login flow started; usernameProvided=${current.username.isNotBlank()}")
            try {
                val selectedServerOrigin = ServerUrl.origin(serverConfig.selection.value?.baseUrl)
                AtlasLogBuffer.log("PASSKEY", "Login selected origin=${selectedServerOrigin ?: "invalid"}")
                if (selectedServerOrigin == null) {
                    reportPasskeyFailure("L'origine du serveur sélectionné est invalide.")
                    _state.update { it.copy(passkeyAvailable = false) }
                    return@launch
                }
                if (!supportsSelectedServerPasskeyOrigin(selectedServerOrigin)) {
                    AtlasLogBuffer.log("PASSKEY", "Login aborted: selected server no longer advertises Android passkeys", "ERROR")
                    reportPasskeyFailure("Les passkeys Android ne sont pas activées sur ce serveur.")
                    _state.update { it.copy(passkeyAvailable = false) }
                    return@launch
                }
                AtlasLogBuffer.log("PASSKEY", "Requesting login options")
                when (val options = authRepository.webAuthnLoginOptions(current.username.trim().ifBlank { null })) {
                    is ApiResult.Failure -> {
                        logPasskeyFailure("login-options", options)
                        reportPasskeyFailure(formatPasskeyFailure(options))
                    }
                    is ApiResult.Success -> {
                        AtlasLogBuffer.log("PASSKEY", "Login options received; credential JSON is intentionally not logged")
                        if (!supportsSelectedServerPasskeyOrigin(selectedServerOrigin)) {
                            reportPasskeyFailure("Les passkeys Android ne sont plus disponibles sur ce serveur.")
                            _state.update { it.copy(passkeyAvailable = false) }
                            return@launch
                        }
                        AtlasLogBuffer.log("PASSKEY", "Opening Android Credential Manager for login")
                        val credentialJson = AndroidPasskeyCredentials.authenticate(context, options.data, selectedServerOrigin)
                        AtlasLogBuffer.log("PASSKEY", "Credential Manager returned an assertion; sending it to selected Worker")
                        when (val verified = authRepository.verifyWebAuthnLogin(credentialJson, deviceIdentity.fingerprint)) {
                            is ApiResult.Failure -> {
                                logPasskeyFailure("login-verify", verified)
                                reportPasskeyFailure(formatPasskeyFailure(verified))
                            }
                            is ApiResult.Success -> when (val signedIn = sessionManager.signInWithPasskey(verified.data)) {
                                is ApiResult.Failure -> {
                                    logPasskeyFailure("session-finalize", signedIn)
                                    reportPasskeyFailure(formatPasskeyFailure(signedIn))
                                }
                                is ApiResult.Success -> {
                                    AtlasLogBuffer.log("PASSKEY", "Login completed and session accepted")
                                    _state.update { it.copy(loading = false, password = "", error = null, passkeyError = null, info = null) }
                                }
                            }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                AtlasLogBuffer.log("PASSKEY", "Login coroutine cancelled")
                throw cancelled
            } catch (error: Exception) {
                val detail = "${error.javaClass.simpleName}: ${AtlasLogBuffer.sanitize(error.message.orEmpty()).take(240)}"
                AtlasLogBuffer.log("PASSKEY", "Login failed in Credential Manager/client: $detail", "ERROR")
                reportPasskeyFailure(formatPasskeyClientError(error))
            }
        }
    }


    fun dismissPasskeyError() {
        _state.update { it.copy(passkeyError = null, error = null) }
    }

    private fun reportPasskeyFailure(message: String) {
        _state.update { it.copy(loading = false, error = message, passkeyError = message) }
    }

    private fun formatPasskeyClientError(error: Exception): String {
        val type = error.javaClass.simpleName
        return when {
            type.contains("NoCredential", ignoreCase = true) ->
                "Aucune passkey n'a été trouvée pour ${ServerUrl.display(serverConfig.selection.value?.baseUrl ?: "serveur sélectionné")} sur cet appareil. " +
                    "Si vous n'en avez pas encore, connectez-vous avec votre mot de passe puis enregistrez-en une dans Paramètres. " +
                    "Si vous en avez déjà une, activez le gestionnaire Android où elle est enregistrée."
            type.contains("ProviderConfiguration", ignoreCase = true) ||
                type.contains("NoCreateOption", ignoreCase = true) ->
                "Aucun gestionnaire de passkeys Android prêt n'a répondu. Activez Google Password Manager ou un fournisseur compatible, " +
                    "choisissez-le dans Paramètres Android > Mots de passe et clés d'accès, puis réessayez."
            type.contains("Cancellation", ignoreCase = true) ->
                "La demande de connexion par passkey a été annulée. Vous pouvez réessayer ou utiliser votre mot de passe."
            else -> {
                val detail = "$type: ${AtlasLogBuffer.sanitize(error.message.orEmpty()).take(200)}"
                "Connexion par passkey impossible — $detail. Vous pouvez réessayer ou utiliser votre mot de passe."
            }
        }
    }

    private fun logPasskeyFailure(stage: String, failure: ApiResult.Failure) {
        AtlasLogBuffer.log(
            "PASSKEY",
            "$stage failed: code=${failure.code}; http=${failure.httpStatus ?: "none"}; message=${failure.message}",
            "ERROR",
        )
    }

    private fun formatPasskeyFailure(failure: ApiResult.Failure): String =
        "${AtlasLogBuffer.sanitize(failure.message)} [${failure.code}${failure.httpStatus?.let { " · HTTP $it" } ?: ""}]"

    private suspend fun supportsSelectedServerPasskeyOrigin(origin: String): Boolean {
        if (ServerUrl.origin(serverConfig.selection.value?.baseUrl) != origin) return false
        val capability = (authRepository.webAuthnCapabilities() as? ApiResult.Success)?.data ?: return false
        return capability.supportsAndroid(
            BuildConfig.APPLICATION_ID,
            AndroidPasskeyOrigin.forInstalledApp(applicationContext),
            origin,
        )
    }

    fun submit() {
        when (_state.value.step) {
            Step.CREDENTIALS -> signIn()
            Step.OTP -> verifyOtp()
        }
    }

    fun resendCode() {
        if (_state.value.loading) return
        signIn(isResend = true)
    }

    fun backToCredentials() {
        _state.update {
            it.copy(step = Step.CREDENTIALS, otp = "", error = null, passkeyError = null, info = null)
        }
    }

    private fun signIn(isResend: Boolean = false) {
        val current = _state.value
        if (current.loading) return
        if (!isResend && (current.username.isBlank() || current.password.isBlank())) {
            _state.update { it.copy(error = "Saisissez votre identifiant et votre mot de passe.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (
                val result = sessionManager.signIn(
                    username = current.username,
                    password = current.password,
                    rememberAccount = current.rememberAccount,
                )
            ) {
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.message)
                }

                is ApiResult.Success -> when (val outcome = result.data) {
                    is LoginOutcome.SignedIn -> _state.update { it.copy(loading = false, password = "", error = null) }
                    is LoginOutcome.OtpRequired -> _state.update {
                        it.copy(
                            loading = false,
                            step = Step.OTP,
                            otp = "",
                            info = outcome.message,
                            error = null,
                        )
                    }
                }
            }
        }
    }

    private fun verifyOtp() {
        val current = _state.value
        if (current.loading) return
        if (current.otp.length < 4) {
            _state.update { it.copy(error = "Saisissez le code reçu par e-mail.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (
                val result = sessionManager.verifyOtp(
                    username = current.username,
                    password = current.password,
                    otp = current.otp,
                    rememberAccount = current.rememberAccount,
                )
            ) {
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.message)
                }

                is ApiResult.Success -> when (val outcome = result.data) {
                    is LoginOutcome.SignedIn -> _state.update { it.copy(loading = false, password = "", error = null) }
                    is LoginOutcome.OtpRequired -> _state.update {
                        it.copy(loading = false, info = outcome.message)
                    }
                }
            }
        }
    }
}
