package com.pacedream.app.feature.inbox

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.TokenStorage
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import com.pacedream.app.feature.settings.AccountSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
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
    private val json: Json,
    val accountSettingsRepository: AccountSettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ThreadUiState())
    val uiState: StateFlow<ThreadUiState> = _uiState.asStateFlow()
    
    private var threadId: String = ""
    private var beforeCursor: String? = null
    private val currentUserId: String? get() = tokenStorage.userId
    // Track temp message IDs for deduplication (iOS PR #205 parity)
    private val pendingTempIds = mutableSetOf<String>()
    private val idempotencyKeyByTempId = mutableMapOf<String, String>()
    
    fun loadThread(threadId: String) {
        this.threadId = threadId
        beforeCursor = null
        loadMessages()
        loadThreadInfo()
        checkAttachmentStatus()
    }
    
    fun refresh() {
        beforeCursor = null
        loadMessages()
    }
    
    fun loadMore() {
        if (_uiState.value.isLoadingMore || beforeCursor == null) return
        // iOS PR #201 parity: skip temp-* IDs as pagination cursor
        if (beforeCursor?.startsWith("temp-") == true) return

        viewModelScope.launch {
            try {
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
                        val (messages, nextCursor) = withContext(Dispatchers.Default) {
                            parseMessagesResponse(result.data)
                        }
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
            } catch (e: Exception) {
                Timber.e(e, "Failed to load more messages")
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
    
    fun sendMessage(text: String) {
        viewModelScope.launch {
            // iOS PR #205 parity: optimistic temp bubble + idempotency key
            val tempId = "temp-${UUID.randomUUID()}"
            val idempotencyKey = UUID.randomUUID().toString()
            pendingTempIds.add(tempId)
            idempotencyKeyByTempId[tempId] = idempotencyKey

            val tempMessage = ThreadMessage(
                id = tempId,
                text = text,
                senderId = currentUserId,
                isFromCurrentUser = true,
                formattedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
                attachments = emptyList()
            )
            _uiState.update { it.copy(isSending = true, messages = it.messages + tempMessage) }

            val url = appConfig.buildApiUrl("inbox", "threads", threadId, "messages")
            val body = json.encodeToString(
                SendMessageRequest.serializer(),
                SendMessageRequest(text = text, idempotencyKey = idempotencyKey)
            )

            when (val result = apiClient.post(url, body, includeAuth = true)) {
                is ApiResult.Success -> {
                    // Replace temp bubble with real message or just refresh
                    pendingTempIds.remove(tempId)
                    idempotencyKeyByTempId.remove(tempId)
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

    /**
     * Send media attachments (iOS PR #207 parity: photo/video sharing in chat).
     * Uses multipart/form-data upload to /inbox/threads/:id/messages
     */
    fun sendMedia(fileData: ByteArray, fileName: String, mimeType: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSending = true) }

                val url = appConfig.buildApiUrl("inbox", "threads", threadId, "messages")
                val fileBody = fileData.toRequestBody(mimeType.toMediaType())
                val filePart = MultipartBody.Part.createFormData("file", fileName, fileBody)

                when (val result = apiClient.postMultipart(url, listOf(filePart), includeAuth = true)) {
                    is ApiResult.Success -> {
                        refresh()
                        _uiState.update { it.copy(isSending = false) }
                    }
                    is ApiResult.Failure -> {
                        Timber.e("Failed to send media: ${result.error.message}")
                        _uiState.update { it.copy(isSending = false, sendError = result.error.message ?: "Failed to send media") }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to send media")
                _uiState.update { it.copy(isSending = false, sendError = e.message ?: "Failed to send media") }
            }
        }
    }

    /**
     * Check if attachments are enabled for this thread (iOS PR #207 parity).
     * Calls GET /inbox/threads/:id/attachment-status
     */
    fun checkAttachmentStatus() {
        viewModelScope.launch {
            try {
                val url = appConfig.buildApiUrl("inbox", "threads", threadId, "attachment-status")
                when (val result = apiClient.get(url, includeAuth = true)) {
                    is ApiResult.Success -> {
                        try {
                            val (enabled, reason) = withContext(Dispatchers.Default) {
                                val element = json.parseToJsonElement(result.data)
                                val obj = element.jsonObject
                                val en = obj["enabled"]?.jsonPrimitive?.booleanOrNull ?: false
                                val rs = obj["reason"]?.jsonPrimitive?.content
                                Pair(en, rs)
                            }
                            _uiState.update { it.copy(attachmentsEnabled = enabled, attachmentDisabledReason = reason) }
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to parse attachment status")
                        }
                    }
                    is ApiResult.Failure -> {
                        _uiState.update { it.copy(attachmentsEnabled = false) }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to check attachment status")
                _uiState.update { it.copy(attachmentsEnabled = false) }
            }
        }
    }

    /**
     * Send media from a content URI (iOS PR #207 parity).
     * Reads file data from ContentResolver and calls sendMedia.
     */
    fun sendMediaFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                // Read file bytes on a background thread to avoid blocking the
                // main thread and causing ANR during large file reads.
                val (bytes, fileName, mimeType) = withContext(Dispatchers.IO) {
                    val contentResolver = context.contentResolver
                    val mime = contentResolver.getType(uri) ?: "application/octet-stream"
                    val name = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
                    } ?: "attachment"
                    val data = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    Triple(data, name, mime)
                }

                if (bytes != null) {
                    sendMedia(bytes, fileName, mimeType)
                } else {
                    _uiState.update { it.copy(sendError = "Could not read selected file") }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to read media URI")
                _uiState.update { it.copy(sendError = "Failed to read selected file") }
            }
        }
    }
    
    /**
     * Block or unblock the opponent in this thread.
     * Calls POST /v1/inbox/threads/:id/block { block: true/false }
     * Web parity with ThreadView.tsx.
     */
    fun toggleBlock(block: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isBlockLoading = true, blockError = null) }

                val url = appConfig.buildApiUrl("inbox", "threads", threadId, "block")
                val body = """{"block": $block}"""

                when (val result = apiClient.post(url, body, includeAuth = true)) {
                    is ApiResult.Success -> {
                        _uiState.update {
                            it.copy(isBlockLoading = false, isBlocked = block, blockError = null)
                        }
                    }
                    is ApiResult.Failure -> {
                        Timber.e("Failed to ${if (block) "block" else "unblock"} user: ${result.error.message}")
                        _uiState.update {
                            it.copy(
                                isBlockLoading = false,
                                blockError = result.error.message ?: "Failed to ${if (block) "block" else "unblock"} user"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to ${if (block) "block" else "unblock"} user")
                _uiState.update {
                    it.copy(
                        isBlockLoading = false,
                        blockError = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val url = appConfig.buildApiUrl(
                    "inbox", "threads", threadId, "messages",
                    queryParams = mapOf("limit" to "50")
                )

                when (val result = apiClient.get(url, includeAuth = true)) {
                    is ApiResult.Success -> {
                        // Parse JSON off the main thread to avoid ANR
                        val (messages, nextCursor) = withContext(Dispatchers.Default) {
                            parseMessagesResponse(result.data)
                        }
                        beforeCursor = nextCursor
                        // Derive opponent ID from messages if not set from thread info
                        val opponentId = messages.firstOrNull { it.senderId != null && it.senderId != currentUserId }?.senderId
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                messages = messages,
                                hasMore = nextCursor != null,
                                error = null,
                                participantId = it.participantId ?: opponentId
                            )
                        }
                    }
                    is ApiResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.error.message ?: "Failed to load messages"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load messages")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }
    
    private fun loadThreadInfo() {
        viewModelScope.launch {
            try {
                val url = appConfig.buildApiUrl("inbox", "threads", threadId)

                when (val result = apiClient.get(url, includeAuth = true)) {
                    is ApiResult.Success -> {
                        // Parse JSON off the main thread to avoid ANR
                        withContext(Dispatchers.Default) {
                            parseThreadInfo(result.data)
                        }
                    }
                    is ApiResult.Failure -> {
                        Timber.w("Failed to load thread info: ${result.error.message}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load thread info")
            }
        }
    }
    
    /**
     * Parse messages response with tolerant decoding.
     * iOS PR #205 parity: handle { items: [...] } wrapper in addition to
     * existing { data: [...] } and { messages: [...] } formats.
     */
    private fun parseMessagesResponse(responseBody: String): Pair<List<ThreadMessage>, String?> {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject

            val messagesArray = obj["items"]?.jsonArray
                ?: obj["data"]?.jsonArray
                ?: obj["messages"]?.jsonArray
                ?: (obj["data"] as? JsonObject)?.get("messages")?.jsonArray
                ?: (obj["data"] as? JsonObject)?.get("items")?.jsonArray
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

        // iOS PR #201 parity: senderId can be a populated Member object OR a plain string
        val senderId = try {
            val senderElement = obj["sender"] ?: obj["senderId"] ?: obj["from"]
            when {
                senderElement == null -> null
                // Try as object first (populated Member)
                senderElement is kotlinx.serialization.json.JsonObject -> {
                    senderElement.jsonObject["_id"]?.jsonPrimitive?.content
                        ?: senderElement.jsonObject["id"]?.jsonPrimitive?.content
                }
                // Then as plain string
                else -> try { senderElement.jsonPrimitive.content } catch (_: Exception) { null }
            }
        } catch (_: Exception) {
            obj["senderId"]?.jsonPrimitive?.content
        }

        val timestamp = obj["createdAt"]?.jsonPrimitive?.content
            ?: obj["timestamp"]?.jsonPrimitive?.content

        // iOS PR #207 parity: parse attachments array
        val attachments = try {
            obj["attachments"]?.jsonArray?.mapNotNull { att ->
                try {
                    val attObj = att.jsonObject
                    MessageAttachment(
                        url = attObj["url"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                        type = attObj["type"]?.jsonPrimitive?.content ?: "image",
                        fileName = attObj["fileName"]?.jsonPrimitive?.content
                            ?: attObj["filename"]?.jsonPrimitive?.content
                    )
                } catch (_: Exception) { null }
            } ?: emptyList()
        } catch (_: Exception) { emptyList() }

        return ThreadMessage(
            id = id,
            text = text,
            senderId = senderId,
            isFromCurrentUser = senderId == currentUserId,
            formattedTime = formatTimestamp(timestamp),
            attachments = attachments
        )
    }
    
    private fun parseThreadInfo(responseBody: String) {
        try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            
            val data = obj["data"]?.jsonObject ?: obj
            
            val participant = data["opponent"]?.jsonObject
                ?: data["participant"]?.jsonObject
                ?: data["otherUser"]?.jsonObject

            val participantName = participant?.get("name")?.jsonPrimitive?.content
                ?: participant?.let {
                    val firstName = it["first_name"]?.jsonPrimitive?.content
                        ?: it["firstName"]?.jsonPrimitive?.content ?: ""
                    val lastName = it["last_name"]?.jsonPrimitive?.content
                        ?: it["lastName"]?.jsonPrimitive?.content ?: ""
                    "$firstName $lastName".trim().ifEmpty { "User" }
                }
                ?: "User"

            val participantAvatar = try {
                participant?.get("profilePic")?.jsonPrimitive?.content
            } catch (_: Exception) {
                try {
                    participant?.get("profilePic")?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
                } catch (_: Exception) { null }
            }
                ?: participant?.get("avatarUrl")?.jsonPrimitive?.content
                ?: participant?.get("avatar")?.jsonPrimitive?.content
                ?: participant?.get("profileImage")?.jsonPrimitive?.content

            val participantId = participant?.get("_id")?.jsonPrimitive?.content
                ?: participant?.get("id")?.jsonPrimitive?.content

            val listing = data["listing"]?.jsonObject
            val listingName = listing?.get("name")?.jsonPrimitive?.content
                ?: listing?.get("title")?.jsonPrimitive?.content

            // Check if the opponent is blocked (web parity)
            val isBlocked = data["isBlocked"]?.jsonPrimitive?.booleanOrNull ?: false

            _uiState.update {
                it.copy(
                    participantName = participantName,
                    participantAvatar = participantAvatar,
                    participantId = participantId,
                    listingName = listingName,
                    isBlocked = isBlocked
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
    val participantId: String? = null,
    val listingName: String? = null,
    val hasMore: Boolean = false,
    val error: String? = null,
    val sendError: String? = null,
    // iOS PR #207 parity: attachment support
    val attachmentsEnabled: Boolean = false,
    val attachmentDisabledReason: String? = null,
    // Block/unblock feature (web parity)
    val isBlocked: Boolean = false,
    val isBlockLoading: Boolean = false,
    val blockError: String? = null
)

/**
 * Thread message model (iOS PR #207 parity: attachments)
 */
data class ThreadMessage(
    val id: String,
    val text: String,
    val senderId: String?,
    val isFromCurrentUser: Boolean,
    val formattedTime: String,
    val attachments: List<MessageAttachment> = emptyList()
)

/**
 * Attachment model for media messages (iOS PR #207 parity)
 */
data class MessageAttachment(
    val url: String,
    val type: String = "image", // "image" or "video"
    val fileName: String? = null
)

/**
 * Send message request
 */
@Serializable
data class SendMessageRequest(
    val text: String,
    val attachments: List<String> = emptyList(),
    val idempotencyKey: String? = null
)


