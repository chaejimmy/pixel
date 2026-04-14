package com.shourov.apps.pacedream.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
    
    if (isHostMode) {
        // Show host mode interface
        // Booking, edit-listing, add-listing, analytics, and withdraw navigation
        // are handled internally by HostModeScreen using its own NavHost.
        // Only onNavigateToProperty escapes to guest mode (intentional mode switch).
        HostModeScreen(
            hostModeManager = appState.hostModeManager,
            onSwitchToGuestMode = {
                appState.hostModeManager.setHostMode(false)
            },
            onNavigateToProperty = { propertyId ->
                // Switch to guest mode — the dashboard's inner NavHost handles property detail.
                // Cross-NavHost navigation is not possible, so we land on the Home tab.
                appState.hostModeManager.setHostMode(false)
                timber.log.Timber.d("Switched to guest mode for property: $propertyId")
            }
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