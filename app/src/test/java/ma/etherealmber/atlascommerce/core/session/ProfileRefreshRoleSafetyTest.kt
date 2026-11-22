package ma.etherealmber.atlascommerce.core.session

import ma.etherealmber.atlascommerce.data.model.MeDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileRefreshRoleSafetyTest {
    @Test
    fun profileRefreshDoesNotTreatTokenEchoedRoleAsAnAuthorizationUpdate() {
        val active = SessionUser(
            username = "staff",
            role = "stock",
            displayName = "Staff",
            serverUrl = "https://shop.example.test",
        )

        listOf("admin", "user").forEach { tokenRole ->
            val refreshed = active.withRefreshedProfile(
                MeDto(ok = true, username = "staff", role = tokenRole, displayName = "Staff Member"),
            )
            assertEquals("stock", refreshed.role)
            assertEquals("Staff Member", refreshed.displayName)
        }
    }
}
