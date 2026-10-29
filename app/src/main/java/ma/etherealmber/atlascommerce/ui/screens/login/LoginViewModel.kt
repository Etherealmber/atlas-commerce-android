package ma.etherealmber.atlascommerce.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.persist.SavedAccountEntry
import ma.etherealmber.atlascommerce.core.security.DeviceUnlockSetupPrompt
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.session.LoginOutcome
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.core.session.SessionManager
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val serverConfig: ServerConfig,
    private val preferences: AtlasPreferences,
    private val deviceUnlockSetupPrompt: DeviceUnlockSetupPrompt,
) : ViewModel() {

    enum class Step { CREDENTIALS, OTP }

    data class UiState(
        val username: String = "",
        val password: String = "",
        val otp: String = "",
        val rememberAccount: Boolean = true,
        val step: Step = Step.CREDENTIALS,
        val loading: Boolean = false,
        val error: String? = null,
        val info: String? = null,
    ) {
        val canSubmit: Boolean
            get() = !loading && username.isNotBlank() && when (step) {
                Step.CREDENTIALS -> password.isNotBlank()
                Step.OTP -> otp.length == 6 && otp.all(Char::isDigit)
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
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun onUsernameChange(value: String) {
        _state.update { it.copy(username = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun onOtpChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _state.update { it.copy(otp = digits, error = null) }
    }

    fun onRememberAccountChange(value: Boolean) {
        _state.update { it.copy(rememberAccount = value) }
    }

    fun signInWithSavedAccount(entry: SavedAccountEntry) {
        if (_state.value.loading || !entry.canUseDeviceUnlock) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val res = sessionManager.switchToSavedAccount(entry)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(loading = false, error = null) }

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

    private fun offerDeviceUnlockIfNeeded() {
        viewModelScope.launch {
            val user = (sessionManager.state.value as? SessionState.Active)?.user ?: return@launch
            val saved = preferences.savedAccountsForServer(user.serverUrl).first()
                .firstOrNull { it.username.equals(user.username, ignoreCase = true) }
            if (saved != null && !saved.deviceUnlockChoiceMade) {
                deviceUnlockSetupPrompt.request(user.serverUrl, user.username)
            }
        }
    }

    private fun safeAuthFailure(code: String, httpStatus: Int?, isOtp: Boolean): String {
        AtlasLogBuffer.log("AUTH", "Authentication rejected: code=$code; http=${httpStatus ?: "none"}", "WARN")
        return authFailureMessage(code, httpStatus, isOtp)
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
            it.copy(step = Step.CREDENTIALS, otp = "", error = null, info = null)
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
                        if (current.rememberAccount) offerDeviceUnlockIfNeeded()
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
        if (current.otp.length != 6 || current.otp.any { !it.isDigit() }) {
            _state.update { it.copy(error = "Saisissez les 6 chiffres du code reçu par e-mail.") }
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
                        if (current.rememberAccount) offerDeviceUnlockIfNeeded()
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
    error = null, info = null,
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
