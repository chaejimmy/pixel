package com.shourov.apps.pacedream

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.metrics.performance.JankStats
import com.shourov.apps.pacedream.feature.host.domain.HostModeManager
import com.shourov.apps.pacedream.feature.notification.NotificationRouter
import com.shourov.apps.pacedream.feature.webflow.DeepLinkHandler
import com.shourov.apps.pacedream.feature.webflow.DeepLinkResult
import com.shourov.apps.pacedream.notification.OneSignalService
import com.shourov.apps.pacedream.ui.PaceDreamApp
import com.shourov.apps.pacedream.ui.rememberPaceDreamAppState
import com.pacedream.common.composables.theme.PaceDreamTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Lazily inject [JankStats], which is used to track jank throughout the app.
     */
    @Inject
    lateinit var lazyStats: dagger.Lazy<JankStats>
    
    /**
     * Deep link handler for processing booking success/cancelled and other deep links
     */
    @Inject
    lateinit var deepLinkHandler: DeepLinkHandler
    
    /**
     * Host mode manager for guest/host switching
     */
    @Inject
    lateinit var hostModeManager: HostModeManager

    @Inject
    lateinit var oneSignalService: OneSignalService

    val viewModel: MainActivityViewModel by viewModels()
    
    // Pending deep link to process after navigation is ready (observable for Compose)
    private val _pendingDeepLink = kotlinx.coroutines.flow.MutableStateFlow<DeepLinkResult?>(null)
    val pendingDeepLink: kotlinx.coroutines.flow.StateFlow<DeepLinkResult?> = _pendingDeepLink

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep splash screen visible until the first composable frame is drawn.
        // This prevents a white flash between splash dismissal and first render.
        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        // Turn off the decor fitting system windows, which allows us to handle insets,
        // including IME animations, and go edge-to-edge
        // This also sets up the initial system bar style based on the platform theme
        enableEdgeToEdge()

        // Handle deep links
        handleIntent(intent)

        setContent {
            // Signal splash screen to dismiss once Compose begins rendering
            isReady = true

            val appState = rememberPaceDreamAppState(
                windowSizeClass = calculateWindowSizeClass(this),
                hostModeManager = hostModeManager
            )

            PaceDreamTheme {
                // iOS parity: keep the main app visible even when logged out.
                // Protected actions should present the AuthFlowSheet modally.
                PaceDreamApp(appState)
            }
        }

        // Request POST_NOTIFICATIONS permission on Android 13+ (API 33).
        // Without this prompt, notifications are silently dropped because the
        // permission defaults to denied. OneSignal handles the system dialog.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            oneSignalService.requestPermission()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // Check for deep links first
        val deepLinkResult = deepLinkHandler.parseDeepLink(intent)
        if (deepLinkResult != null) {
            _pendingDeepLink.value = deepLinkResult
            Timber.d("Deep link parsed: $deepLinkResult")
            return
        }

        // Handle push notification intents via NotificationRouter (iOS parity).
        // Routes to the correct tab and screen based on notification data extras.
        if (NotificationRouter.handleIntent(intent)) {
            Timber.d("Notification intent routed by NotificationRouter")
            return
        }
    }
    
    /**
     * Get pending deep link and clear it
     */
    fun consumePendingDeepLink(): DeepLinkResult? {
        val result = _pendingDeepLink.value
        _pendingDeepLink.value = null
        return result
    }

    override fun onResume() {
        super.onResume()
        try {
            lazyStats.get().isTrackingEnabled = true
        } catch (e: Exception) {
            Timber.e(e, "Failed to enable JankStats tracking")
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            lazyStats.get().isTrackingEnabled = false
        } catch (e: Exception) {
            Timber.e(e, "Failed to disable JankStats tracking")
        }
    }
}

/**
 * The default light scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=35-38;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

/**
 * The default dark scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=40-44;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)

