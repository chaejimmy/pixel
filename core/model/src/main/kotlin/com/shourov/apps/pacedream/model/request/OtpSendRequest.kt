package com.shourov.apps.pacedream.model.request

import com.google.gson.annotations.SerializedName

/**
 * Request model for sending OTP
 * POST /auth/otp/send
 */
data class OtpSendRequest(
    @SerializedName("phone")
    val phone: String
)
