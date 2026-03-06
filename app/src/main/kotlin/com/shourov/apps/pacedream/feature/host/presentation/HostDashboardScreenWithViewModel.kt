package com.shourov.apps.pacedream.feature.host.presentation

/**
 * HostDashboardScreenWithViewModel is now consolidated into HostDashboardScreen
 * which directly integrates the ViewModel with iOS-parity layout.
 *
 * This file delegates to HostDashboardScreen for backwards compatibility.
 */
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HostDashboardScreenWithViewModel(
    onAddListingClick: () -> Unit = {},
    onListingClick: (String) -> Unit = {},
    onBookingClick: (String) -> Unit = {},
    onEarningsClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: HostDashboardViewModel = hiltViewModel()
) {
    HostDashboardScreen(
        onAddListingClick = onAddListingClick,
        onListingClick = onListingClick,
        onBookingClick = onBookingClick,
        onEarningsClick = onEarningsClick,
        onAnalyticsClick = onAnalyticsClick,
        onProfileClick = onProfileClick,
        viewModel = viewModel
    )
}
