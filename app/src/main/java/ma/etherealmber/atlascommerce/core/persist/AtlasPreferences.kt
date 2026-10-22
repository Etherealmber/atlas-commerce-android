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
 * Single source of persistent app preferences: chosen server, appearance and the
 * (encrypted) session. Nothing here is world-readable — DataStore lives in the
 * app-private data directory and the session token is additionally encrypted
 * with an Android Keystore key (see [ma.etherealmber.atlascommerce.core.security.CryptoStore]).
 *
 * All reads are guarded against corrupted protobuf files and type-mismatched keys
 * left behind by older installs (e.g. upgrading in-place from 1.0.4 / 1.0.5).
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

    /** False in consumers until DataStore has emitted: the UI must not flash a default theme first. */
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
        safeEdit { prefs ->
            prefs.putClean(Keys.THEME, mode.storageKey)
        }
    }

    suspend fun setDensityMode(mode: DensityMode) {
        safeEdit { prefs ->
            prefs.putClean(Keys.DENSITY, mode.storageKey)
        }
    }

    suspend fun markOnboardingDone() {
        safeEdit { prefs ->
            prefs.putClean(Keys.ONBOARDING_DONE, true)
        }
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
