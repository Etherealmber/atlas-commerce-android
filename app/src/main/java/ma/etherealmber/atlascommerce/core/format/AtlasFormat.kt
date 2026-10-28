package ma.etherealmber.atlascommerce.core.format

import ma.etherealmber.atlascommerce.core.AppLanguage
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
 * Locale-aware formatting used throughout Atlas. Money always retains the server's currency code;
 * Arabic output uses locale digits/separators and isolates the Latin ISO code to prevent bidi
 * reordering. Values are rounded only for display and are never used to derive financial totals.
 */
object AtlasFormat {

    private fun locale(language: AppLanguage): Locale = when (language) {
        AppLanguage.FRENCH -> Locale.FRANCE
        AppLanguage.ENGLISH -> Locale.UK
        AppLanguage.ARABIC -> Locale.forLanguageTag("ar-MA")
    }

    fun number(value: Double, decimals: Int = 2, language: AppLanguage = AppLanguage.FRENCH): String {
        val safe = if (value.isFinite()) value else 0.0
        return NumberFormat.getNumberInstance(locale(language)).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }.format(safe)
    }

    fun integer(value: Double, language: AppLanguage = AppLanguage.FRENCH): String {
        val safe = if (value.isFinite()) value else 0.0
        return NumberFormat.getIntegerInstance(locale(language)).format(safe.roundToLong())
    }

    /** Example: "1 250,00 MAD"; currency remains an ISO code in every language. */
    fun money(
        amount: Double,
        currency: String? = null,
        withDecimals: Boolean = true,
        language: AppLanguage = AppLanguage.FRENCH,
    ): String {
        val code = currency?.takeIf { it.isNotBlank() } ?: "MAD"
        val formatted = if (withDecimals) number(amount, 2, language) else integer(amount, language)
        val bidiSafeCode = if (language.isRtl) "\u2066$code\u2069" else code
        return "$formatted $bidiSafeCode"
    }

    /** Compact dashboard money; scale words are localized instead of leaking English `k` in Arabic. */
    fun moneyCompact(
        amount: Double,
        currency: String? = null,
        language: AppLanguage = AppLanguage.FRENCH,
    ): String {
        val code = currency?.takeIf { it.isNotBlank() } ?: "MAD"
        val absolute = abs(amount)
        return when {
            absolute >= 1_000_000 -> {
                val scaled = amount / 1_000_000
                val decimals = if (language.isRtl && scaled % 1.0 == 0.0) 0 else 1
                val suffix = if (language.isRtl) "مليون" else "M"
                "${number(scaled, decimals, language)} $suffix ${if (language.isRtl) "\u2066$code\u2069" else code}"
            }
            absolute >= 10_000 -> {
                val scaled = amount / 1_000
                val decimals = if (language.isRtl && scaled % 1.0 == 0.0) 0 else 1
                val suffix = if (language.isRtl) "ألف" else "k"
                "${number(scaled, decimals, language)} $suffix ${if (language.isRtl) "\u2066$code\u2069" else code}"
            }
            else -> money(amount, code, language = language)
        }
    }

    /** Quantities hide trailing zeros: 2.0 → "2", 1.5 → "1,5". */
    fun quantity(value: Double, language: AppLanguage = AppLanguage.FRENCH): String {
        val safe = if (value.isFinite()) value else 0.0
        return if (abs(safe - safe.roundToLong()) < 0.0001) {
            integer(safe, language)
        } else {
            val symbols = java.text.DecimalFormatSymbols.getInstance(locale(language))
            number(safe, 2, language)
                .trimEnd(symbols.zeroDigit)
                .trimEnd(symbols.decimalSeparator)
        }
    }

    fun percent(fraction: Double, language: AppLanguage = AppLanguage.FRENCH): String =
        "${number(fraction * 100, 0, language)} %"

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
