
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
val surfaceContainerLowestAndroid = Color(0xFFFFFFFF)
val surfaceContainerLowAndroid = PaceDreamGray50
val surfaceContainerAndroid = PaceDreamGray100
val surfaceContainerHighAndroid = PaceDreamGray200
val surfaceContainerHighestAndroid = PaceDreamGray300

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
