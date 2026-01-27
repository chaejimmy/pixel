package com.shourov.apps.pacedream.model.response.otp

/**
 * Sealed class for OTP-related errors
 */
sealed class OtpError {
    data class NetworkError(val message: String) : OtpError()
    data class RateLimited(val retryAfter: String?) : OtpError()
    data class ServiceUnavailable(val message: String) : OtpError()
    data class InvalidPhone(val message: String) : OtpError()
    data class VerificationFailed(val message: String) : OtpError()
    data class MaxAttemptsReached(val message: String) : OtpError()
    data class UnknownError(val message: String) : OtpError()
}

/**
 * Helper function to get user-friendly error message
 */
fun OtpError.getUserMessage(): String {
    return when (this) {
        is OtpError.ServiceUnavailable -> "OTP service is not configured"
        is OtpError.InvalidPhone -> "Invalid phone number format. Use international format: +1234567890"
        is OtpError.VerificationFailed -> "Invalid verification code. Please try again."
        is OtpError.MaxAttemptsReached -> "Maximum number of attempts reached. Please request a new code."
        is OtpError.RateLimited -> {
            val retryAfter = retryAfter?.toIntOrNull()
            if (retryAfter != null) {
                "Rate limit exceeded. Please try again in ${retryAfter / 60} minutes."
            } else {
                "Rate limit exceeded. Please try again later."
            }
        }
        is OtpError.NetworkError -> "Network error. Please check your connection."
        is OtpError.UnknownError -> "An unexpected error occurred"
    }
}
