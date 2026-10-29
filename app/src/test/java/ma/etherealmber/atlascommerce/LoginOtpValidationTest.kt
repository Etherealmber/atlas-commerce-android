package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.ui.screens.login.LoginViewModel
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
        assertFalse(otp("12345x").canSubmit)
        assertFalse(otp("1234567").canSubmit)
    }

    private fun otp(code: String) = LoginViewModel.UiState(
        username = "operator",
        step = LoginViewModel.Step.OTP,
        otp = code,
    )
}
