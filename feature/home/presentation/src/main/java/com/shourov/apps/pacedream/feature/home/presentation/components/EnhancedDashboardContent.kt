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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenRentedGearsState
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenRoomsState
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenSplitStaysState
import com.shourov.apps.pacedream.feature.home.presentation.R

/**
 * Inline warning banner component for section errors.
 * Matches iOS behavior of showing inline warnings instead of just toasts.
 */
@Composable
fun SectionWarningBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = PaceDreamWarning.copy(alpha = 0.1f),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Icon(
                imageVector = PaceDreamIcons.Warning,
                contentDescription = null,
                tint = PaceDreamWarning,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = PaceDreamTypography.Callout,
                color = PaceDreamTextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                TextButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(contentColor = PaceDreamPrimary)
                ) {
                    Text(
                        text = "Retry",
                        style = PaceDreamTypography.CalloutBold
                    )
                }
            }
        }
    }
}

/**
 * Enhanced Dashboard Content — iOS-matched section order
 *
 * Section order (matching iOS HomeView.swift):
 * 1. Quick Chips (category filter)
 * 2. Spaces section (featured listings — hourly spaces)
 * 3. Items section (featured listings — rent gear)
 * 4. Services section
 * 5. Browse by Type (Spaces / Items / Services segmented selector)
 * 6. Trending Destinations
 *
 * Removed (not in iOS):
 * - Metrics Cards (Available Rooms / Items / Services counts)
 * - Last-Minute Deals section
 * - Find Roommate section
 * - Browse by Category (replaced by Quick Chips + Browse by Type)
 * - Browse by Destination with hardcoded drawable resources
 */
@Composable
fun EnhancedDashboardContent(
    roomsState: HomeScreenRoomsState,
    gearsState: HomeScreenRentedGearsState,
    splitStaysState: HomeScreenSplitStaysState = HomeScreenSplitStaysState(),
    onTimeBasedRoomsChanged: (String) -> Unit,
    onRentedGearsChanged: (String) -> Unit,
    onSplitStaysRetry: () -> Unit = {},
    onPropertyClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onViewAllClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaceDreamBackground),
    ) {
        // 1. Quick Chips — matches iOS QuickChips.swift
        item {
            QuickChipsSection(
                onChipClick = { chip -> onCategoryClick(chip) }
            )
        }

        // 2. Spaces Section (Hourly Spaces) — matches iOS FeaturedListingsSection for hourlySpaces
        item {
            FeaturedSection(
                title = "Spaces",
                subtitle = "Find flexible spaces — restrooms, meeting rooms, parking, and more",
                isLoading = roomsState.loading,
                error = roomsState.error,
                isEmpty = roomsState.rooms.isEmpty(),
                onViewAllClick = { onViewAllClick("time-based") },
                onRetry = { onTimeBasedRoomsChanged("room") },
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (roomsState.loading) {
                        items(3) { PaceDreamShimmerCard() }
                    } else {
                        items(roomsState.rooms.take(10), key = { it.id }) { room ->
                            PaceDreamPropertyCard(
                                title = room.title,
                                location = room.location.city,
                                price = "$${room.price?.firstOrNull()?.amount ?: 0}/hr",
                                rating = room.rating.toDouble(),
                                reviewCount = 0,
                                imageUrl = room.gallery.images.firstOrNull(),
                                onClick = { onPropertyClick(room.id) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Items Section (Rent Gear) — matches iOS FeaturedListingsSection for rentGear
        item {
            FeaturedSection(
                title = "Items",
                subtitle = "Rent what you need — cameras, sports gear, tech, tools, and more",
                isLoading = gearsState.loading,
                error = gearsState.error,
                isEmpty = gearsState.rentedGears.isEmpty(),
                onViewAllClick = { onViewAllClick("gear") },
                onRetry = { onRentedGearsChanged("tech_gear") },
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (gearsState.loading) {
                        items(3) { PaceDreamShimmerCard() }
                    } else {
                        items(gearsState.rentedGears.take(10), key = { it.id }) { gear ->
                            PaceDreamPropertyCard(
                                title = gear.name,
                                location = gear.location,
                                price = "$${gear.hourlyRate}/hr",
                                rating = 0.0,
                                reviewCount = 0,
                                imageUrl = gear.images?.firstOrNull(),
                                onClick = { onPropertyClick(gear.id) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Services Section — matches iOS ServicesGridSection
        item {
            val hasServices = splitStaysState.splitStays.isNotEmpty()
            val isLoading = splitStaysState.loading

            if (isLoading || hasServices) {
                FeaturedSection(
                    title = "Services",
                    subtitle = "Book help when you need it — cleaning, moving, fitness, and more",
                    isLoading = isLoading,
                    error = splitStaysState.error,
                    isEmpty = splitStaysState.splitStays.isEmpty(),
                    onViewAllClick = { onViewAllClick("services") },
                    onRetry = onSplitStaysRetry,
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isLoading) {
                            items(3) { PaceDreamShimmerCard() }
                        } else {
                            items(
                                splitStaysState.splitStays.take(10),
                                key = { it._id ?: it.hashCode() }
                            ) { stay ->
                                PaceDreamPropertyCard(
                                    title = stay.name ?: "Service",
                                    location = stay.location ?: stay.city ?: "Location",
                                    price = "$${stay.price ?: "0"} total",
                                    rating = stay.rating?.toDouble() ?: 0.0,
                                    reviewCount = stay.reviewCount ?: 0,
                                    imageUrl = stay.images?.firstOrNull(),
                                    onClick = { onPropertyClick(stay._id ?: "") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Browse by Type — matches iOS ExploreByTypeSection.swift
        item {
            Spacer(modifier = Modifier.height(32.dp))
            BrowseByTypeSection(
                roomsState = roomsState,
                gearsState = gearsState,
                splitStaysState = splitStaysState,
                onPropertyClick = onPropertyClick,
                onViewAllClick = onViewAllClick,
                onSubcategoryClick = { _, sub -> onCategoryClick(sub.title) },
            )
        }

        // 6. Trending Destinations — matches iOS TrendingDestinationsSection.swift
        // Uses backend data from rooms/gear locations as proxy since iOS
        // destinations endpoint returns real data.
        item {
            Spacer(modifier = Modifier.height(32.dp))

            val destinations = buildTrendingDestinations(roomsState, gearsState)
            if (destinations.isNotEmpty()) {
                TrendingDestinationsSection(
                    destinations = destinations,
                    onDestinationClick = { dest -> onCategoryClick(dest.title) },
                    onViewAllClick = { onViewAllClick("destinations") },
                )
            }
        }

        // 7. Full-width empty state when everything is empty (matches iOS)
        item {
            if (!roomsState.loading && !gearsState.loading && !splitStaysState.loading &&
                roomsState.rooms.isEmpty() && gearsState.rentedGears.isEmpty() &&
                splitStaysState.splitStays.isEmpty()
            ) {
                val hasError = !roomsState.error.isNullOrEmpty() ||
                        !gearsState.error.isNullOrEmpty() ||
                        !splitStaysState.error.isNullOrEmpty()

                if (hasError) {
                    PaceDreamErrorState(
                        title = "Couldn't load Home",
                        description = roomsState.error ?: gearsState.error
                            ?: splitStaysState.error ?: "Something went wrong",
                        onRetryClick = onSplitStaysRetry,
                    )
                } else {
                    PaceDreamEmptyState(
                        title = "Nothing to show right now",
                        description = "Pull to refresh.",
                        icon = PaceDreamIcons.Search,
                    )
                }
            }
        }

        // Bottom padding for tab bar
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Reusable featured section wrapper — matches iOS FeaturedListingsSection pattern.
 * Shows title, subtitle, warning banner (if error), and content slot.
 */
@Composable
private fun FeaturedSection(
    title: String,
    subtitle: String,
    isLoading: Boolean,
    error: String?,
    isEmpty: Boolean,
    onViewAllClick: () -> Unit,
    onRetry: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
    ) {
        // Section header with View All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = PaceDreamTypography.Title2,
                color = PaceDreamTextPrimary,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "View All",
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamPrimary,
                )
            }
        }

        // Subtitle
        Text(
            text = subtitle,
            style = PaceDreamTypography.Footnote,
            color = PaceDreamTextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Warning banner if error
        if (!error.isNullOrEmpty() && !isLoading) {
            SectionWarningBanner(
                message = error,
                onRetry = onRetry,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Content
        content()
    }
}

/**
 * Build trending destinations from loaded data — extracts unique city names
 * from rooms and gear to create destination cards with fallback images.
 * This mirrors how iOS toDestinationData() maps backend DestinationSummary objects.
 */
private fun buildTrendingDestinations(
    roomsState: HomeScreenRoomsState,
    gearsState: HomeScreenRentedGearsState,
): List<TrendingDestination> {
    val citySet = mutableSetOf<String>()
    val destinations = mutableListOf<TrendingDestination>()

    // Extract cities from rooms
    roomsState.rooms.forEach { room ->
        val city = room.location.city.trim()
        if (city.isNotEmpty() && citySet.add(city.lowercase())) {
            destinations.add(
                TrendingDestination(
                    title = city,
                    subtitle = room.location.state.ifEmpty { null },
                    imageUrl = destinationFallbackImage(city),
                    propertyCount = roomsState.rooms.count {
                        it.location.city.trim().equals(city, ignoreCase = true)
                    },
                )
            )
        }
    }

    // Extract cities from gear
    gearsState.rentedGears.forEach { gear ->
        val city = gear.location.trim()
        if (city.isNotEmpty() && citySet.add(city.lowercase())) {
            destinations.add(
                TrendingDestination(
                    title = city,
                    imageUrl = destinationFallbackImage(city),
                )
            )
        }
    }

    return destinations.take(6)
}

/** City-specific fallback images matching iOS DestinationSummary fallback logic. */
private fun destinationFallbackImage(name: String): String = when (name.lowercase().trim()) {
    "new york", "manhattan", "brooklyn" -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=400&q=80"
    "los angeles" -> "https://images.unsplash.com/photo-1534190760961-74e8c1c5c3da?w=400&q=80"
    "san francisco" -> "https://images.unsplash.com/photo-1501594907352-04cda38ebc29?w=400&q=80"
    "chicago" -> "https://images.unsplash.com/photo-1494522855154-9297ac14b55f?w=400&q=80"
    "miami" -> "https://images.unsplash.com/photo-1533106497176-45ae19e68ba2?w=400&q=80"
    "seattle" -> "https://images.unsplash.com/photo-1502175353174-a7a70e73b4c3?w=400&q=80"
    "austin" -> "https://images.unsplash.com/photo-1531218150217-54595bc2b934?w=400&q=80"
    "denver" -> "https://images.unsplash.com/photo-1619856699906-09e1f4ef478b?w=400&q=80"
    "boston" -> "https://images.unsplash.com/photo-1501979376754-1d3b25f22a4e?w=400&q=80"
    "honolulu" -> "https://images.unsplash.com/photo-1507876466758-bc54f384809c?w=400&q=80"
    "maui" -> "https://images.unsplash.com/photo-1542259009477-d625272157b7?w=400&q=80"
    "grand canyon" -> "https://images.unsplash.com/photo-1474044159687-1ee9f3a51722?w=400&q=80"
    "nashville" -> "https://images.unsplash.com/photo-1545419913-775e2e168cd0?w=400&q=80"
    "portland" -> "https://images.unsplash.com/photo-1507245338956-79a3a4b41583?w=400&q=80"
    "san diego" -> "https://images.unsplash.com/photo-1538097304804-2a1b932466a9?w=400&q=80"
    "atlanta" -> "https://images.unsplash.com/photo-1575917649705-5b59aaa12e6b?w=400&q=80"
    "washington", "washington dc" -> "https://images.unsplash.com/photo-1501466044931-62695aada8e9?w=400&q=80"
    else -> "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=400&q=80"
}
