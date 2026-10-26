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
import kotlin.coroutines.cancellation.CancellationException
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
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

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
        val input: String = "",
        val attachments: List<AgentAttachment> = emptyList(),
        val sensitiveConfirmation: Boolean = false,
        val streamingReply: String = "",
        val sending: Boolean = false,
        val approvingId: String? = null,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var pendingSensitivePrompt: String? = null
    private var pendingSensitiveAttachments: List<AgentAttachment> = emptyList()
    @Volatile private var activeSendRequestId: String? = null
    @Volatile private var activeThreadLoadId: String? = null

    private val _notices = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    init {
        viewModelScope.launch {
            sessionManager.state
                .map { st -> (st as? SessionState.Active)?.user?.scopeIdentity }
                .distinctUntilChanged()
                .collect { identity ->
                    activeSendRequestId = null
                    activeThreadLoadId = null
                    _state.value = UiState(activeThreadId = newWebCompatibleThreadId())
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
                sensitiveConfirmation = false,
                error = null,
            )
        }
        viewModelScope.launch {
            val detail = try {
                commerceRepository.getAgentThread(summary.id)
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
        viewModelScope.launch {
            val statusDef = async { commerceRepository.assistStatus() }
            val policyDef = async { commerceRepository.agentPolicy() }
            val nextDef = async { commerceRepository.assistNextActions() }
            val threadsDef = async { commerceRepository.listAgentThreads() }

            val statusRes = statusDef.await()
            val policyRes = policyDef.await()
            val nextRes = nextDef.await()
            val threadsRes = threadsDef.await()

            val remoteThreads = (threadsRes as? ApiResult.Success)?.data.orEmpty()
            _state.update {
                it.copy(
                    status = (statusRes as? ApiResult.Success)?.data ?: it.status,
                    policy = (policyRes as? ApiResult.Success)?.data ?: it.policy,
                    nextActions = (nextRes as? ApiResult.Success)?.data?.items ?: it.nextActions,
                    threads = remoteThreads.ifEmpty { it.threads },
                )
            }

            if (loadLatestRemoteThread && _state.value.messages.isEmpty() && remoteThreads.isNotEmpty()) {
                selectThread(remoteThreads.first())
            }
        }
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
        pendingSensitivePrompt = null
        pendingSensitiveAttachments = emptyList()
        _state.update { it.copy(sensitiveConfirmation = false) }
        sendPrompt(prompt, attachments, sensitiveConfirmed = true)
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
    ) {
        val typed = prompt.trim()
        val clean = typed.ifBlank { if (attachments.isNotEmpty()) "Lis les fichiers joints et résume les informations importantes." else "" }
        val snapshot = _state.value
        if (clean.isBlank() || snapshot.sending || snapshot.loadingThread) return
        if (!sensitiveConfirmed && (requiresSensitiveConfirmation(clean) || attachments.isNotEmpty())) {
            pendingSensitivePrompt = clean
            pendingSensitiveAttachments = attachments
            _state.update { it.copy(sensitiveConfirmation = true) }
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

        AtlasLogBuffer.log("AGENT", "Chat request started")
        val firstTokenLogged = AtomicBoolean(false)
        _state.update {
            it.copy(
                input = if (typed == it.input) "" else it.input,
                activeThreadTitle = resolvedTitle,
                sending = true,
                streamingReply = "",
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
            val result = try {
                commerceRepository.assistChatStream(
                    message = clean,
                    role = activeUser.role,
                    username = activeUser.username,
                    threadId = activeId,
                    images = attachments.flatMap { it.imageDataUrls }.take(4),
                    documents = attachments.flatMap { it.documentDataUrls }.take(2),
                    onToken = { delta ->
                        if (firstTokenLogged.compareAndSet(false, true)) {
                            AtlasLogBuffer.log("AGENT", "First response token received")
                        }
                        _state.update { current ->
                            if (activeSendRequestId == requestId && current.sending && current.activeThreadId == activeId) {
                                current.copy(streamingReply = current.streamingReply + delta)
                            } else current
                        }
                    },
                )
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
                    val replyText = result.data.reply.ifBlank { "Analyse terminée." }
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
