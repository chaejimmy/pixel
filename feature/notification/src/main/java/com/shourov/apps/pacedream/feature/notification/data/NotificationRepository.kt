package com.shourov.apps.pacedream.feature.notification.data

import com.shourov.apps.pacedream.core.network.model.NotificationResponse
import com.shourov.apps.pacedream.core.network.services.PaceDreamApiService
import com.shourov.apps.pacedream.feature.notification.model.AppNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val apiService: PaceDreamApiService
) {

    private val notifications = MutableStateFlow<List<AppNotification>>(emptyList())

    // Local-only badge dismissal: IDs of unread notifications that the user has
    // already "seen" via the bell badge. The rows still read as unread on the
    // notifications screen until the user taps each one and we hit
    // markAsRead(). New unread items that aren't in this set bump the badge
    // back up.
    private val seenUnreadIds = MutableStateFlow<Set<String>>(emptySet())

    suspend fun getNotifications(): Result<List<AppNotification>> {
        return try {
            val response = apiService.getNotifications()
            if (response.isSuccessful) {
                val list = response.body()?.data?.notifications
                    ?.map { it.toAppNotification() }
                    ?.sortedByDescending { it.parsedDate }
                    ?: emptyList()
                notifications.value = list
                Result.success(list)
            } else {
                Result.failure(Exception("Failed to load notifications: ${response.message()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch notifications")
            Result.failure(e)
        }
    }

    /**
     * Stream of unread notification count for the bell badge. Combines the
     * cached notification list with the locally-tracked seen IDs so opening
     * the notifications screen can zero the badge without server round-trip.
     */
    fun unreadCount(): Flow<Int> =
        combine(notifications, seenUnreadIds) { list, seen ->
            list.count { !it.isRead && it.id !in seen }
        }.distinctUntilChanged()

    /**
     * Mark every currently-unread notification as "seen" for badge purposes.
     * Does NOT call the server — rows still display as unread inside the
     * notifications screen until [markAsRead] runs per-row.
     */
    fun markAllAsSeen() {
        val unreadIds = notifications.value.filter { !it.isRead }.map { it.id }
        if (unreadIds.isEmpty()) return
        seenUnreadIds.value = seenUnreadIds.value + unreadIds
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            val response = apiService.markNotificationAsRead(notificationId)
            if (response.isSuccessful) {
                // Reflect server state locally so unreadCount() recomputes.
                notifications.value = notifications.value.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
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
                notifications.value = notifications.value.map { it.copy(isRead = true) }
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
    createdAt = createdAt,
    deepLink = deepLink,
    data = notificationData
)
