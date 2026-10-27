package ma.etherealmber.atlascommerce.ui.i18n

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.Roles

val LocalAtlasLanguage = staticCompositionLocalOf { AppLanguage.FRENCH }
val LocalSetAtlasLanguage = staticCompositionLocalOf<(AppLanguage) -> Unit> { {} }

/** Reviewed French, English, and Arabic UI copy; unknown phrases intentionally remain unchanged. */
private val commonTranslations = mapOf(
    "Annuler" to ("Cancel" to "إلغاء"),
    "Ajouter" to ("Add" to "إضافة"),
    "Actualiser" to ("Refresh" to "تحديث"),
    "Afficher" to ("Show" to "عرض"),
    "Afficher plus" to ("Show more" to "عرض المزيد"),
    "Ajouter au panier" to ("Add to cart" to "أضف إلى السلة"),
    "Ajuster le stock" to ("Adjust stock" to "تعديل المخزون"),
    "Ajuster stock" to ("Adjust stock" to "تعديل المخزون"),
    "Accès réservé" to ("Restricted access" to "الوصول مقيّد"),
    "Adresse e-mail" to ("Email address" to "عنوان البريد الإلكتروني"),
    "Admin" to ("Admin" to "المسؤول"),
    "Activer" to ("Enable" to "تفعيل"),
    "Aucun résultat" to ("No results" to "لا توجد نتائج"),
    "Aucun produit" to ("No products" to "لا توجد منتجات"),
    "Aucun montant en attente." to ("No amount pending." to "لا توجد مبالغ معلّقة."),
    "Aucune demande en attente" to ("No pending requests" to "لا توجد طلبات معلّقة"),
    "Aucune donnée financière reçue." to ("No financial data received." to "لم يتم استلام بيانات مالية."),
    "Aucune notification pour votre rôle" to ("No notifications for your role" to "لا توجد إشعارات لدورك"),
    "Aucune version publiée n'est disponible pour le moment." to ("No published version is available right now." to "لا يتوفر إصدار منشور حالياً."),
    "Aucun versement dû pour cette période." to ("No payments are due for this period." to "لا توجد دفعات مستحقة لهذه الفترة."),
    "Après les mouvements comptabilisés" to ("After posted transactions" to "بعد الحركات المُرحّلة"),
    "Apparence, navigation & animations" to ("Appearance, navigation & animations" to "المظهر والتنقل والحركات"),
    "Bénéficiaire / fournisseur (facultatif)" to ("Recipient / supplier (optional)" to "المستفيد / المورد (اختياري)"),
    "Boîte d'alertes & activité" to ("Alerts & activity inbox" to "صندوق التنبيهات والنشاط"),
    "Caméra & lecteur code-barres" to ("Camera & barcode scanner" to "الكاميرا وماسح الباركود"),
    "Caméra en veille" to ("Camera idle" to "الكاميرا في وضع الاستعداد"),
    "Catalogue hors ligne & système" to ("Offline catalog & system" to "الكتالوج دون اتصال والنظام"),
    "Catégorie" to ("Category" to "الفئة"),
    "Changer de serveur" to ("Change server" to "تغيير الخادم"),
    "Changer ou ajouter un serveur" to ("Change or add a server" to "تغيير خادم أو إضافته"),
    "Charger les mouvements suivants" to ("Load more transactions" to "تحميل المزيد من الحركات"),
    "Choisissez une période et les rubriques à consulter." to ("Choose a period and the sections to view." to "اختر فترة والأقسام التي تريد عرضها."),
    "Client du ticket" to ("Receipt customer" to "عميل الإيصال"),
    "Client pré-rempli par défaut" to ("Default prefilled customer" to "العميل الافتراضي المُعبّأ مسبقاً"),
    "Clients à recevoir" to ("Customer receivables" to "مستحقات العملاء"),
    "Confirmer la création du ticket ?" to ("Confirm receipt creation?" to "هل تريد تأكيد إنشاء الإيصال؟"),
    "Continuer" to ("Continue" to "متابعة"),
    "Copier" to ("Copy" to "نسخ"),
    "Créer au stock" to ("Add to inventory" to "إضافة إلى المخزون"),
    "Créer une passkey ?" to ("Create a passkey?" to "هل تريد إنشاء مفتاح مرور؟"),
    "Créer une passkey" to ("Create a passkey" to "إنشاء مفتاح مرور"),
    "Date (AAAA-MM-JJ)" to ("Date (YYYY-MM-DD)" to "التاريخ (سنة-شهر-يوم)"),
    "Demande en file" to ("Request queued" to "تم وضع الطلب في قائمة الانتظار"),
    "Densité d'affichage" to ("Display density" to "كثافة العرض"),
    "Destinataires (rôle cible)" to ("Recipients (target role)" to "المستلمون (الدور المستهدف)"),
    "Dettes fournisseurs" to ("Supplier payables" to "مستحقات الموردين"),
    "Diagnostics développeur" to ("Developer diagnostics" to "تشخيصات المطوّر"),
    "Données indisponibles" to ("Data unavailable" to "البيانات غير متاحة"),
    "Décrivez ce que vous voyez à l'écran…" to ("Describe what you see on screen…" to "صف ما تراه على الشاشة…"),
    "Détails / Instructions" to ("Details / Instructions" to "التفاصيل / التعليمات"),
    "Effacer de cet appareil" to ("Clear from this device" to "المسح من هذا الجهاز"),
    "Effacer les données locales ?" to ("Clear local data?" to "هل تريد مسح البيانات المحلية؟"),
    "Enregistrer" to ("Save" to "حفظ"),
    "Enregistrer une copie" to ("Save a copy" to "حفظ نسخة"),
    "Entrée" to ("Stock in" to "إدخال"),
    "Envoyer" to ("Send" to "إرسال"),
    "Envoyer une notification par rôle" to ("Send a role-based notification" to "إرسال إشعار حسب الدور"),
    "Ex. Client comptoir" to ("E.g. walk-in customer" to "مثال: عميل مباشر"),
    "Ex. Loyer entrepôt octobre" to ("E.g. October warehouse rent" to "مثال: إيجار المستودع لشهر أكتوبر"),
    "Ex. Nom complet" to ("E.g. full name" to "مثال: الاسم الكامل"),
    "Fermer" to ("Close" to "إغلاق"),
    "Filtrées automatiquement selon vos permissions" to ("Automatically filtered by your permissions" to "تُصفّى تلقائياً وفقاً لصلاحياتك"),
    "Fonctionnalités" to ("Features" to "الميزات"),
    "Fournisseurs à payer" to ("Supplier payables" to "مستحقات الموردين"),
    "Historique des versions" to ("Version history" to "سجل الإصدارات"),
    "Impossible de lire ce PDF. Téléchargez-le à nouveau." to ("Unable to read this PDF. Download it again." to "تعذّرت قراءة ملف PDF. نزّله مجدداً."),
    "Journaux de diagnostic" to ("Diagnostic logs" to "سجلات التشخيص"),
    "La date doit être une date valide au format AAAA-MM-JJ." to ("Enter a valid date in YYYY-MM-DD format." to "أدخل تاريخاً صالحاً بالتنسيق سنة-شهر-يوم."),
    "La description est obligatoire." to ("Description is required." to "الوصف مطلوب."),
    "La liste de paie n'a pas pu être chargée." to ("Could not load the payroll list." to "تعذّر تحميل قائمة الرواتب."),
    "La vente est créée sur le serveur ; l'encaissement se fait depuis la caisse web." to ("The sale is created on the server; payment is collected from the web register." to "يُنشأ البيع على الخادم؛ ويتم تحصيل المبلغ من نقطة البيع على الويب."),
    "Le nom apparaît sur le ticket VR" to ("The name appears on the receipt" to "سيظهر الاسم على الإيصال"),
    "Les soldes et relevés financiers sont réservés aux rôles owner/admin." to ("Balances and financial statements are restricted to owner/admin roles." to "الأرصدة والكشوف المالية متاحة لمالكي النظام والمسؤولين فقط."),
    "Libellé *" to ("Description *" to "الوصف *"),
    "Liste partielle : le serveur limite les documents affichés." to ("Partial list: the server limits the documents shown." to "القائمة جزئية: يحدّ الخادم من عدد المستندات المعروضة."),
    "Masquer" to ("Hide" to "إخفاء"),
    "Afficher les détails techniques" to ("Show technical details" to "عرض التفاصيل التقنية"),
    "Masquer les détails techniques" to ("Hide technical details" to "إخفاء التفاصيل التقنية"),
    "Mise à jour requise" to ("Update required" to "التحديث مطلوب"),
    "Mode développeur" to ("Developer mode" to "وضع المطوّر"),
    "Modifier mon profil" to ("Edit my profile" to "تعديل ملفي الشخصي"),
    "Montant *" to ("Amount *" to "المبلغ *"),
    "Motif (facultatif)" to ("Reason (optional)" to "السبب (اختياري)"),
    "Nom affiché *" to ("Display name *" to "الاسم المعروض *"),
    "Nom du client *" to ("Customer name *" to "اسم العميل *"),
    "Nom du produit *" to ("Product name *" to "اسم المنتج *"),
    "Nom ou identifiant" to ("Name or ID" to "الاسم أو المعرّف"),
    "Nom, SKU ou code-barres" to ("Name, SKU or barcode" to "الاسم أو رمز SKU أو الباركود"),
    "Note interne (facultatif)" to ("Internal note (optional)" to "ملاحظة داخلية (اختياري)"),
    "Note pour l'Administrateur" to ("Note for the administrator" to "ملاحظة للمسؤول"),
    "Note" to ("Note" to "ملاحظة"),
    "Notes (facultatif)" to ("Notes (optional)" to "ملاحظات (اختياري)"),
    "Notifications Android désactivées" to ("Android notifications are disabled" to "إشعارات Android معطّلة"),
    "Notifications" to ("Notifications" to "الإشعارات"),
    "Nouveau ticket" to ("New receipt" to "إيصال جديد"),
    "Nouveautés" to ("What's new" to "الجديد"),
    "Nouveaux produits soumis à validation Admin" to ("New products submitted for admin approval" to "المنتجات الجديدة بانتظار موافقة المسؤول"),
    "Nouvelle vente / caisse" to ("New sale / register" to "بيع جديد / نقطة البيع"),
    "Objet de la notification *" to ("Notification subject *" to "موضوع الإشعار *"),
    "Onglet d'ouverture au lancement" to ("Tab to open at launch" to "علامة التبويب عند بدء التشغيل"),
    "Outils locaux · secrets masqués" to ("Local tools · secrets hidden" to "أدوات محلية · الأسرار مخفية"),
    "Ouvrir l'historique complet des changements publiés" to ("Open the full history of published changes" to "فتح السجل الكامل للتغييرات المنشورة"),
    "Ouvrir le pack de rapports ?" to ("Open the report bundle?" to "هل تريد فتح حزمة التقارير؟"),
    "Ouvrir le rapport combiné ?" to ("Open the combined report?" to "هل تريد فتح التقرير الموحّد؟"),
    "Paie à verser" to ("Payroll to pay" to "رواتب مستحقة للدفع"),
    "Panier vide" to ("Cart is empty" to "السلة فارغة"),
    "Panier" to ("Cart" to "السلة"),
    "Paramètres Android" to ("Android settings" to "إعدادات Android"),
    "Parcourir les produits" to ("Browse products" to "تصفّح المنتجات"),
    "Partager le PDF" to ("Share PDF" to "مشاركة PDF"),
    "Personnaliser le rapport" to ("Customize report" to "تخصيص التقرير"),
    "Plus tard" to ("Later" to "لاحقاً"),
    "Priorité urgente" to ("Urgent priority" to "أولوية عاجلة"),
    "Prix MAD *" to ("Price (MAD) *" to "السعر (درهم مغربي) *"),
    "Précédent" to ("Previous" to "السابق"),
    "Préparer" to ("Prepare" to "تحضير"),
    "Période du relevé" to ("Statement period" to "فترة الكشف"),
    "Quantité *" to ("Quantity *" to "الكمية *"),
    "Quantité" to ("Quantity" to "الكمية"),
    "Que s'est-il passé ?" to ("What happened?" to "ماذا حدث؟"),
    "Rapport mensuel · PDF" to ("Monthly report · PDF" to "التقرير الشهري · PDF"),
    "Rapport prêt" to ("Report ready" to "التقرير جاهز"),
    "Rapports PDF" to ("PDF reports" to "تقارير PDF"),
    "Rapports financiers" to ("Financial reports" to "التقارير المالية"),
    "Rechercher un employé" to ("Search employees" to "البحث عن موظف"),
    "Rechercher un produit" to ("Search products" to "البحث عن منتج"),
    "Rechercher" to ("Search" to "بحث"),
    "Refuser" to ("Reject" to "رفض"),
    "Retour" to ("Back" to "رجوع"),
    "Réessayer" to ("Try again" to "إعادة المحاولة"),
    "Référence SKU *" to ("SKU *" to "رمز SKU *"),
    "Rôle en lecture seule" to ("Read-only role" to "دور للقراءة فقط"),
    "Saisissez un montant supérieur à zéro." to ("Enter an amount greater than zero." to "أدخل مبلغاً أكبر من صفر."),
    "Sans TVA" to ("No VAT" to "بدون ضريبة القيمة المضافة"),
    "Se déconnecter ?" to ("Sign out?" to "هل تريد تسجيل الخروج؟"),
    "Se déconnecter" to ("Sign out" to "تسجيل الخروج"),
    "Serveur & espaces enregistrés" to ("Server & saved workspaces" to "الخادم ومساحات العمل المحفوظة"),
    "Serveur de téléchargement" to ("Download server" to "خادم التنزيل"),
    "Seuil alerte" to ("Alert threshold" to "حد التنبيه"),
    "Signaler un problème (avec journaux & captures)" to ("Report a problem (with logs & screenshots)" to "الإبلاغ عن مشكلة (مع السجلات ولقطات الشاشة)"),
    "Solde & mouvements" to ("Balance & transactions" to "الرصيد والحركات"),
    "Solde clôture" to ("Closing balance" to "الرصيد الختامي"),
    "Sortie" to ("Stock out" to "إخراج"),
    "Suivi de la paie" to ("Payroll tracking" to "متابعة الرواتب"),
    "Supprimer le rapport temporaire" to ("Delete temporary report" to "حذف التقرير المؤقت"),
    "Supprimer les PDF temporaires ?" to ("Delete temporary PDFs?" to "هل تريد حذف ملفات PDF المؤقتة؟"),
    "Supprimer" to ("Delete" to "حذف"),
    "TOTAL" to ("TOTAL" to "الإجمالي"),
    "Texte de la licence" to ("License text" to "نص الترخيص"),
    "Thème" to ("Theme" to "السمة"),
    "Ticket VR" to ("VR receipt" to "إيصال بدون ضريبة"),
    "Ticket créé" to ("Receipt created" to "تم إنشاء الإيصال"),
    "Titre court du problème" to ("Short issue title" to "عنوان مختصر للمشكلة"),
    "Tous" to ("All" to "الكل"),
    "Transactions récentes" to ("Recent transactions" to "الحركات الأخيرة"),
    "Trésorerie · créances · mouvements" to ("Cash · receivables · transactions" to "الخزينة · المستحقات · الحركات"),
    "Téléchargement interrompu" to ("Download interrupted" to "تم إيقاف التنزيل"),
    "Téléphone (facultatif)" to ("Phone (optional)" to "الهاتف (اختياري)"),
    "Valider" to ("Approve" to "اعتماد"),
    "Vente non enregistrée" to ("Sale not recorded" to "لم يتم تسجيل البيع"),
    "Vente rapide & caisse" to ("Quick sale & register" to "البيع السريع ونقطة البيع"),
    "Vente" to ("Sale" to "بيع"),
    "Vider" to ("Clear" to "مسح"),
    "Voir dans Paramètres" to ("View in Settings" to "عرض في الإعدادات"),
    "Voir le panier" to ("View cart" to "عرض السلة"),
    "Voir" to ("View" to "عرض"),
    "Vérifier les mises à jour" to ("Check for updates" to "التحقق من وجود تحديثات"),
    "Vérifier à nouveau" to ("Check again" to "التحقق مجدداً"),
    "Zoom" to ("Zoom" to "تكبير"),
    "À encaisser" to ("To collect" to "مطلوب تحصيله"),
    "À valider" to ("Pending approval" to "بانتظار الاعتماد"),
    "À venir" to ("Upcoming" to "قادم"),
    "Échec du téléchargement" to ("Download failed" to "فشل التنزيل"),
    "Équipe · paie à préparer" to ("Team · payroll to prepare" to "الفريق · رواتب للتحضير"),
    "Étapes (optionnel)" to ("Steps (optional)" to "الخطوات (اختياري)"),
    "Alignez le code-barres produit dans le cadre" to ("Align the product barcode inside the frame" to "ضع باركود المنتج داخل الإطار"),
    "Activez les notifications Android pour voir les alertes et le téléchargement en direct." to ("Enable Android notifications to see alerts and live download progress." to "فعّل إشعارات Android لعرض التنبيهات ومتابعة التنزيل مباشرة."),
    "Utilisez le bouton d'impression Atlas pour enregistrer en PDF. Retour ferme le rapport et vous ramène à l'écran précédent." to ("Use the Atlas print button to save as PDF. Back closes the report and returns to the previous screen." to "استخدم زر الطباعة في Atlas للحفظ بصيغة PDF. يؤدي الرجوع إلى إغلاق التقرير والعودة إلى الشاشة السابقة."),
    "Activez les notifications système dans les Paramètres Android pour recevoir immédiatement les alertes et suivre les téléchargements." to ("Enable system notifications in Android Settings to receive alerts immediately and track downloads." to "فعّل إشعارات النظام من إعدادات Android لاستلام التنبيهات فوراً ومتابعة التنزيلات."),
    "Ajoutez des produits depuis le catalogue ou en scannant un code-barres." to ("Add products from the catalog or by scanning a barcode." to "أضف منتجات من الكتالوج أو امسح باركوداً."),
    "Autorisation et canaux de notification Android" to ("Android notification permission and channels" to "إذن إشعارات Android وقنواتها"),
    "Aucun traqueur publicitaire" to ("No advertising trackers" to "لا توجد أدوات تتبع إعلانية"),
    "CATALOGUE & STOCK" to ("CATALOG & STOCK" to "الكتالوج والمخزون"),
    "Captures jointes" to ("Attached screenshots" to "لقطات الشاشة المرفقة"),
    "Cette version d'Atlas Commerce doit être mise à jour avant de continuer." to ("This version of Atlas Commerce must be updated before you can continue." to "يجب تحديث هذا الإصدار من Atlas Commerce قبل المتابعة."),
    "Chaque composant reste la propriété de ses auteurs et est utilisé selon sa licence." to ("Each component remains the property of its authors and is used under its license." to "تظل كل مكوّنات التطبيق ملكاً لمؤلفيها وتُستخدم وفقاً لترخيصها."),
    "Chaque serveur dispose de son propre stockage chiffré isolé. Changer de serveur ferme automatiquement la session en cours." to ("Each server has separate encrypted storage. Changing servers automatically ends the current session." to "لكل خادم مساحة تخزين مشفّرة ومعزولة. يؤدي تغيير الخادم إلى إنهاء الجلسة الحالية تلقائياً."),
    "Choisissez les rubriques et la période : mois, année en cours, derniers mois ou dates personnalisées. Le PDF bancaire reste mensuel." to ("Choose the sections and period: month, current year, recent months, or custom dates. The bank PDF remains monthly." to "اختر الأقسام والفترة: شهر، السنة الحالية، الأشهر الأخيرة أو تواريخ مخصصة. يظل كشف البنك شهرياً."),
    "Composants tiers" to ("Third-party components" to "مكوّنات الجهات الخارجية"),
    "Configurer le fournisseur Android" to ("Configure the Android provider" to "إعداد مزوّد Android"),
    "Consultation seule · échéances et versements du serveur Atlas" to ("Read-only · due dates and payments from the Atlas server" to "للقراءة فقط · المواعيد والدفعات من خادم Atlas"),
    "Demandes & notifications par rôle" to ("Requests & role-based notifications" to "الطلبات والإشعارات حسب الدور"),
    "Diagnostics inclus automatiquement" to ("Diagnostics included automatically" to "يتم إرفاق بيانات التشخيص تلقائياً"),
    "Distribué sous licence MIT. Forkez, modifiez, citez-nous." to ("Released under the MIT License. Fork, modify, and credit us." to "يُوزّع بموجب ترخيص MIT. يمكنكم إنشاء نسخة وتعديلها مع ذكر المصدر."),
    "Déclenche une alerte haute priorité sur les appareils du rôle" to ("Triggers a high-priority alert on devices for this role" to "يرسل تنبيهاً ذا أولوية عالية إلى أجهزة هذا الدور"),
    "Entrées" to ("Incoming" to "الوارد"),
    "Ex. : Tableau coupé à droite dans Agent IA" to ("E.g. content clipped on the right in Atlas Agent" to "مثال: المحتوى مقطوع من اليمين في Atlas Agent"),
    "Info Box & Notifications" to ("Info Box & Notifications" to "صندوق المعلومات والإشعارات"),
    "Filtré rôle" to ("Filtered by role" to "مصفّى حسب الدور"),
    "Chez l'Admin" to ("With Admin" to "لدى المسؤول"),
    "Journaux expurgés des secrets · les captures restent privées" to ("Logs are redacted · screenshots remain private" to "السجلات منقّحة من الأسرار · لقطات الشاشة خاصة"),
    "Le lien d'accès est temporaire (30 min). Les rubriques sélectionnées s'ouvrent dans Atlas." to ("The access link is temporary (30 min). Selected sections open in Atlas." to "رابط الوصول مؤقت (30 دقيقة). تُفتح الأقسام المحددة في Atlas."),
    "Le mode est uniquement local à cet appareil." to ("This mode is local to this device only." to "هذا الوضع محلي على هذا الجهاز فقط."),
    "Les chiffres proviennent des API financières du serveur sélectionné. Aucun solde n'est calculé ou codé en dur dans l'application." to ("Figures come from the selected server's financial APIs. The app does not calculate or hardcode balances." to "تأتي الأرقام من واجهات API المالية للخادم المحدد. لا يحسب التطبيق الأرصدة ولا يثبتها برمجياً."),
    "Les rapports téléchargés dans le cache privé de cette application seront supprimés de cet appareil. Les copies déjà enregistrées ou partagées ailleurs ne seront pas touchées." to ("Reports in this app's private cache will be removed from this device. Copies saved or shared elsewhere will not be affected." to "ستُحذف التقارير من ذاكرة التطبيق الخاصة على هذا الجهاز. لن تتأثر النسخ المحفوظة أو المشاركة في أماكن أخرى."),
    "Licence MIT" to ("MIT License" to "ترخيص MIT"),
    "Lien de rapport non sécurisé refusé." to ("An insecure report link was rejected." to "تم رفض رابط تقرير غير آمن."),
    "Liens & Support" to ("Links & Support" to "الروابط والدعم"),
    "Liens du projet" to ("Project links" to "روابط المشروع"),
    "Lire dans Atlas" to ("Open in Atlas" to "فتح في Atlas"),
    "Marques" to ("Trademarks" to "العلامات التجارية"),
    "Max 3 images · compression auto ≤ 1 Mo chacune" to ("Up to 3 images · automatically compressed to ≤ 1 MB each" to "حتى 3 صور · ضغط تلقائي إلى 1 ميغابايت كحد أقصى لكل صورة"),
    "Mise à jour & Miroirs CDN" to ("Update & CDN mirrors" to "التحديث ومرايا CDN"),
    "Nouveautés de cette version" to ("What's new in this version" to "الجديد في هذا الإصدار"),
    "Nouveautés des versions" to ("Version updates" to "مستجدات الإصدارات"),
    "Open source" to ("Open source" to "مفتوح المصدر"),
    "Pack rapports détaillés · ouvrir dans Atlas" to ("Detailed report bundle · open in Atlas" to "حزمة تقارير مفصلة · فتح في Atlas"),
    "Passkey Android" to ("Android passkey" to "مفتاح مرور Android"),
    "Ping" to ("Ping" to "اختبار الاتصال"),
    "PÉRIODE" to ("PERIOD" to "الفترة"),
    "QR SÉCURITÉ POSTE PC UNIQUEMENT" to ("QR SECURITY · DESKTOP ONLY" to "رمز QR للأمان · لأجهزة الكمبيوتر فقط"),
    "RUBRIQUES" to ("SECTIONS" to "الأقسام"),
    "Recherche de mises à jour sur le serveur…" to ("Checking the server for updates…" to "جارٍ البحث عن تحديثات على الخادم…"),
    "Relevé bancaire du mois sélectionné, y compris le mois en cours." to ("Bank statement for the selected month, including the current month." to "كشف البنك للشهر المحدد، بما في ذلك الشهر الحالي."),
    "SOLDE DE CLÔTURE" to ("CLOSING BALANCE" to "الرصيد الختامي"),
    "SOLDE DE TRÉSORERIE" to ("CASH BALANCE" to "رصيد الخزينة"),
    "Vérifiez les dates : début avant fin, et aucune date future." to ("Check the dates: the start must precede the end, and neither date can be in the future." to "تحقّق من التواريخ: يجب أن يسبق تاريخ البدء تاريخ الانتهاء، وألا يكون أي منهما في المستقبل."),
    "1. Ouvrir Agent IA  2. Envoyer une demande…" to ("1. Open Atlas Agent  2. Send a request…" to "١. افتح Atlas Agent  ٢. أرسل طلباً…"),
    "OK" to ("OK" to "حسناً"),
    "La liste des employés et les informations de paie sont réservées aux rôles administrateur. Aucune donnée n'a été demandée au serveur." to ("Employee lists and payroll information are restricted to administrator roles. No data was requested from the server." to "قوائم الموظفين ومعلومات الرواتب مخصّصة لأدوار المسؤولين. لم يتم طلب أي بيانات من الخادم."),
    "Création directe réservée à l'Admin : cette fiche est envoyée immédiatement à l'Administrateur pour validation." to ("Direct creation is restricted to Admin: this item is sent to the administrator immediately for approval." to "الإنشاء المباشر مخصّص للمسؤول: يُرسل هذا العنصر فوراً إلى المسؤول للموافقة."),
    "La ventilation diffère du relevé; vérifiez la cohérence des données serveur." to ("The breakdown differs from the statement; verify the server data is consistent." to "يختلف التفصيل عن الكشف؛ تحقّق من اتساق بيانات الخادم."),
    "Android limite les journaux accessibles à l'application. Les événements Atlas, les réponses HTTP et les traces de son processus restent inclus; le bruit graphique courant est regroupé." to ("Android limits which logs an app can access. Atlas events, HTTP responses and its process traces are included; routine UI noise is grouped." to "يقيّد Android السجلات التي يمكن للتطبيق الوصول إليها. تشمل السجلات أحداث Atlas واستجابات HTTP وآثار عمليته، مع تجميع ضوضاء الواجهة المعتادة."),
    "Cette action déconnecte ce compte et supprime les sessions mémorisées, serveurs enregistrés, préférences, cache hors ligne, journaux et PDF temporaires de cet appareil. Elle ne supprime aucune vente, facture ni donnée d'entreprise sur le serveur." to ("This signs out this account and removes remembered sessions, saved servers, preferences, offline cache, logs and temporary PDFs from this device. It does not delete sales, invoices or business data from the server." to "يسجّل هذا الإجراء خروج الحساب ويحذف الجلسات المحفوظة والخوادم المسجلة والتفضيلات وذاكرة التخزين المؤقت والسجلات وملفات PDF المؤقتة من هذا الجهاز. ولا يحذف أي مبيعات أو فواتير أو بيانات تجارية من الخادم."),
    "Dernière" to ("Latest" to "الأحدث"),
    "Scannez un code-barres EAN / SKU pour vérifier ou vendre, ou annulez à tout moment si vous ne souhaitez pas enregistrer l'objet." to ("Scan an EAN / SKU barcode to check or sell an item, or cancel at any time if you do not want to record it." to "امسح باركود EAN أو SKU للتحقق من المنتج أو بيعه، أو ألغِ في أي وقت إذا كنت لا تريد تسجيله."),
    "Ajoutez une connexion rapide et protégée par le gestionnaire de mots de passe de cet appareil. Vous pourrez aussi la créer plus tard dans Paramètres > Sécurité. Cette étape est facultative." to ("Add a quick sign-in protected by this device's password manager. You can also set it up later in Settings > Security. This step is optional." to "أضف طريقة تسجيل دخول سريعة محمية بمدير كلمات المرور على هذا الجهاز. يمكنك إعدادها لاحقاً من الإعدادات > الأمان. هذه الخطوة اختيارية."),
    "• Client Atlas BTP doit régler 3 200 DH.\n• Client Marbre Plus a payé sa facture.\n• Facture fournisseur de 1 450 DH à payer.\n• Paie de l’équipe à préparer avant le 3." to ("• Atlas BTP customer owes 3,200 MAD.\n• Marbre Plus customer paid their invoice.\n• Supplier invoice of 1,450 MAD due.\n• Prepare the team's payroll before the 3rd." to "• على عميل Atlas BTP دفع 3,200 درهم.\n• دفع عميل Marbre Plus فاتورته.\n• فاتورة مورد بقيمة 1,450 درهم مستحقة.\n• جهّز رواتب الفريق قبل اليوم الثالث."),
    "Autorisez « Installer des applis inconnues » dans les réglages Android pour lancer l'installation." to ("Allow installs from unknown sources in Android settings to start installation." to "اسمح بالتثبيت من مصادر غير معروفة في إعدادات Android لبدء التثبيت."),
    "Test en cours…" to ("Testing…" to "جارٍ الاختبار…"),
    "Tester les miroirs" to ("Test mirrors" to "اختبار المرايا"),
    "Télécharger" to ("Download" to "تنزيل"),
    "Autoriser et installer" to ("Allow & install" to "السماح والتثبيت"),
    "Installer maintenant" to ("Install now" to "التثبيت الآن"),
    "Réessayer le téléchargement" to ("Retry download" to "إعادة محاولة التنزيل"),
    "Installer la mise à jour" to ("Install update" to "تثبيت التحديث"),
    "Enregistrement…" to ("Saving…" to "جارٍ الحفظ…"),
    "Continuer vers la présentation" to ("Continue to introduction" to "متابعة إلى المقدمة"),
    "Commencer" to ("Get started" to "ابدأ"),
    "Continuer vers la connexion" to ("Continue to sign in" to "المتابعة إلى تسجيل الدخول"),
    "Chargement des soldes et mouvements…" to ("Loading balances and transactions…" to "جارٍ تحميل الأرصدة والحركات…"),
    "Chargement des données employés…" to ("Loading employee data…" to "جارٍ تحميل بيانات الموظفين…"),
    "Chargement de la paie…" to ("Loading payroll…" to "جارٍ تحميل الرواتب…"),
    "Chargement du tableau de bord…" to ("Loading dashboard…" to "جارٍ تحميل لوحة المعلومات…"),
    "Nouvelle vente" to ("New sale" to "بيع جديد"),
    "Scanner un article" to ("Scan an item" to "مسح منتج"),
    "Valider le code" to ("Verify code" to "تحقّق من الرمز"),
    "Activer les notifications dans Paramètres" to ("Enable notifications in Settings" to "تفعيل الإشعارات من الإعدادات"),
    "Chargement de l'Info Box…" to ("Loading Info Box…" to "جارٍ تحميل صندوق المعلومات…"),
    "Chargement du catalogue…" to ("Loading catalog…" to "جارٍ تحميل الكتالوج…"),
    "Créer le ticket VR" to ("Create VR receipt" to "إنشاء إيصال VR"),
    "Chargement…" to ("Loading…" to "جارٍ التحميل…"),
    "Mode hors ligne — données enregistrées sur l'appareil." to ("Offline mode — data saved on this device." to "وضع عدم الاتصال — البيانات محفوظة على هذا الجهاز."),
    "Connexion impossible" to ("Connection unavailable" to "تعذّر الاتصال"),
    "Progression du téléchargement" to ("Download progress" to "تقدم التنزيل"),
    "Retour au rapport précédent" to ("Back to previous report" to "العودة إلى التقرير السابق"),
    "Retour à la page précédente" to ("Back to previous page" to "العودة إلى الصفحة السابقة"),
    "Supprimer le PDF temporaire" to ("Delete temporary PDF" to "حذف ملف PDF المؤقت"),
    "Réduire le zoom" to ("Zoom out" to "تصغير"),
    "Agrandir le PDF" to ("Enlarge PDF" to "تكبير PDF"),
    "Imprimer ou enregistrer en PDF" to ("Print or save as PDF" to "طباعة أو حفظ كملف PDF"),
    "Générer un SKU" to ("Generate SKU" to "إنشاء رمز SKU"),
    "Actualiser les soldes" to ("Refresh balances" to "تحديث الأرصدة"),
    "Choisir un mois" to ("Choose a month" to "اختر شهراً"),
    "Actualiser les employés" to ("Refresh employees" to "تحديث الموظفين"),
    "Mois précédent" to ("Previous month" to "الشهر السابق"),
    "Mois suivant" to ("Next month" to "الشهر التالي"),
    "Actualiser les finances" to ("Refresh finances" to "تحديث البيانات المالية"),
    "Retirer l'image" to ("Remove image" to "إزالة الصورة"),
    "Rafraîchir les journaux" to ("Refresh logs" to "تحديث السجلات"),
    "Copier les journaux" to ("Copy logs" to "نسخ السجلات"),
    "Ouvrir le profil GitHub Etherealmber" to ("Open Etherealmber's GitHub profile" to "فتح ملف Etherealmber على GitHub"),
    "Envoyer une notification" to ("Send a notification" to "إرسال إشعار"),
    "Scanner un code-barres" to ("Scan a barcode" to "مسح باركود"),
    "Réduire" to ("Decrease" to "تقليل"),
    "Augmenter" to ("Increase" to "زيادة"),
    "Diminuer" to ("Decrease" to "تقليل"),
    "Retirer" to ("Remove" to "إزالة"),
    "Copier tous les journaux" to ("Copy all logs" to "نسخ كل السجلات"),
    "Actualiser l'historique" to ("Refresh history" to "تحديث السجل"),
    "Actualiser l'historique des versions" to ("Refresh version history" to "تحديث سجل الإصدارات"),
    "Supprimer ce serveur" to ("Delete this server" to "حذف هذا الخادم"),
    "Menu rapide (Notifications et Vente)" to ("Quick menu (Notifications and Sale)" to "القائمة السريعة (الإشعارات والبيع)"),
    "Oublier ce compte" to ("Forget this account" to "نسيان هذا الحساب"),
    "Demander un produit à l'Admin" to ("Request a product from Admin" to "طلب منتج من المسؤول"),
    "Copie PDF enregistrée." to ("PDF copy saved." to "تم حفظ نسخة PDF."),
    "Impossible d'enregistrer le PDF." to ("Could not save the PDF." to "تعذّر حفظ ملف PDF."),
    "Impossible de partager le PDF." to ("Could not share the PDF." to "تعذّرت مشاركة ملف PDF."),
    "Impossible d'ouvrir l'impression." to ("Could not open printing." to "تعذّر فتح الطباعة."),
    "Rapport temporaire supprimé." to ("Temporary report deleted." to "تم حذف التقرير المؤقت."),
    "Écriture enregistrée dans la balance." to ("Entry saved to the balance." to "تم حفظ القيد في الرصيد."),
    "PDF téléchargé et ouvert dans Atlas." to ("PDF downloaded and opened in Atlas." to "تم تنزيل ملف PDF وفتحه في Atlas."),
    "Hors ligne — dernières données affichées." to ("Offline — showing the latest available data." to "غير متصل — يتم عرض أحدث البيانات المتاحة."),
    "Maximum 3 captures autorisées par rapport." to ("A maximum of 3 screenshots is allowed per report." to "يُسمح بثلاث لقطات شاشة كحد أقصى لكل تقرير."),
    "Journaux assainis copiés." to ("Sanitized logs copied." to "تم نسخ السجلات المنقحة."),
    "Partage indisponible sur cet appareil." to ("Sharing is unavailable on this device." to "المشاركة غير متاحة على هذا الجهاز."),
    "Rapport reçu par Atlas. Impossible d'ouvrir le suivi sur cet appareil." to ("Atlas received the report. Could not open tracking on this device." to "استلم Atlas البلاغ. تعذّر فتح صفحة المتابعة على هذا الجهاز."),
    "Ouvrez Paramètres Android > Applications > Atlas Commerce > Notifications." to ("Open Android Settings > Apps > Atlas Commerce > Notifications." to "افتح إعدادات Android > التطبيقات > Atlas Commerce > الإشعارات."),
    "Client par défaut enregistré." to ("Default customer saved." to "تم حفظ العميل الافتراضي."),
    "Mode développeur activé. Ouverture des outils développeur." to ("Developer mode enabled. Opening developer tools." to "تم تفعيل وضع المطوّر. جارٍ فتح أدوات المطوّر."),
    "Mode développeur désactivé." to ("Developer mode disabled." to "تم إيقاف وضع المطوّر."),
    "Impossible d'ouvrir l'installateur sur cet appareil." to ("Could not open the installer on this device." to "تعذّر فتح برنامج التثبيت على هذا الجهاز."),
    "L'administrateur doit configurer Firebase Crashlytics pour cette version." to ("The administrator must configure Firebase Crashlytics for this version." to "يجب على المسؤول إعداد Firebase Crashlytics لهذا الإصدار."),
    "Ouvrez Paramètres Android > Mots de passe et clés d'accès, puis choisissez un fournisseur." to ("Open Android Settings > Passwords and passkeys, then choose a provider." to "افتح إعدادات Android > كلمات المرور ومفاتيح المرور، ثم اختر مزوّداً."),
    "Aucun navigateur disponible." to ("No browser is available." to "لا يتوفر متصفح."),
    "Impossible de lire le fichier." to ("Could not read the file." to "تعذّرت قراءة الملف."),
    "Nouvelle discussion prête." to ("New conversation ready." to "المحادثة الجديدة جاهزة."),
    "Réponse copiée." to ("Response copied." to "تم نسخ الرد."),
    "La dictée Android est gratuite dans Atlas ; l'IA cloud peut entraîner des coûts ou quotas." to ("Android speech recognition is free in Atlas; cloud AI may incur costs or quotas." to "الإملاء الصوتي عبر Android مجاني في Atlas؛ وقد تترتب تكاليف أو حصص استخدام على الذكاء الاصطناعي السحابي."),
    "Journaux de diagnostic copiés." to ("Diagnostic logs copied." to "تم نسخ سجلات التشخيص."),
    "Accès Vente réservé aux rôles Administrateur et Secrétaire." to ("Sales access is restricted to Administrator and Secretary roles." to "الوصول إلى المبيعات مخصّص لدوري المسؤول والسكرتير."),
    "Votre rôle ne permet pas d'ajouter au panier de vente." to ("Your role cannot add items to the sales cart." to "لا يسمح دورك بإضافة منتجات إلى سلة المبيعات."),
    "Objet scanné annulé et retiré du panier." to ("Scanned item canceled and removed from the cart." to "تم إلغاء المنتج الممسوح وإزالته من السلة."),
    "Scan annulé — rien n'a été enregistré." to ("Scan canceled — nothing was recorded." to "تم إلغاء المسح — لم يتم تسجيل أي شيء."),
    "Demande produit enregistrée." to ("Product request submitted." to "تم إرسال طلب المنتج."),
    "Seul un Administrateur peut écarter une demande produit." to ("Only an Administrator can dismiss a product request." to "يستطيع المسؤول فقط رفض طلب المنتج."),
    "Votre rôle ne permet pas d'ajuster le stock." to ("Your role cannot adjust inventory." to "لا يسمح دورك بتعديل المخزون."),
    "Indiquez une quantité supérieure à zéro." to ("Enter a quantity greater than zero." to "أدخل كمية أكبر من صفر."),
    "Hors ligne — catalogue enregistré sur l'appareil." to ("Offline — catalog saved on this device." to "غير متصل — الكتالوج محفوظ على هذا الجهاز."),
    "Veuillez saisir un objet pour la notification." to ("Enter a subject for the notification." to "أدخل عنواناً للإشعار."),
    "Seul un Administrateur peut valider et créer ce produit." to ("Only an Administrator can approve and create this product." to "يستطيع المسؤول فقط الموافقة على هذا المنتج وإنشاؤه."),
    "Seul un Administrateur ou responsable peut refuser cette demande." to ("Only an Administrator or manager can reject this request." to "يستطيع المسؤول أو المدير فقط رفض هذا الطلب."),
    "Poste PC connecté à votre session Atlas." to ("Desktop connected to your Atlas session." to "تم ربط الكمبيوتر بجلسة Atlas الخاصة بك."),
    "Demande de connexion PC refusée." to ("Desktop sign-in request denied." to "تم رفض طلب تسجيل دخول الكمبيوتر."),
    "Connecté à Atlas Commerce." to ("Connected to Atlas Commerce." to "تم الاتصال بـ Atlas Commerce."),
    "Discussion supprimée." to ("Conversation deleted." to "تم حذف المحادثة."),
    "Atlas Agent accepte jusqu'à 4 images par message." to ("Atlas Agent accepts up to 4 images per message." to "يقبل Atlas Agent حتى 4 صور في كل رسالة."),
    "Atlas Agent accepte jusqu'à 2 documents par message." to ("Atlas Agent accepts up to 2 documents per message." to "يقبل Atlas Agent حتى مستندين في كل رسالة."),
    "Action approuvée et exécutée." to ("Action approved and executed." to "تمت الموافقة على الإجراء وتنفيذه."),
    "Connexion vérifiée par e-mail — bienvenue !" to ("Email verified — welcome!" to "تم التحقق من البريد الإلكتروني — أهلاً بك!"),
    "Serveur supprimé de cet appareil." to ("Server removed from this device." to "تمت إزالة الخادم من هذا الجهاز."),
    "Cache catalogue hors ligne vidé." to ("Offline catalog cache cleared." to "تم مسح ذاكرة التخزين المؤقت للكتالوج دون اتصال."),
    "Impossible de lire cette image." to ("Could not read this image." to "تعذّرت قراءة هذه الصورة."),
    "Photo de profil mise à jour." to ("Profile photo updated." to "تم تحديث صورة الملف الشخصي."),
    "Le nom affiché ne peut pas être vide." to ("Display name cannot be empty." to "لا يمكن أن يكون اسم العرض فارغاً."),
    "Profil mis à jour." to ("Profile updated." to "تم تحديث الملف الشخصي."),
    "Serveur modifié — veuillez vous reconnecter." to ("Server changed — please sign in again." to "تم تغيير الخادم — يرجى تسجيل الدخول مجدداً."),
    "Session révoquée sur cet appareil." to ("Session revoked on this device." to "تم إلغاء الجلسة على هذا الجهاز."),
    "Cet appareil a été déconnecté." to ("This device has been signed out." to "تم تسجيل الخروج من هذا الجهاز."),
    "Rapports automatiques indisponibles : Firebase Crashlytics n'est pas configuré pour cette version." to ("Automatic reports are unavailable because Firebase Crashlytics is not configured for this version." to "التقارير التلقائية غير متاحة لأن Firebase Crashlytics غير مُعدّ لهذا الإصدار."),
    "Rapports de plantage activés. Les prochains plantages seront envoyés automatiquement à Firebase Crashlytics." to ("Crash reports enabled. Future crashes will be sent automatically to Firebase Crashlytics." to "تم تفعيل تقارير الأعطال. ستُرسل الأعطال القادمة تلقائياً إلى Firebase Crashlytics."),
    "Envoi automatique des rapports de plantage désactivé." to ("Automatic crash report sending disabled." to "تم إيقاف الإرسال التلقائي لتقارير الأعطال."),
    "Aucun rapport PDF temporaire à supprimer." to ("There are no temporary PDF reports to delete." to "لا توجد تقارير PDF مؤقتة لحذفها."),
    "Type" to ("Type" to "النوع"),
    "Mode de paiement" to ("Payment method" to "طريقة الدفع"),
    "Filtrer" to ("Filter" to "تصفية"),
    "Toutes les écritures" to ("All entries" to "كل القيود"),
    "Dépense" to ("Expense" to "مصروف"),
    "Recette" to ("Income" to "إيراد"),
    "Apport propriétaire" to ("Owner contribution" to "مساهمة المالك"),
    "Prélèvement propriétaire" to ("Owner withdrawal" to "سحب المالك"),
    "Loyer" to ("Rent" to "إيجار"),
    "Transport" to ("Transport" to "النقل"),
    "Fournitures" to ("Supplies" to "لوازم"),
    "Salaires" to ("Wages" to "أجور"),
    "Taxes" to ("Taxes" to "ضرائب"),
    "Autre dépense" to ("Other expense" to "مصروف آخر"),
    "Vente divers" to ("Miscellaneous sale" to "مبيعات متنوعة"),
    "Prestation de service" to ("Service" to "خدمة"),
    "Remboursement" to ("Refund" to "استرداد"),
    "Subvention" to ("Grant" to "منحة"),
    "Intérêts" to ("Interest" to "فوائد"),
    "Autre recette" to ("Other income" to "إيراد آخر"),
    "Apport en capital" to ("Capital contribution" to "مساهمة رأسمالية"),
    "Apport en compte courant" to ("Shareholder loan contribution" to "مساهمة في الحساب الجاري"),
    "Autre apport" to ("Other contribution" to "مساهمة أخرى"),
    "Prélèvement personnel" to ("Personal withdrawal" to "سحب شخصي"),
    "Dividende" to ("Dividend" to "توزيعات أرباح"),
    "Remboursement associé" to ("Partner reimbursement" to "استرداد للشريك"),
    "Autre prélèvement" to ("Other withdrawal" to "سحب آخر"),
    "Espèces (caisse)" to ("Cash" to "نقداً"),
    "Banque / virement" to ("Bank transfer" to "تحويل بنكي"),
    "Carte" to ("Card" to "بطاقة"),
    "Chèque" to ("Cheque" to "شيك"),
    "Activez l'autorisation « Installer des applis inconnues » pour terminer l'installation." to ("Enable the ‘Install unknown apps’ permission to finish installing." to "فعّل إذن تثبيت التطبيقات غير المعروفة لإكمال التثبيت."),
    "Du 1er janvier à aujourd'hui, selon le fuseau financier du serveur." to ("From January 1 through today, using the server's financial time zone." to "من 1 يناير حتى اليوم، وفق المنطقة الزمنية المالية للخادم."),
    "Mois en cours jusqu'à aujourd'hui ; les périodes passées couvrent le mois complet." to ("Current month through today; past periods cover the full month." to "الشهر الحالي حتى اليوم؛ الفترات السابقة تغطي الشهر كاملاً."),
    "Période calculée par le serveur dans le fuseau financier configuré." to ("Period calculated by the server in its configured financial time zone." to "الفترة محسوبة بواسطة الخادم وفق المنطقة الزمنية المالية المحددة."),
    "Nouveau produit (Création Admin)" to ("New product (Admin creation)" to "منتج جديد (إنشاء المسؤول)"),
    "Nouveau produit · Envoi à l'Admin" to ("New product · Submit to Admin" to "منتج جديد · الإرسال إلى المسؤول"),
    "Code-barres (EAN / QR)" to ("Barcode (EAN / QR)" to "الباركود (EAN / QR)"),
    "Code-barres / EAN *" to ("Barcode / EAN *" to "الباركود / EAN *"),
    "Envoyer à l'Admin" to ("Send to Admin" to "إرسال إلى المسؤول"),
    "Créer le produit" to ("Create product" to "إنشاء المنتج"),
    "Enregistrer la demande" to ("Submit request" to "إرسال الطلب"),
    "Mois" to ("Month" to "الشهر"),
    "Relevé réconcilié" to ("Statement reconciled" to "تمت مطابقة الكشف"),
    "Client" to ("Customer" to "العميل"),
    "Mois en cours · données provisoires" to ("Current month · provisional data" to "الشهر الحالي · بيانات أولية"),
    "Payé" to ("Paid" to "مدفوع"),
    "En retard" to ("Overdue" to "متأخر"),
    "Compression…" to ("Compressing…" to "جارٍ الضغط…"),
    "Aucun journal récent." to ("No recent logs." to "لا توجد سجلات حديثة."),
    "Validez en 1 clic les demandes produits envoyées par l'équipe" to ("Approve product requests from the team in one tap" to "اعتمد طلبات المنتجات المرسلة من الفريق بلمسة واحدة"),
    "Créer & Valider" to ("Create & approve" to "إنشاء واعتماد"),
    "Notifications reçues pour votre rôle et suivi de vos envois Admin" to ("Notifications for your role and status of submissions to Admin" to "إشعارات دورك ومتابعة الطلبات المرسلة إلى المسؤول"),
    "Marquer traité" to ("Mark handled" to "وضع علامة تمت المعالجة"),
    "Demander à l'Admin" to ("Request from Admin" to "طلب من المسؤول"),
    "Les demandes d'ajout envoyées par l'équipe apparaissent ici pour création immédiate." to ("Product requests submitted by the team appear here for immediate creation." to "تظهر هنا طلبات إضافة المنتجات المرسلة من الفريق لإنشائها فوراً."),
    "Vos fiches produits transmises à l'Administrateur apparaissent ici." to ("Product requests sent to the administrator appear here." to "تظهر هنا طلبات المنتجات التي أرسلتها إلى المسؤول."),
    "Créer un produit" to ("Create product" to "إنشاء منتج"),
    "Créez un produit directement avec le bouton + ou scannez son code-barres." to ("Create a product with the + button or scan its barcode." to "أنشئ منتجاً مباشرةً باستخدام زر + أو امسح الباركود."),
    "Envoyez une demande de création à l'Administrateur avec le bouton d'envoi." to ("Send a product creation request to the administrator using the send button." to "أرسل طلب إنشاء منتج إلى المسؤول باستخدام زر الإرسال."),
    "Essayez un autre nom, SKU ou code-barres." to ("Try another name, SKU or barcode." to "جرّب اسماً أو رمز SKU أو باركوداً آخر."),
    "Créer" to ("Create" to "إنشاء"),
    "Votre rôle permet de consulter le stock, pas de créer une vente. " to ("Your role can view inventory but cannot create sales. " to "يتيح دورك عرض المخزون، وليس إنشاء المبيعات. "),
    "1. Installez et déployez d'abord l'application web Atlas Commerce sur votre domaine HTTPS.\n" to ("1. First install and deploy the Atlas Commerce web app on your HTTPS domain.\n" to "١. ثبّت وانشر أولاً تطبيق Atlas Commerce على نطاق HTTPS الخاص بك.\n"),
    "Le serveur les autorise. La création exige aussi un gestionnaire Android compatible et un verrouillage d'écran." to ("The server must allow them. Creation also requires a compatible Android credential manager and a screen lock." to "يجب أن يسمح بها الخادم. يتطلب الإنشاء أيضاً مدير بيانات اعتماد متوافقاً مع Android وقفل شاشة."),
    "Enregistrement en cours…" to ("Saving…" to "جارٍ الحفظ…"),
    "Aucun serveur configuré" to ("No server configured" to "لم يتم إعداد خادم"),
    "Mois en cours est disponible avant clôture." to ("The current month is available before closing." to "الشهر الحالي متاح قبل الإغلاق."),
    "Licence" to ("License" to "الترخيص"),
    "MIT · open source" to ("MIT · open source" to "MIT · مفتوح المصدر"),
    "Sécurité & confidentialité" to ("Security & privacy" to "الأمان والخصوصية"),
    "Ce que l'app fait de vos données" to ("How the app handles your data" to "كيف يتعامل التطبيق مع بياناتك"),
    "À propos" to ("About" to "حول التطبيق"),
    "Authentification" to ("Authentication" to "المصادقة"),
    "Mot de passe + code e-mail" to ("Password + email code" to "كلمة المرور + رمز البريد الإلكتروني"),
    "Transport réseau" to ("Network transport" to "نقل الشبكة"),
    "HTTPS obligatoire" to ("HTTPS required" to "HTTPS مطلوب"),
    "Données conservées" to ("Stored data" to "البيانات المخزنة"),
    "Minimum vital" to ("Minimum necessary" to "الحد الأدنى الضروري"),
    "Vos responsabilités" to ("Your responsibilities" to "مسؤولياتك"),
    "Auto-hébergement" to ("Self-hosting" to "الاستضافة الذاتية"),
    "Signaler une vulnérabilité" to ("Report a vulnerability" to "الإبلاغ عن ثغرة أمنية"),
    "Réponse sous 48 h" to ("Response within 48 hours" to "الرد خلال 48 ساعة"),
    "L'application ne contacte que le serveur Atlas que vous avez choisi, à une exception : ML Kit." to ("The app contacts only the Atlas server you selected, with one exception: ML Kit." to "لا يتصل التطبيق إلا بخادم Atlas الذي اخترته، باستثناء واحد هو ML Kit."),
    "Aucune image de caméra n'est envoyée : les codes-barres sont décodés sur l'appareil." to ("No camera images are sent: barcodes are decoded on the device." to "لا تُرسل صور الكاميرا؛ إذ تتم قراءة الباركود على الجهاز."),
    "ML Kit (Google, lecture des codes-barres) transmet ses métriques d'usage et de performance à Google : version de l'app, identifiant d'installation, appareil. Aucune image, aucun code scanné." to ("ML Kit (Google barcode scanning) sends usage and performance metrics to Google: app version, installation ID and device. No images or scanned codes are sent." to "يرسل ML Kit من Google، المستخدم لقراءة الباركود، مقاييس الاستخدام والأداء إلى Google: إصدار التطبيق ومعرّف التثبيت والجهاز. لا تُرسل صور أو رموز ممسوحة."),
    "Aucun contact, aucune localisation, aucun accès au stockage n'est demandé." to ("The app does not request contacts, location or storage access." to "لا يطلب التطبيق الوصول إلى جهات الاتصال أو الموقع أو مساحة التخزين."),
    "Le mot de passe part en HTTPS et n'est jamais stocké sur l'appareil." to ("The password is sent over HTTPS and is never stored on the device." to "تُرسل كلمة المرور عبر HTTPS ولا تُخزّن على الجهاز مطلقاً."),
    "Un nouvel appareil déclenche un code OTP à 6 chiffres, valable 10 minutes." to ("A new device triggers a 6-digit OTP valid for 10 minutes." to "يؤدي استخدام جهاز جديد إلى إرسال رمز OTP من 6 أرقام صالح لمدة 10 دقائق."),
    "La session est liée à l'adresse IP et à l'empreinte de l'appareil." to ("The session is bound to the IP address and device fingerprint." to "ترتبط الجلسة بعنوان IP وبصمة الجهاز."),
    "Le jeton est chiffré en AES-256-GCM avec une clé du Keystore Android." to ("The token is encrypted with AES-256-GCM using an Android Keystore key." to "يُشفّر الرمز باستخدام AES-256-GCM ومفتاح من Android Keystore."),
    "La déconnexion efface le jeton, le panier et le cache du catalogue." to ("Signing out clears the token, cart and catalog cache." to "يؤدي تسجيل الخروج إلى مسح الرمز والسلة وذاكرة الكتالوج المؤقتة."),
    "Les versions de production refusent le trafic HTTP en clair." to ("Production builds reject unencrypted HTTP traffic." to "ترفض إصدارات الإنتاج حركة HTTP غير المشفّرة."),
    "Le certificat TLS est vérifié par la pile réseau d'Android." to ("The Android network stack verifies the TLS certificate." to "يتحقق مكدس الشبكة في Android من شهادة TLS."),
    "Seules les versions debug autorisent un serveur local (émulateur)." to ("Only debug builds allow a local server (emulator)." to "تسمح إصدارات التصحيح فقط بخادم محلي (المحاكي)."),
    "Jeton de session chiffré + identifiant, rôle et nom affiché." to ("Encrypted session token + account ID, role and display name." to "رمز جلسة مشفّر + معرّف الحساب والدور والاسم المعروض."),
    "Catalogue produits en cache pour le mode hors ligne (aucune donnée financière)." to ("Product catalog cached for offline use (no financial data)." to "يُخزّن كتالوج المنتجات مؤقتاً للعمل دون اتصال (من دون بيانات مالية)."),
    "Préférences d'apparence et adresse du serveur." to ("Appearance preferences and server address." to "تفضيلات المظهر وعنوان الخادم."),
    "Effacement complet : déconnexion, puis désinstallation de l'app." to ("For a complete wipe, sign out and then uninstall the app." to "للمسح الكامل، سجّل الخروج ثم أزل تثبيت التطبيق."),
    "Tableau de bord : trésorerie, valeur du stock, créances, tendance." to ("Dashboard: cash, inventory value, receivables and trends." to "لوحة المعلومات: الخزينة وقيمة المخزون والمستحقات والاتجاهات."),
    "Catalogue produits avec recherche, stock faible et mode hors ligne." to ("Product catalog with search, low-stock alerts and offline mode." to "كتالوج المنتجات مع البحث وتنبيهات انخفاض المخزون والعمل دون اتصال."),
    "Scan de codes-barres pour ajouter un article au panier." to ("Scan barcodes to add items to the cart." to "امسح الباركود لإضافة المنتجات إلى السلة."),
    "Vente rapide : ticket VR sans TVA, prêt à encaisser sur le web." to ("Quick sale: VAT-free VR receipt, ready for payment on the web." to "بيع سريع: إيصال VR بدون ضريبة، جاهز للتحصيل عبر الويب."),
    "Mises à jour accélérées multi-connexions (4 flux HTTP Range) et diagnostic @atlas-bot." to ("Accelerated multi-connection updates (4 HTTP Range streams) and @atlas-bot diagnostics." to "تحديثات متعددة الاتصالات أسرع (4 تدفقات HTTP Range) وتشخيص عبر @atlas-bot."),
    "Atlas Commerce fonctionne avec votre propre serveur. L'administrateur du serveur voit les ventes, le stock et les comptes. Choisissez un serveur de confiance et un mot de passe fort, et activez la double authentification côté web." to ("Atlas Commerce uses your own server. Its administrator can see sales, inventory and accounts. Choose a trusted server, use a strong password and enable two-factor authentication on the web app." to "يعمل Atlas Commerce عبر خادمك الخاص. يمكن لمسؤول الخادم الاطلاع على المبيعات والمخزون والحسابات. اختر خادماً موثوقاً واستخدم كلمة مرور قوية وفعّل المصادقة الثنائية في تطبيق الويب."),
    "Merci de ne pas ouvrir d'issue publique pour une faille. Écrivez à ${AppDetails.SECURITY_EMAIL} ou à @${AppDetails.GITHUB_HANDLE} avec les étapes de reproduction." to ("Please do not open a public issue for a security vulnerability. Email ${AppDetails.SECURITY_EMAIL} or contact @${AppDetails.GITHUB_HANDLE} with reproduction steps." to "يرجى عدم فتح بلاغ عام عن ثغرة أمنية. راسل ${AppDetails.SECURITY_EMAIL} أو @${AppDetails.GITHUB_HANDLE} مع خطوات إعادة إنتاجها."),
    "Contacter la sécurité" to ("Contact security" to "التواصل مع فريق الأمان"),
    "Stock · Caisse · Facturation" to ("Inventory · Register · Invoicing" to "المخزون · نقطة البيع · الفوترة"),
    "ERP de terrain pour commerces et grossistes : stock multi-dépôts, caisse, tickets VR, facturation TVA." to ("Field ERP for retailers and wholesalers: multi-warehouse inventory, register, VR receipts and VAT invoicing." to "نظام ERP ميداني للمتاجر وتجار الجملة: مخزون متعدد المستودعات ونقطة بيع وإيصالات VR وفوترة ضريبية."),
    "Voir le dépôt web" to ("View web repository" to "عرض مستودع الويب"),
    "Projet Atlas Commerce (web)" to ("Atlas Commerce project (web)" to "مشروع Atlas Commerce (الويب)"),
    "Code source Android" to ("Android source code" to "المصدر البرمجي لـ Android"),
    "Profil GitHub" to ("GitHub profile" to "ملف GitHub"),
    "Pas d'analytics, pas de publicité. Seul ML Kit (scan) transmet ses statistiques d'usage à Google." to ("No analytics or advertising. Only ML Kit (scanning) sends usage statistics to Google." to "لا تحليلات ولا إعلانات. يرسل ML Kit فقط، المستخدم للمسح، إحصاءات الاستخدام إلى Google."),
    "Cette application est le compagnon Android du projet web Atlas Commerce : elle se connecte à votre serveur et ne stocke aucune comptabilité localement." to ("This Android app accompanies the Atlas Commerce web project: it connects to your server and stores no accounting data locally." to "هذا التطبيق هو رفيق Android لمشروع Atlas Commerce على الويب؛ يتصل بخادمك ولا يخزّن بيانات المحاسبة محلياً."),
    "Réconciliation indisponible" to ("Reconciliation unavailable" to "المطابقة غير متاحة"),
)

/** Look up reviewed translations for shared static copy. Unknown text is left unchanged. */
@Composable
fun atlasText(french: String): String {
    val translated = commonTranslations[french] ?: return french
    return when (LocalAtlasLanguage.current) {
        AppLanguage.FRENCH -> french
        AppLanguage.ENGLISH -> translated.first
        AppLanguage.ARABIC -> translated.second
    }
}

@Composable
fun atlasRoleLabel(role: String?): String = when (Roles.normalize(role)) {
    Roles.OWNER -> atlasText("Compte principal", "Primary account", "الحساب الرئيسي")
    Roles.ADMIN -> atlasText("Administrateur", "Administrator", "مدير")
    Roles.SECRETARY -> atlasText("Secrétariat", "Sales secretary", "السكرتارية")
    Roles.STOCK -> atlasText("Stock", "Stock", "المخزون")
    else -> atlasText("Opérateur", "Operator", "مشغّل")
}

/** Select the localized copy for a language-aware, high-priority piece of UI. */
@Composable
fun atlasText(french: String, english: String, arabic: String): String =
    when (LocalAtlasLanguage.current) {
        AppLanguage.FRENCH -> french
        AppLanguage.ENGLISH -> english
        AppLanguage.ARABIC -> arabic
    }

/** Translate known ViewModel and platform notices while preserving their runtime values. */
fun atlasNotice(message: String, language: AppLanguage): String {
    fun choose(french: String, english: String, arabic: String): String = when (language) {
        AppLanguage.FRENCH -> french
        AppLanguage.ENGLISH -> english
        AppLanguage.ARABIC -> arabic
    }
    fun template(pattern: Regex, render: (MatchResult) -> Triple<String, String, String>): String? {
        val match = pattern.matchEntire(message) ?: return null
        val (french, english, arabic) = render(match)
        return choose(french, english, arabic)
    }
    commonTranslations[message]?.let { (english, arabic) ->
        return choose(message, english, arabic)
    }
    template(Regex("^Seules les (\\d+) premières images ont été retenues \\(max 3\\)\\.$")) { m ->
        Triple(m.value, "Only the first ${m.groupValues[1]} images were kept (maximum 3).", "تم الاحتفاظ بأول ${m.groupValues[1]} صور فقط (الحد الأقصى 3).")
    }?.let { return it }
    template(Regex("^Ticket GitHub créé et ouvert( · .+)?\\.$")) { m ->
        Triple(m.value, "GitHub issue created and opened${m.groupValues[1]}.", "تم إنشاء بلاغ GitHub وفتحه${m.groupValues[1] ?: ""}.")
    }?.let { return it }
    template(Regex("^Ticket créé( · .+)?, mais aucun navigateur n'est disponible\\.$")) { m ->
        Triple(m.value, "Issue created${m.groupValues[1]}, but no browser is available.", "تم إنشاء البلاغ${m.groupValues[1] ?: ""}، لكن لا يتوفر متصفح.")
    }?.let { return it }
    template(Regex("^Rapport reçu par Atlas( · .+)?\\. Le formulaire GitHub prérempli est ouvert; envoyez-le pour créer l'issue\\.$")) { m ->
        Triple(m.value, "Atlas received the report${m.groupValues[1]}. The prefilled GitHub form is open; submit it to create the issue.", "استلم Atlas البلاغ${m.groupValues[1] ?: ""}. نموذج GitHub المعبأ مسبقاً مفتوح؛ أرسله لإنشاء البلاغ.")
    }?.let { return it }
    template(Regex("^Rapport reçu · (.+)\\. Impossible d'ouvrir le suivi sur cet appareil\\.$")) { m ->
        Triple(m.value, "Report received · ${m.groupValues[1]}. Could not open tracking on this device.", "تم استلام البلاغ · ${m.groupValues[1]}. تعذّر فتح صفحة المتابعة على هذا الجهاز.")
    }?.let { return it }
    template(Regex("^Rapport reçu par Atlas\\. Impossible d'ouvrir le suivi sur cet appareil\\.$")) { m ->
        Triple(m.value, "Atlas received the report. Could not open tracking on this device.", "استلم Atlas البلاغ. تعذّر فتح صفحة المتابعة على هذا الجهاز.")
    }?.let { return it }
    return template(Regex("^(.+) · ajouté automatiquement au panier$")) { m ->
        Triple(m.value, "${m.groupValues[1]} · added to cart automatically", "${m.groupValues[1]} · أُضيف تلقائياً إلى السلة")
    } ?: template(Regex("^(.+) · ajouté au panier$")) { m ->
        Triple(m.value, "${m.groupValues[1]} · added to cart", "${m.groupValues[1]} · أُضيف إلى السلة")
    } ?: template(Regex("^Produit « (.+) » créé au catalogue\\.$")) { m ->
        Triple(m.value, "Product “${m.groupValues[1]}” created in the catalog.", "تم إنشاء المنتج «${m.groupValues[1]}» في الكتالوج.")
    } ?: template(Regex("^Produit « (.+) » créé et demande validée\\.$")) { m ->
        Triple(m.value, "Product “${m.groupValues[1]}” created and request approved.", "تم إنشاء المنتج «${m.groupValues[1]}» والموافقة على الطلب.")
    } ?: template(Regex("^Ticket (.+) enregistré\\.$")) { m ->
        Triple(m.value, "Receipt ${m.groupValues[1]} saved.", "تم حفظ الإيصال ${m.groupValues[1]}.")
    } ?: template(Regex("^Notification envoyée à : (.+)\\.$")) { m ->
        Triple(m.value, "Notification sent to: ${m.groupValues[1]}.", "تم إرسال الإشعار إلى: ${m.groupValues[1]}.")
    } ?: template(Regex("^Notification « (.+) » marquée comme traitée\\.$")) { m ->
        Triple(m.value, "Notification “${m.groupValues[1]}” marked as handled.", "تم وضع علامة المعالجة على الإشعار «${m.groupValues[1]}».")
    } ?: template(Regex("^Demande « (.+) » validée\\.$")) { m ->
        Triple(m.value, "Request “${m.groupValues[1]}” approved.", "تمت الموافقة على الطلب «${m.groupValues[1]}».")
    } ?: template(Regex("^Demande « (.+) » refusée\\.$")) { m ->
        Triple(m.value, "Request “${m.groupValues[1]}” rejected.", "تم رفض الطلب «${m.groupValues[1]}».")
    } ?: template(Regex("^(.+) · stock (.+)$")) { m ->
        Triple(m.value, "${m.groupValues[1]} · stock ${m.groupValues[2]}", "${m.groupValues[1]} · المخزون ${m.groupValues[2]}")
    } ?: template(Regex("^Connecté en tant que (.+)\\.$")) { m ->
        Triple(m.value, "Signed in as ${m.groupValues[1]}.", "تم تسجيل الدخول باسم ${m.groupValues[1]}.")
    } ?: template(Regex("^Compte @(.+) oublié\\.$")) { m ->
        Triple(m.value, "Account @${m.groupValues[1]} forgotten.", "تم نسيان الحساب @${m.groupValues[1]}.")
    } ?: template(Regex("^Serveur sélectionné : (.+)$")) { m ->
        Triple(m.value, "Selected server: ${m.groupValues[1]}", "الخادم المحدد: ${m.groupValues[1]}")
    } ?: template(Regex("^(.+) révoqué\\.$")) { m ->
        Triple(m.value, "${m.groupValues[1]} revoked.", "تم إلغاء صلاحية ${m.groupValues[1]}.")
    } ?: template(Regex("^(\\d+) rapport\\(s\\) PDF temporaire\\(s\\) supprimé\\(s\\)\\.$")) { m ->
        Triple(m.value, "${m.groupValues[1]} temporary PDF report(s) deleted.", "تم حذف ${m.groupValues[1]} من تقارير PDF المؤقتة.")
    } ?: template(Regex("^Données locales supprimées\\.(.*?) Les données de l'entreprise sur le serveur n'ont pas été modifiées\\.$")) { m ->
        val updateNote = when (m.groupValues[1].trim()) {
            "Le téléchargement de mise à jour actif est conservé jusqu'à sa fin." -> choose(
                " Le téléchargement de mise à jour actif est conservé jusqu'à sa fin.",
                " The active update download will be kept until it finishes.",
                " سيظل تنزيل التحديث الجاري محفوظاً حتى يكتمل.",
            )
            else -> m.groupValues[1]
        }
        Triple(m.value, "Local data deleted.$updateNote Company data on the server was not changed.", "تم حذف البيانات المحلية.$updateNote لم يتم تغيير بيانات الشركة على الخادم.")
    } ?: message
}

@Composable
fun AtlasLanguageSelector(
    modifier: Modifier = Modifier,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    val language = LocalAtlasLanguage.current
    val setLanguage = LocalSetAtlasLanguage.current
    var expanded by remember { mutableStateOf(false) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = atlasText("Langue", "Language", "اللغة"),
                tint = contentColor,
            )
            Spacer(Modifier.width(4.dp))
            Text(language.tag.uppercase(), color = contentColor)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLanguage.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.nativeLabel) },
                    onClick = {
                        expanded = false
                        setLanguage(option)
                    },
                    leadingIcon = if (option == language) {
                        { Text("✓", color = MaterialTheme.colorScheme.primary) }
                    } else null,
                )
            }
        }
    }
}
