package com.shourov.apps.pacedream.core.network.api

import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ApiClient with iOS-parity networking behaviors:
 * - Default timeouts: request 30s, resource/read 60s
 * - Retry rules: Only GET requests, only on timeouts/transient errors, 2 retries with backoff
 * - HTML hardening: Detect and handle HTML responses gracefully
 * - Status mapping: Proper error handling for all HTTP status codes
 * - In-flight GET de-dup: Share results for identical in-flight GET requests
 */
@Singleton
class ApiClient @Inject constructor(
    private val appConfig: AppConfig,
    private val json: Json,
    private val tokenProvider: TokenProvider
) {
    
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(AppConfig.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.RESOURCE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.RESOURCE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
    
    // In-flight GET request deduplication
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<ApiResult<String>>>()
    private val mutex = Mutex()
    
    /**
     * Perform a GET request with retry logic and deduplication
     */
    suspend fun get(
        url: HttpUrl,
        includeAuth: Boolean = true,
        additionalHeaders: Map<String, String> = emptyMap()
    ): ApiResult<String> = coroutineScope {
        val cacheKey = buildCacheKey(url, includeAuth, additionalHeaders)
        
        // Check for in-flight request
        inFlightRequests[cacheKey]?.let { existing ->
            Timber.d("Reusing in-flight GET request for: ${redactUrl(url)}")
            return@coroutineScope existing.await()
        }
        
        // Create new deferred for this request
        val deferred = CompletableDeferred<ApiResult<String>>()
        
        mutex.withLock {
            // Double-check pattern
            inFlightRequests[cacheKey]?.let { existing ->
                return@coroutineScope existing.await()
            }
            inFlightRequests[cacheKey] = deferred
        }
        
        try {
            val result = executeWithRetry("GET", url, includeAuth, additionalHeaders)
            deferred.complete(result)
            result
        } catch (e: Exception) {
            val error = mapException(e)
            deferred.complete(ApiResult.Failure(error))
            ApiResult.Failure(error)
        } finally {
            inFlightRequests.remove(cacheKey)
        }
    }
    
    /**
     * Perform a POST request (no retry)
     */
    suspend fun post(
        url: HttpUrl,
        body: String,
        includeAuth: Boolean = true,
        additionalHeaders: Map<String, String> = emptyMap()
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        executeRequest("POST", url, body, includeAuth, additionalHeaders)
    }
    
    /**
     * Perform a PUT request (no retry)
     */
    suspend fun put(
        url: HttpUrl,
        body: String,
        includeAuth: Boolean = true,
        additionalHeaders: Map<String, String> = emptyMap()
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        executeRequest("PUT", url, body, includeAuth, additionalHeaders)
    }
    
    /**
     * Perform a DELETE request (no retry)
     */
    suspend fun delete(
        url: HttpUrl,
        body: String? = null,
        includeAuth: Boolean = true,
        additionalHeaders: Map<String, String> = emptyMap()
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        executeRequest("DELETE", url, body, includeAuth, additionalHeaders)
    }
    
    /**
     * Execute GET request with retry logic
     */
    private suspend fun executeWithRetry(
        method: String,
        url: HttpUrl,
        includeAuth: Boolean,
        additionalHeaders: Map<String, String>
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        var lastError: ApiError? = null
        
        repeat(AppConfig.MAX_RETRY_ATTEMPTS + 1) { attempt ->
            if (attempt > 0) {
                // Apply backoff delay
                val delayMs = AppConfig.RETRY_BACKOFF_DELAYS.getOrElse(attempt - 1) { 800L }
                Timber.d("Retry attempt $attempt after ${delayMs}ms for: ${redactUrl(url)}")
                delay(delayMs)
            }
            
            val result = executeRequest(method, url, null, includeAuth, additionalHeaders)
            
            when {
                result is ApiResult.Success -> return@withContext result
                result is ApiResult.Failure && shouldRetry(result.error) -> {
                    lastError = result.error
                    Timber.w("Request failed, will retry: ${result.error.message}")
                }
                else -> return@withContext result
            }
        }
        
        ApiResult.Failure(lastError ?: ApiError.Unknown("Request failed after retries"))
    }
    
    /**
     * Determine if an error should trigger a retry
     */
    private fun shouldRetry(error: ApiError): Boolean {
        return when (error) {
            is ApiError.Timeout -> true
            is ApiError.NetworkError -> true
            is ApiError.ServiceUnavailable -> false // Don't retry 502/503/504
            else -> false
        }
    }
    
    /**
     * Execute a single HTTP request
     */
    private fun executeRequest(
        method: String,
        url: HttpUrl,
        body: String?,
        includeAuth: Boolean,
        additionalHeaders: Map<String, String>
    ): ApiResult<String> {
        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
            
            // Add Content-Type for non-GET requests
            if (method != "GET" && body != null) {
                requestBuilder.header("Content-Type", "application/json")
            }
            
            // Add Authorization header if needed
            if (includeAuth) {
                tokenProvider.getAccessToken()?.let { token ->
                    requestBuilder.header("Authorization", "Bearer $token")
                }
            }
            
            // Add additional headers
            additionalHeaders.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            
            // Set method and body
            when (method) {
                "GET" -> requestBuilder.get()
                "POST" -> requestBuilder.post(
                    (body ?: "").toRequestBody("application/json".toMediaType())
                )
                "PUT" -> requestBuilder.put(
                    (body ?: "").toRequestBody("application/json".toMediaType())
                )
                "DELETE" -> {
                    if (body != null) {
                        requestBuilder.delete(body.toRequestBody("application/json".toMediaType()))
                    } else {
                        requestBuilder.delete()
                    }
                }
            }
            
            val response = httpClient.newCall(requestBuilder.build()).execute()
            return processResponse(response)
            
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "Request timeout for: ${redactUrl(url)}")
            return ApiResult.Failure(ApiError.Timeout())
        } catch (e: IOException) {
            Timber.e(e, "Network error for: ${redactUrl(url)}")
            return ApiResult.Failure(ApiError.NetworkError())
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error for: ${redactUrl(url)}")
            return ApiResult.Failure(mapException(e))
        }
    }
    
    /**
     * Process HTTP response with HTML hardening and status mapping
     */
    private fun processResponse(response: Response): ApiResult<String> {
        val statusCode = response.code
        val contentType = response.header("Content-Type") ?: ""
        val bodyString = response.body?.string() ?: ""
        
        // HTML hardening: Check for HTML responses
        if (isHtmlResponse(contentType, bodyString)) {
            Timber.w("Received HTML response instead of JSON for status: $statusCode")
            return when (statusCode) {
                502, 503, 504 -> ApiResult.Failure(ApiError.ServiceUnavailable())
                else -> ApiResult.Failure(ApiError.HtmlResponse())
            }
        }
        
        // Status code mapping
        return when (statusCode) {
            in 200..299 -> ApiResult.Success(bodyString)
            
            401 -> ApiResult.Failure(ApiError.Unauthorized())
            
            403 -> ApiResult.Failure(ApiError.Forbidden())
            
            404 -> ApiResult.Failure(ApiError.NotFound())
            
            429 -> {
                val retryAfter = response.header("Retry-After")?.toIntOrNull()
                ApiResult.Failure(ApiError.RateLimited(retryAfterSeconds = retryAfter))
            }
            
            502, 503, 504 -> ApiResult.Failure(ApiError.ServiceUnavailable())
            
            else -> {
                // Try to extract error message from JSON response
                val errorMessage = extractErrorMessage(bodyString)
                ApiResult.Failure(
                    ApiError.ServerError(errorMessage ?: "Something went wrong. Please try again.")
                )
            }
        }
    }
    
    /**
     * Check if response is HTML instead of JSON
     */
    private fun isHtmlResponse(contentType: String, body: String): Boolean {
        // Check Content-Type header
        if (contentType.contains("text/html", ignoreCase = true)) {
            return true
        }
        
        // Check body starts with HTML markers
        val trimmedBody = body.trimStart().lowercase()
        return trimmedBody.startsWith("<!doctype html") ||
               trimmedBody.startsWith("<html")
    }
    
    /**
     * Extract error message from JSON response body
     */
    private fun extractErrorMessage(body: String): String? {
        return try {
            val errorResponse = json.decodeFromString<ErrorResponse>(body)
            errorResponse.extractMessage()
        } catch (e: Exception) {
            // Try to extract message directly from common patterns
            try {
                val jsonObject = json.parseToJsonElement(body)
                if (jsonObject is kotlinx.serialization.json.JsonObject) {
                    jsonObject["message"]?.toString()?.trim('"')
                        ?: jsonObject["error"]?.toString()?.trim('"')
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Map exceptions to ApiError
     */
    private fun mapException(e: Exception): ApiError {
        return when (e) {
            is SocketTimeoutException -> ApiError.Timeout()
            is IOException -> ApiError.NetworkError()
            else -> ApiError.Unknown(e.message ?: "Unknown error", underlyingCause = e)
        }
    }
    
    /**
     * Build cache key for request deduplication
     */
    private fun buildCacheKey(
        url: HttpUrl,
        includeAuth: Boolean,
        headers: Map<String, String>
    ): String {
        return buildString {
            append(url.toString())
            append("|auth=$includeAuth")
            headers.forEach { (key, value) ->
                append("|$key=$value")
            }
        }
    }
    
    /**
     * Redact sensitive information from URL for logging
     */
    private fun redactUrl(url: HttpUrl): String {
        return url.newBuilder()
            .apply {
                url.queryParameterNames.forEach { name ->
                    if (name.contains("token", ignoreCase = true) ||
                        name.contains("key", ignoreCase = true) ||
                        name.contains("secret", ignoreCase = true)) {
                        setQueryParameter(name, "[REDACTED]")
                    }
                }
            }
            .build()
            .toString()
    }
}

/**
 * Interface for providing authentication tokens
 */
interface TokenProvider {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
}


