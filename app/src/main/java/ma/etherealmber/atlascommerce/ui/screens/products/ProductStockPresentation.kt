package ma.etherealmber.atlascommerce.ui.screens.products

import androidx.compose.runtime.Composable
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.i18n.atlasQuantity

/** Shows the one primary stock balance and its derived secondary-unit equivalent, when configured. */
@Composable
internal fun productStockDisplay(
    quantity: Double,
    unit: String?,
    secondaryUnit: String? = null,
    secondaryUnitFactor: Double? = null,
): String {
    val amount = atlasQuantity(quantity)
    val cleanUnit = unit?.trim()?.takeIf(String::isNotEmpty)
    val primary = if (cleanUnit == null) amount else "$amount $cleanUnit"
    val cleanSecondary = secondaryUnit?.trim()?.takeIf(String::isNotEmpty)
    val converted = convertedSecondaryStock(quantity, secondaryUnitFactor)
    return if (cleanSecondary != null && converted != null) {
        "$primary · ${atlasQuantity(converted)} $cleanSecondary"
    } else {
        primary
    }
}

internal fun convertedSecondaryStock(quantity: Double, factor: Double?): Double? =
    factor?.takeIf { it.isFinite() && it > 0.0 }?.let { quantity * it }

/** Green above 40, orange from 20 through 40, and red below 20. */
internal fun productStockTone(quantity: Double, trackInventory: Boolean = true): AtlasTone = when {
    !trackInventory -> AtlasTone.NEUTRAL
    quantity < 20.0 -> AtlasTone.DANGER
    quantity <= 40.0 -> AtlasTone.WARNING
    else -> AtlasTone.POSITIVE
}
