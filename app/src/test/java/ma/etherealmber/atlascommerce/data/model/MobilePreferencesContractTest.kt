package ma.etherealmber.atlascommerce.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobilePreferencesContractTest {
    private fun payload(settings: Map<String, Any?> = validSettings()) = MobilePreferencesDto(
        schemaVersion = 1,
        settings = settings,
        revision = 0,
        updatedAt = null,
        serverTime = "2026-10-21T00:00:00.000Z",
        syncFrequency = "daily",
        lastSyncedAt = null,
        nextSyncAt = null,
    )

    @Test
    fun acceptsOnlyCompleteAllowlistedSettings() {
        assertTrue(payload().hasValidContractPayload())
        assertFalse(payload(validSettings() - "onboarding_completed").hasValidContractPayload())
        assertFalse(payload(validSettings() + ("saved_accounts" to true)).hasValidContractPayload())
        assertFalse(payload(validSettings() + ("animations" to "yes")).hasValidContractPayload())
    }

    @Test
    fun rejectsUnsupportedSchemaRevisionAndFrequency() {
        assertFalse(payload().copy(schemaVersion = 2).hasValidContractPayload())
        assertFalse(payload().copy(revision = -1).hasValidContractPayload())
        assertFalse(payload().copy(syncFrequency = "hourly").hasValidContractPayload())
        assertFalse(payload().copy(serverTime = "yesterday").hasValidContractPayload())
    }

    @Test
    fun successfulWriteMustAdvanceRevision() {
        assertTrue(payload().copy(revision = 1, lastSyncedAt = "2026-10-21T00:01:00Z")
            .isSuccessfulWriteAfter(previousRevision = 0))
        assertFalse(payload().isSuccessfulWriteAfter(previousRevision = 0))
        assertFalse(payload().copy(revision = 1).isSuccessfulWriteAfter(previousRevision = 1))
    }

    private companion object {
        fun validSettings(): Map<String, Any?> = mapOf(
            "theme" to "system",
            "language" to "system",
            "density" to "comfortable",
            "animations" to true,
            "navigation_default" to "dashboard",
            "scan_behavior" to "preview",
            "sale_confirmation" to true,
            "product_view" to "list",
            "update_notifications" to true,
            "update_channel" to "stable",
            "onboarding_completed" to false,
        )
    }
}
