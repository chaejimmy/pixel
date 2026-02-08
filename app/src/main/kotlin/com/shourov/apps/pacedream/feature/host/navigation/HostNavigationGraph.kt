package com.shourov.apps.pacedream.feature.host.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.shourov.apps.pacedream.feature.host.presentation.CreateListingScreen
import com.shourov.apps.pacedream.feature.host.presentation.HostAnalyticsScreen
import com.shourov.apps.pacedream.feature.host.presentation.HostBookingsScreen
import com.shourov.apps.pacedream.feature.host.presentation.HostDashboardScreenWithViewModel
import com.shourov.apps.pacedream.feature.host.presentation.HostEarningsScreen
import com.shourov.apps.pacedream.feature.host.presentation.HostListingsScreen
import com.shourov.apps.pacedream.feature.host.presentation.HostSettingsScreen

fun NavGraphBuilder.HostNavigationGraph(
    navController: NavController,
    onNavigateToProperty: (String) -> Unit = {},
    onNavigateToBooking: (String) -> Unit = {},
    onNavigateToAddListing: () -> Unit = {},
    onNavigateToEditListing: (String) -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToWithdraw: () -> Unit = {}
) {
    composable(HostScreen.Dashboard.route) {
        HostDashboardScreenWithViewModel(
            onAddListingClick = onNavigateToAddListing,
            onListingClick = onNavigateToProperty,
            onBookingClick = onNavigateToBooking,
            onEarningsClick = onNavigateToWithdraw,
            onAnalyticsClick = onNavigateToAnalytics
        )
    }
    
    composable(HostScreen.Listings.route) {
        HostListingsScreen(
            onListingClick = onNavigateToProperty,
            onAddListingClick = onNavigateToAddListing,
            onEditListingClick = onNavigateToEditListing
        )
    }
    
    composable(HostScreen.Bookings.route) {
        HostBookingsScreen(
            onBookingClick = onNavigateToBooking,
            onAcceptBookingClick = { /* TODO: Handle accept */ },
            onRejectBookingClick = { /* TODO: Handle reject */ },
            onCancelBookingClick = { /* TODO: Handle cancel */ }
        )
    }
    
    composable(HostScreen.Earnings.route) {
        HostEarningsScreen(
            onWithdrawClick = onNavigateToWithdraw
        )
    }
    
    composable(HostScreen.Analytics.route) {
        HostAnalyticsScreen(
            onBackClick = { navController.popBackStack() }
        )
    }
    
    composable(HostScreen.Settings.route) {
        HostSettingsScreen(
            onBackClick = { navController.popBackStack() }
        )
    }

    composable(HostNavigationDestinations.ADD_LISTING) {
        CreateListingScreen(
            onBackClick = { navController.popBackStack() },
            onPublishSuccess = { listingId ->
                navController.popBackStack()
            }
        )
    }
}
