package ma.etherealmber.atlascommerce.core.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.SessionEvents
import ma.etherealmber.atlascommerce.core.net.SessionGuardInterceptor
import ma.etherealmber.atlascommerce.core.net.isStartupResumeExcludedInterceptor
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.data.model.LoginResponse
import ma.etherealmber.atlascommerce.data.model.MeDto
import okhttp3.Interceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshProcessStartupTest {

    @Test
    fun `saved token is prepared only for the selected origin and is redacted in diagnostics`() {
        val plan = StartupResumePreparation.createRequest(
            previousSession = session(serverUrl = "https://shop.example.com/api"),
            selectedServerUrl = "https://shop.example.com:443",
            restoreDisabled = false,
            decryptToken = { "secret-session-proof" },
        )

        assertEquals(StartupResumeResult.PREPARED, plan.result)
        assertEquals("https://shop.example.com:443", plan.request?.serverUrl)
        assertTrue(plan.request.toString().contains("[REDACTED]"))
        assertFalse(plan.request.toString().contains("secret-session-proof"))

        val otherOrigin = StartupResumePreparation.createRequest(
            previousSession = session(serverUrl = "https://other.example.com"),
            selectedServerUrl = "https://shop.example.com",
            restoreDisabled = false,
            decryptToken = { error("Token must not be decrypted for another origin") },
        )
        assertEquals(StartupResumeResult.DIFFERENT_ORIGIN, otherOrigin.result)
        assertEquals(null, otherOrigin.request)
    }

    @Test
    fun `explicit sign out missing server and undecryptable session never create a request`() {
        assertEquals(
            StartupResumeResult.RESTORE_DISABLED,
            StartupResumePreparation.createRequest(session(), "https://shop.example.com", true) { "token" }.result,
        )
        assertEquals(
            StartupResumeResult.NO_SESSION,
            StartupResumePreparation.createRequest(null, "https://shop.example.com", false) { "token" }.result,
        )
        assertEquals(
            StartupResumeResult.NO_SELECTED_SERVER,
            StartupResumePreparation.createRequest(session(), null, false) { "token" }.result,
        )
        assertEquals(
            StartupResumeResult.UNDECRYPTABLE_TOKEN,
            StartupResumePreparation.createRequest(session(), "https://shop.example.com", false) { null }.result,
        )
    }

    @Test
    fun `session is restored only after the server validates the bearer token`() = runBlocking {
        val request = StartupResumePreparation.createRequest(
            session(), "https://shop.example.com", false,
        ) { "saved-token" }.request!!
        val profile = MeDto(ok = true, username = "alice", role = "admin")

        val result = StartupResumePreparation.verify(request) { server, token ->
            assertEquals("https://shop.example.com", server)
            assertEquals("saved-token", token)
            ApiResult.Success(profile)
        }

        assertEquals(StartupSessionVerification.Valid(profile), result)
    }

    @Test
    fun `revoked session is rejected and transient failures remain recoverable`() = runBlocking {
        val request = StartupResumePreparation.createRequest(
            session(), "https://shop.example.com", false,
        ) { "saved-token" }.request!!

        val rejected = StartupResumePreparation.verify(request) { _, _ ->
            ApiResult.Failure("SESSION_EXPIRED", "expired", 401)
        }
        assertEquals(StartupSessionVerification.Rejected, rejected)

        val unavailable = StartupResumePreparation.verify(request) { _, _ ->
            throw IllegalStateException("temporary network failure")
        }
        assertEquals(StartupSessionVerification.Unavailable, unavailable)

        val timeout = StartupResumePreparation.verify(request, timeoutMillis = 20) { _, _ ->
            delay(500)
            ApiResult.Success(MeDto(ok = true))
        }
        assertEquals(StartupSessionVerification.Unavailable, timeout)
    }

    @Test
    fun `restored startup keeps active state and releases startup gate without clearing session`() = runBlocking {
        val events = mutableListOf<String>()
        val startup = FreshProcessStartup(
            restorePreviousSession = {
                events += "restore"
                FreshProcessSessionOutcome.RESTORED
            },
            clearStoredSession = { events += "clear" },
            purgeActiveState = { events += "purge" },
            publishSignedOut = { events += "signed-out" },
            releaseStartupGate = { events += "gate" },
        )

        assertEquals(FreshProcessSessionOutcome.RESTORED, startup.run())
        assertEquals(listOf("restore", "gate"), events)
    }

    @Test
    fun `rejected session is cleared and publishes signed out`() = runBlocking {
        val events = mutableListOf<String>()
        val startup = FreshProcessStartup(
            restorePreviousSession = { FreshProcessSessionOutcome.REJECTED },
            clearStoredSession = { events += "clear" },
            purgeActiveState = { events += "purge" },
            publishSignedOut = { events += "signed-out" },
            releaseStartupGate = { events += "gate" },
        )

        assertEquals(FreshProcessSessionOutcome.REJECTED, startup.run())
        assertEquals(listOf("clear", "purge", "signed-out", "gate"), events)
    }

    @Test
    fun `network failure preserves encrypted session for a later retry but does not trust it offline`() = runBlocking {
        val events = mutableListOf<String>()
        val startup = FreshProcessStartup(
            restorePreviousSession = { FreshProcessSessionOutcome.UNAVAILABLE },
            clearStoredSession = { events += "clear" },
            purgeActiveState = { events += "purge" },
            publishSignedOut = { events += "signed-out" },
            releaseStartupGate = { events += "gate" },
        )

        assertEquals(FreshProcessSessionOutcome.UNAVAILABLE, startup.run())
        assertEquals(listOf("purge", "signed-out", "gate"), events)
    }

    @Test
    fun `cancellation still purges memory and releases startup gate`() = runBlocking {
        val cleanup = mutableListOf<String>()
        val startup = FreshProcessStartup(
            restorePreviousSession = { awaitCancellation() },
            clearStoredSession = { cleanup += "clear" },
            purgeActiveState = { cleanup += "purge" },
            publishSignedOut = { cleanup += "signed-out" },
            releaseStartupGate = { cleanup += "gate" },
        )
        try {
            withTimeout(20) { startup.run() }
            throw AssertionError("Expected startup cancellation")
        } catch (_: CancellationException) {
            // Expected: cleanup runs in NonCancellable context and cancellation propagates.
        }
        assertEquals(listOf("purge", "signed-out", "gate"), cleanup)
    }

    @Test
    fun `cleanup failures do not prevent signed out publication or gate release`() = runBlocking {
        val events = mutableListOf<String>()
        val startup = FreshProcessStartup(
            restorePreviousSession = { throw IllegalStateException("validation failed") },
            clearStoredSession = { throw IllegalStateException("datastore failure") },
            purgeActiveState = { events += "purge" },
            publishSignedOut = { events += "signed-out" },
            releaseStartupGate = { events += "gate" },
        )

        assertEquals(FreshProcessSessionOutcome.UNAVAILABLE, startup.run())
        assertEquals(listOf("purge", "signed-out", "gate"), events)
    }

    @Test
    fun `startup client excludes the global session guard`() {
        assertTrue(isStartupResumeExcludedInterceptor(SessionGuardInterceptor(SessionEvents())))
        val unrelated = Interceptor { chain -> chain.proceed(chain.request()) }
        assertFalse(isStartupResumeExcludedInterceptor(unrelated))
    }

    @Test
    fun `server remains authoritative on whether a fresh password login needs otp`() {
        val required = loginOtpRequirement(
            LoginResponse(success = true, needsOtp = true, message = "OTP required", token = "ignored"),
        )
        assertEquals(LoginOutcome.OtpRequired("OTP required", null), required)
        assertEquals(null, loginOtpRequirement(LoginResponse(success = true, needsOtp = false, token = "server-token")))
    }

    private fun session(
        serverUrl: String = "https://shop.example.com",
        token: String = "encrypted-token",
    ) = AtlasPreferences.StoredSession(
        serverUrl = serverUrl,
        tokenCipher = token,
        username = "alice",
        role = "admin",
        displayName = "Alice",
        email = "",
        avatarUrl = "",
    )
}
