package com.shourov.apps.pacedream.model

/**
 * Message model for chat/messaging - matches backend API schema
 */
data class MessageModel(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val messageType: String = "TEXT",
    val attachmentUrl: String? = null,
    val isRead: Boolean = false,
    val timestamp: String = "",
    val createdAt: String = "",

    // Legacy fields kept for backward compatibility with local DB seeded data
    val profilePic: Int? = null,
    val userName: String? = null,
    val messageTime: String? = null,
    val message: String? = null,
    val newMessageCount: Int? = null
)
