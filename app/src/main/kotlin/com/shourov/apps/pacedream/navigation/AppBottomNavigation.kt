package com.shourov.apps.pacedream.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamIconSize

/**
 * iOS 26 Liquid Glass Floating Tab Bar
 *
 * Matches iOS tab bar design language:
 * - Translucent glass material background
 * - Compact height (iOS standard tab bar)
 * - Caption2 labels (11sp)
 * - System indigo active tint, secondary label inactive
 * - No indicator / transparent selection
 */
@Composable
fun AppBottomNavigation(
    items: List<BottomNavigationItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
) {
    NavigationBar(
        windowInsets = NavigationBarDefaults.windowInsets,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        containerColor = PaceDreamColors.Background.copy(alpha = PaceDreamGlass.ThickAlpha),
        tonalElevation = 0.dp
    ) {
        items.forEachIndexed { index, bottomNavigationItem ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onItemClick(index) },
                icon = {
                    BadgedBox(
                        badge = {
                            val count = bottomNavigationItem.badgeCount
                            if (count != null && count > 0) {
                                Badge(
                                    containerColor = PaceDreamColors.Red,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (count > 99) "99+" else count.toString(),
                                        style = PaceDreamTypography.Caption2,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            modifier = Modifier.size(PaceDreamIconSize.MD),
                            painter = painterResource(id = bottomNavigationItem.icon),
                            contentDescription = stringResource(id = bottomNavigationItem.text),
                            tint = if (selectedIndex == index)
                                PaceDreamColors.Primary
                            else
                                PaceDreamColors.TextSecondary
                        )
                    }
                },
                label = {
                    Text(
                        text = stringResource(id = bottomNavigationItem.text),
                        style = PaceDreamTypography.Caption2.copy(
                            fontWeight = if (selectedIndex == index)
                                FontWeight.SemiBold
                            else
                                FontWeight.Normal
                        ),
                        color = if (selectedIndex == index)
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

@Stable
data class BottomNavigationItem(
    @DrawableRes val icon: Int,
    @StringRes val text: Int,
    val badgeCount: Int? = null
)
