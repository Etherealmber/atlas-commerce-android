package ma.etherealmber.atlascommerce.core.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import ma.etherealmber.atlascommerce.BuildConfig
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

internal fun resolveDeviceInstallId(
    stored: String?,
    decryptAtRest: (String?) -> String?,
    encryptAtRest: (String) -> String,
    generate: () -> String,
    persist: (String) -> Unit,
): String {
    if (!stored.isNullOrBlank()) {
        if (stored.startsWith("v2:")) {
            // Keystore access can fail transiently. Keep this identity unavailable rather
            // than authenticating with a substitute fingerprint that invalidates the session.
            val decoded = runCatching { decryptAtRest(stored) }.getOrNull()?.takeIf(String::isNotBlank)
                ?: throw DeviceIdentityUnavailableException()
            return decoded
        }
        // Migrate the UUID stored by older versions without changing this device's fingerprint.
        val migrated = runCatching { encryptAtRest(stored) }.getOrNull()
        if (migrated != null) runCatching { persist(migrated) }
        return stored
    }
    val generated = generate()
    runCatching { persist(encryptAtRest(generated)) }
    return generated
}

/** Raised when the persisted device identity cannot yet be decrypted. */
internal class DeviceIdentityUnavailableException : IllegalStateException(
    "Saved device identity is temporarily unavailable",
)

/**
 * Stable, privacy-preserving device identity.
 *
 * The worker binds every session token to `(IP, device fingerprint)`, so the
 * fingerprint must survive app restarts and stay identical across requests.
 * We derive it from a locally generated install UUID + ANDROID_ID + package
 * name and expose only a SHA-256 digest — the raw identifiers never leave the
 * device and the digest is not reversible.
 */
@Singleton
class DeviceIdentity @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoStore: CryptoStore,
) {

    private val installId: String by lazy {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_INSTALL_ID, null)
        resolveDeviceInstallId(
            stored = stored,
            decryptAtRest = cryptoStore::decryptAtRest,
            encryptAtRest = cryptoStore::encryptAtRest,
            generate = { UUID.randomUUID().toString() },
            persist = { encrypted -> prefs.edit().putString(KEY_INSTALL_ID, encrypted).apply() },
        )
    }

    val fingerprint: String by lazy {
        sha256("atlas|$installId|${androidId()}|${context.packageName}")
    }

    val userAgent: String by lazy {
        val release = Build.VERSION.RELEASE ?: "?"
        val model = Build.MODEL ?: "device"
        "Atlas-Android/${BuildConfig.VERSION_NAME} (Android $release; $model)"
    }

    fun shortId(): String = fingerprint.take(8)

    private fun androidId(): String = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }.getOrDefault("unknown")

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        val out = StringBuilder(digest.size * 2)
        for (byte in digest) out.append("%02x".format(byte))
        return out.toString()
    }

    private companion object {
        const val PREFS = "atlas_device"
        const val KEY_INSTALL_ID = "install_id"
    }
}
