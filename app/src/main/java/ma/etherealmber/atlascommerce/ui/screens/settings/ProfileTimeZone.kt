package ma.etherealmber.atlascommerce.ui.screens.settings

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ProfileTimeZoneOption(val id: String, val label: String)

val ProfileTimeZoneOptions = listOf(
    ProfileTimeZoneOption("Africa/Casablanca", "Maroc (Casablanca)"),
    ProfileTimeZoneOption("Europe/Paris", "France (Paris)"),
    ProfileTimeZoneOption("Europe/Madrid", "Espagne (Madrid)"),
    ProfileTimeZoneOption("Europe/London", "Royaume-Uni (Londres)"),
    ProfileTimeZoneOption("Africa/Algiers", "Algérie (Alger)"),
    ProfileTimeZoneOption("Africa/Tunis", "Tunisie (Tunis)"),
    ProfileTimeZoneOption("Asia/Dubai", "Émirats (Dubaï)"),
    ProfileTimeZoneOption("America/New_York", "États-Unis (New York)"),
    ProfileTimeZoneOption("UTC", "UTC"),
)

fun profileTimeZoneOptions(@Suppress("UNUSED_PARAMETER") current: String): List<ProfileTimeZoneOption> =
    ProfileTimeZoneOptions

fun profileTimeZoneLabel(id: String): String =
    ProfileTimeZoneOptions.firstOrNull { it.id == id }?.label
        ?: id.takeIf(String::isNotBlank)
        ?: ProfileTimeZoneOptions.first().label

fun editableProfileTimeZone(id: String): String =
    id.takeIf { value -> ProfileTimeZoneOptions.any { it.id == value } } ?: "Africa/Casablanca"

/** Worker profile_updated_at is a UTC D1 datetime string; render it in the device zone. */
fun formatProfileUpdatedAt(raw: String, deviceZone: ZoneId = ZoneId.systemDefault()): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    val instant = runCatching {
        when {
            Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$").matches(trimmed) ->
                LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .toInstant(ZoneOffset.UTC)
            else -> Instant.parse(trimmed)
        }
    }.getOrNull() ?: return ""
    val local = instant.atZone(deviceZone)
    return DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.FRANCE).format(local)
}
