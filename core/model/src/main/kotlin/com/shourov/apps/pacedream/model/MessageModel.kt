package com.shourov.apps.pacedream.model

data class MessageAttachment(
    val url: String = "",
    val thumbnailUrl: String? = null,
    val name: String? = null,
    val type: String? = null, // "image" | "video"
    val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String? = null
) {
    val isImage: Boolean get() = type == "image" || mimeType?.startsWith("image/") == true
    val isVideo: Boolean get() = type == "video" || mimeType?.startsWith("video/") == true
    val displayUrl: String get() = thumbnailUrl ?: url
}

data class MessageModel(
    // Chat message fields
    val id: String? = null,
    val chatId: String? = null,
    val senderId: String? = null,
    val receiverId: String? = null,
    val content: String = "",
    val messageType: String? = "TEXT", // "TEXT", "IMAGE", "VIDEO"
    val attachmentUrl: String? = null,
    val attachments: List<MessageAttachment> = emptyList(),
    val isRead: Boolean = false,
    val timestamp: String? = null,
    val createdAt: String? = null,
    val status: String? = null, // "sending", "sent", "failed"

    // Legacy inbox list fields (backward compatibility)
    val profilePic: Int? = null,
    val userName: String? = null,
    val messageTime: String? = null,
    val message: String? = null,
    val newMessageCount: Int? = null
) {
    val hasImageAttachments: Boolean
        get() = attachments.any { it.isImage }

    val imageAttachments: List<MessageAttachment>
        get() = attachments.filter { it.isImage }

    val displayText: String
        get() = content.ifBlank { message ?: "" }
}
