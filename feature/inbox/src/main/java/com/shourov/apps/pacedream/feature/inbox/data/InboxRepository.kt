package com.shourov.apps.pacedream.feature.inbox.data

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import com.shourov.apps.pacedream.feature.inbox.model.Message
import com.shourov.apps.pacedream.feature.inbox.model.Thread
import com.shourov.apps.pacedream.feature.inbox.model.UnreadCounts
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import timber.log.Timber
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
        val url = appConfig.buildApiUrlWithQuery(
            "inbox", "threads",
            queryParams = mapOf(
                "limit" to limit.toString(),
                "mode" to mode,
                "cursor" to cursor
            )
        )
        
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val threadsResult = parseThreadsResponse(result.data)
                    ApiResult.Success(threadsResult)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse threads response")
                    // Try fallback parsing
                    try {
                        val fallbackResult = parseTolerantThreadsResponse(result.data)
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
                    val counts = parseUnreadCounts(result.data)
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
        
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val messagesResult = parseMessagesResponse(result.data)
                    ApiResult.Success(messagesResult)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse messages response")
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse messages", e))
                }
            }
            is ApiResult.Failure -> result
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
        
        val body = buildString {
            append("{")
            append("\"text\":\"${text.escapeJson()}\"")
            if (attachments.isNotEmpty()) {
                append(",\"attachments\":[")
                append(attachments.joinToString(",") { "\"$it\"" })
                append("]")
            }
            append("}")
        }
        
        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val message = parseMessageFromResponse(result.data)
                    ApiResult.Success(message)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse sent message response")
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse message", e))
                }
            }
            is ApiResult.Failure -> result
        }
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
        val body = buildString {
            append("{")
            append("\"participantId\":\"$opponentId\"")
            listingId?.let { append(",\"listingId\":\"$it\"") }
            append("}")
        }
        
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
        
        val data = jsonObject["data"]?.jsonObject ?: jsonObject
        val threadsArray = findArrayField(data, "threads", "items", "data")
        
        val threads = threadsArray?.mapNotNull { element ->
            try {
                parseThread(element.jsonObject)
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse thread")
                null
            }
        } ?: emptyList()
        
        val nextCursor = data["nextCursor"]?.jsonPrimitive?.content
            ?: data["cursor"]?.jsonPrimitive?.content
        
        val hasMore = data["hasMore"]?.jsonPrimitive?.boolean
            ?: (nextCursor != null)
        
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
                null
            }
        } ?: emptyList()
        
        return ThreadsResult(threads, null, false)
    }
    
    private fun parseThread(obj: JsonObject): Thread {
        val id = obj["_id"]?.jsonPrimitive?.content
            ?: obj["id"]?.jsonPrimitive?.content
            ?: ""
        
        val participants = extractStringArray(obj, "participants", "participantIds")
        
        val lastMessage = obj["lastMessage"]?.jsonObject?.let { parseMessage(it) }
        
        val unreadCount = obj["unreadCount"]?.jsonPrimitive?.int
            ?: obj["unread"]?.jsonPrimitive?.int
            ?: 0
        
        val updatedAt = obj["updatedAt"]?.jsonPrimitive?.content
            ?: obj["updated_at"]?.jsonPrimitive?.content
        
        val listing = obj["listing"]?.jsonObject?.let { parseListing(it) }
        
        val opponent = obj["opponent"]?.jsonObject?.let { parseUser(it) }
            ?: obj["participant"]?.jsonObject?.let { parseUser(it) }
        
        return Thread(
            id = id,
            participants = participants,
            lastMessage = lastMessage,
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
            is JsonObject -> jsonElement["hasMore"]?.jsonPrimitive?.boolean ?: false
            else -> false
        }
        
        return MessagesResult(messages, hasMore)
    }
    
    private fun parseMessage(obj: JsonObject): Message {
        return Message(
            id = obj["_id"]?.jsonPrimitive?.content
                ?: obj["id"]?.jsonPrimitive?.content
                ?: "",
            text = obj["text"]?.jsonPrimitive?.content
                ?: obj["content"]?.jsonPrimitive?.content
                ?: obj["body"]?.jsonPrimitive?.content
                ?: "",
            senderId = obj["senderId"]?.jsonPrimitive?.content
                ?: obj["sender"]?.jsonPrimitive?.content
                ?: obj["from"]?.jsonPrimitive?.content
                ?: "",
            timestamp = obj["createdAt"]?.jsonPrimitive?.content
                ?: obj["timestamp"]?.jsonPrimitive?.content
                ?: obj["created_at"]?.jsonPrimitive?.content,
            attachments = extractStringArray(obj, "attachments"),
            isRead = obj["read"]?.jsonPrimitive?.boolean
                ?: obj["isRead"]?.jsonPrimitive?.boolean
                ?: false
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
            guestUnread = data["guestUnread"]?.jsonPrimitive?.int
                ?: data["guest"]?.jsonPrimitive?.int
                ?: 0,
            hostUnread = data["hostUnread"]?.jsonPrimitive?.int
                ?: data["host"]?.jsonPrimitive?.int
                ?: 0
        )
    }
    
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
        return ThreadUser(
            id = obj["_id"]?.jsonPrimitive?.content
                ?: obj["id"]?.jsonPrimitive?.content
                ?: "",
            name = obj["name"]?.jsonPrimitive?.content
                ?: obj["displayName"]?.jsonPrimitive?.content
                ?: "${obj["firstName"]?.jsonPrimitive?.content ?: ""} ${obj["lastName"]?.jsonPrimitive?.content ?: ""}".trim(),
            avatar = obj["avatar"]?.jsonPrimitive?.content
                ?: obj["profileImage"]?.jsonPrimitive?.content
        )
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
    
    private fun String.escapeJson(): String {
        return this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
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

