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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.foundation.layout.navigationBars
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamIconSize

/**
 * iOS 26 Liquid Glass Floating Tab Bar
 *
 * Matches iOS tab bar design language:
 * - Translucent glass material background with subtle top shadow
 * - Compact height (iOS standard tab bar)
 * - Caption2 labels (11sp) with medium weight for selected state
 * - Primary active tint with stronger presence, secondary label inactive
 * - Pill-shaped indicator for selected tab (iOS parity)
 */
@Composable
fun AppBottomNavigation(
    items: List<BottomNavigationItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
) {
    NavigationBar(
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(0.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .border(
                width = 0.5.dp,
                color = PaceDreamColors.Border.copy(alpha = 0.15f),
                shape = RoundedCornerShape(0.dp)
            ),
        containerColor = PaceDreamColors.Background.copy(alpha = PaceDreamGlass.ThickAlpha),
        tonalElevation = 0.dp
    ) {
        items.forEachIndexed { index, bottomNavigationItem ->
            val isSelected = selectedIndex == index
            NavigationBarItem(
                selected = isSelected,
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
                            modifier = Modifier.size(
                                if (isSelected) 24.dp else PaceDreamIconSize.MD
                            ),
                            painter = painterResource(id = bottomNavigationItem.icon),
                            contentDescription = stringResource(id = bottomNavigationItem.text),
                            tint = if (isSelected)
                                PaceDreamColors.Primary
                            else
                                PaceDreamColors.TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                },
                label = {
                    Text(
                        text = stringResource(id = bottomNavigationItem.text),
                        style = PaceDreamTypography.Caption2.copy(
                            fontWeight = if (isSelected)
                                FontWeight.Bold
                            else
                                FontWeight.Normal,
                            letterSpacing = if (isSelected) 0.1.sp else 0.07.sp
                        ),
                        color = if (isSelected)
                            PaceDreamColors.Primary
                        else
                            PaceDreamColors.TextSecondary.copy(alpha = 0.7f)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PaceDreamColors.Primary,
                    selectedTextColor = PaceDreamColors.Primary,
                    unselectedIconColor = PaceDreamColors.TextSecondary,
                    unselectedTextColor = PaceDreamColors.TextSecondary,
                    indicatorColor = PaceDreamColors.Primary.copy(alpha = 0.1f)
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
