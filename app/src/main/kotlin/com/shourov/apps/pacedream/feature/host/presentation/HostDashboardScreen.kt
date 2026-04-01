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
import androidx.compose.ui.graphics.vector.ImageVector
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
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Header ──
            item {
                DashboardHeader(
                    userName = uiState.userName,
                    payoutState = uiState.payoutState
                )
            }

            // ── Error banner (user-friendly, dismissible) ──
            uiState.error?.let { error ->
                item {
                    HostAlertBanner(
                        text = friendlyErrorMessage(error),
                        color = PaceDreamColors.Warning,
                        actionLabel = "Retry",
                        onAction = { viewModel.refreshData() },
                        modifier = Modifier.padding(top = PaceDreamSpacing.SM)
                    )
                }
            }

            // ── Payout setup prompt (only if no error banner to avoid stacking) ──
            if (uiState.error == null &&
                uiState.shouldShowPayoutSetupPrompt &&
                uiState.payoutState != PayoutConnectionState.CONNECTED
            ) {
                item {
                    PayoutSetupPromptCard(
                        reason = uiState.payoutPromptReason,
                        onSetupClick = onEarningsClick
                    )
                }
            }

            // ── Summary card (KPIs in a 2×2 grid) ──
            item {
                SummaryCard(
                    activeListings = uiState.activeListings,
                    underReviewListings = uiState.underReviewListingsCount,
                    upcomingBookings = uiState.upcomingBookingsCount,
                    pendingRequests = uiState.pendingRequestsCount,
                    monthlyEarnings = uiState.monthlyEarnings,
                    modifier = Modifier.padding(
                        horizontal = PaceDreamSpacing.MD,
                        vertical = PaceDreamSpacing.MD
                    )
                )
            }

            // ── Quick actions (compact row, no scrolling needed) ──
            item {
                QuickActionsRow(
                    onCreateListing = onAddListingClick,
                    onViewListings = onViewAllListings,
                    onManagePayouts = onEarningsClick,
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                )
            }

            // Determine if the host is brand new (nothing to show)
            val isNewHost = uiState.hasLoaded &&
                uiState.topUpcomingBookings.isEmpty() &&
                uiState.topActiveListings.isEmpty() &&
                uiState.recentEvents.isEmpty()

            if (isNewHost && uiState.error == null) {
                // ── Welcome state for new hosts ──
                item {
                    NewHostWelcome(
                        onCreateListing = onAddListingClick,
                        modifier = Modifier.padding(top = PaceDreamSpacing.LG)
                    )
                }
            } else {
                // ── Upcoming Bookings ──
                item {
                    UpcomingBookingsSection(
                        bookings = uiState.topUpcomingBookings,
                        isLoading = uiState.isLoading && !uiState.hasLoaded,
                        onBookingClick = onBookingClick,
                        onViewAllClick = onViewAllBookings
                    )
                }

                // ── Your Listings ──
                item {
                    YourListingsSection(
                        listings = uiState.topActiveListings,
                        isLoading = uiState.isLoading && !uiState.hasLoaded,
                        onListingClick = onListingClick,
                        onViewAllClick = onViewAllListings,
                        onAddListing = onAddListingClick
                    )
                }

                // ── Recent Activity ──
                if (uiState.recentEvents.isNotEmpty() || (uiState.isLoading && !uiState.hasLoaded)) {
                    item {
                        RecentActivitySection(
                            events = uiState.recentEvents,
                            isLoading = uiState.isLoading && !uiState.hasLoaded
                        )
                    }
                }
            }

            // ── Footer actions ──
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                HostSwitchModeRow(
                    onClick = onSwitchToGuestMode,
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                )
            }

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

// ═══════════════════════════════════════════════════════════════════════════════
// Header — clean title with optional name, payout status badge
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DashboardHeader(
    userName: String,
    payoutState: PayoutConnectionState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(top = PaceDreamSpacing.MD, bottom = PaceDreamSpacing.SM)
    ) {
        // Greeting — only personalize if we have a real name
        val displayName = userName.trim().let { if (it == "Host" || it.isBlank()) null else it }
        if (displayName != null) {
            Text(
                text = "Good ${timeOfDayGreeting()}, $displayName",
                style = PaceDreamTypography.Title1,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = "Host Dashboard",
                style = PaceDreamTypography.Title1,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dynamic payout badge with correct color
        val (badgeText, badgeColor) = when (payoutState) {
            PayoutConnectionState.CONNECTED ->
                "Payouts connected" to PaceDreamColors.HostAccent
            PayoutConnectionState.PENDING ->
                "Payout setup pending" to PaceDreamColors.Warning
            PayoutConnectionState.NOT_CONNECTED ->
                "Payouts not connected" to PaceDreamColors.TextSecondary
        }
        HostPayoutBadge(text = badgeText, color = badgeColor)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Summary Card — 2×2 KPI grid replacing the two separate horizontal scroll rows
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SummaryCard(
    activeListings: Int,
    underReviewListings: Int,
    upcomingBookings: Int,
    pendingRequests: Int,
    monthlyEarnings: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat for glass
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            PaceDreamColors.Border.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryMetric(
                    icon = PaceDreamIcons.Home,
                    value = "$activeListings",
                    label = "Active listings",
                    modifier = Modifier.weight(1f)
                )
                // iOS parity: show "Under review" KPI only when count > 0
                if (underReviewListings > 0) {
                    SummaryMetric(
                        icon = PaceDreamIcons.Schedule,
                        value = "$underReviewListings",
                        label = "Under review",
                        valueColor = PaceDreamColors.Warning,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    SummaryMetric(
                        icon = PaceDreamIcons.CalendarToday,
                        value = "$upcomingBookings",
                        label = "Upcoming",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Row(modifier = Modifier.fillMaxWidth()) {
                // When under-review KPI displaced Upcoming above, show Upcoming here
                if (underReviewListings > 0) {
                    SummaryMetric(
                        icon = PaceDreamIcons.CalendarToday,
                        value = "$upcomingBookings",
                        label = "Upcoming",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    SummaryMetric(
                        icon = PaceDreamIcons.Schedule,
                        value = "$pendingRequests",
                        label = "Pending",
                        modifier = Modifier.weight(1f)
                    )
                }
                SummaryMetric(
                    icon = PaceDreamIcons.AttachMoney,
                    value = "$${String.format("%.0f", monthlyEarnings)}",
                    label = "This month",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = PaceDreamColors.TextPrimary
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PaceDreamColors.HostAccent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamColors.HostAccent,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = value,
                style = PaceDreamTypography.Title3,
                color = valueColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Quick Actions — compact row that fits without scrolling
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QuickActionsRow(
    onCreateListing: () -> Unit,
    onViewListings: () -> Unit,
    onManagePayouts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompactActionButton(
            icon = PaceDreamIcons.Add,
            label = "New listing",
            onClick = onCreateListing,
            modifier = Modifier.weight(1f)
        )
        CompactActionButton(
            icon = PaceDreamIcons.Home,
            label = "Listings",
            onClick = onViewListings,
            modifier = Modifier.weight(1f)
        )
        CompactActionButton(
            icon = PaceDreamIcons.CreditCard,
            label = "Payouts",
            onClick = onManagePayouts,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        color = PaceDreamColors.HostAccent.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamColors.HostAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.HostAccent,
                maxLines = 1
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Payout Setup Prompt
// ═══════════════════════════════════════════════════════════════════════════════

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
            .background(PaceDreamColors.Warning.copy(alpha = 0.08f))
            .clickable(onClick = onSetupClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PaceDreamColors.Warning.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PaceDreamIcons.CreditCard,
                contentDescription = null,
                tint = PaceDreamColors.Warning,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Set up payouts to get paid",
                style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.TextPrimary
            )
            Text(
                text = reason ?: "Connect your bank account to start receiving earnings.",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// New Host Welcome — single unified empty state for brand-new hosts
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun NewHostWelcome(
    onCreateListing: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

        Icon(
            imageVector = PaceDreamIcons.Home,
            contentDescription = null,
            tint = PaceDreamColors.HostAccent.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        Text(
            text = "Welcome to hosting",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Text(
            text = "Create your first listing to start welcoming guests. Your bookings, earnings, and activity will appear here.",
            style = PaceDreamTypography.Subheadline,
            color = PaceDreamColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        Button(
            onClick = onCreateListing,
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = PaceDreamIcons.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create your first listing",
                style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Upcoming Bookings
// ═══════════════════════════════════════════════════════════════════════════════

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
        HostSectionHeader(title = "Upcoming bookings", onViewAll = onViewAllClick)

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))

        if (isLoading && bookings.isEmpty()) {
            repeat(2) { index ->
                if (index > 0) Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.LG))
                        .background(PaceDreamColors.Gray100)
                )
            }
        } else if (bookings.isEmpty()) {
            HostEmptyState(
                icon = PaceDreamIcons.CalendarToday,
                title = "No upcoming bookings",
                subtitle = "When guests book your listings, their stays will appear here."
            )
        } else {
            bookings.forEach { booking ->
                BookingRowCard(
                    guestName = booking.resolvedGuestName,
                    listingTitle = booking.resolvedListingTitle,
                    dateRange = buildString {
                        booking.resolvedStart?.let { append(it) }
                        append(" – ")
                        booking.resolvedEnd?.let { append(it) }
                    }.trim(),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            PaceDreamColors.Border.copy(alpha = 0.3f)
        )
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
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = guestName,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateRange,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextTertiary,
                    maxLines = 1
                )
            }

            Text(
                text = payout,
                style = PaceDreamTypography.Subheadline,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Your Listings
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun YourListingsSection(
    listings: List<Property>,
    isLoading: Boolean,
    onListingClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onAddListing: () -> Unit
) {
    Column(modifier = Modifier.padding(top = PaceDreamSpacing.LG)) {
        HostSectionHeader(
            title = "Your listings",
            onViewAll = if (listings.isNotEmpty()) onViewAllClick else null,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))

        if (isLoading && listings.isEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            HostEmptyState(
                icon = PaceDreamIcons.Home,
                title = "No listings yet",
                subtitle = "Create your first listing to start welcoming guests.",
                ctaLabel = "Create listing",
                onCta = onAddListing,
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
                        statusText = listing.displayStatus,
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
    statusText: String = "",
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.width(200.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            PaceDreamColors.Border.copy(alpha = 0.3f)
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
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
                            tint = PaceDreamColors.TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = location.ifBlank { "—" },
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                // iOS parity: price + status badge row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = price,
                        style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Bold),
                        color = PaceDreamColors.HostAccent
                    )
                    if (statusText.isNotBlank()) {
                        ListingStatusBadge(status = statusText)
                    }
                }
            }
        }
    }
}

/** iOS parity: StatusBadge for listing status with color-coded styling */
@Composable
private fun ListingStatusBadge(status: String) {
    val lower = status.lowercase()
    val (bgColor, fgColor) = when {
        lower.contains("review") || lower.contains("pending") ->
            PaceDreamColors.Warning.copy(alpha = 0.16f) to PaceDreamColors.Warning
        lower.contains("reject") ->
            PaceDreamColors.Error.copy(alpha = 0.14f) to PaceDreamColors.Error
        lower.contains("active") || lower.contains("publish") ->
            PaceDreamColors.HostAccent.copy(alpha = 0.14f) to PaceDreamColors.HostAccent
        else ->
            Color.Gray.copy(alpha = 0.14f) to PaceDreamColors.TextSecondary
    }
    Text(
        text = status,
        style = PaceDreamTypography.Caption.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        ),
        color = fgColor,
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// Recent Activity — replaces "History"
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RecentActivitySection(
    events: List<HostDashboardData.DashboardEvent>,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(top = PaceDreamSpacing.LG)
    ) {
        HostSectionHeader(title = "Recent activity")

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))

        if (isLoading && events.isEmpty()) {
            repeat(2) { index ->
                if (index > 0) Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.LG))
                        .background(PaceDreamColors.Gray100)
                )
            }
        } else {
            events.forEach { event ->
                ActivityEventRow(event)
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }
        }
    }
}

@Composable
private fun ActivityEventRow(event: HostDashboardData.DashboardEvent) {
    val titleLower = event.title.lowercase()
    val eventColor = when {
        titleLower.contains("received") -> PaceDreamColors.HostAccent
        titleLower.contains("pending") -> PaceDreamColors.Warning
        titleLower.contains("hold") -> PaceDreamColors.Error
        titleLower.contains("request") -> PaceDreamColors.HostAccent
        else -> PaceDreamColors.HostAccent
    }
    val eventIcon = when {
        titleLower.contains("received") || titleLower.contains("pending") ||
            titleLower.contains("hold") -> PaceDreamIcons.AttachMoney
        else -> PaceDreamIcons.Notifications
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(eventColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = eventIcon,
                    contentDescription = null,
                    tint = eventColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.subtitle,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                color = PaceDreamColors.TextTertiary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════════════════════════════════════════

private fun timeOfDayGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "morning"
        hour < 18 -> "afternoon"
        else -> "evening"
    }
}

/** Map raw backend error strings to user-friendly messages */
private fun friendlyErrorMessage(raw: String): String {
    val lower = raw.lowercase()
    return when {
        lower.contains("network") || lower.contains("connect") || lower.contains("timeout") ->
            "Couldn't connect. Check your internet and try again."
        lower.contains("unauthorized") || lower.contains("401") || lower.contains("auth") ->
            "Your session has expired. Please sign in again."
        lower.contains("server") || lower.contains("500") || lower.contains("internal") ->
            "Something went wrong on our end. Please try again shortly."
        lower.contains("not found") || lower.contains("404") ->
            "We couldn't find your dashboard data. Pull to refresh."
        raw.length > 80 ->
            "Something went wrong. Pull to refresh."
        else -> raw
    }
}
