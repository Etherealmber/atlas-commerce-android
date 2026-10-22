package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
 * Shared dialog for adding a product (`POST /api/products` strictly for Admins) or
 * automatically escalating the complete product creation request straight to an Admin
 * when the signed-in user does NOT hold the Admin role.
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
    // STRICT ROLE POLICY:
    // - Admins (`canCreateDirectly == true`) can create directly in stock OR record a bureau request.
    // - Non-Admins (`canCreateDirectly == false`) NEVER create directly; their full fiche is
    //   immediately dispatched straight to Admins (`onSubmitDirect` -> `repo.submitProductForRole`
    //   which routes non-admin calls directly to Admin notifications + stock requests).
    var adminWantsRequestOnly by remember(canCreateDirectly) { mutableStateOf(false) }
    val modeDirect = canCreateDirectly && !adminWantsRequestOnly

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

    val reqNameOk = name.trim().length in 1..200
    val reqCodeOrSkuOk = if (modeDirect) {
        sku.trim().isNotBlank()
    } else {
        barcode.trim().isNotBlank() || sku.trim().isNotBlank()
    }
    val reqUnitOk = unit.trim().isNotBlank()
    val reqQtyOk = parsedQty != null && parsedQty >= 0.0
    val reqPriceOk = parsedPrice != null && parsedPrice >= 0.0

    val checks = listOf(
        "Nom *" to reqNameOk,
        (if (modeDirect) "SKU *" else "Code/SKU *") to reqCodeOrSkuOk,
        "Unité *" to reqUnitOk,
        "Qté *" to reqQtyOk,
        "Prix *" to reqPriceOk,
    )

    val completedCount = checks.count { it.second }
    val allValid = checks.all { it.second }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (canCreateDirectly && modeDirect) {
                        "Nouveau produit (Création Admin)"
                    } else {
                        "Nouveau produit · Envoi à l'Admin"
                    },
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
                if (!canCreateDirectly) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = toneBackground(AtlasTone.INFO),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AdminPanelSettings,
                                contentDescription = null,
                                tint = toneForeground(AtlasTone.INFO),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Création directe réservée à l'Admin : cette fiche est envoyée immédiatement à l'Administrateur pour validation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = toneForeground(AtlasTone.INFO),
                            )
                        }
                    }
                }

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
                            selected = !adminWantsRequestOnly,
                            onClick = { adminWantsRequestOnly = false },
                            label = { Text("Créer au stock") },
                        )
                        FilterChip(
                            selected = adminWantsRequestOnly,
                            onClick = { adminWantsRequestOnly = true },
                            label = { Text("Demande en file") },
                        )
                    }
                }

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text(if (modeDirect) "Code-barres (EAN / QR)" else "Code-barres / EAN *") },
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
                        label = { Text("Référence SKU *") },
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
                    OutlinedTextField(
                        value = rawPrice,
                        onValueChange = { s -> rawPrice = s.filter { it.isDigit() || it == '.' || it == ',' } },
                        label = { Text("Prix MAD *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Catégorie") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
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

                if (!modeDirect) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note pour l'Administrateur") },
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
                    val generatedSku = sku.trim().ifBlank {
                        "SKU-${barcode.takeLast(6).ifBlank { (1000..9999).random().toString() }}"
                    }
                    if (!canCreateDirectly || modeDirect) {
                        // Calls `onSubmitDirect` which routes through `repo.submitProductForRole(...)`:
                        // - Admin: creates immediately (`POST /api/products`).
                        // - Non-Admin: immediately notifies Admin (`POST /api/mobile-actions` + `product-requests`)!
                        onSubmitDirect(
                            CreateProductRequest(
                                name = name.trim(),
                                sku = generatedSku,
                                barcode = barcode.trim().ifBlank { null },
                                category = category.trim().ifBlank { null },
                                unit = unit.trim().ifBlank { "unité" },
                                price = parsedPrice ?: 0.0,
                                stockQuantity = qty,
                                lowStockThreshold = parsedThreshold,
                                description = note.trim().ifBlank { null },
                            ),
                        )
                    } else {
                        onSubmitRequest(
                            barcode.trim().ifBlank { generatedSku },
                            name.trim(),
                            generatedSku,
                            unit.trim().ifBlank { "unité" },
                            qty,
                            category.trim().ifBlank { null },
                            note.trim().ifBlank { null },
                        )
                    }
                },
            ) {
                if (!canCreateDirectly) {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                Text(
                    when {
                        !canCreateDirectly -> "Envoyer à l'Admin"
                        modeDirect -> "Créer le produit"
                        else -> "Enregistrer la demande"
                    },
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !submitting) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text("Annuler")
            }
        },
    )
}
