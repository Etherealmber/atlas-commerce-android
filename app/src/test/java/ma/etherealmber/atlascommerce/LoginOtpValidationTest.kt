package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.ui.screens.login.LoginViewModel
import ma.etherealmber.atlascommerce.core.session.isProfileRefreshCurrent
import ma.etherealmber.atlascommerce.ui.screens.login.isValidOneTimeCode
import ma.etherealmber.atlascommerce.ui.screens.login.authFailureMessage
import ma.etherealmber.atlascommerce.ui.screens.login.AUTH_NETWORK_ERROR
import ma.etherealmber.atlascommerce.core.net.ApiErrorPayload
import ma.etherealmber.atlascommerce.core.net.parseRetryAfterSeconds
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginOtpValidationTest {
    @Test
    fun otpRequiresExactlySixDigits() {
        for (length in 0..5) {
            assertFalse(otp("1".repeat(length)).canSubmit)
        }
        assertTrue(otp("123456").canSubmit)
        assertTrue(isValidOneTimeCode("123456"))
        assertFalse(isValidOneTimeCode("12345"))
        assertFalse(isValidOneTimeCode("12345x"))
        assertFalse(otp("12345x").canSubmit)
        assertFalse(otp("1234567").canSubmit)
    }

    @Test
    fun expiredSavedUnlockKeepsAccountAvailableForPasswordSignIn() {
        assertEquals(
            "Session expirée — reconnectez-vous avec votre mot de passe.",
            authFailureMessage("SAVED_SESSION_EXPIRED", httpStatus = 401, isOtp = false),
        )
    }

    @Test
    fun rateLimitIsReportedAsAWaitNotAsBadCredentialsOrNetworkFailure() {
        assertEquals(
            "Trop de tentatives de connexion. Réessayez dans 75 s.",
            authFailureMessage("RATE_LIMITED", 429, isOtp = false, retryAfterSeconds = 75),
        )
        assertEquals(
            "Trop de tentatives de connexion. Réessayez plus tard.",
            authFailureMessage("RATE_LIMITED", 429, isOtp = true),
        )
    }

    @Test
    fun transientNetworkFailureOffersActionableSignInRetryGuidance() {
        val message = authFailureMessage("NETWORK", httpStatus = null, isOtp = false)

        assertEquals(AUTH_NETWORK_ERROR, message)
        assertTrue(message.contains("Réessayez"))
        assertTrue(message.contains("vérifiez le réseau"))
    }

    @Test
    fun limiterUnavailableIsDistinctFromBadCredentials() {
        assertEquals(
            "Le contrôle de connexion est temporairement indisponible. Réessayez plus tard.",
            authFailureMessage("AUTH_LIMIT_UNAVAILABLE", 503, isOtp = false),
        )
    }

    @Test
    fun profileRefreshCannotApplyAfterAccountServerOrTokenChanges() {
        assertTrue(isProfileRefreshCurrent("server|alice|admin", "token-a", "server|alice|admin", "token-a"))
        assertFalse(isProfileRefreshCurrent("server|alice|admin", "token-a", "server|bob|admin", "token-a"))
        assertFalse(isProfileRefreshCurrent("server|alice|admin", "token-a", "other-server|alice|admin", "token-a"))
        assertFalse(isProfileRefreshCurrent("server|alice|admin", "token-a", "server|alice|admin", "token-b"))
        assertFalse(isProfileRefreshCurrent("server|alice|admin", "token-a", null, null))
    }

    @Test
    fun errorPayloadReadsRetryAfterSeconds() {
        val payload = Moshi.Builder().build()
            .adapter(ApiErrorPayload::class.java)
            .fromJson("""{"code":"RATE_LIMITED","retry_after":75}""")

        assertEquals("RATE_LIMITED", payload?.code)
        assertEquals(75L, payload?.retryAfterSeconds)
    }

    @Test
    fun retryAfterAcceptsOnlyBoundedPositiveDeltaSeconds() {
        assertEquals(75L, parseRetryAfterSeconds("75", payloadSeconds = null))
        assertEquals(40L, parseRetryAfterSeconds("not-a-number", payloadSeconds = 40L))
        assertEquals(null, parseRetryAfterSeconds("0", payloadSeconds = null))
        assertEquals(null, parseRetryAfterSeconds("90000", payloadSeconds = null))
        assertEquals(null, parseRetryAfterSeconds("-1", payloadSeconds = null))
    }

    private fun otp(code: String) = LoginViewModel.UiState(
        username = "operator",
        step = LoginViewModel.Step.OTP,
        otp = code,
    )
}
