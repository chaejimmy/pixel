package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.presentation.components.*

/**
 * Host Inbox Screen — simplified messaging hub.
 *
 * Single "Messages" title with a segmented control for Messages vs Notifications.
 * The Messages tab receives its content via [messagesContent] (typically the guest
 * InboxScreen thread list). The Notifications tab shows host-specific notifications.
 *
 * Previous issues fixed:
 * - "Inbox" / "Messages" title appeared twice (once from this screen, once from embedded InboxScreen)
 * - HostSegmentedControl + embedded InboxScreen TopAppBar created triple-layered header
 * - Now uses a single TopAppBar with the segmented control directly below it
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostInboxScreen(
    onThreadClick: (String) -> Unit = {},
    messagesContent: @Composable () -> Unit = { MessagesEmptyPlaceholder() },
    notificationsContent: @Composable () -> Unit = { NotificationsEmptyPlaceholder() }
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // ── Single header: title + segmented control ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = PaceDreamSpacing.MD)
                .padding(top = PaceDreamSpacing.MD)
        ) {
            Text(
                text = "Messages",
                style = PaceDreamTypography.Title1.copy(fontWeight = FontWeight.Bold),
                color = PaceDreamColors.TextPrimary
            )
        }

        // Segmented control — Messages / Notifications
        HostSegmentedControl(
            tabs = listOf("Messages", "Notifications"),
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // ── Tab content ──
        when (selectedTab) {
            0 -> messagesContent()
            1 -> notificationsContent()
        }
    }
}

// ── Empty state placeholders ──

@Composable
private fun MessagesEmptyPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PaceDreamSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = PaceDreamIcons.Mail,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = PaceDreamColors.TextSecondary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        Text(
            text = "No messages yet",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "When guests reach out about your listings, their messages will appear here.",
            style = PaceDreamTypography.Subheadline,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NotificationsEmptyPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PaceDreamSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = PaceDreamIcons.Notifications,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = PaceDreamColors.TextSecondary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        Text(
            text = "No notifications yet",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "Booking requests, status updates, and alerts will appear here.",
            style = PaceDreamTypography.Subheadline,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
