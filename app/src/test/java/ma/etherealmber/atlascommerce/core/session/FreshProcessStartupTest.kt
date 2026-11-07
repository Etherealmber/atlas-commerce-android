package ma.etherealmber.atlascommerce.core.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import ma.etherealmber.atlascommerce.core.net.ApiResult
import okhttp3.Interceptor
import ma.etherealmber.atlascommerce.core.net.SessionEvents
import ma.etherealmber.atlascommerce.core.net.SessionGuardInterceptor
import ma.etherealmber.atlascommerce.core.net.isStartupResumeExcludedInterceptor
import ma.etherealmber.atlascommerce.data.model.ActionAckDto
import ma.etherealmber.atlascommerce.data.repo.requireResumePreparationConfirmation
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.data.model.LoginResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshProcessStartupTest {

    @Test
    fun `valid previous session is prepared before erase and startup always ends signed out`() = runBlocking {
        val events = mutableListOf<String>()
        val stateHistory = mutableListOf<SessionState>(SessionState.Loading)
        val stored = session(serverUrl = "https://shop.example.com", token = "cipher")

        val startup = FreshProcessStartup(
            preparePreviousSession = {
                events += "prepare"
                val result = StartupResumePreparation.prepare(
                    previousSession = stored,
                    selectedServerUrl = "https://shop.example.com",
                    restoreDisabled = false,
                    decryptToken = { if (it == "cipher") "old-session-token" else null },
                    notifyServer = { _, token ->
                        assertEquals("old-session-token", token)
                        ApiResult.Success(Unit)
                    },
                )
                assertEquals(StartupResumeResult.PREPARED, result)
            },
            clearStoredSession = { events += "clear" },
            purgeActiveState = { events += "purge" },
            publishSignedOut = {
                events += "signed-out"
                stateHistory += SessionState.SignedOut
            },
            releaseStartupGate = { events += "gate" },
        )

        startup.run()

        assertEquals(listOf("prepare", "clear", "purge", "signed-out", "gate"), events)
        assertEquals(SessionState.SignedOut, stateHistory.last())
        assertFalse(stateHistory.any { it is SessionState.Active })
    }

    @Test
    fun `only same selected origin can receive previous session proof`() = runBlocking {
        val sent = mutableListOf<Pair<String, String>>()
        val result = StartupResumePreparation.prepare(
            previousSession = session(serverUrl = "https://shop.example.com/api"),
            selectedServerUrl = "https://shop.example.com:443",
            restoreDisabled = false,
            decryptToken = { "decrypted-session-proof" },
            notifyServer = { server, token ->
                sent += server to token
                ApiResult.Success(Unit)
            },
        )

        assertEquals(StartupResumeResult.PREPARED, result)
        // Normalization retains an explicitly configured default port in the request URL.
        assertEquals(listOf("https://shop.example.com:443" to "decrypted-session-proof"), sent)
    }

    @Test
    fun `other origin absent session disabled restore and bad decryption never send a token`() = runBlocking {
        var sends = 0
        suspend fun prepare(
            stored: AtlasPreferences.StoredSession?,
            selected: String? = "https://shop.example.com",
            disabled: Boolean = false,
            decrypt: (String) -> String? = { "secret-proof" },
        ) = StartupResumePreparation.prepare(
            previousSession = stored,
            selectedServerUrl = selected,
            restoreDisabled = disabled,
            decryptToken = decrypt,
            notifyServer = { _, _ -> sends++; ApiResult.Success(Unit) },
        )

        assertEquals(StartupResumeResult.NO_SESSION, prepare(null))
        assertEquals(StartupResumeResult.RESTORE_DISABLED, prepare(session(), disabled = true))
        assertEquals(
            StartupResumeResult.DIFFERENT_ORIGIN,
            prepare(session(serverUrl = "https://other.example.com")),
        )
        assertEquals(
            StartupResumeResult.UNDECRYPTABLE_TOKEN,
            prepare(session(), decrypt = { null }),
        )
        assertEquals(0, sends)
    }

    @Test
    fun `server rejection and network failure never become a successful preparation`() = runBlocking {
        val rejected = StartupResumePreparation.prepare(
            previousSession = session(),
            selectedServerUrl = "https://shop.example.com",
            restoreDisabled = false,
            decryptToken = { "old-token" },
            notifyServer = { _, _ -> ApiResult.Failure("SESSION_EXPIRED", "expired", 401) },
        )
        assertEquals(StartupResumeResult.SERVER_REJECTED, rejected)

        val networkFailure = StartupResumePreparation.prepare(
            previousSession = session(),
            selectedServerUrl = "https://shop.example.com",
            restoreDisabled = false,
            decryptToken = { "old-token" },
            notifyServer = { _, _ -> throw IllegalStateException("network") },
        )
        assertEquals(StartupResumeResult.NETWORK_FAILED, networkFailure)
    }

    @Test
    fun `deadline and cancellation abort preparation but still clear and publish signed out`() = runBlocking {
        val outcome = StartupResumePreparation.prepare(
            previousSession = session(),
            selectedServerUrl = "https://shop.example.com",
            restoreDisabled = false,
            decryptToken = { "old-token" },
            timeoutMillis = 20,
            notifyServer = { _, _ -> delay(500); ApiResult.Success(Unit) },
        )
        assertEquals(StartupResumeResult.TIMED_OUT, outcome)

        val cleanup = mutableListOf<String>()
        val startup = FreshProcessStartup(
            preparePreviousSession = { awaitCancellation() },
            clearStoredSession = { cleanup += "clear" },
            purgeActiveState = { cleanup += "purge" },
            publishSignedOut = { cleanup += "signed-out" },
            releaseStartupGate = { cleanup += "gate" },
        )
        try {
            withTimeout(20) { startup.run() }
            throw AssertionError("Expected startup cancellation")
        } catch (_: CancellationException) {
            // Expected: cancellation is propagated after NonCancellable cleanup.
        }
        assertEquals(listOf("clear", "purge", "signed-out", "gate"), cleanup)
    }

    @Test
    fun `cleanup failure does not prevent purge signed out state or gate release`() = runBlocking {
        val events = mutableListOf<String>()
        val startup = FreshProcessStartup(
            preparePreviousSession = { throw IllegalStateException("preparation failed") },
            clearStoredSession = { throw IllegalStateException("datastore failure") },
            purgeActiveState = { events += "purge" },
            publishSignedOut = { events += "signed-out" },
            releaseStartupGate = { events += "gate" },
        )

        startup.run()

        assertEquals(listOf("purge", "signed-out", "gate"), events)
        assertTrue(events.contains("signed-out"))
    }

    @Test
    fun `resume preparation requires explicit worker confirmation`() {
        assertTrue(
            requireResumePreparationConfirmation(
                ApiResult.Success(ActionAckDto(success = true, resumePrepared = true)),
            ) is ApiResult.Success,
        )
        val notPrepared = requireResumePreparationConfirmation(
            ApiResult.Success(ActionAckDto(success = true, resumePrepared = false)),
        ) as ApiResult.Failure
        assertEquals("RESUME_NOT_PREPARED", notPrepared.code)
        val contradictory = requireResumePreparationConfirmation(
            ApiResult.Success(ActionAckDto(success = false, resumePrepared = true)),
        ) as ApiResult.Failure
        assertEquals("RESUME_NOT_PREPARED", contradictory.code)

        val rejected = requireResumePreparationConfirmation(
            ApiResult.Failure("RESUME_PREPARATION_FAILED", "not prepared", 503),
        ) as ApiResult.Failure
        assertEquals(503, rejected.httpStatus)
    }

    @Test
    fun `startup client strips the global session guard to avoid startup expiry races`() {
        assertTrue(isStartupResumeExcludedInterceptor(SessionGuardInterceptor(SessionEvents())))
        val unrelated = Interceptor { chain -> chain.proceed(chain.request()) }
        assertFalse(isStartupResumeExcludedInterceptor(unrelated))
    }

    @Test
    fun `server needs otp remains authoritative after resume preparation`() {
        val twoFactorEnabled = loginOtpRequirement(
            LoginResponse(success = true, needsOtp = true, message = "OTP required", token = "ignored"),
        )
        assertEquals(LoginOutcome.OtpRequired("OTP required", null), twoFactorEnabled)
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
