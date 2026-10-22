package ma.etherealmber.atlascommerce.ui.root

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.DensityMode
import ma.etherealmber.atlascommerce.core.ThemeMode
import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.SessionEvents
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.server.ServerSelection
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.core.update.AppUpdateManager
import ma.etherealmber.atlascommerce.core.update.UpdateState
import ma.etherealmber.atlascommerce.data.OfflineCache
import ma.etherealmber.atlascommerce.data.model.TrustedDeviceDto
import ma.etherealmber.atlascommerce.data.repo.AuthRepository
import ma.etherealmber.atlascommerce.ui.components.AvatarBitmapCache
import javax.inject.Inject

/**
 * Root view model: appearance, motion & user preferences, server choice, session state,
 * profile photo & details, connected devices, offline cache, self-updates and notices.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val preferences: AtlasPreferences,
    private val serverConfig: ServerConfig,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val sessionEvents: SessionEvents,
    private val apiProvider: ApiProvider,
    private val updateManager: AppUpdateManager,
    private val offlineCache: OfflineCache,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val densityMode: StateFlow<DensityMode> = preferences.densityMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, DensityMode.COMPACT)

    val appearanceLoaded: StateFlow<Boolean> = preferences.appearanceLoaded
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val animationsEnabled: StateFlow<Boolean> = preferences.animationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

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

    val server: StateFlow<ServerSelection?> = serverConfig.selection
    val serverLoaded: StateFlow<Boolean> = serverConfig.loaded
    val session: StateFlow<SessionState> = sessionManager.state
    val notices: SharedFlow<String> = sessionManager.notices
    val updateState: StateFlow<UpdateState> = updateManager.state

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
                if (previous != null && next != null && previous != next) {
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
            sessionManager.state.collect { st ->
                if (st is SessionState.Active) {
                    loadDevices()
                    refreshCachedProductCount()
                } else {
                    _devices.value = emptyList()
                    _cachedProductCount.value = 0
                }
            }
        }
    }

    private fun checkUpdatesOnceServerReady() {
        viewModelScope.launch {
            serverConfig.loaded.filter { it }.first()
            if (serverConfig.selection.value != null && autoCheckUpdates.value) {
                updateManager.checkForUpdate(manual = false)
            }
        }
    }

    fun checkForUpdate(manual: Boolean = true) {
        updateManager.checkForUpdate(manual = manual)
    }

    fun startUpdateDownload() {
        updateManager.startDownload()
    }

    fun dismissUpdateBanner() {
        updateManager.dismissBanner()
    }

    fun installDownloadedUpdate(context: Context): Boolean =
        updateManager.installApk(context)

    fun onAppResumed(context: Context) {
        if (autoCheckUpdates.value) {
            updateManager.onAppResumed(context)
        }
        if (sessionManager.state.value is SessionState.Active) {
            viewModelScope.launch { sessionManager.refreshProfile() }
        }
    }

    fun onAppStopped() {
        updateManager.onAppStopped()
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

    fun signOut() {
        viewModelScope.launch { sessionManager.signOut() }
    }
}
