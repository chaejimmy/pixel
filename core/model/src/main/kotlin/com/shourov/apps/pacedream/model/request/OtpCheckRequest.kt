package com.shourov.apps.pacedream.model.request

import com.google.gson.annotations.SerializedName

/**
 * Request model for verifying OTP
 * POST /auth/otp/check
 */
data class OtpCheckRequest(
    @SerializedName("phone")
    val phone: String,
    @SerializedName("code")
    val code: String
)
