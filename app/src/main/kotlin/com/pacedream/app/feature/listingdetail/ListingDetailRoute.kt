package com.pacedream.app.feature.listingdetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ListingDetailRoute(
    listingId: String,
    initialListing: ListingCardModel? = null,
    viewModel: ListingDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onLoginRequired: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToCheckout: (com.pacedream.app.feature.checkout.BookingDraft) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(listingId) {
        viewModel.load(listingId, initialListing)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ListingDetailViewModel.Effect.ShowAuthRequired -> onLoginRequired()
                is ListingDetailViewModel.Effect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
                is ListingDetailViewModel.Effect.OpenMaps -> {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effect.uri))
                        context.startActivity(intent)
                    }
                }
                is ListingDetailViewModel.Effect.Share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, effect.text)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share"))
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ListingDetailScreen(
                uiState = uiState,
                onBackClick = onBackClick,
                onRetry = { viewModel.refresh() },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onShare = { viewModel.share() },
                onContactHost = {
                    // Spec: unauth → route to Inbox tab + show Auth sheet.
                    if (!viewModel.isAuthenticated()) {
                        onNavigateToInbox()
                        onLoginRequired()
                    } else {
                        onNavigateToInbox()
                    }
                },
                onOpenInMaps = { viewModel.openInMaps() },
                onConfirmReserve = { draft ->
                    onNavigateToCheckout(draft)
                },
                onSubmitReview = { rating, comment, catRatings ->
                    viewModel.submitReview(rating, comment, catRatings)
                },
                onLoadReviews = { viewModel.loadReviews() }
            )
        }
    }
}

