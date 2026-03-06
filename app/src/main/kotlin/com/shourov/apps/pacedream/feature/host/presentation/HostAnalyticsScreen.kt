package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostAnalyticsScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Analytics",
                            style = PaceDreamTypography.Title1,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track your performance",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = PaceDreamSpacing.XXL)
        ) {
            // Overview KPI cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    AnalyticsKpiCard(
                        label = "Total Views",
                        value = "--",
                        icon = PaceDreamIcons.Visibility,
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsKpiCard(
                        label = "Bookings",
                        value = "--",
                        icon = PaceDreamIcons.CalendarToday,
                        tint = PaceDreamColors.Success,
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
                        label = "Revenue",
                        value = "--",
                        icon = PaceDreamIcons.AttachMoney,
                        tint = PaceDreamColors.Warning,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsKpiCard(
                        label = "Rating",
                        value = "--",
                        icon = PaceDreamIcons.Star,
                        tint = PaceDreamColors.Info,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Performance chart placeholder
            item {
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
                            text = "Performance",
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(PaceDreamRadius.MD))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            PaceDreamColors.Primary.copy(alpha = 0.06f),
                                            PaceDreamColors.Primary.copy(alpha = 0.02f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = PaceDreamIcons.Analytics,
                                    contentDescription = null,
                                    tint = PaceDreamColors.Primary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(PaceDreamIconSize.XL)
                                )
                                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                                Text(
                                    text = "Chart coming soon",
                                    style = PaceDreamTypography.Caption,
                                    color = PaceDreamColors.TextTertiary
                                )
                            }
                        }
                    }
                }
            }

            // Insights section
            item {
                Column(
                    modifier = Modifier.padding(
                        horizontal = PaceDreamSpacing.MD,
                        vertical = PaceDreamSpacing.SM
                    )
                ) {
                    Text(
                        text = "Insights",
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                    InsightRow(
                        icon = PaceDreamIcons.TrendingUp,
                        title = "Listing Performance",
                        subtitle = "See how your listings compare",
                        tint = PaceDreamColors.Success
                    )
                    InsightRow(
                        icon = PaceDreamIcons.People,
                        title = "Guest Demographics",
                        subtitle = "Understand your audience",
                        tint = PaceDreamColors.Info
                    )
                    InsightRow(
                        icon = PaceDreamIcons.CalendarToday,
                        title = "Booking Trends",
                        subtitle = "Peak days and seasonal patterns",
                        tint = PaceDreamColors.Warning
                    )
                }
            }

            // Coming soon banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(
                        containerColor = PaceDreamColors.Primary.copy(alpha = 0.06f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaceDreamSpacing.XL),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.Analytics,
                                contentDescription = null,
                                tint = PaceDreamColors.Primary,
                                modifier = Modifier.size(PaceDreamIconSize.LG)
                            )
                        }
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        Text(
                            text = "Full Analytics Coming Soon",
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                        Text(
                            text = "We're building detailed analytics and insights to help you optimize your listings and maximize earnings.",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
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
private fun InsightRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PaceDreamSpacing.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.TextTertiary,
                modifier = Modifier.size(PaceDreamIconSize.XS)
            )
        }
    }
}
