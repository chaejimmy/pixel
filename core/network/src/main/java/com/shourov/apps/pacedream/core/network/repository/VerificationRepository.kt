package com.shourov.apps.pacedream.core.network.repository

import com.shourov.apps.pacedream.core.network.auth.TokenStorage
import com.shourov.apps.pacedream.core.network.model.verification.*
import com.shourov.apps.pacedream.core.network.services.VerificationApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for ID verification operations
 * Handles phone verification, ID upload, and status checking
 */
@Singleton
class VerificationRepository @Inject constructor(
    private val api: VerificationApi,
    private val tokenStorage: TokenStorage
) {
    
    /**
     * Send phone verification code
     */
    suspend fun sendPhoneVerificationCode(phoneNumber: String): Result<PhoneVerificationResponse> {
        return try {
            val token = tokenStorage.accessToken
                ?: return Result.failure(Exception("No auth token"))
            
            val request = PhoneVerificationRequest(phoneNumber)
            val response = api.sendPhoneVerificationCode("Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("Failed to send phone verification code: ${response.code()} - $errorBody")
                Result.failure(Exception("Failed to send verification code: ${response.message()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error sending phone verification code")
            Result.failure(e)
        }
    }
    
    /**
     * Confirm phone verification with OTP code
     */
    suspend fun confirmPhoneVerification(phoneNumber: String, code: String): Result<PhoneConfirmResponse> {
        return try {
            val token = tokenStorage.accessToken
                ?: return Result.failure(Exception("No auth token"))
            
            val request = PhoneConfirmRequest(phoneNumber, code)
            val response = api.confirmPhoneVerification("Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("Failed to confirm phone verification: ${response.code()} - $errorBody")
                Result.failure(Exception("Invalid verification code: ${response.message()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error confirming phone verification")
            Result.failure(e)
        }
    }
    
    /**
     * Get verification status
     */
    suspend fun getVerificationStatus(): Result<VerificationStatusResponse> {
        return try {
            val token = tokenStorage.accessToken
                ?: return Result.failure(Exception("No auth token"))
            
            val response = api.getVerificationStatus("Bearer $token")
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("Failed to get verification status: ${response.code()} - $errorBody")
                Result.failure(Exception("Failed to get verification status: ${response.message()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting verification status")
            Result.failure(e)
        }
    }
    
    /**
     * Get pre-signed S3 upload URLs
     */
    suspend fun getUploadURLs(files: List<UploadFileRequest>): Result<UploadURLsResponse> {
        return try {
            val token = tokenStorage.accessToken
                ?: return Result.failure(Exception("No auth token"))
            
            val request = UploadURLsRequest(files)
            val response = api.getUploadURLs("Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("Failed to get upload URLs: ${response.code()} - $errorBody")
                Result.failure(Exception("Failed to get upload URLs: ${response.message()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting upload URLs")
            Result.failure(e)
        }
    }
    
    /**
     * Upload image directly to S3 using pre-signed URL
     */
    suspend fun uploadToS3(uploadUrl: String, imageData: ByteArray, contentType: String): Result<Unit> {
        return try {
            val mediaType = contentType.toMediaType()
            val requestBody = imageData.toRequestBody(mediaType)
            val response = api.uploadToS3(uploadUrl, requestBody)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Timber.e("S3 upload failed: ${response.code()} - ${response.message()}")
                Result.failure(Exception("S3 upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error uploading to S3")
            Result.failure(e)
        }
    }
    
    /**
     * Submit ID verification
     */
    suspend fun submitVerification(
        idType: String,
        frontStorageKey: String,
        backStorageKey: String
    ): Result<SubmitVerificationResponse> {
        return try {
            val token = tokenStorage.accessToken
                ?: return Result.failure(Exception("No auth token"))
            
            val request = SubmitVerificationRequest(idType, frontStorageKey, backStorageKey)
            val response = api.submitVerification("Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("Failed to submit verification: ${response.code()} - $errorBody")
                Result.failure(Exception("Failed to submit verification: ${response.message()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error submitting verification")
            Result.failure(e)
        }
    }
}
