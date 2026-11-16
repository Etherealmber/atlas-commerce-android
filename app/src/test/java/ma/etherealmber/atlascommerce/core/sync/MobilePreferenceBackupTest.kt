package ma.etherealmber.atlascommerce.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MobilePreferenceBackupTest {
    @Test fun roundTripsOnlyTheSettingsAllowlist() {
        val json = encodeMobilePreferenceBackup(settings)
        val decoded = decodeMobilePreferenceBackup(json)!!
        assertEquals(settings, decoded.settings)
        assertTrue(json.contains("schema_version"))
        assertTrue(!json.contains("account") && !json.contains("token") && !json.contains("fingerprint"))
    }

    @Test fun malformedUnknownAndOversizedBackupsAreRejected() {
        assertNull(decodeMobilePreferenceBackup("{}"))
        assertNull(decodeMobilePreferenceBackup(encodeMobilePreferenceBackup(settings).replace("\"system\"", "\"neon\"")))
        assertNull(decodeMobilePreferenceBackup("{\"schema_version\":1,\"settings\":{},\"saved_accounts\":[]}"))
        assertNull(decodeMobilePreferenceBackup(" ".repeat(MOBILE_PREFERENCE_BACKUP_MAX_BYTES + 1)))
    }

    @Test fun importMergeChangesOnlyProvidedAllowedFieldsAndKeepsTheRest() {
        val partial = mapOf("theme" to "dark")
        val merged = mergeMobilePreferenceBackup(settings, partial + settings.filterKeys { it != "theme" })!!
        assertEquals("dark", merged["theme"])
        assertEquals(settings["language"], merged["language"])
    }

    private companion object {
        val settings = mapOf<String, Any?>(
            "theme" to "system", "language" to "system", "density" to "comfortable",
            "animations" to true, "navigation_default" to "dashboard", "scan_behavior" to "preview",
            "sale_confirmation" to true, "product_view" to "list", "update_notifications" to true,
            "update_channel" to "stable", "onboarding_completed" to false,
        )
    }
}
