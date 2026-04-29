/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shourov.apps.pacedream.feature.booking.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.components.PaceDreamHeroHeader
import com.pacedream.common.composables.components.PaceDreamPropertyImage
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    modifier: Modifier = Modifier,
    viewModel: BookingViewModel = hiltViewModel(),
    onExploreListings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadBookings()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
            .statusBarsPadding()
    ) {
        PaceDreamHeroHeader(
            title = "Bookings",
            subtitle = "Manage your reservations",
            onNotificationClick = { /* Handle notification */ }
        )

        // Booking count + Tab picker (like iOS)
        Column {
            if (uiState.allBookings.isNotEmpty()) {
                val count = uiState.allBookings.size
                Text(
                    text = "$count booking${if (count == 1) "" else "s"}",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }

            BookingTabPicker(
                selectedTab = uiState.selectedTab,
                countProvider = { uiState.count(it) },
                onTabSelected = viewModel::selectTab
            )

            HorizontalDivider(
                color = PaceDreamColors.Border,
                thickness = 0.5.dp
            )
        }

        // Content area
        when {
            uiState.error != null && uiState.allBookings.isEmpty() -> {
                BookingErrorState(
                    message = uiState.error ?: "Something went wrong",
                    onRetry = viewModel::loadBookings
                )
            }
            uiState.isLoading && uiState.allBookings.isEmpty() -> {
                BookingLoadingState()
            }
            uiState.filteredBookings.isEmpty() -> {
                BookingEmptyState(
                    tab = uiState.selectedTab,
                    onExploreListings = onExploreListings
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = viewModel::loadBookings,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Inline error banner if we have bookings but also an error
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = PaceDreamSpacing.LG,
                            end = PaceDreamSpacing.LG,
                            top = PaceDreamSpacing.SM,
                            bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
                    ) {
                        if (uiState.error != null) {
                            item {
                                InlineErrorBanner(
                                    text = uiState.error ?: "",
                                    onRetry = viewModel::loadBookings
                                )
                            }
                        }
                        items(uiState.filteredBookings, key = { it.id }) { booking ->
                            UnifiedBookingCard(
                                booking = booking,
                                statusConfig = viewModel.statusConfig(booking),
                                actionInFlight = uiState.actionInFlight,
                                onViewDetails = { viewModel.onBookingClick(booking.id) },
                                onCancel = { viewModel.cancelBooking(booking.id) },
                                onConfirm = { viewModel.confirmBooking(booking.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Tab Picker with Count Badges (matching iOS BookingTabPicker)

@Composable
private fun BookingTabPicker(
    selectedTab: BookingTab,
    countProvider: (BookingTab) -> Int,
    onTabSelected: (BookingTab) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = PaceDreamSpacing.LG),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BookingTab.entries.forEach { tab ->
            val count = countProvider(tab)
            val isSelected = selectedTab == tab
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) PaceDreamColors.Primary.copy(alpha = 0.1f) else Color.Transparent,
                label = "tabBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.TextSecondary,
                label = "tabText"
            )
            val borderColor = if (isSelected) PaceDreamColors.Primary.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.2f)

            Surface(
                onClick = { onTabSelected(tab) },
                shape = CircleShape,
                color = bgColor,
                modifier = Modifier.border(1.dp, borderColor, CircleShape)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (tab) {
                            BookingTab.ALL -> PaceDreamIcons.ListIcon
                            BookingTab.UPCOMING -> PaceDreamIcons.Schedule
                            BookingTab.PAST -> PaceDreamIcons.CheckCircle
                            BookingTab.CANCELLED -> PaceDreamIcons.Cancel
                        },
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = tab.label,
                        style = PaceDreamTypography.Caption,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    if (count > 0) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) PaceDreamColors.Primary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$count",
                                style = PaceDreamTypography.Caption2,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Unified Booking Card (matching iOS UnifiedBookingCard with image header + status badge)

@Composable
private fun UnifiedBookingCard(
    booking: BookingModel,
    statusConfig: BookingStatusConfig,
    actionInFlight: Boolean = false,
    onViewDetails: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column {
            // Property Image with Status Badge overlay (like iOS)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                PaceDreamPropertyImage(
                    imageUrl = booking.propertyImage,
                    contentDescription = "Property: ${booking.propertyName}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = PaceDreamRadius.LG, topEnd = PaceDreamRadius.LG))
                )

                // Status badge (top-right, like iOS)
                StatusBadge(
                    config = statusConfig,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
            }

            // Booking Details
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title
                Text(
                    text = booking.propertyName.ifEmpty { "Booking" },
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Host name
                if (booking.hostName.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Person,
                            contentDescription = null,
                            tint = PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = booking.hostName,
                            style = PaceDreamTypography.Body,
                            color = PaceDreamColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Detail rows (like iOS)
                DetailRow(
                    icon = PaceDreamIcons.CalendarToday,
                    title = "Dates",
                    value = "${formatDate(booking.startDate)} - ${formatDate(booking.endDate)}"
                )

                if (booking.guestCount > 0) {
                    DetailRow(
                        icon = PaceDreamIcons.Group,
                        title = "Guests",
                        value = "${booking.guestCount} guest${if (booking.guestCount == 1) "" else "s"}"
                    )
                }

                // Price section
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
                            text = "${booking.currency} ${String.format("%.2f", booking.totalPrice)}",
                            style = PaceDreamTypography.Title3,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Action Buttons (like iOS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isCancelable = booking.status == BookingStatus.PENDING ||
                    booking.status == BookingStatus.CONFIRMED

                if (isCancelable) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !actionInFlight,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PaceDreamColors.Error
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, PaceDreamColors.Error.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                if (booking.status == BookingStatus.PENDING) {
                    Button(
                        onClick = onConfirm,
                        enabled = !actionInFlight,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PaceDreamColors.Primary
                        )
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Confirm",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Primary
                    )
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View Details",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// MARK: - Status Badge (top-right overlay on card image, like iOS)

@Composable
private fun StatusBadge(
    config: BookingStatusConfig,
    modifier: Modifier = Modifier
) {
    val (bgColor, fgColor, borderColor) = when (config.badgeColor) {
        "yellow" -> Triple(
            Color(0xFFFFF3CD),
            Color(0xFF8C6A00),
            Color(0x66FFCC00)
        )
        "blue" -> Triple(
            Color(0xFFD6E4FF),
            Color(0xFF1F4DA6),
            Color(0x4D3366FF)
        )
        "green" -> Triple(
            Color(0xFFD4EDDA),
            Color(0xFF1A7326),
            Color(0x4D28A745)
        )
        "red" -> Triple(
            Color(0xFFFDD9D7),
            Color(0xFF991A1A),
            Color(0x4DFF3333)
        )
        else -> Triple(
            Color(0xFFE8E8E8),
            Color(0xFF595959),
            Color(0x4D808080)
        )
    }

    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = modifier.border(0.5.dp, borderColor, CircleShape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (config.badgeColor) {
                    "yellow" -> PaceDreamIcons.Schedule
                    "blue" -> PaceDreamIcons.CheckCircle
                    "green" -> PaceDreamIcons.Verified
                    "red" -> PaceDreamIcons.Cancel
                    else -> PaceDreamIcons.Info
                },
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = config.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = fgColor
            )
        }
    }
}

// MARK: - Detail Row (like iOS row(icon:title:value:))

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PaceDreamColors.Primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = PaceDreamTypography.Caption,
            color = PaceDreamColors.TextSecondary
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = PaceDreamTypography.Caption,
            fontWeight = FontWeight.Medium,
            color = PaceDreamColors.TextPrimary
        )
    }
}

// MARK: - Inline Error Banner (like iOS GuestInlineErrorBanner)

@Composable
private fun InlineErrorBanner(
    text: String,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        color = Color(0x1FFF9800)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextPrimary,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(
                    text = "Retry",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// MARK: - Loading State (skeleton cards like iOS)

@Composable
private fun BookingLoadingState() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamSpacing.LG),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(4) {
            BookingCardSkeleton()
        }
    }
}

@Composable
private fun BookingCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(Color.Gray.copy(alpha = 0.15f))
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(18.dp)
                        .background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(PaceDreamRadius.SM))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .background(Color.Gray.copy(alpha = 0.12f), RoundedCornerShape(PaceDreamRadius.SM))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                        .background(Color.Gray.copy(alpha = 0.10f), RoundedCornerShape(PaceDreamRadius.SM))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(20.dp)
                            .background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(PaceDreamRadius.SM))
                    )
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(16.dp)
                            .background(Color.Gray.copy(alpha = 0.12f), RoundedCornerShape(PaceDreamRadius.SM))
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .height(44.dp)
                    .background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(PaceDreamRadius.MD))
            )
        }
    }
}

// MARK: - Per-Tab Empty States (matching iOS GuestBookingsEmptyState)

@Composable
private fun BookingEmptyState(
    tab: BookingTab,
    onExploreListings: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when (tab) {
                    BookingTab.ALL -> PaceDreamIcons.CalendarToday
                    BookingTab.UPCOMING -> PaceDreamIcons.Schedule
                    BookingTab.PAST -> PaceDreamIcons.CheckCircle
                    BookingTab.CANCELLED -> PaceDreamIcons.Cancel
                },
                contentDescription = null,
                tint = PaceDreamColors.Primary.copy(alpha = 0.6f),
                modifier = Modifier.size(52.dp)
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            Text(
                text = when (tab) {
                    BookingTab.ALL -> "No bookings yet"
                    BookingTab.UPCOMING -> "No upcoming bookings"
                    BookingTab.PAST -> "No past bookings"
                    BookingTab.CANCELLED -> "No cancelled bookings"
                },
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = when (tab) {
                    BookingTab.ALL -> "Find a space you love and book your first stay \u2014 it'll show up right here."
                    BookingTab.UPCOMING -> "Your confirmed and pending bookings will show up here."
                    BookingTab.PAST -> "Completed stays will appear here after checkout."
                    BookingTab.CANCELLED -> "Cancelled or refunded bookings will show up here."
                },
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            // CTA \u2014 show on the ALL tab where the empty state is the user's
            // first impression. Other tabs auto-fill once a booking exists.
            if (tab == BookingTab.ALL) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                Button(
                    onClick = onExploreListings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Primary
                    ),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    contentPadding = PaddingValues(
                        horizontal = PaceDreamSpacing.LG,
                        vertical = PaceDreamSpacing.SM2
                    )
                ) {
                    Text(
                        text = "Find a stay",
                        style = PaceDreamTypography.Button,
                        color = PaceDreamColors.OnPrimary
                    )
                }
            }
        }
    }
}

// MARK: - Full Error State (like iOS GuestBookingsErrorState)

@Composable
private fun BookingErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = PaceDreamIcons.WifiOutlined,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Couldn't load bookings",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = message,
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaceDreamColors.Primary
                )
            ) {
                Text("Retry")
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}
