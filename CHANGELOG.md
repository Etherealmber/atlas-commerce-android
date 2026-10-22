# Changelog — Atlas Commerce Android

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et
[SemVer](https://semver.org/lang/fr/).

## [Non publié]

### Ajouté

- **Connexion vérifiée en un tap par lien e-mail signé (`VerifyLinkParser` & `POST /api/auth/verify-link`)** :
  prise en charge des liens profonds `https://<serveur>/verify?user=…&code=…&sig=…&exp=…` et du
  relais natif `atlascommerce://verify?…` depuis le bouton **VÉRIFIER & CONTINUER**, sans saisie
  de code ni de mot de passe, directement sur le Worker correspondant au serveur choisi par
  l'utilisateur et avec isolation stricte de session par compte.
- **Paramètres optionnels avant compilation dans GitHub Actions (`workflow_dispatch`)** : demande
  optionnelle du lien de téléchargement APK (`download_url`), du dépôt GitHub (`repo_url`) et de
  l'URL CDN 1 (`cdn_url`) avant le build, avec repli automatique sur les valeurs par défaut si
  laissés vides et notes concises dans `latest.json`.

### Modifié

- **Téléchargement de mise à jour masquable & notifications système visibles** : possibilité de
  masquer l'encart de téléchargement tout en poursuivant le transfert en arrière-plan, avec rappel
  d'accès via **Paramètres** ou le volet de **Notifications** Android (canaux haute visibilité).

## [1.0.7] — 2026-10-22

### Ajouté

- **Rendu Markdown natif complet dans Atlas Agent IA (`AtlasMarkdown`)** : tableaux GFM avec
  alignement par colonne et défilement horizontal fluide, titres (`#`–`######`), texte **gras**,
  *italique*, ~~barré~~, code en ligne, blocs de code copiables en un geste, listes numérotées / à
  puces / cases à cocher, citations, liens cliquables et bouton **Copier** sur chaque réponse IA.
- **Animations d'accueil et transitions sur chaque page (`AtlasMotion`)** : halo pulsé à
  l'ouverture (`SplashPane`, `LoginScreen`, `ServerSetupScreen`), apparitions étagées sur le
  tableau de bord et transitions directionnelles fluides entre tous les onglets et écrans superposés.
- **Réglages utilisateur enrichis (`SettingsScreen`)** :
  - Édition directe du nom affiché et de l'adresse e-mail (`PATCH /api/auth/me`) + photo de profil.
  - Activation/désactivation des **animations fluides** et choix de l'**onglet d'ouverture par défaut**.
  - **Caméra & lecteur code-barres** : ouverture auto caméra (désactivée par défaut), vibration au
    scan, bip sonore au scan et **ajout automatique au panier** dès lecture d'un code-barres.
  - **Vente rapide & caisse** : nom de client pré-rempli par défaut et confirmation optionnelle
    avant création du ticket VR.
  - **Catalogue hors ligne & système** : compteur d'articles en cache local, vidage en un geste et
    contrôle de la recherche automatique des mises à jour.

### Modifié / Corrigé

- **Caméra fermée par défaut partout (`ScanScreen` & `QrDeviceScreen`)** : l'entrée sur *Scan*,
  *Scanner un poste PC (QR)* ou le raccourci depuis *Produits* n'ouvre **jamais** la caméra ni la
  demande de permission automatiquement — l'utilisateur démarre explicitement la caméra d'un appui,
  et elle se coupe immédiatement dès qu'on quitte l'écran.
- **En-tête unique sans doublon sur Accueil (`HomeScreen`)** : suppression de la seconde ligne
  avatar + nom d'utilisateur sous la barre supérieure ; le profil utilisateur reste affiché une
  seule fois dans la barre supérieure.
- **Barre de navigation épurée (`MainShell`)** : retrait de **Réglages** de la barre inférieure
  (conservation des 5 onglets métier : *Accueil*, *Produits*, *Scan*, *Vente*, *Agent*) et ouverture
  des Réglages directement en touchant l'avatar utilisateur en haut à droite.

## [1.0.6] — 2026-10-22

### Ajouté

- **Confidentialité caméra stricte (`BarcodeCameraView`)** : libération immédiate de CameraX
  (`unbindAll`, arrêt de l'analyseur ML Kit et de l'exécuteur) dès tout changement d'onglet,
  ouverture d'un écran superposé / dialogue ou mise en arrière-plan.
- **Vérification automatique des mises à jour à chaque ouverture / retour au premier plan**
  (`onAppResumed` / `onAppStopped`), tout en garantissant qu'aucun téléchargement APK actif n'est
  jamais interrompu ni écrasé.
- **Scanner un poste PC (`QrDeviceScreen`) distinct du Scan Stock** : cockpit de sécurité
  indigo/cyan à cadre carré `QR_CODE`, liens profonds `/qr` & `/login-qr` (`https` et
  `atlascommerce://qr`), connexion mobile pré-authentification et validation/refus de poste PC.
- **Appareils connectés** (`GET /api/auth/devices` & `POST /api/auth/devices/revoke` sur empreinte
  SHA-256 complète `full`) depuis Réglages et l'écran d'appairage QR.
- **Photo de profil (`avatar_url`) partout** : barre supérieure, Accueil, Réglages et validation QR,
  avec décodage SVG worker, WebP/PNG Base64, chemins relatifs et HTTPS + envoi depuis la galerie
  (`PATCH /api/auth/me`).
- **Articles nécessaires & checklist dynamique des champs requis** lors de l'ajout produit ou du
  scan d'un code inconnu (`POST /api/products` et `POST /api/stock/product-requests`).
- **Centre de notifications** (cloche avec badge en barre supérieure : `/api/alerts` +
  `/api/mobile-actions` compléter / refuser).
- **Atlas Agent IA** natif en barre inférieure (`/api/assist/status`, `/api/assist/next-actions`,
  `/api/assist/chat`, `/api/assist/approve`).

## [1.0.5] — 2026-10-21

### Ajouté

- **Sélection du serveur API avant la connexion** : écran dédié au premier lancement, presets
  (production, émulateur, local), adresse personnalisée et test réel de `/api/health`
  (latence, base D1, stockage R2).
- Changement de serveur depuis l'écran de connexion et depuis **Réglages → Serveur**.
- Connexion complète : mot de passe, `needs_otp`, vérification OTP, restauration de session,
  messages d'erreur du worker en français.
- Tableau de bord : trésorerie, valeur du stock, produits, stock faible, créances, tendance
  mensuelle, derniers documents.
- Catalogue produits : recherche nom / SKU / code-barres, filtre stock faible, fiche produit,
  ajout au panier, cache hors ligne du dernier catalogue.
- Scan de codes-barres (CameraX + ML Kit) avec saisie manuelle de secours.
- Vente rapide : panier, quantités, client, note, création d'un ticket VR sans TVA.
- Pages **Licence MIT**, **Sécurité & confidentialité** et **À propos** intégrées.
- **Ajustement de stock** depuis la fiche produit (entrée / sortie + motif) via
  `POST /api/products/:id/adjust-stock`, avec rafraîchissement de la liste.
- Scanner branché sur le chemin rapide du worker (`GET /api/scan/barcode/:code`, code-barres ou SKU
  exact) et catalogue demandé en projection `fields=mobile` (plus léger, plus rapide).
- Thème système / clair / sombre et densité compacte / confortable.
- Tests unitaires (normalisation d'adresse serveur, formatage fr-MA) et CI GitHub Actions
  (assemble debug, tests, APK en artefact).

### Sécurité

- Jeton de session chiffré en AES-256-GCM avec une clé du Keystore Android.
- HTTPS obligatoire en release ; HTTP réservé aux builds debug pour un serveur local.
- Aucun analytics ni SDK tiers ; caméra traitée localement.
- En-têtes `X-Atlas-Client`, `X-Device-Fingerprint` et `User-Agent` Atlas sur chaque requête.
- `allowBackup=false`, journalisation HTTP désactivée et `Authorization` masqué en release.

### Corrigé

- L'application est désormais compilable et exécutable : `MainActivity`, Application Hilt,
  thème, DI, réseau, écrans et ressources manquaient dans l'ébauche.
- Suppression des références Firebase / Room / identifiants Google inutilisés qui empêchaient
  la compilation et alourdissaient l'APK.
- Les booléens SQLite (`track_inventory`, `is_active`) et les colonnes numériques nullables
  (`low_stock_threshold`, `amount_paid`) sont désormais décodés sans casser le parsing JSON.
- Les écrans rechargent leurs données quand le compte connecté change : aucune donnée du
  précédent utilisateur ne subsiste sur un appareil partagé.
- Un seul `SnackbarHost` pour toute l'application (plus de messages affichés deux fois).
- Barres système lisibles avec le thème sombre, écrans de connexion et de serveur protégés
  par les insets (encoche, gestes).
- Champ de texte : bascule d'affichage du mot de passe, action « Terminé » du clavier et
  libellés d'aide accessibles.
- Adresse serveur : tout autre schéma que `http`/`https` est refusé au lieu d'être transformé.
- Nettoyage des imports inutilisés et des doublons de libellés de statut.
- Aucune requête `POST` rejouée automatiquement (OkHttp) : un ticket VR ou un ajustement de stock
  ne peut plus être appliqué deux fois ; les tickets portent un en-tête `Idempotency-Key`.
- Le test d'adresse serveur utilise un client **anonyme** : le jeton de session n'est jamais envoyé
  à un hôte saisi par l'utilisateur.
- Résultats de scan et de test de serveur : seule la dernière demande compte (plus de produit ou
  de rapport périmé).
- Recherche hors ligne corrigée (le catalogue en cache est réellement filtré) et cache produit
  effacé à la déconnexion / au changement de serveur.
- Thème : plus de flash clair au démarrage pour un utilisateur en thème sombre.
- Caméra : liaison annulée si l'écran est quitté avant l'initialisation.
- Manifest nettoyé : plus de service de push fantôme ni de deep link non traité.
- Compilation release (`:app:kspReleaseKotlin`) après `git pull` sur un dossier local `1.0.4` :
  quarantaine automatique des fichiers non suivis sous `app/src/` (comme
  `push/AtlasFirebaseMessagingService.kt`) vers `.atlas-1.0.4-quarantine/` et purge du cache
  incrémental KSP.
- `keystore.properties` sous Windows : prise en charge des chemins contenant des antislashs non
  échappés (`C:\Users\...\release.jks`).

## [1.0.4] — ébauche précédente

- Thème Material 3, modèles de données et ébauche de panier. Ne compilait pas (écrans, DI et
  point d'entrée absents).
