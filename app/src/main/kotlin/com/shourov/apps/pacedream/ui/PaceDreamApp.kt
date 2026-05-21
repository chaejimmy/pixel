package com.shourov.apps.pacedream.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.shourov.apps.pacedream.MainActivity
import com.pacedream.common.composables.components.PaceDreamTopAppBar
import com.pacedream.notifications.PushDeepLink
import com.pacedream.notifications.PushDeepLinkHandler
import com.shourov.apps.pacedream.feature.host.presentation.HostModeScreen
import com.shourov.apps.pacedream.feature.wifi.presentation.WifiSessionHost
import com.shourov.apps.pacedream.navigation.PaceDreamNavHost
import com.shourov.apps.pacedream.navigation.Routes
import com.shourov.apps.pacedream.signin.navigation.DASHBOARD_ROUTE
import com.shourov.apps.pacedream.signin.navigation.ONBOARDING_ROUTE

@OptIn(
    ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class,
)
@Composable
fun PaceDreamApp(
    appState: PaceDreamAppState,
) {
    val showTopBar = appState.showTopBar
    val navController = appState.navController
    val isHostMode by appState.isHostMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    // User-visible failure message for navigation that hits a destination
    // not registered on the active NavController.  Surfacing this avoids
    // the prior "tap does nothing" silent failure mode.
    val navFailureMessage = "We couldn't open that — try again"
    
    // Handle pending deep links from MainActivity (reactive: handles onNewIntent too)
    val activity = context as? MainActivity
    val pendingDeepLink by activity?.pendingDeepLink?.collectAsStateWithLifecycle()
        ?:  remember { androidx.compose.runtime.mutableStateOf(null) }
    LaunchedEffect(pendingDeepLink) {
        pendingDeepLink?.let { deepLinkResult ->
            activity?.consumePendingDeepLink()
            try {
                appState.handleDeepLink(deepLinkResult)
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Failed to handle deep link: $deepLinkResult")
            }
        }
    }

    // Collect push deep links emitted by PushDeepLinkHandler. The handler is
    // dispatched from MainActivity.onCreate/onNewIntent and from the OneSignal
    // notification-click listener; this LaunchedEffect routes each emission to
    // the matching destination on the top-level NavController. Booking deep
    // links resolve via the unified Routes.bookingDetail helper so the route
    // pattern matches the destination registered on the top-level NavHost.
    //
    // Routing failures here are reported via a snackbar instead of being
    // swallowed: silent failures look identical to "tap did nothing" and
    // make destination/route mismatches invisible until QA spots them.
    LaunchedEffect(Unit) {
        PushDeepLinkHandler.deepLinks.collect { link ->
            val target = when (link) {
                is PushDeepLink.RequestDetail -> "requests/${link.requestId}"
                is PushDeepLink.RequestOffers -> "requests/${link.requestId}"
                is PushDeepLink.BookingDetail -> Routes.bookingDetail(link.bookingId)
            }
            try {
                navController.navigate(target)
            } catch (e: IllegalArgumentException) {
                timber.log.Timber.e(e, "Push deep link route not found: $target")
                snackbarHostState.showSnackbar(navFailureMessage)
            }
        }
    }

    // Surface one-shot messages emitted by handleDeepLink (e.g. Stripe
    // Connect return, mode switches) so they are visible to the user
    // even when the navigation target isn't a fresh screen.
    LaunchedEffect(appState) {
        appState.deepLinkMessages.collect { message ->
            try {
                android.widget.Toast.makeText(
                    context,
                    message,
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Failed to show deep-link toast: $message")
            }
        }
    }
    
    // Wi-Fi session pill is rendered above the Scaffold so it stays visible
    // across host/guest mode and across tabs/screens. The host composable
    // self-mounts a Hilt-scoped WifiSessionViewModel that observes
    // WifiSessionRouter for push-driven intents.
    Box(modifier = Modifier.fillMaxSize()) {
        if (isHostMode) {
            // Show host mode interface
            // Add-listing, analytics, withdraw, and edit-listing navigation
            // are handled internally by HostModeScreen's own NavHost.
            // Property card taps and booking row taps escape the host
            // NavController and land on the unified destinations registered
            // at the top-level NavHost so the user sees the real
            // ListingDetail / BookingDetail without a mode switch.
            HostModeScreen(
                hostModeManager = appState.hostModeManager,
                onSwitchToGuestMode = {
                    appState.hostModeManager.setHostMode(false)
                },
                onNavigateToProperty = { propertyId ->
                    // The Box swaps between HostModeScreen and the guest
                    // Scaffold based on isHostMode, so the top-level NavHost
                    // (which owns the unified listing destination) is only
                    // composed in guest mode. Flip the flag first so the
                    // navigation lands on a mounted host before navigating.
                    appState.hostModeManager.setHostMode(false)
                    try {
                        navController.navigate(Routes.listing(propertyId))
                    } catch (e: IllegalArgumentException) {
                        timber.log.Timber.e(e, "Host listing nav failed for id='$propertyId'")
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar(navFailureMessage)
                        }
                    }
                },
                onNavigateToBooking = { bookingId ->
                    appState.hostModeManager.setHostMode(false)
                    try {
                        navController.navigate(Routes.bookingDetail(bookingId))
                    } catch (e: IllegalArgumentException) {
                        timber.log.Timber.e(e, "Host booking nav failed for id='$bookingId'")
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar(navFailureMessage)
                        }
                    }
                },
            )
        } else {
            // Show guest mode interface
            // The Dashboard has its own inner Scaffold with a bottom bar that handles
            // navigation bar insets. Set contentWindowInsets to zero here so the outer
            // Scaffold does not double-count the bottom system bar insets, which would
            // cause a visible gap below the bottom navigation bar.
            Scaffold(
                modifier = Modifier.semantics {
                    testTagsAsResourceId = true
                },
                contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                topBar = {
                    if (showTopBar) {
                        PaceDreamTopAppBar(
                            title = appState.topBarTitle ?: stringResource(id = android.R.string.untitled),
                            navigationIcon = com.pacedream.common.icon.PaceDreamIcons.ArrowBack,
                            // The navigation icon's onNavigationClick is popBackStack(),
                            // so describe it as "Back" — not a drawer/menu opener.
                            navigationIconContentDescription = "Back",
                            actionIcon = com.pacedream.common.icon.PaceDreamIcons.Search,
                            actionIconContentDescription = "Search",
                            onNavigationClick = {
                                navController.popBackStack()
                            },
                            onActionClick = { },
                            showActionIcon = false,
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) { padding ->
                PaceDreamNavHost(
                    appState = appState,
                    startDestination = DASHBOARD_ROUTE,
                    modifier = Modifier
                        .padding(padding),
                )
            }
        }

        WifiSessionHost(
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Offline banner sits above all mode-specific content so it remains
        // visible across host/guest mode and across tab switches. Dismisses
        // itself when the ConnectivityObserver returns to Available.
        OfflineBanner(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
        )

        // Nav-failure snackbar sits at the bottom so it does not collide
        // with the offline banner or wi-fi session pill above.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}