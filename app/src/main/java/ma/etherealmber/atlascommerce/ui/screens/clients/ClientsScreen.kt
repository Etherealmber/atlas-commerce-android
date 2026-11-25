package ma.etherealmber.atlascommerce.ui.screens.clients

import androidx.activity.compose.BackHandler
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.session.SessionUser
import ma.etherealmber.atlascommerce.data.model.ClientDto
import ma.etherealmber.atlascommerce.data.model.InvoiceDto
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasFinanceBarChart
import ma.etherealmber.atlascommerce.ui.components.AtlasFinanceBarEntry
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasFloatingTopBar
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasFloatingDockPadding
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.rememberAtlasFloatingTopBarScrollConnection
import ma.etherealmber.atlascommerce.ui.icons.AtlasIcons as Icons
import ma.etherealmber.atlascommerce.ui.i18n.atlasMoney
import ma.etherealmber.atlascommerce.ui.i18n.atlasShortDate
import ma.etherealmber.atlascommerce.ui.i18n.atlasText

@Composable
fun ClientsScreen(
    user: SessionUser,
    onDetailsVisibilityChanged: (Boolean) -> Unit = {},
    viewModel: ClientsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val canAccessClients = Roles.isAdmin(user.role) || Roles.normalize(user.role) == Roles.SECRETARY
    if (!canAccessClients) {
        AtlasErrorCard(
            title = atlasText("Accès réservé", "Restricted access", "الوصول مقيد"),
            message = atlasText("La page clients est réservée aux administrateurs et secrétaires.", "The clients page is restricted to administrators and secretaries.", "صفحة العملاء مخصصة للمسؤولين والسكرتارية."),
        )
        return
    }
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
                invoices = state.invoices,
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
                onClose = {
                    selectedClient = null
                    onDetailsVisibilityChanged(false)
                },
            )
        }
    }
}

private const val CLIENT_PAGE_SIZE = 10

@Composable
private fun ClientsListContent(
    clients: List<ClientDto>,
    currency: String,
    invoices: List<InvoiceDto>,
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp + topInset, bottom = 28.dp + LocalAtlasFloatingDockPadding.current),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            AtlasCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(atlasText("Clients", "Clients", "العملاء"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            atlasText(
                                "${clients.size} client(s) · crédits et soldes",
                                "${clients.size} clients · credits and balances",
                                "${clients.size} عميل · الأرصدة والائتمان",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
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
            val clientInvoices = remember(invoices, rowClient.id, rowClient.name) {
                val matchingId = invoices.filter { it.clientId == rowClient.id }
                (if (matchingId.isNotEmpty()) matchingId else invoices.filter {
                    it.clientName?.trim()?.equals(rowClient.name.trim(), ignoreCase = true) == true
                }).sortedByDescending { it.issueDate.orEmpty() }
            }
            ClientDirectoryCard(
                client = rowClient,
                invoices = clientInvoices,
                currency = currency,
                onDetails = { onClientClick(rowClient) },
            )
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
private fun ClientDirectoryCard(
    client: ClientDto,
    invoices: List<InvoiceDto>,
    currency: String,
    onDetails: () -> Unit,
) {
    val displayName = client.name.ifBlank { atlasText("Client sans nom", "Unnamed client", "عميل بدون اسم") }
    val initials = displayName.trim().split(Regex("\\s+")).filter(String::isNotBlank).take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    var showSituation by rememberSaveable(client.id) { mutableStateOf(false) }

    AtlasCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(initials.ifBlank { "?" }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(client.phone?.takeIf(String::isNotBlank), client.email?.takeIf(String::isNotBlank))
                        .joinToString(" · ").ifBlank { atlasText("Coordonnées non renseignées", "No contact details", "بيانات الاتصال غير متوفرة") },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AtlasChip(text = atlasText("Client", "Client", "عميل"), tone = AtlasTone.NEUTRAL)
        }
        AtlasDivider(Modifier.padding(vertical = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ClientMetric(
                label = atlasText("Solde dû", "Balance due", "الرصيد المستحق"),
                value = atlasMoney(client.balanceDue, currency),
                modifier = Modifier.weight(1f),
                valueColor = if (client.balanceDue > 0.009) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            ClientMetric(
                label = atlasText("Factures", "Invoices", "الفواتير"),
                value = invoices.size.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        AtlasDivider(Modifier.padding(vertical = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showSituation = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(atlasText("Situation", "Statement", "الوضعية"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = onDetails, modifier = Modifier.weight(1.5f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(atlasText("Détails & historique", "Details & history", "التفاصيل والسجل"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    if (showSituation) {
        ClientSituationDialog(
            client = client,
            invoices = invoices,
            currency = currency,
            onDismiss = { showSituation = false },
        )
    }
}

@Composable
private fun ClientMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ClientSituationDialog(
    client: ClientDto,
    invoices: List<InvoiceDto>,
    currency: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                atlasText("Situation du client", "Client statement", "وضعية العميل"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(client.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                ClientMetric(
                    label = atlasText("Solde dû", "Balance due", "الرصيد المستحق"),
                    value = atlasMoney(client.balanceDue, currency),
                    valueColor = if (client.balanceDue > 0.009) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Text(atlasText("${invoices.size} facture(s) · ${invoices.count { it.remaining > 0.009 }} avec un solde", "${invoices.size} invoice(s) · ${invoices.count { it.remaining > 0.009 }} with a balance", "${invoices.size} فاتورة · ${invoices.count { it.remaining > 0.009 }} برصيد"), style = MaterialTheme.typography.labelLarge)
                ClientMetric(
                    label = atlasText("Total encaissé dans l’historique", "Collected in history", "المحصل في السجل"),
                    value = atlasMoney(invoices.sumOf { it.amountPaid.coerceAtLeast(0.0) }, currency),
                )
                AtlasDivider()
                if (invoices.isEmpty()) {
                    Text(atlasText("Aucune facture liée à ce client.", "No invoices linked to this client.", "لا توجد فواتير مرتبطة بهذا العميل."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    invoices.take(12).forEach { invoice ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                invoice.number?.takeIf(String::isNotBlank) ?: atlasText("Facture #${invoice.id}", "Invoice #${invoice.id}", "فاتورة #${invoice.id}"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                listOfNotNull(invoice.issueDate?.takeIf(String::isNotBlank)?.let { atlasShortDate(it) }, invoice.status)
                                    .joinToString(" · ").ifBlank { atlasText("Date inconnue", "Date unavailable", "التاريخ غير متوفر") },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                atlasText("Total ${atlasMoney(invoice.total, currency)} · Payé ${atlasMoney(invoice.amountPaid, currency)}", "Total ${atlasMoney(invoice.total, currency)} · Paid ${atlasMoney(invoice.amountPaid, currency)}", "الإجمالي ${atlasMoney(invoice.total, currency)} · المدفوع ${atlasMoney(invoice.amountPaid, currency)}"),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                atlasText("Reste dû : ${atlasMoney(invoice.remaining, currency)}", "Remaining: ${atlasMoney(invoice.remaining, currency)}", "المتبقي: ${atlasMoney(invoice.remaining, currency)}"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (invoice.remaining > 0.009) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                        AtlasDivider()
                    }
                    if (invoices.size > 12) {
                        Text(atlasText("Affichage des 12 factures les plus récentes.", "Showing the 12 most recent invoices.", "عرض أحدث 12 فاتورة."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(atlasText("Fermer", "Close", "إغلاق")) } },
    )
}

@Composable
private fun ClientProfileInfo(label: String, value: String) {
    Text(
        "$label: ${value.takeIf(String::isNotBlank) ?: atlasText("Non renseigné", "Not provided", "غير متوفر")}",
        style = MaterialTheme.typography.bodySmall,
        color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
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
                    Text(
                        client.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    client.phone?.takeIf(String::isNotBlank)?.let {
                        Text("${atlasText("Téléphone", "Phone", "الهاتف")}: $it", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    client.email?.takeIf(String::isNotBlank)?.let {
                        Text("Email: $it", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    ClientProfileInfo(atlasText("ICE", "Tax / ICE ID", "المعرف الضريبي / ICE"), client.ice.orEmpty())
                    ClientProfileInfo(atlasText("Adresse", "Address", "العنوان"), client.address.orEmpty())
                    client.creditLimit?.let { limit ->
                        ClientProfileInfo(atlasText("Limite de crédit", "Credit limit", "حد الائتمان"), atlasMoney(limit, currency))
                    }
                    Spacer(Modifier.height(14.dp))
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        if (maxWidth < 360.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column {
                                    Text(atlasText("Solde impayé", "Outstanding balance", "الرصيد المستحق"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        atlasMoney(client.balanceDue, currency),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (client.balanceDue > 0.009) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(atlasText("Factures", "Invoices", "الفواتير"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.weight(1f))
                                    Text(history.size.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Column(Modifier.weight(1f)) {
                                    Text(atlasText("Solde impayé", "Outstanding balance", "الرصيد المستحق"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        atlasMoney(client.balanceDue, currency),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (client.balanceDue > 0.009) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(atlasText("Factures", "Invoices", "الفواتير"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(history.size.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
            item { Text(atlasText("Crédits et historique", "Credit and invoice history", "الائتمان وسجل الفواتير"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (history.isNotEmpty()) item {
                val invoiced = history.sumOf { it.total.coerceAtLeast(0.0) }
                val received = history.sumOf { it.amountPaid.coerceAtLeast(0.0) }
                val remaining = history.sumOf { it.remaining.coerceAtLeast(0.0) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AtlasFinanceBarChart(
                        title = atlasText("Facturé, encaissé et restant", "Invoiced, collected and remaining", "المفوتر والمحصل والمتبقي"),
                        entries = listOf(
                            AtlasFinanceBarEntry(atlasText("Total facturé", "Invoiced", "المفوتر"), invoiced, MaterialTheme.colorScheme.primary),
                            AtlasFinanceBarEntry(atlasText("Encaissé", "Collected", "المحصل"), received, MaterialTheme.colorScheme.tertiary),
                            AtlasFinanceBarEntry(atlasText("À recevoir", "Receivable", "المستحق"), remaining, MaterialTheme.colorScheme.error),
                        ),
                        currency = currency,
                    )
                    Text(
                        atlasText("Synthèse des ${history.size} factures chargées · créance actuelle ${atlasMoney(client.balanceDue, currency)}", "Summary of ${history.size} loaded invoices · current receivable ${atlasMoney(client.balanceDue, currency)}", "ملخص ${history.size} فاتورة محملة · المستحق الحالي ${atlasMoney(client.balanceDue, currency)}"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (history.isEmpty()) item {
                AtlasEmptyState(title = atlasText("Aucun historique de facture", "No invoice history", "لا يوجد سجل فواتير"), message = atlasText("Aucune facture liée à ce nom de client n’est disponible.", "No invoices linked to this client name are available.", "لا تتوفر فواتير مرتبطة باسم هذا العميل."), icon = Icons.Outlined.History)
            }
            items(history, key = { "invoice-${it.id}" }) { invoice ->
                AtlasCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                invoice.number?.takeIf(String::isNotBlank) ?: atlasText("Facture #${invoice.id}", "Invoice #${invoice.id}", "فاتورة #${invoice.id}"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                listOfNotNull(invoice.issueDate?.takeIf(String::isNotBlank)?.let { atlasShortDate(it) }, invoice.dueDate?.takeIf(String::isNotBlank)?.let { "${atlasText("Échéance", "Due", "الاستحقاق")}: ${atlasShortDate(it)}" }, invoice.status).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Column(
                            modifier = Modifier.widthIn(min = 72.dp, max = 128.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                atlasMoney(invoice.remaining, currency),
                                fontWeight = FontWeight.Bold,
                                color = if (invoice.remaining > 0.009) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(atlasText("Reste dû", "Remaining", "المتبقي"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        atlasText("Total : ${atlasMoney(invoice.total, currency)} · Payé : ${atlasMoney(invoice.amountPaid, currency)}", "Total: ${atlasMoney(invoice.total, currency)} · Paid: ${atlasMoney(invoice.amountPaid, currency)}", "الإجمالي: ${atlasMoney(invoice.total, currency)} · المدفوع: ${atlasMoney(invoice.amountPaid, currency)}"),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
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
