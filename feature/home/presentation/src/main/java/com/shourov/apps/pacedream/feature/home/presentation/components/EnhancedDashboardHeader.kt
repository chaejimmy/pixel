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

package com.shourov.apps.pacedream.feature.home.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.components.PaceDreamSearchBar
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.home.presentation.R

/**
 * iOS 26 Liquid Glass Dashboard Header
 *
 * Layout pattern: Full-bleed background image with translucent overlay,
 * floating glass elements, and content-first typography.
 * - Rounded bottom corners (XL = 20dp)
 * - Translucent primary overlay (60% alpha)
 * - 44dp minimum touch targets
 * - iOS 26 typography scale
 */
@Composable
fun EnhancedDashboardHeader(
    userName: String = "",
    onSearchClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomEnd = PaceDreamRadius.XL, bottomStart = PaceDreamRadius.XL))
            .paint(
                painterResource(id = R.drawable.bg_dashboard_header),
                contentScale = ContentScale.FillBounds,
            ),
    ) {
        // Translucent overlay (Liquid Glass style)
        Surface(
            color = PaceDreamPrimary.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 60.dp,
                        start = PaceDreamSpacing.MD,
                        end = PaceDreamSpacing.MD,
                        bottom = PaceDreamSpacing.XL,
                    ),
            ) {
                // User Profile and Notifications Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar - 56dp circle (iOS standard)
                        Image(
                            painter = painterResource(R.drawable.ic_dummy_user),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                        )

                        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))

                        // Greeting and Name - iOS 26 compact typography
                        val annotatedString = buildAnnotatedString {
                            append(stringResource(R.string.feature_home_good_morning))
                            if (userName.isNotBlank()) {
                                appendLine()
                                withStyle(style = SpanStyle(
                                    fontSize = PaceDreamTypography.Headline.fontSize,
                                    fontWeight = FontWeight.SemiBold
                                )) {
                                    append(userName)
                                }
                            }
                        }

                        Text(
                            text = annotatedString,
                            color = Color.White,
                            style = PaceDreamTypography.Subheadline,
                            modifier = Modifier.weight(1F),
                        )
                    }

                    // Floating Glass Notification Button (44dp iOS touch target)
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier
                            .size(PaceDreamButtonHeight.MD)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = PaceDreamGlass.BorderAlpha))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(PaceDreamIconSize.MD)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                // Subtle divider (iOS separator style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )

                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                // Main Title - iOS 26 bold left-aligned
                Text(
                    text = stringResource(R.string.feature_home_find_your_perfect_stay),
                    style = PaceDreamTypography.Title1,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                // Search Bar + Filter Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    PaceDreamSearchBar(
                        query = "",
                        onQueryChange = { },
                        onSearchClick = onSearchClick,
                        onFilterClick = onFilterClick,
                        placeholder = "Search properties, locations...",
                        modifier = Modifier.weight(1f)
                    )

                    // Glass filter button (44dp iOS touch target)
                    IconButton(
                        onClick = onFilterClick,
                        modifier = Modifier
                            .size(PaceDreamButtonHeight.MD)
                            .background(Color.White.copy(alpha = PaceDreamGlass.BorderAlpha), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color.White,
                            modifier = Modifier.size(PaceDreamIconSize.MD)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact header for scrolled state - iOS 26 large title collapse pattern
 */
@Composable
fun CompactDashboardHeader(
    title: String = "PaceDream",
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = PaceDreamPrimary,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 60.dp,
                    start = PaceDreamSpacing.MD,
                    end = PaceDreamSpacing.MD,
                    bottom = PaceDreamSpacing.SM,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = PaceDreamTypography.Headline,
                color = Color.White
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.size(PaceDreamButtonHeight.MD)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(PaceDreamIconSize.MD)
                    )
                }

                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier.size(PaceDreamButtonHeight.MD)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(PaceDreamIconSize.MD)
                    )
                }
            }
        }
    }
}

/**
 * Minimal header for detail/inner screens - iOS 26 inline navigation bar
 */
@Composable
fun MinimalDashboardHeader(
    title: String = "PaceDream",
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
    actionIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = PaceDreamPrimary,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 60.dp,
                    start = PaceDreamSpacing.MD,
                    end = PaceDreamSpacing.MD,
                    bottom = PaceDreamSpacing.SM,
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button (44dp touch target)
            onBackClick?.let { onClick ->
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(PaceDreamButtonHeight.MD)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(PaceDreamIconSize.MD)
                    )
                }
            }

            // Title and Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Headline,
                    color = Color.White
                )

                subtitle?.let {
                    Text(
                        text = it,
                        style = PaceDreamTypography.Subheadline,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Action Button (44dp touch target)
            onActionClick?.let { onClick ->
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(PaceDreamButtonHeight.MD)
                ) {
                    actionIcon?.invoke() ?: Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier.size(PaceDreamIconSize.MD)
                    )
                }
            }
        }
    }
}
