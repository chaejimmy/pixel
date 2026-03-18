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
import com.shourov.apps.pacedream.model.MessageAttachment
import com.shourov.apps.pacedream.model.MessageModel

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String = "",
    val chatId: String? = null,
    val senderId: String? = null,
    val receiverId: String? = null,
    val content: String = "",
    val messageType: String? = "TEXT",
    val attachmentUrl: String? = null,
    // Attachments stored as JSON string for Room compatibility
    val attachmentsJson: String? = null,
    val isRead: Boolean = false,
    val timestamp: String? = null,
    val createdAt: String? = null,
    val status: String? = null,
    // Legacy fields
    val profilePic: Int? = null,
    val userName: String? = null,
    val messageTime: String? = null,
    val message: String? = null,
    val newMessageCount: Int? = null
)

fun MessageEntity.asExternalModel(): MessageModel {
    return MessageModel(
        id = id,
        chatId = chatId,
        senderId = senderId,
        receiverId = receiverId,
        content = content,
        messageType = messageType,
        attachmentUrl = attachmentUrl,
        attachments = parseAttachmentsJson(attachmentsJson),
        isRead = isRead,
        timestamp = timestamp,
        createdAt = createdAt,
        status = status,
        profilePic = profilePic,
        userName = userName,
        messageTime = messageTime,
        message = message,
        newMessageCount = newMessageCount
    )
}

fun MessageModel.asEntity(): MessageEntity {
    return MessageEntity(
        id = id ?: "",
        chatId = chatId,
        senderId = senderId,
        receiverId = receiverId,
        content = content,
        messageType = messageType,
        attachmentUrl = attachmentUrl,
        attachmentsJson = serializeAttachments(attachments),
        isRead = isRead,
        timestamp = timestamp,
        createdAt = createdAt,
        status = status,
        profilePic = profilePic,
        userName = userName,
        messageTime = messageTime,
        message = message,
        newMessageCount = newMessageCount
    )
}

// Simple JSON serialization for attachments (avoids adding Moshi/Gson dependency to database module)
private fun serializeAttachments(attachments: List<MessageAttachment>): String? {
    if (attachments.isEmpty()) return null
    return buildString {
        append("[")
        attachments.forEachIndexed { index, att ->
            if (index > 0) append(",")
            append("{")
            append("\"url\":\"${att.url.escapeJson()}\"")
            att.thumbnailUrl?.let { append(",\"thumbnailUrl\":\"${it.escapeJson()}\"") }
            att.name?.let { append(",\"name\":\"${it.escapeJson()}\"") }
            att.type?.let { append(",\"type\":\"${it.escapeJson()}\"") }
            att.size?.let { append(",\"size\":$it") }
            att.width?.let { append(",\"width\":$it") }
            att.height?.let { append(",\"height\":$it") }
            att.mimeType?.let { append(",\"mimeType\":\"${it.escapeJson()}\"") }
            append("}")
        }
        append("]")
    }
}

private fun parseAttachmentsJson(json: String?): List<MessageAttachment> {
    if (json.isNullOrBlank() || json == "[]") return emptyList()
    val result = mutableListOf<MessageAttachment>()
    // Simple JSON array parser for our known structure
    try {
        var i = 0
        while (i < json.length) {
            val objStart = json.indexOf('{', i)
            if (objStart == -1) break
            val objEnd = json.indexOf('}', objStart)
            if (objEnd == -1) break
            val objStr = json.substring(objStart + 1, objEnd)
            result.add(parseAttachmentObject(objStr))
            i = objEnd + 1
        }
    } catch (_: Exception) { }
    return result
}

private fun parseAttachmentObject(obj: String): MessageAttachment {
    fun extractString(key: String): String? {
        val pattern = "\"$key\":\""
        val start = obj.indexOf(pattern)
        if (start == -1) return null
        val valueStart = start + pattern.length
        val valueEnd = obj.indexOf('"', valueStart)
        if (valueEnd == -1) return null
        return obj.substring(valueStart, valueEnd).unescapeJson()
    }
    fun extractLong(key: String): Long? {
        val pattern = "\"$key\":"
        val start = obj.indexOf(pattern)
        if (start == -1) return null
        val valueStart = start + pattern.length
        val valueEnd = obj.indexOfFirst(valueStart) { it == ',' || it == '}' || it == '"' }
        if (valueEnd == -1) return null
        return obj.substring(valueStart, valueEnd).trim().toLongOrNull()
    }
    fun extractInt(key: String): Int? = extractLong(key)?.toInt()

    return MessageAttachment(
        url = extractString("url") ?: "",
        thumbnailUrl = extractString("thumbnailUrl"),
        name = extractString("name"),
        type = extractString("type"),
        size = extractLong("size"),
        width = extractInt("width"),
        height = extractInt("height"),
        mimeType = extractString("mimeType")
    )
}

private fun String.indexOfFirst(startIndex: Int, predicate: (Char) -> Boolean): Int {
    for (i in startIndex until length) {
        if (predicate(this[i])) return i
    }
    return -1
}

private fun String.escapeJson(): String = replace("\\", "\\\\").replace("\"", "\\\"")
private fun String.unescapeJson(): String = replace("\\\"", "\"").replace("\\\\", "\\")
