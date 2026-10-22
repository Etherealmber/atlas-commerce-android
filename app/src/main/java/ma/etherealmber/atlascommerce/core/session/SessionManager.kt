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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.TokenHolder
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.persist.SavedAccountEntry
import ma.etherealmber.atlascommerce.core.security.CryptoStore
import ma.etherealmber.atlascommerce.core.security.DeviceIdentity
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.data.CartStore
import ma.etherealmber.atlascommerce.data.OfflineCache
import ma.etherealmber.atlascommerce.data.model.LoginResponse
import ma.etherealmber.atlascommerce.data.model.QrCompleteDto
import ma.etherealmber.atlascommerce.data.repo.AuthRepository
import ma.etherealmber.atlascommerce.di.ApplicationScope
import ma.etherealmber.atlascommerce.ui.components.AvatarBitmapCache
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class SessionUser(
    val username: String,
    val role: String,
    val displayName: String,
    val email: String = "",
    val avatarUrl: String = "",
    val serverUrl: String = "",
) {
    val isOwner: Boolean get() = role.equals("owner", ignoreCase = true)

    /** Unique isolation key across both server origin and account identity. */
    val scopeIdentity: String
        get() = "${serverUrl.trim().lowercase()}|${username.trim().lowercase()}|${role.trim().lowercase()}"
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
 * Owns the authenticated session with strict per-server & per-account isolation:
 * restore on start, password + OTP + QR pairing, optional encrypted account
 * vault switching, profile refresh, and full state purge on sign-out/switch.
 */
@Singleton
class SessionManager @Inject constructor(
    private val preferences: AtlasPreferences,
    private val serverConfig: ServerConfig,
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
        serverConfig.loaded.filter { it }.first()
        val currentServer = serverConfig.selection.value?.baseUrl.orEmpty()

        val restored = runCatching {
            val stored = preferences.readSession() ?: return@runCatching null
            if (currentServer.isBlank()) return@runCatching null
            if (stored.serverUrl.isNotBlank() && !stored.serverUrl.equals(currentServer, ignoreCase = true)) {
                return@runCatching null
            }
            val token = cryptoStore.decrypt(stored.tokenCipher)
            if (token.isNullOrBlank()) null else stored to token
        }.getOrNull()

        if (restored == null) {
            purgeInMemorySession()
            runCatching { preferences.clearSession() }
            _state.value = SessionState.SignedOut
            return
        }

        val (stored, token) = restored
        val username = stored.username.ifBlank { "utilisateur" }
        val resolvedServer = stored.serverUrl.ifBlank { currentServer }
        preferences.bindActiveScope(resolvedServer, username)
        offlineCache.bindScope(resolvedServer, username)
        tokenHolder.set(token)
        _state.value = SessionState.Active(
            SessionUser(
                username = username,
                role = stored.role.ifBlank { "user" },
                displayName = stored.displayName.ifBlank { username },
                email = stored.email,
                avatarUrl = stored.avatarUrl,
                serverUrl = resolvedServer,
            ),
        )
        runCatching { refreshProfile() }
    }

    suspend fun signIn(
        username: String,
        password: String,
        rememberAccount: Boolean = true,
    ): ApiResult<LoginOutcome> {
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
            is ApiResult.Success -> completeLogin(result.data, normalizedUser, rememberAccount)
        }
    }

    suspend fun verifyOtp(
        username: String,
        password: String,
        otp: String,
        rememberAccount: Boolean = true,
    ): ApiResult<LoginOutcome> {
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
            is ApiResult.Success -> completeLogin(result.data, normalizedUser, rememberAccount)
        }
    }

    suspend fun completeQrLogin(
        dto: QrCompleteDto,
        rememberAccount: Boolean = true,
    ): ApiResult<Unit> {
        val token = dto.token
        if (token.isNullOrBlank()) {
            return ApiResult.Failure("NO_TOKEN", "Jeton QR manquant dans la réponse du serveur.")
        }
        val username = (dto.username?.takeIf { it.isNotBlank() } ?: "utilisateur").lowercase(Locale.ROOT)
        val role = dto.role?.takeIf { it.isNotBlank() } ?: "user"
        val displayName = dto.displayName?.takeIf { it.isNotBlank() } ?: username
        val avatarUrl = dto.avatarUrl.orEmpty()
        val serverUrl = serverConfig.baseUrl()

        activateIsolatedSession(
            serverUrl = serverUrl,
            token = token,
            username = username,
            role = role,
            displayName = displayName,
            email = "",
            avatarUrl = avatarUrl,
            rememberAccount = rememberAccount,
        )
        runCatching { refreshProfile(rememberAccount = rememberAccount) }
        return ApiResult.Success(Unit)
    }

    /**
     * Switches directly into an encrypted saved account belonging strictly to
     * `entry.serverUrl`, flushing every trace of any previous session first.
     */
    suspend fun switchToSavedAccount(entry: SavedAccountEntry): ApiResult<Unit> {
        val token = runCatching { cryptoStore.decrypt(entry.tokenCipher) }.getOrNull()
        if (token.isNullOrBlank()) {
            preferences.removeSavedAccount(entry.serverUrl, entry.username)
            return ApiResult.Failure("EXPIRED", "Session expirée — reconnectez-vous avec votre mot de passe.")
        }
        purgeInMemorySession()
        if (!serverConfig.baseUrl().equals(entry.serverUrl, ignoreCase = true)) {
            serverConfig.select(entry.serverUrl, rememberServer = false)
        }
        tokenHolder.set(token)
        when (val verify = authRepository.me()) {
            is ApiResult.Failure -> {
                tokenHolder.clear()
                preferences.removeSavedAccount(entry.serverUrl, entry.username)
                return ApiResult.Failure(
                    "EXPIRED",
                    "La session mémorisée a expiré sur le serveur. Saisissez votre mot de passe.",
                )
            }
            is ApiResult.Success -> {
                val me = verify.data
                val resolvedUser = (me.username?.takeIf { it.isNotBlank() } ?: entry.username).lowercase(Locale.ROOT)
                val resolvedRole = me.role?.takeIf { it.isNotBlank() } ?: entry.role.ifBlank { "user" }
                val resolvedDisplay = me.displayName?.takeIf { it.isNotBlank() } ?: entry.displayName.ifBlank { resolvedUser }
                val resolvedEmail = me.email ?: entry.email
                val resolvedAvatar = me.avatarUrl?.takeIf { it.isNotBlank() } ?: entry.avatarUrl
                activateIsolatedSession(
                    serverUrl = entry.serverUrl,
                    token = token,
                    username = resolvedUser,
                    role = resolvedRole,
                    displayName = resolvedDisplay,
                    email = resolvedEmail,
                    avatarUrl = resolvedAvatar,
                    rememberAccount = true,
                )
                return ApiResult.Success(Unit)
            }
        }
    }

    suspend fun forgetSavedAccount(serverUrl: String, username: String) {
        preferences.removeSavedAccount(serverUrl, username)
        offlineCache.clearAccountScope(serverUrl, username)
    }

    private suspend fun completeLogin(
        response: LoginResponse,
        fallbackUsername: String,
        rememberAccount: Boolean,
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
        val username = (response.username?.takeIf { it.isNotBlank() } ?: fallbackUsername).lowercase(Locale.ROOT)
        val role = response.role?.takeIf { it.isNotBlank() } ?: "user"
        val displayName = response.displayName?.takeIf { it.isNotBlank() } ?: username
        val serverUrl = serverConfig.baseUrl()

        activateIsolatedSession(
            serverUrl = serverUrl,
            token = token,
            username = username,
            role = role,
            displayName = displayName,
            email = "",
            avatarUrl = "",
            rememberAccount = rememberAccount,
        )
        runCatching { refreshProfile(rememberAccount = rememberAccount) }
        return ApiResult.Success(LoginOutcome.SignedIn)
    }

    private suspend fun activateIsolatedSession(
        serverUrl: String,
        token: String,
        username: String,
        role: String,
        displayName: String,
        email: String,
        avatarUrl: String,
        rememberAccount: Boolean,
    ) {
        purgeInMemorySession()
        val cipher = cryptoStore.encrypt(token)
        preferences.bindActiveScope(serverUrl, username)
        offlineCache.bindScope(serverUrl, username)
        tokenHolder.set(token)
        runCatching {
            preferences.writeSession(
                serverUrl = serverUrl,
                tokenCipher = cipher,
                username = username,
                role = role,
                displayName = displayName,
                email = email,
                avatarUrl = avatarUrl,
            )
            if (rememberAccount) {
                preferences.upsertSavedAccount(
                    SavedAccountEntry(
                        serverUrl = serverUrl,
                        username = username,
                        role = role,
                        displayName = displayName,
                        email = email,
                        avatarUrl = avatarUrl,
                        tokenCipher = cipher,
                    ),
                )
            }
        }
        _state.value = SessionState.Active(
            SessionUser(
                username = username,
                role = role,
                displayName = displayName,
                email = email,
                avatarUrl = avatarUrl,
                serverUrl = serverUrl,
            ),
        )
    }

    /** Refresh display name, role, email and PFP (`avatar_url`) from `/api/auth/me`. */
    suspend fun refreshProfile(rememberAccount: Boolean? = null) {
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
                    val cipher = cryptoStore.encrypt(token)
                    preferences.writeSession(
                        serverUrl = updated.serverUrl,
                        tokenCipher = cipher,
                        username = updated.username,
                        role = updated.role,
                        displayName = updated.displayName,
                        email = updated.email,
                        avatarUrl = updated.avatarUrl,
                    )
                    val savedForServer = preferences.savedAccountsForServer(updated.serverUrl).first()
                    val shouldUpdateSaved = rememberAccount == true ||
                        savedForServer.any { it.username.equals(updated.username, ignoreCase = true) }
                    if (shouldUpdateSaved) {
                        preferences.upsertSavedAccount(
                            SavedAccountEntry(
                                serverUrl = updated.serverUrl,
                                username = updated.username,
                                role = updated.role,
                                displayName = updated.displayName,
                                email = updated.email,
                                avatarUrl = updated.avatarUrl,
                                tokenCipher = cipher,
                            ),
                        )
                    }
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
                        val cipher = cryptoStore.encrypt(token)
                        preferences.writeSession(
                            serverUrl = updated.serverUrl,
                            tokenCipher = cipher,
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
        purgeInMemorySession()
        runCatching { preferences.clearSession() }
        _state.value = SessionState.SignedOut
        if (!reason.isNullOrBlank()) _notices.tryEmit(reason)
    }

    private fun purgeInMemorySession() {
        tokenHolder.clear()
        cartStore.clear()
        preferences.bindActiveScope(null, null)
        offlineCache.bindScope(null, null)
        runCatching { AvatarBitmapCache.clear() }
    }

    fun currentUser(): SessionUser? = (_state.value as? SessionState.Active)?.user
}
