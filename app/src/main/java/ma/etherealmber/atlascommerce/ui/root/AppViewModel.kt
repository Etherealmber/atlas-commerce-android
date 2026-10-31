package ma.etherealmber.atlascommerce.ui.root

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.DensityMode
import ma.etherealmber.atlascommerce.core.ThemeMode
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.diagnostics.CrashReportConsent
import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.SessionEvents
import ma.etherealmber.atlascommerce.core.net.VerifyLink
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.persist.SavedAccountEntry
import ma.etherealmber.atlascommerce.core.persist.SavedServerEntry
import ma.etherealmber.atlascommerce.core.security.DeviceIdentity
import ma.etherealmber.atlascommerce.core.security.DeviceUnlockSetupPrompt
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.server.ServerSelection
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.core.update.AppUpdateManager
import ma.etherealmber.atlascommerce.core.update.MirrorPingResult
import ma.etherealmber.atlascommerce.core.update.ReleaseChangelogEntry
import ma.etherealmber.atlascommerce.core.update.UpdateDownloadSource
import ma.etherealmber.atlascommerce.core.update.UpdateState
import ma.etherealmber.atlascommerce.data.OfflineCache
import ma.etherealmber.atlascommerce.data.model.AgentPolicyDto
import ma.etherealmber.atlascommerce.data.model.TrustedDeviceDto
import ma.etherealmber.atlascommerce.data.model.UserReportRequest
import ma.etherealmber.atlascommerce.data.model.UserReportReceiptDto
import ma.etherealmber.atlascommerce.data.repo.AuthRepository
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import ma.etherealmber.atlascommerce.ui.components.AvatarBitmapCache
import javax.inject.Inject

/** Byte-level inventory of app-private storage and the independently managed cache directories. */
data class LocalStorageSummary(
    val totalAppBytes: Long = 0L,
    val cacheBytes: Long = 0L,
    val offlineCatalogBytes: Long = 0L,
    val financeReportBytes: Long = 0L,
    val financeReportCount: Int = 0,
    val diagnosticsBytes: Long = 0L,
    val updateDownloadBytes: Long = 0L,
)

private fun File.directorySizeBytes(): Long =
    runCatching { walkTopDown().filter { it.isFile }.sumOf { it.length() } }.getOrDefault(0L)

/**
 * Root view model: appearance, motion & isolated per-user preferences, server choice,
 * saved accounts/servers, live AI quota policy, profile photo & details, connected devices,
 * offline cache, self-updates and notices.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val preferences: AtlasPreferences,
    private val serverConfig: ServerConfig,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val commerceRepository: CommerceRepository,
    private val sessionEvents: SessionEvents,
    private val apiProvider: ApiProvider,
    private val updateManager: AppUpdateManager,
    private val offlineCache: OfflineCache,
    private val deviceIdentity: DeviceIdentity,
    private val deviceUnlockSetupPrompt: DeviceUnlockSetupPrompt,
) : ViewModel() {

    private var appInForeground = true
    private var lastForegroundRefreshMs = 0L
    private val foregroundRefreshMinimumIntervalMs = 45_000L

    private val _automaticCrashReportsEnabled = MutableStateFlow(false)
    val automaticCrashReportsEnabled: StateFlow<Boolean> = _automaticCrashReportsEnabled.asStateFlow()

    private val _automaticCrashReportsAvailable = MutableStateFlow(false)
    val automaticCrashReportsAvailable: StateFlow<Boolean> = _automaticCrashReportsAvailable.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.state.collect { session ->
                if (!appInForeground && session is SessionState.Active) {
                    sessionManager.lockForBackground()
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            // Discover Crashlytics in the configured Firebase build and restore the user's
            // explicit opt-in. Both state flows used by Settings must reflect persisted consent.
            val available = CrashReportConsent.isConfigured(applicationContext)
            CrashReportConsent.applyStoredConsent(applicationContext)
            _automaticCrashReportsEnabled.value = available && CrashReportConsent.isEnabled(applicationContext)
            _automaticCrashReportsAvailable.value = available
        }
    }

    val deviceShortId: String get() = deviceIdentity.shortId()
    val deviceFingerprint: String get() = deviceIdentity.fingerprint

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val densityMode: StateFlow<DensityMode> = preferences.densityMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, DensityMode.COMPACT)

    val appLanguage: StateFlow<AppLanguage> = (applicationContext as ma.etherealmber.atlascommerce.AtlasApp).activeLanguage
    val appLanguageFollowsSystem: StateFlow<Boolean> = preferences.appLanguageFollowsSystem
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    private val _languageRestartNotice = MutableStateFlow<AppLanguage?>(null)
    val languageRestartNotice = _languageRestartNotice.asStateFlow()
    private val _languageSaveFailed = MutableStateFlow(false)
    val languageSaveFailed = _languageSaveFailed.asStateFlow()
    fun dismissLanguageSaveFailed() { _languageSaveFailed.value = false }
    fun dismissLanguageRestartNotice() { _languageRestartNotice.value = null }

    fun applySavedLanguageAndRestart(restartActivity: () -> Unit) {
        val pendingLanguage = _languageRestartNotice.value ?: return
        viewModelScope.launch {
            val savedLanguage = preferences.appLanguage.first()
            if (savedLanguage != pendingLanguage) {
                _languageSaveFailed.value = true
                return@launch
            }
            (applicationContext as ma.etherealmber.atlascommerce.AtlasApp).activateSavedLanguage(savedLanguage)
            _languageRestartNotice.value = null
            restartActivity()
        }
    }

    val appearanceLoaded: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        preferences.appearanceLoaded, (applicationContext as ma.etherealmber.atlascommerce.AtlasApp).languageLoaded,
    ) { ready, languageReady -> ready && languageReady }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val animationsEnabled: StateFlow<Boolean> = preferences.animationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val developerModeEnabled: StateFlow<Boolean> = preferences.developerModeEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val autoOpenCamera: StateFlow<Boolean> = preferences.autoOpenCamera
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val shakeToReportEnabled: StateFlow<Boolean> = preferences.shakeToReportEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val scanHapticEnabled: StateFlow<Boolean> = preferences.scanHapticEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val scanSoundEnabled: StateFlow<Boolean> = preferences.scanSoundEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val autoAddCartOnScan: StateFlow<Boolean> = preferences.autoAddCartOnScan
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val defaultStartTab: StateFlow<String> = preferences.defaultStartTab
        .stateIn(viewModelScope, SharingStarted.Eagerly, "HOME")

    val defaultClientName: StateFlow<String> = preferences.defaultClientName
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val confirmBeforeSale: StateFlow<Boolean> = preferences.confirmBeforeSale
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val autoCheckUpdates: StateFlow<Boolean> = preferences.autoCheckUpdates
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val parallelUpdateDownload: StateFlow<Boolean> = preferences.parallelUpdateDownload
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val followLatestRelease: StateFlow<Boolean> = preferences.followLatestRelease
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val roleNotificationsEnabled: StateFlow<Boolean> = preferences.roleNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val onboardingDone: StateFlow<Boolean> = preferences.onboardingDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val server: StateFlow<ServerSelection?> = serverConfig.selection
    val serverLoaded: StateFlow<Boolean> = serverConfig.loaded
    val session: StateFlow<SessionState> = sessionManager.state
    val notices: SharedFlow<String> = sessionManager.notices
    val updateState: StateFlow<UpdateState> = updateManager.state
    val mirrorPing: StateFlow<MirrorPingResult> = updateManager.mirrorPing
    val releaseChangelog: StateFlow<List<ReleaseChangelogEntry>> = updateManager.changelog
    val releaseChangelogLoading: StateFlow<Boolean> = updateManager.changelogLoading
    val updateDownloadSource: StateFlow<UpdateDownloadSource> = preferences.updateDownloadSource
        .map { UpdateDownloadSource.fromKey(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UpdateDownloadSource.AUTO)

    val savedServers: StateFlow<List<SavedServerEntry>> = serverConfig.savedServers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val savedAccounts: StateFlow<List<SavedAccountEntry>> = serverConfig.selection
        .flatMapLatest { sel -> preferences.savedAccountsForServer(sel?.baseUrl) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSavedAccounts: StateFlow<List<SavedAccountEntry>> = preferences.savedAccounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deviceUnlockSetupRequests = deviceUnlockSetupPrompt.events

    fun setSavedAccountDeviceUnlock(serverUrl: String, username: String, enabled: Boolean) {
        viewModelScope.launch { preferences.setSavedAccountDeviceUnlock(serverUrl, username, enabled) }
    }

    private val _agentPolicy = MutableStateFlow<AgentPolicyDto?>(null)
    val agentPolicy: StateFlow<AgentPolicyDto?> = _agentPolicy.asStateFlow()

    private val _devices = MutableStateFlow<List<TrustedDeviceDto>>(emptyList())
    val devices: StateFlow<List<TrustedDeviceDto>> = _devices.asStateFlow()

    private val _devicesLoading = MutableStateFlow(false)
    val devicesLoading: StateFlow<Boolean> = _devicesLoading.asStateFlow()

    private val _uploadingAvatar = MutableStateFlow(false)
    val uploadingAvatar: StateFlow<Boolean> = _uploadingAvatar.asStateFlow()

    private val _savingProfile = MutableStateFlow(false)
    val savingProfile: StateFlow<Boolean> = _savingProfile.asStateFlow()

    private val _cachedProductCount = MutableStateFlow(0)
    val cachedProductCount: StateFlow<Int> = _cachedProductCount.asStateFlow()

    private val _localStorageSummary = MutableStateFlow(LocalStorageSummary())
    val localStorageSummary: StateFlow<LocalStorageSummary> = _localStorageSummary.asStateFlow()

    fun refreshLocalStorageSummary() {
        viewModelScope.launch {
            _localStorageSummary.value = withContext(Dispatchers.IO) {
                val cache = applicationContext.cacheDir
                val files = applicationContext.filesDir
                val finance = File(cache, "finance")
                val diagnostics = File(cache, "diagnostics")
                val updates = File(cache, "updates")
                LocalStorageSummary(
                    totalAppBytes = applicationContext.dataDir.directorySizeBytes(),
                    cacheBytes = cache.directorySizeBytes(),
                    offlineCatalogBytes = File(files, "atlas-cache").directorySizeBytes(),
                    financeReportBytes = finance.directorySizeBytes(),
                    financeReportCount = finance.walkTopDown().count { it.isFile && it.extension.equals("pdf", ignoreCase = true) },
                    diagnosticsBytes = diagnostics.directorySizeBytes(),
                    updateDownloadBytes = updates.directorySizeBytes(),
                )
            }
        }
    }

    private fun observeActiveSessionDevices() {
        viewModelScope.launch {
            sessionManager.state
                .map { st -> (st as? SessionState.Active)?.user?.scopeIdentity }
                .distinctUntilChanged()
                .collect { identity ->
                    _devices.value = emptyList()
                    _agentPolicy.value = null
                    _cachedProductCount.value = 0
                    if (identity != null) {
                        loadDevices()
                        refreshAgentPolicy()
                        refreshCachedProductCount()
                    }
                }
        }
    }

    private fun checkUpdatesOnceServerReady() {
        viewModelScope.launch {
            serverConfig.loaded.filter { it }.first()
            updateManager.checkForUpdate(
                manual = false,
                mandatoryOnly = !autoCheckUpdates.value,
            )
        }
    }

    fun checkForUpdate(manual: Boolean = true) {
        updateManager.checkForUpdate(manual = manual)
    }

    fun startUpdateDownload(fromSettings: Boolean = false) {
        updateManager.startDownload(fromSettings = fromSettings)
    }

    fun setUpdateDownloadSource(source: UpdateDownloadSource) {
        viewModelScope.launch {
            preferences.setUpdateDownloadSource(source.key)
        }
    }

    fun pingUpdateMirrors() {
        updateManager.pingMirrors()
    }

    fun dismissUpdateBanner() {
        updateManager.dismissBanner()
    }

    fun hideUpdateDownloadBanner() {
        updateManager.hideDownloadBanner()
    }

    fun showUpdateDownloadBanner() {
        updateManager.showDownloadBanner()
    }

    fun toggleUpdateDownloadBanner() {
        updateManager.toggleDownloadBanner()
    }

    fun repostUpdateNotification() {
        updateManager.repostActiveNotification()
    }

    fun verifyEmailLink(
        link: VerifyLink,
        fallbackServerOrigin: String? = null,
        onDone: (String) -> Unit,
    ) {
        viewModelScope.launch {
            when (
                val res = sessionManager.signInViaEmailLink(
                    username = link.username,
                    otp = link.otp,
                    sig = link.sig,
                    exp = link.exp,
                    fallbackServerOrigin = fallbackServerOrigin,
                )
            ) {
                is ApiResult.Success -> onDone("Connexion vérifiée par e-mail — bienvenue !")
                is ApiResult.Failure -> onDone(res.message)
            }
        }
    }

    fun installDownloadedUpdate(context: Context): Boolean =
        updateManager.installApk(context)

    fun onAppResumed(context: Context) {
        appInForeground = true
        updateManager.onAppResumed(context, autoCheck = autoCheckUpdates.value)
        if (sessionManager.state.value is SessionState.Active) {
            val now = SystemClock.elapsedRealtime()
            if (shouldRefreshForegroundData(lastForegroundRefreshMs, now, foregroundRefreshMinimumIntervalMs)) {
                lastForegroundRefreshMs = now
                viewModelScope.launch {
                    sessionManager.refreshProfile()
                    refreshAgentPolicy()
                }
            }
        }
    }

    fun onAppStopped() {
        appInForeground = false
        updateManager.onAppStopped()
        sessionManager.lockForBackground()
    }

    fun refreshAgentPolicy() {
        if (sessionManager.state.value !is SessionState.Active) return
        viewModelScope.launch {
            when (val res = commerceRepository.agentPolicy()) {
                is ApiResult.Success -> _agentPolicy.value = res.data
                is ApiResult.Failure -> Unit
            }
        }
    }

    fun switchSavedAccount(entry: SavedAccountEntry, onDone: (String) -> Unit) {
        if (!entry.canUseDeviceUnlock) {
            onDone("Sign in with the account password to switch to this account.")
            return
        }
        viewModelScope.launch {
            when (val res = sessionManager.switchToSavedAccount(entry)) {
                is ApiResult.Success -> onDone(
                    "Connecté en tant que ${entry.displayName.ifBlank { entry.username }}.",
                )
                is ApiResult.Failure -> onDone(res.message)
            }
        }
    }

    fun forgetSavedAccount(entry: SavedAccountEntry, onDone: (String) -> Unit) {
        viewModelScope.launch {
            sessionManager.forgetSavedAccount(entry.serverUrl, entry.username)
            onDone("Compte @${entry.username} oublié.")
        }
    }

    fun switchSavedServer(entry: SavedServerEntry, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val current = serverConfig.selection.value?.baseUrl
            if (current != null && !current.equals(entry.url, ignoreCase = true)) {
                sessionManager.signOut()
            }
            serverConfig.select(entry.url, entry.label, rememberServer = true)
            onDone("Serveur sélectionné : ${entry.label.ifBlank { entry.url }}")
        }
    }

    fun forgetSavedServer(entry: SavedServerEntry, onDone: (String) -> Unit) {
        viewModelScope.launch {
            serverConfig.forgetSavedServer(entry.url)
            onDone("Serveur supprimé de cet appareil.")
        }
    }

    fun loadDevices() {
        viewModelScope.launch {
            _devicesLoading.value = true
            when (val res = authRepository.devices()) {
                is ApiResult.Failure -> _devicesLoading.value = false
                is ApiResult.Success -> {
                    _devices.value = res.data
                    _devicesLoading.value = false
                }
            }
        }
    }

    fun refreshCachedProductCount() {
        viewModelScope.launch {
            _cachedProductCount.value = offlineCache.readProducts().size
        }
    }

    fun clearOfflineCache(onDone: (String) -> Unit) {
        viewModelScope.launch {
            offlineCache.clear()
            _cachedProductCount.value = 0
            refreshLocalStorageSummary()
            onDone("Cache catalogue hors ligne vidé.")
        }
    }

    fun revokeDevice(device: TrustedDeviceDto, onDone: (String) -> Unit) {
        val targetId = device.revokeId
        if (targetId.isBlank()) return
        viewModelScope.launch {
            when (val res = authRepository.revokeDevice(targetId)) {
                is ApiResult.Failure -> onDone(res.message)
                is ApiResult.Success -> {
                    onDone("${device.friendlyLabel} révoqué.")
                    if (device.current) {
                        sessionManager.signOut("Session révoquée sur cet appareil.")
                    } else {
                        loadDevices()
                    }
                }
            }
        }
    }

    fun uploadAvatarFromUri(context: Context, uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            _uploadingAvatar.value = true
            val encoded = AvatarBitmapCache.encodeGalleryAvatarDataUri(context, uri)
            if (encoded == null) {
                _uploadingAvatar.value = false
                onDone("Impossible de lire cette image.")
                return@launch
            }
            when (val res = sessionManager.updateProfile(avatarUrl = encoded)) {
                is ApiResult.Failure -> {
                    _uploadingAvatar.value = false
                    onDone(res.message)
                }

                is ApiResult.Success -> {
                    _uploadingAvatar.value = false
                    onDone("Photo de profil mise à jour.")
                }
            }
        }
    }

    fun updateProfileDetails(
        displayName: String,
        email: String,
        onDone: (String) -> Unit,
    ) {
        val cleanName = displayName.trim()
        if (cleanName.isEmpty()) {
            onDone("Le nom affiché ne peut pas être vide.")
            return
        }
        viewModelScope.launch {
            _savingProfile.value = true
            when (
                val res = sessionManager.updateProfile(
                    displayName = cleanName,
                    email = email.trim(),
                )
            ) {
                is ApiResult.Failure -> {
                    _savingProfile.value = false
                    onDone(res.message)
                }

                is ApiResult.Success -> {
                    _savingProfile.value = false
                    onDone("Profil mis à jour.")
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferences.setAppLanguage(language)
            if (preferences.appLanguage.first() != language) {
                _languageSaveFailed.value = true
            } else {
                _languageRestartNotice.value = language.takeIf { it != appLanguage.value }
            }
        }
    }

    fun refreshSystemAppLanguage(language: AppLanguage) {
        if (appLanguageFollowsSystem.value) {
            (applicationContext as ma.etherealmber.atlascommerce.AtlasApp).activateSavedLanguage(language)
        }
    }

    fun followSystemAppLanguage() {
        viewModelScope.launch {
            val current = appLanguage.value
            preferences.useSystemAppLanguage()
            if (!preferences.appLanguageFollowsSystem.first()) {
                _languageSaveFailed.value = true
                return@launch
            }
            val resolved = preferences.appLanguage.first()
            _languageRestartNotice.value = resolved.takeIf { it != current }
        }
    }

    fun setDensityMode(mode: DensityMode) {
        viewModelScope.launch { preferences.setDensityMode(mode) }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setAnimationsEnabled(enabled) }
    }

    fun setDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setDeveloperModeEnabled(enabled) }
    }

    fun setAutomaticCrashReportsEnabled(enabled: Boolean, onDone: (Boolean) -> Unit) {
        if (!CrashReportConsent.setEnabled(applicationContext, enabled)) {
            onDone(false)
            return
        }
        _automaticCrashReportsEnabled.value = enabled
        onDone(true)
    }

    fun clearFinanceReportCache(onDone: (String) -> Unit) {
        viewModelScope.launch {
            val removed = withContext(Dispatchers.IO) {
                val directory = File(applicationContext.cacheDir, "finance")
                val count = directory.walkTopDown().count { it.isFile && it.extension.equals("pdf", true) }
                directory.deleteRecursively()
                count
            }
            refreshLocalStorageSummary()
            onDone(if (removed == 0) "Aucun rapport PDF temporaire à supprimer." else "$removed rapport(s) PDF temporaire(s) supprimé(s).")
        }
    }

    /** Clears local app data only; this does not delete company records from the server. */
    fun clearAllLocalAppData(onDone: (String) -> Unit) {
        viewModelScope.launch {
            val updateFilesCleared = updateManager.clearCachedUpdateFiles()
            sessionManager.signOut()
            serverConfig.clear()
            preferences.clearAllLocalPreferences()
            offlineCache.clearAll()
            withContext(Dispatchers.IO) {
                File(applicationContext.cacheDir, "finance").deleteRecursively()
                File(applicationContext.cacheDir, "diagnostics").deleteRecursively()
                File(applicationContext.filesDir, "atlas-cache").deleteRecursively()
            }
            val updateNote = if (updateFilesCleared) "" else " Le téléchargement de mise à jour actif est conservé jusqu'à sa fin."
            refreshLocalStorageSummary()
            onDone("Données locales supprimées.$updateNote Les données de l'entreprise sur le serveur n'ont pas été modifiées.")
        }
    }

    fun setAutoOpenCamera(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoOpenCamera(enabled) }
    }

    fun setShakeToReportEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setShakeToReportEnabled(enabled) }
    }

    fun setScanHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setScanHapticEnabled(enabled) }
    }

    fun setScanSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setScanSoundEnabled(enabled) }
    }

    fun setAutoAddCartOnScan(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoAddCartOnScan(enabled) }
    }

    fun setDefaultStartTab(tabName: String) {
        viewModelScope.launch { preferences.setDefaultStartTab(tabName) }
    }

    fun setDefaultClientName(clientName: String) {
        viewModelScope.launch { preferences.setDefaultClientName(clientName) }
    }

    fun setConfirmBeforeSale(enabled: Boolean) {
        viewModelScope.launch { preferences.setConfirmBeforeSale(enabled) }
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoCheckUpdates(enabled) }
    }

    fun setParallelUpdateDownload(enabled: Boolean) {
        viewModelScope.launch { preferences.setParallelUpdateDownload(enabled) }
    }

    fun setFollowLatestRelease(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setFollowLatestRelease(enabled)
            updateManager.checkForUpdate(manual = false)
        }
    }

    fun refreshReleaseChangelog() {
        updateManager.refreshChangelog()
    }

    suspend fun submitDiagnosticReport(
        kind: String,
        message: String,
        page: String,
        version: String,
    ): ApiResult<UserReportReceiptDto> {
        AtlasLogBuffer.log("REPORT", "Submitting authenticated user report: kind=$kind page=$page")
        val result = authRepository.submitUserReport(
            UserReportRequest(
                kind = kind,
                message = message,
                page = page.take(200),
                version = version.take(80),
            ),
        )
        when (result) {
            is ApiResult.Success -> AtlasLogBuffer.log(
                "REPORT",
                "Report accepted by Worker: reference=${result.data.reference ?: "none"} githubIssueCreated=${!result.data.issueUrl.isNullOrBlank()}",
            )
            is ApiResult.Failure -> AtlasLogBuffer.log(
                "REPORT",
                "Report submission failed: code=${result.code} http=${result.httpStatus ?: "none"} message=${result.message}",
                "ERROR",
            )
        }
        return result
    }

    fun setRoleNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setRoleNotificationsEnabled(enabled) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { preferences.markOnboardingDone() }
    }

    fun replayOnboarding() {
        viewModelScope.launch { preferences.setOnboardingDone(false) }
    }

    fun signOut() {
        sessionManager.lockForBackground()
    }
}

internal fun shouldRefreshForegroundData(lastRefreshMs: Long, nowMs: Long, minimumIntervalMs: Long): Boolean =
    lastRefreshMs <= 0L || nowMs < lastRefreshMs || nowMs - lastRefreshMs >= minimumIntervalMs
