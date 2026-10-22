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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ma.etherealmber.atlascommerce.core.DensityMode
import ma.etherealmber.atlascommerce.core.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton

private val Context.atlasDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "atlas_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * Single source of persistent app preferences: chosen server, appearance, motion,
 * camera/scanner behavior, quick-sale defaults, and the encrypted session.
 */
@Singleton
class AtlasPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val store: DataStore<Preferences> = context.atlasDataStore

    private val safeData: Flow<Preferences> = store.data
        .catch { emit(emptyPreferences()) }

    private object Keys {
        val API_BASE = stringPreferencesKey("api_base_url")
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
        val ROLE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("role_notifications_enabled")
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val SESSION_USERNAME = stringPreferencesKey("session_username")
        val SESSION_ROLE = stringPreferencesKey("session_role")
        val SESSION_DISPLAY = stringPreferencesKey("session_display_name")
        val SESSION_EMAIL = stringPreferencesKey("session_email")
        val SESSION_AVATAR = stringPreferencesKey("session_avatar_url")
        val ONBOARDING_DONE = booleanPreferencesKey("server_onboarding_done")
    }

    data class StoredSession(
        val tokenCipher: String,
        val username: String,
        val role: String,
        val displayName: String,
        val email: String,
        val avatarUrl: String = "",
    )

    val apiBaseUrl: Flow<String?> = safeData
        .map { it.safeString(Keys.API_BASE.name) }
        .distinctUntilChanged()

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

    val defaultStartTab: Flow<String> = safeData
        .map { it.safeString(Keys.DEFAULT_START_TAB.name)?.takeIf { s -> s.isNotBlank() } ?: "HOME" }
        .distinctUntilChanged()

    val defaultClientName: Flow<String> = safeData
        .map { it.safeString(Keys.DEFAULT_CLIENT_NAME.name).orEmpty() }
        .distinctUntilChanged()

    val confirmBeforeSale: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.CONFIRM_BEFORE_SALE.name) ?: false }
        .distinctUntilChanged()

    val autoCheckUpdates: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.AUTO_CHECK_UPDATES.name) ?: true }
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
        safeEdit { prefs -> prefs.putClean(Keys.DEFAULT_START_TAB, tabName) }
    }

    suspend fun setDefaultClientName(name: String) {
        safeEdit { prefs -> prefs.putClean(Keys.DEFAULT_CLIENT_NAME, name.trim()) }
    }

    suspend fun setConfirmBeforeSale(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.CONFIRM_BEFORE_SALE, enabled) }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.AUTO_CHECK_UPDATES, enabled) }
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
            tokenCipher = token,
            username = prefs.safeString(Keys.SESSION_USERNAME.name).orEmpty(),
            role = prefs.safeString(Keys.SESSION_ROLE.name).orEmpty(),
            displayName = prefs.safeString(Keys.SESSION_DISPLAY.name).orEmpty(),
            email = prefs.safeString(Keys.SESSION_EMAIL.name).orEmpty(),
            avatarUrl = prefs.safeString(Keys.SESSION_AVATAR.name).orEmpty(),
        )
    }

    suspend fun writeSession(
        tokenCipher: String,
        username: String,
        role: String,
        displayName: String,
        email: String,
        avatarUrl: String = "",
    ) {
        safeEdit { prefs ->
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
