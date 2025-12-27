package com.shourov.apps.pacedream

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.metrics.performance.JankStats
import com.google.firebase.FirebaseApp
import com.shourov.apps.pacedream.feature.auth.presentation.AuthScreen
import com.shourov.apps.pacedream.feature.host.domain.HostModeManager
import com.shourov.apps.pacedream.feature.webflow.DeepLinkHandler
import com.shourov.apps.pacedream.feature.webflow.DeepLinkResult
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

    val viewModel: MainActivityViewModel by viewModels()
    
    // Pending deep link to process after navigation is ready
    private var pendingDeepLink: DeepLinkResult? = null

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Turn off the decor fitting system windows, which allows us to handle insets,
        // including IME animations, and go edge-to-edge
        // This also sets up the initial system bar style based on the platform theme
        enableEdgeToEdge()

        // Handle deep links
        handleIntent(intent)

        setContent {
            val isAuthenticated by viewModel.isAuthenticated.collectAsState()

            val hostModeManager = remember { HostModeManager() }
            val appState = rememberPaceDreamAppState(
                windowSizeClass = calculateWindowSizeClass(this),
                hostModeManager = hostModeManager
            )
            
            CompositionLocalProvider {
                PaceDreamTheme {
                    if (isAuthenticated) {
                        PaceDreamApp(appState)
                    } else {
                        AuthScreen(
                            onLoginSuccess = {
                                viewModel.setAuthenticated(true)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        // Check for deep links first
        val deepLinkResult = deepLinkHandler.parseDeepLink(intent)
        if (deepLinkResult != null) {
            pendingDeepLink = deepLinkResult
            Timber.d("Deep link parsed: $deepLinkResult")
            return
        }
        
        // Handle push notification intents
        when {
            intent.hasExtra("chat_id") -> {
                // Navigate to specific chat
                val chatId = intent.getStringExtra("chat_id")
                Timber.d("Navigate to chat: $chatId")
                // Navigation will be handled by the app state
            }
            intent.hasExtra("booking_id") -> {
                // Navigate to specific booking
                val bookingId = intent.getStringExtra("booking_id")
                Timber.d("Navigate to booking: $bookingId")
                // Navigation will be handled by the app state
            }
        }
    }
    
    /**
     * Get pending deep link and clear it
     */
    fun consumePendingDeepLink(): DeepLinkResult? {
        val result = pendingDeepLink
        pendingDeepLink = null
        return result
    }

    override fun onResume() {
        super.onResume()
        lazyStats.get().isTrackingEnabled = true
    }

    override fun onPause() {
        super.onPause()
        lazyStats.get().isTrackingEnabled = false
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

