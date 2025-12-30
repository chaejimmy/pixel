package com.shourov.apps.pacedream.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.feature.inbox.data.InboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DashboardBadgesViewModel
 *
 * Drives unread badge counts shown on the bottom tabs (Inbox tab).
 * Non-blocking: failures do NOT clear the last known badge count.
 */
@HiltViewModel
class DashboardBadgesViewModel @Inject constructor(
    private val inboxRepository: InboxRepository,
    private val authSession: AuthSession
) : ViewModel() {

    private val _inboxUnread = MutableStateFlow(0)
    val inboxUnread: StateFlow<Int> = _inboxUnread.asStateFlow()

    init {
        // Keep badge in sync with auth state: when logged out, clear badge.
        viewModelScope.launch {
            authSession.authState.collectLatest { state ->
                if (state == AuthState.Unauthenticated) {
                    _inboxUnread.value = 0
                } else {
                    refreshInboxUnread()
                }
            }
        }
    }

    fun refreshInboxUnread() {
        viewModelScope.launch {
            if (authSession.authState.value == AuthState.Unauthenticated) {
                _inboxUnread.value = 0
                return@launch
            }

            when (val result = inboxRepository.getUnreadCounts()) {
                is ApiResult.Success -> _inboxUnread.value = result.data.totalUnread
                is ApiResult.Failure -> {
                    // Keep last known value (no silent fallback to fake data).
                }
            }
        }
    }
}

