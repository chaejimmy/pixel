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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val apiService: PaceDreamApiService,
    private val messageDao: MessageDao
) {

    fun getChatMessages(chatId: String): Flow<Result<List<MessageModel>>> {
        return flow {
            try {
                val response = apiService.getChatMessages(chatId)
                if (response.isSuccessful) {
                    val messages = response.body()?.data?.map { resp ->
                        MessageModel(
                            id = resp.id,
                            chatId = chatId,
                            senderId = resp.senderId,
                            content = resp.text.ifBlank { resp.content },
                            messageType = resp.messageType ?: if (resp.attachments.isNotEmpty()) "IMAGE" else "TEXT",
                            attachments = resp.attachments.map { it.toModel() },
                            isRead = resp.isRead,
                            timestamp = resp.createdAt,
                            createdAt = resp.createdAt,
                            status = resp.status
                        )
                    } ?: emptyList()

                    // Cache to local database
                    messages.forEach { messageDao.insertMessage(it.asEntity()) }

                    emit(Result.Success(messages))
                } else {
                    emit(Result.Error(Exception("Failed to load messages: ${response.message()}")))
                }
            } catch (e: Exception) {
                // Fall back to cached messages (use first() to get a single snapshot
                // instead of collect, which would never complete on Room's infinite Flow)
                val cached = messageDao.getChatMessages(chatId).first()
                emit(Result.Success(cached.map { it.asExternalModel() }))
            }
        }
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
        return try {
            val response = apiService.sendMessage(chatId, message)
            if (response.isSuccessful) {
                messageDao.insertMessage(message.asEntity())
                Result.Success(message)
            } else {
                Result.Error(Exception("Failed to send message: ${response.message()}"))
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to send message in chat $chatId")
            Result.Error(e)
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
            val parts = mutableListOf<MultipartBody.Part>()

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
                parts.add(MultipartBody.Part.createFormData("images", fileName, requestBody))
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
            Result.Error(e)
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
            }
            Result.Success(Unit)
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

    suspend fun refreshUserChats(userId: String): Result<Unit> {
        return try {
            val response = apiService.getUserChats(userId)
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
