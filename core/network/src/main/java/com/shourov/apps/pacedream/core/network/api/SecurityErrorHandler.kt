package com.shourov.apps.pacedream.core.network.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import timber.log.Timber

/**
 * Shared handler for security-related backend responses.
 * Parses error bodies and maps them to typed ApiError subclasses
 * so all features handle restrictions, fraud blocks, and rate limits consistently.
 */
object SecurityErrorHandler {

    /**
     * Known error codes from backend security responses.
     */
    private val ACCOUNT_RESTRICTED_CODES = setOf(
        "ACCOUNT_RESTRICTED", "ACCOUNT_SUSPENDED", "ACCOUNT_BLOCKED",
        "TEMPORARILY_BLOCKED", "ACCOUNT_DISABLED"
    )
    private val FRAUD_BLOCKED_CODES = setOf(
        "FRAUD_DETECTED", "FRAUD_BLOCKED", "PAYMENT_BLOCKED",
        "BOOKING_BLOCKED", "ACTION_BLOCKED", "SPAM_DETECTED"
    )
    private val UNDER_REVIEW_CODES = setOf(
        "UNDER_REVIEW", "PENDING_REVIEW", "BOOKING_PAUSED",
        "ACTION_PAUSED", "VERIFICATION_REQUIRED"
    )

    /**
     * Attempt to parse a security-related error from the response body.
     * Returns a typed [ApiError] if the response matches a known security pattern,
     * or null if it is not a security error.
     */
    fun parseSecurityError(statusCode: Int, body: String, json: Json): ApiError? {
        if (body.isBlank()) return null

        return try {
            val root = json.parseToJsonElement(body)
            if (root !is JsonObject) return null

            val code = root["code"]?.jsonPrimitive?.content
                ?: root["error_code"]?.jsonPrimitive?.content
                ?: root["errorCode"]?.jsonPrimitive?.content
            val message = root["message"]?.jsonPrimitive?.content
                ?: root["error"]?.jsonPrimitive?.content
            val reason = root["reason"]?.jsonPrimitive?.content

            // Check for account restriction
            if (code != null && code.uppercase() in ACCOUNT_RESTRICTED_CODES) {
                val requiresLogout = root["requiresLogout"]?.jsonPrimitive?.booleanOrNull ?: false
                val requiresVerification = root["requiresVerification"]?.jsonPrimitive?.booleanOrNull
                    ?: (code.uppercase() == "VERIFICATION_REQUIRED")
                return ApiError.AccountRestricted(
                    message = message ?: "Your account has been temporarily restricted.",
                    reason = reason,
                    requiresLogout = requiresLogout,
                    requiresVerification = requiresVerification
                )
            }

            // Check for fraud block
            if (code != null && code.uppercase() in FRAUD_BLOCKED_CODES) {
                return ApiError.FraudBlocked(
                    message = message ?: "This action has been blocked for security reasons.",
                    reason = reason
                )
            }

            // Check for under review
            if (code != null && code.uppercase() in UNDER_REVIEW_CODES) {
                return ApiError.UnderReview(
                    message = message ?: "This action is paused while under review. Please try again later.",
                    reason = reason
                )
            }

            // Status-code-based fallback for 403 with security keywords
            if (statusCode == 403 && message != null) {
                val lowerMessage = message.lowercase()
                if (lowerMessage.contains("restrict") || lowerMessage.contains("suspend") || lowerMessage.contains("blocked")) {
                    return ApiError.AccountRestricted(message = message, reason = reason)
                }
                if (lowerMessage.contains("fraud") || lowerMessage.contains("spam")) {
                    return ApiError.FraudBlocked(message = message, reason = reason)
                }
                if (lowerMessage.contains("review") || lowerMessage.contains("paused")) {
                    return ApiError.UnderReview(message = message, reason = reason)
                }
            }

            null
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse security error from response body")
            null
        }
    }

    /**
     * Returns a user-friendly message for any [ApiError], with enhanced
     * messages for security-related errors.
     */
    fun getUserMessage(error: ApiError): String {
        return when (error) {
            is ApiError.AccountRestricted -> {
                if (error.requiresVerification) {
                    "Your account requires verification before you can continue."
                } else {
                    error.message
                }
            }
            is ApiError.FraudBlocked -> error.message
            is ApiError.UnderReview -> error.message
            is ApiError.RateLimited -> error.friendlyMessage()
            is ApiError.Unauthorized -> "Your session has expired. Please sign in again."
            is ApiError.Forbidden -> "You don't have permission to perform this action."
            is ApiError.NetworkError -> "Network connection error. Please check your connection."
            is ApiError.Timeout -> "Request timed out. Please try again."
            is ApiError.ServiceUnavailable -> "Service is temporarily unavailable. Please try again shortly."
            else -> error.message ?: "An unexpected error occurred."
        }
    }

    /**
     * Whether this error means the user should be logged out.
     */
    fun requiresLogout(error: ApiError): Boolean {
        return when (error) {
            is ApiError.AccountRestricted -> error.requiresLogout
            is ApiError.Unauthorized -> true
            else -> false
        }
    }

    /**
     * Whether this error means the user should not retry the action.
     */
    fun isNonRetryable(error: ApiError): Boolean {
        return when (error) {
            is ApiError.AccountRestricted -> true
            is ApiError.FraudBlocked -> true
            is ApiError.Forbidden -> true
            else -> false
        }
    }
}
