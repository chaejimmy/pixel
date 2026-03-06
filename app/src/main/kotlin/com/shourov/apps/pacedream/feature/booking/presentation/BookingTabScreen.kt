package com.shourov.apps.pacedream.feature.booking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.booking.model.BookingItem
import com.shourov.apps.pacedream.feature.booking.model.BookingRole
import com.shourov.apps.pacedream.feature.booking.model.BookingStatusFilter
import com.shourov.apps.pacedream.feature.booking.model.BookingTabEvent
import com.shourov.apps.pacedream.feature.booking.model.BookingTabUiState
import com.shourov.apps.pacedream.model.BookingStatus

/**
 * Bookings tab screen with Trips / Hosting role tabs.
 *
 * Mirrors the web platform which uses:
 *   GET /account/bookings?role=renter   (Trips)
 *   GET /account/bookings?role=host     (Hosting)
 *
 * The authenticated user and the selected role (user/host) are sent with
 * every fetch so the backend returns the correct scoped data.
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

    // Role tabs: Trips (renter) and Hosting (host) — matching the web
    val roleTabs = BookingRole.entries
    var selectedRoleIndex by remember { mutableIntStateOf(0) }

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
    ) {
        // Header
        PaceDreamHeroHeader(
            title = "My Bookings",
            subtitle = "Manage your reservations",
            modifier = Modifier.padding(PaceDreamSpacing.LG)
        )

        // Role Tab Row: Trips | Hosting (matches web's Trips / Hosting tabs)
        TabRow(
            selectedTabIndex = selectedRoleIndex,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG),
            containerColor = PaceDreamColors.Card,
            contentColor = PaceDreamColors.Primary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedRoleIndex]),
                    color = PaceDreamColors.Primary,
                    height = 3.dp
                )
            }
        ) {
            roleTabs.forEachIndexed { index, role ->
                Tab(
                    selected = selectedRoleIndex == index,
                    onClick = {
                        selectedRoleIndex = index
                        viewModel.onEvent(BookingTabEvent.RoleChanged(role))
                    },
                    text = {
                        Text(
                            text = role.displayName,
                            style = PaceDreamTypography.Callout.copy(
                                fontWeight = if (selectedRoleIndex == index)
                                    FontWeight.SemiBold
                                else
                                    FontWeight.Normal
                            ),
                            color = if (selectedRoleIndex == index)
                                PaceDreamColors.Primary
                            else
                                PaceDreamColors.TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        // Content based on UI state
        when (val state = uiState) {
            is BookingTabUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PaceDreamColors.Primary)
                }
            }

            is BookingTabUiState.RequiresAuth -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PaceDreamSpacing.XL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = PaceDreamIcons.Lock,
                            contentDescription = null,
                            tint = PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        Text(
                            text = "Sign in to view bookings",
                            style = PaceDreamTypography.Title3,
                            color = PaceDreamColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        Text(
                            text = "Your trips and hosting bookings will appear here",
                            style = PaceDreamTypography.Body,
                            color = PaceDreamColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                        Button(
                            onClick = onShowAuthSheet,
                            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("Sign in / Create account", style = PaceDreamTypography.Headline)
                        }
                    }
                }
            }

            is BookingTabUiState.Error -> {
                PaceDreamEmptyState(
                    icon = PaceDreamIcons.Error,
                    title = "Something went wrong",
                    description = state.message,
                    actionText = "Try Again",
                    onActionClick = { viewModel.onEvent(BookingTabEvent.Refresh) }
                )
            }

            is BookingTabUiState.Empty -> {
                val currentRole = roleTabs[selectedRoleIndex]
                PaceDreamEmptyState(
                    icon = PaceDreamIcons.CalendarToday,
                    title = if (currentRole == BookingRole.RENTER)
                        "No trips yet"
                    else
                        "No hosting bookings yet",
                    description = if (currentRole == BookingRole.RENTER)
                        "Start exploring and book your next stay"
                    else
                        "Bookings from your guests will appear here",
                    actionText = if (currentRole == BookingRole.RENTER) "Explore Properties" else null,
                    onActionClick = if (currentRole == BookingRole.RENTER) onNewBookingClick else null
                )
            }

            is BookingTabUiState.Success -> {
                // iOS-parity: Status filter chips (All / Upcoming / Past / Cancelled)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaceDreamSpacing.LG),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    BookingStatusFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.statusFilter == filter,
                            onClick = { viewModel.onEvent(BookingTabEvent.StatusFilterChanged(filter)) },
                            label = {
                                Text(
                                    text = filter.displayName,
                                    style = PaceDreamTypography.Caption,
                                    fontWeight = if (state.statusFilter == filter) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PaceDreamColors.Primary,
                                selectedLabelColor = PaceDreamColors.OnPrimary,
                                containerColor = PaceDreamColors.Card,
                                labelColor = PaceDreamColors.TextSecondary
                            ),
                            shape = RoundedCornerShape(PaceDreamRadius.Round)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.onEvent(BookingTabEvent.Refresh) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.filteredBookings.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No ${state.statusFilter.displayName.lowercase()} bookings",
                                style = PaceDreamTypography.Body,
                                color = PaceDreamColors.TextSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(PaceDreamSpacing.LG),
                            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
                        ) {
                            items(state.filteredBookings, key = { it.id }) { booking ->
                                BookingItemCard(
                                    booking = booking,
                                    role = state.role,
                                    onClick = { viewModel.onEvent(BookingTabEvent.BookingClicked(booking.id)) }
                                )
                            }

                            // Load more trigger
                            if (state.hasMore && state.statusFilter == BookingStatusFilter.ALL) {
                                item {
                                    LaunchedEffect(Unit) {
                                        viewModel.onEvent(BookingTabEvent.LoadMore)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(PaceDreamSpacing.MD),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = PaceDreamColors.Primary,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingItemCard(
    booking: BookingItem,
    role: BookingRole,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.LG)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.propertyName.ifBlank { "Booking #${booking.id.takeLast(6)}" },
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))

                PaceDreamStatusChip(
                    status = booking.status.name,
                    isActive = booking.status == BookingStatus.CONFIRMED
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            // Property info row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.MD))
                        .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Home,
                        contentDescription = "Property",
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))

                Column {
                    // Show host or guest name depending on role
                    val personLabel = if (role == BookingRole.RENTER) {
                        if (booking.hostName.isNotBlank()) "Host: ${booking.hostName}" else null
                    } else {
                        if (booking.guestName.isNotBlank()) "Guest: ${booking.guestName}" else null
                    }

                    personLabel?.let {
                        Text(
                            text = it,
                            style = PaceDreamTypography.Body,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    booking.location?.let { loc ->
                        Text(
                            text = loc,
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Booking details: dates and price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Check-in",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        text = booking.formattedStartDate.ifBlank { "-" },
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text(
                        text = "Check-out",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        text = booking.formattedEndDate.ifBlank { "-" },
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text(
                        text = "Total",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        text = booking.formattedPrice,
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Guest count
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Person,
                    contentDescription = "Guests",
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                Text(
                    text = booking.guestLabel,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
        }
    }
}
