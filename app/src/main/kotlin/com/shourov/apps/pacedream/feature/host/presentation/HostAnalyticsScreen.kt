package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.data.HostDashboardData

/**
 * HostAnalyticsScreen - iOS parity.
 *
 * Matches iOS HostAnalyticsView: KPI overview (2x2 grid), listings breakdown,
 * bookings breakdown, and earnings section. All data is backend-driven via
 * HostDashboardViewModel (shared data source, same as iOS HostDataStore).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostAnalyticsScreen(
    onBackClick: () -> Unit,
    viewModel: HostDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics",
                        style = PaceDreamTypography.Title1,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowBack,
                            contentDescription = "Back",
                            tint = PaceDreamColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.bookings.isEmpty() && uiState.listings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PaceDreamColors.Primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = PaceDreamSpacing.XXL)
                ) {
                    // Error banner
                    uiState.error?.let { error ->
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                                shape = RoundedCornerShape(PaceDreamRadius.MD),
                                colors = CardDefaults.cardColors(
                                    containerColor = PaceDreamColors.Error.copy(alpha = 0.08f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(PaceDreamSpacing.MD),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = PaceDreamIcons.Warning,
                                        contentDescription = null,
                                        tint = PaceDreamColors.Error,
                                        modifier = Modifier.size(PaceDreamIconSize.SM)
                                    )
                                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                                    Text(
                                        text = error,
                                        style = PaceDreamTypography.Caption,
                                        color = PaceDreamColors.Error,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // KPI Overview (2x2 grid) - matches iOS HostAnalyticsView
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                        ) {
                            AnalyticsKpiCard(
                                label = "Active Listings",
                                value = uiState.activeListingsCount.toString(),
                                icon = PaceDreamIcons.CheckCircle,
                                tint = PaceDreamColors.Success,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsKpiCard(
                                label = "Upcoming Bookings",
                                value = uiState.upcomingBookingsCount.toString(),
                                icon = PaceDreamIcons.CalendarToday,
                                tint = PaceDreamColors.Info,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.MD)
                                .padding(bottom = PaceDreamSpacing.SM),
                            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                        ) {
                            AnalyticsKpiCard(
                                label = "Pending Requests",
                                value = uiState.pendingRequestsCount.toString(),
                                icon = PaceDreamIcons.Schedule,
                                tint = PaceDreamColors.Warning,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsKpiCard(
                                label = "Booked This Month",
                                value = formatCurrency(uiState.monthlyEarnings),
                                icon = PaceDreamIcons.AttachMoney,
                                tint = PaceDreamColors.Primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Listings Breakdown - matches iOS
                    item {
                        BreakdownCard(
                            title = "Listings Breakdown",
                            items = buildListingsBreakdown(uiState)
                        )
                    }

                    // Bookings Breakdown - matches iOS
                    item {
                        BreakdownCard(
                            title = "Bookings Breakdown",
                            items = buildBookingsBreakdown(uiState)
                        )
                    }

                    // Earnings Section - matches iOS
                    item {
                        EarningsSummaryCard(uiState)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsKpiCard(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = value,
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

@Composable
private fun BreakdownCard(
    title: String,
    items: List<BreakdownItem>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Text(
                text = title,
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = PaceDreamSpacing.XS),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = item.tint,
                            modifier = Modifier.size(PaceDreamIconSize.SM)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                        Text(
                            text = item.label,
                            style = if (item.isBold) PaceDreamTypography.Body else PaceDreamTypography.Callout,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Text(
                        text = item.value,
                        style = if (item.isBold) PaceDreamTypography.Body else PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.SemiBold
                    )
                }

                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = PaceDreamSpacing.XS),
                        color = PaceDreamColors.Border
                    )
                }
            }
        }
    }
}

@Composable
private fun EarningsSummaryCard(uiState: HostDashboardData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Text(
                text = "Earnings",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Booked this month (prominent)
            Text(
                text = "Booked this month",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
            Text(
                text = formatCurrency(uiState.monthlyEarnings),
                style = PaceDreamTypography.LargeTitle,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            HorizontalDivider(color = PaceDreamColors.Border)
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Lifetime earnings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Lifetime Earnings",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextSecondary
                )
                Text(
                    text = formatCurrency(uiState.totalRevenue),
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            // Total bookings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Bookings",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextSecondary
                )
                Text(
                    text = uiState.totalBookings.toString(),
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Data helpers ──────────────────────────────────────────────

private data class BreakdownItem(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val tint: Color,
    val isBold: Boolean = false
)

private fun buildListingsBreakdown(uiState: HostDashboardData): List<BreakdownItem> {
    val active = uiState.activeListingsCount
    val inactive = uiState.listings.count { !it.isAvailable }
    val total = uiState.listings.size

    return listOf(
        BreakdownItem("Active", active.toString(), PaceDreamIcons.CheckCircle, PaceDreamColors.Success),
        BreakdownItem("Inactive", inactive.toString(), PaceDreamIcons.VisibilityOff, PaceDreamColors.TextSecondary),
        BreakdownItem("Total", total.toString(), PaceDreamIcons.Home, PaceDreamColors.TextPrimary, isBold = true)
    )
}

private fun buildBookingsBreakdown(uiState: HostDashboardData): List<BreakdownItem> {
    val upcoming = uiState.upcomingBookingsCount
    val pending = uiState.pendingRequestsCount
    val completed = uiState.bookings.count { booking ->
        val s = (booking.status ?: "").lowercase()
        s.contains("complet") || s.contains("finish") || s.contains("past")
    }
    val total = uiState.bookings.size

    return listOf(
        BreakdownItem("Upcoming", upcoming.toString(), PaceDreamIcons.CalendarToday, PaceDreamColors.Info),
        BreakdownItem("Pending Requests", pending.toString(), PaceDreamIcons.Schedule, PaceDreamColors.Warning),
        BreakdownItem("Completed", completed.toString(), PaceDreamIcons.CheckCircle, PaceDreamColors.Success),
        BreakdownItem("Total", total.toString(), PaceDreamIcons.CalendarToday, PaceDreamColors.TextPrimary, isBold = true)
    )
}

private fun formatCurrency(amount: Double): String {
    return if (amount == 0.0) "$0" else "$${String.format("%,.0f", amount)}"
}
