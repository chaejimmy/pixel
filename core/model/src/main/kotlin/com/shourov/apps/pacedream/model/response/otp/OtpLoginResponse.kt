package com.shourov.apps.pacedream.model.response.otp

import com.google.gson.annotations.SerializedName

/**
 * Response model for OTP login
 * POST /auth/otp/login
 */
data class OtpLoginResponse(
    @SerializedName("ok")
    val ok: Boolean,
    @SerializedName("success")
    val success: Boolean? = null,
    @SerializedName("data")
    val data: OtpLoginData? = null,
    @SerializedName("error")
    val error: OtpErrorResponse? = null
)

data class OtpLoginData(
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String,
    @SerializedName("user")
    val user: OtpUserData
)

data class OtpUserData(
    @SerializedName("id")
    val id: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("firstName")
    val firstName: String? = null,
    @SerializedName("lastName")
    val lastName: String? = null,
    @SerializedName("profilePic")
    val profilePic: String? = null,
    @SerializedName("isNewUser")
    val isNewUser: Boolean? = null,
    @SerializedName("incomplete_profile")
    val incompleteProfile: Boolean? = null
)

data class OtpErrorResponse(
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String
)
