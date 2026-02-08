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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostTabScreen(
    onPropertyClick: (String) -> Unit = {},
    onAddPropertyClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {},
    onEarningsClick: () -> Unit = {}
) {
    // Sample data - replace with actual data from ViewModel
    val hostStats = remember {
        listOf(
            HostStatData("Total Earnings", "$2,450", "+12%", true),
            HostStatData("Bookings", "24", "+8%", true),
            HostStatData("Occupancy", "78%", "+5%", true),
            HostStatData("Rating", "4.8", "+0.2", true)
        )
    }
    
    val recentBookings = remember {
        listOf(
            RecentBookingData(
                id = "1",
                propertyName = "Modern Downtown Loft",
                guestName = "Sarah Johnson",
                checkIn = "Feb 15",
                checkOut = "Feb 18",
                amount = 320.0,
                status = "Confirmed"
            ),
            RecentBookingData(
                id = "2",
                propertyName = "Cozy Beach House",
                guestName = "Mike Chen",
                checkIn = "Feb 20",
                checkOut = "Feb 25",
                amount = 450.0,
                status = "Pending"
            )
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Header
        PaceDreamHeroHeader(
            title = "Host Dashboard",
            subtitle = "Manage your properties and earnings",
            modifier = Modifier.padding(PaceDreamSpacing.LG)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(PaceDreamSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.LG)
        ) {
            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary,
                    modifier = Modifier.padding(bottom = PaceDreamSpacing.SM)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    item {
                        QuickActionCard(
                            icon = Icons.Default.Add,
                            title = "Add Property",
                            subtitle = "List your space",
                            onClick = onAddPropertyClick
                        )
                    }
                    item {
                        QuickActionCard(
                            icon = Icons.Default.Analytics,
                            title = "Analytics",
                            subtitle = "View insights",
                            onClick = onAnalyticsClick
                        )
                    }
                    item {
                        QuickActionCard(
                            icon = Icons.Default.AttachMoney,
                            title = "Earnings",
                            subtitle = "Track income",
                            onClick = onEarningsClick
                        )
                    }
                }
            }
            
            // Stats Overview
            item {
                Text(
                    text = "Overview",
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary,
                    modifier = Modifier.padding(bottom = PaceDreamSpacing.SM)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(hostStats) { stat ->
                        HostStatCard(
                            title = stat.title,
                            value = stat.value,
                            change = stat.change,
                            isPositive = stat.isPositive
                        )
                    }
                }
            }
            
            // Recent Bookings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Bookings",
                        style = PaceDreamTypography.Title3,
                        color = PaceDreamColors.TextPrimary
                    )
                    
                    TextButton(onClick = { /* Navigate to all bookings */ }) {
                        Text(
                            text = "View All",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.Primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                
                recentBookings.forEach { booking ->
                    RecentBookingCard(
                        booking = booking,
                        onClick = { onPropertyClick(booking.id) }
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                }
            }
            
            // Properties Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Properties",
                        style = PaceDreamTypography.Title3,
                        color = PaceDreamColors.TextPrimary
                    )
                    
                    TextButton(onClick = { /* Navigate to properties */ }) {
                        Text(
                            text = "Manage",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.Primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                
                // Property cards would go here
                PropertyManagementCard(
                    title = "Modern Downtown Loft",
                    status = "Active",
                    bookings = 12,
                    earnings = 1200.0,
                    onClick = { onPropertyClick("prop1") }
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
fun HostStatCard(
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
                    imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
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
fun RecentBookingCard(
    booking: RecentBookingData,
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
            // Property image placeholder
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.SM))
                    .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Property",
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = booking.propertyName,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = booking.guestName,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
                
                Text(
                    text = "${booking.checkIn} - ${booking.checkOut}",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$${String.format("%.0f", booking.amount)}",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
                
                PaceDreamStatusChip(
                    status = booking.status,
                    isActive = booking.status == "Confirmed"
                )
            }
        }
    }
}

@Composable
fun PropertyManagementCard(
    title: String,
    status: String,
    bookings: Int,
    earnings: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.LG)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                
                PaceDreamStatusChip(
                    status = status,
                    isActive = status == "Active"
                )
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Bookings",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        text = bookings.toString(),
                        style = PaceDreamTypography.Title3,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column {
                    Text(
                        text = "Earnings",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        text = "$${String.format("%.0f", earnings)}",
                        style = PaceDreamTypography.Title3,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class HostStatData(
    val title: String,
    val value: String,
    val change: String,
    val isPositive: Boolean
)

data class RecentBookingData(
    val id: String,
    val propertyName: String,
    val guestName: String,
    val checkIn: String,
    val checkOut: String,
    val amount: Double,
    val status: String
)
