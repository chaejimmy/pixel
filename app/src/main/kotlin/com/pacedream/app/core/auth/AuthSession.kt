package com.pacedream.app.core.auth

import android.app.Activity
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * AuthSession - Authentication state management with iOS parity
 * 
 * Bootstrap behavior:
 * 1. Load tokens from secure storage
 * 2. If accessToken exists, mark authenticated immediately (avoid login loops)
 * 3. Attempt GET /v1/account/me to validate and fetch profile
 * 4. If 401: attempt refresh token once
 *    - POST /v1/auth/refresh-token (no auth header)
 *    - Fallback: POST <frontendBaseUrl>/api/proxy/auth/refresh-token
 * 5. If refresh succeeds: persist tokens, retry /account/me
 * 6. If non-401 error on /account/me: KEEP token and stay authenticated
 * 
 * Auth0 Exchange:
 * - POST /v1/auth/auth0/callback { accessToken, idToken }
 * - Parse response envelope tolerantly
 * - Validate JWT shape before accepting
 */
@Singleton
class AuthSession @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val appConfig: AppConfig,
    private val json: Json
) {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // ApiClient injected after construction to avoid circular dependency
    private var apiClient: ApiClient? = null
    
    val isAuthenticated: Boolean
        get() = _authState.value == AuthState.Authenticated
    
    /**
     * Set ApiClient after construction
     */
    fun setApiClient(client: ApiClient) {
        this.apiClient = client
    }
    
    /**
     * Initialize auth session on app launch
     */
    suspend fun initialize() {
        Timber.d("Initializing auth session")
        
        if (tokenStorage.hasTokens()) {
            // Mark authenticated immediately to avoid login loops (iOS parity)
            _authState.value = AuthState.Authenticated
            
            // Attempt to validate and fetch profile
            bootstrap()
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    /**
     * Bootstrap session by fetching user profile
     */
    private suspend fun bootstrap() {
        val client = apiClient ?: run {
            Timber.e("ApiClient not set")
            return
        }
        
        val url = appConfig.buildApiUrl("account", "me")
        val result = client.get(url, includeAuth = true)
        
        when (result) {
            is ApiResult.Success -> {
                parseAndSetUser(result.data)
            }
            is ApiResult.Failure -> {
                when (result.error) {
                    is ApiError.Unauthorized -> {
                        // Try refresh token once
                        if (!attemptTokenRefresh()) {
                            signOut()
                        }
                    }
                    else -> {
                        // Non-401 error: KEEP token and stay authenticated (iOS parity)
                        Timber.w("Failed to fetch user profile, using cached: ${result.error.message}")
                        loadCachedUser()
                    }
                }
            }
        }
    }
    
    /**
     * Attempt to refresh tokens
     */
    private suspend fun attemptTokenRefresh(): Boolean {
        val refreshToken = tokenStorage.refreshToken
        if (refreshToken.isNullOrBlank()) {
            Timber.d("No refresh token available")
            return false
        }
        
        val client = apiClient ?: return false
        
        // Try primary endpoint
        val primaryUrl = appConfig.buildApiUrl("auth", "refresh-token")
        val body = json.encodeToString(
            RefreshTokenRequest.serializer(),
            RefreshTokenRequest(refreshToken)
        )
        
        var result = client.post(primaryUrl, body, includeAuth = false)
        
        if (result is ApiResult.Failure) {
            // Try fallback endpoint via frontend proxy
            Timber.d("Primary refresh failed, trying fallback")
            val fallbackUrl = appConfig.buildFrontendUrl("api", "proxy", "auth", "refresh-token")
            result = client.post(fallbackUrl, body, includeAuth = false)
        }
        
        return when (result) {
            is ApiResult.Success -> {
                if (parseAndStoreTokens(result.data)) {
                    // Retry /account/me once
                    val meUrl = appConfig.buildApiUrl("account", "me")
                    val meResult = apiClient?.get(meUrl, includeAuth = true)
                    if (meResult is ApiResult.Success) {
                        parseAndSetUser(meResult.data)
                    }
                    true
                } else {
                    false
                }
            }
            is ApiResult.Failure -> {
                Timber.e("Token refresh failed: ${result.error.message}")
                false
            }
        }
    }
    
    /**
     * Login with Auth0 Universal Login
     */
    suspend fun loginWithAuth0(activity: Activity): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val auth0 = Auth0(
            appConfig.auth0ClientId,
            appConfig.auth0Domain
        )
        
        WebAuthProvider.login(auth0)
            .withScheme(appConfig.auth0Scheme)
            .withScope(appConfig.auth0Scopes)
            .withAudience(appConfig.auth0Audience)
            .start(activity, object : Callback<Credentials, AuthenticationException> {
                override fun onSuccess(result: Credentials) {
                    Timber.d("Auth0 login successful")
                    
                    // Exchange for backend tokens
                    CoroutineScope(Dispatchers.IO).launch {
                        val exchangeResult = exchangeAuth0Tokens(
                            accessToken = result.accessToken,
                            idToken = result.idToken
                        )
                        
                        if (exchangeResult) {
                            _authState.value = AuthState.Authenticated
                            bootstrap()
                            continuation.resume(Result.success(Unit))
                        } else {
                            continuation.resume(Result.failure(Exception("Failed to exchange Auth0 tokens")))
                        }
                    }
                }
                
                override fun onFailure(error: AuthenticationException) {
                    Timber.e(error, "Auth0 login failed")
                    if (error.isCanceled) {
                        continuation.resume(Result.failure(Exception("Login cancelled")))
                    } else {
                        continuation.resume(Result.failure(Exception(error.getDescription())))
                    }
                }
            })
    }
    
    /**
     * Exchange Auth0 tokens for backend session
     * POST /v1/auth/auth0/callback
     */
    private suspend fun exchangeAuth0Tokens(accessToken: String, idToken: String): Boolean {
        val client = apiClient ?: return false
        
        // Store Auth0 tokens
        tokenStorage.auth0AccessToken = accessToken
        tokenStorage.auth0IdToken = idToken
        
        val url = appConfig.buildApiUrl("auth", "auth0", "callback")
        val body = json.encodeToString(
            Auth0CallbackRequest.serializer(),
            Auth0CallbackRequest(accessToken, idToken)
        )
        
        val result = client.post(url, body, includeAuth = false)
        
        return when (result) {
            is ApiResult.Success -> parseAndStoreTokens(result.data)
            is ApiResult.Failure -> {
                Timber.e("Auth0 callback failed: ${result.error.message}")
                false
            }
        }
    }
    
    /**
     * Sign out - clear all tokens and state
     */
    fun signOut() {
        Timber.d("Signing out")
        tokenStorage.clearAll()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }
    
    /**
     * Parse auth response and store tokens (tolerant parsing)
     * Response envelope: { success/status: true, data: { accessToken, refreshToken, user? } }
     */
    private fun parseAndStoreTokens(responseBody: String): Boolean {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            
            // Check success/status field tolerantly
            val isSuccess = obj["success"]?.jsonPrimitive?.boolean == true ||
                           obj["status"]?.jsonPrimitive?.boolean == true
            
            if (!isSuccess) {
                Timber.w("Auth response indicates failure")
                return false
            }
            
            // Extract data object or use root
            val data = obj["data"]?.jsonObject ?: obj
            
            // Extract tokens (try multiple key formats)
            val accessToken = data["accessToken"]?.jsonPrimitive?.content
                ?: data["access_token"]?.jsonPrimitive?.content
            val refreshToken = data["refreshToken"]?.jsonPrimitive?.content
                ?: data["refresh_token"]?.jsonPrimitive?.content
            
            // Validate JWT shape (3 dot-separated parts)
            if (!tokenStorage.isValidJwtShape(accessToken)) {
                Timber.e("Invalid access token shape")
                return false
            }
            
            // Store tokens
            tokenStorage.storeTokens(accessToken, refreshToken)
            
            // Try to extract user if present
            data["user"]?.let { userElement ->
                parseAndSetUser(userElement.toString())
            }
            
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse auth response")
            false
        }
    }
    
    /**
     * Parse user from JSON response (tolerant parsing)
     */
    private fun parseAndSetUser(responseBody: String) {
        try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            
            // Find user data in common locations
            val userData = obj["data"]?.jsonObject
                ?: obj["user"]?.jsonObject
                ?: obj
            
            val user = User(
                id = userData["_id"]?.jsonPrimitive?.content
                    ?: userData["id"]?.jsonPrimitive?.content
                    ?: "",
                email = userData["email"]?.jsonPrimitive?.content,
                firstName = userData["firstName"]?.jsonPrimitive?.content
                    ?: userData["first_name"]?.jsonPrimitive?.content,
                lastName = userData["lastName"]?.jsonPrimitive?.content
                    ?: userData["last_name"]?.jsonPrimitive?.content,
                profileImage = userData["profileImage"]?.jsonPrimitive?.content
                    ?: userData["profile_image"]?.jsonPrimitive?.content
                    ?: userData["avatar"]?.jsonPrimitive?.content,
                phone = userData["phone"]?.jsonPrimitive?.content
            )
            
            _currentUser.value = user
            tokenStorage.userId = user.id
            tokenStorage.cachedUserSummary = responseBody
            
            Timber.d("User set: ${user.displayName}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse user")
            loadCachedUser()
        }
    }
    
    /**
     * Load cached user from storage
     */
    private fun loadCachedUser() {
        tokenStorage.cachedUserSummary?.let { cached ->
            try {
                parseAndSetUser(cached)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load cached user")
            }
        }
    }
}

/**
 * Authentication state
 */
sealed class AuthState {
    data object Unknown : AuthState()
    data object Unauthenticated : AuthState()
    data object Authenticated : AuthState()
}

/**
 * User model
 */
data class User(
    val id: String,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val profileImage: String? = null,
    val phone: String? = null
) {
    val displayName: String
        get() = when {
            !firstName.isNullOrBlank() && !lastName.isNullOrBlank() -> "$firstName $lastName"
            !firstName.isNullOrBlank() -> firstName
            !email.isNullOrBlank() -> email.substringBefore("@")
            else -> "User"
        }
}

/**
 * Request models
 */
@Serializable
data class RefreshTokenRequest(
    val refresh_token: String
)

@Serializable
data class Auth0CallbackRequest(
    val accessToken: String,
    val idToken: String
)

