# Android TLS policy: no certificate pinning

## Decision

Atlas Commerce intentionally does **not** implement certificate pinning. Users choose their own Atlas server, including self-hosted deployments on independently managed domains and certificate authorities. `CertificatePinner`, Android Network Security Config `<pin-set>` entries, and server-specific pin preferences must not be added to the Android client.

The CodeQL “missing certificate pinning” findings for the API and server-probe clients are accepted as **Won't fix by design**. Pinning would impose an additional trust requirement on a server selected by the user, and could break self-hosted installations during certificate changes, provider changes, or key rotation.

## TLS requirements remain in force

Not pinning does not mean disabling TLS checks. Release builds require HTTPS, Android's normal certificate-chain trust validation and hostname verification remain enabled, and the app must not add a custom `TrustManager`, permissive hostname verifier, or cleartext exception to work around an invalid certificate. A self-hosted server needs a valid certificate for the hostname users enter.

Do not add user-installed certificate authorities as a production trust anchor merely to make a self-hosted server connect. Operators should configure a certificate chain trusted by Android's normal platform trust store.

## Review guidance

When changing networking code, preserve the selected-server flow and normal platform TLS validation for every host. Do not hard-code pins for the production domain, do not copy pin values from an unverified issue or chat, and do not fetch unsigned pins from the server being pinned. Revisit this decision only through an explicit product/security decision that includes the self-hosting compatibility impact and a tested rollout/rotation plan.
