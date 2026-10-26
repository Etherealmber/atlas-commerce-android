package ma.etherealmber.atlascommerce.ui.screens.agent

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import ma.etherealmber.atlascommerce.data.model.AgentPolicyDto
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasDivider
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasMarkdown
import ma.etherealmber.atlascommerce.ui.components.AtlasMark
import ma.etherealmber.atlascommerce.ui.components.AtlasSectionHeader
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.AtlasTypingDots
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
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
    isActive: Boolean = true,
    viewModel: AgentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val density = Atlas.density
    val scope = rememberCoroutineScope()
    var showHistoryDialog by remember { mutableStateOf(false) }
    var composerExpanded by rememberSaveable { mutableStateOf(true) }
    var showQuotaDetails by rememberSaveable { mutableStateOf(true) }
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
    val conversationListState = rememberLazyListState()
    var userScrolledUp by remember(state.activeThreadId) { mutableStateOf(false) }
    val conversationScrollConnection = remember(conversationListState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    userScrolledUp = true
                }
                return Offset.Zero
            }
        }
    }

    // Once the user manually reaches the tail again, resume following new tokens.
    LaunchedEffect(conversationListState, state.activeThreadId) {
        snapshotFlow {
            !conversationListState.isScrollInProgress && !conversationListState.canScrollForward
        }.collect { atEnd ->
            if (atEnd) userScrolledUp = false
        }
    }
    // The screen remains composed behind other tabs. Reopening or loading a thread
    // lands directly on its latest turn, not through intermediate/random messages.
    LaunchedEffect(isActive, state.activeThreadId, state.loadingThread) {
        if (!isActive || state.loadingThread) return@LaunchedEffect
        userScrolledUp = false
        if (state.messages.isEmpty()) {
            conversationListState.scrollToItem(0)
        } else {
            conversationListState.scrollToConversationEnd()
        }
    }
    // New user turns always become visible. A completed assistant turn only
    // follows automatically if the user hasn't deliberately scrolled upward.
    LaunchedEffect(isActive, state.messages.size, state.sending) {
        if (!isActive || state.loadingThread || state.messages.isEmpty()) return@LaunchedEffect
        if (state.messages.lastOrNull()?.fromUser == true) userScrolledUp = false
        if (!userScrolledUp) conversationListState.scrollToConversationEnd()
    }
    // Keep the latest streamed text visible unless the user chooses to inspect
    // earlier messages; then pause auto-follow until they return to the bottom.
    LaunchedEffect(isActive, state.streamingReply.length) {
        if (!isActive || state.streamingReply.isBlank() || userScrolledUp) return@LaunchedEffect
        conversationListState.scrollToConversationEnd()
    }

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
        // Compact the composer when the remaining phone viewport is short,
        // especially while the keyboard is open.
        val compactComposer = maxHeight < 600.dp
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LazyColumn(
                    state = conversationListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(conversationScrollConnection),
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
                                    AtlasAgentMascot(Modifier.size(42.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = "Atlas Agent IA",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                        )
                                        Text(
                                            text = state.activeThreadTitle.takeIf { it.isNotBlank() }
                                                ?: "Stock, ventes et assistance",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    IconButton(onClick = { showHistoryDialog = true }) {
                                        Icon(Icons.Outlined.History, contentDescription = "Historique des discussions")
                                    }
                                    IconButton(onClick = {
                                        viewModel.startNewConversation()
                                        onToast("Nouvelle discussion prête.")
                                    }) {
                                        Icon(Icons.Outlined.AddComment, contentDescription = "Nouvelle discussion")
                                    }
                                    IconButton(onClick = viewModel::refreshContext) {
                                        Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser le contexte")
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AtlasChip(
                                        text = when {
                                            quotaLocked -> "Quota atteint"
                                            state.status?.configured == true -> "En ligne"
                                            state.status?.configured == false -> "En pause"
                                            else -> "Vérification…"
                                        },
                                        tone = when {
                                            quotaLocked -> AtlasTone.DANGER
                                            state.status?.configured == true -> AtlasTone.POSITIVE
                                            state.status?.configured == false -> AtlasTone.WARNING
                                            else -> AtlasTone.NEUTRAL
                                        },
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    val policy = state.policy
                                    Text(
                                        text = when {
                                            policy == null -> "Quota IA indisponible"
                                            policy.isAdmin -> "Quota IA illimité"
                                            else -> "${NumberFormat.getIntegerInstance(Locale.FRANCE).format(policy.dailyUsed)} / ${NumberFormat.getIntegerInstance(Locale.FRANCE).format(policy.dailyLimit)} jetons aujourd'hui"
                                        },
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    IconButton(
                                        onClick = { showQuotaDetails = !showQuotaDetails },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (showQuotaDetails) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.Info,
                                            contentDescription = if (showQuotaDetails) "Masquer les détails du quota" else "Afficher les détails du quota",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                if (showQuotaDetails) {
                                    state.policy?.let { policy ->
                                        Spacer(Modifier.height(6.dp))
                                        AtlasDivider()
                                        Spacer(Modifier.height(6.dp))
                                        AgentQuotaBarsContent(policy = policy, compact = true)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Atlas.palette.warningSoft,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(11.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Atlas.palette.warning,
                                        )
                                        Spacer(Modifier.width(9.dp))
                                        Text(
                                            "L'IA peut se tromper. Vérifiez les faits importants et évitez les données sensibles.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }

                                if (state.loadingThread) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(10.dp))
                                        Text("Chargement de la discussion…", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                if (state.messages.isEmpty() && !state.loadingThread && !quotaLocked) {
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
                        }
                    }

                    if (state.messages.isEmpty() && !state.loadingThread && state.nextActions.isNotEmpty() && !quotaLocked) {
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
                                    isStreaming = state.sending,
                                )
                            }
                        }
                    }

                    if (state.sending && state.streamingReply.isBlank()) {
                        item(key = "agent_typing") {
                            AgentTypingIndicator(
                                label = if (state.attachments.isNotEmpty()) {
                                    "Analyse des pièces jointes…"
                                } else {
                                    "Atlas Agent réfléchit…"
                                },
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
                if (userScrolledUp && conversationListState.canScrollForward && state.messages.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                conversationListState.scrollToConversationEnd()
                                userScrolledUp = false
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 14.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Aller aux derniers messages",
                        )
                    }
                }
            }

            Box(Modifier.fillMaxWidth().animateContentSize()) {
                if (composerExpanded) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        tonalElevation = 3.dp,
                        shadowElevation = 4.dp,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(
                                horizontal = density.screenPadding,
                                vertical = if (compactComposer) 4.dp else 8.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Écrire à Atlas Agent",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                IconButton(
                                    onClick = {
                                    focusManager.clearFocus()
                                    composerExpanded = false
                                },
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Masquer la zone de rédaction")
                                }
                            }
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
                                            enabled = !state.sending && !state.loadingThread,
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { attachmentPicker.launch(arrayOf("image/*", "application/pdf", "text/plain", "text/csv", "text/markdown", "application/json")) },
                                    enabled = !state.sending && !state.loadingThread && !quotaLocked && (state.attachments.sumOf { it.imageDataUrls.size } < 4 || state.attachments.sumOf { it.documentDataUrls.size } < 2),
                                ) {
                                    Icon(Icons.Outlined.AttachFile, contentDescription = "Joindre un fichier ou une image", tint = MaterialTheme.colorScheme.primary)
                                }
                                AtlasTextField(
                                    value = state.input,
                                    onValueChange = viewModel::onInputChange,
                                    label = if (quotaLocked) "Quota IA atteint — réinitialisation automatique…" else "Message à Atlas Agent",
                                    placeholder = "Écrivez votre question…",
                                    imeAction = ImeAction.Send,
                                    enabled = !state.sending && !state.loadingThread && !quotaLocked,
                                    onDone = { viewModel.sendPrompt() },
                                    singleLine = false,
                                    modifier = Modifier.weight(1f).heightIn(min = 58.dp, max = if (compactComposer) 92.dp else 132.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                IconButton(
                                    onClick = { viewModel.sendPrompt() },
                                    enabled = (state.input.isNotBlank() || state.attachments.isNotEmpty()) && !state.sending && !state.loadingThread && !quotaLocked,
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
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { composerExpanded = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Afficher la zone de rédaction",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text("${state.input.length} caractères", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AtlasAgentMascot(modifier: Modifier = Modifier) {
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    val transition = rememberInfiniteTransition(label = "atlas-agent-mascot")
    val bob by transition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "atlas-agent-mascot-bob",
    )
    val tilt by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "atlas-agent-mascot-tilt",
    )
    val onMascot = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = if (motionEnabled) bob else 0f
                rotationZ = if (motionEnabled) tilt else 0f
            }
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(4.dp).clip(CircleShape).background(onMascot))
                Box(Modifier.size(4.dp).clip(CircleShape).background(onMascot))
            }
            Box(
                Modifier
                    .padding(top = 1.dp)
                    .width(12.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(onMascot),
            )
        }
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = onMascot.copy(alpha = 0.9f),
            modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(11.dp),
        )
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
    isStreaming: Boolean = false,
) {
    val isUser = message.fromUser
    val userGreen = Atlas.palette.positiveSoft
    val userInk = Atlas.palette.positive

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            AgentAvatar(isUser = false)
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Text(
                text = if (isUser) "Vous" else "Atlas Agent",
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isUser) userInk else MaterialTheme.colorScheme.primary,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth(if (isUser) 0.88f else 1f)
                    .animateContentSize(),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomEnd = if (isUser) 5.dp else 16.dp,
                    bottomStart = if (isUser) 16.dp else 5.dp,
                ),
                color = if (isUser) userGreen else MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(Modifier.padding(14.dp)) {
                    if (isUser) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = userInk,
                        )
                        if (message.attachmentNames.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "📎 ${message.attachmentNames.joinToString()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = userInk,
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

                    message.proposals.forEach { proposal ->
                        val proposalId = proposal.proposalId.orEmpty()
                        if (proposalId.isNotBlank()) {
                            val approved = proposalId in message.approvedIds
                            Spacer(Modifier.height(8.dp))
                            AtlasCard(padding = 10.dp) {
                                Text(
                                    text = proposal.title ?: "Action proposée",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (!proposal.summary.isNullOrBlank()) {
                                    Text(
                                        text = proposal.summary,
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
                                        onClick = { onApprove(proposalId) },
                                        enabled = approvingId == null,
                                    ) {
                                        Text("Approuver et exécuter")
                                    }
                                }
                            }
                        }
                    }

                    if (isStreaming) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AtlasTypingDots()
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Réponse en cours…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (!isUser) {
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
        if (isUser) {
            Spacer(Modifier.width(8.dp))
            AgentAvatar(isUser = true)
        }
    }
}

@Composable
private fun AgentAvatar(isUser: Boolean) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        color = if (isUser) Atlas.palette.positiveSoft else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (isUser) Atlas.palette.positive else MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isUser) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Message de l'utilisateur",
                    modifier = Modifier.size(19.dp),
                )
            } else {
                AtlasMark(size = 19.dp)
            }
        }
    }
}

@Composable
private fun AgentTypingIndicator(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        AgentAvatar(isUser = false)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Atlas Agent",
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AtlasTypingDots()
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.34f)
                    .padding(start = 4.dp, top = 5.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private suspend fun LazyListState.scrollToConversationEnd() {
    snapshotFlow { layoutInfo.totalItemsCount }.first { it > 0 }
    withFrameNanos { }
    val lastIndex = layoutInfo.totalItemsCount - 1
    if (lastIndex < 0) return
    scrollToItem(lastIndex)
    withFrameNanos { }
    val finalIndex = (layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
    val lastItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == finalIndex }
    val viewportSize = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
    val overflow = ((lastItem?.size ?: 0) - viewportSize).coerceAtLeast(0)
    scrollToItem(finalIndex, scrollOffset = overflow)
}
