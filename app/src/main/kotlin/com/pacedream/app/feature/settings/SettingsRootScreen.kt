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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * SettingsRootScreen
 *
 * Root Settings screen with grouped sections:
 * - Account: Personal Information, Login & Security
 * - Notifications
 * - Preferences: Language & Region, Payment Methods
 * - Privacy & Sharing
 * - Support: Help & Support
 * - App version footer
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            // Account section
            SectionLabel("Account")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    SettingsRow(
                        icon = PaceDreamIcons.Person,
                        title = "Personal Information",
                        subtitle = "Name, email, and phone",
                        onClick = onPersonalInfoClick
                    )
                    HorizontalDivider(
                        color = PaceDreamColors.Border,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                    )

                    SettingsRow(
                        icon = PaceDreamIcons.Lock,
                        title = "Login & Security",
                        subtitle = "Password and authentication",
                        onClick = onLoginSecurityClick
                    )
                }
            }

            // Notifications section
            SectionLabel("Notifications")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsRow(
                    icon = PaceDreamIcons.Notifications,
                    title = "Notifications",
                    subtitle = "Push, email, and SMS preferences",
                    onClick = onNotificationsClick
                )
            }

            // Preferences section
            SectionLabel("Preferences")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    SettingsRow(
                        icon = PaceDreamIcons.Language,
                        title = "Language & Region",
                        subtitle = "Language, currency, and timezone",
                        onClick = onPreferencesClick
                    )
                    HorizontalDivider(
                        color = PaceDreamColors.Border,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                    )

                    SettingsRow(
                        icon = PaceDreamIcons.CreditCard,
                        title = "Payment Methods",
                        subtitle = "Saved cards and billing",
                        onClick = onPaymentMethodsClick
                    )
                }
            }

            // Support section
            SectionLabel("Support")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsRow(
                    icon = PaceDreamIcons.HelpOutline,
                    title = "Help & Support",
                    subtitle = "FAQs, contact, and feedback",
                    onClick = onHelpSupportClick
                )
            }

            // App version footer
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = "PaceDream v${getAppVersion()}",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PaceDreamSpacing.XL)
            )
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = PaceDreamTypography.Caption,
        color = PaceDreamColors.TextTertiary,
        modifier = Modifier.padding(start = PaceDreamSpacing.XS)
    )
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
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun getAppVersion(): String {
    return try {
        com.shourov.apps.pacedream.BuildConfig.VERSION_NAME
    } catch (_: Exception) {
        "1.0.0"
    }
}
