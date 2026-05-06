package com.shourov.apps.pacedream.ui

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.tracing.trace
import com.shourov.apps.pacedream.core.ui.TrackDisposableJank
import com.shourov.apps.pacedream.feature.webflow.DeepLinkResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import com.shourov.apps.pacedream.navigation.BookingDestination
import com.shourov.apps.pacedream.navigation.InboxDestination
import com.shourov.apps.pacedream.navigation.PropertyDestination
import com.shourov.apps.pacedream.navigation.UserStartTopLevelDestination
import com.shourov.apps.pacedream.signin.navigation.CREATE_ACCOUNT_ROUTE
import com.shourov.apps.pacedream.signin.navigation.ONBOARDING_ROUTE
import com.shourov.apps.pacedream.signin.navigation.START_SIGN_IN_WITH_PHONE_ROUTE
import com.shourov.apps.pacedream.signin.navigation.START_WITH_EMAIL_ROUTE
import com.shourov.apps.pacedream.signin.navigation.navigateToCreateAccountScreen
import com.shourov.apps.pacedream.signin.navigation.navigateToEmailSignInScreen
import com.shourov.apps.pacedream.signin.navigation.navigateToHomeScreen
import com.shourov.apps.pacedream.signin.navigation.navigateToSignInWithPhoneScreen
import com.shourov.apps.pacedream.signin.navigation.navigateToUserOnBoardingScreen
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberPaceDreamAppState(
    windowSizeClass: WindowSizeClass,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
    hostModeManager: com.shourov.apps.pacedream.feature.host.domain.HostModeManager,
): PaceDreamAppState {
    NavigationTrackingSideEffect(navController)
    return remember(
        navController,
        coroutineScope,
        windowSizeClass,
        hostModeManager,
    ) {
        PaceDreamAppState(
            navController = navController,
            coroutineScope = coroutineScope,
            windowSizeClass = windowSizeClass,
            hostModeManager = hostModeManager,
        )
    }
}

@Stable
class PaceDreamAppState(
    val navController: NavHostController,
    coroutineScope: CoroutineScope,
    val windowSizeClass: WindowSizeClass,
    val hostModeManager: com.shourov.apps.pacedream.feature.host.domain.HostModeManager,
) {
    val currentDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination

    val currentUserStartDestination: UserStartTopLevelDestination?
        @Composable get() = when (currentDestination?.route) {
            ONBOARDING_ROUTE -> UserStartTopLevelDestination.ONBOARDING
            START_SIGN_IN_WITH_PHONE_ROUTE -> UserStartTopLevelDestination.SIGN_IN_WITH_PHONE
            START_WITH_EMAIL_ROUTE -> UserStartTopLevelDestination.SIGN_IN_WITH_MAIL
            CREATE_ACCOUNT_ROUTE -> UserStartTopLevelDestination.ACCOUNT_SETUP
            else -> null
        }

    val showTopBar: Boolean
        @Composable get() = currentUserStartDestination != null && currentUserStartDestination != UserStartTopLevelDestination.ONBOARDING

    val topBarTitle: String
        @Composable get() = "" // TODO: Implement top bar title based on current destination
    
    val isHostMode = hostModeManager.isHostMode

    /**
     * One-shot user-facing messages emitted from deep-link / mode-switch
     * handling.  Buffered through a Channel so the message survives the
     * brief window between dispatch and the first collector subscribing
     * after a mode switch.  Consumed by [com.shourov.apps.pacedream.ui.PaceDreamApp]
     * which renders them as a Toast.
     */
    private val _deepLinkMessages = Channel<String>(capacity = Channel.BUFFERED)
    val deepLinkMessages: Flow<String> = _deepLinkMessages.receiveAsFlow()

    fun navigateToUserStartDestination(
        destination: UserStartTopLevelDestination,
    ) {
        try {
            trace(
                label = "Navigation: ${destination.name}",
            ) {
                val destinationOptions = navOptions {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                when (destination) {
                    UserStartTopLevelDestination.ONBOARDING ->
                        navController.navigateToUserOnBoardingScreen(
                            destinationOptions,
                        )

                    UserStartTopLevelDestination.SIGN_IN_WITH_PHONE -> navController.navigateToSignInWithPhoneScreen(
                        destinationOptions,
                    )

                    UserStartTopLevelDestination.SIGN_IN_WITH_MAIL -> navController.navigateToEmailSignInScreen(
                        destinationOptions,
                    )

                    UserStartTopLevelDestination.ACCOUNT_SETUP -> navController.navigateToCreateAccountScreen(
                        destinationOptions,
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "navigateToUserStartDestination failed for: ${destination.name}")
        }
    }
    
    /**
     * Handle deep link navigation
     * Called when a deep link is received (booking success, booking cancelled, etc.)
     */
    fun handleDeepLink(deepLinkResult: DeepLinkResult) {
        try {
            when (deepLinkResult) {
                is DeepLinkResult.BookingSuccess -> {
                    val bookingType = deepLinkResult.bookingType?.name?.lowercase() ?: "time_based"
                    navController.navigate(
                        "${BookingDestination.BOOKING_CONFIRMATION.name}/${deepLinkResult.sessionId}/$bookingType"
                    )
                }
                is DeepLinkResult.BookingCancelled -> {
                    navController.navigate(BookingDestination.BOOKING_CANCELLED.name)
                }
                is DeepLinkResult.ListingDetail -> {
                    navController.navigate("${PropertyDestination.DETAIL.name}/${deepLinkResult.listingId}")
                }
                is DeepLinkResult.GearDetail -> {
                    navController.navigate("${PropertyDestination.DETAIL.name}/${deepLinkResult.gearId}")
                }
                is DeepLinkResult.StripeConnectReturn -> {
                    Timber.d("Stripe Connect return deep link received — switching to host mode")
                    // The top-level NavController does NOT register
                    // "host_earnings" — that route lives inside the host
                    // mode's internal NavHost (HostModeScreen).  Calling
                    // navigate("host_earnings") here used to throw an
                    // IllegalArgumentException that was swallowed silently,
                    // leaving hosts who finished Stripe Connect onboarding
                    // staring at an unchanged Home screen.  Instead, flip
                    // the host-mode flag so HostModeScreen mounts on its
                    // own start destination ("host_dashboard"), and surface
                    // a one-shot message so the host knows onboarding
                    // landed.
                    try {
                        hostModeManager.setHostMode(true)
                    } catch (e: Exception) {
                        Timber.e(
                            e,
                            "Stripe Connect return: failed to switch to host mode",
                        )
                    }
                    _deepLinkMessages.trySend(
                        "Stripe setup completed. Check your earnings."
                    )
                }
            }
        } catch (e: Exception) {
            // Non-fatal diagnostic — `Timber.e` already wires Crashlytics
            // via the prod tree.  Re-emit the deep link in the message so
            // it is visible in logs without dumping PII.
            Timber.e(e, "handleDeepLink failed for: $deepLinkResult")
        }
    }
}

/**
 * Stores information about navigation events to be used with JankStats
 */
@Composable
private fun NavigationTrackingSideEffect(navController: NavHostController) {
    TrackDisposableJank(navController) { metricsHolder ->
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            metricsHolder.state?.putState("Navigation", destination.route.toString())
        }

        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
}