package com.pacedream.app.core.network

import com.pacedream.app.core.auth.TokenStorage
import com.pacedream.app.core.config.AppConfig
import kotlinx.coroutines.CancellationException
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
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import com.shourov.apps.pacedream.BuildConfig
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

    // In-flight refresh de-duplication (avoid multiple concurrent refresh calls)
    private val refreshMutex = Mutex()
    private var inFlightRefresh: Deferred<Boolean>? = null
    
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(createLoggingInterceptor())
            .addInterceptor(createHtmlHardeningInterceptor())
            .build()
    }
    
    companion object {
        private val BEARER_REGEX = Regex("Bearer [A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+")
        private val TOKEN_FIELD_REGEX = Regex("\"(access_?[Tt]oken|refresh_?[Tt]oken|id_?[Tt]oken)\"\\s*:\\s*\"[^\"]+\"")
    }

    /**
     * Create logging interceptor with token redaction
     */
    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            val redacted = message
                .replace(BEARER_REGEX, "Bearer [REDACTED]")
                .replace(TOKEN_FIELD_REGEX, "\"$1\": \"[REDACTED]\"")
            Timber.d(redacted)
        }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
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
        // Important: await() must be called OUTSIDE the mutex lock to avoid deadlock
        val existingDeferred = mutex.withLock {
            inFlightRequests[cacheKey]
        }
        if (existingDeferred != null) {
            Timber.d("Reusing in-flight GET request: $cacheKey")
            return existingDeferred.await()
        }

        // Create new deferred for this request
        val deferred = CompletableDeferred<ApiResult<String>>()
        mutex.withLock {
            // Double-check: another coroutine may have registered between our check and lock
            inFlightRequests[cacheKey]?.let { racing ->
                return@withLock racing
            }
            inFlightRequests[cacheKey] = deferred
            null
        }?.let { racing ->
            return racing.await()
        }

        try {
            val result = executeWithRetry(url, includeAuth)
            deferred.complete(result)
            inFlightRequests.remove(cacheKey)
            return result
        } catch (e: CancellationException) {
            deferred.cancel(e)
            inFlightRequests.remove(cacheKey)
            throw e
        } catch (e: Exception) {
            val error = mapException(e)
            val result = ApiResult.Failure(error)
            deferred.complete(result)
            inFlightRequests.remove(cacheKey)
            return result
        }
    }
    
    /**
     * Execute GET with retry logic
     * 2 retries on timeout/transient errors only, backoff 0.4s then 0.8s.
     *
     * On a 401 with `includeAuth = true`, a single token refresh + request
     * retry is attempted (iOS parity). The refresh is shared across concurrent
     * callers via [refreshAccessTokenIfPossible].
     */
    private suspend fun executeWithRetry(
        url: HttpUrl,
        includeAuth: Boolean,
        allowAuthRefresh: Boolean = true
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        var lastError: ApiError = ApiError.Unknown()

        for (attempt in 0..AppConfig.MAX_RETRY_ATTEMPTS) {
            try {
                val request = buildRequest(url, "GET", null, includeAuth)
                val response = executeRequest(request)

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    return@withContext ApiResult.Success(body)
                }

                // 401 handling: try refresh once, then retry the original request once.
                if (allowAuthRefresh && includeAuth && response.code == 401) {
                    response.close()
                    val refreshed = refreshAccessTokenIfPossible()
                    if (refreshed) {
                        return@withContext executeWithRetry(
                            url = url,
                            includeAuth = true,
                            allowAuthRefresh = false
                        )
                    }
                    return@withContext ApiResult.Failure(ApiError.Unauthorized)
                }

                val body = response.body?.string()
                val serverMessage = ApiError.extractServerMessage(body)
                return@withContext ApiResult.Failure(
                    ApiError.fromStatusCode(response.code, serverMessage)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = mapException(e)

                // Only retry on timeout/transient errors
                if (!isRetryableError(lastError) || attempt >= AppConfig.MAX_RETRY_ATTEMPTS) {
                    return@withContext ApiResult.Failure(lastError)
                }

                // Exponential backoff
                val delayMs = AppConfig.RETRY_DELAYS_MS.getOrElse(attempt) { 800L }
                Timber.d("Retrying GET after ${delayMs}ms (attempt ${attempt + 1})")
                delay(delayMs)
            }
        }

        ApiResult.Failure(lastError)
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
     * DELETE request (no retry). Refreshes once on 401.
     */
    suspend fun delete(
        url: HttpUrl,
        includeAuth: Boolean = false
    ): ApiResult<String> = executeDeleteInternal(url, includeAuth, allowAuthRefresh = true)

    private suspend fun executeDeleteInternal(
        url: HttpUrl,
        includeAuth: Boolean,
        allowAuthRefresh: Boolean
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest(url, "DELETE", null, includeAuth)
            val response = executeRequest(request)

            if (response.isSuccessful) {
                return@withContext ApiResult.Success(response.body?.string() ?: "")
            }

            if (allowAuthRefresh && includeAuth && response.code == 401) {
                response.close()
                val refreshed = refreshAccessTokenIfPossible()
                if (refreshed) {
                    return@withContext executeDeleteInternal(url, includeAuth, allowAuthRefresh = false)
                }
                return@withContext ApiResult.Failure(ApiError.Unauthorized)
            }

            val respBody = response.body?.string()
            val serverMessage = ApiError.extractServerMessage(respBody)
            ApiResult.Failure(ApiError.fromStatusCode(response.code, serverMessage))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Failure(mapException(e))
        }
    }

    /**
     * Execute non-GET request (no retry). Refreshes once on 401.
     */
    private suspend fun executeNonGet(
        method: String,
        url: HttpUrl,
        body: String,
        includeAuth: Boolean,
        allowAuthRefresh: Boolean = true
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest(url, method, body, includeAuth)
            val response = executeRequest(request)

            if (response.isSuccessful) {
                return@withContext ApiResult.Success(response.body?.string() ?: "")
            }

            if (allowAuthRefresh && includeAuth && response.code == 401) {
                response.close()
                val refreshed = refreshAccessTokenIfPossible()
                if (refreshed) {
                    return@withContext executeNonGet(
                        method = method,
                        url = url,
                        body = body,
                        includeAuth = true,
                        allowAuthRefresh = false
                    )
                }
                return@withContext ApiResult.Failure(ApiError.Unauthorized)
            }

            val responseBody = response.body?.string()
            val serverMessage = ApiError.extractServerMessage(responseBody)
            ApiResult.Failure(ApiError.fromStatusCode(response.code, serverMessage))
        } catch (e: CancellationException) {
            throw e
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

    /**
     * POST multipart/form-data request (for file uploads, no retry).
     * Refreshes once on 401. iOS parity: APIClient.requestMultipart()
     */
    suspend fun postMultipart(
        url: HttpUrl,
        parts: List<MultipartBody.Part>,
        includeAuth: Boolean = false
    ): ApiResult<String> = postMultipartInternal(url, parts, includeAuth, allowAuthRefresh = true)

    private suspend fun postMultipartInternal(
        url: HttpUrl,
        parts: List<MultipartBody.Part>,
        includeAuth: Boolean,
        allowAuthRefresh: Boolean
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            parts.forEach { bodyBuilder.addPart(it) }
            val multipartBody = bodyBuilder.build()

            val requestBuilder = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .post(multipartBody)

            if (includeAuth) {
                tokenStorage.accessToken?.let { token ->
                    requestBuilder.header("Authorization", "Bearer $token")
                }
            }

            val response = executeRequest(requestBuilder.build())
            if (response.isSuccessful) {
                return@withContext ApiResult.Success(response.body?.string() ?: "")
            }

            if (allowAuthRefresh && includeAuth && response.code == 401) {
                response.close()
                val refreshed = refreshAccessTokenIfPossible()
                if (refreshed) {
                    return@withContext postMultipartInternal(
                        url = url,
                        parts = parts,
                        includeAuth = true,
                        allowAuthRefresh = false
                    )
                }
                return@withContext ApiResult.Failure(ApiError.Unauthorized)
            }

            val responseBody = response.body?.string()
            val serverMessage = ApiError.extractServerMessage(responseBody)
            ApiResult.Failure(ApiError.fromStatusCode(response.code, serverMessage))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Failure(mapException(e))
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 401 refresh handling (iOS parity).
    //
    // We call the refresh endpoints directly via OkHttp here (rather than
    // via our own post() helpers) for two reasons:
    //   1. Avoid recursion through the 401-handling code path.
    //   2. Avoid a DI cycle with AuthRepository (which depends on ApiClient).
    // ─────────────────────────────────────────────────────────────────────

    private suspend fun refreshAccessTokenIfPossible(): Boolean = coroutineScope {
        val refreshToken = tokenStorage.refreshToken
        if (refreshToken.isNullOrBlank()) {
            Timber.d("ApiClient: no refresh token available, cannot refresh")
            return@coroutineScope false
        }

        val deferred = refreshMutex.withLock {
            inFlightRefresh?.takeIf { it.isActive } ?: async(Dispatchers.IO) {
                performRefresh(refreshToken)
            }.also { inFlightRefresh = it }
        }

        val ok = deferred.await()

        refreshMutex.withLock {
            if (inFlightRefresh == deferred) inFlightRefresh = null
        }

        ok
    }

    private fun performRefresh(refreshToken: String): Boolean {
        // Use JSON encoder rather than string interpolation in case the
        // refresh token ever contains characters needing escaping.
        val requestBodyJson = kotlinx.serialization.json.buildJsonObject {
            put("refreshToken", kotlinx.serialization.json.JsonPrimitive(refreshToken))
        }.toString()

        // Primary: POST /v1/auth/refresh-token (no auth header)
        val primaryUrl = appConfig.buildApiUrl("auth", "refresh-token")
        val primaryResponse = runCatching {
            client.newCall(
                Request.Builder()
                    .url(primaryUrl)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .post(requestBodyJson.toRequestBody(jsonMediaType))
                    .build()
            ).execute()
        }.getOrNull()

        primaryResponse?.use { resp ->
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string().orEmpty()
                if (parseAndStoreRefreshTokens(bodyStr)) return true
            }
        }

        // Fallback: frontend proxy
        val fallbackUrl = appConfig.buildFrontendUrl("api", "proxy", "auth", "refresh-token")
        val fallbackResponse = runCatching {
            client.newCall(
                Request.Builder()
                    .url(fallbackUrl)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .post(requestBodyJson.toRequestBody(jsonMediaType))
                    .build()
            ).execute()
        }.getOrNull()

        fallbackResponse?.use { resp ->
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string().orEmpty()
                if (parseAndStoreRefreshTokens(bodyStr)) return true
            }
        }

        Timber.w("ApiClient: token refresh failed on both primary and fallback endpoints")
        return false
    }

    private fun parseAndStoreRefreshTokens(responseBody: String): Boolean {
        return try {
            if (responseBody.isBlank()) return false
            val root = json.parseToJsonElement(responseBody).jsonObject

            val isSuccess = root["success"]?.jsonPrimitive?.boolean == true ||
                root["status"]?.jsonPrimitive?.boolean == true ||
                // Some envelopes omit success flag entirely; tolerate when tokens exist.
                (root["data"] != null || root["accessToken"] != null || root["access_token"] != null)

            if (!isSuccess) return false

            val data = root["data"]?.jsonObject ?: root
            val accessToken = data["accessToken"]?.jsonPrimitive?.contentOrNull
                ?: data["access_token"]?.jsonPrimitive?.contentOrNull
            val newRefreshToken = data["refreshToken"]?.jsonPrimitive?.contentOrNull
                ?: data["refresh_token"]?.jsonPrimitive?.contentOrNull

            if (!isValidJwtShape(accessToken)) return false

            tokenStorage.storeTokens(accessToken, newRefreshToken ?: tokenStorage.refreshToken)
            true
        } catch (e: Exception) {
            Timber.e(e, "ApiClient: failed to parse refresh-token response")
            false
        }
    }

    private fun isValidJwtShape(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val parts = token.split(".")
        return parts.size == 3 && parts.all { it.isNotBlank() }
    }
}

/**
 * Exception for HTML response detection
 */
class HtmlResponseException : IOException("Received HTML response instead of JSON")


