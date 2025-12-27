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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.*
import com.pacedream.common.util.showToast
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenRentedGearsState
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenRoomsState
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenSplitStaysState
import com.shourov.apps.pacedream.feature.home.presentation.R

/**
 * Enhanced Dashboard Screen that integrates all the new PaceDream design system components
 * This demonstrates how to use the enhanced components together
 * Now includes:
 * - Pull-to-refresh
 * - Split Stays section
 * - Inline warning banners for failed sections
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EnhancedDashboardScreen(
    roomsState: HomeScreenRoomsState,
    gearsState: HomeScreenRentedGearsState,
    splitStaysState: HomeScreenSplitStaysState = HomeScreenSplitStaysState(),
    isRefreshing: Boolean = false,
    onTimeBasedRoomsChanged: (String) -> Unit,
    onRentedGearsChanged: (String) -> Unit,
    onSplitStaysRetry: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onPropertyClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onViewAllClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Pull-to-refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PaceDreamColors.Background)
                .verticalScroll(rememberScrollState())
        ) {
            // Enhanced Header
            EnhancedDashboardHeader(
                userName = "Darryl Rutledge",
                onSearchClick = onSearchClick,
                onFilterClick = onFilterClick,
                onNotificationClick = onNotificationClick
            )
            
            // Enhanced Content with Split Stays
            EnhancedDashboardContent(
                roomsState = roomsState,
                gearsState = gearsState,
                splitStaysState = splitStaysState,
                onTimeBasedRoomsChanged = onTimeBasedRoomsChanged,
                onRentedGearsChanged = onRentedGearsChanged,
                onSplitStaysRetry = onSplitStaysRetry,
                onPropertyClick = onPropertyClick,
                onCategoryClick = onCategoryClick,
                onViewAllClick = onViewAllClick
            )
        }
        
        // Pull-to-refresh indicator
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = PaceDreamPrimary
        )
    }
}

/**
 * Compact version for smaller screens or when space is limited
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CompactDashboardScreen(
    roomsState: HomeScreenRoomsState,
    gearsState: HomeScreenRentedGearsState,
    splitStaysState: HomeScreenSplitStaysState = HomeScreenSplitStaysState(),
    isRefreshing: Boolean = false,
    onTimeBasedRoomsChanged: (String) -> Unit,
    onRentedGearsChanged: (String) -> Unit,
    onSplitStaysRetry: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onPropertyClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onViewAllClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PaceDreamColors.Background)
        ) {
            // Compact Header
            CompactDashboardHeader(
                title = "PaceDream",
                onSearchClick = onSearchClick,
                onNotificationClick = onNotificationClick
            )
            
            // Enhanced Content (same as full version)
            EnhancedDashboardContent(
                roomsState = roomsState,
                gearsState = gearsState,
                splitStaysState = splitStaysState,
                onTimeBasedRoomsChanged = onTimeBasedRoomsChanged,
                onRentedGearsChanged = onRentedGearsChanged,
                onSplitStaysRetry = onSplitStaysRetry,
                onPropertyClick = onPropertyClick,
                onCategoryClick = onCategoryClick,
                onViewAllClick = onViewAllClick
            )
        }
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = PaceDreamPrimary
        )
    }
}

/**
 * Minimal version for specific use cases
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MinimalDashboardScreen(
    roomsState: HomeScreenRoomsState,
    gearsState: HomeScreenRentedGearsState,
    splitStaysState: HomeScreenSplitStaysState = HomeScreenSplitStaysState(),
    isRefreshing: Boolean = false,
    onTimeBasedRoomsChanged: (String) -> Unit,
    onRentedGearsChanged: (String) -> Unit,
    onSplitStaysRetry: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onPropertyClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onViewAllClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PaceDreamColors.Background)
        ) {
            // Minimal Header
            MinimalDashboardHeader(
                title = "PaceDream",
                subtitle = "Find your perfect stay",
                onBackClick = onBackClick,
                onActionClick = onActionClick
            )
            
            // Enhanced Content (same as full version)
            EnhancedDashboardContent(
                roomsState = roomsState,
                gearsState = gearsState,
                splitStaysState = splitStaysState,
                onTimeBasedRoomsChanged = onTimeBasedRoomsChanged,
                onRentedGearsChanged = onRentedGearsChanged,
                onSplitStaysRetry = onSplitStaysRetry,
                onPropertyClick = onPropertyClick,
                onCategoryClick = onCategoryClick,
                onViewAllClick = onViewAllClick
            )
        }
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = PaceDreamPrimary
        )
    }
}
