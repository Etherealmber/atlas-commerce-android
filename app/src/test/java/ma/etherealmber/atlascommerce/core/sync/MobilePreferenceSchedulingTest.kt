package ma.etherealmber.atlascommerce.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ma.etherealmber.atlascommerce.core.persist.preserveLocalAppearanceSettings

class MobilePreferenceSchedulingTest {
    @Test fun workDelayUsesOnlyTheReturnedServerTimePair() {
        assertEquals(86_400_000L, serverRelativeDelayMillis("2026-10-21T00:00:00Z", "2026-10-22T00:00:00Z"))
        assertEquals(0L, serverRelativeDelayMillis("2026-10-22T00:00:00Z", "2026-10-21T00:00:00Z"))
        assertNull(serverRelativeDelayMillis(null, "2026-10-22T00:00:00Z"))
        assertNull(serverRelativeDelayMillis("not-a-time", "2026-10-22T00:00:00Z"))
    }

    @Test fun backgroundWorkIdentityIsServerAndAccountScoped() {
        val accountOne = mobileScopeHash("https://one.example|alice|owner")
        val accountTwo = mobileScopeHash("https://one.example|bob|owner")
        val otherServer = mobileScopeHash("https://two.example|alice|owner")
        assertNotEquals(accountOne, accountTwo)
        assertNotEquals(accountOne, otherServer)
        assertTrue(accountOne.matches(Regex("[a-f0-9]{24}")))
    }

    @Test fun serverScopedPreferenceHydrationCannotReplaceLocalThemeOrLanguage() {
        val remote = mapOf<String, Any?>(
            "theme" to "dark", "language" to "fr", "density" to "compact",
        )
        val local = mapOf<String, Any?>(
            "theme" to "light", "language" to "en",
        )

        val merged = preserveLocalAppearanceSettings(remote, local)

        assertEquals("light", merged["theme"])
        assertEquals("en", merged["language"])
        assertEquals("compact", merged["density"])
    }
}
