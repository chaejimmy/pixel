
package com.pacedream.common.composables.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// iOS 26 System Colors - Light Mode
// Mapped from Apple's HIG system color palette
// ============================================================================
val SystemBlue = Color(0xFF007AFF)
val SystemGreen = Color(0xFF34C759)
val SystemIndigo = Color(0xFF5856D6)
val SystemOrange = Color(0xFFFF9500)
val SystemPink = Color(0xFFFF2D55)
val SystemPurple = Color(0xFFAF52DE)
val SystemRed = Color(0xFFFF3B30)
val SystemTeal = Color(0xFF5AC8FA)
val SystemYellow = Color(0xFFFFCC00)
val SystemCyan = Color(0xFF32ADE6)
val SystemMint = Color(0xFF00C7BE)
val SystemBrown = Color(0xFFA2845E)

// iOS 26 System Colors - Dark Mode
val SystemBlueDark = Color(0xFF0A84FF)
val SystemGreenDark = Color(0xFF30D158)
val SystemIndigoDark = Color(0xFF5E5CE6)
val SystemOrangeDark = Color(0xFFFF9F0A)
val SystemPinkDark = Color(0xFFFF375F)
val SystemPurpleDark = Color(0xFFBF5AF2)
val SystemRedDark = Color(0xFFFF453A)
val SystemTealDark = Color(0xFF64D2FF)
val SystemYellowDark = Color(0xFFFFD60A)
val SystemCyanDark = Color(0xFF64D2FF)
val SystemMintDark = Color(0xFF63E6E2)
val SystemBrownDark = Color(0xFFAC8E68)

// ============================================================================
// iOS 26 Gray Scale - Light Mode
// ============================================================================
val SystemGray = Color(0xFF8E8E93)
val SystemGray2 = Color(0xFFAEAEB2)
val SystemGray3 = Color(0xFFC7C7CC)
val SystemGray4 = Color(0xFFD1D1D6)
val SystemGray5 = Color(0xFFE5E5EA)
val SystemGray6 = Color(0xFFF2F2F7)

// iOS 26 Gray Scale - Dark Mode
val SystemGrayDark = Color(0xFF8E8E93)
val SystemGray2Dark = Color(0xFF636366)
val SystemGray3Dark = Color(0xFF48484A)
val SystemGray4Dark = Color(0xFF3A3A3C)
val SystemGray5Dark = Color(0xFF2C2C2E)
val SystemGray6Dark = Color(0xFF1C1C1E)

// ============================================================================
// iOS 26 Semantic Colors - Light Mode
// ============================================================================
val iOSLabel = Color(0xFF000000)
val iOSSecondaryLabel = Color(0x993C3C43) // #3C3C43 at 60% alpha
val iOSTertiaryLabel = Color(0x4D3C3C43) // #3C3C43 at 30% alpha
val iOSQuaternaryLabel = Color(0x2E3C3C43) // #3C3C43 at 18% alpha

val iOSSystemBackground = Color(0xFFFFFFFF)
val iOSSecondarySystemBackground = Color(0xFFF2F2F7)
val iOSTertiarySystemBackground = Color(0xFFFFFFFF)

val iOSSystemGroupedBackground = Color(0xFFF2F2F7)
val iOSSecondarySystemGroupedBackground = Color(0xFFFFFFFF)
val iOSTertiarySystemGroupedBackground = Color(0xFFF2F2F7)

val iOSSeparator = Color(0x4A3C3C43) // #3C3C43 at 29% alpha
val iOSOpaqueSeparator = Color(0xFFC6C6C8)

val iOSSystemFill = Color(0x33787880) // #787880 at 20% alpha
val iOSSecondarySystemFill = Color(0x29787880) // #787880 at 16% alpha
val iOSTertiarySystemFill = Color(0x1F767680) // #767680 at 12% alpha
val iOSQuaternarySystemFill = Color(0x14747480) // #747480 at 8% alpha

// iOS 26 Semantic Colors - Dark Mode
val iOSLabelDark = Color(0xFFFFFFFF)
val iOSSecondaryLabelDark = Color(0x99EBEBF5) // #EBEBF5 at 60% alpha
val iOSTertiaryLabelDark = Color(0x4DEBEBF5) // #EBEBF5 at 30% alpha
val iOSQuaternaryLabelDark = Color(0x2EEBEBF5) // #EBEBF5 at 18% alpha

val iOSSystemBackgroundDark = Color(0xFF000000)
val iOSSecondarySystemBackgroundDark = Color(0xFF1C1C1E)
val iOSTertiarySystemBackgroundDark = Color(0xFF2C2C2E)

val iOSSystemGroupedBackgroundDark = Color(0xFF000000)
val iOSSecondarySystemGroupedBackgroundDark = Color(0xFF1C1C1E)
val iOSTertiarySystemGroupedBackgroundDark = Color(0xFF2C2C2E)

val iOSSeparatorDark = Color(0x99545458) // #545458 at 60% alpha
val iOSOpaqueSeparatorDark = Color(0xFF38383A)

val iOSSystemFillDark = Color(0x5C787880) // #787880 at 36% alpha
val iOSSecondarySystemFillDark = Color(0x52787880) // #787880 at 32% alpha
val iOSTertiarySystemFillDark = Color(0x3D767680) // #767680 at 24% alpha
val iOSQuaternarySystemFillDark = Color(0x2E747480) // #747480 at 18% alpha

// ============================================================================
// PaceDream Brand Colors - Matched to iOS app (PDDesignSystem.swift)
// Primary: #5527D7, Secondary: #3B82F6, Accent: #7B4DFF
// ============================================================================
val PaceDreamPrimary = Color(0xFF5527D7) // iOS PD.Color.primary
val PaceDreamPrimaryLight = PaceDreamPrimary.copy(alpha = 0.10f)
val PaceDreamPrimaryDark = PaceDreamPrimary.copy(alpha = 0.80f)

val PaceDreamSecondary = Color(0xFF3B82F6) // iOS PD.Color.secondary
val PaceDreamAccent = Color(0xFF7B4DFF) // iOS PD.Color.accent / DesignTokens.Colors.primary

val PaceDreamBackground = iOSSystemBackground // #FFFFFF
val PaceDreamSurface = iOSSecondarySystemBackground // #F2F2F7 (iOS grouped bg)
val PaceDreamCardColor = iOSSecondarySystemGroupedBackground // #FFFFFF
val PaceDreamCard = PaceDreamCardColor

val PaceDreamTextPrimary = Color(0xFF111827) // iOS DesignTokens.Colors.text
val PaceDreamTextSecondary = Color(0xFF6B7280) // iOS DesignTokens.Colors.muted
val PaceDreamTextTertiary = iOSTertiaryLabel // 30% opacity

val PaceDreamSuccess = Color(0xFF10B981) // iOS PD.Color.success
val PaceDreamWarning = Color(0xFFF59E0B) // iOS PD.Color.warning
val PaceDreamError = Color(0xFFEF4444) // iOS PD.Color.error
val PaceDreamInfo = Color(0xFF3B82F6) // iOS PD.Color.info

// iOS 26 Gray Scale (mapped to PaceDream naming)
val PaceDreamGray50 = SystemGray6 // #F2F2F7
val PaceDreamGray100 = SystemGray5 // #E5E5EA
val PaceDreamGray200 = SystemGray4 // #D1D1D6
val PaceDreamGray300 = SystemGray3 // #C7C7CC
val PaceDreamGray400 = SystemGray2 // #AEAEB2
val PaceDreamGray500 = SystemGray // #8E8E93
val PaceDreamGray600 = Color(0xFF636366) // Matches iOS dark gray2
val PaceDreamGray700 = Color(0xFF48484A) // Matches iOS dark gray3
val PaceDreamGray800 = Color(0xFF3A3A3C) // Matches iOS dark gray4
val PaceDreamGray900 = Color(0xFF1C1C1E) // Matches iOS dark gray6

/**
 * PaceDream Colors Namespace - iOS 26 aligned
 */
object PaceDreamColors {
    val Primary = PaceDreamPrimary
    val PrimaryLight = PaceDreamPrimaryLight
    val PrimaryDark = PaceDreamPrimaryDark
    val Secondary = PaceDreamSecondary
    val Accent = PaceDreamAccent
    val Background = PaceDreamBackground
    val Surface = PaceDreamSurface
    val Card = PaceDreamCardColor
    val TextPrimary = PaceDreamTextPrimary
    val TextSecondary = PaceDreamTextSecondary
    val TextTertiary = PaceDreamTextTertiary
    val Success = PaceDreamSuccess
    val Warning = PaceDreamWarning
    val Error = PaceDreamError
    val Info = PaceDreamInfo
    val Gray50 = PaceDreamGray50
    val Gray100 = PaceDreamGray100
    val Gray200 = PaceDreamGray200
    val Gray300 = PaceDreamGray300
    val Gray400 = PaceDreamGray400
    val Gray500 = PaceDreamGray500
    val Gray600 = PaceDreamGray600
    val Gray700 = PaceDreamGray700
    val Gray800 = PaceDreamGray800
    val Gray900 = PaceDreamGray900

    // iOS 26 system colors for direct access
    val Blue = SystemBlue
    val Green = SystemGreen
    val Indigo = SystemIndigo
    val Orange = SystemOrange
    val Pink = SystemPink
    val Purple = SystemPurple
    val Red = SystemRed
    val Teal = SystemTeal
    val Yellow = SystemYellow
    val Cyan = SystemCyan
    val Mint = SystemMint
    val Brown = SystemBrown

    // Semantic colors for components
    val SurfaceVariant = SystemGray5 // #E5E5EA
    val OnSurfaceVariant = SystemGray // #8E8E93
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF410002)
    val Border = Color(0xFFE5E7EB) // iOS DesignTokens.Colors.border
    val OnPrimary = Color(0xFFFFFFFF)
    val Outline = SystemGray2
    val OnCard = iOSLabel
    val OnSuccess = Color(0xFFFFFFFF)
    val OnWarning = Color(0xFFFFFFFF)
    val OnError = Color(0xFFFFFFFF)
    val OnBackground = iOSLabel

    // Category colors (matched to iOS DesignTokens.Colors)
    val CategoryRestRoom = Color(0xFF3B82F6) // Blue
    val CategoryTimeBased = Color(0xFF5527D7) // Purple (brand primary)
    val CategoryParking = Color(0xFFF59E0B) // Orange
    val CategoryRentalGear = Color(0xFF10B981) // Green
    val CategoryEVParking = Color(0xFFEC4899) // Pink
    val CategoryMeetingRoom = Color(0xFF8B5CF6) // Indigo

    // Semantic colors (matched to iOS Colors.swift)
    val StarRating = Color(0xFFFFBE0B) // iOS ratingStar
    val ChatBubbleSent = PaceDreamPrimary // iOS chatBubbleSent
    val ChatBubbleReceived = Color(0xFFE5E7EB) // iOS chatBubbleReceived (gray 0.2)
    val BookingConfirmed = Color(0xFF10B981) // green
    val BookingPending = Color(0xFFF59E0B) // orange
    val BookingCancelled = Color(0xFFEF4444) // red
    val ModalOverlay = Color(0xFF000000).copy(alpha = 0.40f) // iOS modalOverlay
    val InputBorder = Color(0xFFD1D5DB) // iOS inputBorder (gray 0.3)
    val InputPlaceholder = Color(0xFF9CA3AF) // iOS inputPlaceholder (gray 0.5)

    // Border aligned with iOS DesignTokens
    val BorderLight = Color(0xFFE5E7EB) // iOS DesignTokens.Colors.border

    // Shadow color aligned with iOS DesignTokens
    val ShadowColor = Color(0xFF101828).copy(alpha = 0.08f) // iOS DesignTokens.Colors.shadow

    // Brand gradient endpoints (iOS DesignTokens.Colors)
    val GradientStart = Color(0xFF3B82F6) // iOS gradientStart
    val GradientEnd = Color(0xFF5527D7) // iOS gradientEnd

    // Liquid Glass specific colors
    val GlassSurface = Color(0xFFFFFFFF).copy(alpha = 0.72f) // translucent white
    val GlassSurfaceDark = Color(0xFF1C1C1E).copy(alpha = 0.72f) // translucent dark
    val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.20f) // subtle glass edge
    val GlassBorderDark = Color(0xFFFFFFFF).copy(alpha = 0.10f)
    val GlassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.40f) // specular highlight
}

// ============================================================================
// Light Theme Color Scheme (iOS 26 aligned)
// ============================================================================
val primaryLight = PaceDreamPrimary
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = PaceDreamPrimaryLight
val onPrimaryContainerLight = PaceDreamPrimary
val secondaryLight = PaceDreamSecondary
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = PaceDreamSecondary.copy(alpha = 0.12f)
val onSecondaryContainerLight = PaceDreamSecondary
val tertiaryLight = PaceDreamAccent
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = PaceDreamAccent.copy(alpha = 0.12f)
val onTertiaryContainerLight = PaceDreamAccent
val errorLight = PaceDreamError
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)
val backgroundLight = iOSSystemBackground
val onBackgroundLight = iOSLabel
val surfaceLight = iOSSecondarySystemBackground
val onSurfaceLight = iOSLabel
val surfaceVariantLight = SystemGray5
val onSurfaceVariantLight = SystemGray
val outlineLight = SystemGray2
val outlineVariantLight = SystemGray3
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = PaceDreamGray800
val inverseOnSurfaceLight = PaceDreamGray50
val inversePrimaryLight = Color(0xFF7B4DFF) // matches primaryDark
val surfaceDimLight = SystemGray5
val surfaceBrightLight = iOSSystemBackground
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = SystemGray6
val surfaceContainerLight = SystemGray5
val surfaceContainerHighLight = SystemGray4
val surfaceContainerHighestLight = SystemGray3

// Light Android color scheme (same as default for consistency)
val primaryLightAndroid = primaryLight
val onPrimaryLightAndroid = onPrimaryLight
val primaryContainerLightAndroid = primaryContainerLight
val onPrimaryContainerLightAndroid = onPrimaryContainerLight
val secondaryLightAndroid = secondaryLight
val onSecondaryLightAndroid = onSecondaryLight
val secondaryContainerLightAndroid = secondaryContainerLight
val onSecondaryContainerLightAndroid = onSecondaryContainerLight
val tertiaryLightAndroid = tertiaryLight
val onTertiaryLightAndroid = onTertiaryLight
val tertiaryContainerLightAndroid = tertiaryContainerLight
val onTertiaryContainerLightAndroid = onTertiaryContainerLight
val errorLightAndroid = errorLight
val onErrorLightAndroid = onErrorLight
val errorContainerLightAndroid = errorContainerLight
val onErrorContainerLightAndroid = onErrorContainerLight
val backgroundLightAndroid = backgroundLight
val onBackgroundLightAndroid = onBackgroundLight
val surfaceLightAndroid = surfaceLight
val onSurfaceLightAndroid = onSurfaceLight
val surfaceVariantLightAndroid = surfaceVariantLight
val onSurfaceVariantLightAndroid = onSurfaceVariantLight
val outlineLightAndroid = outlineLight
val outlineVariantLightAndroid = outlineVariantLight
val scrimLightAndroid = scrimLight
val inverseSurfaceLightAndroid = inverseSurfaceLight
val inverseOnSurfaceLightAndroid = inverseOnSurfaceLight
val inversePrimaryLightAndroid = inversePrimaryLight
val surfaceDimLightAndroid = surfaceDimLight
val surfaceBrightLightAndroid = surfaceBrightLight
val surfaceContainerLowestLightAndroid = surfaceContainerLowestLight
val surfaceContainerLowLightAndroid = surfaceContainerLowLight
val surfaceContainerLightAndroid = surfaceContainerLight
val surfaceContainerHighLightAndroid = surfaceContainerHighLight
val surfaceContainerHighestLightAndroid = surfaceContainerHighestLight

// ============================================================================
// Dark Theme Colors (iOS 26 aligned)
// ============================================================================
val primaryDark = Color(0xFF7B4DFF) // iOS accent as dark mode primary (brighter variant)
val onPrimaryDark = Color(0xFFFFFFFF)
val primaryContainerDark = PaceDreamPrimary.copy(alpha = 0.24f)
val onPrimaryContainerDark = Color(0xFFE8DDFF)
val secondaryDark = Color(0xFF60A5FA) // brighter blue for dark mode
val onSecondaryDark = Color(0xFFFFFFFF)
val secondaryContainerDark = PaceDreamSecondary.copy(alpha = 0.24f)
val onSecondaryContainerDark = Color(0xFFD1E4FF)
val tertiaryDark = Color(0xFF9B7DFF) // brighter accent for dark mode
val onTertiaryDark = Color(0xFFFFFFFF)
val tertiaryContainerDark = PaceDreamAccent.copy(alpha = 0.24f)
val onTertiaryContainerDark = Color(0xFFE9DDFF)
val errorDark = SystemRedDark // #FF453A
val onErrorDark = Color(0xFFFFFFFF)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = iOSSystemBackgroundDark // #000000
val onBackgroundDark = iOSLabelDark
val surfaceDark = iOSSecondarySystemBackgroundDark // #1C1C1E
val onSurfaceDark = iOSLabelDark
val surfaceVariantDark = SystemGray3Dark // #48484A
val onSurfaceVariantDark = SystemGray2 // #AEAEB2
val outlineDark = SystemGrayDark // #8E8E93
val outlineVariantDark = SystemGray3Dark // #48484A
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = iOSLabelDark
val inverseOnSurfaceDark = Color(0xFF313033)
val inversePrimaryDark = PaceDreamPrimary
val surfaceDimDark = Color(0xFF0F0D13)
val surfaceBrightDark = SystemGray4Dark // #3A3A3C
val surfaceContainerLowestDark = Color(0xFF000000)
val surfaceContainerLowDark = SystemGray6Dark // #1C1C1E
val surfaceContainerDark = SystemGray5Dark // #2C2C2E
val surfaceContainerHighDark = SystemGray4Dark // #3A3A3C
val surfaceContainerHighestDark = SystemGray3Dark // #48484A

// Dark Android colors (same as default for consistency)
val primaryDarkAndroid = primaryDark
val onPrimaryDarkAndroid = onPrimaryDark
val primaryContainerDarkAndroid = primaryContainerDark
val onPrimaryContainerDarkAndroid = onPrimaryContainerDark
val secondaryDarkAndroid = secondaryDark
val onSecondaryDarkAndroid = onSecondaryDark
val secondaryContainerDarkAndroid = secondaryContainerDark
val onSecondaryContainerDarkAndroid = onSecondaryContainerDark
val tertiaryDarkAndroid = tertiaryDark
val onTertiaryDarkAndroid = onTertiaryDark
val tertiaryContainerDarkAndroid = tertiaryContainerDark
val onTertiaryContainerDarkAndroid = onTertiaryContainerDark
val errorDarkAndroid = errorDark
val onErrorDarkAndroid = onErrorDark
val errorContainerDarkAndroid = errorContainerDark
val onErrorContainerDarkAndroid = onErrorContainerDark
val backgroundDarkAndroid = backgroundDark
val onBackgroundDarkAndroid = onBackgroundDark
val surfaceDarkAndroid = surfaceDark
val onSurfaceDarkAndroid = onSurfaceDark
val surfaceVariantDarkAndroid = surfaceVariantDark
val onSurfaceVariantDarkAndroid = onSurfaceVariantDark
val outlineDarkAndroid = outlineDark
val outlineVariantDarkAndroid = outlineVariantDark
val scrimDarkAndroid = scrimDark
val inverseSurfaceDarkAndroid = inverseSurfaceDark
val inverseOnSurfaceDarkAndroid = inverseOnSurfaceDark
val inversePrimaryDarkAndroid = inversePrimaryDark
val surfaceDimDarkAndroid = surfaceDimDark
val surfaceBrightDarkAndroid = surfaceBrightDark
val surfaceContainerLowestDarkAndroid = surfaceContainerLowestDark
val surfaceContainerLowDarkAndroid = surfaceContainerLowDark
val surfaceContainerDarkAndroid = surfaceContainerDark
val surfaceContainerHighDarkAndroid = surfaceContainerHighDark
val surfaceContainerHighestDarkAndroid = surfaceContainerHighestDark

// ============================================================================
// Extended Colors: Warning and Success (iOS 26 aligned)
// ============================================================================
// Light
val warningLight = SystemOrange
val onWarningLight = Color(0xFFFFFFFF)
val warningContainerLight = SystemOrange.copy(alpha = 0.15f)
val onWarningContainerLight = Color(0xFF78350F)
val successLight = SystemGreen
val onSuccessLight = Color(0xFFFFFFFF)
val successContainerLight = SystemGreen.copy(alpha = 0.15f)
val onSuccessContainerLight = Color(0xFF065F46)

// Dark
val warningDark = SystemOrangeDark
val onWarningDark = Color(0xFF78350F)
val warningContainerDark = SystemOrange.copy(alpha = 0.24f)
val onWarningContainerDark = SystemOrangeDark
val successDark = SystemGreenDark
val onSuccessDark = Color(0xFF065F46)
val successContainerDark = SystemGreen.copy(alpha = 0.24f)
val onSuccessContainerDark = SystemGreenDark

// Light High Contrast
val warningLightHighContrast = Color(0xFF92400E)
val onWarningLightHighContrast = Color(0xFFFFFFFF)
val warningContainerLightHighContrast = SystemOrange
val onWarningContainerLightHighContrast = Color(0xFFFFFFFF)
val successLightHighContrast = Color(0xFF064E3B)
val onSuccessLightHighContrast = Color(0xFFFFFFFF)
val successContainerLightHighContrast = SystemGreen
val onSuccessContainerLightHighContrast = Color(0xFFFFFFFF)

// Dark High Contrast
val warningDarkHighContrast = Color(0xFFFDE68A)
val onWarningDarkHighContrast = Color(0xFF451A03)
val warningContainerDarkHighContrast = SystemOrangeDark
val onWarningContainerDarkHighContrast = Color(0xFF000000)
val successDarkHighContrast = Color(0xFF6EE7B7)
val onSuccessDarkHighContrast = Color(0xFF022C22)
val successContainerDarkHighContrast = SystemGreenDark
val onSuccessContainerDarkHighContrast = Color(0xFF000000)

// ============================================================================
// Semantic aliases for backward compatibility
// ============================================================================
val DashboardHeaderColor = PaceDreamPrimary
val DarkPurpleColor = PaceDreamPrimaryDark
val NotificationsBgColor = PaceDreamAccent
val WhiteTextColor = Color(0xFFECECEC)
val GreyTextColor = PaceDreamTextSecondary
val HeadlineColor = PaceDreamTextPrimary
val ViewAllColor = PaceDreamTextTertiary
val BorderColor = PaceDreamColors.Border // iOS-aligned border color
val SubHeadingColor = PaceDreamTextSecondary
