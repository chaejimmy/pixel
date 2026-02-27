package com.shourov.apps.pacedream.feature.host.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*

@Composable
fun HostQuickActions(
    onAddListing: () -> Unit = {},
    onViewAnalytics: () -> Unit = {},
    onManageCalendar: () -> Unit = {},
    onViewEarnings: () -> Unit = {},
    onViewBookings: () -> Unit = {},
    onViewListings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = PaceDreamSpacing.LG)
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
            items(getQuickActions()) { action ->
                QuickActionCard(
                    icon = action.icon,
                    title = action.title,
                    subtitle = action.subtitle,
                    onClick = action.onClick
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
fun HostActionGrid(
    onAddListing: () -> Unit = {},
    onViewAnalytics: () -> Unit = {},
    onManageCalendar: () -> Unit = {},
    onViewEarnings: () -> Unit = {},
    onViewBookings: () -> Unit = {},
    onViewListings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Host Actions",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            HostActionButton(
                icon = PaceDreamIcons.Add,
                title = "Add Listing",
                subtitle = "List your space",
                onClick = onAddListing,
                modifier = Modifier.weight(1f)
            )
            
            HostActionButton(
                icon = PaceDreamIcons.Analytics,
                title = "Analytics",
                subtitle = "View insights",
                onClick = onViewAnalytics,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        // Second row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            HostActionButton(
                icon = PaceDreamIcons.CalendarToday,
                title = "Calendar",
                subtitle = "Manage availability",
                onClick = onManageCalendar,
                modifier = Modifier.weight(1f)
            )
            
            HostActionButton(
                icon = PaceDreamIcons.AttachMoney,
                title = "Earnings",
                subtitle = "Track income",
                onClick = onViewEarnings,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        // Third row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            HostActionButton(
                icon = PaceDreamIcons.Calendar,
                title = "Bookings",
                subtitle = "Manage reservations",
                onClick = onViewBookings,
                modifier = Modifier.weight(1f)
            )
            
            HostActionButton(
                icon = PaceDreamIcons.Home,
                title = "Listings",
                subtitle = "Manage properties",
                onClick = onViewListings,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HostActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
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
                modifier = Modifier.size(24.dp)
            )
            
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

private data class QuickAction(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

private fun getQuickActions(): List<QuickAction> {
    return listOf(
        QuickAction(
            icon = PaceDreamIcons.Add,
            title = "Add Listing",
            subtitle = "List your space",
            onClick = {}
        ),
        QuickAction(
            icon = PaceDreamIcons.Analytics,
            title = "Analytics",
            subtitle = "View insights",
            onClick = {}
        ),
        QuickAction(
            icon = PaceDreamIcons.CalendarToday,
            title = "Calendar",
            subtitle = "Manage availability",
            onClick = {}
        ),
        QuickAction(
            icon = PaceDreamIcons.AttachMoney,
            title = "Earnings",
            subtitle = "Track income",
            onClick = {}
        )
    )
}
