package ma.etherealmber.atlascommerce.core.session

import ma.etherealmber.atlascommerce.data.model.MeDto
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionProfileFieldsTest {
    @Test
    fun serverReturnedProfileFieldsAreAuthoritative() {
        val original = SessionUser(
            username = "amina",
            role = "owner",
            displayName = "Amina",
            jobTitle = "",
            workingHours = "",
            timezone = "Africa/Casablanca",
        )

        val updated = original.withRefreshedProfile(
            MeDto(
                username = "amina",
                role = "user", // /auth/me cannot override the active role.
                jobTitle = "Responsable",
                workingHours = "09:00–17:00",
                timezone = "Europe/Paris",
                profileUpdatedAt = "2025-01-01 00:00:00",
            ),
        )

        assertEquals("Responsable", updated.jobTitle)
        assertEquals("09:00–17:00", updated.workingHours)
        assertEquals("Europe/Paris", updated.timezone)
        assertEquals("2025-01-01 00:00:00", updated.profileUpdatedAt)
        assertEquals("owner", updated.role)
    }
}
