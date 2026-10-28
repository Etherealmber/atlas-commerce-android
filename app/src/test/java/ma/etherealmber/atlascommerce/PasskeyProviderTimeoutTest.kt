package ma.etherealmber.atlascommerce

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ma.etherealmber.atlascommerce.core.security.awaitPasskeyProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PasskeyProviderTimeoutTest {
    @Test
    fun returnsCompletedProviderResult() = runBlocking {
        assertEquals("credential", awaitPasskeyProvider(1_000) { "credential" })
    }

    @Test
    fun timesOutAndCancelsAnUnresponsiveProvider() = runBlocking {
        var cancelled = false
        try {
            awaitPasskeyProvider(25) {
                try {
                    delay(10_000)
                } finally {
                    cancelled = true
                }
            }
            fail("Expected the provider to time out")
        } catch (_: TimeoutCancellationException) {
            assertTrue(cancelled)
        }
    }

    @Test
    fun preservesCancellation() = runBlocking {
        try {
            awaitPasskeyProvider(1_000) { throw CancellationException("cancelled") }
            fail("Expected cancellation")
        } catch (error: CancellationException) {
            assertEquals("cancelled", error.message)
        }
    }
    @Test
    fun userCancellationStopsWaitingWithoutContinuingToVerification() = runBlocking {
        val providerStarted = CompletableDeferred<Unit>()
        var providerCancelled = false
        var verified = false
        val job = launch {
            awaitPasskeyProvider(10_000) {
                try {
                    providerStarted.complete(Unit)
                    delay(10_000)
                } finally {
                    providerCancelled = true
                }
            }
            verified = true
        }
        providerStarted.await()
        job.cancelAndJoin()
        assertTrue(providerCancelled)
        assertTrue(!verified)
        assertTrue(job.isCancelled)
    }

}
