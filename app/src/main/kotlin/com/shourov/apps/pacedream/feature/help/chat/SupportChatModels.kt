package com.shourov.apps.pacedream.feature.help.chat

import com.shourov.apps.pacedream.feature.help.HelpCenterCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ============================================================================
// Backend taxonomy (mirrors the web app /v1/support/chat/* endpoints).
// ============================================================================

/**
 * Category keys accepted by the backend. The mobile-native [HelpCenterCategory]
 * is richer; every help-center category maps to one of these.
 */
enum class SupportCategory(val key: String) {
    Booking("booking"),
    Payment("payment"),
    Refund("refund"),
    Account("account"),
    Safety("safety"),
    General("general"),
    ;

    companion object {
        fun fromKey(key: String?): SupportCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: General
    }
}

/** Map a Help Center category to the backend taxonomy. */
fun HelpCenterCategory.toSupportCategory(): SupportCategory = when (this) {
    HelpCenterCategory.MessageHost     -> SupportCategory.Booking
    HelpCenterCategory.PaymentsRefunds -> SupportCategory.Payment
    HelpCenterCategory.BookingIssues   -> SupportCategory.Booking
    HelpCenterCategory.AccountHelp     -> SupportCategory.Account
    HelpCenterCategory.SafetyConcern   -> SupportCategory.Safety
    HelpCenterCategory.HelpArticles    -> SupportCategory.General
}

// ============================================================================
// Domain model
// ============================================================================

enum class SupportSessionStatus(val key: String) {
    Open("open"),
    PendingAdmin("pending_admin"),
    Resolved("resolved"),
    Closed("closed"),
    Unknown("unknown"),
    ;

    val isComposerEnabled: Boolean
        get() = this == Open || this == PendingAdmin || this == Unknown

    val pillLabel: String
        get() = when (this) {
            Open         -> "AI assistant"
            PendingAdmin -> "Queued for a human"
            Resolved     -> "Resolved"
            Closed       -> "Closed"
            Unknown      -> "Open"
        }

    companion object {
        fun fromKey(key: String?): SupportSessionStatus =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Unknown
    }
}

enum class SupportSender(val key: String) {
    User("user"),
    Ai("ai"),
    Admin("admin"),
    System("system"),
    Unknown("unknown"),
    ;

    companion object {
        fun fromKey(key: String?): SupportSender =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Unknown
    }
}

data class SupportMessage(
    val id: String,
    val sender: SupportSender,
    val content: String,
    val createdAtEpochMs: Long,
    /** Local-only: true while the message is in flight (optimistic UI). */
    val isPending: Boolean = false,
    /** Local-only: true if a send failed and the user can retry. */
    val isFailed: Boolean = false,
)

data class SupportSession(
    val sessionId: String,
    val status: SupportSessionStatus,
)

// ============================================================================
// Wire models — request bodies
// ============================================================================

@Serializable
internal data class StartSessionRequest(
    val category: String,
    val message: String? = null,
    val guestName: String? = null,
    val guestEmail: String? = null,
)

@Serializable
internal data class SendMessageRequest(
    val sessionId: String,
    val content: String,
    val guestName: String? = null,
    val guestEmail: String? = null,
)

@Serializable
internal data class EscalateRequest(
    val sessionId: String,
)

// ============================================================================
// Response parser — tolerant of envelope variations
// ============================================================================

internal object SupportChatJsonParser {
    /**
     * Backend may return one of:
     *   { success, data: { sessionId/id, status, messages: [...] } }
     *   { sessionId/id, status, messages: [...] }
     *   { session: { ... }, messages: [...] }
     *
     * We unwrap to the inner payload defensively.
     */
    fun parseStart(json: Json, body: String): StartResult? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val payload = unwrap(root)
        val session = parseSession(payload) ?: payload["session"]?.jsonObjectOrNull?.let(::parseSession)
        ?: return@runCatching null
        val messages = (payload["messages"] ?: root["messages"])?.let { parseMessages(it) }.orEmpty()
        StartResult(session, messages)
    }.getOrNull()

    fun parseSendMessage(json: Json, body: String): SendMessageResult? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val payload = unwrap(root)
        val message = (payload["message"] ?: root["message"])?.jsonObjectOrNull?.let(::parseMessage)
        val session = parseSession(payload) ?: payload["session"]?.jsonObjectOrNull?.let(::parseSession)
        SendMessageResult(message, session)
    }.getOrNull()

    fun parseMessages(json: Json, body: String): MessagesResult? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val payload = unwrap(root)
        val messages = (payload["messages"] ?: root["messages"])?.let { parseMessages(it) }.orEmpty()
        val session = parseSession(payload) ?: payload["session"]?.jsonObjectOrNull?.let(::parseSession)
        MessagesResult(messages, session)
    }.getOrNull()

    private fun unwrap(root: JsonObject): JsonObject =
        root["data"]?.jsonObjectOrNull ?: root

    private fun parseSession(obj: JsonObject): SupportSession? {
        val id = obj["sessionId"]?.jsonPrimitive?.contentOrNull
            ?: obj["_id"]?.jsonPrimitive?.contentOrNull
            ?: obj["id"]?.jsonPrimitive?.contentOrNull
            ?: return null
        val status = SupportSessionStatus.fromKey(
            obj["status"]?.jsonPrimitive?.contentOrNull
        )
        return SupportSession(id, status)
    }

    private fun parseMessages(element: JsonElement): List<SupportMessage> =
        element.jsonArray.mapNotNull { it.jsonObjectOrNull?.let(::parseMessage) }

    private fun parseMessage(obj: JsonObject): SupportMessage {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull
            ?: obj["_id"]?.jsonPrimitive?.contentOrNull
            ?: java.util.UUID.randomUUID().toString()
        val sender = SupportSender.fromKey(obj["sender"]?.jsonPrimitive?.contentOrNull)
        val content = (obj["content"] ?: obj["message"] ?: obj["body"] ?: obj["text"])
            ?.jsonPrimitive?.contentOrNull.orEmpty()
        val createdAt = parseTimestamp(obj["createdAt"])
            ?: parseTimestamp(obj["created_at"])
            ?: parseTimestamp(obj["timestamp"])
            ?: System.currentTimeMillis()
        return SupportMessage(
            id = id,
            sender = sender,
            content = content,
            createdAtEpochMs = createdAt,
        )
    }

    private fun parseTimestamp(element: JsonElement?): Long? {
        if (element == null) return null
        val prim = element as? JsonPrimitive ?: return null
        // ISO-8601 string or epoch millis number.
        prim.contentOrNull?.let { s ->
            if (s.isBlank()) return null
            // Try epoch first
            s.toLongOrNull()?.let { return it }
            return runCatching {
                java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli()
            }.recoverCatching {
                java.time.Instant.parse(s).toEpochMilli()
            }.getOrNull()
        }
        return null
    }
}

internal data class StartResult(val session: SupportSession, val messages: List<SupportMessage>)
internal data class SendMessageResult(val message: SupportMessage?, val session: SupportSession?)
internal data class MessagesResult(val messages: List<SupportMessage>, val session: SupportSession?)

// ============================================================================
// Repository result wrappers (domain layer)
// ============================================================================

data class StartSessionResult(
    val session: SupportSession,
    val messages: List<SupportMessage>,
)

data class SendMessageResultDomain(
    val message: SupportMessage?,
    val session: SupportSession?,
)

data class MessagesResultDomain(
    val messages: List<SupportMessage>,
    val session: SupportSession?,
)

private val JsonElement.jsonObjectOrNull: JsonObject?
    get() = this as? JsonObject
