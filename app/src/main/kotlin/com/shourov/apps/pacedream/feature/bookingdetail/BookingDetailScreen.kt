package com.shourov.apps.pacedream.feature.bookingdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamElevation
import com.pacedream.common.composables.theme.PaceDreamIconSize
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Rich booking detail screen matching iOS BookingDetailView.swift.
 *
 * Features:
 * - Status badge with color coding (iOS BookingStatusHelper parity)
 * - Property name and booking ID
 * - Date/time section with formatted dates
 * - Price breakdown card
 * - Cancel button for eligible bookings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    bookingId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Booking Details", style = PaceDreamTypography.Headline) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        when {
            uiState.isLoading -> {
                BookingDetailSkeleton(modifier = Modifier.padding(padding))
            }
            uiState.error != null -> {
                BookingDetailError(
                    message = uiState.error ?: "Failed",
                    onRetry = { viewModel.load() },
                    onBack = onBack,
                    modifier = Modifier.padding(padding)
                )
            }
            uiState.booking != null -> {
                val booking = uiState.booking!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(PaceDreamSpacing.LG),
                    verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
                ) {
                    // Status Badge (iOS BookingStatusHelper parity)
                    val statusLabel = resolveStatusLabel(booking.status, booking.endDate)
                    val statusColor = statusColor(statusLabel)
                    val statusIcon = statusIcon(statusLabel)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PaceDreamSpacing.LG),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(PaceDreamIconSize.MD)
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                            Text(
                                text = statusLabel,
                                style = PaceDreamTypography.Headline,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Property Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.MD)
                    ) {
                        Column(modifier = Modifier.padding(PaceDreamSpacing.LG)) {
                            Text(
                                text = booking.propertyName.ifBlank { "Booking" },
                                style = PaceDreamTypography.Title2,
                                fontWeight = FontWeight.Bold,
                                color = PaceDreamColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                            Text(
                                text = "Booking ID: ${booking.id.takeLast(8)}",
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary
                            )

                            if (booking.hostName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = PaceDreamIcons.Person,
                                        contentDescription = null,
                                        tint = PaceDreamColors.TextSecondary,
                                        modifier = Modifier.size(PaceDreamIconSize.SM)
                                    )
                                    Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                                    Text(
                                        text = "Host: ${booking.hostName}",
                                        style = PaceDreamTypography.Body,
                                        color = PaceDreamColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Dates Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.MD)
                    ) {
                        Column(modifier = Modifier.padding(PaceDreamSpacing.LG)) {
                            Text(
                                "Dates",
                                style = PaceDreamTypography.Headline,
                                fontWeight = FontWeight.SemiBold,
                                color = PaceDreamColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Check-in",
                                        style = PaceDreamTypography.Caption,
                                        color = PaceDreamColors.TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                                    Text(
                                        formatFullDate(booking.startDate),
                                        style = PaceDreamTypography.Callout,
                                        color = PaceDreamColors.TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        formatTime(booking.startDate),
                                        style = PaceDreamTypography.Caption,
                                        color = PaceDreamColors.TextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = PaceDreamIcons.ArrowForward,
                                    contentDescription = null,
                                    tint = PaceDreamColors.TextSecondary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.CenterVertically)
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Check-out",
                                        style = PaceDreamTypography.Caption,
                                        color = PaceDreamColors.TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                                    Text(
                                        formatFullDate(booking.endDate),
                                        style = PaceDreamTypography.Callout,
                                        color = PaceDreamColors.TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        formatTime(booking.endDate),
                                        style = PaceDreamTypography.Caption,
                                        color = PaceDreamColors.TextSecondary
                                    )
                                }
                            }

                            if (booking.guestCount > 0) {
                                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                                HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = PaceDreamIcons.Person,
                                        contentDescription = null,
                                        tint = PaceDreamColors.TextSecondary,
                                        modifier = Modifier.size(PaceDreamIconSize.SM)
                                    )
                                    Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                                    Text(
                                        "${booking.guestCount} guest${if (booking.guestCount != 1) "s" else ""}",
                                        style = PaceDreamTypography.Body,
                                        color = PaceDreamColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // iOS PR #202 parity: Verification PIN Card
                    val verificationPin = booking.verificationPin
                    val pinStatus = booking.pinStatus
                    if (!verificationPin.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(PaceDreamRadius.LG),
                            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                            elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.MD)
                        ) {
                            Column(
                                modifier = Modifier.padding(PaceDreamSpacing.LG),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Verification PIN",
                                    style = PaceDreamTypography.Headline,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PaceDreamColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                                Text(
                                    text = verificationPin,
                                    style = PaceDreamTypography.Title2.copy(
                                        letterSpacing = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = PaceDreamColors.Primary
                                )
                                if (!pinStatus.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                                    Text(
                                        text = pinStatus.replaceFirstChar { it.uppercase() },
                                        style = PaceDreamTypography.Caption,
                                        color = PaceDreamColors.TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                                Text(
                                    text = "Share this PIN with your host/guest at check-in",
                                    style = PaceDreamTypography.Caption,
                                    color = PaceDreamColors.TextTertiary
                                )
                            }
                        }
                    }

                    // Pricing Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.MD)
                    ) {
                        Column(modifier = Modifier.padding(PaceDreamSpacing.LG)) {
                            Text(
                                "Price Details",
                                style = PaceDreamTypography.Headline,
                                fontWeight = FontWeight.SemiBold,
                                color = PaceDreamColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                            HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Total",
                                    style = PaceDreamTypography.Title3,
                                    fontWeight = FontWeight.Bold,
                                    color = PaceDreamColors.TextPrimary
                                )
                                Text(
                                    "${booking.currency} ${String.format("%.2f", booking.totalPrice)}",
                                    style = PaceDreamTypography.Title3,
                                    fontWeight = FontWeight.Bold,
                                    color = PaceDreamColors.Primary
                                )
                            }
                        }
                    }

                    // Action Buttons
                    if (booking.status == BookingStatus.PENDING || booking.status == BookingStatus.CONFIRMED) {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                        if (booking.status == BookingStatus.PENDING) {
                            Button(
                                onClick = { viewModel.cancelBooking() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PaceDreamColors.Error.copy(alpha = 0.1f),
                                    contentColor = PaceDreamColors.Error
                                ),
                                shape = RoundedCornerShape(PaceDreamRadius.MD)
                            ) {
                                Text("Cancel Booking", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.cancelBooking() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(PaceDreamRadius.MD)
                            ) {
                                Text(
                                    "Cancel Booking",
                                    color = PaceDreamColors.Error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
                }
            }
            else -> {
                BookingDetailError(
                    message = "Booking not found",
                    onRetry = { viewModel.load() },
                    onBack = onBack,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

// ── BookingStatusHelper (iOS parity) ─────────────────────────────

private fun resolveStatusLabel(status: BookingStatus, endDate: String): String {
    val normalized = status.name.lowercase()
    return when {
        normalized == "cancelled" || normalized == "rejected" -> "Cancelled"
        normalized == "completed" -> "Completed"
        normalized == "pending" -> "Pending"
        normalized == "confirmed" && isEndDatePast(endDate) -> "Completed"
        normalized == "confirmed" -> "Upcoming"
        else -> status.name.lowercase().replaceFirstChar { it.uppercase() }
    }
}

private fun statusColor(label: String): Color {
    return when (label.lowercase()) {
        "upcoming", "confirmed" -> PaceDreamColors.Success
        "completed" -> PaceDreamColors.Info
        "pending" -> PaceDreamColors.Warning
        "cancelled", "rejected" -> PaceDreamColors.Error
        else -> PaceDreamColors.TextSecondary
    }
}

private fun statusIcon(label: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (label.lowercase()) {
        "upcoming", "confirmed" -> PaceDreamIcons.CheckCircle
        "completed" -> PaceDreamIcons.CheckCircle
        "pending" -> PaceDreamIcons.Schedule
        "cancelled", "rejected" -> PaceDreamIcons.Cancel
        else -> PaceDreamIcons.Info
    }
}

private fun isEndDatePast(endDate: String): Boolean {
    if (endDate.isBlank()) return false
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )
    for (fmt in formats) {
        try {
            val date = SimpleDateFormat(fmt, Locale.US).parse(endDate) ?: continue
            return date.before(Date())
        } catch (_: Exception) { continue }
    }
    return false
}

private fun formatFullDate(dateString: String): String {
    if (dateString.isBlank()) return "-"
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )
    for (fmt in formats) {
        try {
            val date = SimpleDateFormat(fmt, Locale.US).parse(dateString) ?: continue
            return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date)
        } catch (_: Exception) { continue }
    }
    return dateString
}

private fun formatTime(dateString: String): String {
    if (dateString.isBlank()) return ""
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss"
    )
    for (fmt in formats) {
        try {
            val date = SimpleDateFormat(fmt, Locale.US).parse(dateString) ?: continue
            return SimpleDateFormat("h:mm a", Locale.US).format(date)
        } catch (_: Exception) { continue }
    }
    return ""
}

@Composable
private fun BookingDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.LG),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
    ) {
        repeat(3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (it == 0) 60.dp else 120.dp),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                shape = RoundedCornerShape(PaceDreamRadius.LG)
            ) {}
        }
    }
}

@Composable
private fun BookingDetailError(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = PaceDreamIcons.Error,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Text(message, style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary)
                ) { Text("Retry") }
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Card)
                ) { Text("Back", color = PaceDreamColors.TextPrimary) }
            }
        }
    }
}
