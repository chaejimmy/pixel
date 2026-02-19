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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pacedream.common.composables.tabs.GeneralTab
import com.pacedream.common.composables.theme.LargePadding
import com.pacedream.common.composables.theme.SmallText
import com.pacedream.common.composables.theme.SubHeadingColor
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
import com.shourov.apps.pacedream.feature.home.presentation.R

@Composable
fun DashboardContent(
    roomsState: HomeScreenRoomsState,
    gearsState: HomeScreenRentedGearsState,
    onTimeBasedRoomsChanged: (String) -> Unit,
    onRentedGearsChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(roomsState.error) {
        if (!roomsState.error.isNullOrEmpty()) {
            context.showToast(roomsState.error)
        }
    }
    LaunchedEffect(gearsState.error) {
        if (!gearsState.error.isNullOrEmpty()) {
            context.showToast(gearsState.error)
        }
    }


    Column(modifier = Modifier.padding(LargePadding)) {
        val categories = mutableListOf(
            CategoryModel(
                stringResource(R.string.feature_home_rest_room),
                R.drawable.ic_rest_room,
                Color(0xFF21BDF2),
            ),
            CategoryModel(
                stringResource(R.string.feature_home_nap_pod),
                R.drawable.ic_nap_pod,
                Color(0xFF8B5CF6),
            ),
            CategoryModel(
                stringResource(R.string.feature_home_meeting_room),
                R.drawable.ic_meeting_room,
                Color(0xFF3B82F6),
            ),
            CategoryModel(
                stringResource(R.string.feature_home_study_room),
                R.drawable.ic_study_room,
                Color(0xFF10B981),
            ),
            CategoryModel(
                stringResource(R.string.feature_home_short_stay),
                R.drawable.ic_short_stay,
                Color(0xFFF59E0B),
            ),
            CategoryModel(
                stringResource(R.string.feature_home_apartment),
                R.drawable.ic_apartment,
                Color(0xFFEF4444),
            ),
            CategoryModel(
                stringResource(R.string.feature_home_parking),
                R.drawable.ic_ev_parking,
                Color(0xCCB452DA),
            ),
            CategoryModel(
                stringResource(R.string.feature_home_storage_space),
                R.drawable.ic_storage_room,
                Color(0xCC5753FA),
            ),
        )

        val destinations = mutableListOf(
            DestinationModel("London", R.drawable.london),
            DestinationModel("New York", R.drawable.new_york),
            DestinationModel("Tokyo", R.drawable.tokyo),
            DestinationModel("Toronto", R.drawable.toronto),
        )
        TitleViewAll(stringResource(R.string.feature_home_categories), true, 8)
        LazyRow {
            items(categories) {
                CategoryCard(it) {

                }
            }
        }
        TitleViewAll(stringResource(R.string.recent_searches), false, 12)
        RecentSearchCard(Modifier) {

        }
        TitleViewAll(stringResource(R.string.browse_by_destination), true)
        Text(
            stringResource(R.string.feature_home_explore_perfect_places_by_destination),
            fontSize = SmallText,
            color = SubHeadingColor,
        )
        LazyRow {
            items(destinations) {
                DestinationCard(it) {

                }
            }
        }

        TitleViewAll(stringResource(R.string.time_based), true)
        HelpYouFindText()

        var timeBasedSelectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
        val timeBasedTabs = listOf("Room", "Restroom", "EV Parking", "Parking")

        GeneralTab(
            tabs = timeBasedTabs,
            selectedTabIndex = timeBasedSelectedTabIndex,
            onTabSelected = { index ->
                timeBasedSelectedTabIndex = index
                onTimeBasedRoomsChanged(
                    when (index) {
                        0 -> Consts.ROOM_TYPE
                        1 -> Consts.REST_ROOM_TYPE
                        2 -> Consts.EV_PARKING_TYPE
                        3 -> Consts.PARKING_TYPE
                        else -> Consts.ROOM_TYPE
                    },
                )
            },
        )

        if (roomsState.loading) {
            ShimmerEffect()
        } else {
            LazyRow {
                items(roomsState.rooms) {
                    DealsCard(it) {

                    }
                }
            }
        }

        TitleViewAll(stringResource(R.string.hourly_rented_gear), true)
        HelpYouFindText()
        var rentedGearsSelectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
        val rentedGearTabs = listOf("Tech Gear", "Music Gear", "Photography", "Fashion")

        GeneralTab(
            tabs = rentedGearTabs,
            selectedTabIndex = rentedGearsSelectedTabIndex,
            onTabSelected = { index ->
                rentedGearsSelectedTabIndex = index
                onRentedGearsChanged(
                    when (index) {
                        0 -> TECH_GEAR_TYPE
                        1 -> MUSIC_GEAR_TYPE
                        2 -> PHOTOGRAPHY_TYPE
                        3 -> FASHION_TYPE
                        else -> TECH_GEAR_TYPE
                    },
                )
            },
        )

        if (gearsState.loading) {
            ShimmerEffect()
        } else {
            LazyRow {
                items(gearsState.rentedGears) {
                    RentedGearDealsCard(it) {

                    }
                }
            }
        }

    }
}

@Composable
fun HelpYouFindText() {
    Text(
        stringResource(R.string.help_you_what_needed),
        fontSize = SmallText,
        color = SubHeadingColor,
    )
}

@Composable
private fun ShimmerEffect() {
    Row {
        repeat(10) {
            DealCardShimmer()
        }
    }
}