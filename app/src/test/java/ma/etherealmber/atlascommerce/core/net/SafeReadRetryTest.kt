package ma.etherealmber.atlascommerce.core.net

import java.io.IOException
import java.net.InetAddress
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeReadRetryTest {
    @Test
    fun retriesOnlyIdempotentReadMethods() {
        val failure = IOException("connection reset")

        assertTrue(shouldRetrySafeRead("GET", failure))
        assertTrue(shouldRetrySafeRead("HEAD", failure))
        assertTrue(shouldRetrySafeRead("OPTIONS", failure))
        assertFalse(shouldRetrySafeRead("POST", failure))
        assertFalse(shouldRetrySafeRead("PUT", failure))
        assertFalse(shouldRetrySafeRead("DELETE", failure))
    }

    @Test
    fun diagnosticDnsPreservesThePlatformResolverAnswersAndFailures() {
        val expectedAddresses = listOf(InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)))
        val delegate = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                assertEquals("tenant.example", hostname)
                return expectedAddresses
            }
        }
        assertSame(expectedAddresses, AtlasDiagnosticDns(delegate, logDiagnostic = { _, _, _ -> }).lookup("tenant.example"))

        val expectedFailure = UnknownHostException("resolver unavailable")
        val failingDelegate = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = throw expectedFailure
        }
        val actualFailure = runCatching { AtlasDiagnosticDns(failingDelegate, logDiagnostic = { _, _, _ -> }).lookup("tenant.example") }.exceptionOrNull()
        assertSame(expectedFailure, actualFailure)
    }

    @Test
    fun dnsAnswerSummaryExposesSyntheticClassWithoutRawAddress() {
        val synthetic = InetAddress.getByAddress(byteArrayOf(198.toByte(), 18, 0, 38))
        val summary = summarizeDiagnosticDnsAnswers(listOf(synthetic))

        assertEquals("benchmark:1", summary.classes)
        assertTrue(summary.hasNonGlobalAnswer)
        assertTrue(summary.hasSuspiciousAnswer)
        assertFalse(summary.classes.contains("198.18.0.38"))
    }

    @Test
    fun dnsDiagnosticsClassifySyntheticAndPrivateAnswersWithoutRetainingAddresses() {
        val synthetic = InetAddress.getByAddress(byteArrayOf(198.toByte(), 18, 0, 38))
        val privateAddress = InetAddress.getByAddress(byteArrayOf(192.toByte(), 168.toByte(), 1, 2))
        val uniqueLocalIpv6 = InetAddress.getByName("fd00::1")
        val publicAddress = InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1))

        assertEquals("benchmark", diagnosticAddressClass(synthetic))
        assertEquals("private", diagnosticAddressClass(privateAddress))
        assertEquals("private", diagnosticAddressClass(uniqueLocalIpv6))
        assertTrue(summarizeDiagnosticDnsAnswers(listOf(uniqueLocalIpv6)).hasNonGlobalAnswer)
        assertEquals("global", diagnosticAddressClass(publicAddress))
    }

    @Test
    fun diagnosticPathsRedactQrSessionTokensButKeepRouteContext() {
        assertEquals(
            "/api/auth/qr/session/[REDACTED]/claim",
            diagnosticPath("/api/auth/qr/session/opaque-session-token/claim"),
        )
        assertEquals("/api/auth/me", diagnosticPath("/api/auth/me"))
    }

    @Test
    fun networkDiagnosticsClassifyDnsConnectTlsAndProtocolFailuresWithoutMessages() {
        assertEquals("dns", networkFailureCategory(UnknownHostException("private host detail")))
        assertEquals("timeout", networkFailureCategory(SocketTimeoutException("private address detail")))
        assertEquals("tls", networkFailureCategory(SSLHandshakeException("certificate detail")))
        assertEquals("protocol", networkFailureCategory(ProtocolException("protocol detail")))
        assertEquals("io", networkFailureCategory(IOException("private detail")))
    }

    @Test
    fun neverRetriesTlsOrProtocolFailures() {
        assertFalse(shouldRetrySafeRead("GET", SSLHandshakeException("certificate rejected")))
        assertFalse(shouldRetrySafeRead("GET", ProtocolException("unexpected response")))
    }
}
