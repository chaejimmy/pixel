package com.shourov.apps.pacedream.feature.host.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.data.PayoutConnectionState
import com.shourov.apps.pacedream.feature.host.data.PayoutMethod

/**
 * Host Earnings Screen - iOS parity.
 *
 * Matches iOS HostEarningsView with Stripe Connect integration:
 * - Stripe account card with connection status
 * - Onboarding / continue setup / manage payout settings CTA
 * - View Stripe Dashboard button (for connected hosts)
 * - Payout methods list
 * - How payouts work help section
 * - Pull-to-refresh
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostEarningsScreen(
    viewModel: HostEarningsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle URL opening (matches iOS SafariView sheet)
    LaunchedEffect(uiState.onboardingUrl, uiState.loginUrl) {
        val url = uiState.onboardingUrl ?: uiState.loginUrl
        if (url != null) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            viewModel.clearUrls()
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refreshData() },
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Title (iOS: navigationTitle)
            item {
                Text(
                    text = "Earnings & Payments",
                    style = PaceDreamTypography.Title1,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error banner
            uiState.error?.let { error ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Stripe Account Card (iOS parity: stripeAccountCard)
            item {
                StripeAccountCard(
                    connectionState = uiState.connectionState,
                    requirementsCurrentlyDue = uiState.requirementsCurrentlyDue,
                    isLoading = uiState.isLoading,
                    isBusy = uiState.isBusy,
                    onPrimaryAction = { viewModel.performPrimaryAction() },
                    onOpenDashboard = { viewModel.openDashboard() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Payout Methods (iOS parity: payoutMethodsCard)
            if (uiState.connectionState == PayoutConnectionState.CONNECTED && uiState.payoutMethods.isNotEmpty()) {
                item {
                    PayoutMethodsCard(methods = uiState.payoutMethods)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // How Payouts Work (iOS parity: helpFooter)
            item {
                HowPayoutsWorkCard(connectionState = uiState.connectionState)
            }
        }
    }
}

// ── Stripe Account Card (matches iOS stripeAccountCard) ─────────

@Composable
private fun StripeAccountCard(
    connectionState: PayoutConnectionState,
    requirementsCurrentlyDue: List<String>,
    isLoading: Boolean,
    isBusy: Boolean,
    onPrimaryAction: () -> Unit,
    onOpenDashboard: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PaceDreamColors.Background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.CreditCard,
                        contentDescription = null,
                        tint = PaceDreamColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Payout settings",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when (connectionState) {
                            PayoutConnectionState.CONNECTED ->
                                "Your Stripe account is connected. Payouts are enabled."
                            PayoutConnectionState.PENDING ->
                                "Finish setting up your Stripe account to start receiving payouts."
                            PayoutConnectionState.NOT_CONNECTED ->
                                "Connect a Stripe account to receive payouts from your bookings."
                        },
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status badge
                    if (!isLoading) {
                        val (badgeText, badgeColor) = when (connectionState) {
                            PayoutConnectionState.CONNECTED -> "Connected" to PaceDreamColors.Success
                            PayoutConnectionState.PENDING -> "Action required" to PaceDreamColors.Warning
                            PayoutConnectionState.NOT_CONNECTED -> "Not connected" to PaceDreamColors.TextSecondary
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(badgeColor.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = badgeText,
                                fontSize = 12.sp,
                                color = badgeColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Requirements warning (iOS parity: PENDING state)
            if (connectionState == PayoutConnectionState.PENDING && requirementsCurrentlyDue.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PaceDreamColors.Warning.copy(alpha = 0.08f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Warning,
                        contentDescription = null,
                        tint = PaceDreamColors.Warning,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Action required",
                            fontWeight = FontWeight.SemiBold,
                            color = PaceDreamColors.Warning,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Complete the remaining steps to start receiving payouts.",
                            color = PaceDreamColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary CTA button
            Button(
                onClick = onPrimaryAction,
                enabled = !isBusy && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 13.dp)
            ) {
                Text(
                    text = when (connectionState) {
                        PayoutConnectionState.CONNECTED -> "Manage payout settings"
                        PayoutConnectionState.PENDING -> "Continue setup"
                        PayoutConnectionState.NOT_CONNECTED -> "Set up payouts"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = PaceDreamIcons.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Dashboard button (for connected hosts - iOS parity)
            if (connectionState == PayoutConnectionState.CONNECTED) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenDashboard,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View Stripe Dashboard",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            // Stripe branding (for non-connected states)
            if (connectionState != PayoutConnectionState.CONNECTED) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = PaceDreamIcons.Lock,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Powered by Stripe. Your financial data is securely handled.",
                        color = PaceDreamColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ── Payout Methods Card (iOS parity) ────────────────────────────

@Composable
private fun PayoutMethodsCard(methods: List<PayoutMethod>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Payout methods",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            methods.forEach { method ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PaceDreamColors.Background)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (method.type.lowercase().contains("card"))
                            PaceDreamIcons.CreditCard else PaceDreamIcons.AccountBalance,
                        contentDescription = null,
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = method.label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = PaceDreamColors.TextPrimary
                        )
                        if (method.isPrimary) {
                            Text(
                                text = "Primary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PaceDreamColors.TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// ── How Payouts Work (iOS parity: helpFooter) ───────────────────

@Composable
private fun HowPayoutsWorkCard(connectionState: PayoutConnectionState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "How payouts work",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = PaceDreamColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            HelpStep(step = "1", text = "Guest completes a booking and pays through the platform")
            Spacer(modifier = Modifier.height(8.dp))
            HelpStep(step = "2", text = "Funds are held securely until 24 hours after check-in")
            Spacer(modifier = Modifier.height(8.dp))
            HelpStep(step = "3", text = "Your earnings are automatically transferred to your Stripe account")

            if (connectionState == PayoutConnectionState.NOT_CONNECTED) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Set up your Stripe account above to start receiving payouts.",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = PaceDreamColors.Primary
                )
            }
        }
    }
}

@Composable
private fun HelpStep(step: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(PaceDreamColors.Primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = PaceDreamColors.TextSecondary,
            fontSize = 13.sp
        )
    }
}
