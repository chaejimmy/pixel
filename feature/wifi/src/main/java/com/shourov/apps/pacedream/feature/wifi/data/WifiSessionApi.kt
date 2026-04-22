package com.shourov.apps.pacedream.feature.wifi.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Backend Wi-Fi session endpoints. These are the only authoritative source
 * for `expiresAt` — the client never extrapolates expiry locally.
 *
 * Endpoints intentionally narrow for MVP:
 * - GET    /api/v1/wifi/sessions/{id}       → current state (used for polling)
 * - POST   /api/v1/wifi/sessions/{id}/extend → request an extension
 * - POST   /api/v1/wifi/sessions/{id}/reconnect → grace-period reconnect
 */
interface WifiSessionApi {

    @GET("api/v1/wifi/sessions/{id}")
    suspend fun getSession(@Path("id") sessionId: String): Response<WifiSessionResponse>

    @POST("api/v1/wifi/sessions/{id}/extend")
    suspend fun extendSession(
        @Path("id") sessionId: String,
        @Body body: WifiExtendRequest
    ): Response<WifiExtendResponse>

    @POST("api/v1/wifi/sessions/{id}/reconnect")
    suspend fun reconnect(@Path("id") sessionId: String): Response<WifiSessionResponse>
}
