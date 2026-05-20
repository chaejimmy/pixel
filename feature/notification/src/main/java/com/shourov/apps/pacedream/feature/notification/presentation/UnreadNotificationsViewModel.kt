package com.shourov.apps.pacedream.feature.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.notification.data.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Drives the unread-count badge on the notification bell.
 *
 * The underlying [NotificationRepository] is a process singleton, so every
 * call to `hiltViewModel<UnreadNotificationsViewModel>()` across screens
 * observes the same cached notification list and the same set of locally
 * "seen" IDs — the badge stays consistent across Home / Bookings / ChatList /
 * Dashboard / HostDashboard without each screen re-fetching.
 */
@HiltViewModel
class UnreadNotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    val unreadCount: StateFlow<Int> = repository.unreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            repository.getNotifications()
                .onFailure { Timber.e(it, "Unread badge refresh failed") }
        }
    }

    /**
     * Drops the badge to zero without marking individual rows as read.
     * Called when the user taps the bell to enter the notifications screen.
     */
    fun markAllAsSeen() {
        repository.markAllAsSeen()
    }
}
