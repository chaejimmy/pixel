package com.shourov.apps.pacedream.core.network.model.verification

import com.google.gson.annotations.SerializedName

data class PhoneVerificationRequest(
    @SerializedName("phoneNumber")
    val phoneNumber: String
)
