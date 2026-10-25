package ma.etherealmber.atlascommerce.ui.root

import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.DensityMode
import ma.etherealmber.atlascommerce.core.ThemeMode
import ma.etherealmber.atlascommerce.core.diagnostics.CompressedReportAttachment
import ma.etherealmber.atlascommerce.core.diagnostics.DeviceDiagnosticSnapshot
import ma.etherealmber.atlascommerce.core.diagnostics.DiagnosticReporter
import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.SessionEvents
import ma.etherealmber.atlascommerce.core.net.VerifyLink
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.persist.SavedAccountEntry
import ma.etherealmber.atlascommerce.core.persist.SavedServerEntry
import ma.etherealmber.atlascommerce.core.security.DeviceIdentity
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
import ma.etherealmber.atlascommerce.data.repo.AuthRepository
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import ma.etherealmber.atlascommerce.di.ProbeClient
import ma.etherealmber.atlascommerce.ui.components.AvatarBitmapCache
import okhttp3.OkHttpClient
import javax.inject.Inject

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
    @ProbeClient private val probeClient: OkHttpClient,
) : ViewModel() {

    val deviceShortId: String get() = deviceIdentity.shortId()
    val deviceFingerprint: String get() = deviceIdentity.fingerprint

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val densityMode: StateFlow<DensityMode> = preferences.densityMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, DensityMode.COMPACT)

    val appearanceLoaded: StateFlow<Boolean> = preferences.appearanceLoaded
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val animationsEnabled: StateFlow<Boolean> = preferences.animationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val developerModeEnabled: StateFlow<Boolean> = preferences.developerModeEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val autoOpenCamera: StateFlow<Boolean> = preferences.autoOpenCamera
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    init {
        observeSessionExpiry()
        observeServerChanges()
        observeActiveSessionDevices()
        checkUpdatesOnceServerReady()
    }

    private fun observeSessionExpiry() {
        viewModelScope.launch {
            sessionEvents.expired.collect { message ->
                sessionManager.signOut(message)
            }
        }
    }

    private fun observeServerChanges() {
        viewModelScope.launch {
            var previous = serverConfig.selection.value?.baseUrl
            serverConfig.selection.collect { selection ->
                val next = selection?.baseUrl
                if (previous != null && next != null && !previous.equals(next, ignoreCase = true)) {
                    apiProvider.invalidate()
                    sessionManager.signOut("Serveur modifié — veuillez vous reconnecter.")
                    if (autoCheckUpdates.value) {
                        updateManager.checkForUpdate(manual = false)
                    }
                } else if (previous == null && next != null && autoCheckUpdates.value) {
                    updateManager.checkForUpdate(manual = false)
                }
                previous = next
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
            if (autoCheckUpdates.value) {
                updateManager.checkForUpdate(manual = false)
            }
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
        if (autoCheckUpdates.value) {
            updateManager.onAppResumed(context)
        }
        if (sessionManager.state.value is SessionState.Active) {
            viewModelScope.launch {
                sessionManager.refreshProfile()
                refreshAgentPolicy()
            }
        }
    }

    fun onAppStopped() {
        updateManager.onAppStopped()
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

    fun setDensityMode(mode: DensityMode) {
        viewModelScope.launch { preferences.setDensityMode(mode) }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setAnimationsEnabled(enabled) }
    }

    fun setDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setDeveloperModeEnabled(enabled) }
    }

    fun clearFinanceReportCache(onDone: (String) -> Unit) {
        viewModelScope.launch {
            val removed = withContext(Dispatchers.IO) {
                val directory = File(applicationContext.cacheDir, "finance")
                val count = directory.walkTopDown().count { it.isFile && it.extension.equals("pdf", true) }
                directory.deleteRecursively()
                count
            }
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
            onDone("Données locales supprimées.$updateNote Les données de l'entreprise sur le serveur n'ont pas été modifiées.")
        }
    }

    fun setAutoOpenCamera(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoOpenCamera(enabled) }
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

    suspend fun trySubmitDiagnosticReport(
        title: String,
        category: String,
        markdownBody: String,
        snapshot: DeviceDiagnosticSnapshot,
        logs: String,
        attachments: List<CompressedReportAttachment>,
    ): String? = DiagnosticReporter.tryDirectServerRelay(
        client = probeClient,
        serverBaseUrl = serverConfig.selection.value?.baseUrl,
        title = title,
        category = category,
        markdownBody = markdownBody,
        snapshot = snapshot,
        logs = logs,
        attachments = attachments,
    )

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
        viewModelScope.launch { sessionManager.signOut() }
    }
}
