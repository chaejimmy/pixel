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

package com.shourov.apps.pacedream.feature.home.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.pacedream.common.util.Consts.ROOM_TYPE
import com.pacedream.common.util.Consts.TECH_GEAR_TYPE
import com.shourov.apps.pacedream.feature.home.presentation.components.DashboardContent
import com.shourov.apps.pacedream.feature.home.presentation.components.DashboardHeader
import com.shourov.apps.pacedream.feature.home.presentation.components.EnhancedDashboardScreen

@Composable
fun DashboardScreen(
    modifier: Modifier,
    roomsState: HomeScreenRoomsState,
    gearsState: HomeScreenRentedGearsState,
    event: (HomeScreenEvent) -> Unit,
) {
    LaunchedEffect(Unit) {
        event(HomeScreenEvent.GetTimeBasedRooms(ROOM_TYPE))
        event(HomeScreenEvent.GetRentedGears(TECH_GEAR_TYPE))
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DashboardHeader()
        DashboardContent(
            roomsState, gearsState,
            onTimeBasedRoomsChanged = {
                event(HomeScreenEvent.GetTimeBasedRooms(it))
            },
            onRentedGearsChanged = {
                event(HomeScreenEvent.GetRentedGears(it))
            },
        )
    }
}

/**
 * Enhanced version of DashboardScreen using the new design system
 * This provides a more modern, iOS-matching UI experience with:
 * - All three sections: Hourly Spaces, Rent Gear, Split Stays
 * - Parallel loading for all sections
 * - Pull-to-refresh
 * - Inline warning banners for failed sections
 * - View All navigation
 */
@Composable
fun EnhancedDashboardScreenWrapper(
    modifier: Modifier,
    roomsState: HomeScreenRoomsState,
    gearsState: HomeScreenRentedGearsState,
    splitStaysState: HomeScreenSplitStaysState = HomeScreenSplitStaysState(),
    isRefreshing: Boolean = false,
    event: (HomeScreenEvent) -> Unit,
) {
    // Load all sections in parallel on initial load
    LaunchedEffect(Unit) {
        event(HomeScreenEvent.LoadAllSections(
            roomType = ROOM_TYPE,
            gearType = TECH_GEAR_TYPE
        ))
    }

    EnhancedDashboardScreen(
        roomsState = roomsState,
        gearsState = gearsState,
        splitStaysState = splitStaysState,
        isRefreshing = isRefreshing,
        onTimeBasedRoomsChanged = { type ->
            event(HomeScreenEvent.GetTimeBasedRooms(type))
        },
        onRentedGearsChanged = { type ->
            event(HomeScreenEvent.GetRentedGears(type))
        },
        onSplitStaysRetry = {
            event(HomeScreenEvent.GetSplitStays)
        },
        onRefresh = {
            event(HomeScreenEvent.RefreshAll)
        },
        onPropertyClick = { propertyId ->
            // Navigate to property detail screen
            event(HomeScreenEvent.NavigateToSection("property:$propertyId"))
        },
        onCategoryClick = { category ->
            // Navigate to category listing
            event(HomeScreenEvent.NavigateToSection("category:$category"))
        },
        onViewAllClick = { section ->
            // Navigate to section listing
            event(HomeScreenEvent.NavigateToSection(section))
        },
        onSearchClick = {
            event(HomeScreenEvent.NavigateToSection("search"))
        },
        onFilterClick = {
            event(HomeScreenEvent.NavigateToSection("filters"))
        },
        onNotificationClick = {
            event(HomeScreenEvent.NavigateToSection("notifications"))
        },
        modifier = modifier
    )
}
