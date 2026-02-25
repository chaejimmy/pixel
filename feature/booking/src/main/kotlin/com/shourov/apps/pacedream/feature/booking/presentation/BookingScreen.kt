/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shourov.apps.pacedream.feature.booking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.components.PaceDreamHeroHeader
import com.pacedream.common.composables.components.PaceDreamPropertyImage
import com.pacedream.common.composables.components.PaceDreamUserAvatar
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookingScreen(
    modifier: Modifier = Modifier,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadBookings()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        PaceDreamHeroHeader(
            title = "My Bookings",
            subtitle = "Manage your reservations",
            onNotificationClick = { /* Handle notification */ }
        )
        
        when {
            uiState.isLoading -> {
                BookingLoadingState()
            }
            uiState.bookings.isEmpty() -> {
                BookingEmptyState()
            }
            else -> {
                BookingContent(
                    bookings = uiState.bookings,
                    onBookingClick = viewModel::onBookingClick,
                    onCancelBooking = viewModel::cancelBooking,
                    onConfirmBooking = viewModel::confirmBooking
                )
            }
        }
    }
}

@Composable
private fun BookingContent(
    bookings: List<BookingModel>,
    onBookingClick: (String) -> Unit,
    onCancelBooking: (String) -> Unit,
    onConfirmBooking: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
    ) {
        items(bookings) { booking ->
            BookingCard(
                booking = booking,
                onClick = { onBookingClick(booking.id) },
                onCancel = { onCancelBooking(booking.id) },
                onConfirm = { onConfirmBooking(booking.id) }
            )
        }
    }
}

@Composable
private fun BookingCard(
    booking: BookingModel,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD)
        ) {
            // Property Image and Basic Info
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                PaceDreamPropertyImage(
                    imageUrl = booking.propertyImage,
                    contentDescription = "Property: ${booking.propertyName}",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.SM))
                )
                
                Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = booking.propertyName,
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.OnCard,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    
                    Text(
                        text = "Host: ${booking.hostName}",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.OnCard.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    
                    Text(
                        text = "${booking.currency} ${String.format("%.2f", booking.totalPrice)}",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            // Date separator
            HorizontalDivider(
                color = PaceDreamColors.Border,
                thickness = 0.5.dp
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Booking Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Check-in",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.OnCard.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    Text(
                        text = formatDate(booking.startDate),
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.OnCard,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Check-out",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.OnCard.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    Text(
                        text = formatDate(booking.endDate),
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.OnCard,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            // Status and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(status = booking.status.name)
                
                Row {
                    if (booking.status == BookingStatus.PENDING) {
                        TextButton(onClick = onCancel) {
                            Text("Cancel", style = PaceDreamTypography.Callout, color = PaceDreamColors.Error)
                        }
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                            shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        ) {
                            Text("Confirm", style = PaceDreamTypography.Callout)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "CONFIRMED" -> PaceDreamColors.Success.copy(alpha = 0.15f) to PaceDreamColors.Success
        "PENDING" -> PaceDreamColors.Warning.copy(alpha = 0.15f) to PaceDreamColors.Warning
        "CANCELLED" -> PaceDreamColors.Error.copy(alpha = 0.15f) to PaceDreamColors.Error
        else -> PaceDreamColors.SurfaceVariant to PaceDreamColors.OnSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = status.lowercase().replaceFirstChar { it.uppercase() },
            style = PaceDreamTypography.Caption2,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = PaceDreamSpacing.SM,
                vertical = PaceDreamSpacing.XS
            )
        )
    }
}

@Composable
private fun BookingLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = PaceDreamColors.Primary
        )
    }
}

@Composable
private fun BookingEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "No bookings",
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            Text(
                text = "No bookings yet",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = "Start exploring properties to make your first booking",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}
