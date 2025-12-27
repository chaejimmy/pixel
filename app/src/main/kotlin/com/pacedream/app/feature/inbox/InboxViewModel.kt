package com.pacedream.app.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * InboxViewModel - Inbox threads with tolerant parsing
 * 
 * iOS Parity:
 * - GET /v1/inbox/threads?limit=20&cursor=<threadId>&mode=guest|host
 * - GET /v1/inbox/unread-counts
 * - Tolerant decoding that survives schema drift
 * - Pagination support
 */
@HiltViewModel
class InboxViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()
    
    private var cursor: String? = null
    private var mode: String = "guest"
    
    init {
        loadThreads()
        loadUnreadCounts()
    }
    
    fun refresh() {
        cursor = null
        loadThreads()
        loadUnreadCounts()
    }
    
    fun loadMore() {
        if (_uiState.value.isLoadingMore || cursor == null) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            
            val url = appConfig.buildApiUrl(
                "inbox", "threads",
                queryParams = mapOf(
                    "limit" to "20",
                    "cursor" to cursor,
                    "mode" to mode
                )
            )
            
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val (threads, nextCursor) = parseThreadsResponse(result.data)
                    cursor = nextCursor
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            threads = it.threads + threads,
                            hasMore = nextCursor != null
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }
    
    private fun loadThreads() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isRefreshing = true, error = null) }
            
            val url = appConfig.buildApiUrl(
                "inbox", "threads",
                queryParams = mapOf(
                    "limit" to "20",
                    "mode" to mode
                )
            )
            
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val (threads, nextCursor) = parseThreadsResponse(result.data)
                    cursor = nextCursor
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            threads = threads,
                            hasMore = nextCursor != null,
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
    
    private fun loadUnreadCounts() {
        viewModelScope.launch {
            val url = appConfig.buildApiUrl("inbox", "unread-counts")
            
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val count = parseUnreadCountResponse(result.data)
                    _uiState.update { it.copy(unreadCount = count) }
                }
                is ApiResult.Failure -> {
                    // Ignore unread count errors
                    Timber.w("Failed to fetch unread counts: ${result.error.message}")
                }
            }
        }
    }
    
    /**
     * Parse threads response with tolerant decoding
     * Handles multiple response formats and schema drift
     */
    private fun parseThreadsResponse(responseBody: String): Pair<List<InboxThread>, String?> {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            
            // Find threads array in common locations
            val threadsArray = obj["data"]?.jsonArray
                ?: obj["threads"]?.jsonArray
                ?: (obj["data"] as? JsonObject)?.get("threads")?.jsonArray
                ?: return Pair(emptyList(), null)
            
            val threads = threadsArray.mapNotNull { thread ->
                try {
                    parseThread(thread.jsonObject)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse thread, skipping")
                    null
                }
            }
            
            // Get next cursor
            val nextCursor = obj["nextCursor"]?.jsonPrimitive?.content
                ?: obj["cursor"]?.jsonPrimitive?.content
                ?: (obj["data"] as? JsonObject)?.get("nextCursor")?.jsonPrimitive?.content
            
            Pair(threads, nextCursor)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse threads response")
            Pair(emptyList(), null)
        }
    }
    
    /**
     * Parse individual thread with tolerant decoding
     */
    private fun parseThread(obj: JsonObject): InboxThread? {
        val id = obj["_id"]?.jsonPrimitive?.content
            ?: obj["id"]?.jsonPrimitive?.content
            ?: return null
        
        // Get participant info (may be nested)
        val participant = obj["participant"]?.jsonObject
            ?: obj["otherUser"]?.jsonObject
            ?: obj["user"]?.jsonObject
        
        val participantName = participant?.get("name")?.jsonPrimitive?.content
            ?: participant?.let {
                val firstName = it["firstName"]?.jsonPrimitive?.content ?: ""
                val lastName = it["lastName"]?.jsonPrimitive?.content ?: ""
                "$firstName $lastName".trim().ifEmpty { "User" }
            }
            ?: obj["participantName"]?.jsonPrimitive?.content
            ?: "User"
        
        val participantAvatar = participant?.get("avatar")?.jsonPrimitive?.content
            ?: participant?.get("profileImage")?.jsonPrimitive?.content
            ?: obj["participantAvatar"]?.jsonPrimitive?.content
        
        // Get last message
        val lastMessageObj = obj["lastMessage"]?.jsonObject
        val lastMessage = lastMessageObj?.get("text")?.jsonPrimitive?.content
            ?: lastMessageObj?.get("content")?.jsonPrimitive?.content
            ?: obj["lastMessageText"]?.jsonPrimitive?.content
            ?: ""
        
        // Get listing info
        val listing = obj["listing"]?.jsonObject
        val listingName = listing?.get("name")?.jsonPrimitive?.content
            ?: listing?.get("title")?.jsonPrimitive?.content
        
        // Get unread status
        val isUnread = obj["unread"]?.jsonPrimitive?.booleanOrNull
            ?: obj["isUnread"]?.jsonPrimitive?.booleanOrNull
            ?: (obj["unreadCount"]?.jsonPrimitive?.intOrNull ?: 0) > 0
        
        // Get timestamp
        val timestamp = obj["updatedAt"]?.jsonPrimitive?.content
            ?: obj["lastMessageAt"]?.jsonPrimitive?.content
            ?: lastMessageObj?.get("createdAt")?.jsonPrimitive?.content
        
        return InboxThread(
            id = id,
            participantName = participantName,
            participantAvatar = participantAvatar,
            lastMessage = lastMessage,
            listingName = listingName,
            isUnread = isUnread,
            formattedTime = formatTimestamp(timestamp)
        )
    }
    
    private fun parseUnreadCountResponse(responseBody: String): Int {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            
            obj["count"]?.jsonPrimitive?.intOrNull
                ?: obj["unreadCount"]?.jsonPrimitive?.intOrNull
                ?: (obj["data"] as? JsonObject)?.get("count")?.jsonPrimitive?.intOrNull
                ?: 0
        } catch (e: Exception) {
            0
        }
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
                diffHours < 24 -> "${diffHours}h"
                diffDays < 7 -> "${diffDays}d"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            ""
        }
    }
}

/**
 * Inbox UI State
 */
data class InboxUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val threads: List<InboxThread> = emptyList(),
    val unreadCount: Int = 0,
    val hasMore: Boolean = false,
    val error: String? = null
)

/**
 * Inbox thread model
 */
data class InboxThread(
    val id: String,
    val participantName: String,
    val participantAvatar: String?,
    val lastMessage: String,
    val listingName: String?,
    val isUnread: Boolean,
    val formattedTime: String
)

