package ma.etherealmber.atlascommerce.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.Base64
import java.security.MessageDigest

/** Computes the native Credential Manager origin for the currently installed signing certificate(s). */
object AndroidPasskeyOrigin {
    private const val PREFIX = "android:apk-key-hash:"

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
        certificateBytes.mapNotNull(::fromCertificateDer).toSet()
    }.getOrDefault(emptySet())

    /** Standard native Android origin: URL-safe, unpadded Base64(SHA-256(certificate DER)). */
    internal fun fromCertificateDer(certificateDer: ByteArray): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256").digest(certificateDer)
        PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }.getOrNull()
}
