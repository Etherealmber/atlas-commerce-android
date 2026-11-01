<div align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="app/src/main/assets/logo-dark.png" />
    <img src="app/src/main/assets/logo-light.png" alt="Atlas Commerce" width="234" height="76" />
  </picture>
  <h1>Atlas Commerce — Android</h1>
  <p><strong>Official Native Android Companion for <a href="https://github.com/Etherealmber/atlas-commerce">Atlas Commerce Web</a></strong><br/>Stock · POS &amp; Receipts · Invoicing · Barcode &amp; PC QR Scanner · Atlas Agent AI</p>

![Version](https://img.shields.io/github/v/release/Etherealmber/atlas-commerce-android?label=version)
![License](https://img.shields.io/github/license/Etherealmber/atlas-commerce-android)
![Kotlin](https://img.shields.io/badge/kotlin-2.3.0-7f52ff)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285f4)
![Min SDK](https://img.shields.io/badge/minSdk-28%20(Android%209%2B)-3ddc84)
![Build](https://img.shields.io/github/actions/workflow/status/Etherealmber/atlas-commerce-android/android.yml?branch=main&label=CI)

**[Web Backend Repository (`Etherealmber/atlas-commerce`)](https://github.com/Etherealmber/atlas-commerce)** ·
**[Download Latest APK](https://github.com/Etherealmber/atlas-commerce-android/releases)** ·
**[Report a Bug](https://github.com/Etherealmber/atlas-commerce-android/issues/new)** ·
**[Security Policy](SECURITY.md)**

</div>

---

## ⚠️ Important Requirement Before First Use (Read First!)

> **This Android app is directly linked to an [Atlas Commerce Web (`Etherealmber/atlas-commerce`)](https://github.com/Etherealmber/atlas-commerce) server.**
> It does **not** store a standalone local accounting database on your phone — **you MUST deploy and set up your [Atlas Commerce Web](https://github.com/Etherealmber/atlas-commerce) instance FIRST** before connecting this mobile app!
>
> **New from zero?** Follow the full [self-hosting and Android setup guide](docs/SETUP.md): Cloudflare account, D1 database/migrations, R2, mail/keys, app installation, optional Firebase crash reports, release signing, and local device-unlock setup.

### Step-by-Step Setup for Brand-New Users

1. **Step 1 — Deploy Atlas Commerce Web FIRST**:
   - Follow the instructions in **[Etherealmber/atlas-commerce](https://github.com/Etherealmber/atlas-commerce)** to deploy your own Cloudflare Worker + D1 database + R2 storage bucket on your HTTPS domain (for example `https://erp.your-company.com`).
   - Open your web domain in a browser first and create your administrator account to make sure the web platform is live.
2. **Step 2 — Install the Android App**:
   - Download **`atlas-commerce.apk`** from **[GitHub Releases](https://github.com/Etherealmber/atlas-commerce-android/releases)** onto your Android 9+ phone or handheld scanner.
3. **Step 3 — Connect Your Server URL**:
   - On a new device, choose your server first, enter its **exact HTTPS address** (e.g. `https://erp.your-company.com`), and test the connection. The welcome tour follows; saved servers are reused on later sign-ins.
   - Tap **« Tester la connexion »** (`Test connection`) to verify `/api/health`, database (`D1`), and storage (`R2`) status before signing in.
   - Optionally toggle **« Enregistrer ce serveur »** and give it a label (e.g. *Main Store*, *North Warehouse*) if you manage multiple servers.
4. **Step 4 — Sign In, Tap Your Email Link, or Pair PC via QR Code**:
   - Enter your **Atlas Commerce** username & password — on a new device, either tap the **« VÉRIFIER & CONTINUER »** one-tap button directly inside the verification e-mail (opens the app and authenticates against your selected Worker automatically with no code typing!) or type the 6-digit OTP manually.
   - Android sign-in uses your username/password and any required one-time code. Atlas never stores your account password.
   - If **« Mémoriser ce compte »** is enabled, Atlas asks once whether to allow future sign-ins with biometrics or your Android device PIN/password. The encrypted saved session is verified with the selected server; declining or unavailable device authentication leaves password sign-in available.

---

## 🩺 Troubleshooting — If a Server, Website, or Feature Appears Unavailable

If the app displays ***"Site introuvable"***, ***"Serveur indisponible"***, ***"Aucune API Atlas trouvée (404)"***, or a feature is unavailable:

1. **Double-Check What You Typed Character by Character! ✍️**
   - Most connection errors come from a small typo in the server URL (missing letter, extra space, `.ma` vs `.com`, or typing `http://` instead of `https://`).
   - Open the **three-dot menu → Choose server** on the login screen (or **Mon profil → ⚙️ Paramètres → Changer ou ajouter un serveur**) and carefully re-check every character of the domain name you typed.
2. **Verify Your Website Is Actually Online First 🌐**
   - Open the exact same URL directly inside Chrome on your phone (`https://your-domain.com/api/health`).
   - If it works, you will see `{"success":true,"data":{"ok":true,...}}`. If it does not load in Chrome either, deploy or restart your **[Atlas Commerce Web](https://github.com/Etherealmber/atlas-commerce)** worker first.
3. **Check Your User Role Permissions 🔐**
   - Features adapt dynamically to your assigned role on the server:
     - **Owner / Admin**: Full access, direct product creation, user/device approvals, unlimited Atlas Agent IA tokens.
     - **Secrétaire / Facturation**: Dashboard, POS quick sales (`Vente`), clients, scan requests (sends new product creation requests to Admin).
     - **Magasinier (Warehouse)**: Stock lookup, barcode scanner, stock adjustments, product requests (POS `Vente` tab intentionally hidden).
     - **Lecteur / User**: Read-only stock & scan requests.
4. **Check Atlas Agent IA Quota (`Paramètres` or `Agent`) 🤖**
   - Non-admin accounts have daily (`00:00 UTC`) and weekly (`Monday 00:00 UTC`) token limits enforced by the server. Check the live **Quota Bar** inside **Atlas Agent** or **Mon profil → ⚙️ Paramètres** to see remaining tokens and exact reset countdown time.
5. **Check Cloudflare Bot Fight / WAF Rules on Custom Domains 🛡️**
   - If you enabled aggressive Cloudflare *Super Bot Fight Mode* on your custom domain, allow requests carrying path `/api/*` so mobile API calls and automated release uploads are never challenged by HTML CAPTCHA pages.
6. **Quick device unlock is missing or unavailable 🔐**
   - Quick sign-in is opt-in per saved account and requires an enrolled biometric or secure Android screen lock. If it is disabled or unavailable, sign in with the account password; you can review device unlock in Profile settings. Android does not use WebAuthn passkeys.
7. **Crash reports not appearing 🐞**
   - Firebase is optional and automatic upload is off until the user opts in under Settings. Confirm the correct `google-services.json` is installed in the build, Crashlytics is enabled in Firebase Console, and the test install has opted in. See [Firebase setup](docs/SETUP.md#4-optional-push-notifications-and-automatic-crash-reports-firebase).

---

## ✨ Key Features (`v1.4.4`)

- **Strict Multi-Server & Multi-Account Isolation (Zero Data Leak)**:
  - Save multiple servers and multiple user accounts on one device with **100% cryptographic & filesystem isolation** (`AES-256-GCM` per server + `SHA-256` scoped offline caches). Switching accounts or servers immediately purges in-memory carts, caches, avatars, notifications, and AI chats.
- **Atlas Agent IA Synchronized with Web (`t-<base36>`)**:
  - Grounded on live sales, stock deficits, unpaid balances, and pending mobile actions; shares exact conversation threads with the Web app via `/api/admin/agent/threads` and displays real-time token quota bars.
- **Clean `Mon profil` vs `⚙️ Paramètres` Split**:
  - Tap your top-bar **Avatar** for **Mon profil** (Account & UI/POS/Scanner customizations) and tap **⚙️** in the top-right corner for **Paramètres** (PC QR pairing & devices, live AI quota bars, dual-mirror OTA APK updates, saved servers, and cache controls).
- **Theme-aware Atlas identity and navigation**:
  - The supplied Atlas wordmark switches foreground contrast with the app's light/dark theme, the Android launcher exposes a themed monochrome icon where supported, and the phone navigation centers a raised Scan action while preserving the tablet navigation rail.
- **Camera Privacy & Explicit Controls**:
  - Camera hardware stays completely off when opening *Scan* or *QR PC* until you tap start, unbinds immediately upon leaving the screen, and lets you cancel (`Annuler`) any scanned code at any time.
- **Opt-in local device unlock**:
  - After a successful manual sign-in, a saved account can opt into biometrics or Android device PIN/password. Atlas unlocks the locally encrypted session token and verifies it with the selected server; the account password is never stored. Manual password sign-in remains available when this option is declined or unavailable.
- **Automatic crash reporting is optional and consent-based**:
  - Firebase Crashlytics uploads crash reports automatically only after the user opts in under Settings. It is off by default; builds without Firebase configuration continue to work without push or automatic crash uploads.
- **Temporary finance reports and local-data controls**:
  - Download/open/share combined monthly PDF reports, retain them in private app cache for up to seven days, and delete them from Settings without touching server business records.

---

## 🛠️ Building From Source

### Prerequisites
- **Android Studio** current stable — JDK 17+ (CI uses JDK 21)
- **Android SDK 35** (`minSdk = 28` / Android 9+)

```bash
git clone https://github.com/Etherealmber/atlas-commerce-android.git
cd atlas-commerce-android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For the complete backend-first onboarding and optional provider configuration, see [docs/SETUP.md](docs/SETUP.md).

---

## 🚀 Maintainer Guide — Publishing Releases & Automated Bug Hunting

### 1. Publishing a New Release via GitHub Actions (100% Manual — Never Auto-Published on Push)
Version numbers and releases are **never** bumped or published automatically on pushes or PR merges — releases publish **only** when you run **`workflow_dispatch`** manually (or push a `v*` tag). The version and CI badges at the top use live GitHub endpoints; after a release is published, the Android release workflow’s **Sync README release version** job automatically updates the Key Features heading on `main`.

1. Open **[Actions → Android CI](https://github.com/Etherealmber/atlas-commerce-android/actions/workflows/android.yml)** and click **Run workflow** on branch **`main`**.
2. Fill in the prompt fields before building:
   - **`version_name`** *(required)* — e.g. `1.1.3`
   - **`version_code`** *(required integer)* — e.g. `113`
   - **`release_type`** *(required)* — choose **`release`** (official *Latest* release) or **`prerelease`**
   - **`mandatory`** — force mandatory update screen in app (`true` / `false`)
   - **`download_url`** *(optional)* — custom direct APK URL (uses automatic default if left blank)
   - **`repo_url`** *(optional)* — custom GitHub repository URL (defaults to `https://github.com/Etherealmber/atlas-commerce-android` if left blank)
   - **`cdn_url`** *(optional)* — custom CDN base URL (defaults to `https://cdn1.afmarbre.com/app-updates/android` if left blank)
   - **`release_notes`** *(optional)* — custom short note (concise minimal notes are generated in `latest.json` automatically if left blank)
3. The workflow compiles & signs **a single APK (`atlas-commerce.apk`)**, verifies launch & upgrade on an **Android 14 (API 34)** emulator, syncs `app/build.gradle.kts`, publishes the GitHub Release, and submits the release to the policy-aware backend Worker. Update checks query the configured backend at `GET /api/app/android-update` and also inspect CDN 1 and GitHub `latest.json`; the backend minimum-version policy is retained when evaluating mirror manifests.
4. Direct R2 writes of `latest.json` are intentionally disabled: a generated release manifest does not contain backend-owned minimum-version policy. If Worker ingestion is unavailable, the workflow leaves CDN metadata unchanged and GitHub Releases remains available; it does not publish a policy-stripped manifest.

### 2. Fixing Cloudflare `403 Just a moment...` on `CDN 1` Upload (30-Second Setup)
If Cloudflare Zone Bot Fight Mode challenges `POST /api/admin/android-release` from GitHub Actions runners (`HTTP 403 Just a moment...`), add a WAF Skip rule for the authenticated release-ingestion route:

1. In Cloudflare Dashboard → **Security → WAF → Custom rules → Create rule**:
   - Expression: `(http.request.uri.path eq "/api/admin/android-release")`
   - Action: **Skip** → check *All remaining custom rules* + *All Super Bot Fight Mode rules*.
2. The endpoint remains protected by `Authorization: Bearer <ATLAS_RELEASE_UPLOAD_TOKEN>` (and optional Cloudflare Access service-token headers).
3. The workflow verifies GitHub Release and public CDN `latest.json` separately, records the policy fields observed, and fails the publish job if a successful Worker ingestion does not produce matching CDN release metadata with minimum-version fields.

### 3. Automated Bug & Health Issue Creator (`.github/workflows/bug-hunter.yml`)
- Runs automatically twice every week **and** on demand under **Actions → Bug & Health Audit (Auto Issue Creator) → Run workflow**.
- Checks Gradle compilation, unit tests, KSP Moshi codegen, Android Lint XML errors, multi-tenant privacy invariants, API route parity against `Etherealmber/atlas-commerce`, and **GitHub Releases ↔ CDN 1 `latest.json` sync**, automatically opening deduplicated GitHub Issues when problems appear and auto-closing them as soon as they are resolved.

### 4. Dependency and provider security review
- Before publishing, open **Repository → Security → Dependabot alerts**, review every open high/critical alert, and update or mitigate affected dependencies. Do not dismiss an alert without recording the reason and a safe upgrade plan.
- Review Cloudflare/Firebase/Google Play provider keys and permissions at least once per release. Rotate leaked secrets at the provider; do not try to fix a leak by deleting it from Git history alone.
- See [SECURITY.md](SECURITY.md) and the [from-zero provider setup](docs/SETUP.md) for key handling and data-collection boundaries.

---

## 📄 License

Distributed under the **[MIT License](LICENSE)** — © 2026 Etherealmber / Atlas Commerce.
