package com.shourov.apps.pacedream.feature.host.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
                    val availableBalance = uiState.balance?.available?.firstOrNull()?.amount ?: 0
                    Button(
                        onClick = { viewModel.showPayoutSheet() },
                        enabled = availableBalance > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                        shape = RoundedCornerShape(PaceDreamRadius.Round),
                        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS),
                        modifier = Modifier.height(PaceDreamButtonHeight.SM)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.AttachMoney,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(PaceDreamIconSize.XS)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        Text(
                            text = "Payout",
                            style = PaceDreamTypography.Caption,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Selector
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = PaceDreamColors.Background,
                contentColor = PaceDreamColors.Primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                text = title,
                                style = PaceDreamTypography.Callout,
                                fontWeight = if (uiState.selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (uiState.selectedTab == index) PaceDreamColors.Primary else PaceDreamColors.TextSecondary
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

// ── Balance Tab ───────────────────────────────────────────────

@Composable
private fun BalanceTabContent(
    uiState: HostEarningsUiState,
    onPayoutClick: () -> Unit
) {
    val balance = uiState.balance

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        if (balance != null) {
            // Available Balance - hero card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
                    shape = RoundedCornerShape(PaceDreamRadius.XL)
                ) {
                    val availableAmount = balance.available.firstOrNull()?.amount ?: 0
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        PaceDreamColors.Primary.copy(alpha = 0.06f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(PaceDreamSpacing.LG),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Available Balance",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        Text(
                            text = formatAmount(availableAmount),
                            style = PaceDreamTypography.LargeTitle.copy(fontSize = 36.sp),
                            color = PaceDreamColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                        Text(
                            text = "Ready for payout",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextTertiary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        Button(
                            onClick = onPayoutClick,
                            enabled = availableAmount > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                            shape = RoundedCornerShape(PaceDreamRadius.Round),
                            modifier = Modifier.fillMaxWidth(0.6f),
                            contentPadding = PaddingValues(vertical = PaceDreamSpacing.SM)
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.AttachMoney,
                                contentDescription = null,
                                modifier = Modifier.size(PaceDreamIconSize.SM),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                            Text(
                                text = "Request Payout",
                                style = PaceDreamTypography.Callout,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Pending Balance
            val pendingAmount = balance.pending.firstOrNull()?.amount ?: 0
            if (pendingAmount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
                        shape = RoundedCornerShape(PaceDreamRadius.LG)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PaceDreamSpacing.MD),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PaceDreamColors.Warning.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = PaceDreamIcons.Schedule,
                                    contentDescription = null,
                                    tint = PaceDreamColors.Warning,
                                    modifier = Modifier.size(PaceDreamIconSize.SM)
                                )
                            }
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pending Balance",
                                    style = PaceDreamTypography.Callout,
                                    color = PaceDreamColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Available in 2-7 business days",
                                    style = PaceDreamTypography.Caption,
                                    color = PaceDreamColors.TextSecondary
                                )
                            }
                            Text(
                                text = formatAmount(pendingAmount),
                                style = PaceDreamTypography.Headline,
                                color = PaceDreamColors.Accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Balance Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
                    shape = RoundedCornerShape(PaceDreamRadius.LG)
                ) {
                    Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                        Text(
                            text = "Balance Breakdown",
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        balance.available.forEach { amount ->
                            BalanceBreakdownRow(
                                currency = amount.currency,
                                amount = amount.amount,
                                sourceTypes = amount.resolvedSourceTypes
                            )
                        }
                    }
                }
            }
        } else {
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
            .padding(vertical = PaceDreamSpacing.XS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = currency.uppercase(),
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatAmount(amount, currency),
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            sourceTypes.forEach { (source, value) ->
                Text(
                    text = "$source: $value",
                    style = PaceDreamTypography.Caption2,
                    color = PaceDreamColors.TextTertiary
                )
            }
        }
    }
}

// ── Transfers Tab ─────────────────────────────────────────────

@Composable
private fun TransfersTabContent(transfers: List<Transfer>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        if (transfers.isEmpty()) {
            item {
                EarningsEmptyState(
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
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Payment,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(PaceDreamIconSize.XS)
                )
            }
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transfer.description ?: "Transfer",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDate(transfer.createdAt),
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(transfer.amount, transfer.currency),
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                StatusBadge(status = transfer.status)
            }
        }
    }
}

// ── Payouts Tab ───────────────────────────────────────────────

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
                EarningsEmptyState(
                    icon = PaceDreamIcons.AttachMoney,
                    title = "No Payouts Yet",
                    subtitle = "Your payout history will appear here"
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                Button(
                    onClick = onRequestPayout,
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                    shape = RoundedCornerShape(PaceDreamRadius.Round),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = PaceDreamSpacing.SM)
                ) {
                    Text(
                        text = "Request Payout",
                        style = PaceDreamTypography.Callout,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
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
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Success.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.AttachMoney,
                    contentDescription = null,
                    tint = PaceDreamColors.Success,
                    modifier = Modifier.size(PaceDreamIconSize.XS)
                )
            }
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payout.description ?: "Payout",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Arrives: ${formatDate(payout.arrivalDate)}",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(payout.amount, payout.currency),
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                StatusBadge(status = payout.status)
            }
        }
    }
}

// ── Status Badge ──────────────────────────────────────────────

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
        style = PaceDreamTypography.Caption2.copy(fontWeight = FontWeight.SemiBold),
        color = badgeColor,
        modifier = Modifier
            .background(badgeColor.copy(alpha = 0.1f), RoundedCornerShape(PaceDreamRadius.Round))
            .padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS)
    )
}

// ── Empty State ───────────────────────────────────────────────

@Composable
private fun EarningsEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PaceDreamSpacing.XXXL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(PaceDreamColors.Primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(PaceDreamIconSize.LG)
            )
        }
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        Text(
            text = title,
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
        Text(
            text = subtitle,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ── Payout Bottom Sheet ───────────────────────────────────────

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
                .padding(horizontal = PaceDreamSpacing.LG)
                .padding(bottom = PaceDreamSpacing.XL)
        ) {
            Text(
                text = "Request Payout",
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = "Available: $availableBalanceText",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            // Quick Amount Chips
            Text(
                text = "Quick Amounts",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

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
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = PaceDreamSpacing.SM)
                            ) {
                                Text(
                                    text = "$$quickAmount",
                                    style = PaceDreamTypography.Callout,
                                    color = if (isSelected) Color.White else PaceDreamColors.Primary,
                                    fontWeight = FontWeight.SemiBold
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

            Text(
                text = "Custom Amount",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    selectedAmount = 0
                },
                placeholder = { Text("0.00", color = PaceDreamColors.TextTertiary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Border
                )
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

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
                    .height(PaceDreamButtonHeight.LG)
            ) {
                Text(
                    text = "Request Payout",
                    style = PaceDreamTypography.Button,
                    color = Color.White
                )
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
