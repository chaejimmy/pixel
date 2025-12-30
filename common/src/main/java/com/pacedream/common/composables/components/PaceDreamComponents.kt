package com.pacedream.common.composables.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pacedream.common.composables.shimmerEffect
import com.pacedream.common.composables.theme.*

/**
 * Enhanced UI Components for PaceDream App
 * Matching iOS design patterns and specifications
 */

// Hero Header Component
@Composable
fun PaceDreamHeroHeader(
    title: String,
    subtitle: String? = null,
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PaceDreamPrimary,
                        PaceDreamPrimary.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(PaceDreamSpacing.LG)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = PaceDreamTypography.Title1,
                        color = Color.White
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = PaceDreamTypography.Body,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// Enhanced Search Bar Component
@Composable
fun PaceDreamSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    placeholder: String = "Search properties...",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PaceDreamSearchBar.Height)
            .clip(RoundedCornerShape(PaceDreamSearchBar.CornerRadius))
            .background(PaceDreamCard)
            .padding(PaceDreamSearchBar.Padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = PaceDreamTextSecondary,
            modifier = Modifier.size(PaceDreamSearchBar.IconSize)
        )
        
        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
        
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { 
                Text(
                    text = placeholder,
                    color = PaceDreamTextTertiary
                ) 
            },
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = PaceDreamTextPrimary,
                unfocusedTextColor = PaceDreamTextPrimary
            ),
            textStyle = PaceDreamTypography.Body
        )
        
        IconButton(onClick = onFilterClick) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter",
                tint = PaceDreamPrimary,
                modifier = Modifier.size(PaceDreamSearchBar.IconSize)
            )
        }
    }
}

// Enhanced Metric Card Component
@Composable
fun PaceDreamMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color = PaceDreamPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(PaceDreamMetricCard.MinHeight)
            .padding(PaceDreamSpacing.XS),
        colors = CardDefaults.cardColors(containerColor = PaceDreamCard),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamMetricCard.Elevation),
        shape = RoundedCornerShape(PaceDreamMetricCard.CornerRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamMetricCard.Padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(PaceDreamMetricCard.IconSize)
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            Text(
                text = value,
                style = PaceDreamTypography.Title2,
                color = PaceDreamTextPrimary
            )
            
            Text(
                text = title,
                style = PaceDreamTypography.Caption,
                color = PaceDreamTextSecondary
            )
        }
    }
}

// Enhanced Category Pill Component
@Composable
fun PaceDreamCategoryPill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(PaceDreamCategoryPill.Height)
            .clip(RoundedCornerShape(PaceDreamCategoryPill.CornerRadius)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PaceDreamPrimary else PaceDreamGray100
        ),
        contentPadding = PaceDreamCategoryPill.Padding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else PaceDreamTextSecondary,
                modifier = Modifier.size(PaceDreamCategoryPill.IconSize)
            )
            
            Text(
                text = title,
                style = PaceDreamTypography.Caption,
                color = if (isSelected) Color.White else PaceDreamTextSecondary
            )
        }
    }
}

// Enhanced Section Header Component
@Composable
fun PaceDreamSectionHeader(
    title: String,
    onViewAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = PaceDreamTypography.Title3,
            color = PaceDreamTextPrimary
        )
        
        onViewAllClick?.let { onClick ->
            TextButton(onClick = onClick) {
                Text(
                    text = "View All",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamPrimary
                )
            }
        }
    }
}

// Property Card Component
@Composable
fun PaceDreamPropertyCard(
    title: String,
    location: String,
    price: String,
    rating: Double,
    reviewCount: Int,
    imageUrl: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(200.dp)
            .padding(PaceDreamSpacing.XS),
        colors = CardDefaults.cardColors(containerColor = PaceDreamCard),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamPropertyCard.Elevation),
        shape = RoundedCornerShape(PaceDreamPropertyCard.CornerRadius)
    ) {
        Column {
            // Property Image with Coil
            PaceDreamPropertyImage(
                imageUrl = imageUrl,
                contentDescription = "Property image: $title",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamPropertyCard.ImageHeight)
            )
            
            // Property Details
            Column(
                modifier = Modifier.padding(PaceDreamPropertyCard.ContentPadding)
            ) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamTextPrimary,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PaceDreamTextSecondary,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                    
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                    
                    Text(
                        text = location,
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamTextSecondary,
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = price,
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamPrimary
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = PaceDreamWarning,
                            modifier = Modifier.size(PaceDreamIconSize.SM)
                        )
                        
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        
                        Text(
                            text = "$rating ($reviewCount)",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamTextSecondary
                        )
                    }
                }
            }
        }
    }
}

// Destination Card Component
@Composable
fun PaceDreamDestinationCard(
    name: String,
    imageUrl: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(PaceDreamDestinationCard.Width)
            .height(PaceDreamDestinationCard.Height)
            .padding(PaceDreamSpacing.XS),
        colors = CardDefaults.cardColors(containerColor = PaceDreamCard),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamPropertyCard.Elevation),
        shape = RoundedCornerShape(PaceDreamDestinationCard.CornerRadius)
    ) {
        Column {
            // Destination Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamDestinationCard.ImageHeight)
                    .background(PaceDreamGray200)
            ) {
                if (imageUrl != null) {
                    // TODO: Add Coil image loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = PaceDreamTextTertiary,
                            modifier = Modifier.size(PaceDreamIconSize.LG)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PaceDreamTextTertiary,
                            modifier = Modifier.size(PaceDreamIconSize.LG)
                        )
                    }
                }
            }
            
            // Destination Name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaceDreamDestinationCard.Padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamTextPrimary
                )
            }
        }
    }
}

// Recent Search Item Component
@Composable
fun PaceDreamRecentSearchItem(
    location: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(PaceDreamRecentSearchItem.Height)
            .padding(PaceDreamSpacing.XS),
        colors = CardDefaults.cardColors(containerColor = PaceDreamGray100),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRecentSearchItem.CornerRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamRecentSearchItem.Padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = PaceDreamTextSecondary,
                modifier = Modifier.size(PaceDreamRecentSearchItem.IconSize)
            )
            
            Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
            
            Text(
                text = location,
                style = PaceDreamTypography.Callout,
                color = PaceDreamTextPrimary
            )
        }
    }
}

// Loading Shimmer Component
@Composable
fun PaceDreamShimmerCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .height(PaceDreamPropertyCard.ImageHeight + 120.dp)
            .padding(PaceDreamSpacing.XS),
        colors = CardDefaults.cardColors(containerColor = PaceDreamGray100),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamPropertyCard.CornerRadius)
    ) {
        Column {
            // Shimmer Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamPropertyCard.ImageHeight)
                    .background(PaceDreamGray200)
                    .shimmerEffect()
            )
            
            // Shimmer Content
            Column(
                modifier = Modifier.padding(PaceDreamPropertyCard.ContentPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(PaceDreamGray200)
                        .shimmerEffect()
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(PaceDreamGray200)
                        .shimmerEffect()
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(16.dp)
                            .background(PaceDreamGray200)
                            .shimmerEffect()
                    )
                    
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(16.dp)
                            .background(PaceDreamGray200)
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}

// Empty State Component
@Composable
fun PaceDreamEmptyState(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Search,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(durationMillis = 220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(PaceDreamEmptyState.Padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamTextTertiary,
                modifier = Modifier.size(PaceDreamEmptyState.IconSize)
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Text(
                text = title,
                style = PaceDreamTypography.Title3,
                color = PaceDreamTextPrimary
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            Text(
                text = description,
                style = PaceDreamTypography.Body,
                color = PaceDreamTextSecondary
            )
            
            actionText?.let { text ->
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                
                Button(
                    onClick = { onActionClick?.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamPrimary)
                ) {
                    Text(
                        text = text,
                        style = PaceDreamTypography.Headline,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Error State Component
@Composable
fun PaceDreamErrorState(
    title: String,
    description: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(durationMillis = 220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(PaceDreamErrorState.Padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = PaceDreamError,
                modifier = Modifier.size(PaceDreamErrorState.IconSize)
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Text(
                text = title,
                style = PaceDreamTypography.Title3,
                color = PaceDreamTextPrimary
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            Text(
                text = description,
                style = PaceDreamTypography.Body,
                color = PaceDreamTextSecondary
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Button(
                onClick = onRetryClick,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamPrimary)
            ) {
                Text(
                    text = "Try Again",
                    style = PaceDreamTypography.Headline,
                    color = Color.White
                )
            }
        }
    }
}

// Loading State Component
@Composable
fun PaceDreamLoadingState(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(durationMillis = 180)) + scaleIn(initialScale = 0.98f, animationSpec = tween(180)),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(com.pacedream.common.composables.theme.PaceDreamLoadingState.Padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = PaceDreamPrimary,
                modifier = Modifier.size(com.pacedream.common.composables.theme.PaceDreamLoadingState.IconSize)
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Text(
                text = message,
                style = PaceDreamTypography.Body,
                color = PaceDreamTextSecondary
            )
        }
    }
}

// Category Pill with Resource ID (for use with animated components)
@Composable
fun PaceDreamCategoryPillSimple(
    title: String,
    iconRes: Int,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(PaceDreamCategoryPill.Height)
            .clip(RoundedCornerShape(PaceDreamCategoryPill.CornerRadius)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PaceDreamPrimary else PaceDreamGray100
        ),
        contentPadding = PaceDreamCategoryPill.Padding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = null,
                tint = if (isSelected) Color.White else PaceDreamTextSecondary,
                modifier = Modifier.size(PaceDreamCategoryPill.IconSize)
            )
            
            Text(
                text = title,
                style = PaceDreamTypography.Caption,
                color = if (isSelected) Color.White else PaceDreamTextSecondary
            )
        }
    }
}
