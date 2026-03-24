package com.shourov.apps.pacedream.feature.host.presentation

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.data.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale

/**
 * HostEarningsScreen - iOS parity.
 *
 * Single-scroll view matching iOS HostEarningsView:
 * - Stripe connection status
 * - Balance cards (Available, Settling, Lifetime)
 * - Recent payouts
 * - Recent transfers
 * - How payouts work footer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostEarningsScreen(
    viewModel: HostEarningsViewModel = hiltViewModel()
) {
    val uiState by viewModel.earningsUiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Earnings & Payments",
                        style = PaceDreamTypography.Title1,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshData() },
                        enabled = !uiState.isRefreshing
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.History,
                            contentDescription = "Refresh",
                            tint = PaceDreamColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(PaceDreamSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val balance = uiState.balance

                // Stripe connection status
                uiState.connectAccount?.let { account ->
                    item {
                        StripeConnectionCard(account = account)
                    }
                }

                if (balance != null) {
                    // Balance cards — iOS: Available, Settling, Lifetime
                    item {
                        BalanceCardsSection(balance = balance)
                    }

                    // Recent payouts (inline, like iOS "Recent Bank Deposits")
                    if (uiState.payouts.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent Bank Deposits",
                                style = PaceDreamTypography.Headline.copy(fontSize = 17.sp),
                                color = PaceDreamColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = PaceDreamSpacing.SM)
                            )
                        }
                        items(
                            uiState.payouts.take(5),
                            key = { it.id }
                        ) { payout ->
                            PayoutRow(payout = payout)
                        }
                    }

                    // Recent transfers (inline, like iOS "Booking Earnings")
                    if (uiState.transfers.isNotEmpty()) {
                        item {
                            Text(
                                text = "Booking Earnings",
                                style = PaceDreamTypography.Headline.copy(fontSize = 17.sp),
                                color = PaceDreamColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = PaceDreamSpacing.SM)
                            )
                        }
                        items(
                            uiState.transfers.take(10),
                            key = { it.id }
                        ) { transfer ->
                            TransferRow(transfer = transfer)
                        }
                    }

                    // How Payouts Work footer — iOS: Help footer with 4-step flow
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        PayoutHelpFooter()
                    }
                } else if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PaceDreamColors.Primary)
                        }
                    }
                } else {
                    // Empty state
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = PaceDreamSpacing.XL),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.AttachMoney,
                                contentDescription = null,
                                tint = PaceDreamColors.TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No earnings yet",
                                style = PaceDreamTypography.Headline.copy(fontSize = 17.sp),
                                color = PaceDreamColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your earnings from bookings will appear here.",
                                style = PaceDreamTypography.Subheadline,
                                color = PaceDreamColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Stripe Connection Card ──────────────────────────────────
// iOS: Shows connection state with 3 variants (connected, pending, not connected)

@Composable
private fun StripeConnectionCard(account: ConnectAccount) {
    val isConnected = account.chargesEnabled && account.payoutsEnabled
    val isPending = !isConnected && (account.detailsSubmitted || account.requirements?.currentlyDue?.isNotEmpty() == true)

    val (statusText, statusColor, subtitle) = when {
        isConnected -> Triple("Connected", PaceDreamColors.Success, "Automatic payouts are active")
        isPending -> Triple("Action required", PaceDreamColors.Warning, "Complete your account setup to receive payouts")
        else -> Triple("Not connected", PaceDreamColors.TextSecondary, "Set up Stripe Connect to receive earnings")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.CreditCard,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stripe Connect",
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamColors.TextSecondary
                )
            }
            Text(
                text = statusText,
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Bold),
                color = statusColor,
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.14f), RoundedCornerShape(PaceDreamRadius.Round))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

// ── Balance Cards ───────────────────────────────────────────
// iOS: Three balance cards — Available Now, Settling on Stripe, Lifetime Earnings

@Composable
private fun BalanceCardsSection(balance: ConnectBalance) {
    val availableAmount = balance.available.firstOrNull()?.amount ?: 0
    val pendingAmount = balance.pending.firstOrNull()?.amount ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Available Now
        BalanceCard(
            label = "Available Now",
            amount = formatAmount(availableAmount),
            icon = PaceDreamIcons.CheckCircle,
            color = PaceDreamColors.Success
        )

        // Settling on Stripe
        BalanceCard(
            label = "Settling on Stripe",
            amount = formatAmount(pendingAmount),
            icon = PaceDreamIcons.Schedule,
            color = PaceDreamColors.Warning,
            subtitle = if (pendingAmount > 0) "Available in 2-7 business days" else null
        )

        // Lifetime Earnings
        BalanceCard(
            label = "Lifetime Earnings",
            amount = formatAmount(availableAmount + pendingAmount),
            icon = PaceDreamIcons.AttachMoney,
            color = PaceDreamColors.Primary
        )
    }
}

@Composable
private fun BalanceCard(
    label: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    subtitle: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = PaceDreamTypography.Footnote,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
            Text(
                text = amount,
                style = PaceDreamTypography.Headline,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Payout Row ──────────────────────────────────────────────
// iOS: payoutRow with status icon, destination last 4, date

@Composable
private fun PayoutRow(payout: Payout) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Success.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.AttachMoney,
                    contentDescription = null,
                    tint = PaceDreamColors.Success,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payout.description ?: "Payout",
                    style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp),
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDate(payout.arrivalDate),
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(payout.amount, payout.currency),
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = payout.status)
            }
        }
    }
}

// ── Transfer Row ────────────────────────────────────────────
// iOS: transactionRow with type, status label, amounts

@Composable
private fun TransferRow(transfer: Transfer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Payment,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transfer.description ?: "Transfer",
                    style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp),
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDate(transfer.createdAt),
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(transfer.amount, transfer.currency),
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = transfer.status)
            }
        }
    }
}

// ── Status Badge ──────────────────────────────────────────────
// iOS: 12pt bold, capsule, 10h/6v padding, semantic color bg at 14%

@Composable
private fun StatusBadge(status: String) {
    val badgeColor = when (status.lowercase()) {
        "succeeded", "paid" -> PaceDreamColors.Success
        "pending", "processing" -> PaceDreamColors.Warning
        "failed", "canceled" -> PaceDreamColors.Error
        else -> PaceDreamColors.TextSecondary
    }

    Text(
        text = status.replaceFirstChar { it.uppercase() },
        style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Bold),
        color = badgeColor,
        modifier = Modifier
            .background(badgeColor.copy(alpha = 0.14f), RoundedCornerShape(PaceDreamRadius.Round))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

// ── Payout Help Footer ──────────────────────────────────────
// iOS: Help footer with 4-step payout flow explanation

@Composable
private fun PayoutHelpFooter() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "How Payouts Work",
                style = PaceDreamTypography.Headline.copy(fontSize = 17.sp),
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            val steps = listOf(
                "Guest completes booking" to "Payment is captured by Stripe",
                "Settlement period" to "Funds settle in your Stripe Connect account (2-7 days)",
                "Available for payout" to "Funds move to your available balance",
                "Bank deposit" to "Automatic daily rolling payout to your bank account"
            )

            steps.forEachIndexed { index, (title, description) ->
                Row(modifier = Modifier.padding(vertical = PaceDreamSpacing.XS)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(PaceDreamColors.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = PaceDreamTypography.Caption,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (index < steps.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(24.dp)
                                    .background(PaceDreamColors.Divider)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp),
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = description,
                            style = PaceDreamTypography.Footnote,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────

private fun formatAmount(amountInCents: Int, currency: String = "usd"): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    try {
        formatter.currency = Currency.getInstance(currency.uppercase())
    } catch (_: Exception) { }
    return formatter.format(amountInCents / 100.0)
}

private fun formatDate(dateString: String): String {
    if (dateString.isBlank()) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (_: Exception) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (_: Exception) {
            dateString
        }
    }
}
