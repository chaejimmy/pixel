package com.shourov.apps.pacedream.core.network.services

import com.shourov.apps.pacedream.core.network.model.verification.*
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Verification API service for ID verification
 * Matches web API endpoints at /api/proxy/v1/users/verification/*
 */
interface VerificationApi {
    
    /**
     * Send phone verification code
     * POST /v1/users/verification/phone/send-code
     * Full URL: {baseUrl}/v1/users/verification/phone/send-code
     * Where baseUrl should be configured as: https://www.pacedream.com/api/proxy
     */
    @POST("/v1/users/verification/phone/send-code")
    suspend fun sendPhoneVerificationCode(
        @Header("Authorization") token: String,
        @Body request: PhoneVerificationRequest
    ): Response<PhoneVerificationResponse>
    
    /**
     * Confirm phone verification with OTP code
     * POST /v1/users/verification/phone/confirm
     */
    @POST("/v1/users/verification/phone/confirm")
    suspend fun confirmPhoneVerification(
        @Header("Authorization") token: String,
        @Body request: PhoneConfirmRequest
    ): Response<PhoneConfirmResponse>
    
    /**
     * Get verification status
     * GET /v1/users/verification
     */
    @GET("/v1/users/verification")
    suspend fun getVerificationStatus(
        @Header("Authorization") token: String
    ): Response<VerificationStatusResponse>
    
    /**
     * Get pre-signed S3 upload URLs
     * POST /v1/users/verification/upload-urls
     */
    @POST("/v1/users/verification/upload-urls")
    suspend fun getUploadURLs(
        @Header("Authorization") token: String,
        @Body request: UploadURLsRequest
    ): Response<UploadURLsResponse>
    
    /**
     * Submit ID verification
     * POST /v1/users/verification/submit
     */
    @POST("/v1/users/verification/submit")
    suspend fun submitVerification(
        @Header("Authorization") token: String,
        @Body request: SubmitVerificationRequest
    ): Response<SubmitVerificationResponse>
    
    /**
     * Direct S3 upload (PUT request to pre-signed URL)
     */
    @PUT
    suspend fun uploadToS3(
        @Url url: String,
        @Body imageData: RequestBody
    ): Response<Unit>
}
