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

/** Encrypts app-owned data at rest with AES-256-GCM and an Android Keystore key. */
@Singleton
class CryptoStore @Inject constructor() : AtRestCipher {

    /** Session-token format retained for compatibility with previously stored v1 values. */
    fun encrypt(plain: String): String = encryptWithPrefix(PREFIX_SECURE, plain)

    /** Versioned envelope used for all string values in DataStore and local catalog files. */
    override fun encryptAtRest(plain: String): String = encryptWithPrefix(PREFIX_AT_REST, plain)

    override fun decryptAtRest(stored: String?): String? =
        stored?.takeIf { it.startsWith(PREFIX_AT_REST) }?.let(::decrypt)

    /** Reads v1/v2 ciphertext and the old v1 Base64-only token format for migration. */
    fun decrypt(stored: String?): String? {
        if (stored.isNullOrBlank()) return null
        return try {
            when {
                stored.startsWith(PREFIX_PLAIN) ->
                    String(Base64.decode(stored.removePrefix(PREFIX_PLAIN), Base64.NO_WRAP), Charsets.UTF_8)

                stored.startsWith(PREFIX_SECURE) || stored.startsWith(PREFIX_AT_REST) -> {
                    val prefix = if (stored.startsWith(PREFIX_SECURE)) PREFIX_SECURE else PREFIX_AT_REST
                    val parts = stored.removePrefix(prefix).split(":")
                    if (parts.size != 2) return null
                    val iv = Base64.decode(parts[0], Base64.NO_WRAP)
                    val body = Base64.decode(parts[1], Base64.NO_WRAP)
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
                    String(cipher.doFinal(body), Charsets.UTF_8)
                }

                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun encryptWithPrefix(prefix: String, plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val payload = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val body = Base64.encodeToString(payload, Base64.NO_WRAP)
        return "$prefix$iv:$body"
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
        const val PREFIX_AT_REST = "v2:"
        const val PREFIX_PLAIN = "plain:"
    }
}

/** Small seam so JVM tests can exercise the isolated cache without Android Keystore. */
interface AtRestCipher {
    fun encryptAtRest(plain: String): String
    fun decryptAtRest(stored: String?): String?
}
