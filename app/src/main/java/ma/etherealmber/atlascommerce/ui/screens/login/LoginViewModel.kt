package ma.etherealmber.atlascommerce.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
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
import ma.etherealmber.atlascommerce.core.diagnostics.PasskeyAutoReporter
import ma.etherealmber.atlascommerce.core.diagnostics.PasskeyFailureStage
import ma.etherealmber.atlascommerce.core.security.awaitPasskeyProvider
import ma.etherealmber.atlascommerce.core.security.AndroidPasskeyCredentials
import ma.etherealmber.atlascommerce.core.security.AndroidPasskeyOrigin
import ma.etherealmber.atlascommerce.core.security.DeviceIdentity
import ma.etherealmber.atlascommerce.core.security.PasskeySetupPrompt
import ma.etherealmber.atlascommerce.core.persist.SavedAccountEntry
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.session.LoginOutcome
import ma.etherealmber.atlascommerce.core.session.SessionState
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
    private val passkeySetupPrompt: PasskeySetupPrompt,
    private val passkeyAutoReporter: PasskeyAutoReporter,
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
            sessionManager.state.collect { session ->
                if (session is SessionState.Active) _state.update { it.afterAuthentication() }
            }
        }
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
        val advertisedOrigins = android?.configuredOrigins.orEmpty()
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
                "Login capability request failed: code=${result.code}; http=${result.httpStatus ?: "none"}",
                "ERROR",
            )
        }
        _state.update { it.copy(passkeyAvailable = available) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
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
                is ApiResult.Success -> {
                    _state.update { it.copy(loading = false, error = null) }
                    passkeySetupPrompt.request()
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(
                        loading = false,
                        username = entry.username,
                        error = safeAuthFailure(res.code, res.httpStatus, isOtp = false),
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
        val attemptServer = serverConfig.selection.value?.baseUrl
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
                    passkeyAutoReporter.record(PasskeyFailureStage.LOGIN_CAPABILITY, attemptServer = attemptServer)
                    reportPasskeyFailure("Les passkeys Android ne sont pas activées sur ce serveur.")
                    _state.update { it.copy(passkeyAvailable = false) }
                    return@launch
                }
                AtlasLogBuffer.log("PASSKEY", "Requesting login options")
                when (val options = authRepository.webAuthnLoginOptions(current.username.trim().ifBlank { null })) {
                    is ApiResult.Failure -> {
                        logPasskeyFailure("login-options", options)
                        passkeyAutoReporter.record(PasskeyFailureStage.LOGIN_OPTIONS, options.httpStatus, attemptServer)
                        reportPasskeyFailure(formatPasskeyFailure(options))
                    }
                    is ApiResult.Success -> {
                        AtlasLogBuffer.log("PASSKEY", "Login options received; credential JSON is intentionally not logged")
                        if (!supportsSelectedServerPasskeyOrigin(selectedServerOrigin)) {
                            passkeyAutoReporter.record(PasskeyFailureStage.LOGIN_CAPABILITY, attemptServer = attemptServer)
                            reportPasskeyFailure("Les passkeys Android ne sont plus disponibles sur ce serveur.")
                            _state.update { it.copy(passkeyAvailable = false) }
                            return@launch
                        }
                        AtlasLogBuffer.log("PASSKEY", "Opening Android Credential Manager for login")
                        val providerStarted = android.os.SystemClock.elapsedRealtime()
                        val credentialJson = try {
                            awaitPasskeyProvider { AndroidPasskeyCredentials.authenticate(context, options.data) }
                        } finally {
                            val elapsed = android.os.SystemClock.elapsedRealtime() - providerStarted
                            AtlasLogBuffer.log("PASSKEY", "Login credential provider elapsedMs=$elapsed")
                        }
                        if (ServerUrl.origin(serverConfig.selection.value?.baseUrl) != selectedServerOrigin) {
                            reportPasskeyFailure("Le serveur a changé. Recommencez la connexion par passkey.")
                            return@launch
                        }
                        AtlasLogBuffer.log("PASSKEY", "Credential Manager returned an assertion; sending it to selected Worker")
                        when (val verified = authRepository.verifyWebAuthnLogin(credentialJson, deviceIdentity.fingerprint)) {
                            is ApiResult.Failure -> {
                                logPasskeyFailure("login-verify", verified)
                                passkeyAutoReporter.record(PasskeyFailureStage.LOGIN_VERIFY, verified.httpStatus, attemptServer)
                                reportPasskeyFailure(formatPasskeyFailure(verified))
                            }
                            is ApiResult.Success -> when (val signedIn = sessionManager.signInWithPasskey(verified.data)) {
                                is ApiResult.Failure -> {
                                    logPasskeyFailure("session-finalize", signedIn)
                                    passkeyAutoReporter.record(PasskeyFailureStage.LOGIN_SESSION, signedIn.httpStatus, attemptServer)
                                    reportPasskeyFailure(formatPasskeyFailure(signedIn))
                                }
                                is ApiResult.Success -> {
                                    AtlasLogBuffer.log("PASSKEY", "Login completed and session accepted")
                                    _state.update { it.afterAuthentication() }
                                }
                            }
                        }
                    }
                }
            } catch (timeout: TimeoutCancellationException) {
                AtlasLogBuffer.log("PASSKEY", "Credential Manager login timed out", "ERROR")
                passkeyAutoReporter.record(PasskeyFailureStage.LOGIN_TIMEOUT, attemptServer = attemptServer)
                reportPasskeyFailure("Le gestionnaire Android n'a pas répondu. Vérifiez votre fournisseur de passkeys puis réessayez.")
            } catch (cancelled: CancellationException) {
                _state.update { it.copy(loading = false) }
                AtlasLogBuffer.log("PASSKEY", "Login coroutine cancelled")
                throw cancelled
            } catch (error: Exception) {
                val detail = error.javaClass.simpleName
                AtlasLogBuffer.log("PASSKEY", "Login failed in Credential Manager/client: $detail", "ERROR")
                if (error !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    passkeyAutoReporter.record(PasskeyFailureStage.LOGIN_PROVIDER, attemptServer = attemptServer)
                }
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
                "Aucune passkey n'est disponible sur cet appareil. Connectez-vous avec votre mot de passe, puis ajoutez une passkey dans les paramètres."
            type.contains("ProviderConfiguration", ignoreCase = true) ||
                type.contains("NoCreateOption", ignoreCase = true) ->
                "Aucun fournisseur de passkeys n'est prêt sur cet appareil. Vérifiez les paramètres de mots de passe Android ou utilisez votre mot de passe."
            type.contains("Cancellation", ignoreCase = true) ->
                "La connexion par passkey a été annulée."
            else -> {
                "Connexion par passkey impossible. Réessayez ou utilisez votre mot de passe."
            }
        }
    }

    private fun logPasskeyFailure(stage: String, failure: ApiResult.Failure) {
        AtlasLogBuffer.log(
            "PASSKEY",
            "$stage failed: code=${failure.code}; http=${failure.httpStatus ?: "none"}",
            "ERROR",
        )
    }

    private fun formatPasskeyFailure(failure: ApiResult.Failure): String {
        AtlasLogBuffer.log(
            "PASSKEY",
            "Passkey request failed: code=${failure.code}; http=${failure.httpStatus ?: "none"}",
            "ERROR",
        )
        return if (failure.code == ma.etherealmber.atlascommerce.core.net.ApiRunner.CODE_NETWORK) {
            "Impossible de joindre le serveur. Vérifiez votre connexion et réessayez."
        } else {
            "Connexion par passkey impossible. Réessayez ou utilisez votre mot de passe."
        }
    }

    private fun safeAuthFailure(code: String, httpStatus: Int?, isOtp: Boolean): String {
        AtlasLogBuffer.log("AUTH", "Authentication rejected: code=$code; http=${httpStatus ?: "none"}", "WARN")
        return authFailureMessage(code, httpStatus, isOtp)
    }

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
                is ApiResult.Failure -> {
                    AtlasLogBuffer.log("AUTH", "Sign-in rejected: code=${result.code}; http=${result.httpStatus ?: "none"}", "WARN")
                    _state.update { it.copy(loading = false, error = safeAuthFailure(result.code, result.httpStatus, isOtp = false)) }
                }

                is ApiResult.Success -> when (val outcome = result.data) {
                    is LoginOutcome.SignedIn -> {
                        _state.update { it.afterAuthentication() }
                        passkeySetupPrompt.request()
                    }
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
                is ApiResult.Failure -> {
                    AtlasLogBuffer.log("AUTH", "OTP verification rejected: code=${result.code}; http=${result.httpStatus ?: "none"}", "WARN")
                    _state.update { it.copy(loading = false, error = safeAuthFailure(result.code, result.httpStatus, isOtp = true)) }
                }

                is ApiResult.Success -> when (val outcome = result.data) {
                    is LoginOutcome.SignedIn -> {
                        _state.update { it.afterAuthentication() }
                        passkeySetupPrompt.request()
                    }
                    is LoginOutcome.OtpRequired -> _state.update {
                        it.copy(loading = false, info = outcome.message)
                    }
                }
            }
        }
    }
}

/** Clear the one-time verification flow whenever a session becomes active. */
internal fun LoginViewModel.UiState.afterAuthentication(): LoginViewModel.UiState = copy(
    step = LoginViewModel.Step.CREDENTIALS, password = "", otp = "", loading = false,
    error = null, passkeyError = null, info = null,
)

internal fun authFailureMessage(code: String, httpStatus: Int?, isOtp: Boolean): String = when {
    code == ma.etherealmber.atlascommerce.core.net.ApiRunner.CODE_NETWORK ->
        "Impossible de joindre le serveur. Vérifiez votre connexion et réessayez."
    code == "APP_UPDATE_REQUIRED" -> "Une mise à jour est nécessaire pour continuer."
    code == "PASSWORD_REHASH_REQUIRED" -> "Un administrateur doit réinitialiser votre mot de passe."
    isOtp && (httpStatus == 401 || code == "BAD_OTP" || code == "OTP_EXPIRED") ->
        "Code invalide ou expiré. Vérifiez-le ou demandez-en un nouveau."
    !isOtp && (httpStatus == 401 || code == "BAD_PASSWORD") ->
        "Identifiant ou mot de passe incorrect. Vérifiez votre saisie."
    isOtp -> "Vérification impossible. Vérifiez le code et réessayez."
    else -> "Connexion impossible. Vérifiez vos informations et réessayez."
}
