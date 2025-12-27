package com.shourov.apps.pacedream.feature.home.data.remote

import com.shourov.apps.pacedream.core.network.ApiEndPoints
import com.shourov.apps.pacedream.feature.home.data.dto.retned_gears.RentedGearResponse
import com.shourov.apps.pacedream.feature.home.data.dto.split_stays.SplitStayResponse
import com.shourov.apps.pacedream.feature.home.data.dto.time_based_deals.TimeBasedDealsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HomeApiService {

    @GET(ApiEndPoints.HOURLY_RENTED_GEAR)
    suspend fun getHourlyRentedGears(
        @Path("type") type: String,
    ): RentedGearResponse

    @GET(ApiEndPoints.GET_TIMEBASE_ROOM)
    suspend fun getTimeBasedRooms(
        @Query("item_type") itemType: String,
    ): TimeBasedDealsResponse

    @GET(ApiEndPoints.ROOMMATE_ROOM_STAY)
    suspend fun getSplitStays(): SplitStayResponse
}