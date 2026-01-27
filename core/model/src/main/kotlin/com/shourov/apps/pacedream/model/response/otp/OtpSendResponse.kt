package com.shourov.apps.pacedream.model.response.otp

import com.google.gson.annotations.SerializedName

/**
 * Response model for sending OTP
 * POST /auth/otp/send
 */
data class OtpSendResponse(
    @SerializedName("ok")
    val ok: Boolean,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("sid")
    val sid: String? = null,
    @SerializedName("error")
    val error: OtpErrorResponse? = null
)
