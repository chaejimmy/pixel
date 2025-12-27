package com.shourov.apps.pacedream.feature.home.domain.repository

import com.shourov.apps.pacedream.feature.home.domain.models.RentedGearModel
import com.shourov.apps.pacedream.feature.home.domain.models.SplitStayModel
import com.shourov.apps.pacedream.feature.home.domain.models.rooms.RoomModel
import kotlinx.coroutines.flow.Flow

interface HomeRepository {

    suspend fun getRentedGears(type: String): Flow<List<RentedGearModel>>
    suspend fun getTimeBasedRooms(itemType: String): Flow<List<RoomModel>>
    suspend fun getSplitStays(): Flow<List<SplitStayModel>>
}