package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.diagnostics.DeviceDiagnosticSnapshot
import ma.etherealmber.atlascommerce.core.diagnostics.DiagnosticReporter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticReportMessageTest {
    private val snapshot = DeviceDiagnosticSnapshot(
        reportId = "DIAG-20261026-test",
        timestampUtc = "2026-10-26T00:00:00Z",
        deviceShortId = "test",
        maskedFingerprint = "12345678…abcd",
        appVersion = "v1.1.6",
        versionCode = 116,
        manufacturerModel = "Test Phone",
        androidVersion = "Android 16 (SDK 36)",
        cpuAbi = "arm64-v8a",
        screenMetrics = "1080x2400px · 420dpi",
        memoryUsage = "100 Mo / 512 Mo",
        networkStatus = "Wi-Fi actif",
        userRole = "Utilisateur",
        screenOrigin = "https://stock.afmarbre.com",
    )

    @Test
    fun `user report stays within Worker limit and retains latest diagnostic lines`() {
        val message = DiagnosticReporter.buildUserReportMessage(
            kind = "bug",
            category = "Passkey",
            title = "Credential Manager failed",
            description = "Could not complete sign in.",
            stepsToReproduce = "Tap passkey sign in.",
            snapshot = snapshot,
            logs = (1..200).joinToString("\n") { "PASSKEY event=$it" },
        )

        assertTrue(message.length <= 2_000)
        assertTrue(message.contains("Credential Manager failed"))
        assertTrue(message.contains("PASSKEY event=200"))
        assertFalse(message.contains("PASSKEY event=1\n"))
    }

    @Test
    fun `log sanitizer removes bearer tokens JWTs email and report tickets`() {
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature0123456789"
        val raw = "Authorization: Bearer superSecret123 rt=reportTicket456 jwt=$jwt user=person@example.com"
        val clean = AtlasLogBuffer.sanitize(raw)

        assertFalse(clean.contains("superSecret123"))
        assertFalse(clean.contains("reportTicket456"))
        assertFalse(clean.contains(jwt))
        assertFalse(clean.contains("person@example.com"))
    }
}
