// @DesignSystemEscape (reason="legacy debt tracked in DESIGN_SYSTEM_COVERAGE.md — migrate per the suggested order in that file before removing this opt-out")
package com.pacedream.app.feature.settings.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.app.core.notifications.NotificationChannels
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.notification.isNotificationPermissionGranted
import com.shourov.apps.pacedream.notification.openAppNotificationSettings
import kotlinx.coroutines.launch

/**
 * Notification settings screen.
 *
 * Layout (top → bottom):
 * - Push notifications (this device): master "System notifications" row +
 *   one card per [NotificationChannels] entry. Read-only mirrors of the
 *   system state — tapping a row deep-links to system Settings.
 * - Email preferences: backend opt-ins for each category, prefixed
 *   "Email · …" so users see that push and email are separate controls.
 * - Other notifications: SMS + server-side "send pushes at all" toggle.
 * - Social, Product updates, Quiet Hours: remaining iOS-parity backend
 *   preferences.
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // The Android notification-channel state lives in system Settings, not in
    // the app. Re-read it on every ON_RESUME so that toggling the Bookings
    // channel from Settings → Apps → PaceDream → Notifications reflects back
    // into this screen on the next composition.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemNotificationState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val message = uiState.errorMessage ?: uiState.successMessage
        if (!message.isNullOrBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.consumeMessages()
            }
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

            // ── Device push notifications (Android NotificationChannels) ──
            // The state here is read from the system and cannot be mutated
            // from inside the app; tapping a row deep-links to the matching
            // system Settings page.
            SectionLabel("Push notifications · this device")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
                    SystemMasterRow(
                        enabled = uiState.systemMasterEnabled,
                        onClick = { NotificationChannels.openAppNotificationSettings(context) }
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
                    ChannelRow(
                        channel = NotificationChannels.BOOKINGS,
                        icon = Icons.Filled.EventAvailable,
                        iconColor = PaceDreamColors.Success,
                        enabled = uiState.systemMasterEnabled &&
                            (uiState.channelEnabled[NotificationChannels.BOOKINGS.id] ?: true),
                        onClick = {
                            NotificationChannels.openChannelSettings(context, NotificationChannels.BOOKINGS)
                        }
                    )
                    ChannelRow(
                        channel = NotificationChannels.MESSAGES,
                        icon = Icons.Filled.Message,
                        iconColor = PaceDreamColors.Secondary,
                        enabled = uiState.systemMasterEnabled &&
                            (uiState.channelEnabled[NotificationChannels.MESSAGES.id] ?: true),
                        onClick = {
                            NotificationChannels.openChannelSettings(context, NotificationChannels.MESSAGES)
                        }
                    )
                    ChannelRow(
                        channel = NotificationChannels.PAYMENTS,
                        icon = Icons.Filled.AttachMoney,
                        iconColor = PaceDreamColors.Primary,
                        enabled = uiState.systemMasterEnabled &&
                            (uiState.channelEnabled[NotificationChannels.PAYMENTS.id] ?: true),
                        onClick = {
                            NotificationChannels.openChannelSettings(context, NotificationChannels.PAYMENTS)
                        }
                    )
                    ChannelRow(
                        channel = NotificationChannels.HOST_UPDATES,
                        icon = Icons.Filled.Home,
                        iconColor = PaceDreamColors.Info,
                        enabled = uiState.systemMasterEnabled &&
                            (uiState.channelEnabled[NotificationChannels.HOST_UPDATES.id] ?: true),
                        onClick = {
                            NotificationChannels.openChannelSettings(context, NotificationChannels.HOST_UPDATES)
                        }
                    )
                    ChannelRow(
                        channel = NotificationChannels.MARKETING,
                        icon = Icons.Outlined.Campaign,
                        iconColor = PaceDreamColors.Accent,
                        enabled = uiState.systemMasterEnabled &&
                            (uiState.channelEnabled[NotificationChannels.MARKETING.id] ?: true),
                        onClick = {
                            NotificationChannels.openChannelSettings(context, NotificationChannels.MARKETING)
                        }
                    )
                }
            }

            // Helper text clarifying why these rows behave differently from
            // the email rows below — toggling them deep-links to the system.
            Text(
                text = "These categories are controlled by Android. " +
                    "Tap a row to open system settings, where you can change its importance or mute it independently.",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.XS)
            )

            // ── Backend (email/SMS) preferences ─────────────────────────────
            // These are server-side opt-ins that determine whether PaceDream
            // sends a given category at all, regardless of the device channels
            // above. They are clearly labelled "Email"/"SMS" so users see that
            // push and email are two separate controls.
            SectionLabel("Email preferences")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
                    ModernToggleRow(
                        title = "Email · general updates",
                        description = "Account, security, and product updates from PaceDream",
                        icon = Icons.Filled.Email,
                        iconColor = PaceDreamColors.Primary,
                        checked = uiState.emailGeneral,
                        onCheckedChange = { viewModel.toggleEmailGeneral() }
                    )
                    ModernToggleRow(
                        title = "Email · messages",
                        description = "Receive email when someone sends you a chat message",
                        icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notifications),
                        iconColor = PaceDreamColors.Secondary,
                        checked = uiState.messageNotifications,
                        onCheckedChange = { viewModel.toggleMessageNotifications() }
                    )
                    ModernToggleRow(
                        title = "Email · booking updates",
                        description = "Email about confirmations, changes, and check-in",
                        icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notifications),
                        iconColor = PaceDreamColors.Success,
                        checked = uiState.bookingUpdates,
                        onCheckedChange = { viewModel.toggleBookingUpdates() }
                    )
                    ModernToggleRow(
                        title = "Email · booking alerts",
                        description = "Email for urgent booking alerts",
                        icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notification),
                        iconColor = PaceDreamColors.Warning,
                        checked = uiState.bookingAlerts,
                        onCheckedChange = { viewModel.toggleBookingAlerts() }
                    )
                    ModernToggleRow(
                        title = "Email · marketing & promotions",
                        description = "Email about deals, offers, and partner promotions",
                        icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notification),
                        iconColor = PaceDreamColors.Accent,
                        checked = uiState.marketingPromotions,
                        onCheckedChange = { viewModel.toggleMarketingPromotions() }
                    )
                }
            }

            // ── Other channels: SMS + in-app extras ─────────────────────────
            SectionLabel("Other notifications")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
                    ModernToggleRow(
                        title = "SMS notifications",
                        description = "Receive important updates via text message",
                        icon = Icons.Filled.Sms,
                        iconColor = PaceDreamColors.Accent,
                        checked = uiState.smsNotifications,
                        onCheckedChange = { viewModel.toggleSmsNotifications() }
                    )
                    ModernToggleRow(
                        title = "Push · general (server)",
                        description = "Allow PaceDream's server to send pushes at all",
                        icon = Icons.Filled.Notifications,
                        iconColor = PaceDreamColors.Primary,
                        checked = uiState.pushGeneral,
                        onCheckedChange = { viewModel.togglePushGeneral() }
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

            // Product updates section (legacy field name: systemNotifications).
            // Renamed to avoid confusion with the master "System notifications"
            // row at the top, which reflects the OS-level toggle.
            SectionLabel("Product updates")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
                    SystemNotificationPermissionRow()
                    ModernToggleRow(
                        title = "Product & maintenance updates",
                        description = "Server-side notices about maintenance and changes",
                        icon = ImageVector.vectorResource(id = com.shourov.apps.pacedream.R.drawable.ic_notification),
                        iconColor = PaceDreamColors.Gray500,
                        checked = uiState.systemNotifications,
                        onCheckedChange = { viewModel.toggleSystemNotifications() }
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

/**
 * Master "System notifications" row showing whether the OS-level toggle
 * for the app is on, with a chevron that deep-links to the per-app
 * notification settings page.
 */
@Composable
private fun SystemMasterRow(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val iconColor = if (enabled) PaceDreamColors.Success else PaceDreamColors.Warning
    val statusLabel = if (enabled) "On in system settings" else "Off · tap to enable"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                imageVector = if (enabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "System notifications",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextPrimary
            )
            Text(
                text = statusLabel,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Open system settings",
            tint = PaceDreamColors.TextTertiary
        )
    }
}

/**
 * Surfaces the OS-level POST_NOTIFICATIONS state (Allowed / Blocked) with a
 * deep-link to the system "App notifications" page. The status chip is
 * refreshed when the screen returns to the foreground so that flipping the
 * toggle in Settings is reflected here without needing to re-enter the
 * screen.
 */
@Composable
private fun SystemNotificationPermissionRow() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAllowed by remember {
        mutableStateOf(isNotificationPermissionGranted(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAllowed = isNotificationPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.SM))
            .clickable { openAppNotificationSettings(context) }
            .padding(vertical = PaceDreamSpacing.SM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(PaceDreamRadius.SM))
                .background(PaceDreamColors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "System notification permission",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextPrimary
            )
            Text(
                text = if (isAllowed) {
                    "Tap to manage in Android Settings"
                } else {
                    "Blocked at the OS level — tap to open Settings"
                },
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }

        PermissionStatusChip(allowed = isAllowed)
    }
}

@Composable
private fun PermissionStatusChip(allowed: Boolean) {
    val container = if (allowed) {
        PaceDreamColors.Success.copy(alpha = 0.12f)
    } else {
        PaceDreamColors.Error.copy(alpha = 0.12f)
    }
    val content = if (allowed) PaceDreamColors.Success else PaceDreamColors.Error
    val label = if (allowed) "Allowed" else "Blocked"
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(PaceDreamRadius.Round))
            .background(container)
            .padding(
                horizontal = PaceDreamSpacing.SM,
                vertical = PaceDreamSpacing.XS
            )
    ) {
        Text(
            text = label,
            style = PaceDreamTypography.Caption,
            color = content
        )
    }
}

/**
 * Per-channel row. The switch reflects the channel's current importance
 * (read from the system) — it never mutates channel state from inside
 * the app. Tapping the row deep-links to the channel's system page where
 * the user can change its importance.
 */
@Composable
private fun ChannelRow(
    channel: NotificationChannels.Channel,
    icon: ImageVector,
    iconColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.displayName,
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.TextPrimary
            )
            Text(
                text = channel.description,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }
        // Reflect the live system state without claiming we can change it.
        // Tapping the switch is treated the same as tapping the row —
        // both deep-link to system settings.
        Switch(
            checked = enabled,
            onCheckedChange = { onClick() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = PaceDreamColors.Primary,
                checkedThumbColor = PaceDreamColors.OnPrimary
            )
        )
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
