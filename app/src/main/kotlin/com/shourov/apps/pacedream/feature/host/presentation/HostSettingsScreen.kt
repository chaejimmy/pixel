package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostSettingsScreen(
    onBackClick: () -> Unit = {},
    onPaymentSetupClick: () -> Unit = {},
    onEarningsClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onListingsClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PaceDreamColors.Background)
                .padding(horizontal = PaceDreamSpacing.MD)
        ) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Bookings Section (matches iOS HostSettingView)
            SettingsRow(
                icon = PaceDreamIcons.CalendarToday,
                title = "Bookings",
                subtitle = "Manage your bookings",
                onClick = onBookingsClick
            )

            HorizontalDivider(color = PaceDreamColors.Border, modifier = Modifier.padding(vertical = PaceDreamSpacing.SM))

            // Listings / Space Section (matches iOS "Space" item)
            SettingsRow(
                icon = PaceDreamIcons.Home,
                title = "Space",
                subtitle = "Manage your listings and spaces",
                onClick = onListingsClick
            )

            HorizontalDivider(color = PaceDreamColors.Border, modifier = Modifier.padding(vertical = PaceDreamSpacing.SM))

            // Business / Earnings Section (matches iOS "Business" nav item)
            SettingsRow(
                icon = PaceDreamIcons.Analytics,
                title = "Business",
                subtitle = "View earnings, transfers, and payouts",
                onClick = onEarningsClick
            )

            HorizontalDivider(color = PaceDreamColors.Border, modifier = Modifier.padding(vertical = PaceDreamSpacing.SM))

            // Payment Setup (Stripe Connect) - matches iOS StripeConnectOnboardingView
            SettingsRow(
                icon = PaceDreamIcons.CreditCard,
                title = "Payment Setup",
                subtitle = "Set up Stripe Connect to receive payments",
                onClick = onPaymentSetupClick
            )

            HorizontalDivider(color = PaceDreamColors.Border, modifier = Modifier.padding(vertical = PaceDreamSpacing.SM))

            // Notification preferences
            SettingsRow(
                icon = PaceDreamIcons.Notifications,
                title = "Notifications",
                subtitle = "Manage notification preferences",
                onClick = { /* TODO */ }
            )

            HorizontalDivider(color = PaceDreamColors.Border, modifier = Modifier.padding(vertical = PaceDreamSpacing.SM))

            // Help & Support
            SettingsRow(
                icon = PaceDreamIcons.Help,
                title = "Help & Support",
                subtitle = "Get help with hosting",
                onClick = { /* TODO */ }
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = PaceDreamSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PaceDreamColors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Medium
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
            modifier = Modifier.size(20.dp)
        )
    }
}
