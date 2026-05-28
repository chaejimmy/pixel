@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.shourov.apps.pacedream.feature.notification.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.launch

/**
 * Notification screen matching iOS NotificationView.
 * Groups notifications by Today/This Week/This Month/Earlier.
 */
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = hiltViewModel(),
    onNotificationClick: (AppNotification) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = PaceDreamIcons.ArrowBack,
                                contentDescription = "Back",
                                tint = PaceDreamColors.TextPrimary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = PaceDreamIcons.MoreVert,
                            contentDescription = "More options",
                            tint = PaceDreamColors.TextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark all as read") },
                            onClick = {
                                menuExpanded = false
                                viewModel.markAllAsRead()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Notification settings") },
                            onClick = {
                                menuExpanded = false
                                onSettingsClick()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background,
                    titleContentColor = PaceDreamColors.TextPrimary
                )
            )
        },
        containerColor = PaceDreamColors.Background,
        modifier = modifier
    ) { paddingValues ->
        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "notification_state",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { state ->
            when (state) {
                is NotificationUiState.Loading -> PaceDreamLoadingState(
                    message = "Loading notifications…",
                    modifier = Modifier.fillMaxSize()
                )
                is NotificationUiState.Success -> NotificationSuccessState(
                    state = state,
                    onRefresh = viewModel::refresh,
                    onNotificationClick = { notification ->
                        viewModel.markAsRead(notification.id)
                        onNotificationClick(notification)
                    },
                    onMarkAsRead = viewModel::markAsRead,
                    onMarkAsUnread = viewModel::markAsUnread
                )
                is NotificationUiState.Error -> PaceDreamErrorState(
                    title = "Couldn’t load notifications",
                    description = errorDescriptionFor(state.message),
                    onRetryClick = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                )
                is NotificationUiState.Empty -> NotificationEmptyState(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun NotificationSuccessState(
    state: NotificationUiState.Success,
    onRefresh: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAsUnread: (String) -> Unit
) {
    var sheetTarget by remember { mutableStateOf<AppNotification?>(null) }

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
                if (notifications.isEmpty()) return@forEach

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

                items(
                    items = notifications,
                    key = { it.id }
                ) { notification ->
                    SwipeableNotificationRow(
                        notification = notification,
                        onClick = { onNotificationClick(notification) },
                        onLongPress = { sheetTarget = notification },
                        onSwipeMarkAsRead = { onMarkAsRead(notification.id) }
                    )
                }
            }
        }
    }

    sheetTarget?.let { target ->
        NotificationActionSheet(
            notification = target,
            onDismiss = { sheetTarget = null },
            onMarkAsRead = {
                onMarkAsRead(target.id)
                sheetTarget = null
            },
            onMarkAsUnread = {
                onMarkAsUnread(target.id)
                sheetTarget = null
            }
        )
    }
}

@Composable
private fun SwipeableNotificationRow(
    notification: AppNotification,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeMarkAsRead: () -> Unit
) {
    // Swiping past the threshold marks the row read but the row should stay in
    // place. confirmValueChange returns false so SwipeToDismissBox snaps back
    // to Settled instead of dismissing the item.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                if (!notification.isRead) onSwipeMarkAsRead()
                false
            } else {
                false
            }
        },
        positionalThreshold = { distance -> distance * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeReadBackground() },
        enableDismissFromStartToEnd = !notification.isRead,
        enableDismissFromEndToStart = false
    ) {
        NotificationRow(
            notification = notification,
            onClick = onClick,
            onLongPress = onLongPress
        )
    }
}

@Composable
private fun SwipeReadBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = PaceDreamColors.Success,
                shape = RoundedCornerShape(PaceDreamRadius.MD)
            )
            .padding(horizontal = PaceDreamSpacing.LG),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White
            )
            Text(
                text = "Mark as read",
                style = PaceDreamTypography.Body,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Single notification row matching iOS NotificationRow.
 */
@Composable
private fun NotificationRow(
    notification: AppNotification,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                }
            ),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.Top
        ) {
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

@Composable
private fun NotificationActionSheet(
    notification: AppNotification,
    onDismiss: () -> Unit,
    onMarkAsRead: () -> Unit,
    onMarkAsUnread: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val close: (() -> Unit) -> Unit = { action ->
        scope.launch {
            try { sheetState.hide() } finally { action() }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PaceDreamColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PaceDreamSpacing.MD,
                    vertical = PaceDreamSpacing.SM
                ),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
        ) {
            if (notification.isRead) {
                ActionSheetItem(
                    icon = Icons.Rounded.MarkEmailUnread,
                    label = "Mark as unread",
                    onClick = { close(onMarkAsUnread) }
                )
            } else {
                ActionSheetItem(
                    icon = Icons.Rounded.MarkEmailRead,
                    label = "Mark as read",
                    onClick = { close(onMarkAsRead) }
                )
            }
            // "Delete" intentionally omitted — NotificationRepository does not
            // expose a delete endpoint, so the affordance is hidden.
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        }
    }
}

@Composable
private fun ActionSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
            .padding(
                horizontal = PaceDreamSpacing.SM,
                vertical = PaceDreamSpacing.MD
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PaceDreamColors.TextPrimary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextPrimary
        )
    }
}

@Composable
private fun NotificationEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(PaceDreamSpacing.LG)
        ) {
            DottedBellGlyph()
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            Text(
                text = "You’re all caught up",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = "We’ll notify you about bookings, messages, and payments.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

@Composable
private fun DottedBellGlyph() {
    val ring = PaceDreamColors.TextTertiary
    Box(
        modifier = Modifier
            .size(120.dp)
            .drawBehind {
                drawCircle(
                    color = ring,
                    radius = size.minDimension / 2f,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                            0f
                        )
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = PaceDreamIcons.Notifications,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Tailor the error description to whether the underlying failure looked like a
 * network problem or a server problem. We rely on the user-facing string the
 * VM already produced via UserFacingErrorMapper — it always classifies into
 * one of those buckets before reaching the UI.
 */
private fun errorDescriptionFor(message: String): String {
    val lower = message.lowercase()
    val networkSignals = listOf(
        "internet connection",
        "connection timed out",
        "connection",
        "secure connection",
        "interrupted"
    )
    val serverSignals = listOf(
        "on our end",
        "temporarily unavailable",
        "didn’t get a valid response",
        "didn't get a valid response"
    )
    return when {
        networkSignals.any { lower.contains(it) } ->
            "Looks like you’re offline. Check your connection and try again."
        serverSignals.any { lower.contains(it) } ->
            "Our server is having trouble right now. Please try again in a moment."
        else -> message
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun NotificationScreenPreview() {
    val now = java.util.Calendar.getInstance()
    val todayIso = java.text.SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        java.util.Locale.US
    ).format(now.time)
    val earlierIso = java.text.SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        java.util.Locale.US
    ).format(java.util.Date(now.timeInMillis - 1000L * 60 * 60 * 24 * 40))

    val mock = listOf(
        AppNotification(
            id = "n1",
            title = "Booking confirmed",
            body = "Your stay at Sunset Loft is booked for May 25–27.",
            type = "booking_confirmed",
            isRead = false,
            createdAt = todayIso,
        ),
        AppNotification(
            id = "n2",
            title = "New message from Alex",
            body = "Hey! Checking in re: parking instructions for tomorrow.",
            type = "message_received",
            isRead = false,
            createdAt = todayIso,
        ),
        AppNotification(
            id = "n3",
            title = "Payout sent",
            body = "Your payout of $148.00 is on the way to your bank.",
            type = "payout_sent",
            isRead = true,
            createdAt = earlierIso,
        ),
    )

    val state = NotificationUiState.Success(
        groupedNotifications = mock
            .groupBy { NotificationGroup.forDate(it.parsedDate) }
            .toSortedMap(compareBy { it.ordinal })
    )

    NotificationSuccessState(
        state = state,
        onRefresh = { /* preview no-op */ },
        onNotificationClick = { /* preview no-op */ },
        onMarkAsRead = { /* preview no-op */ },
        onMarkAsUnread = { /* preview no-op */ },
    )
}

