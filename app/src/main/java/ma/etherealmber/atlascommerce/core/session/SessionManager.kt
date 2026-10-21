package ma.etherealmber.atlascommerce.core.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.TokenHolder
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.security.CryptoStore
import ma.etherealmber.atlascommerce.core.security.DeviceIdentity
import ma.etherealmber.atlascommerce.data.CartStore
import ma.etherealmber.atlascommerce.data.OfflineCache
import ma.etherealmber.atlascommerce.data.model.LoginResponse
import ma.etherealmber.atlascommerce.data.repo.AuthRepository
import ma.etherealmber.atlascommerce.di.ApplicationScope
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class SessionUser(
    val username: String,
    val role: String,
    val displayName: String,
    val email: String = "",
) {
    val isOwner: Boolean get() = role.equals("owner", ignoreCase = true)
}

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class Active(val user: SessionUser) : SessionState
}

sealed interface LoginOutcome {
    data object SignedIn : LoginOutcome
    data class OtpRequired(val message: String, val emailHint: String?) : LoginOutcome
}

/**
 * Owns the authenticated session: restore on start, login (password + optional
 * OTP), profile refresh and logout.
 *
 * The token is kept in memory for requests and encrypted on disk between runs.
 * Logging out also clears the in-memory cart and any cached catalogue.
 */
@Singleton
class SessionManager @Inject constructor(
    private val preferences: AtlasPreferences,
    private val authRepository: AuthRepository,
    private val tokenHolder: TokenHolder,
    private val cryptoStore: CryptoStore,
    private val deviceIdentity: DeviceIdentity,
    private val cartStore: CartStore,
    private val offlineCache: OfflineCache,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _notices = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    init {
        scope.launch { restore() }
    }

    private suspend fun restore() {
        // Any failure here (corrupted DataStore, rotated Keystore key) must land
        // on the login screen — never leave the app stuck on the splash.
        val restored = runCatching {
            val stored = preferences.readSession() ?: return@runCatching null
            val token = cryptoStore.decrypt(stored.tokenCipher)
            if (token.isNullOrBlank()) null else stored to token
        }.getOrNull()

        if (restored == null) {
            tokenHolder.clear()
            runCatching { preferences.clearSession() }
            _state.value = SessionState.SignedOut
            return
        }

        val (stored, token) = restored
        tokenHolder.set(token)
        _state.value = SessionState.Active(
            SessionUser(
                username = stored.username.ifBlank { "utilisateur" },
                role = stored.role.ifBlank { "user" },
                displayName = stored.displayName.ifBlank { stored.username },
                email = stored.email,
            ),
        )
        // Keep role/display name fresh; a failure only means we are offline.
        runCatching { refreshProfile() }
    }

    suspend fun signIn(username: String, password: String): ApiResult<LoginOutcome> {
        val normalizedUser = username.trim().lowercase(Locale.ROOT)
        if (normalizedUser.isBlank()) {
            return ApiResult.Failure("VALIDATION", "Saisissez votre identifiant.")
        }
        if (password.isBlank()) {
            return ApiResult.Failure("VALIDATION", "Saisissez votre mot de passe.")
        }
        return when (
            val result = authRepository.login(
                username = normalizedUser,
                password = password,
                fingerprint = deviceIdentity.fingerprint,
            )
        ) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> completeLogin(result.data, normalizedUser)
        }
    }

    suspend fun verifyOtp(username: String, password: String, otp: String): ApiResult<LoginOutcome> {
        val normalizedUser = username.trim().lowercase(Locale.ROOT)
        val code = otp.filter { it.isDigit() }
        if (code.length < 4) {
            return ApiResult.Failure("VALIDATION", "Saisissez le code complet reçu par e-mail.")
        }
        return when (
            val result = authRepository.verifyOtp(
                username = normalizedUser,
                password = password,
                otp = code,
                fingerprint = deviceIdentity.fingerprint,
            )
        ) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> completeLogin(result.data, normalizedUser)
        }
    }

    private suspend fun completeLogin(
        response: LoginResponse,
        fallbackUsername: String,
    ): ApiResult<LoginOutcome> {
        if (response.needsOtp) {
            return ApiResult.Success(
                LoginOutcome.OtpRequired(
                    message = response.message ?: "Un code de vérification a été envoyé.",
                    emailHint = null,
                ),
            )
        }
        val token = response.token
        if (token.isNullOrBlank()) {
            return ApiResult.Failure(
                "NO_TOKEN",
                response.message ?: "Connexion incomplète — réessayez.",
            )
        }
        val username = response.username?.takeIf { it.isNotBlank() } ?: fallbackUsername
        val role = response.role?.takeIf { it.isNotBlank() } ?: "user"
        val displayName = response.displayName?.takeIf { it.isNotBlank() } ?: username

        tokenHolder.set(token)
        runCatching {
            preferences.writeSession(
                tokenCipher = cryptoStore.encrypt(token),
                username = username,
                role = role,
                displayName = displayName,
                email = "",
            )
        }
        _state.value = SessionState.Active(
            SessionUser(username = username, role = role, displayName = displayName),
        )
        return ApiResult.Success(LoginOutcome.SignedIn)
    }

    /** Refresh the display name / role from `/api/auth/me`. */
    suspend fun refreshProfile() {
        val current = (_state.value as? SessionState.Active)?.user ?: return
        val result = authRepository.me()
        if (result is ApiResult.Success) {
            val me = result.data
            val updated = current.copy(
                username = me.username?.takeIf { it.isNotBlank() } ?: current.username,
                role = me.role?.takeIf { it.isNotBlank() } ?: current.role,
                displayName = me.displayName?.takeIf { it.isNotBlank() } ?: current.displayName,
                email = me.email.orEmpty(),
            )
            _state.value = SessionState.Active(updated)
            val token = tokenHolder.token
            if (!token.isNullOrBlank()) {
                runCatching {
                    preferences.writeSession(
                        tokenCipher = cryptoStore.encrypt(token),
                        username = updated.username,
                        role = updated.role,
                        displayName = updated.displayName,
                        email = updated.email,
                    )
                }
            }
        }
    }

    suspend fun signOut(reason: String? = null) {
        tokenHolder.clear()
        runCatching { preferences.clearSession() }
        cartStore.clear()
        // The catalogue cache is shared by every account and every server.
        runCatching { offlineCache.clear() }
        _state.value = SessionState.SignedOut
        if (!reason.isNullOrBlank()) _notices.tryEmit(reason)
    }

    fun currentUser(): SessionUser? = (_state.value as? SessionState.Active)?.user
}
