package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.layout.*
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.presentation.components.*

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
    val sections = listOf(
            SettingsSection(
                title = "Management",
                items = listOf(
                    SettingsItem(
                        icon = PaceDreamIcons.CalendarToday,
                        title = "Bookings",
                        subtitle = "Manage your bookings",
                        tint = PaceDreamColors.HostAccent,
                        onClick = onBookingsClick
                    ),
                    SettingsItem(
                        icon = PaceDreamIcons.Home,
                        title = "Space",
                        subtitle = "Manage your listings and spaces",
                        tint = PaceDreamColors.HostAccent,
                        onClick = onListingsClick
                    ),
                    SettingsItem(
                        icon = PaceDreamIcons.Analytics,
                        title = "Business",
                        subtitle = "View earnings, transfers, and payouts",
                        tint = PaceDreamColors.HostAccent,
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
                        tint = PaceDreamColors.HostAccent,
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
                        tint = PaceDreamColors.TextSecondary,
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
        androidx.compose.foundation.lazy.LazyColumn(
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
                    HostSettingsSectionHeader(title = section.title)

                    HostGroupedCard {
                        section.items.forEachIndexed { index, item ->
                            HostSettingsRow(
                                icon = item.icon,
                                title = item.title,
                                subtitle = item.subtitle,
                                tint = item.tint,
                                onClick = item.onClick
                            )
                            if (index < section.items.size - 1) {
                                HostRowDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
