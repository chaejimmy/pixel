package com.pacedream.app.feature.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    draft: BookingDraft,
    onBackClick: () -> Unit,
    onConfirmSuccess: (bookingId: String) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(draft) {
        viewModel.setDraft(draft)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CheckoutViewModel.Effect.NavigateToConfirmation -> onConfirmSuccess(effect.bookingId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Booking summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Listing: ${draft.listingId}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Date: ${draft.date}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Start: ${draft.startTimeISO}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("End: ${draft.endTimeISO}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Guests: ${draft.guests}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            draft.totalAmountEstimate?.let {
                Text("Estimated total: $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            uiState.errorMessage?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.submitBooking() },
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .height(18.dp)
                            .padding(end = 10.dp)
                    )
                }
                Text(if (uiState.isSubmitting) "Confirming…" else "Confirm")
            }
        }
    }
}

