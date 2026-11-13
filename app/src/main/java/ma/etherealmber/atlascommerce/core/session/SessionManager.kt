package ma.etherealmber.atlascommerce.core.session

import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.TokenHolder
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.persist.DEVICE_UNLOCK_TOKEN_PREFIX
import ma.etherealmber.atlascommerce.core.persist.SavedAccountEntry
import ma.etherealmber.atlascommerce.core.security.CryptoStore
import ma.etherealmber.atlascommerce.core.security.DeviceIdentity
import ma.etherealmber.atlascommerce.core.security.DeviceIdentityUnavailableException
import ma.etherealmber.atlascommerce.core.security.DeviceUnlockAuthenticator
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.data.CartStore
import ma.etherealmber.atlascommerce.data.OfflineCache
import ma.etherealmber.atlascommerce.data.model.LoginResponse
import ma.etherealmber.atlascommerce.data.model.MeDto
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
    val phone: String = "",
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

/** Prevents an in-flight profile response from crossing an account/server/token switch. */
internal fun isProfileRefreshCurrent(
    expectedScopeIdentity: String,
    expectedToken: String,
    activeScopeIdentity: String?,
    activeToken: String?,
): Boolean = expectedScopeIdentity == activeScopeIdentity && expectedToken == activeToken

private const val STARTUP_RETRY_INITIAL_DELAY_MS = 1_000L
private const val STARTUP_RETRY_MAX_DELAY_MS = 30_000L
private const val LOGOUT_RESUME_UNCONFIRMED_NOTICE =
    "Déconnexion effectuée, mais le serveur n’a pas confirmé la reprise par mot de passe. Une vérification OTP peut être nécessaire."

/** The Worker remains the sole authority on whether this password attempt needs OTP. */
internal fun loginOtpRequirement(response: LoginResponse): LoginOutcome.OtpRequired? =
    if (response.needsOtp) {
        LoginOutcome.OtpRequired(
            message = response.message ?: "Un code de vérification a été envoyé.",
            emailHint = null,
        )
    } else {
        null
    }

/**
 * Owns the authenticated session with strict per-server & per-account isolation:
 * password + OTP + QR pairing, explicit device-authenticated saved-account
 * switching, profile refresh, and full state purge on account switch/sign-out.
 */
@Singleton
class SessionManager @Inject constructor(
    private val preferences: AtlasPreferences,
    private val serverConfig: ServerConfig,
    private val apiProvider: ApiProvider,
    private val authRepository: AuthRepository,
    private val tokenHolder: TokenHolder,
    private val cryptoStore: CryptoStore,
    private val deviceIdentity: DeviceIdentity,
    private val cartStore: CartStore,
    private val offlineCache: OfflineCache,
    private val pushTokens: ma.etherealmber.atlascommerce.push.PushTokenRegistrar,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()
    private val startupCleanup = CompletableDeferred<Unit>()
    /** Serializes active-session state and encrypted persistence across refresh/login/logout. */
    private val sessionTransitionMutex = Mutex()

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
        scope.launch { initializeFreshSession() }
        // v1.2 FCM bridge — (re)register the device push token with the worker
        // whenever a session becomes active (password login, saved-account unlock, QR pairing).
        scope.launch {
            state
                .map { (it as? SessionState.Active)?.user?.scopeIdentity }
                .distinctUntilChanged()
                .collect { identity -> if (identity != null) pushTokens.onSessionActive() }
        }
    }

    /** Revalidate an encrypted same-device token at its original selected server on cold start. */
    private suspend fun initializeFreshSession() {
        val startupGateStartedAt = SystemClock.elapsedRealtime()
        val outcome = FreshProcessStartup(
            restorePreviousSession = ::restoreSavedSession,
            clearStoredSession = preferences::clearSession,
            purgeActiveState = ::purgeInMemorySession,
            publishSignedOut = { _state.value = SessionState.SignedOut },
            releaseStartupGate = { startupCleanup.complete(Unit) },
            onFailure = { error ->
                ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer.log(
                    tag = "AUTH",
                    message = "Could not validate saved session at startup (${error.javaClass.simpleName})",
                    level = "WARN",
                )
            },
        ).run()
        ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer.log(
            tag = "AUTH",
            message = "Cold-start session check completed ($outcome) in ${SystemClock.elapsedRealtime() - startupGateStartedAt} ms",
        )
    }

    /**
     * Do not send a saved-token user to password/OTP because startup networking or Keystore
     * access is temporarily unavailable. Retry the read-only /auth/me until it is verified or
     * the server definitively rejects the token.
     */
    private suspend fun restoreSavedSession(): FreshProcessSessionOutcome {
        preferences.migrateLegacyPreferencesAtRest()
        serverConfig.loaded.filter { it }.first()
        var stored = preferences.readSession()
            ?: return FreshProcessSessionOutcome.NO_SESSION
        var retryDelayMs = STARTUP_RETRY_INITIAL_DELAY_MS

        while (true) {
            if (preferences.isSessionRestoreDisabled()) return FreshProcessSessionOutcome.REJECTED
            val selectedServerUrl = serverConfig.normalizedSelection()
            val plan = StartupResumePreparation.createRequest(
                previousSession = stored,
                selectedServerUrl = selectedServerUrl,
                restoreDisabled = false,
                decryptToken = cryptoStore::decrypt,
            )

            if (plan.result == StartupResumeResult.UNDECRYPTABLE_TOKEN) {
                kotlinx.coroutines.delay(retryDelayMs)
                val latest = preferences.readSession()
                if (latest == null) return FreshProcessSessionOutcome.NO_SESSION
                if (latest.tokenCipher != stored.tokenCipher || preferences.isSessionRestoreDisabled()) {
                    return FreshProcessSessionOutcome.UNAVAILABLE
                }
                stored = latest
                retryDelayMs = (retryDelayMs * 2).coerceAtMost(STARTUP_RETRY_MAX_DELAY_MS)
                continue
            }

            val request = plan.request
            if (request == null) {
                if (plan.result != StartupResumeResult.NO_SESSION) {
                    ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer.log(
                        tag = "AUTH",
                        message = "Saved-session restore skipped (${plan.result})",
                        level = "WARN",
                    )
                }
                return startupOutcomeForResumePreparation(plan.result)
                    ?: FreshProcessSessionOutcome.REJECTED
            }

            when (
                val verification = StartupResumePreparation.verifyWithRetry(
                    request = request,
                    validate = authRepository::validateStartupSession,
                )
            ) {
                is StartupSessionVerification.Valid -> {
                    return if (restoreVerifiedSession(request, stored, verification.profile)) {
                        FreshProcessSessionOutcome.RESTORED
                    } else {
                        FreshProcessSessionOutcome.REJECTED
                    }
                }
                StartupSessionVerification.Rejected -> return FreshProcessSessionOutcome.REJECTED
                StartupSessionVerification.Unavailable -> {
                    kotlinx.coroutines.delay(retryDelayMs)
                    val latest = preferences.readSession()
                    if (latest == null) return FreshProcessSessionOutcome.NO_SESSION
                    if (latest.tokenCipher != stored.tokenCipher || preferences.isSessionRestoreDisabled()) {
                        return FreshProcessSessionOutcome.UNAVAILABLE
                    }
                    val latestOrigin = ServerUrl.origin(serverConfig.normalizedSelection())
                    val requestOrigin = ServerUrl.origin(request.serverUrl)
                    if (latestOrigin == null || latestOrigin != requestOrigin) {
                        return FreshProcessSessionOutcome.UNAVAILABLE
                    }
                    stored = latest
                    retryDelayMs = (retryDelayMs * 2).coerceAtMost(STARTUP_RETRY_MAX_DELAY_MS)
                }
            }
        }
    }

    private suspend fun restoreVerifiedSession(
        request: StartupResumeRequest,
        stored: AtlasPreferences.StoredSession,
        profile: MeDto,
    ): Boolean {
        val username = (profile.username?.takeIf { it.isNotBlank() } ?: stored.username)
            .trim().lowercase(Locale.ROOT)
        if (username.isBlank()) return false
        val user = SessionUser(
            username = username,
            role = profile.role?.takeIf { it.isNotBlank() } ?: stored.role.ifBlank { "user" },
            displayName = profile.displayName?.takeIf { it.isNotBlank() } ?: stored.displayName.ifBlank { username },
            email = profile.email ?: stored.email,
            phone = profile.phone.orEmpty(),
            avatarUrl = profile.avatarUrl?.takeIf { it.isNotBlank() } ?: stored.avatarUrl,
            serverUrl = request.serverUrl,
        )
        val sessionToken = validatedSessionToken(profile.token, request.bearerToken)
        return sessionTransitionMutex.withLock {
            val latestStored = preferences.readSession()
            val selectedOrigin = ServerUrl.origin(serverConfig.normalizedSelection())
            val requestOrigin = ServerUrl.origin(request.serverUrl)
            if (
                preferences.isSessionRestoreDisabled() ||
                latestStored?.tokenCipher != stored.tokenCipher ||
                selectedOrigin == null || selectedOrigin != requestOrigin
            ) {
                return@withLock false
            }
            purgeInMemorySession()
            tokenHolder.set(sessionToken)
            preferences.bindActiveScope(user.serverUrl, user.username)
            offlineCache.bindScope(user.serverUrl, user.username)
            runCatching {
                val encryptedToken = cryptoStore.encrypt(sessionToken)
                preferences.writeSession(
                    serverUrl = user.serverUrl,
                    tokenCipher = encryptedToken,
                    username = user.username,
                    role = user.role,
                    displayName = user.displayName,
                    email = user.email,
                    avatarUrl = user.avatarUrl,
                )
                val savedForServer = preferences.savedAccountsForServer(user.serverUrl).first()
                val savedAccount = savedForServer.firstOrNull {
                    it.username.equals(user.username, ignoreCase = true)
                }
                if (savedAccount != null) {
                    val retainBiometricCipher = savedAccount.canUseDeviceUnlock && sessionToken == request.bearerToken
                    preferences.upsertSavedAccount(
                        savedAccount.copy(
                            role = user.role,
                            displayName = user.displayName,
                            email = user.email,
                            avatarUrl = user.avatarUrl,
                            tokenCipher = if (retainBiometricCipher) savedAccount.tokenCipher else encryptedToken,
                            deviceUnlockEnabled = retainBiometricCipher,
                            deviceUnlockChoiceMade = retainBiometricCipher && savedAccount.deviceUnlockChoiceMade,
                        ),
                    )
                }
            }
            _state.value = SessionState.Active(user)
            true
        }
    }

    private fun deviceIdentityUnavailableFailure() = ApiResult.Failure(
        code = "DEVICE_IDENTITY_UNAVAILABLE",
        message = "L'identité sécurisée de cet appareil est temporairement indisponible. Réessayez dans quelques instants.",
    )

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
        val fingerprint = try {
            deviceIdentity.fingerprint
        } catch (_: DeviceIdentityUnavailableException) {
            return deviceIdentityUnavailableFailure()
        }
        return when (
            val result = authRepository.login(
                username = normalizedUser,
                password = password,
                fingerprint = fingerprint,
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
        val fingerprint = try {
            deviceIdentity.fingerprint
        } catch (_: DeviceIdentityUnavailableException) {
            return deviceIdentityUnavailableFailure()
        }
        return when (
            val result = authRepository.verifyOtp(
                username = normalizedUser,
                password = password,
                otp = code,
                fingerprint = fingerprint,
            )
        ) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> completeLogin(result.data, normalizedUser, rememberAccount)
        }
    }

    /**
     * Verifies a signed single-use e-mail deep link (`POST /api/auth/verify-link`)
     * directly against the Worker server selected by the user.
     */
    suspend fun signInViaEmailLink(
        username: String,
        otp: String,
        sig: String,
        exp: Long,
        fallbackServerOrigin: String? = null,
    ): ApiResult<LoginOutcome> {
        val normalizedUser = username.trim().lowercase(Locale.ROOT)
        val code = otp.filter { it.isDigit() }
        val cleanSig = sig.trim()
        if (normalizedUser.isBlank() || code.length < 4 || cleanSig.isBlank() || exp <= 0L) {
            return ApiResult.Failure("LINK_MALFORMED", "Lien de vérification incomplet ou invalide.")
        }

        // Wait for saved server preference on cold deep-link launch and honor the user's chosen Worker.
        serverConfig.loaded.filter { it }.first()
        if (serverConfig.normalizedSelection() == null) {
            val initialServer = fallbackServerOrigin?.takeIf { it.isNotBlank() }
                ?: AppDetails.DEFAULT_API_BASE
            serverConfig.select(initialServer, rememberServer = true)
            apiProvider.invalidate()
        }

        val fingerprint = try {
            deviceIdentity.fingerprint
        } catch (_: DeviceIdentityUnavailableException) {
            return deviceIdentityUnavailableFailure()
        }
        return when (
            val result = authRepository.verifyLink(
                username = normalizedUser,
                otp = code,
                sig = cleanSig,
                exp = exp,
                fingerprint = fingerprint,
            )
        ) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> completeLogin(result.data, normalizedUser, rememberAccount = true)
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
     * Switches into an opted-in, encrypted saved account belonging strictly to
     * `entry.serverUrl`, flushing every trace of any previous session first.
     */
    suspend fun switchToSavedAccount(entry: SavedAccountEntry, authenticatedToken: String): ApiResult<Unit> {
        if (!entry.canUseDeviceUnlock || !entry.tokenCipher.startsWith(DEVICE_UNLOCK_TOKEN_PREFIX)) {
            return ApiResult.Failure(
                "DEVICE_UNLOCK_DISABLED",
                "Déverrouillage rapide désactivé — reconnectez-vous avec votre mot de passe.",
            )
        }
        val token = authenticatedToken.takeIf(String::isNotBlank)
        if (token == null) {
            preferences.resetSavedAccountDeviceUnlockChoice(entry.serverUrl, entry.username)
            return ApiResult.Failure(
                "SAVED_SESSION_EXPIRED",
                "Session expirée — reconnectez-vous avec votre mot de passe.",
            )
        }
        val savedServerUrl = serverConfig.validateSelection(entry.serverUrl).getOrElse {
            return ApiResult.Failure(
                "SAVED_SERVER_CONFIG",
                "Le serveur enregistré ne peut pas être utilisé dans cette version. Vérifiez son adresse HTTPS puis réessayez.",
            )
        }.baseUrl
        purgeInMemorySession()
        val selectedServer = serverConfig.select(savedServerUrl, rememberServer = false)
        if (selectedServer.isFailure) {
            tokenHolder.clear()
            apiProvider.invalidate()
            signOut()
            return ApiResult.Failure(
                "SAVED_SERVER_CONFIG",
                "Le serveur enregistré ne peut pas être utilisé dans cette version. Vérifiez son adresse HTTPS puis réessayez.",
            )
        }
        // Do not send a saved bearer token using a stale Retrofit instance or a different
        // selected origin. This also re-applies the release build's HTTPS-only server policy.
        apiProvider.invalidate()
        tokenHolder.set(token)
        when (val verify = authRepository.me()) {
            is ApiResult.Failure -> {
                tokenHolder.clear()
                // Keep the encrypted account and its opt-in unlock setting on
                // transient network/server failures. Only a definitive 401
                // means the saved token can no longer authenticate.
                if (verify.httpStatus == 401 || verify.code == "SESSION_EXPIRED") {
                    // Preserve the account tile so the user can reauthenticate with its password.
                    preferences.resetSavedAccountDeviceUnlockChoice(entry.serverUrl, entry.username)
                    return ApiResult.Failure(
                        "SAVED_SESSION_EXPIRED",
                        "Session expirée — reconnectez-vous avec votre mot de passe.",
                        httpStatus = verify.httpStatus,
                    )
                }
                return verify
            }
            is ApiResult.Success -> {
                val me = verify.data
                val resolvedUser = (me.username?.takeIf { it.isNotBlank() } ?: entry.username).lowercase(Locale.ROOT)
                val resolvedRole = me.role?.takeIf { it.isNotBlank() } ?: entry.role.ifBlank { "user" }
                val resolvedDisplay = me.displayName?.takeIf { it.isNotBlank() } ?: entry.displayName.ifBlank { resolvedUser }
                val resolvedEmail = me.email ?: entry.email
                val resolvedAvatar = me.avatarUrl?.takeIf { it.isNotBlank() } ?: entry.avatarUrl
                activateIsolatedSession(
                    serverUrl = savedServerUrl,
                    token = token,
                    username = resolvedUser,
                    role = resolvedRole,
                    displayName = resolvedDisplay,
                    email = resolvedEmail,
                    avatarUrl = resolvedAvatar,
                    phone = me.phone.orEmpty(),
                    rememberAccount = true,
                    savedAccountTokenCipher = entry.tokenCipher,
                )
                return ApiResult.Success(Unit)
            }
        }
    }

    suspend fun forgetSavedAccount(serverUrl: String, username: String) {
        preferences.removeSavedAccount(serverUrl, username)
        DeviceUnlockAuthenticator.forgetSavedAccountKey(serverUrl, username)
        offlineCache.clearAccountScope(serverUrl, username)
    }

    private suspend fun completeLogin(
        response: LoginResponse,
        fallbackUsername: String,
        rememberAccount: Boolean,
    ): ApiResult<LoginOutcome> {
        loginOtpRequirement(response)?.let { return ApiResult.Success(it) }
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
            clearRestoreDisabledAfterPasswordLogin = true,
        )
        // Do not make successful authentication wait for the follow-up profile read.
        // The login response already supplied the essential session fields; a slow or
        // temporarily unreachable /auth/me must not keep the sign-in form busy.
        val expectedScope = (_state.value as? SessionState.Active)?.user?.scopeIdentity
        if (expectedScope != null && tokenHolder.token == token) {
            scope.launch {
                val stillSameSession = isProfileRefreshCurrent(
                    expectedScopeIdentity = expectedScope,
                    expectedToken = token,
                    activeScopeIdentity = (_state.value as? SessionState.Active)?.user?.scopeIdentity,
                    activeToken = tokenHolder.token,
                )
                if (stillSameSession) runCatching { refreshProfile(rememberAccount = rememberAccount) }
            }
        }
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
        phone: String = "",
        rememberAccount: Boolean,
        clearRestoreDisabledAfterPasswordLogin: Boolean = false,
        savedAccountTokenCipher: String? = null,
    ) {
        // A cold deep link can authenticate before startup cleanup finishes. Await cleanup
        // before writing the new active session so its final purge cannot erase this login.
        startupCleanup.await()
        sessionTransitionMutex.withLock {
            if (clearRestoreDisabledAfterPasswordLogin) {
                runCatching { preferences.disableSessionRestore(disabled = false) }
            }
            purgeInMemorySession()
            val cipher = cryptoStore.encrypt(token)
            preferences.bindActiveScope(serverUrl, username)
            offlineCache.bindScope(serverUrl, username)
            tokenHolder.set(token)
            runCatching {
                preferences.markOnboardingDone()
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
                            tokenCipher = savedAccountTokenCipher ?: cipher,
                            deviceUnlockEnabled = savedAccountTokenCipher?.startsWith(DEVICE_UNLOCK_TOKEN_PREFIX) == true,
                            deviceUnlockChoiceMade = savedAccountTokenCipher?.startsWith(DEVICE_UNLOCK_TOKEN_PREFIX) == true,
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
                    phone = phone,
                    avatarUrl = avatarUrl,
                    serverUrl = serverUrl,
                ),
            )
        }
    }

    /** Refresh profile and, when the Worker rotates it, the encrypted session token. */
    suspend fun refreshProfile(rememberAccount: Boolean? = null) {
        val current = (_state.value as? SessionState.Active)?.user ?: return
        val tokenAtRequest = tokenHolder.token ?: return
        val result = authRepository.me()
        if (result !is ApiResult.Success) return

        sessionTransitionMutex.withLock {
            if (!isProfileRefreshCurrent(
                    expectedScopeIdentity = current.scopeIdentity,
                    expectedToken = tokenAtRequest,
                    activeScopeIdentity = (_state.value as? SessionState.Active)?.user?.scopeIdentity,
                    activeToken = tokenHolder.token,
                )
            ) return@withLock

            val me = result.data
            val updated = current.copy(
                username = me.username?.takeIf { it.isNotBlank() } ?: current.username,
                role = me.role?.takeIf { it.isNotBlank() } ?: current.role,
                displayName = me.displayName?.takeIf { it.isNotBlank() } ?: current.displayName,
                email = me.email ?: current.email,
                phone = me.phone ?: current.phone,
                avatarUrl = me.avatarUrl?.takeIf { it.isNotBlank() } ?: current.avatarUrl,
            )
            val currentToken = validatedSessionToken(me.token, tokenAtRequest)
            val tokenRotated = currentToken != tokenAtRequest
            tokenHolder.set(currentToken)
            _state.value = SessionState.Active(updated)
            runCatching {
                val cipher = cryptoStore.encrypt(currentToken)
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
                val existingSaved = savedForServer.firstOrNull {
                    it.username.equals(updated.username, ignoreCase = true)
                }
                val shouldUpdateSaved = rememberAccount == true || existingSaved != null
                if (shouldUpdateSaved) {
                    preferences.upsertSavedAccount(
                        SavedAccountEntry(
                            serverUrl = updated.serverUrl,
                            username = updated.username,
                            role = updated.role,
                            displayName = updated.displayName,
                            email = updated.email,
                            avatarUrl = updated.avatarUrl,
                            tokenCipher = existingSaved?.takeIf { it.canUseDeviceUnlock && !tokenRotated }
                                ?.tokenCipher ?: cipher,
                            deviceUnlockEnabled = existingSaved?.canUseDeviceUnlock == true && !tokenRotated,
                            deviceUnlockChoiceMade = existingSaved?.deviceUnlockChoiceMade == true && !tokenRotated,
                        ),
                    )
                }
            }
        }
    }

    /** Updates profile photo (`avatar_url`) or display name via `PATCH /api/auth/me`. */
    suspend fun updateProfile(
        displayName: String? = null,
        email: String? = null,
        phone: String? = null,
        avatarUrl: String? = null,
    ): ApiResult<Unit> {
        val startingUser = (_state.value as? SessionState.Active)?.user
        val startingToken = tokenHolder.token
        val res = authRepository.updateProfile(
            displayName = displayName,
            email = email,
            phone = phone,
            avatarUrl = avatarUrl,
        )
        if (res is ApiResult.Success && startingUser != null && !startingToken.isNullOrBlank()) {
            var stillCurrent = false
            sessionTransitionMutex.withLock {
                if (!isProfileRefreshCurrent(
                        expectedScopeIdentity = startingUser.scopeIdentity,
                        expectedToken = startingToken,
                        activeScopeIdentity = (_state.value as? SessionState.Active)?.user?.scopeIdentity,
                        activeToken = tokenHolder.token,
                    )
                ) return@withLock
                val updated = startingUser.copy(
                    displayName = displayName ?: startingUser.displayName,
                    email = email ?: startingUser.email,
                    phone = phone ?: startingUser.phone,
                    avatarUrl = avatarUrl ?: startingUser.avatarUrl,
                )
                _state.value = SessionState.Active(updated)
                stillCurrent = true
                runCatching {
                    preferences.writeSession(
                        serverUrl = updated.serverUrl,
                        tokenCipher = cryptoStore.encrypt(startingToken),
                        username = updated.username,
                        role = updated.role,
                        displayName = updated.displayName,
                        email = updated.email,
                        avatarUrl = updated.avatarUrl,
                    )
                }
            }
            if (stillCurrent) runCatching { refreshProfile() }
        }
        return res
    }

    suspend fun signOut(reason: String? = null, notifyServer: Boolean = false) {
        var logoutNotice: String? = null
        sessionTransitionMutex.withLock {
            val tokenAtLogout = authToken
            if (notifyServer && !tokenAtLogout.isNullOrBlank()) {
                val result = try {
                    withTimeoutOrNull(8_000L) { authRepository.logout() }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                val resumePrepared = (result as? ApiResult.Success)?.data?.resumePrepared == true
                if (!resumePrepared) {
                    logoutNotice = LOGOUT_RESUME_UNCONFIRMED_NOTICE
                }
            }
            purgeInMemorySession()
            runCatching { preferences.disableSessionRestore(disabled = true) }
            runCatching { preferences.clearSession() }
            _state.value = SessionState.SignedOut
        }
        (reason?.takeIf(String::isNotBlank) ?: logoutNotice)?.let(_notices::tryEmit)
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
