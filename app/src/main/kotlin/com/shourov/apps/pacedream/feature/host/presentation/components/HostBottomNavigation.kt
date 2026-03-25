package com.shourov.apps.pacedream.feature.host.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*

@Composable
fun HostBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    // iOS parity: Dashboard, Earnings, Post, Inbox, Profile tabs
    val hostScreens = listOf(
        HostScreenItem("host_dashboard", "Dashboard", PaceDreamIcons.Dashboard),
        HostScreenItem("host_earnings", "Earnings", PaceDreamIcons.AttachMoney),
        HostScreenItem("host_post", "Post", PaceDreamIcons.AddCircle),
        HostScreenItem("host_inbox", "Messages", PaceDreamIcons.Mail),
        HostScreenItem("host_profile", "Profile", PaceDreamIcons.Person)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PaceDreamColors.Background,
        shadowElevation = PaceDreamElevation.LG,
        tonalElevation = 0.dp
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            hostScreens.forEach { screen ->
                val selected = currentRoute == screen.route
                val iconColor by animateColorAsState(
                    targetValue = if (selected) PaceDreamColors.HostAccent else PaceDreamColors.TextSecondary,
                    animationSpec = tween(PaceDreamAnimationDuration.FAST),
                    label = "iconColor"
                )

                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(screen.route) },
                    icon = {
                        Icon(
                            modifier = Modifier.size(PaceDreamIconSize.MD),
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            tint = iconColor
                        )
                    },
                    label = {
                        Text(
                            text = screen.title,
                            style = PaceDreamTypography.Caption2.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = iconColor,
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PaceDreamColors.HostAccent,
                        selectedTextColor = PaceDreamColors.HostAccent,
                        unselectedIconColor = PaceDreamColors.TextSecondary,
                        unselectedTextColor = PaceDreamColors.TextSecondary,
                        indicatorColor = PaceDreamColors.HostAccent.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}

data class HostScreenItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
