package ma.etherealmber.atlascommerce.ui.i18n

import ma.etherealmber.atlascommerce.core.AppLanguage

/** Translate reviewed release-note lines only. Preserve unknown release authors' text verbatim. */
fun localizeReleaseNotes(notes: String, language: AppLanguage): String = notes.lineSequence().joinToString("\n") { line ->
    val prefix = if (line.startsWith("• ")) "• " else ""
    val text = line.removePrefix(prefix)
    val translated = when (text) {
        "Complete localization audit findings" -> "Résoudre les points de l'audit de traduction" to "معالجة نتائج تدقيق الترجمة"
        "Resolve composable invocation in share action" -> "Corriger l'appel Compose dans l'action de partage" to "إصلاح استدعاء Compose في إجراء المشاركة"
        "Fix release URL and improve crash reporting settings" -> "Corriger l'URL de publication et améliorer les paramètres de rapports de plantage" to "إصلاح رابط الإصدار وتحسين إعدادات تقارير الأعطال"
        "Make floating navigation translucent and scroll-safe" -> "Rendre la navigation flottante translucide et compatible avec le défilement" to "جعل شريط التنقل العائم شبه شفاف مع دعم التمرير"
        "Fix localized device timestamp composition" -> "Corriger l'affichage localisé des dates des appareils" to "إصلاح عرض تواريخ الأجهزة المترجمة"
        "Localize remaining Android financial and settings screens" -> "Traduire les écrans financiers et les paramètres Android restants" to "ترجمة الشاشات المالية وإعدادات Android المتبقية"
        "Fix diagnostics action localization scope" -> "Corriger la portée de traduction des actions de diagnostic" to "إصلاح نطاق ترجمة إجراءات التشخيص"
        "Polish Agent navigation and diagnostics flows" -> "Améliorer la navigation de l'Agent et les parcours de diagnostic" to "تحسين تنقل المساعد ومسارات التشخيص"
        "Upload R2 objects using raw request bodies" -> "Envoyer les objets R2 avec des requêtes brutes" to "رفع كائنات R2 باستخدام نص الطلب الخام"
        "Fix remaining Android unit test failures" -> "Corriger les derniers échecs des tests unitaires Android" to "إصلاح إخفاقات اختبارات Android المتبقية"
        "Fix Android compile errors found by CI" -> "Corriger les erreurs de compilation Android détectées par la CI" to "إصلاح أخطاء ترجمة Android المكتشفة في CI"
        "Keep release verification within Actions expression limits" -> "Respecter les limites des expressions Actions lors de la vérification" to "مراعاة حدود تعبيرات Actions أثناء التحقق من الإصدار"
        "Fail release when CDN publication is incomplete" -> "Faire échouer la publication si le CDN est incomplet" to "إيقاف الإصدار إذا لم يكتمل النشر على CDN"
        "Resolve Android CI compilation errors" -> "Corriger les erreurs de compilation Android dans la CI" to "إصلاح أخطاء ترجمة Android في CI"
        "Localize notices outside composable callbacks" -> "Traduire les messages hors des callbacks Compose" to "ترجمة التنبيهات خارج استدعاءات Compose"
        "Complete audited French English Arabic UI localization" -> "Terminer la traduction vérifiée de l’interface en français, anglais et arabe" to "إكمال ترجمة الواجهة المدققة إلى الفرنسية والإنجليزية والعربية"
        "Sync app source quarantine manifest" -> "Synchroniser le manifeste de quarantaine des sources" to "مزامنة ملف عزل مصادر التطبيق"
        "Send app version policy headers" -> "Envoyer les en-têtes de politique de version" to "إرسال ترويسات سياسة إصدار التطبيق"
        "Improve Arabic RTL and agent language" -> "Améliorer l’affichage arabe et la langue de l’Agent" to "تحسين اتجاه العربية ولغة المساعد"
        "Keep agent active while dismissing keyboard" -> "Garder l’Agent actif à la fermeture du clavier" to "إبقاء المساعد نشطاً عند إغلاق لوحة المفاتيح"
        "Fix Java manifest initializer syntax" -> "Corriger la syntaxe de l’initialiseur du manifeste Java" to "إصلاح صياغة مهيّئ ملف Java"
        "Resolve automated audit false positives" -> "Corriger les faux positifs de l’audit automatique" to "معالجة النتائج الخاطئة للتدقيق الآلي"
        "Keep WorkManager reflective constructors in release" -> "Conserver les constructeurs réflexifs de WorkManager dans la version publiée" to "الحفاظ على منشئات WorkManager الانعكاسية في الإصدار"
        "Keep AndroidX startup initializers in minified APK" -> "Conserver les initialiseurs AndroidX dans l’APK optimisé" to "الحفاظ على مهيئات AndroidX في ملف APK المصغّر"
        "Include Android 9 release crash stack in diagnostics" -> "Inclure la pile d’erreur Android 9 dans les diagnostics" to "تضمين أثر تعطل Android 9 في التشخيصات"
        "Fix mascot animation import" -> "Corriger l’import de l’animation de la mascotte" to "إصلاح استيراد حركة الشخصية"
        "Refresh dashboard, login and agent experience" -> "Améliorer le tableau de bord, la connexion et l’Agent" to "تحسين لوحة المعلومات وتسجيل الدخول والمساعد"
        "Improve passkey diagnostics and remove nav edge glow" -> "Améliorer le diagnostic des passkeys et retirer l’éclat du bord de navigation" to "تحسين تشخيص مفاتيح المرور وإزالة توهج حافة التنقل"
        "Animate floating navigation across app state changes" -> "Animer la navigation flottante lors des changements d’état" to "تحريك شريط التنقل العائم عند تغير حالة التطبيق"
        "Fix diagnostics status expression" -> "Corriger l’expression de l’état des diagnostics" to "إصلاح تعبير حالة التشخيص"
        "Fix passkey diagnostics and direct report submission" -> "Corriger le diagnostic des passkeys et l’envoi direct des rapports" to "إصلاح تشخيص مفاتيح المرور والإرسال المباشر للتقارير"
        "Polish translucent navigation and release triggers" -> "Améliorer la navigation translucide et les déclencheurs de publication" to "تحسين التنقل شبه الشفاف ومشغلات الإصدار"
        else -> null
    }
    val version = Regex("^Set version ([0-9.]+) \\((\\d+)\\) \\[skip ci]$").matchEntire(text)
    val metadata = Regex("^Version : (v[0-9.]+) \\(versionCode = (\\d+)\\)$").matchEntire(text)
    val offline = when (text) {
        "Téléchargement multi-connexions adapté au réseau : CDN 6 sur mobile, 16 en Wi-Fi ; GitHub reste limité à 4." -> "Network-adaptive multi-connection downloads: CDN uses 6 on mobile and 16 on Wi-Fi; GitHub stays capped at 4." to "تنزيل متعدد الاتصالات يتكيف مع الشبكة: يستخدم CDN ستة على الهاتف و16 على Wi-Fi؛ يظل GitHub محدوداً بأربعة."
        "Suivi direct de la dernière version publiée sur GitHub Releases & CDN 1." -> "Direct tracking of the latest release on GitHub Releases and CDN 1." to "متابعة أحدث إصدار على GitHub وCDN 1 مباشرة."
        "Journal complet des versions intégré dans Paramètres et notes JSON allégées." -> "Full release history in Settings and compact JSON notes." to "سجل الإصدارات الكامل في الإعدادات وملاحظات JSON مختصرة."
        "Licence MIT © 2026 Etherealmber & navigation persistante entre sous-écrans." -> "MIT license © 2026 Etherealmber and persistent navigation between screens." to "ترخيص MIT © 2026 Etherealmber وتنقل مستمر بين الشاشات."
        else -> null
    }
    val fallback = Regex("^Mise à jour (Atlas Commerce )?(v?[0-9.]+) — optimisations et correctifs\\.$").matchEntire(text)
    val result = when {
        offline != null && language == AppLanguage.ENGLISH -> offline.first
        offline != null && language == AppLanguage.ARABIC -> offline.second
        fallback != null && language == AppLanguage.ENGLISH -> "${fallback.groupValues[1]}update ${fallback.groupValues[2]} — improvements and fixes."
        fallback != null && language == AppLanguage.ARABIC -> "تحديث ${fallback.groupValues[1]}${fallback.groupValues[2]} — تحسينات وإصلاحات."
        metadata != null && language == AppLanguage.ENGLISH -> "Version: ${metadata.groupValues[1]} (version code ${metadata.groupValues[2]})"
        language == AppLanguage.ENGLISH -> text
        version != null -> when (language) {
            AppLanguage.FRENCH -> "Définir la version ${version.groupValues[1]} (${version.groupValues[2]}) [CI ignorée]"
            AppLanguage.ARABIC -> "تعيين الإصدار ${version.groupValues[1]} (${version.groupValues[2]}) [تخطي CI]"
            else -> text
        }
        metadata != null -> when (language) {
            AppLanguage.FRENCH -> "Version : ${metadata.groupValues[1]} (code ${metadata.groupValues[2]})"
            AppLanguage.ARABIC -> "الإصدار: ${metadata.groupValues[1]} (رمز الإصدار ${metadata.groupValues[2]})"
            else -> text
        }
        translated != null && language == AppLanguage.FRENCH -> translated.first
        translated != null && language == AppLanguage.ARABIC -> translated.second
        else -> text
    }
    prefix + result
}
