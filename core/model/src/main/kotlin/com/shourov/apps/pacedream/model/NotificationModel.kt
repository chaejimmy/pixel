package com.shourov.apps.pacedream.model

/**
 * Notification model - matches backend API schema
 */
data class NotificationModel(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notificationMessage: String? = null,
    val type: String = "",
    val isRead: Boolean = false,
    val createdAt: String = "",
    val actionUrl: String? = null,
    val actionId: String? = null,

    // Legacy fields kept for backward compatibility
    val profilePic: Int? = null,
    val notificationTime: String? = null,
    val notificationImage: Int? = null,
    val notificationCategory: String? = null
)
