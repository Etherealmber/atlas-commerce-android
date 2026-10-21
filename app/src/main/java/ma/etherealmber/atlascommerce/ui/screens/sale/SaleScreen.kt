package ma.etherealmber.atlascommerce.ui.screens.sale

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingCart
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasSectionHeader
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
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
    val density = Atlas.density
    val canSell = Roles.canCreateDocuments(user.role)
    val total = remember(lines) { lines.sumOf { it.lineTotal } }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message -> onToast(message) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = density.screenPadding,
                end = density.screenPadding,
                top = density.cardMargin,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(density.itemGap),
        ) {
            if (!canSell) {
                item {
                    AtlasErrorCard(
                        title = "Rôle en lecture seule",
                        message = "Votre rôle permet de consulter le stock, pas de créer une vente. " +
                            "Demandez à un gérant d'encaisser, ou utilisez le web.",
                    )
                }
            }

            item {
                AtlasCard {
                    AtlasSectionHeader(
                        title = "Panier",
                        subtitle = if (lines.isEmpty()) null else "${lines.size} article(s)",
                        actionLabel = if (lines.isEmpty()) null else "Vider",
                        onAction = if (lines.isEmpty()) null else viewModel::clearCart,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (lines.isEmpty()) {
                        AtlasEmptyState(
                            title = "Panier vide",
                            message = "Ajoutez des produits depuis le catalogue ou en scannant un code-barres.",
                            icon = Icons.Outlined.ShoppingCart,
                            actionLabel = "Parcourir les produits",
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
                        title = "Client du ticket",
                        subtitle = "Le nom apparaît sur le ticket VR",
                    )
                    Spacer(Modifier.height(10.dp))
                    AtlasTextField(
                        value = state.clientName,
                        onValueChange = viewModel::onClientNameChange,
                        label = "Nom du client *",
                        leadingIcon = Icons.Outlined.Person,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        enabled = !state.submitting,
                    )
                    Spacer(Modifier.height(10.dp))
                    AtlasTextField(
                        value = state.clientPhone,
                        onValueChange = viewModel::onClientPhoneChange,
                        label = "Téléphone (facultatif)",
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
                        text = "Note interne (facultatif)",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    AtlasTextField(
                        value = state.note,
                        onValueChange = viewModel::onNoteChange,
                        label = "Note",
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
                        title = "Vente non enregistrée",
                        message = state.error.orEmpty(),
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AtlasChip(text = "Ticket VR", tone = AtlasTone.INFO)
                    AtlasChip(text = "Sans TVA", tone = AtlasTone.NEUTRAL)
                    AtlasChip(text = "À encaisser", tone = AtlasTone.WARNING)
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
                            text = "TOTAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = AtlasFormat.money(total, state.currency),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = "Sans TVA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                AtlasButton(
                    text = if (state.submitting) "Enregistrement…" else "Créer le ticket VR",
                    onClick = viewModel::submit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSell && state.canSubmit && lines.isNotEmpty(),
                    loading = state.submitting,
                    icon = Icons.Outlined.PointOfSale,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "La vente est créée sur le serveur ; l'encaissement se fait depuis la caisse web.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
                    Text("Ticket créé")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Ticket ${created.number ?: "#${created.id}"}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Total ${AtlasFormat.money(created.total, state.currency)} · en attente d'encaissement.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissCreated) { Text("Nouveau ticket") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCreated) { Text("Fermer") }
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
                text = "${AtlasFormat.money(line.product.price, currency)} · ${AtlasFormat.quantity(line.quantity)} × = " +
                    AtlasFormat.money(line.lineTotal, currency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Diminuer", modifier = Modifier.size(16.dp))
                }
                Text(
                    text = AtlasFormat.quantity(line.quantity),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(36.dp),
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = "Augmenter", modifier = Modifier.size(16.dp))
                }
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Retirer",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
