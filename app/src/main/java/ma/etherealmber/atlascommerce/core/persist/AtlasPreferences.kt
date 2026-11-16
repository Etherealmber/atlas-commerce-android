package ma.etherealmber.atlascommerce.core.persist

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ma.etherealmber.atlascommerce.core.security.CryptoStore
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.DensityMode
import ma.etherealmber.atlascommerce.core.ThemeMode
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

// du2 is bound to a per-operation strong biometric key. Legacy du1 envelopes intentionally
// fall back to account-password sign-in instead of being upgraded without fresh authentication.
const val LEGACY_DEVICE_UNLOCK_TOKEN_PREFIX = "du1:"
const val DEVICE_UNLOCK_TOKEN_PREFIX = "du2:"
private const val MOBILE_SETTINGS_MIGRATION_OWNER = "mobile_preferences_legacy_migration_owner"

private fun mobileScopeHash(scope: String): String = java.security.MessageDigest.getInstance("SHA-256")
    .digest(scope.toByteArray(Charsets.UTF_8))
    .take(12)
    .joinToString("") { "%02x".format(it.toInt() and 0xff) }

private fun mobileSettingsKey(scope: String): String = "mobile_sync_settings::${mobileScopeHash(scope)}"
private fun mobileFrequencyKey(scope: String): String = "mobile_sync_frequency::${mobileScopeHash(scope)}"
private fun mobileFrequencyDirtyKey(scope: String): String = "mobile_sync_frequency_dirty::${mobileScopeHash(scope)}"
private fun mobileRevisionKey(scope: String): String = "mobile_sync_revision::${mobileScopeHash(scope)}"
private fun mobileMetadataKey(scope: String): String = "mobile_sync_metadata::${mobileScopeHash(scope)}"

data class MobilePreferenceSyncMetadata(
    val revision: Long,
    val serverTime: String,
    val syncFrequency: String,
    val lastSyncedAt: String?,
    val nextSyncAt: String?,
)

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
    val deviceUnlockEnabled: Boolean = false,
    val deviceUnlockChoiceMade: Boolean = false,
    val savedAtMs: Long = 0L,
) {
    val canUseDeviceUnlock: Boolean
        get() = deviceUnlockEnabled && deviceUnlockChoiceMade && tokenCipher.startsWith(DEVICE_UNLOCK_TOKEN_PREFIX)

    val hasLegacyDeviceUnlockEnvelope: Boolean
        get() = tokenCipher.startsWith(LEGACY_DEVICE_UNLOCK_TOKEN_PREFIX)
}

/**
 * Single source of persistent app preferences: chosen server, saved servers,
 * per-server encrypted saved accounts, per-user scoped preferences, and the
 * active encrypted session bound strictly to its origin server.
 */
@Singleton
class AtlasPreferences @Inject constructor(
    @ApplicationContext context: Context,
    private val cryptoStore: CryptoStore,
) {

    constructor(context: Context) : this(context, CryptoStore())

    private val store: DataStore<Preferences> = context.atlasDataStore

    private val activeScopeFlow = MutableStateFlow("")

    private val safeData: Flow<Preferences> = store.data
        .catch { emit(emptyPreferences()) }
        .map(::decryptPersistedPreferences)

    private object Keys {
        val API_BASE = stringPreferencesKey("api_base_url")
        val SAVED_SERVERS = stringPreferencesKey("saved_servers_v1")
        val SAVED_ACCOUNTS = stringPreferencesKey("saved_accounts_v1")
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("app_language")
        val DENSITY = stringPreferencesKey("density_mode")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val DEVELOPER_MODE_ENABLED = booleanPreferencesKey("developer_mode_enabled")
        val AUTO_OPEN_CAMERA = booleanPreferencesKey("auto_open_camera")
        val SHAKE_TO_REPORT = booleanPreferencesKey("shake_to_report_enabled")
        val SCAN_HAPTIC = booleanPreferencesKey("scan_haptic_enabled")
        val SCAN_SOUND = booleanPreferencesKey("scan_sound_enabled")
        val AUTO_ADD_CART_ON_SCAN = booleanPreferencesKey("auto_add_cart_on_scan")
        val DEFAULT_START_TAB = stringPreferencesKey("default_start_tab")
        val DEFAULT_CLIENT_NAME = stringPreferencesKey("default_client_name")
        val CONFIRM_BEFORE_SALE = booleanPreferencesKey("confirm_before_sale")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val PARALLEL_UPDATE_DOWNLOAD = booleanPreferencesKey("parallel_update_download")
        val FOLLOW_LATEST_RELEASE = booleanPreferencesKey("follow_latest_release")
        val UPDATE_SOURCE = stringPreferencesKey("update_download_source")
        val PRODUCTS_LIST_LAYOUT = booleanPreferencesKey("products_list_layout")
        val ROLE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("role_notifications_enabled")
        val SESSION_SERVER = stringPreferencesKey("session_server_url")
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val SESSION_USERNAME = stringPreferencesKey("session_username")
        val SESSION_ROLE = stringPreferencesKey("session_role")
        val SESSION_DISPLAY = stringPreferencesKey("session_display_name")
        val SESSION_EMAIL = stringPreferencesKey("session_email")
        val SESSION_AVATAR = stringPreferencesKey("session_avatar_url")
        val SESSION_RESTORE_DISABLED = booleanPreferencesKey("session_restore_disabled")
        val ONBOARDING_DONE = booleanPreferencesKey("server_onboarding_done")
        val ENCRYPTED_PAYLOAD = stringPreferencesKey("atlas_encrypted_preferences_v1")
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

    /** Persist the outgoing account's safe settings and hydrate only the incoming account. */
    suspend fun activateMobilePreferenceScope(serverUrl: String, username: String) {
        val targetScope = "${serverUrl.trim().lowercase()}|${username.trim().lowercase()}"
        val previousScope = activeScopeFlow.value
        if (previousScope.isNotBlank() && previousScope != targetScope) {
            persistScopedMobileSettings(previousScope, captureMobileSettings(safeData.first(), previousScope))
        }
        val current = safeData.first()
        val stored = decodeMobileSettings(current.safeString(mobileSettingsKey(targetScope)))
        val isLegacySeed = stored == null && current.safeString(MOBILE_SETTINGS_MIGRATION_OWNER).isNullOrBlank()
        val settings = stored ?: if (isLegacySeed) captureMobileSettings(current, targetScope) else defaultMobileSettings()
        if (stored == null) {
            safeEdit { prefs ->
                prefs.putClean(stringPreferencesKey(mobileSettingsKey(targetScope)), encodeMobileSettings(settings))
                val defaults = defaultMobileSettings()
                val dirty = if (isLegacySeed) settings.keys.filterTo(mutableSetOf()) { settings[it] != defaults[it] } else emptySet()
                prefs.putClean(stringPreferencesKey(mobileSettingsDirtyKey(targetScope)), encodeMobileDirtyKeys(dirty))
                if (isLegacySeed) {
                    prefs.putClean(stringPreferencesKey(MOBILE_SETTINGS_MIGRATION_OWNER), mobileScopeHash(targetScope))
                }
            }
        }
        activeScopeFlow.value = targetScope
        applyMobileSettingsToPreferences(settings, targetScope)
    }

    /** Flush the current account snapshot before its active scope is cleared. */
    suspend fun persistAndClearMobilePreferenceScope() {
        val scope = activeScopeFlow.value
        if (scope.isNotBlank()) persistScopedMobileSettings(scope, captureMobileSettings(safeData.first(), scope))
        activeScopeFlow.value = ""
    }

    suspend fun mobilePreferenceSettings(): Map<String, Any?> =
        captureMobileSettings(safeData.first(), activeScopeFlow.value)

    suspend fun applyMobilePreferenceSettings(
        settings: Map<String, Any?>,
        expectedScope: String? = null,
        markDirty: Boolean = true,
    ): Boolean {
        if (!ma.etherealmber.atlascommerce.data.model.isValidMobilePreferenceSettings(settings)) return false
        val scope = activeScopeFlow.value
        if (scope.isBlank() || (expectedScope != null && scope != expectedScope)) return false
        val before = safeData.first()
        val oldSettings = captureMobileSettings(before, scope)
        val dirtyKeys = if (markDirty) {
            decodeMobileDirtyKeys(before.safeString(mobileSettingsDirtyKey(scope))) +
                settings.keys.filter { settings[it] != oldSettings[it] }
        } else emptySet()
        return safeEditResult { prefs ->
            check(activeScopeFlow.value == scope && (expectedScope == null || activeScopeFlow.value == expectedScope))
            writeMobileSettingsToPreferences(prefs, settings, scope)
            prefs.putClean(stringPreferencesKey(mobileSettingsKey(scope)), encodeMobileSettings(settings))
            prefs.putClean(stringPreferencesKey(mobileSettingsDirtyKey(scope)), encodeMobileDirtyKeys(dirtyKeys))
        }
    }

    suspend fun applySuccessfulMobilePreferenceSettings(
        serverSettings: Map<String, Any?>,
        submittedSettings: Map<String, Any?>,
        expectedScope: String,
    ): Boolean {
        if (!ma.etherealmber.atlascommerce.data.model.isValidMobilePreferenceSettings(serverSettings) ||
            !ma.etherealmber.atlascommerce.data.model.isValidMobilePreferenceSettings(submittedSettings) ||
            activeScopeFlow.value != expectedScope
        ) return false
        return safeEditResult { prefs ->
            check(activeScopeFlow.value == expectedScope)
            val current = captureMobileSettings(prefs, expectedScope)
            val changedDuringRequest = current.keys.filter { current[it] != submittedSettings[it] }.toSet()
            val merged = serverSettings + changedDuringRequest.associateWith { current[it] }
            check(ma.etherealmber.atlascommerce.data.model.isValidMobilePreferenceSettings(merged))
            writeMobileSettingsToPreferences(prefs, merged, expectedScope)
            prefs.putClean(stringPreferencesKey(mobileSettingsKey(expectedScope)), encodeMobileSettings(merged))
            prefs.putClean(stringPreferencesKey(mobileSettingsDirtyKey(expectedScope)), encodeMobileDirtyKeys(changedDuringRequest))
        }
    }

    suspend fun applyRemoteMobilePreferenceSettings(
        serverSettings: Map<String, Any?>,
        expectedScope: String,
    ): Boolean {
        if (!ma.etherealmber.atlascommerce.data.model.isValidMobilePreferenceSettings(serverSettings) ||
            activeScopeFlow.value != expectedScope
        ) return false
        return safeEditResult { prefs ->
            check(activeScopeFlow.value == expectedScope)
            val current = captureMobileSettings(prefs, expectedScope)
            val dirtyKeys = decodeMobileDirtyKeys(prefs.safeString(mobileSettingsDirtyKey(expectedScope)))
            val merged = serverSettings + dirtyKeys.associateWith { current[it] }
            check(ma.etherealmber.atlascommerce.data.model.isValidMobilePreferenceSettings(merged))
            writeMobileSettingsToPreferences(prefs, merged, expectedScope)
            prefs.putClean(stringPreferencesKey(mobileSettingsKey(expectedScope)), encodeMobileSettings(merged))
            prefs.putClean(stringPreferencesKey(mobileSettingsDirtyKey(expectedScope)), encodeMobileDirtyKeys(dirtyKeys))
        }
    }

    suspend fun mobilePreferenceFrequency(): String {
        val scope = activeScopeFlow.value
        val raw = safeData.first().safeString(mobileFrequencyKey(scope))
        return raw?.takeIf { it in setOf("daily", "weekly", "monthly", "yearly") } ?: "daily"
    }

    suspend fun mobilePreferenceDirtySettings(scope: String = activeScopeFlow.value): Set<String> =
        if (scope.isBlank()) emptySet() else decodeMobileDirtyKeys(safeData.first().safeString(mobileSettingsDirtyKey(scope)))

    suspend fun mobilePreferenceSettingsDirty(scope: String = activeScopeFlow.value): Boolean =
        mobilePreferenceDirtySettings(scope).isNotEmpty()

    suspend fun hasAtLeastKnownMobilePreferenceRevision(revision: Long, scope: String): Boolean {
        if (revision < 0 || scope.isBlank() || activeScopeFlow.value != scope) return false
        val known = safeData.first().safeString(mobileRevisionKey(scope))?.toLongOrNull() ?: 0L
        return revision >= known
    }

    suspend fun recordMobilePreferenceRevision(revision: Long, expectedScope: String): Boolean {
        if (revision < 0 || activeScopeFlow.value != expectedScope) return false
        val current = safeData.first().safeString(mobileRevisionKey(expectedScope))?.toLongOrNull() ?: 0L
        if (revision < current) return false
        return safeEditResult { prefs ->
            check(activeScopeFlow.value == expectedScope)
            val latest = prefs.safeString(mobileRevisionKey(expectedScope))?.toLongOrNull() ?: 0L
            check(revision >= latest)
            prefs.putClean(stringPreferencesKey(mobileRevisionKey(expectedScope)), revision.toString())
        }
    }

    suspend fun recordMobilePreferenceMetadata(
        revision: Long,
        serverTime: String,
        syncFrequency: String,
        lastSyncedAt: String?,
        nextSyncAt: String?,
        expectedScope: String,
    ): Boolean {
        if (revision < 0 || syncFrequency !in setOf("daily", "weekly", "monthly", "yearly") ||
            activeScopeFlow.value != expectedScope
        ) return false
        val json = JSONObject()
            .put("revision", revision)
            .put("server_time", serverTime)
            .put("sync_frequency", syncFrequency)
            .put("last_synced_at", lastSyncedAt ?: JSONObject.NULL)
            .put("next_sync_at", nextSyncAt ?: JSONObject.NULL)
            .toString()
        return safeEditResult { prefs ->
            check(activeScopeFlow.value == expectedScope)
            prefs.putClean(stringPreferencesKey(mobileMetadataKey(expectedScope)), json)
        }
    }

    suspend fun mobilePreferenceSyncMetadata(scope: String = activeScopeFlow.value): MobilePreferenceSyncMetadata? {
        if (scope.isBlank()) return null
        val raw = safeData.first().safeString(mobileMetadataKey(scope)) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val revision = json.optLong("revision", -1L)
            val serverTime = json.optString("server_time")
            val frequency = json.optString("sync_frequency")
            if (revision < 0 || serverTime.isBlank() || frequency !in setOf("daily", "weekly", "monthly", "yearly")) return null
            MobilePreferenceSyncMetadata(
                revision = revision,
                serverTime = serverTime,
                syncFrequency = frequency,
                lastSyncedAt = json.optString("last_synced_at").takeIf { it.isNotBlank() && it != "null" },
                nextSyncAt = json.optString("next_sync_at").takeIf { it.isNotBlank() && it != "null" },
            )
        }.getOrNull()
    }

    private suspend fun persistCurrentMobileSettingsAsDirty() {
        val scope = activeScopeFlow.value
        if (scope.isBlank()) return
        val current = safeData.first()
        val currentSettings = captureMobileSettings(current, scope)
        val previousSettings = decodeMobileSettings(current.safeString(mobileSettingsKey(scope))) ?: defaultMobileSettings()
        val changedKeys = currentSettings.keys.filter { currentSettings[it] != previousSettings[it] }
        if (changedKeys.isEmpty()) return
        val dirtyKeys = decodeMobileDirtyKeys(current.safeString(mobileSettingsDirtyKey(scope))) + changedKeys
        safeEdit { prefs ->
            if (activeScopeFlow.value == scope) {
                prefs.putClean(stringPreferencesKey(mobileSettingsKey(scope)), encodeMobileSettings(currentSettings))
                prefs.putClean(stringPreferencesKey(mobileSettingsDirtyKey(scope)), encodeMobileDirtyKeys(dirtyKeys))
            }
        }
    }

    suspend fun setMobilePreferenceFrequency(frequency: String): Boolean {
        if (frequency !in setOf("daily", "weekly", "monthly", "yearly")) return false
        val scope = activeScopeFlow.value
        if (scope.isBlank()) return false
        return safeEditResult {
            it.putClean(stringPreferencesKey(mobileFrequencyKey(scope)), frequency)
            it.putClean(stringPreferencesKey(mobileFrequencyDirtyKey(scope)), "true")
        }
    }

    suspend fun mobilePreferenceFrequencyDirty(scope: String = activeScopeFlow.value): Boolean =
        scope.isNotBlank() && safeData.first().safeString(mobileFrequencyDirtyKey(scope)) == "true"

    suspend fun acceptServerMobilePreferenceFrequency(frequency: String, expectedScope: String): Boolean {
        if (frequency !in setOf("daily", "weekly", "monthly", "yearly") || activeScopeFlow.value != expectedScope) return false
        return safeEditResult {
            check(activeScopeFlow.value == expectedScope)
            it.putClean(stringPreferencesKey(mobileFrequencyKey(expectedScope)), frequency)
            it.putClean(stringPreferencesKey(mobileFrequencyDirtyKey(expectedScope)), "false")
        }
    }

    suspend fun acceptSuccessfulMobilePreferenceFrequency(
        serverFrequency: String,
        submittedFrequency: String,
        expectedScope: String,
    ): Boolean {
        if (serverFrequency !in setOf("daily", "weekly", "monthly", "yearly") ||
            submittedFrequency !in setOf("daily", "weekly", "monthly", "yearly") ||
            activeScopeFlow.value != expectedScope
        ) return false
        var accepted = false
        val saved = safeEditResult { prefs ->
            check(activeScopeFlow.value == expectedScope)
            val current = prefs.safeString(mobileFrequencyKey(expectedScope)) ?: "daily"
            if (current == submittedFrequency) {
                prefs.putClean(stringPreferencesKey(mobileFrequencyKey(expectedScope)), serverFrequency)
                prefs.putClean(stringPreferencesKey(mobileFrequencyDirtyKey(expectedScope)), "false")
                accepted = true
            }
        }
        return saved && accepted
    }

    private suspend fun persistScopedMobileSettings(scope: String, settings: Map<String, Any?>) {
        if (scope.isBlank() || !ma.etherealmber.atlascommerce.data.model.isValidMobilePreferenceSettings(settings)) return
        safeEdit { prefs ->
            prefs.putClean(stringPreferencesKey(mobileSettingsKey(scope)), encodeMobileSettings(settings))
        }
    }

    private suspend fun applyMobileSettingsToPreferences(settings: Map<String, Any?>, scope: String) {
        safeEdit { prefs -> writeMobileSettingsToPreferences(prefs, settings, scope) }
    }

    private fun captureMobileSettings(prefs: Preferences, scope: String): Map<String, Any?> {
        val savedTab = prefs.safeString("default_start_tab::$scope") ?: prefs.safeString(Keys.DEFAULT_START_TAB.name)
        val defaultTab = when (savedTab?.uppercase(Locale.ROOT)) {
            "PRODUCTS", "PRODUCT" -> "products"
            "SCAN" -> "scan"
            "SALES", "SALE" -> "sales"
            else -> "dashboard"
        }
        val storedDensity = prefs.safeString(Keys.DENSITY.name)?.lowercase(Locale.ROOT)
        return linkedMapOf(
            "theme" to (prefs.safeString(Keys.THEME.name)?.lowercase(Locale.ROOT)
                ?.takeIf { it in setOf("system", "light", "dark") } ?: "system"),
            "language" to (prefs.safeString(Keys.LANGUAGE.name)?.lowercase(Locale.ROOT)
                ?.takeIf { it in setOf("system", "en", "fr", "ar") } ?: "system"),
            "density" to if (storedDensity == "compact") "compact" else "comfortable",
            "animations" to (prefs.safeBoolean(Keys.ANIMATIONS_ENABLED.name) ?: true),
            "navigation_default" to defaultTab,
            "scan_behavior" to if (prefs.safeBoolean(Keys.AUTO_OPEN_CAMERA.name) == true) "auto" else "preview",
            "sale_confirmation" to (prefs.safeBoolean(Keys.CONFIRM_BEFORE_SALE.name) ?: true),
            "product_view" to if (prefs.safeBoolean(Keys.PRODUCTS_LIST_LAYOUT.name) == false) "grid" else "list",
            "update_notifications" to (prefs.safeBoolean(Keys.AUTO_CHECK_UPDATES.name) ?: true),
            "update_channel" to (prefs.safeString("mobile_setting_update_channel::${mobileScopeHash(scope)}")
                ?.takeIf { it in setOf("stable", "beta") } ?: "stable"),
            "onboarding_completed" to (prefs.safeBoolean(Keys.ONBOARDING_DONE.name) ?: false),
        )
    }

    private fun writeMobileSettingsToPreferences(
        prefs: MutablePreferences,
        settings: Map<String, Any?>,
        scope: String,
    ) {
        prefs.putClean(Keys.THEME, settings.getValue("theme") as String)
        prefs.putClean(Keys.LANGUAGE, settings.getValue("language") as String)
        prefs.putClean(Keys.DENSITY, if (settings.getValue("density") == "compact") "compact" else "comfort")
        prefs.putClean(Keys.ANIMATIONS_ENABLED, settings.getValue("animations") as Boolean)
        val tab = when (settings.getValue("navigation_default")) {
            "products" -> "PRODUCTS"
            "scan" -> "SCAN"
            "sales" -> "SALES"
            else -> "HOME"
        }
        prefs.putClean(stringPreferencesKey("default_start_tab::$scope"), tab)
        prefs.putClean(Keys.AUTO_OPEN_CAMERA, settings.getValue("scan_behavior") == "auto")
        prefs.putClean(Keys.CONFIRM_BEFORE_SALE, settings.getValue("sale_confirmation") as Boolean)
        prefs.putClean(Keys.PRODUCTS_LIST_LAYOUT, settings.getValue("product_view") == "list")
        prefs.putClean(Keys.AUTO_CHECK_UPDATES, settings.getValue("update_notifications") as Boolean)
        prefs.putClean(
            stringPreferencesKey("mobile_setting_update_channel::${mobileScopeHash(scope)}"),
            settings.getValue("update_channel") as String,
        )
        prefs.putClean(Keys.ONBOARDING_DONE, settings.getValue("onboarding_completed") as Boolean)
    }

    private fun defaultMobileSettings(): Map<String, Any?> = linkedMapOf(
        "theme" to "system", "language" to "system", "density" to "comfortable", "animations" to true,
        "navigation_default" to "dashboard", "scan_behavior" to "preview", "sale_confirmation" to true,
        "product_view" to "list", "update_notifications" to true, "update_channel" to "stable",
        "onboarding_completed" to false,
    )

    private fun encodeMobileSettings(settings: Map<String, Any?>): String =
        JSONObject(settings).toString()

    private fun decodeMobileSettings(raw: String?): Map<String, Any?>? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    val value = json.get(key)
                    put(key, if (value == JSONObject.NULL) null else value)
                }
            }.takeIf { ma.etherealmber.atlascommerce.data.model.isValidMobilePreferenceSettings(it) }
        }.getOrNull()
    }

    private fun encodeMobileDirtyKeys(keys: Set<String>): String = JSONArray(keys.sorted()).toString()

    private fun decodeMobileDirtyKeys(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return runCatching {
            val array = JSONArray(raw)
            buildSet { for (index in 0 until array.length()) array.optString(index).takeIf { it.isNotBlank() }?.let(::add) }
        }.getOrDefault(emptySet())
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

    val appLanguageFollowsSystem: Flow<Boolean> = safeData
        .map { prefs ->
            val stored = prefs.safeString(Keys.LANGUAGE.name)
            stored.isNullOrBlank() || stored.equals("system", ignoreCase = true)
        }
        .distinctUntilChanged()

    val appLanguage: Flow<AppLanguage> = safeData
        .map { prefs ->
            val stored = prefs.safeString(Keys.LANGUAGE.name)
            if (stored.isNullOrBlank() || stored.equals("system", ignoreCase = true)) {
                AppLanguage.fromSystemLocale()
            } else {
                AppLanguage.fromTag(stored)
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

    val developerModeEnabled: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.DEVELOPER_MODE_ENABLED.name) ?: false }
        .distinctUntilChanged()

    /** Strictly false by default so entering Scan / Scan Device never starts camera immediately. */
    val autoOpenCamera: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.AUTO_OPEN_CAMERA.name) ?: false }
        .distinctUntilChanged()

    val shakeToReportEnabled: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.SHAKE_TO_REPORT.name) ?: true }
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

    val parallelUpdateDownload: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.PARALLEL_UPDATE_DOWNLOAD.name) ?: true }
        .distinctUntilChanged()

    val followLatestRelease: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.FOLLOW_LATEST_RELEASE.name) ?: true }
        .distinctUntilChanged()

    val updateDownloadSource: Flow<String> = safeData
        .map { it.safeString(Keys.UPDATE_SOURCE.name)?.takeIf { s -> s.isNotBlank() } ?: "auto" }
        .distinctUntilChanged()

    val productsListLayout: Flow<Boolean> = safeData
        .map { it.safeBoolean(Keys.PRODUCTS_LIST_LAYOUT.name) ?: true }
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
            val existing = current.firstOrNull {
                it.serverUrl.equals(srv, ignoreCase = true) && it.username.equals(usr, ignoreCase = true)
            }
            val protectedForUnlock = entry.tokenCipher.startsWith(DEVICE_UNLOCK_TOKEN_PREFIX)
            val unlockMustBeReconfigured = existing?.deviceUnlockEnabled == true && !protectedForUnlock
            val updated = listOf(
                entry.copy(
                    serverUrl = srv,
                    username = usr,
                    deviceUnlockEnabled = protectedForUnlock && (entry.deviceUnlockEnabled || existing?.deviceUnlockEnabled == true),
                    deviceUnlockChoiceMade = if (unlockMustBeReconfigured) false
                    else entry.deviceUnlockChoiceMade || existing?.deviceUnlockChoiceMade == true,
                    savedAtMs = System.currentTimeMillis(),
                ),
            ) + current.filterNot {
                it.serverUrl.equals(srv, ignoreCase = true) &&
                    it.username.equals(usr, ignoreCase = true)
            }
            prefs.putClean(Keys.SAVED_ACCOUNTS, encodeSavedAccounts(updated.take(20)))
        }
    }

    suspend fun setSavedAccountDeviceUnlock(
        serverUrl: String,
        username: String,
        enabled: Boolean,
        protectedTokenCipher: String? = null,
    ) {
        val srv = serverUrl.trim()
        val usr = username.trim().lowercase(Locale.ROOT)
        if (srv.isEmpty() || usr.isEmpty()) return
        safeEdit { prefs ->
            val accounts = decodeSavedAccounts(prefs.safeString(Keys.SAVED_ACCOUNTS.name))
            val updated = accounts.map { account ->
                if (account.serverUrl.equals(srv, ignoreCase = true) && account.username.equals(usr, ignoreCase = true)) {
                    val tokenCipher = protectedTokenCipher
                        ?.takeIf { it.startsWith(DEVICE_UNLOCK_TOKEN_PREFIX) }
                        ?: account.tokenCipher
                    val canEnable = enabled && tokenCipher.startsWith(DEVICE_UNLOCK_TOKEN_PREFIX)
                    account.copy(
                        tokenCipher = tokenCipher,
                        deviceUnlockEnabled = canEnable,
                        deviceUnlockChoiceMade = true,
                    )
                } else account
            }
            prefs.putClean(Keys.SAVED_ACCOUNTS, encodeSavedAccounts(updated))
        }
    }

    /** An expired saved token disables unlock but lets the next password login offer it again. */
    suspend fun resetSavedAccountDeviceUnlockChoice(serverUrl: String, username: String) {
        val srv = serverUrl.trim()
        val usr = username.trim().lowercase(Locale.ROOT)
        if (srv.isEmpty() || usr.isEmpty()) return
        safeEdit { prefs ->
            val accounts = decodeSavedAccounts(prefs.safeString(Keys.SAVED_ACCOUNTS.name))
            val updated = accounts.map { account ->
                if (account.serverUrl.equals(srv, ignoreCase = true) && account.username.equals(usr, ignoreCase = true)) {
                    account.copy(deviceUnlockEnabled = false, deviceUnlockChoiceMade = false)
                } else account
            }
            prefs.putClean(Keys.SAVED_ACCOUNTS, encodeSavedAccounts(updated))
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

    suspend fun setThemeMode(mode: ThemeMode): Boolean {
        val saved = safeEditResult { prefs -> prefs.putClean(Keys.THEME, mode.storageKey) }
        if (saved) persistCurrentMobileSettingsAsDirty()
        return saved
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        safeEdit { prefs -> prefs.putClean(Keys.LANGUAGE, language.tag) }
        persistCurrentMobileSettingsAsDirty()
    }

    suspend fun useSystemAppLanguage() {
        safeEdit { prefs -> prefs.putClean(Keys.LANGUAGE, "system") }
        persistCurrentMobileSettingsAsDirty()
    }

    suspend fun setDensityMode(mode: DensityMode) {
        safeEdit { prefs -> prefs.putClean(Keys.DENSITY, mode.storageKey) }
        persistCurrentMobileSettingsAsDirty()
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.ANIMATIONS_ENABLED, enabled) }
        persistCurrentMobileSettingsAsDirty()
    }

    suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.DEVELOPER_MODE_ENABLED, enabled) }
    }

    /** Removes all on-device preferences, saved accounts, server choices, and sessions. */
    suspend fun clearAllLocalPreferences() {
        safeEdit { prefs -> prefs.clear() }
    }

    suspend fun setAutoOpenCamera(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.AUTO_OPEN_CAMERA, enabled) }
        persistCurrentMobileSettingsAsDirty()
    }

    suspend fun setShakeToReportEnabled(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.SHAKE_TO_REPORT, enabled) }
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
        persistCurrentMobileSettingsAsDirty()
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
        persistCurrentMobileSettingsAsDirty()
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.AUTO_CHECK_UPDATES, enabled) }
        persistCurrentMobileSettingsAsDirty()
    }

    suspend fun setParallelUpdateDownload(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.PARALLEL_UPDATE_DOWNLOAD, enabled) }
    }

    suspend fun setFollowLatestRelease(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.FOLLOW_LATEST_RELEASE, enabled) }
    }

    suspend fun setUpdateDownloadSource(sourceKey: String) {
        safeEdit { prefs -> prefs.putClean(Keys.UPDATE_SOURCE, sourceKey.trim().lowercase()) }
    }

    suspend fun setProductsListLayout(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.PRODUCTS_LIST_LAYOUT, enabled) }
        persistCurrentMobileSettingsAsDirty()
    }

    suspend fun setRoleNotificationsEnabled(enabled: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.ROLE_NOTIFICATIONS_ENABLED, enabled) }
    }

    suspend fun markOnboardingDone() {
        safeEdit { prefs -> prefs.putClean(Keys.ONBOARDING_DONE, true) }
        persistCurrentMobileSettingsAsDirty()
    }

    suspend fun setOnboardingDone(done: Boolean) {
        safeEdit { prefs -> prefs.putClean(Keys.ONBOARDING_DONE, done) }
        persistCurrentMobileSettingsAsDirty()
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
            prefs.remove(Keys.SESSION_RESTORE_DISABLED)
        }
    }

    suspend fun isSessionRestoreDisabled(): Boolean =
        runCatching { safeData.first()[Keys.SESSION_RESTORE_DISABLED] ?: false }.getOrDefault(false)

    suspend fun disableSessionRestore(disabled: Boolean) {
        safeEdit { prefs ->
            if (disabled) prefs.putClean(Keys.SESSION_RESTORE_DISABLED, true)
            else prefs.remove(Keys.SESSION_RESTORE_DISABLED)
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

    /** Encrypts/migrates the entire preference payload on startup and every subsequent write. */
    suspend fun migrateLegacyPreferencesAtRest() {
        safeEdit { }
    }

    private suspend fun safeEdit(block: (MutablePreferences) -> Unit) {
        runCatching {
            store.updateData { persisted ->
                val editable = decryptPersistedPreferences(persisted)
                block(editable)
                encryptPreferences(editable)
            }
        }
    }

    private suspend fun safeEditResult(block: (MutablePreferences) -> Unit): Boolean = try {
        store.updateData { persisted ->
            val editable = decryptPersistedPreferences(persisted)
            block(editable)
            encryptPreferences(editable)
        }
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }

    /** Older releases wrote the typed Preferences map directly; newer releases keep one AES-GCM blob. */
    private fun decryptPersistedPreferences(source: Preferences): MutablePreferences {
        val cipherText = source[Keys.ENCRYPTED_PAYLOAD]
        if (cipherText.isNullOrBlank()) return source.toMutableCopy()
        val clearJson = cryptoStore.decryptAtRest(cipherText) ?: return mutablePreferencesOf()
        return runCatching {
            val records = JSONObject(clearJson).optJSONArray("entries") ?: JSONArray()
            val result = mutablePreferencesOf()
            for (index in 0 until records.length()) {
                val record = records.optJSONObject(index) ?: continue
                val name = record.optString("name").takeIf(String::isNotBlank) ?: continue
                when (record.optString("type")) {
                    "string" -> result[stringPreferencesKey(name)] = record.optString("value")
                    "boolean" -> result[booleanPreferencesKey(name)] = record.optBoolean("value")
                    "int" -> result[intPreferencesKey(name)] = record.optInt("value")
                    "long" -> result[longPreferencesKey(name)] = record.optLong("value")
                    "float" -> result[floatPreferencesKey(name)] = record.optDouble("value").toFloat()
                    "double" -> result[doublePreferencesKey(name)] = record.optDouble("value")
                    "string_set" -> {
                        val values = record.optJSONArray("value") ?: JSONArray()
                        result[stringSetPreferencesKey(name)] = buildSet {
                            for (valueIndex in 0 until values.length()) {
                                add(values.optString(valueIndex))
                            }
                        }
                    }
                }
            }
            result
        }.getOrDefault(mutablePreferencesOf())
    }

    private fun encryptPreferences(source: Preferences): Preferences {
        val records = JSONArray()
        source.asMap().forEach { (key, value) ->
            if (key.name == Keys.ENCRYPTED_PAYLOAD.name) return@forEach
            val record = JSONObject().put("name", key.name)
            when (value) {
                is String -> record.put("type", "string").put("value", value)
                is Boolean -> record.put("type", "boolean").put("value", value)
                is Int -> record.put("type", "int").put("value", value)
                is Long -> record.put("type", "long").put("value", value)
                is Float -> record.put("type", "float").put("value", value.toDouble())
                is Double -> record.put("type", "double").put("value", value)
                is Set<*> -> {
                    val strings = value.filterIsInstance<String>()
                    if (strings.size == value.size) {
                        record.put("type", "string_set").put("value", JSONArray(strings))
                    } else return@forEach
                }
                else -> return@forEach
            }
            records.put(record)
        }
        val encrypted = cryptoStore.encryptAtRest(JSONObject().put("entries", records).toString())
        val result = mutablePreferencesOf()
        result[Keys.ENCRYPTED_PAYLOAD] = encrypted
        return result
    }
}

private fun Preferences.toMutableCopy(): MutablePreferences {
    val result = mutablePreferencesOf()
    asMap().forEach { (key, value) ->
        when (value) {
            is String -> result[stringPreferencesKey(key.name)] = value
            is Boolean -> result[booleanPreferencesKey(key.name)] = value
            is Int -> result[intPreferencesKey(key.name)] = value
            is Long -> result[longPreferencesKey(key.name)] = value
            is Float -> result[floatPreferencesKey(key.name)] = value
            is Double -> result[doublePreferencesKey(key.name)] = value
            is Set<*> -> {
                val strings = value.filterIsInstance<String>()
                if (strings.size == value.size) result[stringSetPreferencesKey(key.name)] = strings.toSet()
            }
        }
    }
    return result
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
                            deviceUnlockEnabled = obj.optBoolean("deviceUnlockEnabled", false),
                            deviceUnlockChoiceMade = obj.optBoolean("deviceUnlockChoiceMade", false),
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
                .put("deviceUnlockEnabled", item.deviceUnlockEnabled)
                .put("deviceUnlockChoiceMade", item.deviceUnlockChoiceMade)
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
