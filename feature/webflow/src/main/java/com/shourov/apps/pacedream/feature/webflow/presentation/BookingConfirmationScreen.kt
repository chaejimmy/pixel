package com.shourov.apps.pacedream.feature.webflow.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(
    sessionId: String,
    bookingType: String,
    viewModel: BookingConfirmationViewModel = hiltViewModel(),
    onViewBooking: (String) -> Unit,
    onGoHome: () -> Unit,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(sessionId, bookingType) {
        viewModel.confirmBooking(sessionId, bookingType)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(PaceDreamIcons.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is BookingConfirmationUiState.Loading -> LoadingState()
                is BookingConfirmationUiState.Success -> SuccessState(
                    state = state,
                    onViewBooking = { onViewBooking(state.confirmation.bookingId) },
                    onGoHome = onGoHome
                )
                is BookingConfirmationUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.confirmBooking(sessionId, bookingType) },
                    onGoHome = onGoHome
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(PaceDreamSpacing.XL)
    ) {
        CircularProgressIndicator(color = PaceDreamColors.Primary)
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        Text(
            text = "Confirming your booking...",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary
        )
    }
}

@Composable
private fun SuccessState(
    state: BookingConfirmationUiState.Success,
    onViewBooking: () -> Unit,
    onGoHome: () -> Unit
) {
    val confirmation = state.confirmation
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.LG)
    ) {
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
        
        // Success icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(PaceDreamColors.Success.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PaceDreamIcons.Check,
                contentDescription = "Success",
                tint = PaceDreamColors.Success,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        
        Text(
            text = "Booking Confirmed!",
            style = PaceDreamTypography.Title1,
            color = PaceDreamColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        Text(
            text = confirmation.message,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
        
        // Booking details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Surface)
        ) {
            Column(
                modifier = Modifier.padding(PaceDreamSpacing.MD)
            ) {
                confirmation.itemTitle?.let { title ->
                    Text(
                        text = title,
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                }
                
                // Date row
                if (confirmation.startDate != null || confirmation.endDate != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Calendar,
                            contentDescription = null,
                            tint = PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(PaceDreamSpacing.SM))
                        Text(
                            text = buildString {
                                confirmation.startDate?.let { append(it) }
                                if (confirmation.startDate != null && confirmation.endDate != null) {
                                    append(" - ")
                                }
                                confirmation.endDate?.let { append(it) }
                            },
                            style = PaceDreamTypography.Body,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                }
                
                // Amount row
                confirmation.amount?.let { amount ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Payment,
                            contentDescription = null,
                            tint = PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(PaceDreamSpacing.SM))
                        Text(
                            text = confirmation.formattedAmount,
                            style = PaceDreamTypography.Headline.copy(fontWeight = FontWeight.Bold),
                            color = PaceDreamColors.Primary
                        )
                    }
                }
                
                // Booking ID
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Text(
                    text = "Booking ID: ${confirmation.bookingId}",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextTertiary
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action buttons
        Button(
            onClick = onViewBooking,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
            shape = RoundedCornerShape(PaceDreamRadius.MD)
        ) {
            Text("View Booking", style = PaceDreamTypography.Headline)
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        OutlinedButton(
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.MD)
        ) {
            Text("Go Home", style = PaceDreamTypography.Headline)
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(PaceDreamSpacing.LG)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(PaceDreamColors.Error.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PaceDreamIcons.Error,
                contentDescription = "Error",
                tint = PaceDreamColors.Error,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        
        Text(
            text = "Something went wrong",
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        Text(
            text = message,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
            shape = RoundedCornerShape(PaceDreamRadius.MD)
        ) {
            Text("Try Again")
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        OutlinedButton(
            onClick = onGoHome,
            shape = RoundedCornerShape(PaceDreamRadius.MD)
        ) {
            Text("Go Home")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingCancelledScreen(
    onGoHome: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Cancelled") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(PaceDreamSpacing.LG)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(PaceDreamColors.Warning.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Close,
                    contentDescription = "Cancelled",
                    tint = PaceDreamColors.Warning,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Text(
                text = "Booking Cancelled",
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            Text(
                text = "Your payment was not processed. You can try again anytime.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
            
            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Go Home", style = PaceDreamTypography.Headline)
            }
        }
    }
}


