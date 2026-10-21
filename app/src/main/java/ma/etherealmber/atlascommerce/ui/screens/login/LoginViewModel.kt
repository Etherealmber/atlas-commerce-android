package ma.etherealmber.atlascommerce.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.session.LoginOutcome
import ma.etherealmber.atlascommerce.core.session.SessionManager
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val serverConfig: ServerConfig,
) : ViewModel() {

    enum class Step { CREDENTIALS, OTP }

    data class UiState(
        val username: String = "",
        val password: String = "",
        val otp: String = "",
        val step: Step = Step.CREDENTIALS,
        val loading: Boolean = false,
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
            when (val result = sessionManager.signIn(current.username, current.password)) {
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.message)
                }

                is ApiResult.Success -> when (val outcome = result.data) {
                    is LoginOutcome.SignedIn -> _state.update { it.copy(loading = false, error = null) }
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
            when (val result = sessionManager.verifyOtp(current.username, current.password, current.otp)) {
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.message)
                }

                is ApiResult.Success -> when (val outcome = result.data) {
                    is LoginOutcome.SignedIn -> _state.update { it.copy(loading = false, error = null) }
                    is LoginOutcome.OtpRequired -> _state.update {
                        it.copy(loading = false, info = outcome.message)
                    }
                }
            }
        }
    }
}
