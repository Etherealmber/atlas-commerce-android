# Security Policy — Atlas Commerce (Android)

© 2026 Etherealmber — MIT — current app baseline: **1.1.2** (versionCode 112); use the [Releases page](https://github.com/Etherealmber/atlas-commerce-android/releases) for the latest published build.

This document covers the **Android client** in this repository. The server (Cloudflare Worker,
D1, R2) has its own policy in
[atlas-commerce/SECURITY.md](https://github.com/Etherealmber/atlas-commerce/blob/main/SECURITY.md).

## Supported versions

| Version | Supported |
| --- | --- |
| 1.0.x | :white_check_mark: |
| 0.x / unreleased branches | :x: |

Only the latest release on `main` receives security fixes. Debug builds are development artifacts
and are not supported for production use.

## Reporting a vulnerability

- Email **security@afmarbre.com** (or contact the maintainer, [@Etherealmber](https://github.com/Etherealmber),
  privately on GitHub).
- Acknowledgement within **48 h**, status update every 7 days.
- Please include: app version, Android version, the server used (yours or production), a minimal
  reproduction, and whether the issue is client-only or server-side.
- **Do not** open a public issue for an exploit. Accepted reports are fixed on a private branch and
  shipped with a version bump; declined reports receive a written rationale.

## Security model of the Android app

### Credentials and sessions

- Passwords are sent only over HTTPS to the selected server and are **never persisted**.
- Login may return `needs_otp`; the app completes `/api/auth/verify-otp` with the 6-digit code.
  The worker issues the OTP; the app cannot bypass or forge it.
- Session tokens are bound by the worker to `(IP, device fingerprint)`. The fingerprint is a
  SHA-256 digest of a locally generated install UUID, `ANDROID_ID` and the package name — raw
  identifiers never leave the device and the digest is not reversible.
- The token is kept in memory for requests and stored **encrypted at rest** with AES-256-GCM using
  a non-exportable Android Keystore key (`atlas.session.v1`). If a device cannot use the Keystore,
  the app falls back to app-private storage so a user is never locked out; the backup system is
  disabled (`allowBackup=false`) either way.
- Logout clears the token, the cart and the offline catalogue; sessions are never shared between
  servers, and changing the server forces a fresh login.

### Passkeys / WebAuthn

- Passkey UI is fail-closed and scoped to the selected Worker. It requires `data.enabled`,
  `data.android.enabled`, the exact application ID, the server's exact accepted signing origin to
  match the installed APK certificate, and the selected web origin to be listed in `data.origins`.
- Android uses Credential Manager with the server challenge and selected HTTPS origin; the request
  uses `CREDENTIAL_MANAGER_SET_ORIGIN` so the Worker can validate the web origin, `crossOrigin`,
  and Android package together. The server performs cryptographic registration/assertion verification;
  the app does not create a session until login verification succeeds.
- Registration endpoints are called only from an existing authenticated session. Challenges and
  credential JSON are not stored in app preferences or logs. Password/OTP remains the fallback.
- Keep the backend disabled until the Play signing certificate, `assetlinks.json`, both origins,
  and physical-device staging test are verified. See [the passkey enablement guide](https://github.com/Etherealmber/atlas-commerce/blob/main/docs/webauthn.md)
  and the client [setup guide](docs/SETUP.md#5-optional-android-passkeys--keep-disabled-until-the-full-smoke-test-passes).

### Transport

- Release builds set `android:usesCleartextTraffic="false"` and refuse `http://` server addresses.
- `HttpLoggingInterceptor` is limited to `BASIC` and redacts `Authorization`, and only runs in
  debug builds.
- TLS verification uses the platform trust store (no custom `TrustManager`, no hostname bypass).

### Data stored on the device

| Data | Where | Purpose |
| --- | --- | --- |
| Encrypted session token | DataStore (app-private) | Stay signed in |
| Username, role, display name, email | DataStore | Show the profile without a request |
| Server address, theme, density | DataStore | Settings |
| Last product catalogue | App-private files (`atlas-cache/products.json`) | Offline browsing |
| Install UUID | SharedPreferences (app-private) | Stable device fingerprint |
| Downloaded finance-report PDFs | App-private cache (`cache/finance`) | Open/share monthly combined reports; automatically pruned after 7 days and deletable in Settings |
| Last sanitized uncaught-crash trace | App-private cache (`cache/diagnostics/last_crash.txt`) | Included only if the user later submits an in-app diagnostic report |
| Crash-report consent | App-private SharedPreferences | Remembers the user's explicit Firebase Crashlytics opt-in/out |

The app does not maintain a local accounting database or cache server-side customer records. A finance PDF exists temporarily in private cache only when the user downloads a report; the user can open/share it or delete cached PDFs from Settings. Camera images are not stored unless explicitly attached to a user-submitted issue report.

### Permissions

| Permission | Why |
| --- | --- |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Talk to your Atlas server |
| `CAMERA` | Decode barcodes on-device with ML Kit (no frame is uploaded or saved) |
| `POST_NOTIFICATIONS` | Display live progress notifications while downloading an APK update |
| `REQUEST_INSTALL_PACKAGES` | Hand downloaded `.apk` updates to Android's system package installer via `FileProvider` |
| `CREDENTIAL_MANAGER_SET_ORIGIN` | Let Credential Manager sign requests for the selected, allowlisted Worker origin; server also verifies the Android package and exact origin |

No location, contacts, microphone, storage or advertising permissions are requested.

### Third parties

There is no advertising SDK and no Firebase Analytics dependency. Firebase is an optional build-time provider for FCM push and Firebase Crashlytics. **Automatic Crashlytics collection is off by default in the manifest and remains off until the user opts in under Settings**; opting out disables future automatic uploads. If opted in and the build has a valid Firebase Android configuration, Crashlytics can upload an uncaught crash report (stack trace, app/device/OS version, timestamp and Firebase installation identifiers) automatically when connectivity permits. The app does not attach session tokens, passwords, OTPs, financial records, or its redacted in-app log buffer as Crashlytics custom data. Firebase provider configuration and the exact consent flow are documented in [docs/SETUP.md](docs/SETUP.md#4-optional-push-notifications-and-automatic-crash-reports-firebase).

ML Kit barcode decoding runs on-device with the bundled model; per Google's ML Kit terms the SDK may report its own usage/performance metrics (app version, installation and device identifiers — never images or scanned codes). Network calls for business data go only to the selected Atlas Worker; Crashlytics/FCM use Google's Firebase services only when configured. Opening GitHub or e-mail links hands control to the user's browser/mail app.

## Hardening checklist for self-hosters

- Serve the worker over HTTPS (`https://`). Plain HTTP is only for debug builds on an emulator.
- Use a strong administrator password and enable the web app's 2FA / passkeys.
- Keep `TURNSTILE_SECRET_KEY`, `ADMIN_PASSWORD` and e-mail secrets in Worker secrets, never in git.
- Keep the app updated: the client follows the server API and security fixes ship in releases.
- If a device is lost, revoke it from the web app's device manager; the worker will reject the
  token even though a copy exists on the device.

## Out of scope

- Compromised Android devices (root, malware, screen readers) and modified builds.
- Attacks that require the server to be malicious — self-hosting means you trust your operator.
- Rate-limit or account-lockout behaviour, which is enforced server-side.
