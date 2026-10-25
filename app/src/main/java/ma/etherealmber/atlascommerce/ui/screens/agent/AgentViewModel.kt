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
import ma.etherealmber.atlascommerce.core.net.ApiResult
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
                    _state.value = UiState(activeThreadId = newWebCompatibleThreadId())
                    if (identity != null) {
                        refreshContext(loadLatestRemoteThread = true)
                    }
                }
        }
    }

    /** Starts a fresh conversation using the exact web `t-<base36>` thread ID format. */
    fun startNewConversation() {
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
        viewModelScope.launch {
            _state.update {
                it.copy(
                    activeThreadId = summary.id,
                    activeThreadTitle = summary.title,
                    loadingThread = true,
                    attachments = emptyList(),
                    streamingReply = "",
                    sensitiveConfirmation = false,
                    error = null,
                )
            }
            when (val detail = commerceRepository.getAgentThread(summary.id)) {
                is ApiResult.Failure -> {
                    _state.update {
                        it.copy(
                            loadingThread = false,
                            error = detail.message,
                        )
                    }
                }

                is ApiResult.Success -> {
                    val mapped = detail.data.messages
                        .filter { it.text.isNotBlank() && (it.role == "user" || it.role == "assistant") }
                        .map { m ->
                            ChatMessage(
                                fromUser = m.role == "user",
                                text = m.text,
                                ms = m.ms,
                            )
                        }
                    _state.update {
                        it.copy(
                            loadingThread = false,
                            activeThreadTitle = detail.data.title?.takeIf { t -> t.isNotBlank() } ?: summary.title,
                            messages = mapped,
                        )
                    }
                }
            }
        }
    }

    fun deleteThread(threadId: String) {
        if (threadId.isBlank()) return
        viewModelScope.launch {
            commerceRepository.deleteAgentThread(threadId)
            val remaining = _state.value.threads.filterNot { it.id == threadId }
            val deletingActive = _state.value.activeThreadId == threadId
            _state.update {
                it.copy(
                    threads = remaining,
                    activeThreadId = if (deletingActive) newWebCompatibleThreadId() else it.activeThreadId,
                    activeThreadTitle = if (deletingActive) "" else it.activeThreadTitle,
                    messages = if (deletingActive) emptyList() else it.messages,
                )
            }
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
        if (clean.isBlank() || snapshot.sending) return
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
        val resolvedTitle = snapshot.activeThreadTitle.ifBlank { clean.take(48) }
        val userBubble = ChatMessage(
            fromUser = true,
            text = clean,
            attachmentNames = attachments.map { it.name },
        )

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
            val activeUser = (sessionManager.state.value as? SessionState.Active)?.user ?: return@launch
            val requestScope = activeUser.scopeIdentity
            val result = commerceRepository.assistChatStream(
                message = clean,
                role = activeUser.role,
                username = activeUser.username,
                threadId = activeId,
                images = attachments.flatMap { it.imageDataUrls }.take(4),
                documents = attachments.flatMap { it.documentDataUrls }.take(2),
                onToken = { delta ->
                    _state.update { current ->
                        if (current.sending && current.activeThreadId == activeId) current.copy(streamingReply = current.streamingReply + delta) else current
                    }
                },
            )
            val currentScope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity
            if (requestScope != currentScope) return@launch
            when (val res = result) {
                is ApiResult.Failure -> {
                    // Refresh quota right away in case server returned 429 / quota reached
                    val refreshedPolicy = (commerceRepository.agentPolicy() as? ApiResult.Success)?.data
                    _state.update {
                        it.copy(
                            sending = false,
                            streamingReply = "",
                            policy = refreshedPolicy ?: it.policy,
                            error = res.message,
                        )
                    }
                }

                is ApiResult.Success -> {
                    val elapsedMs = (System.currentTimeMillis() - startedMs).coerceAtLeast(1L)
                    val replyText = res.data.reply.ifBlank { "Analyse terminée." }
                    val replyBubble = ChatMessage(
                        fromUser = false,
                        text = replyText,
                        ms = elapsedMs,
                        tools = res.data.toolTrace,
                        proposals = res.data.proposals,
                    )
                    val allMessages = _state.value.messages + replyBubble
                    val summaryEntry = AgentThreadSummaryDto(
                        id = activeId,
                        title = resolvedTitle,
                        messageCount = allMessages.size,
                    )
                    _state.update { cur ->
                        cur.copy(
                            sending = false,
                            streamingReply = "",
                            attachments = emptyList(),
                            messages = allMessages,
                            threads = listOf(summaryEntry) + cur.threads.filterNot { it.id == activeId },
                        )
                    }

                    // Persist exact web-compatible R2 thread (`t-...`) with clean user/assistant turns
                    val r2Messages = allMessages.map { m ->
                        AgentThreadMessageDto(
                            role = if (m.fromUser) "user" else "assistant",
                            text = if (m.fromUser && m.attachmentNames.isNotEmpty()) {
                                "${m.text}\n\n📎 ${m.attachmentNames.joinToString()}"
                            } else m.text,
                            ms = m.ms,
                        )
                    }
                    runCatching {
                        commerceRepository.putAgentThread(
                            id = activeId,
                            title = resolvedTitle,
                            messages = r2Messages,
                        )
                    }

                    // Immediately refresh live token quota counter
                    val updatedPolicy = (commerceRepository.agentPolicy() as? ApiResult.Success)?.data
                    if (updatedPolicy != null) {
                        _state.update { it.copy(policy = updatedPolicy) }
                    }
                }
            }
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
