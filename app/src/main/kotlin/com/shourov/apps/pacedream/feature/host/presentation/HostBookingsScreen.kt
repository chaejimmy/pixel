package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.data.HostBookingsData
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.BookingStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostBookingsScreen(
    onBookingClick: (String) -> Unit = {},
    onAcceptBookingClick: (String) -> Unit = {},
    onRejectBookingClick: (String) -> Unit = {},
    onCancelBookingClick: (String) -> Unit = {},
    viewModel: HostBookingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Bookings Header
        item {
            HostBookingsHeader(
                totalBookings = uiState.totalBookings,
                pendingBookings = uiState.pendingBookings
            )
        }
        
        // Booking Status Tabs
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            BookingStatusTabs(
                selectedStatus = uiState.selectedStatus,
                onStatusChanged = { viewModel.updateStatus(it) }
            )
        }
        
        // Bookings Content
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            if (uiState.bookings.isEmpty()) {
                EmptyBookingsState()
            } else {
                BookingsContent(
                    bookings = uiState.bookings,
                    onBookingClick = onBookingClick,
                    onAcceptBookingClick = onAcceptBookingClick,
                    onRejectBookingClick = onRejectBookingClick,
                    onCancelBookingClick = onCancelBookingClick
                )
            }
        }
    }
}

@Composable
fun HostBookingsHeader(
    totalBookings: Int,
    pendingBookings: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        PaceDreamColors.Info,
                        PaceDreamColors.Info.copy(alpha = 0.9f)
                    )
                )
            )
            .padding(PaceDreamSpacing.LG)
    ) {
        Column {
            Text(
                text = "Booking Management",
                style = PaceDreamTypography.Title1,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Manage your property bookings",
                style = PaceDreamTypography.Body,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Bookings",
                        style = PaceDreamTypography.Caption,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        text = totalBookings.toString(),
                        style = PaceDreamTypography.Title1,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Pending Review",
                        style = PaceDreamTypography.Caption,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        text = pendingBookings.toString(),
                        style = PaceDreamTypography.Title2,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BookingStatusTabs(
    selectedStatus: String,
    onStatusChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Filter by Status",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        val statuses = listOf("All", "Pending", "Confirmed", "Completed", "Cancelled")
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(statuses) { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { onStatusChanged(status) },
                    label = {
                        Text(
                            text = status,
                            style = PaceDreamTypography.Callout,
                            fontWeight = if (selectedStatus == status) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PaceDreamColors.Primary,
                        selectedLabelColor = Color.White,
                        containerColor = PaceDreamColors.Card,
                        labelColor = PaceDreamColors.TextPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun EmptyBookingsState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG)
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.XXXL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "No bookings",
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Text(
                text = "No bookings yet",
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            Text(
                text = "When guests book your properties, they'll appear here for you to manage.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun BookingsContent(
    bookings: List<BookingModel>,
    onBookingClick: (String) -> Unit,
    onAcceptBookingClick: (String) -> Unit,
    onRejectBookingClick: (String) -> Unit,
    onCancelBookingClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Recent Bookings",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        bookings.forEach { booking ->
            HostBookingCard(
                booking = booking,
                onBookingClick = onBookingClick,
                onAcceptClick = onAcceptBookingClick,
                onRejectClick = onRejectBookingClick,
                onCancelClick = onCancelBookingClick
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        }
    }
}

@Composable
fun HostBookingCard(
    booking: BookingModel,
    onBookingClick: (String) -> Unit,
    onAcceptClick: (String) -> Unit,
    onRejectClick: (String) -> Unit,
    onCancelClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Booking #${booking.id.takeLast(6)}",
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                PaceDreamStatusChip(status = booking.status.name, isActive = booking.status == BookingStatus.CONFIRMED)
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            // Guest Info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Guest",
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                
                Column {
                    Text(
                        text = "Guest Booking",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = "${booking.guestCount} guests",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            // Property Info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Property",
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                
                Text(
                    text = booking.propertyName.ifEmpty { "Property" },
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            // Dates and Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Check-in",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    
                    Text(
                        text = booking.startDate,
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Check-out",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    
                    Text(
                        text = booking.endDate,
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Total",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    
                    Text(
                        text = "$${String.format("%.2f", booking.totalPrice)}",
                        style = PaceDreamTypography.Title3,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Action Buttons
            if (booking.status == BookingStatus.PENDING) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    Button(
                        onClick = { onAcceptClick(booking.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Success),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Accept",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    OutlinedButton(
                        onClick = { onRejectClick(booking.id) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PaceDreamColors.Error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Reject",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (booking.status == BookingStatus.CONFIRMED) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                
                OutlinedButton(
                    onClick = { onCancelClick(booking.id) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PaceDreamColors.Error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel Booking",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            // View Details Button
            TextButton(
                onClick = { onBookingClick(booking.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "View Details",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
