package ma.etherealmber.atlascommerce.ui.screens.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException
import ma.etherealmber.atlascommerce.core.AppLanguage
import ma.etherealmber.atlascommerce.data.repo.AGENT_MAX_QUESTION_LENGTH
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.model.AgentPolicyDto
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

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val commerceRepository: CommerceRepository,
    private val sessionManager: SessionManager,
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
    )

    data class UiState(
        val status: AssistStatusDto? = null,
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
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var pendingSensitivePrompt: String? = null
    private var pendingSensitiveAttachments: List<AgentAttachment> = emptyList()
    private var pendingSensitiveResponseLanguage: AppLanguage = AppLanguage.FRENCH
    @Volatile private var activeSendRequestId: String? = null
    @Volatile private var activeThreadLoadId: String? = null

    private val _notices = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    private val _navigationRequests = MutableSharedFlow<AgentNavigationSuggestion>(extraBufferCapacity = 1)
    val navigationRequests: SharedFlow<AgentNavigationSuggestion> = _navigationRequests.asSharedFlow()

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

    fun clearConversation() {
        val currentId = _state.value.activeThreadId
        val existedRemotely = _state.value.threads.any { it.id == currentId }
        startNewConversation()
        if (existedRemotely) {
            viewModelScope.launch {
                runCatching { commerceRepository.deleteAgentThread(currentId) }
                _state.update { st -> st.copy(threads = st.threads.filterNot { it.id == currentId }) }
            }
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

    fun deleteThread(threadId: String) {
        if (threadId.isBlank()) return
        val deletingActive = _state.value.activeThreadId == threadId
        val replacementThreadId = if (deletingActive) newWebCompatibleThreadId() else null
        if (deletingActive) {
            activeSendRequestId = null
            activeThreadLoadId = null
            _state.update { current ->
                if (current.activeThreadId != threadId) current
                else current.copy(
                    activeThreadId = replacementThreadId.orEmpty(),
                    activeThreadTitle = "",
                    messages = emptyList(),
                    sending = false,
                    loadingThread = false,
                    streamingReply = "",
                    streamingTools = emptyList(),
                    error = null,
                )
            }
        }
        viewModelScope.launch {
            commerceRepository.deleteAgentThread(threadId)
            _state.update { current -> current.copy(threads = current.threads.filterNot { it.id == threadId }) }
            _notices.tryEmit("Discussion supprimée.")
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
            val threadsRes = threadsDef.await()
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

        // Explicit native navigation is safe and deterministic. Handle it locally instead of
        // waiting for business-context requests and an LLM round-trip to perform a tab switch.
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

                    // Persist only the response for the conversation that still owns this request.
                    val r2Messages = allMessages.map { message ->
                        AgentThreadMessageDto(
                            role = if (message.fromUser) "user" else "assistant",
                            text = if (message.fromUser && message.attachmentNames.isNotEmpty()) {
                                "${message.text}\n\n📎 ${message.attachmentNames.joinToString()}"
                            } else message.text,
                            ms = message.ms,
                        )
                    }
                    runCatching {
                        commerceRepository.putAgentThread(
                            id = activeId,
                            title = resolvedTitle,
                            messages = r2Messages,
                        )
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
