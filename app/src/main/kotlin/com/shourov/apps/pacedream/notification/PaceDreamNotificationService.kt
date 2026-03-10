package com.shourov.apps.pacedream.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.shourov.apps.pacedream.MainActivity
import com.shourov.apps.pacedream.R
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification display service matching iOS notification presentation (iOS parity).
 *
 * Channels match iOS notification categories:
 * - Messages (HIGH) - chat and messaging
 * - Bookings (HIGH) - booking updates, confirmations, check-in reminders
 * - Payments (DEFAULT) - payments, payouts, refunds
 * - Reviews (DEFAULT) - review received
 * - Social (DEFAULT) - friend requests, roommate requests
 * - Property (DEFAULT) - listing updates, approvals
 * - Security (HIGH) - security alerts, account warnings
 * - Marketing (LOW) - promotions, deals
 * - General (LOW) - system updates, maintenance
 */
@Singleton
class PaceDreamNotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Channel IDs matching iOS notification categories
        const val CHANNEL_ID_MESSAGES = "messages"
        const val CHANNEL_ID_BOOKINGS = "bookings"
        const val CHANNEL_ID_PAYMENTS = "payments"
        const val CHANNEL_ID_REVIEWS = "reviews"
        const val CHANNEL_ID_SOCIAL = "social"
        const val CHANNEL_ID_PROPERTY = "property"
        const val CHANNEL_ID_SECURITY = "security"
        const val CHANNEL_ID_MARKETING = "marketing"
        const val CHANNEL_ID_GENERAL = "general"

        // Channel group IDs
        private const val GROUP_ID_ACTIVITY = "activity"
        private const val GROUP_ID_ACCOUNT = "account"
        private const val GROUP_ID_OTHER = "other"

        // Notification ID base offsets for unique IDs per category
        private const val ID_BASE_MESSAGE = 1000
        private const val ID_BASE_BOOKING = 2000
        private const val ID_BASE_PAYMENT = 3000
        private const val ID_BASE_REVIEW = 4000
        private const val ID_BASE_SOCIAL = 5000
        private const val ID_BASE_PROPERTY = 6000
        private const val ID_BASE_SECURITY = 7000
        private const val ID_BASE_MARKETING = 8000
        private const val ID_BASE_GENERAL = 9000
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create channel groups
            notificationManager.createNotificationChannelGroups(
                listOf(
                    NotificationChannelGroup(GROUP_ID_ACTIVITY, "Activity"),
                    NotificationChannelGroup(GROUP_ID_ACCOUNT, "Account"),
                    NotificationChannelGroup(GROUP_ID_OTHER, "Other")
                )
            )

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_MESSAGES,
                    "Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "New messages and chat notifications"
                    group = GROUP_ID_ACTIVITY
                    enableVibration(true)
                    enableLights(true)
                },
                NotificationChannel(
                    CHANNEL_ID_BOOKINGS,
                    "Bookings",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Booking updates, confirmations, and check-in reminders"
                    group = GROUP_ID_ACTIVITY
                    enableVibration(true)
                    enableLights(true)
                },
                NotificationChannel(
                    CHANNEL_ID_PAYMENTS,
                    "Payments",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Payment receipts, payouts, and refunds"
                    group = GROUP_ID_ACCOUNT
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_ID_REVIEWS,
                    "Reviews",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "New reviews and review requests"
                    group = GROUP_ID_ACTIVITY
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_ID_SOCIAL,
                    "Social",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Friend requests and roommate updates"
                    group = GROUP_ID_ACTIVITY
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_ID_PROPERTY,
                    "Property",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Listing approvals, inquiries, and updates"
                    group = GROUP_ID_ACTIVITY
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_ID_SECURITY,
                    "Security",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Security alerts, account warnings, and verification"
                    group = GROUP_ID_ACCOUNT
                    enableVibration(true)
                    enableLights(true)
                },
                NotificationChannel(
                    CHANNEL_ID_MARKETING,
                    "Promotions",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Deals, promotions, and marketing offers"
                    group = GROUP_ID_OTHER
                },
                NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "System updates, maintenance, and general notifications"
                    group = GROUP_ID_OTHER
                }
            )

            notificationManager.createNotificationChannels(channels)
        }
    }

    /**
     * Check if POST_NOTIFICATIONS permission is granted (required on Android 13+).
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Show a notification from parsed NotificationData.
     * This is the primary entry point — routes to the correct display method
     * based on notification type and channel.
     */
    fun showNotification(data: NotificationData) {
        if (!hasNotificationPermission()) {
            Timber.w("POST_NOTIFICATIONS permission not granted; dropping notification")
            return
        }

        val title = data.title ?: getDefaultTitle(data.type)
        val message = data.message ?: ""

        val pendingIntent = createPendingIntent(data)
        val notificationId = getNotificationId(data)
        val priority = getPriority(data.channelId)

        val builder = NotificationCompat.Builder(context, data.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Add sub-text for context
        getSubText(data)?.let { builder.setSubText(it) }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to show notification — permission denied")
        }
    }

    /**
     * Legacy method for backward compatibility — creates a general notification.
     */
    fun showGeneralNotification(
        title: String,
        message: String,
        actionIntent: Intent? = null
    ) {
        if (!hasNotificationPermission()) return

        val intent = actionIntent ?: Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(ID_BASE_GENERAL + title.hashCode(), notification)
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to show general notification")
        }
    }

    /**
     * Legacy method for backward compatibility.
     */
    fun showMessageNotification(
        chatId: String,
        senderName: String,
        message: String,
        chatName: String? = null
    ) {
        showNotification(
            NotificationData(
                type = NotificationType.MESSAGE_RECEIVED,
                title = chatName ?: senderName,
                message = message,
                threadId = chatId,
                senderName = senderName,
                chatName = chatName
            )
        )
    }

    /**
     * Legacy method for backward compatibility.
     */
    fun showBookingNotification(
        bookingId: String,
        title: String,
        message: String,
        propertyName: String
    ) {
        showNotification(
            NotificationData(
                type = NotificationType.BOOKING_CONFIRMED,
                title = title,
                message = message,
                bookingId = bookingId,
                propertyName = propertyName
            )
        )
    }

    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    // ── Private helpers ─────────────────────────────────

    /**
     * Create a PendingIntent that carries all notification data as extras.
     * When tapped, MainActivity receives the intent and routes via NotificationRouter.
     */
    private fun createPendingIntent(data: NotificationData): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Carry structured data for NotificationRouter
            putExtra("from_notification", true)
            data.screen?.let { putExtra("screen", it) }
            data.type.value.let { putExtra("type", it) }
            data.bookingId?.let { putExtra("booking_id", it) }
            data.propertyId?.let { putExtra("property_id", it) }
            data.threadId?.let { putExtra("chat_id", it) }
            data.reviewId?.let { putExtra("review_id", it) }
            data.splitId?.let { putExtra("split_id", it) }
            data.disputeId?.let { putExtra("dispute_id", it) }
            data.payoutId?.let { putExtra("payout_id", it) }
            data.ticketId?.let { putExtra("ticket_id", it) }
            data.deepLink?.let { putExtra("deep_link", it) }
        }

        val requestCode = getNotificationId(data)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getNotificationId(data: NotificationData): Int {
        val base = when (data.type) {
            NotificationType.MESSAGE_RECEIVED -> ID_BASE_MESSAGE
            NotificationType.BOOKING_REQUEST,
            NotificationType.BOOKING_CONFIRMED,
            NotificationType.BOOKING_CANCELLED,
            NotificationType.BOOKING_RECEIPT,
            NotificationType.BOOKING_REFUND,
            NotificationType.CHECKIN_REMINDER,
            NotificationType.EXTEND_PROMPT,
            NotificationType.OVERTIME_WARNING,
            NotificationType.SESSION_ENDED -> ID_BASE_BOOKING

            NotificationType.PAYMENT_RECEIVED,
            NotificationType.PAYMENT_FAILED,
            NotificationType.PAYOUT_INITIATED,
            NotificationType.PAYOUT_FAILED,
            NotificationType.CHARGEBACK_CREATED,
            NotificationType.CHARGEBACK_RESOLVED -> ID_BASE_PAYMENT

            NotificationType.REVIEW_RECEIVED -> ID_BASE_REVIEW

            NotificationType.FRIEND_REQUEST,
            NotificationType.FRIEND_ACCEPTED,
            NotificationType.ROOMMATE_REQUEST,
            NotificationType.ROOMMATE_ACCEPTED -> ID_BASE_SOCIAL

            NotificationType.PROPERTY_APPROVED,
            NotificationType.PROPERTY_REJECTED,
            NotificationType.LISTING_PAUSED,
            NotificationType.LISTING_REPORTED,
            NotificationType.LISTING_INQUIRY -> ID_BASE_PROPERTY

            NotificationType.SECURITY_ALERT,
            NotificationType.ACCOUNT_WARNING,
            NotificationType.ACCOUNT_SUSPENDED,
            NotificationType.VERIFICATION_STATUS -> ID_BASE_SECURITY

            NotificationType.MARKETING -> ID_BASE_MARKETING

            NotificationType.SPLIT_INVITE,
            NotificationType.SPLIT_PAYMENT_NEEDED,
            NotificationType.SPLIT_COMPLETED,
            NotificationType.SPLIT_CREDIT -> ID_BASE_BOOKING

            else -> ID_BASE_GENERAL
        }

        // Use a unique-ish hash from the primary ID
        val uniqueKey = data.bookingId ?: data.threadId ?: data.propertyId
            ?: data.reviewId ?: data.splitId ?: data.disputeId
            ?: data.title ?: ""
        return base + uniqueKey.hashCode()
    }

    private fun getPriority(channelId: String): Int {
        return when (channelId) {
            CHANNEL_ID_MESSAGES, CHANNEL_ID_BOOKINGS, CHANNEL_ID_SECURITY ->
                NotificationCompat.PRIORITY_HIGH
            CHANNEL_ID_PAYMENTS, CHANNEL_ID_REVIEWS, CHANNEL_ID_SOCIAL, CHANNEL_ID_PROPERTY ->
                NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }
    }

    private fun getDefaultTitle(type: NotificationType): String {
        return when (type) {
            NotificationType.BOOKING_REQUEST -> "New Booking Request"
            NotificationType.BOOKING_CONFIRMED -> "Booking Confirmed"
            NotificationType.BOOKING_CANCELLED -> "Booking Cancelled"
            NotificationType.BOOKING_RECEIPT -> "Booking Receipt"
            NotificationType.BOOKING_REFUND -> "Booking Refund"
            NotificationType.PAYMENT_RECEIVED -> "Payment Received"
            NotificationType.PAYMENT_FAILED -> "Payment Failed"
            NotificationType.PAYOUT_INITIATED -> "Payout Initiated"
            NotificationType.PAYOUT_FAILED -> "Payout Failed"
            NotificationType.REVIEW_RECEIVED -> "New Review"
            NotificationType.MESSAGE_RECEIVED -> "New Message"
            NotificationType.FRIEND_REQUEST -> "Friend Request"
            NotificationType.FRIEND_ACCEPTED -> "Friend Request Accepted"
            NotificationType.ROOMMATE_REQUEST -> "Roommate Request"
            NotificationType.ROOMMATE_ACCEPTED -> "Roommate Request Accepted"
            NotificationType.PROPERTY_APPROVED -> "Listing Approved"
            NotificationType.PROPERTY_REJECTED -> "Listing Rejected"
            NotificationType.LISTING_PAUSED -> "Listing Paused"
            NotificationType.LISTING_REPORTED -> "Listing Reported"
            NotificationType.LISTING_INQUIRY -> "New Inquiry"
            NotificationType.CHECKIN_REMINDER -> "Check-in Reminder"
            NotificationType.EXTEND_PROMPT -> "Extend Your Stay"
            NotificationType.OVERTIME_WARNING -> "Overtime Warning"
            NotificationType.SESSION_ENDED -> "Session Ended"
            NotificationType.SPLIT_INVITE -> "Split Booking Invite"
            NotificationType.SPLIT_PAYMENT_NEEDED -> "Split Payment Needed"
            NotificationType.SPLIT_COMPLETED -> "Split Completed"
            NotificationType.SPLIT_CREDIT -> "Split Credit"
            NotificationType.SECURITY_ALERT -> "Security Alert"
            NotificationType.VERIFICATION_STATUS -> "Verification Update"
            NotificationType.CHARGEBACK_CREATED -> "Dispute Created"
            NotificationType.CHARGEBACK_RESOLVED -> "Dispute Resolved"
            NotificationType.SUPPORT_UPDATE -> "Support Update"
            NotificationType.ACCOUNT_WARNING -> "Account Warning"
            NotificationType.ACCOUNT_SUSPENDED -> "Account Suspended"
            NotificationType.CONTENT_REMOVED -> "Content Removed"
            NotificationType.MAINTENANCE_REQUEST -> "Maintenance Request"
            NotificationType.SYSTEM_UPDATE -> "System Update"
            NotificationType.MARKETING -> "Special Offer"
            NotificationType.REMINDER -> "Reminder"
            NotificationType.ALERT -> "Alert"
            NotificationType.UNKNOWN -> "PaceDream"
        }
    }

    private fun getSubText(data: NotificationData): String? {
        return when (data.type) {
            NotificationType.BOOKING_CONFIRMED,
            NotificationType.BOOKING_CANCELLED,
            NotificationType.BOOKING_RECEIPT,
            NotificationType.BOOKING_REFUND -> data.propertyName

            NotificationType.MESSAGE_RECEIVED -> data.chatName

            NotificationType.PAYMENT_RECEIVED,
            NotificationType.PAYMENT_FAILED -> {
                if (data.amount != null) {
                    val curr = data.currency ?: "USD"
                    "$curr ${"%.2f".format(data.amount)}"
                } else null
            }

            else -> null
        }
    }
}
