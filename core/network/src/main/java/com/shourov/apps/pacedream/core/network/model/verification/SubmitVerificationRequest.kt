package com.shourov.apps.pacedream.core.network.model.verification

import com.google.gson.annotations.SerializedName

data class SubmitVerificationRequest(
    @SerializedName("idType")
    val idType: String,
    @SerializedName("frontStorageKey")
    val frontStorageKey: String,
    @SerializedName("backStorageKey")
    val backStorageKey: String
)
