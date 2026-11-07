# Android certificate-pinning policy for hosted and custom servers

## Goal and compatibility boundary

Pin only the Atlas-operated production API host (`stock.afmarbre.com`). A user-selected or self-hosted server must not inherit the production host's pins: it continues to use Android's normal hostname and system-CA validation, so changing or rotating a custom server's certificate does not break server selection or sign-in. Do not redirect custom-server traffic to the hosted API and do not send its credentials to the pin-policy service.

This is a deployment design, not an active pin configuration. No pin values or signing key are checked into the Android source until the service operator provisions and verifies them.

## Why the policy must be signed

Fetching pins over the same TLS connection that the pins are intended to secure is not sufficient. A network attacker could replace an unsigned response. The Android app must contain a public verification key; the server keeps the matching signing private key in its secret manager/HSM. The private key must never be placed in this repository or returned to clients.

Use a detached `SHA256withECDSA` signature over a documented canonical representation of the policy. Verify the signature before parsing/applying any host or pin values. The first policy request is anonymous and carries no session token; it may use ordinary Android TLS to bootstrap the signed policy. A tampered response is rejected, while a blocked request can only deny service, not install attacker pins.

## Backend contract

Provide an anonymous, rate-limited endpoint on the first-party API, for example `GET /api/mobile/security/pin-policy`. It returns a versioned document containing:

- a monotonically increasing policy version;
- the exact allowed host `stock.afmarbre.com` (no wildcard, URL, port, or redirect host);
- at least two SHA-256 SPKI pins: the active key and a tested backup/next key;
- `notBefore` and `expiresAt` timestamps;
- a signing-key identifier and detached signature.

The backend obtains pin values from deployment configuration controlled by the TLS/service operator. It must not calculate and publish whatever certificate it happens to observe at request time. Validate the policy at deploy time: correct host, at least two well-formed pins, reasonable validity window, and signature from the active signing key.

## Android client behavior

1. Fetch and verify this policy before making authenticated requests to the first-party API. Keep policy retrieval anonymous; do not include the Atlas session token.
2. Cache only a successfully verified policy. Construct a first-party OkHttp client with `CertificatePinner` entries for the exact hosted API hostname, and use it for that hostname only.
3. If there is no valid signed policy for the first-party host, fail closed for first-party authenticated traffic and show a localized, actionable connection/update notice. Never silently fall back to an unpinned connection for that host.
4. For any user-selected custom server, do not apply the first-party pin policy. Preserve the existing normal TLS hostname/system-CA validation and existing custom-server flow. An optional future custom-server pin can be administrator-entered and confirmed out of band; do not trust a pin delivered by that same custom server without a separate trust anchor.
5. Do not pin GitHub/CDN update mirrors under this policy. They are separate hosts, may redirect, and their trust/integrity model should be addressed independently (the updater already checks the APK package/signing identity and advertised digest).

## Rotation and recovery

Before changing the production TLS key, publish a newly signed policy containing both the currently active and next SPKI pins, allow clients to receive it, then rotate the TLS key. Keep the old pin during the overlap window. Keep a tested backup key and an operator runbook for emergency policy publication. Rotate the policy-signing key only through a separately authenticated key-rotation mechanism rooted in a public key already trusted by the app; never let an ordinary TLS response replace that root key.

## Required rollout work

The Android client and the backend repository must be updated together. The Android repository needs the verified public signing key and policy-verification implementation; the backend needs the endpoint, private signing key in its secret store, and TLS pin inventory/rotation ownership. Until those are supplied and independently verified, retain normal CA validation rather than shipping fabricated pins or an unsigned dynamic pin list.
