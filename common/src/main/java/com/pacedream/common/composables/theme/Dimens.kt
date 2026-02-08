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

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// Legacy Padding Tokens - iOS 26 HIG aligned
// Prefer PaceDreamSpacing for new code
// ============================================================================
val ExtraSmallPadding = 4.dp   // = PaceDreamSpacing.XS
val SmallPadding = 6.dp
val NormalPadding = 8.dp       // = PaceDreamSpacing.SM
val MediumPadding = 12.dp
val LargePadding = 16.dp      // = PaceDreamSpacing.MD
val ExtraLargePadding = 20.dp  // = PaceDreamSpacing.LG
val LargerPadding = 24.dp     // = PaceDreamSpacing.XL

val BorderWidth = 1.dp

// ============================================================================
// Legacy Text Size Tokens - iOS 26 HIG type scale
// Prefer PaceDreamTypography named styles for new code
// ============================================================================
val SmallText = 11.sp   // Caption 2 (iOS)
val NormalText = 13.sp   // Footnote (iOS)
val MediumText = 16.sp   // Callout (iOS)
val LargeText = 17.sp    // Body / Headline (iOS default)
val ExtraLargeText = 20.sp // Title 3 (iOS)
val LargerText = 22.sp    // Title 2 (iOS)
