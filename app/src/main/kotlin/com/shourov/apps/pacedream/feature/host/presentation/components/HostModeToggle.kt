package com.shourov.apps.pacedream.feature.host.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
            containerColor = if (isHostMode) PaceDreamColors.Primary else PaceDreamColors.Card
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
                    imageVector = if (isHostMode) Icons.Default.Home else Icons.Default.Person,
                    contentDescription = if (isHostMode) "Host Mode" else "Guest Mode",
                    tint = if (isHostMode) Color.White else PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                
                Column {
                    Text(
                        text = if (isHostMode) "Host Mode" else "Guest Mode",
                        style = PaceDreamTypography.Headline,
                        color = if (isHostMode) Color.White else PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = if (isHostMode) "Manage your properties" else "Book amazing stays",
                        style = PaceDreamTypography.Caption,
                        color = if (isHostMode) Color.White.copy(alpha = 0.8f) else PaceDreamColors.TextSecondary
                    )
                }
            }
            
            Switch(
                checked = isHostMode,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.White.copy(alpha = 0.3f),
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
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Primary),
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
                    imageVector = Icons.Default.Home,
                    contentDescription = "Host Mode",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                
                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                
                Text(
                    text = "HOST",
                    style = PaceDreamTypography.Caption.copy(fontSize = 10.sp),
                    color = Color.White,
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
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Primary.copy(alpha = 0.1f)),
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
                        imageVector = Icons.Default.Home,
                        contentDescription = "Host Mode",
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    
                    Text(
                        text = "You're in Host Mode",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                TextButton(
                    onClick = onSwitchToGuest
                ) {
                    Text(
                        text = "Switch to Guest",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.Primary,
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
                        imageVector = Icons.Default.Person,
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
                        text = "Switch to Host",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.Info,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
