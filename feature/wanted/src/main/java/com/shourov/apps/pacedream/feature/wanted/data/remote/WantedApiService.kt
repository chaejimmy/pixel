package com.shourov.apps.pacedream.feature.wanted.data.remote

import com.shourov.apps.pacedream.core.network.ApiEndPoints
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.OfferEnvelope
import com.shourov.apps.pacedream.feature.wanted.data.dto.OffersResponse
import com.shourov.apps.pacedream.feature.wanted.data.dto.RequestEnvelope
import com.shourov.apps.pacedream.feature.wanted.data.dto.RequestsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WantedApiService {

    @GET(ApiEndPoints.GET_REQUESTS)
    suspend fun getRequests(
        /**
         * When `true`, the backend filters the feed to requests where
         * `authorId == currentUserId` so the "Mine" tab can render
         * without leaking other people's posts.
         */
        @Query("mine") mine: Boolean? = null,
    ): RequestsResponse

    @GET(ApiEndPoints.GET_REQUEST_BY_ID)
    suspend fun getRequest(@Path("id") id: String): RequestEnvelope

    @GET(ApiEndPoints.GET_REQUEST_OFFERS)
    suspend fun getOffersForRequest(@Path("id") requestId: String): OffersResponse

    /**
     * Offers index. With `mine=true` returns the offers the current user
     * has submitted (across all requests) — used to populate the host's
     * "My offers" tab.
     */
    @GET(ApiEndPoints.GET_OFFERS)
    suspend fun getOffers(@Query("mine") mine: Boolean? = null): OffersResponse

    @POST(ApiEndPoints.CREATE_REQUEST)
    suspend fun createRequest(@Body body: CreateRequestBody): RequestEnvelope

    @POST(ApiEndPoints.CREATE_REQUEST_OFFER)
    suspend fun createOffer(
        @Path("id") requestId: String,
        @Body body: CreateOfferBody,
    ): OfferEnvelope
}
