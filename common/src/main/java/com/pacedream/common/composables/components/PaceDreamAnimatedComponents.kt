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

package com.pacedream.common.composables.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.animations.*
import com.pacedream.common.composables.theme.PaceDreamDesignSystem
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * Enhanced Animated Components for PaceDream App
 * Provides smooth, engaging animations for better user experience
 */

// Animated Property Card
@Composable
fun AnimatedPropertyCard(
    title: String,
    location: String,
    price: String,
    rating: Double,
    reviewCount: Int,
    imageUrl: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    index: Int = 0
) {
    StaggeredAnimation(
        visible = isVisible,
        index = index,
        modifier = modifier
    ) {
        CardHoverAnimation {
            PaceDreamPropertyCard(
                title = title,
                location = location,
                price = price,
                rating = rating,
                reviewCount = reviewCount,
                imageUrl = imageUrl,
                onClick = onClick
            )
        }
    }
}

// Animated Property List
@Composable
fun AnimatedPropertyList(
    properties: List<PropertyItem>,
    onPropertyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
    ) {
        itemsIndexed(properties) { index, property ->
            AnimatedPropertyCard(
                title = property.title,
                location = property.location,
                price = property.price,
                rating = property.rating,
                reviewCount = property.reviewCount,
                imageUrl = property.imageUrl,
                onClick = { onPropertyClick(property.id) },
                isVisible = isVisible,
                index = index
            )
        }
    }
}

// Animated Category Pills
@Composable
fun AnimatedCategoryPills(
    categories: List<CategoryItem>,
    selectedCategory: String? = null,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        itemsIndexed(categories) { index, category ->
            AnimatedCategoryPill(
                name = category.name,
                icon = category.icon,
                isSelected = selectedCategory == category.id,
                onClick = { onCategoryClick(category.id) },
                isVisible = isVisible,
                index = index
            )
        }
    }
}

@Composable
private fun AnimatedCategoryPill(
    name: String,
    icon: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    index: Int = 0
) {
    StaggeredAnimation(
        visible = isVisible,
        index = index,
        modifier = modifier
    ) {
        var isPressed by remember { mutableStateOf(false) }
        
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = tween(
                durationMillis = PaceDreamAnimationDuration.SHORT,
                easing = PaceDreamEasing.EaseOut
            ),
            label = "pill_scale"
        )
        
        PaceDreamCategoryPillSimple(
            title = name,
            iconRes = icon,
            isSelected = isSelected,
            onClick = {
                isPressed = true
                onClick()
            },
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

// Animated Loading State
@Composable
fun AnimatedLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoadingAnimation {
                CircularProgressIndicator(
                    color = PaceDreamColors.Primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            FadeInAnimation(
                visible = true,
                duration = PaceDreamAnimationDuration.LONG
            ) {
                Text(
                    text = "Loading...",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextPrimary
                )
            }
        }
    }
}

// Animated Empty State
@Composable
fun AnimatedEmptyState(
    title: String,
    subtitle: String,
    icon: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScaleAnimation(
                visible = true,
                duration = PaceDreamAnimationDuration.LONG
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            FadeInAnimation(
                visible = true,
                duration = PaceDreamAnimationDuration.LONG,
                delay = 200
            ) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            FadeInAnimation(
                visible = true,
                duration = PaceDreamAnimationDuration.LONG,
                delay = 400
            ) {
                Text(
                    text = subtitle,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
            }
        }
    }
}

// Animated Search Bar
@Composable
fun AnimatedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false
) {
    val animatedHeight by animateDpAsState(
        targetValue = if (isExpanded) 56.dp else 48.dp,
        animationSpec = tween(
            durationMillis = PaceDreamAnimationDuration.MEDIUM,
            easing = PaceDreamEasing.EaseInOut
        ),
        label = "search_height"
    )
    
    val animatedElevation by animateDpAsState(
        targetValue = if (isExpanded) 8.dp else 4.dp,
        animationSpec = tween(
            durationMillis = PaceDreamAnimationDuration.MEDIUM,
            easing = PaceDreamEasing.EaseInOut
        ),
        label = "search_elevation"
    )
    
    PaceDreamSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearchClick = onSearchClick,
        modifier = modifier
    )
}

// Animated Message Bubble
@Composable
fun AnimatedMessageBubble(
    message: String,
    isFromCurrentUser: Boolean,
    timestamp: String,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    SlideInAnimation(
        visible = isVisible,
        slideDirection = if (isFromCurrentUser) SlideDirection.Right else SlideDirection.Left,
        duration = PaceDreamAnimationDuration.SHORT
    ) {
        // Message bubble implementation would go here
        // This is a placeholder for the actual message bubble component
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = if (isFromCurrentUser) {
                    PaceDreamColors.Primary
                } else {
                    PaceDreamColors.Gray200
                }
            )
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(PaceDreamSpacing.SM)
            )
        }
    }
}

// Data classes for animated components
data class PropertyItem(
    val id: String,
    val title: String,
    val location: String,
    val price: String,
    val rating: Double,
    val reviewCount: Int,
    val imageUrl: String?
)

data class CategoryItem(
    val id: String,
    val name: String,
    val icon: Int
)
