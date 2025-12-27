package com.shourov.apps.pacedream.core.network.api

import kotlinx.serialization.Serializable

/**
 * Represents API errors with iOS-parity error handling
 */
sealed class ApiError : Exception() {
    
    data class Unauthorized(override val message: String = "Unauthorized") : ApiError()
    
    data class Forbidden(override val message: String = "Access forbidden") : ApiError()
    
    data class NotFound(override val message: String = "Resource not found") : ApiError()
    
    data class RateLimited(
        override val message: String = "Too many requests. Please slow down.",
        val retryAfterSeconds: Int? = null
    ) : ApiError() {
        fun friendlyMessage(): String {
            return if (retryAfterSeconds != null) {
                val minutes = (retryAfterSeconds / 60).coerceAtLeast(1)
                "Too many requests. Please try again in $minutes minute${if (minutes > 1) "s" else ""}."
            } else {
                message
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
        override val message: String = "Received unexpected response. Please try again later."
    ) : ApiError()
    
    data class ServerError(
        override val message: String = "Something went wrong. Please try again."
    ) : ApiError()
    
    data class DecodingError(
        override val message: String = "Failed to process response.",
        val cause: Throwable? = null
    ) : ApiError()
    
    data class Unknown(
        override val message: String = "An unexpected error occurred.",
        val statusCode: Int? = null,
        val cause: Throwable? = null
    ) : ApiError()
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
    
    fun getOrDefault(default: T): T = when (this) {
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

