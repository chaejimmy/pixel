package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.data.HostEarningsData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostEarningsScreen(
    onWithdrawClick: () -> Unit = {},
    onTransactionClick: (String) -> Unit = {},
    viewModel: HostEarningsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Earnings Header
        item {
            EarningsHeader(
                totalEarnings = uiState.totalEarnings,
                availableBalance = uiState.availableBalance,
                onWithdrawClick = onWithdrawClick
            )
        }
        
        // Time Range Selector
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            TimeRangeSelector(
                selectedRange = uiState.selectedTimeRange,
                onRangeChanged = { viewModel.updateTimeRange(it) }
            )
        }
        
        // Earnings Chart
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            EarningsChart(
                earningsData = uiState.earningsData,
                timeRange = uiState.selectedTimeRange
            )
        }
        
        // Earnings Breakdown
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            EarningsBreakdownSection(
                breakdown = uiState.earningsBreakdown
            )
        }
        
        // Recent Transactions
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            RecentTransactionsSection(
                transactions = uiState.recentTransactions,
                onTransactionClick = onTransactionClick
            )
        }
    }
}

@Composable
fun EarningsHeader(
    totalEarnings: Double,
    availableBalance: Double,
    onWithdrawClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PaceDreamColors.Success,
                        PaceDreamColors.Success.copy(alpha = 0.9f)
                    )
                )
            )
            .padding(PaceDreamSpacing.LG)
    ) {
        Column {
            Text(
                text = "Earnings Overview",
                style = PaceDreamTypography.Title1,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Track your hosting income",
                style = PaceDreamTypography.Body,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Earnings",
                        style = PaceDreamTypography.Caption,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        text = "$${String.format("%.2f", totalEarnings)}",
                        style = PaceDreamTypography.Title1,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Available Balance",
                        style = PaceDreamTypography.Caption,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        text = "$${String.format("%.2f", availableBalance)}",
                        style = PaceDreamTypography.Title2,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            Button(
                onClick = onWithdrawClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Withdraw Funds",
                    color = PaceDreamColors.Success,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun TimeRangeSelector(
    selectedRange: String,
    onRangeChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Time Period",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        val timeRanges = listOf("Week", "Month", "Quarter", "Year", "All Time")
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(timeRanges) { range ->
                FilterChip(
                    selected = selectedRange == range,
                    onClick = { onRangeChanged(range) },
                    label = {
                        Text(
                            text = range,
                            style = PaceDreamTypography.Callout,
                            fontWeight = if (selectedRange == range) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PaceDreamColors.Primary,
                        selectedLabelColor = Color.White,
                        containerColor = PaceDreamColors.Card,
                        labelColor = PaceDreamColors.TextPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun EarningsChart(
    earningsData: List<Pair<String, Double>>,
    timeRange: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG)
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.LG)
        ) {
            Text(
                text = "Earnings Trend",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            Text(
                text = "Last $timeRange",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            // Simple bar chart representation
            if (earningsData.isNotEmpty()) {
                val maxEarnings = earningsData.maxOfOrNull { it.second } ?: 0.0
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    earningsData.forEach { (period, amount) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val barHeight = if (maxEarnings > 0) {
                                (amount / maxEarnings * 100).dp.coerceAtLeast(4.dp)
                            } else {
                                4.dp
                            }
                            
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(PaceDreamRadius.SM))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                PaceDreamColors.Primary,
                                                PaceDreamColors.Primary.copy(alpha = 0.7f)
                                            )
                                        )
                                    )
                            )
                            
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                            
                            Text(
                                text = period,
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary
                            )
                            
                            Text(
                                text = "$${String.format("%.0f", amount)}",
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No earnings data available",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun EarningsBreakdownSection(
    breakdown: List<com.shourov.apps.pacedream.feature.host.data.EarningsBreakdownItem>
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Earnings Breakdown",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(PaceDreamSpacing.MD)
            ) {
                breakdown.forEach { item ->
                    EarningsBreakdownRow(
                        category = item.category,
                        amount = item.amount,
                        percentage = item.percentage
                    )
                    
                    if (item != breakdown.last()) {
                        Divider(
                            color = PaceDreamColors.Border,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = PaceDreamSpacing.SM)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EarningsBreakdownRow(
    category: String,
    amount: Double,
    percentage: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = category,
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "${String.format("%.1f", percentage)}%",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }
        
        Text(
            text = "$${String.format("%.2f", amount)}",
            style = PaceDreamTypography.Headline,
            color = PaceDreamColors.Primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RecentTransactionsSection(
    transactions: List<com.shourov.apps.pacedream.feature.host.data.Transaction>,
    onTransactionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Recent Transactions",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        if (transactions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card)
            ) {
                Column(
                    modifier = Modifier.padding(PaceDreamSpacing.LG),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Receipt,
                        contentDescription = "No transactions",
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    
                    Text(
                        text = "No transactions yet",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
        } else {
            transactions.take(5).forEach { transaction ->
                TransactionCard(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction.id) }
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }
        }
    }
}

@Composable
fun TransactionCard(
    transaction: com.shourov.apps.pacedream.feature.host.data.Transaction,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.MD)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (transaction.type) {
                            com.shourov.apps.pacedream.feature.host.data.TransactionType.BOOKING -> PaceDreamColors.Success.copy(alpha = 0.1f)
                            com.shourov.apps.pacedream.feature.host.data.TransactionType.WITHDRAWAL -> PaceDreamColors.Primary.copy(alpha = 0.1f)
                            com.shourov.apps.pacedream.feature.host.data.TransactionType.FEE -> PaceDreamColors.Warning.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (transaction.type) {
                        com.shourov.apps.pacedream.feature.host.data.TransactionType.BOOKING -> PaceDreamIcons.CalendarToday
                        com.shourov.apps.pacedream.feature.host.data.TransactionType.WITHDRAWAL -> PaceDreamIcons.AttachMoney
                        com.shourov.apps.pacedream.feature.host.data.TransactionType.FEE -> PaceDreamIcons.Receipt
                    },
                    contentDescription = transaction.type.name,
                    tint = when (transaction.type) {
                        com.shourov.apps.pacedream.feature.host.data.TransactionType.BOOKING -> PaceDreamColors.Success
                        com.shourov.apps.pacedream.feature.host.data.TransactionType.WITHDRAWAL -> PaceDreamColors.Primary
                        com.shourov.apps.pacedream.feature.host.data.TransactionType.FEE -> PaceDreamColors.Warning
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.description,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = transaction.date,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
            
            Text(
                text = "$${String.format("%.2f", transaction.amount)}",
                style = PaceDreamTypography.Headline,
                color = if (transaction.amount > 0) PaceDreamColors.Success else PaceDreamColors.Error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
