package com.shourov.apps.pacedream.feature.inbox.model

import com.shourov.apps.pacedream.feature.inbox.data.ThreadListing
import com.shourov.apps.pacedream.feature.inbox.data.ThreadUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thread model for inbox
 */
data class Thread(
    val id: String,
    val participants: List<String>,
    val lastMessage: Message?,
    val unreadCount: Int,
    val updatedAt: String?,
    val listing: ThreadListing?,
    val opponent: ThreadUser?
) {
    val displayName: String
        get() = opponent?.name ?: "Unknown"
    
    val avatarUrl: String?
        get() = opponent?.avatar
    
    val lastMessagePreview: String
        get() = lastMessage?.text?.take(100) ?: ""
    
    val formattedTime: String
        get() {
            val timestamp = updatedAt ?: lastMessage?.timestamp ?: return ""
            return try {
                formatTimestamp(timestamp)
            } catch (e: Exception) {
                timestamp
            }
        }
    
    val hasUnread: Boolean
        get() = unreadCount > 0
    
    private fun formatTimestamp(timestamp: String): String {
        // Try ISO 8601 format
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        
        for (format in formats) {
            try {
                val inputFormat = SimpleDateFormat(format, Locale.US)
                val date = inputFormat.parse(timestamp) ?: continue
                
                val now = Date()
                val diff = now.time - date.time
                
                return when {
                    diff < 60_000 -> "Just now"
                    diff < 3600_000 -> "${diff / 60_000}m ago"
                    diff < 86400_000 -> "${diff / 3600_000}h ago"
                    diff < 604800_000 -> SimpleDateFormat("EEE", Locale.US).format(date)
                    else -> SimpleDateFormat("MMM d", Locale.US).format(date)
                }
            } catch (e: Exception) {
                continue
            }
        }
        return timestamp
    }
}

/**
 * Message model
 */
data class Message(
    val id: String,
    val text: String,
    val senderId: String,
    val timestamp: String?,
    val attachments: List<String> = emptyList(),
    val isRead: Boolean = false
) {
    val formattedTime: String
        get() {
            val ts = timestamp ?: return ""
            return try {
                val formats = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'"
                )
                for (format in formats) {
                    try {
                        val inputFormat = SimpleDateFormat(format, Locale.US)
                        val date = inputFormat.parse(ts) ?: continue
                        return SimpleDateFormat("h:mm a", Locale.US).format(date)
                    } catch (e: Exception) {
                        continue
                    }
                }
                ts
            } catch (e: Exception) {
                ts
            }
        }
    
    val hasAttachments: Boolean
        get() = attachments.isNotEmpty()
}

/**
 * Unread counts for guest and host modes
 */
data class UnreadCounts(
    val guestUnread: Int,
    val hostUnread: Int
) {
    val totalUnread: Int
        get() = guestUnread + hostUnread
}

/**
 * Inbox mode - guest or host
 */
enum class InboxMode(val apiValue: String, val displayName: String) {
    GUEST("guest", "Guest"),
    HOST("host", "Host")
}

/**
 * UI state for inbox screen
 */
sealed class InboxUiState {
    object Loading : InboxUiState()
    data class Success(
        val threads: List<Thread>,
        val mode: InboxMode,
        val unreadCounts: UnreadCounts,
        val isRefreshing: Boolean = false,
        val hasMore: Boolean = false
    ) : InboxUiState() {
        val isEmpty: Boolean
            get() = threads.isEmpty()
    }
    data class Error(val message: String) : InboxUiState()
    object Empty : InboxUiState()
    object RequiresAuth : InboxUiState()
}

/**
 * UI state for thread detail screen
 */
sealed class ThreadDetailUiState {
    object Loading : ThreadDetailUiState()
    data class Success(
        val thread: Thread,
        val messages: List<Message>,
        val currentUserId: String,
        val isRefreshing: Boolean = false,
        val hasMore: Boolean = false,
        val isSending: Boolean = false
    ) : ThreadDetailUiState()
    data class Error(val message: String) : ThreadDetailUiState()
}

/**
 * Events from inbox UI
 */
sealed class InboxEvent {
    object Refresh : InboxEvent()
    data class ModeChanged(val mode: InboxMode) : InboxEvent()
    data class ThreadClicked(val thread: Thread) : InboxEvent()
    data class ArchiveThread(val thread: Thread) : InboxEvent()
    object LoadMore : InboxEvent()
}

/**
 * Events from thread detail UI
 */
sealed class ThreadDetailEvent {
    object Refresh : ThreadDetailEvent()
    data class SendMessage(val text: String) : ThreadDetailEvent()
    object LoadMore : ThreadDetailEvent()
}


