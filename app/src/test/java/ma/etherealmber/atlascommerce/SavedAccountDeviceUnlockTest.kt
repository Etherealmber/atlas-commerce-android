package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.core.persist.SavedAccountEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedAccountDeviceUnlockTest {
    @Test
    fun deviceUnlockIsOptInAndCanOnlyBeEnabledAfterARecordedChoice() {
        val newlySaved = SavedAccountEntry(
            serverUrl = "https://atlas.example",
            username = "operator",
            role = "user",
            displayName = "operator",
            tokenCipher = "v1:encrypted-token",
        )

        assertFalse(newlySaved.deviceUnlockEnabled)
        assertFalse(newlySaved.deviceUnlockChoiceMade)
        assertFalse(newlySaved.canUseDeviceUnlock)
        assertFalse(newlySaved.copy(deviceUnlockEnabled = true).canUseDeviceUnlock)

        val enabledWithoutProtectedToken = newlySaved.copy(deviceUnlockEnabled = true, deviceUnlockChoiceMade = true)
        assertTrue(enabledWithoutProtectedToken.deviceUnlockEnabled)
        assertTrue(enabledWithoutProtectedToken.deviceUnlockChoiceMade)
        assertFalse(enabledWithoutProtectedToken.canUseDeviceUnlock)

        val enabled = enabledWithoutProtectedToken.copy(tokenCipher = "du2:protected-token")
        assertTrue(enabled.canUseDeviceUnlock)

        // Tokens wrapped with the old short-window Android 9/10 key must require password sign-in.
        val legacy = enabledWithoutProtectedToken.copy(tokenCipher = "du1:legacy-token")
        assertFalse(legacy.canUseDeviceUnlock)
        assertTrue(legacy.hasLegacyDeviceUnlockEnvelope)
    }
}
