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

import com.shourov.apps.pacedream.feature.home.domain.models.RentedGearModel
import com.shourov.apps.pacedream.feature.home.domain.models.SplitStayModel
import com.shourov.apps.pacedream.feature.home.domain.models.rooms.RoomModel


data class HomeScreenRoomsState(
    val rooms: List<RoomModel> = mutableListOf(),
    val error: String? = null,
    val loading: Boolean = true,
)

data class HomeScreenRentedGearsState(
    val rentedGears: List<RentedGearModel> = mutableListOf(),
    val error: String? = null,
    val loading: Boolean = true,
)

data class HomeScreenSplitStaysState(
    val splitStays: List<SplitStayModel> = mutableListOf(),
    val error: String? = null,
    val loading: Boolean = true,
)

/**
 * Combined home screen UI state for better state management
 */
data class HomeUiState(
    val roomsState: HomeScreenRoomsState = HomeScreenRoomsState(),
    val gearsState: HomeScreenRentedGearsState = HomeScreenRentedGearsState(),
    val splitStaysState: HomeScreenSplitStaysState = HomeScreenSplitStaysState(),
    val isRefreshing: Boolean = false,
)
