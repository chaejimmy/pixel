package com.shourov.apps.pacedream.core.network.model.verification

import com.google.gson.annotations.SerializedName

data class PhoneVerificationResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: PhoneVerificationData?,
    @SerializedName("message")
    val message: String?
) {
    data class PhoneVerificationData(
        @SerializedName("status")
        val status: String,
        @SerializedName("sid")
        val sid: String?
    )
}
