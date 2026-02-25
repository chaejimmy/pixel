/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shourov.apps.pacedream.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*

@Composable
fun AmenityChip(
    amenity: String,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.Gray100
        ),
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = PaceDreamSpacing.MD,
                vertical = PaceDreamSpacing.SM
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = amenity,
                tint = if (isSelected) Color.White else PaceDreamColors.TextSecondary,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
            
            Text(
                text = amenity,
                style = PaceDreamTypography.Caption,
                color = if (isSelected) Color.White else PaceDreamColors.TextSecondary,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
fun AmenityList(
    amenities: List<String>,
    selectedAmenities: Set<String> = emptySet(),
    onAmenityClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val amenityIcons = mapOf(
        "WiFi" to PaceDreamIcons.Wifi,
        "Parking" to PaceDreamIcons.LocalParking,
        "Pool" to PaceDreamIcons.Pool,
        "Gym" to PaceDreamIcons.FitnessCenter,
        "Kitchen" to PaceDreamIcons.Kitchen,
        "AC" to PaceDreamIcons.Air,
        "TV" to PaceDreamIcons.Tv,
        "Pet Friendly" to PaceDreamIcons.Pets,
        "Balcony" to PaceDreamIcons.Balcony,
        "Garden" to PaceDreamIcons.Yard,
        "Security" to PaceDreamIcons.Security,
        "Elevator" to PaceDreamIcons.Elevator,
        "Laundry" to PaceDreamIcons.LocalLaundryService,
        "Heating" to PaceDreamIcons.Thermostat,
        "Breakfast" to PaceDreamIcons.Restaurant
    )
    
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        items(amenities) { amenity ->
            AmenityChip(
                amenity = amenity,
                icon = amenityIcons[amenity] ?: PaceDreamIcons.Star,
                isSelected = selectedAmenities.contains(amenity),
                onClick = { onAmenityClick(amenity) }
            )
        }
    }
}

@Composable
fun HostCard(
    hostName: String,
    hostTitle: String,
    hostImage: String? = null,
    rating: Float,
    responseTime: String,
    isSuperhost: Boolean = false,
    onContactClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.LG)
        ) {
            // Host Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                // Host Avatar
                Card(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(PaceDreamRadius.Round),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray200)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Person,
                            contentDescription = "Host avatar",
                            tint = PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(PaceDreamIconSize.LG)
                        )
                    }
                }
                
                // Host Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
                    ) {
                        Text(
                            text = hostName,
                            style = PaceDreamTypography.Title3,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (isSuperhost) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Warning),
                                shape = RoundedCornerShape(PaceDreamRadius.SM)
                            ) {
                                Text(
                                    text = "Superhost",
                                    style = PaceDreamTypography.Caption,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        horizontal = PaceDreamSpacing.SM,
                                        vertical = PaceDreamSpacing.XS
                                    )
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = hostTitle,
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextSecondary
                    )
                }
                
                // Contact Button
                IconButton(
                    onClick = onContactClick,
                    modifier = Modifier
                        .background(
                            PaceDreamColors.Primary,
                            RoundedCornerShape(PaceDreamRadius.SM)
                        )
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Message,
                        contentDescription = "Contact host",
                        tint = Color.White,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            // Host Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Star,
                        contentDescription = null,
                        tint = PaceDreamColors.Warning,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                    
                    Text(
                        text = rating.toString(),
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "rating",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
                
                // Response Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Schedule,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                    
                    Text(
                        text = responseTime,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            // View Profile Button
            OutlinedButton(
                onClick = onProfileClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PaceDreamColors.Primary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(PaceDreamColors.Primary, PaceDreamColors.Primary)
                    )
                )
            ) {
                Text(
                    text = "View Profile",
                    style = PaceDreamTypography.Callout,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CompactHostCard(
    hostName: String,
    rating: Float,
    isSuperhost: Boolean = false,
    onContactClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onContactClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray50),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            // Host Avatar
            Card(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray200)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Person,
                        contentDescription = "Host avatar",
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                }
            }
            
            // Host Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
                ) {
                    Text(
                        text = hostName,
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (isSuperhost) {
                        Icon(
                            imageVector = PaceDreamIcons.Star,
                            contentDescription = "Superhost",
                            tint = PaceDreamColors.Warning,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Star,
                        contentDescription = null,
                        tint = PaceDreamColors.Warning,
                        modifier = Modifier.size(10.dp)
                    )
                    
                    Text(
                        text = rating.toString(),
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
            
            // Contact Icon
            Icon(
                imageVector = PaceDreamIcons.Message,
                contentDescription = "Contact host",
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
        }
    }
}

@Composable
fun AmenityFilterChip(
    amenity: String,
    icon: ImageVector,
    isSelected: Boolean = false,
    count: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.Card
        ),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = PaceDreamSpacing.MD,
                vertical = PaceDreamSpacing.SM
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = amenity,
                tint = if (isSelected) Color.White else PaceDreamColors.TextSecondary,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
            
            Text(
                text = amenity,
                style = PaceDreamTypography.Callout,
                color = if (isSelected) Color.White else PaceDreamColors.TextPrimary,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            
            count?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color.White else PaceDreamColors.Primary
                    ),
                    shape = RoundedCornerShape(PaceDreamRadius.XS)
                ) {
                    Text(
                        text = it.toString(),
                        style = PaceDreamTypography.Caption,
                        color = if (isSelected) PaceDreamColors.Primary else Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = PaceDreamSpacing.XS,
                            vertical = 2.dp
                        )
                    )
                }
            }
        }
    }
}
