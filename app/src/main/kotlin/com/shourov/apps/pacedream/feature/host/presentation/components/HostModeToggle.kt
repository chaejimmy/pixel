// @DesignSystemEscape (reason="legacy debt tracked in DESIGN_SYSTEM_COVERAGE.md — migrate per the suggested order in that file before removing this opt-out")
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
import com.shourov.apps.pacedream.designsystem.OnBrandSurface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import com.pacedream.common.composables.theme.*

@Composable
fun HostModeToggle(
    isHostMode: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(
            containerColor = if (isHostMode) PaceDreamColors.HostAccent else PaceDreamColors.Card
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isHostMode) PaceDreamIcons.Home else PaceDreamIcons.Person,
                    contentDescription = if (isHostMode) "Host Mode" else "Guest Mode",
                    tint = if (isHostMode) OnBrandSurface else PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                
                Column {
                    Text(
                        text = if (isHostMode) "Host Mode" else "Guest Mode",
                        style = PaceDreamTypography.Headline,
                        color = if (isHostMode) OnBrandSurface else PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = if (isHostMode) "Manage your properties" else "Book amazing stays",
                        style = PaceDreamTypography.Caption,
                        color = if (isHostMode) OnBrandSurface.copy(alpha = 0.8f) else PaceDreamColors.TextSecondary
                    )
                }
            }
            
            Switch(
                checked = isHostMode,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnBrandSurface,
                    checkedTrackColor = OnBrandSurface.copy(alpha = 0.3f),
                    uncheckedThumbColor = PaceDreamColors.TextSecondary,
                    uncheckedTrackColor = PaceDreamColors.Border
                )
            )
        }
    }
}

@Composable
fun HostModeIndicator(
    isHostMode: Boolean,
    modifier: Modifier = Modifier
) {
    if (isHostMode) {
        Card(
            modifier = modifier
                .clip(RoundedCornerShape(PaceDreamRadius.SM)),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.HostAccent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = PaceDreamSpacing.SM,
                    vertical = PaceDreamSpacing.XS
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Home,
                    contentDescription = "Host Mode",
                    tint = OnBrandSurface,
                    modifier = Modifier.size(12.dp)
                )
                
                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                
                Text(
                    text = "HOST",
                    style = PaceDreamTypography.Caption.copy(fontSize = 10.sp),
                    color = OnBrandSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HostModeBanner(
    isHostMode: Boolean,
    onSwitchToGuest: () -> Unit,
    onSwitchToHost: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isHostMode) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PaceDreamRadius.MD)),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.HostAccent.copy(alpha = 0.1f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaceDreamSpacing.MD),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Home,
                        contentDescription = "Host Mode",
                        tint = PaceDreamColors.HostAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    
                    Text(
                        text = "You're in Host Mode",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.HostAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                TextButton(
                    onClick = onSwitchToGuest
                ) {
                    Text(
                        text = "Switch to Guest",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.HostAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PaceDreamRadius.MD)),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Info.copy(alpha = 0.1f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaceDreamSpacing.MD),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Person,
                        contentDescription = "Guest Mode",
                        tint = PaceDreamColors.Info,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    
                    Text(
                        text = "You're in Guest Mode",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.Info,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                TextButton(
                    onClick = onSwitchToHost
                ) {
                    Text(
                        text = "Switch to Host Mode",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.Info,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Preview(name = "HostModeToggle host=on — light", showBackground = true)
@Composable
private fun HostModeToggleHostLightPreview() {
    PaceDreamTheme(darkTheme = false) {
        HostModeToggle(isHostMode = true, onToggle = {})
    }
}

@Preview(
    name = "HostModeToggle host=on — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun HostModeToggleHostDarkPreview() {
    PaceDreamTheme(darkTheme = true) {
        HostModeToggle(isHostMode = true, onToggle = {})
    }
}

@Preview(name = "HostModeToggle host=off — light", showBackground = true)
@Composable
private fun HostModeToggleGuestLightPreview() {
    PaceDreamTheme(darkTheme = false) {
        HostModeToggle(isHostMode = false, onToggle = {})
    }
}

@Preview(
    name = "HostModeToggle host=off — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun HostModeToggleGuestDarkPreview() {
    PaceDreamTheme(darkTheme = true) {
        HostModeToggle(isHostMode = false, onToggle = {})
    }
}
