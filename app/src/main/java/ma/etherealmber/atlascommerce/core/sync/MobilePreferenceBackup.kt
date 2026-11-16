package ma.etherealmber.atlascommerce.core.sync

import ma.etherealmber.atlascommerce.data.model.isValidMobilePreferenceSettings

const val MOBILE_PREFERENCE_BACKUP_SCHEMA = 1
const val MOBILE_PREFERENCE_BACKUP_MAX_BYTES = 16 * 1024

data class MobilePreferenceBackup(val settings: Map<String, Any?>)

/** A versioned export containing only the backend's explicit mobile-settings allowlist. */
fun encodeMobilePreferenceBackup(settings: Map<String, Any?>): String {
    require(isValidMobilePreferenceSettings(settings)) { "Invalid mobile preference settings" }
    return MobilePreferenceJson.encodeObject(
        mapOf("schema_version" to MOBILE_PREFERENCE_BACKUP_SCHEMA, "settings" to settings),
    )
}

/** Reject unknown root keys, unknown settings, malformed types, and oversized files before preview. */
fun decodeMobilePreferenceBackup(json: String): MobilePreferenceBackup? {
    if (json.toByteArray(Charsets.UTF_8).size > MOBILE_PREFERENCE_BACKUP_MAX_BYTES) return null
    val root = MobilePreferenceJson.decodeObject(json) ?: return null
    if (root.keys != setOf("schema_version", "settings")) return null
    if ((root["schema_version"] as? Number)?.toInt() != MOBILE_PREFERENCE_BACKUP_SCHEMA) return null
    val rawSettings = root["settings"] as? Map<*, *> ?: return null
    val settings = mutableMapOf<String, Any?>()
    for ((key, value) in rawSettings) {
        val name = key as? String ?: return null
        settings[name] = value
    }
    if (!isValidMobilePreferenceSettings(settings)) return null
    return MobilePreferenceBackup(settings)
}

/** This app intentionally supports merge-only import: supplied allowlisted fields replace their values. */
fun mergeMobilePreferenceBackup(
    current: Map<String, Any?>,
    imported: Map<String, Any?>,
): Map<String, Any?>? {
    if (!isValidMobilePreferenceSettings(current) || !isValidMobilePreferenceSettings(imported)) return null
    return current + imported
}
