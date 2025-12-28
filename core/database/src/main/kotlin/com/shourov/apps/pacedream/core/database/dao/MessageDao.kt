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

package com.shourov.apps.pacedream.core.database.dao

import androidx.room.*
import com.shourov.apps.pacedream.core.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun getMessageById(messageId: Int): Flow<MessageEntity?>

    @Query("SELECT * FROM messages WHERE userName = :userName ORDER BY messageTime ASC")
    fun getMessagesByUser(userName: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE newMessageCount > 0 ORDER BY messageTime DESC")
    fun getUnreadMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY messageTime DESC LIMIT 1")
    fun getLastMessage(): Flow<MessageEntity?>

    @Query("SELECT * FROM messages ORDER BY messageTime DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Int)

    @Query("DELETE FROM messages WHERE userName = :userName")
    suspend fun deleteMessagesByUser(userName: String)

    @Query("UPDATE messages SET newMessageCount = 0 WHERE userName = :userName")
    suspend fun markMessagesAsRead(userName: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}
