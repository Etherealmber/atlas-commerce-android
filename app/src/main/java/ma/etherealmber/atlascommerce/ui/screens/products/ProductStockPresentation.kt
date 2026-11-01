package ma.etherealmber.atlascommerce.ui.screens.products

import androidx.compose.runtime.Composable
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.i18n.atlasQuantity

@Composable
internal fun productStockDisplay(quantity: Double, unit: String?): String {
    val amount = atlasQuantity(quantity)
    val cleanUnit = unit?.trim()?.takeIf(String::isNotEmpty)
    return if (cleanUnit == null) amount else "$amount $cleanUnit"
}

/** Stock badge colors requested for the catalog: <10 red, 10–20 orange, >20 green. */
internal fun productStockTone(quantity: Double, trackInventory: Boolean = true): AtlasTone = when {
    !trackInventory -> AtlasTone.NEUTRAL
    quantity < 10.0 -> AtlasTone.DANGER
    quantity <= 20.0 -> AtlasTone.WARNING
    else -> AtlasTone.POSITIVE
}
