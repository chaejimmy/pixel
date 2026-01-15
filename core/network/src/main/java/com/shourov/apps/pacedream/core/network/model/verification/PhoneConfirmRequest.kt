package com.shourov.apps.pacedream.core.network.model.verification

import com.google.gson.annotations.SerializedName

data class PhoneConfirmRequest(
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    @SerializedName("code")
    val code: String
)
