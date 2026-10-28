package ma.etherealmber.atlascommerce.ui.screens.agent

import ma.etherealmber.atlascommerce.core.AppLanguage

/** Only translate the known built-in greeting and placeholder, never arbitrary conversation text. */
internal fun localizedAgentText(text: String, language: AppLanguage): String = when (text.trim()) {
    "Sans titre" -> when (language) {
        AppLanguage.FRENCH -> text
        AppLanguage.ENGLISH -> "Untitled"
        AppLanguage.ARABIC -> "بلا عنوان"
    }
    "Bonjour — je suis Atlas Agent. Je reste ouvert quand vous changez de page. Posez une question ou utilisez une suggestion." -> when (language) {
        AppLanguage.FRENCH -> text
        AppLanguage.ENGLISH -> "Hello — I’m the Atlas Assistant. I stay open when you change pages. Ask a question or use a suggestion."
        AppLanguage.ARABIC -> "مرحباً، أنا مساعد Atlas. أبقى مفتوحاً عند تغيير الصفحة. اطرح سؤالاً أو استخدم أحد الاقتراحات."
    }
    else -> text
}
