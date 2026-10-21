package ma.etherealmber.atlascommerce.core.persist

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ma.etherealmber.atlascommerce.core.DensityMode
import ma.etherealmber.atlascommerce.core.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton

private val Context.atlasDataStore: DataStore<Preferences> by preferencesDataStore(name = "atlas_prefs")

/**
 * Single source of persistent app preferences: chosen server, appearance and the
 * (encrypted) session. Nothing here is world-readable — DataStore lives in the
 * app-private data directory and the session token is additionally encrypted
 * with an Android Keystore key (see [ma.etherealmber.atlascommerce.core.security.CryptoStore]).
 */
@Singleton
class AtlasPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val store: DataStore<Preferences> = context.atlasDataStore

    private object Keys {
        val API_BASE = stringPreferencesKey("api_base_url")
        val THEME = stringPreferencesKey("theme_mode")
        val DENSITY = stringPreferencesKey("density_mode")
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val SESSION_USERNAME = stringPreferencesKey("session_username")
        val SESSION_ROLE = stringPreferencesKey("session_role")
        val SESSION_DISPLAY = stringPreferencesKey("session_display_name")
        val SESSION_EMAIL = stringPreferencesKey("session_email")
        val ONBOARDING_DONE = booleanPreferencesKey("server_onboarding_done")
    }

    data class StoredSession(
        val tokenCipher: String,
        val username: String,
        val role: String,
        val displayName: String,
        val email: String,
    )

    val apiBaseUrl: Flow<String?> = store.data
        .map { it[Keys.API_BASE] }
        .distinctUntilChanged()

    /** `null` until DataStore has emitted: the UI must not paint a default theme first. */
    val appearanceLoaded: Flow<Boolean> = store.data
        .map { true }
        .distinctUntilChanged()

    val themeMode: Flow<ThemeMode> = store.data
        .map { ThemeMode.fromKey(it[Keys.THEME]) }
        .distinctUntilChanged()

    val densityMode: Flow<DensityMode> = store.data
        .map { DensityMode.fromKey(it[Keys.DENSITY]) }
        .distinctUntilChanged()

    val onboardingDone: Flow<Boolean> = store.data
        .map { it[Keys.ONBOARDING_DONE] ?: false }
        .distinctUntilChanged()

    suspend fun setApiBase(url: String?) {
        store.edit { prefs ->
            if (url.isNullOrBlank()) prefs.remove(Keys.API_BASE) else prefs[Keys.API_BASE] = url
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME] = mode.storageKey }
    }

    suspend fun setDensityMode(mode: DensityMode) {
        store.edit { it[Keys.DENSITY] = mode.storageKey }
    }

    suspend fun markOnboardingDone() {
        store.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun readSession(): StoredSession? {
        val prefs = store.data.first()
        val token = prefs[Keys.SESSION_TOKEN] ?: return null
        return StoredSession(
            tokenCipher = token,
            username = prefs[Keys.SESSION_USERNAME].orEmpty(),
            role = prefs[Keys.SESSION_ROLE].orEmpty(),
            displayName = prefs[Keys.SESSION_DISPLAY].orEmpty(),
            email = prefs[Keys.SESSION_EMAIL].orEmpty(),
        )
    }

    suspend fun writeSession(
        tokenCipher: String,
        username: String,
        role: String,
        displayName: String,
        email: String,
    ) {
        store.edit { prefs ->
            prefs[Keys.SESSION_TOKEN] = tokenCipher
            prefs[Keys.SESSION_USERNAME] = username
            prefs[Keys.SESSION_ROLE] = role
            prefs[Keys.SESSION_DISPLAY] = displayName
            prefs[Keys.SESSION_EMAIL] = email
        }
    }

    suspend fun clearSession() {
        store.edit { prefs ->
            prefs.remove(Keys.SESSION_TOKEN)
            prefs.remove(Keys.SESSION_USERNAME)
            prefs.remove(Keys.SESSION_ROLE)
            prefs.remove(Keys.SESSION_DISPLAY)
            prefs.remove(Keys.SESSION_EMAIL)
        }
    }
}
