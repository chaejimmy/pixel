package com.shourov.apps.pacedream.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.shourov.apps.pacedream.MainActivity
import com.pacedream.common.composables.components.PaceDreamTopAppBar
import com.shourov.apps.pacedream.feature.host.presentation.HostModeScreen
import com.shourov.apps.pacedream.navigation.PaceDreamNavHost
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
    
    // Handle pending deep links from MainActivity
    LaunchedEffect(Unit) {
        val activity = context as? MainActivity
        activity?.consumePendingDeepLink()?.let { deepLinkResult ->
            appState.handleDeepLink(deepLinkResult)
        }
    }
    
    if (isHostMode) {
        // Show host mode interface
        HostModeScreen(
            hostModeManager = appState.hostModeManager,
            onSwitchToGuestMode = {
                appState.hostModeManager.setHostMode(false)
            },
            onNavigateToProperty = { propertyId ->
                // TODO: Navigate to property details
            },
            onNavigateToBooking = { bookingId ->
                // TODO: Navigate to booking details
            },
            onNavigateToAddListing = {
                // TODO: Navigate to add listing
            },
            onNavigateToEditListing = { listingId ->
                // TODO: Navigate to edit listing
            },
            onNavigateToAnalytics = {
                // TODO: Navigate to analytics
            },
            onNavigateToWithdraw = {
                // TODO: Navigate to withdraw earnings
            }
        )
    } else {
        // Show guest mode interface
        Scaffold(
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
            },
            topBar = {
                if (showTopBar) {
                    PaceDreamTopAppBar(
                        title = appState.topBarTitle ?: stringResource(id = android.R.string.untitled),
                        navigationIcon = com.pacedream.common.icon.PaceDreamIcons.ArrowBack,
                        navigationIconContentDescription = "Open navigation menu",
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
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            PaceDreamNavHost(
                appState = appState,
                startDestination = DASHBOARD_ROUTE,
                modifier = Modifier
                    .padding(padding),
            )
        }
    }
}