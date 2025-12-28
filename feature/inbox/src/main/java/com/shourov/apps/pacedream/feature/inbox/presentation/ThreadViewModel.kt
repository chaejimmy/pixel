package com.shourov.apps.pacedream.feature.inbox.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.feature.inbox.data.InboxRepository
import com.shourov.apps.pacedream.feature.inbox.model.Message
import com.shourov.apps.pacedream.feature.inbox.model.ThreadDetailEvent
import com.shourov.apps.pacedream.feature.inbox.model.ThreadDetailUiState
import com.shourov.apps.pacedream.feature.inbox.model.Thread
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
            loadMessages()
        } else {
            _uiState.value = ThreadDetailUiState.Error("Invalid thread ID")
        }
    }
    
    /**
     * Handle UI events
     */
    fun onEvent(event: ThreadDetailEvent) {
        when (event) {
            is ThreadDetailEvent.Refresh -> refresh()
            is ThreadDetailEvent.SendMessage -> sendMessage(event.text)
            is ThreadDetailEvent.LoadMore -> loadMore()
        }
    }
    
    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.value = ThreadDetailUiState.Loading
            
            val result = inboxRepository.getMessages(
                threadId = threadId,
                limit = 50,
                before = null
            )
            
            when (result) {
                is ApiResult.Success -> {
                    beforeCursor = result.data.messages.lastOrNull()?.id
                    
                    // Get current user ID
                    val currentUserId = authSession.currentUser.value?.id ?: ""
                    
                    // Create a minimal thread object
                    val thread = Thread(
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
                    _uiState.value = ThreadDetailUiState.Error(
                        result.error.message ?: "Failed to load messages"
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
        val currentState = _uiState.value as? ThreadDetailUiState.Success ?: return
        
        viewModelScope.launch {
            val result = inboxRepository.getMessages(
                threadId = threadId,
                limit = 50,
                before = cursor
            )
            
            when (result) {
                is ApiResult.Success -> {
                    beforeCursor = result.data.messages.lastOrNull()?.id
                    _uiState.value = currentState.copy(
                        messages = currentState.messages + result.data.messages,
                        hasMore = result.data.hasMore
                    )
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to load more messages: ${result.error.message}")
                }
            }
        }
    }
    
    private fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val currentState = _uiState.value as? ThreadDetailUiState.Success ?: return
        
        viewModelScope.launch {
            // Mark as sending
            _uiState.value = currentState.copy(isSending = true)
            
            val result = inboxRepository.sendMessage(
                threadId = threadId,
                text = text.trim()
            )
            
            when (result) {
                is ApiResult.Success -> {
                    // Add the new message optimistically to the top
                    val newMessage = result.data
                    _uiState.value = currentState.copy(
                        messages = listOf(newMessage) + currentState.messages,
                        isSending = false
                    )
                    
                    // Optionally refetch to ensure sync
                    // refresh()
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to send message: ${result.error.message}")
                    _uiState.value = currentState.copy(isSending = false)
                    // Could show a toast/snackbar here
                }
            }
        }
    }
}


