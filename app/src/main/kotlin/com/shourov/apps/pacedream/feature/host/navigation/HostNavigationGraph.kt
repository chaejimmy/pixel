package com.shourov.apps.pacedream.feature.host.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.shourov.apps.pacedream.feature.host.data.ImageUploadService
import com.shourov.apps.pacedream.feature.host.presentation.CreateListingScreen
import com.shourov.apps.pacedream.feature.host.presentation.ListingMode
import com.shourov.apps.pacedream.feature.host.presentation.HostAnalyticsScreen
import com.shourov.apps.pacedream.feature.host.presentation.HostBookingsScreen
import com.shourov.apps.pacedream.feature.host.presentation.HostDashboardScreenWithViewModel
import com.shourov.apps.pacedream.feature.host.presentation.HostEarningsScreen
import com.shourov.apps.pacedream.feature.host.presentation.HostInboxScreen
import com.shourov.apps.pacedream.feature.host.presentation.HostListingsScreen
import com.shourov.apps.pacedream.feature.host.presentation.HostPostScreen
import com.shourov.apps.pacedream.feature.host.presentation.HostProfileScreen
import com.shourov.apps.pacedream.feature.host.presentation.HostSettingsScreen
import com.shourov.apps.pacedream.feature.host.presentation.StripeConnectOnboardingScreen
import com.pacedream.app.feature.inbox.InboxScreen
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImageUploadEntryPoint {
    fun imageUploadService(): ImageUploadService
}

fun NavGraphBuilder.HostNavigationGraph(
    navController: NavController,
    onSwitchToGuestMode: () -> Unit = {},
    onSignOut: () -> Unit = {},
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
            onAnalyticsClick = onNavigateToAnalytics,
            onSwitchToGuestMode = onSwitchToGuestMode,
            onSignOut = onSignOut
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
            onBookingClick = onNavigateToBooking
        )
    }

    composable(HostScreen.Earnings.route) {
        HostEarningsScreen(
            onSignInClick = onSignOut, // triggers re-auth flow
            onConnectStripeClick = { navController.navigate(HostScreen.PaymentSetup.route) },
            onCompleteSetupClick = { navController.navigate(HostScreen.PaymentSetup.route) }
        )
    }

    composable(HostScreen.Analytics.route) {
        HostAnalyticsScreen(
            onBackClick = { navController.popBackStack() }
        )
    }

    // iOS parity: Post tab routes to PostStartView hub (not directly to create listing)
    composable(HostScreen.Post.route) {
        HostPostScreen(
            onCreateListingClick = onNavigateToAddListing,
            onCreateListingWithType = { type ->
                navController.navigate("${HostNavigationDestinations.ADD_LISTING}?type=$type")
            },
            onMyListingsClick = { navController.navigate(HostScreen.Listings.route) },
            onAnalyticsClick = { navController.navigate(HostScreen.Analytics.route) }
        )
    }

    // iOS parity: Inbox tab uses dedicated host inbox with Messages/Notifications segments
    composable(HostScreen.Inbox.route) {
        HostInboxScreen(
            onThreadClick = { threadId -> },
            messagesContent = {
                InboxScreen(
                    onThreadClick = { threadId -> }
                )
            }
        )
    }

    // iOS parity: Dedicated Host Profile screen (no longer reuses guest ProfileScreen)
    composable(HostScreen.Profile.route) {
        HostProfileScreen(
            onEditProfileClick = { /* TODO: open edit profile sheet */ },
            onEditPhotoClick = { /* TODO: open photo picker */ },
            onListingsClick = { navController.navigate(HostScreen.Listings.route) },
            onBookingsClick = { navController.navigate(HostScreen.Bookings.route) },
            onInboxClick = { navController.navigate(HostScreen.Inbox.route) },
            onEarningsClick = { navController.navigate(HostScreen.Earnings.route) },
            onAccountSettingsClick = { navController.navigate(HostScreen.Settings.route) },
            onPersonalInfoClick = { navController.navigate(HostScreen.Settings.route) },
            onPaymentPayoutClick = { navController.navigate(HostScreen.Earnings.route) },
            onSwitchToGuestMode = onSwitchToGuestMode,
            onLoggedOut = onSwitchToGuestMode
        )
    }

    composable(HostScreen.Settings.route) {
        HostSettingsScreen(
            onBackClick = { navController.popBackStack() },
            onPaymentSetupClick = { navController.navigate(HostScreen.PaymentSetup.route) },
            onEarningsClick = { navController.navigate(HostScreen.Earnings.route) },
            onBookingsClick = { navController.navigate(HostScreen.Bookings.route) },
            onListingsClick = { navController.navigate(HostScreen.Listings.route) }
        )
    }

    composable(HostScreen.PaymentSetup.route) {
        StripeConnectOnboardingScreen(
            onBackClick = { navController.popBackStack() }
        )
    }

    composable("${HostNavigationDestinations.ADD_LISTING}?type={type}") { backStackEntry ->
        val typeParam = backStackEntry.arguments?.getString("type") ?: "share"
        val listingMode = when (typeParam.lowercase()) {
            "borrow" -> ListingMode.BORROW
            "split" -> ListingMode.SPLIT
            else -> ListingMode.SHARE
        }
        val context = LocalContext.current
        val uploadService = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ImageUploadEntryPoint::class.java
            ).imageUploadService()
        } catch (_: Exception) { null }

        CreateListingScreen(
            listingMode = listingMode,
            imageUploadService = uploadService,
            onBackClick = { navController.popBackStack() },
            onPublishSuccess = { listingId ->
                navController.popBackStack()
            }
        )
    }

    // Keep backward-compatible route without type param (defaults to SHARE)
    composable(HostNavigationDestinations.ADD_LISTING) {
        val context = LocalContext.current
        val uploadService = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ImageUploadEntryPoint::class.java
            ).imageUploadService()
        } catch (_: Exception) { null }

        CreateListingScreen(
            listingMode = ListingMode.SHARE,
            imageUploadService = uploadService,
            onBackClick = { navController.popBackStack() },
            onPublishSuccess = { listingId ->
                navController.popBackStack()
            }
        )
    }
}
