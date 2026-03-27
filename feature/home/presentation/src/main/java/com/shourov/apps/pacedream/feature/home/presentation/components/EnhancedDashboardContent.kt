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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenRentedGearsState
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenRoomsState
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenSplitStaysState

/**
 * Inline warning banner component for section errors.
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
 * Enhanced Dashboard Content
 *
 * Section order (unified hierarchy):
 * 1. Category Rail — Spaces / Items / Services (taxonomy-aligned)
 * 2. Featured Listings — one highlighted horizontal row from the first
 *    data source that has content (gives immediate value on load)
 * 3. Explore by Category — segmented Spaces / Items / Services browser
 *    with subcategory chips and inline listing previews
 * 4. Trending Destinations
 * 5. Global empty / error state
 *
 * The previous layout showed three near-identical sections (Spaces, Items,
 * Services) each with their own LazyRow, and then a Browse by Type section
 * that duplicated the same data.  This revision consolidates them into a
 * single featured row + the interactive Browse by Type explorer, which
 * eliminates redundancy and gives users a clearer mental model.
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
        // ── 1. Category Rail ────────────────────────────────────────────
        item {
            QuickChipsSection(
                onChipClick = { chip -> onCategoryClick(chip) }
            )
        }

        // ── 2. Featured Listings ────────────────────────────────────────
        // Show one curated horizontal section from the first available data
        // source.  This gives the user immediate visual content while loading.
        item {
            FeaturedListingsSection(
                roomsState = roomsState,
                gearsState = gearsState,
                splitStaysState = splitStaysState,
                onPropertyClick = onPropertyClick,
                onViewAllClick = onViewAllClick,
                onRetryRooms = { onTimeBasedRoomsChanged("room") },
                onRetryGears = { onRentedGearsChanged("tech_gear") },
                onRetrySplitStays = onSplitStaysRetry,
            )
        }

        // ── 3. Explore by Category ──────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))
            BrowseByTypeSection(
                roomsState = roomsState,
                gearsState = gearsState,
                splitStaysState = splitStaysState,
                onPropertyClick = onPropertyClick,
                onViewAllClick = onViewAllClick,
                onSubcategoryClick = { _, sub -> onCategoryClick(sub.title) },
            )
        }

        // ── 4. Trending Destinations ────────────────────────────────────
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

        // ── 5. Global empty / error state ───────────────────────────────
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
 * Featured Listings — a single highlighted row showing the best available
 * content across all three pillars.  Renders the first non-empty data source
 * as the primary spotlight, giving users immediate visual value.
 */
@Composable
private fun FeaturedListingsSection(
    roomsState: HomeScreenRoomsState,
    gearsState: HomeScreenRentedGearsState,
    splitStaysState: HomeScreenSplitStaysState,
    onPropertyClick: (String) -> Unit,
    onViewAllClick: (String) -> Unit,
    onRetryRooms: () -> Unit,
    onRetryGears: () -> Unit,
    onRetrySplitStays: () -> Unit,
) {
    // Determine which section to feature — first one with data wins
    val hasRooms = roomsState.rooms.isNotEmpty()
    val hasGears = gearsState.rentedGears.isNotEmpty()
    val hasServices = splitStaysState.splitStays.isNotEmpty()
    val anyLoading = roomsState.loading || gearsState.loading || splitStaysState.loading

    // Pick the primary section to highlight
    data class FeaturedConfig(
        val title: String,
        val subtitle: String,
        val sectionKey: String,
        val error: String?,
        val isLoading: Boolean,
        val onRetry: () -> Unit,
    )

    val featured = when {
        hasRooms || roomsState.loading -> FeaturedConfig(
            title = "Featured Spaces",
            subtitle = "Flexible spaces nearby — parking, rooms, pods & more",
            sectionKey = "time-based",
            error = roomsState.error,
            isLoading = roomsState.loading,
            onRetry = onRetryRooms,
        )
        hasGears || gearsState.loading -> FeaturedConfig(
            title = "Featured Items",
            subtitle = "Rent what you need — cameras, gear, tech & tools",
            sectionKey = "gear",
            error = gearsState.error,
            isLoading = gearsState.loading,
            onRetry = onRetryGears,
        )
        hasServices || splitStaysState.loading -> FeaturedConfig(
            title = "Featured Services",
            subtitle = "Book help when you need it — cleaning, moving & more",
            sectionKey = "services",
            error = splitStaysState.error,
            isLoading = splitStaysState.loading,
            onRetry = onRetrySplitStays,
        )
        else -> null
    }

    if (featured != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            // Section header
            SectionHeader(
                title = featured.title,
                subtitle = featured.subtitle,
                onViewAllClick = { onViewAllClick(featured.sectionKey) },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Error banner
            if (!featured.error.isNullOrEmpty() && !featured.isLoading) {
                SectionWarningBanner(
                    message = featured.error,
                    onRetry = featured.onRetry,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Content row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (featured.isLoading) {
                    items(3) { PaceDreamShimmerCard() }
                } else {
                    when {
                        hasRooms -> items(
                            roomsState.rooms.take(10),
                            key = { it.id }
                        ) { room ->
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
                        hasGears -> items(
                            gearsState.rentedGears.take(10),
                            key = { it.id }
                        ) { gear ->
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
                        hasServices -> items(
                            splitStaysState.splitStays.take(10),
                            key = { it._id ?: it.hashCode() }
                        ) { stay ->
                            val priceUnit = stay.priceUnit?.lowercase()?.let { unit ->
                                when {
                                    unit.contains("hour") || unit == "hr" -> "hr"
                                    unit.contains("day") -> "day"
                                    unit.contains("week") -> "wk"
                                    unit.contains("month") -> "mo"
                                    unit == "once" || unit == "total" -> "total"
                                    else -> unit
                                }
                            } ?: "hr"
                            PaceDreamPropertyCard(
                                title = stay.name ?: "Service",
                                location = stay.location ?: stay.city ?: "Location",
                                price = "$${stay.price?.toInt() ?: "0"}/$priceUnit",
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
}

/**
 * Reusable section header with title, subtitle, and View All action.
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onViewAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = PaceDreamTypography.Title2,
                color = PaceDreamTextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (onViewAllClick != null) {
                TextButton(
                    onClick = onViewAllClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "View All",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamPrimary,
                            fontWeight = FontWeight.Medium,
                        )
                        Icon(
                            imageVector = PaceDreamIcons.ChevronRight,
                            contentDescription = null,
                            tint = PaceDreamPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = PaceDreamTypography.Footnote,
                color = PaceDreamTextSecondary,
            )
        }
    }
}

/**
 * Build trending destinations from loaded data — extracts unique city names.
 */
private fun buildTrendingDestinations(
    roomsState: HomeScreenRoomsState,
    gearsState: HomeScreenRentedGearsState,
): List<TrendingDestination> {
    val citySet = mutableSetOf<String>()
    val destinations = mutableListOf<TrendingDestination>()

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

/** City-specific fallback images. */
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
