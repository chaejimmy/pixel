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

package com.shourov.apps.pacedream.feature.home.data.repository

import com.shourov.apps.pacedream.feature.home.data.dto.retned_gears.toRentedGearModel
import com.shourov.apps.pacedream.feature.home.data.dto.split_stays.toSplitStayModel
import com.shourov.apps.pacedream.feature.home.data.dto.time_based_deals.toRoomModel
import com.shourov.apps.pacedream.feature.home.data.remote.HomeApiService
import com.shourov.apps.pacedream.feature.home.domain.repository.HomeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class HomeRepositoryImpl(
    private val homeApiService: HomeApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : HomeRepository {
    override suspend fun getRentedGears(type: String) = flow {
        val response = homeApiService.getHourlyRentedGears(type)
        if (response.status) {
            val models = response.data.map {
                it.toRentedGearModel()
            }
            emit(models)
        } else throw Throwable(response.message)
    }.flowOn(dispatcher)

    override suspend fun getTimeBasedRooms(itemType: String) = flow {
        val response = homeApiService.getTimeBasedRooms(itemType)
        if (response.status) {
            val rooms = response.data.map {
                it.toRoomModel()
            }
            emit(rooms)
        } else throw Throwable(response.message)
    }.flowOn(dispatcher)

    override suspend fun getSplitStays() = flow {
        val response = homeApiService.getSplitStays()
        if (response.status) {
            val splitStays = response.data.map {
                it.toSplitStayModel()
            }
            emit(splitStays)
        } else throw Throwable(response.message)
    }.flowOn(dispatcher)
}