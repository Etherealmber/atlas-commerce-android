<div align="center">
  <img src="app/src/main/assets/logo-light.svg" alt="Atlas Commerce" width="96" height="96" />
  <h1>Atlas Commerce — Android</h1>
  <p><strong>Compagnon Android natif de l'ERP Atlas Commerce</strong><br/>Stock · Caisse · Tickets VR · Tableau de bord · Scan</p>

![Version](https://img.shields.io/badge/version-1.0.6-blue)
![Licence](https://img.shields.io/badge/licence-MIT-yellow)
![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-7f52ff)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285f4)
![Min SDK](https://img.shields.io/badge/minSdk-28-3ddc84)
![Build](https://img.shields.io/badge/CI-GitHub%20Actions-2088ff)

**Projet web : [Etherealmber/atlas-commerce](https://github.com/Etherealmber/atlas-commerce)** ·
[Signaler un bug](https://github.com/Etherealmber/atlas-commerce/issues/new) ·
[Politique de sécurité](SECURITY.md) ·
[Journal des versions](CHANGELOG.md)

</div>

---

## 📱 À propos

Atlas Commerce pour Android est l'application **native** (Kotlin + Jetpack Compose) du projet
[Atlas Commerce](https://github.com/Etherealmber/atlas-commerce) : un ERP / caisse / facturation
auto-hébergé sur Cloudflare (Workers + D1 + R2) pensé pour les commerces et grossistes au Maroc.

L'application ne contient **aucune comptabilité locale** : elle se connecte à **votre** serveur
Atlas et travaille en client léger sur son API REST. Elle est complémentaire de la coquille web
(Capacitor) et reprend la même identité visuelle (zinc / papier / safran, densités compactes).

### Ce que fait l'app

| Écran | Contenu |
| --- | --- |
| **Choix du serveur** | Avant toute connexion : serveur de production, émulateur, serveur local ou adresse personnalisée, avec test réel de `/api/health` |
| **Connexion & QR PC** | Identifiant + mot de passe, OTP e-mail ou **connexion mobile par QR** depuis un ordinateur connecté |
| **Accueil** | Photo de profil (`avatar_url`), trésorerie, valeur du stock, produits, stock faible, créances, tendance mensuelle, derniers documents |
| **Produits** | Recherche, filtres **À réapprovisionner** (`Manque X u.`) et **Demandes scan**, ajout direct ou demande bureau avec checklist dynamique des champs requis (`Nom *`, `SKU *`, `Unité *`, `Quantité *`, `Prix *`), catalogue hors ligne |
| **Scan Stock** | CameraX + ML Kit : extinction immédiate de la caméra dès changement d'onglet / dialogue / mise en arrière-plan, bande des articles nécessaires, création directe des codes inconnus |
| **Scan Poste PC (QR)** | Interface de sécurité distincte (cadre QR carré indigo/cyan, liens profonds `/qr` & `/login-qr`), validation ou refus de connexion PC et gestion des **Appareils connectés** |
| **Notifications** | Cloche avec badge en barre supérieure regroupant les **Alertes** (`/api/alerts`) et les **Actions terrain PC ↔ Mobile** (`/api/mobile-actions`) |
| **Atlas Agent IA** | Copilote conversationnel natif (`/api/assist/chat`), suggestions proactives et approbation des actions proposées |
| **Vente** | Panier, quantification, client, note, création d'un **ticket VR sans TVA** prêt à encaisser |
| **Réglages** | Photo de profil (envoi galerie → WebP), appareils connectés & révocation, mises à jour automatiques à chaque ouverture, serveur, thème, densité |
| **Pages légales** | Licence MIT complète, sécurité & confidentialité, à propos — intégrées à l'app |

## 🆕 Version 1.0.6 (versionCode 106)

- **Confidentialité caméra stricte** : fermeture et libération immédiates de CameraX (`unbindAll`, analyseur et exécuteur) dès tout changement d'onglet, ouverture d'écran superposé ou passage en arrière-plan.
- **Vérification systématique des mises à jour** à chaque lancement et retour au premier plan, sans jamais interrompre ni écraser un téléchargement APK en cours.
- **Scanner un poste PC (`QrDeviceScreen`) distinct du Scan Stock** : cockpit de sécurité indigo/cyan à cadre carré `QR_CODE`, liens profonds `/qr` & `/login-qr`, et gestion/révocation des **Appareils connectés** (`/api/auth/devices`).
- **Photo de profil (`avatar_url`) partout** : décodage des avatars SVG du worker, WebP/PNG Base64, chemins relatifs et CDN HTTPS avec téléversement depuis la galerie (`PATCH /api/auth/me`).
- **Articles nécessaires, Centre de notifications & Atlas Agent IA** intégrés nativement.

## Historique — Version 1.0.5 (versionCode 105)

- **Choix du serveur API avant la connexion** — écran dédié au premier lancement, presets et test de
  disponibilité (`/api/health`), changement possible depuis la connexion **et** les réglages.
- **Parcours d'authentification complet** : mot de passe, `needs_otp`, vérification OTP, messages
  d'erreur du worker traduits en français, session restaurée au redémarrage.
- **Tableau de bord, catalogue, scan et vente rapide** réellement branchés sur l'API.
- **Sécurité** : jeton chiffré en AES-256-GCM via le Keystore Android, HTTPS obligatoire en release,
  aucune analytic, pages *Sécurité* et *Licence* dans l'app.
- **Thème** clair / sombre / système et densité compacte ou confortable, persistés.
- **Mode hors ligne** : dernier catalogue conservé sur l'appareil, bandeau explicite.
- **Robustesse** : booléens SQLite (`track_inventory`, `is_active`) et colonnes nullables
  (`low_stock_threshold`, `amount_paid`) décodés sans casser le JSON ; restauration de session
  tolérante à un Keystore réinitialisé ; chaque écran recharge ses données quand le compte change.
- **CI Android** : compilation + tests unitaires sur GitHub Actions, APK debug en artefact.

## 🚀 Démarrage

### Prérequis

- **Android Studio** Ladybug (ou plus récent) — AGP 8.7, JDK 17
- **Android SDK 35** (compileSdk / targetSdk)
- Un appareil ou émulateur **Android 9+** (minSdk 28)
- Un serveur Atlas (par défaut `https://stock.afmarbre.com`, ou le vôtre)

### Compiler

```bash
git clone https://github.com/Etherealmber/atlas-commerce-android.git
cd atlas-commerce-android
# Ouvrir dans Android Studio puis "Run", ou en ligne de commande :
gradle :app:assembleDebug
```

L'APK est généré dans `app/build/outputs/apk/debug/app-debug.apk`.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> Le wrapper Gradle n'est pas versionné : Android Studio propose la distribution locale,
> ou utilisez `gradle` (8.9+) installé sur la machine. La CI utilise Gradle 8.9.

### Signer une version release (facultatif)

`assembleRelease` produit un APK **non signé** tant qu'aucun keystore n'est configuré. Pour signer :

```bash
# à la racine du dépôt — ce fichier est ignoré par git
cat > keystore.properties <<'EOF'
storeFile=atlas-release.jks
storePassword=********
keyAlias=atlas
keyPassword=********
EOF

gradle :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

La release active R8 (`isMinifyEnabled`), réduit les ressources et **refuse HTTP en clair**.
Sous Windows, les chemins contenant des antislashs dans `keystore.properties`
(`storeFile=C:\Users\...\atlas-release.jks`) sont acceptés tels quels.

### Mise à jour depuis une copie locale 1.0.4

`git pull` ne supprime jamais les fichiers **non suivis** laissés par l'ancienne ébauche `1.0.4`
(par exemple `app/src/main/java/.../push/AtlasFirebaseMessagingService.kt`, qui fait échouer
`:app:kspReleaseKotlin` car Firebase a été retiré en `1.0.5`). Le script `app/build.gradle.kts`
déplace désormais automatiquement tout fichier non suivi sous `app/src/` vers
`.atlas-1.0.4-quarantine/` et purge le cache KSP (pensez à `git add` vos nouveaux fichiers sources,
ou passez `-PkeepUntrackedSources=true`) ; vous pouvez aussi nettoyer `app/src/` explicitement sans
toucher à `local.properties` ni `keystore.properties` :

```bash
git pull
git clean -fd app/src
```

### Choisir son serveur (première ouverture)

1. L'app propose **Production** (`https://stock.afmarbre.com`), **Émulateur** (`http://10.0.2.2:8787`)
   et **Local** (`http://127.0.0.1:8787`).
2. Pour un serveur auto-hébergé, saisissez son adresse (le suffixe `/api` est retiré automatiquement).
3. « Tester la connexion » interroge `/api/health` et affiche l'état D1/R2 et la latence.
4. « Continuer » enregistre l'adresse ; la connexion se fait ensuite sur ce serveur.

Le choix est modifiable à tout moment (chip serveur sur l'écran de connexion, ou **Réglages → Serveur**).
Changer de serveur déconnecte la session en cours : les jetons ne sont jamais réutilisés entre serveurs.

### Serveur local (développement)

```bash
# dans le dépôt web
printf 'ADMIN_USERNAME=admin\nADMIN_PASSWORD=ChangeMo123!\n' > .dev.vars
npx wrangler d1 execute DB --local --file=schema.sql
npm run dev -- --port 8787
```

Sur l'émulateur, utilisez `http://10.0.2.2:8787`. Le HTTP en clair n'est autorisé que par les
builds **debug** (`app/src/debug/AndroidManifest.xml`) ; les builds release exigent HTTPS.

## 🧱 Architecture

```
app/src/main/java/ma/etherealmber/atlascommerce/
├── AtlasApp.kt              Application Hilt
├── MainActivity.kt          Point d'entrée Compose (edge-to-edge)
├── core/
│   ├── AppDetails.kt        Identité, version, liens
│   ├── Appearance.kt        ThemeMode / DensityMode / Roles
│   ├── format/              Formatage fr-MA (monnaie, quantités, dates)
│   ├── net/                 Retrofit, OkHttp, intercepteurs, ApiResult, probe /health
│   ├── persist/             DataStore (serveur, apparence, session chiffrée)
│   ├── security/            Keystore AES-GCM + empreinte d'appareil
│   ├── server/              Serveur sélectionné (source de vérité)
│   └── session/             Restauration, login, OTP, déconnexion
├── data/                    DTO, dépôts, panier, cache hors ligne
├── di/                      Modules Hilt
└── ui/
    ├── root/                Gating serveur → session → shell
    ├── shell/               Barre d'onglets + top bar
    ├── screens/             server, login, home, products, scan, sale, settings, legal
    ├── components/          Design system Atlas
    └── theme/               Couleurs, densités, typographie
```

Choix techniques :

- **Hilt** pour l'injection, **DataStore** pour les préférences, **Retrofit/Moshi** pour l'API.
- **Un seul `OkHttpClient`** : en-têtes `X-Atlas-Client: android`, `X-Device-Fingerprint`,
  `User-Agent` Atlas, jeton Bearer injecté en mémoire.
- **Client Retrofit reconstruit** quand le serveur change (une seule instance conservée).
- **Aucune donnée métier en base locale** : seule la dernière liste de produits est mise en cache
  pour le mode hors ligne.

## 🔌 API utilisée

| Méthode | Chemin | Usage |
| --- | --- | --- |
| `GET` | `/api/health` | Test du serveur avant connexion (public) |
| `GET` | `/api/app/android-update` | Vérification des mises à jour APK (public, optionnelles ou obligatoires, progression temps réel dans l'app et en notification) |
| `POST` | `/api/auth/login` | Mot de passe (+ OTP si nouvel appareil) |
| `POST` | `/api/auth/verify-otp` | Validation du code e-mail |
| `GET` | `/api/auth/me` | Profil / rôle / rafraîchissement de session |
| `GET` | `/api/dashboard` | Tableau de bord |
| `GET` | `/api/products` | Catalogue (`q`, `limit`, `fields=mobile` : projection téléphone, sans image ni DDL) |
| `GET` | `/api/scan/barcode/:code` | Chemin rapide du scanner (code-barres **ou** SKU exact) |
| `POST` | `/api/products/:id/adjust-stock` | Entrée / sortie de stock manuelle, auditée côté worker |
| `GET` | `/api/settings` | Nom de société, devise |
| `GET` | `/api/clients` | Liste clients (préparé pour les évolutions) |
| `GET` | `/api/invoices` | Documents |
| `POST` | `/api/invoices` | Création du ticket VR (`quick_sale`, `tax_exempt`) |

En-têtes attendus par le worker : `Authorization: Bearer <token>`, `X-Device-Fingerprint`,
`X-Atlas-Client: android`. Le worker lie chaque session à l'IP et à l'empreinte de l'appareil.

## 🧪 Intégration continue

Le workflow `.github/workflows/android.yml` compile (`assembleDebug`), exécute les tests unitaires,
construit aussi la variante release (R8 + shrinkResources) puis publie l'APK debug en artefact.
Les écritures réseau (`POST`) ne sont **jamais** rejouées automatiquement par OkHttp ; la création
d'un ticket VR porte un en-tête `Idempotency-Key` que le worker déduplique.

## 🔐 Sécurité

- Jeton de session **chiffré au repos** (AES-256-GCM, clé non exportable du Keystore Android).
- **HTTPS obligatoire** en release, `usesCleartextTraffic=false`, réseau en clair réservé au debug.
- **Aucun analytics**, aucun crash reporter, aucune requête vers un tiers — à l'exception des
  métriques d'usage de **ML Kit** (Google), nécessaires au décodage des codes-barres : aucune
  image, aucun code scanné ne sont transmis.
- **Deep links** : aucun filtre `autoVerify` n'est déclaré tant que l'app ne sait pas router
  les URL de l'ERP ; les liens restent donc ouverts dans le navigateur.
- La caméra sert uniquement au décodage local des codes-barres (ML Kit embarqué).
- Déconnexion = jeton effacé, panier vidé, cache purgé.
- Détails, périmètre et procédure de signalement : [SECURITY.md](SECURITY.md).

## 🤝 Contribuer

Les issues et pull requests sont bienvenues sur
[Etherealmber/atlas-commerce-android](https://github.com/Etherealmber/atlas-commerce-android).
Merci d'indiquer la version de l'app, la version d'Android et le serveur utilisé. Pour une faille de
sécurité, écrivez à **security@afmarbre.com** plutôt que d'ouvrir une issue publique.

## 📄 Licence

**MIT © 2026 loxdpsy** — voir [LICENSE](LICENSE). Le texte intégral est aussi disponible dans
l'application (*Réglages → Licence*), avec la liste des composants tiers (Kotlin, AndroidX,
Compose, Hilt, OkHttp/Retrofit/Moshi, CameraX, ML Kit).

Atlas Commerce est un projet indépendant ; Android et Google Play sont des marques de Google LLC.
