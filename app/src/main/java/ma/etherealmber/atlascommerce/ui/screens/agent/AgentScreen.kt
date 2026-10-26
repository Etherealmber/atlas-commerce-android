package ma.etherealmber.atlascommerce.ui.screens.agent

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.data.model.AgentPolicyDto
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasMarkdown
import ma.etherealmber.atlascommerce.ui.components.AtlasSectionHeader
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.AtlasTypingDots
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val context = LocalContext.current
    val density = Atlas.density
    val scope = rememberCoroutineScope()
    var showHistoryDialog by remember { mutableStateOf(false) }
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            readAgentAttachment(context, uri).fold(
                onSuccess = viewModel::addAttachment,
                onFailure = { error -> onToast(error.message ?: "Impossible de lire le fichier.") },
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.notices.collect { msg -> onToast(msg) }
    }

    val quotaLocked = state.policy?.isExhausted == true

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Discussions synchronisées (Web & Mobile)") },
            text = {
                if (state.threads.isEmpty()) {
                    Text(
                        text = "Aucune discussion enregistrée sur ce compte pour le moment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.threads.take(12).forEach { thread ->
                            val isCurrent = thread.id == state.activeThreadId
                            AtlasCard(
                                onClick = {
                                    viewModel.selectThread(thread)
                                    showHistoryDialog = false
                                },
                                padding = 10.dp,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = thread.title.ifBlank { "Discussion" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = if (isCurrent) "Active actuellement" else "Synchronisée Web/R2",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteThread(thread.id) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Supprimer",
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.startNewConversation()
                        showHistoryDialog = false
                        onToast("Nouvelle discussion ouverte.")
                    },
                ) {
                    Text("Nouvelle discussion")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Fermer")
                }
            },
        )
    }

    if (state.sensitiveConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::cancelSensitivePrompt,
            title = { Text("Vérifiez avant d'envoyer") },
            text = {
                Text(
                    "Atlas Agent peut se tromper. Les questions sensibles (gouvernement, santé, identité, sécurité ou données financières privées) et les fichiers joints seront transmis au serveur Atlas et au fournisseur IA configuré. N'envoyez pas de mots de passe, secrets ou données personnelles inutiles. Vérifiez les réponses auprès d'une source officielle.",
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmSensitivePrompt) { Text("Envoyer quand même") } },
            dismissButton = { TextButton(onClick = viewModel::cancelSensitivePrompt) { Text("Annuler") } },
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        // The dock takes a fixed slice of phone height. Compact the chat composer when
        // the remaining viewport is short (especially while the keyboard is open).
        val compactComposer = maxHeight < 600.dp
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = density.screenPadding,
                    end = density.screenPadding,
                    top = density.cardMargin,
                    bottom = if (compactComposer) 8.dp else 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(density.itemGap),
            ) {
                item(key = "agent_header") {
                    AtlasEntrance {
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
                                        text = state.activeThreadTitle.takeIf { it.isNotBlank() }
                                            ?: state.status?.model?.let { "Modèle actif : $it" }
                                            ?: "Données réelles synchronisées en direct",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                AtlasChip(
                                    text = when {
                                        quotaLocked -> "Quota atteint"
                                        state.status?.configured == false -> "Veille"
                                        else -> "En ligne"
                                    },
                                    tone = when {
                                        quotaLocked -> AtlasTone.DANGER
                                        state.status?.configured == false -> AtlasTone.WARNING
                                        else -> AtlasTone.POSITIVE
                                    },
                                )
                                IconButton(onClick = { showHistoryDialog = true }) {
                                    Icon(
                                        Icons.Outlined.History,
                                        contentDescription = "Historique Web & Mobile",
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.startNewConversation()
                                        onToast("Nouvelle discussion prête.")
                                    },
                                ) {
                                    Icon(
                                        Icons.Outlined.AddComment,
                                        contentDescription = "Nouvelle discussion",
                                    )
                                }
                                IconButton(onClick = { viewModel.refreshContext() }) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser")
                                }
                            }

                            // Real-time quota progress bar right inside Atlas Agent!
                            state.policy?.let { policy ->
                                Spacer(Modifier.height(10.dp))
                                AtlasDivider()
                                Spacer(Modifier.height(10.dp))
                                AgentQuotaBarsContent(policy = policy, compact = true)
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            ) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Outlined.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "L'IA peut se tromper. Vérifiez les faits importants auprès d'une source officielle et évitez les données sensibles.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                            }

                            if (state.messages.isEmpty() && !quotaLocked) {
                                Spacer(Modifier.height(10.dp))
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
                    }
                }

                if (state.messages.isEmpty() && state.nextActions.isNotEmpty() && !quotaLocked) {
                    item(key = "agent_suggestions_hdr") {
                        AtlasSectionHeader(title = "Actions suggérées par l'IA")
                    }
                    items(
                        items = state.nextActions,
                        key = { "suggestion_${it.title}_${it.priority}" },
                    ) { item ->
                        AtlasCard(
                            onClick = { viewModel.sendPrompt("Aide-moi sur : ${item.title} (${item.detail})") },
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
                    AtlasEntrance(offsetY = 10.dp) {
                        ChatBubbleRow(
                            message = msg,
                            approvingId = state.approvingId,
                            onCopy = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("Atlas Agent", msg.text))
                                onToast("Réponse copiée.")
                            },
                            onApprove = { proposalId -> viewModel.approveProposal(msg.id, proposalId) },
                        )
                    }
                }

                if (state.streamingReply.isNotBlank()) {
                    item(key = "agent_streaming_reply") {
                        AtlasEntrance(offsetY = 8.dp) {
                            ChatBubbleRow(
                                message = AgentViewModel.ChatMessage(fromUser = false, text = state.streamingReply),
                                approvingId = state.approvingId,
                                onCopy = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Atlas Agent", state.streamingReply))
                                    onToast("Réponse copiée.")
                                },
                                onApprove = {},
                            )
                        }
                    }
                }

                if (state.sending) {
                    item(key = "agent_typing") {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AtlasTypingDots()
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = when {
                                    state.streamingReply.isNotBlank() -> "Atlas Agent rédige sa réponse…"
                                    state.attachments.isNotEmpty() -> "Atlas Agent extrait les documents et analyse…"
                                    else -> "Atlas Agent vérifie vos données en direct…"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(0.42f).padding(start = 4.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (!state.error.isNullOrBlank()) {
                    item(key = "agent_err") {
                        AtlasErrorCard(
                            title = "Réponse IA indisponible",
                            message = state.error.orEmpty(),
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = density.screenPadding,
                        vertical = if (compactComposer) 4.dp else 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (state.attachments.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            state.attachments.forEachIndexed { index, attachment ->
                                AssistChip(
                                    onClick = { viewModel.removeAttachment(index) },
                                    label = { Text(attachment.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = "Retirer ${attachment.name}", modifier = Modifier.size(16.dp)) },
                                    enabled = !state.sending,
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { attachmentPicker.launch(arrayOf("image/*", "application/pdf", "text/plain", "text/csv", "text/markdown", "application/json")) },
                            enabled = !state.sending && !quotaLocked && (state.attachments.sumOf { it.imageDataUrls.size } < 4 || state.attachments.sumOf { it.documentDataUrls.size } < 2),
                        ) {
                            Icon(Icons.Outlined.AttachFile, contentDescription = "Joindre un fichier ou une image", tint = MaterialTheme.colorScheme.primary)
                        }
                        AtlasTextField(
                            value = state.input,
                            onValueChange = viewModel::onInputChange,
                            label = if (quotaLocked) {
                                "Quota IA atteint — réinitialisation automatique…"
                            } else {
                                "Poser une question à Atlas Agent…"
                            },
                            placeholder = "Ex. Résumé des ventes et impayés du jour",
                            imeAction = ImeAction.Send,
                            enabled = !state.sending && !quotaLocked,
                            onDone = { viewModel.sendPrompt() },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.sendPrompt() },
                            enabled = (state.input.isNotBlank() || state.attachments.isNotEmpty()) && !state.sending && !quotaLocked,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "Envoyer",
                                tint = if (quotaLocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shared live Quota card used in Settings AND inside Atlas Agent (`GET /api/agent/policy`).
 */
@Composable
fun AgentQuotaCard(
    policy: AgentPolicyDto?,
    onRefresh: () -> Unit,
) {
    AtlasCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Atlas Agent IA — Quotas & Limites",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            policy?.let { p ->
                AtlasChip(
                    text = when {
                        p.blocked -> "Suspendu"
                        p.isAdmin -> "Admin · Illimité"
                        p.isExhausted -> "Limite atteinte"
                        else -> "Actif"
                    },
                    tone = when {
                        p.blocked || p.isExhausted -> AtlasTone.DANGER
                        p.isAdmin -> AtlasTone.INFO
                        else -> AtlasTone.POSITIVE
                    },
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Actualiser le quota",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        if (policy == null) {
            Text(
                text = "Chargement des compteurs de jetons depuis le serveur…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AgentQuotaBarsContent(policy = policy, compact = false)
        }
    }
}

@Composable
fun AgentQuotaBarsContent(
    policy: AgentPolicyDto,
    compact: Boolean,
) {
    val nf = remember { NumberFormat.getIntegerInstance(Locale.FRANCE) }

    if (policy.blocked) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Atlas.palette.danger.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.HourglassBottom,
                    contentDescription = null,
                    tint = Atlas.palette.danger,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Accès Atlas Agent suspendu sur ce compte. Contactez un administrateur pour le rétablir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Atlas.palette.danger,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        return
    }

    if (policy.isDailyExhausted || policy.isWeeklyExhausted) {
        val waitText = if (policy.isDailyExhausted) {
            "Limite quotidienne atteinte — veuillez patienter ${formatQuotaWait(policy.dailyResetsAt, "minuit UTC")}."
        } else {
            "Limite hebdomadaire atteinte — veuillez patienter ${formatQuotaWait(policy.weeklyResetsAt, "lundi 00:00 UTC")}."
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Atlas.palette.danger.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.HourglassBottom,
                    contentDescription = null,
                    tint = Atlas.palette.danger,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = waitText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Atlas.palette.danger,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    // Daily bar
    val animatedDaily by androidx.compose.animation.core.animateFloatAsState(
        targetValue = policy.dailyFraction,
        label = "quota_daily_bar",
    )
    val animatedWeekly by androidx.compose.animation.core.animateFloatAsState(
        targetValue = policy.weeklyFraction,
        label = "quota_weekly_bar",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (policy.isAdmin) "Quota quotidien · Illimité" else "Quota quotidien",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "${nf.format(policy.dailyUsed)} / ${nf.format(policy.dailyLimit)} jetons",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
        )
    }
    Spacer(Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { animatedDaily },
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = when {
            policy.isDailyExhausted -> Atlas.palette.danger
            !policy.isAdmin && policy.dailyFraction > 0.8f -> Atlas.palette.warning
            else -> MaterialTheme.colorScheme.primary
        },
    )
    if (!compact) {
        Spacer(Modifier.height(3.dp))
        Text(
            text = "Renouvellement ${formatQuotaWait(policy.dailyResetsAt, "à minuit UTC chaque jour")}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(8.dp))

    // Weekly bar
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (policy.isAdmin) "Quota hebdomadaire · Illimité" else "Quota hebdomadaire",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "${nf.format(policy.weeklyUsed)} / ${nf.format(policy.weeklyLimit)} jetons",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
        )
    }
    Spacer(Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { animatedWeekly },
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = when {
            policy.isWeeklyExhausted -> Atlas.palette.danger
            !policy.isAdmin && policy.weeklyFraction > 0.8f -> Atlas.palette.warning
            else -> MaterialTheme.colorScheme.primary
        },
    )
    if (!compact) {
        Spacer(Modifier.height(3.dp))
        Text(
            text = "Renouvellement ${formatQuotaWait(policy.weeklyResetsAt, "lundi à 00:00 UTC")}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatQuotaWait(iso: String?, fallback: String): String {
    if (iso.isNullOrBlank()) return "prévu à $fallback"
    return runCatching {
        val target = Instant.parse(iso)
        val now = Instant.now()
        val mins = Duration.between(now, target).toMinutes()
        val stamp = DateTimeFormatter.ofPattern("dd/MM HH:mm 'UTC'", Locale.FRANCE)
            .withZone(ZoneOffset.UTC)
            .format(target)
        if (mins > 0) {
            val h = mins / 60
            val m = mins % 60
            if (h > 0) "dans ${h}h ${m}min ($stamp)" else "dans ${m} min ($stamp)"
        } else {
            "à $stamp"
        }
    }.getOrElse { "prévu à $fallback" }
}

@Composable
private fun ChatBubbleRow(
    message: AgentViewModel.ChatMessage,
    approvingId: String?,
    onCopy: () -> Unit,
    onApprove: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (message.fromUser) 0.85f else 0.98f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (message.fromUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                )
                .animateContentSize()
                .padding(14.dp),
        ) {
            Column {
                if (message.fromUser) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (message.attachmentNames.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "📎 ${message.attachmentNames.joinToString()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                        )
                    }
                } else {
                    AtlasMarkdown(
                        markdown = message.text,
                        textColor = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (message.tools.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
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

                if (!message.fromUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onCopy) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copier la réponse",
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Copier", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
