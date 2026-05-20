package com.shourov.apps.pacedream.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pacedream.app.feature.listingdetail.ListingDetailRoute
import com.shourov.apps.pacedream.feature.bookingdetail.BookingDetailScreen
import com.shourov.apps.pacedream.signin.navigation.userOnBoardingScreen
import com.shourov.apps.pacedream.ui.PaceDreamAppState

@Composable
fun PaceDreamNavHost(
    modifier: Modifier = Modifier,
    startDestination: String,
    appState: PaceDreamAppState,
) {
    val navController = appState.navController

    // Top-level NavHost: fade-only transitions to avoid double-animation jank
    // (the nested dashboard NavHost handles its own slide transitions for tab switches)
    val iOSEaseInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(150, easing = iOSEaseInOut))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(150, easing = iOSEaseInOut))
        },
    ) {
        userOnBoardingScreen(
            navController = navController,
            onNavigateToSignInWithEmail = {
                appState.navigateToUserStartDestination(UserStartTopLevelDestination.SIGN_IN_WITH_MAIL)
            },
            onStartWithPhone = {
                appState.navigateToUserStartDestination(UserStartTopLevelDestination.SIGN_IN_WITH_PHONE)
            },
            onNavigateToAccountSetup = {
                appState.navigateToUserStartDestination(UserStartTopLevelDestination.ACCOUNT_SETUP)
            },
        )

        DashboardNavigation(hostModeManager = appState.hostModeManager)

        // Unified listing + booking-detail destinations. Registered at the
        // top-level NavHost so they are reachable from both host and guest
        // contexts via [Routes.listing] / [Routes.bookingDetail]. Inner
        // dashboard navigation still has its own callbacks for cross-tab
        // hops (inbox/host profile/checkout); the top-level entries pop
        // back to the previous screen for those interactions because the
        // dashboard's own NavController is the only one that knows how to
        // reach them.
        composable(
            route = Routes.LISTING_PATTERN,
            arguments = listOf(navArgument(Routes.LISTING_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString(Routes.LISTING_ARG).orEmpty()
            ListingDetailRoute(
                listingId = listingId,
                onBackClick = { navController.popBackStack() },
                onLoginRequired = { navController.popBackStack() },
                onNavigateToInbox = { navController.popBackStack() },
                onNavigateToThread = { navController.popBackStack() },
                onNavigateToHostProfile = { navController.popBackStack() },
                onNavigateToCheckout = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.BOOKING_DETAIL_PATTERN,
            arguments = listOf(navArgument(Routes.BOOKING_DETAIL_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString(Routes.BOOKING_DETAIL_ARG).orEmpty()
            BookingDetailScreen(
                bookingId = bookingId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}