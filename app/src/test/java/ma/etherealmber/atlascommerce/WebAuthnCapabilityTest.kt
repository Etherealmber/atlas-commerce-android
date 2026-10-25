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
    private val nativeOrigin = "android:apk-key-hash:47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU"
    private val webOrigin = "https://stock.afmarbre.com"

    @Test
    fun androidPasskeyUiRequiresBothServerGatesPackageSigningOriginAndAllowedWebOrigin() {
        assertTrue(
            capabilities().supportsAndroid(packageName, setOf(nativeOrigin), webOrigin),
        )
        assertFalse(
            capabilities(enabled = false).supportsAndroid(packageName, setOf(nativeOrigin), webOrigin),
        )
        assertFalse(
            capabilities(androidEnabled = false).supportsAndroid(packageName, setOf(nativeOrigin), webOrigin),
        )
        assertFalse(
            capabilities(packageNameValue = "other.package").supportsAndroid(packageName, setOf(nativeOrigin), webOrigin),
        )
        assertFalse(
            capabilities().supportsAndroid(packageName, setOf("android:apk-key-hash:wrong"), webOrigin),
        )
        assertFalse(
            capabilities().supportsAndroid(packageName, setOf(nativeOrigin), "https://unlisted.example"),
        )
        assertFalse(
            capabilities(androidOrigin = null).supportsAndroid(packageName, setOf(nativeOrigin), webOrigin),
        )
        assertFalse(
            capabilities(androidOrigin = nativeOrigin.replace("android:apk-key-hash:", "android:apk-key-hash-sha256:"))
                .supportsAndroid(packageName, setOf(nativeOrigin), webOrigin),
        )
    }

    @Test
    fun nativeOriginUsesAndroidCredentialManagerStandardPrefixAndBase64Url() {
        assertEquals(nativeOrigin, AndroidPasskeyOrigin.fromCertificateDer(byteArrayOf()))
    }

    private fun capabilities(
        enabled: Boolean = true,
        androidEnabled: Boolean = true,
        packageNameValue: String = packageName,
        androidOrigin: String? = nativeOrigin,
    ) = WebAuthnCapabilitiesDto(
        enabled = enabled,
        origins = listOf(webOrigin),
        android = WebAuthnAndroidCapabilityDto(
            enabled = androidEnabled,
            packageName = packageNameValue,
            origin = androidOrigin,
        ),
    )
}
