package ma.etherealmber.atlascommerce.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceIdentityTest {
    @Test
    fun `keystore decrypt failure retains stable fallback without overwriting ciphertext`() {
        val ciphertext = "v2:iv:body"
        val writes = mutableListOf<String>()
        val unavailable = resolveDeviceInstallId(
            stored = ciphertext,
            decryptAtRest = { null },
            encryptAtRest = { "encrypted:$it" },
            generate = { error("must not generate a new identity when ciphertext exists") },
            persist = { writes.add(it) },
        )
        val nextProcess = resolveDeviceInstallId(
            stored = ciphertext,
            decryptAtRest = { null },
            encryptAtRest = { "encrypted:$it" },
            generate = { error("must not generate a new identity when ciphertext exists") },
            persist = { writes.add(it) },
        )

        assertEquals(unavailable, nextProcess)
        assertTrue(unavailable.startsWith("unavailable:"))
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
    fun `a later successful decrypt recovers the original install identity`() {
        val ciphertext = "v2:iv:body"
        val unavailable = resolveDeviceInstallId(
            stored = ciphertext,
            decryptAtRest = { null },
            encryptAtRest = { "unused" },
            generate = { error("must retain ciphertext") },
            persist = {},
        )
        val recovered = resolveDeviceInstallId(
            stored = ciphertext,
            decryptAtRest = { "original-install-id" },
            encryptAtRest = { "unused" },
            generate = { error("must retain ciphertext") },
            persist = {},
        )

        assertNotEquals(unavailable, recovered)
        assertEquals("original-install-id", recovered)
    }
}
