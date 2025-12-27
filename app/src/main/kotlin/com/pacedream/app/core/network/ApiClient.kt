package com.pacedream.app.core.network

import com.pacedream.app.core.auth.TokenStorage
import com.pacedream.app.core.config.AppConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ApiClient - Network layer with iOS parity
 * 
 * Features:
 * - URL building via HttpUrl.Builder (NO string concatenation)
 * - Header management (Accept, Content-Type, Authorization)
 * - GET-only retry with exponential backoff (2 retries, 0.4s/0.8s)
 * - HTML response hardening (treat as service error)
 * - In-flight GET request deduplication
 * - Timeout configuration matching iOS
 * - Server message extraction from JSON
 */
@Singleton
class ApiClient @Inject constructor(
    private val appConfig: AppConfig,
    private val tokenStorage: TokenStorage,
    private val json: Json
) {
    
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
    // In-flight request deduplication for GET requests
    private val inFlightRequests = ConcurrentHashMap<String, CompletableDeferred<ApiResult<String>>>()
    private val mutex = Mutex()
    
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(createLoggingInterceptor())
            .addInterceptor(createHtmlHardeningInterceptor())
            .build()
    }
    
    /**
     * Create logging interceptor with token redaction
     */
    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            // Redact tokens in logs
            val redacted = message
                .replace(Regex("Bearer [A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+"), "Bearer [REDACTED]")
                .replace(Regex("\"(access_?[Tt]oken|refresh_?[Tt]oken|id_?[Tt]oken)\"\\s*:\\s*\"[^\"]+\""), "\"$1\": \"[REDACTED]\"")
            Timber.d(redacted)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    /**
     * Create interceptor that detects HTML responses and throws error
     * Never surface HTML to UI - show friendly service unavailable message
     */
    private fun createHtmlHardeningInterceptor(): okhttp3.Interceptor {
        return okhttp3.Interceptor { chain ->
            val response = chain.proceed(chain.request())
            
            val contentType = response.header("Content-Type") ?: ""
            if (contentType.contains("text/html", ignoreCase = true)) {
                response.close()
                throw HtmlResponseException()
            }
            
            response
        }
    }
    
    /**
     * GET request with retry and deduplication
     */
    suspend fun get(
        url: HttpUrl,
        includeAuth: Boolean = false
    ): ApiResult<String> {
        val cacheKey = url.toString()
        
        // Check for in-flight request (deduplication)
        mutex.withLock {
            inFlightRequests[cacheKey]?.let { existing ->
                Timber.d("Reusing in-flight GET request: $cacheKey")
                return existing.await()
            }
            
            // Create new deferred for this request
            val deferred = CompletableDeferred<ApiResult<String>>()
            inFlightRequests[cacheKey] = deferred
        }
        
        try {
            val result = executeWithRetry(url, includeAuth)
            
            mutex.withLock {
                inFlightRequests[cacheKey]?.complete(result)
                inFlightRequests.remove(cacheKey)
            }
            
            return result
        } catch (e: Exception) {
            val error = mapException(e)
            val result = ApiResult.Failure(error)
            
            mutex.withLock {
                inFlightRequests[cacheKey]?.complete(result)
                inFlightRequests.remove(cacheKey)
            }
            
            return result
        }
    }
    
    /**
     * Execute GET with retry logic
     * 2 retries on timeout/transient errors only, backoff 0.4s then 0.8s
     */
    private suspend fun executeWithRetry(
        url: HttpUrl,
        includeAuth: Boolean
    ): ApiResult<String> {
        var lastError: ApiError = ApiError.Unknown()
        
        for (attempt in 0..AppConfig.MAX_RETRY_ATTEMPTS) {
            try {
                val request = buildRequest(url, "GET", null, includeAuth)
                val response = executeRequest(request)
                
                return when {
                    response.isSuccessful -> {
                        val body = response.body?.string() ?: ""
                        ApiResult.Success(body)
                    }
                    else -> {
                        val body = response.body?.string()
                        val serverMessage = ApiError.extractServerMessage(body)
                        ApiResult.Failure(ApiError.fromStatusCode(response.code, serverMessage))
                    }
                }
            } catch (e: Exception) {
                lastError = mapException(e)
                
                // Only retry on timeout/transient errors
                if (!isRetryableError(lastError) || attempt >= AppConfig.MAX_RETRY_ATTEMPTS) {
                    return ApiResult.Failure(lastError)
                }
                
                // Exponential backoff
                val delayMs = AppConfig.RETRY_DELAYS_MS.getOrElse(attempt) { 800L }
                Timber.d("Retrying GET after ${delayMs}ms (attempt ${attempt + 1})")
                delay(delayMs)
            }
        }
        
        return ApiResult.Failure(lastError)
    }
    
    /**
     * POST request (no retry)
     */
    suspend fun post(
        url: HttpUrl,
        body: String,
        includeAuth: Boolean = false
    ): ApiResult<String> {
        return executeNonGet("POST", url, body, includeAuth)
    }
    
    /**
     * PUT request (no retry)
     */
    suspend fun put(
        url: HttpUrl,
        body: String,
        includeAuth: Boolean = false
    ): ApiResult<String> {
        return executeNonGet("PUT", url, body, includeAuth)
    }
    
    /**
     * PATCH request (no retry)
     */
    suspend fun patch(
        url: HttpUrl,
        body: String,
        includeAuth: Boolean = false
    ): ApiResult<String> {
        return executeNonGet("PATCH", url, body, includeAuth)
    }
    
    /**
     * DELETE request (no retry)
     */
    suspend fun delete(
        url: HttpUrl,
        includeAuth: Boolean = false
    ): ApiResult<String> {
        return try {
            val request = buildRequest(url, "DELETE", null, includeAuth)
            val response = executeRequest(request)
            
            when {
                response.isSuccessful -> {
                    ApiResult.Success(response.body?.string() ?: "")
                }
                else -> {
                    val body = response.body?.string()
                    val serverMessage = ApiError.extractServerMessage(body)
                    ApiResult.Failure(ApiError.fromStatusCode(response.code, serverMessage))
                }
            }
        } catch (e: Exception) {
            ApiResult.Failure(mapException(e))
        }
    }
    
    /**
     * Execute non-GET request (no retry)
     */
    private suspend fun executeNonGet(
        method: String,
        url: HttpUrl,
        body: String,
        includeAuth: Boolean
    ): ApiResult<String> {
        return try {
            val request = buildRequest(url, method, body, includeAuth)
            val response = executeRequest(request)
            
            when {
                response.isSuccessful -> {
                    ApiResult.Success(response.body?.string() ?: "")
                }
                else -> {
                    val responseBody = response.body?.string()
                    val serverMessage = ApiError.extractServerMessage(responseBody)
                    ApiResult.Failure(ApiError.fromStatusCode(response.code, serverMessage))
                }
            }
        } catch (e: Exception) {
            ApiResult.Failure(mapException(e))
        }
    }
    
    /**
     * Build request with headers
     */
    private fun buildRequest(
        url: HttpUrl,
        method: String,
        body: String?,
        includeAuth: Boolean
    ): Request {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
        
        // Add Content-Type for non-GET requests
        if (method != "GET" && body != null) {
            builder.header("Content-Type", "application/json")
        }
        
        // Add Authorization header if requested and token exists
        if (includeAuth) {
            tokenStorage.accessToken?.let { token ->
                builder.header("Authorization", "Bearer $token")
            }
        }
        
        // Set method and body
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(body?.toRequestBody(jsonMediaType) ?: "".toRequestBody(jsonMediaType))
            "PUT" -> builder.put(body?.toRequestBody(jsonMediaType) ?: "".toRequestBody(jsonMediaType))
            "PATCH" -> builder.patch(body?.toRequestBody(jsonMediaType) ?: "".toRequestBody(jsonMediaType))
            "DELETE" -> builder.delete(body?.toRequestBody(jsonMediaType))
        }
        
        return builder.build()
    }
    
    /**
     * Execute request synchronously (called from coroutine)
     */
    private fun executeRequest(request: Request): Response {
        return client.newCall(request).execute()
    }
    
    /**
     * Map exception to ApiError
     */
    private fun mapException(e: Exception): ApiError {
        return when (e) {
            is CancellationException -> ApiError.Cancelled
            is SocketTimeoutException -> ApiError.NetworkTimeout
            is UnknownHostException -> ApiError.NoConnection
            is HtmlResponseException -> ApiError.HtmlResponse
            is IOException -> ApiError.NoConnection
            else -> ApiError.Unknown(e.message ?: "An unexpected error occurred")
        }
    }
    
    /**
     * Check if error is retryable (timeout/transient only)
     */
    private fun isRetryableError(error: ApiError): Boolean {
        return when (error) {
            is ApiError.NetworkTimeout,
            is ApiError.ServiceUnavailable,
            is ApiError.NoConnection -> true
            else -> false
        }
    }
}

/**
 * Exception for HTML response detection
 */
class HtmlResponseException : IOException("Received HTML response instead of JSON")

