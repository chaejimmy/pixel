package com.shourov.apps.pacedream.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Notification tab filter matching iOS NotificationView tabs (iOS parity).
 *
 * iOS has 5 tabs: All, Bookings, Messages, Payments, System
 */
enum class NotificationTab(val displayName: String) {
    ALL("All"),
    BOOKINGS("Bookings"),
    MESSAGES("Messages"),
    PAYMENTS("Payments"),
    SYSTEM("System")
}

data class NotificationCenterUiState(
    val isLoading: Boolean = false,
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val selectedTab: NotificationTab = NotificationTab.ALL,
    val errorMessage: String? = null
) {
    /**
     * Filtered notifications based on the selected tab (iOS parity).
     */
    val filteredNotifications: List<AppNotification>
        get() = when (selectedTab) {
            NotificationTab.ALL -> notifications
            NotificationTab.BOOKINGS -> notifications.filter { n ->
                n.type.startsWith("booking") || n.type.startsWith("checkin") ||
                    n.type.startsWith("extend") || n.type.startsWith("overtime") ||
                    n.type.startsWith("session") || n.type.startsWith("split")
            }
            NotificationTab.MESSAGES -> notifications.filter { n ->
                n.type == "message_received" || n.type == "message"
            }
            NotificationTab.PAYMENTS -> notifications.filter { n ->
                n.type.startsWith("payment") || n.type.startsWith("payout") ||
                    n.type.startsWith("chargeback")
            }
            NotificationTab.SYSTEM -> notifications.filter { n ->
                n.type.startsWith("system") || n.type.startsWith("maintenance") ||
                    n.type.startsWith("security") || n.type.startsWith("account") ||
                    n.type.startsWith("verification") || n.type.startsWith("support") ||
                    n.type.startsWith("content") || n.type == "marketing" ||
                    n.type == "reminder" || n.type == "alert"
            }
        }

    /**
     * Group notifications by date (iOS parity: Today, This Week, This Month, Earlier).
     */
    val groupedNotifications: Map<String, List<AppNotification>>
        get() = filteredNotifications.groupBy { notification ->
            // Simple grouping by date prefix
            when {
                notification.createdAt.isEmpty() -> "Earlier"
                else -> "Recent"
            }
        }
}

/**
 * ViewModel for the notification center screen matching iOS NotificationView (iOS parity).
 */
@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    private val notificationApiService: NotificationApiService
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(NotificationTab.ALL)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NotificationCenterUiState> = combine(
        notificationApiService.notifications,
        notificationApiService.unreadCount,
        notificationApiService.isLoading,
        _selectedTab,
        _errorMessage
    ) { notifications, unreadCount, isLoading, selectedTab, error ->
        NotificationCenterUiState(
            isLoading = isLoading,
            notifications = notifications,
            unreadCount = unreadCount,
            selectedTab = selectedTab,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationCenterUiState(isLoading = true)
    )

    private var currentPage = 1

    init {
        loadNotifications()
    }

    fun selectTab(tab: NotificationTab) {
        _selectedTab.value = tab
    }

    fun loadNotifications() {
        currentPage = 1
        viewModelScope.launch {
            _errorMessage.value = null
            notificationApiService.getNotifications(page = 1)
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            currentPage++
            notificationApiService.getNotifications(page = currentPage)
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationApiService.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationApiService.markAllAsRead()
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationApiService.deleteNotification(notificationId)
        }
    }

    fun refresh() {
        loadNotifications()
    }
}
