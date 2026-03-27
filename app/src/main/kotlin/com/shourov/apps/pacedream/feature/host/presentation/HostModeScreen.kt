package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.domain.HostModeManager
import com.shourov.apps.pacedream.feature.host.navigation.HostNavigationDestinations
import com.shourov.apps.pacedream.feature.host.navigation.HostNavigationGraph
import com.shourov.apps.pacedream.feature.host.presentation.components.HostBottomNavigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostModeScreen(
    hostModeManager: HostModeManager,
    onSwitchToGuestMode: () -> Unit,
    onSignOut: () -> Unit = {},
    onNavigateToProperty: (String) -> Unit = {},
    onNavigateToBooking: (String) -> Unit = {},
    onNavigateToAddListing: () -> Unit = {},
    onNavigateToEditListing: (String) -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToWithdraw: () -> Unit = {}
) {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf("host_dashboard") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PaceDreamColors.Background,
        bottomBar = {
            HostBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    currentRoute = route
                    navController.navigate(route) {
                        popUpTo("host_dashboard") {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "host_dashboard",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HostNavigationGraph(
                navController = navController,
                onSwitchToGuestMode = onSwitchToGuestMode,
                onSignOut = onSignOut,
                onNavigateToProperty = onNavigateToProperty,
                onNavigateToBooking = onNavigateToBooking,
                onNavigateToAddListing = {
                    navController.navigate(HostNavigationDestinations.ADD_LISTING) {
                        launchSingleTop = true
                    }
                },
                onNavigateToEditListing = onNavigateToEditListing,
                onNavigateToAnalytics = onNavigateToAnalytics,
                onNavigateToWithdraw = onNavigateToWithdraw
            )
        }
    }
}
