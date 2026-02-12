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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.pacedream.common.util.Consts
import com.pacedream.common.util.Consts.FASHION_TYPE
import com.pacedream.common.util.Consts.MUSIC_GEAR_TYPE
import com.pacedream.common.util.Consts.PHOTOGRAPHY_TYPE
import com.pacedream.common.util.Consts.TECH_GEAR_TYPE
import com.pacedream.common.util.showToast
import com.shourov.apps.pacedream.feature.home.domain.models.CategoryModel
import com.shourov.apps.pacedream.feature.home.domain.models.DestinationModel
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenRentedGearsState
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenRoomsState
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenSplitStaysState
import com.shourov.apps.pacedream.feature.home.presentation.R
import kotlin.math.abs

/**
 * Inline warning banner component for section errors
 * Matches iOS behavior of showing inline warnings instead of just toasts
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
                imageVector = Icons.Default.Warning,
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
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = PaceDreamPrimary
                    )
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
    onDestinationClick: (String) -> Unit = {},
    onViewAllClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaceDreamBackground)
            .padding(PaceDreamSpacing.LG)
    ) {
        // Metrics Cards Section - derived from actual loaded data
        item {
            val roomCount = roomsState.rooms.size
            val gearCount = gearsState.rentedGears.size
            val splitCount = splitStaysState.splitStays.size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                PaceDreamMetricCard(
                    title = "Available Rooms",
                    value = if (roomsState.loading) "—" else roomCount.toString(),
                    icon = Icons.Default.Home,
                    modifier = Modifier.weight(1f)
                )
                PaceDreamMetricCard(
                    title = "Rent Gear",
                    value = if (gearsState.loading) "—" else gearCount.toString(),
                    icon = Icons.Default.ShoppingBag,
                    modifier = Modifier.weight(1f)
                )
                PaceDreamMetricCard(
                    title = "Split Stays",
                    value = if (splitStaysState.loading) "—" else splitCount.toString(),
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Categories Section
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            PaceDreamSectionHeader(
                title = "Browse by Category",
                onViewAllClick = { onViewAllClick("categories") }
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            val categories = listOf(
                CategoryModel(
                    stringResource(R.string.feature_home_entire_home),
                    R.drawable.ic_apartment,
                    Color(0xFFEF4444),
                ),
                CategoryModel(
                    stringResource(R.string.feature_home_private_room),
                    R.drawable.ic_luxury_room,
                    Color(0xFFEC4899),
                ),
                CategoryModel(
                    stringResource(R.string.feature_home_rest_room),
                    R.drawable.ic_rest_room,
                    Color(0xFF21BDF2),
                ),
                CategoryModel(
                    stringResource(R.string.feature_home_nap_room),
                    R.drawable.ic_nap_pod,
                    Color(0xFF8B5CF6),
                ),
                CategoryModel(
                    stringResource(R.string.feature_home_meeting_room),
                    R.drawable.ic_meeting_room,
                    Color(0xFF3B82F6),
                ),
                CategoryModel(
                    stringResource(R.string.feature_home_workspace),
                    R.drawable.ic_study_room,
                    Color(0xFF10B981),
                ),
                CategoryModel(
                    stringResource(R.string.feature_home_ev_parking),
                    R.drawable.ic_ev_parking,
                    Color(0xCCB452DA),
                ),
                CategoryModel(
                    stringResource(R.string.feature_home_nap_pod),
                    R.drawable.ic_nap_pod,
                    Color(0xFF7C3AED),
                ),
                CategoryModel(
                    stringResource(R.string.feature_home_study_room),
                    R.drawable.ic_study_room,
                    Color(0xFF059669),
                ),
                CategoryModel(
                    stringResource(R.string.feature_home_short_stay),
                    R.drawable.ic_short_stay,
                    Color(0xFFF59E0B),
                ),
                CategoryModel(
                    stringResource(R.string.feature_home_apartment),
                    R.drawable.ic_apartment,
                    Color(0xFFDC2626),
                ),
                CategoryModel(
                    stringResource(R.string.feature_home_parking),
                    R.drawable.ic_ev_parking,
                    Color(0xFF6366F1),
                ),
                CategoryModel(
                    stringResource(R.string.feature_home_storage_space),
                    R.drawable.ic_storage_room,
                    Color(0xCC5753FA),
                ),
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                items(categories) { category ->
                    PaceDreamCategoryPill(
                        title = category.title,
                        icon = when (category.title) {
                            stringResource(R.string.feature_home_rest_room) -> Icons.Default.Bed
                            stringResource(R.string.feature_home_ev_parking) -> Icons.Default.ElectricCar
                            stringResource(R.string.feature_home_storage_room) -> Icons.Default.Storage
                            stringResource(R.string.feature_home_parking_spot) -> Icons.Default.LocalParking
                            else -> Icons.Default.Category
                        },
                        isSelected = false,
                        onClick = { onCategoryClick(category.title) }
                    )
                }
            }
        }
        
        // Recent Searches Section - starts empty until persistent history is wired
        // No hardcoded seed data; actual search history will be populated from local storage
        
        // Browse by Destination Section
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            PaceDreamSectionHeader(
                title = "Browse by Destination",
                onViewAllClick = { onViewAllClick("destinations") }
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            Text(
                text = stringResource(R.string.feature_home_explore_perfect_places_by_destination),
                style = PaceDreamTypography.Callout,
                color = PaceDreamTextSecondary
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            val destinations = listOf(
                DestinationModel("London", R.drawable.london),
                DestinationModel("New York", R.drawable.new_york),
                DestinationModel("Tokyo", R.drawable.tokyo),
                DestinationModel("Toronto", R.drawable.toronto),
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                items(destinations) { destination ->
                    PaceDreamDestinationCard(
                        name = destination.title,
                        imageUrl = null, // Will use drawable resource
                        onClick = { onDestinationClick(destination.title) }
                    )
                }
            }
        }
        
        // Last-Minute Deals Section (Web parity: 20-40% discounts on available spaces)
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            PaceDreamSectionHeader(
                title = stringResource(R.string.feature_home_last_minute_deals),
                onViewAllClick = { onViewAllClick("last-minute-deals") }
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = stringResource(R.string.feature_home_last_minute_deals_subtitle),
                style = PaceDreamTypography.Callout,
                color = PaceDreamTextSecondary
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            if (roomsState.loading) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(3) {
                        PaceDreamShimmerCard()
                    }
                }
            } else if (roomsState.rooms.isNotEmpty()) {
                // Show available rooms as last-minute deals with dynamic discount
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    val dealRooms = roomsState.rooms.filter { it.available }.take(6)
                    items(dealRooms) { room ->
                        val discountPercent = 20 + (abs(room.id.hashCode()) % 21) // 20-40%
                        val spotsLeft = 1 + (abs(room.id.hashCode()) % 5) // 1-5
                        LastMinuteDealCard(
                            roomModel = room,
                            discountPercent = discountPercent,
                            spotsLeft = spotsLeft,
                            onClick = { onPropertyClick(room.id) }
                        )
                    }
                }
            } else {
                PaceDreamEmptyState(
                    title = "No Deals Right Now",
                    description = "Check back later for last-minute deals on available spaces.",
                    icon = Icons.Default.LocalOffer
                )
            }
        }

        // Find Roommate Section (Web parity: Roommate Finder feature)
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            PaceDreamSectionHeader(
                title = stringResource(R.string.feature_home_find_roommate),
                onViewAllClick = { onViewAllClick("roommate") }
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = stringResource(R.string.feature_home_find_roommate_subtitle),
                style = PaceDreamTypography.Callout,
                color = PaceDreamTextSecondary
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Inline warning banner if section failed
            if (!splitStaysState.error.isNullOrEmpty() && !splitStaysState.loading) {
                SectionWarningBanner(
                    message = splitStaysState.error,
                    onRetry = onSplitStaysRetry
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            }

            if (splitStaysState.loading) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(3) {
                        PaceDreamShimmerCard()
                    }
                }
            } else if (splitStaysState.splitStays.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(splitStaysState.splitStays) { stay ->
                        PaceDreamPropertyCard(
                            title = stay.name ?: "Roommate Listing",
                            location = stay.location ?: stay.city ?: "Location",
                            price = "$${stay.price ?: "0"}/${stay.priceUnit ?: "month"}",
                            rating = stay.rating?.toDouble() ?: 0.0,
                            reviewCount = stay.reviewCount ?: 0,
                            imageUrl = stay.images?.firstOrNull(),
                            onClick = { onPropertyClick(stay._id ?: "") }
                        )
                    }
                }
            } else {
                PaceDreamEmptyState(
                    title = "No Roommate Listings",
                    description = "Be the first to post a roommate listing, or check back later.",
                    icon = Icons.Default.People
                )
            }
        }

        // Hourly Spaces Section (Time-based Properties)
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            PaceDreamSectionHeader(
                title = "Hourly Spaces",
                onViewAllClick = { onViewAllClick("time-based") }
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            Text(
                text = stringResource(R.string.help_you_what_needed),
                style = PaceDreamTypography.Callout,
                color = PaceDreamTextSecondary
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            // Inline warning banner if section failed
            if (!roomsState.error.isNullOrEmpty() && !roomsState.loading) {
                SectionWarningBanner(
                    message = roomsState.error,
                    onRetry = { onTimeBasedRoomsChanged(Consts.ROOM_TYPE) }
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            }
            
            var timeBasedSelectedTabIndex by remember { mutableIntStateOf(0) }
            val timeBasedTabs = listOf("Room", "Restroom", "EV Parking", "Parking")
            
            // Enhanced Tab Component
            ScrollableTabRow(
                selectedTabIndex = timeBasedSelectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                containerColor = PaceDreamSurface,
                contentColor = PaceDreamTextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[timeBasedSelectedTabIndex]),
                        color = PaceDreamPrimary,
                        height = 3.dp
                    )
                }
            ) {
                timeBasedTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = timeBasedSelectedTabIndex == index,
                        onClick = {
                            timeBasedSelectedTabIndex = index
                            onTimeBasedRoomsChanged(
                                when (index) {
                                    0 -> Consts.ROOM_TYPE
                                    1 -> Consts.REST_ROOM_TYPE
                                    2 -> Consts.EV_PARKING_TYPE
                                    3 -> Consts.PARKING_TYPE
                                    else -> Consts.ROOM_TYPE
                                }
                            )
                        },
                        text = {
                            Text(
                                text = title,
                                style = PaceDreamTypography.Callout,
                                color = if (timeBasedSelectedTabIndex == index) 
                                    PaceDreamPrimary else PaceDreamTextSecondary
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            if (roomsState.loading) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(5) {
                        PaceDreamShimmerCard()
                    }
                }
            } else if (roomsState.error.isNullOrEmpty() && roomsState.rooms.isEmpty()) {
                PaceDreamEmptyState(
                    title = "No Properties Found",
                    description = "Try adjusting your search criteria or check back later.",
                    icon = Icons.Default.Search
                )
            } else if (roomsState.rooms.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(roomsState.rooms) { room ->
                        PaceDreamPropertyCard(
                            title = room.title,
                            location = room.location.city,
                            price = "$${room.price?.firstOrNull()?.amount ?: 0}/hour",
                            rating = room.rating.toDouble(),
                            reviewCount = 0, // TODO: Add review count to model
                            imageUrl = room.gallery.images.firstOrNull(),
                            onClick = { onPropertyClick(room.id) }
                        )
                    }
                }
            }
        }
        
        // Rent Gear Section (Hourly Rented Gear)
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            PaceDreamSectionHeader(
                title = "Rent Gear",
                onViewAllClick = { onViewAllClick("gear") }
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            Text(
                text = stringResource(R.string.help_you_what_needed),
                style = PaceDreamTypography.Callout,
                color = PaceDreamTextSecondary
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            // Inline warning banner if section failed
            if (!gearsState.error.isNullOrEmpty() && !gearsState.loading) {
                SectionWarningBanner(
                    message = gearsState.error,
                    onRetry = { onRentedGearsChanged(TECH_GEAR_TYPE) }
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            }
            
            var rentedGearsSelectedTabIndex by remember { mutableIntStateOf(0) }
            val rentedGearTabs = listOf("Tech Gear", "Music Gear", "Photography", "Fashion")
            
            // Enhanced Tab Component
            ScrollableTabRow(
                selectedTabIndex = rentedGearsSelectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                containerColor = PaceDreamSurface,
                contentColor = PaceDreamTextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[rentedGearsSelectedTabIndex]),
                        color = PaceDreamPrimary,
                        height = 3.dp
                    )
                }
            ) {
                rentedGearTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = rentedGearsSelectedTabIndex == index,
                        onClick = {
                            rentedGearsSelectedTabIndex = index
                            onRentedGearsChanged(
                                when (index) {
                                    0 -> TECH_GEAR_TYPE
                                    1 -> MUSIC_GEAR_TYPE
                                    2 -> PHOTOGRAPHY_TYPE
                                    3 -> FASHION_TYPE
                                    else -> TECH_GEAR_TYPE
                                }
                            )
                        },
                        text = {
                            Text(
                                text = title,
                                style = PaceDreamTypography.Callout,
                                color = if (rentedGearsSelectedTabIndex == index) 
                                    PaceDreamPrimary else PaceDreamTextSecondary
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            if (gearsState.loading) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(5) {
                        PaceDreamShimmerCard()
                    }
                }
            } else if (gearsState.error.isNullOrEmpty() && gearsState.rentedGears.isEmpty()) {
                PaceDreamEmptyState(
                    title = "No Gear Available",
                    description = "Check back later for available rental gear.",
                    icon = Icons.Default.ShoppingBag
                )
            } else if (gearsState.rentedGears.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(gearsState.rentedGears) { gear ->
                        PaceDreamPropertyCard(
                            title = gear.name,
                            location = gear.location,
                            price = "$${gear.hourlyRate}/hour",
                            rating = 0.0, // Gear model doesn't have rating
                            reviewCount = 0, // TODO: Add review count to model
                            imageUrl = gear.images?.firstOrNull(),
                            onClick = { onPropertyClick(gear.id) }
                        )
                    }
                }
            }
        }
        
        // Split Stays Section (NEW - matches iOS)
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            PaceDreamSectionHeader(
                title = "Split Stays",
                onViewAllClick = { onViewAllClick("split-stays") }
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            Text(
                text = "Find roommates and share costs",
                style = PaceDreamTypography.Callout,
                color = PaceDreamTextSecondary
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            // Inline warning banner if section failed
            if (!splitStaysState.error.isNullOrEmpty() && !splitStaysState.loading) {
                SectionWarningBanner(
                    message = splitStaysState.error,
                    onRetry = onSplitStaysRetry
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            }
            
            if (splitStaysState.loading) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(5) {
                        PaceDreamShimmerCard()
                    }
                }
            } else if (splitStaysState.error.isNullOrEmpty() && splitStaysState.splitStays.isEmpty()) {
                PaceDreamEmptyState(
                    title = "No Split Stays Available",
                    description = "Check back later for shared accommodation options.",
                    icon = Icons.Default.People
                )
            } else if (splitStaysState.splitStays.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(splitStaysState.splitStays) { stay ->
                        PaceDreamPropertyCard(
                            title = stay.name ?: "Split Stay",
                            location = stay.location ?: stay.city ?: "Location",
                            price = "$${stay.price ?: "0"}/${stay.priceUnit ?: "night"}",
                            rating = stay.rating?.toDouble() ?: 0.0,
                            reviewCount = stay.reviewCount ?: 0,
                            imageUrl = stay.images?.firstOrNull(),
                            onClick = { onPropertyClick(stay._id ?: "") }
                        )
                    }
                }
            }
        }
        
        // Bottom padding for better scrolling
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))
        }
    }
}
