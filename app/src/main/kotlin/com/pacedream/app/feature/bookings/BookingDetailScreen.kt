package com.pacedream.app.feature.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    onBack: () -> Unit,
    onContactHost: (String) -> Unit = {},
    viewModel: BookingDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Booking Details",
                        style = PaceDreamTypography.Headline
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowBack,
                            contentDescription = "Back",
                            tint = PaceDreamColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        when {
            // Loading with no cached data
            uiState.isLoading && uiState.booking == null -> {
                BookingDetailLoadingSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            // Error with no data
            uiState.error != null && uiState.booking == null -> {
                DetailErrorState(
                    message = uiState.error ?: "An unexpected error occurred",
                    onRetry = { viewModel.loadBookingDetail() },
                    onBack = onBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            // Content (either cached or fresh)
            uiState.booking != null -> {
                val booking = uiState.booking ?: return@Scaffold

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Hero Image with status badge overlay ──
                    HeroImage(
                        imageUrl = booking.propertyImageUrl,
                        statusLabel = booking.statusLabel,
                        status = booking.status
                    )

                    Column(
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

                        // ── Title Section (matching iOS) ──
                        TitleSection(booking = booking)

                        // ── Booking Details Card (matching iOS) ──
                        BookingDetailsCard(booking = booking)

                        // ── Pricing Card (matching iOS) ──
                        PricingCard(booking = booking)

                        // ── Cancel Button (matching iOS) ──
                        if (booking.status != BookingStatus.CANCELLED && booking.status != BookingStatus.COMPLETED) {
                            CancelSection(
                                isCancelling = uiState.isCancelling,
                                onCancelClick = { showCancelDialog = true }
                            )
                        }

                        // ── Cancel Error ──
                        uiState.cancelError?.let { error ->
                            Text(
                                text = error,
                                style = PaceDreamTypography.Footnote,
                                color = PaceDreamColors.Error
                            )
                        }

                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
                    }
                }

                // Cancel Confirmation Dialog
                if (showCancelDialog) {
                    CancelConfirmationDialog(
                        onConfirm = {
                            showCancelDialog = false
                            viewModel.cancelBooking()
                        },
                        onDismiss = { showCancelDialog = false }
                    )
                }
            }

            // Fallback - should not happen
            else -> {
                DetailErrorState(
                    message = "Booking not found.",
                    onRetry = { viewModel.loadBookingDetail() },
                    onBack = onBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

// ============================================================================
// Hero Image — matching iOS with status badge overlay
// ============================================================================
@Composable
private fun HeroImage(imageUrl: String?, statusLabel: String, status: BookingStatus) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Property image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder matching iOS
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PaceDreamColors.Gray100),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = PaceDreamIcons.Image,
                        contentDescription = null,
                        tint = PaceDreamColors.Gray400,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No image available",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.Gray400
                    )
                }
            }
        }

        // Status badge overlay (top-right, matching iOS)
        val (badgeFg, badgeBg, badgeBorder, badgeIcon) = statusBadgeStyle(status)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(badgeBg, RoundedCornerShape(PaceDreamRadius.Round))
                .border(0.5.dp, badgeBorder, RoundedCornerShape(PaceDreamRadius.Round))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = badgeFg,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = statusLabel,
                style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.SemiBold),
                color = badgeFg
            )
        }
    }
}

private data class StatusBadgeStyle(
    val fg: Color,
    val bg: Color,
    val border: Color,
    val icon: ImageVector
)

private fun statusBadgeStyle(status: BookingStatus): StatusBadgeStyle {
    return when (status) {
        BookingStatus.PENDING -> StatusBadgeStyle(
            fg = Color(0xFF8C6600),
            bg = Color(0xFFFFCC00).copy(alpha = 0.15f),
            border = Color(0xFFFFCC00).copy(alpha = 0.4f),
            icon = PaceDreamIcons.AccessTime
        )
        BookingStatus.CONFIRMED -> StatusBadgeStyle(
            fg = Color(0xFF1F4DA6),
            bg = Color(0xFF007AFF).copy(alpha = 0.12f),
            border = Color(0xFF007AFF).copy(alpha = 0.3f),
            icon = PaceDreamIcons.CheckCircle
        )
        BookingStatus.COMPLETED -> StatusBadgeStyle(
            fg = Color(0xFF1A7326),
            bg = Color(0xFF34C759).copy(alpha = 0.12f),
            border = Color(0xFF34C759).copy(alpha = 0.3f),
            icon = PaceDreamIcons.Verified
        )
        BookingStatus.CANCELLED -> StatusBadgeStyle(
            fg = Color(0xFF991A1A),
            bg = Color(0xFFFF3B30).copy(alpha = 0.12f),
            border = Color(0xFFFF3B30).copy(alpha = 0.3f),
            icon = PaceDreamIcons.Cancel
        )
    }
}

// ============================================================================
// Title Section — matching iOS titleSection
// ============================================================================
@Composable
private fun TitleSection(booking: BookingDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = booking.propertyName,
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (booking.propertyLocation.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = PaceDreamIcons.LocationOn,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = booking.propertyLocation,
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (booking.id.isNotEmpty()) {
            Text(
                text = "Booking #${booking.referenceId}",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

// ============================================================================
// Booking Details Card — matching iOS detailsCard
// ============================================================================
@Composable
private fun BookingDetailsCard(booking: BookingDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.5.dp,
                    color = PaceDreamColors.Gray200,
                    shape = RoundedCornerShape(PaceDreamRadius.LG)
                )
                .padding(20.dp)
        ) {
            Text(
                text = "Booking Details",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Check-in
            if (booking.checkInDate.isNotBlank()) {
                DetailIconRow(
                    icon = PaceDreamIcons.ArrowForward,
                    iconColor = PaceDreamColors.Success,
                    label = "Check-in",
                    value = booking.checkInDate,
                    subValue = booking.checkInTime
                )
                CardDivider()
            }

            // Check-out
            if (booking.checkOutDate.isNotBlank()) {
                DetailIconRow(
                    icon = PaceDreamIcons.ArrowBack,
                    iconColor = PaceDreamColors.Error,
                    label = "Check-out",
                    value = booking.checkOutDate,
                    subValue = booking.checkOutTime
                )
                CardDivider()
            }

            // Location
            if (booking.propertyLocation.isNotBlank()) {
                DetailIconRow(
                    icon = PaceDreamIcons.LocationOn,
                    iconColor = PaceDreamColors.Primary,
                    label = "Location",
                    value = booking.propertyLocation
                )
                CardDivider()
            }

            // Guests
            DetailIconRow(
                icon = PaceDreamIcons.People,
                iconColor = PaceDreamColors.Info,
                label = "Guests",
                value = "${booking.guestCount} guest${if (booking.guestCount == 1) "" else "s"}"
            )
            CardDivider()

            // Nights
            if (booking.nightsCount > 0) {
                DetailIconRow(
                    icon = PaceDreamIcons.Hotel,
                    iconColor = PaceDreamColors.Indigo,
                    label = "Duration",
                    value = "${booking.nightsCount} night${if (booking.nightsCount == 1) "" else "s"}"
                )
                CardDivider()
            }

            // Verification PIN
            if (!booking.verificationPin.isNullOrBlank()) {
                DetailIconRow(
                    icon = PaceDreamIcons.Lock,
                    iconColor = PaceDreamColors.Orange,
                    label = "Verification PIN",
                    value = booking.verificationPin,
                    subValue = booking.pinStatus?.let { "Status: ${it.replaceFirstChar { c -> c.uppercase() }}" }
                )
                CardDivider()
            }

            // Status
            DetailIconRow(
                icon = PaceDreamIcons.Info,
                iconColor = statusColor(booking.status),
                label = "Status",
                value = booking.statusLabel
            )
        }
    }
}

private fun statusColor(status: BookingStatus): Color {
    return when (status) {
        BookingStatus.PENDING -> PaceDreamColors.Orange
        BookingStatus.CONFIRMED -> PaceDreamColors.Blue
        BookingStatus.COMPLETED -> PaceDreamColors.Success
        BookingStatus.CANCELLED -> PaceDreamColors.Error
    }
}

// ============================================================================
// Detail Icon Row — matching iOS detailRow
// ============================================================================
@Composable
private fun DetailIconRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    subValue: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
            Text(
                text = value,
                style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.TextPrimary
            )
            if (!subValue.isNullOrBlank()) {
                Text(
                    text = subValue,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        color = PaceDreamColors.Gray200.copy(alpha = 0.5f),
        thickness = 0.5.dp
    )
}

// ============================================================================
// Pricing Card — matching iOS pricingCard
// ============================================================================
@Composable
private fun PricingCard(booking: BookingDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.5.dp,
                    color = PaceDreamColors.Gray200,
                    shape = RoundedCornerShape(PaceDreamRadius.LG)
                )
                .padding(20.dp)
        ) {
            Text(
                text = "Pricing",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Per-night rate (if available, matching iOS)
            if (booking.perNightPrice != null && booking.nightsCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Rate per night",
                        style = PaceDreamTypography.Subheadline,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        booking.perNightPrice,
                        style = PaceDreamTypography.Subheadline,
                        color = PaceDreamColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${booking.nightsCount} night${if (booking.nightsCount == 1) "" else "s"}",
                        style = PaceDreamTypography.Subheadline,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        "× ${booking.nightsCount}",
                        style = PaceDreamTypography.Subheadline,
                        color = PaceDreamColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = PaceDreamColors.Gray200)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total",
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary
                )
                Text(
                    booking.totalPrice,
                    style = PaceDreamTypography.Title2,
                    color = PaceDreamColors.TextPrimary
                )
            }
        }
    }
}

// ============================================================================
// Cancel Section — matching iOS cancelSection
// ============================================================================
@Composable
private fun CancelSection(isCancelling: Boolean, onCancelClick: () -> Unit) {
    OutlinedButton(
        onClick = onCancelClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(PaceDreamButtonHeight.LG),
        enabled = !isCancelling,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = PaceDreamColors.Error
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            PaceDreamColors.Error.copy(alpha = 0.25f)
        )
    ) {
        if (isCancelling) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = PaceDreamColors.Error,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "Cancelling...",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.Error
            )
        } else {
            Icon(
                imageVector = PaceDreamIcons.Cancel,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "Cancel Booking",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.Error
            )
        }
    }
}

// ============================================================================
// Cancel Confirmation Dialog
// ============================================================================
@Composable
private fun CancelConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaceDreamColors.Card,
        shape = RoundedCornerShape(PaceDreamRadius.XL),
        icon = {
            Icon(
                imageVector = PaceDreamIcons.Warning,
                contentDescription = null,
                tint = PaceDreamColors.Error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                "Cancel Booking?",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary
            )
        },
        text = {
            Text(
                "This will cancel your reservation. This action may not be reversible.",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaceDreamColors.Error
                ),
                shape = RoundedCornerShape(PaceDreamRadius.MD)
            ) {
                Text("Cancel Booking", style = PaceDreamTypography.Button)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Keep Booking",
                    style = PaceDreamTypography.Button,
                    color = PaceDreamColors.Primary
                )
            }
        }
    )
}

// ============================================================================
// Loading Skeleton — matching iOS BookingDetailLoadingSkeleton
// ============================================================================
@Composable
private fun BookingDetailLoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        // Hero image skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(PaceDreamColors.Gray100)
        )

        Column(
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

            // Title skeleton
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(22.dp)
                        .background(PaceDreamColors.Gray200.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(16.dp)
                        .background(PaceDreamColors.Gray200.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                )
            }

            // Details card skeleton
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray50)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    repeat(5) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        PaceDreamColors.Gray200.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(10.dp)
                                        .background(
                                            PaceDreamColors.Gray200.copy(alpha = 0.3f),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(14.dp)
                                        .background(
                                            PaceDreamColors.Gray200.copy(alpha = 0.4f),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Pricing skeleton
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray50)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(14.dp)
                                .background(
                                    PaceDreamColors.Gray200.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(24.dp)
                                .background(
                                    PaceDreamColors.Gray200.copy(alpha = 0.4f),
                                    RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Error State — matching iOS errorState
// ============================================================================
@Composable
private fun DetailErrorState(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = PaceDreamIcons.Warning,
            contentDescription = null,
            tint = PaceDreamColors.Orange,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        Text(
            text = message,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.XL)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2)) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(PaceDreamRadius.MD)
            ) {
                Text("Go Back", style = PaceDreamTypography.Button, color = PaceDreamColors.Primary)
            }

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamRadius.MD)
            ) {
                Text("Retry", style = PaceDreamTypography.Button, color = Color.White)
            }
        }
    }
}
