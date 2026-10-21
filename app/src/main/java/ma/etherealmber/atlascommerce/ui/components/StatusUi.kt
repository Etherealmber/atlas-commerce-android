package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Human label + tone for an invoice status coming from the worker. */
fun statusLabel(status: String?): String = when (status?.lowercase()) {
    null, "" -> "—"
    "paid" -> "Payée"
    "partial" -> "Partiellement payée"
    "sent" -> "Envoyée"
    "overdue" -> "En retard"
    "draft" -> "Brouillon"
    "cancelled" -> "Annulée"
    "quote", "devis" -> "Devis"
    "remboursement" -> "Remboursement"
    else -> status.replaceFirstChar { char -> char.uppercase() }
}

fun statusTone(status: String?): AtlasTone = when (status?.lowercase()) {
    "paid" -> AtlasTone.POSITIVE
    "partial" -> AtlasTone.WARNING
    "overdue" -> AtlasTone.DANGER
    "quote", "devis" -> AtlasTone.INFO
    else -> AtlasTone.NEUTRAL
}

@Composable
fun StatusChip(status: String?, modifier: Modifier = Modifier) {
    AtlasChip(text = statusLabel(status), tone = statusTone(status), modifier = modifier)
}
