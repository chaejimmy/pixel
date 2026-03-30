package com.shourov.apps.pacedream.model.request

import com.google.gson.annotations.SerializedName

/**
 * Request model for sending OTP
 * Website parity: POST /auth/phone/send-otp with { mobile }
 */
data class OtpSendRequest(
    @SerializedName("mobile")
    val phone: String
)
