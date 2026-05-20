package com.shourov.apps.pacedream.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Notification bell with an unread-count badge. Wraps a Material 3 [BadgedBox]
 * around an [IconButton] so the badge announces unread items consistently
 * across every screen that hosts the bell (Home / Bookings / ChatList /
 * Dashboard / HostDashboard).
 *
 * The badge is suppressed when [unreadCount] is zero, displays the literal
 * count up to 99, and caps at the string "99+" beyond that. Appearance is
 * animated with a 150ms scale-in / fade-in to avoid a jarring pop when a
 * push notification lands while the screen is visible.
 *
 * Color / shape parameters let callers match the existing bell-container
 * styling on each surface (purple glass on Home, white circle on the
 * Discover header, etc.) without re-implementing the badge logic.
 */
@Composable
fun NotificationBellButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    iconTint: Color = LocalContentColor.current,
    border: BorderStroke? = null,
    iconSize: Dp = 22.dp,
    contentDescription: String = "Notifications",
) {
    val unreadDescription = when {
        unreadCount <= 0 -> contentDescription
        unreadCount == 1 -> "$contentDescription, 1 unread"
        unreadCount > MAX_DISPLAY_COUNT -> "$contentDescription, ${MAX_DISPLAY_COUNT}+ unread"
        else -> "$contentDescription, $unreadCount unread"
    }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .let { base -> if (border != null) base.border(border, CircleShape) else base }
            .semantics { stateDescription = unreadDescription },
    ) {
        BadgedBox(
            badge = {
                AnimatedVisibility(
                    visible = unreadCount > 0,
                    enter = scaleIn(animationSpec = tween(BADGE_APPEAR_MILLIS)) +
                        fadeIn(animationSpec = tween(BADGE_APPEAR_MILLIS)),
                    exit = scaleOut(animationSpec = tween(BADGE_DISAPPEAR_MILLIS)) +
                        fadeOut(animationSpec = tween(BADGE_DISAPPEAR_MILLIS)),
                ) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ) {
                        Text(text = formatBadgeCount(unreadCount))
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

private const val MAX_DISPLAY_COUNT = 99
private const val BADGE_APPEAR_MILLIS = 150
private const val BADGE_DISAPPEAR_MILLIS = 100

internal fun formatBadgeCount(count: Int): String =
    if (count > MAX_DISPLAY_COUNT) "${MAX_DISPLAY_COUNT}+" else count.toString()
