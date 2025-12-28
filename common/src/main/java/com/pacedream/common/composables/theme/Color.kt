
package com.pacedream.common.composables.theme

import androidx.compose.ui.graphics.Color

// PaceDream Brand Colors (matching iOS)
val PaceDreamPrimary = Color(0xFF5527D7) // #5527D7
val PaceDreamPrimaryLight = Color(0xFF5527D7).copy(alpha = 0.1f)
val PaceDreamPrimaryDark = Color(0xFF5527D7).copy(alpha = 0.8f)

val PaceDreamSecondary = Color(0xFF3B82F6) // #3B82F6
val PaceDreamAccent = Color(0xFF7B4DFF) // #7B4DFF

val PaceDreamBackground = Color(0xFFFFFFFF)
val PaceDreamSurface = Color(0xFFF8F9FA)
val PaceDreamCardColor = Color(0xFFFFFFFF) // Renamed from PaceDreamCard to avoid conflict
val PaceDreamCard = PaceDreamCardColor // Alias for backward compatibility

val PaceDreamTextPrimary = Color(0xFF1A1A1A)
val PaceDreamTextSecondary = Color(0xFF6B7280)
val PaceDreamTextTertiary = Color(0xFF9CA3AF)

val PaceDreamSuccess = Color(0xFF10B981)
val PaceDreamWarning = Color(0xFFF59E0B)
val PaceDreamError = Color(0xFFEF4444)
val PaceDreamInfo = Color(0xFF3B82F6)

// Neutral Grays
val PaceDreamGray50 = Color(0xFFF9FAFB)
val PaceDreamGray100 = Color(0xFFF3F4F6)
val PaceDreamGray200 = Color(0xFFE5E7EB)
val PaceDreamGray300 = Color(0xFFD1D5DB)
val PaceDreamGray400 = Color(0xFF9CA3AF)
val PaceDreamGray500 = Color(0xFF6B7280)
val PaceDreamGray600 = Color(0xFF4B5563)
val PaceDreamGray700 = Color(0xFF374151)
val PaceDreamGray800 = Color(0xFF1F2937)
val PaceDreamGray900 = Color(0xFF111827)

/**
 * PaceDream Colors Namespace
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
}

// Legacy colors for backward compatibility
val primaryLight = PaceDreamPrimary
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = PaceDreamPrimaryLight
val onPrimaryContainerLight = Color(0xFFFFFFFF)
val secondaryLight = PaceDreamSecondary
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = PaceDreamAccent
val onSecondaryContainerLight = Color(0xFFFFFFFF)
val tertiaryLight = PaceDreamAccent
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = PaceDreamAccent
val onTertiaryContainerLight = Color(0xFFFFFFFF)
val errorLight = PaceDreamError
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)
val backgroundLight = PaceDreamBackground
val onBackgroundLight = PaceDreamTextPrimary
val surfaceLight = PaceDreamSurface
val onSurfaceLight = PaceDreamTextPrimary
val surfaceVariantLight = PaceDreamGray200
val onSurfaceVariantLight = PaceDreamGray600
val outlineLight = PaceDreamGray400
val outlineVariantLight = PaceDreamGray300
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = PaceDreamGray800
val inverseOnSurfaceLight = PaceDreamGray50
val inversePrimaryLight = PaceDreamAccent
val surfaceDimLight = PaceDreamGray100
val surfaceBrightLight = PaceDreamBackground
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = PaceDreamGray50
val surfaceContainerLight = PaceDreamGray100
val surfaceContainerHighLight = PaceDreamGray200
val surfaceContainerHighestLight = PaceDreamGray300

val primaryLightAndroid = PaceDreamPrimary
val onPrimaryLightAndroid = Color(0xFFFFFFFF)
val primaryContainerLightAndroid = PaceDreamPrimaryLight
val onPrimaryContainerLightAndroid = Color(0xFFFFFFFF)
val secondaryLightAndroid = PaceDreamSecondary
val onSecondaryLightAndroid = Color(0xFFFFFFFF)
val secondaryContainerLightAndroid = PaceDreamAccent
val onSecondaryContainerLightAndroid = Color(0xFFFFFFFF)
val tertiaryLightAndroid = PaceDreamAccent
val onTertiaryLightAndroid = Color(0xFFFFFFFF)
val tertiaryContainerLightAndroid = PaceDreamAccent
val onTertiaryContainerLightAndroid = Color(0xFFFFFFFF)
val errorLightAndroid = PaceDreamError
val onErrorLightAndroid = Color(0xFFFFFFFF)
val errorContainerLightAndroid = Color(0xFFFFDAD6)
val onErrorContainerLightAndroid = Color(0xFFFFFFFF)
val backgroundLightAndroid = PaceDreamBackground
val onBackgroundLightAndroid = PaceDreamTextPrimary
val surfaceLightAndroid = PaceDreamSurface
val onSurfaceLightAndroid = PaceDreamTextPrimary
val surfaceVariantLightAndroid = PaceDreamGray200
val onSurfaceVariantLightAndroid = PaceDreamGray600
val outlineLightAndroid = PaceDreamGray400
val outlineVariantLightAndroid = PaceDreamGray300
val scrimLightAndroid = Color(0xFF000000)
val inverseSurfaceLightAndroid = PaceDreamGray800
val inverseOnSurfaceLightAndroid = PaceDreamGray50
val inversePrimaryLightAndroid = PaceDreamAccent
val surfaceDimLightAndroid = PaceDreamGray100
val surfaceBrightLightAndroid = PaceDreamBackground
val surfaceContainerLowestLightAndroid = Color(0xFFFFFFFF)
val surfaceContainerLowLightAndroid = PaceDreamGray50
val surfaceContainerLightAndroid = PaceDreamGray100
val surfaceContainerHighLightAndroid = PaceDreamGray200
val surfaceContainerHighestLightAndroid = PaceDreamGray300

// Dark theme colors
val primaryDark = Color(0xFF9D8AFF)
val onPrimaryDark = Color(0xFF3700B3)
val primaryContainerDark = Color(0xFF4D3DCC)
val onPrimaryContainerDark = Color(0xFFE8DDFF)
val secondaryDark = Color(0xFF7AB3FF)
val onSecondaryDark = Color(0xFF003258)
val secondaryContainerDark = Color(0xFF004880)
val onSecondaryContainerDark = Color(0xFFD1E4FF)
val tertiaryDark = Color(0xFFA58CFF)
val onTertiaryDark = Color(0xFF3B1E99)
val tertiaryContainerDark = Color(0xFF5636B2)
val onTertiaryContainerDark = Color(0xFFE9DDFF)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF1A1A1A)
val onBackgroundDark = Color(0xFFE6E1E5)
val surfaceDark = Color(0xFF1A1A1A)
val onSurfaceDark = Color(0xFFE6E1E5)
val surfaceVariantDark = Color(0xFF49454F)
val onSurfaceVariantDark = Color(0xFFCAC4D0)
val outlineDark = Color(0xFF938F99)
val outlineVariantDark = Color(0xFF49454F)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFE6E1E5)
val inverseOnSurfaceDark = Color(0xFF313033)
val inversePrimaryDark = PaceDreamPrimary
val surfaceDimDark = Color(0xFF141218)
val surfaceBrightDark = Color(0xFF3B383E)
val surfaceContainerLowestDark = Color(0xFF0F0D13)
val surfaceContainerLowDark = Color(0xFF1D1B20)
val surfaceContainerDark = Color(0xFF211F26)
val surfaceContainerHighDark = Color(0xFF2B2930)
val surfaceContainerHighestDark = Color(0xFF36343B)

// Dark Android colors
val primaryDarkAndroid = Color(0xFF9D8AFF)
val onPrimaryDarkAndroid = Color(0xFF3700B3)
val primaryContainerDarkAndroid = Color(0xFF4D3DCC)
val onPrimaryContainerDarkAndroid = Color(0xFFE8DDFF)
val secondaryDarkAndroid = Color(0xFF7AB3FF)
val onSecondaryDarkAndroid = Color(0xFF003258)
val secondaryContainerDarkAndroid = Color(0xFF004880)
val onSecondaryContainerDarkAndroid = Color(0xFFD1E4FF)
val tertiaryDarkAndroid = Color(0xFFA58CFF)
val onTertiaryDarkAndroid = Color(0xFF3B1E99)
val tertiaryContainerDarkAndroid = Color(0xFF5636B2)
val onTertiaryContainerDarkAndroid = Color(0xFFE9DDFF)
val errorDarkAndroid = Color(0xFFFFB4AB)
val onErrorDarkAndroid = Color(0xFF690005)
val errorContainerDarkAndroid = Color(0xFF93000A)
val onErrorContainerDarkAndroid = Color(0xFFFFDAD6)
val backgroundDarkAndroid = Color(0xFF1A1A1A)
val onBackgroundDarkAndroid = Color(0xFFE6E1E5)
val surfaceDarkAndroid = Color(0xFF1A1A1A)
val onSurfaceDarkAndroid = Color(0xFFE6E1E5)
val surfaceVariantDarkAndroid = Color(0xFF49454F)
val onSurfaceVariantDarkAndroid = Color(0xFFCAC4D0)
val outlineDarkAndroid = Color(0xFF938F99)
val outlineVariantDarkAndroid = Color(0xFF49454F)
val scrimDarkAndroid = Color(0xFF000000)
val inverseSurfaceDarkAndroid = Color(0xFFE6E1E5)
val inverseOnSurfaceDarkAndroid = Color(0xFF313033)
val inversePrimaryDarkAndroid = PaceDreamPrimary
val surfaceDimDarkAndroid = Color(0xFF141218)
val surfaceBrightDarkAndroid = Color(0xFF3B383E)
val surfaceContainerLowestDarkAndroid = Color(0xFF0F0D13)
val surfaceContainerLowDarkAndroid = Color(0xFF1D1B20)
val surfaceContainerDarkAndroid = Color(0xFF211F26)
val surfaceContainerHighDarkAndroid = Color(0xFF2B2930)
val surfaceContainerHighestDarkAndroid = Color(0xFF36343B)

// Warning and Success colors - Light
val warningLight = Color(0xFFF59E0B)
val onWarningLight = Color(0xFFFFFFFF)
val warningContainerLight = Color(0xFFFEF3C7)
val onWarningContainerLight = Color(0xFF78350F)
val successLight = Color(0xFF10B981)
val onSuccessLight = Color(0xFFFFFFFF)
val successContainerLight = Color(0xFFD1FAE5)
val onSuccessContainerLight = Color(0xFF065F46)

// Warning and Success colors - Dark
val warningDark = Color(0xFFFCD34D)
val onWarningDark = Color(0xFF78350F)
val warningContainerDark = Color(0xFFB45309)
val onWarningContainerDark = Color(0xFFFEF3C7)
val successDark = Color(0xFF34D399)
val onSuccessDark = Color(0xFF065F46)
val successContainerDark = Color(0xFF047857)
val onSuccessContainerDark = Color(0xFFD1FAE5)

// Warning and Success colors - Light High Contrast
val warningLightHighContrast = Color(0xFF92400E)
val onWarningLightHighContrast = Color(0xFFFFFFFF)
val warningContainerLightHighContrast = Color(0xFFF59E0B)
val onWarningContainerLightHighContrast = Color(0xFFFFFFFF)
val successLightHighContrast = Color(0xFF064E3B)
val onSuccessLightHighContrast = Color(0xFFFFFFFF)
val successContainerLightHighContrast = Color(0xFF10B981)
val onSuccessContainerLightHighContrast = Color(0xFFFFFFFF)

// Warning and Success colors - Dark High Contrast
val warningDarkHighContrast = Color(0xFFFDE68A)
val onWarningDarkHighContrast = Color(0xFF451A03)
val warningContainerDarkHighContrast = Color(0xFFFCD34D)
val onWarningContainerDarkHighContrast = Color(0xFF000000)
val successDarkHighContrast = Color(0xFF6EE7B7)
val onSuccessDarkHighContrast = Color(0xFF022C22)
val successContainerDarkHighContrast = Color(0xFF34D399)
val onSuccessContainerDarkHighContrast = Color(0xFF000000)

//other colors
val DashboardHeaderColor = PaceDreamPrimary
val DarkPurpleColor = PaceDreamPrimaryDark
val NotificationsBgColor = PaceDreamAccent
val WhiteTextColor = Color(0xFFECECEC)
val GreyTextColor = PaceDreamTextSecondary
val HeadlineColor = PaceDreamTextPrimary
val ViewAllColor = PaceDreamTextTertiary
val BorderColor = PaceDreamGray200
val SubHeadingColor = PaceDreamTextSecondary
