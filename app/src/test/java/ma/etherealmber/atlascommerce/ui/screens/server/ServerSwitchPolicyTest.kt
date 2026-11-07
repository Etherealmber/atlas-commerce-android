package ma.etherealmber.atlascommerce.ui.screens.server

import ma.etherealmber.atlascommerce.core.server.ServerSelection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerSwitchPolicyTest {
    @Test
    fun invalidTargetDoesNotRevokeCurrentSession() {
        val validation = Result.failure<ServerSelection>(IllegalArgumentException("invalid URL"))

        assertFalse(shouldRevokeSessionForServerSwitch("https://old.example", validation))
    }

    @Test
    fun selectingSameNormalizedServerKeepsCurrentSession() {
        val validation = Result.success(ServerSelection("https://shop.example/api/"))

        assertFalse(shouldRevokeSessionForServerSwitch("HTTPS://SHOP.EXAMPLE/api", validation))
    }

    @Test
    fun validDifferentServerRevokesCurrentSession() {
        val validation = Result.success(ServerSelection("https://new.example"))

        assertTrue(shouldRevokeSessionForServerSwitch("https://old.example", validation))
    }

    @Test
    fun invalidPreviouslySelectedOriginFailsClosed() {
        val validation = Result.success(ServerSelection("https://new.example"))

        assertTrue(shouldRevokeSessionForServerSwitch("not a server URL", validation))
    }
}
