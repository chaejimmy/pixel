package com.pacedream.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * SettingsRootScreen - iOS parity
 *
 * Matches iOS SettingsHomeView.swift structure:
 * - Single main section: Personal Information, Login & Security, Notifications,
 *   Preferences, Identity Verification, Payment Methods
 * - Separate Help & Support section
 * - Log out button (red, destructive) with confirmation
 * - Icon background containers with colored rounded rect
 * - No section labels (flat structure like iOS)
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
    onHelpSupportClick: () -> Unit,
    onIdentityVerificationClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

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

            // Main settings section (iOS parity: all 6 items in one card)
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    SettingsRow(
                        icon = PaceDreamIcons.Person,
                        iconColor = PaceDreamColors.Success,
                        title = "Personal Information",
                        onClick = onPersonalInfoClick
                    )
                    SettingsDivider()

                    SettingsRow(
                        icon = PaceDreamIcons.Lock,
                        iconColor = PaceDreamColors.Success,
                        title = "Login & Security",
                        onClick = onLoginSecurityClick
                    )
                    SettingsDivider()

                    SettingsRow(
                        icon = PaceDreamIcons.Notifications,
                        iconColor = PaceDreamColors.Success,
                        title = "Notifications",
                        onClick = onNotificationsClick
                    )
                    SettingsDivider()

                    SettingsRow(
                        icon = PaceDreamIcons.Tune,
                        iconColor = PaceDreamColors.Success,
                        title = "Preferences",
                        onClick = onPreferencesClick
                    )
                    SettingsDivider()

                    SettingsRow(
                        icon = PaceDreamIcons.VerifiedUser,
                        iconColor = PaceDreamColors.Success,
                        title = "Identity Verification",
                        onClick = onIdentityVerificationClick
                    )
                    SettingsDivider()

                    SettingsRow(
                        icon = PaceDreamIcons.CreditCard,
                        iconColor = PaceDreamColors.Success,
                        title = "Payment Methods",
                        onClick = onPaymentMethodsClick
                    )
                }
            }

            // Help & Support section (iOS parity: separate card)
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsRow(
                    icon = PaceDreamIcons.HelpOutline,
                    iconColor = PaceDreamColors.Success,
                    title = "Help & Support",
                    onClick = onHelpSupportClick
                )
            }

            // Log out button (iOS parity: red destructive button in separate card)
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutConfirm = true }
                        .padding(horizontal = PaceDreamSpacing.MD, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.ExitToApp,
                        contentDescription = null,
                        tint = PaceDreamColors.Error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Log out",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.Error
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        }
    }

    // Logout confirmation dialog (iOS parity: "Log out?" / "You can sign back in anytime.")
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = {
                Text(
                    "Log out?",
                    style = PaceDreamTypography.Title3
                )
            },
            text = {
                Text(
                    "You can sign back in anytime.",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Error
                    ),
                    shape = RoundedCornerShape(PaceDreamRadius.MD)
                ) {
                    Text("Log out", style = PaceDreamTypography.Button)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirm = false }
                ) {
                    Text(
                        "Cancel",
                        color = PaceDreamColors.TextPrimary
                    )
                }
            },
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            containerColor = PaceDreamColors.Card
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = PaceDreamColors.Border,
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
    )
}

/**
 * SettingsRow with icon background container (iOS parity)
 *
 * iOS uses a colored rounded rect behind each icon:
 * ZStack { RoundedRectangle.fill(iconColor.opacity(0.15)) ; Image }
 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: Color = PaceDreamColors.Primary,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with colored background container (iOS parity)
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    iconColor.copy(alpha = 0.15f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}
