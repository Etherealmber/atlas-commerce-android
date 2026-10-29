package ma.etherealmber.atlascommerce.core.security

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Uses the system biometric prompt with device PIN/password fallback. */
object DeviceUnlockAuthenticator {
    enum class Outcome { AUTHENTICATED, CANCELED, UNAVAILABLE, FAILED }

    fun isAvailable(activity: FragmentActivity): Boolean {
        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val secureDeviceCredential = keyguard?.isDeviceSecure == true
        val enrolledBiometric = BiometricManager.from(activity).canAuthenticate() ==
            BiometricManager.BIOMETRIC_SUCCESS
        return secureDeviceCredential || enrolledBiometric
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (Outcome) -> Unit,
    ) {
        if (!isAvailable(activity)) {
            onResult(Outcome.UNAVAILABLE)
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(Outcome.AUTHENTICATED)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val outcome = when (errorCode) {
                        BiometricPrompt.ERROR_CANCELED,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> Outcome.CANCELED
                        BiometricPrompt.ERROR_NO_BIOMETRICS,
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> Outcome.UNAVAILABLE
                        else -> Outcome.FAILED
                    }
                    onResult(outcome)
                }
            },
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDeviceCredentialAllowed(true)
            .build()
        runCatching { prompt.authenticate(promptInfo) }
            .onFailure { onResult(Outcome.UNAVAILABLE) }
    }
}
