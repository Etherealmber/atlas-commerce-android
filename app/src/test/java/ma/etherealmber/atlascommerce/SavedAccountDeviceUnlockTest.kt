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

        val enabled = newlySaved.copy(deviceUnlockEnabled = true, deviceUnlockChoiceMade = true)
        assertTrue(enabled.deviceUnlockEnabled)
        assertTrue(enabled.deviceUnlockChoiceMade)
    }
}
