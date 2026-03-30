package com.shourov.apps.pacedream.model.request

import com.google.gson.annotations.SerializedName

/**
 * Request model for OTP login
 * Website parity: POST /auth/login/mobile with { mobile, otp }
 */
data class OtpLoginRequest(
    @SerializedName("mobile")
    val phone: String
)
