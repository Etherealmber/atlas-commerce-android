package ma.etherealmber.atlascommerce.ui.screens.products

/** Parses the localized decimal input without allowing malformed or non-finite quantities. */
internal fun parseStockAdjustmentQuantity(input: String): Double? {
    val normalized = input.trim().replace(',', '.')
    if (normalized.isEmpty() || normalized.count { it == '.' } > 1) return null
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
}
