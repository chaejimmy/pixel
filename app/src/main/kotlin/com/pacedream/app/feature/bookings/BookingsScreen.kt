package com.pacedream.app.feature.bookings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamIconSize
import com.pacedream.common.icon.PaceDreamIcons
import com.pacedream.common.composables.components.PaceDreamEmptyState
import com.pacedream.common.composables.components.PaceDreamErrorState
import com.pacedream.common.composables.components.PaceDreamLockedState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    onBookingClick: (String) -> Unit,
    onBookingClickWithData: ((BookingListItem) -> Unit)? = null,
    viewModel: BookingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // ── Header (matching iOS: title + count + filter tabs) ──
        BookingsHeader(
            bookingCount = uiState.allBookings.size,
            selectedTab = uiState.selectedTab,
            countProvider = { uiState.count(it) },
            onTabSelected = { viewModel.selectTab(it) }
        )

        HorizontalDivider(color = PaceDreamColors.Gray200, thickness = 0.5.dp)

        // ── Main content area ──
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                // Error with no data
                uiState.error != null && uiState.allBookings.isEmpty() -> {
                    PaceDreamErrorState(
                        title = "Couldn't load bookings",
                        description = uiState.error ?: "An unexpected error occurred",
                        onRetryClick = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Loading with no data → skeleton
                uiState.isLoading && uiState.allBookings.isEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = PaceDreamSpacing.MD,
                            vertical = PaceDreamSpacing.SM2
                        ),
                        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
                    ) {
                        items(4) {
                            BookingCardSkeleton()
                        }
                    }
                }

                // Empty state for selected tab
                uiState.filteredBookings.isEmpty() -> {
                    val emptyConfig = when (uiState.selectedTab) {
                        BookingTab.ALL -> Pair(PaceDreamIcons.ListIcon, "No bookings found")
                        BookingTab.UPCOMING -> Pair(PaceDreamIcons.CalendarToday, "No upcoming bookings")
                        BookingTab.PAST -> Pair(PaceDreamIcons.CheckCircle, "No past bookings")
                        BookingTab.CANCELLED -> Pair(PaceDreamIcons.Cancel, "No cancelled bookings")
                    }
                    PaceDreamEmptyState(
                        title = emptyConfig.second,
                        description = when (uiState.selectedTab) {
                            BookingTab.UPCOMING -> "When you book a stay, it will appear here."
                            BookingTab.PAST -> "Completed stays will appear here after checkout."
                            BookingTab.CANCELLED -> "Cancelled or refunded bookings will show up here."
                            else -> "Start exploring and find your next stay!"
                        },
                        icon = emptyConfig.first,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Content
                else -> {
                    // Inline error banner if there's an error but we still have data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = PaceDreamSpacing.MD,
                            vertical = PaceDreamSpacing.SM2
                        ),
                        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
                    ) {
                        if (uiState.error != null) {
                            item {
                                com.pacedream.common.composables.components.InlineErrorBanner(
                                    message = uiState.error ?: "An unexpected error occurred",
                                    onAction = { viewModel.refresh() },
                                    actionText = "Retry"
                                )
                            }
                        }

                        if (uiState.hostBookingsError != null) {
                            item {
                                com.pacedream.common.composables.components.InlineErrorBanner(
                                    message = uiState.hostBookingsError ?: "",
                                    onAction = { viewModel.refresh() },
                                    actionText = "Retry"
                                )
                            }
                        }

                        items(uiState.filteredBookings, key = { it.id }) { booking ->
                            UnifiedBookingCard(
                                item = booking,
                                statusConfig = viewModel.statusConfig(booking),
                                onViewDetails = {
                                    if (onBookingClickWithData != null) {
                                        onBookingClickWithData(booking)
                                    } else {
                                        onBookingClick(booking.id)
                                    }
                                }
                            )
                        }

                        // Bottom padding for nav bar
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Header — matching iOS "Bookings" + count + filter tabs
// ============================================================================
@Composable
private fun BookingsHeader(
    bookingCount: Int,
    selectedTab: BookingTab,
    countProvider: (BookingTab) -> Int,
    onTabSelected: (BookingTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = PaceDreamSpacing.MD,
                end = PaceDreamSpacing.MD,
                top = PaceDreamSpacing.MD,
                bottom = PaceDreamSpacing.SM
            )
    ) {
        Text(
            text = "Bookings",
            style = PaceDreamTypography.Title1,
            color = PaceDreamColors.TextPrimary
        )

        if (bookingCount > 0) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = "$bookingCount booking${if (bookingCount == 1) "" else "s"}",
                style = PaceDreamTypography.Subheadline,
                color = PaceDreamColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))

        // Filter tabs — matching iOS BookingTabPicker
        BookingTabPicker(
            selectedTab = selectedTab,
            countProvider = countProvider,
            onTabSelected = onTabSelected
        )
    }
}

// ============================================================================
// Tab Picker — matching iOS horizontal scroll capsule pills
// ============================================================================
@Composable
private fun BookingTabPicker(
    selectedTab: BookingTab,
    countProvider: (BookingTab) -> Int,
    onTabSelected: (BookingTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        BookingTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            val count = countProvider(tab)

            val bgColor by animateColorAsState(
                if (isSelected) PaceDreamColors.PrimaryLight else Color.Transparent,
                label = "tabBg"
            )
            val contentColor by animateColorAsState(
                if (isSelected) PaceDreamColors.Primary else PaceDreamColors.TextSecondary,
                label = "tabContent"
            )
            val borderColor = if (isSelected) {
                PaceDreamColors.Primary.copy(alpha = 0.3f)
            } else {
                PaceDreamColors.Gray300.copy(alpha = 0.5f)
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(PaceDreamRadius.Round))
                    .background(bgColor)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(PaceDreamRadius.Round)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tab icon
                val icon = when (tab) {
                    BookingTab.ALL -> PaceDreamIcons.ListIcon
                    BookingTab.UPCOMING -> PaceDreamIcons.CalendarToday
                    BookingTab.PAST -> PaceDreamIcons.CheckCircle
                    BookingTab.CANCELLED -> PaceDreamIcons.Cancel
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )

                Text(
                    text = tab.label,
                    style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor
                )

                // Count badge
                if (count > 0) {
                    Text(
                        text = "$count",
                        style = PaceDreamTypography.Caption2.copy(fontWeight = FontWeight.Bold),
                        color = contentColor,
                        modifier = Modifier
                            .background(
                                if (isSelected) PaceDreamColors.Primary.copy(alpha = 0.15f)
                                else PaceDreamColors.Gray300.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(PaceDreamRadius.Round)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// Unified Booking Card — matching iOS UnifiedBookingCard
// ============================================================================
@Composable
private fun UnifiedBookingCard(
    item: BookingListItem,
    statusConfig: BookingStatusConfig,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.5.dp,
                    color = PaceDreamColors.Gray200.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(PaceDreamRadius.LG)
                )
        ) {
            // ── Image + badges ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(topStart = PaceDreamRadius.LG, topEnd = PaceDreamRadius.LG))
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PaceDreamColors.Gray200.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Image,
                            contentDescription = null,
                            tint = PaceDreamColors.Gray400,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Top-left: Role badge
                RoleBadge(
                    role = item.role,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                )

                // Top-right: Status badge
                StatusBadge(
                    config = statusConfig,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
            }

            // ── Booking Details ──
            Column(
                modifier = Modifier.padding(PaceDreamSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title + location
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.title,
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.location != "—") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.LocationOn,
                                contentDescription = null,
                                tint = PaceDreamColors.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = item.location,
                                style = PaceDreamTypography.Subheadline,
                                color = PaceDreamColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Detail rows
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BookingDetailRow(
                        icon = PaceDreamIcons.CalendarToday,
                        label = "Dates",
                        value = item.dateRange
                    )

                    if (item.role == BookingRole.GUEST) {
                        BookingDetailRow(
                            icon = PaceDreamIcons.People,
                            label = "Guests",
                            value = item.guestsDisplay
                        )
                    } else if (item.guestName != null) {
                        BookingDetailRow(
                            icon = PaceDreamIcons.Person,
                            label = "Guest",
                            value = item.guestName
                        )
                    }

                    if (item.nightsCount > 0) {
                        BookingDetailRow(
                            icon = PaceDreamIcons.Hotel,
                            label = "Nights",
                            value = "${item.nightsCount} night${if (item.nightsCount == 1) "" else "s"}"
                        )
                    }
                }

                // Total + per-night
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Total",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                        Text(
                            text = item.amount?.let { BookingsViewModel.formatUsd(it) } ?: "—",
                            style = PaceDreamTypography.Title3,
                            color = PaceDreamColors.TextPrimary
                        )
                    }

                    if (item.perNightPrice != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Per night",
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary
                            )
                            Text(
                                text = BookingsViewModel.formatUsd(item.perNightPrice),
                                style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.Medium),
                                color = PaceDreamColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // ── Action Buttons ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = PaceDreamSpacing.MD,
                        end = PaceDreamSpacing.MD,
                        bottom = PaceDreamSpacing.MD
                    ),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2)
            ) {
                // View Details button — matching iOS green primary button
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Primary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "View Details",
                        style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================================================
// Role Badge — matching iOS RoleBadge
// ============================================================================
@Composable
private fun RoleBadge(role: BookingRole, modifier: Modifier = Modifier) {
    val textColor: Color
    val bgColor: Color
    val borderColor: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    when (role) {
        BookingRole.GUEST -> {
            textColor = Color(0xFF59339A)
            bgColor = PaceDreamColors.Purple.copy(alpha = 0.12f)
            borderColor = PaceDreamColors.Purple.copy(alpha = 0.3f)
            icon = PaceDreamIcons.Person
        }
        BookingRole.HOST -> {
            textColor = Color(0xFF1A6B8C)
            bgColor = PaceDreamColors.Teal.copy(alpha = 0.12f)
            borderColor = PaceDreamColors.Teal.copy(alpha = 0.3f)
            icon = PaceDreamIcons.Home
        }
    }

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(PaceDreamRadius.Round))
            .border(0.5.dp, borderColor, RoundedCornerShape(PaceDreamRadius.Round))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(10.dp)
        )
        Text(
            text = role.label,
            style = PaceDreamTypography.Caption2.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

// ============================================================================
// Status Badge — matching iOS status capsule
// ============================================================================
@Composable
private fun StatusBadge(config: BookingStatusConfig, modifier: Modifier = Modifier) {
    val (fgColor, bgColor, borderColor, icon) = statusBadgeColors(config.badgeColor)

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(PaceDreamRadius.Round))
            .border(0.5.dp, borderColor, RoundedCornerShape(PaceDreamRadius.Round))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fgColor,
            modifier = Modifier.size(10.dp)
        )
        Text(
            text = config.label,
            style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
            color = fgColor
        )
    }
}

private data class BadgeColors(
    val fg: Color,
    val bg: Color,
    val border: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun statusBadgeColors(badgeColor: String): BadgeColors {
    return when (badgeColor) {
        "yellow" -> BadgeColors(
            fg = Color(0xFF8C6600), // Keep darker for contrast
            bg = PaceDreamColors.Warning.copy(alpha = 0.15f),
            border = PaceDreamColors.Warning.copy(alpha = 0.4f),
            icon = PaceDreamIcons.AccessTime
        )
        "blue" -> BadgeColors(
            fg = Color(0xFF1F4DA6),
            bg = PaceDreamColors.Info.copy(alpha = 0.12f),
            border = PaceDreamColors.Info.copy(alpha = 0.3f),
            icon = PaceDreamIcons.CheckCircle
        )
        "green" -> BadgeColors(
            fg = Color(0xFF1A7326),
            bg = PaceDreamColors.Success.copy(alpha = 0.12f),
            border = PaceDreamColors.Success.copy(alpha = 0.3f),
            icon = PaceDreamIcons.Verified
        )
        "red" -> BadgeColors(
            fg = Color(0xFF991A1A),
            bg = PaceDreamColors.Error.copy(alpha = 0.12f),
            border = PaceDreamColors.Error.copy(alpha = 0.3f),
            icon = PaceDreamIcons.Cancel
        )
        else -> BadgeColors(
            fg = PaceDreamColors.TextSecondary,
            bg = PaceDreamColors.Gray100,
            border = PaceDreamColors.Gray200,
            icon = PaceDreamIcons.Info
        )
    }
}

// ============================================================================
// Detail Row — matching iOS row(icon, title, value)
// ============================================================================
@Composable
private fun BookingDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PaceDreamColors.Primary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = PaceDreamTypography.Caption,
            color = PaceDreamColors.TextSecondary
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Medium),
            color = PaceDreamColors.TextPrimary
        )
    }
}

// ============================================================================
// Skeleton — matching iOS GuestBookingCardSkeleton
// ============================================================================
@Composable
private fun BookingCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.5.dp,
                    color = PaceDreamColors.Gray200.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(PaceDreamRadius.LG)
                )
        ) {
            // Image skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = PaceDreamRadius.LG,
                            topEnd = PaceDreamRadius.LG
                        )
                    )
                    .background(PaceDreamColors.Gray200.copy(alpha = 0.4f))
            )

            Column(
                modifier = Modifier.padding(PaceDreamSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(18.dp)
                        .background(
                            PaceDreamColors.Gray200.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                )
                // Subtitle
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .background(
                            PaceDreamColors.Gray200.copy(alpha = 0.3f),
                            RoundedCornerShape(6.dp)
                        )
                )
                // Detail rows
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .background(
                                PaceDreamColors.Gray200.copy(alpha = 0.25f),
                                RoundedCornerShape(6.dp)
                            )
                    )
                }
                // Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(20.dp)
                            .background(
                                PaceDreamColors.Gray200.copy(alpha = 0.4f),
                                RoundedCornerShape(6.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(16.dp)
                            .background(
                                PaceDreamColors.Gray200.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                    )
                }
            }

            // Button skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaceDreamSpacing.MD)
                    .padding(bottom = PaceDreamSpacing.MD)
                    .height(44.dp)
                    .background(
                        PaceDreamColors.Gray200.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp)
                    )
            )
        }
    }
}
