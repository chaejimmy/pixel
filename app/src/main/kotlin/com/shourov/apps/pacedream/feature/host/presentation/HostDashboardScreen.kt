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
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // Greeting header
            item {
                DashboardHeaderSection(
                    userName = uiState.userName,
                    payoutState = uiState.payoutState,
                    onProfileClick = onProfileClick
                )
            }

            // Error banner with retry
            uiState.error?.let { error ->
                item {
                    InlineErrorBannerWithRetry(
                        text = error,
                        onRetry = { viewModel.refreshData() }
                    )
                }
            }

            // Payout setup prompt (website parity: show when eligible)
            if (uiState.shouldShowPayoutSetupPrompt &&
                uiState.payoutState != PayoutConnectionState.CONNECTED) {
                item {
                    PayoutSetupPromptCard(
                        reason = uiState.payoutPromptReason,
                        onSetupClick = onEarningsClick
                    )
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
                    isLoading = uiState.isLoading && !uiState.hasLoaded,
                    onBookingClick = onBookingClick,
                    onViewAllClick = onViewAllBookings
                )
            }

            // Your Listings
            item {
                YourListingsSection(
                    listings = uiState.topActiveListings,
                    isLoading = uiState.isLoading && !uiState.hasLoaded,
                    onListingClick = onListingClick,
                    onViewAllClick = onViewAllListings
                )
            }

            // History
            item {
                HistorySection(
                    events = uiState.recentEvents,
                    isLoading = uiState.isLoading && !uiState.hasLoaded
                )
            }

            // Switch to Guest Mode — iOS parity
            item {
                Spacer(modifier = Modifier.height(18.dp))
                TextButton(
                    onClick = onSwitchToGuestMode,
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD),
                    contentPadding = PaddingValues(vertical = PaceDreamSpacing.SM)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.SwapHoriz,
                        contentDescription = null,
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text(
                        text = "Switch to Guest Mode",
                        style = PaceDreamTypography.Subheadline,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Sign Out — iOS parity
            item {
                TextButton(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD),
                    contentPadding = PaddingValues(vertical = PaceDreamSpacing.SM)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.ExitToApp,
                        contentDescription = null,
                        tint = PaceDreamColors.Error,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text(
                        text = "Sign Out",
                        style = PaceDreamTypography.Subheadline,
                        color = PaceDreamColors.Error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Header ─────────────────────────────────────────────────────
// iOS: Single-line greeting "Good morning, [Name]" at 28pt bold
// with status badge below. No gradient background box.

@Composable
private fun DashboardHeaderSection(
    userName: String,
    payoutState: PayoutConnectionState,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(top = PaceDreamSpacing.MD, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayName = userName.ifBlank { "Host" }
            Text(
                text = "Good ${timeOfDayGreeting()}, $displayName",
                style = PaceDreamTypography.Title1,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

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

        Spacer(modifier = Modifier.height(10.dp))

        // Payout status badge — iOS: StatusBadge capsule
        val (badgeText, badgeColor) = when (payoutState) {
            PayoutConnectionState.CONNECTED -> "Payouts: Connected" to PaceDreamColors.Success
            PayoutConnectionState.PENDING -> "Payouts: Action required" to PaceDreamColors.Warning
            PayoutConnectionState.NOT_CONNECTED -> "Payouts: Not connected" to PaceDreamColors.TextSecondary
        }
        Text(
            text = badgeText,
            style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Bold),
            color = badgeColor,
            modifier = Modifier
                .background(badgeColor.copy(alpha = 0.12f), shape = RoundedCornerShape(PaceDreamRadius.Round))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

// ── Error Banner with Retry ──────────────────────────────────
// iOS: Orange bg, triangle icon, semibold subheadline text, no card elevation

@Composable
private fun InlineErrorBannerWithRetry(text: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS)
            .clip(RoundedCornerShape(12.dp))
            .background(PaceDreamColors.Warning.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = PaceDreamIcons.Warning,
            contentDescription = null,
            tint = PaceDreamColors.Warning,
            modifier = Modifier.size(PaceDreamIconSize.SM)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = PaceDreamTypography.Subheadline,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onRetry,
            contentPadding = PaddingValues(horizontal = PaceDreamSpacing.SM)
        ) {
            Text(
                text = "Retry",
                style = PaceDreamTypography.Footnote,
                color = PaceDreamColors.Warning,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Payout Setup Prompt (website parity) ─────────────────────

@Composable
private fun PayoutSetupPromptCard(
    reason: String?,
    onSetupClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(top = PaceDreamSpacing.SM)
            .clip(RoundedCornerShape(14.dp))
            .background(PaceDreamColors.Warning.copy(alpha = 0.08f))
            .clickable(onClick = onSetupClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PaceDreamColors.Warning.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PaceDreamIcons.CreditCard,
                contentDescription = null,
                tint = PaceDreamColors.Warning,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Set up payouts",
                style = PaceDreamTypography.Subheadline,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = reason ?: "Connect your account to receive earnings from bookings.",
                style = PaceDreamTypography.Footnote,
                color = PaceDreamColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.Warning,
            modifier = Modifier.size(PaceDreamIconSize.SM)
        )
    }
}

// ── Quick Actions ─────────────────────────────────────────────
// iOS: Capsule buttons with 14h/10v padding, 14pt bold text

@Composable
private fun QuickActionsCapsules(
    onCreateListing: () -> Unit,
    onViewListings: () -> Unit,
    onManagePayouts: () -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(top = 18.dp),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
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
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(PaceDreamIconSize.SM)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            color = Color.White,
            style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Bold
        )
    }
}

// ── KPI Chips ─────────────────────────────────────────────────
// iOS: Vertical layout — icon top-left, value 22pt bold, title 12pt semibold
//      160pt width, 14pt padding, 16pt corner radius, soft shadow

@Composable
private fun KPIChipsRow(
    activeListings: Int,
    upcomingBookings: Int,
    pendingRequests: Int,
    monthlyEarnings: Double
) {
    LazyRow(
        modifier = Modifier.padding(top = 18.dp),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
        Column(
            modifier = Modifier
                .width(160.dp)
                .padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = value,
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

// ── Upcoming Bookings ─────────────────────────────────────────
// iOS: 18pt bold header, 13pt semibold "See all", 10pt spacing between cards

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
            .padding(top = 18.dp)
    ) {
        SectionHeader(title = "Upcoming bookings", onViewAll = onViewAllClick)

        Spacer(modifier = Modifier.height(12.dp))

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
            Text(
                text = "No upcoming bookings yet.",
                style = PaceDreamTypography.Subheadline,
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
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Guest initials — iOS: 40pt circle
            val initials = guestName.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .take(2)
                .joinToString("")
                .ifEmpty { "G" }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold,
                    style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listingTitle,
                    style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp),
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateRange,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = payout,
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Your Listings ─────────────────────────────────────────────
// iOS: 224pt card width, 200x120pt image, 12pt padding, 18pt radius

@Composable
private fun YourListingsSection(
    listings: List<Property>,
    isLoading: Boolean,
    onListingClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 18.dp)) {
        SectionHeader(
            title = "Your listings",
            onViewAll = onViewAllClick,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading && listings.isEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .width(224.dp)
                            .height(220.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(PaceDreamColors.Gray100)
                    )
                }
            }
        } else if (listings.isEmpty()) {
            Text(
                text = "No listings yet.",
                style = PaceDreamTypography.Subheadline,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier
                    .padding(horizontal = PaceDreamSpacing.MD)
                    .padding(vertical = 8.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listings) { listing ->
                    ListingMiniCard(
                        title = listing.title,
                        location = "${listing.location.city}, ${listing.location.state}",
                        price = "$${listing.pricing.basePrice.toInt()}/${listing.pricing.unit.ifBlank { "hr" }}",
                        imageUrl = listing.images.firstOrNull() ?: "",
                        onClick = { onListingClick(listing.id) },
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
    imageUrl: String = "",
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.width(224.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUrl.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PaceDreamColors.Gray100),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Home,
                            contentDescription = null,
                            tint = PaceDreamColors.TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp),
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = location.ifBlank { "—" },
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = price,
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── History Section ───────────────────────────────────────────
// iOS: 18pt bold "History" title, 38pt icon circles, 14pt padding, 16pt radius

@Composable
private fun HistorySection(
    events: List<HostDashboardData.DashboardEvent>,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(top = 18.dp)
    ) {
        Text(
            text = "History",
            style = PaceDreamTypography.Headline.copy(fontSize = 18.sp),
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

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
            Text(
                text = "No recent activity yet.",
                style = PaceDreamTypography.Subheadline,
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
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
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
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp),
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.subtitle,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
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
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Shared Section Components ─────────────────────────────────
// iOS: 18pt bold title, 13pt semibold primary-colored "See all"

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
            style = PaceDreamTypography.Headline.copy(fontSize = 18.sp),
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onViewAll) {
            Text(
                text = "See all",
                style = PaceDreamTypography.Footnote,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
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
