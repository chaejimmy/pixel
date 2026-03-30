package com.shourov.apps.pacedream.core.network.services

import com.shourov.apps.pacedream.model.request.OtpCheckRequest
import com.shourov.apps.pacedream.model.request.OtpLoginRequest
import com.shourov.apps.pacedream.model.request.OtpSendRequest
import com.shourov.apps.pacedream.model.response.otp.OtpCheckResponse
import com.shourov.apps.pacedream.model.response.otp.OtpLoginResponse
import com.shourov.apps.pacedream.model.response.otp.OtpSendResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * OTP API service interface
 *
 * Website parity: uses /auth/phone/send-otp and /auth/phone/verify-otp
 * with field names { mobile, otp } matching the website's login page.
 *
 * Legacy endpoints (/auth/otp/send, /auth/otp/check, /auth/otp/login) are
 * kept as fallbacks in OtpRepository if the primary endpoints fail.
 */
interface OtpService {
    /**
     * Send OTP to phone number (website parity)
     * POST /v1/auth/phone/send-otp  { mobile }
     */
    @POST("auth/phone/send-otp")
    suspend fun sendOTP(@Body request: OtpSendRequest): Response<OtpSendResponse>

    /**
     * Verify OTP code (website parity)
     * POST /v1/auth/phone/verify-otp  { mobile, otp }
     */
    @POST("auth/phone/verify-otp")
    suspend fun verifyOTP(@Body request: OtpCheckRequest): Response<OtpCheckResponse>

    /**
     * Login with verified phone number (website parity)
     * POST /v1/auth/login/mobile  { mobile, otp }
     */
    @POST("auth/login/mobile")
    suspend fun login(@Body request: OtpLoginRequest): Response<OtpLoginResponse>

    // ── Legacy fallback endpoints ────────────────────────────

    @POST("auth/otp/send")
    suspend fun sendOTPLegacy(@Body request: OtpSendRequest): Response<OtpSendResponse>

    @POST("auth/otp/check")
    suspend fun verifyOTPLegacy(@Body request: OtpCheckRequest): Response<OtpCheckResponse>

    @POST("auth/otp/login")
    suspend fun loginLegacy(@Body request: OtpLoginRequest): Response<OtpLoginResponse>
}
