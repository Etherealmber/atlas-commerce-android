package ma.etherealmber.atlascommerce.data.repo

import java.text.Normalizer
import java.util.Locale

/** The minimum local data sources needed to ground a chat request. */
internal data class AgentContextNeeds(
    val dashboard: Boolean = false,
    val finance: Boolean = false,
    val products: Boolean = false,
    val clients: Boolean = false,
    val actions: Boolean = false,
    val alerts: Boolean = false,
) {
    val isEmpty: Boolean
        get() = !dashboard && !finance && !products && !clients && !actions && !alerts
}

/**
 * Avoid making business API calls for greetings, app navigation/settings help, and other
 * non-business requests. Business questions fetch only the relevant sources.
 */
internal fun agentContextNeeds(question: String): AgentContextNeeds {
    val normalized = Normalizer.normalize(question, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
    val words = normalized.split(Regex("[^\\p{L}\\p{N}]+")).filter(String::isNotBlank).toSet()
    fun has(vararg terms: String): Boolean = terms.any { term ->
        if (term.contains(' ')) normalized.contains(term) else term in words
    }

    val products = has(
        "product", "products", "item", "items", "stock", "inventory", "catalog", "catalogue",
        "barcode", "sku", "article", "articles", "produit", "produits", "inventaire", "مخزون", "منتج",
    )
    val clients = has("client", "clients", "customer", "customers", "customer", "clientele", "زبون", "عميل", "عملاء")
    val finance = has(
        "sale", "sales", "revenue", "invoice", "invoices", "payment", "payments", "expense", "expenses",
        "profit", "margin", "balance", "receivable", "payable", "finance", "financial", "paid", "unpaid",
        "vente", "ventes", "facture", "factures", "dépense", "depense", "dépenses", "depenses", "bénéfice", "benefice",
        "رصيد", "مبيعات", "فاتورة", "فواتير", "مدفوعات",
    )
    val actions = has("request", "requests", "pending", "approval", "approvals", "action", "actions", "inbox", "demande", "demandes", "scan request", "order", "orders", "commande", "commandes", "طلبات", "معلق")
    val alerts = has("alert", "alerts", "notification", "notifications", "alerte", "alertes", "تنبيه", "اشعار")
    val dashboard = has("dashboard", "overview", "summary", "summarize", "total", "overall", "today", "resume", "apercu", "tableau de bord", "ملخص", "اليوم")

    val specific = products || clients || finance || actions || alerts
    val businessWords = specific || has("business", "company", "enterprise", "commerce", "entreprise", "activite", "أعمال", "شركة")
    val knownNonBusiness = has(
        "hello", "hi", "hey", "good morning", "good evening", "bonjour", "bonsoir", "coucou", "salam", "salut", "merci", "thanks", "thank you", "شكرا", "مرحبا",
        "joke", "poem", "story", "translate", "translation", "rewrite", "rephrase", "grammar", "write an email",
        "settings", "setting", "profile", "account", "screen", "page", "menu", "navigation", "navigate", "button",
        "parametre", "parametres", "reglage", "profil", "compte", "صفحة", "إعدادات",
    )
    if (!businessWords && knownNonBusiness) return AgentContextNeeds()
    if (!specific && businessWords) return AgentContextNeeds(dashboard = true)
    if (!specific) return AgentContextNeeds()

    // A broad business question still gets one compact dashboard call, but a targeted question
    // never pays for unrelated catalogue, client, finance, alert, and action requests.
    return AgentContextNeeds(
        dashboard = dashboard,
        finance = finance,
        products = products,
        clients = clients,
        actions = actions,
        alerts = alerts,
    )
}
