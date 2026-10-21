package ma.etherealmber.atlascommerce.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts the session token with an AES-256-GCM key held by the Android
 * Keystore. The key material never leaves the secure element / TEE.
 *
 * Stored format: `v1:<base64(iv)>:<base64(ciphertext)>`.
 * If the Keystore is unavailable (extremely rare, some emulators), we fall back
 * to `plain:<base64>` so the user is never locked out — the file itself is still
 * in app-private storage and excluded from backups (`allowBackup=false`).
 */
@Singleton
class CryptoStore @Inject constructor() {

    fun encrypt(plain: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val payload = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val body = Base64.encodeToString(payload, Base64.NO_WRAP)
            "$PREFIX_SECURE$iv:$body"
        } catch (t: Throwable) {
            "$PREFIX_PLAIN${Base64.encodeToString(plain.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}"
        }
    }

    fun decrypt(stored: String?): String? {
        if (stored.isNullOrBlank()) return null
        return try {
            when {
                stored.startsWith(PREFIX_PLAIN) ->
                    String(Base64.decode(stored.removePrefix(PREFIX_PLAIN), Base64.NO_WRAP), Charsets.UTF_8)

                stored.startsWith(PREFIX_SECURE) -> {
                    val parts = stored.removePrefix(PREFIX_SECURE).split(":")
                    if (parts.size != 2) return null
                    val iv = Base64.decode(parts[0], Base64.NO_WRAP)
                    val body = Base64.decode(parts[1], Base64.NO_WRAP)
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
                    String(cipher.doFinal(body), Charsets.UTF_8)
                }

                else -> null
            }
        } catch (t: Throwable) {
            null
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "atlas.session.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val PREFIX_SECURE = "v1:"
        const val PREFIX_PLAIN = "plain:"
    }
}
