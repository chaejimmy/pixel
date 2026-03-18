package com.shourov.apps.pacedream.feature.notification.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.components.PaceDreamEmptyState
import com.pacedream.common.composables.components.PaceDreamErrorState
import com.pacedream.common.composables.components.PaceDreamLoadingState
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.feature.notification.model.AppNotification
import com.shourov.apps.pacedream.feature.notification.model.NotificationGroup
import com.shourov.apps.pacedream.feature.notification.model.NotificationUiState

/**
 * Notification screen matching iOS NotificationView.
 * Groups notifications by Today/This Week/This Month/Earlier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = hiltViewModel(),
    onNotificationClick: (AppNotification) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = uiState,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
        label = "notification_state",
        modifier = modifier
    ) { state ->
        when (state) {
            is NotificationUiState.Loading -> PaceDreamLoadingState(
                message = "Loading notifications\u2026",
                modifier = Modifier.fillMaxSize()
            )
            is NotificationUiState.Success -> NotificationSuccessState(
                state = state,
                onRefresh = viewModel::refresh,
                onNotificationClick = { notification ->
                    viewModel.markAsRead(notification.id)
                    onNotificationClick(notification)
                }
            )
            is NotificationUiState.Error -> PaceDreamErrorState(
                title = "Couldn\u2019t load notifications",
                description = state.message,
                onRetryClick = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            )
            is NotificationUiState.Empty -> PaceDreamEmptyState(
                title = "No notifications",
                description = "We\u2019ll let you know when something important happens.",
                icon = PaceDreamIcons.Notifications,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSuccessState(
    state: NotificationUiState.Success,
    onRefresh: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = PaceDreamSpacing.MD,
                vertical = PaceDreamSpacing.SM
            ),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
            modifier = Modifier.fillMaxSize()
        ) {
            state.groupedNotifications.forEach { (group, notifications) ->
                // Section header
                item(key = "header_${group.name}") {
                    Text(
                        text = group.displayName,
                        style = PaceDreamTypography.Headline,
                        fontWeight = FontWeight.Bold,
                        color = PaceDreamColors.TextPrimary,
                        modifier = Modifier.padding(
                            top = if (group == NotificationGroup.TODAY) 0.dp else PaceDreamSpacing.SM,
                            bottom = PaceDreamSpacing.XS
                        )
                    )
                }

                // Notification items
                items(
                    items = notifications,
                    key = { it.id }
                ) { notification ->
                    NotificationRow(
                        notification = notification,
                        onClick = { onNotificationClick(notification) }
                    )
                }
            }
        }
    }
}

/**
 * Single notification row matching iOS NotificationRow.
 */
@Composable
private fun NotificationRow(
    notification: AppNotification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) PaceDreamColors.Card else PaceDreamColors.Card
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.Top
        ) {
            // Bell icon - matches iOS bell/bell.badge
            Icon(
                imageVector = PaceDreamIcons.Notifications,
                contentDescription = null,
                tint = if (notification.isRead) PaceDreamColors.TextSecondary else PaceDreamColors.Primary,
                modifier = Modifier.size(22.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = PaceDreamTypography.Body,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                    color = PaceDreamColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
