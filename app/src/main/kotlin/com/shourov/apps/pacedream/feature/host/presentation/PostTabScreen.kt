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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.data.HostBookingDTO
import com.shourov.apps.pacedream.feature.host.data.HostDashboardData
import com.shourov.apps.pacedream.model.Property

/**
 * Post Tab Screen - fully backend-driven.
 *
 * Uses HostDashboardViewModel to load real data from:
 * - GET /hosts/dashboard/overview (KPIs)
 * - GET /bookings/host (bookings)
 * - GET /host/listings (listings)
 * - GET /host/payouts/status (payout state)
 *
 * Supports: loading, success, empty, and error states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostTabScreen(
    onPropertyClick: (String) -> Unit = {},
    onAddPropertyClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {},
    onEarningsClick: () -> Unit = {},
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            PaceDreamHeroHeader(
                title = "Host Dashboard",
                subtitle = "Manage your properties and earnings",
                modifier = Modifier.padding(PaceDreamSpacing.LG)
            )

            // Error banner
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = CardDefaults.cardColors(
                        containerColor = PaceDreamColors.Error.copy(alpha = 0.08f)
                    ),
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
                            text = error,
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.Error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.refreshData() }) {
                            Text(
                                text = "Retry",
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.Error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

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
                                icon = PaceDreamIcons.Add,
                                title = "Add Property",
                                subtitle = "List your space",
                                onClick = onAddPropertyClick
                            )
                        }
                        item {
                            QuickActionCard(
                                icon = PaceDreamIcons.Analytics,
                                title = "Analytics",
                                subtitle = "View insights",
                                onClick = onAnalyticsClick
                            )
                        }
                        item {
                            QuickActionCard(
                                icon = PaceDreamIcons.AttachMoney,
                                title = "Earnings",
                                subtitle = "Track income",
                                onClick = onEarningsClick
                            )
                        }
                    }
                }

                // Stats Overview - from backend
                item {
                    Text(
                        text = "Overview",
                        style = PaceDreamTypography.Title3,
                        color = PaceDreamColors.TextPrimary,
                        modifier = Modifier.padding(bottom = PaceDreamSpacing.SM)
                    )

                    if (uiState.isLoading && uiState.totalBookings == 0) {
                        // Loading placeholders
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                        ) {
                            items(4) {
                                Box(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .height(90.dp)
                                        .clip(RoundedCornerShape(PaceDreamRadius.LG))
                                        .background(PaceDreamColors.Gray100)
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                        ) {
                            item {
                                HostStatCard(
                                    title = "Active Listings",
                                    value = "${uiState.activeListingsCount}",
                                    change = "",
                                    isPositive = true
                                )
                            }
                            item {
                                HostStatCard(
                                    title = "Bookings",
                                    value = "${uiState.totalBookings}",
                                    change = "",
                                    isPositive = true
                                )
                            }
                            item {
                                HostStatCard(
                                    title = "This Month",
                                    value = "$${String.format("%.0f", uiState.monthlyEarnings)}",
                                    change = "",
                                    isPositive = uiState.monthlyEarnings > 0
                                )
                            }
                            item {
                                HostStatCard(
                                    title = "Rating",
                                    value = if (uiState.averageRating > 0) String.format("%.1f", uiState.averageRating) else "—",
                                    change = "",
                                    isPositive = uiState.averageRating >= 4.0
                                )
                            }
                        }
                    }
                }

                // Recent Bookings - from backend
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

                    val upcomingBookings = uiState.topUpcomingBookings
                    if (uiState.isLoading && upcomingBookings.isEmpty()) {
                        // Loading placeholders
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
                    } else if (upcomingBookings.isEmpty()) {
                        // Empty state
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(PaceDreamRadius.MD))
                                .background(PaceDreamColors.Gray50)
                                .padding(PaceDreamSpacing.MD),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.CalendarToday,
                                contentDescription = null,
                                tint = PaceDreamColors.TextTertiary,
                                modifier = Modifier.size(PaceDreamIconSize.SM)
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                            Text(
                                text = "No upcoming bookings yet",
                                style = PaceDreamTypography.Callout,
                                color = PaceDreamColors.TextSecondary
                            )
                        }
                    } else {
                        upcomingBookings.forEach { booking ->
                            RecentBookingCardFromApi(
                                booking = booking,
                                onClick = { onPropertyClick(booking.id) }
                            )
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        }
                    }
                }

                // Properties Section - from backend
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

                    val activeListings = uiState.topActiveListings
                    if (uiState.isLoading && activeListings.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(PaceDreamRadius.LG))
                                .background(PaceDreamColors.Gray100)
                        )
                    } else if (activeListings.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(PaceDreamRadius.MD))
                                .background(PaceDreamColors.Gray50)
                                .padding(PaceDreamSpacing.MD),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.Home,
                                contentDescription = null,
                                tint = PaceDreamColors.TextTertiary,
                                modifier = Modifier.size(PaceDreamIconSize.SM)
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                            Text(
                                text = "No active listings yet",
                                style = PaceDreamTypography.Callout,
                                color = PaceDreamColors.TextSecondary
                            )
                        }
                    } else {
                        activeListings.forEach { listing ->
                            PropertyManagementCardFromApi(
                                listing = listing,
                                onClick = { onPropertyClick(listing.id) }
                            )
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        }
                    }
                }
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

            if (change.isNotBlank()) {
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
}

/**
 * Booking card driven by real HostBookingDTO from the API.
 */
@Composable
fun RecentBookingCardFromApi(
    booking: HostBookingDTO,
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
            // Guest initials
            val initials = booking.resolvedGuestName.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .take(2)
                .joinToString("")
                .ifEmpty { "G" }

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = booking.resolvedListingTitle,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = booking.resolvedGuestName,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )

                val dateRange = listOfNotNull(booking.resolvedStart, booking.resolvedEnd)
                    .joinToString(" - ")
                if (dateRange.isNotBlank()) {
                    Text(
                        text = dateRange,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (booking.resolvedTotal > 0) {
                    Text(
                        text = "$${String.format("%.0f", booking.resolvedTotal)}",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                booking.status?.let { status ->
                    PaceDreamStatusChip(
                        status = status,
                        isActive = status.lowercase().contains("confirm") ||
                            status.lowercase().contains("active") ||
                            status.lowercase().contains("accept")
                    )
                }
            }
        }
    }
}

/**
 * Property management card driven by real Property from the API.
 */
@Composable
fun PropertyManagementCardFromApi(
    listing: Property,
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
                    text = listing.title,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                val statusText = if (listing.isAvailable) "Active" else "Inactive"
                PaceDreamStatusChip(
                    status = statusText,
                    isActive = listing.isAvailable
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            val location = "${listing.location.city}, ${listing.location.state}".trim(", ".toCharArray())
            if (location.isNotBlank() && location != ",") {
                Text(
                    text = location,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }

            if (listing.pricing.basePrice > 0) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                Text(
                    text = "$${listing.pricing.basePrice.toInt()}/${listing.pricing.unit.ifBlank { "hr" }}",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Keep these data classes for backward compat if referenced elsewhere,
// but they are no longer used by PostTabScreen.
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
