package com.shourov.apps.pacedream.notification

/**
 * Notification type enum matching iOS NotificationType (iOS parity).
 *
 * Maps backend `type` field values to strongly-typed categories
 * for routing and channel selection.
 */
enum class NotificationType(val value: String) {
    // Booking
    BOOKING_REQUEST("booking_request"),
    BOOKING_CONFIRMED("booking_confirmed"),
    BOOKING_CANCELLED("booking_cancelled"),
    BOOKING_RECEIPT("booking_receipt"),
    BOOKING_REFUND("booking_refund"),

    // Payments
    PAYMENT_RECEIVED("payment_received"),
    PAYMENT_FAILED("payment_failed"),
    PAYOUT_INITIATED("payout_initiated"),
    PAYOUT_FAILED("payout_failed"),

    // Reviews
    REVIEW_RECEIVED("review_received"),

    // Messaging
    MESSAGE_RECEIVED("message_received"),

    // Social
    FRIEND_REQUEST("friend_request"),
    FRIEND_ACCEPTED("friend_accepted"),
    ROOMMATE_REQUEST("roommate_request"),
    ROOMMATE_ACCEPTED("roommate_accepted"),

    // Listings
    PROPERTY_APPROVED("property_approved"),
    PROPERTY_REJECTED("property_rejected"),
    LISTING_PAUSED("listing_paused"),
    LISTING_REPORTED("listing_reported"),
    LISTING_INQUIRY("listing_inquiry"),

    // Time-based / check-in
    CHECKIN_REMINDER("checkin_reminder"),
    EXTEND_PROMPT("extend_prompt"),
    OVERTIME_WARNING("overtime_warning"),
    SESSION_ENDED("session_ended"),

    // Split
    SPLIT_INVITE("split_invite"),
    SPLIT_PAYMENT_NEEDED("split_payment_needed"),
    SPLIT_COMPLETED("split_completed"),
    SPLIT_CREDIT("split_credit"),

    // Security / Account
    SECURITY_ALERT("security_alert"),
    VERIFICATION_STATUS("verification_status"),

    // Chargebacks / Disputes
    CHARGEBACK_CREATED("chargeback_created"),
    CHARGEBACK_RESOLVED("chargeback_resolved"),

    // Support / Trust & Safety
    SUPPORT_UPDATE("support_update"),
    ACCOUNT_WARNING("account_warning"),
    ACCOUNT_SUSPENDED("account_suspended"),
    CONTENT_REMOVED("content_removed"),

    // System
    MAINTENANCE_REQUEST("maintenance_request"),
    SYSTEM_UPDATE("system_update"),
    MARKETING("marketing"),
    REMINDER("reminder"),
    ALERT("alert"),

    // Catch-all
    UNKNOWN("unknown");

    companion object {
        fun fromString(value: String?): NotificationType {
            if (value == null) return UNKNOWN
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

/**
 * Structured notification data matching iOS NotificationData (iOS parity).
 *
 * Parsed from FCM data payload with tolerant key lookup supporting
 * both camelCase and snake_case variants.
 */
data class NotificationData(
    val screen: String? = null,
    val type: NotificationType = NotificationType.UNKNOWN,
    val title: String? = null,
    val message: String? = null,
    val bookingId: String? = null,
    val propertyId: String? = null,
    val userId: String? = null,
    val hostId: String? = null,
    val messageId: String? = null,
    val threadId: String? = null,
    val senderId: String? = null,
    val senderName: String? = null,
    val chatName: String? = null,
    val propertyName: String? = null,
    val reviewId: String? = null,
    val splitId: String? = null,
    val disputeId: String? = null,
    val payoutId: String? = null,
    val ticketId: String? = null,
    val promotionId: String? = null,
    val searchId: String? = null, // iOS parity
    val amount: Double? = null,
    val currency: String? = null,
    val deepLink: String? = null,
    val actionUrl: String? = null,
    val metadata: Map<String, String>? = null // iOS parity
) {
    /**
     * Determine the notification channel based on the notification type.
     */
    val channelId: String
        get() = when (type) {
            NotificationType.MESSAGE_RECEIVED -> PaceDreamNotificationService.CHANNEL_ID_MESSAGES

            NotificationType.BOOKING_REQUEST,
            NotificationType.BOOKING_CONFIRMED,
            NotificationType.BOOKING_CANCELLED,
            NotificationType.BOOKING_RECEIPT,
            NotificationType.BOOKING_REFUND,
            NotificationType.CHECKIN_REMINDER,
            NotificationType.EXTEND_PROMPT,
            NotificationType.OVERTIME_WARNING,
            NotificationType.SESSION_ENDED -> PaceDreamNotificationService.CHANNEL_ID_BOOKINGS

            NotificationType.PAYMENT_RECEIVED,
            NotificationType.PAYMENT_FAILED,
            NotificationType.PAYOUT_INITIATED,
            NotificationType.PAYOUT_FAILED -> PaceDreamNotificationService.CHANNEL_ID_PAYMENTS

            NotificationType.REVIEW_RECEIVED -> PaceDreamNotificationService.CHANNEL_ID_REVIEWS

            NotificationType.FRIEND_REQUEST,
            NotificationType.FRIEND_ACCEPTED,
            NotificationType.ROOMMATE_REQUEST,
            NotificationType.ROOMMATE_ACCEPTED -> PaceDreamNotificationService.CHANNEL_ID_SOCIAL

            NotificationType.PROPERTY_APPROVED,
            NotificationType.PROPERTY_REJECTED,
            NotificationType.LISTING_PAUSED,
            NotificationType.LISTING_REPORTED,
            NotificationType.LISTING_INQUIRY -> PaceDreamNotificationService.CHANNEL_ID_PROPERTY

            NotificationType.SECURITY_ALERT,
            NotificationType.ACCOUNT_WARNING,
            NotificationType.ACCOUNT_SUSPENDED,
            NotificationType.VERIFICATION_STATUS -> PaceDreamNotificationService.CHANNEL_ID_SECURITY

            NotificationType.SPLIT_INVITE,
            NotificationType.SPLIT_PAYMENT_NEEDED,
            NotificationType.SPLIT_COMPLETED,
            NotificationType.SPLIT_CREDIT -> PaceDreamNotificationService.CHANNEL_ID_BOOKINGS

            NotificationType.CHARGEBACK_CREATED,
            NotificationType.CHARGEBACK_RESOLVED -> PaceDreamNotificationService.CHANNEL_ID_PAYMENTS

            NotificationType.SUPPORT_UPDATE,
            NotificationType.CONTENT_REMOVED -> PaceDreamNotificationService.CHANNEL_ID_GENERAL

            NotificationType.MARKETING -> PaceDreamNotificationService.CHANNEL_ID_MARKETING

            NotificationType.MAINTENANCE_REQUEST,
            NotificationType.SYSTEM_UPDATE,
            NotificationType.REMINDER,
            NotificationType.ALERT,
            NotificationType.UNKNOWN -> PaceDreamNotificationService.CHANNEL_ID_GENERAL
        }

    companion object {
        /**
         * Parse FCM data payload into NotificationData with tolerant key lookup.
         * Supports both camelCase and snake_case field names from the backend.
         */
        fun fromMap(data: Map<String, String>): NotificationData {
            return NotificationData(
                screen = data["screen"] ?: data["notification_type"],
                type = NotificationType.fromString(data["type"] ?: data["notification_type"]),
                title = data["title"],
                message = data["message"] ?: data["body"],
                bookingId = data["bookingId"] ?: data["booking_id"],
                propertyId = data["propertyId"] ?: data["property_id"]
                    ?: data["listingId"] ?: data["listing_id"],
                userId = data["userId"] ?: data["user_id"],
                hostId = data["hostId"] ?: data["host_id"],
                messageId = data["messageId"] ?: data["message_id"],
                threadId = data["threadId"] ?: data["thread_id"]
                    ?: data["conversationId"] ?: data["conversation_id"]
                    ?: data["chat_id"],
                senderId = data["senderId"] ?: data["sender_id"],
                senderName = data["senderName"] ?: data["sender_name"],
                chatName = data["chatName"] ?: data["chat_name"],
                propertyName = data["propertyName"] ?: data["property_name"],
                reviewId = data["reviewId"] ?: data["review_id"],
                splitId = data["splitId"] ?: data["split_id"],
                disputeId = data["disputeId"] ?: data["dispute_id"],
                payoutId = data["payoutId"] ?: data["payout_id"],
                ticketId = data["ticketId"] ?: data["ticket_id"],
                promotionId = data["promotionId"] ?: data["promotion_id"],
                searchId = data["searchId"] ?: data["search_id"],
                amount = (data["amount"] ?: data["total"])?.toDoubleOrNull(),
                currency = data["currency"],
                deepLink = data["deep_link"] ?: data["deepLink"],
                actionUrl = data["action_url"] ?: data["actionUrl"]
            )
        }
    }
}
