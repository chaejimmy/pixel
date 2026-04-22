package com.shourov.apps.pacedream.feature.wifi.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Mount once in the app shell. Renders the persistent pill above whatever
 * Scaffold the app is currently showing and presents the matching sheet/
 * modal based on view-model state.
 *
 * The view model subscribes to [com.shourov.apps.pacedream.feature.wifi.WifiSessionRouter]
 * so push-notification routing reaches this surface even when the user is on
 * an unrelated tab.
 */
@Composable
fun WifiSessionHost(
    modifier: Modifier = Modifier,
    viewModel: WifiSessionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        WifiSessionPill(
            state = state,
            onTap = { viewModel.openSheet() },
            onQuickExtend = { viewModel.openExtendSheet() }
        )
    }

    when (state.sheet) {
        WifiSessionUiState.Sheet.Session -> WifiSessionSheet(
            state = state,
            onDismiss = viewModel::dismissSheet,
            onExtend = viewModel::openExtendSheet
        )
        WifiSessionUiState.Sheet.Extend -> WifiExtensionBottomSheet(
            state = state,
            onExtend = viewModel::extend,
            onDismiss = viewModel::dismissSheet
        )
        WifiSessionUiState.Sheet.Expired -> WifiExpiredModal(
            state = state,
            onReconnect = viewModel::reconnect,
            onDismiss = viewModel::clear
        )
        WifiSessionUiState.Sheet.None -> Unit
    }
}
