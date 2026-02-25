package com.shourov.apps.pacedream.feature.booking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingTabScreen(
    onBookingClick: (String) -> Unit = {},
    onNewBookingClick: () -> Unit = {}
) {
    val bookingTabs = listOf("Upcoming", "Past", "Cancelled")
    var selectedTab by remember { mutableStateOf(0) }
    
    // Sample data - replace with actual data from ViewModel
    val upcomingBookings = remember {
        listOf(
            BookingModel(
                id = "1",
                propertyId = "prop1",
                userId = "user1",
                hostId = "host1",
                startDate = "2024-02-15",
                endDate = "2024-02-18",
                totalPrice = 450.0,
                status = BookingStatus.CONFIRMED,
                guestCount = 2,
                createdAt = "2024-01-15T10:00:00Z",
                updatedAt = "2024-01-15T10:00:00Z"
            ),
            BookingModel(
                id = "2",
                propertyId = "prop2",
                userId = "user1",
                hostId = "host2",
                startDate = "2024-03-01",
                endDate = "2024-03-05",
                totalPrice = 320.0,
                status = BookingStatus.CONFIRMED,
                guestCount = 1,
                createdAt = "2024-01-20T14:30:00Z",
                updatedAt = "2024-01-20T14:30:00Z"
            )
        )
    }
    
    val pastBookings = remember {
        listOf(
            BookingModel(
                id = "3",
                propertyId = "prop3",
                userId = "user1",
                hostId = "host3",
                startDate = "2023-12-10",
                endDate = "2023-12-15",
                totalPrice = 280.0,
                status = BookingStatus.COMPLETED,
                guestCount = 2,
                createdAt = "2023-11-15T09:00:00Z",
                updatedAt = "2023-12-15T12:00:00Z"
            )
        )
    }
    
    val cancelledBookings = remember {
        listOf(
            BookingModel(
                id = "4",
                propertyId = "prop4",
                userId = "user1",
                hostId = "host4",
                startDate = "2024-01-05",
                endDate = "2024-01-08",
                totalPrice = 180.0,
                status = BookingStatus.CANCELLED,
                guestCount = 1,
                createdAt = "2023-12-20T16:45:00Z",
                updatedAt = "2024-01-02T10:30:00Z"
            )
        )
    }
    
    val currentBookings = when (selectedTab) {
        0 -> upcomingBookings
        1 -> pastBookings
        2 -> cancelledBookings
        else -> emptyList()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Header
        PaceDreamHeroHeader(
            title = "My Bookings",
            subtitle = "Manage your reservations",
            modifier = Modifier.padding(PaceDreamSpacing.LG)
        )
        
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG),
            containerColor = PaceDreamColors.Card,
            contentColor = PaceDreamColors.Primary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = PaceDreamColors.Primary,
                    height = 3.dp
                )
            }
        ) {
            bookingTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = PaceDreamTypography.Callout.copy(
                                fontWeight = if (selectedTab == index) 
                                    FontWeight.SemiBold 
                                else 
                                    FontWeight.Normal
                            ),
                            color = if (selectedTab == index) 
                                PaceDreamColors.Primary 
                            else 
                                PaceDreamColors.TextSecondary
                        )
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        // Content
        if (currentBookings.isEmpty()) {
            PaceDreamEmptyState(
                icon = PaceDreamIcons.CalendarToday,
                title = when (selectedTab) {
                    0 -> "No upcoming bookings"
                    1 -> "No past bookings"
                    2 -> "No cancelled bookings"
                    else -> "No bookings found"
                },
                description = when (selectedTab) {
                    0 -> "Start exploring and book your next stay"
                    1 -> "Your completed trips will appear here"
                    2 -> "Cancelled bookings will appear here"
                    else -> "No bookings to show"
                },
                actionText = if (selectedTab == 0) "Explore Properties" else null,
                onActionClick = if (selectedTab == 0) onNewBookingClick else null
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(PaceDreamSpacing.LG),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                items(currentBookings) { booking ->
                    BookingCard(
                        booking = booking,
                        onClick = { onBookingClick(booking.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun BookingCard(
    booking: BookingModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.LG)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Booking #${booking.id.takeLast(6)}",
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary
                )
                
                PaceDreamStatusChip(
                    status = booking.status.name,
                    isActive = booking.status == BookingStatus.CONFIRMED
                )
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            // Property info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Property image placeholder
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.MD))
                        .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Home,
                        contentDescription = "Property",
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                
                Column {
                    Text(
                        text = "Beautiful Apartment",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Downtown Location",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            // Booking details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Check-in",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        text = formatDate(booking.startDate),
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Column {
                    Text(
                        text = "Check-out",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        text = formatDate(booking.endDate),
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Column {
                    Text(
                        text = "Total",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        text = "$${String.format("%.0f", booking.totalPrice)}",
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            // Guest count
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Person,
                    contentDescription = "Guests",
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                Text(
                    text = "${booking.guestCount} guest${if (booking.guestCount > 1) "s" else ""}",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}
