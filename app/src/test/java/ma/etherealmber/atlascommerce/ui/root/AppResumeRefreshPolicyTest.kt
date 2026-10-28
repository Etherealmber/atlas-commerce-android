package ma.etherealmber.atlascommerce.ui.root

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResumeRefreshPolicyTest {
    @Test fun refreshesInitiallyAndAfterTheThrottle() {
        assertTrue(shouldRefreshForegroundData(lastRefreshMs = 0, nowMs = 1, minimumIntervalMs = 45_000))
        assertFalse(shouldRefreshForegroundData(lastRefreshMs = 10_000, nowMs = 54_999, minimumIntervalMs = 45_000))
        assertTrue(shouldRefreshForegroundData(lastRefreshMs = 10_000, nowMs = 55_000, minimumIntervalMs = 45_000))
    }

    @Test fun monotonicClockResetDoesNotKeepDataStaleForever() {
        assertTrue(shouldRefreshForegroundData(lastRefreshMs = 80_000, nowMs = 1_000, minimumIntervalMs = 45_000))
    }
}
