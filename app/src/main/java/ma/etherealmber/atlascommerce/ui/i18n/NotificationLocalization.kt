package ma.etherealmber.atlascommerce.ui.i18n

import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.Roles

/** App-generated notification copy only; server/user-authored titles and bodies are not changed. */
fun localizedNotificationRole(role: String, language: AppLanguage): String = when (Roles.normalize(role)) {
    Roles.OWNER -> notificationText(language, "Compte principal", "Owner", "صاحب الحساب")
    Roles.ADMIN -> notificationText(language, "Administrateur", "Administrator", "المسؤول")
    Roles.SECRETARY -> notificationText(language, "Secrétariat", "Secretary", "السكرتارية")
    Roles.STOCK -> notificationText(language, "Stock", "Stock", "المخزون")
    else -> notificationText(language, "Opérateur", "Operator", "المشغّل")
}

fun notificationText(language: AppLanguage, french: String, english: String, arabic: String): String = when (language) {
    AppLanguage.FRENCH -> french
    AppLanguage.ENGLISH -> english
    AppLanguage.ARABIC -> arabic
}
