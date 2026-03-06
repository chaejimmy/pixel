package com.shourov.apps.pacedream.feature.host.presentation

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.data.HostDashboardData
import com.shourov.apps.pacedream.feature.host.data.HostBookingDTO
import com.shourov.apps.pacedream.feature.host.data.PayoutConnectionState
import com.shourov.apps.pacedream.model.Property
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostDashboardScreen(
    onAddListingClick: () -> Unit = {},
    onListingClick: (String) -> Unit = {},
    onBookingClick: (String) -> Unit = {},
    onEarningsClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onViewAllBookings: () -> Unit = {},
    onViewAllListings: () -> Unit = {},
    onSwitchToGuestMode: () -> Unit = {},
    viewModel: HostDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refreshData() },
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = PaceDreamSpacing.XXL)
        ) {
            // Greeting header with gradient
            item {
                DashboardHeaderSection(
                    userName = uiState.userName,
                    payoutState = uiState.payoutState,
                    onProfileClick = onProfileClick
                )
            }

            // Error banner
            uiState.error?.let { error ->
                item {
                    InlineErrorBannerComposable(text = error)
                }
            }

            // Quick Actions
            item {
                QuickActionsCapsules(
                    onCreateListing = onAddListingClick,
                    onViewListings = onViewAllListings,
                    onManagePayouts = onEarningsClick
                )
            }

            // KPI Overview
            item {
                KPIChipsRow(
                    activeListings = uiState.activeListingsCount,
                    upcomingBookings = uiState.upcomingBookingsCount,
                    pendingRequests = uiState.pendingRequestsCount,
                    monthlyEarnings = uiState.monthlyEarnings
                )
            }

            // Upcoming Bookings
            item {
                UpcomingBookingsSection(
                    bookings = uiState.topUpcomingBookings,
                    isLoading = uiState.isLoading,
                    onBookingClick = onBookingClick,
                    onViewAllClick = onViewAllBookings
                )
            }

            // Your Listings
            item {
                YourListingsSection(
                    listings = uiState.topActiveListings,
                    isLoading = uiState.isLoading,
                    onListingClick = onListingClick,
                    onViewAllClick = onViewAllListings
                )
            }

            // History
            item {
                HistorySection(
                    events = uiState.recentEvents,
                    isLoading = uiState.isLoading
                )
            }

            // Switch to Guest Mode
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaceDreamSpacing.MD),
                    onClick = onSwitchToGuestMode,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = CardDefaults.cardColors(
                        containerColor = PaceDreamColors.Info.copy(alpha = 0.08f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.ExitToApp,
                            contentDescription = null,
                            tint = PaceDreamColors.Info,
                            modifier = Modifier.size(PaceDreamIconSize.SM)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        Text(
                            text = "Switch to Guest Mode",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.Info,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ── Header ─────────────────────────────────────────────────────

@Composable
private fun DashboardHeaderSection(
    userName: String,
    payoutState: PayoutConnectionState,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PaceDreamColors.Primary.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(top = PaceDreamSpacing.MD, bottom = PaceDreamSpacing.SM)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Good ${timeOfDayGreeting()},",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextSecondary
                )
                Text(
                    text = userName.ifBlank { "Host" },
                    style = PaceDreamTypography.Title1,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onProfileClick) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Person,
                        contentDescription = "Profile",
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        // Payout status badge
        val (badgeText, badgeColor) = when (payoutState) {
            PayoutConnectionState.CONNECTED -> "Payouts Connected" to PaceDreamColors.Success
            PayoutConnectionState.PENDING -> "Payouts: Action required" to PaceDreamColors.Warning
            PayoutConnectionState.NOT_CONNECTED -> "Payouts: Not connected" to PaceDreamColors.TextSecondary
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(PaceDreamRadius.Round))
                .background(badgeColor.copy(alpha = 0.1f))
                .padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
            Text(
                text = badgeText,
                style = PaceDreamTypography.Caption2,
                color = badgeColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Error Banner ──────────────────────────────────────────────

@Composable
private fun InlineErrorBannerComposable(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Error.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.Warning,
                contentDescription = null,
                tint = PaceDreamColors.Error,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = text,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.Error,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Quick Actions ─────────────────────────────────────────────

@Composable
private fun QuickActionsCapsules(
    onCreateListing: () -> Unit,
    onViewListings: () -> Unit,
    onManagePayouts: () -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(top = PaceDreamSpacing.MD),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        item {
            CapsuleButton(
                icon = PaceDreamIcons.Add,
                title = "Create listing",
                onClick = onCreateListing
            )
        }
        item {
            CapsuleButton(
                icon = PaceDreamIcons.Home,
                title = "View listings",
                onClick = onViewListings
            )
        }
        item {
            CapsuleButton(
                icon = PaceDreamIcons.CreditCard,
                title = "Manage payouts",
                onClick = onManagePayouts
            )
        }
    }
}

@Composable
private fun CapsuleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(PaceDreamIconSize.XS)
        )
        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
        Text(
            text = title,
            color = Color.White,
            style = PaceDreamTypography.Caption,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── KPI Chips ─────────────────────────────────────────────────

@Composable
private fun KPIChipsRow(
    activeListings: Int,
    upcomingBookings: Int,
    pendingRequests: Int,
    monthlyEarnings: Double
) {
    LazyRow(
        modifier = Modifier.padding(top = PaceDreamSpacing.MD),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        item { KPIChip(title = "Active listings", value = "$activeListings", icon = PaceDreamIcons.Home) }
        item { KPIChip(title = "Upcoming", value = "$upcomingBookings", icon = PaceDreamIcons.CalendarToday) }
        item { KPIChip(title = "Pending", value = "$pendingRequests", icon = PaceDreamIcons.Schedule) }
        item { KPIChip(title = "This month", value = "$${String.format("%.0f", monthlyEarnings)}", icon = PaceDreamIcons.AttachMoney) }
    }
}

@Composable
private fun KPIChip(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.SM))
                    .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(PaceDreamIconSize.XS)
                )
            }
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Column {
                Text(
                    text = value,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = PaceDreamTypography.Caption2,
                    color = PaceDreamColors.TextSecondary
                )
            }
        }
    }
}

// ── Upcoming Bookings ─────────────────────────────────────────

@Composable
private fun UpcomingBookingsSection(
    bookings: List<HostBookingDTO>,
    isLoading: Boolean,
    onBookingClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(top = PaceDreamSpacing.LG)
    ) {
        SectionHeader(title = "Upcoming bookings", onViewAll = onViewAllClick)

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        if (isLoading && bookings.isEmpty()) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(vertical = PaceDreamSpacing.XS)
                        .clip(RoundedCornerShape(PaceDreamRadius.LG))
                        .background(PaceDreamColors.Gray100)
                )
            }
        } else if (bookings.isEmpty()) {
            EmptyStateInline(
                text = "No upcoming bookings yet",
                icon = PaceDreamIcons.CalendarToday
            )
        } else {
            bookings.forEach { booking ->
                BookingRowCard(
                    guestName = booking.resolvedGuestName,
                    listingTitle = booking.resolvedListingTitle,
                    dateRange = "${booking.resolvedStart ?: ""} - ${booking.resolvedEnd ?: ""}",
                    payout = "$${String.format("%.0f", booking.resolvedTotal)}",
                    status = booking.status ?: "",
                    onClick = { onBookingClick(booking.id) }
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }
        }
    }
}

@Composable
private fun BookingRowCard(
    guestName: String,
    listingTitle: String,
    dateRange: String,
    payout: String,
    status: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Guest initials
            val initials = guestName.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .take(2)
                .joinToString("")
                .ifEmpty { "G" }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold,
                    style = PaceDreamTypography.Callout
                )
            }

            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listingTitle,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateRange,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    maxLines = 1
                )
            }

            Text(
                text = payout,
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Your Listings ─────────────────────────────────────────────

@Composable
private fun YourListingsSection(
    listings: List<Property>,
    isLoading: Boolean,
    onListingClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(modifier = Modifier.padding(top = PaceDreamSpacing.LG)) {
        SectionHeader(
            title = "Your listings",
            onViewAll = onViewAllClick,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        if (isLoading && listings.isEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(180.dp)
                            .clip(RoundedCornerShape(PaceDreamRadius.LG))
                            .background(PaceDreamColors.Gray100)
                    )
                }
            }
        } else if (listings.isEmpty()) {
            EmptyStateInline(
                text = "No active listings yet",
                icon = PaceDreamIcons.Home,
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                items(listings) { listing ->
                    ListingMiniCard(
                        title = listing.title,
                        location = "${listing.location.city}, ${listing.location.state}",
                        price = "$${listing.pricing.basePrice.toInt()}",
                        onClick = { onListingClick(listing.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingMiniCard(
    title: String,
    location: String,
    price: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.width(200.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PaceDreamColors.Primary.copy(alpha = 0.08f),
                                PaceDreamColors.Primary.copy(alpha = 0.04f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Home,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary.copy(alpha = 0.3f),
                    modifier = Modifier.size(PaceDreamIconSize.XL)
                )
            }

            Column(modifier = Modifier.padding(PaceDreamSpacing.SM)) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = location,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                Text(
                    text = price,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── History Section ───────────────────────────────────────────

@Composable
private fun HistorySection(
    events: List<HostDashboardData.DashboardEvent>,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(top = PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Recent activity",
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        if (isLoading && events.isEmpty()) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = PaceDreamSpacing.XS)
                        .clip(RoundedCornerShape(PaceDreamRadius.LG))
                        .background(PaceDreamColors.Gray100)
                )
            }
        } else if (events.isEmpty()) {
            EmptyStateInline(
                text = "No recent activity yet",
                icon = PaceDreamIcons.Notifications
            )
        } else {
            events.forEach { event ->
                HistoryEventRow(event)
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }
        }
    }
}

@Composable
private fun HistoryEventRow(event: HostDashboardData.DashboardEvent) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (event.title.lowercase().contains("payment"))
                        PaceDreamIcons.AttachMoney else PaceDreamIcons.Notifications,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(PaceDreamIconSize.XS)
                )
            }

            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.subtitle,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    maxLines = 1
                )
            }

            Text(
                text = DateUtils.getRelativeTimeSpanString(
                    event.createdAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString(),
                style = PaceDreamTypography.Caption2,
                color = PaceDreamColors.TextTertiary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Shared Section Components ─────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onViewAll) {
            Text(
                text = "See all",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyStateInline(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .background(PaceDreamColors.Gray50)
            .padding(PaceDreamSpacing.MD),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(PaceDreamIconSize.SM)
        )
        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
        Text(
            text = text,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextSecondary
        )
    }
}

// ── Helper ────────────────────────────────────────────────────

private fun timeOfDayGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "morning"
        hour < 18 -> "afternoon"
        else -> "evening"
    }
}
