package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.data.HostBookingDTO
import com.shourov.apps.pacedream.feature.host.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostBookingsScreen(
    onBookingClick: (String) -> Unit = {},
    viewModel: HostBookingsViewModel = hiltViewModel()
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
            contentPadding = PaddingValues(bottom = PaceDreamSpacing.XXL)
        ) {
            // Title
            item {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = PaceDreamSpacing.MD)
                        .padding(top = PaceDreamSpacing.MD, bottom = PaceDreamSpacing.SM)
                ) {
                    Text(
                        text = "Bookings",
                        style = PaceDreamTypography.Title1,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    Text(
                        text = "Manage your guest reservations",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }

            // Error banner — shared component
            uiState.error?.let { error ->
                item {
                    HostAlertBanner(
                        text = error,
                        color = PaceDreamColors.Error,
                        icon = PaceDreamIcons.Warning
                    )
                }
            }

            // Segmented filter tabs — shared chips
            item {
                val segments = listOf("Pending", "Confirmed", "Past", "Cancelled")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                    modifier = Modifier.padding(bottom = PaceDreamSpacing.MD)
                ) {
                    items(segments) { segment ->
                        HostFilterChip(
                            label = segment,
                            selected = uiState.selectedStatus == segment,
                            onClick = { viewModel.updateStatus(segment) }
                        )
                    }
                }
            }

            // Bookings list
            if (uiState.bookings.isEmpty() && !uiState.isLoading) {
                item {
                    HostEmptyState(
                        icon = PaceDreamIcons.CalendarToday,
                        title = "No ${uiState.selectedStatus.lowercase()} bookings",
                        subtitle = when (uiState.selectedStatus.lowercase()) {
                            "pending" -> "New booking requests from guests will appear here."
                            "confirmed" -> "Confirmed upcoming stays will appear here."
                            "past" -> "Completed bookings will appear here after checkout."
                            "cancelled" -> "Cancelled or declined bookings will show up here."
                            else -> "Bookings will appear here once guests make reservations."
                        }
                    )
                }
            } else {
                items(uiState.bookings) { booking ->
                    HostBookingCard(
                        booking = booking,
                        selectedSegment = uiState.selectedStatus,
                        onBookingClick = { onBookingClick(booking.id) },
                        onAcceptClick = { viewModel.acceptBooking(booking.id) },
                        onDeclineClick = { viewModel.declineBooking(booking.id) },
                        onCancelClick = { viewModel.cancelBooking(booking.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HostBookingCard(
    booking: HostBookingDTO,
    selectedSegment: String,
    onBookingClick: () -> Unit,
    onAcceptClick: () -> Unit,
    onDeclineClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS),
        onClick = onBookingClick,
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            // Guest info row
            Row(verticalAlignment = Alignment.CenterVertically) {
                HostInitialsAvatar(
                    name = booking.resolvedGuestName,
                    size = 44,
                    color = PaceDreamColors.HostAccent
                )

                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.resolvedListingTitle,
                        style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold),
                        color = PaceDreamColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = PaceDreamIcons.Person,
                            contentDescription = null,
                            tint = PaceDreamColors.TextTertiary,
                            modifier = Modifier.size(PaceDreamIconSize.XS)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        Text(
                            text = booking.resolvedGuestName,
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = PaceDreamIcons.CalendarToday,
                            contentDescription = null,
                            tint = PaceDreamColors.TextTertiary,
                            modifier = Modifier.size(PaceDreamIconSize.XS)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        Text(
                            text = "${booking.resolvedStart ?: ""} - ${booking.resolvedEnd ?: ""}",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format("%.0f", booking.resolvedTotal)}",
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.HostAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Action buttons for Pending segment
            if (selectedSegment == "Pending") {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    Button(
                        onClick = onAcceptClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
                        shape = RoundedCornerShape(PaceDreamRadius.SM),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = PaceDreamSpacing.SM)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(PaceDreamIconSize.XS)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        Text("Accept", color = Color.White, fontWeight = FontWeight.SemiBold, style = PaceDreamTypography.Caption)
                    }
                    OutlinedButton(
                        onClick = onDeclineClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PaceDreamColors.Error),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(PaceDreamColors.Error.copy(alpha = 0.5f))
                        ),
                        shape = RoundedCornerShape(PaceDreamRadius.SM),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = PaceDreamSpacing.SM)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Close,
                            contentDescription = null,
                            tint = PaceDreamColors.Error,
                            modifier = Modifier.size(PaceDreamIconSize.XS)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        Text("Decline", fontWeight = FontWeight.SemiBold, style = PaceDreamTypography.Caption)
                    }
                }
            }

            // Cancel button for Confirmed segment
            if (selectedSegment == "Confirmed") {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                OutlinedButton(
                    onClick = onCancelClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PaceDreamColors.Error),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(PaceDreamColors.Error.copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(PaceDreamRadius.SM),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = PaceDreamSpacing.SM)
                ) {
                    Text("Cancel Booking", fontWeight = FontWeight.SemiBold, style = PaceDreamTypography.Caption)
                }
            }
        }
    }
}
