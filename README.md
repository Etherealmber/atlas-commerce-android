<div align="center">
  <img src="app/src/main/assets/logo-light.svg" alt="Atlas Commerce" width="96" height="96" />
  <h1>Atlas Commerce — Android</h1>
  <p><strong>Official Native Android Companion for <a href="https://github.com/Etherealmber/atlas-commerce">Atlas Commerce Web</a></strong><br/>Stock · POS &amp; Receipts · Invoicing · Barcode &amp; PC QR Scanner · Atlas Agent AI</p>

![Version](https://img.shields.io/badge/version-1.0.8-blue)
![License](https://img.shields.io/badge/license-MIT-yellow)
![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-7f52ff)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285f4)
![Min SDK](https://img.shields.io/badge/minSdk-28%20(Android%209%2B)-3ddc84)
![Build](https://img.shields.io/badge/CI-GitHub%20Actions-2088ff)

**[Web Backend Repository (`Etherealmber/atlas-commerce`)](https://github.com/Etherealmber/atlas-commerce)** ·
**[Download Latest APK](https://github.com/Etherealmber/atlas-commerce-android/releases)** ·
**[Report a Bug](https://github.com/Etherealmber/atlas-commerce-android/issues/new)** ·
**[Security Policy](SECURITY.md)**

</div>

---

## ⚠️ Important Requirement Before First Use (Read First!)

> **This Android app is directly linked to an [Atlas Commerce Web (`Etherealmber/atlas-commerce`)](https://github.com/Etherealmber/atlas-commerce) server.**
> It does **not** store a standalone local accounting database on your phone — **you MUST deploy and set up your [Atlas Commerce Web](https://github.com/Etherealmber/atlas-commerce) instance FIRST** before connecting this mobile app!

### Step-by-Step Setup for Brand-New Users

1. **Step 1 — Deploy Atlas Commerce Web FIRST**:
   - Follow the instructions in **[Etherealmber/atlas-commerce](https://github.com/Etherealmber/atlas-commerce)** to deploy your own Cloudflare Worker + D1 database + R2 storage bucket on your HTTPS domain (for example `https://erp.your-company.com`).
   - Open your web domain in a browser first and create your administrator account to make sure the web platform is live.
2. **Step 2 — Install the Android App**:
   - Download **`atlas-commerce.apk`** from **[GitHub Releases](https://github.com/Etherealmber/atlas-commerce-android/releases)** onto your Android 9+ phone or handheld scanner.
3. **Step 3 — Connect Your Server URL**:
   - Launch **Atlas Commerce** on your phone, complete the quick welcome tour, and enter your **exact HTTPS server address** (e.g. `https://erp.your-company.com`).
   - Tap **« Tester la connexion »** (`Test connection`) to verify `/api/health`, database (`D1`), and storage (`R2`) status before signing in.
   - Optionally toggle **« Enregistrer ce serveur »** and give it a label (e.g. *Main Store*, *North Warehouse*) if you manage multiple servers.
4. **Step 4 — Sign In or Pair PC via QR Code**:
   - Enter your **Atlas Commerce** username & password (and 6-digit email verification code on a brand-new device).
   - Optionally toggle **« Mémoriser ce compte »** to save encrypted credentials **strictly isolated to that specific server**.

---

## 🩺 Troubleshooting — If a Server, Website, or Feature Appears Unavailable

If the app displays ***"Site introuvable"***, ***"Serveur indisponible"***, ***"Aucune API Atlas trouvée (404)"***, or a feature is unavailable:

1. **Double-Check What You Typed Character by Character! ✍️**
   - Most connection errors come from a small typo in the server URL (missing letter, extra space, `.ma` vs `.com`, or typing `http://` instead of `https://`).
   - Tap the server chip on the login screen (or **Mon profil → ⚙️ Paramètres → Changer ou ajouter un serveur**) and carefully re-check every character of the domain name you typed.
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

---

## ✨ Key Features (`v1.0.8`)

- **Strict Multi-Server & Multi-Account Isolation (Zero Data Leak)**:
  - Save multiple servers and multiple user accounts on one device with **100% cryptographic & filesystem isolation** (`AES-256-GCM` per server + `SHA-256` scoped offline caches). Switching accounts or servers immediately purges in-memory carts, caches, avatars, notifications, and AI chats.
- **Atlas Agent IA Synchronized with Web (`t-<base36>`)**:
  - Grounded on live sales, stock deficits, unpaid balances, and pending mobile actions; shares exact conversation threads with the Web app via `/api/admin/agent/threads` and displays real-time token quota bars.
- **Clean `Mon profil` vs `⚙️ Paramètres` Split**:
  - Tap your top-bar **Avatar** for **Mon profil** (Account & UI/POS/Scanner customizations) and tap **⚙️** in the top-right corner for **Paramètres** (PC QR pairing & devices, live AI quota bars, dual-mirror OTA APK updates, saved servers, and cache controls).
- **Camera Privacy & Explicit Controls**:
  - Camera hardware stays completely off when opening *Scan* or *QR PC* until you tap start, unbinds immediately upon leaving the screen, and lets you cancel (`Annuler`) any scanned code at any time.

---

## 🛠️ Building From Source

### Prerequisites
- **Android Studio** Ladybug (or newer) — JDK 17+, Gradle 8.9+
- **Android SDK 35** (`minSdk = 28` / Android 9+)

```bash
git clone https://github.com/Etherealmber/atlas-commerce-android.git
cd atlas-commerce-android
gradle :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🚀 Maintainer Guide — Publishing Releases & Automated Bug Hunting

### 1. Publishing a New Release via GitHub Actions (Manual Version & Channel Control)
Version numbers are **never** bumped automatically without your decision. When ready to publish:

1. Open **[Actions → Android CI](https://github.com/Etherealmber/atlas-commerce-android/actions/workflows/android.yml)** and click **Run workflow** on branch **`main`**.
2. Fill in the prompt fields manually:
   - **`version_name`** *(required)* — e.g. `1.0.8` or `1.0.9`
   - **`version_code`** *(required integer)* — e.g. `108` or `109`
   - **`release_type`** *(required)* — choose **`release`** (official *Latest* release) or **`prerelease`**
   - **`mandatory`** — force mandatory update screen in app (`true` / `false`)
   - **`release_notes`** — optional headline (detailed release notes are **auto-generated automatically** for both GitHub Releases and `CDN 1` `latest.json` when left empty)
3. The workflow compiles & signs **a single APK (`atlas-commerce.apk`)**, verifies launch & upgrade on an **Android 14 (API 34)** emulator, syncs `app/build.gradle.kts`, publishes the GitHub Release, and syncs `CDN 1`.

### 2. Fixing Cloudflare `403 Just a moment...` on `CDN 1` Upload (30-Second Setup)
If Cloudflare Zone Bot Fight Mode challenges `POST /api/admin/android-release` from GitHub Actions runners (`HTTP 403 Just a moment...`), use **either** of these two permanent options (both already supported automatically in `.github/workflows/android.yml`):

- **Option A — Direct Cloudflare API Token (Recommended, Zero WAF Involvement)**:
  1. In Cloudflare Dashboard → **My Profile → API Tokens → Create Token**, grant **Account → Workers R2 Storage → Edit** on bucket `company-stock-images`.
  2. Add repository secret **`CLOUDFLARE_API_TOKEN`** (and **`CLOUDFLARE_ACCOUNT_ID`**) in `Settings → Secrets and variables → Actions`. The workflow uploads `atlas-commerce-v<ver>.apk`, `latest.apk`, and `latest.json` straight to R2!
- **Option B — One Cloudflare WAF Skip Rule**:
  1. In Cloudflare Dashboard → **Security → WAF → Custom rules → Create rule**:
     - Expression: `(http.request.uri.path eq "/api/admin/android-release")`
     - Action: **Skip** → check *All remaining custom rules* + *All Super Bot Fight Mode rules*.
  2. The endpoint remains protected by `Authorization: Bearer <ATLAS_RELEASE_UPLOAD_TOKEN>`.

### 3. Automated Bug & Health Issue Creator (`.github/workflows/bug-hunter.yml`)
- Runs automatically twice every week **and** on demand under **Actions → Bug & Health Audit (Auto Issue Creator) → Run workflow**.
- Checks Gradle compilation, unit tests, KSP Moshi codegen, Android Lint XML errors, multi-tenant privacy invariants, API route parity against `Etherealmber/atlas-commerce`, and **GitHub Releases ↔ CDN 1 `latest.json` sync**, automatically opening deduplicated GitHub Issues when problems appear and auto-closing them as soon as they are resolved.

---

## 📄 License

Distributed under the **[MIT License](LICENSE)** — © 2026 Etherealmber / Atlas Commerce.
