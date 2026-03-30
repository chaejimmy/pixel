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

    private val _currentMode = MutableStateFlow(InboxMode.GUEST)
    val currentMode: StateFlow<InboxMode> = _currentMode.asStateFlow()

    private val _unreadCounts = MutableStateFlow(UnreadCounts(0, 0))
    val unreadCounts: StateFlow<UnreadCounts> = _unreadCounts.asStateFlow()

    private var nextCursor: String? = null
    
    init {
        observeAuthState()
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
    
    /**
     * Continuously observe auth state so the screen reacts immediately
     * when the user logs in (no app restart required).
     */
    private fun observeAuthState() {
        viewModelScope.launch {
            authSession.authState.collect { state ->
                Timber.d("InboxVM: authState changed → $state")
                when (state) {
                    AuthState.Authenticated -> {
                        val userId = authSession.currentUser.value?.id
                        Timber.d("InboxVM: authenticated — userId=${userId ?: "(null)"}, mode=${_currentMode.value.apiValue}")
                        nextCursor = null
                        loadThreadsAndCounts()
                    }
                    AuthState.Unauthenticated -> {
                        _uiState.value = InboxUiState.RequiresAuth
                    }
                    else -> { /* Unknown — wait for auth to settle */ }
                }
            }
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
        Timber.d("InboxViewModel: loadThreadsAndCounts START — mode=${_currentMode.value.apiValue}")
        try {
            // Load threads and unread counts in parallel
            val threadsDeferred = viewModelScope.async {
                inboxRepository.getThreads(
                    mode = _currentMode.value.apiValue,
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
            _unreadCounts.value = unreadCounts

            // Handle threads result
            when (threadsResult) {
                is ApiResult.Success -> {
                    // Guard: only store non-blank, non-"null" cursor (empty/null would cause 400)
                    nextCursor = threadsResult.data.nextCursor?.takeIf { it.isNotBlank() && it != "null" }
                    // Cannot paginate without a valid cursor
                    val hasMore = threadsResult.data.hasMore && nextCursor != null
                    Timber.d("InboxViewModel: loaded ${threadsResult.data.threads.size} threads, nextCursor=${nextCursor ?: "(none)"}, hasMore=$hasMore")

                    if (threadsResult.data.threads.isEmpty()) {
                        _uiState.value = InboxUiState.Empty
                    } else {
                        _uiState.value = InboxUiState.Success(
                            threads = threadsResult.data.threads,
                            mode = _currentMode.value,
                            unreadCounts = unreadCounts,
                            isRefreshing = false,
                            hasMore = hasMore
                        )
                    }
                }
                is ApiResult.Failure -> {
                    Timber.e("InboxViewModel: loadThreadsAndCounts FAILED — ${threadsResult.error.message}")
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
        } catch (e: Exception) {
            // Catch any unexpected exception to guarantee we leave Loading state
            Timber.e(e, "InboxViewModel: loadThreadsAndCounts EXCEPTION")
            _uiState.value = InboxUiState.Error(
                e.message ?: "An unexpected error occurred"
            )
        }
        Timber.d("InboxViewModel: loadThreadsAndCounts END — state=${_uiState.value::class.simpleName}")
    }
    
    private fun loadMore() {
        val cursor = nextCursor?.takeIf { it.isNotBlank() && it != "null" } ?: return
        if (_uiState.value !is InboxUiState.Success) return

        Timber.d("InboxViewModel: loadMore START — cursor=$cursor, mode=${_currentMode.value.apiValue}")

        viewModelScope.launch {
            try {
                val result = inboxRepository.getThreads(
                    mode = _currentMode.value.apiValue,
                    limit = 20,
                    cursor = cursor
                )

                when (result) {
                    is ApiResult.Success -> {
                        // Guard: only store non-blank, non-"null" cursor
                        nextCursor = result.data.nextCursor?.takeIf { it.isNotBlank() && it != "null" }
                        val hasMore = result.data.hasMore && nextCursor != null
                        Timber.d("InboxViewModel: loadMore loaded ${result.data.threads.size} more threads, nextCursor=${nextCursor ?: "(none)"}")
                        // Use latest state to avoid overwriting concurrent mutations
                        val latestState = _uiState.value as? InboxUiState.Success ?: return@launch
                        _uiState.value = latestState.copy(
                            threads = latestState.threads + result.data.threads,
                            hasMore = hasMore
                        )
                    }
                    is ApiResult.Failure -> {
                        Timber.e("InboxViewModel: loadMore FAILED — ${result.error.message}")
                        // Keep current state on pagination failure (don't break existing view)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "InboxViewModel: loadMore EXCEPTION")
            }
        }
    }
    
    private fun onModeChanged(mode: InboxMode) {
        if (mode == _currentMode.value) return

        _currentMode.value = mode
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
        // No-op: observeAuthState() now handles this automatically.
        // Kept for API compatibility with callers.
    }

    /**
     * Silently refresh threads in the background (e.g., when returning from a thread).
     * Does not show loading state -- only updates data on success.
     */
    fun refreshIfNeeded() {
        val current = _uiState.value
        if (current is InboxUiState.Success || current is InboxUiState.Empty) {
            viewModelScope.launch {
                nextCursor = null
                loadThreadsAndCounts()
            }
        }
    }

    /**
     * Mark thread as locally read when entering a thread (matches iOS markThreadReadLocally).
     * Optimistically reduces unread count in the UI without waiting for a server round-trip.
     * Also decrements the unread counts used by mode-toggle badges.
     */
    fun markThreadReadLocally(threadId: String) {
        val currentState = _uiState.value as? InboxUiState.Success ?: return
        var decremented = 0
        val updated = currentState.threads.map { thread ->
            if (thread.id == threadId && thread.hasUnread) {
                decremented = thread.unreadCount
                thread.copy(unreadCount = 0)
            } else {
                thread
            }
        }
        if (decremented > 0) {
            // Optimistically reduce the mode-specific unread counter
            val counts = _unreadCounts.value
            val newCounts = when (_currentMode.value) {
                InboxMode.GUEST -> counts.copy(guestUnread = (counts.guestUnread - decremented).coerceAtLeast(0))
                InboxMode.HOST -> counts.copy(hostUnread = (counts.hostUnread - decremented).coerceAtLeast(0))
            }
            _unreadCounts.value = newCounts
            _uiState.value = currentState.copy(threads = updated, unreadCounts = newCounts)
        } else {
            _uiState.value = currentState.copy(threads = updated)
        }
    }
}

/**
 * Navigation events from inbox
 */
sealed class InboxNavigation {
    data class ToThread(val threadId: String) : InboxNavigation()
}


