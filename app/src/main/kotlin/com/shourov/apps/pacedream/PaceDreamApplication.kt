package com.shourov.apps.pacedream

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.google.firebase.FirebaseApp
import com.pacedream.app.core.auth.SessionManager
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.notification.FcmTokenRegistrar
import com.shourov.apps.pacedream.notification.OneSignalService
import com.shourov.apps.pacedream.notification.PaceDreamNotificationService
import com.shourov.apps.pacedream.util.ProfileVerifierLogger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * [Application] class for PaceDream
 */
@HiltAndroidApp
class PaceDreamApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var profileVerifierLogger: ProfileVerifierLogger

    @Inject
    lateinit var authSession: AuthSession

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var oneSignalService: OneSignalService

    // Eagerly inject so notification channels are created at app startup.
    // Without this, background FCM notifications can arrive before channels exist
    // (e.g., after fresh install or data clear) and get silently dropped on Android 8+.
    @Inject
    lateinit var notificationService: PaceDreamNotificationService

    @Inject
    lateinit var fcmTokenRegistrar: FcmTokenRegistrar

    override fun onCreate() {
        super.onCreate()

        // Plant Timber tree for debug builds only.
        // Release builds intentionally have no Timber tree, so all Timber.d/e/w calls
        // become no-ops (zero overhead). Crashlytics captures fatal crashes separately.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Install global exception handler to prevent background thread crashes
        // from killing the app. Fatal errors (OOM, etc.) still propagate to Crashlytics.
        com.shourov.apps.pacedream.stability.GlobalExceptionHandler.install()

        // Initialize Firebase before any network calls. The firebase-perf gradle plugin
        // instruments OkHttp at bytecode level, so it requires Firebase to be initialized
        // prior to any OkHttpClient.execute() call (including those in authSession.initialize()).
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Timber.e(e, "Firebase initialization failed; continuing without Firebase services")
        }

        try {
            profileVerifierLogger()
        } catch (e: Exception) {
            Timber.e(e, "ProfileVerifierLogger failed; continuing")
        }

        // iOS parity: Initialize OneSignal (matches iOS setupOneSignal in AppDelegate).
        // Non-blocking; must run after Firebase init so FCM token is available.
        try {
            oneSignalService.initialize(BuildConfig.ONESIGNAL_APP_ID)
            // Push-readiness diagnostics are routed through Timber so that release
            // builds (which plant no Timber tree) never leak subscription IDs or
            // push tokens to logcat.
            Timber.tag("PushInit").i(
                "OneSignal initialized. permission=%s subscriptionId=%s token=%s...",
                com.onesignal.OneSignal.Notifications.permission,
                com.onesignal.OneSignal.User.pushSubscription.id,
                com.onesignal.OneSignal.User.pushSubscription.token?.take(15)
            )
        } catch (e: Exception) {
            Timber.tag("PushInit").e(e, "OneSignal initialization failed")
        }

        // iOS parity: bootstrap session on app start if tokens exist.
        // Do not block app startup; protected actions can still gate via AuthFlowSheet.
        // Register FCM token AFTER auth is initialized so tokenStorage.hasTokens()
        // returns true. Previously registerCurrentToken() ran before initialize()
        // completed, causing the registration to be skipped for returning users.
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            try {
                authSession.initialize()
                sessionManager.initialize()

                // Now that auth tokens are loaded from storage, register FCM token.
                // OneSignal owns the FirebaseMessagingService (via manifest merger),
                // so we retrieve the token explicitly here instead of relying on
                // onNewToken(). Safe to call on every launch; deduplicates internally.
                Timber.tag("PushInit").i("Auth initialized, registering FCM token")
                fcmTokenRegistrar.registerCurrentToken()
            } catch (e: Exception) {
                Timber.e(e, "Auth/FCM initialization failed; app will show unauthenticated state")
            }
        }

        // iOS parity: bind OneSignal external user ID when user is authenticated.
        // Mirrors iOS OneSignalService.setExternalUserId() called after auth.
        // Also re-registers FCM token when user changes so backend has the correct mapping.
        //
        // CRITICAL: We must await login() BEFORE registering the FCM token.
        // Previously setExternalUserId() launched a fire-and-forget coroutine,
        // so registerCurrentToken() ran before login() finished. The captured
        // OneSignal subscription ID was pre-login (stale) or null, causing the
        // backend to create a conflicting REST player. Chat push notifications
        // then silently failed because the stale player ID reported recipients=1
        // but FCM delivery failed (token owned by the SDK subscription).
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            try {
                authSession.currentUser.collect { user ->
                    if (user != null && user.id.isNotBlank()) {
                        Timber.tag("PushInit").i(
                            "User authenticated, binding OneSignal + FCM. permission=%s",
                            com.onesignal.OneSignal.Notifications.permission
                        )
                        // Await login() so the subscription ID is post-login
                        oneSignalService.setExternalUserIdAndAwait(user.id)
                        Timber.tag("PushInit").i(
                            "OneSignal login() completed — now registering FCM token"
                        )
                        // Clear dedup cache so the device always re-registers after
                        // login. This fixes the case where a previous registration
                        // was marked as done but the backend lost the PushDevice record.
                        fcmTokenRegistrar.clearRegistrationCache()
                        fcmTokenRegistrar.registerCurrentToken()
                    } else {
                        oneSignalService.setExternalUserId(null)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "User observation coroutine failed")
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(200)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            .build()
    }
}
