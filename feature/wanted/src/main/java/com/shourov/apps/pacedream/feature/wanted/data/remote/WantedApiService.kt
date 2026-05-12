package com.shourov.apps.pacedream.feature.wanted.data.remote

import com.google.gson.JsonElement
import com.shourov.apps.pacedream.core.network.ApiEndPoints
import com.shourov.apps.pacedream.feature.wanted.data.dto.CategoriesResponse
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateRequestBody
import com.shourov.apps.pacedream.feature.wanted.data.dto.OfferEnvelope
import com.shourov.apps.pacedream.feature.wanted.data.dto.RequestEnvelope
import com.shourov.apps.pacedream.feature.wanted.data.dto.RequestsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface WantedApiService {

    @GET(ApiEndPoints.GET_REQUESTS)
    suspend fun getRequests(): RequestsResponse

    @GET(ApiEndPoints.GET_REQUEST_BY_ID)
    suspend fun getRequest(@Path("id") id: String): RequestEnvelope

    @POST(ApiEndPoints.CREATE_REQUEST)
    suspend fun createRequest(@Body body: CreateRequestBody): RequestEnvelope

    @POST(ApiEndPoints.CREATE_REQUEST_OFFER)
    suspend fun createOffer(
        @Path("id") requestId: String,
        @Body body: CreateOfferBody,
    ): OfferEnvelope

    @GET(ApiEndPoints.GET_REQUEST_CATEGORIES)
    suspend fun getCategories(): CategoriesResponse

    /**
     * Returns the authenticated host's listings. The response shape varies
     * (raw array, `{data: […]}`, or category-keyed `{rooms, rentableItems,
     * …}`), so the body is decoded as a raw [JsonElement] and the repository
     * extracts only the (id, title) summaries the offer composer needs.
     */
    @GET(ApiEndPoints.HOST_GET_LISTINGS)
    suspend fun getHostListings(): JsonElement
}
