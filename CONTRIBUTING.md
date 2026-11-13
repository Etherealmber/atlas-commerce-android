# Contributing to Atlas Commerce for Android

Thanks for helping improve Atlas Commerce. The Android app is designed to work with user-selected, self-hosted Atlas servers; please preserve that compatibility in code, tests, and documentation.

## Before opening a pull request

1. Read the [README](README.md), [Android security policy](SECURITY.md), and [setup guide](docs/SETUP.md).
2. Keep changes focused and explain user-visible behavior, security impact, and any backend coordination in the pull-request description.
3. Add or update tests for behavior changes. Do not weaken authentication, OTP checks, TLS verification, tenant isolation, privacy, or localization to make a test pass.
4. Do not commit credentials, tokens, production data, or generated build artifacts. Never include secrets in logs or diagnostic messages.
5. Keep the app version unchanged except as part of an explicitly authorized release change.

## Build and test

Use Android Studio's supported JDK (17 or newer) and Android SDK 35. From the repository root, run:

```bash
./gradlew test
```

For focused Android unit tests, use the relevant task, for example:

```bash
./gradlew :app:testDebugUnitTest --tests 'ma.etherealmber.atlascommerce.core.session.FreshProcessStartupTest'
```

For an APK build, run `./gradlew :app:assembleDebug`. In the pull request, report commands that could not run and why; do not claim a check passed if it was unavailable.

## Security and self-hosting requirements

- Release networking must use HTTPS and Android's normal certificate-chain and hostname validation. Do not add custom trust managers, permissive hostname verification, or release cleartext exceptions.
- **Certificate pinning is intentionally not used.** Do not add OkHttp `CertificatePinner` rules, Android Network Security Config `<pin-set>` entries, or a custom-server pin preference. Users choose independently operated server domains and certificates; pinning would make their deployments fragile during certificate renewal or provider/key changes. The related CodeQL findings are accepted as **Won't fix by design**; this does not disable platform TLS validation.
- Finance-report WebViews must keep both `allowContentAccess` and `allowFileAccess` disabled. Preserve the existing same-origin navigation restrictions and other WebView hardening.
- Keep login and OTP behavior server-authoritative. A device fingerprint alone must never bypass OTP or authorize a session.
- Avoid logging request credentials, bearer tokens, OTPs, QR-session tokens, or private customer data. Redact sensitive path/query material before it enters diagnostics.

See [docs/android-certificate-pinning-policy.md](docs/android-certificate-pinning-policy.md) for the no-pinning decision and review guidance. Security vulnerabilities should be reported privately using [SECURITY.md](SECURITY.md), not as public issues.
