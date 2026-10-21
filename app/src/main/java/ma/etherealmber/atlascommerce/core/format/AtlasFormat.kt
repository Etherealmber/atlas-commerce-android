package ma.etherealmber.atlascommerce.core.format

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Locale-aware formatting (fr-MA) used everywhere in the UI.
 *
 * The app never derives money from floating point sums in the UI layer without
 * rounding: totals are rounded once, at display time.
 */
object AtlasFormat {

    private val decimalFormatter: NumberFormat
        get() = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    private val integerFormatter: NumberFormat
        get() = NumberFormat.getIntegerInstance(Locale.FRANCE)

    fun number(value: Double, decimals: Int = 2): String {
        val safe = if (value.isFinite()) value else 0.0
        return NumberFormat.getNumberInstance(Locale.FRANCE).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }.format(safe)
    }

    fun integer(value: Double): String {
        val safe = if (value.isFinite()) value else 0.0
        return integerFormatter.format(safe.roundToLong())
    }

    /** "1 250,00 MAD" — currency is appended as a code, never a symbol. */
    fun money(amount: Double, currency: String? = null, withDecimals: Boolean = true): String {
        val code = currency?.takeIf { it.isNotBlank() } ?: "MAD"
        val formatted = if (withDecimals) number(amount, 2) else integer(amount)
        return "$formatted $code"
    }

    /** Compact money for dashboard tiles: "1,2 M" / "12,5 k". */
    fun moneyCompact(amount: Double, currency: String? = null): String {
        val code = currency?.takeIf { it.isNotBlank() } ?: "MAD"
        val abs = abs(amount)
        return when {
            abs >= 1_000_000 -> "${number(amount / 1_000_000, 1)} M $code"
            abs >= 10_000 -> "${number(amount / 1_000, 1)} k $code"
            else -> money(amount, code)
        }
    }

    /** Quantities hide trailing zeros: 2.0 → "2", 1.5 → "1,5". */
    fun quantity(value: Double): String {
        val safe = if (value.isFinite()) value else 0.0
        return if (abs(safe - safe.roundToLong()) < 0.0001) {
            integer(safe)
        } else {
            number(safe, 2).trimEnd('0').trimEnd(',')
        }
    }

    fun percent(fraction: Double): String = "${number(fraction * 100, 0)} %"

    fun dateTime(iso: String?): String = format(iso, "dd/MM/yyyy HH:mm")

    fun date(iso: String?): String = format(iso, "dd MMM yyyy")

    fun shortDate(iso: String?): String = format(iso, "dd/MM/yy")

    private fun format(iso: String?, pattern: String): String {
        if (iso.isNullOrBlank()) return "—"
        val instant = parseInstant(iso) ?: return iso.take(10)
        val zone = ZoneId.systemDefault()
        return DateTimeFormatter.ofPattern(pattern, Locale.FRENCH).format(instant.atZone(zone))
    }

    private fun parseInstant(raw: String): Instant? {
        val trimmed = raw.trim()
        runCatching { return Instant.parse(trimmed) }
        runCatching {
            return LocalDateTime.parse(trimmed.replace(' ', 'T').take(19))
                .atZone(ZoneId.systemDefault())
                .toInstant()
        }
        runCatching {
            return LocalDate.parse(trimmed.take(10))
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        }
        return null
    }

    /** Relative wording for lists: "à l'instant", "il y a 3 h", "hier". */
    fun relative(iso: String?): String {
        val instant = iso?.let { parseInstant(it) } ?: return "—"
        val seconds = (System.currentTimeMillis() - instant.toEpochMilli()) / 1000
        return when {
            seconds < 60 -> "à l'instant"
            seconds < 3_600 -> "il y a ${seconds / 60} min"
            seconds < 86_400 -> "il y a ${seconds / 3_600} h"
            seconds < 172_800 -> "hier"
            seconds < 604_800 -> "il y a ${seconds / 86_400} j"
            else -> date(iso)
        }
    }
}
