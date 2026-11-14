package ma.etherealmber.atlascommerce.ui.screens.agent

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.CreateProductRequest
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.ProductCreateDialog
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.root.AppViewModel
import ma.etherealmber.atlascommerce.ui.screens.products.ProductsViewModel
import ma.etherealmber.atlascommerce.ui.screens.scan.ScanScreen
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage

/** Role-gated native actions surfaced as chat controls rather than routes to other pages. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AgentInlineActions(
    user: SessionUser,
    appViewModel: AppViewModel,
    onSendPrompt: (String) -> Unit,
    onNavigate: (AgentNavigationSuggestion) -> Unit,
    onToast: (String) -> Unit,
) {
    val productsViewModel: ProductsViewModel = hiltViewModel()
    val productState by productsViewModel.state.collectAsStateWithLifecycle()
    val appLanguage = LocalAtlasLanguage.current
    val context = LocalContext.current
    val uploadingAvatar by appViewModel.uploadingAvatar.collectAsStateWithLifecycle()
    val savingProfile by appViewModel.savingProfile.collectAsStateWithLifecycle()

    var showLanguagePicker by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showProfileEditor by remember { mutableStateOf(false) }
    var showProductForm by remember { mutableStateOf(false) }
    var showStockEditor by remember { mutableStateOf(false) }
    var showScan by remember { mutableStateOf(false) }
    var displayName by remember(user.scopeIdentity, user.displayName) { mutableStateOf(user.displayName) }
    var email by remember(user.scopeIdentity, user.email) { mutableStateOf(user.email) }
    var phone by remember(user.scopeIdentity, user.phone) { mutableStateOf(user.phone) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) appViewModel.uploadAvatarFromUri(context, uri, onToast)
    }
    LaunchedEffect(productsViewModel) {
        productsViewModel.messages.collect { message -> onToast(message) }
    }

    AtlasCard(padding = 12.dp) {
        Text(
            atlasText("Résumé et actions", "Summary and actions", "ملخص وإجراءات"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            atlasText(
                "Les actions restent ici. Les changements de stock et de produit respectent votre rôle.",
                "Actions stay in this chat. Product and stock changes follow your role.",
                "تبقى الإجراءات هنا. تتبع تغييرات المنتجات والمخزون دورك.",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AgentActionChip(atlasText("Mises à jour", "Check updates", "التحديثات")) {
                onSendPrompt("Please check for updates")
            }
            AgentActionChip(atlasText("Thème clair", "Light theme", "المظهر الفاتح")) {
                onSendPrompt("Use the light theme")
            }
            AgentActionChip(atlasText("Thème sombre", "Dark theme", "المظهر الداكن")) {
                onSendPrompt("Use dark mode")
            }
            AgentActionChip(atlasText("Thème système", "System theme", "مظهر النظام")) {
                onSendPrompt("Use the system theme")
            }
            AgentActionChip(atlasText("Langue", "Language", "اللغة")) { showLanguagePicker = true }
            AgentActionChip(atlasText("Vider le cache", "Clear cache", "مسح ذاكرة التخزين")) { showClearCacheConfirm = true }
            AgentActionChip(atlasText("Modifier mon profil", "Edit my profile", "تعديل ملفي")) { showProfileEditor = true }
            AgentActionChip(
                if (uploadingAvatar) atlasText("Envoi de la photo…", "Uploading photo…", "جارٍ رفع الصورة…")
                else atlasText("Changer ma photo", "Change my photo", "تغيير صورتي"),
                enabled = !uploadingAvatar,
            ) { photoPicker.launch("image/*") }
            AgentActionChip(atlasText("Créer un produit", "Create product", "إنشاء منتج")) { showProductForm = true }
            if (Roles.canRequestStockAdjustment(user.role)) {
                AgentActionChip(atlasText("Modifier le stock", "Adjust stock", "تعديل المخزون")) {
                    productsViewModel.onQueryChange("")
                    productsViewModel.refresh()
                    showStockEditor = true
                }
            }
            AgentActionChip(atlasText("Scanner", "Scan", "مسح")) { showScan = true }
        }
    }

    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text(atlasText("Langue de l’application", "App language", "لغة التطبيق")) },
            text = {
                Column {
                    AppLanguage.entries.forEach { language ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                appViewModel.setAppLanguage(language)
                                showLanguagePicker = false
                            },
                        ) {
                            Text(
                                when (language) {
                                    AppLanguage.FRENCH -> "Français"
                                    AppLanguage.ENGLISH -> "English"
                                    AppLanguage.ARABIC -> "العربية"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (language == appLanguage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguagePicker = false }) { Text(atlasText("Annuler", "Cancel", "إلغاء")) } },
        )
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text(atlasText("Vider le cache local ?", "Clear local cache?", "مسح ذاكرة التخزين المحلية؟")) },
            text = {
                Text(atlasText(
                    "Seul le cache hors ligne de ce compte sur cet appareil sera supprimé. Les données du serveur ne seront pas modifiées.",
                    "Only this account’s offline cache on this device will be removed. Server data will not change.",
                    "سيتم مسح ذاكرة هذا الحساب المحلية على هذا الجهاز فقط. لن تتغير بيانات الخادم.",
                ))
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheConfirm = false
                    appViewModel.clearOfflineCache(onToast)
                }) { Text(atlasText("Vider le cache", "Clear cache", "مسح الذاكرة")) }
            },
            dismissButton = { TextButton(onClick = { showClearCacheConfirm = false }) { Text(atlasText("Annuler", "Cancel", "إلغاء")) } },
        )
    }

    if (showProfileEditor) {
        AlertDialog(
            onDismissRequest = { if (!savingProfile) showProfileEditor = false },
            title = { Text(atlasText("Modifier mon profil", "Edit my profile", "تعديل ملفي")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtlasTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = atlasText("Nom affiché", "Display name", "الاسم المعروض"),
                    )
                    AtlasTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = atlasText("E-mail", "Email", "البريد الإلكتروني"),
                    )
                    AtlasTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = atlasText("Téléphone", "Phone", "الهاتف"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !savingProfile && displayName.isNotBlank(),
                    onClick = {
                        appViewModel.updateProfileDetails(displayName, email, phone) { message, saved ->
                            onToast(message)
                            if (saved) showProfileEditor = false
                        }
                    },
                ) { Text(if (savingProfile) atlasText("Enregistrement…", "Saving…", "جارٍ الحفظ…") else atlasText("Enregistrer", "Save", "حفظ")) }
            },
            dismissButton = {
                TextButton(onClick = { showProfileEditor = false }) { Text(atlasText("Annuler", "Cancel", "إلغاء")) }
            },
        )
    }

    if (showProductForm) {
        ProductCreateDialog(
            canCreateDirectly = Roles.canCreateProductDirectly(user.role),
            imageServerBaseUrl = user.serverUrl,
            submitting = productState.submitting,
            onDismiss = { showProductForm = false },
            onSubmitDirect = { request: CreateProductRequest ->
                showProductForm = false
                productsViewModel.createProduct(request)
            },
            onSubmitRequest = { code, name, sku, unit, quantity, category, note, photoUrl ->
                showProductForm = false
                productsViewModel.submitProductRequest(code, name, sku, unit, quantity, category, note, photoUrl)
            },
        )
    }

    if (showStockEditor) {
        AgentStockAdjustmentDialog(
            products = productState.products,
            onDismiss = { showStockEditor = false },
            onSubmit = { product, quantity, out, reason ->
                productsViewModel.adjustStock(product, quantity, out, reason)
                showStockEditor = false
            },
        )
    }

    if (showScan) {
        Dialog(
            onDismissRequest = { showScan = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            androidx.compose.material3.Surface(Modifier.fillMaxSize()) {
                ScanScreen(
                    user = user,
                    isActive = true,
                    onOpenSale = {
                        showScan = false
                        onNavigate(AgentNavigationSuggestion(AgentNavigationDestination.SALES))
                    },
                    onOpenProducts = {
                        showScan = false
                        onNavigate(AgentNavigationSuggestion(AgentNavigationDestination.PRODUCTS))
                    },
                    onOpenQrDevice = { showScan = false },
                    onToast = onToast,
                )
            }
        }
    }
}

@Composable
private fun AgentActionChip(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) }, enabled = enabled)
}

@Composable
private fun AgentStockAdjustmentDialog(
    products: List<ProductDto>,
    onDismiss: () -> Unit,
    onSubmit: (ProductDto, Double, Boolean, String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<ProductDto?>(null) }
    var quantityText by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }
    var takeOut by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    val filteredProducts = remember(products, query) {
        products.filter { product ->
            product.trackInventory && (query.isBlank() || listOfNotNull(product.name, product.sku, product.barcode)
                .any { it.contains(query.trim(), ignoreCase = true) })
        }.take(30)
    }
    val quantity = quantityText.replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preview) atlasText("Vérifier la demande", "Review stock change", "مراجعة تغيير المخزون") else atlasText("Ajuster le stock", "Adjust stock", "تعديل المخزون")) },
        text = {
            if (preview) {
                val product = selected
                Text(
                    if (product == null || quantity == null) ""
                    else atlasText(
                        "${if (takeOut) "Retirer" else "Ajouter"} ${quantity} ${product.unit} · ${product.name}. Votre rôle déterminera si l’ajustement est appliqué directement ou soumis à validation.",
                        "${if (takeOut) "Remove" else "Add"} $quantity ${product.unit} · ${product.name}. Your role determines whether this is applied directly or sent for approval.",
                        "${if (takeOut) "إزالة" else "إضافة"} $quantity ${product.unit} · ${product.name}. يحدد دورك ما إذا كان التعديل سينفذ مباشرة أو يرسل للموافقة.",
                    ),
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AtlasTextField(query, { query = it }, atlasText("Rechercher un produit", "Find a product", "البحث عن منتج"))
                    if (selected == null) {
                        filteredProducts.forEach { product ->
                            TextButton(onClick = { selected = product }) {
                                Text("${product.name} · ${atlasText("Stock", "Stock", "المخزون")} ${product.stock} ${product.unit}")
                            }
                        }
                    } else {
                        TextButton(onClick = { selected = null }) {
                            Text(atlasText("Produit : ${selected?.name} (changer)", "Product: ${selected?.name} (change)", "المنتج: ${selected?.name} (تغيير)"))
                        }
                        AtlasTextField(quantityText, { quantityText = it }, atlasText("Quantité", "Quantity", "الكمية"))
                        AtlasTextField(reason, { reason = it }, atlasText("Motif", "Reason", "السبب"), singleLine = false)
                        TextButton(onClick = { takeOut = !takeOut }) {
                            Text(if (takeOut) atlasText("Retrait sélectionné · toucher pour ajouter", "Removal selected · tap to add", "تم اختيار الإزالة · اضغط للإضافة") else atlasText("Ajout sélectionné · toucher pour retirer", "Addition selected · tap to remove", "تم اختيار الإضافة · اضغط للإزالة"))
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (preview) {
                TextButton(onClick = {
                    val product = selected
                    val amount = quantity
                    if (product != null && amount != null && amount > 0.0 && reason.isNotBlank()) {
                        onSubmit(product, amount, takeOut, reason.trim())
                    }
                }) { Text(atlasText("Confirmer", "Confirm", "تأكيد")) }
            } else {
                TextButton(
                    enabled = selected != null && quantity != null && quantity > 0.0 && reason.isNotBlank(),
                    onClick = { preview = true },
                ) { Text(atlasText("Aperçu", "Preview", "معاينة")) }
            }
        },
        dismissButton = {
            TextButton(onClick = if (preview) ({ preview = false }) else onDismiss) {
                Text(if (preview) atlasText("Modifier", "Edit", "تعديل") else atlasText("Annuler", "Cancel", "إلغاء"))
            }
        },
    )
}
