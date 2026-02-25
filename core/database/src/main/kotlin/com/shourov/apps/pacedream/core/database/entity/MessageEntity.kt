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

package com.shourov.apps.pacedream.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shourov.apps.pacedream.model.MessageModel

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val messageType: String = "TEXT",
    val attachmentUrl: String? = null,
    val isRead: Boolean = false,
    val timestamp: String = "",
    val createdAt: String = "",

    // Legacy columns (kept for migration compatibility)
    val profilePic: Int? = null,
    val userName: String? = null,
    val messageTime: String? = null,
    val message: String? = null,
    val newMessageCount: Int? = null
)

fun MessageEntity.asExternalModel(): MessageModel {
    return MessageModel(
        id = messageId,
        chatId = chatId,
        senderId = senderId,
        receiverId = receiverId,
        content = content.ifEmpty { message ?: "" },
        messageType = messageType,
        attachmentUrl = attachmentUrl,
        isRead = isRead || (newMessageCount == 0),
        timestamp = timestamp.ifEmpty { messageTime ?: "" },
        createdAt = createdAt,
        profilePic = profilePic,
        userName = userName,
        messageTime = messageTime,
        message = message,
        newMessageCount = newMessageCount
    )
}

fun MessageModel.asEntity(): MessageEntity {
    return MessageEntity(
        messageId = id.ifEmpty { "${chatId}_${timestamp}" },
        chatId = chatId,
        senderId = senderId,
        receiverId = receiverId,
        content = content,
        messageType = messageType,
        attachmentUrl = attachmentUrl,
        isRead = isRead,
        timestamp = timestamp,
        createdAt = createdAt,
        profilePic = profilePic,
        userName = userName,
        messageTime = messageTime,
        message = message,
        newMessageCount = newMessageCount
    )
}
