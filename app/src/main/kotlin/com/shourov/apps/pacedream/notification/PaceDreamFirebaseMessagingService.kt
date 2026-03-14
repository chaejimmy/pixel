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

/**
 * Firebase Cloud Messaging service matching iOS push notification handling (iOS parity).
 *
 * Handles all notification types that iOS supports via OneSignal/APNs:
 * - Booking notifications (request, confirmed, cancelled, receipt, refund)
 * - Payment notifications (received, failed, payout)
 * - Message notifications (new message)
 * - Review notifications
 * - Social notifications (friend request, roommate)
 * - Property notifications (approved, rejected, inquiry)
 * - Check-in reminders, extend prompts, overtime warnings
 * - Split booking notifications
 * - Security alerts, verification
 * - Chargeback/dispute notifications
 * - Support, marketing, system updates
 *
 * Key behavior:
 * - Data-only payloads: parsed into NotificationData and displayed via PaceDreamNotificationService
 * - Notification+data payloads: only processes data to avoid duplicate display
 *   (FCM auto-displays notification payload when app is in background)
 */
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

        val data = remoteMessage.data
        val hasDataPayload = data.isNotEmpty()
        val hasNotificationPayload = remoteMessage.notification != null

        Timber.d(
            "FCM payload: hasData=%s, hasNotification=%s, type=%s",
            hasDataPayload, hasNotificationPayload, data["type"] ?: "unknown"
        )

        if (hasDataPayload) {
            // Parse structured notification data (iOS parity)
            val notificationData = NotificationData.fromMap(data)

            // Merge title/body from notification payload if data payload lacks them
            val mergedData = if (hasNotificationPayload) {
                notificationData.copy(
                    title = notificationData.title ?: remoteMessage.notification?.title,
                    message = notificationData.message ?: remoteMessage.notification?.body
                )
            } else {
                notificationData
            }

            // Display the notification
            notificationService.showNotification(mergedData)
        } else if (hasNotificationPayload) {
            // Notification-only payload (no data) — display as general notification.
            // This is rare; most backend messages include a data payload.
            notificationService.showGeneralNotification(
                title = remoteMessage.notification?.title ?: "PaceDream",
                message = remoteMessage.notification?.body ?: ""
            )
        }

        // Note: We do NOT separately handle remoteMessage.notification here.
        // When the app is in the background, FCM auto-displays the notification payload.
        // When in the foreground, we display via our NotificationService above.
        // This prevents the duplicate notification bug that existed before.
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token refreshed")
        sendTokenToServer(token)
    }

    /**
     * iOS PR #201 parity: register FCM token with backend at /push-devices.
     * Sends fcmToken, platform, and deviceInfo for push notification routing.
     * Fire-and-forget: deduplicates by token+userId pair.
     */
    private fun sendTokenToServer(token: String) {
        if (!tokenStorage.hasTokens()) {
            Timber.d("Skipping FCM token registration: user not authenticated")
            return
        }

        // Deduplication: skip if same token+user already sent
        val deduplicationKey = "${token}_${tokenStorage.userId}"
        if (lastRegisteredKey == deduplicationKey) {
            Timber.d("FCM token already registered for this user, skipping")
            return
        }

        serviceScope.launch {
            try {
                // Try the iOS-parity endpoint first (/push-devices)
                val url = appConfig.buildApiUrl("push-devices")
                val deviceInfo = mapOf(
                    "model" to android.os.Build.MODEL,
                    "systemVersion" to "Android ${android.os.Build.VERSION.RELEASE}",
                    "appVersion" to try {
                        applicationContext.packageManager.getPackageInfo(packageName, 0).versionName ?: ""
                    } catch (_: Exception) { "" }
                )
                val body = json.encodeToString(
                    RegisterDeviceRequest.serializer(),
                    RegisterDeviceRequest(
                        fcmToken = token,
                        platform = "android",
                        deviceInfo = deviceInfo
                    )
                )
                when (val result = apiClient.post(url, body, includeAuth = true)) {
                    is ApiResult.Success -> {
                        Timber.d("FCM token registered with server via /push-devices")
                        lastRegisteredKey = deduplicationKey
                    }
                    is ApiResult.Failure -> {
                        // Fallback to legacy endpoint
                        val legacyUrl = appConfig.buildApiUrl("notifications", "register-device")
                        val legacyBody = json.encodeToString(
                            LegacyRegisterDeviceRequest.serializer(),
                            LegacyRegisterDeviceRequest(token = token, platform = "android")
                        )
                        when (val legacyResult = apiClient.post(legacyUrl, legacyBody, includeAuth = true)) {
                            is ApiResult.Success -> {
                                Timber.d("FCM token registered via legacy endpoint")
                                lastRegisteredKey = deduplicationKey
                            }
                            is ApiResult.Failure -> {
                                Timber.e("Failed to register FCM token: ${legacyResult.error.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to send FCM token to server")
            }
        }
    }

    @Serializable
    private data class RegisterDeviceRequest(
        val fcmToken: String,
        val platform: String,
        val deviceInfo: Map<String, String> = emptyMap()
    )

    @Serializable
    private data class LegacyRegisterDeviceRequest(
        val token: String,
        val platform: String
    )

    companion object {
        @Volatile
        private var lastRegisteredKey: String? = null
    }
}
