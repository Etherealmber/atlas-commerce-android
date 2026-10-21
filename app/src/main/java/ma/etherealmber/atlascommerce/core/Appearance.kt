package ma.etherealmber.atlascommerce.core

/** How the app resolves light/dark colours. */
enum class ThemeMode(val storageKey: String, val label: String) {
    SYSTEM("system", "Système"),
    LIGHT("light", "Clair"),
    DARK("dark", "Sombre"),
    ;

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

/** Spacing scale used by every screen. */
enum class DensityMode(val storageKey: String, val label: String) {
    COMPACT("compact", "Compact"),
    COMFORT("comfort", "Confort"),
    ;

    companion object {
        fun fromKey(key: String?): DensityMode = entries.firstOrNull { it.storageKey == key } ?: COMPACT
    }
}

/** Roles exposed by the worker, with the French labels used in the UI. */
object Roles {
    const val OWNER = "owner"
    const val ADMIN = "admin"
    const val SECRETARY = "secretaire"
    const val STOCK = "stock"
    const val STOREKEEPER = "magasinier"
    const val USER = "user"

    fun label(role: String?): String = when (role?.lowercase()) {
        OWNER -> "Propriétaire"
        ADMIN -> "Administrateur"
        SECRETARY -> "Secrétaire"
        STOCK, STOREKEEPER -> "Magasinier"
        USER -> "Utilisateur"
        else -> role?.replaceFirstChar { it.uppercase() }?.takeIf { it.isNotBlank() } ?: "Invité"
    }

    /** Owner, admin and secretary can create documents (worker: requireBizMutator). */
    fun canCreateDocuments(role: String?): Boolean =
        role?.lowercase() in setOf(OWNER, ADMIN, SECRETARY)
}
