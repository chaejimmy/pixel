/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shourov.apps.pacedream.core.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.shourov.apps.pacedream.core.common.result.Result
import com.shourov.apps.pacedream.core.database.dao.MessageDao
import com.shourov.apps.pacedream.core.database.entity.asEntity
import com.shourov.apps.pacedream.core.database.entity.asExternalModel
import com.shourov.apps.pacedream.core.network.model.AttachmentResponse
import com.shourov.apps.pacedream.core.network.services.PaceDreamApiService
import com.shourov.apps.pacedream.model.MessageAttachment
import com.shourov.apps.pacedream.model.MessageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

@Singleton
class MessageRepository @Inject constructor(
    private val apiService: PaceDreamApiService,
    private val messageDao: MessageDao
) {

    fun getChatMessages(chatId: String): Flow<Result<List<MessageModel>>> {
        return flow {
            try {
                // Try modern inbox endpoint first
                val response = apiService.getChatMessages(chatId)
                if (response.isSuccessful) {
                    val messages = response.body()?.items?.map { resp ->
                        MessageModel(
                            id = resp.id,
                            chatId = chatId,
                            senderId = resp.senderId,
                            content = resp.resolvedText,
                            messageType = resp.messageType ?: resp.type ?: if (resp.attachments.isNotEmpty()) "IMAGE" else "TEXT",
                            attachments = resp.attachments.map { it.toModel() },
                            isRead = resp.resolvedIsRead,
                            timestamp = resp.createdAt,
                            createdAt = resp.createdAt,
                            status = resp.status
                        )
                    }?.distinctBy { it.id } ?: emptyList()

                    messages.forEach { messageDao.insertMessage(it.asEntity()) }
                    emit(Result.Success(messages))
                } else {
                    val inboxCode = response.code()
                    // Try legacy: GET /chat/{chatId}/messages (chat_controller)
                    val legacyMessages = fetchFromLegacyEndpoint(chatId)
                    if (legacyMessages != null) {
                        legacyMessages.forEach { messageDao.insertMessage(it.asEntity()) }
                        emit(Result.Success(legacyMessages))
                    } else {
                        // Try second legacy: GET /messages?chatId=xxx (message_controller)
                        val fallbackMessages = fetchFromMessageEndpoint(chatId)
                        if (fallbackMessages != null) {
                            fallbackMessages.forEach { messageDao.insertMessage(it.asEntity()) }
                            emit(Result.Success(fallbackMessages))
                        } else {
                            val cached = messageDao.getChatMessages(chatId).first()
                            if (cached.isNotEmpty()) {
                                emit(Result.Success(cached.map { it.asExternalModel() }.distinctBy { it.id }))
                            } else {
                                // Show actual error details so user can report
                                emit(Result.Error(Exception("All endpoints failed (inbox=$inboxCode). chatId=$chatId")))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                timber.log.Timber.e(e, "getChatMessages exception — falling back to cache")
                val cached = messageDao.getChatMessages(chatId).first()
                emit(Result.Success(cached.map { it.asExternalModel() }.distinctBy { it.id }))
            }
        }.flowOn(Dispatchers.IO)
    }

    /** Fetch messages from the legacy GET /chat/:chatId/messages endpoint.
     *  Uses raw JsonElement to avoid Gson crashes when sender is a string vs object. */
    private suspend fun fetchFromLegacyEndpoint(chatId: String): List<MessageModel>? {
        return try {
            val response = apiService.getChatMessagesLegacy(chatId, chatId)
            if (response.isSuccessful) {
                val json = response.body() ?: return null
                val root = json.asJsonObject ?: return null
                // chat_controller.getMessages wraps messages as:
                // { data: { messages: [...], total, ... } }  (chat_controller)
                // OR { data: [...] } (message_controller)
                val dataElement = root.get("data") ?: return emptyList()
                val messagesArray = when {
                    dataElement.isJsonArray -> dataElement.asJsonArray
                    dataElement.isJsonObject -> dataElement.asJsonObject.getAsJsonArray("messages")
                    else -> null
                } ?: return emptyList()
                messagesArray.mapNotNull { element ->
                    try {
                        val msg = element.asJsonObject
                        val id = msg.get("_id")?.asString ?: return@mapNotNull null
                        // sender can be a string ObjectId or a populated object
                        val senderElement = msg.get("sender")
                        val senderId = when {
                            senderElement == null || senderElement.isJsonNull -> ""
                            senderElement.isJsonObject -> senderElement.asJsonObject.get("_id")?.asString ?: ""
                            senderElement.isJsonPrimitive -> senderElement.asString
                            else -> ""
                        }
                        val content = msg.get("message")?.asString ?: ""
                        val type = msg.get("type")?.asString ?: "text"
                        val createdAt = msg.get("createdAt")?.asString ?: ""
                        MessageModel(
                            id = id,
                            chatId = chatId,
                            senderId = senderId,
                            content = content,
                            messageType = type.uppercase().let { if (it == "TEXT" || it.isBlank()) "TEXT" else it },
                            isRead = msg.get("messageRead")?.asBoolean ?: false,
                            timestamp = createdAt,
                            createdAt = createdAt,
                            status = "sent"
                        )
                    } catch (_: Exception) { null }
                }.distinctBy { it.id }
            } else {
                timber.log.Timber.w("Legacy messages also failed (HTTP %d)", response.code())
                null
            }
        } catch (e: Exception) {
            timber.log.Timber.w(e, "Legacy messages endpoint exception")
            null
        }
    }

    /** Fallback: GET /messages?chatId=xxx (message_controller, flat data array) */
    private suspend fun fetchFromMessageEndpoint(chatId: String): List<MessageModel>? {
        return try {
            val response = apiService.getMessagesFallback(chatId)
            if (response.isSuccessful) {
                val json = response.body() ?: return null
                val root = json.asJsonObject ?: return null
                val dataArray = root.getAsJsonArray("data") ?: return emptyList()
                dataArray.mapNotNull { element ->
                    try {
                        val msg = element.asJsonObject
                        val id = msg.get("_id")?.asString ?: return@mapNotNull null
                        val senderElement = msg.get("sender")
                        val senderId = when {
                            senderElement == null || senderElement.isJsonNull -> ""
                            senderElement.isJsonObject -> senderElement.asJsonObject.get("_id")?.asString ?: ""
                            senderElement.isJsonPrimitive -> senderElement.asString
                            else -> ""
                        }
                        MessageModel(
                            id = id, chatId = chatId, senderId = senderId,
                            content = msg.get("message")?.asString ?: "",
                            messageType = "TEXT",
                            isRead = msg.get("messageRead")?.asBoolean ?: false,
                            timestamp = msg.get("createdAt")?.asString ?: "",
                            createdAt = msg.get("createdAt")?.asString ?: "",
                            status = "sent"
                        )
                    } catch (_: Exception) { null }
                }.distinctBy { it.id }
            } else null
        } catch (_: Exception) { null }
    }

    fun getUserMessages(userName: String): Flow<Result<List<MessageModel>>> {
        return messageDao.getMessagesByUser(userName).map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    fun getUnreadMessages(): Flow<Result<List<MessageModel>>> {
        return messageDao.getUnreadMessages().map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    fun getLastMessage(): Flow<Result<MessageModel?>> {
        return messageDao.getLastMessage().map { entity ->
            Result.Success(entity?.asExternalModel())
        }
    }

    fun getAllMessages(): Flow<Result<List<MessageModel>>> {
        return messageDao.getAllMessages().map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    suspend fun sendMessage(chatId: String, message: MessageModel): Result<MessageModel> {
        return withContext(Dispatchers.IO) {
            try {
                // Inbox endpoint expects { text: "..." } body
                val messageData = mapOf("text" to message.content)
                val response = apiService.sendMessage(chatId, messageData)
                if (response.isSuccessful) {
                    // Update optimistic message with server-assigned ID
                    val serverId = response.body()?.id
                    val confirmedMessage = if (!serverId.isNullOrBlank()) {
                        message.copy(id = serverId, status = "sent")
                    } else {
                        message.copy(status = "sent")
                    }
                    messageDao.insertMessage(confirmedMessage.asEntity())
                    Result.Success(confirmedMessage)
                } else {
                    val errorMsg = when (response.code()) {
                        429 -> "You're sending messages too quickly. Please wait a moment."
                        403 -> {
                            val errorBody = response.errorBody()?.string()
                            val lower = errorBody?.lowercase() ?: ""
                            when {
                                lower.contains("spam") -> "This message was flagged as spam."
                                lower.contains("restrict") || lower.contains("blocked") ->
                                    "Your account is restricted from sending messages."
                                else -> "You don't have permission to send messages in this chat."
                            }
                        }
                        else -> "Failed to send message: ${response.message()}"
                    }
                    Result.Error(Exception(errorMsg))
                }
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Failed to send message in chat $chatId")
                Result.Error(classifyNetworkException(e))
            }
        }
    }

    /**
     * Upload images to a chat thread as attachments.
     * Compresses and downscales images before upload.
     */
    suspend fun uploadChatMedia(
        context: Context,
        threadId: String,
        imageUris: List<Uri>,
        text: String? = null
    ): Result<MessageModel> {
        return try {
            // Perform all bitmap I/O on a background thread to avoid ANR.
            // BitmapFactory.decodeStream / compress are heavy and must not run
            // on Dispatchers.Main (the default for viewModelScope).
            val parts = withContext(Dispatchers.IO) {
                val result = mutableListOf<MultipartBody.Part>()

                for (uri in imageUris) {
                    val inputStream = context.contentResolver.openInputStream(uri) ?: continue
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (bitmap == null) continue

                    // Downscale to max 1280px
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
                return Result.Error(Exception("No valid images to upload"))
            }

            val textBody = text?.toRequestBody("text/plain".toMediaType())
            val response = apiService.uploadChatMedia(threadId, parts, textBody)

            if (response.isSuccessful) {
                val body = response.body()
                val message = MessageModel(
                    id = body?.id ?: "",
                    chatId = threadId,
                    content = body?.text ?: text ?: "",
                    messageType = "IMAGE",
                    attachments = body?.attachments?.map { it.toModel() } ?: emptyList(),
                    createdAt = body?.createdAt ?: "",
                    status = "sent"
                )
                Result.Success(message)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = when (response.code()) {
                    403 -> "Photos can only be shared after a booking is confirmed"
                    400 -> errorBody ?: "Invalid file type or size"
                    else -> "Failed to upload photos: ${response.message()}"
                }
                Result.Error(Exception(errorMsg))
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to upload chat media to thread $threadId")
            Result.Error(classifyNetworkException(e))
        }
    }

    /**
     * Check if attachments are enabled for a thread.
     */
    suspend fun getAttachmentStatus(threadId: String): Result<Boolean> {
        return try {
            val response = apiService.getAttachmentStatus(threadId)
            if (response.isSuccessful) {
                Result.Success(response.body()?.data?.attachmentsEnabled ?: false)
            } else {
                Result.Success(false)
            }
        } catch (e: Exception) {
            timber.log.Timber.w(e, "Attachment status check failed for thread $threadId")
            Result.Success(false)
        }
    }

    suspend fun createChat(userId: String, otherUserId: String): Result<String> {
        return try {
            val chatData = mapOf("userId" to userId, "otherUserId" to otherUserId)
            val response = apiService.createChat(chatData)
            if (response.isSuccessful) {
                val chatId = response.body()?.data?.id ?: ""
                Result.Success(chatId)
            } else {
                Result.Error(Exception("Failed to create chat: ${response.message()}"))
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to create chat")
            Result.Error(e)
        }
    }

    suspend fun markMessagesAsRead(userName: String): Result<Unit> {
        return try {
            messageDao.markMessagesAsRead(userName)
            Result.Success(Unit)
        } catch (e: Exception) {
            timber.log.Timber.w(e, "Failed to mark messages as read for user $userName")
            Result.Error(e)
        }
    }

    /**
     * Mark all messages in a chat as read on the server.
     * Also updates the local database.
     */
    suspend fun markChatAsReadOnServer(chatId: String): Result<Unit> {
        return try {
            val response = apiService.markChatAsRead(mapOf("chat_id" to chatId))
            if (response.isSuccessful) {
                // Also update local cache
                messageDao.markMessagesAsRead(chatId)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to mark chat as read: ${response.message()}"))
            }
        } catch (e: Exception) {
            timber.log.Timber.w(e, "Failed to mark chat $chatId as read on server")
            Result.Error(e)
        }
    }

    suspend fun refreshChatMessages(chatId: String): Result<Unit> {
        return try {
            val response = apiService.getChatMessages(chatId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to refresh messages: ${response.message()}"))
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to refresh chat messages")
            Result.Error(e)
        }
    }

    /**
     * Fetch the raw chat list JSON from GET /chat/all.
     * Returns the parsed JsonElement so the ViewModel can extract ChatItem data.
     */
    suspend fun fetchChatListRaw(): com.google.gson.JsonElement? {
        return try {
            val response = apiService.getUserChats()
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to fetch chat list")
            null
        }
    }

    suspend fun refreshUserChats(userId: String): Result<Unit> {
        return try {
            val response = apiService.getUserChats()
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to refresh chats: ${response.message()}"))
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to refresh user chats")
            Result.Error(e)
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
     * Wraps raw network exceptions with user-friendly messages.
     * SSL/TLS failures (often caused by device-level KeyMint/SPU errors)
     * and connectivity issues get actionable guidance.
     */
    private fun classifyNetworkException(e: Exception): Exception {
        val message = when {
            e is SSLHandshakeException || e is SSLException ->
                "Secure connection failed. Please restart your device and try again."
            e is SocketTimeoutException ->
                "Connection timed out. Please check your internet and try again."
            e is UnknownHostException ->
                "No internet connection. Please check your network and try again."
            e is java.net.ConnectException ->
                "Unable to reach the server. Please check your connection and try again."
            e.cause is SSLException || e.cause is SSLHandshakeException ->
                "Secure connection failed. Please restart your device and try again."
            else -> return e
        }
        return Exception(message, e)
    }

    companion object {
        private const val MAX_UPLOAD_DIMENSION = 1280
        private const val JPEG_QUALITY = 80
    }
}

private fun AttachmentResponse.toModel() = MessageAttachment(
    url = url,
    thumbnailUrl = thumbnailUrl,
    name = name,
    type = type,
    size = size,
    width = width,
    height = height,
    mimeType = mimeType
)
