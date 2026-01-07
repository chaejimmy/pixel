package com.shourov.apps.pacedream.core.network.repository

import com.shourov.apps.pacedream.model.request.OtpCheckRequest
import com.shourov.apps.pacedream.model.request.OtpLoginRequest
import com.shourov.apps.pacedream.model.request.OtpSendRequest
import com.shourov.apps.pacedream.model.response.otp.OtpCheckResponse
import com.shourov.apps.pacedream.model.response.otp.OtpError
import com.shourov.apps.pacedream.model.response.otp.OtpLoginResponse
import com.shourov.apps.pacedream.model.response.otp.OtpSendResponse
import com.shourov.apps.pacedream.model.response.otp.getUserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for OTP-related API calls
 * Handles error parsing and rate limiting
 */
@Singleton
class OtpRepository @Inject constructor(
    private val otpService: com.shourov.apps.pacedream.core.network.services.OtpService
) {
    
    /**
     * Send OTP to phone number
     */
    suspend fun sendOTP(phone: String): Result<OtpSendResponse> = withContext(Dispatchers.IO) {
        try {
            val response = otpService.sendOTP(OtpSendRequest(phone))
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.ok) {
                    Result.success(body)
                } else {
                    Result.failure(parseError(body.error))
                }
            } else if (response.code() == 429) {
                // Rate limited
                val retryAfter = response.headers()["Retry-After"]
                Result.failure(OtpError.RateLimited(retryAfter))
            } else {
                Result.failure(OtpError.NetworkError("Failed to send OTP"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error sending OTP")
            Result.failure(OtpError.NetworkError(e.message ?: "Network error"))
        }
    }
    
    /**
     * Verify OTP code
     */
    suspend fun verifyOTP(phone: String, code: String): Result<OtpCheckResponse> = withContext(Dispatchers.IO) {
        try {
            val response = otpService.verifyOTP(OtpCheckRequest(phone, code))
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.ok) {
                    Result.success(body)
                } else {
                    Result.failure(parseError(body.error))
                }
            } else if (response.code() == 429) {
                val retryAfter = response.headers()["Retry-After"]
                Result.failure(OtpError.RateLimited(retryAfter))
            } else {
                Result.failure(OtpError.NetworkError("Failed to verify OTP"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error verifying OTP")
            Result.failure(OtpError.NetworkError(e.message ?: "Network error"))
        }
    }
    
    /**
     * Login with verified phone number
     */
    suspend fun login(phone: String): Result<OtpLoginResponse> = withContext(Dispatchers.IO) {
        try {
            val response = otpService.login(OtpLoginRequest(phone))
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.ok && body.success == true) {
                    Result.success(body)
                } else {
                    Result.failure(parseError(body.error))
                }
            } else {
                Result.failure(OtpError.NetworkError("Failed to login"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error logging in")
            Result.failure(OtpError.NetworkError(e.message ?: "Network error"))
        }
    }
    
    /**
     * Parse error response into OtpError
     */
    private fun parseError(error: com.shourov.apps.pacedream.model.response.otp.OtpErrorResponse?): OtpError {
        return when (error?.code) {
            "SERVICE_UNAVAILABLE" -> OtpError.ServiceUnavailable(error.message)
            "INVALID_PHONE" -> OtpError.InvalidPhone(error.message)
            "VERIFICATION_FAILED" -> OtpError.VerificationFailed(error.message)
            "60203" -> OtpError.MaxAttemptsReached(error.message)
            else -> OtpError.UnknownError(error?.message ?: "Unknown error")
        }
    }
}
