package ma.etherealmber.atlascommerce.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DeviceIdentityTest {
    @Test
    fun `keystore decrypt failure keeps ciphertext and never returns an authenticated fallback`() {
        val ciphertext = "v2:iv:body"
        val writes = mutableListOf<String>()
        try {
            resolveDeviceInstallId(
                stored = ciphertext,
                decryptAtRest = { null },
                encryptAtRest = { "encrypted:$it" },
                generate = { error("must not generate a new identity when ciphertext exists") },
                persist = { writes.add(it) },
            )
            fail("identity must remain unavailable until its original value can be decrypted")
        } catch (_: DeviceIdentityUnavailableException) {
            // Expected: never substitute a different authenticated fingerprint.
        }
        assertTrue(writes.isEmpty())
    }

    @Test
    fun `legacy install identity migrates without changing its value`() {
        val writes = mutableListOf<String>()
        val identity = resolveDeviceInstallId(
            stored = "legacy-install-id",
            decryptAtRest = { null },
            encryptAtRest = { "v2:$it" },
            generate = { error("legacy identity must be retained") },
            persist = { writes.add(it) },
        )

        assertEquals("legacy-install-id", identity)
        assertEquals(listOf("v2:legacy-install-id"), writes)
    }

    @Test
    fun `decryption retry recovers the original install identity without caching failure`() {
        val ciphertext = "v2:iv:body"
        var decryptCalls = 0
        val identity by lazy {
            resolveDeviceInstallId(
                stored = ciphertext,
                decryptAtRest = {
                    decryptCalls++
                    if (decryptCalls == 1) null else "original-install-id"
                },
                encryptAtRest = { "unused" },
                generate = { error("must retain ciphertext") },
                persist = {},
            )
        }

        try {
            identity
            fail("first transient decryption failure must not return a fallback")
        } catch (_: DeviceIdentityUnavailableException) {
            // A subsequent identity access retries because Kotlin lazy does not cache exceptions.
        }
        assertEquals("original-install-id", identity)
        assertEquals(2, decryptCalls)
    }
}
