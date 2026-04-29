package com.shourov.apps.pacedream.feature.inbox.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import com.shourov.apps.pacedream.feature.inbox.model.Message
import com.shourov.apps.pacedream.feature.inbox.model.Thread
import com.shourov.apps.pacedream.feature.inbox.model.UnreadCounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import androidx.annotation.VisibleForTesting
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Inbox/Messaging operations with tolerant parsing
 * 
 * Endpoints:
 * - GET /v1/inbox/threads?limit=20&cursor=<threadId>&mode=guest|host
 * - GET /v1/inbox/unread-counts → { guestUnread, hostUnread }
 * - GET /v1/inbox/threads/:id/messages?limit=50&before=<messageId>
 * - POST /v1/inbox/threads/:id/messages body { text, attachments?: [] }
 * - POST /v1/inbox/threads/:id/archive
 * 
 * Tolerant decoding:
 * - threads response may fail strict decode; implement fallback parser extracting minimal thread fields
 * - messages response may be array or wrapper (items/messages/data/items/messages)
 */
@Singleton
class InboxRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    
    /**
     * Get threads with pagination
     */
    suspend fun getThreads(
        mode: String = "guest",
        limit: Int = 20,
        cursor: String? = null
    ): ApiResult<ThreadsResult> {
        // Guard: never send blank cursor — backend rejects it as "Invalid ID format"
        val safeCursor = cursor?.takeIf { it.isNotBlank() }
        Timber.d("InboxRepository: getThreads — mode=$mode, limit=$limit, cursor=${safeCursor ?: "(none)"}")

        val url = appConfig.buildApiUrlWithQuery(
            "inbox", "threads",
            queryParams = mapOf(
                "limit" to limit.toString(),
                "mode" to mode,
                "cursor" to safeCursor
            )
        )
        
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val threadsResult = withContext(Dispatchers.Default) {
                        parseThreadsResponse(result.data)
                    }
                    Timber.d("InboxRepository: parsed ${threadsResult.threads.size} threads, hasMore=${threadsResult.hasMore}")
                    ApiResult.Success(threadsResult)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse threads response, trying fallback")
                    // Try fallback parsing
                    try {
                        val fallbackResult = withContext(Dispatchers.Default) {
                            parseTolerantThreadsResponse(result.data)
                        }
                        Timber.d("InboxRepository: fallback parsed ${fallbackResult.threads.size} threads")
                        if (fallbackResult.threads.isEmpty()) {
                            // If fallback also returns empty, it may be a parse failure
                            Timber.w("InboxRepository: fallback returned empty list — raw response length=${result.data.length}")
                        }
                        ApiResult.Success(fallbackResult)
                    } catch (e2: Exception) {
                        Timber.e(e2, "Fallback parsing also failed")
                        ApiResult.Failure(ApiError.DecodingError("Failed to parse threads", e2))
                    }
                }
            }
            is ApiResult.Failure -> result
        }
    }
    
    /**
     * Get unread counts for guest and host modes
     */
    suspend fun getUnreadCounts(): ApiResult<UnreadCounts> {
        val url = appConfig.buildApiUrl("inbox", "unread-counts")
        
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val counts = withContext(Dispatchers.Default) {
                        parseUnreadCounts(result.data)
                    }
                    ApiResult.Success(counts)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse unread counts")
                    ApiResult.Success(UnreadCounts(0, 0)) // Default to zero on parse error
                }
            }
            is ApiResult.Failure -> result
        }
    }
    
    /**
     * Get messages for a thread with pagination
     */
    suspend fun getMessages(
        threadId: String,
        limit: Int = 50,
        before: String? = null
    ): ApiResult<MessagesResult> {
        val url = appConfig.buildApiUrlWithQuery(
            "inbox", "threads", threadId, "messages",
            queryParams = mapOf(
                "limit" to limit.toString(),
                "before" to before
            )
        )

        val TAG = "InboxRepo"
        android.util.Log.d(TAG, "getMessages: threadId=$threadId url=$url")
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    // Log raw response (first 500 chars) for debugging
                    android.util.Log.d(TAG, "getMessages SUCCESS: rawLen=${result.data.length} raw=${result.data.take(500)}")
                    val messagesResult = withContext(Dispatchers.Default) {
                        parseMessagesResponse(result.data)
                    }
                    // Log each parsed message's fields
                    messagesResult.messages.forEachIndexed { i, msg ->
                        android.util.Log.d(TAG, "  msg[$i]: id=${msg.id} text='${msg.text.take(50)}' senderId=${msg.senderId} status=${msg.status}")
                    }
                    android.util.Log.d(TAG, "getMessages: parsed ${messagesResult.messages.size} messages, hasMore=${messagesResult.hasMore}")
                    ApiResult.Success(messagesResult)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "getMessages PARSE FAILED: ${e.message}", e)
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse messages", e))
                }
            }
            is ApiResult.Failure -> {
                android.util.Log.e(TAG, "getMessages FAILED: ${result.error.message}, trying legacy")
                // Fall back to legacy: GET /chat/{threadId}/messages
                getMessagesFromLegacyEndpoint(threadId, limit)
            }
        }
    }

    /**
     * Fallback: fetch messages from legacy GET /chat/:chatId/messages endpoint.
     * This works when the inbox endpoint returns 404 (Thread not found for Chat IDs).
     */
    private suspend fun getMessagesFromLegacyEndpoint(
        chatId: String,
        limit: Int
    ): ApiResult<MessagesResult> {
        try {
            val url = appConfig.buildApiUrlWithQuery(
                "chat", chatId, "messages",
                queryParams = mapOf("limit" to limit.toString())
            )
            android.util.Log.d("InboxRepo", "LEGACY: trying GET /chat/$chatId/messages url=$url")
            return when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    try {
                        android.util.Log.d("InboxRepo", "LEGACY SUCCESS: rawLen=${result.data.length} raw=${result.data.take(500)}")
                        val messagesResult = withContext(Dispatchers.Default) {
                            parseLegacyMessagesResponse(result.data)
                        }
                        Timber.d("InboxRepository: legacy parsed ${messagesResult.messages.size} messages")
                        ApiResult.Success(messagesResult)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse legacy messages response")
                        // Try second legacy: GET /messages?chatId=xxx
                        getMessagesFromMessageEndpoint(chatId, limit)
                    }
                }
                is ApiResult.Failure -> {
                    Timber.e("InboxRepository: legacy also failed — ${result.error.message}")
                    // Try second legacy: GET /messages?chatId=xxx
                    getMessagesFromMessageEndpoint(chatId, limit)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Legacy messages endpoint exception")
            return getMessagesFromMessageEndpoint(chatId, limit)
        }
    }

    /**
     * Second fallback: GET /messages?chatId=xxx (message_controller)
     */
    private suspend fun getMessagesFromMessageEndpoint(
        chatId: String,
        limit: Int
    ): ApiResult<MessagesResult> {
        try {
            val url = appConfig.buildApiUrlWithQuery(
                "messages",
                queryParams = mapOf("chatId" to chatId, "limit" to limit.toString())
            )
            Timber.d("InboxRepository: trying message_controller endpoint for chatId=$chatId")
            return when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    try {
                        val messagesResult = withContext(Dispatchers.Default) {
                            parseLegacyFlatMessagesResponse(result.data)
                        }
                        Timber.d("InboxRepository: message_controller parsed ${messagesResult.messages.size} messages")
                        ApiResult.Success(messagesResult)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse message_controller response")
                        ApiResult.Failure(ApiError.DecodingError("All message endpoints failed for chatId=$chatId", e))
                    }
                }
                is ApiResult.Failure -> {
                    Timber.e("InboxRepository: message_controller also failed — ${result.error.message}")
                    ApiResult.Failure(ApiError.NetworkError("All message endpoints failed for chatId=$chatId"))
                }
            }
        } catch (e: Exception) {
            // Log the raw exception for ops, but never surface SocketTimeoutException,
            // JSON parser errors, etc. to the user.
            Timber.e(e, "InboxRepository: all message endpoints failed for chatId=$chatId")
            return ApiResult.Failure(
                ApiError.NetworkError(
                    com.pacedream.common.util.UserFacingErrorMapper.forLoadMessages(e)
                )
            )
        }
    }

    /**
     * Send a message to a thread
     */
    suspend fun sendMessage(
        threadId: String,
        text: String,
        attachments: List<String> = emptyList()
    ): ApiResult<Message> {
        val url = appConfig.buildApiUrl("inbox", "threads", threadId, "messages")

        // Use kotlinx.serialization for safe JSON construction
        val bodyObj = kotlinx.serialization.json.buildJsonObject {
            put("text", kotlinx.serialization.json.JsonPrimitive(text))
            if (attachments.isNotEmpty()) {
                put("attachments", kotlinx.serialization.json.JsonArray(
                    attachments.map { kotlinx.serialization.json.JsonPrimitive(it) }
                ))
            }
        }
        val body = bodyObj.toString()
        
        val TAG = "InboxRepo"
        android.util.Log.d(TAG, "sendMessage: threadId=$threadId text='${text.take(50)}' url=$url body=$body")
        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    android.util.Log.d(TAG, "sendMessage SUCCESS: raw=${result.data.take(500)}")
                    val message = withContext(Dispatchers.Default) {
                        parseMessageFromResponse(result.data)
                    }
                    android.util.Log.d(TAG, "sendMessage parsed: id=${message.id} text='${message.text.take(50)}' senderId=${message.senderId}")
                    ApiResult.Success(message)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "sendMessage PARSE FAILED: ${e.message}", e)
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse message", e))
                }
            }
            is ApiResult.Failure -> {
                android.util.Log.e(TAG, "sendMessage FAILED: ${result.error.message}")
                result
            }
        }
    }
    
    /**
     * Get a single thread's details (opponent, listing, unread count, etc.)
     * GET /v1/inbox/threads/:id
     */
    suspend fun getThread(threadId: String): ApiResult<Thread> {
        val url = appConfig.buildApiUrl("inbox", "threads", threadId)

        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val thread = withContext(Dispatchers.Default) {
                        val jsonElement = json.parseToJsonElement(result.data)
                        val data = jsonElement.jsonObject["data"]?.jsonObject ?: jsonElement.jsonObject
                        parseThread(data)
                    }
                    ApiResult.Success(thread)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse thread details")
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse thread details", e))
                }
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Mark a thread as read on the server.
     * POST /v1/inbox/threads/:id/read
     */
    suspend fun markThreadAsRead(threadId: String): ApiResult<Boolean> {
        val url = appConfig.buildApiUrl("inbox", "threads", threadId, "read")

        return when (val result = apiClient.post(url, "{}", includeAuth = true)) {
            is ApiResult.Success -> ApiResult.Success(true)
            is ApiResult.Failure -> {
                Timber.e("InboxRepository: markThreadAsRead FAILED — ${result.error.message}")
                result
            }
        }
    }

    /**
     * Upload images to a chat thread and send as a message with attachments.
     * Compresses/downscales images on Dispatchers.IO to avoid ANR.
     * Uses POST /v1/chat-media/upload then sends message with attachment URLs.
     */
    suspend fun uploadChatMedia(
        context: Context,
        threadId: String,
        imageUris: List<Uri>,
        text: String? = null
    ): ApiResult<Message> {
        // Compress images on background thread
        val parts = withContext(Dispatchers.IO) {
            val result = mutableListOf<MultipartBody.Part>()
            for (uri in imageUris) {
                val inputStream = context.contentResolver.openInputStream(uri) ?: continue
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (bitmap == null) continue

                val scaled = downscaleBitmap(bitmap, MAX_UPLOAD_DIMENSION)
                val outputStream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
                val bytes = outputStream.toByteArray()

                if (scaled != bitmap) scaled.recycle()
                bitmap.recycle()

                val fileName = "photo_${System.currentTimeMillis()}.jpg"
                val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
                result.add(MultipartBody.Part.createFormData("images", fileName, requestBody))
            }
            result
        }

        if (parts.isEmpty()) {
            return ApiResult.Failure(ApiError.DecodingError("No valid images to upload"))
        }

        val url = appConfig.buildApiUrl("chat-media", "upload")
        return when (val result = apiClient.postMultipartParts(url, parts, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val element = json.parseToJsonElement(result.data)
                    val obj = element.jsonObject
                    val data = obj["data"]?.jsonObject ?: obj

                    // Extract attachment URLs from upload response
                    val attachmentUrls = try {
                        data["attachments"]?.jsonArray?.mapNotNull { att ->
                            att.jsonObject["url"]?.jsonPrimitive?.content
                        } ?: emptyList()
                    } catch (_: Exception) { emptyList() }

                    // Send message with attachment URLs
                    sendMessage(threadId, text ?: "", attachmentUrls)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse upload response")
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse upload response", e))
                }
            }
            is ApiResult.Failure -> result
        }
    }

    private fun downscaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        return Bitmap.createScaledBitmap(bitmap, (width * ratio).toInt(), (height * ratio).toInt(), true)
    }

    /**
     * Archive a thread
     */
    suspend fun archiveThread(threadId: String): ApiResult<Boolean> {
        val url = appConfig.buildApiUrl("inbox", "threads", threadId, "archive")
        
        return when (val result = apiClient.post(url, "{}", includeAuth = true)) {
            is ApiResult.Success -> ApiResult.Success(true)
            is ApiResult.Failure -> result
        }
    }
    
    /**
     * Create or find thread with opponent
     * POST /v1/inbox/threads (fallback /v1/inbox/thread if 404)
     */
    suspend fun ensureThreadId(opponentId: String, listingId: String? = null): ApiResult<String> {
        // First try to find existing thread
        val existingThread = findExistingThread(opponentId)
        if (existingThread != null) {
            return ApiResult.Success(existingThread)
        }
        
        // Create new thread
        val bodyObj = kotlinx.serialization.json.buildJsonObject {
            put("participantId", kotlinx.serialization.json.JsonPrimitive(opponentId))
            listingId?.let { put("listingId", kotlinx.serialization.json.JsonPrimitive(it)) }
        }
        val body = bodyObj.toString()
        
        // Try primary endpoint
        val primaryUrl = appConfig.buildApiUrl("inbox", "threads")
        var result = apiClient.post(primaryUrl, body, includeAuth = true)
        
        // Fallback to alternate endpoint if 404
        if (result is ApiResult.Failure && result.error is ApiError.NotFound) {
            val fallbackUrl = appConfig.buildApiUrl("inbox", "thread")
            result = apiClient.post(fallbackUrl, body, includeAuth = true)
        }
        
        return when (result) {
            is ApiResult.Success -> {
                try {
                    val jsonElement = json.parseToJsonElement(result.data)
                    val data = jsonElement.jsonObject["data"]?.jsonObject ?: jsonElement.jsonObject
                    val threadId = data["_id"]?.jsonPrimitive?.content
                        ?: data["id"]?.jsonPrimitive?.content
                        ?: data["threadId"]?.jsonPrimitive?.content
                    
                    if (threadId != null) {
                        ApiResult.Success(threadId)
                    } else {
                        ApiResult.Failure(ApiError.DecodingError("No thread ID in response"))
                    }
                } catch (e: Exception) {
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse thread creation response", e))
                }
            }
            is ApiResult.Failure -> result
        }
    }
    
    /**
     * Find existing thread with opponent on first page
     */
    private suspend fun findExistingThread(opponentId: String): String? {
        val threadsResult = getThreads(limit = 50)
        if (threadsResult is ApiResult.Success) {
            return threadsResult.data.threads.find { thread ->
                thread.participants.any { it == opponentId }
            }?.id
        }
        return null
    }
    
    // Parsing methods with tolerant extraction
    
    private fun parseThreadsResponse(responseBody: String): ThreadsResult {
        val jsonElement = json.parseToJsonElement(responseBody)
        val jsonObject = jsonElement.jsonObject

        // Backend returns { items: [...], nextCursor } at root level
        // Also support wrapped formats: { data: { threads: [...] } }
        val root = jsonObject
        val data = jsonObject["data"]?.jsonObject
        val threadsArray = findArrayField(root, "items", "threads")
            ?: (data?.let { findArrayField(it, "threads", "items", "data") })

        val threads = threadsArray?.mapNotNull { element ->
            try {
                parseThread(element.jsonObject)
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse thread")
                null
            }
        } ?: emptyList()

        // Cursor can be at root level or nested in data
        val cursorSource = if (root.containsKey("nextCursor")) root else data ?: root
        val rawCursor = cursorSource["nextCursor"]?.jsonPrimitive?.content
            ?: cursorSource["cursor"]?.jsonPrimitive?.content
        val nextCursor = rawCursor?.takeIf { it.isNotBlank() && it != "null" }

        val serverHasMore = try { cursorSource["hasMore"]?.jsonPrimitive?.booleanOrNull ?: false } catch (_: Exception) { false }
        val hasMore = (serverHasMore || nextCursor != null) && nextCursor != null

        return ThreadsResult(threads, nextCursor, hasMore)
    }
    
    /**
     * Fallback tolerant parser for threads - extracts minimal fields
     */
    private fun parseTolerantThreadsResponse(responseBody: String): ThreadsResult {
        val jsonElement = json.parseToJsonElement(responseBody)
        
        // Try to find any array in the response
        val arrayElement = findAnyArrayInJson(jsonElement)
        
        val threads = arrayElement?.mapNotNull { element ->
            try {
                parseTolerantThread(element.jsonObject)
            } catch (e: Exception) {
                timber.log.Timber.w(e, "Failed to parse tolerant thread element")
                null
            }
        } ?: emptyList()
        
        return ThreadsResult(threads, null, false)
    }
    
    private fun parseThread(obj: JsonObject): Thread {
        val rawId = obj["_id"]?.jsonPrimitive?.content
            ?: obj["id"]?.jsonPrimitive?.content
        val id = rawId?.takeIf { it.isNotBlank() } ?: ""

        val participants = extractStringArray(obj, "participants", "participantIds", "members")

        val lastMessage = obj["lastMessage"]?.jsonObject?.let { parseMessage(it) }

        // Backend enriches unread as a plain integer; fall back to 0 on any parse issue
        val unreadCount = try {
            obj["unreadCount"]?.jsonPrimitive?.intOrNull
                ?: obj["unread"]?.jsonPrimitive?.intOrNull
                ?: 0
        } catch (_: Exception) { 0 }

        val updatedAt = obj["updatedAt"]?.jsonPrimitive?.content
            ?: obj["updated_at"]?.jsonPrimitive?.content
            ?: obj["lastMessageAt"]?.jsonPrimitive?.content

        val listing = obj["listing"]?.jsonObject?.let { parseListing(it) }

        val opponent = obj["opponent"]?.jsonObject?.let { parseUser(it) }
            ?: obj["participant"]?.jsonObject?.let { parseUser(it) }

        // Use lastMessageText from thread if no embedded lastMessage object
        val resolvedLastMessage = lastMessage ?: obj["lastMessageText"]?.jsonPrimitive?.content?.let { text ->
            if (text.isNotBlank()) Message(id = "", text = text, senderId = "", timestamp = updatedAt) else null
        }

        return Thread(
            id = id,
            participants = participants,
            lastMessage = resolvedLastMessage,
            unreadCount = unreadCount,
            updatedAt = updatedAt,
            listing = listing,
            opponent = opponent
        )
    }
    
    /**
     * Tolerant thread parser - extracts only essential fields
     */
    private fun parseTolerantThread(obj: JsonObject): Thread? {
        val id = obj["_id"]?.jsonPrimitive?.content
            ?: obj["id"]?.jsonPrimitive?.content
            ?: return null // ID is required
        
        return Thread(
            id = id,
            participants = extractStringArray(obj, "participants", "participantIds"),
            lastMessage = null,
            unreadCount = 0,
            updatedAt = null,
            listing = null,
            opponent = null
        )
    }
    
    private fun parseMessagesResponse(responseBody: String): MessagesResult {
        val jsonElement = json.parseToJsonElement(responseBody)
        
        // Messages may be array or wrapper
        val messagesArray = when {
            jsonElement is JsonArray -> jsonElement.toList()
            jsonElement is JsonObject -> {
                val data = jsonElement.jsonObject["data"]
                when (data) {
                    is JsonArray -> data.toList()
                    is JsonObject -> findArrayField(data.jsonObject, "messages", "items", "data")
                    else -> findArrayField(jsonElement.jsonObject, "messages", "items", "data")
                }
            }
            else -> null
        }
        
        val messages = messagesArray?.mapNotNull { element ->
            try {
                parseMessage(element.jsonObject)
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse message")
                null
            }
        } ?: emptyList()
        
        val hasMore = when (jsonElement) {
            is JsonObject -> try { jsonElement["hasMore"]?.jsonPrimitive?.booleanOrNull ?: false } catch (_: Exception) { false }
            else -> false
        }
        
        return MessagesResult(messages, hasMore)
    }
    
    private fun parseMessage(obj: JsonObject): Message {
        // Debug: log available fields for diagnosing blank messages
        val rawText = obj["text"]?.toString()
        val rawContent = obj["content"]?.toString()
        val rawMessage = obj["message"]?.toString()
        val rawSenderId = obj["senderId"]?.toString()
        val rawSender = obj["sender"]?.toString()
        android.util.Log.d("InboxRepo", "parseMessage: keys=${obj.keys.take(15)} text=$rawText content=$rawContent message=$rawMessage senderId=$rawSenderId sender=${rawSender?.take(60)}")

        return Message(
            id = obj["_id"]?.jsonPrimitive?.content
                ?: obj["id"]?.jsonPrimitive?.content
                ?: "",
            text = obj["text"]?.jsonPrimitive?.content
                ?: obj["content"]?.jsonPrimitive?.content
                ?: obj["message"]?.jsonPrimitive?.content
                ?: obj["body"]?.jsonPrimitive?.content
                ?: "",
            senderId = try {
                val senderElement = obj["senderId"] ?: obj["sender"] ?: obj["from"]
                when (senderElement) {
                    is JsonObject -> senderElement["_id"]?.jsonPrimitive?.content
                        ?: senderElement["id"]?.jsonPrimitive?.content ?: ""
                    else -> senderElement?.jsonPrimitive?.content ?: ""
                }
            } catch (_: Exception) { "" },
            timestamp = obj["createdAt"]?.jsonPrimitive?.content
                ?: obj["timestamp"]?.jsonPrimitive?.content
                ?: obj["created_at"]?.jsonPrimitive?.content,
            attachments = extractStringArray(obj, "attachments"),
            isRead = try {
                obj["read"]?.jsonPrimitive?.booleanOrNull
                    ?: obj["isRead"]?.jsonPrimitive?.booleanOrNull
                    ?: false
            } catch (_: Exception) { false }
        )
    }
    
    private fun parseMessageFromResponse(responseBody: String): Message {
        val jsonElement = json.parseToJsonElement(responseBody)
        val jsonObject = jsonElement.jsonObject
        val data = jsonObject["data"]?.jsonObject ?: jsonObject
        return parseMessage(data)
    }
    
    private fun parseUnreadCounts(responseBody: String): UnreadCounts {
        val jsonElement = json.parseToJsonElement(responseBody)
        val jsonObject = jsonElement.jsonObject
        val data = jsonObject["data"]?.jsonObject ?: jsonObject
        
        return UnreadCounts(
            guestUnread = try {
                data["guestUnread"]?.jsonPrimitive?.intOrNull
                    ?: data["guest"]?.jsonPrimitive?.intOrNull
                    ?: 0
            } catch (_: Exception) { 0 },
            hostUnread = try {
                data["hostUnread"]?.jsonPrimitive?.intOrNull
                    ?: data["host"]?.jsonPrimitive?.intOrNull
                    ?: 0
            } catch (_: Exception) { 0 }
        )
    }

    @VisibleForTesting
    internal fun parseThreadsResponseForTest(responseBody: String): ThreadsResult =
        parseThreadsResponse(responseBody)

    /**
     * Parse legacy chat_controller response: { data: { messages: [...] } }
     * Delegates to the same tolerant parser since it handles nested formats.
     */
    private fun parseLegacyMessagesResponse(responseBody: String): MessagesResult =
        parseMessagesResponse(responseBody)

    /**
     * Parse message_controller response: { data: [...messages] }
     * The data field is a flat array of message documents.
     */
    private fun parseLegacyFlatMessagesResponse(responseBody: String): MessagesResult =
        parseMessagesResponse(responseBody)

    @VisibleForTesting
    internal fun parseMessagesResponseForTest(responseBody: String): MessagesResult =
        parseMessagesResponse(responseBody)

    @VisibleForTesting
    internal fun parseUnreadCountsForTest(responseBody: String): UnreadCounts =
        parseUnreadCounts(responseBody)
    
    private fun parseListing(obj: JsonObject): ThreadListing {
        return ThreadListing(
            id = obj["_id"]?.jsonPrimitive?.content
                ?: obj["id"]?.jsonPrimitive?.content
                ?: "",
            title = obj["title"]?.jsonPrimitive?.content
                ?: obj["name"]?.jsonPrimitive?.content
                ?: "",
            imageUrl = obj["image"]?.jsonPrimitive?.content
                ?: obj["imageUrl"]?.jsonPrimitive?.content
                ?: obj["thumbnail"]?.jsonPrimitive?.content
        )
    }
    
    private fun parseUser(obj: JsonObject): ThreadUser {
        // Backend uses snake_case: first_name, last_name, profilePic (array or string)
        val firstName = obj["first_name"]?.jsonPrimitive?.content
            ?: obj["firstName"]?.jsonPrimitive?.content ?: ""
        val lastName = obj["last_name"]?.jsonPrimitive?.content
            ?: obj["lastName"]?.jsonPrimitive?.content ?: ""
        val fullName = "$firstName $lastName".trim()

        val name = obj["name"]?.jsonPrimitive?.content
            ?: obj["displayName"]?.jsonPrimitive?.content
            ?: fullName.ifEmpty { "User" }

        // profilePic may be a string or array of strings; also try avatarUrl
        val avatar = try {
            obj["profilePic"]?.jsonPrimitive?.content
        } catch (_: Exception) {
            try { obj["profilePic"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content } catch (_: Exception) { null }
        }
            ?: obj["avatarUrl"]?.jsonPrimitive?.content
            ?: obj["avatar"]?.jsonPrimitive?.content
            ?: obj["profileImage"]?.jsonPrimitive?.content

        return ThreadUser(
            id = obj["_id"]?.jsonPrimitive?.content
                ?: obj["id"]?.jsonPrimitive?.content
                ?: "",
            name = name,
            avatar = avatar
        )
    }
    
    companion object {
        private const val MAX_UPLOAD_DIMENSION = 1280
        private const val JPEG_QUALITY = 80
    }

    // Utility methods

    private fun findArrayField(obj: JsonObject, vararg keys: String): List<JsonElement>? {
        for (key in keys) {
            try {
                obj[key]?.jsonArray?.let { return it.toList() }
            } catch (e: Exception) {
                // Not an array, try next key
            }
        }
        return null
    }
    
    private fun findAnyArrayInJson(element: JsonElement): List<JsonElement>? {
        when (element) {
            is JsonArray -> return element.toList()
            is JsonObject -> {
                // Check common keys first
                for (key in listOf("data", "threads", "items", "messages")) {
                    element[key]?.let { 
                        findAnyArrayInJson(it)?.let { arr -> return arr }
                    }
                }
                // Check all values
                for ((_, value) in element) {
                    findAnyArrayInJson(value)?.let { return it }
                }
            }
            else -> {}
        }
        return null
    }
    
    private fun extractStringArray(obj: JsonObject, vararg keys: String): List<String> {
        for (key in keys) {
            try {
                obj[key]?.jsonArray?.let { arr ->
                    return arr.mapNotNull { it.jsonPrimitive.content }
                }
            } catch (e: Exception) {
                // Not an array or not strings, try next key
            }
        }
        return emptyList()
    }
    
}

// Result classes

data class ThreadsResult(
    val threads: List<Thread>,
    val nextCursor: String?,
    val hasMore: Boolean
)

data class MessagesResult(
    val messages: List<Message>,
    val hasMore: Boolean
)

data class ThreadListing(
    val id: String,
    val title: String,
    val imageUrl: String?
)

data class ThreadUser(
    val id: String,
    val name: String,
    val avatar: String?
)


