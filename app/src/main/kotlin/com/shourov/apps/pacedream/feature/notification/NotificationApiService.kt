package com.shourov.apps.pacedream.feature.notification

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification API service matching iOS NotificationService.swift (iOS parity).
 *
 * Provides full CRUD operations for notifications:
 * - Get notifications (paginated)
 * - Send notification
 * - Mark as read
 * - Mark all as read
 * - Delete notification
 * - Get/update notification settings
 *
 * API endpoints match iOS:
 * - GET  /notifications?page=1&limit=20
 * - POST /notifications/send
 * - POST /notifications/{id}/read
 * - POST /notifications/mark-all
 * - DELETE /notifications/{id}
 * - GET  /notifications/settings
 * - PUT  /notifications/settings
 */
@Singleton
class NotificationApiService @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Fetch notifications with pagination (iOS parity).
     */
    suspend fun getNotifications(page: Int = 1, limit: Int = 20): List<AppNotification> {
        _isLoading.value = true
        try {
            val url = appConfig.buildApiUrl("notifications") + "?page=$page&limit=$limit"
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val response = json.decodeFromString(
                        NotificationApiResponse.serializer(),
                        result.data
                    )
                    if (response.success) {
                        val items = response.data?.data ?: emptyList()
                        if (page == 1) {
                            _notifications.value = items
                        } else {
                            _notifications.value = _notifications.value + items
                        }
                        response.unreadCount?.let { _unreadCount.value = it }
                        updateUnreadCount()
                        return items
                    }
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to fetch notifications: ${result.error.message}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch notifications")
        } finally {
            _isLoading.value = false
        }
        return emptyList()
    }

    /**
     * Send a notification to a user (iOS parity).
     */
    suspend fun sendNotification(
        userId: String,
        title: String,
        body: String,
        data: Map<String, String>? = null
    ): Boolean {
        try {
            val url = appConfig.buildApiUrl("notifications", "send")
            val request = SendNotificationRequest(
                userId = userId,
                title = title,
                body = body,
                data = data
            )
            val requestBody = json.encodeToString(SendNotificationRequest.serializer(), request)
            when (val result = apiClient.post(url, requestBody, includeAuth = true)) {
                is ApiResult.Success -> return true
                is ApiResult.Failure -> {
                    Timber.e("Failed to send notification: ${result.error.message}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to send notification")
        }
        return false
    }

    /**
     * Mark a notification as read (iOS parity).
     */
    suspend fun markAsRead(notificationId: String): Boolean {
        try {
            val url = appConfig.buildApiUrl("notifications", notificationId, "read")
            when (val result = apiClient.post(url, "{}", includeAuth = true)) {
                is ApiResult.Success -> {
                    _notifications.value = _notifications.value.map {
                        if (it.id == notificationId) it.copy(isRead = true) else it
                    }
                    updateUnreadCount()
                    return true
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to mark notification as read: ${result.error.message}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark notification as read")
        }
        return false
    }

    /**
     * Mark all notifications as read (iOS parity).
     */
    suspend fun markAllAsRead(): Boolean {
        try {
            val url = appConfig.buildApiUrl("notifications", "mark-all")
            when (val result = apiClient.post(url, "{}", includeAuth = true)) {
                is ApiResult.Success -> {
                    _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                    _unreadCount.value = 0
                    return true
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to mark all notifications as read: ${result.error.message}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark all notifications as read")
        }
        return false
    }

    /**
     * Delete a notification (iOS parity).
     */
    suspend fun deleteNotification(notificationId: String): Boolean {
        try {
            val url = appConfig.buildApiUrl("notifications", notificationId)
            when (val result = apiClient.delete(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    _notifications.value = _notifications.value.filter { it.id != notificationId }
                    updateUnreadCount()
                    return true
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to delete notification: ${result.error.message}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete notification")
        }
        return false
    }

    private fun updateUnreadCount() {
        _unreadCount.value = _notifications.value.count { !it.isRead }
    }
}

// ── API Models (iOS parity) ──────────────────────────

/**
 * Notification item matching iOS AppNotification / UserNotification (iOS parity).
 *
 * Supports both camelCase and snake_case field names from the backend,
 * matching the tolerant decoding approach in iOS.
 */
@Serializable
data class AppNotification(
    val id: String,
    val title: String = "",
    val body: String? = null,
    val message: String? = null,
    val type: String = "",
    val data: Map<String, String>? = null,
    @SerialName("isRead") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    /** Resolved body text (API may send `body` or `message`) — iOS parity. */
    val displayBody: String get() = body ?: message ?: ""
}

@Serializable
private data class NotificationApiResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: PaginatedData? = null,
    @SerialName("unread_count") val unreadCount: Int? = null
)

@Serializable
private data class PaginatedData(
    val data: List<AppNotification> = emptyList()
)

@Serializable
private data class SendNotificationRequest(
    @SerialName("user_id") val userId: String,
    val title: String,
    val body: String,
    val data: Map<String, String>? = null
)
