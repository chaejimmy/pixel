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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

/**
 * Host Dashboard Screen - iOS parity.
 *
 * Matches iOS HostDashboardView layout:
 * - Time-of-day greeting header
 * - Payout status badge
 * - Quick action capsules (Create listing, View listings, Manage payouts)
 * - KPI chips (Active listings, Upcoming bookings, Pending requests, Monthly earnings)
 * - Upcoming bookings section
 * - Your listings section (horizontal scroll)
 * - History section (recent events)
 * - Switch to Guest Mode
 * - Pull-to-refresh
 * - Inline error banners
 */
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
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // Header with greeting
            item {
                DashboardHeader(
                    userName = uiState.userName,
                    payoutState = uiState.payoutState,
                    onProfileClick = onProfileClick
                )
            }

            // Inline error banner (iOS parity)
            uiState.error?.let { error ->
                item {
                    InlineErrorBannerComposable(text = error)
                }
            }

            // Quick Actions (capsule buttons like iOS)
            item {
                QuickActionsCapsules(
                    onCreateListing = onAddListingClick,
                    onViewListings = onViewAllListings,
                    onManagePayouts = onEarningsClick
                )
            }

            // KPI Chips (iOS parity)
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
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onSwitchToGuestMode,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.SwapHoriz,
                        contentDescription = null,
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Switch to Guest Mode",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Header (iOS parity: time-of-day greeting + payout badge) ────

@Composable
private fun DashboardHeader(
    userName: String,
    payoutState: PayoutConnectionState,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Good ${timeOfDayGreeting()}, $userName",
            style = PaceDreamTypography.Title1,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Payout status badge (iOS parity)
        val (badgeText, badgeColor) = when (payoutState) {
            PayoutConnectionState.CONNECTED -> "Payouts: Connected" to PaceDreamColors.Success
            PayoutConnectionState.PENDING -> "Payouts: Action required" to PaceDreamColors.Warning
            PayoutConnectionState.NOT_CONNECTED -> "Payouts: Not connected" to PaceDreamColors.TextSecondary
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(badgeColor.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = badgeText,
                style = PaceDreamTypography.Caption,
                color = badgeColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Inline Error Banner (iOS parity: InlineErrorBanner) ─────────

@Composable
private fun InlineErrorBannerComposable(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PaceDreamColors.Error.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = PaceDreamIcons.Warning,
            contentDescription = null,
            tint = PaceDreamColors.Error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = PaceDreamTypography.Caption,
            color = PaceDreamColors.Error,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Quick Actions (iOS parity: capsule buttons) ─────────────────

@Composable
private fun QuickActionsCapsules(
    onCreateListing: () -> Unit,
    onViewListings: () -> Unit,
    onManagePayouts: () -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(top = 16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

// ── KPI Chips (iOS parity) ──────────────────────────────────────

@Composable
private fun KPIChipsRow(
    activeListings: Int,
    upcomingBookings: Int,
    pendingRequests: Int,
    monthlyEarnings: Double
) {
    LazyRow(
        modifier = Modifier.padding(top = 16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { KPIChip(title = "Active listings", value = "$activeListings", icon = PaceDreamIcons.Home) }
        item { KPIChip(title = "Upcoming bookings", value = "$upcomingBookings", icon = PaceDreamIcons.CalendarToday) }
        item { KPIChip(title = "Pending requests", value = "$pendingRequests", icon = PaceDreamIcons.HourglassEmpty) }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
        }
    }
}

// ── Upcoming Bookings (iOS parity) ──────────────────────────────

@Composable
private fun UpcomingBookingsSection(
    bookings: List<HostBookingDTO>,
    isLoading: Boolean,
    onBookingClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming bookings",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "See all",
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        if (isLoading && bookings.isEmpty()) {
            // Shimmer placeholders (iOS parity)
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Gray.copy(alpha = 0.12f))
                )
            }
        } else if (bookings.isEmpty()) {
            Text(
                text = "No upcoming bookings yet.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
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
                Spacer(modifier = Modifier.height(10.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Guest initials circle
            val initials = guestName.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .take(2)
                .joinToString("")
                .ifEmpty { "G" }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listingTitle,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = dateRange,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = payout,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Your Listings (iOS parity: horizontal scroll with cards) ────

@Composable
private fun YourListingsSection(
    listings: List<Property>,
    isLoading: Boolean,
    onListingClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your listings",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "See all",
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        if (isLoading && listings.isEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .width(224.dp)
                            .height(220.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Gray.copy(alpha = 0.12f))
                    )
                }
            }
        } else if (listings.isEmpty()) {
            Text(
                text = "No active listings yet.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
        modifier = Modifier
            .width(224.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Home,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = location,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = price,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── History Section (iOS parity: DashboardEvent feed) ───────────

@Composable
private fun HistorySection(
    events: List<HostDashboardData.DashboardEvent>,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 22.dp)
    ) {
        Text(
            text = "History",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading && events.isEmpty()) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Gray.copy(alpha = 0.12f))
                )
            }
        } else if (events.isEmpty()) {
            Text(
                text = "No recent activity yet.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            events.forEach { event ->
                HistoryEventRow(event)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun HistoryEventRow(event: HostDashboardData.DashboardEvent) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (event.title.lowercase().contains("payment"))
                        PaceDreamIcons.AttachMoney else PaceDreamIcons.Notifications,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    fontSize = 14.sp
                )
                Text(
                    text = event.subtitle,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    maxLines = 1,
                    fontSize = 12.sp
                )
            }

            Text(
                text = DateUtils.getRelativeTimeSpanString(
                    event.createdAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString(),
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

// ── Helper ──────────────────────────────────────────────────────

private fun timeOfDayGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "morning"
        hour < 18 -> "afternoon"
        else -> "evening"
    }
}
