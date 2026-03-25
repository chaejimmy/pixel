package com.shourov.apps.pacedream.feature.inbox.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.feature.inbox.data.InboxRepository
import com.shourov.apps.pacedream.feature.inbox.model.Message
import com.shourov.apps.pacedream.feature.inbox.model.MessageStatus
import com.shourov.apps.pacedream.feature.inbox.model.ThreadDetailEvent
import com.shourov.apps.pacedream.feature.inbox.model.ThreadDetailUiState
import com.shourov.apps.pacedream.feature.inbox.model.Thread
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Thread detail screen
 * 
 * Features:
 * - Load messages with pagination (before cursor)
 * - Send new messages
 * - Refetch after sending
 * - Pull-to-refresh
 */
@HiltViewModel
class ThreadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val inboxRepository: InboxRepository,
    private val authSession: AuthSession
) : ViewModel() {
    
    private val threadId: String = savedStateHandle.get<String>("threadId") ?: ""
    
    private val _uiState = MutableStateFlow<ThreadDetailUiState>(ThreadDetailUiState.Loading)
    val uiState: StateFlow<ThreadDetailUiState> = _uiState.asStateFlow()
    
    private var beforeCursor: String? = null
    
    init {
        if (threadId.isNotBlank()) {
            loadThreadDetails()
            loadMessages()
        } else {
            _uiState.value = ThreadDetailUiState.Error("Invalid thread ID")
        }
    }

    /**
     * Fetch thread metadata (opponent name, avatar, listing) so the
     * top-bar title shows the real name instead of "Unknown".
     */
    private fun loadThreadDetails() {
        viewModelScope.launch {
            val result = inboxRepository.getThread(threadId)
            if (result is ApiResult.Success) {
                val latestState = _uiState.value as? ThreadDetailUiState.Success
                if (latestState != null) {
                    _uiState.value = latestState.copy(thread = result.data)
                }
                // If messages haven't loaded yet, stash the thread for later
                cachedThread = result.data
            }
        }
    }

    private var cachedThread: com.shourov.apps.pacedream.feature.inbox.model.Thread? = null
    
    /**
     * Handle UI events
     */
    fun onEvent(event: ThreadDetailEvent) {
        when (event) {
            is ThreadDetailEvent.Refresh -> refresh()
            is ThreadDetailEvent.SendMessage -> sendMessage(event.text)
            is ThreadDetailEvent.RetryMessage -> retryMessage(event.tempId)
            is ThreadDetailEvent.DismissSendError -> dismissSendError()
            is ThreadDetailEvent.LoadMore -> loadMore()
        }
    }
    
    private fun loadMessages() {
        viewModelScope.launch {
            // Only show full loading state when we have no existing content
            if (_uiState.value !is ThreadDetailUiState.Success) {
                _uiState.value = ThreadDetailUiState.Loading
            }

            try {
                val result = inboxRepository.getMessages(
                    threadId = threadId,
                    limit = 50,
                    before = null
                )

                when (result) {
                    is ApiResult.Success -> {
                        beforeCursor = result.data.messages.lastOrNull()?.id
                        val existingState = _uiState.value as? ThreadDetailUiState.Success

                        // Get current user ID
                        val currentUserId = existingState?.currentUserId
                            ?: authSession.currentUser.value?.id ?: ""

                        // Reuse existing thread, cached details from getThread, or create minimal object
                        val thread = existingState?.thread ?: cachedThread ?: Thread(
                            id = threadId,
                            participants = emptyList(),
                            lastMessage = result.data.messages.firstOrNull(),
                            unreadCount = 0,
                            updatedAt = null,
                            listing = null,
                            opponent = null
                        )

                        _uiState.value = ThreadDetailUiState.Success(
                            thread = thread,
                            messages = result.data.messages,
                            currentUserId = currentUserId,
                            isRefreshing = false,
                            hasMore = result.data.hasMore,
                            isSending = false
                        )
                    }
                    is ApiResult.Failure -> {
                        Timber.e("ThreadViewModel: loadMessages FAILED — ${result.error.message}")
                        // On refresh failure, show error only if we have no existing content
                        val existing = _uiState.value as? ThreadDetailUiState.Success
                        if (existing != null) {
                            _uiState.value = existing.copy(isRefreshing = false)
                        } else {
                            _uiState.value = ThreadDetailUiState.Error(
                                result.error.message ?: "Failed to load messages"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Guarantee we never stay stuck in Loading state
                Timber.e(e, "ThreadViewModel: loadMessages EXCEPTION")
                val existing = _uiState.value as? ThreadDetailUiState.Success
                if (existing != null) {
                    _uiState.value = existing.copy(isRefreshing = false)
                } else {
                    _uiState.value = ThreadDetailUiState.Error(
                        e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }
    
    private fun refresh() {
        val currentState = _uiState.value as? ThreadDetailUiState.Success
        currentState?.let {
            _uiState.value = it.copy(isRefreshing = true)
        }
        
        beforeCursor = null
        loadMessages()
    }
    
    private fun loadMore() {
        val cursor = beforeCursor ?: return
        if (_uiState.value !is ThreadDetailUiState.Success) return

        viewModelScope.launch {
            val result = inboxRepository.getMessages(
                threadId = threadId,
                limit = 50,
                before = cursor
            )

            when (result) {
                is ApiResult.Success -> {
                    beforeCursor = result.data.messages.lastOrNull()?.id
                    // Use latest state to avoid overwriting concurrent mutations
                    val latestState = _uiState.value as? ThreadDetailUiState.Success ?: return@launch
                    _uiState.value = latestState.copy(
                        messages = latestState.messages + result.data.messages,
                        hasMore = result.data.hasMore
                    )
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to load more messages: ${result.error.message}")
                }
            }
        }
    }

    /**
     * Send message with optimistic insert and proper failure tracking.
     * Matches iOS behavior: temp message shown immediately, marked failed on error,
     * user can retry. Pre-checks content moderation locally before sending.
     */
    private fun sendMessage(text: String) {
        if (text.isBlank()) return

        val currentState = _uiState.value as? ThreadDetailUiState.Success ?: return
        val trimmedText = text.trim()

        // Local moderation pre-check (word-boundary matching, aligned with backend)
        val moderationResult = com.shourov.apps.pacedream.feature.inbox.data.ContentModerationCheck.check(trimmedText)
        if (moderationResult.status != com.shourov.apps.pacedream.feature.inbox.data.ContentModerationCheck.Status.ALLOW) {
            _uiState.value = currentState.copy(
                sendError = moderationResult.message
            )
            return
        }

        // Create optimistic temp message (iOS uses "temp-" prefix)
        val tempId = "temp-${UUID.randomUUID()}"
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val tempMessage = Message(
            id = tempId,
            text = trimmedText,
            senderId = currentState.currentUserId,
            timestamp = nowIso,
            status = MessageStatus.SENDING
        )

        // Optimistic insert: show message immediately with "Sending" status
        _uiState.value = currentState.copy(
            messages = listOf(tempMessage) + currentState.messages,
            isSending = true,
            sendError = null
        )

        viewModelScope.launch {
            val result = inboxRepository.sendMessage(
                threadId = threadId,
                text = trimmedText
            )

            val latestState = _uiState.value as? ThreadDetailUiState.Success ?: return@launch

            when (result) {
                is ApiResult.Success -> {
                    // Replace temp message with real server message
                    val serverMessage = result.data
                    _uiState.value = latestState.copy(
                        messages = latestState.messages.map { msg ->
                            if (msg.id == tempId) serverMessage else msg
                        },
                        isSending = false
                    )
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to send message: ${result.error.message}")
                    // Mark temp message as FAILED (visible to user)
                    _uiState.value = latestState.copy(
                        messages = latestState.messages.map { msg ->
                            if (msg.id == tempId) msg.copy(status = MessageStatus.FAILED) else msg
                        },
                        isSending = false,
                        sendError = result.error.message ?: "Failed to send message"
                    )
                }
            }
        }
    }

    /**
     * Retry sending a failed temp message. Matches iOS "Failed • Tap to retry" behavior.
     */
    private fun retryMessage(tempId: String) {
        val currentState = _uiState.value as? ThreadDetailUiState.Success ?: return
        val failedMessage = currentState.messages.find { it.id == tempId && it.isFailed } ?: return

        // Mark as sending again
        _uiState.value = currentState.copy(
            messages = currentState.messages.map { msg ->
                if (msg.id == tempId) msg.copy(status = MessageStatus.SENDING) else msg
            },
            isSending = true,
            sendError = null
        )

        viewModelScope.launch {
            val result = inboxRepository.sendMessage(
                threadId = threadId,
                text = failedMessage.text
            )

            val latestState = _uiState.value as? ThreadDetailUiState.Success ?: return@launch

            when (result) {
                is ApiResult.Success -> {
                    val serverMessage = result.data
                    _uiState.value = latestState.copy(
                        messages = latestState.messages.map { msg ->
                            if (msg.id == tempId) serverMessage else msg
                        },
                        isSending = false
                    )
                }
                is ApiResult.Failure -> {
                    Timber.e("Retry failed for message: ${result.error.message}")
                    _uiState.value = latestState.copy(
                        messages = latestState.messages.map { msg ->
                            if (msg.id == tempId) msg.copy(status = MessageStatus.FAILED) else msg
                        },
                        isSending = false,
                        sendError = result.error.message ?: "Failed to send message"
                    )
                }
            }
        }
    }

    private fun dismissSendError() {
        val currentState = _uiState.value as? ThreadDetailUiState.Success ?: return
        _uiState.value = currentState.copy(sendError = null)
    }
}


