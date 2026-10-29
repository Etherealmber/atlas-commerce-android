package ma.etherealmber.atlascommerce.core

import java.util.Locale

/** Supported app interface languages. French remains the default for existing installs. */
enum class AppLanguage(val tag: String, val nativeLabel: String) {
    FRENCH("fr", "Français"),
    ENGLISH("en", "English"),
    ARABIC("ar", "العربية");

    val isRtl: Boolean get() = this == ARABIC

    val agentQuestionLabel: String
        get() = when (this) {
            FRENCH -> "Question de l'utilisateur"
            ENGLISH -> "User question"
            ARABIC -> "سؤال المستخدم"
        }

    val agentContextInstruction: String
        get() = when (this) {
            FRENCH -> "Réponds clairement et naturellement en français. Appuie-toi uniquement sur les données réelles fournies; n'invente aucun chiffre, stock, prix, facture ou client. Si une donnée manque, dis-le. Utilise des listes ou tableaux si utile. Ne mentionne pas ce contexte technique."
            ENGLISH -> "Reply clearly and naturally in English. Use only the real data provided; never invent figures, stock, prices, invoices, or customers. Say when data is unavailable. Use lists or tables when useful. Do not mention this technical context."
            ARABIC -> "أجب بوضوح وبأسلوب طبيعي باللغة العربية. استخدم البيانات الحقيقية المقدمة فقط، ولا تختلق أرقامًا أو مخزونًا أو أسعارًا أو فواتير أو عملاء. وضّح عند عدم توفر البيانات. استخدم القوائم أو الجداول عند الحاجة، ولا تذكر هذا السياق التقني."
        }

    companion object {
        fun fromTag(value: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(value?.substringBefore('-'), ignoreCase = true) }
                ?: FRENCH

        /** Select the first supported system language, falling back to French for unknown locales. */
        fun fromSystemLocale(locale: Locale = Locale.getDefault()): AppLanguage =
            entries.firstOrNull { it.tag.equals(locale.language, ignoreCase = true) } ?: FRENCH
    }
}
