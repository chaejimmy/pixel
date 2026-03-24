package com.shourov.apps.pacedream.feature.notification.data

import com.shourov.apps.pacedream.core.network.model.NotificationResponse
import com.shourov.apps.pacedream.core.network.services.PaceDreamApiService
import com.shourov.apps.pacedream.feature.notification.model.AppNotification
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val apiService: PaceDreamApiService
) {
    suspend fun getNotifications(): Result<List<AppNotification>> {
        return try {
            val response = apiService.getNotifications()
            if (response.isSuccessful) {
                val notifications = response.body()?.data?.notifications
                    ?.map { it.toAppNotification() } ?: emptyList()
                Result.success(notifications.sortedByDescending { it.parsedDate })
            } else {
                Result.failure(Exception("Failed to load notifications: ${response.message()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch notifications")
            Result.failure(e)
        }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            val response = apiService.markNotificationAsRead(notificationId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark notification as read"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark notification as read")
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val response = apiService.markAllNotificationsAsRead()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark all notifications as read"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark all notifications as read")
            Result.failure(e)
        }
    }
}

private fun NotificationResponse.toAppNotification() = AppNotification(
    id = id,
    title = title,
    body = body,
    type = type,
    isRead = isRead,
    createdAt = createdAt
)
