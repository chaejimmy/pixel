package com.shourov.apps.pacedream.feature.host.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacedream.common.composables.theme.*

@Composable
fun HostBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val hostScreens = listOf(
        HostScreenItem("host_dashboard", "Dashboard", PaceDreamIcons.Dashboard),
        HostScreenItem("host_listings", "Listings", PaceDreamIcons.Home),
        HostScreenItem("host_bookings", "Bookings", PaceDreamIcons.CalendarToday),
        HostScreenItem("host_earnings", "Earnings", PaceDreamIcons.AttachMoney),
        HostScreenItem("host_analytics", "Analytics", PaceDreamIcons.Analytics)
    )

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = PaceDreamColors.Background,
        tonalElevation = 8.dp
    ) {
        hostScreens.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = { onNavigate(screen.route) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (currentRoute == screen.route)
                                    PaceDreamColors.Primary.copy(alpha = 0.1f)
                                else
                                    Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            tint = if (currentRoute == screen.route)
                                PaceDreamColors.Primary
                            else
                                PaceDreamColors.TextSecondary
                        )
                    }
                },
                label = {
                    Text(
                        text = screen.title,
                        style = PaceDreamTypography.Caption.copy(
                            fontSize = 11.sp,
                            fontWeight = if (currentRoute == screen.route)
                                FontWeight.SemiBold
                            else
                                FontWeight.Normal
                        ),
                        color = if (currentRoute == screen.route)
                            PaceDreamColors.Primary
                        else
                            PaceDreamColors.TextSecondary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PaceDreamColors.Primary,
                    selectedTextColor = PaceDreamColors.Primary,
                    unselectedIconColor = PaceDreamColors.TextSecondary,
                    unselectedTextColor = PaceDreamColors.TextSecondary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

data class HostScreenItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)