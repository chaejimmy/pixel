package com.pacedream.app.feature.inbox

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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ThreadViewModel - Thread messages with tolerant parsing
 * 
 * iOS Parity:
 * - GET /v1/inbox/threads/:id/messages?limit=50&before=<messageId>
 * - POST /v1/inbox/threads/:id/messages { text, attachments?: [] }
 * - Refetch messages after sending
 * - Tolerant decoding for messages
 */
@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val tokenStorage: TokenStorage,
    private val json: Json
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ThreadUiState())
    val uiState: StateFlow<ThreadUiState> = _uiState.asStateFlow()
    
    private var threadId: String = ""
    private var beforeCursor: String? = null
    private val currentUserId: String? get() = tokenStorage.userId
    
    fun loadThread(threadId: String) {
        this.threadId = threadId
        beforeCursor = null
        loadMessages()
        loadThreadInfo()
    }
    
    fun refresh() {
        beforeCursor = null
        loadMessages()
    }
    
    fun loadMore() {
        if (_uiState.value.isLoadingMore || beforeCursor == null) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            
            val url = appConfig.buildApiUrl(
                "inbox", "threads", threadId, "messages",
                queryParams = mapOf(
                    "limit" to "50",
                    "before" to beforeCursor
                )
            )
            
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val (messages, nextCursor) = parseMessagesResponse(result.data)
                    beforeCursor = nextCursor
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            messages = messages + it.messages, // Prepend older messages
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
    
    fun sendMessage(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            
            val url = appConfig.buildApiUrl("inbox", "threads", threadId, "messages")
            val body = json.encodeToString(
                SendMessageRequest.serializer(),
                SendMessageRequest(text = text)
            )
            
            when (val result = apiClient.post(url, body, includeAuth = true)) {
                is ApiResult.Success -> {
                    // Refetch messages after sending (iOS parity)
                    refresh()
                    _uiState.update { it.copy(isSending = false) }
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to send message: ${result.error.message}")
                    _uiState.update { it.copy(isSending = false, sendError = result.error.message) }
                }
            }
        }
    }
    
    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val url = appConfig.buildApiUrl(
                "inbox", "threads", threadId, "messages",
                queryParams = mapOf("limit" to "50")
            )
            
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val (messages, nextCursor) = parseMessagesResponse(result.data)
                    beforeCursor = nextCursor
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = messages,
                            hasMore = nextCursor != null,
                            error = null
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }
    
    private fun loadThreadInfo() {
        viewModelScope.launch {
            val url = appConfig.buildApiUrl("inbox", "threads", threadId)
            
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    parseThreadInfo(result.data)
                }
                is ApiResult.Failure -> {
                    Timber.w("Failed to load thread info: ${result.error.message}")
                }
            }
        }
    }
    
    /**
     * Parse messages response with tolerant decoding
     */
    private fun parseMessagesResponse(responseBody: String): Pair<List<ThreadMessage>, String?> {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            
            val messagesArray = obj["data"]?.jsonArray
                ?: obj["messages"]?.jsonArray
                ?: (obj["data"] as? JsonObject)?.get("messages")?.jsonArray
                ?: return Pair(emptyList(), null)
            
            val messages = messagesArray.mapNotNull { message ->
                try {
                    parseMessage(message.jsonObject)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse message, skipping")
                    null
                }
            }
            
            val nextCursor = obj["before"]?.jsonPrimitive?.content
                ?: obj["nextCursor"]?.jsonPrimitive?.content
            
            Pair(messages, nextCursor)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse messages response")
            Pair(emptyList(), null)
        }
    }
    
    private fun parseMessage(obj: JsonObject): ThreadMessage? {
        val id = obj["_id"]?.jsonPrimitive?.content
            ?: obj["id"]?.jsonPrimitive?.content
            ?: return null
        
        val text = obj["text"]?.jsonPrimitive?.content
            ?: obj["content"]?.jsonPrimitive?.content
            ?: obj["body"]?.jsonPrimitive?.content
            ?: ""
        
        // Get sender info
        val sender = obj["sender"]?.jsonObject
            ?: obj["from"]?.jsonObject
            ?: obj["user"]?.jsonObject
        
        val senderId = sender?.get("_id")?.jsonPrimitive?.content
            ?: sender?.get("id")?.jsonPrimitive?.content
            ?: obj["senderId"]?.jsonPrimitive?.content
        
        val timestamp = obj["createdAt"]?.jsonPrimitive?.content
            ?: obj["timestamp"]?.jsonPrimitive?.content
        
        return ThreadMessage(
            id = id,
            text = text,
            senderId = senderId,
            isFromCurrentUser = senderId == currentUserId,
            formattedTime = formatTimestamp(timestamp)
        )
    }
    
    private fun parseThreadInfo(responseBody: String) {
        try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            
            val data = obj["data"]?.jsonObject ?: obj
            
            val participant = data["participant"]?.jsonObject
                ?: data["otherUser"]?.jsonObject
            
            val participantName = participant?.get("name")?.jsonPrimitive?.content
                ?: participant?.let {
                    val firstName = it["firstName"]?.jsonPrimitive?.content ?: ""
                    val lastName = it["lastName"]?.jsonPrimitive?.content ?: ""
                    "$firstName $lastName".trim().ifEmpty { "User" }
                }
                ?: "User"
            
            val participantAvatar = participant?.get("avatar")?.jsonPrimitive?.content
                ?: participant?.get("profileImage")?.jsonPrimitive?.content
            
            val listing = data["listing"]?.jsonObject
            val listingName = listing?.get("name")?.jsonPrimitive?.content
                ?: listing?.get("title")?.jsonPrimitive?.content
            
            _uiState.update {
                it.copy(
                    participantName = participantName,
                    participantAvatar = participantAvatar,
                    listingName = listingName
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse thread info")
        }
    }
    
    private fun formatTimestamp(timestamp: String?): String {
        if (timestamp == null) return ""
        
        return try {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
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
            
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            ""
        }
    }
}

/**
 * Thread UI State
 */
data class ThreadUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val messages: List<ThreadMessage> = emptyList(),
    val participantName: String = "",
    val participantAvatar: String? = null,
    val listingName: String? = null,
    val hasMore: Boolean = false,
    val error: String? = null,
    val sendError: String? = null
)

/**
 * Thread message model
 */
data class ThreadMessage(
    val id: String,
    val text: String,
    val senderId: String?,
    val isFromCurrentUser: Boolean,
    val formattedTime: String
)

/**
 * Send message request
 */
@Serializable
data class SendMessageRequest(
    val text: String,
    val attachments: List<String> = emptyList()
)

