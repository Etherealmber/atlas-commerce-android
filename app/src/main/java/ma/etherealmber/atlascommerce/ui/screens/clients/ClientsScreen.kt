package ma.etherealmber.atlascommerce.ui.screens.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.data.model.ClientDto
import ma.etherealmber.atlascommerce.data.model.InvoiceDto
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasShortDate
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

@Composable
fun ClientsScreen(viewModel: ClientsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedClient by remember { mutableStateOf<ClientDto?>(null) }
    val clients = remember(state.clients, query) {
        state.clients.filter { query.isBlank() || listOfNotNull(it.name, it.phone, it.email).any { value -> value.contains(query.trim(), true) } }
            .sortedByDescending { it.balanceDue }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(atlasText("Clients", "Clients", "العملاء"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(atlasText("Crédits et soldes clients", "Client credits and balances", "أرصدة العملاء والائتمان"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = viewModel::load, enabled = !state.loading) {
                        if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser", "Refresh", "تحديث"))
                    }
                }
                AtlasTextField(value = query, onValueChange = { query = it }, label = atlasText("Rechercher un client", "Search clients", "البحث عن عميل"), placeholder = atlasText("Nom, téléphone ou e-mail", "Name, phone or email", "الاسم أو الهاتف أو البريد الإلكتروني"), modifier = Modifier.fillMaxWidth())
            }
        }
        if (!state.error.isNullOrBlank() && state.clients.isEmpty()) item {
            AtlasErrorCard(title = atlasText("Clients indisponibles", "Clients unavailable", "العملاء غير متاحين"), message = state.error.orEmpty(), onRetry = viewModel::load)
        }
        if (!state.loading && clients.isEmpty()) item {
            AtlasEmptyState(title = atlasText("Aucun client", "No clients", "لا يوجد عملاء"), message = atlasText("Les clients et leurs crédits apparaîtront ici.", "Clients and their balances will appear here.", "سيظهر العملاء وأرصدتهم هنا."), icon = Icons.Outlined.Groups)
        }
        items(clients, key = { "client-${it.id}-${it.name}" }) { client ->
            AtlasCard(onClick = { selectedClient = client }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(client.name.ifBlank { atlasText("Client sans nom", "Unnamed client", "عميل بدون اسم") }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        listOfNotNull(client.phone?.takeIf(String::isNotBlank), client.email?.takeIf(String::isNotBlank)).takeIf { it.isNotEmpty() }?.let {
                            Text(it.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(atlasMoney(client.balanceDue, state.currency), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (client.balanceDue > 0.009) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        Text(atlasText("Solde dû", "Balance due", "المبلغ المستحق"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    selectedClient?.let { client ->
        val idMatches = state.invoices.filter { it.clientId == client.id }
        val history = (if (idMatches.isNotEmpty()) idMatches else state.invoices.filter { invoice -> invoice.clientName?.trim()?.equals(client.name.trim(), ignoreCase = true) == true })
            .sortedByDescending { it.issueDate.orEmpty() }
        ClientCreditDetails(client, history, state.currency) { selectedClient = null }
    }
}

@Composable
private fun ClientCreditDetails(client: ClientDto, history: List<InvoiceDto>, currency: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = atlasText("Retour", "Back", "رجوع")) }
                        Column(Modifier.weight(1f)) {
                            Text(client.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(atlasText("Détails du crédit client", "Client credit details", "تفاصيل ائتمان العميل"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    AtlasCard {
                        Text(atlasText("Solde impayé", "Outstanding balance", "الرصيد المستحق"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(atlasMoney(client.balanceDue, currency), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        client.phone?.takeIf(String::isNotBlank)?.let { Text("${atlasText("Téléphone", "Phone", "الهاتف")}: $it") }
                        client.email?.takeIf(String::isNotBlank)?.let { Text("Email: $it") }
                    }
                }
                item { Text(atlasText("Crédits et historique", "Credit and history", "الائتمان والسجل"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                if (history.isEmpty()) item {
                    AtlasEmptyState(title = atlasText("Aucun historique de facture", "No invoice history", "لا يوجد سجل فواتير"), message = atlasText("Aucune facture liée à ce nom de client n’est disponible.", "No invoices linked to this client name are available.", "لا تتوفر فواتير مرتبطة باسم هذا العميل."), icon = Icons.Outlined.History)
                }
                items(history, key = { "invoice-${it.id}" }) { invoice ->
                    AtlasCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(invoice.number?.takeIf(String::isNotBlank) ?: atlasText("Facture #${invoice.id}", "Invoice #${invoice.id}", "فاتورة #${invoice.id}"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(listOfNotNull(invoice.issueDate?.takeIf(String::isNotBlank)?.let { atlasShortDate(it) }, invoice.dueDate?.takeIf(String::isNotBlank)?.let { "${atlasText("Échéance", "Due", "الاستحقاق")}: ${atlasShortDate(it)}" }, invoice.status).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(atlasMoney(invoice.remaining, currency), fontWeight = FontWeight.Bold, color = if (invoice.remaining > 0.009) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                Text(atlasText("Reste dû", "Remaining", "المتبقي"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(atlasText("Total : ${atlasMoney(invoice.total, currency)} · Payé : ${atlasMoney(invoice.amountPaid, currency)}", "Total: ${atlasMoney(invoice.total, currency)} · Paid: ${atlasMoney(invoice.amountPaid, currency)}", "الإجمالي: ${atlasMoney(invoice.total, currency)} · المدفوع: ${atlasMoney(invoice.amountPaid, currency)}"), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
