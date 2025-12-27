package com.pacedream.app.core.config

import com.pacedream.app.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppConfig - Configuration management with iOS parity
 * 
 * URL Normalization Rules (matching iOS):
 * - Scheme defaults to https if missing
 * - Remove trailing slashes
 * - Append /v1 exactly once for API base URL
 * - Frontend URL does NOT get /v1
 * 
 * All URL building MUST use HttpUrl.Builder - NO string concatenation
 */
@Singleton
class AppConfig @Inject constructor() {
    
    /**
     * Normalized API base URL with /v1 suffix
     * Example: https://pacedream-backend.onrender.com/v1
     */
    val apiBaseUrl: HttpUrl by lazy {
        normalizeApiUrl(getBackendBaseUrl())
    }
    
    /**
     * Frontend base URL (NO /v1 suffix)
     * Example: https://www.pacedream.com
     */
    val frontendBaseUrl: HttpUrl by lazy {
        normalizeFrontendUrl(getFrontendBaseUrl())
    }
    
    // Auth0 Configuration
    val auth0Domain: String by lazy {
        getConfigValue("AUTH0_DOMAIN", DEFAULT_AUTH0_DOMAIN)
    }
    
    val auth0ClientId: String by lazy {
        getConfigValue("AUTH0_CLIENT_ID", DEFAULT_AUTH0_CLIENT_ID)
    }
    
    val auth0Audience: String by lazy {
        getConfigValue("AUTH0_AUDIENCE", "https://$auth0Domain/api/v2/")
    }
    
    val auth0Scopes: String by lazy {
        getConfigValue("AUTH0_SCOPES", DEFAULT_AUTH0_SCOPES)
    }
    
    val auth0Scheme: String = "pacedream"
    
    // Timeout configurations (matching iOS)
    companion object {
        const val REQUEST_TIMEOUT_SECONDS = 30L
        const val READ_TIMEOUT_SECONDS = 60L
        const val CONNECT_TIMEOUT_SECONDS = 30L
        
        // Retry configuration (GET only, 2 retries with exponential backoff)
        const val MAX_RETRY_ATTEMPTS = 2
        val RETRY_DELAYS_MS = listOf(400L, 800L)
        
        // Default URLs
        private const val DEFAULT_BACKEND_URL = "https://pacedream-backend.onrender.com"
        private const val DEFAULT_FRONTEND_URL = "https://www.pacedream.com"
        
        // Auth0 defaults (must be configured in BuildConfig)
        private const val DEFAULT_AUTH0_DOMAIN = "dev-pacedream.us.auth0.com"
        private const val DEFAULT_AUTH0_CLIENT_ID = "YOUR_AUTH0_CLIENT_ID"
        private const val DEFAULT_AUTH0_SCOPES = "openid profile email offline_access"
    }
    
    /**
     * Get backend base URL with fallback support
     * Priority: BACKEND_BASE_URL > PD_BACKEND_BASE_URL > default
     */
    private fun getBackendBaseUrl(): String {
        return getConfigValue("BACKEND_BASE_URL", null)
            ?: getConfigValue("PD_BACKEND_BASE_URL", null)
            ?: DEFAULT_BACKEND_URL
    }
    
    /**
     * Get frontend base URL with fallback support
     * Priority: FRONTEND_BASE_URL > PD_FRONTEND_BASE_URL > default
     */
    private fun getFrontendBaseUrl(): String {
        return getConfigValue("FRONTEND_BASE_URL", null)
            ?: getConfigValue("PD_FRONTEND_BASE_URL", null)
            ?: DEFAULT_FRONTEND_URL
    }
    
    /**
     * Get config value from BuildConfig using reflection
     */
    private fun getConfigValue(fieldName: String, default: String?): String? {
        return try {
            val field = BuildConfig::class.java.getField(fieldName)
            val value = field.get(null) as? String
            if (!value.isNullOrBlank()) value else default
        } catch (e: Exception) {
            default
        }
    }
    
    /**
     * Normalize API URL:
     * - Ensure https scheme
     * - Remove trailing slashes
     * - Append /v1 exactly once
     */
    private fun normalizeApiUrl(rawUrl: String): HttpUrl {
        var url = rawUrl.trim()
        
        // Ensure scheme exists (default to https)
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        
        // Remove trailing slashes
        url = url.trimEnd('/')
        
        // Remove existing /v1 if present (we'll add it back exactly once)
        if (url.endsWith("/v1")) {
            url = url.dropLast(3)
        }
        
        // Parse and rebuild with /v1
        val httpUrl = url.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid backend URL: $rawUrl")
        
        return httpUrl.newBuilder()
            .addPathSegment("v1")
            .build()
    }
    
    /**
     * Normalize frontend URL:
     * - Ensure https scheme
     * - Remove trailing slashes
     * - NO /v1 suffix
     */
    private fun normalizeFrontendUrl(rawUrl: String): HttpUrl {
        var url = rawUrl.trim()
        
        // Ensure scheme exists
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        
        // Remove trailing slashes
        url = url.trimEnd('/')
        
        return url.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid frontend URL: $rawUrl")
    }
    
    /**
     * Build API URL using HttpUrl.Builder (NO string concatenation)
     */
    fun buildApiUrl(vararg pathSegments: String): HttpUrl {
        val builder = apiBaseUrl.newBuilder()
        pathSegments.forEach { segment ->
            segment.split("/").filter { it.isNotBlank() }.forEach { part ->
                builder.addPathSegment(part)
            }
        }
        return builder.build()
    }
    
    /**
     * Build API URL with query parameters
     */
    fun buildApiUrl(
        vararg pathSegments: String,
        queryParams: Map<String, String?> = emptyMap()
    ): HttpUrl {
        val builder = apiBaseUrl.newBuilder()
        pathSegments.forEach { segment ->
            segment.split("/").filter { it.isNotBlank() }.forEach { part ->
                builder.addPathSegment(part)
            }
        }
        queryParams.forEach { (key, value) ->
            if (value != null) {
                builder.addQueryParameter(key, value)
            }
        }
        return builder.build()
    }
    
    /**
     * Build frontend URL using HttpUrl.Builder
     */
    fun buildFrontendUrl(vararg pathSegments: String): HttpUrl {
        val builder = frontendBaseUrl.newBuilder()
        pathSegments.forEach { segment ->
            segment.split("/").filter { it.isNotBlank() }.forEach { part ->
                builder.addPathSegment(part)
            }
        }
        return builder.build()
    }
}

