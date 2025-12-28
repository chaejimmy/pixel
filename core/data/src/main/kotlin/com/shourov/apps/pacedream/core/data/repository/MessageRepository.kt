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
import com.shourov.apps.pacedream.core.database.entity.MessageEntity
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
                // Extract chat ID from response
                val chatId = "generated_chat_id" // This would come from the response
                Result.Success(chatId)
            } else {
                Result.Error(Exception("Failed to create chat: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun markMessagesAsRead(userName: String): Result<Unit> {
        return try {
            // Update local database
            messageDao.markMessagesAsRead(userName)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun refreshChatMessages(chatId: String): Result<Unit> {
        return try {
            val response = apiService.getChatMessages(chatId)
            if (response.isSuccessful) {
                // Handle response and save to database
                // This would need to be implemented based on your API response structure
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
                // Handle response and save to database
                // This would need to be implemented based on your API response structure
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to refresh chats: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
