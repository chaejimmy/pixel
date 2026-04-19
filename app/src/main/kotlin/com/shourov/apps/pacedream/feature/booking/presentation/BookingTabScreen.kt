package com.shourov.apps.pacedream.feature.booking.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
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
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.booking.model.BookingItem
import com.shourov.apps.pacedream.feature.booking.model.BookingRole
import com.shourov.apps.pacedream.feature.booking.model.BookingStatusConfig
import com.shourov.apps.pacedream.feature.booking.model.BookingStatusFilter
import com.shourov.apps.pacedream.feature.booking.model.BookingTabEvent
import com.shourov.apps.pacedream.feature.booking.model.BookingTabUiState

/**
 * Bookings tab screen — iOS parity.
 *
 * Shows all bookings (both guest and host) in a unified list with
 * All / Upcoming / Past / Cancelled tab picker, matching iOS BookingView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingTabScreen(
    onBookingClick: (String) -> Unit = {},
    onNewBookingClick: () -> Unit = {},
    onShowAuthSheet: () -> Unit = {}
) {
    val viewModel: BookingTabViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Listen for navigation events
    LaunchedEffect(Unit) {
        viewModel.navigation.collect { nav ->
            when (nav) {
                is BookingTabNavigation.ToBookingDetail -> onBookingClick(nav.bookingId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
            .statusBarsPadding()
    ) {
        // Header
        PaceDreamHeroHeader(
            title = "Bookings",
            subtitle = "Manage your reservations"
        )

        // Booking count + Tab picker (always visible, matching iOS)
        val successState = uiState as? BookingTabUiState.Success
        Column(
            modifier = Modifier.padding(
                start = PaceDreamSpacing.MD,
                end = PaceDreamSpacing.MD,
                top = PaceDreamSpacing.SM,
                bottom = PaceDreamSpacing.SM
            )
        ) {
            if (successState != null && successState.bookings.isNotEmpty()) {
                val count = successState.bookings.size
                Text(
                    text = "$count booking${if (count == 1) "" else "s"}",
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }

            BookingTabPicker(
                selectedTab = successState?.statusFilter ?: BookingStatusFilter.ALL,
                countProvider = { successState?.count(it) ?: 0 },
                onTabSelected = { viewModel.onEvent(BookingTabEvent.StatusFilterChanged(it)) }
            )
        }

        HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)

        // Content area
        when (val state = uiState) {
            is BookingTabUiState.Loading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = PaceDreamSpacing.MD,
                        vertical = PaceDreamSpacing.SM
                    ),
                    verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
                ) {
                    items(4) { BookingCardSkeleton() }
                }
            }

            is BookingTabUiState.RequiresAuth -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = PaceDreamSpacing.LG),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))
                    Icon(
                        imageVector = PaceDreamIcons.Lock,
                        contentDescription = null,
                        tint = PaceDreamColors.TextTertiary,
                        modifier = Modifier.size(PaceDreamIconSize.XXL)
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                    Text(
                        text = "Sign in to view bookings",
                        style = PaceDreamTypography.Title3,
                        color = PaceDreamColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    Text(
                        text = "Your bookings will appear here once you sign in",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                    Button(
                        onClick = onShowAuthSheet,
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        modifier = Modifier.height(PaceDreamButtonHeight.LG),
                        contentPadding = PaddingValues(
                            horizontal = PaceDreamSpacing.XL,
                            vertical = PaceDreamSpacing.SM2
                        )
                    ) {
                        Text("Sign In", style = PaceDreamTypography.Button, maxLines = 1)
                    }
                }
            }

            is BookingTabUiState.Error -> {
                BookingsErrorState(
                    message = state.message,
                    onRetry = { viewModel.onEvent(BookingTabEvent.Refresh) }
                )
            }

            is BookingTabUiState.Empty -> {
                BookingsEmptyState(
                    tab = BookingStatusFilter.ALL,
                    onExplore = onNewBookingClick
                )
            }

            is BookingTabUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.onEvent(BookingTabEvent.Refresh) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.filteredBookings.isEmpty()) {
                        BookingsEmptyState(
                            tab = state.statusFilter,
                            onExplore = if (state.statusFilter == BookingStatusFilter.ALL ||
                                state.statusFilter == BookingStatusFilter.UPCOMING
                            ) onNewBookingClick else null
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = PaceDreamSpacing.MD,
                                vertical = PaceDreamSpacing.SM
                            ),
                            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
                        ) {
                            items(state.filteredBookings, key = { it.id }) { booking ->
                                UnifiedBookingCard(
                                    item = booking,
                                    statusConfig = viewModel.statusConfig(booking),
                                    onViewDetails = {
                                        viewModel.onEvent(BookingTabEvent.BookingClicked(booking.id))
                                    }
                                )
                            }

                            // Bottom padding for nav bar
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Tab Picker — matching iOS BookingTabPicker (horizontal scroll capsule pills)
// ============================================================================

@Composable
private fun BookingTabPicker(
    selectedTab: BookingStatusFilter,
    countProvider: (BookingStatusFilter) -> Int,
    onTabSelected: (BookingStatusFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BookingStatusFilter.entries.forEach { tab ->
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
                    .border(1.dp, borderColor, RoundedCornerShape(PaceDreamRadius.Round))
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val icon = when (tab) {
                    BookingStatusFilter.ALL -> PaceDreamIcons.ListIcon
                    BookingStatusFilter.UPCOMING -> PaceDreamIcons.CalendarToday
                    BookingStatusFilter.PAST -> PaceDreamIcons.CheckCircle
                    BookingStatusFilter.CANCELLED -> PaceDreamIcons.Cancel
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = tab.displayName,
                    style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor
                )
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
    item: BookingItem,
    statusConfig: BookingStatusConfig,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
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
            // Image + badges
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
            ) {
                if (!item.propertyImage.isNullOrBlank()) {
                    AsyncImage(
                        model = item.propertyImage,
                        contentDescription = item.propertyName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
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

                // Top-left: Role badge (Guest / Host)
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

            // Booking details
            Column(
                modifier = Modifier.padding(PaceDreamSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title + location
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.propertyName.ifBlank { "Booking" },
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!item.location.isNullOrBlank() && item.location != "\u2014") {
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

                // Detail rows (matching iOS)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BookingDetailRow(
                        icon = PaceDreamIcons.CalendarToday,
                        label = "Dates",
                        value = item.formattedDateRange
                    )

                    if (item.role == BookingRole.RENTER) {
                        BookingDetailRow(
                            icon = PaceDreamIcons.People,
                            label = "Guests",
                            value = item.guestLabel
                        )
                    } else if (item.guestName.isNotBlank()) {
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
                            text = item.formattedPrice,
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
                                text = "$${String.format("%.0f", item.perNightPrice)}",
                                style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.Medium),
                                color = PaceDreamColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // View Details button (matching iOS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = PaceDreamSpacing.MD,
                        end = PaceDreamSpacing.MD,
                        bottom = PaceDreamSpacing.MD
                    )
            ) {
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
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
// Role Badge — matching iOS RoleBadge (Guest / Host)
// ============================================================================

@Composable
private fun RoleBadge(role: BookingRole, modifier: Modifier = Modifier) {
    val textColor: Color
    val bgColor: Color
    val borderColor: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector

    when (role) {
        BookingRole.RENTER -> {
            // Darker Purple foreground for text contrast on the tinted chip background.
            textColor = Color(0xFF59339A)
            bgColor = PaceDreamColors.Purple.copy(alpha = 0.12f)
            borderColor = PaceDreamColors.Purple.copy(alpha = 0.3f)
            icon = PaceDreamIcons.Person
        }
        BookingRole.HOST -> {
            // Darker Teal foreground for text contrast on the tinted chip background.
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
            text = role.displayName,
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
    val fgColor: Color
    val bgColor: Color
    val borderColor: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector

    // Tinted status capsules: background/border tint comes from the iOS system
    // color token; foreground keeps a darker variant so text stays readable on
    // the semi-transparent fill (AA contrast on the light theme).
    when (config.badgeColor) {
        "yellow" -> {
            fgColor = Color(0xFF8C6600)
            bgColor = PaceDreamColors.Yellow.copy(alpha = 0.15f)
            borderColor = PaceDreamColors.Yellow.copy(alpha = 0.4f)
            icon = PaceDreamIcons.AccessTime
        }
        "blue" -> {
            fgColor = Color(0xFF1F4DA6)
            bgColor = PaceDreamColors.Blue.copy(alpha = 0.12f)
            borderColor = PaceDreamColors.Blue.copy(alpha = 0.3f)
            icon = PaceDreamIcons.CheckCircle
        }
        "green" -> {
            fgColor = Color(0xFF1A7326)
            bgColor = PaceDreamColors.Green.copy(alpha = 0.12f)
            borderColor = PaceDreamColors.Green.copy(alpha = 0.3f)
            icon = PaceDreamIcons.Verified
        }
        "red" -> {
            fgColor = Color(0xFF991A1A)
            bgColor = PaceDreamColors.Red.copy(alpha = 0.12f)
            borderColor = PaceDreamColors.Red.copy(alpha = 0.3f)
            icon = PaceDreamIcons.Cancel
        }
        else -> {
            fgColor = Color(0xFF595959)
            bgColor = PaceDreamColors.Gray500.copy(alpha = 0.12f)
            borderColor = PaceDreamColors.Gray500.copy(alpha = 0.3f)
            icon = PaceDreamIcons.Info
        }
    }

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
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    0.5.dp,
                    PaceDreamColors.Gray200.copy(alpha = 0.3f),
                    RoundedCornerShape(PaceDreamRadius.LG)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(topStart = PaceDreamRadius.LG, topEnd = PaceDreamRadius.LG))
                    .background(PaceDreamColors.Gray200.copy(alpha = 0.4f))
            )
            Column(
                modifier = Modifier.padding(PaceDreamSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(18.dp)
                        .background(PaceDreamColors.Gray200.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .background(PaceDreamColors.Gray200.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                )
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .background(PaceDreamColors.Gray200.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(20.dp)
                            .background(PaceDreamColors.Gray200.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(16.dp)
                            .background(PaceDreamColors.Gray200.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaceDreamSpacing.MD)
                    .padding(bottom = PaceDreamSpacing.MD)
                    .height(44.dp)
                    .background(PaceDreamColors.Gray200.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            )
        }
    }
}

// ============================================================================
// Empty States — per-tab messaging matching iOS GuestBookingsEmptyState
// ============================================================================

@Composable
private fun BookingsEmptyState(
    tab: BookingStatusFilter,
    onExplore: (() -> Unit)? = null
) {
    val icon = when (tab) {
        BookingStatusFilter.ALL -> PaceDreamIcons.CalendarToday
        BookingStatusFilter.UPCOMING -> PaceDreamIcons.Schedule
        BookingStatusFilter.PAST -> PaceDreamIcons.CheckCircle
        BookingStatusFilter.CANCELLED -> PaceDreamIcons.Cancel
    }
    val title = when (tab) {
        BookingStatusFilter.ALL -> "No bookings yet"
        BookingStatusFilter.UPCOMING -> "No upcoming bookings"
        BookingStatusFilter.PAST -> "No past bookings"
        BookingStatusFilter.CANCELLED -> "No cancelled bookings"
    }
    val subtitle = when (tab) {
        BookingStatusFilter.ALL -> "Find a space you love and book your first stay \u2014 it\u2019ll show up right here."
        BookingStatusFilter.UPCOMING -> "Your confirmed and pending bookings will show up here."
        BookingStatusFilter.PAST -> "Completed stays will appear here after checkout."
        BookingStatusFilter.CANCELLED -> "Cancelled or refunded bookings will show up here."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PaceDreamSpacing.LG),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PaceDreamColors.Primary.copy(alpha = 0.6f),
            modifier = Modifier.size(52.dp)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        Text(
            text = title,
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Text(
            text = subtitle,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        if (onExplore != null && (tab == BookingStatusFilter.ALL || tab == BookingStatusFilter.UPCOMING)) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            Button(
                onClick = onExplore,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp),
                contentPadding = PaddingValues(horizontal = 32.dp)
            ) {
                Text(
                    "Explore spaces",
                    style = PaceDreamTypography.Button,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

// ============================================================================
// Error State — matching iOS GuestBookingsErrorState
// ============================================================================

@Composable
private fun BookingsErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PaceDreamSpacing.LG),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = PaceDreamIcons.ErrorOutline,
            contentDescription = null,
            tint = PaceDreamColors.TextSecondary,
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        Text(
            text = "Couldn\u2019t load bookings",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Text(
            text = message,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            modifier = Modifier.height(PaceDreamButtonHeight.MD)
        ) {
            Text("Retry", style = PaceDreamTypography.Button, color = Color.White)
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
