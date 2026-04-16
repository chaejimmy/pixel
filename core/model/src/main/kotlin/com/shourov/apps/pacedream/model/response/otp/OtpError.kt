package com.shourov.apps.pacedream.model.response.otp

/**
 * Sealed class for OTP-related errors.
 * Extends Exception so it can be used with Result.failure() and exception handling.
 */
sealed class OtpError(message: String) : Exception(message) {
    data class NetworkError(override val message: String) : OtpError(message)
    data class RateLimited(val retryAfter: String?) : OtpError("Rate limited${retryAfter?.let { " (retry after $it)" } ?: ""}") {
        /** Retry-After value parsed as seconds, or null. */
        val retryAfterSeconds: Int? get() = retryAfter?.toIntOrNull()
    }
    data class ServiceUnavailable(override val message: String) : OtpError(message)
    data class InvalidPhone(override val message: String) : OtpError(message)
    data class VerificationFailed(override val message: String) : OtpError(message)
    data class MaxAttemptsReached(override val message: String) : OtpError(message)
    data class AccountBlocked(override val message: String = "Your account has been temporarily blocked. Please contact support.") : OtpError(message)
    data class UnsupportedCountry(override val message: String = "Phone verification is currently available in the United States and Canada only.") : OtpError(message)
    data class TooManyAttempts(override val message: String = "Too many failed attempts. Please try again later.") : OtpError(message)
    data class UnknownError(override val message: String) : OtpError(message)
}

/**
 * Helper function to get user-friendly error message
 */
fun OtpError.getUserMessage(): String {
    return when (this) {
        is OtpError.ServiceUnavailable -> "Phone verification is temporarily unavailable. Please try again later."
        is OtpError.InvalidPhone -> "Please enter a valid phone number."
        is OtpError.VerificationFailed -> "Invalid verification code. Please try again."
        is OtpError.MaxAttemptsReached -> "Maximum number of attempts reached. Please request a new code."
        is OtpError.RateLimited -> {
            val seconds = retryAfterSeconds
            if (seconds != null && seconds > 0) {
                if (seconds < 60) {
                    "Too many requests. Please wait $seconds second${if (seconds > 1) "s" else ""} before trying again."
                } else {
                    val minutes = (seconds / 60).coerceAtLeast(1)
                    "Too many requests. Please try again in $minutes minute${if (minutes > 1) "s" else ""}."
                }
            } else {
                "Too many requests. Please try again later."
            }
        }
        is OtpError.AccountBlocked -> message
        is OtpError.UnsupportedCountry -> message
        is OtpError.TooManyAttempts -> message
        is OtpError.NetworkError -> "Please check your internet connection and try again."
        is OtpError.UnknownError -> "Something went wrong. Please try again."
    }
}
