package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
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
import com.shourov.apps.pacedream.feature.host.data.HostDashboardData
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.Property

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostDashboardScreen(
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
        
        // Quick Actions Grid
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            QuickActionsGrid(
                onAddListing = onAddListingClick,
                onViewAnalytics = onAnalyticsClick,
                onManageCalendar = { /* Navigate to calendar */ },
                onViewEarnings = onEarningsClick
            )
        }
        
        // Performance Metrics
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            PerformanceMetricsSection(
                monthlyEarnings = uiState.monthlyEarnings,
                occupancyRate = uiState.occupancyRate,
                averageRating = uiState.averageRating,
                totalBookings = uiState.totalBookings
            )
        }
        
        // Recent Bookings
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            RecentBookingsSection(
                bookings = uiState.recentBookings,
                onBookingClick = onBookingClick,
                onViewAllClick = { /* Navigate to all bookings */ }
            )
        }
        
        // My Listings Preview
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            MyListingsSection(
                listings = uiState.myListings,
                onListingClick = onListingClick,
                onViewAllClick = { /* Navigate to all listings */ }
            )
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PaceDreamColors.Primary,
                        PaceDreamColors.Primary.copy(alpha = 0.9f)
                    )
                )
            )
            .padding(PaceDreamSpacing.LG)
    ) {
        Column {
            // Welcome Message
            Text(
                text = "Welcome back, $userName!",
                style = PaceDreamTypography.Title1,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Manage your hosting business",
                style = PaceDreamTypography.Body,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            // Quick Stats Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                QuickStatCard(
                    title = "Total Earnings",
                    value = "$${String.format("%.0f", totalEarnings)}",
                    icon = PaceDreamIcons.AttachMoney,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                
                QuickStatCard(
                    title = "Active Listings",
                    value = activeListings.toString(),
                    icon = PaceDreamIcons.Home,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun QuickStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            Text(
                text = value,
                style = PaceDreamTypography.Title2,
                color = color,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = title,
                style = PaceDreamTypography.Caption,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun QuickActionsGrid(
    onAddListing: () -> Unit,
    onViewAnalytics: () -> Unit,
    onManageCalendar: () -> Unit,
    onViewEarnings: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Quick Actions",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            item {
                QuickActionCard(
                    icon = PaceDreamIcons.Add,
                    title = "Add Listing",
                    subtitle = "List your space",
                    onClick = onAddListing
                )
            }
            item {
                QuickActionCard(
                    icon = PaceDreamIcons.Analytics,
                    title = "Analytics",
                    subtitle = "View insights",
                    onClick = onViewAnalytics
                )
            }
            item {
                QuickActionCard(
                    icon = PaceDreamIcons.CalendarToday,
                    title = "Calendar",
                    subtitle = "Manage availability",
                    onClick = onManageCalendar
                )
            }
            item {
                QuickActionCard(
                    icon = PaceDreamIcons.AttachMoney,
                    title = "Earnings",
                    subtitle = "Track income",
                    onClick = onViewEarnings
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.MD))
                    .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            Text(
                text = title,
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = subtitle,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

@Composable
fun PerformanceMetricsSection(
    monthlyEarnings: Double,
    occupancyRate: Double,
    averageRating: Double,
    totalBookings: Int
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Performance Overview",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            item {
                PerformanceMetricCard(
                    title = "Monthly Earnings",
                    value = "$${String.format("%.0f", monthlyEarnings)}",
                    change = "+12%",
                    isPositive = true
                )
            }
            item {
                PerformanceMetricCard(
                    title = "Occupancy Rate",
                    value = "${String.format("%.0f", occupancyRate)}%",
                    change = "+5%",
                    isPositive = true
                )
            }
            item {
                PerformanceMetricCard(
                    title = "Average Rating",
                    value = String.format("%.1f", averageRating),
                    change = "+0.2",
                    isPositive = true
                )
            }
            item {
                PerformanceMetricCard(
                    title = "Total Bookings",
                    value = totalBookings.toString(),
                    change = "+8",
                    isPositive = true
                )
            }
        }
    }
}

@Composable
fun PerformanceMetricCard(
    title: String,
    value: String,
    change: String,
    isPositive: Boolean
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD)
        ) {
            Text(
                text = title,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            
            Text(
                text = value,
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPositive) PaceDreamIcons.TrendingUp else PaceDreamIcons.TrendingDown,
                    contentDescription = "Trend",
                    tint = if (isPositive) PaceDreamColors.Success else PaceDreamColors.Error,
                    modifier = Modifier.size(12.dp)
                )
                
                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                
                Text(
                    text = change,
                    style = PaceDreamTypography.Caption,
                    color = if (isPositive) PaceDreamColors.Success else PaceDreamColors.Error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RecentBookingsSection(
    bookings: List<BookingModel>,
    onBookingClick: (String) -> Unit,
    onViewAllClick: () -> Unit
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
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "View All",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.Primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        if (bookings.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card)
            ) {
                Column(
                    modifier = Modifier.padding(PaceDreamSpacing.LG),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.CalendarToday,
                        contentDescription = "No bookings",
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    
                    Text(
                        text = "No recent bookings",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
        } else {
            bookings.take(3).forEach { booking ->
                HostBookingPreviewCard(
                    booking = booking,
                    onClick = { onBookingClick(booking.id) }
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }
        }
    }
}

@Composable
fun HostBookingPreviewCard(
    booking: BookingModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.MD)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
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
                    imageVector = PaceDreamIcons.Person,
                    contentDescription = "Guest",
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Guest Booking",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "${booking.startDate} - ${booking.endDate}",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
            
            Text(
                text = "$${String.format("%.0f", booking.totalPrice)}",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MyListingsSection(
    listings: List<Property>,
    onListingClick: (String) -> Unit,
    onViewAllClick: () -> Unit
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
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "Manage All",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.Primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        if (listings.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card)
            ) {
                Column(
                    modifier = Modifier.padding(PaceDreamSpacing.LG),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Home,
                        contentDescription = "No listings",
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    
                    Text(
                        text = "No listings yet",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    
                    Button(
                        onClick = { /* Navigate to add listing */ },
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary)
                    ) {
                        Text("Add Your First Listing")
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                items(listings.take(3)) { listing ->
                    HostListingPreviewCard(
                        listing = listing,
                        onClick = { onListingClick(listing.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HostListingPreviewCard(
    listing: Property,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Column {
            // Property image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Home,
                    contentDescription = "Property",
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Column(
                modifier = Modifier.padding(PaceDreamSpacing.MD)
            ) {
                Text(
                    text = listing.title,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                
                Text(
                    text = "${listing.location.city}, ${listing.location.country}",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                
                Text(
                    text = "$${listing.pricing.basePrice.toInt()}/hour",
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
