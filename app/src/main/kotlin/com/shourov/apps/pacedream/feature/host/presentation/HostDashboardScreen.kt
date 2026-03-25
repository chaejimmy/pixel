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
import com.shourov.apps.pacedream.feature.host.presentation.components.*
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
    onSignOut: () -> Unit = {},
    viewModel: HostDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    if (showLogoutConfirm) {
        HostSignOutDialog(
            onConfirm = {
                showLogoutConfirm = false
                onSignOut()
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }

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
            // Greeting header with dynamic payout status
            item {
                DashboardHeaderSection(
                    userName = uiState.userName,
                    payoutState = uiState.payoutState
                )
            }

            // Error banner with retry
            uiState.error?.let { error ->
                item {
                    HostAlertBanner(
                        text = error,
                        color = PaceDreamColors.Warning,
                        actionLabel = "Retry",
                        onAction = { viewModel.refreshData() }
                    )
                }
            }

            // Payout setup prompt
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

            // Switch to Guest Mode
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                HostSwitchModeRow(
                    onClick = onSwitchToGuestMode,
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                )
            }

            // Sign Out
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                HostSignOutRow(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                )
            }
        }
    }
}

// ── Header ─────────────────────────────────────────────────────

@Composable
private fun DashboardHeaderSection(
    userName: String,
    payoutState: PayoutConnectionState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(top = PaceDreamSpacing.MD, bottom = 6.dp)
    ) {
        val displayName = userName.ifBlank { "Host" }
        // Only show personalized greeting if we have a real name
        val greeting = if (displayName != "Host") {
            "Good ${timeOfDayGreeting()}, $displayName"
        } else {
            "Good ${timeOfDayGreeting()}"
        }
        Text(
            text = greeting,
            style = PaceDreamTypography.Title1,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic payout badge based on actual state
        val (badgeText, badgeColor) = when (payoutState) {
            PayoutConnectionState.CONNECTED -> "Payouts: Connected" to PaceDreamColors.HostAccent
            PayoutConnectionState.PENDING -> "Payouts: Pending setup" to PaceDreamColors.Warning
            PayoutConnectionState.NOT_CONNECTED -> "Payouts: Not connected" to PaceDreamColors.TextSecondary
        }
        HostPayoutBadge(text = badgeText)
    }
}

// ── Payout Setup Prompt ─────────────────────────────────────────

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
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .background(PaceDreamColors.Warning.copy(alpha = 0.10f))
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
                style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.TextPrimary
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
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Quick Actions ─────────────────────────────────────────────────

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
            HostCapsuleButton(
                icon = PaceDreamIcons.Add,
                title = "Create listing",
                onClick = onCreateListing
            )
        }
        item {
            HostCapsuleButton(
                icon = PaceDreamIcons.Home,
                title = "View listings",
                onClick = onViewListings
            )
        }
        item {
            HostCapsuleButton(
                icon = PaceDreamIcons.CreditCard,
                title = "Manage payouts",
                onClick = onManagePayouts
            )
        }
    }
}

// ── KPI Chips ─────────────────────────────────────────────────────

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
        item { HostKpiChip(title = "Active listings", value = "$activeListings", icon = PaceDreamIcons.Home) }
        item { HostKpiChip(title = "Upcoming", value = "$upcomingBookings", icon = PaceDreamIcons.CalendarToday) }
        item { HostKpiChip(title = "Pending", value = "$pendingRequests", icon = PaceDreamIcons.Schedule) }
        item { HostKpiChip(title = "This month", value = "$${String.format("%.0f", monthlyEarnings)}", icon = PaceDreamIcons.AttachMoney) }
    }
}

// ── Upcoming Bookings ─────────────────────────────────────────────

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
        HostSectionHeader(title = "Upcoming bookings", onViewAll = onViewAllClick)

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
            HostEmptyState(
                icon = PaceDreamIcons.CalendarToday,
                title = "No upcoming bookings",
                subtitle = "When guests book your listings, upcoming stays will appear here."
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
            HostInitialsAvatar(name = guestName)

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

// ── Your Listings ─────────────────────────────────────────────────

@Composable
private fun YourListingsSection(
    listings: List<Property>,
    isLoading: Boolean,
    onListingClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 18.dp)) {
        HostSectionHeader(
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
                            .clip(RoundedCornerShape(PaceDreamRadius.LG))
                            .background(PaceDreamColors.Gray100)
                    )
                }
            }
        } else if (listings.isEmpty()) {
            HostEmptyState(
                icon = PaceDreamIcons.Home,
                title = "No listings yet",
                subtitle = "Create your first listing and start welcoming guests.",
                ctaLabel = "Add a Listing",
                onCta = onViewAllClick,
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
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
        shape = RoundedCornerShape(PaceDreamRadius.LG)
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

// ── History Section ───────────────────────────────────────────────

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
        HostSectionHeader(title = "History")

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
            HostEmptyState(
                icon = PaceDreamIcons.Notifications,
                title = "No activity yet",
                subtitle = "Booking confirmations, payouts, and updates will show up here."
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
    val titleLower = event.title.lowercase()
    val eventColor = when {
        titleLower.contains("received") -> PaceDreamColors.HostAccent
        titleLower.contains("pending") -> PaceDreamColors.Warning
        titleLower.contains("hold") -> PaceDreamColors.Error
        titleLower.contains("request") -> PaceDreamColors.HostAccent
        else -> PaceDreamColors.HostAccent
    }
    val eventIcon = when {
        titleLower.contains("received") || titleLower.contains("pending") || titleLower.contains("hold") ->
            PaceDreamIcons.AttachMoney
        else -> PaceDreamIcons.Notifications
    }

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
                    .background(eventColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = eventIcon,
                    contentDescription = null,
                    tint = eventColor,
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

// ── Helper ────────────────────────────────────────────────────────

private fun timeOfDayGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "morning"
        hour < 18 -> "afternoon"
        else -> "evening"
    }
}
