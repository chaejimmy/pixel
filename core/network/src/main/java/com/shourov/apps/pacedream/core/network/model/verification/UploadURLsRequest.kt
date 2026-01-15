package com.shourov.apps.pacedream.core.network.model.verification

import com.google.gson.annotations.SerializedName

data class UploadURLsRequest(
    @SerializedName("files")
    val files: List<UploadFileRequest>
)

data class UploadFileRequest(
    @SerializedName("side")
    val side: String,
    @SerializedName("contentType")
    val contentType: String
)
