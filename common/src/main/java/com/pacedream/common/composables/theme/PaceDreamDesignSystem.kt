package com.pacedream.common.composables.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PaceDream Design System
 * Comprehensive design tokens matching iOS app design
 */

object PaceDreamDesignSystem {
    val Spacing = PaceDreamSpacing
    val Radius = PaceDreamRadius
    val IconSize = PaceDreamIconSize
    val Elevation = PaceDreamElevation
    val Typography = PaceDreamTypography
    val Colors = PaceDreamColors
}

// Spacing System
object PaceDreamSpacing {
    val XS = 4.dp
    val SM = 8.dp
    val MD = 16.dp
    val LG = 24.dp
    val XL = 32.dp
    val XXL = 48.dp
    val XXXL = 64.dp
}

// Corner Radius System
object PaceDreamRadius {
    val XS = 4.dp
    val SM = 8.dp
    val MD = 12.dp
    val LG = 16.dp
    val XL = 20.dp
    val XXL = 24.dp
    val Round = 50.dp
}

// Icon Size System
object PaceDreamIconSize {
    val XS = 12.dp
    val SM = 16.dp
    val MD = 20.dp
    val LG = 24.dp
    val XL = 32.dp
    val XXL = 48.dp
    val XXXL = 64.dp
}

// Component Dimensions
object PaceDreamSearchBar {
    val Height = 56.dp
    val CornerRadius = PaceDreamRadius.MD
    val Padding = PaceDreamSpacing.SM
    val IconSize = PaceDreamIconSize.MD
}

object PaceDreamMetricCard {
    val MinHeight = 120.dp
    val Elevation = 4.dp
    val CornerRadius = PaceDreamRadius.MD
    val Padding = PaceDreamSpacing.MD
    val IconSize = PaceDreamIconSize.LG
}

object PaceDreamCategoryPill {
    val Height = 40.dp
    val CornerRadius = PaceDreamRadius.Round
    val Padding = PaddingValues(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM)
    val IconSize = PaceDreamIconSize.SM
}

object PaceDreamPropertyCard {
    val Elevation = 6.dp
    val CornerRadius = PaceDreamRadius.LG
    val ImageHeight = 180.dp
    val ContentPadding = PaddingValues(PaceDreamSpacing.MD)
}

object PaceDreamDestinationCard {
    val Width = 200.dp
    val Height = 120.dp
    val ImageHeight = 80.dp
    val CornerRadius = PaceDreamRadius.MD
    val Padding = PaddingValues(PaceDreamSpacing.MD)
}

object PaceDreamRecentSearchItem {
    val Height = 48.dp
    val CornerRadius = PaceDreamRadius.MD
    val Padding = PaddingValues(PaceDreamSpacing.MD)
    val IconSize = PaceDreamIconSize.SM
}

object PaceDreamEmptyState {
    val Padding = PaceDreamSpacing.XXXL
    val IconSize = PaceDreamIconSize.XXXL
}

object PaceDreamErrorState {
    val Padding = PaceDreamSpacing.XXXL
    val IconSize = PaceDreamIconSize.XXXL
}

object PaceDreamLoadingState {
    val Padding = PaceDreamSpacing.XXXL
    val IconSize = PaceDreamIconSize.XXL
}

// Typography System
object PaceDreamTypography {
    val LargeTitle = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp
    )
    
    val Title1 = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 34.sp
    )
    
    val Title2 = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp
    )
    
    val Title3 = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp
    )
    
    val Headline = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp
    )
    
    val Body = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp
    )
    
    val Callout = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    )
    
    val Subheadline = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    )
    
    val Caption = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp
    )
}

// Elevation System
object PaceDreamElevation {
    val None = 0.dp
    val XS = 1.dp
    val SM = 2.dp
    val MD = 4.dp
    val LG = 8.dp
    val XL = 12.dp
    val XXL = 16.dp
}

// Button Heights
object PaceDreamButtonHeight {
    val SM = 32.dp
    val MD = 40.dp
    val LG = 48.dp
    val XL = 56.dp
}

// Animation Durations
object PaceDreamAnimationDuration {
    val FAST = 150
    val SHORT = 250
    val MEDIUM = 500
    val NORMAL = 300
    val LONG = 800
    val SLOW = 1000
    val VERY_SLOW = 2000
}

// Animation Easings
object PaceDreamEasing {
    val EaseOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val EaseInOut = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
    val Sharp = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)
}
