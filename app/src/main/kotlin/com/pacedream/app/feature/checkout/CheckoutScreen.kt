package com.pacedream.app.feature.checkout

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamElevation
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    draft: BookingDraft,
    onBackClick: () -> Unit,
    onConfirmSuccess: (bookingId: String) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(draft) {
        viewModel.setDraft(draft)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CheckoutViewModel.Effect.NavigateToConfirmation -> onConfirmSuccess(effect.bookingId)
                is CheckoutViewModel.Effect.LaunchStripeCheckout -> {
                    runCatching {
                        val customTabsIntent = CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .build()
                        customTabsIntent.launchUrl(context, Uri.parse(effect.checkoutUrl))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", style = PaceDreamTypography.Title2) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PaceDreamSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

                // Listing info card with type icon
                ListingInfoCard(draft = draft)

                // Booking summary card
                Card(
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
                ) {
                    Column(
                        modifier = Modifier.padding(PaceDreamSpacing.MD),
                        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                    ) {
                        Text(
                            "Booking details",
                            style = PaceDreamTypography.Callout,
                            fontWeight = FontWeight.SemiBold,
                            color = PaceDreamColors.TextPrimary
                        )
                        HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)
                        SummaryRow("Date", draft.date)
                        SummaryRow("Check-in", formatTimeDisplay(draft.startTimeISO))
                        SummaryRow("Check-out", formatTimeDisplay(draft.endTimeISO))
                        SummaryRow("Guests", "${draft.guests} guest${if (draft.guests != 1) "s" else ""}")
                    }
                }

                // Price breakdown
                draft.totalAmountEstimate?.let { total ->
                    PriceBreakdownCard(total = total)
                }

                // Cancellation policy
                CancellationPolicyCard()

                // Inline error banner (iOS parity: inline, not snackbar)
                uiState.errorMessage?.let {
                    Card(
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.ErrorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(PaceDreamSpacing.MD),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.Warning,
                                contentDescription = null,
                                tint = PaceDreamColors.Error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                            Text(
                                it.removePrefix("Server error 200: "),
                                color = PaceDreamColors.OnErrorContainer,
                                style = PaceDreamTypography.Callout,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            }

            // Sticky bottom bar with pay button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PaceDreamColors.Card)
                    .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.MD)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "You won't be charged yet",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    Button(
                        onClick = { viewModel.submitBooking() },
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = PaceDreamColors.OnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        }
                        Text(
                            text = if (uiState.isSubmitting) "Confirming…"
                            else draft.totalAmountEstimate?.let { "Pay $${String.format("%.2f", it)}" }
                                ?: "Confirm Booking",
                            style = PaceDreamTypography.Button,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/** Listing image + type card at top of checkout */
@Composable
private fun ListingInfoCard(draft: BookingDraft) {
    Card(
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.MD))
                    .background(PaceDreamColors.Primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (draft.listingType) {
                        "gear" -> PaceDreamIcons.ShoppingBag
                        "split-stay" -> PaceDreamIcons.Group
                        else -> PaceDreamIcons.Home
                    },
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (draft.listingType) {
                        "gear" -> "Gear Rental"
                        "split-stay" -> "Split Stay"
                        else -> "Hourly Space"
                    },
                    style = PaceDreamTypography.Callout,
                    fontWeight = FontWeight.SemiBold,
                    color = PaceDreamColors.TextPrimary
                )
                Text(
                    text = "Booking for ${draft.date}",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
        }
    }
}

/** Price breakdown with subtotal, service fee, total */
@Composable
private fun PriceBreakdownCard(total: Double) {
    Card(
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Text(
                "Price details",
                style = PaceDreamTypography.Callout,
                fontWeight = FontWeight.SemiBold,
                color = PaceDreamColors.TextPrimary
            )
            HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)
            SummaryRow("Subtotal", "$${String.format("%.2f", total)}")
            SummaryRow("Service fee", "$0.00")
            HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total",
                    fontWeight = FontWeight.Bold,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary
                )
                Text(
                    "$${String.format("%.2f", total)}",
                    fontWeight = FontWeight.Bold,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.Primary
                )
            }
        }
    }
}

/** Cancellation policy summary */
@Composable
private fun CancellationPolicyCard() {
    Card(
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PaceDreamIcons.Info,
                    contentDescription = null,
                    tint = PaceDreamColors.Info,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    "Cancellation policy",
                    style = PaceDreamTypography.Callout,
                    fontWeight = FontWeight.SemiBold,
                    color = PaceDreamColors.TextPrimary
                )
            }
            Text(
                "Free cancellation before the booking start time. " +
                    "After that, cancel before check-in and get a full refund, minus the service fee.",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = PaceDreamColors.TextSecondary, style = PaceDreamTypography.Callout)
        Text(value, fontWeight = FontWeight.Medium, style = PaceDreamTypography.Callout, color = PaceDreamColors.TextPrimary)
    }
}

private fun formatTimeDisplay(iso: String): String {
    val timePart = iso.substringAfter("T").take(5)
    return try {
        val hour = timePart.substringBefore(":").toInt()
        val minute = timePart.substringAfter(":").toInt()
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        "$displayHour:${String.format("%02d", minute)} $amPm"
    } catch (_: Exception) {
        timePart
    }
}
