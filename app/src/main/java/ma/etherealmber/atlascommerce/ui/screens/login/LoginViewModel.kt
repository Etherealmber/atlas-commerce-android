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
                if (selectedServer != null) {
                    val result = authRepository.webAuthnCapabilities()
                    val currentBaseUrl = serverConfig.selection.value?.baseUrl
                    if (currentBaseUrl != null && currentBaseUrl.equals(selectedServer.baseUrl, ignoreCase = true)) {
                        val capabilities = (result as? ApiResult.Success)?.data
                        _state.update {
                            it.copy(passkeyAvailable = capabilities?.supportsAndroid(
                                BuildConfig.APPLICATION_ID,
                                AndroidPasskeyOrigin.forInstalledApp(applicationContext),
                                ServerUrl.origin(selectedServer.baseUrl),
                            ) == true)
                        }
                    }
                }
            }
        }
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
            _state.update { it.copy(loading = true, error = null, info = null) }
            try {
                val selectedServerOrigin = ServerUrl.origin(serverConfig.selection.value?.baseUrl)
                if (selectedServerOrigin == null) {
                    _state.update { it.copy(loading = false, passkeyAvailable = false, error = "L'origine du serveur sélectionné est invalide.") }
                    return@launch
                }
                if (!supportsSelectedServerPasskeyOrigin(selectedServerOrigin)) {
                    _state.update {
                        it.copy(
                            loading = false,
                            passkeyAvailable = false,
                            error = "Les passkeys Android ne sont pas activées sur ce serveur.",
                        )
                    }
                    return@launch
                }
                when (val options = authRepository.webAuthnLoginOptions(current.username.trim().ifBlank { null })) {
                    is ApiResult.Failure -> _state.update { it.copy(loading = false, error = options.message) }
                    is ApiResult.Success -> {
                        if (!supportsSelectedServerPasskeyOrigin(selectedServerOrigin)) {
                            _state.update {
                                it.copy(loading = false, passkeyAvailable = false, error = "Les passkeys Android ne sont plus disponibles sur ce serveur.")
                            }
                            return@launch
                        }
                        val credentialJson = AndroidPasskeyCredentials.authenticate(context, options.data, selectedServerOrigin)
                        when (val verified = authRepository.verifyWebAuthnLogin(credentialJson, deviceIdentity.fingerprint)) {
                            is ApiResult.Failure -> _state.update { it.copy(loading = false, error = verified.message) }
                            is ApiResult.Success -> when (val signedIn = sessionManager.signInWithPasskey(verified.data)) {
                                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = signedIn.message) }
                                is ApiResult.Success -> _state.update {
                                    it.copy(loading = false, password = "", error = null, info = null)
                                }
                            }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = "Connexion passkey annulée ou indisponible. Vous pouvez vous connecter avec votre mot de passe.",
                    )
                }
            }
        }
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
