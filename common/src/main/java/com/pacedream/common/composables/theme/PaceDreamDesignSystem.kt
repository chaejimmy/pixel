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
 * PaceDream Design System - iOS 26 Liquid Glass Aligned
 *
 * Design tokens mapped from Apple's Human Interface Guidelines (iOS 26)
 * with Liquid Glass design language adaptations for Android/Compose.
 */

object PaceDreamDesignSystem {
    val Spacing get() = com.pacedream.common.composables.theme.PaceDreamSpacing
    val Radius get() = com.pacedream.common.composables.theme.PaceDreamRadius
    val IconSize get() = com.pacedream.common.composables.theme.PaceDreamIconSize
    val Elevation get() = com.pacedream.common.composables.theme.PaceDreamElevation
    val Typography get() = com.pacedream.common.composables.theme.PaceDreamTypography
    val Colors get() = com.pacedream.common.composables.theme.PaceDreamColors
    val Glass get() = com.pacedream.common.composables.theme.PaceDreamGlass
}

/**
 * Extended color palette for PaceDreamDesignSystem
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

    val SurfaceVariant = Gray200
    val OnSurfaceVariant = Gray600
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF410002)
}

// ============================================================================
// Spacing System - iOS 26 HIG aligned
// Based on iOS standard 4pt/8pt grid with semantic naming
// ============================================================================
object PaceDreamSpacing {
    val XXS = 2.dp     // Hairline spacing
    val XS = 4.dp      // Minimal spacing (icon gaps, tight elements)
    val SM = 8.dp      // Small spacing (related elements, list item internal)
    val MD = 16.dp     // Medium spacing (standard content padding, iOS default margin)
    val LG = 20.dp     // Large spacing (section headers, grouped content)
    val XL = 24.dp     // Extra large (between sections)
    val XXL = 32.dp    // Section separation
    val XXXL = 40.dp   // Major section breaks
    val Page = 48.dp   // Page-level vertical spacing
}

// ============================================================================
// Corner Radius System - iOS 26 Liquid Glass aligned
// Uses continuous corner curves (squircle) matching iOS
// ============================================================================
object PaceDreamRadius {
    val XS = 6.dp      // Small elements (badges, tags)
    val SM = 8.dp      // Buttons, chips, small cards
    val MD = 12.dp     // Standard cards, search bars, inputs
    val LG = 16.dp     // Large cards, sheets, popovers
    val XL = 20.dp     // Modals, bottom sheets
    val XXL = 24.dp    // Hero cards, large containers
    val Round = 9999.dp // Pills, capsules, avatars (effectively full-round)
}

// ============================================================================
// Icon Size System - iOS 26 HIG SF Symbols sizes
// ============================================================================
object PaceDreamIconSize {
    val XS = 14.dp   // Extra small (inline decorative)
    val SM = 17.dp   // Small (list item accessories, matches iOS body text height)
    val MD = 22.dp   // Medium / standard (nav bar, tab bar icons)
    val LG = 28.dp   // Large (prominent actions, card icons)
    val XL = 36.dp   // Extra large (featured, hero areas)
    val XXL = 48.dp  // Empty state illustrations
    val XXXL = 64.dp // Landing page illustrations
}

// ============================================================================
// Liquid Glass Design Tokens
// Translucent materials and blur properties for glass-like surfaces
// ============================================================================
object PaceDreamGlass {
    // Glass material opacity levels
    val RegularAlpha = 0.72f     // Standard glass surface
    val ThinAlpha = 0.55f        // Subtle glass (floating toolbars)
    val UltraThinAlpha = 0.40f   // Very subtle glass overlay
    val ThickAlpha = 0.85f       // High emphasis glass (nav bars)

    // Glass blur radius (for BackdropFilter on supported APIs)
    val BlurRegular = 20.dp
    val BlurThin = 12.dp
    val BlurThick = 32.dp

    // Glass border properties
    val BorderWidth = 0.5.dp     // Subtle glass edge highlight
    val BorderAlpha = 0.20f      // Border opacity

    // Glass specular highlight
    val HighlightAlpha = 0.40f
    val HighlightOffset = 1.dp   // Inner highlight offset

    // Container merge spacing (when glass elements merge like water droplets)
    val MergeSpacing = 8.dp

    // Glass corner radii (concentric - child radius = parent radius - padding)
    val CardRadius = PaceDreamRadius.XXL   // 24dp
    val ToolbarRadius = 22.dp
    val PillRadius = PaceDreamRadius.Round
    val ButtonRadius = PaceDreamRadius.MD  // 12dp
}

// ============================================================================
// Component Dimensions - iOS 26 HIG aligned
// ============================================================================
object PaceDreamSearchBar {
    val Height = 36.dp  // iOS search bar compact height
    val ExpandedHeight = 44.dp // Expanded search bar
    val CornerRadius = PaceDreamRadius.SM // 8dp (iOS uses rounded rect 8 for search)
    val Padding = PaceDreamSpacing.SM
    val HorizontalPadding = PaceDreamSpacing.SM
    val IconSize = PaceDreamIconSize.SM
}

object PaceDreamMetricCard {
    val MinHeight = 120.dp
    val Elevation = 0.dp  // iOS 26: no shadow, use glass material instead
    val CornerRadius = PaceDreamRadius.LG // 16dp
    val Padding = PaddingValues(PaceDreamSpacing.MD)
    val IconSize = PaceDreamIconSize.LG
}

object PaceDreamCategoryPill {
    val Height = 34.dp  // iOS compact pill height
    val CornerRadius = PaceDreamRadius.Round
    val Padding = PaddingValues(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM)
    val IconSize = PaceDreamIconSize.SM
}

object PaceDreamPropertyCard {
    val Elevation = 0.dp  // iOS 26: no shadow, use glass material instead
    val CornerRadius = PaceDreamRadius.LG // 16dp concentric
    val ImageHeight = 200.dp
    val ContentPadding = PaddingValues(
        horizontal = PaceDreamSpacing.MD,
        vertical = PaceDreamSpacing.SM
    )
}

object PaceDreamDestinationCard {
    val Width = 200.dp
    val Height = 120.dp
    val ImageHeight = 80.dp
    val CornerRadius = PaceDreamRadius.MD // 12dp
    val Padding = PaddingValues(PaceDreamSpacing.SM)
}

object PaceDreamRecentSearchItem {
    val Height = 44.dp  // iOS standard tap target height
    val CornerRadius = PaceDreamRadius.SM // 8dp
    val Padding = PaddingValues(
        horizontal = PaceDreamSpacing.MD,
        vertical = PaceDreamSpacing.SM
    )
    val IconSize = PaceDreamIconSize.SM
}

object PaceDreamEmptyState {
    val Padding = PaceDreamSpacing.Page
    val IconSize = PaceDreamIconSize.XXXL
}

object PaceDreamErrorState {
    val Padding = PaceDreamSpacing.Page
    val IconSize = PaceDreamIconSize.XXXL
}

object PaceDreamLoadingState {
    val Padding = PaceDreamSpacing.Page
    val IconSize = PaceDreamIconSize.XXL
}

// ============================================================================
// Typography System - iOS 26 HIG named styles
// These use the exact iOS point sizes for cross-platform consistency
// ============================================================================
object PaceDreamTypography {
    // Large Title: 34sp Bold
    val LargeTitle = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 41.sp,
        letterSpacing = 0.37.sp
    )

    // Title 1: 28sp Bold
    val Title1 = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 34.sp,
        letterSpacing = 0.36.sp
    )

    // Title 2: 22sp Bold
    val Title2 = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp,
        letterSpacing = 0.35.sp
    )

    // Title 3: 20sp Regular
    val Title3 = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 25.sp,
        letterSpacing = 0.38.sp
    )

    // Headline: 17sp Semi-Bold
    val Headline = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    )

    // Body: 17sp Regular (iOS default text size)
    val Body = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    )

    // Callout: 16sp Regular
    val Callout = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp,
        letterSpacing = (-0.32).sp
    )

    // Callout Bold: 16sp Bold
    val CalloutBold = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 21.sp,
        letterSpacing = (-0.32).sp
    )

    // Subheadline: 15sp Regular
    val Subheadline = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    )

    // Footnote: 13sp Regular
    val Footnote = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        letterSpacing = (-0.08).sp
    )

    // Caption 1: 12sp Regular
    val Caption = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )

    // Caption 2: 11sp Regular
    val Caption2 = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 13.sp,
        letterSpacing = 0.07.sp
    )

    // Button: 17sp Semi-Bold (matches iOS button label)
    val Button = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    )
}

// ============================================================================
// Elevation System - iOS 26 Liquid Glass: minimal shadows, use material instead
// ============================================================================
object PaceDreamElevation {
    val None = 0.dp
    val XS = 0.5.dp   // Subtle, barely visible
    val SM = 1.dp      // Cards resting on surface
    val MD = 2.dp      // Raised elements (floating action buttons)
    val LG = 4.dp      // Popovers, dropdowns
    val XL = 8.dp      // Modals, dialogs
    val XXL = 12.dp    // Reserved for maximum emphasis
}

// ============================================================================
// Button Heights - iOS 26 HIG tap targets
// Minimum 44dp for accessibility (Apple standard)
// ============================================================================
object PaceDreamButtonHeight {
    val SM = 34.dp   // Compact buttons (toolbar actions)
    val MD = 44.dp   // Standard iOS tap target (recommended minimum)
    val LG = 50.dp   // Large / primary actions
    val XL = 56.dp   // Hero buttons (full-width CTA)
}

// ============================================================================
// Animation Durations - iOS 26 Liquid Glass fluid transitions
// ============================================================================
object PaceDreamAnimationDuration {
    val FAST = 150     // Micro-interactions (button press, toggle)
    val SHORT = 250    // Standard transitions (page slides)
    val NORMAL = 350   // Default iOS spring duration
    val MEDIUM = 500   // Glass morph transitions
    val LONG = 800     // Complex reveal animations
    val SLOW = 1000    // Splash / onboarding
    val VERY_SLOW = 2000
}

// ============================================================================
// Animation Easings - iOS 26 spring-based curves
// ============================================================================
object PaceDreamEasing {
    // iOS default ease-out (system animations)
    val EaseOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    // iOS standard ease-in-out
    val EaseInOut = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
    // iOS interactive / sharp (responsive to touch)
    val Sharp = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)
    // iOS spring-like bounce approximation
    val Spring = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)
    // iOS deceleration curve (for scroll settling)
    val Decelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
}
