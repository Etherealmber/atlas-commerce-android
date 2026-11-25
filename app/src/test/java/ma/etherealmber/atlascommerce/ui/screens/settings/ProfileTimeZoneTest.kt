package ma.etherealmber.atlascommerce.ui.screens.settings

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTimeZoneTest {
    @Test
    fun timezoneMenuContainsOnlyTheNineSupportedChoicesInOrder() {
        assertEquals(
            listOf(
                "Africa/Casablanca",
                "Europe/Paris",
                "Europe/Madrid",
                "Europe/London",
                "Africa/Algiers",
                "Africa/Tunis",
                "Asia/Dubai",
                "America/New_York",
                "UTC",
            ),
            profileTimeZoneOptions("Europe/Tokyo").map { it.id },
        )
    }

    @Test
    fun blankProfileTimezoneUsesCasablancaAsTheDefaultLabel() {
        assertEquals("Maroc (Casablanca)", profileTimeZoneLabel(""))
    }

    @Test
    fun unsupportedServerTimezoneFallsBackToCasablancaForEditing() {
        assertEquals("Africa/Casablanca", editableProfileTimeZone("Europe/Tokyo"))
        assertEquals("Europe/Paris", editableProfileTimeZone("Europe/Paris"))
    }

    @Test
    fun utcServerTimestampIsRenderedInTheDeviceTimezone() {
        assertEquals(
            "1 janv. 2025, 09:00",
            formatProfileUpdatedAt("2025-01-01 00:00:00", ZoneId.of("Asia/Tokyo")),
        )
    }

    @Test
    fun malformedTimestampDoesNotCrashOrInventAValue() {
        assertTrue(formatProfileUpdatedAt("not-a-time", ZoneId.of("UTC")).isEmpty())
    }
}
