package com.shourov.apps.pacedream.feature.host.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
 * Host Earnings Screen - iOS parity.
 *
 * Matches iOS HostEarningsView with Stripe Connect integration:
 * - Tabbed layout: Balance / Transfers / Payouts
 * - Stripe account status and onboarding
 * - Payout request bottom sheet
 * - Pull-to-refresh
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostEarningsScreen(
    viewModel: HostEarningsViewModel = hiltViewModel()
) {
    val uiState by viewModel.earningsUiState.collectAsStateWithLifecycle()
    val tabs = listOf("Balance", "Transfers", "Payouts")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Earnings",
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary
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
                    // Payout button in toolbar like iOS
                    val availableBalance = uiState.balance?.available?.firstOrNull()?.amount ?: 0
                    Button(
                        onClick = { viewModel.showPayoutSheet() },
                        enabled = availableBalance > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                        shape = RoundedCornerShape(PaceDreamRadius.Round),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Payout",
                            style = PaceDreamTypography.Caption,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PaceDreamColors.Background)
        ) {
            // Tab Selector (matching iOS SegmentedControl)
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = PaceDreamColors.Background,
                contentColor = PaceDreamColors.Primary,
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                text = title,
                                style = PaceDreamTypography.Callout,
                                fontWeight = if (uiState.selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Content based on selected tab
            when (uiState.selectedTab) {
                0 -> BalanceTabContent(uiState = uiState, onPayoutClick = { viewModel.showPayoutSheet() })
                1 -> TransfersTabContent(transfers = uiState.transfers)
                2 -> PayoutsTabContent(payouts = uiState.payouts, onRequestPayout = { viewModel.showPayoutSheet() })
            }
        }

        // Payout Request Bottom Sheet
        if (uiState.showPayoutSheet) {
            PayoutRequestBottomSheet(
                balance = uiState.balance,
                onDismiss = { viewModel.hidePayoutSheet() },
                onPayoutRequested = { amount -> viewModel.requestPayout(amount) }
            )
        }
    }
}

// ============================================================================
// Balance Tab - matches iOS balanceView
// ============================================================================
@Composable
private fun BalanceTabContent(
    uiState: HostEarningsUiState,
    onPayoutClick: () -> Unit
) {
    val balance = uiState.balance

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.LG)
    ) {
        if (balance != null) {
            // Available Balance Card
            item {
                val availableAmount = balance.available.firstOrNull()?.amount ?: 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(PaceDreamRadius.MD)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaceDreamSpacing.MD),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Available Balance",
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        Text(
                            text = formatAmount(availableAmount),
                            style = PaceDreamTypography.LargeTitle.copy(fontSize = 32.sp),
                            color = PaceDreamColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                        Text(
                            text = "Ready for payout",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                }
            }

            // Pending Balance Card
            val pendingAmount = balance.pending.firstOrNull()?.amount ?: 0
            if (pendingAmount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(PaceDreamRadius.MD)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PaceDreamSpacing.MD),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Pending Balance",
                                style = PaceDreamTypography.Headline,
                                color = PaceDreamColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                            Text(
                                text = formatAmount(pendingAmount),
                                style = PaceDreamTypography.Title2,
                                color = PaceDreamColors.Accent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                            Text(
                                text = "Will be available in 2-7 business days",
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // Balance Breakdown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(PaceDreamRadius.MD)
                ) {
                    Column(
                        modifier = Modifier.padding(PaceDreamSpacing.MD)
                    ) {
                        Text(
                            text = "Balance Breakdown",
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        balance.available.forEach { amount ->
                            BalanceBreakdownRow(
                                currency = amount.currency,
                                amount = amount.amount,
                                sourceTypes = amount.sourceTypes
                            )
                        }
                    }
                }
            }

            // Quick Actions Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(PaceDreamRadius.MD)
                ) {
                    Column(
                        modifier = Modifier.padding(PaceDreamSpacing.MD)
                    ) {
                        Text(
                            text = "Quick Actions",
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                        val availableBalance = balance.available.firstOrNull()?.amount ?: 0
                        Button(
                            onClick = onPayoutClick,
                            enabled = availableBalance > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                            shape = RoundedCornerShape(PaceDreamRadius.MD),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.AttachMoney,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                            Text(
                                text = "Request Payout",
                                style = PaceDreamTypography.Caption,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            // Loading state
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
        }
    }
}

@Composable
private fun BalanceBreakdownRow(
    currency: String,
    amount: Int,
    sourceTypes: Map<String, Int>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PaceDreamSpacing.SM),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = currency.uppercase(),
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
            Text(
                text = formatAmount(amount, currency),
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextPrimary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Sources",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
            sourceTypes.forEach { (source, value) ->
                Text(
                    text = "$source: $value",
                    style = PaceDreamTypography.Caption2,
                    color = PaceDreamColors.TextSecondary
                )
            }
        }
    }
}

// ============================================================================
// Transfers Tab - matches iOS transfersView
// ============================================================================
@Composable
private fun TransfersTabContent(transfers: List<Transfer>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        if (transfers.isEmpty()) {
            item {
                EmptyStateView(
                    icon = PaceDreamIcons.Payment,
                    title = "No Transfers Yet",
                    subtitle = "Transfers from bookings will appear here"
                )
            }
        } else {
            items(transfers, key = { it.id }) { transfer ->
                TransferRow(transfer = transfer)
            }
        }
    }
}

@Composable
private fun TransferRow(transfer: Transfer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transfer.description ?: "Transfer",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary
                )
                transfer.bookingId?.let { bookingId ->
                    Text(
                        text = "Booking: $bookingId",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
                Text(
                    text = formatDate(transfer.createdAt),
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(transfer.amount, transfer.currency),
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
                StatusBadge(status = transfer.status)
            }
        }
    }
}

// ============================================================================
// Payouts Tab - matches iOS payoutsView
// ============================================================================
@Composable
private fun PayoutsTabContent(
    payouts: List<Payout>,
    onRequestPayout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        if (payouts.isEmpty()) {
            item {
                EmptyStateView(
                    icon = PaceDreamIcons.AttachMoney,
                    title = "No Payouts Yet",
                    subtitle = "Your payout history will appear here"
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                Button(
                    onClick = onRequestPayout,
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                    shape = RoundedCornerShape(PaceDreamRadius.Round),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Request Payout",
                        style = PaceDreamTypography.Caption,
                        color = Color.White
                    )
                }
            }
        } else {
            items(payouts, key = { it.id }) { payout ->
                PayoutRow(payout = payout)
            }
        }
    }
}

@Composable
private fun PayoutRow(payout: Payout) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Payout",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary
                )
                payout.description?.let { desc ->
                    Text(
                        text = desc,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
                Text(
                    text = "Arrives: ${formatDate(payout.arrivalDate)}",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(payout.amount, payout.currency),
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
                StatusBadge(status = payout.status)
            }
        }
    }
}

// ============================================================================
// Status Badge - matches iOS StatusBadge
// ============================================================================
@Composable
private fun StatusBadge(status: String) {
    val backgroundColor = when (status.lowercase()) {
        "succeeded", "paid" -> PaceDreamColors.Success
        "pending", "processing" -> PaceDreamColors.Warning
        "failed", "canceled" -> PaceDreamColors.Error
        else -> PaceDreamColors.TextSecondary
    }

    Text(
        text = status.replaceFirstChar { it.uppercase() },
        style = PaceDreamTypography.Caption2.copy(fontWeight = FontWeight.Medium),
        color = Color.White,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(PaceDreamRadius.Round))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// ============================================================================
// Empty State View
// ============================================================================
@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PaceDreamColors.TextSecondary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        Text(
            text = title,
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = subtitle,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================================
// Payout Request Bottom Sheet - matches iOS PayoutRequestSheet
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayoutRequestBottomSheet(
    balance: ConnectBalance?,
    onDismiss: () -> Unit,
    onPayoutRequested: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedAmount by remember { mutableIntStateOf(0) }
    val quickAmounts = listOf(25, 50, 100, 250, 500)

    val availableBalance = balance?.available?.firstOrNull()?.amount ?: 0
    val availableBalanceText = formatAmount(availableBalance)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PaceDreamColors.Background,
        shape = RoundedCornerShape(topStart = PaceDreamRadius.XL, topEnd = PaceDreamRadius.XL)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.LG)
        ) {
            // Header
            Text(
                text = "Request Payout",
                style = PaceDreamTypography.Title1,
                color = PaceDreamColors.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = "Available Balance: $availableBalanceText",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            // Quick Amount Buttons
            Text(
                text = "Quick Amounts",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            // Grid of quick amounts (3 columns like iOS)
            val columns = 3
            val rows = (quickAmounts.size + columns - 1) / columns
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        if (index < quickAmounts.size) {
                            val quickAmount = quickAmounts[index]
                            val isSelected = selectedAmount == quickAmount
                            Button(
                                onClick = {
                                    amount = quickAmount.toString()
                                    selectedAmount = quickAmount
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.Surface
                                ),
                                shape = RoundedCornerShape(PaceDreamRadius.SM),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$$quickAmount",
                                    style = PaceDreamTypography.Caption,
                                    color = if (isSelected) Color.White else PaceDreamColors.Primary
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (row < rows - 1) {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            // Custom Amount
            Text(
                text = "Custom Amount",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    selectedAmount = 0
                },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.SM)
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            // Request Payout Button
            val parsedAmount = amount.toDoubleOrNull()
            Button(
                onClick = {
                    parsedAmount?.let { onPayoutRequested(it) }
                },
                enabled = parsedAmount != null && parsedAmount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Request Payout",
                    style = PaceDreamTypography.Button,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
        }
    }
}

// ============================================================================
// Helpers
// ============================================================================
private fun formatAmount(amountInCents: Int, currency: String = "usd"): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    try {
        formatter.currency = Currency.getInstance(currency.uppercase())
    } catch (_: Exception) {
        // fallback to USD
    }
    return formatter.format(amountInCents / 100.0)
}

private fun formatDate(dateString: String): String {
    if (dateString.isBlank()) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val outputFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)
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
