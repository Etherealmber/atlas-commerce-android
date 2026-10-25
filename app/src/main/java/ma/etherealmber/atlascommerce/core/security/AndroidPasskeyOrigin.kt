package ma.etherealmber.atlascommerce.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import java.util.Base64

/** Computes the current and legacy Credential Manager origins for installed signing certificates. */
object AndroidPasskeyOrigin {
    private const val SHA256_PREFIX = "android:apk-key-hash-sha256:"
    private const val LEGACY_SHA1_PREFIX = "android:apk-key-hash:"

    fun forInstalledApp(context: Context): Set<String> = runCatching {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }
        val certificateBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.map { it.toByteArray() }.orEmpty()
        }
        certificateBytes.flatMapTo(mutableSetOf(), ::fromCertificateDer)
    }.getOrDefault(emptySet())

    /**
     * Return the canonical SHA-256 and historical SHA-1 origin strings for one certificate DER.
     * Both are compared with exact values advertised by the selected server; no origin is guessed.
     */
    internal fun fromCertificateDer(certificateDer: ByteArray): Set<String> = runCatching {
        val sha256 = MessageDigest.getInstance("SHA-256").digest(certificateDer)
        val sha1 = MessageDigest.getInstance("SHA-1").digest(certificateDer)
        setOf(
            SHA256_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(sha256),
            LEGACY_SHA1_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(sha1),
        )
    }.getOrDefault(emptySet())
}
