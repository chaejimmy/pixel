package com.pacedream.app.feature.settings.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Notification settings screen matching iOS NotificationsSettingsView (iOS parity).
 *
 * Sections:
 * - General: email, push, SMS
 * - Messages: message notifications
 * - Bookings: booking updates, booking alerts
 * - Social: friend request, reviews
 * - System: system notifications
 * - Marketing: marketing & promotions
 * - Quiet Hours: enable/disable with time display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNotificationsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsNotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val message = uiState.errorMessage ?: uiState.successMessage
        if (!message.isNullOrBlank()) {
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── General Section ──────────────────────────
            SectionHeader("General")
            ModernToggleRow(
                title = "Email notifications",
                description = "Receive notifications via email",
                icon = Icons.Filled.Email,
                iconColor = MaterialTheme.colorScheme.primary,
                checked = uiState.emailGeneral,
                onCheckedChange = { viewModel.toggleEmailGeneral() }
            )
            ModernToggleRow(
                title = "Push notifications",
                description = "Receive push notifications on your device",
                icon = Icons.Filled.Notifications,
                iconColor = MaterialTheme.colorScheme.primary,
                checked = uiState.pushGeneral,
                onCheckedChange = { viewModel.togglePushGeneral() }
            )
            ModernToggleRow(
                title = "SMS notifications",
                description = "Receive important updates via text message",
                icon = Icons.Filled.Sms,
                iconColor = MaterialTheme.colorScheme.tertiary,
                checked = uiState.smsNotifications,
                onCheckedChange = { viewModel.toggleSmsNotifications() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Messages Section ─────────────────────────
            SectionHeader("Messages")
            ModernToggleRow(
                title = "Message notifications",
                description = "Get notified about new messages",
                icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notifications),
                iconColor = MaterialTheme.colorScheme.secondary,
                checked = uiState.messageNotifications,
                onCheckedChange = { viewModel.toggleMessageNotifications() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Bookings Section ─────────────────────────
            SectionHeader("Bookings")
            ModernToggleRow(
                title = "Booking updates",
                description = "Notifications about booking changes",
                icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notifications),
                iconColor = Color(0xFF4CAF50),
                checked = uiState.bookingUpdates,
                onCheckedChange = { viewModel.toggleBookingUpdates() }
            )
            ModernToggleRow(
                title = "Booking alerts",
                description = "Important alerts about your bookings",
                icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notification),
                iconColor = Color(0xFFFF9800),
                checked = uiState.bookingAlerts,
                onCheckedChange = { viewModel.toggleBookingAlerts() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Social Section (iOS parity) ──────────────
            SectionHeader("Social")
            ModernToggleRow(
                title = "Friend requests",
                description = "Notifications for friend and roommate requests",
                icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notifications),
                iconColor = Color(0xFF2196F3),
                checked = uiState.friendRequestNotifications,
                onCheckedChange = { viewModel.toggleFriendRequestNotifications() }
            )
            ModernToggleRow(
                title = "Reviews",
                description = "Get notified when you receive reviews",
                icon = Icons.Filled.Star,
                iconColor = Color(0xFFFFC107),
                checked = uiState.reviewNotifications,
                onCheckedChange = { viewModel.toggleReviewNotifications() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── System Section (iOS parity) ──────────────
            SectionHeader("System")
            ModernToggleRow(
                title = "System notifications",
                description = "Updates about maintenance and system changes",
                icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notification),
                iconColor = Color(0xFF9E9E9E),
                checked = uiState.systemNotifications,
                onCheckedChange = { viewModel.toggleSystemNotifications() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Marketing Section ────────────────────────
            SectionHeader("Marketing")
            ModernToggleRow(
                title = "Marketing & promotions",
                description = "Receive promotional offers and updates",
                icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notification),
                iconColor = Color(0xFFE91E63),
                checked = uiState.marketingPromotions,
                onCheckedChange = { viewModel.toggleMarketingPromotions() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Quiet Hours (iOS parity) ─────────────────
            SectionHeader("Quiet Hours")
            ModernToggleRow(
                title = "Quiet hours",
                description = if (uiState.quietHoursEnabled)
                    "${uiState.quietHoursStart} - ${uiState.quietHoursEnd}"
                else
                    "Mute notifications during set hours",
                icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notification),
                iconColor = Color(0xFF673AB7),
                checked = uiState.quietHoursEnabled,
                onCheckedChange = { viewModel.toggleQuietHours() }
            )

            // ── Save Button ──────────────────────────────
            Button(
                onClick = { viewModel.save() },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    "Save",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Section header matching iOS Form section headers.
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

/**
 * Modern toggle row matching iOS ModernToggleRow (iOS parity).
 *
 * Features an icon in a rounded colored background, title, description,
 * and a toggle switch — same layout as the iOS SwiftUI version.
 */
@Composable
private fun ModernToggleRow(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // iOS parity: icon in rounded colored background
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Toggle
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
