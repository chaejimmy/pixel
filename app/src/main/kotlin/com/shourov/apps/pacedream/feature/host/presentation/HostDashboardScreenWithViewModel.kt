package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostDashboardScreenWithViewModel(
    onAddListingClick: () -> Unit = {},
    onListingClick: (String) -> Unit = {},
    onBookingClick: (String) -> Unit = {},
    onEarningsClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: HostDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Host Header with Quick Stats
        item {
            HostHeader(
                userName = uiState.userName,
                totalEarnings = uiState.totalEarnings,
                activeListings = uiState.activeListings,
                onProfileClick = onProfileClick
            )
        }
        
        // Quick Actions
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            QuickActionsSection(
                onAddListingClick = onAddListingClick,
                onEarningsClick = onEarningsClick,
                onAnalyticsClick = onAnalyticsClick
            )
        }
        
        // Performance Metrics
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            PerformanceMetricsSectionVM(
                totalEarnings = uiState.totalEarnings,
                totalBookings = uiState.totalBookings,
                occupancyRate = uiState.occupancyRate,
                averageRating = uiState.averageRating
            )
        }
        
        // Recent Bookings
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            RecentBookingsSection(
                recentBookings = uiState.recentBookings,
                onBookingClick = onBookingClick,
                onViewAllBookingsClick = { /* TODO: Navigate to all bookings */ }
            )
        }
        
        // My Listings
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            MyListingsSection(
                myListings = uiState.myListings,
                onListingClick = onListingClick,
                onViewAllListingsClick = { /* TODO: Navigate to all listings */ }
            )
        }
    }
    
    // Loading and Error States
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PaceDreamColors.Primary)
        }
    }
    
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // TODO: Show error snackbar
        }
    }
}

@Composable
private fun HostHeader(
    userName: String,
    totalEarnings: Double,
    activeListings: Int,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaceDreamSpacing.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.LG)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome back,",
                        style = PaceDreamTypography.Title3,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        text = userName,
                        style = PaceDreamTypography.Title1,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(onClick = onProfileClick) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = PaceDreamColors.Primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickStatCard(
                    title = "Total Earnings",
                    value = "$${String.format("%.0f", totalEarnings)}",
                    icon = Icons.Default.AttachMoney,
                    color = PaceDreamColors.Success
                )
                QuickStatCard(
                    title = "Active Listings",
                    value = activeListings.toString(),
                    icon = Icons.Default.Home,
                    color = PaceDreamColors.Primary
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onAddListingClick: () -> Unit,
    onEarningsClick: () -> Unit,
    onAnalyticsClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Quick Actions",
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            item {
                QuickActionCard(
                    title = "Add Listing",
                    icon = Icons.Default.AddHome,
                    onClick = onAddListingClick
                )
            }
            item {
                QuickActionCard(
                    title = "Earnings",
                    icon = Icons.Default.AttachMoney,
                    onClick = onEarningsClick
                )
            }
            item {
                QuickActionCard(
                    title = "Analytics",
                    icon = Icons.Default.Analytics,
                    onClick = onAnalyticsClick
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamSpacing.MD),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(PaceDreamIconSize.LG)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = title,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PerformanceMetricsSectionVM(
    totalEarnings: Double,
    totalBookings: Int,
    occupancyRate: Double,
    averageRating: Double
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Performance Metrics",
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            PerformanceMetricCard(
                title = "Total Earnings",
                value = "$${String.format("%.0f", totalEarnings)}",
                icon = Icons.Default.AttachMoney,
                color = PaceDreamColors.Success,
                modifier = Modifier.weight(1f)
            )
            PerformanceMetricCard(
                title = "Total Bookings",
                value = totalBookings.toString(),
                icon = Icons.Default.CalendarMonth,
                color = PaceDreamColors.Primary,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            PerformanceMetricCard(
                title = "Occupancy Rate",
                value = "${String.format("%.0f", occupancyRate)}%",
                icon = Icons.Default.TrendingUp,
                color = PaceDreamColors.Warning,
                modifier = Modifier.weight(1f)
            )
            PerformanceMetricCard(
                title = "Average Rating",
                value = String.format("%.1f", averageRating),
                icon = Icons.Default.Star,
                color = PaceDreamColors.Info,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PerformanceMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(PaceDreamIconSize.MD)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    text = title,
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.TextSecondary
                )
            }
            Text(
                text = value,
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecentBookingsSection(
    recentBookings: List<com.shourov.apps.pacedream.model.BookingModel>,
    onBookingClick: (String) -> Unit,
    onViewAllBookingsClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Bookings",
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onViewAllBookingsClick) {
                Text("View All", color = PaceDreamColors.Primary)
            }
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        if (recentBookings.isEmpty()) {
            Text(
                text = "No recent bookings.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                items(recentBookings.take(3)) { booking ->
                    BookingPreviewCard(
                        booking = booking,
                        onClick = { onBookingClick(booking.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MyListingsSection(
    myListings: List<com.shourov.apps.pacedream.model.Property>,
    onListingClick: (String) -> Unit,
    onViewAllListingsClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Listings",
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onViewAllListingsClick) {
                Text("View All", color = PaceDreamColors.Primary)
            }
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        if (myListings.isEmpty()) {
            Text(
                text = "No listings yet.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                items(myListings.take(3)) { listing ->
                    ListingPreviewCard(
                        listing = listing,
                        onClick = { onListingClick(listing.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingPreviewCard(
    booking: com.shourov.apps.pacedream.model.BookingModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = booking.propertyName.ifEmpty { "N/A" },
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
                maxLines = 1
            )
            Text(
                text = "${booking.startDate} - ${booking.endDate}",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
            Text(
                text = "$${String.format("%.0f", booking.totalPrice)}",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ListingPreviewCard(
    listing: com.shourov.apps.pacedream.model.Property,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = listing.title,
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
                maxLines = 1
            )
            Text(
                text = "${listing.location.city}, ${listing.location.country}",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
            Text(
                text = "$${String.format("%.0f", listing.pricing.basePrice)}/night",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
