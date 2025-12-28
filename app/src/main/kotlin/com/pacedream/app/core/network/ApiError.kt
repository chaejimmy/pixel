package com.pacedream.app.core.network

/**
 * ApiError - Sealed class matching iOS error types
 * 
 * Maps HTTP status codes and network conditions to typed errors
 * with user-friendly messages.
 */
sealed class ApiError(
    open val message: String,
    open val code: Int? = null
) : Exception(message) {
    
    /** 401 - Token invalid or expired */
    data object Unauthorized : ApiError("Unauthorized. Please sign in again.")
    
    /** 403 - Access denied */
    data object Forbidden : ApiError("Access denied.")
    
    /** 404 - Resource not found */
    data object NotFound : ApiError("Resource not found.")
    
    /** 429 - Rate limited */
    data class RateLimited(
        val retryAfterSeconds: Int? = null
    ) : ApiError(
        retryAfterSeconds?.let { "Too many requests. Please try again in $it seconds." }
            ?: "Too many requests. Please try again later."
    )
    
    /** 502/503/504 - Service unavailable (matching iOS message exactly) */
    data object ServiceUnavailable : ApiError(
        "Service is temporarily unavailable. Please try again in a minute."
    )
    
    /** Network timeout (matching iOS message exactly) */
    data object NetworkTimeout : ApiError("Network timeout. Please try again.")
    
    /** No network connection */
    data object NoConnection : ApiError("No internet connection. Please check your network.")
    
    /** Request was cancelled */
    data object Cancelled : ApiError("Request was cancelled.")
    
    /** HTML response received instead of JSON (service error) */
    data object HtmlResponse : ApiError(
        "Service is temporarily unavailable. Please try again in a minute."
    )
    
    /** JSON decoding failed */
    data class DecodingError(
        override val message: String = "Failed to parse server response."
    ) : ApiError(message)
    
    /** Server error with extracted message */
    data class ServerError(
        override val code: Int,
        override val message: String
    ) : ApiError(message, code)
    
    /** Generic/unknown error */
    data class Unknown(
        override val message: String = "An unexpected error occurred."
    ) : ApiError(message)
    
    companion object {
        /**
         * Map HTTP status code to ApiError
         * Matches iOS status mapping behavior
         */
        fun fromStatusCode(code: Int, serverMessage: String? = null): ApiError {
            return when (code) {
                401 -> Unauthorized
                403 -> Forbidden
                404 -> NotFound
                429 -> RateLimited()
                in 502..504 -> ServiceUnavailable
                in 500..599 -> ServerError(
                    code,
                    serverMessage ?: "Server error. Please try again."
                )
                else -> ServerError(
                    code,
                    serverMessage ?: "Request failed with status $code"
                )
            }
        }
        
        /**
         * Extract error message from JSON response body
         * Tries multiple common keys matching iOS behavior
         */
        fun extractServerMessage(jsonBody: String?): String? {
            if (jsonBody.isNullOrBlank()) return null
            
            return try {
                // Try to parse as JSON and extract message from common keys
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val element = json.parseToJsonElement(jsonBody)
                val obj = element as? kotlinx.serialization.json.JsonObject ?: return null
                
                // Priority order for message extraction (matching iOS)
                listOf(
                    obj["message"],
                    obj["error"],
                    obj["data"]?.let { (it as? kotlinx.serialization.json.JsonObject)?.get("message") },
                    obj["data"]?.let { (it as? kotlinx.serialization.json.JsonObject)?.get("error") },
                    obj["errors"]?.let { errors ->
                        when (errors) {
                            is kotlinx.serialization.json.JsonArray -> errors.firstOrNull()
                            is kotlinx.serialization.json.JsonPrimitive -> errors
                            else -> null
                        }
                    }
                ).firstNotNullOfOrNull { element ->
                    (element as? kotlinx.serialization.json.JsonPrimitive)?.content
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Result type for API calls
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val error: ApiError) : ApiResult<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    
    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): ApiError? = (this as? Failure)?.error
    
    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (ApiError) -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Failure -> onFailure(error)
    }
    
    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onFailure(action: (ApiError) -> Unit): ApiResult<T> {
        if (this is Failure) action(error)
        return this
    }
}


