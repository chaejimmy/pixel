package com.shourov.apps.pacedream.feature.host.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    Column(modifier = modifier.padding(horizontal = PaceDreamSpacing.MD)) {
        Text(
            text = "Quick Actions",
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(
                getQuickActions(
                    onAddListing = onAddListing,
                    onViewAnalytics = onViewAnalytics,
                    onManageCalendar = onManageCalendar,
                    onViewEarnings = onViewEarnings
                )
            ) { action ->
                QuickActionCard(
                    icon = action.icon,
                    title = action.title,
                    subtitle = action.subtitle,
                    tint = action.tint,
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
    tint: Color = PaceDreamColors.Primary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(120.dp),
        onClick = onClick,
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = title,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = PaceDreamTypography.Caption2,
                color = PaceDreamColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
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
    Column(modifier = modifier.padding(horizontal = PaceDreamSpacing.MD)) {
        Text(
            text = "Host Actions",
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            HostActionButton(
                icon = PaceDreamIcons.Add,
                title = "Add Listing",
                subtitle = "List your space",
                tint = PaceDreamColors.Primary,
                onClick = onAddListing,
                modifier = Modifier.weight(1f)
            )
            HostActionButton(
                icon = PaceDreamIcons.Analytics,
                title = "Analytics",
                subtitle = "View insights",
                tint = PaceDreamColors.Info,
                onClick = onViewAnalytics,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            HostActionButton(
                icon = PaceDreamIcons.CalendarToday,
                title = "Calendar",
                subtitle = "Manage availability",
                tint = PaceDreamColors.Warning,
                onClick = onManageCalendar,
                modifier = Modifier.weight(1f)
            )
            HostActionButton(
                icon = PaceDreamIcons.AttachMoney,
                title = "Earnings",
                subtitle = "Track income",
                tint = PaceDreamColors.Success,
                onClick = onViewEarnings,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            HostActionButton(
                icon = PaceDreamIcons.Calendar,
                title = "Bookings",
                subtitle = "Manage reservations",
                tint = PaceDreamColors.Error,
                onClick = onViewBookings,
                modifier = Modifier.weight(1f)
            )
            HostActionButton(
                icon = PaceDreamIcons.Home,
                title = "Listings",
                subtitle = "Manage properties",
                tint = PaceDreamColors.Primary,
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
    tint: Color = PaceDreamColors.Primary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        onClick = onClick,
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamSpacing.MD),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = title,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                style = PaceDreamTypography.Caption2,
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

private data class QuickAction(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val tint: Color,
    val onClick: () -> Unit
)

private fun getQuickActions(
    onAddListing: () -> Unit = {},
    onViewAnalytics: () -> Unit = {},
    onManageCalendar: () -> Unit = {},
    onViewEarnings: () -> Unit = {}
): List<QuickAction> {
    return listOf(
        QuickAction(
            icon = PaceDreamIcons.Add,
            title = "Add Listing",
            subtitle = "List your space",
            tint = PaceDreamColors.Primary,
            onClick = onAddListing
        ),
        QuickAction(
            icon = PaceDreamIcons.Analytics,
            title = "Analytics",
            subtitle = "View insights",
            tint = PaceDreamColors.Info,
            onClick = onViewAnalytics
        ),
        QuickAction(
            icon = PaceDreamIcons.CalendarToday,
            title = "Calendar",
            subtitle = "Manage availability",
            tint = PaceDreamColors.Warning,
            onClick = onManageCalendar
        ),
        QuickAction(
            icon = PaceDreamIcons.AttachMoney,
            title = "Earnings",
            subtitle = "Track income",
            tint = PaceDreamColors.Success,
            onClick = onViewEarnings
        )
    )
}
