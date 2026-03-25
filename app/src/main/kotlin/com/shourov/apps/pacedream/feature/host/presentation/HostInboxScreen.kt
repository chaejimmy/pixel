package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.layout.*
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
                        text = "Messages",
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
            // Segmented Control — shared component
            HostSegmentedControl(
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

// ── Default Placeholders — using shared HostEmptyState pattern ──

@Composable
private fun DefaultMessagesPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        HostEmptyState(
            icon = PaceDreamIcons.Mail,
            title = "No messages yet",
            subtitle = "When guests reach out about your listings, their messages will show up here."
        )
    }
}

@Composable
private fun DefaultNotificationsPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        HostEmptyState(
            icon = PaceDreamIcons.Notifications,
            title = "No notifications yet",
            subtitle = "Booking requests, updates, and alerts will show up here."
        )
    }
}
