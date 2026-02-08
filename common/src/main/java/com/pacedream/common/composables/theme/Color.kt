
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
// PaceDream Brand Colors (remapped to iOS 26 palette)
// Primary uses SystemIndigo to align with iOS Liquid Glass purple tones
// ============================================================================
val PaceDreamPrimary = SystemIndigo // #5856D6 - closer to iOS system indigo
val PaceDreamPrimaryLight = SystemIndigo.copy(alpha = 0.12f)
val PaceDreamPrimaryDark = SystemIndigo.copy(alpha = 0.8f)

val PaceDreamSecondary = SystemBlue // #007AFF - iOS system blue
val PaceDreamAccent = SystemPurple // #AF52DE - iOS system purple

val PaceDreamBackground = iOSSystemBackground // #FFFFFF
val PaceDreamSurface = iOSSecondarySystemBackground // #F2F2F7 (iOS grouped bg)
val PaceDreamCardColor = iOSSecondarySystemGroupedBackground // #FFFFFF
val PaceDreamCard = PaceDreamCardColor

val PaceDreamTextPrimary = iOSLabel // #000000
val PaceDreamTextSecondary = iOSSecondaryLabel // 60% opacity
val PaceDreamTextTertiary = iOSTertiaryLabel // 30% opacity

val PaceDreamSuccess = SystemGreen // #34C759
val PaceDreamWarning = SystemOrange // #FF9500
val PaceDreamError = SystemRed // #FF3B30
val PaceDreamInfo = SystemBlue // #007AFF

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
    val Border = iOSSeparator
    val OnPrimary = Color(0xFFFFFFFF)
    val Outline = SystemGray2
    val OnCard = iOSLabel
    val OnSuccess = Color(0xFFFFFFFF)
    val OnWarning = Color(0xFFFFFFFF)
    val OnError = Color(0xFFFFFFFF)
    val OnBackground = iOSLabel

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
val onPrimaryContainerLight = SystemIndigo
val secondaryLight = PaceDreamSecondary
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = SystemBlue.copy(alpha = 0.12f)
val onSecondaryContainerLight = SystemBlue
val tertiaryLight = PaceDreamAccent
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = SystemPurple.copy(alpha = 0.12f)
val onTertiaryContainerLight = SystemPurple
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
val inversePrimaryLight = SystemIndigoDark
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
val primaryDark = SystemIndigoDark // #5E5CE6
val onPrimaryDark = Color(0xFFFFFFFF)
val primaryContainerDark = SystemIndigo.copy(alpha = 0.24f)
val onPrimaryContainerDark = Color(0xFFE8DDFF)
val secondaryDark = SystemBlueDark // #0A84FF
val onSecondaryDark = Color(0xFFFFFFFF)
val secondaryContainerDark = SystemBlue.copy(alpha = 0.24f)
val onSecondaryContainerDark = Color(0xFFD1E4FF)
val tertiaryDark = SystemPurpleDark // #BF5AF2
val onTertiaryDark = Color(0xFFFFFFFF)
val tertiaryContainerDark = SystemPurple.copy(alpha = 0.24f)
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
val BorderColor = iOSSeparator
val SubHeadingColor = PaceDreamTextSecondary
