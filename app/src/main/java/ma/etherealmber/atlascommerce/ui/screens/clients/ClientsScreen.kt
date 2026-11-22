package ma.etherealmber.atlascommerce.ui.screens.clients

import androidx.activity.compose.BackHandler
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.data.model.ClientDto
import ma.etherealmber.atlascommerce.data.model.InvoiceDto
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasFloatingTopBar
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.rememberAtlasFloatingTopBarScrollConnection
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasShortDate
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

@Composable
fun ClientsScreen(
    onDetailsVisibilityChanged: (Boolean) -> Unit = {},
    viewModel: ClientsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableStateOf(1) }
    var selectedClient by remember { mutableStateOf<ClientDto?>(null) }
    LaunchedEffect(selectedClient != null) { onDetailsVisibilityChanged(selectedClient != null) }
    BackHandler(enabled = selectedClient != null) {
        selectedClient = null
        onDetailsVisibilityChanged(false)
    }
    val clients = remember(state.clients, query) {
        state.clients.filter { query.isBlank() || listOfNotNull(it.name, it.phone, it.email).any { value -> value.contains(query.trim(), true) } }
            .sortedByDescending { it.balanceDue }
    }
    val pageCount = ((clients.size + CLIENT_PAGE_SIZE - 1) / CLIENT_PAGE_SIZE).coerceAtLeast(1)
    LaunchedEffect(pageCount) { if (page > pageCount) page = pageCount }

    androidx.compose.animation.AnimatedContent(
        targetState = selectedClient,
        modifier = Modifier.fillMaxSize(),
        contentKey = { it?.id ?: 0L },
        transitionSpec = {
            if (targetState != null) {
                (androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(280)) { it } +
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180))) togetherWith
                    (androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(220)) { -it / 6 } +
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)))
            } else {
                (androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(240)) { -it / 6 } +
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(170))) togetherWith
                    (androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(260)) { it } +
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)))
            }
        },
        label = "client-directory-history-transition",
    ) { client ->
        if (client == null) {
            ClientsListContent(
                clients = clients,
                currency = state.currency,
                query = query,
                loading = state.loading,
                error = state.error,
                page = page,
                pageCount = pageCount,
                onQueryChange = { query = it; page = 1 },
                onRefresh = viewModel::load,
                onPageChange = { page = it },
                onClientClick = { selectedClient = it; onDetailsVisibilityChanged(true) },
            )
        } else {
            val byId = state.invoices.filter { it.clientId == client.id }
            val history = (if (byId.isNotEmpty()) byId else state.invoices.filter { invoice -> invoice.clientName?.trim()?.equals(client.name.trim(), ignoreCase = true) == true })
                .sortedByDescending { it.issueDate.orEmpty() }
            ClientCreditDetails(
                client = client,
                history = history,
                currency = state.currency,
                refreshing = state.loading,
                onRefresh = viewModel::load,
                onClose = { selectedClient = null },
            )
        }
    }
}

private const val CLIENT_PAGE_SIZE = 10

@Composable
private fun ClientsListContent(
    clients: List<ClientDto>,
    currency: String,
    query: String,
    loading: Boolean,
    error: String?,
    page: Int,
    pageCount: Int,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onPageChange: (Int) -> Unit,
    onClientClick: (ClientDto) -> Unit,
) {
    val topInset = ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingTopBarPadding.current
    val currentPageClients = clients.drop((page - 1) * CLIENT_PAGE_SIZE).take(CLIENT_PAGE_SIZE)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp + topInset, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(atlasText("Clients", "Clients", "العملاء"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(atlasText("Crédits et soldes clients", "Client credits and balances", "أرصدة العملاء والائتمان"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onRefresh, enabled = !loading) {
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser", "Refresh", "تحديث"))
                    }
                }
                AtlasTextField(value = query, onValueChange = onQueryChange, label = atlasText("Rechercher un client", "Search clients", "البحث عن عميل"), placeholder = atlasText("Nom, téléphone ou e-mail", "Name, phone or email", "الاسم أو الهاتف أو البريد الإلكتروني"), modifier = Modifier.fillMaxWidth())
            }
        }
        if (!error.isNullOrBlank() && clients.isEmpty()) item {
            AtlasErrorCard(title = atlasText("Clients indisponibles", "Clients unavailable", "العملاء غير متاحين"), message = error, onRetry = onRefresh)
        }
        if (!loading && clients.isEmpty()) item {
            AtlasEmptyState(title = atlasText("Aucun client", "No clients", "لا يوجد عملاء"), message = atlasText("Les clients et leurs crédits apparaîtront ici.", "Clients and their balances will appear here.", "سيظهر العملاء وأرصدتهم هنا."), icon = Icons.Outlined.Groups)
        }
        items(currentPageClients, key = { "client-${it.id}-${it.name}" }) { rowClient ->
            AtlasCard(onClick = { onClientClick(rowClient) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(rowClient.name.ifBlank { atlasText("Client sans nom", "Unnamed client", "عميل بدون اسم") }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        listOfNotNull(rowClient.phone?.takeIf(String::isNotBlank), rowClient.email?.takeIf(String::isNotBlank)).takeIf { it.isNotEmpty() }?.let {
                            Text(it.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(atlasMoney(rowClient.balanceDue, currency), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (rowClient.balanceDue > 0.009) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        Text(atlasText("Solde dû", "Balance due", "المبلغ المستحق"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (clients.size > CLIENT_PAGE_SIZE) item(key = "clients-pagination") {
            ma.etherealmber.atlascommerce.ui.components.AtlasPaginationControls(
                totalItems = clients.size,
                currentPage = page.coerceAtMost(pageCount),
                onPageSelected = onPageChange,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
                pageSize = CLIENT_PAGE_SIZE,
            )
        }
    }
}

@Composable
private fun ClientCreditDetails(
    client: ClientDto,
    history: List<InvoiceDto>,
    currency: String,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
) {
    var headerExpanded by remember(client.id) { mutableStateOf(true) }
    val scrollConnection = rememberAtlasFloatingTopBarScrollConnection(headerExpanded, onExpandedChange = { headerExpanded = it })
    Box(Modifier.fillMaxSize().nestedScroll(scrollConnection)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(104.dp)) }
            item {
                // This profile banner scrolls with the page; only the floating toolbar stays pinned.
                AtlasCard {
                    Text(atlasText("CLIENT PROFILE", "CLIENT PROFILE", "ملف العميل"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(client.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    client.phone?.takeIf(String::isNotBlank)?.let { Text("${atlasText("Téléphone", "Phone", "الهاتف")}: $it", style = MaterialTheme.typography.bodyMedium) }
                    client.email?.takeIf(String::isNotBlank)?.let { Text("Email: $it", style = MaterialTheme.typography.bodyMedium) }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(atlasText("Solde impayé", "Outstanding balance", "الرصيد المستحق"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(atlasMoney(client.balanceDue, currency), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (client.balanceDue > 0.009) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(atlasText("Factures", "Invoices", "الفواتير"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(history.size.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item { Text(atlasText("Crédits et historique", "Credit and invoice history", "الائتمان وسجل الفواتير"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
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
        AtlasFloatingTopBar(
            title = client.name,
            subtitle = atlasText("Profil et factures", "Profile and invoices", "الملف والفواتير"),
            onBack = onClose,
            expanded = headerExpanded,
            onCollapsedClick = { headerExpanded = true },
            modifier = Modifier.align(Alignment.TopCenter).zIndex(1f),
            actions = {
                IconButton(onClick = onRefresh, enabled = !refreshing) {
                    if (refreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser l’historique", "Refresh history", "تحديث السجل"))
                }
            },
        )
    }
}
