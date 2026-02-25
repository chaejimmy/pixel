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
import androidx.compose.ui.unit.dp
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
                        PaceDreamPrimary.copy(alpha = 0.85f)
                    )
                ),
                shape = RoundedCornerShape(
                    bottomStart = PaceDreamRadius.XL,
                    bottomEnd = PaceDreamRadius.XL
                )
            )
            .padding(
                horizontal = PaceDreamSpacing.MD,
                vertical = PaceDreamSpacing.XL
            )
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = PaceDreamTypography.Title1,
                        color = Color.White
                    )
                    subtitle?.let {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                        Text(
                            text = it,
                            style = PaceDreamTypography.Body,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // Floating glass notification button
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .size(PaceDreamButtonHeight.MD)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f))
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(PaceDreamIconSize.MD)
                    )
                }
            }
        }
    }
}

// ============================================================================
// Search Bar - iOS 26 compact floating search field
// ============================================================================
@Composable
fun PaceDreamSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    placeholder: String = "Search properties...",
    modifier: Modifier = Modifier
) {
    val searchShape = RoundedCornerShape(PaceDreamSearchBar.CornerRadius)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PaceDreamSearchBar.ExpandedHeight)
            .glassSurface(shape = searchShape)
            .padding(horizontal = PaceDreamSpacing.SM),
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
                    style = PaceDreamTypography.Body
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
            textStyle = PaceDreamTypography.Body,
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
                style = PaceDreamTypography.Footnote,
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
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamPropertyCard.Elevation),
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
                            tint = PaceDreamWarning,
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (imageUrl != null) PaceDreamIcons.Image else PaceDreamIcons.LocationOn,
                        contentDescription = null,
                        tint = PaceDreamTextTertiary,
                        modifier = Modifier.size(PaceDreamIconSize.LG)
                    )
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
                .padding(PaceDreamEmptyState.Padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamTextTertiary,
                modifier = Modifier.size(PaceDreamEmptyState.IconSize)
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

            actionText?.let { text ->
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

                Button(
                    onClick = { onActionClick?.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamPrimary),
                    modifier = Modifier.height(PaceDreamButtonHeight.MD),
                    shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = text,
                        style = PaceDreamTypography.Button,
                        color = Color.White
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
                style = PaceDreamTypography.Footnote,
                color = if (isSelected) Color.White else PaceDreamTextPrimary
            )
        }
    }
}
