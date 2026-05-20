package com.shourov.apps.pacedream.feature.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.notification.data.NotificationRepository
import com.shourov.apps.pacedream.feature.notification.model.AppNotification
import com.shourov.apps.pacedream.feature.notification.model.NotificationGroup
import com.shourov.apps.pacedream.feature.notification.model.NotificationUiState
import com.pacedream.common.util.UserFacingErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Notifications screen.
 * Matches iOS NotificationListViewModel: load, group by date, mark as read.
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun refresh() {
        val currentState = _uiState.value
        if (currentState is NotificationUiState.Success) {
            _uiState.value = currentState.copy(isRefreshing = true)
        }
        loadNotifications()
    }

    fun markAsRead(notificationId: String) {
        // Optimistic: flip the row to read locally so the UI animates bold → regular
        // without having to drop and re-render the list.
        updateNotification(notificationId) { it.copy(isRead = true) }
        viewModelScope.launch {
            repository.markAsRead(notificationId).onFailure { e ->
                Timber.e(e, "Failed to mark notification as read")
            }
        }
    }

    fun markAsUnread(notificationId: String) {
        // Repository has no markAsUnread endpoint yet; keep the change local so
        // the long-press affordance still feels responsive.
        updateNotification(notificationId) { it.copy(isRead = false) }
    }

    fun markAllAsRead() {
        // Optimistic single-pass update so the overflow action visibly zeroes
        // out every row, even if the network call is slow.
        _uiState.update { state ->
            if (state is NotificationUiState.Success) {
                state.copy(
                    groupedNotifications = state.groupedNotifications.mapValues { (_, list) ->
                        list.map { it.copy(isRead = true) }
                    }
                )
            } else state
        }
        viewModelScope.launch {
            repository.markAllAsRead().onFailure { e ->
                Timber.e(e, "Failed to mark all notifications as read")
            }
        }
    }

    private fun updateNotification(
        notificationId: String,
        transform: (AppNotification) -> AppNotification
    ) {
        _uiState.update { state ->
            if (state is NotificationUiState.Success) {
                state.copy(
                    groupedNotifications = state.groupedNotifications.mapValues { (_, list) ->
                        list.map { if (it.id == notificationId) transform(it) else it }
                    }
                )
            } else state
        }
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            repository.getNotifications().fold(
                onSuccess = { notifications ->
                    if (notifications.isEmpty()) {
                        _uiState.value = NotificationUiState.Empty
                    } else {
                        val grouped = notifications.groupBy { notification ->
                            NotificationGroup.forDate(notification.parsedDate)
                        }.toSortedMap(compareBy { it.ordinal })
                        _uiState.value = NotificationUiState.Success(
                            groupedNotifications = grouped,
                            isRefreshing = false
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load notifications")
                    _uiState.value = NotificationUiState.Error(
                        UserFacingErrorMapper.forLoadNotifications(error)
                    )
                }
            )
        }
    }
}
