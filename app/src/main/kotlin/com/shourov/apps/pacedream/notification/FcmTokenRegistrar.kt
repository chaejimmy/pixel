package com.shourov.apps.pacedream.notification

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.shourov.apps.pacedream.BuildConfig
import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.TokenStorage
import com.shourov.apps.pacedream.core.network.config.AppConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers the FCM token with the backend, independent of FirebaseMessagingService.
 *
 * This is needed because the app uses OneSignal for push display, which registers its
 * own FirebaseMessagingService. Having two services competing for MESSAGING_EVENT causes
 * undefined behavior. Instead, OneSignal owns FCM message delivery and display, while
 * this class ensures our backend also has the FCM token for direct push routing.
 *
 * Called from Application.onCreate() on each app start to catch token refreshes.
 */
@Singleton
class FcmTokenRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val tokenStorage: TokenStorage,
    private val json: Json,
    private val fcmTokenStore: FcmTokenStore,
    private val oneSignalService: OneSignalService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Get the current FCM token and register it with the backend.
     * Safe to call on every app start; deduplicates via FcmTokenStore.
     */
    fun registerCurrentToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Timber.d("FCM token retrieved: %s...", token.take(10))
                fcmTokenStore.saveToken(token)
                scope.launch { sendTokenToServer(token) }
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Failed to retrieve FCM token")
            }
    }

    private suspend fun sendTokenToServer(token: String) {
        if (!tokenStorage.hasTokens()) {
            Timber.d("Skipping FCM token registration: user not authenticated")
            return
        }

        if (fcmTokenStore.isAlreadyRegistered(token, tokenStorage.userId)) {
            Timber.d("FCM token already registered for this user, skipping")
            return
        }

        try {
            val url = appConfig.buildApiUrl("push-devices")
            val deviceInfo = mapOf(
                "model" to android.os.Build.MODEL,
                "systemVersion" to "Android ${android.os.Build.VERSION.RELEASE}",
                "appVersion" to try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
                } catch (_: Exception) { "" }
            )

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
                    sandbox = if (BuildConfig.DEBUG) true else null
                )
            )
            when (val result = apiClient.post(url, body, includeAuth = true)) {
                is ApiResult.Success -> {
                    Timber.d("FCM token registered with server via /push-devices")
                    fcmTokenStore.markRegistered(token, tokenStorage.userId)
                }
                is ApiResult.Failure -> {
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

    @Serializable
    private data class RegisterDeviceRequest(
        val fcmToken: String,
        val platform: String,
        val deviceInfo: Map<String, String> = emptyMap(),
        val onesignalPlayerId: String? = null,
        val sandbox: Boolean? = null,
    )

    @Serializable
    private data class LegacyRegisterDeviceRequest(
        val token: String,
        val platform: String,
    )
}
