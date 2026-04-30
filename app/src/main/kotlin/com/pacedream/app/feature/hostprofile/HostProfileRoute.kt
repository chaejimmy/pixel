package com.pacedream.app.feature.hostprofile

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HostProfileRoute(
    hostId: String,
    seed: HostProfileModel? = null,
    viewModel: HostProfileViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onLoginRequired: () -> Unit,
    onNavigateToThread: (String) -> Unit,
    onNavigateToListing: (HostListingSummary) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(hostId) {
        viewModel.load(hostId, seed)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HostProfileViewModel.Effect.ShowAuthRequired -> onLoginRequired()
                is HostProfileViewModel.Effect.ShowToast ->
                    snackbarHostState.showSnackbar(effect.message)
                is HostProfileViewModel.Effect.NavigateToThread ->
                    onNavigateToThread(effect.threadId)
            }
        }
    }

    HostProfileScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onMessageHost = { viewModel.contactHost() },
        onListingClick = onNavigateToListing,
        onRetry = { viewModel.refresh() },
    )
}
