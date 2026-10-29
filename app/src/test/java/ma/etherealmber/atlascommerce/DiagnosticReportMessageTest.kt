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
            category = "Authentication",
            title = "Sign-in failed",
            description = "Could not complete sign in.",
            stepsToReproduce = "Tap sign in.",
            snapshot = snapshot,
            logs = (1..200).joinToString("\n") { "AUTH event=$it" },
        )

        assertTrue(message.length <= 2_000)
        assertTrue(message.contains("Sign-in failed"))
        assertTrue(message.contains("AUTH event=200"))
        assertFalse(message.contains("AUTH event=1\n"))
    }

    @Test
    fun `short Worker report preserves crash trace before recent log tail`() {
        val logs = buildString {
            appendLine("=== LAST CRASH TRACE ===")
            appendLine("java.lang.IllegalStateException: CRASH_TRACE_ROOT_CAUSE")
            appendLine("at ma.atlas.checkout.Screen(Checkout.kt:42)")
            appendLine()
            appendLine("=== APP EVENTS / NETWORK / AUTH (200) ===")
            (1..200).forEach { appendLine("AUTH event=$it") }
        }

        val message = DiagnosticReporter.buildUserReportMessage(
            kind = "bug",
            category = "Crash",
            title = "App closed unexpectedly",
            description = "The app crashed.",
            stepsToReproduce = "Open checkout.",
            snapshot = snapshot,
            logs = logs,
        )

        assertTrue(message.length <= 2_000)
        assertTrue(message.contains("=== LAST CRASH TRACE ==="))
        assertTrue(message.contains("CRASH_TRACE_ROOT_CAUSE"))
        assertTrue(message.contains("AUTH event=200"))
    }

    @Test
    fun `log sanitizer removes bearer tokens JWTs email and report tickets`() {
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature0123456789"
        val raw = "Authorization: Bearer superSecret123 rt=reportTicket456 jwt=$jwt user=person@example.com ip=192.168.1.55 mac=aa:bb:cc:dd:ee:ff ipv6=2001:db8::1 time=12:34:56"
        val clean = AtlasLogBuffer.sanitize(raw)

        assertFalse(clean.contains("superSecret123"))
        assertFalse(clean.contains("reportTicket456"))
        assertFalse(clean.contains(jwt))
        assertFalse(clean.contains("person@example.com"))
        assertFalse(clean.contains("192.168.1.55"))
        assertFalse(clean.contains("aa:bb:cc:dd:ee:ff"))
        assertFalse(clean.contains("2001:db8::1"))
        assertTrue(clean.contains("12:34:56"))
    }
}
