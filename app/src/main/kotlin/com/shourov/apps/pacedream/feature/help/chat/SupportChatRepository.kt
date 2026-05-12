package com.shourov.apps.pacedream.feature.help.chat

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin HTTP facade for the customer support chat feature.
 *
 * Mirrors the web product at `/v1/support/chat/*`:
 *  - POST /support/chat/start
 *  - POST /support/chat/message
 *  - GET  /support/chat/{sessionId}/messages
 *  - POST /support/escalate
 *
 * Realtime (Ably) parity is a separate, optional layer — the ViewModel polls
 * /messages while the screen is visible so the experience degrades gracefully
 * on devices where Ably is not wired up yet.
 */
@Singleton
class SupportChatRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json,
) {

    suspend fun startSession(
        category: SupportCategory,
        message: String?,
        guestName: String?,
        guestEmail: String?,
    ): Result<StartSessionResult> {
        val url = appConfig.buildApiUrl("support", "chat", "start")
        val body = json.encodeToString(
            StartSessionRequest.serializer(),
            StartSessionRequest(
                category = category.key,
                message = message?.takeIf { it.isNotBlank() },
                guestName = guestName?.takeIf { it.isNotBlank() },
                guestEmail = guestEmail?.takeIf { it.isNotBlank() },
            )
        )
        return when (val result = apiClient.post(url = url, body = body, includeAuth = true)) {
            is ApiResult.Success -> {
                val parsed = SupportChatJsonParser.parseStart(json, result.data)
                if (parsed == null) {
                    Result.failure(SupportChatException.Generic)
                } else {
                    Result.success(StartSessionResult(parsed.session, parsed.messages))
                }
            }
            is ApiResult.Failure -> Result.failure(result.error.toSupportException())
        }
    }

    suspend fun sendMessage(
        sessionId: String,
        content: String,
        guestName: String?,
        guestEmail: String?,
    ): Result<SendMessageResultDomain> {
        val url = appConfig.buildApiUrl("support", "chat", "message")
        val body = json.encodeToString(
            SendMessageRequest.serializer(),
            SendMessageRequest(
                sessionId = sessionId,
                content = content,
                guestName = guestName?.takeIf { it.isNotBlank() },
                guestEmail = guestEmail?.takeIf { it.isNotBlank() },
            )
        )
        return when (val result = apiClient.post(url = url, body = body, includeAuth = true)) {
            is ApiResult.Success -> {
                val parsed = SupportChatJsonParser.parseSendMessage(json, result.data)
                Result.success(
                    SendMessageResultDomain(
                        message = parsed?.message,
                        session = parsed?.session,
                    )
                )
            }
            is ApiResult.Failure -> Result.failure(result.error.toSupportException())
        }
    }

    suspend fun fetchMessages(sessionId: String): Result<MessagesResultDomain> {
        val url = appConfig.buildApiUrl("support", "chat", sessionId, "messages")
        return when (val result = apiClient.get(url = url, includeAuth = true)) {
            is ApiResult.Success -> {
                val parsed = SupportChatJsonParser.parseMessages(json, result.data)
                Result.success(
                    MessagesResultDomain(
                        messages = parsed?.messages.orEmpty(),
                        session = parsed?.session,
                    )
                )
            }
            is ApiResult.Failure -> Result.failure(result.error.toSupportException())
        }
    }

    suspend fun escalate(sessionId: String): Result<Unit> {
        val url = appConfig.buildApiUrl("support", "escalate")
        val body = json.encodeToString(
            EscalateRequest.serializer(),
            EscalateRequest(sessionId)
        )
        return when (val result = apiClient.post(url = url, body = body, includeAuth = true)) {
            is ApiResult.Success -> Result.success(Unit)
            is ApiResult.Failure -> Result.failure(result.error.toSupportException())
        }
    }

    private fun ApiError.toSupportException(): SupportChatException = when (this) {
        is ApiError.Unauthorized      -> SupportChatException.SessionExpired
        is ApiError.Forbidden         -> SupportChatException.Restricted
        is ApiError.NotFound          -> SupportChatException.SessionMissing
        is ApiError.RateLimited       -> SupportChatException.TooFast
        is ApiError.Timeout,
        is ApiError.NetworkError      -> SupportChatException.Offline
        is ApiError.ServiceUnavailable,
        is ApiError.HtmlResponse,
        is ApiError.ServerError,
        is ApiError.DecodingError     -> SupportChatException.Generic
        else                          -> SupportChatException.Generic
    }
}
