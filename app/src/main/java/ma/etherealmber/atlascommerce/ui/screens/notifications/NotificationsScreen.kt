package ma.etherealmber.atlascommerce.ui.screens.notifications

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.data.model.AlertItemDto
import ma.etherealmber.atlascommerce.data.model.MobileActionDto
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasLoading
import ma.etherealmber.atlascommerce.ui.components.AtlasSectionHeader
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.AtlasTopBar
import ma.etherealmber.atlascommerce.ui.theme.Atlas

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateHref: (String?) -> Unit,
    onToast: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val density = Atlas.density

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg -> onToast(msg) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AtlasTopBar(
                title = "Notifications & activité",
                subtitle = "${state.alerts.size} alerte(s) · ${state.pendingCount} action(s) en attente",
                navigation = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading && state.alerts.isEmpty() && state.actions.isEmpty() ->
                AtlasLoading(message = "Chargement des notifications…", modifier = Modifier.padding(padding))

            !state.error.isNullOrBlank() && state.alerts.isEmpty() && state.actions.isEmpty() ->
                Column(Modifier.padding(padding).padding(density.screenPadding)) {
                    AtlasErrorCard(message = state.error.orEmpty(), onRetry = viewModel::refresh)
                }

            state.alerts.isEmpty() && state.actions.isEmpty() ->
                AtlasEmptyState(
                    title = "Aucune alerte active",
                    message = "Tout votre stock, vos ventes et vos actions PC ↔ mobile sont à jour.",
                    icon = Icons.Outlined.NotificationsNone,
                    actionLabel = "Actualiser",
                    onAction = viewModel::refresh,
                    modifier = Modifier.padding(padding),
                )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = density.screenPadding,
                    end = density.screenPadding,
                    top = density.cardMargin,
                    bottom = 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(density.itemGap),
            ) {
                if (state.alerts.isNotEmpty()) {
                    item {
                        AtlasSectionHeader(title = "Alertes prioritaires (${state.alerts.size})")
                    }
                    items(state.alerts) { alert ->
                        AlertRow(
                            alert = alert,
                            onClick = { onNavigateHref(alert.href) },
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    AtlasSectionHeader(title = "Flux terrain PC ↔ Mobile")
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !state.pendingOnly,
                            onClick = { viewModel.togglePendingOnly(false) },
                            label = { Text("Toutes (${state.actions.size})") },
                        )
                        FilterChip(
                            selected = state.pendingOnly,
                            onClick = { viewModel.togglePendingOnly(true) },
                            label = { Text("En attente (${state.pendingCount})") },
                        )
                    }
                }

                items(state.visibleActions, key = { it.id }) { action ->
                    MobileActionRow(
                        action = action,
                        busy = state.busyId == action.id,
                        onComplete = { viewModel.completeAction(action) },
                        onReject = { viewModel.rejectAction(action) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertRow(
    alert: AlertItemDto,
    onClick: () -> Unit,
) {
    val isHigh = alert.severity.equals("high", ignoreCase = true) ||
        alert.severity.equals("danger", ignoreCase = true) ||
        alert.kind.contains("stock", ignoreCase = true)
    AtlasCard(onClick = onClick, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isHigh) Icons.Outlined.Warning else Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = if (isHigh) Atlas.palette.warning else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = alert.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            if ((alert.count ?: 0) > 0) {
                Spacer(Modifier.width(8.dp))
                AtlasChip(
                    text = alert.count.toString(),
                    tone = if (isHigh) AtlasTone.WARNING else AtlasTone.INFO,
                )
            }
        }
    }
}

@Composable
private fun MobileActionRow(
    action: MobileActionDto,
    busy: Boolean,
    onComplete: () -> Unit,
    onReject: () -> Unit,
) {
    val isPending = action.status.equals("pending", ignoreCase = true)
    AtlasCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!action.detail.isNullOrBlank()) {
                    Text(
                        text = action.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val meta = listOfNotNull(
                    action.barcode?.takeIf { it.isNotBlank() }?.let { "Code $it" },
                    action.createdBy?.takeIf { it.isNotBlank() }?.let { "par $it" },
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AtlasChip(
                text = when (action.status.lowercase()) {
                    "pending" -> "En attente"
                    "completed", "done" -> "Validée"
                    "rejected" -> "Refusée"
                    else -> action.status
                },
                tone = when (action.status.lowercase()) {
                    "pending" -> AtlasTone.WARNING
                    "completed", "done" -> AtlasTone.POSITIVE
                    else -> AtlasTone.NEUTRAL
                },
            )
        }
        if (isPending) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onComplete, enabled = !busy) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Appliquer")
                }
                TextButton(onClick = onReject, enabled = !busy) {
                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Refuser")
                }
            }
        }
    }
}
