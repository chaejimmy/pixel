package com.shourov.apps.pacedream.model.request

import com.google.gson.annotations.SerializedName

/**
 * Request model for verifying OTP
 * Website parity: POST /auth/phone/verify-otp with { mobile, otp }
 */
data class OtpCheckRequest(
    @SerializedName("mobile")
    val phone: String,
    @SerializedName("otp")
    val code: String
)
