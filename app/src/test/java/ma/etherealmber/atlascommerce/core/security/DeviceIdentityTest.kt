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
    fun `new identity is durably encrypted before it is returned`() {
        var stored: String? = null
        var generated = 0
        val firstLaunchIdentity = resolveDeviceInstallId(
            stored = stored,
            decryptAtRest = { null },
            encryptAtRest = { "v2:$it" },
            generate = { generated++; "stable-install-id" },
            persist = { stored = it; true },
        )
        val nextLaunchIdentity = resolveDeviceInstallId(
            stored = stored,
            decryptAtRest = { it?.removePrefix("v2:") },
            encryptAtRest = { error("existing identity must not be replaced") },
            generate = { error("a persisted identity must be reused") },
            persist = { true }, // Confirm that the encrypted identity remains durable before reuse.
        )

        assertEquals("stable-install-id", firstLaunchIdentity)
        assertEquals(firstLaunchIdentity, nextLaunchIdentity)
        assertEquals(1, generated)
        assertEquals("v2:stable-install-id", stored)
    }

    @Test
    fun `unconfirmed generated write is retried before using its in-memory ciphertext`() {
        var stored: String? = null
        var persistCalls = 0
        val persist: (String) -> Boolean = { encrypted ->
            stored = encrypted // SharedPreferences may expose this even when commit() fails.
            persistCalls++ > 0
        }

        try {
            resolveDeviceInstallId(
                stored = stored,
                decryptAtRest = { null },
                encryptAtRest = { "v2:$it" },
                generate = { "stable-install-id" },
                persist = persist,
            )
            fail("identity must not be used before its disk write is confirmed")
        } catch (_: DeviceIdentityUnavailableException) {
            // Retry persistence of the same encrypted value on the next read.
        }

        val retried = resolveDeviceInstallId(
            stored = stored,
            decryptAtRest = { it?.removePrefix("v2:") },
            encryptAtRest = { error("must retain existing encrypted identity") },
            generate = { error("must not replace an unconfirmed identity") },
            persist = persist,
        )

        assertEquals("stable-install-id", retried)
        assertEquals(2, persistCalls)
        assertEquals("v2:stable-install-id", stored)
    }

    @Test
    fun `identity is unavailable rather than ephemeral when secure persistence fails`() {
        var generated = 0
        try {
            resolveDeviceInstallId(
                stored = null,
                decryptAtRest = { null },
                encryptAtRest = { "v2:$it" },
                generate = { generated++; "not-persisted" },
                persist = { false },
            )
            fail("must not return an install identity that will change on next process launch")
        } catch (_: DeviceIdentityUnavailableException) {
            // Fail closed so no login can be bound to an identity that was not stored.
        }
        assertEquals(1, generated)
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
                persist = { true },
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
