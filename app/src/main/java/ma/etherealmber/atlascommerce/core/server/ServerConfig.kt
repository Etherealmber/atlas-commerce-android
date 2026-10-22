package ma.etherealmber.atlascommerce.core.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.BuildConfig
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.persist.SavedServerEntry
import ma.etherealmber.atlascommerce.data.OfflineCache
import ma.etherealmber.atlascommerce.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

data class ServerSelection(val baseUrl: String) {
    val display: String get() = ServerUrl.display(baseUrl)
    val host: String get() = ServerUrl.host(baseUrl)
    val isProduction: Boolean get() = ServerUrl.isProduction(baseUrl)
    val isCleartext: Boolean get() = ServerUrl.isCleartext(baseUrl)
}

/**
 * Which Atlas server this install talks to, plus optional user-saved servers.
 */
@Singleton
class ServerConfig @Inject constructor(
    private val preferences: AtlasPreferences,
    private val offlineCache: OfflineCache,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val _selection = MutableStateFlow<ServerSelection?>(null)
    val selection: StateFlow<ServerSelection?> = _selection.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    val savedServers: Flow<List<SavedServerEntry>> = preferences.savedServers

    init {
        scope.launch {
            val stored = runCatching { preferences.apiBaseUrl.first() }.getOrNull()
            if (!stored.isNullOrBlank()) {
                ServerUrl.normalize(stored)?.let { _selection.value = ServerSelection(it) }
            }
            _loaded.value = true
        }
    }

    fun baseUrl(): String = _selection.value?.baseUrl ?: "https://localhost"

    fun normalizedSelection(): String? = _selection.value?.baseUrl

    /**
     * Validates and stores a server address, optionally adding it to the saved
     * server list with an optional friendly label.
     */
    suspend fun select(
        rawUrl: String,
        label: String = "",
        rememberServer: Boolean = true,
    ): Result<ServerSelection> {
        val normalized = ServerUrl.normalize(rawUrl)
            ?: return Result.failure(IllegalArgumentException("Adresse de serveur invalide."))
        if (ServerUrl.isCleartext(normalized) && !BuildConfig.ALLOW_CLEARTEXT) {
            return Result.failure(
                IllegalArgumentException(
                    "Cette version refuse HTTP. Utilisez une adresse HTTPS (ou une version debug pour un serveur local).",
                ),
            )
        }
        preferences.setApiBase(normalized)
        preferences.markOnboardingDone()
        if (rememberServer) {
            preferences.upsertSavedServer(normalized, label.trim())
        }
        val selection = ServerSelection(normalized)
        _selection.value = selection
        return Result.success(selection)
    }

    suspend fun forgetSavedServer(url: String) {
        preferences.removeSavedServer(url)
        offlineCache.clearServerScope(url)
    }

    suspend fun clear() {
        preferences.setApiBase(null)
        _selection.value = null
    }
}
