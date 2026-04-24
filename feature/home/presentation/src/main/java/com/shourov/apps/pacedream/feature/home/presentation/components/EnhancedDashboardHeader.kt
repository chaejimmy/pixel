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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pacedream.common.composables.components.PaceDreamSearchBar
import com.pacedream.common.composables.theme.*
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.feature.home.presentation.R
import java.util.Calendar

/**
 * Premium Hero Header — matching iOS HomeHeroHeader.swift + ProminentSearchBar.swift
 *
 * Features:
 * - Gradient background (#4F46E5 → #7B4DFF) matching iOS
 * - Decorative blurred circles
 * - Avatar + daypart greeting + user name
 * - Notification bell with glass background
 * - Tagline "Find your perfect stay!"
 * - Overlapping prominent search bar with shadow
 * - Rounded bottom corners
 */
@Composable
fun EnhancedDashboardHeader(
    userName: String = "",
    profileImageUrl: String? = null,
    onSearchClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // Main hero section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        bottomEnd = PaceDreamRadius.XL,
                        bottomStart = PaceDreamRadius.XL
                    )
                )
        ) {
            // Gradient background matching iOS #4F46E5 → #7B4DFF
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4F46E5),
                                Color(0xFF7B4DFF),
                            )
                        )
                    )
            )

            // Background image overlay (if available)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .paint(
                        painterResource(id = R.drawable.bg_dashboard_header),
                        contentScale = ContentScale.FillBounds,
                        alpha = 0.3f,
                    )
            )

            // Translucent overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color(0xFF4F46E5).copy(alpha = 0.4f))
            )

            // Decorative blurred circles (matching iOS decorativeElements)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = (-80).dp, y = (-40).dp)
                    .blur(20.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = 280.dp, y = 20.dp)
                    .blur(15.dp)
                    .background(Color.White.copy(alpha = 0.10f), CircleShape)
            )

            // Header content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 52.dp,
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 20.dp,
                    ),
            ) {
                // Top row: Profile + Notification
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Avatar
                    if (!profileImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.Person,
                                contentDescription = "Profile",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Greeting text (daypart + name) matching iOS
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = daypartGreeting(),
                            style = PaceDreamTypography.Caption,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Text(
                            text = userName.ifBlank { "Guest" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                        )
                    }

                    // Notification bell (glass circle matching iOS)
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tagline matching iOS "Find your perfect stay!"
                Text(
                    text = stringResource(R.string.feature_home_find_your_perfect_stay),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Structured search pill: What • Where • When, matching the web
                // search structure.  Single tap anywhere opens SearchScreen; the
                // dedicated filter button remains as a trailing circular
                // affordance.  Guest count is handled later in the booking flow.
                StructuredSearchPill(
                    onSearchClick = onSearchClick,
                    onFilterClick = onFilterClick,
                )
            }
        }
    }
}

@Composable
private fun StructuredSearchPill(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
) {
    Surface(
        onClick = onSearchClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp),
        color = Color.White,
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchSegment(
                label = stringResource(R.string.feature_home_search_what),
                value = stringResource(R.string.feature_home_search_what_placeholder),
                modifier = Modifier.weight(1.3f),
            )
            SegmentDivider()
            SearchSegment(
                label = stringResource(R.string.feature_home_search_where),
                value = stringResource(R.string.feature_home_search_where_placeholder),
                modifier = Modifier.weight(1f),
            )
            SegmentDivider()
            SearchSegment(
                label = stringResource(R.string.feature_home_search_when),
                value = stringResource(R.string.feature_home_search_when_placeholder),
                modifier = Modifier.weight(1f),
            )

            // Trailing filter / search affordance — brand-colored circle.
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4F46E5),
                                Color(0xFF7B4DFF),
                            )
                        )
                    ),
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchSegment(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111827),
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
            maxLines = 1,
        )
    }
}

@Composable
private fun SegmentDivider() {
    Box(
        modifier = Modifier
            .height(28.dp)
            .width(1.dp)
            .background(Color(0xFFE5E7EB))
    )
}

/**
 * Compact header for scrolled state — iOS 26 large title collapse pattern.
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
                    top = 56.dp,
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
                        imageVector = PaceDreamIcons.Search,
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
                        imageVector = PaceDreamIcons.Notifications,
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
 * Minimal header for detail/inner screens — iOS 26 inline navigation bar.
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
                    top = 56.dp,
                    start = PaceDreamSpacing.MD,
                    end = PaceDreamSpacing.MD,
                    bottom = PaceDreamSpacing.SM,
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onBackClick?.let { onClick ->
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(PaceDreamButtonHeight.MD)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(PaceDreamIconSize.MD)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
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

            onActionClick?.let { onClick ->
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(PaceDreamButtonHeight.MD)
                ) {
                    actionIcon?.invoke() ?: Icon(
                        imageVector = PaceDreamIcons.MoreVert,
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier.size(PaceDreamIconSize.MD)
                    )
                }
            }
        }
    }
}

/** Returns a daypart-based greeting matching iOS HomeHeroHeader.swift logic. */
private fun daypartGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..10 -> "Good Morning,"
        in 11..16 -> "Good Afternoon,"
        in 17..21 -> "Good Evening,"
        else -> "Good Night,"
    }
}
