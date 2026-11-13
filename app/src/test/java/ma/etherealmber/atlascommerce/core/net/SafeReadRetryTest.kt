package ma.etherealmber.atlascommerce.core.net

import java.io.IOException
import java.net.ProtocolException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.assertFalse
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
    fun neverRetriesTlsOrProtocolFailures() {
        assertFalse(shouldRetrySafeRead("GET", SSLHandshakeException("certificate rejected")))
        assertFalse(shouldRetrySafeRead("GET", ProtocolException("unexpected response")))
    }
}
