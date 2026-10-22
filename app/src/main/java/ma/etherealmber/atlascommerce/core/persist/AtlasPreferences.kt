package ma.etherealmber.atlascommerce.core.persist

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ma.etherealmber.atlascommerce.core.DensityMode
import ma.etherealmber.atlascommerce.core.ThemeMode
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.atlasDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "atlas_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

data class SavedServerEntry(
    val url: String,
    val label: String = "",
    val savedAtMs: Long = 0L,
)

data class SavedAccountEntry(
    val serverUrl: String,
    val username: String,
    val role: String,
    val displayName: String,
    val email: String = "",
    val avatarUrl: String = "",
    val tokenCipher: String = "",
    val savedAtMs: Long = 0L,
)

/**
 * Single source of persistent app preferences: chosen server, saved servers,
 * per-server encrypted saved accounts, per-user scoped preferences, and the
 * active encrypted session bound strictly to its origin server.
 */
@Singleton
class AtlasPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val store: DataStore<Preferences> = context.atlasDataStore

    private val activeScopeFlow = MutableStateFlow("")

    private val safeData: Flow<Preferences> = store.data
        .catch { emit(emptyPreferences()) }

    private object Keys {
        val API_BASE = stringPreferencesKey("api_base_url")
        val SAVED_SERVERS = stringPreferencesKey("saved_servers_v1")
        val SAVED_ACCOUNTS = stringPreferencesKey("saved_accounts_v1")
        val THEME = stringPreferencesKey("theme_mode")
        val DENSITY = stringPreferencesKey("density_mode")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val AUTO_OPEN_CAMERA = booleanPreferencesKey("auto_open_camera")
        val SCAN_HAPTIC = booleanPreferencesKey("scan_haptic_enabled")
        val SCAN_SOUND = booleanPreferencesKey("scan_sound_enabled")
        val AUTO_ADD_CART_ON_SCAN = booleanPreferencesKey("auto_add_cart_on_scan")
        val DEFAULT_START_TAB = stringPreferencesKey("default_start_tab")
        val DEFAULT_CLIENT_NAME = stringPreferencesKey("default_client_name")
        val CONFIRM_BEFORE_SALE = booleanPreferencesKey("confirm_before_sale")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val UPDATE_SOURCE = stringPreferencesKey("update_download_source")
        val ROLE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("role_notifications_enabled")
        val SESSION_SERVER = stringPreferencesKey("session_server_url")
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val SESSION_USERNAME = stringPreferencesKey("session_username")
        val SESSION_ROLE = stringPreferencesKey("session_role")
        val SESSION_DISPLAY = stringPreferencesKey("session_display_name")
        val SESSION_EMAIL = stringPreferencesKey("session_email")
        val SESSION_AVATAR = stringPreferencesKey("session_avatar_url")
        val ONBOARDING_DONE = booleanPreferencesKey("server_onboarding_done")
    }

    data class StoredSession(
        val serverUrl: String,
        val tokenCipher: String,
        val username: String,
        val role: String,
        val displayName: String,
        val email: String,
        val avatarUrl: String = "",
    )

    fun bindActiveScope(serverUrl: String?, username: String?) {
        val srv = serverUrl?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val usr = username?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        activeScopeFlow.value = if (srv != null && usr != null) "$srv|$usr" else ""
    }

    val apiBaseUrl: Flow<String?> = safeData
        .map { it.safeString(Keys.API_BASE.name) }
        .distinctUntilChanged()

    val savedServers: Flow<List<SavedServerEntry>> = safeData
        .map { prefs -> decodeSavedServers(prefs.safeString(Keys.SAVED_SERVERS.name)) }
        .distinctUntilChanged()

    val savedAccounts: Flow<List<SavedAccountEntry>> = safeData
        .map { prefs -> decodeSavedAccounts(prefs.safeString(Keys.SAVED_ACCOUNTS.name)) }
        .distinctUntilChanged()

    fun savedAccountsForServer(serverUrl: String?): Flow<List<SavedAccountEntry>> {
        val norm = serverUrl?.trim()?.lowercase().orEmpty()
        return savedAccounts
            .map { list ->
                if (norm.isEmpty()) emptyList()
                else list.filter { it.serverUrl.trim().lowercase() == norm }
            }
            .distinctUntilChanged()
    }

    val appearanceLoaded: Flow<Boolean> = safeData
        .map { true }
        .distinctUntilChanged()

    val themeMode: Flow<ThemeMode> = safeData
        .map { prefs ->
            when (val raw = prefs.rawByName(Keys.THEME.name)) {
                is String -> ThemeMode.fromKey(raw)
                is Number -> when (raw.toInt()) {
                    1 -> ThemeMode.LIGHT
                    2 -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
                is Boolean -> if (raw) ThemeMode.DARK else ThemeMode.LIGHT
                else -> ThemeMode.SYSTEM
            }
        }
        .distinctUntilChanged()

    val densityMode: Flow<DensityMode> = safeData
        .map { prefs ->
            when (val raw = prefs.rawByName(Keys.DENSITY.name)) {
                is String -> DensityMode.fromKey(raw)
                is Number -> if (raw.toInt() == 1) DensityMode.COMFORT else DensityMode.COMPACT
                is Boolean -> if (raw) DensityMode.COMPACT else DensityMode.COMFORT
                else -> DensityMode.COMPACT
            }
        }
        .distinctUntilChanged()

    val animationsEnabled: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.ANIMATIONS_ENABLED.name) ?: true }
        .distinctUntilChanged()

    /** Strictly false by default so entering Scan / Scan Device never starts camera immediately. */
    val autoOpenCamera: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.AUTO_OPEN_CAMERA.name) ?: false }
        .distinctUntilChanged()

    val scanHapticEnabled: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.SCAN_HAPTIC.name) ?: true }
        .distinctUntilChanged()

    val scanSoundEnabled: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.SCAN_SOUND.name) ?: true }
        .distinctUntilChanged()

    val autoAddCartOnScan: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.AUTO_ADD_CART_ON_SCAN.name) ?: false }
        .distinctUntilChanged()

    val defaultStartTab: Flow<String> = combine(safeData, activeScopeFlow) { prefs, scope ->
        val scopedKey = if (scope.isNotEmpty()) "default_start_tab::$scope" else Keys.DEFAULT_START_TAB.name
        prefs.safeString(scopedKey)?.takeIf { s -> s.isNotBlank() } ?: "HOME"
    }.distinctUntilChanged()

    val defaultClientName: Flow<String> = combine(safeData, activeScopeFlow) { prefs, scope ->
        if (scope.isEmpty()) ""
        else prefs.safeString("default_client_name::$scope").orEmpty()
    }.distinctUntilChanged()

    val confirmBeforeSale: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.CONFIRM_BEFORE_SALE.name) ?: false }
        .distinctUntilChanged()

    val autoCheckUpdates: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.AUTO_CHECK_UPDATES.name) ?: true }
        .distinctUntilChanged()

    val updateDownloadSource: Flow<String> = safeData
        .map { it.safeString(Keys.UPDATE_SOURCE.name)?.takeIf { s -> s.isNotBlank() } ?: "auto" }
        .distinctUntilChanged()

    val roleNotificationsEnabled: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.ROLE_NOTIFICATIONS_ENABLED.name) ?: true }
        .distinctUntilChanged()

    val onboardingDone: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.ONBOARDING_DONE.name) ?: false }
        .distinctUntilChanged()

    suspend fun setApiBase(url: String?) {
        safeEdit { prefs ->
            prefs.removeByName(Keys.API_BASE.name)
            if (!url.isNullOrBlank()) prefs[Keys.API_BASE] = url
        }
    }

    suspend fun upsertSavedServer(url: String, label: String = "") {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return
        safeEdit { prefs ->
            val current = decodeSavedServers(prefs.safeString(Keys.SAVED_SERVERS.name))
            val existing = current.firstOrNull { it.url.equals(cleanUrl, ignoreCase = true) }
            val resolvedLabel = label.trim().ifBlank { existing?.label.orEmpty() }
            val updated = listOf(
                SavedServerEntry(
                    url = cleanUrl,
                    label = resolvedLabel,
                    savedAtMs = System.currentTimeMillis(),
                ),
            ) + current.filterNot { it.url.equals(cleanUrl, ignoreCase = true) }
            prefs.putClean(Keys.SAVED_SERVERS, encodeSavedServers(updated.take(15)))
        }
    }

    suspend fun removeSavedServer(url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return
        safeEdit { prefs ->
            val servers = decodeSavedServers(prefs.safeString(Keys.SAVED_SERVERS.name))
                .filterNot { it.url.equals(cleanUrl, ignoreCase = true) }
            prefs.putClean(Keys.SAVED_SERVERS, encodeSavedServers(servers))
            // Also purge every saved account belonging to that server so zero data lingers
            val accounts = decodeSavedAccounts(prefs.safeString(Keys.SAVED_ACCOUNTS.name))
                .filterNot { it.serverUrl.equals(cleanUrl, ignoreCase = true) }
            prefs.putClean(Keys.SAVED_ACCOUNTS, encodeSavedAccounts(accounts))
        }
    }

    suspend fun upsertSavedAccount(entry: SavedAccountEntry) {
        val srv = entry.serverUrl.trim()
        val usr = entry.username.trim().lowercase()
        if (srv.isEmpty() || usr.isEmpty() || entry.tokenCipher.isEmpty()) return
        safeEdit { prefs ->
            val current = decodeSavedAccounts(prefs.safeString(Keys.SAVED_ACCOUNTS.name))
            val updated = listOf(
                entry.copy(
                    serverUrl = srv,
                    username = usr,
                    savedAtMs = System.currentTimeMillis(),
                ),
            ) + current.filterNot {
                it.serverUrl.equals(srv, ignoreCase = true) &&
                    it.username.equals(usr, ignoreCase = true)
            }
            prefs.putClean(Keys.SAVED_ACCOUNTS, encodeSavedAccounts(updated.take(20)))
        }
    }

    suspend fun removeSavedAccount(serverUrl: String, username: String) {
        val srv = serverUrl.trim()
        val usr = username.trim().lowercase()
        safeEdit { prefs ->
            val updated = decodeSavedAccounts(prefs.safeString(Keys.SAVED_ACCOUNTS.name))
                .filterNot {
                    it.serverUrl.equals(srv, ignoreCase = true) &&
                        it.username.equals(usr, ignoreCase = true)
                }
            prefs.putClean(Keys.SAVED_ACCOUNTS, encodeSavedAccounts(updated))
            // Purge user-scoped preferences for this `(server, user)` too
            val prefix = "::${srv.lowercase()}|$usr"
            prefs.asMap().keys
                .filter { it.name.endsWith(prefix) }
                .forEach { prefs.remove(it) }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        safeEdit { prefs -> prefs.putClean(Keys.THEME, mode.storageKey) }
    }

    suspend fun setDensityMode(mode: DensityMode) {
        safeEdit { prefs -> prefs.putClean(Keys.DENSITY, mode.storageKey) }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.ANIMATIONS_ENABLED, enabled) }
    }

    suspend fun setAutoOpenCamera(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.AUTO_OPEN_CAMERA, enabled) }
    }

    suspend fun setScanHapticEnabled(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.SCAN_HAPTIC, enabled) }
    }

    suspend fun setScanSoundEnabled(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.SCAN_SOUND, enabled) }
    }

    suspend fun setAutoAddCartOnScan(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.AUTO_ADD_CART_ON_SCAN, enabled) }
    }

    suspend fun setDefaultStartTab(tabName: String) {
        val scope = activeScopeFlow.value
        safeEdit { prefs ->
            if (scope.isNotEmpty()) {
                prefs.putClean(stringPreferencesKey("default_start_tab::$scope"), tabName)
            } else {
                prefs.putClean(Keys.DEFAULT_START_TAB, tabName)
            }
        }
    }

    suspend fun setDefaultClientName(name: String) {
        val scope = activeScopeFlow.value
        if (scope.isEmpty()) return
        safeEdit { prefs ->
            prefs.putClean(stringPreferencesKey("default_client_name::$scope"), name.trim())
        }
    }

    suspend fun setConfirmBeforeSale(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.CONFIRM_BEFORE_SALE, enabled) }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.AUTO_CHECK_UPDATES, enabled) }
    }

    suspend fun setUpdateDownloadSource(sourceKey: String) {
        safeEdit { prefs -> prefs.putClean(Keys.UPDATE_SOURCE, sourceKey.trim().lowercase()) }
    }

    suspend fun setRoleNotificationsEnabled(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.ROLE_NOTIFICATIONS_ENABLED, enabled) }
    }

    suspend fun markOnboardingDone() {
        safeEdit { prefs -> prefs.putClean(Keys.ONBOARDING_DONE, true) }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.ONBOARDING_DONE, done) }
    }

    suspend fun readSession(): StoredSession? {
        val prefs = runCatching { safeData.first() }.getOrDefault(emptyPreferences())
        val token = prefs.safeString(Keys.SESSION_TOKEN.name) ?: return null
        return StoredSession(
            serverUrl = prefs.safeString(Keys.SESSION_SERVER.name).orEmpty(),
            tokenCipher = token,
            username = prefs.safeString(Keys.SESSION_USERNAME.name).orEmpty(),
            role = prefs.safeString(Keys.SESSION_ROLE.name).orEmpty(),
            displayName = prefs.safeString(Keys.SESSION_DISPLAY.name).orEmpty(),
            email = prefs.safeString(Keys.SESSION_EMAIL.name).orEmpty(),
            avatarUrl = prefs.safeString(Keys.SESSION_AVATAR.name).orEmpty(),
        )
    }

    suspend fun writeSession(
        serverUrl: String,
        tokenCipher: String,
        username: String,
        role: String,
        displayName: String,
        email: String,
        avatarUrl: String = "",
    ) {
        safeEdit { prefs ->
            prefs.putClean(Keys.SESSION_SERVER, serverUrl)
            prefs.putClean(Keys.SESSION_TOKEN, tokenCipher)
            prefs.putClean(Keys.SESSION_USERNAME, username)
            prefs.putClean(Keys.SESSION_ROLE, role)
            prefs.putClean(Keys.SESSION_DISPLAY, displayName)
            prefs.putClean(Keys.SESSION_EMAIL, email)
            prefs.putClean(Keys.SESSION_AVATAR, avatarUrl)
        }
    }

    suspend fun clearSession() {
        safeEdit { prefs ->
            prefs.removeByName(Keys.SESSION_SERVER.name)
            prefs.removeByName(Keys.SESSION_TOKEN.name)
            prefs.removeByName(Keys.SESSION_USERNAME.name)
            prefs.removeByName(Keys.SESSION_ROLE.name)
            prefs.removeByName(Keys.SESSION_DISPLAY.name)
            prefs.removeByName(Keys.SESSION_EMAIL.name)
            prefs.removeByName(Keys.SESSION_AVATAR.name)
        }
    }

    private suspend fun safeEdit(block: (MutablePreferences) -> Unit) {
        runCatching { store.edit(block) }
    }
}

private fun decodeSavedServers(raw: String?): List<SavedServerEntry> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val url = obj.optString("url").trim()
                if (url.isNotEmpty()) {
                    add(
                        SavedServerEntry(
                            url = url,
                            label = obj.optString("label").trim(),
                            savedAtMs = obj.optLong("savedAtMs", 0L),
                        ),
                    )
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun encodeSavedServers(items: List<SavedServerEntry>): String {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(
            JSONObject()
                .put("url", item.url)
                .put("label", item.label)
                .put("savedAtMs", item.savedAtMs),
        )
    }
    return arr.toString()
}

private fun decodeSavedAccounts(raw: String?): List<SavedAccountEntry> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val serverUrl = obj.optString("serverUrl").trim()
                val username = obj.optString("username").trim()
                val tokenCipher = obj.optString("tokenCipher").trim()
                if (serverUrl.isNotEmpty() && username.isNotEmpty() && tokenCipher.isNotEmpty()) {
                    add(
                        SavedAccountEntry(
                            serverUrl = serverUrl,
                            username = username,
                            role = obj.optString("role").trim(),
                            displayName = obj.optString("displayName").trim(),
                            email = obj.optString("email").trim(),
                            avatarUrl = obj.optString("avatarUrl").trim(),
                            tokenCipher = tokenCipher,
                            savedAtMs = obj.optLong("savedAtMs", 0L),
                        ),
                    )
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun encodeSavedAccounts(items: List<SavedAccountEntry>): String {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(
            JSONObject()
                .put("serverUrl", item.serverUrl)
                .put("username", item.username)
                .put("role", item.role)
                .put("displayName", item.displayName)
                .put("email", item.email)
                .put("avatarUrl", item.avatarUrl)
                .put("tokenCipher", item.tokenCipher)
                .put("savedAtMs", item.savedAtMs),
        )
    }
    return arr.toString()
}

private fun Preferences.rawByName(name: String): Any? =
    asMap().entries.firstOrNull { it.key.name == name }?.value

private fun Preferences.safeString(name: String): String? =
    rawByName(name) as? String

private fun Preferences.safeBoolean(name: String): Boolean? =
    when (val raw = rawByName(name)) {
        is Boolean -> raw
        is Number -> raw.toInt() != 0
        is String -> raw.equals("true", ignoreCase = true) || raw == "1"
        else -> null
    }

private fun MutablePreferences.removeByName(name: String) {
    val existing = asMap().keys.filter { it.name == name }
    existing.forEach { remove(it) }
}

private fun <T> MutablePreferences.putClean(key: Preferences.Key<T>, value: T) {
    removeByName(key.name)
    this[key] = value
}
