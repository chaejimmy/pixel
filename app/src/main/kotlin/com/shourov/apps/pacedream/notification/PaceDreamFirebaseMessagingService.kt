package com.shourov.apps.pacedream.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.TokenStorage
import com.shourov.apps.pacedream.BuildConfig
import com.shourov.apps.pacedream.core.network.config.AppConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
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

    @Inject
    lateinit var fcmTokenStore: FcmTokenStore

    @Inject
    lateinit var oneSignalService: OneSignalService

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        try {
            handleMessage(remoteMessage)
        } catch (e: Exception) {
            Timber.e(e, "FCM onMessageReceived crashed, swallowed to prevent service death")
        }
    }

    private fun handleMessage(remoteMessage: RemoteMessage) {
        Timber.d("FCM message received from: %s", remoteMessage.from)

        val rawData = remoteMessage.data
        val hasDataPayload = rawData.isNotEmpty()
        val hasNotificationPayload = remoteMessage.notification != null

        Timber.d(
            "FCM payload: hasData=%s, hasNotification=%s, type=%s",
            hasDataPayload, hasNotificationPayload, rawData["type"] ?: "unknown"
        )

        // OneSignal wraps custom data inside a "custom" JSON key with an "a" (additional data)
        // sub-object. Flatten it into the top-level map so NotificationData.fromMap() can parse it.
        val data = flattenOneSignalData(rawData)

        if (hasDataPayload) {
            // Parse structured notification data (iOS parity)
            val notificationData = NotificationData.fromMap(data)

            // Merge title/body from notification payload if data payload lacks them.
            // Also pull from OneSignal's "alert"/"title" keys which live at the top level.
            val mergedData = if (hasNotificationPayload || notificationData.title == null || notificationData.message == null) {
                notificationData.copy(
                    title = notificationData.title
                        ?: remoteMessage.notification?.title
                        ?: data["title"]
                        ?: data["alert_title"],
                    message = notificationData.message
                        ?: remoteMessage.notification?.body
                        ?: data["alert"]
                        ?: data["body"]
                )
            } else {
                notificationData
            }

            // Auth guard: suppress payout/payment setup notifications for logged-out users.
            // This prevents the bug where unauthenticated users receive "Set up payments"
            // or payout-related notifications.
            if (mergedData.isPayoutRelated && !tokenStorage.hasTokens()) {
                Timber.d("Suppressing payout notification for unauthenticated user: type=%s", mergedData.type)
                return
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
        try {
            Timber.d("FCM token refreshed")
            sendTokenToServer(token)
        } catch (e: Exception) {
            Timber.e(e, "FCM onNewToken crashed, swallowed to prevent service death")
        }
    }

    /**
     * Flatten OneSignal's nested data format into a flat map.
     *
     * OneSignal sends custom data as:
     *   { "custom": "{\"a\":{\"type\":\"booking_confirmed\",\"bookingId\":\"123\"},\"i\":\"notif-id\"}", "alert": "...", "title": "..." }
     *
     * This extracts the "a" (additional data) sub-object and merges it into the top-level map
     * so that NotificationData.fromMap() can read keys like "type", "bookingId", etc.
     */
    private fun flattenOneSignalData(data: Map<String, String>): Map<String, String> {
        val customJson = data["custom"] ?: return data
        return try {
            val custom = JSONObject(customJson)
            val additional = custom.optJSONObject("a") ?: return data
            val merged = data.toMutableMap()
            additional.keys().forEach { key ->
                // Only add if not already present at top level (top-level takes precedence)
                if (!merged.containsKey(key)) {
                    merged[key] = additional.optString(key, "")
                }
            }
            merged
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse OneSignal custom data")
            data
        }
    }

    /**
     * iOS PR #201 parity: register FCM token with backend at /push-devices.
     * Sends fcmToken, platform, and deviceInfo for push notification routing.
     * Fire-and-forget: deduplicates by token+userId pair.
     *
     * iOS parity: uses FcmTokenStore for persistent deduplication across process
     * restarts, matching PushDeviceRegistrar + PushTokenStore on iOS.
     */
    private fun sendTokenToServer(token: String) {
        // Persist token (iOS parity: PushTokenStore.save)
        fcmTokenStore.saveToken(token)

        if (!tokenStorage.hasTokens()) {
            Timber.d("Skipping FCM token registration: user not authenticated")
            return
        }

        // Persistent deduplication (iOS parity: PushDeviceRegistrar dedup)
        if (fcmTokenStore.isAlreadyRegistered(token, tokenStorage.userId)) {
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

                // iOS parity: include OneSignal subscription ID so the backend
                // can route pushes via OneSignal without creating a duplicate player.
                val onesignalId = oneSignalService.getSubscriptionId()
                if (onesignalId != null) {
                    Timber.d("Including OneSignal subscriptionId=%s", onesignalId)
                }

                val body = json.encodeToString(
                    RegisterDeviceRequest.serializer(),
                    RegisterDeviceRequest(
                        fcmToken = token,
                        platform = "android",
                        deviceInfo = deviceInfo,
                        onesignalPlayerId = onesignalId,
                        // iOS parity: debug builds use sandbox APNs; inform backend.
                        sandbox = if (BuildConfig.DEBUG) true else null
                    )
                )
                when (val result = apiClient.post(url, body, includeAuth = true)) {
                    is ApiResult.Success -> {
                        Timber.d("FCM token registered with server via /push-devices")
                        fcmTokenStore.markRegistered(token, tokenStorage.userId)
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
                                fcmTokenStore.markRegistered(token, tokenStorage.userId)
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
        val deviceInfo: Map<String, String> = emptyMap(),
        val onesignalPlayerId: String? = null,
        val sandbox: Boolean? = null
    )

    @Serializable
    private data class LegacyRegisterDeviceRequest(
        val token: String,
        val platform: String
    )

    companion object
}
