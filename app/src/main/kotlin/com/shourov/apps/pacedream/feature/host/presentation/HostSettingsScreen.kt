package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*

private data class SettingsSection(
    val title: String,
    val items: List<SettingsItem>
)

private data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val tint: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostSettingsScreen(
    onBackClick: () -> Unit = {},
    onPaymentSetupClick: () -> Unit = {},
    onEarningsClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onListingsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onHelpClick: () -> Unit = {}
) {
    val sections = remember {
        listOf(
            SettingsSection(
                title = "Management",
                items = listOf(
                    SettingsItem(
                        icon = PaceDreamIcons.CalendarToday,
                        title = "Bookings",
                        subtitle = "Manage your bookings",
                        tint = PaceDreamColors.Info,
                        onClick = onBookingsClick
                    ),
                    SettingsItem(
                        icon = PaceDreamIcons.Home,
                        title = "Space",
                        subtitle = "Manage your listings and spaces",
                        tint = PaceDreamColors.Primary,
                        onClick = onListingsClick
                    ),
                    SettingsItem(
                        icon = PaceDreamIcons.Analytics,
                        title = "Business",
                        subtitle = "View earnings, transfers, and payouts",
                        tint = PaceDreamColors.Success,
                        onClick = onEarningsClick
                    )
                )
            ),
            SettingsSection(
                title = "Payments",
                items = listOf(
                    SettingsItem(
                        icon = PaceDreamIcons.CreditCard,
                        title = "Payment Setup",
                        subtitle = "Set up Stripe Connect to receive payments",
                        tint = PaceDreamColors.Warning,
                        onClick = onPaymentSetupClick
                    )
                )
            ),
            SettingsSection(
                title = "Preferences",
                items = listOf(
                    SettingsItem(
                        icon = PaceDreamIcons.Notifications,
                        title = "Notifications",
                        subtitle = "Manage notification preferences",
                        tint = PaceDreamColors.Error,
                        onClick = onNotificationsClick
                    ),
                    SettingsItem(
                        icon = PaceDreamIcons.Help,
                        title = "Help & Support",
                        subtitle = "Get help with hosting",
                        tint = PaceDreamColors.TextSecondary,
                        onClick = onHelpClick
                    )
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings",
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary
                        )
                        Text(
                            text = "Host preferences",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = PaceDreamSpacing.MD,
                end = PaceDreamSpacing.MD,
                top = PaceDreamSpacing.SM,
                bottom = PaceDreamSpacing.XXL
            ),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            sections.forEach { section ->
                item {
                    Text(
                        text = section.title,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextTertiary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            start = PaceDreamSpacing.XS,
                            bottom = PaceDreamSpacing.XS
                        )
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
                    ) {
                        Column {
                            section.items.forEachIndexed { index, item ->
                                SettingsRow(
                                    icon = item.icon,
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    tint = item.tint,
                                    onClick = item.onClick
                                )
                                if (index < section.items.size - 1) {
                                    HorizontalDivider(
                                        color = PaceDreamColors.Border,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 56.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
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
