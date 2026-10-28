package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.core.diagnostics.PasskeyFailureStage
import ma.etherealmber.atlascommerce.core.diagnostics.passkeyAutomaticReport
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasskeyAutomaticReportTest {
    @Test
    fun reportContainsOnlyFixedFailureMetadata() {
        val report = passkeyAutomaticReport(PasskeyFailureStage.REGISTRATION_VERIFY, 403, 34)
        assertTrue(report.contains("REGISTRATION_VERIFY"))
        assertTrue(report.contains("HTTP status: 403"))
        assertTrue(report.contains("Android SDK: 34"))
        assertTrue(report.contains("@atlas-bot"))
        assertTrue(report.length <= 2000)
        assertFalse(report.contains("credentialJson"))
    }

    @Test
    fun invalidHttpStatusIsNotIncluded() {
        val report = passkeyAutomaticReport(PasskeyFailureStage.LOGIN_PROVIDER, 9999, 35)
        assertTrue(report.contains("HTTP status: not available"))
        assertFalse(report.contains("9999"))
    }
}
