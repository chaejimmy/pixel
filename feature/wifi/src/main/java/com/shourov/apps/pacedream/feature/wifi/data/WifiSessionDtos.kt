package com.shourov.apps.pacedream.feature.wifi.data

import com.google.gson.annotations.SerializedName

/**
 * Server response for an active Wi-Fi access session.
 *
 * `expiresAt` is server-authoritative ISO-8601 (UTC). The client must never
 * fabricate or extrapolate this value from local time alone — instead, anchor
 * a local ticker to it and reconcile every 30s by re-reading from the server.
 */
data class WifiSessionResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("booking_id") val bookingId: String?,
    @SerializedName("ssid") val ssid: String?,
    @SerializedName("password") val password: String?,
    @SerializedName("status") val status: String, // active | expired | ended
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("expires_at") val expiresAt: String,
    @SerializedName("can_extend") val canExtend: Boolean = true,
    @SerializedName("grace_seconds") val graceSeconds: Long = 0L
)

data class WifiExtendRequest(
    @SerializedName("minutes") val minutes: Int
)

data class WifiExtendResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("expires_at") val expiresAt: String,
    @SerializedName("can_extend") val canExtend: Boolean = true
)
