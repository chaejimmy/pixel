package com.pacedream.app.feature.checkout

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
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
import com.pacedream.common.icon.PaceDreamIcons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

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
                .verticalScroll(rememberScrollState())
                .padding(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            // Booking Details Card
            Text(
                "Your booking",
                style = PaceDreamTypography.Title3,
                fontWeight = FontWeight.SemiBold,
                color = PaceDreamColors.TextPrimary
            )

            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(PaceDreamSpacing.MD),
                    verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    SummaryRow(
                        label = "Date",
                        value = draft.date,
                        icon = PaceDreamIcons.CalendarToday
                    )
                    SummaryRow(
                        label = "Check-in",
                        value = formatTimeDisplay(draft.startTimeISO),
                        icon = PaceDreamIcons.Schedule
                    )
                    SummaryRow(
                        label = "Check-out",
                        value = formatTimeDisplay(draft.endTimeISO),
                        icon = PaceDreamIcons.Schedule
                    )
                    SummaryRow(
                        label = "Guests",
                        value = "${draft.guests} guest${if (draft.guests != 1) "s" else ""}",
                        icon = PaceDreamIcons.Person
                    )
                }
            }

            // Price Breakdown Card
            draft.totalAmountEstimate?.let { total ->
                Text(
                    "Price details",
                    style = PaceDreamTypography.Title3,
                    fontWeight = FontWeight.SemiBold,
                    color = PaceDreamColors.TextPrimary
                )

                Card(
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                        // Subtotal (before taxes)
                        val taxes = total * 0.20
                        val subtotal = total - taxes
                        PriceRow("Subtotal", "$${String.format("%.2f", subtotal)}")
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                        PriceRow("Taxes & fees", "$${String.format("%.2f", taxes)}")
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        HorizontalDivider(color = PaceDreamColors.Border.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Total",
                                style = PaceDreamTypography.Headline,
                                fontWeight = FontWeight.Bold,
                                color = PaceDreamColors.TextPrimary
                            )
                            Text(
                                "$${String.format("%.2f", total)}",
                                style = PaceDreamTypography.Headline,
                                fontWeight = FontWeight.Bold,
                                color = PaceDreamColors.Primary
                            )
                        }
                    }
                }
            }

            // Cancellation info
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Primary.copy(alpha = 0.05f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(PaceDreamSpacing.MD),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        PaceDreamIcons.Info,
                        contentDescription = null,
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text(
                        "Free cancellation up to 2 hours before check-in",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }

            // Inline Error Banner (iOS parity: inline, not snackbar)
            uiState.errorMessage?.let { error ->
                Card(
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = CardDefaults.cardColors(
                        containerColor = PaceDreamColors.Error.copy(alpha = 0.08f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(PaceDreamSpacing.MD),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            PaceDreamIcons.Error,
                            contentDescription = null,
                            tint = PaceDreamColors.Error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        Text(
                            error.removePrefix("Server error 200: "),
                            color = PaceDreamColors.TextPrimary,
                            style = PaceDreamTypography.Callout
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Reassurance text
            Text(
                "You won't be charged yet",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

            // Confirm Button
            Button(
                onClick = { viewModel.submitBooking() },
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                contentPadding = PaddingValues(vertical = 14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = PaceDreamColors.OnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    if (uiState.isSubmitting) "Confirming…" else "Confirm Booking",
                    style = PaceDreamTypography.Button
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(label, color = PaceDreamColors.TextSecondary, style = PaceDreamTypography.Callout)
        }
        Text(
            value,
            fontWeight = FontWeight.Medium,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextPrimary
        )
    }
}

@Composable
private fun PriceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = PaceDreamColors.TextSecondary, style = PaceDreamTypography.Body)
        Text(value, style = PaceDreamTypography.Body, color = PaceDreamColors.TextPrimary)
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
