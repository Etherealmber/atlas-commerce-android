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
import ma.etherealmber.atlascommerce.core.update.UpdateState
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

    /** Opens and pages the read-only work-status endpoint without retaining data across accounts. */
    fun loadWorkStatus(loadMore: Boolean = false) {
        val scope = (sessionManager.state.value as? SessionState.Active)?.user?.scopeIdentity ?: return
        val current = _state.value.mobileWorkStatus
        if (loadMore && current == null) return
        val inCursor = if (loadMore) current?.inApp?.nextCursor else null
        val github = if (loadMore) current?.trackers?.get("github") else null
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
                is AgentLocalAction.SetTheme -> {
                    val saved = try {
                        preferences.setThemeMode(action.mode)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        false
                    }
                    if (!saved) {
                        when (responseLanguage) {
                            AppLanguage.FRENCH -> "Je n’ai pas pu enregistrer ce thème. Vous pouvez réessayer dans Profil et personnalisation."
                            AppLanguage.ENGLISH -> "I couldn't save that theme. You can try again in Profile & Personalization."
                            AppLanguage.ARABIC -> "تعذّر حفظ هذا المظهر. يمكنك إعادة المحاولة من الملف الشخصي والتخصيص."
                        }
                    } else when (responseLanguage) {
                        AppLanguage.FRENCH -> when (action.mode) {
                            ThemeMode.SYSTEM -> "D’accord, le thème suit maintenant les réglages du système."
                            ThemeMode.LIGHT -> "C’est fait : le thème clair est activé."
                            ThemeMode.DARK -> "C’est fait : le thème sombre est activé."
                        }
                        AppLanguage.ENGLISH -> when (action.mode) {
                            ThemeMode.SYSTEM -> "Done. Atlas now follows your system theme."
                            ThemeMode.LIGHT -> "Done. The light theme is on."
                            ThemeMode.DARK -> "Done. The dark theme is on."
                        }
                        AppLanguage.ARABIC -> when (action.mode) {
                            ThemeMode.SYSTEM -> "تم. سيتبع Atlas الآن مظهر النظام."
                            ThemeMode.LIGHT -> "تم تفعيل المظهر الفاتح."
                            ThemeMode.DARK -> "تم تفعيل المظهر الداكن."
                        }
                    }
                }
                AgentLocalAction.CheckForUpdate -> {
                    appUpdateManager.checkForUpdate(manual = true)
                    val result = withTimeoutOrNull(20_000L) {
                        appUpdateManager.state.first { it !is UpdateState.Checking }
                    }
                    localUpdateReply(result, responseLanguage)
                }
            }
            val assistantMessage = ChatMessage(fromUser = false, text = reply)
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
                        threads = listOf(summary) + current.threads.filterNot { it.id == activeId },
                        sending = false,
                        streamingReply = "",
                        streamingTools = emptyList(),
                        error = null,
                    )
                }
            }
            val messagesToPersist = _state.value.messages.takeIf { _state.value.activeThreadId == activeId }
            if (messagesToPersist != null) {
                try {
                    commerceRepository.putAgentThread(
                        id = activeId,
                        title = resolvedTitle,
                        messages = messagesToPersist.map { message ->
                            AgentThreadMessageDto(
                                role = if (message.fromUser) "user" else "assistant",
                                text = if (message.fromUser && message.attachmentNames.isNotEmpty()) {
                                    "${message.text}\n\n📎 ${message.attachmentNames.joinToString()}"
                                } else message.text,
                                ms = message.ms,
                            )
                        },
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    AtlasLogBuffer.log("AGENT", "Could not persist the local action reply", "WARN")
                }
            }
        }
    }

    private fun localUpdateReply(state: UpdateState?, language: AppLanguage): String = when (state) {
        is UpdateState.Available -> when (language) {
            AppLanguage.FRENCH -> "Une mise à jour ${state.info.displayVersion} est disponible. Vous pouvez consulter les détails et lancer le téléchargement depuis la bannière ou Paramètres."
            AppLanguage.ENGLISH -> "Update ${state.info.displayVersion} is available. You can review it and start the download from the update banner or Settings."
            AppLanguage.ARABIC -> "يتوفر التحديث ${state.info.displayVersion}. يمكنك مراجعة التفاصيل وبدء التنزيل من إشعار التحديث أو الإعدادات."
        }
        is UpdateState.UpToDate -> when (language) {
            AppLanguage.FRENCH -> "Atlas est à jour."
            AppLanguage.ENGLISH -> "Atlas is up to date."
            AppLanguage.ARABIC -> "تطبيق Atlas محدّث."
        }
        is UpdateState.Downloading -> when (language) {
            AppLanguage.FRENCH -> "Le téléchargement de la mise à jour est en cours${state.percent?.let { " : $it %" }.orEmpty()}. Sa progression reste visible dans le menu latéral."
            AppLanguage.ENGLISH -> "The update is downloading${state.percent?.let { ": $it%" }.orEmpty()}. Its progress remains visible in the sidebar."
            AppLanguage.ARABIC -> "جارٍ تنزيل التحديث${state.percent?.let { " بنسبة $it٪" }.orEmpty()}. ستبقى نسبة التقدم ظاهرة في القائمة الجانبية."
        }
        is UpdateState.ReadyToInstall -> when (language) {
            AppLanguage.FRENCH -> "La mise à jour ${state.info.displayVersion} est téléchargée et prête à installer."
            AppLanguage.ENGLISH -> "Update ${state.info.displayVersion} is downloaded and ready to install."
            AppLanguage.ARABIC -> "تم تنزيل التحديث ${state.info.displayVersion} وأصبح جاهزاً للتثبيت."
        }
        is UpdateState.Error -> when (language) {
            AppLanguage.FRENCH -> "Je n’ai pas pu vérifier la mise à jour. Réessayez depuis Paramètres lorsque la connexion sera disponible."
            AppLanguage.ENGLISH -> "I couldn’t check for an update. Try again from Settings when you’re connected."
            AppLanguage.ARABIC -> "تعذّر التحقق من التحديث. أعد المحاولة من الإعدادات عند توفر الاتصال."
        }
        UpdateState.Idle, UpdateState.Checking, null -> when (language) {
            AppLanguage.FRENCH -> "La vérification prend plus de temps que prévu. Le statut apparaîtra dans le menu latéral dès qu’il sera disponible."
            AppLanguage.ENGLISH -> "The check is taking longer than expected. Its status will appear in the sidebar when available."
            AppLanguage.ARABIC -> "يستغرق التحقق وقتاً أطول من المتوقع. ستظهر حالته في القائمة الجانبية عند توفرها."
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
