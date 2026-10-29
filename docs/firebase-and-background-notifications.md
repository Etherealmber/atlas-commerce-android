# Firebase Crashlytics, FCM and background work

## Crashlytics (Android)

Crashlytics does not need an Atlas Worker/Cloudflare backend integration. It receives Android crash reports through Firebase.

1. In Firebase Console, create/select the project, add the Android app with package `ma.etherealmber.atlascommerce`, and enable Crashlytics.
2. Download that app's `google-services.json`. Keep it out of Git. For a local build, place it at `app/google-services.json`.
3. For GitHub Actions builds, store the base64-encoded file as the Actions secret `GOOGLE_SERVICES_JSON_BASE64`. The Android workflow validates the package ID and applies Google Services + Crashlytics only when this file is present. A recent CI build reported Firebase config validated; an older APK may not include it.
4. Install a newly built APK and opt in under **Settings → Diagnostics → Envoyer automatiquement les rapports de plantage**. Collection is deliberately off until the user enables it. Check **Firebase Console → Crashlytics** after the next real crash and a subsequent app launch.

Cloudflare Worker exceptions are not Android Crashlytics events; review those in Cloudflare Worker logs/observability.

## Push notifications (FCM)

The Android app can receive pushes only if the APK has the matching Firebase config, notification permission is granted, the token is registered with the selected Atlas Worker, and that Worker actually sends an FCM message.

### Firebase / Cloudflare setup

1. In Google Cloud for the Firebase project, enable **Firebase Cloud Messaging API**.
2. Create a service account with permission to send FCM messages (Firebase Cloud Messaging API Admin), and keep the private key secret.
3. Set Worker secrets (never put these values in the Android app or Git):

   ```sh
   npx wrangler secret put FIREBASE_PROJECT_ID
   npx wrangler secret put FIREBASE_CLIENT_EMAIL
   npx wrangler secret put FIREBASE_PRIVATE_KEY
   ```

   `FIREBASE_PRIVATE_KEY` is the service-account PEM private key; the Worker accepts escaped `\n` line breaks. Deploy the Worker after setting secrets.

4. Sign in with a Firebase-configured APK and allow Android notifications. The app registers its FCM token using `POST /api/assist/device-token`; the Worker stores it in D1 table `device_push_tokens`.
5. Check Cloudflare Worker logs for `/api/assist/device-token`. A 500 means the token was **not** registered, so pushes cannot reach that device. The Android diagnostics now log the HTTP status and the sanitized Worker error without recording the FCM token.
6. Verify end-to-end by sending a test message to a registered token from Firebase Console. Then verify the Worker's `sendFcmToUser` calls for each business event.

### What runs in background

- The app now schedules a WorkManager update-manifest check about every 12 hours when **Recherche automatique des mises à jour** is enabled. Android may defer it for Doze/battery saving; it checks release metadata and posts an update alert, but it does not download or install an APK automatically.
- Payroll and employee records are not polled or cached in a background Android service. They are sensitive, permission-controlled data. For background alerts, the Worker must send a generic push to authorized recipients when a payroll reminder is due or an employee/action request needs approval/resolution. If a reminder is time-based, configure a Cloudflare Cron Trigger and implement the server-side authorized query + `sendFcmToUser` call. Do not include payroll amounts or employee private data in a push payload.
- The existing app's role-notification polling runs while the app is open; it is not a substitute for server FCM while the app is closed. The Worker must trigger pushes for the events you want (new approval request, approval/rejection, payroll reminder, employee change).

### Android / Xiaomi settings

Allow **Notifications** for Atlas Commerce and enable the `Assistant & notifications serveur`, `Notifications & alertes métier`, and update channels. Xiaomi may also require **Autostart** and **Battery → No restrictions** for reliable background delivery. Android/MIUI can still defer background work; an app cannot override the operating system's battery policy.

## Local device unlock

Quick sign-in is an opt-in convenience for a saved account. It uses the Android system prompt with biometrics and device PIN/password fallback, then verifies the encrypted session token with the selected Worker. If the prompt is unavailable or the quick action disappears, sign in manually and check that biometric authentication or a secure screen lock is configured. Atlas never stores the account password; do not weaken server session verification.
