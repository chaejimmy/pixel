package com.pacedream.app.feature.checkout

import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

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
                title = { Text("Checkout", style = PaceDreamTypography.Title2) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Text("Booking summary", style = PaceDreamTypography.Title3, color = PaceDreamColors.TextPrimary)

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(PaceDreamSpacing.MD), verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                    SummaryRow("Date", draft.date)
                    SummaryRow("Start", draft.startTimeISO.substringAfter("T").take(5))
                    SummaryRow("End", draft.endTimeISO.substringAfter("T").take(5))
                    SummaryRow("Guests", "${draft.guests}")
                }
            }

            draft.totalAmountEstimate?.let { total ->
                Card(
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                        SummaryRow("Estimated total", "$${String.format("%.2f", total)}")
                    }
                }
            }

            uiState.errorMessage?.let {
                Card(
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.ErrorContainer)
                ) {
                    Text(
                        it,
                        color = PaceDreamColors.OnErrorContainer,
                        style = PaceDreamTypography.Callout,
                        modifier = Modifier.padding(PaceDreamSpacing.MD)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.submitBooking() },
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = PaceDreamColors.OnPrimary,
                        modifier = Modifier
                            .height(18.dp)
                            .padding(end = 10.dp)
                    )
                }
                Text(
                    if (uiState.isSubmitting) "Confirming…" else "Confirm Booking",
                    style = PaceDreamTypography.Button
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = PaceDreamColors.TextSecondary, style = PaceDreamTypography.Callout)
        Text(value, fontWeight = FontWeight.Medium, style = PaceDreamTypography.Callout, color = PaceDreamColors.TextPrimary)
    }
}

