package ma.etherealmber.atlascommerce.core

import ma.etherealmber.atlascommerce.data.model.AlertItemDto
import ma.etherealmber.atlascommerce.data.model.MobileActionDto

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

/** Target audience when sending or filtering role-based notifications & Info Box cards. */
enum class RoleTarget(val key: String, val label: String, val actionType: String) {
    ADMIN("admin", "Administrateurs", "new_product"),
    SECRETARY("secretaire", "Secrétariat & Ventes", "scan"),
    STOCK("stock", "Magasiniers & Stock", "stock_check"),
    ALL("all", "Tous les rôles", "scan"),
    ;

    companion object {
        fun fromKey(raw: String?): RoleTarget =
            entries.firstOrNull { it.key.equals(raw?.trim(), ignoreCase = true) } ?: ADMIN
    }
}

/**
 * Role-based permissions & Info-Box / Notification visibility rules aligned with
 * the Cloudflare Worker (`worker/lib/rbac.ts`) and Atlas field security policy:
 *
 * - **Admin / Owner (`owner`, `admin`)**: Full access — ONLY roles allowed to create
 *   products immediately in stock (`POST /api/products`), approve/reject product requests,
 *   manage devices, view full treasury & send notifications to all roles.
 * - **Secrétaire (`secretaire`)**: Sales, quick POS tickets, invoices, stock adjustments,
 *   receivables & sales/stock alerts; product additions are sent immediately to Admins.
 * - **Magasinier / Stock (`stock`, `magasinier`)**: Warehouse stock checks, stock adjustments,
 *   scanning & immediate product requests to Admins (no sales/treasury access).
 * - **Utilisateur (`user`)**: Catalogue consultation, scanning & immediate product/info
 *   requests routed straight to Admins.
 */
object Roles {
    const val OWNER = "owner"
    const val ADMIN = "admin"
    const val SECRETARY = "secretaire"
    const val STOCK = "stock"
    const val STOREKEEPER = "magasinier"
    const val USER = "user"

    fun normalize(role: String?): String = when (role?.trim()?.lowercase()) {
        OWNER -> OWNER
        ADMIN -> ADMIN
        SECRETARY -> SECRETARY
        STOCK, STOREKEEPER -> STOCK
        else -> USER
    }

    fun label(role: String?): String = when (role?.trim()?.lowercase()) {
        OWNER -> "Propriétaire (Admin)"
        ADMIN -> "Administrateur"
        SECRETARY -> "Secrétaire · Ventes"
        STOCK, STOREKEEPER -> "Magasinier · Stock"
        USER -> "Opérateur"
        else -> role?.replaceFirstChar { it.uppercase() }?.takeIf { it.isNotBlank() } ?: "Opérateur"
    }

    fun scopeSummary(role: String?): String = when (normalize(role)) {
        OWNER, ADMIN -> "Accès complet : création directe produits, validations, finances & alertes globales"
        SECRETARY -> "Accès Ventes & Caisse : facturation, stock & envoi direct des nouveaux produits à l'Admin"
        STOCK -> "Accès Entrepôt : inventaire, ajustements stock & demandes envoyées directement à l'Admin"
        else -> "Accès Opérateur : consultation catalogue, scan & demandes transmises à l'Admin"
    }

    /** STRICT: Only Owner / Admin can create products directly in stock or approve requests. */
    fun isAdmin(role: String?): Boolean = normalize(role) in setOf(OWNER, ADMIN)

    /** STRICT: Only Admin / Owner can create products immediately without admin approval. */
    fun canCreateProductDirectly(role: String?): Boolean = isAdmin(role)

    /** Kept as explicit alias for callers checking direct product creation authority. */
    fun canMutateStock(role: String?): Boolean = isAdmin(role)

    /** Warehouse & office staff who can record stock movements on existing items. */
    fun canAdjustStock(role: String?): Boolean =
        normalize(role) in setOf(OWNER, ADMIN, SECRETARY, STOCK)

    /** Owner, admin and secretary can issue sales / VR tickets and see treasury KPIs. */
    fun canCreateDocuments(role: String?): Boolean =
        normalize(role) in setOf(OWNER, ADMIN, SECRETARY)

    /** Authoritative treasury, payroll and admin-report UI is owner/admin-only. */
    fun canViewFinancials(role: String?): Boolean = isAdmin(role)

    /** Which role audiences this user can send notifications to. */
    fun sendableTargets(role: String?): List<RoleTarget> = when (normalize(role)) {
        OWNER, ADMIN -> listOf(RoleTarget.ALL, RoleTarget.ADMIN, RoleTarget.SECRETARY, RoleTarget.STOCK)
        SECRETARY -> listOf(RoleTarget.ADMIN, RoleTarget.STOCK, RoleTarget.SECRETARY)
        STOCK -> listOf(RoleTarget.ADMIN, RoleTarget.STOCK, RoleTarget.SECRETARY)
        else -> listOf(RoleTarget.ADMIN, RoleTarget.STOCK)
    }

    /** Filters `/api/alerts` strictly according to the active user's role. */
    fun filterAlertsForRole(role: String?, alerts: List<AlertItemDto>): List<AlertItemDto> {
        val norm = normalize(role)
        if (norm == OWNER || norm == ADMIN) return alerts
        return alerts.filter { alert ->
            val k = alert.kind.lowercase()
            val href = alert.href.orEmpty().lowercase()
            val text = alert.label.lowercase()
            val isFinancial = k.contains("invoice") || k.contains("overdue") ||
                k.contains("unpaid") || k.contains("payment") || k.contains("purchase") ||
                href.contains("invoice") || text.contains("facture") || text.contains("impay")
            val isAdminOnlyRequest = k.contains("request") || text.contains("demande")
            when (norm) {
                SECRETARY -> !isAdminOnlyRequest
                STOCK -> !isFinancial
                else -> !isFinancial && !isAdminOnlyRequest
            }
        }
    }

    /**
     * Determines whether a `MobileActionDto` (notification / product request / workflow item)
     * belongs in this user's Role-Based Info Box & Notification stream.
     */
    fun isActionVisibleForUser(
        role: String?,
        username: String?,
        action: MobileActionDto,
    ): Boolean {
        val normRole = normalize(role)
        // 1. Admins / Owners oversee every notification and staff request.
        if (normRole == OWNER || normRole == ADMIN) return true

        // 2. Creators always see their own requests & whether Admin approved/rejected them!
        if (!username.isNullOrBlank() && action.createdBy.equals(username, ignoreCase = true)) {
            return true
        }

        // 3. Explicit role target from meta_json or title/detail tag.
        val target = action.resolvedTargetRole
        if (target == RoleTarget.ALL.key) return true
        if (target == normRole) return true

        // 4. Legacy untagged actions: match by domain (`type`).
        if (target == null) {
            return when (action.type.lowercase()) {
                "new_product" -> false // Strictly Admin-only unless created by this user!
                "invoice" -> normRole == SECRETARY
                "stock_check", "scan" -> normRole == STOCK || normRole == SECRETARY
                else -> false
            }
        }
        return false
    }
}
