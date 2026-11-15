package ma.etherealmber.atlascommerce.ui.screens.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.core.ThemeMode
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.update.AppUpdateManager
import ma.etherealmber.atlascommerce.data.repo.AGENT_MAX_QUESTION_LENGTH
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.model.AgentPolicyDto
import ma.etherealmber.atlascommerce.data.model.MobileWorkStatusDto
import ma.etherealmber.atlascommerce.data.model.WorkStatusSourceDto
import ma.etherealmber.atlascommerce.data.model.WorkStatusTrackersDto
import ma.etherealmber.atlascommerce.data.model.AgentThreadMessageDto
import ma.etherealmber.atlascommerce.data.model.AgentThreadSummaryDto
import ma.etherealmber.atlascommerce.data.model.AssistNextActionItemDto
import ma.etherealmber.atlascommerce.data.model.AssistProposalDto
import ma.etherealmber.atlascommerce.data.model.AssistStatusDto
import ma.etherealmber.atlascommerce.data.model.AssistToolTraceDto
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import java.text.Normalizer
import java.util.UUID
import javax.inject.Inject

/** Preserve any draft edited after Send while clearing the submitted text (including its whitespace). */
internal fun clearSubmittedAgentDraft(currentInput: String, submittedInput: String): String =
    if (currentInput.trim() == submittedInput) "" else currentInput

/** API timestamps are ISO-8601 strings, so lexicographic descending order is chronological. */
internal fun newestAgentThreadsFirst(threads: List<AgentThreadSummaryDto>): List<AgentThreadSummaryDto> =
    threads.withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<AgentThreadSummaryDto>> { it.value.updatedAt.orEmpty() }
                .thenBy { it.index },
        )
        .map { it.value }

internal fun firstSelectableAgentThread(threads: List<AgentThreadSummaryDto>): AgentThreadSummaryDto? =
    threads.firstOrNull { it.id.isNotBlank() }

/** The Worker’s done.reply is authoritative; never append it to already-rendered token fragments. */
@Suppress("UNUSED_PARAMETER")
internal fun replaceProvisionalReply(provisional: String, doneReply: String): String =
    doneReply.ifBlank { "Analyse terminée." }

internal fun agentThemeChoiceReply(mode: ThemeMode?, language: AppLanguage): String {
    val themeName = when (language) {
        AppLanguage.FRENCH -> when (mode) {
            ThemeMode.SYSTEM -> "système"
            ThemeMode.LIGHT -> "clair"
            ThemeMode.DARK -> "sombre"
            null -> ""
        }
        AppLanguage.ENGLISH -> when (mode) {
            ThemeMode.SYSTEM -> "system"
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK -> "dark"
            null -> ""
        }
        AppLanguage.ARABIC -> when (mode) {
            ThemeMode.SYSTEM -> "النظام"
            ThemeMode.LIGHT -> "الفاتح"
            ThemeMode.DARK -> "الداكن"
            null -> ""
        }
    }
    return when (language) {
        AppLanguage.FRENCH -> if (mode == null) {
            "Quel thème souhaitez-vous ? Choisissez une option ci-dessous. Aucun changement ne sera appliqué avant votre choix."
        } else {
            "Vous avez demandé le thème $themeName. Confirmez ci-dessous pour l’appliquer, ou choisissez-en un autre."
        }
        AppLanguage.ENGLISH -> if (mode == null) {
            "Which theme would you like? Choose one below. Nothing changes until you confirm."
        } else {
            "You asked for the $themeName theme. Confirm below to apply it, or choose another theme."
        }
        AppLanguage.ARABIC -> if (mode == null) {
            "ما المظهر الذي تفضله؟ اختر أحد الخيارات أدناه. لن يتغير المظهر حتى تؤكد اختيارك."
        } else {
            "طلبت المظهر $themeName. أكّد أدناه لتطبيقه أو اختر مظهراً آخر."
        }
    }
}

internal fun agentThemeChoiceMode(text: String): ThemeMode? {
    val clean = text.trim()
    return ThemeMode.entries.firstOrNull { mode ->
        AppLanguage.entries.any { language -> agentThemeChoiceReply(mode, language) == clean }
    }
}

internal fun isAgentThemeChoicePrompt(text: String): Boolean {
    val clean = text.trim()
    return agentThemeChoiceMode(clean) != null || clean in setOf(
        "Quel thème souhaitez-vous ? Choisissez une option ci-dessous. Aucun changement ne sera appliqué avant votre choix.",
        "Which theme would you like? Choose one below. Nothing changes until you confirm.",
        "ما المظهر الذي تفضله؟ اختر أحد الخيارات أدناه. لن يتغير المظهر حتى تؤكد اختيارك.",
        "Quel thème souhaitez-vous ? Choisissez une option ci-dessous.",
        "Which theme would you like? Choose one below.",
        "ما المظهر الذي تفضله؟ اختر أحد الخيارات أدناه.",
        "Choisissez le thème à appliquer ici.",
        "Choose the theme to apply here.",
        "اختر المظهر الذي تريد تطبيقه هنا.",
    )
}

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val commerceRepository: CommerceRepository,
    private val sessionManager: SessionManager,
    private val preferences: AtlasPreferences,
    private val appUpdateManager: AppUpdateManager,
) : ViewModel() {

    data class AgentAttachment(
        val name: String,
        val imageDataUrls: List<String>,
        val documentDataUrls: List<String>,
    )

    data class ChatMessage(
        val id: String = UUID.randomUUID().toString(),
        val fromUser: Boolean,
        val text: String,
        val attachmentNames: List<String> = emptyList(),
        val ms: Long? = null,
        val tools: List<AssistToolTraceDto> = emptyList(),
        val proposals: List<AssistProposalDto> = emptyList(),
        val approvedIds: Set<String> = emptySet(),
        val showThemeOptions: Boolean = false,
        val suggestedThemeMode: ThemeMode? = null,
    )

    data class UiState(
        val status: AssistStatusDto? = null,
        val mobileWorkStatus: MobileWorkStatusDto? = null,
        val workStatusLoading: Boolean = false,
        val workStatusError: String? = null,
        val policy: AgentPolicyDto? = null,
        val nextActions: List<AssistNextActionItemDto> = emptyList(),
        val threads: List<AgentThreadSummaryDto> = emptyList(),
        val activeThreadId: String = newWebCompatibleThreadId(),
        val activeThreadTitle: String = "",
        val messages: List<ChatMessage> = emptyList(),
        val loadingThread: Boolean = false,
        val loadingContext: Boolean = false,
        val input: String = "",
        val attachments: List<AgentAttachment> = emptyList(),
        val sensitiveConfirmation: Boolean = false,
        val streamingReply: String = "",
        val streamingTools: List<AssistToolTraceDto> = emptyList(),
        val sending: Boolean = false,
        val approvingId: String? = null,
        val error: String? = null,
        val updateConversationActive: Boolean = false,
        val updateCheckApprovalPending: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var pendingSensitivePrompt: String? = null
    private var pendingSensitiveAttachments: List<AgentAttachment> = emptyList()
    private var pendingSensitiveResponseLanguage: AppLanguage = AppLanguage.FRENCH
    @Volatile private var activeSendRequestId: String? = null
    @Volatile private var activeThreadLoadId: String? = null
    private val pendingThemeChoiceIds = mutableSetOf<String>()

    private val _notices = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    private val _navigationRequests = MutableSharedFlow<AgentNavigationSuggestion>(extraBufferCapacity = 1)
    val navigationRequests: SharedFlow<AgentNavigationSuggestion> = _navigationRequests.asSharedFlow()

    private val _deviceUnlockRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deviceUnlockRequests: SharedFlow<Unit> = _deviceUnlockRequests.asSharedFlow()

    private val _workStatusRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val workStatusRequests: SharedFlow<Unit> = _workStatusRequests.asSharedFlow()

    init {
        viewModelScope.launch {
            sessionManager.state
                .map { st -> (st as? SessionState.Active)?.user?.scopeIdentity }
                .distinctUntilChanged()
                .collect { identity ->
                    activeSendRequestId = null
                    activeThreadLoadId = null
                    _state.value = UiState(
                        activeThreadId = newWebCompatibleThreadId(),
                        loadingContext = identity != null,
                    )
                    if (identity != null) {
                        refreshContext(loadLatestRemoteThread = true)
                    }
                }
        }
    }

    fun approveUpdateCheck() {
        _state.update { it.copy(updateCheckApprovalPending = false) }
        appUpdateManager.checkForUpdate(manual = true)
    }

    /** Opens and pages the read-only work-status endpoint without retaining data across accounts. */
    fun loadWorkStatus(loadMore: Boolean = false) {
        val scope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity ?: return
        val current = _state.value.mobileWorkStatus
        if (loadMore && current == null) return
        val inCursor = if (loadMore) current?.inApp?.nextCursor else null
        val github = if (loadMore) current?.trackers?.github else null
        if (loadMore && inCursor == null && github?.nextCursor == null) return
        _state.update { it.copy(workStatusLoading = true, workStatusError = null) }
        viewModelScope.launch {
            when (val result = commerceRepository.mobileWorkStatus(inCursor, github?.nextCursor)) {
                is ApiResult.Success -> {
                    if ((sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != scope) return@launch
                    val page = result.data
                    val previous = if (loadMore) _state.value.mobileWorkStatus else null
                    val merged = if (previous == null) page else page.copy(
                        inApp = appendWorkStatusPage(previous.inApp, page.inApp),
                        trackers = WorkStatusTrackersDto(
                            github = if (previous.trackers.github != null || page.trackers.github != null) {
                                appendWorkStatusPage(previous.trackers.github ?: WorkStatusSourceDto(), page.trackers.github ?: WorkStatusSourceDto())
                            } else null,
                        ),
                    )
                    _state.update { it.copy(mobileWorkStatus = merged, workStatusLoading = false, workStatusError = null) }
                }
                is ApiResult.Failure -> {
                    if ((sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != scope) return@launch
                    _state.update { it.copy(workStatusLoading = false, workStatusError = result.message) }
                }
            }
        }
    }

    private fun appendWorkStatusPage(old: WorkStatusSourceDto, page: WorkStatusSourceDto): WorkStatusSourceDto =
        page.copy(items = (old.items + page.items).distinctBy { it.id })

    /** Starts a fresh conversation using the exact web `t-<base36>` thread ID format. */
    fun startNewConversation() {
        activeSendRequestId = null
        activeThreadLoadId = null
        val freshId = newWebCompatibleThreadId()
        pendingSensitivePrompt = null
        pendingSensitiveAttachments = emptyList()
        _state.update {
            it.copy(
                activeThreadId = freshId,
                activeThreadTitle = "",
                messages = emptyList(),
                updateConversationActive = false,
                updateCheckApprovalPending = false,
                attachments = emptyList(),
                streamingReply = "",
                streamingTools = emptyList(),
                sensitiveConfirmation = false,
                error = null,
                sending = false,
                loadingThread = false,
            )
        }
    }


    fun selectThread(summary: AgentThreadSummaryDto) {
        if (summary.id.isBlank()) return
        pendingSensitivePrompt = null
        pendingSensitiveAttachments = emptyList()
        activeSendRequestId = null
        val loadId = UUID.randomUUID().toString()
        activeThreadLoadId = loadId
        _state.update {
            it.copy(
                activeThreadId = summary.id,
                activeThreadTitle = summary.title,
                messages = emptyList(),
                updateConversationActive = false,
                updateCheckApprovalPending = false,
                loadingThread = true,
                sending = false,
                attachments = emptyList(),
                streamingReply = "",
                streamingTools = emptyList(),
                sensitiveConfirmation = false,
                error = null,
            )
        }
        viewModelScope.launch {
            val detail = try {
                withTimeoutOrNull(6_000L) { commerceRepository.getAgentThread(summary.id) }
                    ?: ApiResult.Failure("THREAD_LOAD_TIMEOUT", "Impossible de charger cette discussion à temps. Réessayez ou démarrez une nouvelle discussion.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AtlasLogBuffer.log(
                    "AGENT",
                    "Thread load failed: ${error.javaClass.simpleName}: ${AtlasLogBuffer.sanitize(error.message.orEmpty()).take(180)}",
                    "ERROR",
                )
                ApiResult.Failure("THREAD_LOAD_FAILED", "Impossible de charger cette discussion. Vérifiez la connexion puis réessayez.")
            }
            if (activeThreadLoadId != loadId || _state.value.activeThreadId != summary.id) return@launch
            when (detail) {
                is ApiResult.Failure -> {
                    _state.update { current ->
                        if (activeThreadLoadId != loadId || current.activeThreadId != summary.id) current
                        else current.copy(loadingThread = false, error = detail.message)
                    }
                }

                is ApiResult.Success -> {
                    val mapped = detail.data.messages
                        .filter { it.text.isNotBlank() && (it.role == "user" || it.role == "assistant") }
                        .map { message ->
                            ChatMessage(
                                fromUser = message.role == "user",
                                text = message.text,
                                ms = message.ms,
                                showThemeOptions = message.role == "assistant" && isAgentThemeChoicePrompt(message.text),
                                suggestedThemeMode = if (message.role == "assistant") agentThemeChoiceMode(message.text) else null,
                            )
                        }
                    _state.update { current ->
                        if (activeThreadLoadId != loadId || current.activeThreadId != summary.id) current
                        else current.copy(
                            loadingThread = false,
                            activeThreadTitle = detail.data.title?.takeIf { it.isNotBlank() } ?: summary.title,
                            messages = mapped,
                        )
                    }
                }
            }
        }
    }


    fun refreshContext(loadLatestRemoteThread: Boolean = false) {
        val requestedScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity ?: return
        val current = _state.value
        val showInitialLoader = current.status == null && current.policy == null && current.threads.isEmpty() && current.messages.isEmpty()
        if (showInitialLoader) _state.update { it.copy(loadingContext = true) }
        viewModelScope.launch {
            // These independent boot requests run concurrently and each has a hard deadline.
            // One stalled status/thread endpoint must not leave the entire Agent screen blank.
            val statusDef = async { loadAgentContext { commerceRepository.assistStatus() } }
            val policyDef = async { loadAgentContext { commerceRepository.agentPolicy() } }
            val nextDef = async { loadAgentContext { commerceRepository.assistNextActions() } }
            val threadsDef = async { loadAgentContext { commerceRepository.listAgentThreads() } }

            // Prioritize the conversation list: waiting for status/policy first could hold
            // a ready thread behind an unrelated slow endpoint. Start loading its detail as
            // soon as the list arrives, while the other context calls continue concurrently.
            var threadsRes = threadsDef.await()
            if (threadsRes !is ApiResult.Success) {
                AtlasLogBuffer.log("AGENT", "Conversation list request failed; retrying once", "WARN")
                threadsRes = loadAgentContext { commerceRepository.listAgentThreads() }
            }
            val threadLoadError = when (threadsRes) {
                is ApiResult.Failure -> "Saved conversations could not be loaded (${threadsRes.code}). Check the connection and retry."
                null -> "Saved conversations could not be loaded. Check the connection and retry."
                else -> null
            }
            val currentScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity
            if (currentScope != requestedScope) return@launch
            val remoteThreads = newestAgentThreadsFirst((threadsRes as? ApiResult.Success)?.data.orEmpty())
            val selectedThread = if (loadLatestRemoteThread && _state.value.messages.isEmpty()) {
                firstSelectableAgentThread(remoteThreads)
            } else {
                null
            }
            _state.update { current ->
                current.copy(
                    threads = if (threadsRes is ApiResult.Success) remoteThreads else current.threads,
                    loadingContext = false,
                    // Keep one continuous loading state while moving from context to thread detail.
                    loadingThread = selectedThread != null || current.loadingThread,
                    error = threadLoadError,
                )
            }
            selectedThread?.let(::selectThread)

            val statusRes = statusDef.await()
            val policyRes = policyDef.await()
            val nextRes = nextDef.await()
            val stillSameScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity
            if (stillSameScope != requestedScope) return@launch
            _state.update { current ->
                current.copy(
                    status = (statusRes as? ApiResult.Success)?.data ?: current.status,
                    policy = (policyRes as? ApiResult.Success)?.data ?: current.policy,
                    nextActions = (nextRes as? ApiResult.Success)?.data?.items ?: current.nextActions,
                )
            }
        }
    }

    private fun executeLocalAction(
        prompt: String,
        typedDraft: String,
        action: AgentLocalAction,
        responseLanguage: AppLanguage,
        snapshot: UiState,
    ) {
        val activeId = snapshot.activeThreadId
        val requestScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity ?: return
        val requestId = UUID.randomUUID().toString()
        val resolvedTitle = snapshot.activeThreadTitle.ifBlank { prompt.take(48) }
        val userMessage = ChatMessage(fromUser = true, text = prompt)
        activeSendRequestId = requestId
        _state.update { current ->
            if (current.activeThreadId != activeId) current
            else current.copy(
                input = clearSubmittedAgentDraft(current.input, typedDraft),
                activeThreadTitle = resolvedTitle,
                sending = true,
                streamingReply = "",
                streamingTools = emptyList(),
                error = null,
                messages = current.messages + userMessage,
            )
        }

        viewModelScope.launch {
            val reply = when (action) {
                AgentLocalAction.ShowWorkStatus -> {
                    _workStatusRequests.tryEmit(Unit)
                    when (responseLanguage) {
                        AppLanguage.FRENCH -> "J’ouvre votre activité dans ce panneau."
                        AppLanguage.ENGLISH -> "Opening your work status in this panel."
                        AppLanguage.ARABIC -> "أفتح حالة أعمالك في هذه اللوحة."
                    }
                }
                is AgentLocalAction.ChooseTheme -> agentThemeChoiceReply(action.requestedMode, responseLanguage)
                AgentLocalAction.ShowSettingsPanel -> when (responseLanguage) {
                    AppLanguage.FRENCH -> "Dites-moi quel réglage vous souhaitez modifier et je le ferai ici."
                    AppLanguage.ENGLISH -> "Tell me which preference you want to change and I’ll handle it here."
                    AppLanguage.ARABIC -> "أخبرني بالإعداد الذي تريد تغييره وسأعدّله هنا."
                }
                AgentLocalAction.ConfigureDeviceUnlock -> {
                    _deviceUnlockRequests.tryEmit(Unit)
                    when (responseLanguage) {
                        AppLanguage.FRENCH -> "J’ai ouvert les réglages du déverrouillage ici. Activez-le pour lancer la configuration biométrique."
                        AppLanguage.ENGLISH -> "I opened device unlock settings here. Turn it on to start biometric setup."
                        AppLanguage.ARABIC -> "فتحت إعدادات إلغاء القفل هنا. فعّله لبدء إعداد البصمة."
                    }
                }
                AgentLocalAction.CheckForUpdate -> when (responseLanguage) {
                    AppLanguage.FRENCH -> "Je peux vérifier les mises à jour ici. Confirmez avant de démarrer la vérification."
                    AppLanguage.ENGLISH -> "I can check for updates here. Confirm to start the check."
                    AppLanguage.ARABIC -> "يمكنني التحقق من التحديثات هنا. أكّد لبدء التحقق."
                }
            }
            val assistantMessage = ChatMessage(
                fromUser = false,
                text = reply,
                showThemeOptions = action is AgentLocalAction.ChooseTheme,
                suggestedThemeMode = (action as? AgentLocalAction.ChooseTheme)?.requestedMode,
            )
            val currentScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity
            if (currentScope != requestScope || activeSendRequestId != requestId) return@launch
            activeSendRequestId = null
            _state.update { current ->
                if (current.activeThreadId != activeId) current
                else {
                    val allMessages = current.messages + assistantMessage
                    val summary = AgentThreadSummaryDto(
                        id = activeId,
                        title = resolvedTitle,
                        messageCount = allMessages.size,
                    )
                    current.copy(
                        activeThreadTitle = resolvedTitle,
                        messages = allMessages,
                        updateConversationActive = action == AgentLocalAction.CheckForUpdate || current.updateConversationActive,
                        updateCheckApprovalPending = action == AgentLocalAction.CheckForUpdate || current.updateCheckApprovalPending,
                        threads = listOf(summary) + current.threads.filterNot { it.id == activeId },
                        sending = false,
                        streamingReply = "",
                        streamingTools = emptyList(),
                        error = null,
                    )
                }
            }
            val messagesToPersist = _state.value.messages.takeIf { _state.value.activeThreadId == activeId }
            if (messagesToPersist != null && !persistAgentThread(requestScope, activeId, resolvedTitle, messagesToPersist, attempts = 3)) {
                _notices.tryEmit(conversationSaveFailureNotice(responseLanguage))
            }
        }
    }


    fun selectThemeOption(messageId: String, mode: ThemeMode) {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return
        val scope = activeUser.scopeIdentity
        val current = _state.value
        if (current.messages.none { it.id == messageId && it.showThemeOptions }) return
        if (!pendingThemeChoiceIds.add(messageId)) return

        viewModelScope.launch {
            try {
                val saved = try {
                    preferences.setThemeMode(mode)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    false
                }
                if (!saved) {
                    _notices.tryEmit("Could not save the selected theme. Please try again.")
                    return@launch
                }
                val responseLanguage = try {
                    preferences.appLanguage.first()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    AppLanguage.ENGLISH
                }
                val confirmation = when (responseLanguage) {
                    AppLanguage.FRENCH -> when (mode) {
                        ThemeMode.SYSTEM -> "C’est fait : Atlas suit maintenant le thème du système."
                        ThemeMode.LIGHT -> "C’est fait : le thème clair est activé."
                        ThemeMode.DARK -> "C’est fait : le thème sombre est activé."
                    }
                    AppLanguage.ENGLISH -> when (mode) {
                        ThemeMode.SYSTEM -> "Done. Atlas now follows your system theme."
                        ThemeMode.LIGHT -> "Done. The light theme is on."
                        ThemeMode.DARK -> "Done. The dark theme is on."
                    }
                    AppLanguage.ARABIC -> when (mode) {
                        ThemeMode.SYSTEM -> "تم. سيتبع Atlas الآن مظهر النظام."
                        ThemeMode.LIGHT -> "تم تفعيل المظهر الفاتح."
                        ThemeMode.DARK -> "تم تفعيل المظهر الداكن."
                    }
                }
                if ((sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != scope) return@launch
                _state.update { state ->
                    if (state.messages.none { it.id == messageId && it.showThemeOptions }) state
                    else state.copy(
                        messages = state.messages.map { message ->
                            if (message.id == messageId) {
                                message.copy(text = confirmation, showThemeOptions = false, suggestedThemeMode = null)
                            } else message
                        },
                    )
                }
                val latest = _state.value
                if (latest.activeThreadId == current.activeThreadId &&
                    !persistAgentThread(scope, latest.activeThreadId, latest.activeThreadTitle, latest.messages, attempts = 3)
                ) {
                    _notices.tryEmit(conversationSaveFailureNotice(responseLanguage))
                }
            } finally {
                pendingThemeChoiceIds.remove(messageId)
            }
        }
    }

    private suspend fun persistAgentThread(
        scope: String,
        id: String,
        title: String,
        messages: List<ChatMessage>,
        attempts: Int,
    ): Boolean {
        val requestMessages = messages.takeLast(40).map { message ->
            AgentThreadMessageDto(
                role = if (message.fromUser) "user" else "assistant",
                text = if (message.fromUser && message.attachmentNames.isNotEmpty()) {
                    "${message.text}\n\n📎 ${message.attachmentNames.joinToString()}"
                } else message.text,
                ms = message.ms,
            )
        }
        repeat(attempts.coerceAtLeast(1)) { attempt ->
            if ((sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity != scope) return false
            val result = try {
                commerceRepository.putAgentThread(id = id, title = title, messages = requestMessages)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AtlasLogBuffer.log(
                    "AGENT",
                    "Thread persistence exception: ${error.javaClass.simpleName}: ${AtlasLogBuffer.sanitize(error.message.orEmpty()).take(160)}",
                    "WARN",
                )
                null
            }
            when (result) {
                is ApiResult.Success -> {
                    AtlasLogBuffer.log("AGENT", "Conversation thread saved to backend: id=${id.take(18)} messages=${requestMessages.size}")
                    return true
                }
                is ApiResult.Failure -> AtlasLogBuffer.log(
                    "AGENT",
                    "Thread persistence failed (${result.code}): ${AtlasLogBuffer.sanitize(result.message).take(160)}",
                    "WARN",
                )
                null -> Unit
            }
            if (attempt + 1 < attempts) delay(350L * (attempt + 1))
        }
        return false
    }

    private fun conversationSaveFailureNotice(language: AppLanguage): String = when (language) {
        AppLanguage.FRENCH -> "Impossible d’enregistrer cette conversation sur votre compte. Vérifiez la connexion avant de fermer l’application."
        AppLanguage.ENGLISH -> "This conversation could not be saved to your account. Check your connection before closing the app."
        AppLanguage.ARABIC -> "تعذر حفظ هذه المحادثة في حسابك. تحقق من الاتصال قبل إغلاق التطبيق."
    }


    private suspend fun <T> loadAgentContext(request: suspend () -> ApiResult<T>): ApiResult<T>? = try {
        withTimeoutOrNull(4_000L) { request() }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    fun onInputChange(value: String) {
        _state.update { it.copy(input = value, error = null) }
    }

    fun addAttachment(attachment: PreparedAgentAttachment) {
        val current = _state.value.attachments
        val usedPages = current.sumOf { it.imageDataUrls.size }
        val usedDocuments = current.sumOf { it.documentDataUrls.size }
        if (usedPages + attachment.imageDataUrls.size > 4) {
            _notices.tryEmit("Atlas Agent accepte jusqu'à 4 images par message.")
            return
        }
        if (usedDocuments + attachment.documentDataUrls.size > 2) {
            _notices.tryEmit("Atlas Agent accepte jusqu'à 2 documents par message.")
            return
        }
        _state.update {
            it.copy(
                attachments = it.attachments + AgentAttachment(attachment.displayName, attachment.imageDataUrls, attachment.documentDataUrls),
                error = null,
            )
        }
    }

    fun removeAttachment(index: Int) {
        _state.update { current -> current.copy(attachments = current.attachments.filterIndexed { i, _ -> i != index }) }
    }

    fun confirmSensitivePrompt() {
        val prompt = pendingSensitivePrompt ?: return
        val attachments = pendingSensitiveAttachments
        val responseLanguage = pendingSensitiveResponseLanguage
        pendingSensitivePrompt = null
        pendingSensitiveAttachments = emptyList()
        pendingSensitiveResponseLanguage = AppLanguage.FRENCH
        _state.update { it.copy(sensitiveConfirmation = false) }
        sendPrompt(prompt, attachments, sensitiveConfirmed = true, responseLanguage = responseLanguage)
    }

    fun cancelSensitivePrompt() {
        pendingSensitivePrompt = null
        pendingSensitiveAttachments = emptyList()
        _state.update { it.copy(sensitiveConfirmation = false) }
    }

    fun sendPrompt(
        prompt: String = _state.value.input,
        attachments: List<AgentAttachment> = _state.value.attachments,
        sensitiveConfirmed: Boolean = false,
        responseLanguage: AppLanguage = AppLanguage.FRENCH,
    ) {
        val typed = prompt.trim()
        val attachmentOnlyPrompt = when (responseLanguage) {
            AppLanguage.FRENCH -> "Lis les fichiers joints et résume les informations importantes."
            AppLanguage.ENGLISH -> "Read the attached files and summarize the important information."
            AppLanguage.ARABIC -> "اقرأ الملفات المرفقة ولخّص المعلومات المهمة."
        }
        val clean = typed.ifBlank { if (attachments.isNotEmpty()) attachmentOnlyPrompt else "" }
        val snapshot = _state.value
        if (clean.isBlank() || snapshot.sending || snapshot.loadingThread) return
        if (clean.length > AGENT_MAX_QUESTION_LENGTH) {
            _state.update { it.copy(error = "Le message est trop long. Limitez-le à 1 800 caractères.") }
            return
        }
        if (!sensitiveConfirmed && (requiresSensitiveConfirmation(clean) || attachments.isNotEmpty())) {
            pendingSensitivePrompt = clean
            pendingSensitiveAttachments = attachments
            pendingSensitiveResponseLanguage = responseLanguage
            _state.update { it.copy(sensitiveConfirmation = true) }
            return
        }

        if (isForbiddenAgentRequest(clean)) {
            val refusal = when (responseLanguage) {
                AppLanguage.FRENCH -> "Je ne peux pas accéder aux données locales ou au cache, supprimer des éléments ou des serveurs, ni fermer votre session."
                AppLanguage.ENGLISH -> "I can’t access local data or cache, delete items or servers, or sign you out."
                AppLanguage.ARABIC -> "لا يمكنني الوصول إلى البيانات المحلية أو ذاكرة التخزين المؤقت، أو حذف العناصر أو الخوادم، أو تسجيل خروجك."
            }
            activeSendRequestId = null
            _state.update { current -> current.copy(
                input = clearSubmittedAgentDraft(current.input, typed),
                messages = current.messages + ChatMessage(fromUser = true, text = clean) + ChatMessage(fromUser = false, text = refusal),
                sending = false,
                error = null,
            ) }
            return
        }

        // Explicit native navigation is safe and deterministic. Handle it locally instead of
        // waiting for business-context requests and an LLM round-trip to perform a tab switch.
        val localAction = if (attachments.isEmpty()) detectExplicitAgentLocalAction(clean) else null
        if (localAction != null) {
            executeLocalAction(clean, typed, localAction, responseLanguage, snapshot)
            return
        }

        val localNavigation = if (attachments.isEmpty()) detectExplicitAgentNavigationRequest(clean) else null
        if (localNavigation != null) {
            activeSendRequestId = null
            val localReply = when (responseLanguage) {
                AppLanguage.FRENCH -> "J’ouvre la rubrique demandée."
                AppLanguage.ENGLISH -> "Opening that section."
                AppLanguage.ARABIC -> "جارٍ فتح القسم المطلوب."
            }
            _state.update { current ->
                current.copy(
                    input = clearSubmittedAgentDraft(current.input, typed),
                    activeThreadTitle = current.activeThreadTitle.ifBlank { clean.take(48) },
                    messages = current.messages +
                        ChatMessage(fromUser = true, text = clean) +
                        ChatMessage(fromUser = false, text = localReply),
                    streamingReply = "",
                    streamingTools = emptyList(),
                    sending = false,
                    error = null,
                )
            }
            _navigationRequests.tryEmit(localNavigation)
            AtlasLogBuffer.log("AGENT", "Handled explicit native navigation locally: ${localNavigation.destination}")
            return
        }

        val currentPolicy = snapshot.policy
        if (currentPolicy != null && currentPolicy.blocked) {
            _state.update {
                it.copy(error = "Accès à Atlas Agent suspendu pour ce compte. Contactez un administrateur.")
            }
            return
        }
        if (currentPolicy != null && currentPolicy.isDailyExhausted) {
            _state.update {
                it.copy(error = "Quota quotidien atteint — réinitialisation à minuit UTC. Veuillez patienter.")
            }
            return
        }
        if (currentPolicy != null && currentPolicy.isWeeklyExhausted) {
            _state.update {
                it.copy(error = "Quota hebdomadaire atteint — réinitialisation lundi à 00:00 UTC. Veuillez patienter.")
            }
            return
        }

        val activeId = snapshot.activeThreadId
        val requestId = UUID.randomUUID().toString()
        activeSendRequestId = requestId
        val resolvedTitle = snapshot.activeThreadTitle.ifBlank { clean.take(48) }
        val userBubble = ChatMessage(
            fromUser = true,
            text = clean,
            attachmentNames = attachments.map { it.name },
        )

        AtlasLogBuffer.log("AGENT", "Chat request started: request=${requestId.take(8)} thread=$activeId")
        _state.update {
            it.copy(
                input = clearSubmittedAgentDraft(it.input, typed),
                activeThreadTitle = resolvedTitle,
                sending = true,
                streamingReply = "",
                streamingTools = emptyList(),
                error = null,
                messages = it.messages + userBubble,
            )
        }

        viewModelScope.launch {
            val startedMs = System.currentTimeMillis()
            val activeUser = (sessionManager.state.value as? SessionState.Active)?.user
            if (activeUser == null) {
                finishSendWithError(requestId, activeId, "Votre session a expiré. Reconnectez-vous puis réessayez.")
                return@launch
            }
            val requestScope = activeUser.scopeIdentity
            // Save the user's turn before the network-backed assistant response starts. If the
            // app is closed while generation is running, the conversation still has a backend row.
            persistAgentThread(requestScope, activeId, resolvedTitle, snapshot.messages + userBubble, attempts = 1)
            val imageAttachments = attachments.flatMap { it.imageDataUrls }.take(4)
            val documentAttachments = attachments.flatMap { it.documentDataUrls }.take(2)
            val result = try {
                if (imageAttachments.isNotEmpty()) {
                    // The Worker keeps vision on the established buffered response path.
                    commerceRepository.assistChat(
                        message = clean,
                        role = activeUser.role,
                        username = activeUser.username,
                        threadId = activeId,
                        images = imageAttachments,
                        documents = documentAttachments,
                        responseLanguage = responseLanguage,
                    )
                } else {
                    commerceRepository.assistChatStream(
                        message = clean,
                        role = activeUser.role,
                        username = activeUser.username,
                        threadId = activeId,
                        documents = documentAttachments,
                        responseLanguage = responseLanguage,
                        onToken = { token ->
                            if (isCurrentSend(requestId, activeId)) {
                                _state.update { current ->
                                    if (current.activeThreadId == activeId && current.sending) {
                                        current.copy(streamingReply = current.streamingReply + token)
                                    } else current
                                }
                            }
                        },
                        onTool = { tool ->
                            if (isCurrentSend(requestId, activeId)) {
                                _state.update { current ->
                                    if (current.activeThreadId == activeId && current.sending) {
                                        current.copy(streamingTools = current.streamingTools + tool)
                                    } else current
                                }
                            }
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                finishSendWithError(requestId, activeId, "Réponse interrompue. Vous pouvez réessayer.")
                throw cancelled
            } catch (error: Exception) {
                AtlasLogBuffer.log(
                    "AGENT",
                    "Chat request failed: ${error.javaClass.simpleName}: ${AtlasLogBuffer.sanitize(error.message.orEmpty()).take(180)}",
                    "ERROR",
                )
                finishSendWithError(requestId, activeId, "Atlas Agent n'a pas pu terminer la réponse. Vérifiez la connexion puis réessayez.")
                val failedTurn = _state.value.messages.takeIf { _state.value.activeThreadId == activeId }
                if (failedTurn != null && !persistAgentThread(requestScope, activeId, resolvedTitle, failedTurn, attempts = 3)) {
                    _notices.tryEmit(conversationSaveFailureNotice(responseLanguage))
                }
                return@launch
            }

            val currentScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity
            if (requestScope != currentScope) {
                finishSendWithError(requestId, activeId, "La session a changé pendant la réponse. Réessayez.")
                return@launch
            }
            if (!isCurrentSend(requestId, activeId)) {
                AtlasLogBuffer.log("AGENT", "Discarded response for an inactive conversation")
                return@launch
            }
            when (result) {
                is ApiResult.Failure -> {
                    AtlasLogBuffer.log("AGENT", "Chat request failed: ${result.code}", "ERROR")
                    // Refresh quota right away in case server returned 429 / quota reached.
                    val refreshedPolicy = (commerceRepository.agentPolicy() as? ApiResult.Success)?.data
                    finishSendWithError(requestId, activeId, result.message, refreshedPolicy)
                    val failedTurn = _state.value.messages.takeIf { _state.value.activeThreadId == activeId }
                    if (failedTurn != null && !persistAgentThread(requestScope, activeId, resolvedTitle, failedTurn, attempts = 3)) {
                        _notices.tryEmit(conversationSaveFailureNotice(responseLanguage))
                    }
                }

                is ApiResult.Success -> {
                    val elapsedMs = (System.currentTimeMillis() - startedMs).coerceAtLeast(1L)
                    AtlasLogBuffer.log("AGENT", "Chat response completed in ${elapsedMs}ms")
                    val replyText = replaceProvisionalReply(_state.value.streamingReply, result.data.reply)
                    val replyBubble = ChatMessage(
                        fromUser = false,
                        text = replyText,
                        ms = elapsedMs,
                        tools = result.data.toolTrace,
                        proposals = result.data.proposals,
                    )
                    if (!isCurrentSend(requestId, activeId)) return@launch
                    val allMessages = _state.value.messages + replyBubble
                    val summaryEntry = AgentThreadSummaryDto(
                        id = activeId,
                        title = resolvedTitle,
                        messageCount = allMessages.size,
                    )
                    // Finish the backend write before presenting the turn as fully complete.
                    val threadSaved = persistAgentThread(requestScope, activeId, resolvedTitle, allMessages, attempts = 3)
                    activeSendRequestId = null
                    _state.update { current ->
                        if (current.activeThreadId != activeId) current
                        else current.copy(
                            sending = false,
                            streamingReply = "",
                            streamingTools = emptyList(),
                            attachments = emptyList(),
                            messages = allMessages,
                            threads = listOf(summaryEntry) + current.threads.filterNot { it.id == activeId },
                        )
                    }

                    if (!threadSaved && requestScope == (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity) {
                        _notices.tryEmit(conversationSaveFailureNotice(responseLanguage))
                    }

                    // Immediately refresh live token quota counter.
                    val updatedPolicy = (commerceRepository.agentPolicy() as? ApiResult.Success)?.data
                    if (updatedPolicy != null && requestScope == (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity) {
                        _state.update { it.copy(policy = updatedPolicy) }
                    }
                }
            }
        }
    }

    private fun isCurrentSend(requestId: String, threadId: String): Boolean =
        activeSendRequestId == requestId && _state.value.activeThreadId == threadId

    private fun finishSendWithError(
        requestId: String,
        threadId: String,
        message: String,
        updatedPolicy: AgentPolicyDto? = null,
    ) {
        if (!isCurrentSend(requestId, threadId)) return
        activeSendRequestId = null
        _state.update { current ->
            if (current.activeThreadId != threadId) current
            else current.copy(
                sending = false,
                streamingReply = "",
                streamingTools = emptyList(),
                policy = updatedPolicy ?: current.policy,
                error = message,
            )
        }
    }

    fun approveProposal(messageId: String, proposalId: String) {
        if (proposalId.isBlank() || _state.value.approvingId != null) return
        val proposal = _state.value.messages
            .firstOrNull { it.id == messageId }?.proposals
            ?.firstOrNull { it.proposalId == proposalId }
        if (proposal == null) {
            _notices.tryEmit("Cette action ne peut pas être vérifiée et n’a pas été exécutée.")
            return
        }
        if (isForbiddenAgentProposal(proposal)) {
            _notices.tryEmit("Cette action n’est pas autorisée dans Atlas Agent.")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(approvingId = proposalId) }
            when (val res = commerceRepository.assistApprove(proposalId)) {
                is ApiResult.Failure -> {
                    _state.update { it.copy(approvingId = null) }
                    _notices.tryEmit(res.message)
                }

                is ApiResult.Success -> {
                    _state.update { current ->
                        current.copy(
                            approvingId = null,
                            messages = current.messages.map { msg ->
                                if (msg.id == messageId) {
                                    msg.copy(approvedIds = msg.approvedIds + proposalId)
                                } else {
                                    msg
                                }
                            },
                        )
                    }
                    _notices.tryEmit(res.data.message ?: "Action approuvée et exécutée.")
                    refreshContext()
                }
            }
        }
    }

    private fun requiresSensitiveConfirmation(prompt: String): Boolean {
        val normalized = Normalizer.normalize(prompt.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        val markers = listOf(
            "government", "gouvernement", "ministere", "ministry", "police", "tribunal", "justice",
            "impot", "tax authority", "passeport", "passport", "carte nationale", "carte d'identite",
            "identifiant fiscal", "numero fiscal", "mot de passe", "password", "api key", "cle api",
            "iban", "rib", "medical", "sante", "health", "dossier medical", "numero de securite sociale",
        )
        return markers.any { marker ->
            if (marker.contains(' ')) normalized.contains(marker)
            else Regex("(^|[^a-z0-9])${Regex.escape(marker)}([^a-z0-9]|$)").containsMatchIn(normalized)
        }
    }

    companion object {
        /** Identical `t-${Date.now().toString(36)}` thread ID format used by `AtlasAgent.tsx` on Web. */
        fun newWebCompatibleThreadId(): String =
            "t-${java.lang.Long.toString(System.currentTimeMillis(), 36)}"
    }
}

/** Keep the empty conversation placeholder visible only while there is no content to show. */
internal fun shouldShowAgentChatSkeleton(state: AgentViewModel.UiState): Boolean =
    state.messages.isEmpty() && (state.loadingContext || state.loadingThread)
