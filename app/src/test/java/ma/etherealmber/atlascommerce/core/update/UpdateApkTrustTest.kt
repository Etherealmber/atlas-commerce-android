package ma.etherealmber.atlascommerce.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateApkTrustTest {

    @Test
    fun `accepts exact installed package and signer`() {
        assertTrue(
            isTrustedUpdateApkIdentity(
                expectedPackageName = "ma.etherealmber.atlascommerce",
                candidatePackageName = "ma.etherealmber.atlascommerce",
                candidateCurrentSigners = listOf(byteArrayOf(1, 2, 3)),
                candidateSigningHistory = listOf(byteArrayOf(1, 2, 3)),
                installedCurrentSigners = listOf(byteArrayOf(1, 2, 3)),
            ),
        )
    }

    @Test
    fun `accepts forward rotation only when candidate lineage contains installed signer`() {
        assertTrue(
            isTrustedUpdateApkIdentity(
                expectedPackageName = "ma.etherealmber.atlascommerce",
                candidatePackageName = "ma.etherealmber.atlascommerce",
                candidateCurrentSigners = listOf(byteArrayOf(4, 5, 6)),
                candidateSigningHistory = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6)),
                installedCurrentSigners = listOf(byteArrayOf(1, 2, 3)),
            ),
        )
    }

    @Test
    fun `rejects another package unrelated signer and partial multisigner match`() {
        val signer = byteArrayOf(1, 2, 3)
        assertFalse(
            isTrustedUpdateApkIdentity(
                expectedPackageName = "ma.etherealmber.atlascommerce",
                candidatePackageName = "attacker.package",
                candidateCurrentSigners = listOf(signer),
                candidateSigningHistory = listOf(signer),
                installedCurrentSigners = listOf(signer),
            ),
        )
        assertFalse(
            isTrustedUpdateApkIdentity(
                expectedPackageName = "ma.etherealmber.atlascommerce",
                candidatePackageName = "ma.etherealmber.atlascommerce",
                candidateCurrentSigners = listOf(byteArrayOf(9)),
                candidateSigningHistory = listOf(byteArrayOf(9)),
                installedCurrentSigners = listOf(signer),
            ),
        )
        assertFalse(
            isTrustedUpdateApkIdentity(
                expectedPackageName = "ma.etherealmber.atlascommerce",
                candidatePackageName = "ma.etherealmber.atlascommerce",
                candidateCurrentSigners = listOf(signer, byteArrayOf(8)),
                candidateSigningHistory = listOf(signer, byteArrayOf(8)),
                installedCurrentSigners = listOf(signer),
            ),
        )
    }
}
