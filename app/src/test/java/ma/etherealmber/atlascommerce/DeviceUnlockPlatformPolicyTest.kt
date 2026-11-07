package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.core.security.DeviceUnlockAuthenticator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceUnlockPlatformPolicyTest {

    @Test
    fun savedAccountBiometricsRequireAndroid11OrNewer() {
        assertFalse(DeviceUnlockAuthenticator.supportsPerUseStrongBiometric(28))
        assertFalse(DeviceUnlockAuthenticator.supportsPerUseStrongBiometric(29))
        assertTrue(DeviceUnlockAuthenticator.supportsPerUseStrongBiometric(30))
        assertTrue(DeviceUnlockAuthenticator.supportsPerUseStrongBiometric(35))
    }
}
