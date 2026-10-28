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
 * Locale-aware formatting used throughout Atlas. Server currency codes remain authoritative;
 * Arabic output uses locale digits/separators and displays MAD with Morocco's Arabic abbreviation.
 * Other ISO codes are bidi-isolated. Values are rounded only for display and are never used to
 * derive financial totals.
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

    /** Display-only currency labels; Arabic uses the conventional Moroccan abbreviation for MAD. */
    private fun currencyLabel(code: String, language: AppLanguage): String =
        if (language == AppLanguage.ARABIC && code.equals("MAD", ignoreCase = true)) "د.م."
        else if (language.isRtl) "\u2066$code\u2069" else code

    /** Example: "1 250,00 MAD"; Arabic MAD uses the localized Moroccan label د.م. */
    fun money(
        amount: Double,
        currency: String? = null,
        withDecimals: Boolean = true,
        language: AppLanguage = AppLanguage.FRENCH,
    ): String {
        val code = currency?.takeIf { it.isNotBlank() } ?: "MAD"
        val formatted = if (withDecimals) number(amount, 2, language) else integer(amount, language)
        return "$formatted ${currencyLabel(code, language)}"
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
                "${number(scaled, decimals, language)} $suffix ${currencyLabel(code, language)}"
            }
            absolute >= 10_000 -> {
                val scaled = amount / 1_000
                val decimals = if (language.isRtl && scaled % 1.0 == 0.0) 0 else 1
                val suffix = if (language.isRtl) "ألف" else "k"
                "${number(scaled, decimals, language)} $suffix ${currencyLabel(code, language)}"
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

    fun dateTime(iso: String?, language: AppLanguage = AppLanguage.FRENCH): String = format(iso, "dd/MM/yyyy HH:mm", language)

    fun date(iso: String?, language: AppLanguage = AppLanguage.FRENCH): String = format(iso, "dd MMM yyyy", language)

    fun shortDate(iso: String?, language: AppLanguage = AppLanguage.FRENCH): String = format(iso, "dd/MM/yy", language)

    private fun format(iso: String?, pattern: String, language: AppLanguage): String {
        if (iso.isNullOrBlank()) return "—"
        val instant = parseInstant(iso) ?: return iso.take(10)
        val zone = ZoneId.systemDefault()
        return DateTimeFormatter.ofPattern(pattern, locale(language)).format(instant.atZone(zone))
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

    /** Relative wording for lists localized with the selected app language. */
    fun relative(iso: String?, language: AppLanguage = AppLanguage.FRENCH): String {
        val instant = iso?.let { parseInstant(it) } ?: return "—"
        val seconds = (System.currentTimeMillis() - instant.toEpochMilli()) / 1000
        return when {
            seconds < 60 -> when (language) {
                AppLanguage.FRENCH -> "à l'instant"
                AppLanguage.ENGLISH -> "just now"
                AppLanguage.ARABIC -> "الآن"
            }
            seconds < 3_600 -> {
                val count = number((seconds / 60).toDouble(), 0, language)
                when (language) {
                    AppLanguage.FRENCH -> "il y a $count min"
                    AppLanguage.ENGLISH -> "$count min ago"
                    AppLanguage.ARABIC -> "قبل $count د"
                }
            }
            seconds < 86_400 -> {
                val count = number((seconds / 3_600).toDouble(), 0, language)
                when (language) {
                    AppLanguage.FRENCH -> "il y a $count h"
                    AppLanguage.ENGLISH -> "$count hr ago"
                    AppLanguage.ARABIC -> "قبل $count س"
                }
            }
            seconds < 172_800 -> when (language) {
                AppLanguage.FRENCH -> "hier"
                AppLanguage.ENGLISH -> "yesterday"
                AppLanguage.ARABIC -> "أمس"
            }
            seconds < 604_800 -> {
                val count = number((seconds / 86_400).toDouble(), 0, language)
                when (language) {
                    AppLanguage.FRENCH -> "il y a $count j"
                    AppLanguage.ENGLISH -> "$count days ago"
                    AppLanguage.ARABIC -> "قبل $count أيام"
                }
            }
            else -> date(iso, language)
        }
    }

}
