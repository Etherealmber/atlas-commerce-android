package ma.etherealmber.atlascommerce.core.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.BuildConfig
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
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
 * Which Atlas server this install talks to.
 *
 * The choice is made *before* login and persisted, so a shop can host its own
 * worker. `selection == null` means "not chosen yet" and the UI shows the
 * server picker; [baseUrl] still returns the production default so background
 * code never builds a request against an empty host.
 */
@Singleton
class ServerConfig @Inject constructor(
    private val preferences: AtlasPreferences,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val _selection = MutableStateFlow<ServerSelection?>(null)
    val selection: StateFlow<ServerSelection?> = _selection.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        scope.launch {
            val stored = runCatching { preferences.apiBaseUrl.first() }.getOrNull()
            if (!stored.isNullOrBlank()) {
                ServerUrl.normalize(stored)?.let { _selection.value = ServerSelection(it) }
            }
            _loaded.value = true
        }
    }

    fun baseUrl(): String = _selection.value?.baseUrl ?: AppDetails.DEFAULT_API_BASE

    fun normalizedSelection(): String? = _selection.value?.baseUrl

    /**
     * Validates and stores a server address. [Result.failure] carries a
     * user-facing French message when the address is refused.
     */
    suspend fun select(rawUrl: String): Result<ServerSelection> {
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
        val selection = ServerSelection(normalized)
        _selection.value = selection
        return Result.success(selection)
    }

    suspend fun clear() {
        preferences.setApiBase(null)
        _selection.value = null
    }
}
