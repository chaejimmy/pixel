package com.shourov.apps.pacedream.model.response.otp

import com.google.gson.annotations.SerializedName

/**
 * Response model for verifying OTP
 * POST /auth/otp/check
 */
data class OtpCheckResponse(
    @SerializedName("ok")
    val ok: Boolean,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("error")
    val error: OtpErrorResponse? = null
)
