package com.shourov.apps.pacedream.model.response.otp

/**
 * Sealed class for OTP-related errors.
 * Extends Exception so it can be used with Result.failure() and exception handling.
 */
sealed class OtpError(message: String) : Exception(message) {
    data class NetworkError(override val message: String) : OtpError(message)
    data class RateLimited(val retryAfter: String?) : OtpError("Rate limited${retryAfter?.let { " (retry after $it)" } ?: ""}")
    data class ServiceUnavailable(override val message: String) : OtpError(message)
    data class InvalidPhone(override val message: String) : OtpError(message)
    data class VerificationFailed(override val message: String) : OtpError(message)
    data class MaxAttemptsReached(override val message: String) : OtpError(message)
    data class UnknownError(override val message: String) : OtpError(message)
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
