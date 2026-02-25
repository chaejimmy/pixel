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

import com.shourov.apps.pacedream.core.common.result.Result
import com.shourov.apps.pacedream.core.database.dao.MessageDao
import com.shourov.apps.pacedream.core.database.entity.asEntity
import com.shourov.apps.pacedream.core.database.entity.asExternalModel
import com.shourov.apps.pacedream.core.network.services.PaceDreamApiService
import com.shourov.apps.pacedream.model.MessageModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val apiService: PaceDreamApiService,
    private val messageDao: MessageDao
) {

    fun getUserMessages(userId: String): Flow<Result<List<MessageModel>>> {
        return messageDao.getMessagesByUser(userId).map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    fun getChatMessages(chatId: String): Flow<Result<List<MessageModel>>> {
        return messageDao.getMessagesByChatId(chatId).map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    fun getUnreadMessages(userId: String): Flow<Result<List<MessageModel>>> {
        return messageDao.getUnreadMessages(userId).map { entities ->
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
                // Save to local database
                messageDao.insertMessage(message.asEntity())
                Result.Success(message)
            } else {
                Result.Error(Exception("Failed to send message: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createChat(userId: String, otherUserId: String): Result<String> {
        return try {
            val chatData = mapOf(
                "userId" to userId,
                "otherUserId" to otherUserId
            )
            val response = apiService.createChat(chatData)
            if (response.isSuccessful) {
                val chatId = response.body()?.data?.id ?: ""
                Result.Success(chatId)
            } else {
                Result.Error(Exception("Failed to create chat: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        return try {
            messageDao.markMessagesAsRead(chatId, userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun refreshChatMessages(chatId: String): Result<Unit> {
        return try {
            val response = apiService.getChatMessages(chatId)
            if (response.isSuccessful) {
                val messageResponses = response.body()?.data ?: emptyList()
                val messageEntities = messageResponses.map { apiMsg ->
                    MessageModel(
                        id = apiMsg.id,
                        chatId = apiMsg.chatId,
                        senderId = apiMsg.senderId,
                        content = apiMsg.text,
                        isRead = apiMsg.isRead,
                        createdAt = apiMsg.createdAt,
                        timestamp = apiMsg.createdAt
                    ).asEntity()
                }
                messageDao.insertMessages(messageEntities)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to refresh messages: ${response.message()}"))
            }
        } catch (e: Exception) {
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
            Result.Error(e)
        }
    }
}
