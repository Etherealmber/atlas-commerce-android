package ma.etherealmber.atlascommerce.core.session

import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.TokenHolder
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.security.CryptoStore
import ma.etherealmber.atlascommerce.core.security.DeviceIdentity
import ma.etherealmber.atlascommerce.data.CartStore
import ma.etherealmber.atlascommerce.data.OfflineCache
import ma.etherealmber.atlascommerce.data.model.LoginResponse
import ma.etherealmber.atlascommerce.data.model.QrCompleteDto
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
    val avatarUrl: String = "",
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
 * OTP + QR pairing), profile / avatar refresh and logout.
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

    val fingerprint: String get() = deviceIdentity.fingerprint

    val authToken: String? get() = tokenHolder.token

    fun deviceUserAgent(): String =
        "Atlas-Android/${AppDetails.VERSION_NAME} (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"

    init {
        scope.launch { restore() }
    }

    private suspend fun restore() {
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
                avatarUrl = stored.avatarUrl,
            ),
        )
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

    suspend fun completeQrLogin(dto: QrCompleteDto): ApiResult<Unit> {
        val token = dto.token
        if (token.isNullOrBlank()) {
            return ApiResult.Failure("NO_TOKEN", "Jeton QR manquant dans la réponse du serveur.")
        }
        val username = dto.username?.takeIf { it.isNotBlank() } ?: "utilisateur"
        val role = dto.role?.takeIf { it.isNotBlank() } ?: "user"
        val displayName = dto.displayName?.takeIf { it.isNotBlank() } ?: username
        val avatarUrl = dto.avatarUrl.orEmpty()

        tokenHolder.set(token)
        runCatching {
            preferences.writeSession(
                tokenCipher = cryptoStore.encrypt(token),
                username = username,
                role = role,
                displayName = displayName,
                email = "",
                avatarUrl = avatarUrl,
            )
        }
        _state.value = SessionState.Active(
            SessionUser(
                username = username,
                role = role,
                displayName = displayName,
                avatarUrl = avatarUrl,
            ),
        )
        runCatching { refreshProfile() }
        return ApiResult.Success(Unit)
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
        // Immediately fetch /api/auth/me so user PFP (avatar_url) & email populate right away.
        runCatching { refreshProfile() }
        return ApiResult.Success(LoginOutcome.SignedIn)
    }

    /** Refresh display name, role, email and PFP (`avatar_url`) from `/api/auth/me`. */
    suspend fun refreshProfile() {
        val current = (_state.value as? SessionState.Active)?.user ?: return
        val result = authRepository.me()
        if (result is ApiResult.Success) {
            val me = result.data
            val updated = current.copy(
                username = me.username?.takeIf { it.isNotBlank() } ?: current.username,
                role = me.role?.takeIf { it.isNotBlank() } ?: current.role,
                displayName = me.displayName?.takeIf { it.isNotBlank() } ?: current.displayName,
                email = me.email ?: current.email,
                avatarUrl = me.avatarUrl?.takeIf { it.isNotBlank() } ?: current.avatarUrl,
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
                        avatarUrl = updated.avatarUrl,
                    )
                }
            }
        }
    }

    /** Updates profile photo (`avatar_url`) or display name via `PATCH /api/auth/me`. */
    suspend fun updateProfile(
        displayName: String? = null,
        email: String? = null,
        avatarUrl: String? = null,
    ): ApiResult<Unit> {
        val res = authRepository.updateProfile(
            displayName = displayName,
            email = email,
            avatarUrl = avatarUrl,
        )
        if (res is ApiResult.Success) {
            val current = (_state.value as? SessionState.Active)?.user
            if (current != null) {
                val updated = current.copy(
                    displayName = displayName ?: current.displayName,
                    email = email ?: current.email,
                    avatarUrl = avatarUrl ?: current.avatarUrl,
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
                            avatarUrl = updated.avatarUrl,
                        )
                    }
                }
            }
            runCatching { refreshProfile() }
        }
        return res
    }

    suspend fun signOut(reason: String? = null) {
        tokenHolder.clear()
        runCatching { preferences.clearSession() }
        cartStore.clear()
        runCatching { offlineCache.clear() }
        _state.value = SessionState.SignedOut
        if (!reason.isNullOrBlank()) _notices.tryEmit(reason)
    }

    fun currentUser(): SessionUser? = (_state.value as? SessionState.Active)?.user
}
