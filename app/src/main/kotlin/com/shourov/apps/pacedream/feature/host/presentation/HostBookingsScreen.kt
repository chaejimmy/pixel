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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.data.HostBookingDTO

/**
 * Host Bookings Screen - iOS parity.
 *
 * Matches iOS HostBookingsView with segmented filter:
 * Pending | Confirmed | Past | Cancelled
 *
 * Features: pull-to-refresh, accept/decline/cancel actions,
 * guest initials, booking row cards, inline error banner.
 */
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
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // Title
            item {
                Text(
                    text = "Bookings",
                    style = PaceDreamTypography.Title1,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            // Error banner
            uiState.error?.let { error ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PaceDreamColors.Error.copy(alpha = 0.08f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Warning,
                            contentDescription = null,
                            tint = PaceDreamColors.Error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = error, color = PaceDreamColors.Error, fontSize = 13.sp)
                    }
                }
            }

            // Segmented filter tabs (iOS parity: Pending/Confirmed/Past/Cancelled)
            item {
                val segments = listOf("Pending", "Confirmed", "Past", "Cancelled")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(segments) { segment ->
                        FilterChip(
                            selected = uiState.selectedStatus == segment,
                            onClick = { viewModel.updateStatus(segment) },
                            label = {
                                Text(
                                    text = segment,
                                    fontWeight = if (uiState.selectedStatus == segment) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PaceDreamColors.Primary,
                                selectedLabelColor = Color.White,
                                containerColor = PaceDreamColors.Card,
                                labelColor = PaceDreamColors.TextPrimary
                            )
                        )
                    }
                }
            }

            // Bookings list
            if (uiState.bookings.isEmpty() && !uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.CalendarToday,
                            contentDescription = null,
                            tint = PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No ${uiState.selectedStatus.lowercase()} bookings",
                            style = PaceDreamTypography.Body,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
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
            .padding(horizontal = 16.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = onBookingClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Guest info row
            Row(verticalAlignment = Alignment.CenterVertically) {
                val guestName = booking.resolvedGuestName
                val initials = guestName.split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .take(2)
                    .joinToString("")
                    .ifEmpty { "G" }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PaceDreamColors.Primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.resolvedListingTitle,
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "${booking.resolvedStart ?: ""} - ${booking.resolvedEnd ?: ""}",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }

                Text(
                    text = "$${String.format("%.0f", booking.resolvedTotal)}",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Action buttons for Pending segment (iOS parity: Accept/Decline)
            if (selectedSegment == "Pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAcceptClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Success),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Accept", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onDeclineClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PaceDreamColors.Error),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Decline", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Cancel button for Confirmed segment
            if (selectedSegment == "Confirmed") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancelClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PaceDreamColors.Error),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("Cancel Booking", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
