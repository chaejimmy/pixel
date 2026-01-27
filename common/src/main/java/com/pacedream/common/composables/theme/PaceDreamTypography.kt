/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pacedream.common.composables.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import com.pacedream.common.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.core_designsystems_com_google_android_gms_fonts_certs
)

val paceDreamFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Inter"),
        fontProvider = provider,
    )
)

val paceDreamDisplayFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Poppins"),
        fontProvider = provider,
    )
)

/**
 * PaceDream Typography that maps to Material 3 Typography
 * This ensures compatibility with Material 3 while using our custom design system
 * Enhanced with modern, professional typography with proper letter spacing and line heights
 */
val PaceDreamMaterialTypography = Typography(
    // Display styles - for large headings
    // Modern typography: larger sizes with better line height ratios and letter spacing
    displayLarge = TextStyle(
        fontFamily = paceDreamDisplayFontFamily,
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 44.sp,
        letterSpacing = (-0.02).em
    ),
    displayMedium = TextStyle(
        fontFamily = paceDreamDisplayFontFamily,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 38.sp,
        letterSpacing = (-0.01).em
    ),
    displaySmall = TextStyle(
        fontFamily = paceDreamDisplayFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 32.sp,
        letterSpacing = 0.em
    ),
    
    // Headline styles - for section headings
    // Improved readability with better line height ratios
    headlineLarge = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
        letterSpacing = 0.em
    ),
    headlineMedium = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
        letterSpacing = 0.01.em
    ),
    headlineSmall = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
        letterSpacing = 0.01.em
    ),
    
    // Title styles - for card titles and important text
    // Professional sizing with optimal readability
    titleLarge = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp,
        letterSpacing = 0.01.em
    ),
    titleMedium = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
        letterSpacing = 0.01.em
    ),
    titleSmall = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp,
        letterSpacing = 0.01.em
    ),
    
    // Body styles - for regular text
    // Optimized for readability with comfortable line heights
    bodyLarge = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 26.sp,
        letterSpacing = 0.01.em
    ),
    bodyMedium = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
        letterSpacing = 0.01.em
    ),
    bodySmall = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
        letterSpacing = 0.01.em
    ),
    
    // Label styles - for small text and labels
    // Clear and readable even at smaller sizes
    labelLarge = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp,
        letterSpacing = 0.01.em
    ),
    labelMedium = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        letterSpacing = 0.01.em
    ),
    labelSmall = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = 0.02.em
    )
)

/**
 * Legacy typography for backward compatibility
 */
val FauTypography = PaceDreamMaterialTypography
