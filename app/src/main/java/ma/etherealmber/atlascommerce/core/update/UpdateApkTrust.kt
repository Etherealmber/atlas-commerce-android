package ma.etherealmber.atlascommerce.core.update

import java.security.MessageDigest

/**
 * Accept only the installed app's signing identity or a forward key rotation explicitly
 * attested by the candidate APK's signing lineage.
 */
internal fun isTrustedUpdateApkIdentity(
    expectedPackageName: String,
    candidatePackageName: String?,
    candidateCurrentSigners: List<ByteArray>,
    candidateSigningHistory: List<ByteArray>,
    installedCurrentSigners: List<ByteArray>,
): Boolean {
    if (candidatePackageName != expectedPackageName) return false
    if (candidateCurrentSigners.isEmpty() || installedCurrentSigners.isEmpty()) return false

    if (sameCertificateSet(candidateCurrentSigners, installedCurrentSigners)) return true

    // Android signing lineage supports forward rotation for a single-signer application.
    // Multi-signer sets must match exactly; accepting a partial set could broaden trust.
    if (candidateCurrentSigners.size != 1 || installedCurrentSigners.size != 1) return false
    val installedSigner = installedCurrentSigners.single()
    return candidateSigningHistory.any { certificate ->
        MessageDigest.isEqual(certificate, installedSigner)
    }
}

private fun sameCertificateSet(left: List<ByteArray>, right: List<ByteArray>): Boolean =
    left.size == right.size && left.all { certificate ->
        right.any { candidate -> MessageDigest.isEqual(certificate, candidate) }
    }
