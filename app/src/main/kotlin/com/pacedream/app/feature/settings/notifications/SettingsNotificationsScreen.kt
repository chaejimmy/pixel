package com.pacedream.app.feature.settings.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
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
                        style = PaceDreamTypography.Headline
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

            // General Section
            SectionLabel("General")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
                    ModernToggleRow(
                        title = "Email notifications",
                        description = "Receive notifications via email",
                        icon = Icons.Filled.Email,
                        iconColor = PaceDreamColors.Primary,
                        checked = uiState.emailGeneral,
                        onCheckedChange = { viewModel.toggleEmailGeneral() }
                    )
                    ModernToggleRow(
                        title = "Push notifications",
                        description = "Receive push notifications on your device",
                        icon = Icons.Filled.Notifications,
                        iconColor = PaceDreamColors.Primary,
                        checked = uiState.pushGeneral,
                        onCheckedChange = { viewModel.togglePushGeneral() }
                    )
                    ModernToggleRow(
                        title = "SMS notifications",
                        description = "Receive important updates via text message",
                        icon = Icons.Filled.Sms,
                        iconColor = PaceDreamColors.Accent,
                        checked = uiState.smsNotifications,
                        onCheckedChange = { viewModel.toggleSmsNotifications() }
                    )
                }
            }

            // Messages Section
            SectionLabel("Messages")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
                    ModernToggleRow(
                        title = "Message notifications",
                        description = "Get notified about new messages",
                        icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notifications),
                        iconColor = PaceDreamColors.Secondary,
                        checked = uiState.messageNotifications,
                        onCheckedChange = { viewModel.toggleMessageNotifications() }
                    )
                }
            }

            // Bookings Section
            SectionLabel("Bookings")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
                    ModernToggleRow(
                        title = "Booking updates",
                        description = "Notifications about booking changes",
                        icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notifications),
                        iconColor = PaceDreamColors.Success,
                        checked = uiState.bookingUpdates,
                        onCheckedChange = { viewModel.toggleBookingUpdates() }
                    )
                    ModernToggleRow(
                        title = "Booking alerts",
                        description = "Important alerts about your bookings",
                        icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notification),
                        iconColor = PaceDreamColors.Warning,
                        checked = uiState.bookingAlerts,
                        onCheckedChange = { viewModel.toggleBookingAlerts() }
                    )
                }
            }

            // Social Section
            SectionLabel("Social")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
                    ModernToggleRow(
                        title = "Friend requests",
                        description = "Notifications for friend requests",
                        icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notifications),
                        iconColor = PaceDreamColors.Info,
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
                }
            }

            // System Section
            SectionLabel("System")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
                    ModernToggleRow(
                        title = "System notifications",
                        description = "Updates about maintenance and system changes",
                        icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notification),
                        iconColor = PaceDreamColors.Gray500,
                        checked = uiState.systemNotifications,
                        onCheckedChange = { viewModel.toggleSystemNotifications() }
                    )
                }
            }

            // Marketing Section
            SectionLabel("Marketing")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
                    ModernToggleRow(
                        title = "Marketing & promotions",
                        description = "Receive promotional offers and updates",
                        icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notification),
                        iconColor = Color(0xFFE91E63),
                        checked = uiState.marketingPromotions,
                        onCheckedChange = { viewModel.toggleMarketingPromotions() }
                    )
                }
            }

            // Quiet Hours Section
            SectionLabel("Quiet Hours")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
                    ModernToggleRow(
                        title = "Quiet hours",
                        description = if (uiState.quietHoursEnabled)
                            "${uiState.quietHoursStart} - ${uiState.quietHoursEnd}"
                        else
                            "Mute notifications during set hours",
                        icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notification),
                        iconColor = PaceDreamColors.Accent,
                        checked = uiState.quietHoursEnabled,
                        onCheckedChange = { viewModel.toggleQuietHours() }
                    )
                }
            }

            // Auto-save status indicator
            if (uiState.isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = PaceDreamSpacing.SM),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                        color = PaceDreamColors.Primary
                    )
                    Spacer(modifier = Modifier.size(PaceDreamSpacing.SM))
                    Text(
                        "Saving...",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            } else if (uiState.successMessage != null) {
                Text(
                    text = "Settings saved",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.Success,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = PaceDreamSpacing.SM),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
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
            .padding(vertical = PaceDreamSpacing.SM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(PaceDreamRadius.SM))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextPrimary
            )
            Text(
                text = description,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = PaceDreamColors.Primary,
                checkedThumbColor = PaceDreamColors.OnPrimary
            )
        )
    }
}
