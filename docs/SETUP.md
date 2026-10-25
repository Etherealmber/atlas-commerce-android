# Atlas Commerce — installation and operator setup

This guide starts from an empty Cloudflare account and ends with the native Android app connected to your own Atlas Commerce Worker. It covers the backend database, file storage, mail, Android signing, optional Firebase push/crash reporting, and passkey enablement. Keep production credentials in the provider's secret store; never paste them into an issue, chat, source file, or committed `.dev.vars`.

> **Architecture:** Android is an API client, not a stand-alone ERP. The selected Atlas Commerce Worker owns accounts, roles, inventory, sales, invoices, and the D1 database. The app stores only app-private session/cache data. Deploy and test the web backend before connecting Android.

## 1. Accounts and tools

Create or obtain access to:

- [Cloudflare](https://dash.cloudflare.com/sign-up) for Workers, D1, R2, and a custom domain.
- [GitHub](https://github.com/) for the backend and Android source repositories.
- [Node.js 20 or newer](https://nodejs.org/en/download/) and npm for the Worker.
- [Android Studio](https://developer.android.com/studio) with Android SDK 35 and JDK 17 or newer (the CI build uses JDK 21).
- Optional: [Firebase Console](https://console.firebase.google.com/) for push notifications and consent-based Crashlytics reports.
- Optional for Play distribution/passkeys: a [Google Play Console](https://play.google.com/console/) account.

Authoritative project docs: [Atlas Commerce Web](https://github.com/Etherealmber/atlas-commerce), its [deployment README](https://github.com/Etherealmber/atlas-commerce#-starter--d%C3%A9marrer-de-z%C3%A9ro), [API/backend security policy](https://github.com/Etherealmber/atlas-commerce/blob/main/SECURITY.md), and [WebAuthn deployment guide](https://github.com/Etherealmber/atlas-commerce/blob/main/docs/webauthn.md).

## 2. Deploy the backend and database first

### 2.1 Get the Worker source and install dependencies

```bash
git clone https://github.com/Etherealmber/atlas-commerce.git
cd atlas-commerce
npm ci
npx wrangler login
```

`wrangler login` opens Cloudflare OAuth in a browser. Do not put an API token in a committed file. For CI or unattended deploys, create a narrowly scoped Cloudflare API token using [Cloudflare API token guidance](https://developers.cloudflare.com/fundamentals/api/get-started/create-token/) and store it as a protected GitHub Actions secret.

### 2.2 Run locally against a local D1 database

Create a private `.dev.vars` file at the backend root using the variable names shown in the backend's current [README](https://github.com/Etherealmber/atlas-commerce#-starter--d%C3%A9marrer-de-z%C3%A9ro) and Worker configuration; at minimum, set a unique local `ADMIN_USERNAME` and strong `ADMIN_PASSWORD`. `.dev.vars` is ignored by Git. Do not use example passwords outside a disposable local database.

Apply the checked-in base schema and migrations, then start the Worker:

```bash
npx wrangler d1 execute DB --local --file=schema.sql
npx wrangler d1 migrations apply DB --local
npm run dev -- --port 36521
```

Open `http://localhost:36521` and test the API:

```bash
curl -i http://localhost:36521/api/health
```

A successful response should indicate the Worker is up and report the local D1/storage bindings. Keep local test data separate from production.

### 2.3 Create production resources

In the Cloudflare dashboard, select the account that owns the Worker. The resource names below match the backend's `wrangler.jsonc`; if you choose different names, update that config's binding names too.

```bash
# Run from the backend repository root after wrangler login.
npx wrangler d1 create company-stock-db
npx wrangler r2 bucket create company-stock-images
npx wrangler kv namespace create CACHE
```

Copy the returned D1 `database_id` and KV namespace `id` into your private deployment configuration or update the checked-in non-secret binding IDs if you maintain the project fork. Do not point a test deployment at the production database. The R2 bucket name must match the `IMAGES` binding in `wrangler.jsonc`.

Apply the initial SQL schema and all migrations to production **once**:

```bash
npx wrangler d1 execute DB --remote --file=schema.sql
npx wrangler d1 migrations apply DB --remote
```

Keep the D1 migration history with the code. For backups and restore, use the backend's documented owner-only backup/restore flow; do not manually edit live business tables while the Worker is serving traffic. See [Cloudflare D1 migrations](https://developers.cloudflare.com/d1/reference/migrations/) and [D1 backups](https://developers.cloudflare.com/d1/reference/backup-and-restore/).

### 2.4 Set secrets and service providers

Use `npx wrangler secret put NAME` for each secret required by the backend's current `.dev.vars.example`/security docs. At minimum, configure a unique production administrator password and all authentication/session secrets required by the version you deploy. Do not copy example secrets into production.

Optional integrations:

- **One-time codes by e-mail:** configure the backend's Cloudflare Email Routing/Workers Email binding or its supported transactional mail provider (for example [Resend](https://resend.com/docs)); verify the sender/domain and set its API key as a Worker secret. In production, new-device OTP must reach the user's inbox.
- **Cloudflare Turnstile:** create a site at [Turnstile](https://dash.cloudflare.com/?to=/:account/turnstile), configure the public site key in the Worker, and keep the secret key in Worker Secrets. Never put the secret key in Android.
- **AI features:** add only the AI provider key for a provider the backend supports, using `wrangler secret put`; the Android app calls the Worker API and does not receive provider keys.
- **R2/CDN custom domain:** configure the public media/update hostname in Cloudflare and the backend's supported variables. Keep bucket credentials out of the APK.

See [Cloudflare Workers secrets](https://developers.cloudflare.com/workers/configuration/secrets/), [D1](https://developers.cloudflare.com/d1/), and [R2](https://developers.cloudflare.com/r2/) for provider-specific steps.

### 2.5 Deploy, create the admin account, and test

Configure a Cloudflare Workers custom domain with HTTPS, then deploy the backend using its documented production command (currently `npm run deploy`, from the backend root, after confirming the target environment and D1 binding). Open the HTTPS domain in a browser, sign in with the administrator credentials, and verify:

- `https://YOUR_DOMAIN/api/health` returns JSON and healthy D1/storage status.
- The admin can create a test user, assign the minimum required role, and revoke a test device.
- E-mail OTP works for a new device if e-mail is enabled.
- Backups and restore procedures are understood before importing real business data.

Never use the public production data set as a test fixture. Record the Worker URL; you will enter it into Android in §3.

## 3. Install or build the Android client

### 3.1 Install a release APK

Download the latest signed APK from [Android releases](https://github.com/Etherealmber/atlas-commerce-android/releases). Install only APKs signed by a publisher you trust. Android may ask you to allow installation from the browser or file manager used to open the APK.

### 3.2 Build from source

```bash
git clone https://github.com/Etherealmber/atlas-commerce-android.git
cd atlas-commerce-android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open Atlas Commerce, complete the welcome screen, enter your exact HTTPS Worker origin (for example `https://erp.example.com`), and tap **Test connection**. The check calls `/api/health`; it does not create a database or account. Sign in using the account created on the web app. On a shared device, turn off **Remember this account** when appropriate and use the app's sign-out/device-revocation controls.

The Android application ID is `ma.etherealmber.atlascommerce`. Debug builds are signed with the local Android debug key; release/Play builds use their own signing certificate. Do not confuse the upload key with the Play app-signing key.

## 4. Optional push notifications and automatic crash reports (Firebase)

Firebase is optional. If `app/google-services.json` is absent, Android still builds; Firebase push registration and automatic Crashlytics upload remain inactive. Crash-report collection is **off by default**. A user must enable **Paramètres → Envoyer automatiquement les rapports de plantage** once; after opting in, subsequent crash reports are uploaded automatically when connectivity is available. Turning the setting off withdraws that choice. Manual in-app diagnostic reports remain a separate, user-submitted flow.

### 4.1 Create the Firebase project and Android app

1. Create/select a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package `ma.etherealmber.atlascommerce` (exact match; do not use your Worker domain as the package name).
3. Enable **Crashlytics** for the project. If the backend push-notification routes are in use, enable **Firebase Cloud Messaging** in the same project.
4. Download that Android app's `google-services.json` and place it at `app/google-services.json`. This file is excluded from Git in this repository. Do not commit private release signing material or service-account JSON.
5. Build and install a QA/release candidate. In Settings, enable automatic crash reports only on a test device/account that has consented. Verify a report in Firebase Console → **Crashlytics**. Do not trigger a deliberate crash in a production user's build.

Firebase's public client configuration identifies the Firebase app; it is not a replacement for a server secret. Never embed a service-account key or FCM server credential in Android. See [Add Firebase to Android](https://firebase.google.com/docs/android/setup) and [Crashlytics setup](https://firebase.google.com/docs/crashlytics/android/get-started).

### 4.2 Configure GitHub Actions for Firebase-enabled builds

If CI should generate Firebase-configured APKs, base64-encode the same app config and add it as a repository Actions secret named `GOOGLE_SERVICES_JSON_BASE64`:

```bash
# Linux
base64 -w0 app/google-services.json
# macOS
base64 < app/google-services.json | tr -d '\n'
```

In GitHub: **Repository → Settings → Secrets and variables → Actions → New repository secret**. A fork that does not have the secret builds without Firebase. The Crashlytics Gradle plugin is conditionally applied only when the config file is present, so obfuscation mapping upload is available for configured release builds.

Crash reports may contain a stack trace, app version, Android version/model, timestamps, and Firebase installation identifiers. This app does not attach session tokens, passwords, OTPs, financial records, or custom log breadcrumbs to Crashlytics. See the [Android security/privacy policy](../SECURITY.md) and Google's [Crashlytics data collection documentation](https://firebase.google.com/docs/crashlytics/android/data-collection).

## 5. Optional Android passkeys — keep disabled until the full smoke test passes

Passkeys use the selected Worker as the relying-party server. Password/OTP login continues to work when passkeys are unavailable. The backend deliberately keeps `WEBAUTHN_ENABLED=false` by default; never turn it on just because the Android button is present in a development build.

### 5.1 Backend and domain prerequisites

Follow the backend's current [WebAuthn guide](https://github.com/Etherealmber/atlas-commerce/blob/main/docs/webauthn.md). In summary:

1. Deploy the hardened verifier and tests to a **staging/preview Worker** first.
2. Keep `WEBAUTHN_RP_ID` set to the registrable web domain (no scheme), and `WEBAUTHN_ALLOWED_ORIGINS` set to exact HTTPS origins. Do not use an Android package name as the RP ID.
3. Set `WEBAUTHN_ANDROID_PACKAGE=ma.etherealmber.atlascommerce`.
4. Obtain the **Play App Signing key certificate SHA-256** from Play Console → **Setup → App integrity → App signing key certificate**. It is not derivable from this repository or the upload keystore. Configure that fingerprint as `WEBAUTHN_ANDROID_CERT_HASH` in the backend's non-secret Worker vars. Add a separate debug/upload fingerprint only to a staging environment if you intentionally test builds signed by that key.
5. Deploy and check `https://YOUR_DOMAIN/.well-known/assetlinks.json`. It must list the exact package and Play signing certificate, including Credential Manager's `delegate_permission/common.get_login_creds` relation. The backend should return `data.android.enabled: true` only after this config is valid.
6. Query `GET /api/auth/webauthn/status` on the same server the Android app will select. Check `data.enabled`, `data.android.enabled`, `data.android.package`, `data.android.origin`, and `data.origins`.

The Android client fails closed: it checks both server enablement flags, exact package ID, that `data.android.origin` equals the native-origin value derived locally from the installed build's signing certificate DER, and that the selected server origin appears in the backend's `data.origins`. Registration is available only inside an authenticated session; login creates a session only after backend verification succeeds.

### 5.2 Origin mode and the compatibility gate

For Android 14+ the client sets the selected server's exact web origin on the Credential Manager request and declares `android.permission.CREDENTIAL_MANAGER_SET_ORIGIN`. That is the mode described by the backend verifier: the response must have the configured HTTPS web origin, `crossOrigin: true`, and the expected Android package. On older Android versions where the explicit origin parameter is not effective, the native app origin path must be accepted by the Worker. The server-provided `data.android.origin` is compared against the certificate-derived origin; the client never invents a signing fingerprint or trusts a user-entered origin.

**Important format check:** Google's Credential Manager documentation describes the native app origin as `android:apk-key-hash:<base64url(SHA-256(certificate DER))>` (without `-sha256` in the prefix). The current backend guide describes its derived value with `android:apk-key-hash-sha256:`. These spellings are not interchangeable. Before enabling passkeys, compare the exact `data.android.origin` value from the Worker with the origin produced by the installed Play-signed build and confirm both native and explicit-origin flows in the staging smoke test. If they differ, fix the backend/client contract; do not weaken the exact-origin verifier or force the button on. See [Android passkey origin](https://developer.android.com/identity/passkeys/create-passkeys), [Credential Manager sign-in](https://developer.android.com/identity/passkeys/sign-in-with-passkeys), and [origin delegation API](https://developer.android.com/identity/sign-in/privileged-apps).

### 5.3 Staging smoke test and production switch

1. Keep production `WEBAUTHN_ENABLED=false`.
2. On staging, confirm the status response and assetlinks file; install the **Play-signed** APK (or the exact staging signer configured server-side).
3. Compare the app's origin with `status.data.android.origin` exactly, including prefix and Base64URL padding rules. The Settings/Login passkey affordance must remain hidden on mismatch.
4. Register a passkey for a test account, sign out, sign in with that passkey, and confirm the Worker verifies the response. Replay a consumed challenge and try a wrong account; both must fail.
5. Test the web/PWA passkey flow too. Ensure Android settings, Android passkey origin, and web-origin + `crossOrigin` behavior do not broaden each other's allowlists.
6. Only after successful staging checks and review of backend WebAuthn CI results: set production `WEBAUTHN_ENABLED=true`, deploy, verify `/api/auth/webauthn/status`, then test one production account. Keep password/OTP as a recovery route.

### 5.4 WebAuthn API endpoints used by Android

The app talks only to the selected Worker; it does not call a key provider directly or hold a server secret:

| Endpoint | Purpose |
|---|---|
| `GET /api/auth/webauthn/status` | Public capability, package, accepted origin, and origin allowlist |
| `POST /api/auth/webauthn/login/options` | Start assertion ceremony |
| `POST /api/auth/webauthn/login/verify` | Verify signed assertion and issue session on success |
| `POST /api/auth/webauthn/register/options` | Authenticated registration challenge |
| `POST /api/auth/webauthn/register/verify` | Verify and store public key for the signed-in account |

Do not log or persist challenge payloads, signed credential JSON, bearer tokens, or report-ticket values. See backend [WebAuthn API/security details](https://github.com/Etherealmber/atlas-commerce/blob/main/docs/webauthn.md).

## 6. Release signing and distribution

For a local signed build, create a release keystore and keep it outside Git. Put only the required `storeFile`, `storePassword`, `keyAlias`, and `keyPassword` entries in an ignored `keystore.properties`; keep backups of the keystore and credentials in an approved password manager. For Play distribution, configure Play App Signing and use the **Play app-signing certificate** for assetlinks/WebAuthn, not merely the upload certificate.

For GitHub Actions releases, use the repository's manual Android CI workflow and set protected secrets (`KEYSTORE_BASE64`, `KEYSTORE_PROPERTIES` or individual signing secrets, `GOOGLE_SERVICES_JSON_BASE64` when Firebase is used, and the backend's upload token where configured). Never print secret values in logs. Review [Android CI workflow](../.github/workflows/android.yml) and the README maintainer guide before publishing. Version code must increase for every Play release.

Before release, inspect **GitHub Repository → Security → Dependabot alerts**, triage every open high/critical dependency alert, and either upgrade or document a time-bound mitigation. Also verify Cloudflare Worker secrets, Firebase project membership, and Play signing access remain limited to the maintainers who need them.

## 7. First-run checklist and troubleshooting

- Worker is HTTPS and `/api/health` reports D1 and required storage ready.
- Production database is separate from local/preview; migrations are applied and backups are configured.
- Admin can sign in on the web; OTP e-mail works where required.
- Android's selected server is exactly the deployed Worker URL and connection test succeeds.
- User role is least-privilege; test sales/inventory on a non-production account first.
- Firebase is optional; a missing config keeps push and crash uploading off. Automatic crash uploads require an explicit in-app opt-in.
- Passkeys are hidden unless every selected-server/package/certificate/origin gate passes. Do not enable until the physical-device checklist above is complete.
- Never diagnose a production outage by copying a session token or customer/financial data into an issue. Use the app's redacted diagnostic-report flow and remove sensitive screenshots first.

For URL or API errors, open `https://YOUR_DOMAIN/api/health` in a browser and check Cloudflare Worker logs. For database failures, inspect D1 migration status and bindings. For passkey failures, compare status JSON, signing-certificate fingerprint, `assetlinks.json`, device-produced origin, and the backend verifier's exact-origin rules before changing code.
