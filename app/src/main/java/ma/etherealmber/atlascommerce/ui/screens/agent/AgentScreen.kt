package ma.etherealmber.atlascommerce.ui.screens.agent

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.KeyboardArrowDown
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import ma.etherealmber.atlascommerce.data.model.AgentPolicyDto
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChatSkeleton
import ma.etherealmber.atlascommerce.ui.components.AtlasShimmer
import ma.etherealmber.atlascommerce.ui.components.AtlasSkeletonBlock
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasMarkdown
import ma.etherealmber.atlascommerce.ui.components.AtlasMark
import ma.etherealmber.atlascommerce.ui.components.AtlasSectionHeader
import ma.etherealmber.atlascommerce.ui.components.AtlasTextField
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.AtlasTypingDots
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import ma.etherealmber.atlascommerce.ui.theme.LocalAtlasDarkTheme
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.text.Normalizer

enum class AgentNavigationDestination { HOME, PRODUCTS, SALES, BALANCE_REPORT, EMPLOYEES, SCAN, NOTIFICATIONS, SETTINGS, PROFILE }

data class AgentNavigationSuggestion(
    val destination: AgentNavigationDestination,
    val fromDate: String? = null,
    val toDate: String? = null,
)

/** Return only routes the app actually exposes; never infer a data result or business action. */
internal fun detectAgentNavigationSuggestion(prompt: String): AgentNavigationSuggestion? {
    val normalized = Normalizer.normalize(prompt, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
    val asksLocation = listOf(
        "where is", "where are", "where can i find", "where do i find", "where can i go",
        "where is the", "where are the", "where can i access", "where do i access",
        "ou est", "ou sont", "ou se trouve", "ou trouver", "ou puis-je", "ou se situe",
        "أين", "فين", "وين",
    ).any { phrase -> normalized.contains(phrase) }
    val mentionsReport = listOf("report", "reports", "rapport", "rapports", "bilan", "financial statement")
        .any { phrase -> normalized.contains(phrase) }
    if (mentionsReport) {
        val year = Regex("\\b(20\\d{2})\\b").find(normalized)?.groupValues?.get(1)?.toIntOrNull()
        val monthNames = "january|jan|janvier|february|feb|fevrier|march|mar|mars|april|apr|avril|may|mai|june|jun|juin|july|jul|juillet|august|aug|aout|september|sep|sept|septembre|october|oct|octobre|november|nov|novembre|december|dec|decembre"
        val monthIndex = mapOf(
            "january" to 1, "jan" to 1, "janvier" to 1,
            "february" to 2, "feb" to 2, "fevrier" to 2,
            "march" to 3, "mar" to 3, "mars" to 3,
            "april" to 4, "apr" to 4, "avril" to 4,
            "may" to 5, "mai" to 5,
            "june" to 6, "jun" to 6, "juin" to 6,
            "july" to 7, "jul" to 7, "juillet" to 7,
            "august" to 8, "aug" to 8, "aout" to 8,
            "september" to 9, "sep" to 9, "sept" to 9, "septembre" to 9,
            "october" to 10, "oct" to 10, "octobre" to 10,
            "november" to 11, "nov" to 11, "novembre" to 11,
            "december" to 12, "dec" to 12, "decembre" to 12,
        )
        if (year != null) {
            val range = Regex("($monthNames)\\s*(?:to|through|until|au|a|and|[-–—])\\s*($monthNames)\\s*(?:of\\s*)?(20\\d{2})")
                .find(normalized)
            if (range != null) {
                val fromMonth = monthIndex[range.groupValues[1]]
                val toMonth = monthIndex[range.groupValues[2]]
                val rangeYear = range.groupValues[3].toIntOrNull()
                if (fromMonth != null && toMonth != null && rangeYear != null && fromMonth <= toMonth) {
                    val from = YearMonth.of(rangeYear, fromMonth).atDay(1).toString()
                    val to = YearMonth.of(rangeYear, toMonth).atEndOfMonth().toString()
                    return AgentNavigationSuggestion(AgentNavigationDestination.BALANCE_REPORT, from, to)
                }
            }
            val singleMonth = Regex("($monthNames)\\s+(20\\d{2})").find(normalized)
            val month = singleMonth?.groupValues?.get(1)?.let(monthIndex::get)
            val singleYear = singleMonth?.groupValues?.get(2)?.toIntOrNull()
            if (month != null && singleYear != null) {
                val ym = YearMonth.of(singleYear, month)
                return AgentNavigationSuggestion(
                    AgentNavigationDestination.BALANCE_REPORT,
                    ym.atDay(1).toString(),
                    ym.atEndOfMonth().toString(),
                )
            }
        }
        return AgentNavigationSuggestion(AgentNavigationDestination.BALANCE_REPORT)
    }
    if (!asksLocation) return null
    val destination = when {
        listOf("product", "products", "item", "items", "stock", "inventory", "catalogue", "catalog", "article", "articles", "produit", "produits", "inventaire", "مخزون", "منتج").any { phrase -> normalized.contains(phrase) } -> AgentNavigationDestination.PRODUCTS
        listOf("sale", "sales", "invoice", "invoices", "vente", "ventes", "facture", "الفاتورة", "مبيعات").any { phrase -> normalized.contains(phrase) } -> AgentNavigationDestination.SALES
        listOf("balance", "report", "reports", "rapport", "bilan", "finance", "finances", "الرصيد").any { phrase -> normalized.contains(phrase) } -> AgentNavigationDestination.BALANCE_REPORT
        listOf("employee", "employees", "staff", "employe", "employes", "personnel", "موظف", "الموظفين").any { phrase -> normalized.contains(phrase) } -> AgentNavigationDestination.EMPLOYEES
        listOf("scan", "scanner", "barcode", "qr", "code-barre", "مسح").any { phrase -> normalized.contains(phrase) } -> AgentNavigationDestination.SCAN
        listOf("notification", "notifications", "alerte", "alertes", "تنبيه").any { phrase -> normalized.contains(phrase) } -> AgentNavigationDestination.NOTIFICATIONS
        listOf("setting", "settings", "parametre", "parametres", "réglage", "إعدادات").any { phrase -> normalized.contains(phrase) } -> AgentNavigationDestination.SETTINGS
        listOf("profile", "account", "profil", "compte", "الحساب").any { phrase -> normalized.contains(phrase) } -> AgentNavigationDestination.PROFILE
        listOf("home", "dashboard", "accueil", "الرئيسية").any { phrase -> normalized.contains(phrase) } -> AgentNavigationDestination.HOME
        else -> null
    }
    return destination?.let { AgentNavigationSuggestion(destination = it) }
}

@Composable
fun AgentScreen(
    onToast: (String) -> Unit,
    onNavigate: (AgentNavigationSuggestion) -> Unit = {},
    isActive: Boolean = true,
    historyRequested: Boolean = false,
    onHistoryHandled: () -> Unit = {},
    viewModel: AgentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var protectAgentWhileImeCloses by remember { mutableStateOf(false) }
    LaunchedEffect(isActive, isImeVisible) {
        if (!isActive) {
            protectAgentWhileImeCloses = false
        } else if (isImeVisible) {
            protectAgentWhileImeCloses = true
        } else if (protectAgentWhileImeCloses) {
            // Some keyboards dispatch a final Back while their inset is disappearing.
            // Keep the Agent handler active briefly so Send cannot pop the screen.
            delay(600)
            protectAgentWhileImeCloses = false
        }
    }
    BackHandler(enabled = isActive && (isImeVisible || protectAgentWhileImeCloses)) {
        focusManager.clearFocus()
    }
    val selectedAppLanguage = LocalAtlasLanguage.current
    // Financial prompt access cannot be inferred from the mobile role model today: the policy
    // endpoint exposes only isAdmin, so keep money-related prompts admin-only until the API
    // provides an explicit capability set.
    val quickPrompts = buildList {
        add(atlasText("Quels articles sont en rupture ou à réapprovisionner ?", "Which items are out of stock or need replenishment?", "ما المنتجات التي نفدت أو تحتاج إلى إعادة التخزين؟"))
        if (state.policy?.isAdmin == true) {
            add(atlasText("Résumé des ventes et impayés du jour", "Summarize today's sales and unpaid invoices", "لخّص مبيعات اليوم والفواتير غير المدفوعة"))
        }
        add(atlasText("Quelles demandes scan sont en attente ?", "Which scan requests are pending?", "ما طلبات المسح المعلّقة؟"))
    }
    // Server next-actions carry no role/capability metadata. Hide them for non-admin roles rather
    // than risk prompting users into actions their account cannot access.
    val visibleNextActions = if (state.policy?.isAdmin == true) state.nextActions else emptyList()
    val newConversationToast = atlasText("Nouvelle discussion ouverte.", "New conversation started.", "بدأت محادثة جديدة.")
    val density = Atlas.density
    val scope = rememberCoroutineScope()
    var showHistoryDialog by remember { mutableStateOf(false) }
    LaunchedEffect(historyRequested) {
        if (historyRequested) { showHistoryDialog = true; onHistoryHandled() }
    }
    val latestUserPrompt = state.messages.lastOrNull { it.fromUser }?.text.orEmpty()
    val detectedNavigationSuggestion = remember(latestUserPrompt) { detectAgentNavigationSuggestion(latestUserPrompt) }
    val navigationSuggestion = detectedNavigationSuggestion?.takeUnless {
        it.destination == AgentNavigationDestination.BALANCE_REPORT && state.policy?.isAdmin != true
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
            title = { Text(atlasText("Discussions synchronisées (Web & Mobile)", "Synced conversations (Web & mobile)", "المحادثات المتزامنة (الويب والهاتف)")) },
            text = {
                if (state.threads.isEmpty()) {
                    Text(
                        text = atlasText("Aucune discussion enregistrée sur ce compte pour le moment.", "No conversations are saved to this account yet.", "لا توجد محادثات محفوظة لهذا الحساب حتى الآن."),
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
                                            text = localizedAgentText(thread.title, selectedAppLanguage).ifBlank { atlasText("Discussion", "Conversation", "محادثة") },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = if (isCurrent) {
                                                atlasText("Active actuellement", "Currently active", "نشطة حاليًا")
                                            } else {
                                                atlasText("Synchronisée Web/R2", "Synced with Web/R2", "متزامنة مع الويب وR2")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteThread(thread.id) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = atlasText("Supprimer", "Delete", "حذف"),
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
                        onToast(newConversationToast)
                    },
                ) {
                    Text(atlasText("Nouvelle discussion", "New conversation", "محادثة جديدة"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text(atlasText("Fermer", "Close", "إغلاق"))
                }
            },
        )
    }

    if (state.sensitiveConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::cancelSensitivePrompt,
            title = { Text(atlasText("Vérifiez avant d'envoyer", "Review before sending", "راجِع قبل الإرسال")) },
            text = {
                Text(atlasText(
                    "Atlas Agent peut se tromper. Votre message sera transmis au serveur Atlas et au fournisseur IA configuré. N'envoyez pas de mots de passe ni de données personnelles inutiles.",
                    "Atlas Agent can make mistakes. Your message will be sent to the Atlas server and configured AI provider. Do not include passwords or unnecessary personal data.",
                    "قد يخطئ مساعد Atlas. ستُرسل رسالتك إلى خادم Atlas ومزوّد الذكاء الاصطناعي المُعدّ. لا ترسل كلمات مرور أو بيانات شخصية غير ضرورية.",
                ))
            },
            confirmButton = { TextButton(onClick = viewModel::confirmSensitivePrompt) { Text(atlasText("Envoyer quand même", "Send anyway", "إرسال على أي حال")) } },
            dismissButton = { TextButton(onClick = viewModel::cancelSensitivePrompt) { Text(atlasText("Annuler", "Cancel", "إلغاء")) } },
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
                    if (state.loadingThread) {
                        item(key = "agent_loading") {
                            AtlasChatSkeleton(Modifier.fillMaxWidth())
                        }
                    }
                    if (state.messages.isEmpty() && !state.loadingThread && !quotaLocked) {
                        item(key = "agent_quick_prompts") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                quickPrompts.forEach { prompt ->
                                    AssistChip(onClick = { viewModel.sendPrompt(prompt, responseLanguage = selectedAppLanguage) }, label = { Text(prompt, style = MaterialTheme.typography.bodySmall) })
                                }
                            }
                        }
                    }

                    if (state.messages.isEmpty() && !state.loadingThread && visibleNextActions.isNotEmpty() && !quotaLocked) {
                        item(key = "agent_suggestions_hdr") {
                            AtlasSectionHeader(title = atlasText("Actions suggérées par l'IA", "AI suggestions", "اقتراحات الذكاء الاصطناعي"))
                        }
                        items(
                            items = visibleNextActions,
                            key = { "suggestion_${it.title}_${it.priority}" },
                        ) { item ->
                            val suggestionPrompt = atlasText(
                                "Aide-moi sur : ${item.title} (${item.detail})",
                                "Help me with: ${item.title} (${item.detail})",
                                "ساعدني في: ${item.title} (${item.detail})",
                            )
                            AtlasCard(
                                onClick = { viewModel.sendPrompt(suggestionPrompt, responseLanguage = selectedAppLanguage) },
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
                            onCopy = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("Atlas", if (msg.fromUser) msg.text else localizedAgentText(msg.text, selectedAppLanguage)))
                                onToast("Réponse copiée.")
                            },
                            onApprove = { proposalId -> viewModel.approveProposal(msg.id, proposalId) },
                        )
                    }

                    navigationSuggestion?.let { suggestion ->
                        item(key = "agent_navigation_${state.messages.lastOrNull { it.fromUser }?.id}") {
                            val targetLabel = when (suggestion.destination) {
                                AgentNavigationDestination.HOME -> atlasText("Accueil", "Home", "الرئيسية")
                                AgentNavigationDestination.PRODUCTS -> atlasText("Produits et stock", "Products and inventory", "المنتجات والمخزون")
                                AgentNavigationDestination.SALES -> atlasText("Ventes", "Sales", "المبيعات")
                                AgentNavigationDestination.BALANCE_REPORT -> atlasText("Rapports financiers", "Finance reports", "التقارير المالية")
                                AgentNavigationDestination.EMPLOYEES -> atlasText("Employés", "Employees", "الموظفون")
                                AgentNavigationDestination.SCAN -> atlasText("Scan", "Scan", "المسح")
                                AgentNavigationDestination.NOTIFICATIONS -> atlasText("Notifications", "Notifications", "الإشعارات")
                                AgentNavigationDestination.SETTINGS -> atlasText("Paramètres", "Settings", "الإعدادات")
                                AgentNavigationDestination.PROFILE -> atlasText("Profil", "Profile", "الملف الشخصي")
                            }
                            val dateRange = if (suggestion.fromDate != null && suggestion.toDate != null) {
                                " · ${suggestion.fromDate} → ${suggestion.toDate}"
                            } else ""
                            AtlasCard(onClick = { onNavigate(suggestion) }, padding = 10.dp) {
                                Text(
                                    atlasText("Ouvrir une rubrique Atlas", "Open an Atlas area", "افتح قسماً في Atlas"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    "$targetLabel$dateRange",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    if (state.streamingReply.isNotBlank()) {
                        item(key = "agent_streaming_reply") {
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

                    if (state.sending && state.streamingReply.isBlank()) {
                        item(key = "agent_typing") {
                            AgentTypingIndicator(
                                label = atlasText("Atlas Agent réfléchit…", "Atlas Agent is thinking…", "مساعد Atlas يفكر…"),
                            )
                        }
                    }

                    if (!state.error.isNullOrBlank()) {
                        item(key = "agent_err") {
                            AtlasErrorCard(
                                title = atlasText("Réponse IA indisponible", "AI response unavailable", "تعذّرت استجابة الذكاء الاصطناعي"),
                                message = atlasNotice(state.error.orEmpty(), LocalAtlasLanguage.current),
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
                            contentDescription = atlasText("Aller aux derniers messages", "Jump to latest messages", "الانتقال إلى أحدث الرسائل"),
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = density.screenPadding,
                        vertical = if (compactComposer) 4.dp else 8.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AtlasTextField(
                        value = state.input,
                        onValueChange = viewModel::onInputChange,
                        label = if (quotaLocked) {
                            atlasText("Quota IA atteint — réinitialisation automatique…", "AI quota reached — resets automatically…", "تم بلوغ حصة الذكاء الاصطناعي — تُجدّد تلقائيًا…")
                        } else {
                            atlasText("Message à l’assistant Atlas", "Message Atlas Assistant", "رسالة إلى مساعد Atlas")
                        },
                        placeholder = atlasText("Écrivez votre question…", "Write your question…", "اكتب سؤالك…"),
                        imeAction = ImeAction.Send,
                        enabled = !state.sending && !state.loadingThread && !quotaLocked,
                        onDone = { viewModel.sendPrompt(responseLanguage = selectedAppLanguage) },
                        // Keep the chat focused after IME Send; dismissing the keyboard here
                        // could race the shell's back handler and navigate away from Agent.
                        clearFocusOnSend = false,
                        singleLine = false,
                        modifier = Modifier.weight(1f)
                            .heightIn(min = 58.dp, max = if (compactComposer) 92.dp else 132.dp)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
                                    if (state.input.isNotBlank() && !state.sending && !state.loadingThread && !quotaLocked) {
                                        viewModel.sendPrompt(responseLanguage = selectedAppLanguage)
                                    }
                                    true
                                } else {
                                    false
                                }
                            },
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = { viewModel.sendPrompt(responseLanguage = selectedAppLanguage) },
                        enabled = state.input.isNotBlank() && !state.sending && !state.loadingThread && !quotaLocked,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = atlasText("Envoyer", "Send", "إرسال"),
                            tint = if (quotaLocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AtlasAgentMascot(
    modifier: Modifier = Modifier,
    isTyping: Boolean = false,
    isThinking: Boolean = false,
) {
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    val isEngaged = isTyping || isThinking
    val transition = rememberInfiniteTransition(label = "atlas-agent-mascot")
    var blinkClosed by remember { mutableStateOf(false) }
    LaunchedEffect(motionEnabled, isEngaged) {
        blinkClosed = false
        if (!motionEnabled) return@LaunchedEffect
        while (true) {
            delay(if (isEngaged) 900L else 2_500L)
            blinkClosed = true
            delay(110L)
            blinkClosed = false
        }
    }
    val eyeScale by animateFloatAsState(
        targetValue = if (motionEnabled && blinkClosed) 0.15f else 1f,
        animationSpec = tween(90),
        label = "atlas-agent-eye-blink",
    )
    val gaze by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isEngaged) 440 else 1_300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "atlas-agent-eye-gaze",
    )
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
                Box(
                    Modifier.size(5.dp).graphicsLayer {
                        scaleY = eyeScale
                        translationX = if (motionEnabled && isEngaged) gaze * 2.2f else 0f
                    }.clip(CircleShape).background(onMascot),
                )
                Box(
                    Modifier.size(5.dp).graphicsLayer {
                        scaleY = eyeScale
                        translationX = if (motionEnabled && isEngaged) gaze * 2.2f else 0f
                    }.clip(CircleShape).background(onMascot),
                )
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
                text = atlasText("Atlas Agent IA — Quotas & Limites", "Atlas Agent AI — quotas & limits", "مساعد Atlas — الحصص والحدود"),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            policy?.let { p ->
                AtlasChip(
                    text = when {
                        p.blocked -> atlasText("Suspendu", "Suspended", "موقوف")
                        p.isAdmin -> atlasText("Admin · Illimité", "Admin · Unlimited", "مدير · غير محدود")
                        p.isExhausted -> atlasText("Limite atteinte", "Limit reached", "تم بلوغ الحد")
                        else -> atlasText("Actif", "Active", "نشط")
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
                    contentDescription = atlasText("Actualiser le quota", "Refresh quota", "تحديث الحصة"),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        if (policy == null) {
            AtlasShimmer {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    AtlasSkeletonBlock(Modifier.fillMaxWidth(0.7f).height(12.dp))
                    repeat(2) {
                        AtlasSkeletonBlock(Modifier.fillMaxWidth().height(9.dp), RoundedCornerShape(99.dp))
                    }
                }
            }
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
    val quotaLocale = Locale.forLanguageTag(LocalAtlasLanguage.current.tag)
    val nf = remember(quotaLocale) { NumberFormat.getIntegerInstance(quotaLocale) }

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
                    text = atlasText("Accès Atlas Agent suspendu sur ce compte. Contactez un administrateur pour le rétablir.", "Atlas Agent access is suspended for this account. Contact an administrator to restore it.", "تم تعليق الوصول إلى مساعد Atlas لهذا الحساب. تواصل مع المدير لإعادته."),
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
            atlasText(
                "Limite quotidienne atteinte — réinitialisation ${formatQuotaWait(policy.dailyResetsAt, "minuit UTC")}",
                "Daily limit reached — resets ${formatQuotaWait(policy.dailyResetsAt, "at midnight UTC")}",
                "تم بلوغ الحد اليومي — يُعاد الضبط ${formatQuotaWait(policy.dailyResetsAt, "منتصف الليل UTC")}",
            )
        } else {
            atlasText(
                "Limite hebdomadaire atteinte — réinitialisation ${formatQuotaWait(policy.weeklyResetsAt, "lundi 00:00 UTC")}",
                "Weekly limit reached — resets ${formatQuotaWait(policy.weeklyResetsAt, "Monday at 00:00 UTC")}",
                "تم بلوغ الحد الأسبوعي — يُعاد الضبط ${formatQuotaWait(policy.weeklyResetsAt, "الاثنين 00:00 UTC")}",
            )
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
            text = if (policy.isAdmin) {
                atlasText("Quota quotidien · Illimité", "Daily quota · Unlimited", "الحصة اليومية · غير محدودة")
            } else {
                atlasText("Quota quotidien", "Daily quota", "الحصة اليومية")
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${nf.format(policy.dailyUsed)} / ${nf.format(policy.dailyLimit)} ${atlasText("jetons", "tokens", "رمز")}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
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
            text = atlasText(
                "Renouvellement ${formatQuotaWait(policy.dailyResetsAt, "à minuit UTC chaque jour")}",
                "Resets ${formatQuotaWait(policy.dailyResetsAt, "at midnight UTC each day")}",
                "يُعاد الضبط ${formatQuotaWait(policy.dailyResetsAt, "منتصف الليل UTC يوميًا")}",
            ),
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
            text = if (policy.isAdmin) {
                atlasText("Quota hebdomadaire · Illimité", "Weekly quota · Unlimited", "الحصة الأسبوعية · غير محدودة")
            } else {
                atlasText("Quota hebdomadaire", "Weekly quota", "الحصة الأسبوعية")
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${nf.format(policy.weeklyUsed)} / ${nf.format(policy.weeklyLimit)} ${atlasText("jetons", "tokens", "رمز")}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
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
            text = atlasText(
                "Renouvellement ${formatQuotaWait(policy.weeklyResetsAt, "lundi à 00:00 UTC")}",
                "Resets ${formatQuotaWait(policy.weeklyResetsAt, "Monday at 00:00 UTC")}",
                "يُعاد الضبط ${formatQuotaWait(policy.weeklyResetsAt, "الاثنين 00:00 UTC")}",
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun formatQuotaWait(iso: String?, fallback: String): String {
    val language = LocalAtlasLanguage.current
    val locale = Locale.forLanguageTag(language.tag)
    val scheduledText = atlasText("prévu à $fallback", "scheduled for $fallback", "مجدول في $fallback")
    if (iso.isNullOrBlank()) return scheduledText
    val wait = runCatching {
        val target = Instant.parse(iso)
        val minutes = Duration.between(Instant.now(), target).toMinutes()
        val pattern = if (language == ma.etherealmber.atlascommerce.core.AppLanguage.FRENCH) {
            "dd/MM HH:mm 'UTC'"
        } else {
            "MMM d, HH:mm 'UTC'"
        }
        val stamp = DateTimeFormatter.ofPattern(pattern, locale)
            .withZone(ZoneOffset.UTC)
            .format(target)
        minutes to stamp
    }.getOrNull() ?: return scheduledText

    val (minutes, stamp) = wait
    if (minutes <= 0) return atlasText("à $stamp", "at $stamp", "في $stamp")
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (hours > 0) {
        atlasText(
            "dans ${hours}h ${remainder}min ($stamp)",
            "in ${hours}h ${remainder}m ($stamp)",
            "خلال ${hours} س و${remainder} د ($stamp)",
        )
    } else {
        atlasText("dans ${remainder} min ($stamp)", "in ${remainder} min ($stamp)", "خلال ${remainder} د ($stamp)")
    }
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
    val darkTheme = LocalAtlasDarkTheme.current
    val userGreen = if (darkTheme) Color(0xFF173D2B) else Color(0xFFE8F5E9)
    val userInk = if (darkTheme) Color(0xFFB7E4C7) else Color(0xFF14532D)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            AgentAvatar()
            Spacer(Modifier.width(8.dp))
        }
        val displayText = if (isUser || isStreaming) message.text else localizedAgentText(message.text, LocalAtlasLanguage.current)
        val messageDirection = messageTextDirection(displayText)
        CompositionLocalProvider(LocalLayoutDirection provides messageDirection) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = if (isUser == (messageDirection == LayoutDirection.Ltr)) Alignment.End else Alignment.Start,
            ) {
                Text(
                    text = if (isUser) atlasText("Vous", "You", "أنت") else atlasText("Assistant Atlas", "Atlas Assistant", "مساعد Atlas"),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUser) userInk else MaterialTheme.colorScheme.primary,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 1f),
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
                                markdown = displayText,
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
                                        text = proposal.title ?: atlasText("Action proposée", "Suggested action", "إجراء مقترح"),
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
                                            text = atlasText("Approuvée", "Approved", "تمت الموافقة"),
                                            tone = AtlasTone.POSITIVE,
                                            icon = Icons.Outlined.CheckCircle,
                                        )
                                    } else {
                                        TextButton(
                                            onClick = { onApprove(proposalId) },
                                            enabled = approvingId == null,
                                        ) {
                                            Text(atlasText("Approuver et exécuter", "Approve and run", "الموافقة والتنفيذ"))
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
                                    text = atlasText("Réponse en cours…", "Replying…", "جارٍ الرد…"),
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
                                        contentDescription = atlasText("Copier la réponse", "Copy reply", "نسخ الرد"),
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(atlasText("Copier", "Copy", "نسخ"), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Align each message's text to its actual script, independently of the app's UI language. */
private fun messageTextDirection(text: String): LayoutDirection {
    for (character in text) {
        if (character in '\u0590'..'\u08FF' || character in '\uFB1D'..'\uFEFC') {
            return LayoutDirection.Rtl
        }
        if (character.isLetter()) return LayoutDirection.Ltr
    }
    return LayoutDirection.Ltr
}

@Composable
private fun AgentAvatar() {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            AtlasMark(size = 19.dp)
        }
    }
}

@Composable
private fun AgentTypingIndicator(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        AtlasAgentMascot(Modifier.size(32.dp), isThinking = true)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = atlasText("Assistant Atlas", "Atlas Assistant", "مساعد Atlas"),
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
