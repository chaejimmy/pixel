package com.shourov.apps.pacedream.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PaceDreamFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationService: PaceDreamNotificationService

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Timber.d("FCM message received from: %s", remoteMessage.from)
        Timber.d("FCM message type: %s", remoteMessage.data["type"] ?: "unknown")

        // Handle data payload
        remoteMessage.data.let { data ->
            val type = data["type"]
            when (type) {
                "message" -> handleMessageNotification(data)
                "booking" -> handleBookingNotification(data)
                "general" -> handleGeneralNotification(data)
                else -> handleDefaultNotification(remoteMessage)
            }
        }

        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            handleNotificationPayload(notification.title, notification.body)
        }
    }

    private fun handleMessageNotification(data: Map<String, String>) {
        val chatId = data["chat_id"] ?: return
        val senderName = data["sender_name"] ?: "Unknown"
        val message = data["message"] ?: ""
        val chatName = data["chat_name"]

        notificationService.showMessageNotification(
            chatId = chatId,
            senderName = senderName,
            message = message,
            chatName = chatName
        )
    }

    private fun handleBookingNotification(data: Map<String, String>) {
        val bookingId = data["booking_id"] ?: return
        val title = data["title"] ?: "Booking Update"
        val message = data["message"] ?: ""
        val propertyName = data["property_name"] ?: "Property"

        notificationService.showBookingNotification(
            bookingId = bookingId,
            title = title,
            message = message,
            propertyName = propertyName
        )
    }

    private fun handleGeneralNotification(data: Map<String, String>) {
        val title = data["title"] ?: "PaceDream"
        val message = data["message"] ?: ""

        notificationService.showGeneralNotification(
            title = title,
            message = message
        )
    }

    private fun handleDefaultNotification(remoteMessage: RemoteMessage) {
        val title = remoteMessage.data["title"] ?: "PaceDream"
        val message = remoteMessage.data["message"] ?: remoteMessage.notification?.body ?: ""

        notificationService.showGeneralNotification(
            title = title,
            message = message
        )
    }

    private fun handleNotificationPayload(title: String?, body: String?) {
        notificationService.showGeneralNotification(
            title = title ?: "PaceDream",
            message = body ?: ""
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token refreshed")

        // Send token to your server
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        // TODO: Implement token sending to your server
        // This could be done through your API service
        Timber.d("FCM token sent to server")
    }
}
