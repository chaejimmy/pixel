package com.shourov.apps.pacedream.feature.bookingdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    bookingId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Booking", style = PaceDreamTypography.Headline) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        when {
            uiState.isLoading -> {
                BookingDetailSkeleton(modifier = Modifier.padding(padding))
            }
            uiState.error != null -> {
                BookingDetailError(
                    message = uiState.error ?: "Failed",
                    onRetry = { viewModel.load() },
                    onBack = onBack,
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.booking != null -> {
                val booking = uiState.booking!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(PaceDreamSpacing.LG),
                    verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(PaceDreamSpacing.LG)) {
                            Text(
                                booking.propertyName,
                                style = PaceDreamTypography.Title2,
                                fontWeight = FontWeight.Bold,
                                color = PaceDreamColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Booking ID: ${booking.id}",
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card)
                    ) {
                        Column(modifier = Modifier.padding(PaceDreamSpacing.LG)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Start", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                                    Text(booking.startDate, style = PaceDreamTypography.Body, color = PaceDreamColors.TextPrimary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("End", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                                    Text(booking.endDate, style = PaceDreamTypography.Body, color = PaceDreamColors.TextPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                            Text(
                                "${booking.currency} ${String.format("%.2f", booking.totalPrice)}",
                                style = PaceDreamTypography.Title3,
                                fontWeight = FontWeight.Bold,
                                color = PaceDreamColors.Primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Status: ${booking.status.name}",
                                style = PaceDreamTypography.Body,
                                color = PaceDreamColors.TextSecondary
                            )
                        }
                    }
                }
            }
            else -> {
                BookingDetailError(
                    message = "Booking not found",
                    onRetry = { viewModel.load() },
                    onBack = onBack,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun BookingDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.LG),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            shape = RoundedCornerShape(PaceDreamRadius.LG)
        ) {}
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            shape = RoundedCornerShape(PaceDreamRadius.LG)
        ) {}
    }
}

@Composable
private fun BookingDetailError(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary)
                ) { Text("Retry") }
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Card)
                ) { Text("Back", color = PaceDreamColors.TextPrimary) }
            }
        }
    }
}

