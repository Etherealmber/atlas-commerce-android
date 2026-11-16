package ma.etherealmber.atlascommerce.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MobilePreferenceFailureParsingTest {
    @Test fun extractsConflictRevisionWithoutTurningErrorIntoSettings() {
        val parsed = parseMobilePreferenceError("""{"code":"CONFLICT","revision":12,"server_time":"2026-10-21T00:00:00Z"}""")
        assertEquals("CONFLICT", parsed.code)
        assertEquals(12L, parsed.revision)
        assertEquals("2026-10-21T00:00:00Z", parsed.serverTime)
    }

    @Test fun cooldownPrefersBoundedBodyThenRetryAfterHeader() {
        assertEquals(301L, parseMobilePreferenceError("""{"retry_after_seconds":301}""", "10").retryAfterSeconds)
        assertEquals(10L, parseMobilePreferenceError("{}", "10").retryAfterSeconds)
        assertNull(parseMobilePreferenceError("{}", "999999").retryAfterSeconds)
    }

    @Test fun invalidErrorBodyCannotInventRevisionOrRetryTime() {
        val parsed = parseMobilePreferenceError("not-json", "-1")
        assertNull(parsed.revision)
        assertNull(parsed.retryAfterSeconds)
        assertNull(parsed.serverTime)
    }
}
