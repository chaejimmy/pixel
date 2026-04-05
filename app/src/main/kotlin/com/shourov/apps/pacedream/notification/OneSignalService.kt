package com.shourov.apps.pacedream.notification

import android.content.Context
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.notifications.INotificationWillDisplayEvent
import com.onesignal.notifications.INotificationLifecycleListener
import com.shourov.apps.pacedream.BuildConfig
import com.shourov.apps.pacedream.feature.notification.NotificationRouter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OneSignal push notification service matching iOS OneSignalService.swift (iOS parity).
 *
 * Handles:
 * - SDK initialization with App ID from BuildConfig
 * - External user ID binding for targeted push (with retry logic)
 * - Foreground notification display
 * - Notification tap routing via [NotificationRouter]
 * - Tags for user segmentation
 *
 * iOS parity notes:
 * - Matches OneSignalService.swift initialization and external user ID flow
 * - Retry logic with exponential backoff matches iOS polling + subscription observer
 * - Foreground handler mirrors setNotificationWillShowInForegroundHandler
 * - Tap handler mirrors setNotificationOpenedHandler
 */
@Singleton
class OneSignalService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var externalUserIdBound = false
    private var pendingExternalUserId: String? = null
    private var initialized = false

    /**
     * Initialize OneSignal SDK. Call once from Application.onCreate().
     *
     * iOS parity: matches OneSignalService.start(launchOptions:)
     */
    fun initialize(appId: String) {
        if (appId.isBlank()) {
            Timber.w("[OneSignalService] App ID is blank — push notifications via OneSignal disabled")
            return
        }

        Timber.d("[OneSignalService] Starting OneSignal service...")

        // Set log level (iOS parity: debug=verbose, release=error)
        if (BuildConfig.DEBUG) {
            OneSignal.Debug.logLevel = LogLevel.INFO
        } else {
            OneSignal.Debug.logLevel = LogLevel.ERROR
        }

        // Initialize the SDK
        OneSignal.initWithContext(context, appId)

        // Foreground notification handler (iOS parity: setNotificationWillShowInForegroundHandler)
        OneSignal.Notifications.addForegroundLifecycleListener(object : INotificationLifecycleListener {
            override fun onWillDisplay(event: INotificationWillDisplayEvent) {
                val notificationId = event.notification.notificationId
                Timber.d("[OneSignalService] Foreground notification received: $notificationId")

                // Check if we should suppress for active chat (iOS parity: ActiveChatTracker)
                val additionalData = event.notification.additionalData
                val threadId = additionalData?.optString("threadId")
                    ?: additionalData?.optString("thread_id")
                    ?: additionalData?.optString("chat_id")

                if (!threadId.isNullOrBlank() && ActiveChatTracker.shouldSuppress(threadId)) {
                    Timber.d("[OneSignalService] Suppressing notification for active chat thread: $threadId")
                    event.preventDefault()
                    return
                }

                // Allow display (iOS parity: completion(notification))
                event.notification.display()
            }
        })

        // Tap handler (iOS parity: setNotificationOpenedHandler)
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val notificationId = event.notification.notificationId
                Timber.d("[OneSignalService] Notification tapped: $notificationId")

                val additionalData = event.notification.additionalData
                if (additionalData != null) {
                    Timber.d("[OneSignalService] Additional data: $additionalData")
                    val dataMap = mutableMapOf<String, String>()
                    additionalData.keys().forEach { key ->
                        additionalData.optString(key)?.let { value ->
                            dataMap[key] = value
                        }
                    }
                    NotificationRouter.handleNotification(dataMap)
                }
            }
        })

        // Log initial state
        val subscriptionId = OneSignal.User.pushSubscription.id
        val token = OneSignal.User.pushSubscription.token
        val optedIn = OneSignal.User.pushSubscription.optedIn
        Timber.d(
            "[OneSignalService] Initial state: subscriptionId=%s token=%s optedIn=%s",
            subscriptionId ?: "nil", token ?: "nil", optedIn
        )

        initialized = true
        Timber.d("[OneSignalService] OneSignal service started successfully (AppId=$appId)")
    }

    /**
     * Request push notification permission (Android 13+).
     *
     * iOS parity: matches OneSignalService.requestPermission()
     */
    fun requestPermission() {
        Timber.d("[OneSignalService] Requesting push notification permission...")
        scope.launch {
            val granted = OneSignal.Notifications.requestPermission(false)
            Timber.d("[OneSignalService] Push permission %s", if (granted) "granted" else "denied")
        }
    }

    /**
     * Set external user ID for targeting (fire-and-forget version).
     *
     * iOS parity: matches OneSignalService.setExternalUserId(_:)
     * OneSignal v5 SDK uses login() instead of setExternalUserId().
     * Includes retry logic matching iOS polling + subscription observer pattern.
     */
    fun setExternalUserId(userId: String?) {
        if (!userId.isNullOrBlank()) {
            Timber.d("[OneSignalService] setExternalUserId requested: $userId")
            pendingExternalUserId = userId
            if (!initialized) {
                Timber.w("[OneSignalService] SDK not initialized, skipping login for $userId")
                return
            }
            scope.launch {
                setExternalUserIdWithRetry(userId)
            }
        } else {
            Timber.d("[OneSignalService] Removing external user ID")
            pendingExternalUserId = null
            externalUserIdBound = false
            if (initialized) {
                OneSignal.logout()
            }
        }
    }

    /**
     * Suspend version: sets external user ID and waits for login() to complete.
     * Use this when you need to register FCM tokens AFTER login() finishes,
     * so the captured OneSignal subscription ID is the post-login one.
     */
    suspend fun setExternalUserIdAndAwait(userId: String) {
        Timber.d("[OneSignalService] setExternalUserIdAndAwait: $userId")
        pendingExternalUserId = userId
        if (!initialized) {
            Timber.w("[OneSignalService] SDK not initialized, skipping login for $userId")
            return
        }
        setExternalUserIdWithRetry(userId)
    }

    /**
     * Get the OneSignal subscription ID (equivalent to iOS player ID).
     * Returns null if not yet registered.
     */
    fun getSubscriptionId(): String? {
        return OneSignal.User.pushSubscription.id?.takeIf { it.isNotBlank() }
    }

    /**
     * Get the push token (FCM token managed by OneSignal).
     */
    fun getPushToken(): String? {
        return OneSignal.User.pushSubscription.token?.takeIf { it.isNotBlank() }
    }

    /**
     * Add tags for user segmentation (iOS parity).
     */
    fun addTags(tags: Map<String, String>) {
        Timber.d("[OneSignalService] Adding tags: $tags")
        tags.forEach { (key, value) ->
            OneSignal.User.addTag(key, value)
        }
    }

    /**
     * Remove tags (iOS parity).
     */
    fun removeTags(keys: List<String>) {
        Timber.d("[OneSignalService] Removing tags: $keys")
        keys.forEach { key ->
            OneSignal.User.removeTag(key)
        }
    }

    // ── Private helpers ─────────────────────────────────

    /**
     * Wait for OneSignal registration and login with retry.
     * iOS parity: matches _setExternalUserIdWithRetry pattern.
     */
    private suspend fun setExternalUserIdWithRetry(userId: String) {
        // Delays matching iOS: 0.5s, 1s, 2s, 3s (total ~6.5s max)
        val delays = listOf(500L, 1000L, 2000L, 3000L)

        var subscriptionId = getSubscriptionId()
        if (subscriptionId != null) {
            Timber.d("[OneSignalService] Already registered: $subscriptionId")
        } else {
            Timber.d("[OneSignalService] Waiting for OneSignal registration...")
            for (d in delays) {
                delay(d)
                subscriptionId = getSubscriptionId()
                if (subscriptionId != null) {
                    Timber.d("[OneSignalService] Registered after polling: $subscriptionId")
                    break
                }
            }
        }

        // Attempt login
        try {
            OneSignal.login(userId)
            externalUserIdBound = true
            android.util.Log.i("PushInit", "✅ OneSignal login() succeeded for $userId. subscriptionId=${getSubscriptionId()} optedIn=${OneSignal.User.pushSubscription.optedIn}")
        } catch (e: Exception) {
            android.util.Log.w("PushInit", "OneSignal login() failed for $userId, retrying in 3s...", e)
            delay(3000)
            try {
                OneSignal.login(userId)
                externalUserIdBound = true
                android.util.Log.i("PushInit", "✅ OneSignal login() succeeded on retry for $userId")
            } catch (e2: Exception) {
                android.util.Log.e("PushInit", "❌ OneSignal login() failed after retry for $userId", e2)
            }
        }

        // Request POST_NOTIFICATIONS permission (Android 13+) after login.
        // Without this, the OneSignal subscription stays "not subscribed" and
        // all push notifications silently fail with "All included players are
        // not subscribed". Must be called after login() so OneSignal can
        // associate the permission grant with the correct external user ID.
        if (!OneSignal.Notifications.permission) {
            android.util.Log.w("PushInit", "Push permission NOT granted, requesting...")
            val granted = OneSignal.Notifications.requestPermission(false)
            android.util.Log.i("PushInit", "Push permission request result: granted=$granted")
        } else {
            android.util.Log.i("PushInit", "Push permission already granted ✅")
        }

        // Final verification: log the subscription state so we can diagnose
        // "invalid_aliases.external_id" errors from the backend.
        val finalSubId = getSubscriptionId()
        val finalOptedIn = OneSignal.User.pushSubscription.optedIn
        val finalToken = OneSignal.User.pushSubscription.token
        if (finalOptedIn && finalSubId != null) {
            android.util.Log.i("PushInit", "✅ Push ready: subscriptionId=$finalSubId optedIn=true token=${finalToken?.take(15)}...")
        } else {
            // This is the root cause of include_aliases failures.
            // If optedIn=false, OneSignal won't bind the external_id to any
            // active subscription → backend's include_aliases returns
            // "invalid_aliases.external_id" → push silently fails.
            android.util.Log.e("PushInit", "❌ Push NOT ready after login+permission: subscriptionId=$finalSubId optedIn=$finalOptedIn token=${finalToken?.take(15)}... — include_aliases WILL FAIL on backend")
        }
    }
}
