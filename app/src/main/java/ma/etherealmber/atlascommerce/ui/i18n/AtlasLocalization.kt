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
import ma.etherealmber.atlascommerce.core.format.AtlasFormat
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.Roles

val LocalAtlasLanguage = staticCompositionLocalOf { AppLanguage.FRENCH }
val LocalSetAtlasLanguage = staticCompositionLocalOf<(AppLanguage) -> Unit> { {} }

/** Reviewed French, English, and Arabic UI copy; unknown phrases intentionally remain unchanged. */
private val commonTranslations = mapOf(
    "La fenêtre Android n'est plus disponible. Rouvrez l'application puis réessayez." to ("The Android window is no longer available. Reopen the app and try again." to "نافذة Android لم تعد متاحة. أعد فتح التطبيق وحاول مجدداً."),
    "La création de la passkey n'a pas été terminée à temps. Réessayez ou vérifiez votre fournisseur Android." to ("Passkey creation did not finish in time. Try again or check your Android provider." to "لم يكتمل إنشاء مفتاح المرور في الوقت المحدد. حاول مجدداً أو تحقق من مزوّد Android."),
    "La création de la passkey a été annulée." to ("Passkey creation was cancelled." to "تم إلغاء إنشاء مفتاح المرور."),
    "Le serveur a changé. Recommencez la connexion par passkey." to ("The server changed. Start passkey sign-in again." to "تغيّر الخادم. أعد تسجيل الدخول بمفتاح المرور."),
    "La session ou le serveur a changé. Recommencez l'enregistrement de la passkey." to ("The session or server changed. Start passkey registration again." to "تغيّرت الجلسة أو الخادم. أعد تسجيل مفتاح المرور."),
    "Le message est trop long. Limitez-le à 1 800 caractères." to ("The message is too long. Limit it to 1,800 characters." to "الرسالة طويلة جداً. الحد الأقصى ١٨٠٠ حرف."),
    "Votre session a expiré. Reconnectez-vous puis réessayez." to ("Your session expired. Sign in and try again." to "انتهت جلستك. سجّل الدخول ثم حاول مجدداً."),
    "Le serveur refuse cette opération. Vérifiez vos autorisations." to ("The server denied this operation. Check your permissions." to "رفض الخادم هذه العملية. تحقّق من صلاحياتك."),
    "Le serveur a signalé un conflit. Réessayez ou contactez l'administrateur." to ("The server reported a conflict. Try again or contact your administrator." to "أبلغ الخادم عن تعارض. حاول مجدداً أو اتصل بالمسؤول."),
    "Le serveur n'a pas pu vérifier la passkey. Réessayez ou contactez l'administrateur." to ("The server could not verify the passkey. Try again or contact your administrator." to "تعذّر على الخادم التحقق من مفتاح المرور. حاول مجدداً أو اتصل بالمسؤول."),
    "BIENVENUE" to ("WELCOME" to "مرحباً"),
    "GOUVERNANCE & RÔLES" to ("GOVERNANCE & ROLES" to "الحوكمة والأدوار"),
    "SCANNER DE PRÉCISION" to ("PRECISION SCANNER" to "الماسح الدقيق"),
    "INFO BOX & AGENT IA" to ("INFO BOX & AI AGENT" to "صندوق المعلومات والمساعد الذكي"),
    "Bienvenue sur Atlas Commerce" to ("Welcome to Atlas Commerce" to "مرحباً بك في Atlas Commerce"),
    "Pilotez vos ventes, votre catalogue, vos stocks et votre trésorerie mobile en synchronisation directe avec votre serveur Atlas." to ("Manage sales, catalog, inventory, and cash flow on mobile with direct synchronization to your Atlas server." to "أدر مبيعاتك وكتالوجك ومخزونك وخزينتك من الهاتف مع المزامنة المباشرة مع خادم Atlas."),
    "Synchronisation temps réel & cache hors ligne automatique" to ("Real-time synchronization and automatic offline cache" to "مزامنة فورية وتخزين تلقائي للاستخدام دون اتصال"),
    "Interface fluide, rapide et pensée pour le terrain" to ("A fast, smooth interface designed for use on the go" to "واجهة سريعة وسلسة مصممة للعمل الميداني"),
    "Connexion rapide par identifiants ou QR code PC" to ("Quick sign-in with credentials or a PC QR code" to "تسجيل دخول سريع بالبيانات أو رمز QR للكمبيوتر"),
    "Permissions intelligentes par rôle" to ("Role-based permissions" to "صلاحيات حسب الدور"),
    "Chaque collaborateur dispose exactement des outils adaptés à sa mission : Administrateur, Secrétaire, Magasinier ou Opérateur." to ("Each team member has tools suited to their role: administrator, secretary, stockkeeper, or operator." to "لكل عضو أدوات تناسب دوره: المسؤول أو السكرتير أو أمين المخزن أو المشغّل."),
    "Seul un Administrateur crée un produit directement au stock" to ("Only an administrator can create a product directly in stock" to "يمكن للمسؤول فقط إنشاء منتج مباشرة في المخزون"),
    "Tout autre rôle envoie automatiquement la fiche à l'Admin" to ("Other roles send the product details to the administrator" to "ترسل الأدوار الأخرى بيانات المنتج إلى المسؤول"),
    "Validation & création en 1 geste depuis l'espace Admin" to ("Approve and create in one step from the administrator workspace" to "الموافقة والإنشاء بخطوة واحدة من مساحة المسؤول"),
    "Scan maîtrisé & annulation instantanée" to ("Controlled scanning and instant cancellation" to "مسح متحكم فيه وإلغاء فوري"),
    "La caméra ne s'active qu'à votre demande explicite et se coupe dès que vous quittez l'écran." to ("The camera starts only when you request it and stops when you leave the screen." to "لا تعمل الكاميرا إلا بطلب منك وتتوقف عند مغادرة الشاشة."),
    "Recherche instantanée EAN / QR et ajustement rapide" to ("Instant EAN / QR lookup and quick adjustment" to "بحث فوري عبر EAN / QR وتعديل سريع"),
    "Bouton « Annuler » immédiat si vous ne souhaitez plus enregistrer l'objet" to ("Immediate Cancel button if you no longer want to save the item" to "زر إلغاء فوري إذا لم تعد ترغب في حفظ العنصر"),
    "Retour haptique et sonore paramétrable" to ("Configurable haptic and sound feedback" to "اهتزاز وصوت قابلان للضبط"),
    "Notifications ciblées & copilote IA" to ("Targeted notifications and AI assistant" to "إشعارات موجهة ومساعد ذكي"),
    "Recevez et diffusez des alertes filtrées selon chaque rôle métier, et interrogez l'Agent IA avec tableaux Markdown." to ("Receive and send role-filtered alerts, and ask the AI assistant with Markdown tables." to "استقبل وأرسل تنبيهات مصفاة حسب الدور، واسأل المساعد الذكي مع جداول Markdown."),
    "Info Box filtrée par rôle + envoi direct entre équipes" to ("Role-filtered Info Box and direct messages between teams" to "صندوق معلومات مصفى حسب الدور ورسائل مباشرة بين الفرق"),
    "Notifications système Android pour les demandes urgentes" to ("Android system notifications for urgent requests" to "إشعارات نظام Android للطلبات العاجلة"),
    "Agent IA commercial avec tableaux et analyses structurées" to ("Business AI assistant with structured tables and analysis" to "مساعد أعمال ذكي بجداول وتحليلات منظمة"),
    "Émulateur" to ("Emulator" to "المحاكي"),
    "Worker local vu depuis l'émulateur (npm run dev)" to ("Local Worker from the emulator (npm run dev)" to "Worker المحلي من المحاكي (npm run dev)"),
    "Adresse invalide — vérifiez attentivement que ce que vous avez tapé est correct (ex. https://votre-domaine.com)." to ("Invalid address. Check what you entered (for example, https://your-domain.com)." to "العنوان غير صالح. تحقّق مما أدخلته (مثال: https://your-domain.com)."),
    "HTTP n'est pas autorisé dans cette version. Vérifiez que l'adresse commence bien par https://." to ("HTTP is not allowed in this version. Make sure the address starts with https://." to "HTTP غير مسموح في هذا الإصدار. تأكد من أن العنوان يبدأ بـ https://."),
    "Aucune API Atlas trouvée à cette adresse (404). Vérifiez que l'URL saisie est exacte et qu'Atlas Commerce Web y est bien déployé." to ("No Atlas API was found at this address (404). Check the URL and the Atlas Commerce Web deployment." to "لم يُعثر على واجهة Atlas في هذا العنوان (404). تحقّق من الرابط ونشر Atlas Commerce Web."),
    "Serveur joignable mais temporairement limité (429). Patientez quelques instants puis réessayez." to ("The server is reachable but temporarily rate-limited (429). Wait a moment and try again." to "الخادم متاح لكن الطلبات محدودة مؤقتاً (429). انتظر قليلاً وحاول مجدداً."),
    "Ce site ne répond pas comme un espace Atlas Commerce. Vérifiez l'orthographe exacte de l'adresse saisie et assurez-vous d'avoir installé Atlas Commerce Web d'abord." to ("This site did not respond as an Atlas Commerce server. Check the address and deployment." to "لم يستجب الموقع كخادم Atlas Commerce. تحقّق من العنوان والنشر."),
    "Impossible de résoudre le nom du serveur. Vérifiez l’adresse et votre connexion, puis réessayez." to ("Could not resolve the server name. Check the address and your connection, then try again." to "تعذّر تحديد عنوان الخادم. تحقّق من العنوان واتصالك، ثم حاول مجدداً."),
    "Délai d'attente dépassé — vérifiez que l'adresse saisie est correcte et que votre site Atlas Commerce est bien en ligne." to ("Connection timed out. Check the address and your connection, then try again." to "انتهت مهلة الاتصال. تحقّق من العنوان واتصالك، ثم حاول مجدداً."),
    "Certificat HTTPS invalide — vérifiez l'exactitude du nom de domaine saisi." to ("Invalid HTTPS certificate. Check the domain name." to "شهادة HTTPS غير صالحة. تحقّق من اسم النطاق."),
    "Connexion au serveur impossible. Vérifiez l’adresse et votre connexion, puis réessayez." to ("Could not connect to the server. Check the address and your connection, then try again." to "تعذّر الاتصال بالخادم. تحقّق من العنوان واتصالك، ثم حاول مجدداً."),
    "Vérification impossible — vérifiez votre saisie." to ("Verification failed. Check the address you entered." to "تعذّر التحقق. راجع العنوان الذي أدخلته."),
    "Impossible de joindre le serveur pour vérifier les mises à jour." to ("Could not reach the server to check for updates." to "تعذّر الاتصال بالخادم للتحقق من التحديثات."),
    "Le panier est vide." to ("The cart is empty." to "السلة فارغة."),
    "Indiquez le nom du client pour le ticket." to ("Enter the customer name for the receipt." to "أدخل اسم العميل للإيصال."),
    "Le serveur configuré n'utilise pas HTTPS." to ("The configured server does not use HTTPS." to "الخادم المحدد لا يستخدم HTTPS."),
    "Réponse de mise à jour invalide." to ("Invalid update response." to "استجابة التحديث غير صالحة."),
    "Le fichier d'installation n'est plus en cache. Relancez le téléchargement." to ("The installer is no longer cached. Download it again." to "لم يعد ملف التثبيت محفوظاً مؤقتاً. أعد تنزيله."),
    "Impossible d'ouvrir l'installateur Android." to ("Could not open the Android installer." to "تعذّر فتح مثبّت Android."),
    "Atlas Android · Mobile" to ("Atlas Android · Mobile" to "Atlas Android · هاتف"),
    "Appareil Android" to ("Android device" to "جهاز Android"),
    "iPhone / iPad" to ("iPhone / iPad" to "آيفون / آيباد"),
    "PC Windows · Edge" to ("Windows PC · Edge" to "حاسوب Windows · Edge"),
    "PC Windows · Chrome" to ("Windows PC · Chrome" to "حاسوب Windows · Chrome"),
    "PC Windows · Firefox" to ("Windows PC · Firefox" to "حاسوب Windows · Firefox"),
    "Ordinateur Windows" to ("Windows computer" to "حاسوب Windows"),
    "Mac · Navigateur" to ("Mac · Browser" to "Mac · متصفح"),
    "Poste Linux" to ("Linux computer" to "حاسوب Linux"),
    "Session Atlas" to ("Atlas session" to "جلسة Atlas"),
    "Le gestionnaire Android n'a pas répondu. Vérifiez votre fournisseur de passkeys puis réessayez." to ("Android's credential manager did not respond. Check your passkey provider and try again." to "لم يستجب مدير بيانات الاعتماد في Android. تحقّق من مزوّد مفاتيح المرور وحاول مجدداً."),
    "Le serveur n'a pas pu enregistrer la passkey. Réessayez plus tard." to ("The server could not register the passkey. Try again later." to "تعذّر على الخادم تسجيل مفتاح المرور. حاول مجدداً لاحقاً."),
    "Les passkeys Android ne sont pas activées pour le serveur sélectionné." to ("Android passkeys are not enabled on the selected server." to "مفاتيح مرور Android غير مفعّلة على الخادم المحدد."),
    "Les passkeys Android ne sont plus activées pour le serveur sélectionné." to ("Android passkeys are no longer enabled on the selected server." to "لم تعد مفاتيح مرور Android مفعّلة على الخادم المحدد."),
    "Les passkeys Android ne sont plus disponibles sur ce serveur." to ("Android passkeys are no longer available on this server." to "لم تعد مفاتيح مرور Android متاحة على هذا الخادم."),
    "Impossible de vérifier la disponibilité des passkeys. Réessayez plus tard." to ("Could not check passkey availability. Try again later." to "تعذّر التحقق من توفر مفاتيح المرور. حاول مجدداً لاحقاً."),
    "Impossible de préparer la création de la passkey. Réessayez plus tard." to ("Could not prepare passkey creation. Try again later." to "تعذّر التحضير لإنشاء مفتاح المرور. حاول مجدداً لاحقاً."),
    "La création de la passkey a échoué. Réessayez plus tard ou contactez l'administrateur." to ("Passkey creation failed. Try again later or contact your administrator." to "فشل إنشاء مفتاح المرور. حاول مجدداً لاحقاً أو اتصل بالمسؤول."),
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
    "Aucun fournisseur de passkeys n'a accepté la création. Activez Google Password Manager ou un fournisseur compatible, sélectionnez-le dans Paramètres Android > Mots de passe, clés d'accès et comptes, et vérifiez qu'un verrouillage d'écran est activé. Aucune passkey n'a été créée." to ("No passkey provider accepted the creation request. Enable Google Password Manager or another compatible provider, select it in Android Settings > Passwords, passkeys & accounts, and make sure a screen lock is enabled. No passkey was created." to "لم يقبل أي موفّر لمفاتيح المرور طلب الإنشاء. فعّل Google Password Manager أو موفّراً متوافقاً آخر، وحدّده في إعدادات Android > كلمات المرور ومفاتيح المرور والحسابات، وتأكد من تفعيل قفل الشاشة. لم يتم إنشاء أي مفتاح مرور."),
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
    "Système" to ("System" to "النظام"),
    "Clair" to ("Light" to "فاتح"),
    "Sombre" to ("Dark" to "داكن"),
    "Compact" to ("Compact" to "مضغوط"),
    "Confort" to ("Comfort" to "مريح"),
    "Animations fluides" to ("Smooth animations" to "حركات سلسة"),
    "Transitions entre les pages, apparitions d'accueil et effets de mouvement." to ("Page transitions, home-screen entrances, and motion effects." to "انتقالات الصفحات وحركات ظهور الشاشة الرئيسية وتأثيرات الحركة."),
    "Notifications système par rôle" to ("Role-based system notifications" to "إشعارات النظام حسب الدور"),
    "Ouvrir la caméra automatiquement" to ("Open camera automatically" to "فتح الكاميرا تلقائيًا"),
    "Désactivé par défaut — la caméra reste fermée en entrant dans Scan ou QR PC." to ("Off by default — the camera stays closed when opening Scan or PC QR." to "معطّلة افتراضيًا — تظل الكاميرا مغلقة عند فتح المسح أو QR الكمبيوتر."),
    "Vibration au scan" to ("Vibrate on scan" to "الاهتزاز عند المسح"),
    "Retour haptique dès qu'un code-barres ou QR est détecté." to ("Vibrate when a barcode or QR code is detected." to "اهتزاز عند اكتشاف باركود أو رمز QR."),
    "Signal sonore au scan" to ("Scan sound" to "صوت المسح"),
    "Bip court de confirmation lors de la lecture d'un code." to ("Play a short confirmation beep when a code is read." to "إصدار نغمة تأكيد قصيرة عند قراءة الرمز."),
    "Ajout automatique au panier" to ("Automatically add to cart" to "الإضافة التلقائية إلى السلة"),
    "Ajoute immédiatement 1 unité au ticket VR lorsqu'un article est scanné." to ("Immediately add one unit to the VR receipt when an item is scanned." to "إضافة وحدة واحدة فورًا إلى إيصال VR عند مسح المنتج."),
    "Confirmer avant création du ticket" to ("Confirm before creating receipt" to "التأكيد قبل إنشاء الإيصال"),
    "Affiche un récapitulatif du montant avant d'envoyer la vente au serveur." to ("Show an amount summary before sending the sale to the server." to "عرض ملخص للمبلغ قبل إرسال البيع إلى الخادم."),
    "Téléchargement accéléré multi-connexions" to ("Faster multi-connection downloads" to "تنزيل أسرع عبر اتصالات متعددة"),
    "Télécharge les mises à jour via 4 connexions HTTP Range parallèles haute vitesse." to ("Downloads updates over four high-speed parallel HTTP Range connections." to "تنزيل التحديثات عبر أربعة اتصالات HTTP Range متوازية وعالية السرعة."),
    "Toujours suivre la dernière version publiée" to ("Always follow the latest published release" to "اتباع أحدث إصدار منشور دائمًا"),
    "Considère toute version publiée en dernier sur GitHub Releases ou CDN 1 comme la version active." to ("Treat the newest version on GitHub Releases or CDN 1 as the active release." to "اعتبار أحدث إصدار على GitHub Releases أو CDN 1 هو الإصدار النشط."),
    "Recherche automatique des mises à jour" to ("Automatically check for updates" to "البحث تلقائيًا عن التحديثات"),
    "Vérifie à l'ouverture et environ toutes les 12 h en arrière-plan (selon les règles batterie d'Android)." to ("Checks on launch and about every 12 hours in the background, subject to Android battery rules." to "التحقق عند التشغيل وحوالي كل 12 ساعة في الخلفية، وفقًا لقواعد البطارية في Android."),
    "Envoyer automatiquement les rapports de plantage" to ("Automatically send crash reports" to "إرسال تقارير الأعطال تلقائيًا"),
    "Envoi opt-in à Firebase Crashlytics, sans jetons, mots de passe ni données commerciales jointes." to ("Opt-in upload to Firebase Crashlytics; tokens, passwords, and business data are excluded." to "إرسال اختياري إلى Firebase Crashlytics؛ لا تُرفق الرموز أو كلمات المرور أو بيانات العمل."),
    "Rapports automatiques de plantage" to ("Automatic crash reports" to "تقارير الأعطال التلقائية"),
    "Indisponible : cette version n'est pas configurée avec Firebase Crashlytics." to ("Unavailable: this build is not configured with Firebase Crashlytics." to "غير متاح: لم يتم إعداد هذا الإصدار باستخدام Firebase Crashlytics."),
    "Revoir l'écran de bienvenue" to ("Replay the welcome screen" to "إعادة عرض شاشة الترحيب"),
    "Relancer la présentation animée d'Atlas Commerce" to ("Replay the Atlas Commerce animated introduction." to "إعادة تشغيل المقدمة المتحركة لـ Atlas Commerce."),
    "Supprimer les PDF temporaires" to ("Delete temporary PDFs" to "حذف ملفات PDF المؤقتة"),
    "Les rapports privés sont purgés automatiquement après 7 jours." to ("Private reports are automatically deleted after 7 days." to "تُحذف التقارير الخاصة تلقائيًا بعد 7 أيام."),
    "Effacer toutes les données locales" to ("Clear all local data" to "مسح جميع البيانات المحلية"),
    "Déconnecte le compte et efface les données stockées sur cet appareil uniquement." to ("Signs out and clears data stored on this device only." to "تسجيل الخروج ومسح البيانات المخزنة على هذا الجهاز فقط."),
    "Outils développeur" to ("Developer tools" to "أدوات المطوّر"),
    "État passkey, journaux assainis, copie et diagnostic avancé" to ("Passkey status, sanitized logs, copying, and advanced diagnostics." to "حالة مفتاح المرور والسجلات المنقّحة والنسخ والتشخيص المتقدم."),
    "Sources et composants tiers" to ("Source code and third-party components" to "المصدر البرمجي والمكونات الخارجية"),
    "Signaler un problème" to ("Report a problem" to "الإبلاغ عن مشكلة"),
    "Capturer les journaux, ID appareil & jusqu'à 3 images (≤ 1 Mo) avec @atlas-bot" to ("Include logs, device ID, and up to 3 images (≤ 1 MB) for @atlas-bot." to "إرفاق السجلات ومعرّف الجهاز وما يصل إلى 3 صور (≤ 1 ميغابايت) لـ @atlas-bot."),
    "Langue de l'application" to ("App language" to "لغة التطبيق"),
    "Signaler un problème en secouant" to ("Shake to report a problem" to "هز الهاتف للإبلاغ عن مشكلة"),
    "Affiche une confirmation uniquement. Aucun rapport ni diagnostic n'est envoyé sans votre accord." to ("Only opens a confirmation. No report or diagnostics are sent without your approval." to "يعرض طلب تأكيد فقط. لا يُرسل أي تقرير أو بيانات تشخيصية دون موافقتك."),
    "Appareils connectés & QR PC" to ("Connected devices & PC QR" to "الأجهزة المتصلة وQR الكمبيوتر"),
    "Autorisez un ordinateur par QR ou révoquez une session à distance." to ("Approve a computer using a QR code or revoke a session remotely." to "اعتماد كمبيوتر باستخدام رمز QR أو إلغاء جلسة عن بُعد."),
    "Scanner un poste PC (QR)" to ("Scan a PC sign-in QR" to "مسح رمز تسجيل الدخول للكمبيوتر"),
    "Actualiser les appareils" to ("Refresh devices" to "تحديث الأجهزة"),
    "Paramètres" to ("Settings" to "الإعدادات"),
    "Projet web Atlas Commerce" to ("Atlas Commerce web project" to "مشروع Atlas Commerce على الويب"),
    "Envoi via Atlas Bot · le statut GitHub est confirmé après l'envoi." to ("Sent via Atlas Bot · GitHub status is confirmed after submission." to "يُرسل عبر Atlas Bot · يتم تأكيد حالة GitHub بعد الإرسال."),
    "Non envoyées avec le rapport · 3 max · compression auto ≤ 1 Mo chacune" to ("Not sent with the report · max 3 · auto-compressed to ≤ 1 MB each" to "لا تُرسل مع التقرير · 3 كحد أقصى · ضغط تلقائي إلى ≤ 1 ميغابايت لكل صورة"),
    "Diagnostics protégés" to ("Protected diagnostics" to "بيانات التشخيص محمية"),
    "Aucun journal brut n'est affiché. Un résumé expurgé sera joint uniquement si vous envoyez ce rapport." to ("Raw logs are never displayed. A redacted summary is attached only if you submit this report." to "لا تُعرض السجلات الخام. لا يُرفق ملخص منقّح إلا عند إرسال هذا التقرير."),
    "Envoi impossible. Vérifiez votre connexion et réessayez." to ("Could not submit. Check your connection and try again." to "تعذّر الإرسال. تحقق من الاتصال وحاول مجدداً."),
    "Envoyer le rapport à Atlas" to ("Send report to Atlas" to "إرسال التقرير إلى Atlas"),
    "Bonjour" to ("Good morning" to "صباح الخير"),
    "Bonsoir" to ("Good evening" to "مساء الخير"),
    "Changer la photo de profil" to ("Change profile photo" to "تغيير صورة الملف الشخصي"),
    "Modifier le profil" to ("Edit profile" to "تعديل الملف الشخصي"),
    "E-mail" to ("Email" to "البريد الإلكتروني"),
    "Se déconnecter de cette session" to ("Sign out of this session" to "تسجيل الخروج من هذه الجلسة"),
    "Autres comptes mémorisés sur ce serveur" to ("Other accounts saved on this server" to "حسابات أخرى محفوظة على هذا الخادم"),
    "Bascule instantanée — les paniers, caches et discussions restent strictement séparés." to ("Instant switching — carts, caches, and conversations stay strictly separate." to "تبديل فوري — تبقى السلال وذاكرات التخزين والمحادثات منفصلة تماماً."),
    "Langue" to ("Language" to "اللغة"),
    "La session active, le panier et les données temporaires seront effacés. Aucune donnée métier n'est supprimée sur le serveur." to ("The active session, cart, and temporary data will be cleared. No business data is deleted from the server." to "سيتم مسح الجلسة النشطة والسلة والبيانات المؤقتة. لن تُحذف أي بيانات عمل من الخادم."),
    "Crashlytics n'est pas configuré pour cette version." to ("Crashlytics is not configured for this build." to "لم يتم إعداد Crashlytics لهذا الإصدار."),
    "Rapports de plantage activés." to ("Crash reports enabled." to "تم تفعيل تقارير الأعطال."),
    "Envoi automatique désactivé." to ("Automatic submission disabled." to "تم تعطيل الإرسال التلقائي."),
    "Vérifie à l'ouverture et environ toutes les 12 h en arrière-plan, selon les règles batterie d'Android." to ("Checks on launch and about every 12 hours in the background, subject to Android battery rules." to "يتحقق عند التشغيل ونحو كل 12 ساعة في الخلفية، وفقاً لقواعد البطارية في Android."),
    "Rapports de plantage automatiques (Firebase)" to ("Automatic crash reports (Firebase)" to "تقارير الأعطال التلقائية (Firebase)"),
    "Option désactivée par défaut. Les rapports sont envoyés à Firebase Crashlytics, pas à Atlas Bot." to ("Off by default. Reports are sent to Firebase Crashlytics, not Atlas Bot." to "معطّل افتراضياً. تُرسل التقارير إلى Firebase Crashlytics وليس Atlas Bot."),
    "État local résumé · journaux masqués" to ("Local status summary · logs hidden" to "ملخص الحالة المحلية · السجلات مخفية"),
    "Configuration Android non confirmée par le serveur sélectionné. L'inscription reste masquée tant que le serveur ne l'annonce pas comme disponible." to ("Android configuration is not confirmed by the selected server. Registration stays hidden until the server reports it as available." to "لم يؤكد الخادم المحدد إعداد Android. يظل التسجيل مخفياً حتى يعلن الخادم توفره."),
    "Enregistrer une passkey" to ("Register a passkey" to "تسجيل مفتاح مرور"),
    "Un résumé technique privé sera joint uniquement si vous envoyez le rapport. Les captures (3 max) se partagent séparément et ne sont pas envoyées avec le formulaire." to ("A private technical summary is attached only if you submit the report. Up to 3 screenshots can be shared separately and are not sent with the form." to "لا يُرفق الملخص التقني الخاص إلا عند إرسال التقرير. يمكن مشاركة 3 لقطات شاشة كحد أقصى بشكل منفصل، ولا تُرسل مع النموذج."),
    "Compte principal" to ("Primary account" to "الحساب الرئيسي"),
    "Administrateur" to ("Administrator" to "المسؤول"),
    "Secrétariat" to ("Sales secretary" to "السكرتارية"),
    "Stock" to ("Stock" to "المخزون"),
    "Opérateur" to ("Operator" to "مشغّل"),
    "Caméra refusée — collez le lien ou code QR ci-dessous." to ("Camera permission denied — paste the link or QR code below." to "تم رفض إذن الكاميرا — الصق الرابط أو رمز QR أدناه."),
    "Scanner un poste PC" to ("Scan a PC" to "مسح رمز الكمبيوتر"),
    "Connexion mobile par QR" to ("Mobile sign-in by QR" to "تسجيل الدخول للهاتف عبر QR"),
    "APPAIRAGE SÉCURISÉ · HORS CATALOGUE STOCK" to ("SECURE PAIRING · OUTSIDE STOCK CATALOG" to "اقتران آمن · خارج كتالوج المخزون"),
    "Couper la caméra" to ("Turn camera off" to "إيقاف الكاميرا"),
    "Activer la caméra" to ("Turn camera on" to "تشغيل الكاميرا"),
    "Prêt à autoriser" to ("Ready to approve" to "جاهز للاعتماد"),
    "Appareil synchronisé avec succès." to ("Device synced successfully." to "تمت مزامنة الجهاز بنجاح."),
    "Lecteur QR en veille" to ("QR scanner is idle" to "ماسح QR في وضع الاستعداد"),
    "Appuyez ci-dessous pour démarrer la caméra QR, ou collez directement le lien ci-dessous." to ("Tap below to start the QR camera, or paste the link directly below." to "اضغط أدناه لتشغيل كاميرا QR، أو الصق الرابط مباشرةً أدناه."),
    "Démarrer la caméra QR" to ("Start QR camera" to "تشغيل كاميرا QR"),
    "Sur l'ordinateur, ouvrez l'écran de connexion Atlas Commerce puis cliquez sur « Connexion par QR »." to ("On the computer, open the Atlas Commerce sign-in screen and click “Sign in with QR”." to "على الكمبيوتر، افتح شاشة تسجيل الدخول إلى Atlas Commerce ثم اضغط «تسجيل الدخول عبر QR»."),
    "Sur un ordinateur déjà connecté, ouvrez Paramètres → Appareils → « Connecter un téléphone par QR »." to ("On a signed-in computer, open Settings → Devices → “Connect a phone by QR”." to "على الكمبيوتر المسجل دخوله، افتح الإعدادات ← الأجهزة ← «ربط هاتف عبر QR»."),
    "Lien ou jeton QR" to ("Link or QR token" to "الرابط أو رمز QR"),
    "Lier" to ("Link" to "ربط"),
    "Aucun autre appareil enregistré sur ce compte." to ("No other devices are registered to this account." to "لا توجد أجهزة أخرى مسجلة لهذا الحساب."),
    "Autoriser ce poste PC ?" to ("Approve this PC?" to "هل تريد اعتماد هذا الكمبيوتر؟"),
    "Un navigateur demande l'accès à votre compte Atlas. L'approbation est liée à cette session QR à usage unique." to ("A browser is requesting access to your Atlas account. Approval is bound to this one-time QR session." to "يطلب متصفح الوصول إلى حساب Atlas. يرتبط الاعتماد بجلسة QR هذه لمرة واحدة."),
    "Autoriser le PC" to ("Approve PC" to "اعتماد الكمبيوتر"),
    "En attente de confirmation sur l'ordinateur…" to ("Waiting for confirmation on the computer…" to "بانتظار التأكيد على الكمبيوتر…"),
    "Autre poste" to ("Other device" to "جهاز آخر"),
    "Terminé" to ("Done" to "تم"),
    "Révoquer cet appareil" to ("Revoke this device" to "إلغاء هذا الجهاز"),
    "Cet appareil" to ("This device" to "هذا الجهاز"),
    "Discussions synchronisées (Web & Mobile)" to ("Conversations synced (Web & Mobile)" to "محادثات متزامنة (الويب والهاتف)"),
    "Aucune discussion enregistrée sur ce compte pour le moment." to ("No conversations are saved to this account yet." to "لا توجد محادثات محفوظة لهذا الحساب حالياً."),
    "Discussion" to ("Conversation" to "محادثة"),
    "Active actuellement" to ("Currently active" to "نشطة حالياً"),
    "Synchronisée Web/R2" to ("Synced with Web/R2" to "متزامنة مع الويب/R2"),
    "Nouvelle discussion" to ("New conversation" to "محادثة جديدة"),
    "Vérifiez avant d'envoyer" to ("Review before sending" to "راجع قبل الإرسال"),
    "Envoyer quand même" to ("Send anyway" to "إرسال على أي حال"),
    "Atlas Agent IA" to ("Atlas AI Agent" to "مساعد Atlas الذكي"),
    "Stock, ventes et assistance" to ("Inventory, sales, and help" to "المخزون والمبيعات والمساعدة"),
    "Historique des discussions" to ("Conversation history" to "سجل المحادثات"),
    "Actualiser le contexte" to ("Refresh context" to "تحديث السياق"),
    "Quota atteint" to ("Quota reached" to "تم بلوغ الحد"),
    "En ligne" to ("Online" to "متصل"),
    "En pause" to ("Paused" to "متوقف مؤقتاً"),
    "Vérification…" to ("Checking…" to "جارٍ التحقق…"),
    "Chargement de la discussion…" to ("Loading conversation…" to "جارٍ تحميل المحادثة…"),
    "Actions suggérées par l'IA" to ("AI-suggested actions" to "إجراءات مقترحة من الذكاء الاصطناعي"),
    "Accueil" to ("Home" to "الرئيسية"),
    "Produits et stock" to ("Products and inventory" to "المنتجات والمخزون"),
    "Ventes" to ("Sales" to "المبيعات"),
    "Employés" to ("Employees" to "الموظفون"),
    "Scan" to ("Scan" to "مسح"),
    "Profil" to ("Profile" to "الملف الشخصي"),
    "Ouvrir une rubrique Atlas" to ("Open an Atlas section" to "فتح قسم في Atlas"),
    "Réponse IA indisponible" to ("AI response unavailable" to "رد الذكاء الاصطناعي غير متاح"),
    "Aller aux derniers messages" to ("Go to latest messages" to "الانتقال إلى أحدث الرسائل"),
    "Quota IA atteint — réinitialisation automatique…" to ("AI quota reached — resets automatically…" to "تم بلوغ حصة الذكاء الاصطناعي — يعاد ضبطها تلقائياً…"),
    "Message à Atlas Agent" to ("Message Atlas Agent" to "أرسل رسالة إلى مساعد Atlas"),
    "Écrivez votre question…" to ("Type your question…" to "اكتب سؤالك…"),
    "Atlas Agent IA — Quotas & Limites" to ("Atlas AI Agent — Quotas & Limits" to "مساعد Atlas الذكي — الحصص والحدود"),
    "Suspendu" to ("Suspended" to "معلّق"),
    "Admin · Illimité" to ("Admin · Unlimited" to "المسؤول · غير محدود"),
    "Limite atteinte" to ("Limit reached" to "تم بلوغ الحد"),
    "Actif" to ("Active" to "نشط"),
    "Actualiser le quota" to ("Refresh quota" to "تحديث الحصة"),
    "Chargement des compteurs de jetons depuis le serveur…" to ("Loading token counters from the server…" to "جارٍ تحميل عدادات الرموز من الخادم…"),
    "Accès Atlas Agent suspendu sur ce compte. Contactez un administrateur pour le rétablir." to ("Atlas Agent access is suspended for this account. Contact an administrator to restore it." to "تم تعليق الوصول إلى مساعد Atlas لهذا الحساب. تواصل مع مسؤول لإعادته."),
    "Quota quotidien" to ("Daily quota" to "الحصة اليومية"),
    "Quota hebdomadaire" to ("Weekly quota" to "الحصة الأسبوعية"),
    "jetons" to ("tokens" to "رمزاً"),
    "Vous" to ("You" to "أنت"),
    "Atlas Agent" to ("Atlas Agent" to "مساعد Atlas"),
    "Action proposée" to ("Suggested action" to "إجراء مقترح"),
    "Approuvée" to ("Approved" to "تم الاعتماد"),
    "Approuver et exécuter" to ("Approve and execute" to "اعتماد وتنفيذ"),
    "Réponse en cours…" to ("Replying…" to "جارٍ الرد…"),
    "Copier la réponse" to ("Copy response" to "نسخ الرد"),
    "Quels articles sont en rupture ou à réapprovisionner ?" to ("Which items are out of stock or need restocking?" to "ما المنتجات النافدة أو التي تحتاج إلى إعادة التوريد؟"),
    "Résumé des ventes et impayés du jour" to ("Today's sales and unpaid balance summary" to "ملخص مبيعات اليوم والمبالغ غير المدفوعة"),
    "Quelles demandes scan sont en attente ?" to ("Which scan requests are pending?" to "ما طلبات المسح المعلّقة؟"),
    "CDN 1 : test…" to ("CDN 1: testing…" to "CDN 1: جارٍ الاختبار…"),
    "CDN 1 : indisponible" to ("CDN 1: unavailable" to "CDN 1: غير متاح"),
    "CDN 1 : prêt" to ("CDN 1: ready" to "CDN 1: جاهز"),
    "GitHub : test…" to ("GitHub: testing…" to "GitHub: جارٍ الاختبار…"),
    "GitHub : indisponible" to ("GitHub: unavailable" to "GitHub: غير متاح"),
    "GitHub : prêt" to ("GitHub: ready" to "GitHub: جاهز"),
    "Employés concernés" to ("Employees" to "الموظفون"),
    "Montant dû" to ("Amount due" to "المبلغ المستحق"),
    "À payer" to ("To pay" to "مستحق الدفع"),
    "Période de paie sélectionnée" to ("Selected payroll period" to "فترة الرواتب المحددة"),
    "Aucun versement pour cette période" to ("No payments for this period" to "لا توجد دفعات لهذه الفترة"),
    "Modifiez la recherche ou le filtre de paiement." to ("Change the search or payment filter." to "غيّر البحث أو عامل تصفية الدفعات."),
    "À recevoir" to ("Receivable" to "مستحق القبض"),
    "Entrées et sorties" to ("Income and expenses" to "الإيرادات والمصروفات"),
    "Sorties" to ("Expenses" to "المصروفات"),
    "Dépenses" to ("Expenses" to "المصروفات"),
    "Ouverture" to ("Opening balance" to "الرصيد الافتتاحي"),
    "Sorties payées" to ("Paid expenses" to "المصروفات المدفوعة"),
    "Paie encore due" to ("Payroll still due" to "رواتب لا تزال مستحقة"),
    "Préparation…" to ("Preparing…" to "جارٍ التحضير…"),
    "Télécharger le PDF" to ("Download PDF" to "تنزيل PDF"),
    "Pack de rapports personnalisable" to ("Customizable report bundle" to "حزمة تقارير قابلة للتخصيص"),
    "Ouvrir dans Atlas" to ("Open in Atlas" to "فتح في Atlas"),
    "Autre anomalie" to ("Other issue" to "مشكلة أخرى"),
    "Affichage / Interface" to ("Display / Interface" to "العرض / الواجهة"),
    "Agent IA / Tableaux" to ("AI Agent / Dashboards" to "مساعد الذكاء الاصطناعي / لوحات المعلومات"),
    "Mise à jour / Réseau" to ("Update / Network" to "التحديث / الشبكة"),
    "Scan / Caméra" to ("Scan / Camera" to "المسح / الكاميرا"),
    "Demande de fonctionnalité" to ("Feature request" to "طلب ميزة"),
    "Produits" to ("Products" to "المنتجات"),
    "Balance" to ("Balance" to "الرصيد"),
    "Mon profil" to ("My profile" to "ملفي الشخصي"),
    "Nouvelle discussion ouverte." to ("New conversation started." to "بدأت محادثة جديدة."),
    "Atlas Agent réfléchit…" to ("Atlas Agent is thinking…" to "مساعد Atlas يفكر…"),
    "Quota quotidien · Illimité" to ("Daily quota · Unlimited" to "الحصة اليومية · غير محدودة"),
    "Quota hebdomadaire · Illimité" to ("Weekly quota · Unlimited" to "الحصة الأسبوعية · غير محدودة"),
    "Téléchargement terminé à 100 %. Lancez l'installation maintenant." to ("Download complete at 100%. Start installation now." to "اكتمل التنزيل بنسبة 100٪. ابدأ التثبيت الآن."),
    "Paquet téléchargé. Autorisez l'installation depuis cette source pour appliquer la mise à jour." to ("Package downloaded. Allow installation from this source to apply the update." to "تم تنزيل الحزمة. اسمح بالتثبيت من هذا المصدر لتطبيق التحديث."),
    "Auto" to ("Auto" to "تلقائي"),
    "Appuyez ci-dessous pour ouvrir l'installateur Android." to ("Tap below to open the Android installer." to "اضغط أدناه لفتح مثبت Android."),
    "Administrateurs" to ("Administrators" to "المسؤولون"),
    "Secrétariat & Ventes" to ("Sales secretaries" to "السكرتارية والمبيعات"),
    "Magasiniers & Stock" to ("Warehouse & Stock" to "المخازن والمخزون"),
    "Tous les rôles" to ("All roles" to "كل الأدوار"),
    "Mouvements" to ("Transactions" to "الحركات"),
    "Clients" to ("Customers" to "العملاء"),
    "Rapports" to ("Reports" to "التقارير"),
    "Relevé mensuel PDF" to ("Monthly statement PDF" to "كشف شهري PDF"),
    "Choisissez les rubriques et la période : mois, année en cours, derniers mois ou dates exactes." to ("Choose sections and a period: month, current year, recent months, or exact dates." to "اختر الأقسام والفترة: شهر أو السنة الحالية أو الأشهر الأخيرة أو تواريخ محددة."),
    "Caisse" to ("Cash" to "النقد"),
    "Banque" to ("Bank" to "البنك"),
    "Apports" to ("Contributions" to "المساهمات"),
    "Prélèvements" to ("Withdrawals" to "السحوبات"),
    "Encaissement" to ("Payment received" to "تحصيل"),
    "Paie" to ("Payroll" to "الرواتب"),
    "Fournisseur" to ("Supplier" to "المورد"),
    "Mouvement" to ("Transaction" to "حركة"),
    "Étape 1/2 · Type et libellé" to ("Step 1/2 · Type and description" to "الخطوة 1/2 · النوع والوصف"),
    "Étape 2/2 · Montant et règlement" to ("Step 2/2 · Amount and payment" to "الخطوة 2/2 · المبلغ والدفع"),
    "Ce mois" to ("This month" to "هذا الشهر"),
    "3 mois" to ("3 months" to "3 أشهر"),
    "6 mois" to ("6 months" to "6 أشهر"),
    "12 mois" to ("12 months" to "12 شهراً"),
    "Année en cours" to ("Current year" to "السنة الحالية"),
    "Du… au…" to ("From… to…" to "من… إلى…"),
    "Du" to ("From" to "من"),
    "Au" to ("To" to "إلى"),
    "Créances clients" to ("Customer receivables" to "مستحقات العملاء"),
    "Fournisseurs" to ("Suppliers" to "الموردون"),
    "Employés & paie" to ("Employees & payroll" to "الموظفون والرواتب"),
    "Balance âgée" to ("Aging report" to "تقرير أعمار الديون"),
    "TVA" to ("VAT" to "ضريبة القيمة المضافة"),
    "Cet écran sert uniquement aux QR codes de sécurité PC — utilisez l'onglet Scan pour le stock." to ("This screen is only for secure PC QR codes — use the Scan tab for inventory." to "هذه الشاشة مخصصة لرموز QR الآمنة للكمبيوتر فقط — استخدم تبويب المسح للمخزون."),
    "QR code non reconnu. Scannez le QR affiché sur votre ordinateur Atlas Commerce." to ("QR code not recognized. Scan the code shown on your Atlas Commerce computer." to "رمز QR غير معروف. امسح الرمز المعروض على كمبيوتر Atlas Commerce."),
    "Vérification de la demande du poste PC…" to ("Verifying the PC request…" to "جارٍ التحقق من طلب الكمبيوتر…"),
    "Confirmez-vous la connexion de ce poste PC ?" to ("Do you approve this PC sign-in?" to "هل توافق على تسجيل دخول هذا الكمبيوتر؟"),
    "Poste PC autorisé avec succès." to ("PC approved successfully." to "تم اعتماد الكمبيوتر بنجاح."),
    "Association avec la session ouverte sur l'ordinateur…" to ("Linking to the session open on the computer…" to "جارٍ الربط بالجلسة المفتوحة على الكمبيوتر…"),
    "Validez maintenant la connexion sur l'écran de votre ordinateur…" to ("Approve the sign-in now on your computer screen…" to "اعتمد تسجيل الدخول الآن على شاشة الكمبيوتر…"),
    "Délai d'approbation dépassé — régénérez le QR sur le PC." to ("Approval timed out — generate a new QR code on the PC." to "انتهت مهلة الاعتماد — أنشئ رمز QR جديداً على الكمبيوتر."),
    "Ouverture de votre session Atlas…" to ("Opening your Atlas session…" to "جارٍ فتح جلسة Atlas…"),
    "Session mobile connectée !" to ("Phone connected!" to "تم ربط الهاتف!"),
    "Isolation multi-comptes, sessions, chiffrement" to ("Account isolation, sessions, encryption" to "عزل الحسابات والجلسات والتشفير"),
    "Seuil dépassé" to ("Threshold exceeded" to "تم تجاوز الحد"),
    "Atlas Agent peut se tromper. Votre message sera transmis au serveur Atlas et au fournisseur IA configuré. N'envoyez pas de mots de passe ni de données personnelles inutiles." to ("Atlas Agent can make mistakes. Your message will be sent to the Atlas server and configured AI provider. Do not include passwords or unnecessary personal data." to "قد يخطئ مساعد Atlas. ستُرسل رسالتك إلى خادم Atlas ومزوّد الذكاء الاصطناعي المُعدّ. لا ترسل كلمات مرور أو بيانات شخصية غير ضرورية."),
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
fun atlasRoleScopeSummary(role: String?): String = when (Roles.normalize(role)) {
    Roles.OWNER, Roles.ADMIN -> atlasText(
        "Accès complet : création directe produits, validations, finances & alertes globales",
        "Full access: direct product creation, approvals, finances & global alerts",
        "وصول كامل: إنشاء المنتجات مباشرةً والاعتمادات والمالية والتنبيهات العامة",
    )
    Roles.SECRETARY -> atlasText(
        "Accès Ventes & Caisse : facturation, stock & envoi direct des nouveaux produits à l'Admin",
        "Sales & Register access: invoicing, inventory & direct submission of new products to Admin",
        "وصول إلى المبيعات ونقطة البيع: الفوترة والمخزون وإرسال المنتجات الجديدة مباشرةً إلى المسؤول",
    )
    Roles.STOCK -> atlasText(
        "Accès Entrepôt : inventaire, ajustements stock & demandes envoyées directement à l'Admin",
        "Warehouse access: inventory, stock adjustments & requests sent directly to Admin",
        "وصول إلى المستودع: الجرد وتعديلات المخزون والطلبات المرسلة مباشرةً إلى المسؤول",
    )
    else -> atlasText(
        "Accès Opérateur : consultation catalogue, scan & demandes transmises à l'Admin",
        "Operator access: catalog browsing, scanning & requests sent to Admin",
        "وصول المشغّل: تصفح الكتالوج والمسح والطلبات المرسلة إلى المسؤول",
    )
}

@Composable
fun atlasRoleLabel(role: String?): String = when (Roles.normalize(role)) {
    Roles.OWNER -> atlasText("Compte principal", "Primary account", "الحساب الرئيسي")
    Roles.ADMIN -> atlasText("Administrateur", "Administrator", "مدير")
    Roles.SECRETARY -> atlasText("Secrétariat", "Sales secretary", "السكرتارية")
    Roles.STOCK -> atlasText("Stock", "Stock", "المخزون")
    else -> atlasText("Opérateur", "Operator", "مشغّل")
}

/** Date helpers bind locale-sensitive month and date names to the selected UI language. */
@Composable
fun atlasLocale(): java.util.Locale = when (LocalAtlasLanguage.current) {
    AppLanguage.FRENCH -> java.util.Locale.FRANCE
    AppLanguage.ENGLISH -> java.util.Locale.UK
    AppLanguage.ARABIC -> java.util.Locale.forLanguageTag("ar-MA")
}
@Composable
fun atlasDateTime(iso: String?): String = AtlasFormat.dateTime(iso, LocalAtlasLanguage.current)

@Composable
fun atlasDate(iso: String?): String = AtlasFormat.date(iso, LocalAtlasLanguage.current)

@Composable
fun atlasShortDate(iso: String?): String = AtlasFormat.shortDate(iso, LocalAtlasLanguage.current)

/** Money helpers bind shared numeric formatting to the app's selected UI language. */
@Composable
fun atlasMoney(amount: Double, currency: String? = null, withDecimals: Boolean = true): String =
    AtlasFormat.money(amount, currency, withDecimals, LocalAtlasLanguage.current)

@Composable
fun atlasMoneyCompact(amount: Double, currency: String? = null): String =
    AtlasFormat.moneyCompact(amount, currency, LocalAtlasLanguage.current)

@Composable
fun atlasInteger(value: Double): String = AtlasFormat.integer(value, LocalAtlasLanguage.current)

@Composable
fun atlasQuantity(value: Double): String = AtlasFormat.quantity(value, LocalAtlasLanguage.current)

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
    template(Regex("^Serveur Atlas Commerce vérifié · (\\d+) ms$")) { m ->
        Triple(m.value, "Atlas Commerce server verified · ${m.groupValues[1]} ms", "تم التحقق من خادم Atlas Commerce · ${m.groupValues[1]} مللي ثانية")
    }?.let { return it }
    template(Regex("^Accès bloqué par la protection anti-bot Cloudflare \\((\\d+)\\)\\. Autorisez /api/\\* dans votre pare-feu ou vérifiez le domaine saisi\\.$")) { m ->
        Triple(m.value, "Cloudflare blocked access (${m.groupValues[1]}). Allow /api/* in your firewall or check the domain.", "حجبت حماية Cloudflare الوصول (${m.groupValues[1]}). اسمح بالمسار /api/* في جدار الحماية أو تحقّق من النطاق.")
    }?.let { return it }
    template(Regex("^Site temporairement indisponible \\((\\d+)\\)\\. Vérifiez votre déploiement Atlas Commerce Web\\.$")) { m ->
        Triple(m.value, "Server returned an error (${m.groupValues[1]}). Check the Atlas Commerce Web deployment.", "أعاد الخادم خطأ (${m.groupValues[1]}). تحقّق من نشر Atlas Commerce Web.")
    }?.let { return it }
    template(Regex("^Connexion refusée \\((\\d+)\\)\\. Vérifiez que l'adresse saisie est parfaitement exacte\\.$")) { m ->
        Triple(m.value, "Connection rejected (${m.groupValues[1]}). Check the address.", "رُفض الاتصال (${m.groupValues[1]}). تحقّق من العنوان.")
    }?.let { return it }
    template(Regex("^Impossible de vérifier les mises à jour \\((\\d+)\\)\\.$")) { m ->
        Triple(m.value, "Could not check for updates (${m.groupValues[1]}).", "تعذّر التحقق من التحديثات (${m.groupValues[1]}).")
    }?.let { return it }
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
