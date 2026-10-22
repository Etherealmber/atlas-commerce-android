package ma.etherealmber.atlascommerce.ui.screens.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasSectionHeader
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.theme.Atlas

private val QuickPrompts = listOf(
    "Quels articles sont en rupture ou à réapprovisionner ?",
    "Résumé des ventes et impayés du jour",
    "Quelles demandes scan sont en attente ?",
)

@Composable
fun AgentScreen(
    onToast: (String) -> Unit,
    viewModel: AgentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val density = Atlas.density

    LaunchedEffect(Unit) {
        viewModel.notices.collect { msg -> onToast(msg) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = density.screenPadding,
                end = density.screenPadding,
                top = density.cardMargin,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(density.itemGap),
        ) {
            item {
                AtlasCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Atlas Agent IA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = state.status?.model?.let { "Modèle actif : $it" }
                                    ?: "Copilote stock, facturation & opérations terrain",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AtlasChip(
                            text = if (state.status?.configured == false) "Veille" else "En ligne",
                            tone = if (state.status?.configured == false) AtlasTone.WARNING else AtlasTone.POSITIVE,
                        )
                        IconButton(onClick = viewModel::refreshContext) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        QuickPrompts.forEach { prompt ->
                            AssistChip(
                                onClick = { viewModel.sendPrompt(prompt) },
                                label = { Text(prompt, style = MaterialTheme.typography.bodySmall) },
                            )
                        }
                    }
                }
            }

            if (state.messages.isEmpty() && state.nextActions.isNotEmpty()) {
                item {
                    AtlasSectionHeader(title = "Actions suggérées par l'IA")
                }
                items(state.nextActions) { item ->
                    AtlasCard(
                        onClick = { viewModel.sendPrompt(" Aide-moi sur : ${item.title} (${item.detail})") },
                        padding = 12.dp,
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (item.detail.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            items(state.messages, key = { it.id }) { msg ->
                ChatBubbleRow(
                    message = msg,
                    approvingId = state.approvingId,
                    onApprove = { proposalId -> viewModel.approveProposal(msg.id, proposalId) },
                )
            }

            if (state.sending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Atlas Agent analyse vos données…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (!state.error.isNullOrBlank()) {
                item {
                    AtlasErrorCard(
                        title = "Réponse IA indisponible",
                        message = state.error.orEmpty(),
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = density.screenPadding, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AtlasTextField(
                    value = state.input,
                    onValueChange = viewModel::onInputChange,
                    label = "Poser une question à Atlas Agent…",
                    placeholder = "Ex. Quels produits commander aujourd'hui ?",
                    imeAction = ImeAction.Send,
                    enabled = !state.sending,
                    onDone = { viewModel.sendPrompt() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.sendPrompt() },
                    enabled = state.input.isNotBlank() && !state.sending,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Envoyer",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleRow(
    message: AgentViewModel.ChatMessage,
    approvingId: String?,
    onApprove: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (message.fromUser) 0.85f else 0.96f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (message.fromUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                )
                .padding(12.dp),
        ) {
            Column {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.fromUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (message.tools.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        message.tools.take(3).forEach { tool ->
                            AtlasChip(
                                text = tool.name,
                                tone = if (tool.ok) AtlasTone.INFO else AtlasTone.WARNING,
                            )
                        }
                    }
                }
                message.proposals.forEach { prop ->
                    val pid = prop.proposalId.orEmpty()
                    if (pid.isNotBlank()) {
                        val approved = pid in message.approvedIds
                        Spacer(Modifier.height(8.dp))
                        AtlasCard(padding = 10.dp) {
                            Text(
                                text = prop.title ?: "Action proposée",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (!prop.summary.isNullOrBlank()) {
                                Text(
                                    text = prop.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            if (approved) {
                                AtlasChip(
                                    text = "Approuvée",
                                    tone = AtlasTone.POSITIVE,
                                    icon = Icons.Outlined.CheckCircle,
                                )
                            } else {
                                TextButton(
                                    onClick = { onApprove(pid) },
                                    enabled = approvingId == null,
                                ) {
                                    Text("Approuver et exécuter")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
