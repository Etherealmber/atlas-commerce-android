package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.localizeReleaseNotes
import ma.etherealmber.atlascommerce.ui.i18n.localizedUpdateSize
import ma.etherealmber.atlascommerce.ui.i18n.localizedUpdateSummary
import ma.etherealmber.atlascommerce.ui.i18n.localizedNotificationRole
import ma.etherealmber.atlascommerce.ui.i18n.notificationText
import ma.etherealmber.atlascommerce.core.update.UpdateInfo
import ma.etherealmber.atlascommerce.core.update.UpdateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationRegressionTest {
    @Test
    fun deviceNamesAndPasskeyErrorsUseSelectedLanguage() {
        assertEquals("حاسوب Windows · Chrome", atlasNotice("PC Windows · Chrome", AppLanguage.ARABIC))
        assertEquals("Android device", atlasNotice("Appareil Android", AppLanguage.ENGLISH))
        assertEquals("Custom device", atlasNotice("Custom device", AppLanguage.ARABIC))
        val error = "Le serveur n'a pas pu enregistrer la passkey. Réessayez plus tard."
        assertTrue(atlasNotice(error, AppLanguage.ENGLISH).startsWith("The server"))
        assertTrue(atlasNotice(error, AppLanguage.ARABIC).startsWith("تعذّر"))
    }

    @Test
    fun releaseNotesTranslateReviewedLinesButPreserveUnknownText() {
        val input = "• Set version 1.2.4 (124) [skip ci]\n• Complete localization audit findings\n• Version : v1.2.4 (versionCode = 124)\n• A custom release note"
        val arabic = localizeReleaseNotes(input, AppLanguage.ARABIC)
        assertTrue(arabic.contains("تعيين الإصدار 1.2.4 (124)"))
        assertTrue(arabic.contains("معالجة نتائج تدقيق الترجمة"))
        assertTrue(arabic.contains("رمز الإصدار 124"))
        assertTrue(arabic.endsWith("• A custom release note"))
        assertTrue(localizeReleaseNotes(input, AppLanguage.ENGLISH).contains("Version: v1.2.4 (version code 124)"))
        assertTrue(localizeReleaseNotes(input, AppLanguage.FRENCH).contains("Définir la version"))
    }
    @Test
    fun olderReleasesAndOfflineUpdateTextAreLocalized() {
        val older = "• Upload R2 objects using raw request bodies\n• Improve Arabic RTL and agent language\n• Fix diagnostics status expression"
        val arabic = localizeReleaseNotes(older, AppLanguage.ARABIC)
        assertTrue(arabic.contains("رفع كائنات R2"))
        assertTrue(arabic.contains("تحسين اتجاه العربية"))
        assertTrue(arabic.contains("إصلاح تعبير حالة التشخيص"))
        assertTrue(localizeReleaseNotes("Mise à jour Atlas Commerce v1.2.4 — optimisations et correctifs.", AppLanguage.ENGLISH).contains("improvements and fixes"))
        assertTrue(localizeReleaseNotes("• Journal complet des versions intégré dans Paramètres et notes JSON allégées.", AppLanguage.ARABIC).contains("سجل الإصدارات"))
        assertEquals("السلة فارغة.", atlasNotice("Le panier est vide.", AppLanguage.ARABIC))
    }

    @Test
    fun updateProgressAndServerDiagnosticsAreLocalized() {
        val info = UpdateInfo(versionCode = 123, versionName = "1.2.3", apkUrl = "https://example.com/latest.apk", mandatory = false)
        val state = UpdateState.Downloading(info, bytesRead = 1_048_576, totalBytes = 2_097_152, activeConnections = 2, speedBytesPerSec = 1024)
        val english = localizedUpdateSummary(state, AppLanguage.ENGLISH)
        val arabic = localizedUpdateSummary(state, AppLanguage.ARABIC)
        assertTrue(english.contains("50 %") && english.contains("MB") && english.contains("2 streams"))
        assertTrue(arabic.contains("م.ب") && arabic.contains("اتصالات"))
        assertTrue(localizedUpdateSize(1024, AppLanguage.FRENCH).contains("Ko"))
        assertEquals("Atlas Commerce server verified · 23 ms", atlasNotice("Serveur Atlas Commerce vérifié · 23 ms", AppLanguage.ENGLISH))
        assertTrue(atlasNotice("Connexion refusée (403). Vérifiez que l'adresse saisie est parfaitement exacte.", AppLanguage.ARABIC).contains("403"))
        assertTrue(atlasNotice("Impossible de résoudre le nom du serveur. Vérifiez l’adresse et votre connexion, puis réessayez.", AppLanguage.ENGLISH).startsWith("Could not resolve"))
    }

    @Test
    fun notificationCopyUsesSelectedLanguageWithoutTranslatingUserText() {
        assertEquals("المخزون", localizedNotificationRole("magasinier", AppLanguage.ARABIC))
        assertEquals("Secretary", localizedNotificationRole("secretaire", AppLanguage.ENGLISH))
        assertEquals("Opérateur", localizedNotificationRole("user", AppLanguage.FRENCH))
        assertEquals("New Atlas notification", notificationText(AppLanguage.ENGLISH, "Nouvelle notification Atlas", "New Atlas notification", "إشعار جديد من Atlas"))
    }

    @Test
    fun onboardingCopyHasReviewedTranslations() {
        val titles = listOf("Bienvenue sur Atlas Commerce", "Permissions intelligentes par rôle", "Scan maîtrisé & annulation instantanée", "Notifications ciblées & copilote IA")
        val highlights = listOf("Synchronisation temps réel & cache hors ligne automatique", "Retour haptique et sonore paramétrable", "Notifications système Android pour les demandes urgentes")
        for (text in titles + highlights) {
            assertTrue(atlasNotice(text, AppLanguage.ARABIC) != text)
            assertTrue(atlasNotice(text, AppLanguage.ENGLISH) != text)
        }
    }

}
