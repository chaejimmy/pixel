package com.pacedream.app.feature.host

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons

/**
 * HostHomeScreen - Host Dashboard with iOS parity
 *
 * iOS Parity (HostHomeView.swift):
 * - Top bar with "Host Dashboard" title
 * - Stats cards row (Listings, Bookings, Earnings)
 * - Quick actions grid (Create Listing, View Bookings, View Earnings, Manage Payouts)
 * - "Your Listings" horizontal card scroll
 * - "Recent Bookings" section with status badges
 * - Switch to Guest mode button
 * - Pull-to-refresh support
 * - Loading / empty / error states
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostHomeScreen(
    viewModel: HostHomeViewModel = hiltViewModel(),
    onCreateListingClick: () -> Unit = {},
    onViewBookingsClick: () -> Unit = {},
    onViewEarningsClick: () -> Unit = {},
    onManagePayoutsClick: () -> Unit = {},
    onListingClick: (String) -> Unit = {},
    onBookingClick: (String) -> Unit = {},
    onSwitchToGuestMode: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSignOutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Host Dashboard",
                        style = PaceDreamTypography.Title2
                    )
                },
                actions = {
                    IconButton(onClick = { /* notifications */ }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    PaceDreamColors.Card.copy(alpha = 0.9f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.Notifications,
                                contentDescription = "Notifications",
                                modifier = Modifier.size(16.dp),
                                tint = PaceDreamColors.TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            PaceDreamColors.Primary.copy(alpha = 0.06f),
                            PaceDreamColors.Primary.copy(alpha = 0.03f),
                            PaceDreamColors.Background
                        )
                    )
                )
        ) {
            when {
                uiState.isLoading && !uiState.isRefreshing -> {
                    HostDashboardLoading(
                        modifier = Modifier.padding(padding)
                    )
                }
                uiState.error != null && uiState.listings.isEmpty() && uiState.bookings.isEmpty() -> {
                    HostDashboardError(
                        message = uiState.error ?: "Something went wrong",
                        onRetry = { viewModel.loadDashboard() },
                        modifier = Modifier.padding(padding)
                    )
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        HostDashboardContent(
                            uiState = uiState,
                            onCreateListingClick = onCreateListingClick,
                            onViewBookingsClick = onViewBookingsClick,
                            onViewEarningsClick = onViewEarningsClick,
                            onManagePayoutsClick = onManagePayoutsClick,
                            onListingClick = onListingClick,
                            onBookingClick = onBookingClick,
                            onSwitchToGuestMode = {
                                viewModel.switchToGuestMode()
                                onSwitchToGuestMode()
                            },
                            onSignOutClick = { showSignOutDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Sign out confirmation dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    "Sign Out",
                    style = PaceDreamTypography.Title3
                )
            },
            text = {
                Text(
                    "Are you sure you want to sign out of your host account?",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Error
                    ),
                    shape = RoundedCornerShape(PaceDreamRadius.MD)
                ) {
                    Text("Sign Out", style = PaceDreamTypography.Button)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false }
                ) {
                    Text(
                        "Cancel",
                        color = PaceDreamColors.TextPrimary
                    )
                }
            },
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            containerColor = PaceDreamColors.Card
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dashboard Content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HostDashboardContent(
    uiState: HostHomeUiState,
    onCreateListingClick: () -> Unit,
    onViewBookingsClick: () -> Unit,
    onViewEarningsClick: () -> Unit,
    onManagePayoutsClick: () -> Unit,
    onListingClick: (String) -> Unit,
    onBookingClick: (String) -> Unit,
    onSwitchToGuestMode: () -> Unit,
    onSignOutClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.LG)
    ) {
        // Stats Cards Row
        item {
            StatsCardsRow(
                totalListings = uiState.totalListings,
                totalBookings = uiState.totalBookings,
                totalEarnings = uiState.totalEarnings
            )
        }

        // Quick Actions Grid
        item {
            QuickActionsSection(
                onCreateListingClick = onCreateListingClick,
                onViewBookingsClick = onViewBookingsClick,
                onViewEarningsClick = onViewEarningsClick,
                onManagePayoutsClick = onManagePayoutsClick
            )
        }

        // Your Listings Section
        item {
            YourListingsSection(
                listings = uiState.listings,
                onListingClick = onListingClick,
                onCreateListingClick = onCreateListingClick
            )
        }

        // Recent Bookings Section
        item {
            RecentBookingsSection(
                bookings = uiState.bookings,
                onBookingClick = onBookingClick,
                onViewAllClick = onViewBookingsClick
            )
        }

        // Switch to Guest Mode
        item {
            SwitchToGuestModeCTA(onClick = onSwitchToGuestMode)
        }

        // Sign Out
        item {
            SignOutButton(onClick = onSignOutClick)
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stats Cards Row (iOS parity: 3 metric cards in a row)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsCardsRow(
    totalListings: Int,
    totalBookings: Int,
    totalEarnings: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = PaceDreamIcons.Home,
            value = "$totalListings",
            label = "Listings",
            accentColor = PaceDreamColors.Primary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = PaceDreamIcons.DateRange,
            value = "$totalBookings",
            label = "Bookings",
            accentColor = PaceDreamColors.Info
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = PaceDreamIcons.AttachMoney,
            value = formatEarnings(totalEarnings),
            label = "Earnings",
            accentColor = PaceDreamColors.Success
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: Color
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            ),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = PaceDreamColors.Card
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.SM2),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        accentColor.copy(alpha = 0.10f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accentColor
                )
            }

            Text(
                text = value,
                style = PaceDreamTypography.Title2.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = PaceDreamColors.TextPrimary,
                maxLines = 1
            )

            Text(
                text = label,
                style = PaceDreamTypography.Caption.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = PaceDreamColors.TextSecondary,
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick Actions Grid (iOS parity: 2x2 action grid)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsSection(
    onCreateListingClick: () -> Unit,
    onViewBookingsClick: () -> Unit,
    onViewEarningsClick: () -> Unit,
    onManagePayoutsClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
        Text(
            text = "Quick Actions",
            style = PaceDreamTypography.Headline.copy(fontWeight = FontWeight.Bold),
            color = PaceDreamColors.TextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = PaceDreamIcons.AddHome,
                title = "Create Listing",
                subtitle = "Add new space",
                accentColor = PaceDreamColors.Primary,
                onClick = onCreateListingClick
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = PaceDreamIcons.Calendar,
                title = "Bookings",
                subtitle = "View requests",
                accentColor = PaceDreamColors.Info,
                onClick = onViewBookingsClick
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = PaceDreamIcons.Analytics,
                title = "Earnings",
                subtitle = "Track income",
                accentColor = PaceDreamColors.Success,
                onClick = onViewEarningsClick
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = PaceDreamIcons.Payment,
                title = "Payouts",
                subtitle = "Manage payments",
                accentColor = PaceDreamColors.Orange,
                onClick = onManagePayoutsClick
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = PaceDreamColors.Card
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.SM2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        accentColor.copy(alpha = 0.10f),
                        RoundedCornerShape(PaceDreamRadius.MD)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accentColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Subheadline.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = PaceDreamColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Your Listings Section (iOS parity: horizontal scrolling card list)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun YourListingsSection(
    listings: List<HostListing>,
    onListingClick: (String) -> Unit,
    onCreateListingClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Listings",
                style = PaceDreamTypography.Headline.copy(fontWeight = FontWeight.Bold),
                color = PaceDreamColors.TextPrimary
            )
            if (listings.isNotEmpty()) {
                Text(
                    text = "See all",
                    style = PaceDreamTypography.Subheadline.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = PaceDreamColors.Primary,
                    modifier = Modifier.clickable { /* navigate to all listings */ }
                )
            }
        }

        if (listings.isEmpty()) {
            EmptyListingsCard(onCreateListingClick = onCreateListingClick)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2),
                contentPadding = PaddingValues(end = PaceDreamSpacing.MD)
            ) {
                items(listings, key = { it.id }) { listing ->
                    ListingCard(
                        listing = listing,
                        onClick = { onListingClick(listing.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingCard(
    listing: HostListing,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(220.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                ambientColor = Color.Black.copy(alpha = 0.06f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = PaceDreamColors.Card
    ) {
        Column {
            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = PaceDreamRadius.LG,
                            topEnd = PaceDreamRadius.LG
                        )
                    )
                    .background(PaceDreamColors.Gray100)
            ) {
                if (listing.imageUrl != null) {
                    AsyncImage(
                        model = listing.imageUrl,
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = PaceDreamIcons.Image,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        tint = PaceDreamColors.Gray400
                    )
                }

                // Status badge
                Surface(
                    modifier = Modifier
                        .padding(PaceDreamSpacing.SM)
                        .align(Alignment.TopEnd),
                    shape = RoundedCornerShape(PaceDreamRadius.Round),
                    color = statusColor(listing.status).copy(alpha = 0.90f)
                ) {
                    Text(
                        text = listing.status.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = PaceDreamTypography.Caption2.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier.padding(PaceDreamSpacing.SM2),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
            ) {
                Text(
                    text = listing.title,
                    style = PaceDreamTypography.Subheadline.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = PaceDreamColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                listing.location?.let { location ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = PaceDreamColors.TextTertiary
                        )
                        Text(
                            text = location,
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${String.format("%.0f", listing.price)}/${listing.priceUnit}",
                        style = PaceDreamTypography.Subheadline.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = PaceDreamColors.Primary
                    )

                    listing.rating?.let { rating ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = PaceDreamColors.StarRating
                            )
                            Text(
                                text = String.format("%.1f", rating),
                                style = PaceDreamTypography.Caption.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = PaceDreamColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyListingsCard(onCreateListingClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            )
            .clickable(onClick = onCreateListingClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = PaceDreamColors.Card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.LG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        PaceDreamColors.Primary.copy(alpha = 0.10f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.AddHome,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = PaceDreamColors.Primary
                )
            }

            Text(
                text = "No listings yet",
                style = PaceDreamTypography.Headline.copy(fontWeight = FontWeight.Bold),
                color = PaceDreamColors.TextPrimary
            )

            Text(
                text = "Create your first listing and start earning",
                style = PaceDreamTypography.Subheadline,
                color = PaceDreamColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onCreateListingClick,
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaceDreamColors.Primary
                ),
                contentPadding = PaddingValues(
                    horizontal = PaceDreamSpacing.LG,
                    vertical = PaceDreamSpacing.SM2
                )
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    "Create Listing",
                    style = PaceDreamTypography.Button.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Recent Bookings Section (iOS parity: booking cards with status badges)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentBookingsSection(
    bookings: List<HostBooking>,
    onBookingClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Bookings",
                style = PaceDreamTypography.Headline.copy(fontWeight = FontWeight.Bold),
                color = PaceDreamColors.TextPrimary
            )
            if (bookings.isNotEmpty()) {
                Text(
                    text = "See all",
                    style = PaceDreamTypography.Subheadline.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = PaceDreamColors.Primary,
                    modifier = Modifier.clickable(onClick = onViewAllClick)
                )
            }
        }

        if (bookings.isEmpty()) {
            EmptyBookingsCard()
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    bookings.forEachIndexed { index, booking ->
                        BookingRow(
                            booking = booking,
                            onClick = { onBookingClick(booking.id) }
                        )
                        if (index < bookings.lastIndex) {
                            HorizontalDivider(
                                color = PaceDreamColors.Border,
                                modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingRow(
    booking: HostBooking,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2)
    ) {
        // Guest avatar placeholder
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(PaceDreamColors.Gray100, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val initials = booking.guestName.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("")
            Text(
                text = initials.ifEmpty { "?" },
                style = PaceDreamTypography.Subheadline.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = PaceDreamColors.TextSecondary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                Text(
                    text = booking.guestName,
                    style = PaceDreamTypography.Callout.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = PaceDreamColors.TextPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                BookingStatusBadge(status = booking.status)
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = booking.listingTitle,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (booking.checkIn.isNotBlank() || booking.checkOut.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = PaceDreamColors.TextTertiary
                    )
                    Text(
                        text = formatDateRange(booking.checkIn, booking.checkOut),
                        style = PaceDreamTypography.Caption2,
                        color = PaceDreamColors.TextTertiary
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$${String.format("%.2f", booking.totalAmount)}",
                style = PaceDreamTypography.Subheadline.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = PaceDreamColors.TextPrimary
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

@Composable
private fun BookingStatusBadge(status: String) {
    val (bgColor, textColor) = when (status.lowercase()) {
        "confirmed", "approved" -> PaceDreamColors.BookingConfirmed to Color.White
        "pending" -> PaceDreamColors.BookingPending to Color.White
        "cancelled", "canceled", "declined" -> PaceDreamColors.BookingCancelled to Color.White
        "completed" -> PaceDreamColors.Primary to Color.White
        else -> PaceDreamColors.Gray400 to Color.White
    }

    Surface(
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        color = bgColor
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = PaceDreamTypography.Caption2.copy(
                fontWeight = FontWeight.Bold
            ),
            color = textColor
        )
    }
}

@Composable
private fun EmptyBookingsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = PaceDreamColors.Card,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.LG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Icon(
                imageVector = PaceDreamIcons.DateRange,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = PaceDreamColors.Gray400
            )
            Text(
                text = "No bookings yet",
                style = PaceDreamTypography.Subheadline.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = PaceDreamColors.TextSecondary
            )
            Text(
                text = "Once guests book your listings, they'll appear here",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Switch to Guest Mode CTA (iOS parity: gradient button with icon)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SwitchToGuestModeCTA(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PaceDreamColors.Secondary,
                            PaceDreamColors.Secondary.copy(alpha = 0.80f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.SwapHoriz,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Switch to Guest Mode",
                    style = PaceDreamTypography.Headline.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = "Browse and book listings",
                    style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Icon(
                imageVector = PaceDreamIcons.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sign Out Button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SignOutButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.ExitToApp,
                contentDescription = "Sign Out",
                tint = PaceDreamColors.Error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Text(
                text = "Sign Out",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.Error,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading State
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HostDashboardLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.LG)
    ) {
        // Stats shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            repeat(3) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    color = PaceDreamColors.Gray100
                ) {}
            }
        }

        // Quick actions shimmer
        Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    repeat(2) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(PaceDreamRadius.LG),
                            color = PaceDreamColors.Gray100
                        ) {}
                    }
                }
            }
        }

        // Listings shimmer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            color = PaceDreamColors.Gray100
        ) {}

        // Center spinner
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = PaceDreamColors.Primary,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Error State
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HostDashboardError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = PaceDreamIcons.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = PaceDreamColors.Error
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        Text(
            text = "Something went wrong",
            style = PaceDreamTypography.Title3.copy(fontWeight = FontWeight.Bold),
            color = PaceDreamColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Text(
            text = message,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(PaceDreamRadius.Round),
            colors = ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Primary
            ),
            contentPadding = PaddingValues(
                horizontal = PaceDreamSpacing.LG,
                vertical = PaceDreamSpacing.SM2
            )
        ) {
            Text(
                "Try Again",
                style = PaceDreamTypography.Button.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Utilities
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Format earnings amount with appropriate abbreviation
 */
private fun formatEarnings(amount: Double): String {
    return when {
        amount >= 1_000_000 -> "$${String.format("%.1fM", amount / 1_000_000)}"
        amount >= 1_000 -> "$${String.format("%.1fK", amount / 1_000)}"
        amount > 0 -> "$${String.format("%.0f", amount)}"
        else -> "$0"
    }
}

/**
 * Format date range for booking display
 */
private fun formatDateRange(checkIn: String, checkOut: String): String {
    // Truncate ISO dates to short format for display
    val start = checkIn.take(10)
    val end = checkOut.take(10)
    return when {
        start.isNotBlank() && end.isNotBlank() -> "$start - $end"
        start.isNotBlank() -> "From $start"
        end.isNotBlank() -> "Until $end"
        else -> ""
    }
}

/**
 * Map listing status to color
 */
private fun statusColor(status: String): Color {
    return when (status.lowercase()) {
        "active", "published" -> PaceDreamColors.Success
        "draft" -> PaceDreamColors.Warning
        "paused", "inactive" -> PaceDreamColors.Gray500
        "archived" -> PaceDreamColors.Error
        else -> PaceDreamColors.Gray400
    }
}
