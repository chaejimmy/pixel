package com.pacedream.app.feature.checkout

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamElevation
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import timber.log.Timber
import java.text.NumberFormat
import java.util.Currency

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

    // Use Stripe's Compose-aware API which properly registers the
    // ActivityResultLauncher via rememberLauncherForActivityResult.
    // The old PaymentSheet(activity, callback) constructor requires
    // registration before onStart(), which is too late when navigating
    // to a Compose screen via Navigation.
    val paymentSheet = rememberPaymentSheet { result ->
        when (result) {
            is PaymentSheetResult.Completed -> viewModel.onPaymentSheetCompleted()
            is PaymentSheetResult.Canceled -> viewModel.onPaymentSheetCancelled()
            is PaymentSheetResult.Failed -> viewModel.onPaymentSheetFailed(
                result.error.localizedMessage ?: "Payment failed. Please try again."
            )
        }
    }

    LaunchedEffect(draft) {
        viewModel.setDraft(draft)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CheckoutViewModel.Effect.NavigateToConfirmation -> {
                    onConfirmSuccess(effect.bookingId)
                }
                is CheckoutViewModel.Effect.PresentPaymentSheet -> {
                    try {
                        // Initialize Stripe with the resolved publishable key
                        PaymentConfiguration.init(context, effect.publishableKey)

                        // Configure PaymentSheet with Google Pay (iOS parity: Apple Pay)
                        val configBuilder = PaymentSheet.Configuration.Builder(effect.merchantDisplayName)
                            // Enable Google Pay (Android equivalent of iOS Apple Pay)
                            .googlePay(PaymentSheet.GooglePayConfiguration(
                                environment = if (effect.publishableKey.startsWith("pk_test_")) {
                                    PaymentSheet.GooglePayConfiguration.Environment.Test
                                } else {
                                    PaymentSheet.GooglePayConfiguration.Environment.Production
                                },
                                countryCode = "US",
                                currencyCode = uiState.quote?.currency?.uppercase() ?: "USD"
                            ))
                            // Block ACH / bank debits / any delayed-notification payment methods.
                            // Guest checkout should only offer Card + Google Pay.
                            .allowsDelayedPaymentMethods(false)
                            // Prioritize card (Google Pay is presented automatically when
                            // available and googlePay config is set). Any other method types
                            // not in this list are deprioritized / hidden.
                            .paymentMethodOrder(listOf("card"))
                            // PaceDream brand appearance
                            .appearance(PaymentSheet.Appearance(
                                shapes = PaymentSheet.Shapes(
                                    cornerRadiusDp = 12f,
                                    borderStrokeWidthDp = 0.5f
                                )
                            ))

                        // Customer session for saved cards
                        if (effect.customerId != null && effect.ephemeralKeySecret != null) {
                            configBuilder.customer(
                                PaymentSheet.CustomerConfiguration(
                                    id = effect.customerId,
                                    ephemeralKeySecret = effect.ephemeralKeySecret
                                )
                            )
                        }

                        val config = configBuilder.build()

                        paymentSheet.presentWithPaymentIntent(
                            effect.clientSecret,
                            config
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to present PaymentSheet")
                        viewModel.onPaymentSheetFailed(
                            e.localizedMessage ?: "Unable to open payment form. Please try again."
                        )
                    }
                }
            }
        }
    }

    // Post-payment success state (iOS parity: NativeCheckoutView.successContent)
    if (uiState.status == CheckoutStatus.SUCCEEDED) {
        Scaffold(containerColor = PaceDreamColors.Background) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Only show the green CheckCircle when the backend has actually
                // confirmed the booking. Otherwise show a neutral icon and truthful
                // "payment received, still finalizing" copy so the user is not told
                // the booking is confirmed before it actually is.
                val bookingConfirmed = uiState.bookingId != null
                Icon(
                    imageVector = if (bookingConfirmed) PaceDreamIcons.CheckCircle else PaceDreamIcons.Info,
                    contentDescription = null,
                    tint = if (bookingConfirmed) PaceDreamColors.Success else PaceDreamColors.Primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                Text(
                    if (bookingConfirmed) "Booking Confirmed" else "Payment Received",
                    style = PaceDreamTypography.Title2,
                    fontWeight = FontWeight.Bold,
                    color = PaceDreamColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Text(
                    if (bookingConfirmed) {
                        "Your booking has been confirmed."
                    } else {
                        "Payment received \u2014 finalizing your booking\u2026"
                    },
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                if (uiState.bookingId != null) {
                    // Booking confirmed — normal success path
                    Button(
                        onClick = { uiState.bookingId?.let(onConfirmSuccess) },
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
                    ) {
                        Text("View Booking", style = PaceDreamTypography.Button, fontWeight = FontWeight.Bold)
                    }
                } else if (uiState.isConfirmingBooking) {
                    // Actively confirming
                    CircularProgressIndicator(color = PaceDreamColors.Primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    Text(
                        "Finalizing your booking\u2026",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                } else if (uiState.hasExhaustedRetries && uiState.pendingPaymentIntentId != null) {
                    // Local retries exhausted — truthful stuck copy
                    // with payment reference + copy + contact support.
                    StuckFallbackBanner(
                        paymentIntentId = uiState.pendingPaymentIntentId!!,
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    SupportActionRow(
                        paymentIntentId = uiState.pendingPaymentIntentId!!,
                        listingTitle = uiState.draft?.listingId ?: "",
                    )
                } else {
                    // Payment succeeded but booking confirmation failed — show retry
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.MD)
                            .background(
                                color = PaceDreamColors.Warning.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(PaceDreamRadius.SM)
                            )
                            .padding(PaceDreamSpacing.SM2),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Info,
                            contentDescription = null,
                            tint = PaceDreamColors.Warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Your payment was received. Your booking is being finalized \u2014 it will appear in the Bookings tab shortly.",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    if (uiState.confirmRetryCount < CheckoutViewModel.MAX_CONFIRM_RETRIES) {
                        Button(
                            onClick = { viewModel.retryConfirmBooking() },
                            shape = RoundedCornerShape(PaceDreamRadius.MD),
                            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
                        ) {
                            Text("Retry", style = PaceDreamTypography.Button, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm & Pay", style = PaceDreamTypography.Headline) },
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
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2)
            ) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XXS))

                // Listing info card
                ListingInfoCard(draft = draft)

                // Booking summary card
                BookingDetailsCard(draft = draft)

                // Price breakdown from backend quote (iOS parity: priceBreakdownCard)
                // Early access note is integrated into the price card
                PriceBreakdownCard(
                    quote = uiState.quote,
                    isLoadingQuote = uiState.status == CheckoutStatus.LOADING_QUOTE
                )

                // Cancellation policy
                CancellationPolicyCard()

                // Error banner
                uiState.errorMessage?.let { message ->
                    ErrorBanner(message = message, onRetry = { viewModel.retryQuote() })
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }

            // Sticky bottom pay bar (iOS parity: payBar)
            PayBar(
                quote = uiState.quote,
                status = uiState.status,
                onPayClick = { viewModel.submitPayment() }
            )
        }
    }
}

// ── Pay Bar (iOS parity: NativeCheckoutView.payBar) ──

@Composable
private fun PayBar(
    quote: QuoteResponse?,
    status: CheckoutStatus,
    onPayClick: () -> Unit
) {
    val isEnabled = status == CheckoutStatus.READY && quote != null
    val isProcessing = status == CheckoutStatus.PROCESSING

    HorizontalDivider(thickness = 0.5.dp, color = PaceDreamColors.Border)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaceDreamColors.Card)
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM2),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onPayClick,
            enabled = isEnabled && !isProcessing,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Total",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.OnPrimary.copy(alpha = 0.8f)
                    )
                    if (quote != null) {
                        Text(
                            formatCents(quote.totalCents, quote.currency),
                            style = PaceDreamTypography.Headline,
                            fontWeight = FontWeight.Bold,
                            color = PaceDreamColors.OnPrimary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = PaceDreamColors.OnPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    }
                    Text(
                        text = if (isProcessing) "Processing\u2026" else "Pay",
                        style = PaceDreamTypography.Headline,
                        fontWeight = FontWeight.Bold,
                        color = PaceDreamColors.OnPrimary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = PaceDreamIcons.Lock,
                contentDescription = null,
                tint = PaceDreamColors.TextTertiary,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Secure payment powered by Stripe",
                style = PaceDreamTypography.Caption2,
                color = PaceDreamColors.TextTertiary
            )
        }
    }
}

// ── Listing Info Card ──

@Composable
private fun ListingInfoCard(draft: BookingDraft) {
    Card(
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            PaceDreamColors.Border.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
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
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM2))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (draft.listingType) {
                        "gear" -> "Item Rental"
                        "split-stay" -> "Service Booking"
                        else -> "Space Booking"
                    },
                    style = PaceDreamTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    color = PaceDreamColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatDateDisplay(draft.date),
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.TextSecondary
                )
                Text(
                    text = "${formatTimeDisplay(draft.startTimeISO)} \u2013 ${formatTimeDisplay(draft.endTimeISO)}",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextTertiary
                )
            }
        }
    }
}

// ── Booking Details Card ──

@Composable
private fun BookingDetailsCard(draft: BookingDraft) {
    Card(
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            PaceDreamColors.Border.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Text(
                "Schedule",
                style = PaceDreamTypography.Callout,
                fontWeight = FontWeight.SemiBold,
                color = PaceDreamColors.TextPrimary
            )
            HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)
            SummaryRow("Date", formatDateDisplay(draft.date))
            SummaryRow("Start", formatTimeDisplay(draft.startTimeISO))
            SummaryRow("End", formatTimeDisplay(draft.endTimeISO))
            SummaryRow("Guests", "${draft.guests} guest${if (draft.guests != 1) "s" else ""}")
        }
    }
}

// ── Price Breakdown Card (iOS parity: uses backend quote) ──

@Composable
private fun PriceBreakdownCard(
    quote: QuoteResponse?,
    isLoadingQuote: Boolean
) {
    Card(
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            PaceDreamColors.Border.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Text(
                "Price breakdown",
                style = PaceDreamTypography.Callout,
                fontWeight = FontWeight.SemiBold,
                color = PaceDreamColors.TextPrimary
            )
            HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)

            if (quote != null) {
                SummaryRow("Price", formatCents(quote.baseAmountCents, quote.currency))

                if (quote.serviceFeeCents > 0) {
                    SummaryRow("Service fee", formatCents(quote.serviceFeeCents, quote.currency))
                } else {
                    // Early access: service fee waived (iOS parity)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Service fee", color = PaceDreamColors.TextSecondary, style = PaceDreamTypography.Callout)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "$0.00",
                                color = PaceDreamColors.Success,
                                style = PaceDreamTypography.Callout,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Waived",
                                color = PaceDreamColors.Success.copy(alpha = 0.8f),
                                style = PaceDreamTypography.Caption
                            )
                        }
                    }
                }

                if (quote.taxCents > 0) {
                    SummaryRow("Tax", formatCents(quote.taxCents, quote.currency))
                }

                HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Total",
                        fontWeight = FontWeight.Bold,
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary
                    )
                    Text(
                        formatCents(quote.totalCents, quote.currency),
                        fontWeight = FontWeight.Bold,
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary
                    )
                }

                // Early access note — integrated into price card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = PaceDreamColors.Primary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(PaceDreamRadius.SM)
                        )
                        .padding(horizontal = PaceDreamSpacing.SM2, vertical = PaceDreamSpacing.SM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Star,
                        contentDescription = null,
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text(
                        "Platform fee is \$0 during early access",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            } else if (isLoadingQuote) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = PaceDreamColors.Primary
                    )
                    Text(
                        "Calculating price\u2026",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            } else {
                Text(
                    "Unable to calculate price",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.Error
                )
            }
        }
    }
}

// ── Cancellation Policy Card ──

@Composable
private fun CancellationPolicyCard() {
    Card(
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            PaceDreamColors.Border.copy(alpha = 0.4f)
        )
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

// ── Stuck / Support Fallback (post-payment, retries exhausted) ──

/**
 * Shown after local confirm-booking retries are exhausted.  Truthful
 * copy: payment IS captured, booking has not yet been created, this
 * is the reference the user should quote to support.  The backend
 * has been notified via /payments/native/report-failure by the
 * ViewModel, so admin can reconcile while the user reads this.
 */
@Composable
private fun StuckFallbackBanner(paymentIntentId: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD)
            .background(
                color = PaceDreamColors.Warning.copy(alpha = 0.08f),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
            )
            .padding(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            Icon(
                imageVector = PaceDreamIcons.Warning,
                contentDescription = null,
                tint = PaceDreamColors.Warning,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "We received your payment, but we couldn\u2019t finish creating your booking.",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary,
                )
                Text(
                    "Our team has been notified and is reconciling your payment. You don\u2019t need to pay again.",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Payment reference",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
            )
            Text(
                text = paymentIntentId,
                style = PaceDreamTypography.Caption,
                fontFamily = FontFamily.Monospace,
                color = PaceDreamColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = PaceDreamSpacing.SM),
            )
        }
    }
}

/**
 * Two-button row under the stuck banner: copies the PaymentIntent id
 * to the clipboard, or launches a pre-filled support email intent.
 */
@Composable
private fun SupportActionRow(paymentIntentId: String, listingTitle: String) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
    ) {
        OutlinedButton(
            onClick = { clipboard.setText(AnnotatedString(paymentIntentId)) },
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                "Copy payment reference",
                style = PaceDreamTypography.Button,
                fontWeight = FontWeight.SemiBold,
                color = PaceDreamColors.Primary,
            )
        }
        Button(
            onClick = {
                val subject = "PaceDream payment reference $paymentIntentId"
                val body = buildString {
                    append("Hi PaceDream support,\n\n")
                    append("My payment went through but the booking wasn\u2019t created.\n\n")
                    append("Payment reference: $paymentIntentId\n")
                    if (listingTitle.isNotBlank()) append("Listing: $listingTitle\n")
                    append("\nPlease reconcile and confirm my booking.")
                }
                val uri = android.net.Uri.parse(
                    "mailto:support@pacedream.com" +
                        "?subject=" + android.net.Uri.encode(subject) +
                        "&body=" + android.net.Uri.encode(body)
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, uri)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to launch mailto intent")
                }
            },
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Text("Contact support", style = PaceDreamTypography.Button, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Error Banner (iOS parity: errorBanner) ──

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.ErrorContainer)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PaceDreamIcons.Warning,
                    contentDescription = null,
                    tint = PaceDreamColors.Error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    message.removePrefix("Server error 200: "),
                    color = PaceDreamColors.OnErrorContainer,
                    style = PaceDreamTypography.Callout,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Error),
                shape = RoundedCornerShape(PaceDreamRadius.SM),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Retry", style = PaceDreamTypography.Caption, color = PaceDreamColors.OnPrimary)
            }
        }
    }
}

// ── Helpers ──

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

private val threadLocalCurrencyFormatter = ThreadLocal.withInitial {
    mutableMapOf<String, NumberFormat>()
}

private fun formatCents(cents: Int, currency: String): String {
    val dollars = cents / 100.0
    return try {
        val cache = threadLocalCurrencyFormatter.get()
        val formatter = cache.getOrPut(currency.uppercase()) {
            NumberFormat.getCurrencyInstance().apply {
                this.currency = java.util.Currency.getInstance(currency.uppercase())
            }
        }
        formatter.format(dollars)
    } catch (_: Exception) {
        String.format("$%.2f", dollars)
    }
}

private fun formatDateDisplay(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val month = when (parts[1].toInt()) {
                1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
                5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
                9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
                else -> parts[1]
            }
            "$month ${parts[2].toInt()}, ${parts[0]}"
        } else dateStr
    } catch (_: Exception) { dateStr }
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
