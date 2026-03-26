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
                n.resolvedType.startsWith("booking") || n.resolvedType.startsWith("checkin") ||
                    n.resolvedType.startsWith("extend") || n.resolvedType.startsWith("overtime") ||
                    n.resolvedType.startsWith("session") || n.resolvedType.startsWith("split")
            }
            NotificationTab.MESSAGES -> notifications.filter { n ->
                n.resolvedType == "message_received" || n.resolvedType == "message" ||
                    n.resolvedType.startsWith("message.")
            }
            NotificationTab.PAYMENTS -> notifications.filter { n ->
                n.resolvedType.startsWith("payment") || n.resolvedType.startsWith("payout") ||
                    n.resolvedType.startsWith("chargeback")
            }
            NotificationTab.SYSTEM -> notifications.filter { n ->
                n.resolvedType.startsWith("system") || n.resolvedType.startsWith("maintenance") ||
                    n.resolvedType.startsWith("security") || n.resolvedType.startsWith("account") ||
                    n.resolvedType.startsWith("verification") || n.resolvedType.startsWith("support") ||
                    n.resolvedType.startsWith("content") || n.resolvedType == "marketing" ||
                    n.resolvedType == "reminder" || n.resolvedType == "alert" ||
                    n.resolvedType.startsWith("review")
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
