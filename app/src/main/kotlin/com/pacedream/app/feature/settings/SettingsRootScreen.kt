package com.pacedream.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * SettingsRootScreen
 *
 * Root Settings screen with sections:
 * - Personal Information
 * - Login & Security
 * - Notifications
 * - Preferences (Language & Region)
 * - Payment Methods
 * - Help & Support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRootScreen(
    onBackClick: () -> Unit,
    onPersonalInfoClick: () -> Unit,
    onLoginSecurityClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onPaymentMethodsClick: () -> Unit,
    onHelpSupportClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = PaceDreamTypography.Title2
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    SettingsRow(
                        icon = PaceDreamIcons.Person,
                        title = "Personal Information",
                        subtitle = "Name, email, and phone",
                        onClick = onPersonalInfoClick
                    )
                    HorizontalDivider(color = PaceDreamColors.Border)

                    SettingsRow(
                        icon = PaceDreamIcons.Lock,
                        title = "Login & Security",
                        subtitle = "Password and authentication",
                        onClick = onLoginSecurityClick
                    )
                    HorizontalDivider(color = PaceDreamColors.Border)

                    SettingsRow(
                        icon = PaceDreamIcons.Notifications,
                        title = "Notifications",
                        subtitle = "Push and email preferences",
                        onClick = onNotificationsClick
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    SettingsRow(
                        icon = PaceDreamIcons.Tune,
                        title = "Preferences",
                        subtitle = "Language and region",
                        onClick = onPreferencesClick
                    )
                    HorizontalDivider(color = PaceDreamColors.Border)

                    SettingsRow(
                        icon = PaceDreamIcons.CreditCard,
                        title = "Payment Methods",
                        subtitle = "Saved cards and billing",
                        onClick = onPaymentMethodsClick
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingsRow(
                    icon = PaceDreamIcons.HelpOutline,
                    title = "Help & Support",
                    subtitle = "FAQs, contact, and feedback",
                    onClick = onHelpSupportClick
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = PaceDreamColors.Primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
        }

        Icon(
            imageVector = PaceDreamIcons.ArrowForward,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}
