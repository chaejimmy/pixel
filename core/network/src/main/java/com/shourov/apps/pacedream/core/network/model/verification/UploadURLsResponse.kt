package com.shourov.apps.pacedream.core.network.model.verification

import com.google.gson.annotations.SerializedName

data class UploadURLsResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: UploadURLsData?
) {
    data class UploadURLsData(
        @SerializedName("uploads")
        val uploads: List<UploadInfo>
    ) {
        data class UploadInfo(
            @SerializedName("side")
            val side: String,
            @SerializedName("uploadUrl")
            val uploadUrl: String,
            @SerializedName("storageKey")
            val storageKey: String
        )
    }
}
