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

/**
 * SF Pro equivalent on Android - Inter is the closest match to Apple's San Francisco font.
 * Used for body text, labels, and UI elements (matches iOS HIG Body/Callout/Caption roles).
 */
val paceDreamFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Inter"),
        fontProvider = provider,
    )
)

/**
 * Display font - Poppins for large titles and display headings.
 * Provides visual distinction for hero/display text (matches iOS Large Title / Title 1 role).
 */
val paceDreamDisplayFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Poppins"),
        fontProvider = provider,
    )
)

/**
 * PaceDream Typography - iOS 26 HIG Type Scale
 *
 * Mapped from Apple's Human Interface Guidelines typography sizes:
 * - Large Title: 34pt → displayLarge
 * - Title 1: 28pt → displayMedium
 * - Title 2: 22pt → displaySmall
 * - Title 3: 20pt → headlineLarge / titleLarge
 * - Headline: 17pt Semi-Bold → headlineMedium
 * - Body: 17pt → bodyLarge
 * - Callout: 16pt → bodyMedium
 * - Subheadline: 15pt → bodySmall
 * - Footnote: 13pt → labelMedium
 * - Caption 1: 12pt → labelSmall
 * - Caption 2: 11pt → (available via PaceDreamTypography.Caption2)
 *
 * iOS 26 Liquid Glass updates: bolder left-aligned typography for improved readability.
 */
val PaceDreamMaterialTypography = Typography(
    // Large Title → 34sp, Bold (Poppins for display impact)
    displayLarge = TextStyle(
        fontFamily = paceDreamDisplayFontFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 41.sp,
        letterSpacing = 0.37.sp
    ),
    // Title 1 → 28sp, Bold
    displayMedium = TextStyle(
        fontFamily = paceDreamDisplayFontFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 34.sp,
        letterSpacing = 0.36.sp
    ),
    // Title 2 → 22sp, Bold
    displaySmall = TextStyle(
        fontFamily = paceDreamDisplayFontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp,
        letterSpacing = 0.35.sp
    ),

    // Title 3 → 20sp, Regular (iOS uses regular weight for Title 3)
    headlineLarge = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 25.sp,
        letterSpacing = 0.38.sp
    ),
    // Headline → 17sp, Semi-Bold (iOS HIG: semi-bold emphasis for headings)
    headlineMedium = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),
    // Headline (small variant)
    headlineSmall = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),

    // Title 3 (duplicate mapping for M3 titleLarge) → 20sp
    titleLarge = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 25.sp,
        letterSpacing = 0.38.sp
    ),
    // Headline → 17sp, Semi-Bold
    titleMedium = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),
    // Callout bold variant → 16sp, Medium
    titleSmall = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 21.sp,
        letterSpacing = (-0.32).sp
    ),

    // Body → 17sp, Regular (iOS default text size)
    bodyLarge = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),
    // Callout → 16sp, Regular
    bodyMedium = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp,
        letterSpacing = (-0.32).sp
    ),
    // Subheadline → 15sp, Regular
    bodySmall = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),

    // Footnote → 13sp, Regular
    labelLarge = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        letterSpacing = (-0.08).sp
    ),
    // Caption 1 → 12sp, Regular
    labelMedium = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    // Caption 2 → 11sp, Regular
    labelSmall = TextStyle(
        fontFamily = paceDreamFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 13.sp,
        letterSpacing = 0.07.sp
    )
)

/**
 * Legacy typography for backward compatibility
 */
val FauTypography = PaceDreamMaterialTypography
