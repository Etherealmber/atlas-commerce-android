package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.core.security.AndroidPasskeyOrigin
import ma.etherealmber.atlascommerce.data.model.WebAuthnAndroidCapabilityDto
import ma.etherealmber.atlascommerce.data.model.WebAuthnCapabilitiesDto
import ma.etherealmber.atlascommerce.data.model.supportsAndroid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebAuthnCapabilityTest {
    private val packageName = "ma.etherealmber.atlascommerce"
    private val sha256Origin = "android:apk-key-hash-sha256:47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU"
    private val legacySha1Origin = "android:apk-key-hash:2jmj7l5rSw0yVb_vlWAYkK_YBwk"
    private val webOrigin = "https://stock.afmarbre.com"

    @Test
    fun androidPasskeyUiAllowsTheConfiguredWebOriginWhenInstalledSigningKeyDiffers() {
        assertTrue(capabilities().supportsAndroid(packageName, setOf(sha256Origin, legacySha1Origin), webOrigin))
        assertTrue(
            capabilities(originLegacy = null)
                .supportsAndroid(packageName, setOf(sha256Origin), webOrigin),
        )
        assertTrue(
            capabilities(origin = null)
                .supportsAndroid(packageName, setOf(legacySha1Origin), webOrigin),
        )
        assertFalse(
            capabilities(enabled = false)
                .supportsAndroid(packageName, setOf(sha256Origin, legacySha1Origin), webOrigin),
        )
        assertFalse(
            capabilities(androidEnabled = false)
                .supportsAndroid(packageName, setOf(sha256Origin, legacySha1Origin), webOrigin),
        )
        assertFalse(
            capabilities(packageNameValue = "other.package")
                .supportsAndroid(packageName, setOf(sha256Origin, legacySha1Origin), webOrigin),
        )
        assertTrue(
            "The explicit selected web origin is package-checked by the backend, so a debug/upload signing key mismatch must not hide passkeys.",
            capabilities().supportsAndroid(packageName, setOf("android:apk-key-hash-sha256:wrong"), webOrigin),
        )
        assertFalse(
            capabilities().supportsAndroid(packageName, setOf(sha256Origin, legacySha1Origin), "https://unlisted.example"),
        )
        assertFalse(
            capabilities(origin = null, originLegacy = null)
                .supportsAndroid(packageName, setOf(sha256Origin, legacySha1Origin), webOrigin),
        )
    }

    @Test
    fun installedCertificateProducesCurrentSha256AndLegacySha1Origins() {
        assertEquals(
            setOf(sha256Origin, legacySha1Origin),
            AndroidPasskeyOrigin.fromCertificateDer(byteArrayOf()),
        )
    }

    private fun capabilities(
        enabled: Boolean = true,
        androidEnabled: Boolean = true,
        packageNameValue: String = packageName,
        origin: String? = sha256Origin,
        originLegacy: String? = legacySha1Origin,
    ) = WebAuthnCapabilitiesDto(
        enabled = enabled,
        origins = listOf(webOrigin),
        android = WebAuthnAndroidCapabilityDto(
            enabled = androidEnabled,
            packageName = packageNameValue,
            origin = origin,
            originLegacy = originLegacy,
        ),
    )
}
