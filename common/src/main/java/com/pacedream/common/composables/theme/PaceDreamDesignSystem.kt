package com.pacedream.common.composables.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em

/**
 * PaceDream Design System
 * Comprehensive design tokens matching iOS app design
 */

object PaceDreamDesignSystem {
    // Use fully qualified references to avoid forward reference issues
    val Spacing get() = com.pacedream.common.composables.theme.PaceDreamSpacing
    val Radius get() = com.pacedream.common.composables.theme.PaceDreamRadius
    val IconSize get() = com.pacedream.common.composables.theme.PaceDreamIconSize
    val Elevation get() = com.pacedream.common.composables.theme.PaceDreamElevation
    val Typography get() = com.pacedream.common.composables.theme.PaceDreamTypography
    val Colors get() = com.pacedream.common.composables.theme.PaceDreamColors
}

/**
 * Extended color palette for PaceDreamDesignSystem
 * Provides additional colors needed by components
 */
object PaceDreamDesignSystemColors {
    val Primary = com.pacedream.common.composables.theme.PaceDreamColors.Primary
    val PrimaryLight = com.pacedream.common.composables.theme.PaceDreamColors.PrimaryLight
    val PrimaryDark = com.pacedream.common.composables.theme.PaceDreamColors.PrimaryDark
    val Secondary = com.pacedream.common.composables.theme.PaceDreamColors.Secondary
    val Accent = com.pacedream.common.composables.theme.PaceDreamColors.Accent
    val Background = com.pacedream.common.composables.theme.PaceDreamColors.Background
    val Surface = com.pacedream.common.composables.theme.PaceDreamColors.Surface
    val Card = com.pacedream.common.composables.theme.PaceDreamColors.Card
    val TextPrimary = com.pacedream.common.composables.theme.PaceDreamColors.TextPrimary
    val TextSecondary = com.pacedream.common.composables.theme.PaceDreamColors.TextSecondary
    val TextTertiary = com.pacedream.common.composables.theme.PaceDreamColors.TextTertiary
    val Success = com.pacedream.common.composables.theme.PaceDreamColors.Success
    val Warning = com.pacedream.common.composables.theme.PaceDreamColors.Warning
    val Error = com.pacedream.common.composables.theme.PaceDreamColors.Error
    val Info = com.pacedream.common.composables.theme.PaceDreamColors.Info
    val Gray50 = com.pacedream.common.composables.theme.PaceDreamColors.Gray50
    val Gray100 = com.pacedream.common.composables.theme.PaceDreamColors.Gray100
    val Gray200 = com.pacedream.common.composables.theme.PaceDreamColors.Gray200
    val Gray300 = com.pacedream.common.composables.theme.PaceDreamColors.Gray300
    val Gray400 = com.pacedream.common.composables.theme.PaceDreamColors.Gray400
    val Gray500 = com.pacedream.common.composables.theme.PaceDreamColors.Gray500
    val Gray600 = com.pacedream.common.composables.theme.PaceDreamColors.Gray600
    val Gray700 = com.pacedream.common.composables.theme.PaceDreamColors.Gray700
    val Gray800 = com.pacedream.common.composables.theme.PaceDreamColors.Gray800
    val Gray900 = com.pacedream.common.composables.theme.PaceDreamColors.Gray900
    
    // Additional colors used by image components
    val SurfaceVariant = Gray200
    val OnSurfaceVariant = Gray600
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF410002)
}

// Spacing System
// Enhanced with modern spacing scale for better visual hierarchy
object PaceDreamSpacing {
    val XS = 4.dp      // Minimal spacing (icons, tight elements)
    val SM = 8.dp      // Small spacing (related elements)
    val MD = 16.dp     // Medium spacing (standard padding)
    val LG = 24.dp     // Large spacing (section separation)
    val XL = 32.dp     // Extra large spacing (major sections)
    val XXL = 48.dp    // Extra extra large (page margins)
    val XXXL = 64.dp   // Maximum spacing (full page sections)
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
// Enhanced with modern icon sizing for better visibility and touch targets
object PaceDreamIconSize {
    val XS = 14.dp   // Extra small icons (inline, compact)
    val SM = 18.dp   // Small icons (list items, secondary)
    val MD = 24.dp   // Medium icons (standard, recommended)
    val LG = 28.dp   // Large icons (prominent, cards)
    val XL = 36.dp   // Extra large (featured, hero)
    val XXL = 48.dp  // Extra extra large (empty states)
    val XXXL = 64.dp // Maximum (landing pages, illustrations)
}

// Component Dimensions
// Enhanced with modern, professional sizing for better UX
object PaceDreamSearchBar {
    val Height = 56.dp  // Optimal touch target height
    val CornerRadius = PaceDreamRadius.MD
    val Padding = PaceDreamSpacing.SM  // Internal padding
    val HorizontalPadding = PaceDreamSpacing.MD  // Horizontal padding for content
    val IconSize = PaceDreamIconSize.MD
}

object PaceDreamMetricCard {
    val MinHeight = 128.dp  // Increased for better visual balance
    val Elevation = 4.dp
    val CornerRadius = PaceDreamRadius.MD
    val Padding = PaddingValues(PaceDreamSpacing.MD)
    val IconSize = PaceDreamIconSize.LG
}

object PaceDreamCategoryPill {
    val Height = 44.dp  // Better touch target (minimum 44dp recommended)
    val CornerRadius = PaceDreamRadius.Round
    val Padding = PaddingValues(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.MD)
    val IconSize = PaceDreamIconSize.MD  // Slightly larger for better visibility
}

object PaceDreamPropertyCard {
    val Elevation = 6.dp
    val CornerRadius = PaceDreamRadius.LG
    val ImageHeight = 200.dp  // Increased for better image display
    val ContentPadding = PaddingValues(
        horizontal = PaceDreamSpacing.MD,
        vertical = PaceDreamSpacing.MD
    )
}

object PaceDreamDestinationCard {
    val Width = 200.dp
    val Height = 120.dp
    val ImageHeight = 80.dp
    val CornerRadius = PaceDreamRadius.MD
    val Padding = PaddingValues(PaceDreamSpacing.MD)
}

object PaceDreamRecentSearchItem {
    val Height = 52.dp  // Better touch target
    val CornerRadius = PaceDreamRadius.MD
    val Padding = PaddingValues(
        horizontal = PaceDreamSpacing.MD,
        vertical = PaceDreamSpacing.MD
    )
    val IconSize = PaceDreamIconSize.MD  // Better visibility
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
// Enhanced with modern, professional typography with proper letter spacing and line heights
object PaceDreamTypography {
    val LargeTitle = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 44.sp,
        letterSpacing = (-0.02).em
    )
    
    val Title1 = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 38.sp,
        letterSpacing = (-0.01).em
    )
    
    val Title2 = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 32.sp,
        letterSpacing = 0.em
    )
    
    val Title3 = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp,
        letterSpacing = 0.01.em
    )
    
    val Headline = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
        letterSpacing = 0.01.em
    )
    
    val Body = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 26.sp,
        letterSpacing = 0.01.em
    )
    
    val Callout = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
        letterSpacing = 0.01.em
    )
    
    val CalloutBold = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp,
        letterSpacing = 0.01.em
    )
    
    val Subheadline = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
        letterSpacing = 0.01.em
    )
    
    val Caption = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        letterSpacing = 0.01.em
    )
    
    val Button = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
        letterSpacing = 0.02.em
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
// Enhanced with modern touch target sizes (minimum 44dp for accessibility)
object PaceDreamButtonHeight {
    val SM = 36.dp   // Small buttons (compact UI)
    val MD = 48.dp   // Standard buttons (recommended minimum)
    val LG = 56.dp   // Large buttons (primary actions)
    val XL = 64.dp   // Extra large (hero buttons)
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
