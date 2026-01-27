package com.shourov.apps.pacedream.core.network.config

import com.shourov.apps.pacedream.core.network.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppConfig - Configuration management matching iOS conventions
 * 
 * Handles URL normalization for backend and frontend URLs:
 * - Read BACKEND_BASE_URL from config (BuildConfig field). Support fallback key PD_BACKEND_BASE_URL if present.
 * - Normalize into apiBaseUrl = https://<host>/v1 (append /v1 exactly once, remove trailing slashes, ensure scheme exists default https).
 * - Also read FRONTEND_BASE_URL (fallback PD_FRONTEND_BASE_URL) default https://www.pacedream.com (NO /v1).
 */
@Singleton
class AppConfig @Inject constructor() {
    
    /**
     * The normalized API base URL with /v1 suffix
     * Example: https://api.pacedream.com/v1
     */
    val apiBaseUrl: HttpUrl by lazy {
        val rawUrl = getBackendBaseUrl()
        normalizeApiUrl(rawUrl)
    }
    
    /**
     * The frontend base URL (NO /v1 suffix)
     * Example: https://www.pacedream.com
     */
    val frontendBaseUrl: HttpUrl by lazy {
        val rawUrl = getFrontendBaseUrl()
        normalizeFrontendUrl(rawUrl)
    }
    
    /**
     * Get the API base URL as a string (for Retrofit baseUrl)
     */
    val apiBaseUrlString: String
        get() = apiBaseUrl.toString()
    
    /**
     * Get the frontend base URL as a string
     */
    val frontendBaseUrlString: String
        get() = frontendBaseUrl.toString()
    
    // Auth0 Configuration
    val auth0Domain: String
        get() = getAuth0Config("AUTH0_DOMAIN", DEFAULT_AUTH0_DOMAIN)
    
    val auth0ClientId: String
        get() = getAuth0Config("AUTH0_CLIENT_ID", DEFAULT_AUTH0_CLIENT_ID)
    
    val auth0Scheme: String
        get() = "pacedream"
    
    val auth0Audience: String
        get() = getAuth0Config("AUTH0_AUDIENCE", "https://$auth0Domain/api/v2/")
    
    private fun getAuth0Config(fieldName: String, default: String): String {
        return try {
            val configClass = BuildConfig::class.java
            val field = configClass.getField(fieldName)
            val value = field.get(null) as? String
            if (!value.isNullOrBlank()) value else default
        } catch (e: Exception) {
            default
        }
    }
    
    /**
     * Gets the backend base URL from BuildConfig with fallback
     */
    private fun getBackendBaseUrl(): String {
        // Try BACKEND_BASE_URL first, then PD_BACKEND_BASE_URL
        return try {
            val url = BuildConfig.SERVICE_URL
            if (url.isNotBlank()) url else getDefaultBackendUrl()
        } catch (e: Exception) {
            getDefaultBackendUrl()
        }
    }
    
    /**
     * Gets the frontend base URL from BuildConfig with fallback
     */
    private fun getFrontendBaseUrl(): String {
        // Try FRONTEND_BASE_URL first, then default
        return try {
            val configClass = BuildConfig::class.java
            val field = try {
                configClass.getField("FRONTEND_BASE_URL")
            } catch (e: NoSuchFieldException) {
                try {
                    configClass.getField("PD_FRONTEND_BASE_URL")
                } catch (e: NoSuchFieldException) {
                    null
                }
            }
            val url = field?.get(null) as? String
            if (!url.isNullOrBlank()) url else DEFAULT_FRONTEND_URL
        } catch (e: Exception) {
            DEFAULT_FRONTEND_URL
        }
    }
    
    private fun getDefaultBackendUrl(): String = DEFAULT_BACKEND_URL
    
    /**
     * Normalizes the API URL:
     * - Ensures scheme exists (defaults to https)
     * - Removes trailing slashes
     * - Appends /v1 exactly once
     */
    private fun normalizeApiUrl(rawUrl: String): HttpUrl {
        var url = rawUrl.trim()
        
        // Ensure scheme exists
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        
        // Remove trailing slashes
        url = url.trimEnd('/')
        
        // Remove existing /v1 suffix if present (we'll add it back)
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
     * Normalizes the frontend URL:
     * - Ensures scheme exists (defaults to https)
     * - Removes trailing slashes
     * - Does NOT append /v1
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
     * Build a URL for an API endpoint using HttpUrl.Builder
     * This method ensures NO string concatenation for URL building
     */
    fun buildApiUrl(vararg pathSegments: String): HttpUrl {
        val builder = apiBaseUrl.newBuilder()
        pathSegments.forEach { segment ->
            // Handle segments that may contain slashes
            segment.split("/").filter { it.isNotBlank() }.forEach { part ->
                builder.addPathSegment(part)
            }
        }
        return builder.build()
    }
    
    /**
     * Build a URL for a frontend endpoint using HttpUrl.Builder
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
    
    /**
     * Build a URL with query parameters
     */
    fun buildApiUrlWithQuery(
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
    
    companion object {
        // Default backend URL - matches iOS (must normalize to /v1 exactly once)
        private const val DEFAULT_BACKEND_URL = "https://pacedream-backend.onrender.com"
        private const val DEFAULT_FRONTEND_URL = "https://www.pacedream.com"
        
        // Auth0 defaults - configure in secrets.defaults.properties or BuildConfig
        // The actual client ID must be set in secrets.defaults.properties as AUTH0_CLIENT_ID
        private const val DEFAULT_AUTH0_DOMAIN = "dev-pacedream.us.auth0.com"
        private const val DEFAULT_AUTH0_CLIENT_ID = "" // Empty string - must be configured in secrets file
        
        // Timeout configurations (matching iOS)
        const val REQUEST_TIMEOUT_SECONDS = 30L
        const val RESOURCE_TIMEOUT_SECONDS = 60L
        const val READ_TIMEOUT_SECONDS = 60L
        
        // Retry configurations
        // iOS parity: retry transient failures up to 3 attempts total (initial + 2 retries)
        // Backoff: 0.5s, 1.0s
        const val MAX_RETRY_ATTEMPTS = 2
        val RETRY_BACKOFF_DELAYS = listOf(500L, 1000L) // milliseconds
    }
}

