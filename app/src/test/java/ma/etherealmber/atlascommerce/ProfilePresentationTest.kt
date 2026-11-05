package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.ui.screens.settings.profileNameOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfilePresentationTest {
    @Test
    fun showsActualDisplayNameButNotUsernameFallbackAsASecondName() {
        assertEquals("Amine El Fassi", profileNameOrNull("  Amine El Fassi  ", "amine"))
        assertNull(profileNameOrNull("admin", "ADMIN"))
        assertNull(profileNameOrNull("  ", "admin"))
        assertNull(profileNameOrNull(null, "admin"))
    }
}
