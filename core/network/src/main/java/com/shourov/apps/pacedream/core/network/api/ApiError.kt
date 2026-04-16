package com.shourov.apps.pacedream.core.network.api

import kotlinx.serialization.Serializable

/**
 * Represents API errors with iOS-parity error handling
 */
sealed class ApiError : Exception() {
    
    data class Unauthorized(override val message: String = "Your session has expired. Please sign in again.") : ApiError()

    data class Forbidden(override val message: String = "You don't have permission to perform this action.") : ApiError()

    data class NotFound(override val message: String = "The item you're looking for could not be found.") : ApiError()

    data class AccountRestricted(
        override val message: String = "Your account has been temporarily restricted.",
        val reason: String? = null,
        val requiresLogout: Boolean = false,
        val requiresVerification: Boolean = false
    ) : ApiError()

    data class FraudBlocked(
        override val message: String = "This action has been blocked for security reasons.",
        val reason: String? = null
    ) : ApiError()

    data class UnderReview(
        override val message: String = "This action is paused while under review. Please try again later.",
        val reason: String? = null
    ) : ApiError()
    
    data class RateLimited(
        override val message: String = "Too many requests. Please slow down.",
        val retryAfterSeconds: Int? = null
    ) : ApiError() {
        fun friendlyMessage(): String {
            if (retryAfterSeconds == null) return message
            return if (retryAfterSeconds < 60) {
                "Too many requests. Please try again in $retryAfterSeconds second${if (retryAfterSeconds > 1) "s" else ""}."
            } else {
                val minutes = (retryAfterSeconds / 60).coerceAtLeast(1)
                "Too many requests. Please try again in $minutes minute${if (minutes > 1) "s" else ""}."
            }
        }
    }
    
    data class ServiceUnavailable(
        override val message: String = "Service is temporarily unavailable. Please try again in a minute."
    ) : ApiError()
    
    data class Timeout(
        override val message: String = "Network timeout. Please try again."
    ) : ApiError()
    
    data class NetworkError(
        override val message: String = "Network connection error. Please check your connection."
    ) : ApiError()
    
    data class HtmlResponse(
        override val message: String = "Our service is temporarily unavailable. Please try again in a moment."
    ) : ApiError()
    
    data class ServerError(
        override val message: String = "Something went wrong. Please try again."
    ) : ApiError()
    
    data class DecodingError(
        override val message: String = "Something went wrong. Please try again.",
        val underlyingCause: Throwable? = null
    ) : ApiError() {
        override val cause: Throwable? get() = underlyingCause
    }
    
    data class Unknown(
        override val message: String = "Something went wrong. Please try again.",
        val statusCode: Int? = null,
        val underlyingCause: Throwable? = null
    ) : ApiError() {
        override val cause: Throwable? get() = underlyingCause
    }
}

/**
 * Result wrapper for API responses
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val error: ApiError) : ApiResult<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    
    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): ApiError? = (this as? Failure)?.error
    
    inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }
    
    inline fun <R> flatMap(transform: (T) -> ApiResult<R>): ApiResult<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this
    }
    
    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onFailure(action: (ApiError) -> Unit): ApiResult<T> {
        if (this is Failure) action(error)
        return this
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw error
    }
    
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Failure -> default
    }
}

/**
 * Standard API response envelope matching iOS patterns
 */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean? = null,
    val status: Boolean? = null,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null,
    val errors: List<String>? = null
) {
    val isSuccessful: Boolean
        get() = success == true || status == true
}

/**
 * Error response structure for parsing server errors
 */
@Serializable
data class ErrorResponse(
    val message: String? = null,
    val error: String? = null,
    val errors: List<String>? = null,
    val data: ErrorData? = null
) {
    @Serializable
    data class ErrorData(
        val message: String? = null,
        val error: String? = null
    )
    
    fun extractMessage(): String? {
        return message 
            ?: error 
            ?: errors?.firstOrNull()
            ?: data?.message
            ?: data?.error
    }
}


