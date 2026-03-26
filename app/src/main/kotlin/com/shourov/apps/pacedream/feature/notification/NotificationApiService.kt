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
    suspend fun getNotifications(page: Int = 1, pageSize: Int = 20): List<AppNotification> {
        _isLoading.value = true
        try {
            val url = appConfig.buildApiUrlWithQuery("notifications", queryParams = mapOf("page" to page.toString(), "pageSize" to pageSize.toString()))
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val response = json.decodeFromString(
                        NotificationApiResponse.serializer(),
                        result.data
                    )
                    if (response.isSuccessful) {
                        val items = response.data?.notifications ?: emptyList()
                        if (page == 1) {
                            _notifications.value = items
                        } else {
                            _notifications.value = _notifications.value + items
                        }
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

    /**
     * Fetch notification settings (iOS parity: GET /notifications/settings).
     */
    suspend fun getNotificationSettings(): NotificationSettings? {
        try {
            val url = appConfig.buildApiUrl("notifications", "settings")
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val response = json.decodeFromString(
                        NotificationSettingsResponse.serializer(),
                        result.data
                    )
                    return response.data
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to fetch notification settings: ${result.error.message}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch notification settings")
        }
        return null
    }

    /**
     * Update notification settings (iOS parity: PUT /notifications/settings).
     */
    suspend fun updateNotificationSettings(settings: NotificationSettings): NotificationSettings? {
        try {
            val url = appConfig.buildApiUrl("notifications", "settings")
            val body = json.encodeToString(NotificationSettings.serializer(), settings)
            when (val result = apiClient.put(url, body, includeAuth = true)) {
                is ApiResult.Success -> {
                    val response = json.decodeFromString(
                        NotificationSettingsResponse.serializer(),
                        result.data
                    )
                    return response.data
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to update notification settings: ${result.error.message}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update notification settings")
        }
        return null
    }

    private fun updateUnreadCount() {
        _unreadCount.value = _notifications.value.count { !it.isRead }
    }
}

// ── API Models (iOS parity) ──────────────────────────

/**
 * Notification item matching iOS AppNotification / UserNotification (iOS parity).
 *
 * Backend NotificationV2 model uses `body` (not `message`) and `readAt` (not `isRead`).
 * Supports both field naming conventions for tolerance.
 */
@Serializable
data class AppNotification(
    @SerialName("_id") val id: String = "",
    val title: String = "",
    val body: String? = null,
    val message: String? = null,
    val event: String = "",
    val type: String = "",
    @SerialName("readAt") val readAt: String? = null,
    @SerialName("deepLink") val deepLink: String? = null,
    val status: String = "",
    val data: Map<String, String>? = null,
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("updatedAt") val updatedAt: String = ""
) {
    /** Resolved body text (API may send `body` or `message`) — iOS parity. */
    val displayBody: String get() = body ?: message ?: ""

    /** Read state derived from readAt timestamp (matches backend NotificationV2 schema). */
    val isRead: Boolean get() = readAt != null

    /** Resolved type (backend uses `event` field, fall back to `type`). */
    val resolvedType: String get() = event.ifEmpty { type }

    fun copy(isRead: Boolean): AppNotification {
        return copy(readAt = if (isRead) (readAt ?: "marked") else null)
    }
}

@Serializable
private data class NotificationApiResponse(
    val success: Boolean = false,
    val status: Boolean = false,
    val message: String? = null,
    val data: NotificationListPayload? = null
) {
    val isSuccessful: Boolean get() = success || status
}

@Serializable
private data class NotificationListPayload(
    val notifications: List<AppNotification> = emptyList(),
    val pagination: PaginationInfo? = null
)

@Serializable
private data class PaginationInfo(
    val page: Int = 1,
    val pageSize: Int = 20,
    val total: Int = 0,
    val totalPages: Int = 0
)

@Serializable
private data class SendNotificationRequest(
    @SerialName("user_id") val userId: String,
    val title: String,
    val body: String,
    val data: Map<String, String>? = null
)

/**
 * Notification settings matching iOS LocalNotificationSettings (iOS parity).
 *
 * Maps to backend notification preferences for push, email, SMS,
 * and per-category notification toggles with quiet hours support.
 */
@Serializable
data class NotificationSettings(
    @SerialName("push_enabled") val pushEnabled: Boolean = true,
    @SerialName("email_enabled") val emailEnabled: Boolean = true,
    @SerialName("sms_enabled") val smsEnabled: Boolean = false,
    @SerialName("booking_notifications") val bookingNotifications: Boolean = true,
    @SerialName("message_notifications") val messageNotifications: Boolean = true,
    @SerialName("review_notifications") val reviewNotifications: Boolean = true,
    @SerialName("marketing_notifications") val marketingNotifications: Boolean = false,
    @SerialName("quiet_hours_enabled") val quietHoursEnabled: Boolean = false,
    @SerialName("quiet_hours_start") val quietHoursStart: String = "22:00",
    @SerialName("quiet_hours_end") val quietHoursEnd: String = "07:00",
    val timezone: String = ""
)

@Serializable
private data class NotificationSettingsResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: NotificationSettings? = null
)
