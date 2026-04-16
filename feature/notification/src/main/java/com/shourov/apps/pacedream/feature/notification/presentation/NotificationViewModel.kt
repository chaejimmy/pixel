package com.shourov.apps.pacedream.feature.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.notification.data.NotificationRepository
import com.shourov.apps.pacedream.feature.notification.model.NotificationGroup
import com.shourov.apps.pacedream.feature.notification.model.NotificationUiState
import com.pacedream.common.util.UserFacingErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        viewModelScope.launch {
            repository.markAsRead(notificationId).onSuccess {
                // Reload to reflect read state
                loadNotifications()
            }.onFailure { e ->
                Timber.e(e, "Failed to mark notification as read")
            }
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
