package com.shourov.apps.pacedream.feature.inbox.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.feature.inbox.data.InboxRepository
import com.shourov.apps.pacedream.feature.inbox.model.InboxEvent
import com.shourov.apps.pacedream.feature.inbox.model.InboxMode
import com.shourov.apps.pacedream.feature.inbox.model.InboxUiState
import com.shourov.apps.pacedream.feature.inbox.model.UnreadCounts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Inbox screen
 * 
 * Features:
 * - Load threads with pagination
 * - Guest/Host mode toggle
 * - Unread counts badge
 * - Pull-to-refresh
 * - Tolerant parsing from repository
 */
@HiltViewModel
class InboxViewModel @Inject constructor(
    private val inboxRepository: InboxRepository,
    private val authSession: AuthSession
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<InboxUiState>(InboxUiState.Loading)
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()
    
    private val _navigation = Channel<InboxNavigation>(Channel.BUFFERED)
    val navigation = _navigation.receiveAsFlow()
    
    private var currentMode = InboxMode.GUEST
    private var nextCursor: String? = null
    
    init {
        checkAuthAndLoad()
    }
    
    /**
     * Handle UI events
     */
    fun onEvent(event: InboxEvent) {
        when (event) {
            is InboxEvent.Refresh -> refresh()
            is InboxEvent.ModeChanged -> onModeChanged(event.mode)
            is InboxEvent.ThreadClicked -> onThreadClicked(event.thread)
            is InboxEvent.ArchiveThread -> onArchiveThread(event.thread)
            is InboxEvent.LoadMore -> loadMore()
        }
    }
    
    private fun checkAuthAndLoad() {
        viewModelScope.launch {
            if (authSession.authState.value == AuthState.Unauthenticated) {
                _uiState.value = InboxUiState.RequiresAuth
                return@launch
            }
            
            loadThreadsAndCounts()
        }
    }
    
    private fun refresh() {
        viewModelScope.launch {
            if (authSession.authState.value == AuthState.Unauthenticated) {
                _uiState.value = InboxUiState.RequiresAuth
                return@launch
            }
            
            // Mark as refreshing
            (_uiState.value as? InboxUiState.Success)?.let { current ->
                _uiState.value = current.copy(isRefreshing = true)
            }
            
            nextCursor = null
            loadThreadsAndCounts()
        }
    }
    
    private suspend fun loadThreadsAndCounts() {
        // Load threads and unread counts in parallel
        val threadsDeferred = viewModelScope.async {
            inboxRepository.getThreads(
                mode = currentMode.apiValue,
                limit = 20,
                cursor = null
            )
        }
        
        val unreadDeferred = viewModelScope.async {
            inboxRepository.getUnreadCounts()
        }
        
        val threadsResult = threadsDeferred.await()
        val unreadResult = unreadDeferred.await()
        
        // Get unread counts (default to zero on error)
        val unreadCounts = when (unreadResult) {
            is ApiResult.Success -> unreadResult.data
            is ApiResult.Failure -> UnreadCounts(0, 0)
        }
        
        // Handle threads result
        when (threadsResult) {
            is ApiResult.Success -> {
                nextCursor = threadsResult.data.nextCursor
                
                if (threadsResult.data.threads.isEmpty()) {
                    _uiState.value = InboxUiState.Empty
                } else {
                    _uiState.value = InboxUiState.Success(
                        threads = threadsResult.data.threads,
                        mode = currentMode,
                        unreadCounts = unreadCounts,
                        isRefreshing = false,
                        hasMore = threadsResult.data.hasMore
                    )
                }
            }
            is ApiResult.Failure -> {
                when (threadsResult.error) {
                    is ApiError.Unauthorized -> {
                        _uiState.value = InboxUiState.RequiresAuth
                    }
                    else -> {
                        _uiState.value = InboxUiState.Error(
                            threadsResult.error.message ?: "Failed to load messages"
                        )
                    }
                }
            }
        }
    }
    
    private fun loadMore() {
        val cursor = nextCursor ?: return
        val currentState = _uiState.value as? InboxUiState.Success ?: return
        
        viewModelScope.launch {
            val result = inboxRepository.getThreads(
                mode = currentMode.apiValue,
                limit = 20,
                cursor = cursor
            )
            
            when (result) {
                is ApiResult.Success -> {
                    nextCursor = result.data.nextCursor
                    _uiState.value = currentState.copy(
                        threads = currentState.threads + result.data.threads,
                        hasMore = result.data.hasMore
                    )
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to load more threads: ${result.error.message}")
                    // Keep current state, just log error
                }
            }
        }
    }
    
    private fun onModeChanged(mode: InboxMode) {
        if (mode == currentMode) return
        
        currentMode = mode
        nextCursor = null
        _uiState.value = InboxUiState.Loading
        
        viewModelScope.launch {
            loadThreadsAndCounts()
        }
    }
    
    private fun onThreadClicked(thread: com.shourov.apps.pacedream.feature.inbox.model.Thread) {
        viewModelScope.launch {
            _navigation.send(InboxNavigation.ToThread(thread.id))
        }
    }
    
    private fun onArchiveThread(thread: com.shourov.apps.pacedream.feature.inbox.model.Thread) {
        viewModelScope.launch {
            // Optimistically remove from list
            val currentState = _uiState.value as? InboxUiState.Success ?: return@launch
            val updatedThreads = currentState.threads.filter { it.id != thread.id }
            _uiState.value = currentState.copy(threads = updatedThreads)
            
            // Call API
            val result = inboxRepository.archiveThread(thread.id)
            
            if (result is ApiResult.Failure) {
                // Restore thread on failure
                _uiState.value = currentState
                Timber.e("Failed to archive thread: ${result.error.message}")
            }
        }
    }
    
    /**
     * Called when auth is completed
     */
    fun onAuthCompleted() {
        checkAuthAndLoad()
    }
}

/**
 * Navigation events from inbox
 */
sealed class InboxNavigation {
    data class ToThread(val threadId: String) : InboxNavigation()
}


