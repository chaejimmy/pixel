package com.pacedream.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            SettingsRow(
                icon = Icons.Filled.Person,
                title = "Personal Information",
                onClick = onPersonalInfoClick
            )
            Divider()

            SettingsRow(
                icon = Icons.Filled.Lock,
                title = "Login & Security",
                onClick = onLoginSecurityClick
            )
            Divider()

            SettingsRow(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                onClick = onNotificationsClick
            )
            Divider()

            SettingsRow(
                icon = Icons.Filled.Tune,
                title = "Preferences",
                onClick = onPreferencesClick
            )
            Divider()

            SettingsRow(
                icon = Icons.Filled.CreditCard,
                title = "Payment Methods",
                onClick = onPaymentMethodsClick
            )
            Divider()

            SettingsRow(
                icon = Icons.Filled.HelpOutline,
                title = "Help & Support",
                onClick = onHelpSupportClick
            )
            Divider()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(4.dp))
}

