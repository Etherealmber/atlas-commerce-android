package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.data.model.WebAuthnAndroidCapabilityDto
import ma.etherealmber.atlascommerce.data.model.WebAuthnCapabilitiesDto
import ma.etherealmber.atlascommerce.data.model.supportsAndroid
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebAuthnCapabilityTest {
    @Test
    fun androidPasskeyUiRequiresBothServerGatesAndMatchingPackage() {
        assertTrue(
            WebAuthnCapabilitiesDto(
                enabled = true,
                android = WebAuthnAndroidCapabilityDto(enabled = true, packageName = "ma.etherealmber.atlascommerce"),
            ).supportsAndroid("ma.etherealmber.atlascommerce"),
        )
        assertFalse(
            WebAuthnCapabilitiesDto(
                enabled = false,
                android = WebAuthnAndroidCapabilityDto(enabled = true, packageName = "ma.etherealmber.atlascommerce"),
            ).supportsAndroid("ma.etherealmber.atlascommerce"),
        )
        assertFalse(
            WebAuthnCapabilitiesDto(
                enabled = true,
                android = WebAuthnAndroidCapabilityDto(enabled = false, packageName = "ma.etherealmber.atlascommerce"),
            ).supportsAndroid("ma.etherealmber.atlascommerce"),
        )
        assertFalse(
            WebAuthnCapabilitiesDto(
                enabled = true,
                android = WebAuthnAndroidCapabilityDto(enabled = true, packageName = "other.package"),
            ).supportsAndroid("ma.etherealmber.atlascommerce"),
        )
    }
}
