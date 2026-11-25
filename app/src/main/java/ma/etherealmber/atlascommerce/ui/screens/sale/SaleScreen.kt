package ma.etherealmber.atlascommerce.ui.screens.sale

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import ma.etherealmber.atlascommerce.data.ThermalPrinter
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoneyCompact
import ma.etherealmber.atlascommerce.ui.i18n.atlasQuantity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.format.AtlasFormat
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.CartLine
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasSectionHeader
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingDockPadding
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingTopBarPadding
import ma.etherealmber.atlascommerce.ui.theme.Atlas

@Composable
fun SaleScreen(
    user: SessionUser,
    onOpenProducts: () -> Unit,
    onToast: (String) -> Unit,
    viewModel: SaleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val confirmBeforeSale by viewModel.confirmBeforeSale.collectAsStateWithLifecycle()
    val density = Atlas.density
    val canSell = Roles.canCreateDocuments(user.role)
    val total = remember(lines) { lines.sumOf { it.lineTotal } }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showPrinterPicker by remember { mutableStateOf(false) }
    var printerDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var printerPaperWidth by remember { mutableStateOf(58) }
    val context = LocalContext.current
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) printerDevices = runCatching { ThermalPrinter.pairedDevices(context) }.getOrDefault(emptyList())
    }
    fun openPrinterPicker() {
        showPrinterPicker = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            printerDevices = runCatching { ThermalPrinter.pairedDevices(context) }.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message -> onToast(message) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = LocalAtlasFloatingDockPadding.current),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = density.screenPadding,
                end = density.screenPadding,
                top = density.cardMargin + LocalAtlasFloatingTopBarPadding.current,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(density.itemGap),
        ) {
            if (!canSell) {
                item {
                    AtlasErrorCard(
                        title = atlasText("Rôle en lecture seule"),
                        message = atlasText(
                            "Votre rôle permet de consulter le stock, pas de créer une vente. Demandez à un gérant d'encaisser, ou utilisez le web.",
                            "Your role can view stock but cannot create sales. Ask a manager to process the sale, or use the web app.",
                            "يتيح دورك عرض المخزون، وليس إنشاء المبيعات. اطلب من المدير إتمام البيع أو استخدم تطبيق الويب.",
                        ),
                    )
                }
            }

            item {
                AtlasCard {
                    AtlasSectionHeader(
                        title = atlasText("Panier"),
                        subtitle = if (lines.isEmpty()) null else atlasText("${lines.size} article(s)", "${lines.size} items", "${lines.size} عنصر"),
                        actionLabel = if (lines.isEmpty()) null else atlasText("Vider"),
                        onAction = if (lines.isEmpty()) null else viewModel::clearCart,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (lines.isEmpty()) {
                        AtlasEmptyState(
                            title = atlasText("Panier vide"),
                            message = atlasText("Ajoutez des produits depuis le catalogue ou en scannant un code-barres."),
                            icon = Icons.Outlined.ShoppingCart,
                            actionLabel = atlasText("Parcourir les produits"),
                            onAction = onOpenProducts,
                        )
                    } else {
                        lines.forEach { line ->
                            CartLineRow(
                                line = line,
                                currency = state.currency,
                                onIncrement = { viewModel.increment(line.product.id) },
                                onDecrement = { viewModel.decrement(line.product.id) },
                                onRemove = { viewModel.remove(line.product.id) },
                            )
                        }
                    }
                }
            }

            item {
                AtlasCard {
                    AtlasSectionHeader(
                        title = atlasText("Client du ticket"),
                        subtitle = atlasText("Le nom apparaît sur le ticket VR"),
                    )
                    Spacer(Modifier.height(10.dp))
                    AtlasTextField(
                        value = state.clientName,
                        onValueChange = viewModel::onClientNameChange,
                        label = atlasText("Nom du client *"),
                        leadingIcon = Icons.Outlined.Person,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        enabled = !state.submitting,
                    )
                    Spacer(Modifier.height(10.dp))
                    AtlasTextField(
                        value = state.clientPhone,
                        onValueChange = viewModel::onClientPhoneChange,
                        label = atlasText("Téléphone (facultatif)"),
                        leadingIcon = Icons.Outlined.Phone,
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                        enabled = !state.submitting,
                    )
                }
            }

            item {
                AtlasCard {
                    Text(
                        text = atlasText("Note interne (facultatif)"),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    AtlasTextField(
                        value = state.note,
                        onValueChange = viewModel::onNoteChange,
                        label = atlasText("Note"),
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        singleLine = false,
                        enabled = !state.submitting,
                    )
                }
            }

            if (!state.error.isNullOrBlank()) {
                item {
                    AtlasErrorCard(
                        title = atlasText("Vente non enregistrée"),
                        message = atlasNotice(state.error.orEmpty(), LocalAtlasLanguage.current),
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AtlasChip(text = atlasText("Ticket VR"), tone = AtlasTone.INFO)
                    AtlasChip(text = atlasText("Sans TVA"), tone = AtlasTone.NEUTRAL)
                    AtlasChip(text = atlasText("À encaisser"), tone = AtlasTone.WARNING)
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = density.screenPadding, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = atlasText("TOTAL"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = atlasMoney(total, state.currency),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = atlasText("Sans TVA"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                AtlasButton(
                    text = if (state.submitting) "Enregistrement…" else "Créer le ticket VR",
                    onClick = {
                        if (confirmBeforeSale) {
                            showConfirmDialog = true
                        } else {
                            viewModel.submit()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSell && state.canSubmit && lines.isNotEmpty(),
                    loading = state.submitting,
                    icon = Icons.Outlined.CreditCard,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = atlasText("La vente est créée sur le serveur ; l'encaissement se fait depuis la caisse web."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(atlasText("Confirmer la création du ticket ?")) },
            text = {
                Text(
                    atlasText(
                        "Client : ${state.clientName.trim()}\n",
                        "Customer: ${state.clientName.trim()}\n",
                        "العميل: ${state.clientName.trim()}\n",
                    ) +
                        atlasText(
                            "Articles : ${lines.size} · Total : ${atlasMoney(total, state.currency)}",
                            "Items: ${lines.size} · Total: ${atlasMoney(total, state.currency)}",
                            "العناصر: ${lines.size} · الإجمالي: ${atlasMoney(total, state.currency)}",
                        ),
                )
            },
            confirmButton = {
                AtlasButton(
                    text = "Valider",
                    onClick = {
                        showConfirmDialog = false
                        viewModel.submit()
                    },
                )
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(atlasText("Annuler"))
                }
            },
        )
    }

    val created = state.created
    if (created != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCreated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Atlas.palette.positive,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(atlasText("Ticket créé"))
                }
            },
            text = {
                Column {
                    Text(
                        text = atlasText(
                            "Ticket ${created.number ?: "#${created.id}"}",
                            "Receipt ${created.number ?: "#${created.id}"}",
                            "إيصال ${created.number ?: "#${created.id}"}",
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = created.total?.takeIf(Double::isFinite)?.let { total ->
                            atlasText(
                                "Total ${atlasMoney(total, state.currency)} · en attente d'encaissement.",
                                "Total ${atlasMoney(total, state.currency)} · awaiting payment collection.",
                                "الإجمالي ${atlasMoney(total, state.currency)} · بانتظار تحصيل المبلغ.",
                            )
                        } ?: atlasText(
                            "Total serveur indisponible — impression désactivée.",
                            "Server total unavailable — printing disabled.",
                            "إجمالي الخادم غير متاح — الطباعة معطلة.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = ::openPrinterPicker) { Text(atlasText("Imprimer", "Print", "طباعة")) }
                    TextButton(onClick = viewModel::dismissCreated) { Text(atlasText("Nouveau ticket")) }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCreated) { Text(atlasText("Fermer")) }
            },
        )
    }

    if (showPrinterPicker) {
        AlertDialog(
            onDismissRequest = { showPrinterPicker = false },
            title = { Text(atlasText("Imprimante Bluetooth", "Bluetooth printer", "طابعة بلوتوث")) },
            text = {
                Column {
                    Text(atlasText("Largeur du papier", "Paper width", "عرض الورق"), style = MaterialTheme.typography.labelLarge)
                    Row {
                        TextButton(onClick = { printerPaperWidth = 58 }) { Text(if (printerPaperWidth == 58) "✓ 58 mm" else "58 mm") }
                        TextButton(onClick = { printerPaperWidth = 80 }) { Text(if (printerPaperWidth == 80) "✓ 80 mm" else "80 mm") }
                    }
                    if (printerDevices.isEmpty()) {
                        Text(atlasText("Aucune imprimante associée. Associez votre imprimante dans les paramètres Bluetooth Android, puis réessayez.", "No paired printers. Pair your printer in Android Bluetooth settings, then try again.", "لا توجد طابعات مقترنة. اربط الطابعة من إعدادات البلوتوث ثم أعد المحاولة."))
                    } else {
                        printerDevices.forEach { device ->
                            val label = runCatching { device.name?.takeIf(String::isNotBlank) ?: device.address }.getOrDefault("Bluetooth printer")
                            TextButton(
                                onClick = {
                                    val address = runCatching { device.address }.getOrNull() ?: return@TextButton
                                    viewModel.printReceipt(context, address, printerPaperWidth)
                                    showPrinterPicker = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    printerDevices = runCatching { ThermalPrinter.pairedDevices(context) }.getOrDefault(emptyList())
                }) { Text(atlasText("Actualiser", "Refresh", "تحديث")) }
            },
            dismissButton = {
                TextButton(onClick = { showPrinterPicker = false }) { Text(atlasText("Fermer", "Close", "إغلاق")) }
            },
        )
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    currency: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = line.product.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${atlasQuantity(line.quantity)} × " +
                    "${atlasMoney(line.product.price, currency)} = " +
                    atlasMoney(line.lineTotal, currency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Remove, contentDescription = atlasText("Diminuer"), modifier = Modifier.size(16.dp))
                }
                Text(
                    text = atlasQuantity(line.quantity),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(36.dp),
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = atlasText("Augmenter"), modifier = Modifier.size(16.dp))
                }
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = atlasText("Retirer"),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
