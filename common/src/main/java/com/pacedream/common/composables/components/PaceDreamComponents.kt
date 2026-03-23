package com.pacedream.common.composables.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pacedream.common.composables.shimmerEffect
import com.pacedream.common.composables.theme.*

/**
 * iOS 26 Liquid Glass Components for PaceDream App
 *
 * Components follow Apple's Liquid Glass design language:
 * - Translucent surfaces with subtle borders
 * - Minimal elevation, prefer material over shadow
 * - Content-first: UI recedes, content extends to edges
 * - Floating elements that adapt to context
 * - 44dp minimum touch targets
 * - Concentric corner radii
 */

// ============================================================================
// Glass Surface Modifier - Reusable Liquid Glass appearance
// ============================================================================
@Composable
fun Modifier.glassSurface(
    shape: RoundedCornerShape = RoundedCornerShape(PaceDreamRadius.LG),
    alpha: Float = PaceDreamGlass.RegularAlpha,
): Modifier {
    val glassTheme = LocalGlassTheme.current
    val surfaceColor = if (glassTheme.isDark) {
        PaceDreamColors.GlassSurfaceDark
    } else {
        PaceDreamColors.GlassSurface
    }
    val borderColor = if (glassTheme.isDark) {
        PaceDreamColors.GlassBorderDark
    } else {
        PaceDreamColors.GlassBorder
    }
    return this
        .clip(shape)
        .background(surfaceColor.copy(alpha = alpha))
        .border(
            width = PaceDreamGlass.BorderWidth,
            color = borderColor,
            shape = shape
        )
}

// ============================================================================
// Hero Header - Floating glass header with gradient
// ============================================================================
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
                        PaceDreamPrimary.copy(alpha = 0.88f)
                    )
                ),
                shape = RoundedCornerShape(
                    bottomStart = PaceDreamRadius.LG,
                    bottomEnd = PaceDreamRadius.LG
                )
            )
            .padding(
                start = PaceDreamSpacing.MD,
                end = PaceDreamSpacing.MD,
                top = PaceDreamSpacing.LG,
                bottom = PaceDreamSpacing.MD
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Title2,
                    color = Color.White
                )
                subtitle?.let {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    Text(
                        text = it,
                        style = PaceDreamTypography.Subheadline,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                }
            }

            // Floating glass notification button
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier
                    .size(PaceDreamButtonHeight.SM)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }
        }
    }
}

// ============================================================================
// Search Bar - Matched to iOS DesignTokens.Sizes.searchBarHeight (48dp)
// iOS uses systemBackground with 1pt shadow, 12pt corner radius
// ============================================================================
@Composable
fun PaceDreamSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    placeholder: String = "Where to? Anywhere \u2022 Any week \u2022 Add guests",
    modifier: Modifier = Modifier
) {
    val searchShape = RoundedCornerShape(PaceDreamSearchBar.CornerRadius)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PaceDreamSearchBar.ExpandedHeight)
            .clip(searchShape)
            .background(PaceDreamColors.Background)
            .border(
                width = 0.5.dp,
                color = PaceDreamColors.Border.copy(alpha = 0.5f),
                shape = searchShape
            )
            .padding(horizontal = PaceDreamSpacing.MD),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = PaceDreamIcons.Search,
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
                    color = PaceDreamTextTertiary,
                    style = PaceDreamTypography.Callout
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
            textStyle = PaceDreamTypography.Callout,
            singleLine = true
        )

        IconButton(
            onClick = onFilterClick,
            modifier = Modifier.size(PaceDreamButtonHeight.SM)
        ) {
            Icon(
                imageVector = PaceDreamIcons.FilterList,
                contentDescription = "Filter",
                tint = PaceDreamPrimary,
                modifier = Modifier.size(PaceDreamSearchBar.IconSize)
            )
        }
    }
}

// ============================================================================
// Metric Card - Glass material stat card
// ============================================================================
@Composable
fun PaceDreamMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color = PaceDreamPrimary,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(PaceDreamMetricCard.CornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PaceDreamMetricCard.MinHeight)
            .padding(PaceDreamSpacing.XS)
            .glassSurface(shape = cardShape)
            .padding(PaceDreamSpacing.MD)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

            Text(
                text = title,
                style = PaceDreamTypography.Caption,
                color = PaceDreamTextSecondary
            )
        }
    }
}

// ============================================================================
// Category Pill - iOS 26 compact pill with glass material
// ============================================================================
@Composable
fun PaceDreamCategoryPill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(PaceDreamCategoryPill.CornerRadius)

    Button(
        onClick = onClick,
        modifier = modifier
            .height(PaceDreamCategoryPill.Height),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PaceDreamPrimary else iOSSystemFill
        ),
        contentPadding = PaceDreamCategoryPill.Padding,
        shape = pillShape,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
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
                style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) Color.White else PaceDreamTextPrimary
            )
        }
    }
}

// ============================================================================
// Section Header - iOS 26 bold left-aligned typography
// ============================================================================
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

// ============================================================================
// Property Card - Glass material card with concentric radii
// ============================================================================
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
    val cardShape = RoundedCornerShape(PaceDreamPropertyCard.CornerRadius)

    Card(
        onClick = onClick,
        modifier = modifier
            .width(200.dp)
            .padding(PaceDreamSpacing.XS),
        colors = CardDefaults.cardColors(containerColor = PaceDreamCard),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM),
        shape = cardShape
    ) {
        Column {
            // Property Image
            PaceDreamPropertyImage(
                imageUrl = imageUrl,
                contentDescription = "Property image: $title",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamPropertyCard.ImageHeight)
            )

            // Property Details with iOS-style compact spacing
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
                        imageVector = PaceDreamIcons.LocationOn,
                        contentDescription = null,
                        tint = PaceDreamTextSecondary,
                        modifier = Modifier.size(PaceDreamIconSize.XS)
                    )

                    Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))

                    Text(
                        text = location,
                        style = PaceDreamTypography.Footnote,
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
                            imageVector = PaceDreamIcons.Star,
                            contentDescription = null,
                            tint = PaceDreamColors.StarRating,
                            modifier = Modifier.size(PaceDreamIconSize.XS)
                        )

                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XXS))

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

// ============================================================================
// ============================================================================
// City Fallback Images - Maps city names to relevant Unsplash images
// ============================================================================
private fun destinationFallbackImage(name: String): String = when (name.lowercase().trim()) {
    "new york", "manhattan", "brooklyn" -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=400&q=80"
    "los angeles" -> "https://images.unsplash.com/photo-1534190760961-74e8c1c5c3da?w=400&q=80"
    "san francisco" -> "https://images.unsplash.com/photo-1501594907352-04cda38ebc29?w=400&q=80"
    "chicago" -> "https://images.unsplash.com/photo-1494522855154-9297ac14b55f?w=400&q=80"
    "miami" -> "https://images.unsplash.com/photo-1533106497176-45ae19e68ba2?w=400&q=80"
    "seattle" -> "https://images.unsplash.com/photo-1502175353174-a7a70e73b4c3?w=400&q=80"
    "austin" -> "https://images.unsplash.com/photo-1531218150217-54595bc2b934?w=400&q=80"
    "denver" -> "https://images.unsplash.com/photo-1619856699906-09e1f4ef478b?w=400&q=80"
    "boston" -> "https://images.unsplash.com/photo-1501979376754-1d3b25f22a4e?w=400&q=80"
    "honolulu" -> "https://images.unsplash.com/photo-1507876466758-bc54f384809c?w=400&q=80"
    "maui" -> "https://images.unsplash.com/photo-1542259009477-d625272157b7?w=400&q=80"
    "grand canyon" -> "https://images.unsplash.com/photo-1474044159687-1ee9f3a51722?w=400&q=80"
    "nashville" -> "https://images.unsplash.com/photo-1545419913-775e2e168cd0?w=400&q=80"
    "portland" -> "https://images.unsplash.com/photo-1507245338956-79a3a4b41583?w=400&q=80"
    "san diego" -> "https://images.unsplash.com/photo-1538097304804-2a1b932466a9?w=400&q=80"
    "atlanta" -> "https://images.unsplash.com/photo-1575917649705-5b59aaa12e6b?w=400&q=80"
    "washington", "washington dc" -> "https://images.unsplash.com/photo-1501466044931-62695aada8e9?w=400&q=80"
    else -> "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=400&q=80"
}

// ============================================================================
// Destination Card - Compact glass card
// ============================================================================
@Composable
fun PaceDreamDestinationCard(
    name: String,
    imageUrl: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(PaceDreamDestinationCard.CornerRadius)

    Card(
        onClick = onClick,
        modifier = modifier
            .width(PaceDreamDestinationCard.Width)
            .height(PaceDreamDestinationCard.Height)
            .padding(PaceDreamSpacing.XS),
        colors = CardDefaults.cardColors(containerColor = PaceDreamCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = cardShape
    ) {
        Column {
            // Destination Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamDestinationCard.ImageHeight)
                    .background(PaceDreamGray100)
            ) {
                val resolvedUrl = imageUrl?.takeIf { it.isNotBlank() }
                    ?: destinationFallbackImage(name)
                AsyncImage(
                    model = resolvedUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
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
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamTextPrimary
                )
            }
        }
    }
}

// ============================================================================
// Recent Search Item - iOS-style list row
// ============================================================================
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
        colors = CardDefaults.cardColors(containerColor = PaceDreamGray50),
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
                imageVector = PaceDreamIcons.LocationOn,
                contentDescription = null,
                tint = PaceDreamTextSecondary,
                modifier = Modifier.size(PaceDreamRecentSearchItem.IconSize)
            )

            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))

            Text(
                text = location,
                style = PaceDreamTypography.Body,
                color = PaceDreamTextPrimary
            )
        }
    }
}

// ============================================================================
// Shimmer Loading Card
// ============================================================================
@Composable
fun PaceDreamShimmerCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .height(PaceDreamPropertyCard.ImageHeight + 100.dp)
            .padding(PaceDreamSpacing.XS),
        colors = CardDefaults.cardColors(containerColor = PaceDreamGray50),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamPropertyCard.CornerRadius)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamPropertyCard.ImageHeight)
                    .background(PaceDreamGray100)
                    .shimmerEffect()
            )

            Column(
                modifier = Modifier.padding(PaceDreamPropertyCard.ContentPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(17.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.XS))
                        .background(PaceDreamGray100)
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(13.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.XS))
                        .background(PaceDreamGray100)
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
                            .height(15.dp)
                            .clip(RoundedCornerShape(PaceDreamRadius.XS))
                            .background(PaceDreamGray100)
                            .shimmerEffect()
                    )

                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(15.dp)
                            .clip(RoundedCornerShape(PaceDreamRadius.XS))
                            .background(PaceDreamGray100)
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}

// ============================================================================
// Empty State - Centered with iOS 26 typography
// ============================================================================
@Composable
fun PaceDreamEmptyState(
    title: String,
    description: String,
    icon: ImageVector = PaceDreamIcons.Search,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(durationMillis = PaceDreamAnimationDuration.SHORT)) +
                scaleIn(initialScale = 0.98f, animationSpec = tween(PaceDreamAnimationDuration.SHORT)),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.XL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamTextTertiary,
                modifier = Modifier.size(PaceDreamIconSize.XXL)
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            Text(
                text = title,
                style = PaceDreamTypography.Title3,
                color = PaceDreamTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = description,
                style = PaceDreamTypography.Body,
                color = PaceDreamTextSecondary,
                textAlign = TextAlign.Center
            )

            actionText?.let { text ->
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

                Button(
                    onClick = { onActionClick?.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamPrimary),
                    modifier = Modifier.height(PaceDreamButtonHeight.MD),
                    shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(
                        horizontal = PaceDreamSpacing.LG,
                        vertical = PaceDreamSpacing.SM2
                    )
                ) {
                    Text(
                        text = text,
                        style = PaceDreamTypography.Button,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ============================================================================
// Error State
// ============================================================================
@Composable
fun PaceDreamErrorState(
    title: String,
    description: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(durationMillis = PaceDreamAnimationDuration.SHORT)) +
                scaleIn(initialScale = 0.98f, animationSpec = tween(PaceDreamAnimationDuration.SHORT)),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(PaceDreamErrorState.Padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = PaceDreamIcons.Error,
                contentDescription = null,
                tint = PaceDreamError,
                modifier = Modifier.size(PaceDreamErrorState.IconSize)
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            Text(
                text = title,
                style = PaceDreamTypography.Title3,
                color = PaceDreamTextPrimary
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = description,
                style = PaceDreamTypography.Body,
                color = PaceDreamTextSecondary,
                lineHeight = PaceDreamTypography.Body.lineHeight
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            Button(
                onClick = onRetryClick,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamPrimary),
                modifier = Modifier.height(PaceDreamButtonHeight.MD),
                shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Try Again",
                    style = PaceDreamTypography.Button,
                    color = Color.White
                )
            }
        }
    }
}

// ============================================================================
// Loading State
// ============================================================================
@Composable
fun PaceDreamLoadingState(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(durationMillis = PaceDreamAnimationDuration.FAST)) +
                scaleIn(initialScale = 0.98f, animationSpec = tween(PaceDreamAnimationDuration.FAST)),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(com.pacedream.common.composables.theme.PaceDreamLoadingState.Padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = PaceDreamPrimary,
                modifier = Modifier.size(com.pacedream.common.composables.theme.PaceDreamLoadingState.IconSize),
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            Text(
                text = message,
                style = PaceDreamTypography.Body,
                color = PaceDreamTextSecondary
            )
        }
    }
}

// ============================================================================
// Category Pill (Resource ID variant)
// ============================================================================
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
        modifier = modifier.height(PaceDreamCategoryPill.Height),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PaceDreamPrimary else iOSSystemFill
        ),
        contentPadding = PaceDreamCategoryPill.Padding,
        shape = RoundedCornerShape(PaceDreamCategoryPill.CornerRadius),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
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
                style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) Color.White else PaceDreamTextPrimary
            )
        }
    }
}
