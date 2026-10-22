package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.data.model.CreateProductRequest
import java.util.Locale

private val StandardUnits = listOf("unité", "pièce", "m²", "kg", "carton")

/**
 * Shared dialog for adding a new product (`POST /api/products`) or submitting a
 * field creation request (`POST /api/stock/product-requests`), dynamically displaying
 * every required field (`Éléments requis`) and completion progress.
 */
@Composable
fun ProductCreateDialog(
    canCreateDirectly: Boolean,
    initialBarcode: String = "",
    initialName: String = "",
    initialSku: String = "",
    initialUnit: String = "unité",
    initialQuantity: String = "1",
    initialCategory: String = "",
    submitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmitDirect: (CreateProductRequest) -> Unit,
    onSubmitRequest: (
        code: String,
        name: String,
        sku: String?,
        unit: String,
        quantity: Double,
        category: String?,
        note: String?,
    ) -> Unit,
) {
    var modeDirect by remember(canCreateDirectly) { mutableStateOf(canCreateDirectly) }
    var barcode by remember(initialBarcode) { mutableStateOf(initialBarcode) }
    var name by remember(initialName) { mutableStateOf(initialName) }
    var sku by remember(initialSku) {
        mutableStateOf(
            initialSku.ifBlank {
                if (initialBarcode.isNotBlank()) "SKU-${initialBarcode.takeLast(6)}" else ""
            },
        )
    }
    var unit by remember(initialUnit) { mutableStateOf(initialUnit.ifBlank { "unité" }) }
    var rawQty by remember(initialQuantity) { mutableStateOf(initialQuantity) }
    var rawPrice by remember { mutableStateOf("0") }
    var rawThreshold by remember { mutableStateOf("5") }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var note by remember { mutableStateOf("") }

    val parsedQty = rawQty.replace(',', '.').toDoubleOrNull()
    val parsedPrice = rawPrice.replace(',', '.').toDoubleOrNull()
    val parsedThreshold = rawThreshold.replace(',', '.').toDoubleOrNull() ?: 5.0

    val reqBarcodeOk = modeDirect || barcode.isNotBlank()
    val reqNameOk = name.trim().length in 1..200
    val reqSkuOk = !modeDirect || sku.trim().isNotBlank()
    val reqUnitOk = unit.trim().isNotBlank()
    val reqQtyOk = parsedQty != null && parsedQty >= 0.0
    val reqPriceOk = !modeDirect || (parsedPrice != null && parsedPrice >= 0.0)

    val checks = listOf(
        "Nom *" to reqNameOk,
        (if (modeDirect) "SKU *" else "Code *") to (if (modeDirect) reqSkuOk else reqBarcodeOk),
        "Unité *" to reqUnitOk,
        "Qté *" to reqQtyOk,
    ) + if (modeDirect) listOf("Prix *" to reqPriceOk) else emptyList()

    val completedCount = checks.count { it.second }
    val allValid = checks.all { it.second } && reqBarcodeOk && reqSkuOk && reqPriceOk

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (modeDirect) "Nouveau produit catalogue" else "Demande d'ajout produit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Éléments requis : $completedCount/${checks.size} complétés",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (allValid) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Live checklist chips showing what is required vs satisfied
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    checks.take(3).forEach { (label, ok) ->
                        AtlasChip(
                            text = label,
                            tone = if (ok) AtlasTone.POSITIVE else AtlasTone.WARNING,
                            icon = if (ok) Icons.Outlined.CheckCircle else null,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    checks.drop(3).forEach { (label, ok) ->
                        AtlasChip(
                            text = label,
                            tone = if (ok) AtlasTone.POSITIVE else AtlasTone.WARNING,
                            icon = if (ok) Icons.Outlined.CheckCircle else null,
                        )
                    }
                }

                if (canCreateDirectly) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = modeDirect,
                            onClick = { modeDirect = true },
                            label = { Text("Créer au stock") },
                        )
                        FilterChip(
                            selected = !modeDirect,
                            onClick = { modeDirect = false },
                            label = { Text("Demande bureau") },
                        )
                    }
                }

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text(if (modeDirect) "Code-barres (EAN / QR)" else "Code-barres *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du produit *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text(if (modeDirect) "Référence SKU *" else "SKU (facultatif)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            val base = name.trim()
                                .uppercase(Locale.ROOT)
                                .replace(Regex("[^A-Z0-9]+"), "-")
                                .trim('-')
                                .take(8)
                                .ifBlank { "ART" }
                            val tail = barcode.takeLast(4).ifBlank { (1000..9999).random().toString() }
                            sku = "$base-$tail"
                        },
                    ) {
                        Icon(Icons.Outlined.AutoFixHigh, contentDescription = "Générer un SKU")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StandardUnits.take(4).forEach { candidate ->
                        FilterChip(
                            selected = unit.equals(candidate, ignoreCase = true),
                            onClick = { unit = candidate },
                            label = { Text(candidate) },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rawQty,
                        onValueChange = { s -> rawQty = s.filter { it.isDigit() || it == '.' || it == ',' } },
                        label = { Text("Quantité *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    if (modeDirect) {
                        OutlinedTextField(
                            value = rawPrice,
                            onValueChange = { s -> rawPrice = s.filter { it.isDigit() || it == '.' || it == ',' } },
                            label = { Text("Prix MAD *") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unité *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Catégorie") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    if (modeDirect) {
                        OutlinedTextField(
                            value = rawThreshold,
                            onValueChange = { s ->
                                rawThreshold = s.filter { it.isDigit() || it == '.' || it == ',' }
                            },
                            label = { Text("Seuil alerte") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (!modeDirect) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note pour le bureau") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = allValid && !submitting,
                onClick = {
                    val qty = parsedQty ?: 0.0
                    if (modeDirect) {
                        onSubmitDirect(
                            CreateProductRequest(
                                name = name.trim(),
                                sku = sku.trim(),
                                barcode = barcode.trim().ifBlank { null },
                                category = category.trim().ifBlank { null },
                                unit = unit.trim().ifBlank { "unité" },
                                price = parsedPrice ?: 0.0,
                                stockQuantity = qty,
                                lowStockThreshold = parsedThreshold,
                            ),
                        )
                    } else {
                        onSubmitRequest(
                            barcode.trim(),
                            name.trim(),
                            sku.trim().ifBlank { null },
                            unit.trim().ifBlank { "unité" },
                            qty,
                            category.trim().ifBlank { null },
                            note.trim().ifBlank { null },
                        )
                    }
                },
            ) {
                Text(if (modeDirect) "Enregistrer" else "Envoyer la demande")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text("Annuler")
            }
        },
    )
}
