package com.shourov.apps.pacedream.core.network.auth

import android.app.Activity
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.callback.Callback
import com.auth0.android.result.Credentials
import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * AuthSession - Manages authentication state matching iOS behavior
 * 
 * On app launch:
 * - Load tokens; if accessToken exists mark authenticated=true immediately
 * - Then attempt GET /v1/account/me to fetch profile
 * - If 401: attempt refresh, if fails sign out
 * - If non-401 error: keep token, show cached user
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
    
    private var apiClient: ApiClient? = null
    
    /**
     * Set the API client (injected after creation to avoid circular dependency)
     */
    fun setApiClient(client: ApiClient) {
        this.apiClient = client
    }
    
    /**
     * Initialize auth session on app launch
     */
    suspend fun initialize() {
        Timber.d("Initializing auth session")
        
        // Load stored tokens
        if (tokenStorage.hasTokens()) {
            // Mark authenticated immediately to avoid UI loops
            _authState.value = AuthState.Authenticated
            
            // Attempt to fetch user profile
            bootstrap()
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    /**
     * Refresh the current user's profile without changing auth state on non-401 failures.
     * iOS parity: do not force logout for transient/profile failures; keep last good cached user if present.
     */
    suspend fun refreshProfile() {
        if (!tokenStorage.hasTokens()) {
            _authState.value = AuthState.Unauthenticated
            _currentUser.value = null
            return
        }
        // Keep authenticated immediately; bootstrap will handle 401 via refresh/signout.
        if (_authState.value != AuthState.Authenticated) {
            _authState.value = AuthState.Authenticated
        }
        bootstrap()
    }
    
    /**
     * Bootstrap the session by fetching user profile
     */
    private suspend fun bootstrap(retriedAfterRefresh: Boolean = false) {
        val client = apiClient ?: run {
            Timber.e("ApiClient not set")
            return
        }

        // iOS parity: try /account/me first, then fallback endpoints for profile.
        val urls = listOf(
            appConfig.buildApiUrl("account", "me"),
            appConfig.buildApiUrl("users", "get", "profile"),
            appConfig.buildApiUrl("user", "get", "profile"),
        )

        for (url in urls) {
            when (val result = client.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    parseAndSetUser(result.data)
                    return
                }
                is ApiResult.Failure -> {
                    when (result.error) {
                        is ApiError.Unauthorized -> {
                            // Refresh once, retry once (iOS parity). Avoid infinite loops.
                            if (!retriedAfterRefresh && attemptTokenRefresh()) {
                                bootstrap(retriedAfterRefresh = true)
                            } else {
                                signOut()
                            }
                            return
                        }
                        else -> {
                            // Try the next endpoint; do not clear last-good user.
                            Timber.w("Profile endpoint failed (${url.encodedPath}): ${result.error.message}")
                        }
                    }
                }
            }
        }

        // Keep token, try cached user (or keep last-good in-memory user if present).
        Timber.w("All profile endpoints failed; using cached user if available.")
        loadCachedUser()
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
        
        // Try primary refresh endpoint
        val primaryUrl = appConfig.buildApiUrl("auth", "refresh-token")
        val body = json.encodeToString(RefreshTokenRequest.serializer(), RefreshTokenRequest(refreshToken))
        
        var result = client.post(primaryUrl, body, includeAuth = false)
        
        if (result is ApiResult.Failure) {
            // Try fallback endpoint via frontend proxy
            Timber.d("Primary refresh failed, trying fallback")
            val fallbackUrl = appConfig.buildFrontendUrl("api", "proxy", "auth", "refresh-token")
            result = client.post(fallbackUrl, body, includeAuth = false)
        }
        
        return when (result) {
            is ApiResult.Success -> {
                parseAndStoreTokens(result.data)
            }
            is ApiResult.Failure -> {
                Timber.e("Token refresh failed: ${result.error.message}")
                false
            }
        }
    }
    
    /**
     * Handle Auth0 login callback
     * Exchange Auth0 tokens for backend session
     */
    suspend fun handleAuth0Login(auth0AccessToken: String, auth0IdToken: String): Boolean {
        val client = apiClient ?: return false
        
        // Store Auth0 tokens
        tokenStorage.auth0AccessToken = auth0AccessToken
        tokenStorage.auth0IdToken = auth0IdToken
        
        // Exchange for backend session
        val url = appConfig.buildApiUrl("auth", "auth0", "callback")
        val body = json.encodeToString(
            Auth0CallbackRequest.serializer(),
            Auth0CallbackRequest(auth0AccessToken, auth0IdToken)
        )
        
        val result = client.post(url, body, includeAuth = false)
        
        return when (result) {
            is ApiResult.Success -> {
                if (parseAndStoreTokens(result.data)) {
                    _authState.value = AuthState.Authenticated
                    bootstrap()
                    true
                } else {
                    false
                }
            }
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
     * Login with Auth0 Universal Login
     * Launches Auth0 WebAuth and exchanges tokens for backend session
     * 
     * @param connection Optional Auth0 connection name (e.g., "google-oauth2" for Google OAuth)
     */
    suspend fun loginWithAuth0(
        activity: Activity,
        connection: String? = null
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val auth0 = Auth0(
            appConfig.auth0ClientId,
            appConfig.auth0Domain
        )
        
        val webAuth = WebAuthProvider.login(auth0)
            .withScheme(appConfig.auth0Scheme)
            .withScope("openid profile email offline_access")
            .withAudience(appConfig.auth0Audience)
        
        // Add connection parameter if specified (for social logins like Google OAuth)
        connection?.let {
            webAuth.withConnection(it)
        }
        
        webAuth.start(activity, object : Callback<Credentials, AuthenticationException> {
                override fun onSuccess(result: Credentials) {
                    Timber.d("Auth0 login successful")
                    // Exchange for backend tokens
                    CoroutineScope(Dispatchers.IO).launch {
                        val exchangeResult = handleAuth0Login(
                            auth0AccessToken = result.accessToken,
                            auth0IdToken = result.idToken
                        )
                        if (exchangeResult) {
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
     * Login with email and password
     */
    suspend fun loginWithEmailPassword(email: String, password: String): Result<Unit> {
        val client = apiClient ?: return Result.failure(Exception("API client not initialized"))
        
        val url = appConfig.buildApiUrl("auth", "login", "email")
        val body = json.encodeToString(
            EmailLoginRequest.serializer(),
            EmailLoginRequest(email, password)
        )
        
        val result = client.post(url, body, includeAuth = false)
        
        return when (result) {
            is ApiResult.Success -> {
                if (parseAndStoreTokens(result.data)) {
                    _authState.value = AuthState.Authenticated
                    bootstrap()
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Invalid response from server"))
                }
            }
            is ApiResult.Failure -> {
                Result.failure(Exception(result.error.message ?: "Login failed"))
            }
        }
    }
    
    /**
     * Register with email and password
     */
    suspend fun registerWithEmailPassword(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<Unit> {
        val client = apiClient ?: return Result.failure(Exception("API client not initialized"))
        
        val url = appConfig.buildApiUrl("auth", "signup", "email")
        val body = json.encodeToString(
            EmailRegisterRequest.serializer(),
            EmailRegisterRequest(
                email = email,
                password = password,
                firstName = firstName,
                lastName = lastName
            )
        )
        
        val result = client.post(url, body, includeAuth = false)
        
        return when (result) {
            is ApiResult.Success -> {
                if (parseAndStoreTokens(result.data)) {
                    _authState.value = AuthState.Authenticated
                    bootstrap()
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Invalid response from server"))
                }
            }
            is ApiResult.Failure -> {
                Result.failure(Exception(result.error.message ?: "Registration failed"))
            }
        }
    }
    
    /**
     * Parse auth response and store tokens
     * Response is an envelope: { success:true, data:{ accessToken, refreshToken, user? } } 
     * OR { status:true, data:{...} }
     */
    private fun parseAndStoreTokens(responseBody: String): Boolean {
        return try {
            val jsonElement = json.parseToJsonElement(responseBody)
            val jsonObject = jsonElement.jsonObject
            
            // Check success/status field tolerantly
            val isSuccess = jsonObject["success"]?.jsonPrimitive?.boolean == true ||
                           jsonObject["status"]?.jsonPrimitive?.boolean == true
            
            if (!isSuccess) {
                Timber.w("Auth response indicates failure")
                return false
            }
            
            // Extract data object
            val data = jsonObject["data"]?.jsonObject ?: jsonObject
            
            // Extract tokens
            val accessToken = data["accessToken"]?.jsonPrimitive?.content
                ?: data["access_token"]?.jsonPrimitive?.content
            val refreshToken = data["refreshToken"]?.jsonPrimitive?.content
                ?: data["refresh_token"]?.jsonPrimitive?.content
            
            // Validate JWT shape
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
     * Parse user from JSON response
     */
    private fun parseAndSetUser(responseBody: String) {
        try {
            val jsonElement = json.parseToJsonElement(responseBody)
            val jsonObject = jsonElement.jsonObject
            
            // Try to find user data in common locations
            val userData = jsonObject["data"]?.jsonObject
                ?: jsonObject["user"]?.jsonObject
                ?: jsonObject
            
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
    
    /**
     * Check if user is currently authenticated
     */
    val isAuthenticated: Boolean
        get() = _authState.value == AuthState.Authenticated
}

/**
 * Authentication state
 */
sealed class AuthState {
    object Unknown : AuthState()
    object Unauthenticated : AuthState()
    object Authenticated : AuthState()
    object AuthenticatedWithRefreshNeeded : AuthState()
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

@Serializable
data class EmailLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class EmailRegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)

