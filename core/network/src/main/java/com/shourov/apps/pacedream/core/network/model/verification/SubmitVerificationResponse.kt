package com.shourov.apps.pacedream.core.network.model.verification

import com.google.gson.annotations.SerializedName

data class SubmitVerificationResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: SubmitVerificationData?,
    @SerializedName("message")
    val message: String?
) {
    data class SubmitVerificationData(
        @SerializedName("verification")
        val verification: VerificationInfo
    ) {
        data class VerificationInfo(
            @SerializedName("status")
            val status: String,
            @SerializedName("submittedAt")
            val submittedAt: String?,
            @SerializedName("idType")
            val idType: String?
        )
    }
}
