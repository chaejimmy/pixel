package com.shourov.apps.pacedream.core.network.model.verification

import com.google.gson.annotations.SerializedName

data class PhoneConfirmResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: PhoneConfirmData?,
    @SerializedName("message")
    val message: String?
) {
    data class PhoneConfirmData(
        @SerializedName("phoneVerified")
        val phoneVerified: Boolean,
        @SerializedName("phone_verified_at")
        val phoneVerifiedAt: String?,
        @SerializedName("phone_e164")
        val phoneE164: String?
    )
}
