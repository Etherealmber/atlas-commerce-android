package ma.etherealmber.atlascommerce.ui.screens.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.data.model.AssistNextActionItemDto
import ma.etherealmber.atlascommerce.data.model.AssistProposalDto
import ma.etherealmber.atlascommerce.data.model.AssistStatusDto
import ma.etherealmber.atlascommerce.data.model.AssistToolTraceDto
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val commerceRepository: CommerceRepository,
) : ViewModel() {

    data class ChatMessage(
        val id: String = UUID.randomUUID().toString(),
        val fromUser: Boolean,
        val text: String,
        val tools: List<AssistToolTraceDto> = emptyList(),
        val proposals: List<AssistProposalDto> = emptyList(),
        val approvedIds: Set<String> = emptySet(),
    )

    data class UiState(
        val status: AssistStatusDto? = null,
        val nextActions: List<AssistNextActionItemDto> = emptyList(),
        val messages: List<ChatMessage> = emptyList(),
        val input: String = "",
        val sending: Boolean = false,
        val approvingId: String? = null,
        val error: String? = null,
    )

    private var threadId = "android-${UUID.randomUUID().toString().take(8)}"

    fun clearConversation() {
        threadId = "android-${UUID.randomUUID().toString().take(8)}"
        _state.update { it.copy(messages = emptyList(), error = null, sending = false) }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _notices = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    init {
        refreshContext()
    }

    fun refreshContext() {
        viewModelScope.launch {
            val statusRes = commerceRepository.assistStatus()
            val nextRes = commerceRepository.assistNextActions()
            _state.update {
                it.copy(
                    status = (statusRes as? ApiResult.Success)?.data ?: it.status,
                    nextActions = (nextRes as? ApiResult.Success)?.data?.items ?: it.nextActions,
                )
            }
        }
    }

    fun onInputChange(value: String) {
        _state.update { it.copy(input = value, error = null) }
    }

    fun sendPrompt(prompt: String = _state.value.input) {
        val clean = prompt.trim()
        if (clean.isBlank() || _state.value.sending) return

        val userBubble = ChatMessage(fromUser = true, text = clean)
        _state.update {
            it.copy(
                input = if (prompt == it.input) "" else it.input,
                sending = true,
                error = null,
                messages = it.messages + userBubble,
            )
        }

        viewModelScope.launch {
            when (val res = commerceRepository.assistChat(clean, threadId = threadId)) {
                is ApiResult.Failure -> {
                    _state.update {
                        it.copy(sending = false, error = res.message)
                    }
                }

                is ApiResult.Success -> {
                    val replyBubble = ChatMessage(
                        fromUser = false,
                        text = res.data.reply.ifBlank { "Analyse terminée." },
                        tools = res.data.toolTrace,
                        proposals = res.data.proposals,
                    )
                    _state.update {
                        it.copy(
                            sending = false,
                            messages = it.messages + replyBubble,
                        )
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
}
