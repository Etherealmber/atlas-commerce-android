package ma.etherealmber.atlascommerce.core.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ma.etherealmber.atlascommerce.core.persist.DEVICE_UNLOCK_TOKEN_PREFIX
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Protects remembered-account tokens with a per-account Android Keystore key that requires
 * fresh local authentication. The prompt's CryptoObject decrypts the token before the app
 * reports success; the saved session is never released by the prompt callback alone.
 */
object DeviceUnlockAuthenticator {
    enum class Outcome { AUTHENTICATED, CANCELED, UNAVAILABLE, FAILED }

    data class Result(
        val outcome: Outcome,
        val sessionToken: String? = null,
        val protectedTokenCipher: String? = null,
    )

    fun isAvailable(activity: FragmentActivity): Boolean {
        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val secureDeviceCredential = keyguard?.isDeviceSecure == true
        val strongBiometric = BiometricManager.from(activity).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG,
        ) == BiometricManager.BIOMETRIC_SUCCESS
        return secureDeviceCredential || strongBiometric
    }

    /**
     * For an older app-encrypted saved token, the user is already in an active authenticated
     * session; this operation upgrades it to an auth-bound Keystore envelope. For an auth-bound
     * envelope, the CryptoObject returned by BiometricPrompt is required to decrypt the token.
     */
    fun authenticate(
        activity: FragmentActivity,
        serverUrl: String,
        username: String,
        storedTokenCipher: String,
        title: String,
        subtitle: String,
        onResult: (Result) -> Unit,
    ) {
        if (!isAvailable(activity)) {
            onResult(Result(Outcome.UNAVAILABLE))
            return
        }

        val alias = keyAlias(serverUrl, username) ?: run {
            onResult(Result(Outcome.FAILED))
            return
        }
        val protectsExistingToken = storedTokenCipher.startsWith(DEVICE_UNLOCK_TOKEN_PREFIX)
        val plaintextToken: ByteArray?
        val cipher: Cipher
        try {
            if (protectsExistingToken) {
                val (iv, ciphertext) = decodeEnvelope(storedTokenCipher)
                cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, loadKey(alias), GCMParameterSpec(TAG_BITS, iv))
                plaintextToken = null
            } else {
                val token = CryptoStore().decrypt(storedTokenCipher)?.takeIf(String::isNotBlank)
                    ?: throw IllegalArgumentException("Saved session token is unavailable")
                plaintextToken = token.toByteArray(Charsets.UTF_8)
                cipher = createEncryptCipher(alias)
            }
        } catch (_: Exception) {
            onResult(Result(Outcome.FAILED))
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    try {
                        // Android 11+ supports a per-operation auth-bound CryptoObject together
                        // with the device-credential fallback. On Android 9/10 the prompt's
                        // credential auth unlocks the key for the short validity window below.
                        val authorizedCipher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            result.cryptoObject?.cipher ?: throw IllegalStateException("Missing authenticated CryptoObject")
                        } else {
                            cipher
                        }
                        val payload = if (protectsExistingToken) {
                            authorizedCipher.doFinal(decodeEnvelope(storedTokenCipher).second)
                        } else {
                            authorizedCipher.doFinal(plaintextToken ?: throw IllegalStateException("Missing token payload"))
                        }
                        val token = String(payload, Charsets.UTF_8).takeIf(String::isNotBlank)
                            ?: throw IllegalStateException("The protected session token is empty")
                        val protectedCipher = if (protectsExistingToken) {
                            storedTokenCipher
                        } else {
                            encodeEnvelope(authorizedCipher.iv, payload)
                        }
                        onResult(
                            Result(
                                outcome = Outcome.AUTHENTICATED,
                                sessionToken = token,
                                protectedTokenCipher = protectedCipher,
                            ),
                        )
                    } catch (_: Exception) {
                        onResult(Result(Outcome.FAILED))
                    }
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
                    onResult(Result(outcome))
                }
            },
        )
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
        } else {
            promptInfoBuilder.setDeviceCredentialAllowed(true)
        }
        val promptInfo = promptInfoBuilder.build()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            } else {
                prompt.authenticate(promptInfo)
            }
        }.onFailure {
            onResult(Result(Outcome.UNAVAILABLE))
        }
    }

    private fun createEncryptCipher(alias: String): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val key = try {
            loadKey(alias)
        } catch (_: Exception) {
            generateKey(alias)
        }
        return try {
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher
        } catch (_: Exception) {
            // A legacy token is available to re-wrap, so an invalid per-account key may be
            // replaced without affecting any other saved account.
            deleteKey(alias)
            val replacement = generateKey(alias)
            Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, replacement) }
        }
    }

    private fun loadKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: throw IllegalStateException("Device-unlock key is missing")
    }

    private fun generateKey(alias: String): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
        } else {
            // Device-credential fallback is supported by the pre-Android-11 prompt. The
            // callback immediately consumes the key operation within this short auth window.
            builder.setUserAuthenticationValidityDurationSeconds(1)
        }
        generator.init(builder.build())
        return generator.generateKey()
    }

    fun forgetSavedAccountKey(serverUrl: String, username: String) {
        keyAlias(serverUrl, username)?.let(::deleteKey)
    }

    private fun deleteKey(alias: String) {
        runCatching {
            KeyStore.getInstance(KEYSTORE).apply {
                load(null)
                deleteEntry(alias)
            }
        }
    }

    private fun decodeEnvelope(value: String): Pair<ByteArray, ByteArray> {
        require(value.startsWith(DEVICE_UNLOCK_TOKEN_PREFIX))
        val parts = value.removePrefix(DEVICE_UNLOCK_TOKEN_PREFIX).split(':')
        require(parts.size == 2)
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
        require(iv.isNotEmpty() && ciphertext.isNotEmpty())
        return iv to ciphertext
    }

    private fun encodeEnvelope(iv: ByteArray, ciphertext: ByteArray): String =
        "$DEVICE_UNLOCK_TOKEN_PREFIX${Base64.encodeToString(iv, Base64.NO_WRAP)}:${Base64.encodeToString(ciphertext, Base64.NO_WRAP)}"

    private fun keyAlias(serverUrl: String, username: String): String? {
        val normalizedServer = ServerUrl.normalize(serverUrl) ?: return null
        val identity = "$normalizedServer\n${username.trim().lowercase(java.util.Locale.ROOT)}"
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        val suffix = digest.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return "$KEY_ALIAS_PREFIX$suffix"
    }

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS_PREFIX = "atlas.device.unlock.v1."
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128
}
