package com.pacedream.app.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.TokenStorage
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val tokenStorage: TokenStorage,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun refresh() {
        loadNotifications()
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            val userId = tokenStorage.userId ?: return@launch
            val url = appConfig.buildApiUrl(
                "notifications", notificationId, "read"
            )

            when (apiClient.put(url, "{}", includeAuth = true)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map {
                                if (it.id == notificationId) it.copy(isRead = true) else it
                            }
                        )
                    }
                }
                is ApiResult.Failure -> {
                    Timber.w("Failed to mark notification as read")
                }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val userId = tokenStorage.userId ?: return@launch
            val url = appConfig.buildApiUrl(
                "notifications", "user", userId, "read-all"
            )

            when (apiClient.put(url, "{}", includeAuth = true)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { it.copy(isRead = true) }
                        )
                    }
                }
                is ApiResult.Failure -> {
                    Timber.w("Failed to mark all notifications as read")
                }
            }
        }
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            val userId = tokenStorage.userId
            if (userId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, isRefreshing = true, error = null) }

            val url = appConfig.buildApiUrl("notifications", "user", userId)

            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val notifications = parseNotificationsResponse(result.data)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            notifications = notifications,
                            error = null
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }

    private fun parseNotificationsResponse(responseBody: String): List<NotificationItem> {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject

            val notificationsArray = obj["data"]?.jsonArray
                ?: obj["notifications"]?.jsonArray
                ?: (obj["data"] as? JsonObject)?.get("notifications")?.jsonArray
                ?: return emptyList()

            notificationsArray.mapNotNull { notification ->
                try {
                    parseNotification(notification.jsonObject)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse notification, skipping")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse notifications response")
            emptyList()
        }
    }

    private fun parseNotification(obj: JsonObject): NotificationItem? {
        val id = obj["_id"]?.jsonPrimitive?.content
            ?: obj["id"]?.jsonPrimitive?.content
            ?: return null

        val title = obj["title"]?.jsonPrimitive?.content ?: ""
        val message = obj["message"]?.jsonPrimitive?.content
            ?: obj["body"]?.jsonPrimitive?.content
            ?: obj["text"]?.jsonPrimitive?.content
            ?: ""

        val type = obj["type"]?.jsonPrimitive?.content ?: "general"

        val isRead = obj["isRead"]?.jsonPrimitive?.booleanOrNull
            ?: obj["read"]?.jsonPrimitive?.booleanOrNull
            ?: false

        val createdAt = obj["createdAt"]?.jsonPrimitive?.content
            ?: obj["created_at"]?.jsonPrimitive?.content

        val actionUrl = obj["actionUrl"]?.jsonPrimitive?.content
        val actionId = obj["actionId"]?.jsonPrimitive?.content
            ?: obj["referenceId"]?.jsonPrimitive?.content

        return NotificationItem(
            id = id,
            title = title,
            message = message,
            type = type,
            isRead = isRead,
            formattedTime = formatTimestamp(createdAt),
            actionUrl = actionUrl,
            actionId = actionId
        )
    }

    private fun formatTimestamp(timestamp: String?): String {
        if (timestamp == null) return ""

        return try {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss"
            )

            var date: Date? = null
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    date = sdf.parse(timestamp)
                    if (date != null) break
                } catch (e: Exception) {
                    continue
                }
            }

            if (date == null) return ""

            val now = Date()
            val diffMs = now.time - date.time
            val diffHours = diffMs / (1000 * 60 * 60)
            val diffDays = diffHours / 24

            when {
                diffHours < 1 -> "Just now"
                diffHours < 24 -> "${diffHours}h ago"
                diffDays < 7 -> "${diffDays}d ago"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            ""
        }
    }
}

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val notifications: List<NotificationItem> = emptyList(),
    val error: String? = null
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean,
    val formattedTime: String,
    val actionUrl: String? = null,
    val actionId: String? = null
)
