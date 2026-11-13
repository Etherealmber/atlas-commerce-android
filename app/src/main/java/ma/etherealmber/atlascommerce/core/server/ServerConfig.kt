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
import ma.etherealmber.atlascommerce.core.security.DeviceUnlockAuthenticator
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
            try {
                kotlinx.coroutines.withTimeoutOrNull(2_000L) {
                    val stored = runCatching { preferences.apiBaseUrl.first() }.getOrNull()
                    if (!stored.isNullOrBlank()) {
                        ServerUrl.normalize(stored)?.let { _selection.value = ServerSelection(it) }
                    }
                }
            } finally {
                _loaded.value = true
            }
        }
    }

    fun baseUrl(): String = _selection.value?.baseUrl ?: "https://localhost"

    fun normalizedSelection(): String? = _selection.value?.baseUrl

    /** Validates a server address without changing the active selection. */
    fun validateSelection(rawUrl: String): Result<ServerSelection> {
        val normalized = ServerUrl.normalize(rawUrl)
            ?: return Result.failure(IllegalArgumentException("Adresse de serveur invalide."))
        if (ServerUrl.isCleartext(normalized) && !BuildConfig.ALLOW_CLEARTEXT) {
            return Result.failure(
                IllegalArgumentException(
                    "Cette version refuse HTTP. Utilisez une adresse HTTPS (ou une version debug pour un serveur local).",
                ),
            )
        }
        return Result.success(ServerSelection(normalized))
    }

    /** Stores the selected address and, optionally, remembers it for the server picker. */
    suspend fun select(
        rawUrl: String,
        label: String = "",
        rememberServer: Boolean = true,
    ): Result<ServerSelection> {
        val selection = validateSelection(rawUrl).getOrElse { return Result.failure(it) }
        val normalized = selection.baseUrl
        preferences.setApiBase(normalized)
        if (rememberServer) {
            preferences.upsertSavedServer(normalized, label.trim())
        }
        _selection.value = selection
        return Result.success(selection)
    }

    suspend fun forgetSavedServer(url: String) {
        preferences.savedAccountsForServer(url).first().forEach { account ->
            DeviceUnlockAuthenticator.forgetSavedAccountKey(account.serverUrl, account.username)
        }
        preferences.removeSavedServer(url)
        offlineCache.clearServerScope(url)
    }

    suspend fun clear() {
        preferences.setApiBase(null)
        _selection.value = null
    }
}
