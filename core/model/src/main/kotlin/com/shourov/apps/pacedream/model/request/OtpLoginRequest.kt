package com.shourov.apps.pacedream.model.request

import com.google.gson.annotations.SerializedName

/**
 * Request model for OTP login
 * POST /auth/otp/login
 */
data class OtpLoginRequest(
    @SerializedName("phone")
    val phone: String
)
