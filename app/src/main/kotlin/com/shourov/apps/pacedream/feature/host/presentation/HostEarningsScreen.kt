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
                    val availableBalance = uiState.dashboard?.balances?.available ?: 0.0
                    Button(
                        onClick = { viewModel.showPayoutSheet() },
                        enabled = availableBalance > 0 && uiState.connectionState == EarningsConnectionState.CONNECTED,
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                        shape = RoundedCornerShape(PaceDreamRadius.Round),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.height(PaceDreamButtonHeight.SM)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.AttachMoney,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(PaceDreamIconSize.SM)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        Text(
                            text = "Payout",
                            style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
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
            // Error banner
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Error.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(PaceDreamRadius.MD)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Info,
                            contentDescription = null,
                            tint = PaceDreamColors.Error,
                            modifier = Modifier.size(PaceDreamIconSize.SM)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        Text(
                            text = error,
                            style = PaceDreamTypography.Footnote,
                            color = PaceDreamColors.Error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", style = PaceDreamTypography.Caption, color = PaceDreamColors.Error)
                        }
                    }
                }
            }

            // Loading state — only show on initial load
            if (uiState.isLoading && !uiState.hasLoaded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PaceDreamColors.Primary)
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        Text(
                            text = "Loading earnings...",
                            style = PaceDreamTypography.Subheadline,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                }
                return@Scaffold
            }

            // Not connected to Stripe
            if (uiState.hasLoaded && uiState.connectionState == EarningsConnectionState.NOT_CONNECTED) {
                NotConnectedContent(
                    stripe = uiState.dashboard?.stripe,
                    onSetupClick = { /* handled by StripeConnectOnboarding nav */ }
                )
                return@Scaffold
            }

            // Pending Stripe setup
            if (uiState.hasLoaded && uiState.connectionState == EarningsConnectionState.PENDING) {
                PendingSetupContent(stripe = uiState.dashboard?.stripe)
                return@Scaffold
            }

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
                                style = PaceDreamTypography.Subheadline,
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
                1 -> TransfersTabContent(transactions = uiState.dashboard?.transactions ?: emptyList())
                2 -> PayoutsTabContent(payouts = uiState.dashboard?.payouts ?: emptyList(), onRequestPayout = { viewModel.showPayoutSheet() })
            }
        }

        // Payout Request Bottom Sheet
        if (uiState.showPayoutSheet) {
            PayoutRequestBottomSheet(
                availableAmount = uiState.dashboard?.balances?.available ?: 0.0,
                currency = uiState.dashboard?.balances?.currency ?: "usd",
                onDismiss = { viewModel.hidePayoutSheet() },
                onPayoutRequested = { amount -> viewModel.requestPayout(amount) }
            )
        }
    }
}

// ── Not Connected State ──────────────────────────────────────

@Composable
private fun NotConnectedContent(stripe: DashboardStripeStatus?, onSetupClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(PaceDreamSpacing.LG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(PaceDreamIcons.AttachMoney, null, tint = PaceDreamColors.TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        Text("Connect a payout account", style = PaceDreamTypography.Title2, color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text("Set up Stripe to receive payouts from your bookings. You'll need to provide basic business information.", style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        Button(onClick = onSetupClick, colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary), shape = RoundedCornerShape(PaceDreamRadius.Round), modifier = Modifier.fillMaxWidth(0.7f), contentPadding = PaddingValues(vertical = 14.dp)) {
            Text("Set Up Payouts", style = PaceDreamTypography.Button, color = Color.White)
        }
    }
}

// ── Pending Setup State ──────────────────────────────────────

@Composable
private fun PendingSetupContent(stripe: DashboardStripeStatus?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(PaceDreamSpacing.LG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(PaceDreamIcons.Schedule, null, tint = PaceDreamColors.Warning, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        Text("Stripe setup incomplete", style = PaceDreamTypography.Title2, color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text("Your Stripe account is being reviewed or has pending requirements. Complete the setup to start receiving payouts.", style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextSecondary, textAlign = TextAlign.Center)

        val requirements = stripe?.requirements ?: emptyList()
        if (requirements.isNotEmpty()) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Warning.copy(alpha = 0.08f)), shape = RoundedCornerShape(PaceDreamRadius.MD)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Requirements", style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    requirements.forEach { req -> Text("• ${req.replace("_", " ")}", style = PaceDreamTypography.Footnote, color = PaceDreamColors.TextSecondary) }
                }
            }
        }

        stripe?.disabledReason?.let { reason ->
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text("Reason: ${reason.replace("_", " ")}", style = PaceDreamTypography.Footnote, color = PaceDreamColors.Warning, textAlign = TextAlign.Center)
        }
    }
}

// ── Balance Tab ───────────────────────────────────────────────

@Composable
private fun BalanceTabContent(uiState: HostEarningsUiState, onPayoutClick: () -> Unit) {
    val balances = uiState.dashboard?.balances

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (balances != null) {
            // Available Balance - hero card
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card), elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS), shape = RoundedCornerShape(PaceDreamRadius.XL)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(PaceDreamSpacing.LG), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Available Balance", style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextSecondary)
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        Text(formatDollarAmount(balances.available, balances.currency), style = PaceDreamTypography.LargeTitle.copy(fontSize = 36.sp), color = PaceDreamColors.Primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                        Text("Ready for payout", style = PaceDreamTypography.Footnote, color = PaceDreamColors.TextTertiary)
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        Button(onClick = onPayoutClick, enabled = balances.available > 0, colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary), shape = RoundedCornerShape(PaceDreamRadius.Round), modifier = Modifier.fillMaxWidth(0.6f), contentPadding = PaddingValues(vertical = 12.dp)) {
                            Icon(PaceDreamIcons.AttachMoney, null, modifier = Modifier.size(PaceDreamIconSize.SM), tint = Color.White)
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                            Text("Request Payout", style = PaceDreamTypography.Subheadline, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Pending Balance
            if (balances.pending > 0) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card), elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS), shape = RoundedCornerShape(PaceDreamRadius.LG)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PaceDreamColors.Warning.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(PaceDreamIcons.Schedule, null, tint = PaceDreamColors.Warning, modifier = Modifier.size(PaceDreamIconSize.SM))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pending Balance", style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Available in 2-7 business days", style = PaceDreamTypography.Footnote, color = PaceDreamColors.TextSecondary)
                            }
                            Text(formatDollarAmount(balances.pending, balances.currency), style = PaceDreamTypography.Headline, color = PaceDreamColors.Accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Settlement Status Card
            item { SettlementStatusCard(balances = balances) }

            // Payout Timeline
            item { PayoutTimelineCard() }
        } else if (uiState.hasLoaded) {
            item { EarningsEmptyState(PaceDreamIcons.AttachMoney, "No balance data", uiState.errorMessage ?: "Pull to refresh to try again.") }
        } else {
            item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PaceDreamColors.Primary) } }
        }
    }
}

// ── Transfers Tab ─────────────────────────────────────────────

@Composable
private fun TransfersTabContent(transactions: List<DashboardTransaction>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(PaceDreamSpacing.MD), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (transactions.isEmpty()) {
            item { EarningsEmptyState(PaceDreamIcons.Payment, "No transfers yet", "Transfers from bookings will appear here.") }
        } else {
            items(transactions, key = { it.id }) { TransactionRow(it) }
        }
    }
}

@Composable
private fun TransactionRow(transaction: DashboardTransaction) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card), elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS), shape = RoundedCornerShape(PaceDreamRadius.LG)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(PaceDreamColors.Primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(PaceDreamIcons.Payment, null, tint = PaceDreamColors.Primary, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.description ?: transaction.bookingType ?: "Booking earning", style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp), color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.Medium)
                Text(formatDate(transaction.createdAt ?: ""), style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary, fontWeight = FontWeight.Medium)
                if (transaction.grossAmount > 0) {
                    Text("Gross: ${formatDollarAmount(transaction.grossAmount)} • Fee: ${formatDollarAmount(transaction.stripeProcessingFee)}", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatDollarAmount(transaction.amount, transaction.currency), style = PaceDreamTypography.Footnote, color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = transaction.payoutStatus ?: transaction.status ?: "")
            }
        }
    }
}

// ── Payouts Tab ───────────────────────────────────────────────

@Composable
private fun PayoutsTabContent(payouts: List<DashboardPayout>, onRequestPayout: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(PaceDreamSpacing.MD), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (payouts.isEmpty()) {
            item {
                EarningsEmptyState(PaceDreamIcons.AttachMoney, "No payouts yet", "Your payout history will appear here.")
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                Button(onClick = onRequestPayout, colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary), shape = RoundedCornerShape(PaceDreamRadius.Round), modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) {
                    Text("Request Payout", style = PaceDreamTypography.Subheadline, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            items(payouts, key = { it.id }) { PayoutRow(it) }
        }
    }
}

@Composable
private fun PayoutRow(payout: DashboardPayout) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card), elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS), shape = RoundedCornerShape(PaceDreamRadius.LG)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(PaceDreamColors.Success.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(PaceDreamIcons.AttachMoney, null, tint = PaceDreamColors.Success, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(payout.description ?: "Bank payout", style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp), color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.Medium)
                val arrivalText = payout.arrivalDate?.let { "Arrives: ${formatDate(it)}" } ?: formatDate(payout.createdAt ?: "")
                Text(arrivalText, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary, fontWeight = FontWeight.Medium)
                payout.destination?.let { dest ->
                    val bankInfo = listOfNotNull(dest.bankName, dest.last4?.let { "•••• $it" }).joinToString(" ")
                    if (bankInfo.isNotEmpty()) Text(bankInfo, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatDollarAmount(payout.amount, payout.currency), style = PaceDreamTypography.Footnote, color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = payout.status)
            }
        }
    }
}

// ── Status Badge ──────────────────────────────────────────────

@Composable
private fun StatusBadge(status: String) {
    val badgeColor = when (status.lowercase()) {
        "succeeded", "paid", "transferred", "paid_out" -> PaceDreamColors.Success
        "pending", "processing", "pending_settlement", "in_transit" -> PaceDreamColors.Warning
        "held", "ready_for_transfer" -> PaceDreamColors.Primary
        "failed", "canceled", "blocked", "clawed_back" -> PaceDreamColors.Error
        else -> PaceDreamColors.TextSecondary
    }
    Text(
        text = status.replace("_", " ").replaceFirstChar { it.uppercase() },
        style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Bold),
        color = badgeColor,
        modifier = Modifier.background(badgeColor.copy(alpha = 0.14f), RoundedCornerShape(PaceDreamRadius.Round)).padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

// ── Empty State ───────────────────────────────────────────────

@Composable
private fun EarningsEmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = PaceDreamSpacing.XL), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = PaceDreamColors.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, style = PaceDreamTypography.Headline.copy(fontSize = 17.sp), color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextSecondary, textAlign = TextAlign.Center)
    }
}

// ── Payout Bottom Sheet ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayoutRequestBottomSheet(availableAmount: Double, currency: String, onDismiss: () -> Unit, onPayoutRequested: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var selectedAmount by remember { mutableIntStateOf(0) }
    val quickAmounts = listOf(25, 50, 100, 250, 500)
    val availableBalanceText = formatDollarAmount(availableAmount, currency)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PaceDreamColors.Background, shape = RoundedCornerShape(topStart = PaceDreamRadius.XL, topEnd = PaceDreamRadius.XL)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = PaceDreamSpacing.LG).padding(bottom = PaceDreamSpacing.XL)) {
            Text("Request Payout", style = PaceDreamTypography.Title2, color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text("Available: $availableBalanceText", style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            Text("Quick Amounts", style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            val columns = 3
            val rows = (quickAmounts.size + columns - 1) / columns
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        if (index < quickAmounts.size) {
                            val quickAmount = quickAmounts[index]
                            val isSelected = selectedAmount == quickAmount
                            Button(onClick = { amount = quickAmount.toString(); selectedAmount = quickAmount }, colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.Surface), shape = RoundedCornerShape(PaceDreamRadius.SM), modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 12.dp)) {
                                Text("$$quickAmount", style = PaceDreamTypography.Subheadline, color = if (isSelected) Color.White else PaceDreamColors.Primary, fontWeight = FontWeight.SemiBold)
                            }
                        } else { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                if (row < rows - 1) Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            Text("Custom Amount", style = PaceDreamTypography.Subheadline, color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            OutlinedTextField(value = amount, onValueChange = { amount = it; selectedAmount = 0 }, placeholder = { Text("0.00", color = PaceDreamColors.TextTertiary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(PaceDreamRadius.MD), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PaceDreamColors.Primary, unfocusedBorderColor = PaceDreamColors.Border))
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            val parsedAmount = amount.toDoubleOrNull()
            Button(onClick = { parsedAmount?.let { onPayoutRequested(it) } }, enabled = parsedAmount != null && parsedAmount > 0, colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary), shape = RoundedCornerShape(PaceDreamRadius.MD), modifier = Modifier.fillMaxWidth().height(PaceDreamButtonHeight.LG)) {
                Text("Request Payout", style = PaceDreamTypography.Button, color = Color.White)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────

private fun formatDollarAmount(amount: Double, currency: String = "usd"): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    try { formatter.currency = Currency.getInstance(currency.uppercase()) } catch (_: Exception) { }
    return formatter.format(amount)
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
        } catch (_: Exception) { dateString }
    }
}

// ── Settlement Status Card ────────────────────────────────────

@Composable
private fun SettlementStatusCard(balances: DashboardBalances) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card), elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS), shape = RoundedCornerShape(PaceDreamRadius.LG)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Payout Settlement Status", style = PaceDreamTypography.Headline.copy(fontSize = 17.sp), color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SettlementColumn("Available Now", formatDollarAmount(balances.available, balances.currency), PaceDreamColors.Success)
                SettlementColumn("Settling on Stripe", formatDollarAmount(balances.settling, balances.currency), PaceDreamColors.Warning)
                SettlementColumn("Lifetime Earnings", formatDollarAmount(balances.lifetime, balances.currency), PaceDreamColors.Primary)
            }
            if (balances.fundsSettling) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PaceDreamColors.Warning.copy(alpha = 0.08f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(PaceDreamIcons.Info, null, tint = PaceDreamColors.Warning, modifier = Modifier.size(PaceDreamIconSize.SM))
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text(balances.settlingNote ?: "Stripe holds funds for 2-7 business days after a booking completes before they become available for payout.", style = PaceDreamTypography.Footnote, color = PaceDreamColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun SettlementColumn(label: String, amount: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(amount, style = PaceDreamTypography.Headline, color = color, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary, textAlign = TextAlign.Center)
    }
}

// ── Payout Timeline ───────────────────────────────────────────

@Composable
private fun PayoutTimelineCard() {
    val steps = listOf(
        "Guest completes booking" to "Payment is captured by Stripe",
        "Settlement period" to "Funds settle in your Stripe Connect account (2-7 days)",
        "Available for payout" to "Funds move to your available balance",
        "Bank deposit" to "Automatic daily rolling payout to your bank account"
    )
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card), elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS), shape = RoundedCornerShape(PaceDreamRadius.LG)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("How Payouts Work", style = PaceDreamTypography.Headline.copy(fontSize = 17.sp), color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(14.dp))
            steps.forEachIndexed { index, (title, description) ->
                Row(modifier = Modifier.padding(vertical = PaceDreamSpacing.XS)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(PaceDreamColors.Primary), contentAlignment = Alignment.Center) {
                            Text("${index + 1}", style = PaceDreamTypography.Caption, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        if (index < steps.size - 1) Box(modifier = Modifier.width(2.dp).height(24.dp).background(PaceDreamColors.Divider))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(title, style = PaceDreamTypography.Subheadline.copy(fontSize = 14.sp), color = PaceDreamColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(description, style = PaceDreamTypography.Footnote, color = PaceDreamColors.TextSecondary)
                    }
                }
            }
        }
    }
}
