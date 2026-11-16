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

/** Green above 40, orange from 20 through 40, and red below 20. */
internal fun productStockTone(quantity: Double, trackInventory: Boolean = true): AtlasTone = when {
    !trackInventory -> AtlasTone.NEUTRAL
    quantity < 20.0 -> AtlasTone.DANGER
    quantity <= 40.0 -> AtlasTone.WARNING
    else -> AtlasTone.POSITIVE
}
