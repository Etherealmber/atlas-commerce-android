package ma.etherealmber.atlascommerce.ui.root

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.DensityMode
import ma.etherealmber.atlascommerce.core.ThemeMode
import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.net.SessionEvents
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.server.ServerConfig
import ma.etherealmber.atlascommerce.core.server.ServerSelection
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.core.update.AppUpdateManager
import ma.etherealmber.atlascommerce.core.update.UpdateState
import javax.inject.Inject

/**
 * Root view model: appearance, server choice, session state, self-updates and
 * app-wide notices. Screens never talk to preferences directly.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val preferences: AtlasPreferences,
    private val serverConfig: ServerConfig,
    private val sessionManager: SessionManager,
    private val sessionEvents: SessionEvents,
    private val apiProvider: ApiProvider,
    private val updateManager: AppUpdateManager,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val densityMode: StateFlow<DensityMode> = preferences.densityMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, DensityMode.COMPACT)

    /** False until the stored appearance is known, so the first frame is neutral. */
    val appearanceLoaded: StateFlow<Boolean> = preferences.appearanceLoaded
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val server: StateFlow<ServerSelection?> = serverConfig.selection
    val serverLoaded: StateFlow<Boolean> = serverConfig.loaded
    val session: StateFlow<SessionState> = sessionManager.state
    val notices: SharedFlow<String> = sessionManager.notices
    val updateState: StateFlow<UpdateState> = updateManager.state

    init {
        observeSessionExpiry()
        observeServerChanges()
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
                    updateManager.checkForUpdate(manual = false)
                } else if (previous == null && next != null) {
                    updateManager.checkForUpdate(manual = false)
                }
                previous = next
            }
        }
    }

    private fun checkUpdatesOnceServerReady() {
        viewModelScope.launch {
            serverConfig.loaded.filter { it }.first()
            if (serverConfig.selection.value != null) {
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
        updateManager.onAppResumed(context)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setDensityMode(mode: DensityMode) {
        viewModelScope.launch { preferences.setDensityMode(mode) }
    }

    fun signOut() {
        viewModelScope.launch { sessionManager.signOut() }
    }
}
