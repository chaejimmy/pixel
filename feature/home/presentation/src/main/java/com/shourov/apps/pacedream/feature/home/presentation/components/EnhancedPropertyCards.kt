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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.home.domain.models.DestinationModel
import com.shourov.apps.pacedream.feature.home.presentation.R

@Composable
fun EnhancedDestinationCard(
    destination: DestinationModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(200.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background Image
            Image(
                painter = painterResource(destination.icon),
                contentDescription = destination.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(PaceDreamRadius.MD)),
                contentScale = ContentScale.Crop
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PaceDreamSpacing.MD),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = destination.title,
                    style = PaceDreamTypography.Title3,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                
                Text(
                    text = "Explore properties",
                    style = PaceDreamTypography.Caption,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun EnhancedPropertyCard(
    propertyName: String,
    location: String,
    price: String,
    rating: Float,
    imageUrl: String? = null,
    amenities: List<String> = emptyList(),
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(280.dp)
            .height(320.dp),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // Property Image
                Image(
                    painter = painterResource(R.drawable.ic_dummy_user), // Replace with actual image
                    contentDescription = propertyName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = PaceDreamRadius.LG, topEnd = PaceDreamRadius.LG)),
                    contentScale = ContentScale.Crop
                )
                
                // Favorite Button
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(PaceDreamSpacing.SM)
                        .background(
                            Color.White.copy(alpha = 0.9f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) PaceDreamColors.Error else PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                }
                
                // Price Badge
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(PaceDreamSpacing.SM),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Primary),
                    shape = RoundedCornerShape(PaceDreamRadius.SM)
                ) {
                    Text(
                        text = price,
                        style = PaceDreamTypography.Callout,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = PaceDreamSpacing.SM,
                            vertical = PaceDreamSpacing.XS
                        )
                    )
                }
            }
            
            // Content Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaceDreamSpacing.MD)
            ) {
                // Property Name
                Text(
                    text = propertyName,
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                
                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(PaceDreamIconSize.XS)
                    )
                    
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                    
                    Text(
                        text = location,
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                
                // Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = PaceDreamColors.Warning,
                        modifier = Modifier.size(PaceDreamIconSize.XS)
                    )
                    
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                    
                    Text(
                        text = rating.toString(),
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                    
                    Text(
                        text = "• Excellent",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
                
                // Amenities
                if (amenities.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalHotel,
                            contentDescription = null,
                            tint = PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(PaceDreamIconSize.XS)
                        )
                        
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        
                        Text(
                            text = amenities.take(2).joinToString(" • "),
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactPropertyCard(
    propertyName: String,
    location: String,
    price: String,
    rating: Float,
    imageUrl: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image
            Image(
                painter = painterResource(R.drawable.ic_dummy_user), // Replace with actual image
                contentDescription = propertyName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = PaceDreamRadius.MD, topEnd = PaceDreamRadius.MD)),
                contentScale = ContentScale.Crop
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaceDreamSpacing.SM)
            ) {
                Text(
                    text = propertyName,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                
                Text(
                    text = location,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = price,
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = PaceDreamColors.Warning,
                            modifier = Modifier.size(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(2.dp))
                        
                        Text(
                            text = rating.toString(),
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PropertyImageCarousel(
    images: List<String>,
    currentImageIndex: Int,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // Main Image
        Image(
            painter = painterResource(R.drawable.ic_dummy_user), // Replace with actual image
            contentDescription = "Property image",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(PaceDreamRadius.MD)),
            contentScale = ContentScale.Crop
        )
        
        // Image Counter
        Card(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(PaceDreamSpacing.SM),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(PaceDreamRadius.SM)
        ) {
            Text(
                text = "${currentImageIndex + 1}/${images.size}",
                style = PaceDreamTypography.Caption,
                color = Color.White,
                modifier = Modifier.padding(
                    horizontal = PaceDreamSpacing.SM,
                    vertical = PaceDreamSpacing.XS
                )
            )
        }
        
        // Navigation Arrows
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onImageClick((currentImageIndex - 1 + images.size) % images.size) },
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Previous image",
                        tint = Color.White,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                }
                
                IconButton(
                    onClick = { onImageClick((currentImageIndex + 1) % images.size) },
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next image",
                        tint = Color.White,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                }
            }
        }
    }
}
