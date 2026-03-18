package com.shourov.apps.pacedream.feature.notification.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * App notification model matching iOS AppNotification
 */
data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: String
) {
    val parsedDate: Date
        get() {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
            )
            for (format in formats) {
                try {
                    return SimpleDateFormat(format, Locale.US).parse(createdAt) ?: continue
                } catch (_: Exception) {
                    continue
                }
            }
            return Date(0)
        }
}

/**
 * Notification grouping key matching iOS groupedNotifications
 */
enum class NotificationGroup(val displayName: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    EARLIER("Earlier");

    companion object {
        fun forDate(date: Date): NotificationGroup {
            val calendar = Calendar.getInstance()
            val now = Date()

            return when {
                isToday(date, calendar) -> TODAY
                isSameWeek(date, now, calendar) -> THIS_WEEK
                isSameMonth(date, now, calendar) -> THIS_MONTH
                else -> EARLIER
            }
        }

        private fun isToday(date: Date, calendar: Calendar): Boolean {
            val today = Calendar.getInstance()
            calendar.time = date
            return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        }

        private fun isSameWeek(date: Date, now: Date, calendar: Calendar): Boolean {
            val nowCal = Calendar.getInstance().apply { time = now }
            calendar.time = date
            return calendar.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                    calendar.get(Calendar.WEEK_OF_YEAR) == nowCal.get(Calendar.WEEK_OF_YEAR)
        }

        private fun isSameMonth(date: Date, now: Date, calendar: Calendar): Boolean {
            val nowCal = Calendar.getInstance().apply { time = now }
            calendar.time = date
            return calendar.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
        }
    }
}

/**
 * UI state for notifications screen
 */
sealed class NotificationUiState {
    object Loading : NotificationUiState()
    data class Success(
        val groupedNotifications: Map<NotificationGroup, List<AppNotification>>,
        val isRefreshing: Boolean = false
    ) : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
    object Empty : NotificationUiState()
}
