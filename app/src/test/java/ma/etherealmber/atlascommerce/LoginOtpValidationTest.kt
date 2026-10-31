package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.ui.screens.login.LoginViewModel
import ma.etherealmber.atlascommerce.ui.screens.login.isValidOneTimeCode
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

    private fun otp(code: String) = LoginViewModel.UiState(
        username = "operator",
        step = LoginViewModel.Step.OTP,
        otp = code,
    )
}
