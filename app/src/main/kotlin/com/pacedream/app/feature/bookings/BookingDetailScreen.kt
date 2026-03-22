package com.pacedream.app.feature.bookings

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
                        style = PaceDreamTypography.Title3
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
            // Loading
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PaceDreamColors.Primary)
                }
            }

            // Error
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = PaceDreamIcons.ErrorOutline,
                            contentDescription = null,
                            tint = PaceDreamColors.Error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        Text(
                            uiState.error!!,
                            color = PaceDreamColors.TextSecondary,
                            style = PaceDreamTypography.Body
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        Button(
                            onClick = { viewModel.loadBookingDetail() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PaceDreamColors.Primary
                            ),
                            shape = RoundedCornerShape(PaceDreamRadius.MD)
                        ) {
                            Text("Retry", style = PaceDreamTypography.Button)
                        }
                    }
                }
            }

            // Content
            uiState.booking != null -> {
                val booking = uiState.booking!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Hero Property Image ────────────────────────────
                    PropertyHeroImage(imageUrl = booking.propertyImageUrl)

                    Column(
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                    ) {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                        // ── Status Badge + Reference ID ────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BookingStatusBadge(status = booking.status)
                            Text(
                                text = "Ref: ${booking.referenceId}",
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                        // ── Property Name ──────────────────────────────
                        Text(
                            text = booking.propertyName,
                            style = PaceDreamTypography.Title2,
                            color = PaceDreamColors.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // ── Location ───────────────────────────────────
                        if (booking.propertyLocation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = PaceDreamIcons.LocationOn,
                                    contentDescription = null,
                                    tint = PaceDreamColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                                Text(
                                    text = booking.propertyLocation,
                                    style = PaceDreamTypography.Callout,
                                    color = PaceDreamColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

                        // ── Booking Dates Card ─────────────────────────
                        BookingDatesCard(booking = booking)

                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                        // ── Pricing Breakdown Card ─────────────────────
                        PricingBreakdownCard(booking = booking)

                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                        // ── Host Info Card ─────────────────────────────
                        HostInfoCard(
                            hostName = booking.hostName,
                            hostAvatarUrl = booking.hostAvatarUrl,
                            onMessageHost = {
                                booking.hostId?.let { onContactHost(it) }
                            }
                        )

                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                        // ── Cancellation Policy ────────────────────────
                        CancellationPolicyCard(policy = booking.cancellationPolicy)

                        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

                        // ── Action Buttons ─────────────────────────────
                        if (booking.status != BookingStatus.CANCELLED && booking.status != BookingStatus.COMPLETED) {
                            ActionButtons(
                                isCancelling = uiState.isCancelling,
                                onCancelClick = { showCancelDialog = true },
                                onContactHostClick = {
                                    booking.hostId?.let { onContactHost(it) }
                                }
                            )
                        }

                        // ── Cancel Error ───────────────────────────────
                        uiState.cancelError?.let { error ->
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                            Text(
                                text = error,
                                style = PaceDreamTypography.Footnote,
                                color = PaceDreamColors.Error
                            )
                        }

                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XXL))
                    }
                }

                // ── Cancel Confirmation Dialog ─────────────────────
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
        }
    }
}

// ============================================================================
// Hero Property Image
// ============================================================================
@Composable
private fun PropertyHeroImage(imageUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(PaceDreamColors.Gray100)
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Property image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Image,
                    contentDescription = null,
                    tint = PaceDreamColors.TextTertiary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

// ============================================================================
// Booking Status Badge
// ============================================================================
@Composable
private fun BookingStatusBadge(status: BookingStatus) {
    val (backgroundColor, textColor) = when (status) {
        BookingStatus.PENDING -> PaceDreamColors.Warning.copy(alpha = 0.15f) to PaceDreamColors.Warning
        BookingStatus.CONFIRMED -> PaceDreamColors.Success.copy(alpha = 0.15f) to PaceDreamColors.Success
        BookingStatus.CANCELLED -> PaceDreamColors.Error.copy(alpha = 0.15f) to PaceDreamColors.Error
        BookingStatus.COMPLETED -> PaceDreamColors.Info.copy(alpha = 0.15f) to PaceDreamColors.Info
    }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(PaceDreamRadius.Round)
            )
            .padding(horizontal = PaceDreamSpacing.SM2, vertical = PaceDreamSpacing.XS)
    ) {
        Text(
            text = status.label,
            style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
            color = textColor
        )
    }
}

// ============================================================================
// Booking Dates Card
// ============================================================================
@Composable
private fun BookingDatesCard(booking: BookingDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Text(
                text = "Booking Dates",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))

            // Check-in
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PaceDreamIcons.CalendarToday,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Column {
                    Text(
                        text = "Check-in",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        text = buildString {
                            append(booking.checkInDate)
                            booking.checkInTime?.let { append(" at $it") }
                        },
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            // Check-out
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PaceDreamIcons.CalendarToday,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Column {
                    Text(
                        text = "Check-out",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        text = buildString {
                            append(booking.checkOutDate)
                            booking.checkOutTime?.let { append(" at $it") }
                        },
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            HorizontalDivider(color = PaceDreamColors.Border)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            // Guest count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PaceDreamIcons.People,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    text = "${booking.guestCount} ${if (booking.guestCount == 1) "Guest" else "Guests"}",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary
                )
            }
        }
    }
}

// ============================================================================
// Pricing Breakdown Card
// ============================================================================
@Composable
private fun PricingBreakdownCard(booking: BookingDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Text(
                text = "Price Details",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))

            PriceRow(label = "Base price", amount = booking.basePrice)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            PriceRow(label = "Service fee", amount = booking.serviceFee)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            PriceRow(label = "Taxes", amount = booking.taxes)

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            HorizontalDivider(color = PaceDreamColors.Border)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary
                )
                Text(
                    text = booking.totalPrice,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextSecondary
        )
        Text(
            text = amount,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextPrimary
        )
    }
}

// ============================================================================
// Host Info Card
// ============================================================================
@Composable
private fun HostInfoCard(
    hostName: String,
    hostAvatarUrl: String?,
    onMessageHost: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Host avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Gray100),
                contentAlignment = Alignment.Center
            ) {
                if (hostAvatarUrl != null) {
                    AsyncImage(
                        model = hostAvatarUrl,
                        contentDescription = "Host avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = PaceDreamIcons.Person,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM2))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hosted by",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
                Text(
                    text = hostName,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary
                )
            }

            OutlinedButton(
                onClick = onMessageHost,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PaceDreamColors.Primary
                )
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Message,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                Text("Message", style = PaceDreamTypography.Footnote)
            }
        }
    }
}

// ============================================================================
// Cancellation Policy Card
// ============================================================================
@Composable
private fun CancellationPolicyCard(policy: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PaceDreamIcons.Policy,
                    contentDescription = null,
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    text = "Cancellation Policy",
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = policy,
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

// ============================================================================
// Action Buttons
// ============================================================================
@Composable
private fun ActionButtons(
    isCancelling: Boolean,
    onCancelClick: () -> Unit,
    onContactHostClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Contact Host — primary
        Button(
            onClick = onContactHostClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(PaceDreamButtonHeight.LG),
            colors = ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Primary
            ),
            shape = RoundedCornerShape(PaceDreamRadius.MD)
        ) {
            Icon(
                imageVector = PaceDreamIcons.Message,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text("Contact Host", style = PaceDreamTypography.Button)
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        // Cancel Booking — destructive
        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(PaceDreamButtonHeight.LG),
            enabled = !isCancelling,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PaceDreamColors.Error
            ),
            shape = RoundedCornerShape(PaceDreamRadius.MD)
        ) {
            if (isCancelling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = PaceDreamColors.Error,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text("Cancelling...", style = PaceDreamTypography.Button, color = PaceDreamColors.Error)
            } else {
                Icon(
                    imageVector = PaceDreamIcons.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text("Cancel Booking", style = PaceDreamTypography.Button, color = PaceDreamColors.Error)
            }
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
                "Cancel Booking",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary
            )
        },
        text = {
            Text(
                "Are you sure you want to cancel this booking? This action cannot be undone. " +
                    "Refund eligibility depends on the cancellation policy.",
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
                Text("Yes, Cancel", style = PaceDreamTypography.Button)
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
