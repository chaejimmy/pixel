package com.shourov.apps.pacedream.feature.help.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.SessionManager
import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.config.AppConfig
import com.shourov.apps.pacedream.feature.help.HelpCenterAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * State container for [SupportChatScreen].
 *
 * Behavior parity with the web `SupportChat.tsx`:
 *  - Unauthenticated users supply a name + email before chatting.
 *  - Authenticated users start immediately; backend infers contact details.
 *  - Session is created lazily on the user's first send.
 *  - "Talk to a human" appears after 2+ user messages.
 *  - Composer is disabled once the session is resolved/closed.
 *
 * Realtime: we poll `/v1/support/chat/{sessionId}/messages` every 5s while
 * the screen is visible. If Ably is wired in later, the same merge logic
 * keeps the transcript consistent.
 */
@HiltViewModel
class SupportChatViewModel @Inject constructor(
    apiClient: ApiClient,
    appConfig: AppConfig,
    json: Json,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Constructed locally instead of injected by Hilt: KSP2 + Hilt 2.58 +
    // Kotlin 2.3 fails to resolve the `SupportChatRepository` symbol when
    // it's requested as an `@Inject constructor` parameter or as a
    // `@Provides` return type, aborting `:app:kspProdReleaseKotlin`.
    private val repository: SupportChatRepository =
        SupportChatRepository(apiClient, appConfig, json)

    /** Backend category seeded from the entry point. Defaults to "general". */
    private val initialCategory: SupportCategory =
        SupportCategory.fromKey(savedStateHandle.get<String>(ARG_CATEGORY))

    /** Optional seeded message from a contextual entry point. */
    private val initialMessage: String? =
        savedStateHandle.get<String>(ARG_INITIAL_MESSAGE)?.takeIf { it.isNotBlank() }

    /** Where this chat was opened from — for analytics. */
    private val entrySource: String =
        savedStateHandle.get<String>(ARG_ENTRY_SOURCE) ?: "unknown"

    private val _uiState = MutableStateFlow(
        SupportChatUiState(
            category = initialCategory,
            requiresGuestForm = !sessionManager.isAuthenticated,
            guestName = sessionManager.currentUser.value?.let { user ->
                listOfNotNull(user.firstName, user.lastName).joinToString(" ").trim()
            }.orEmpty(),
            guestEmail = sessionManager.currentUser.value?.email.orEmpty(),
        )
    )
    val uiState: StateFlow<SupportChatUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        HelpCenterAnalytics.log(HelpCenterAnalytics.Event.ContextualTapped(entrySource))
        // Reuse the existing logger to emit the canonical event names.
        Timber.d("support_chat_opened source=$entrySource category=${initialCategory.key}")

        // If the user is logged in AND we got a seeded message from a contextual
        // entry point, start the session immediately so the user lands directly
        // in a focused conversation.
        if (sessionManager.isAuthenticated && initialMessage != null) {
            viewModelScope.launch { startSessionWithSeedMessage() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }

    // ------------------------------------------------------------------------
    // User actions
    // ------------------------------------------------------------------------

    fun onComposerTextChanged(text: String) {
        _uiState.update { it.copy(composerText = text, inlineError = null) }
    }

    fun onGuestNameChanged(text: String) {
        _uiState.update { it.copy(guestName = text, inlineError = null) }
    }

    fun onGuestEmailChanged(text: String) {
        _uiState.update { it.copy(guestEmail = text, inlineError = null) }
    }

    fun send() {
        val state = _uiState.value
        val trimmed = state.composerText.trim()
        if (trimmed.isEmpty() || state.isSending) return

        validateGuestForm()?.let { err ->
            _uiState.update { it.copy(inlineError = err) }
            return
        }

        // Optimistic local message.
        val optimisticId = "local-" + UUID.randomUUID().toString()
        val optimistic = SupportMessage(
            id = optimisticId,
            sender = SupportSender.User,
            content = trimmed,
            createdAtEpochMs = System.currentTimeMillis(),
            isPending = true,
        )
        _uiState.update {
            it.copy(
                messages = it.messages + optimistic,
                composerText = "",
                isSending = true,
                inlineError = null,
            )
        }

        viewModelScope.launch {
            val sessionId = state.session?.sessionId
            if (sessionId.isNullOrBlank()) {
                doStart(trimmed, optimisticId)
            } else {
                doSend(sessionId, trimmed, optimisticId)
            }
        }
    }

    fun retry(messageId: String) {
        val state = _uiState.value
        val failed = state.messages.firstOrNull { it.id == messageId && it.isFailed } ?: return
        // Remove the failed entry and re-queue it through send().
        _uiState.update {
            it.copy(
                messages = it.messages.filterNot { m -> m.id == messageId },
                composerText = failed.content,
            )
        }
        send()
    }

    fun escalate() {
        val sessionId = _uiState.value.session?.sessionId ?: return
        if (_uiState.value.hasEscalated) return
        viewModelScope.launch {
            repository.escalate(sessionId).fold(
                onSuccess = {
                    Timber.d("support_chat_escalated sessionId=$sessionId")
                    _uiState.update { it.copy(hasEscalated = true) }
                    refreshSilently()
                },
                onFailure = {
                    _uiState.update {
                        it.copy(inlineError = "Couldn't request a human agent right now. Please try again in a moment.")
                    }
                }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshSilently() }
    }

    // ------------------------------------------------------------------------
    // Private
    // ------------------------------------------------------------------------

    private fun validateGuestForm(): String? {
        val s = _uiState.value
        if (!s.requiresGuestForm) return null
        if (s.guestName.isBlank()) return "Please enter your name."
        val email = s.guestEmail.trim()
        if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
            return "Please enter a valid email address."
        }
        return null
    }

    private suspend fun startSessionWithSeedMessage() {
        validateGuestForm()?.let { err ->
            _uiState.update { it.copy(inlineError = err) }
            return
        }

        _uiState.update { it.copy(loadState = SupportChatUiState.LoadState.Starting) }

        repository.startSession(
            category = initialCategory,
            message = initialMessage,
            guestName = _uiState.value.guestName.takeIf { it.isNotBlank() },
            guestEmail = _uiState.value.guestEmail.takeIf { it.isNotBlank() },
        ).fold(
            onSuccess = { result ->
                _uiState.update {
                    it.copy(
                        session = result.session,
                        messages = result.messages,
                        loadState = SupportChatUiState.LoadState.Ready,
                    )
                }
                Timber.d("support_chat_session_started category=${initialCategory.key}")
                startPolling()
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(loadState = SupportChatUiState.LoadState.Error(err.userMessage()))
                }
            }
        )
    }

    private suspend fun doStart(trimmed: String, optimisticId: String) {
        repository.startSession(
            category = initialCategory,
            message = trimmed,
            guestName = _uiState.value.guestName.takeIf { it.isNotBlank() },
            guestEmail = _uiState.value.guestEmail.takeIf { it.isNotBlank() },
        ).fold(
            onSuccess = { result ->
                _uiState.update { state ->
                    val merged = mergeAfterStart(
                        existing = state.messages,
                        optimisticId = optimisticId,
                        serverMessages = result.messages,
                        userContent = trimmed,
                    )
                    state.copy(
                        session = result.session,
                        messages = merged,
                        isSending = false,
                        loadState = SupportChatUiState.LoadState.Ready,
                    )
                }
                Timber.d("support_chat_message_sent sessionId=${result.session.sessionId} length=${trimmed.length}")
                startPolling()
            },
            onFailure = { err -> handleSendFailure(optimisticId, err) }
        )
    }

    private suspend fun doSend(sessionId: String, trimmed: String, optimisticId: String) {
        repository.sendMessage(
            sessionId = sessionId,
            content = trimmed,
            guestName = _uiState.value.guestName.takeIf { it.isNotBlank() },
            guestEmail = _uiState.value.guestEmail.takeIf { it.isNotBlank() },
        ).fold(
            onSuccess = { result ->
                _uiState.update { state ->
                    val updatedMessages = if (result.message != null) {
                        state.messages.map { m -> if (m.id == optimisticId) result.message else m }
                    } else {
                        state.messages.map { m -> if (m.id == optimisticId) m.copy(isPending = false) else m }
                    }
                    state.copy(
                        session = result.session ?: state.session,
                        messages = updatedMessages,
                        isSending = false,
                    )
                }
                Timber.d("support_chat_message_sent sessionId=$sessionId length=${trimmed.length}")
            },
            onFailure = { err -> handleSendFailure(optimisticId, err) }
        )
    }

    private fun handleSendFailure(optimisticId: String, err: Throwable) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { m ->
                    if (m.id == optimisticId) m.copy(isPending = false, isFailed = true) else m
                },
                isSending = false,
                inlineError = err.userMessage(),
            )
        }
        Timber.d("support_chat_send_failed reason=${err.javaClass.simpleName}")
    }

    private fun mergeAfterStart(
        existing: List<SupportMessage>,
        optimisticId: String,
        serverMessages: List<SupportMessage>,
        userContent: String,
    ): List<SupportMessage> {
        val trimmedContent = userContent.trim()
        val serverHasUser = serverMessages.any {
            it.sender == SupportSender.User && it.content.trim() == trimmedContent
        }
        return if (serverHasUser) {
            serverMessages
        } else {
            // Keep the optimistic copy (marked as confirmed) and append any
            // non-user messages from the server (AI greeting, system notes).
            val confirmedOptimistic = existing.map { m ->
                if (m.id == optimisticId) m.copy(isPending = false) else m
            }
            confirmedOptimistic + serverMessages.filter { it.sender != SupportSender.User }
        }
    }

    private fun startPolling() {
        val sessionId = _uiState.value.session?.sessionId ?: return
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                val currentSid = _uiState.value.session?.sessionId
                if (currentSid != sessionId) return@launch
                refreshSilently()
            }
        }
    }

    private suspend fun refreshSilently() {
        val sessionId = _uiState.value.session?.sessionId ?: return
        repository.fetchMessages(sessionId).onSuccess { result ->
            _uiState.update { state ->
                state.copy(
                    messages = mergeIncoming(state.messages, result.messages),
                    session = result.session ?: state.session,
                )
            }
        }
        // Polling failures are silent on purpose.
    }

    private fun mergeIncoming(existing: List<SupportMessage>, incoming: List<SupportMessage>): List<SupportMessage> {
        val existingIds = existing.filterNot { it.id.startsWith("local-") }.map { it.id }.toHashSet()
        val merged = existing.filterNot { it.isFailed }.toMutableList()
        for (m in incoming) if (m.id !in existingIds) merged += m
        return merged.sortedBy { it.createdAtEpochMs }
    }

    companion object {
        const val ARG_CATEGORY = "category"
        const val ARG_INITIAL_MESSAGE = "initialMessage"
        // Must match the nav-graph arg name used in DashboardNavigation.kt.
        const val ARG_ENTRY_SOURCE = "source"
        private const val POLL_INTERVAL_MS = 5_000L
    }
}

// ============================================================================
// UI state
// ============================================================================

data class SupportChatUiState(
    val category: SupportCategory = SupportCategory.General,
    val loadState: LoadState = LoadState.Idle,
    val session: SupportSession? = null,
    val messages: List<SupportMessage> = emptyList(),
    val composerText: String = "",
    val isSending: Boolean = false,
    val hasEscalated: Boolean = false,
    val inlineError: String? = null,
    val requiresGuestForm: Boolean = false,
    val guestName: String = "",
    val guestEmail: String = "",
) {
    sealed interface LoadState {
        data object Idle : LoadState
        data object Starting : LoadState
        data object Ready : LoadState
        data class Error(val message: String) : LoadState
    }

    /** Web rule: enable after 2+ user messages, hide once already requested. */
    val canEscalate: Boolean
        get() {
            if (hasEscalated) return false
            if (session == null) return false
            return messages.count { it.sender == SupportSender.User } >= 2
        }

    val composerEnabled: Boolean
        get() = session?.status?.isComposerEnabled ?: true
}

// ============================================================================
// Helpers
// ============================================================================

private fun Throwable.userMessage(): String =
    (this as? SupportChatException)?.message ?: "Something went wrong. Please try again."
