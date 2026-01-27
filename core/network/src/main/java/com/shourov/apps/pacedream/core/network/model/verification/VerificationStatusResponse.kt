package com.shourov.apps.pacedream.core.network.model.verification

import com.google.gson.annotations.SerializedName

data class VerificationStatusResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: VerificationStatusData?
) {
    data class VerificationStatusData(
        @SerializedName("phone")
        val phone: PhoneStatus?,
        @SerializedName("id")
        val id: IDStatus?,
        @SerializedName("level")
        val level: Int
    ) {
        data class PhoneStatus(
            @SerializedName("verified")
            val verified: Boolean,
            @SerializedName("phone_e164")
            val phoneE164: String?,
            @SerializedName("phone_verified_at")
            val phoneVerifiedAt: String?
        )
        
        data class IDStatus(
            @SerializedName("status")
            val status: String, // "not_started" | "submitted" | "approved" | "rejected"
            @SerializedName("idType")
            val idType: String?,
            @SerializedName("frontImage")
            val frontImage: ImageInfo?,
            @SerializedName("backImage")
            val backImage: ImageInfo?,
            @SerializedName("submittedAt")
            val submittedAt: String?,
            @SerializedName("rejectionReason")
            val rejectionReason: String?
        ) {
            data class ImageInfo(
                @SerializedName("storageKey")
                val storageKey: String,
                @SerializedName("url")
                val url: String
            )
        }
    }
}
