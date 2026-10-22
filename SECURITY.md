# Security Policy — Atlas Commerce (Android)

© 2026 loxdpsy — MIT — current release: **1.0.5** (versionCode 105)

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

- Email **security@afmarbre.com** (or contact the maintainer, [@loxdpsy](https://github.com/loxdpsy),
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

No financial documents, no invoices, no customer databases and no images are stored locally.

### Permissions

| Permission | Why |
| --- | --- |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Talk to your Atlas server |
| `CAMERA` | Decode barcodes on-device with ML Kit (no frame is uploaded or saved) |
| `POST_NOTIFICATIONS` | Display live progress notifications while downloading an APK update |
| `REQUEST_INSTALL_PACKAGES` | Hand downloaded `.apk` updates to Android's system package installer via `FileProvider` |

No location, contacts, microphone, storage or advertising permissions are requested.

### Third parties

There is **no analytics, telemetry, crash reporting or advertising SDK**. Network calls go only to
the server you chose. ML Kit barcode decoding runs on-device with the bundled model; per Google's
ML Kit terms the SDK also reports its own usage/performance metrics (app version, installation and
device identifiers — never images or scanned codes). Opening GitHub or e-mail links hands control
to the user's browser/mail app.

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
