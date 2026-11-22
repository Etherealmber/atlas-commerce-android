package ma.etherealmber.atlascommerce.ui.components

import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.data.model.CreateProductRequest
import java.util.Locale


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
    initialSecondaryUnit: String = "",
    initialSecondaryUnitFactor: String = "",
    initialQuantity: String = "0",
    initialPrice: String = "",
    initialInvoicePriceMode: String = "exclusive",
    initialLowStockThreshold: String = "5",
    initialCategory: String = "",
    initialImageUrl: String? = null,
    formTitle: String? = null,
    requesterName: String? = null,
    allowRequestOnly: Boolean = true,
    imageServerBaseUrl: String? = null,
    submitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmitDirect: (CreateProductRequest) -> Unit,
    onSubmitRequest: (
        code: String,
        name: String,
        sku: String?,
        unit: String,
        secondaryUnit: String?,
        secondaryUnitFactor: Double?,
        quantity: Double,
        category: String?,
        note: String?,
        photoUrl: String?,
    ) -> Unit,
) {
    // STRICT ROLE POLICY:
    // - Admins (`canCreateDirectly == true`) can create directly in stock OR record a bureau request.
    // - Non-Admins (`canCreateDirectly == false`) NEVER create directly; their full fiche is
    //   immediately dispatched straight to Admins (`onSubmitDirect` -> `repo.submitProductForRole`
    //   which routes non-admin calls directly to Admin notifications + stock requests).
    var adminWantsRequestOnly by remember(canCreateDirectly, allowRequestOnly) { mutableStateOf(false) }
    val modeDirect = canCreateDirectly && (!allowRequestOnly || !adminWantsRequestOnly)
    val unitCatalogViewModel: ProductUnitCatalogViewModel = hiltViewModel()
    val unitOptions by unitCatalogViewModel.units.collectAsState()
    LaunchedEffect(unitCatalogViewModel) { unitCatalogViewModel.load() }

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
    val initialSecondaryMatchesPrimary = initialSecondaryUnit.isNotBlank() &&
        initialSecondaryUnit.trim().equals(initialUnit.trim(), ignoreCase = true)
    var secondaryUnit by remember(initialSecondaryUnit, initialUnit) {
        mutableStateOf(if (initialSecondaryMatchesPrimary) "" else initialSecondaryUnit)
    }
    var secondaryUnitFactorText by remember(initialSecondaryUnitFactor, initialSecondaryUnit, initialUnit) {
        mutableStateOf(if (initialSecondaryMatchesPrimary) "" else initialSecondaryUnitFactor)
    }
    var rawQty by remember(initialQuantity) { mutableStateOf(initialQuantity) }
    var rawPrice by remember(initialPrice) { mutableStateOf(initialPrice) }
    var invoicePriceMode by remember(initialInvoicePriceMode) { mutableStateOf(initialInvoicePriceMode) }
    var rawThreshold by remember(initialLowStockThreshold) { mutableStateOf(initialLowStockThreshold) }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var note by remember { mutableStateOf("") }
    var photoUrl by remember(initialImageUrl) { mutableStateOf(initialImageUrl) }
    var encodingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val photoScope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            photoScope.launch {
                encodingPhoto = true
                val encoded = encodeProductImageDataUri(context, uri)
                photoUrl = encoded
                photoError = encoded == null
                encodingPhoto = false
            }
        }
    }

    val parsedQty = rawQty.replace(',', '.').toDoubleOrNull()
    val parsedPrice = rawPrice.replace(',', '.').toDoubleOrNull()
    val parsedSecondaryUnitFactor = secondaryUnitFactorText.replace(',', '.').toDoubleOrNull()
    val secondaryUnitsDiffer = secondaryUnit.isBlank() || !secondaryUnit.trim().equals(unit.trim(), ignoreCase = true)
    val secondaryUnitValid = secondaryUnitsDiffer && when {
        secondaryUnit.isBlank() && secondaryUnitFactorText.isBlank() -> true
        secondaryUnit.isNotBlank() && parsedSecondaryUnitFactor != null -> parsedSecondaryUnitFactor.isFinite() && parsedSecondaryUnitFactor > 0.0
        else -> false
    }
    val submittedSecondaryUnit = secondaryUnit.trim().takeIf { it.isNotEmpty() }
    val submittedSecondaryUnitFactor = parsedSecondaryUnitFactor?.takeIf {
        submittedSecondaryUnit != null && it.isFinite() && it > 0.0
    }
    val onPrimaryUnitChange: (String) -> Unit = { selected ->
        unit = selected
        if (secondaryUnit.isNotBlank() && selected.isNotBlank() && secondaryUnit.trim().equals(selected.trim(), ignoreCase = true)) {
            secondaryUnit = ""
            secondaryUnitFactorText = ""
        }
    }
    val onSecondaryUnitChange: (String) -> Unit = { selected ->
        secondaryUnit = selected
        if (selected.isBlank()) secondaryUnitFactorText = ""
    }
    val parsedThreshold = rawThreshold.replace(',', '.').toDoubleOrNull() ?: 5.0

    val reqNameOk = name.trim().length in 1..200
    val reqCodeOrSkuOk = if (!canCreateDirectly || modeDirect) {
        sku.trim().isNotBlank()
    } else {
        barcode.trim().isNotBlank() || sku.trim().isNotBlank()
    }
    val reqUnitOk = unit.trim().isNotBlank()
    val reqQtyOk = parsedQty != null && parsedQty >= 0.0
    val reqPriceOk = !modeDirect || (parsedPrice != null && parsedPrice >= 0.0)

    val checks = buildList {
        add("Nom *" to reqNameOk)
        add((if (!canCreateDirectly || modeDirect) "SKU *" else "Code/SKU *") to reqCodeOrSkuOk)
        add("Unité *" to reqUnitOk)
        add("Qté *" to reqQtyOk)
        add("Unité secondaire / facteur" to secondaryUnitValid)
        if (modeDirect) add("Prix *" to reqPriceOk)
    }

    val completedCount = checks.count { it.second }
    val allValid = checks.all { it.second }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formTitle ?: if (canCreateDirectly && modeDirect) {
                        atlasText("Nouveau produit (Création Admin)")
                    } else {
                        atlasText("Nouveau produit · Envoi à l'Admin")
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = atlasText("Éléments requis : $completedCount/${checks.size} complétés", "Required items: $completedCount/${checks.size} complete", "العناصر المطلوبة: $completedCount/${checks.size} مكتملة"),
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
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                tint = toneForeground(AtlasTone.INFO),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = atlasText(
                                    "Demandeur : ${requesterName?.takeIf { it.isNotBlank() } ?: "Compte connecté"}. Nom affiché depuis votre profil, sans saisir ni transmettre un nom manuellement.",
                                    "Requester: ${requesterName?.takeIf { it.isNotBlank() } ?: "Signed-in account"}. Displayed from your profile; no requester name is entered or sent manually.",
                                    "مقدم الطلب: ${requesterName?.takeIf { it.isNotBlank() } ?: "الحساب المتصل"}. الاسم معروض من ملفك الشخصي ولا يُدخل أو يُرسل يدوياً.",
                                ),
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

                if (canCreateDirectly && allowRequestOnly) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !adminWantsRequestOnly,
                            onClick = { adminWantsRequestOnly = false },
                            label = { Text(atlasText("Créer au stock")) },
                        )
                        FilterChip(
                            selected = adminWantsRequestOnly,
                            onClick = { adminWantsRequestOnly = true },
                            label = { Text(atlasText("Demande en file")) },
                        )
                    }
                }

                if (canCreateDirectly) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = {
                            Text(
                                if (modeDirect) atlasText("Code-barres (EAN / QR)") else atlasText("Code-barres / EAN *"),
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(atlasText("Nom du produit *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text(atlasText("Référence SKU *")) },
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
                        Icon(Icons.Outlined.AutoFixHigh, contentDescription = atlasText("Générer un SKU"))
                    }
                }

                ProductUnitDropdown(
                    value = unit,
                    options = unitOptions,
                    label = atlasText("Unité principale *", "Primary unit *", "الوحدة الأساسية *"),
                    allowNone = false,
                    onValueChange = onPrimaryUnitChange,
                )
                ProductUnitDropdown(
                    value = secondaryUnit,
                    options = unitOptions,
                    label = atlasText("Unité secondaire (facultative)", "Secondary unit (optional)", "الوحدة الثانوية (اختيارية)"),
                    allowNone = true,
                    disabledUnit = unit,
                    isError = !secondaryUnitsDiffer,
                    onValueChange = onSecondaryUnitChange,
                )
                if (!secondaryUnitsDiffer) {
                    Text(
                        atlasText("Choisissez une unité secondaire différente de l'unité principale.", "Choose a secondary unit different from the primary unit.", "اختر وحدة ثانوية مختلفة عن الوحدة الأساسية."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (secondaryUnit.isNotBlank()) {
                    OutlinedTextField(
                        value = secondaryUnitFactorText,
                        onValueChange = { value -> secondaryUnitFactorText = value.filter { it.isDigit() || it == '.' || it == ',' } },
                        label = { Text(atlasText("Facteur secondaire *", "Secondary-unit factor *", "معامل الوحدة الثانوية *")) },
                        singleLine = true,
                        isError = !secondaryUnitValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    atlasText(
                        "Facteur = unités secondaires par unité principale (ex. 1 carton = 1,59 m²). Stock et prix restent en unité principale.",
                        "Factor = secondary units per primary unit (e.g. 1 carton = 1.59 m²). Stock and prices stay in the primary unit.",
                        "المعامل = عدد الوحدات الثانوية لكل وحدة أساسية. يبقى المخزون والسعر بالوحدة الأساسية.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProductArtwork(
                        imageUrl = photoUrl,
                        serverBaseUrl = imageServerBaseUrl,
                        label = name.ifBlank { barcode.ifBlank { "Atlas" } },
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            atlasText("Photo du produit (facultative)", "Product photo (optional)", "صورة المنتج (اختيارية)"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            atlasText("Ajoutez une image si vous le souhaitez.", "Add an image if you like.", "أضف صورة إذا رغبت."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (encodingPhoto) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { photoPicker.launch("image/*") }) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = atlasText("Choisir une photo", "Choose a photo", "اختيار صورة"))
                        }
                    }
                    if (photoUrl != null) {
                        IconButton(onClick = { photoUrl = null }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = atlasText("Retirer la photo", "Remove photo", "إزالة الصورة"))
                        }
                    }
                }
                if (photoError) {
                    Text(
                        atlasText("Impossible de lire cette image. Essayez un autre fichier.", "Could not read that image. Try another file.", "تعذرت قراءة الصورة. جرّب ملفاً آخر."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (modeDirect) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rawQty,
                            onValueChange = { s -> rawQty = s.filter { it.isDigit() || it == '.' || it == ',' } },
                            label = { Text(atlasText("Quantité * · ${unit.ifBlank { "unité" }}", "Quantity * · ${unit.ifBlank { "unit" }}", "الكمية * · ${unit.ifBlank { "وحدة" }}")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = rawPrice,
                            onValueChange = { s -> rawPrice = s.filter { it.isDigit() || it == '.' || it == ',' } },
                            label = { Text(atlasText("Prix MAD *")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = invoicePriceMode == "exclusive",
                            onClick = { invoicePriceMode = "exclusive" },
                            label = { Text(atlasText("HT · TVA ajoutée", "Exclusive · VAT added", "غير شامل · تُضاف الضريبة")) },
                        )
                        FilterChip(
                            selected = invoicePriceMode == "inclusive",
                            onClick = { invoicePriceMode = "inclusive" },
                            label = { Text(atlasText("TTC · TVA incluse", "Inclusive · VAT included", "شامل · الضريبة مضمنة")) },
                        )
                    }
                    Text(
                        if (invoicePriceMode == "exclusive") {
                            atlasText("Le prix saisi est hors taxe ; la TVA sera ajoutée sur les factures.", "The entered price excludes VAT; VAT will be added to invoices.", "السعر المدخل غير شامل للضريبة؛ ستُضاف الضريبة إلى الفواتير.")
                        } else {
                            atlasText("Le prix saisi inclut déjà la TVA.", "The entered price already includes VAT.", "السعر المدخل يشمل الضريبة بالفعل.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedTextField(
                        value = rawQty,
                        onValueChange = { s -> rawQty = s.filter { it.isDigit() || it == '.' || it == ',' } },
                        label = { Text(atlasText("Quantité * · ${unit.ifBlank { "unité" }}", "Quantity * · ${unit.ifBlank { "unit" }}", "الكمية * · ${unit.ifBlank { "وحدة" }}")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (canCreateDirectly) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text(atlasText("Catégorie")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = rawThreshold,
                            onValueChange = { s ->
                                rawThreshold = s.filter { it.isDigit() || it == '.' || it == ',' }
                            },
                            label = { Text(atlasText("Seuil alerte")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (canCreateDirectly && !modeDirect) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(atlasText("Note pour l'Administrateur")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = allValid && !submitting && !encodingPhoto,
                onClick = {
                    val qty = parsedQty ?: 0.0
                    val generatedSku = sku.trim().ifBlank {
                        "SKU-${barcode.takeLast(6).ifBlank { (1000..9999).random().toString() }}"
                    }
                    if (!canCreateDirectly) {
                        // Repository sends only name, SKU, unit, stock and optional image data;
                        // requester identity comes from the authenticated server session.
                        onSubmitDirect(
                            CreateProductRequest(
                                name = name.trim(),
                                sku = sku.trim(),
                                unit = unit.trim().ifBlank { "unité" },
                                secondaryUnit = submittedSecondaryUnit,
                                secondaryUnitFactor = submittedSecondaryUnitFactor,
                                stockQuantity = qty,
                                imageUrl = photoUrl,
                            ),
                        )
                    } else if (modeDirect) {
                        onSubmitDirect(
                            CreateProductRequest(
                                name = name.trim(),
                                sku = generatedSku,
                                barcode = barcode.trim().ifBlank { null },
                                category = category.trim().ifBlank { null },
                                unit = unit.trim().ifBlank { "unité" },
                                secondaryUnit = submittedSecondaryUnit,
                                secondaryUnitFactor = submittedSecondaryUnitFactor,
                                price = parsedPrice ?: 0.0,
                                invoicePriceMode = invoicePriceMode,
                                stockQuantity = qty,
                                lowStockThreshold = parsedThreshold,
                                imageUrl = photoUrl,
                            ),
                        )
                    } else {
                        onSubmitRequest(
                            barcode.trim().ifBlank { generatedSku },
                            name.trim(),
                            generatedSku,
                            unit.trim().ifBlank { "unité" },
                            submittedSecondaryUnit,
                            submittedSecondaryUnitFactor,
                            qty,
                            category.trim().ifBlank { null },
                            note.trim().ifBlank { null },
                            photoUrl,
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
                        !canCreateDirectly -> atlasText("Envoyer à l'Admin")
                        modeDirect -> atlasText("Créer le produit")
                        else -> atlasText("Enregistrer la demande")
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
                Text(atlasText("Annuler"))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductUnitDropdown(
    value: String,
    options: List<String>,
    label: String,
    allowNone: Boolean,
    disabledUnit: String? = null,
    isError: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember(label) { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { typed ->
                onValueChange(typed)
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            isError = isError,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = if (expanded) 180f else 0f },
                )
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (allowNone) {
                DropdownMenuItem(
                    text = { Text(atlasText("Aucune", "None", "بدون")) },
                    onClick = {
                        onValueChange("")
                        expanded = false
                    },
                )
            }
            options.forEach { option ->
                val disabled = !disabledUnit.isNullOrBlank() &&
                    option.trim().equals(disabledUnit.trim(), ignoreCase = true)
                DropdownMenuItem(
                    text = { Text(option) },
                    enabled = !disabled,
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(atlasText("Autre unité…", "Other unit…", "وحدة أخرى…")) },
                onClick = {
                    onValueChange("")
                    expanded = false
                },
            )
        }
    }
}
