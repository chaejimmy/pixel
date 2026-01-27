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
 * Matches web API endpoints exactly
 */
interface OtpService {
    /**
     * Send OTP to phone number
     * POST /v1/auth/otp/send
     */
    @POST("auth/otp/send")
    suspend fun sendOTP(@Body request: OtpSendRequest): Response<OtpSendResponse>
    
    /**
     * Verify OTP code
     * POST /v1/auth/otp/check
     */
    @POST("auth/otp/check")
    suspend fun verifyOTP(@Body request: OtpCheckRequest): Response<OtpCheckResponse>
    
    /**
     * Login with verified phone number
     * POST /v1/auth/otp/login
     */
    @POST("auth/otp/login")
    suspend fun login(@Body request: OtpLoginRequest): Response<OtpLoginResponse>
}
