package com.shourov.apps.pacedream.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.TokenStorage
import com.shourov.apps.pacedream.core.network.config.AppConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PaceDreamFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationService: PaceDreamNotificationService

    @Inject
    lateinit var apiClient: ApiClient

    @Inject
    lateinit var appConfig: AppConfig

    @Inject
    lateinit var tokenStorage: TokenStorage

    @Inject
    lateinit var json: Json

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        // Only register if user is authenticated
        if (!tokenStorage.hasTokens()) {
            Timber.d("Skipping FCM token registration: user not authenticated")
            return
        }

        serviceScope.launch {
            try {
                val url = appConfig.buildApiUrl("notifications", "register-device")
                val body = json.encodeToString(
                    RegisterDeviceRequest.serializer(),
                    RegisterDeviceRequest(
                        token = token,
                        platform = "android"
                    )
                )
                when (val result = apiClient.post(url, body, includeAuth = true)) {
                    is ApiResult.Success -> {
                        Timber.d("FCM token registered with server")
                    }
                    is ApiResult.Failure -> {
                        Timber.e("Failed to register FCM token: ${result.error.message}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to send FCM token to server")
            }
        }
    }

    @Serializable
    private data class RegisterDeviceRequest(
        val token: String,
        val platform: String
    )
}
