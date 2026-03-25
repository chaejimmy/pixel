package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacedream.common.composables.theme.*

/**
 * Host Inbox Screen — iOS HostInboxView parity.
 *
 * Segmented control with Messages and Notifications tabs,
 * matching iOS HostInboxView.swift structure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostInboxScreen(
    onThreadClick: (String) -> Unit = {},
    messagesContent: @Composable () -> Unit = { DefaultMessagesPlaceholder() },
    notificationsContent: @Composable () -> Unit = { DefaultNotificationsPlaceholder() }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Messages", "Notifications")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inbox",
                        style = PaceDreamTypography.Title1.copy(fontWeight = FontWeight.Bold),
                        color = PaceDreamColors.TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Segmented Control — iOS CustomSegmentedControl parity
            HostInboxSegmentedControl(
                tabs = tabs,
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Tab Content
            when (selectedTab) {
                0 -> messagesContent()
                1 -> notificationsContent()
            }
        }
    }
}

// ── Segmented Control ───────────────────────────────────────────

@Composable
private fun HostInboxSegmentedControl(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM)
            .background(PaceDreamColors.Surface, RoundedCornerShape(PaceDreamRadius.SM))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            Surface(
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(index) },
                color = if (selected) PaceDreamColors.Card else PaceDreamColors.Surface,
                shadowElevation = if (selected) PaceDreamElevation.XS else 0.dp,
                shape = RoundedCornerShape(PaceDreamRadius.SM)
            ) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Subheadline,
                    color = if (selected) PaceDreamColors.TextPrimary else PaceDreamColors.TextSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}

// ── Default Placeholders ────────────────────────────────────────

@Composable
private fun DefaultMessagesPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(PaceDreamSpacing.LG)
        ) {
            Icon(
                imageVector = PaceDreamIcons.Mail,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Text(
                text = "No messages yet",
                style = PaceDreamTypography.Headline.copy(fontSize = 17.sp),
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = "Messages from guests about your listings will appear here.",
                style = PaceDreamTypography.Subheadline,
                color = PaceDreamColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DefaultNotificationsPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(PaceDreamSpacing.LG)
        ) {
            Icon(
                imageVector = PaceDreamIcons.Notifications,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Text(
                text = "No notifications yet",
                style = PaceDreamTypography.Headline.copy(fontSize = 17.sp),
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = "Booking requests and host notifications will appear here.",
                style = PaceDreamTypography.Subheadline,
                color = PaceDreamColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
