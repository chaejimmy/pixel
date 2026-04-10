package com.pacedream.app.core.auth

import android.app.Activity
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthSession as LegacyAuthSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * SessionManager - Authentication state management with iOS parity
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
class SessionManager @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val appConfig: AppConfig,
    private val authRepository: AuthRepository,
    private val json: Json,
    private val legacyAuthSession: LegacyAuthSession
) {

    /**
     * UI-facing result for auth actions (cancel is not an error).
     */
    sealed class AuthActionResult {
        data object Success : AuthActionResult()
        data object Cancelled : AuthActionResult()
        data class Error(val message: String) : AuthActionResult()
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Sync the legacy AuthSession (core/network) after login so that all
     * production screens observing its authState see the change immediately.
     *
     * The two auth systems share the same EncryptedSharedPreferences file.
     * Tokens are already persisted by the time this runs — we just need
     * to tell the legacy layer to re-read them and update its in-memory
     * StateFlows.
     */
    private fun syncLegacyAuthSession() {
        scope.launch {
            Timber.d("SessionManager: syncing legacy AuthSession after login")
            legacyAuthSession.refreshProfile()
            Timber.d("SessionManager: legacy AuthSession synced — authState=${legacyAuthSession.authState.value}")
        }
    }
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    val isAuthenticated: Boolean
        get() = _authState.value == AuthState.Authenticated
    
    /**
     * Initialize auth session on app launch
     */
    suspend fun initialize() {
        try {
            Timber.d("Initializing auth session")

            if (tokenStorage.hasTokens()) {
                // Mark authenticated immediately to avoid login loops (iOS parity)
                _authState.value = AuthState.Authenticated

                // Attempt to validate and fetch profile
                bootstrap()
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            Timber.e(e, "Auth session initialization failed, falling back to unauthenticated")
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    /**
     * Bootstrap session by fetching user profile
     */
    private suspend fun bootstrap() {
        val result = authRepository.fetchProfileWithFallbacks()
        when (result) {
            is ApiResult.Success -> parseAndSetUser(result.data)
            is ApiResult.Failure -> {
                when (result.error) {
                    is ApiError.Unauthorized -> {
                        // Try refresh token once; if succeeds retry profile once
                        if (attemptTokenRefresh()) {
                            val retry = authRepository.fetchProfileWithFallbacks()
                            if (retry is ApiResult.Success) {
                                parseAndSetUser(retry.data)
                            }
                        } else {
                            signOut()
                        }
                    }
                    else -> {
                        // Non-401 error: KEEP token and stay authenticated (avoid login loops)
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
        
        val result = authRepository.refresh(refreshToken)
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
     * Login with Auth0 Universal Login
     */
    suspend fun loginWithAuth0(activity: Activity, connection: Auth0Connection): AuthActionResult =
        suspendCancellableCoroutine { continuation ->
        // Guard: crash-proof when Auth0 credentials are not configured
        if (appConfig.auth0Domain.isBlank() || appConfig.auth0ClientId.isBlank()) {
            Timber.e("Auth0 credentials not configured — domain='${appConfig.auth0Domain}', clientId length=${appConfig.auth0ClientId.length}")
            continuation.resume(AuthActionResult.Error("Auth0 is not configured. Please check app settings."))
            return@suspendCancellableCoroutine
        }

        try {
            val auth0 = Auth0(
                appConfig.auth0ClientId,
                appConfig.auth0Domain
            )

            WebAuthProvider.login(auth0)
                .withScheme(appConfig.auth0Scheme)
                .withScope(appConfig.auth0Scopes)
                .withAudience(appConfig.auth0Audience)
                .withConnection(connection.connection)
                .withParameters(mapOf("prompt" to "select_account"))
                .start(activity, object : Callback<Credentials, AuthenticationException> {
                    override fun onSuccess(result: Credentials) {
                        Timber.d("Auth0 login successful")

                        // Exchange for backend tokens
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val exchangeResult = exchangeAuth0Tokens(
                                    accessToken = result.accessToken,
                                    idToken = result.idToken
                                )

                                if (exchangeResult) {
                                    Timber.d("SessionManager: Auth0 token exchange succeeded, emitting Authenticated")
                                    _authState.value = AuthState.Authenticated
                                    // Fetch user profile in background so the UI updates immediately
                                    scope.launch { bootstrap() }
                                    // Sync legacy AuthSession so all production screens update
                                    syncLegacyAuthSession()
                                    continuation.resume(AuthActionResult.Success)
                                } else {
                                    continuation.resume(AuthActionResult.Error("Failed to exchange Auth0 tokens"))
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Auth0 token exchange crashed")
                                continuation.resume(AuthActionResult.Error("Token exchange failed: ${e.message ?: "Unknown error"}"))
                            }
                        }
                    }

                    override fun onFailure(error: AuthenticationException) {
                        if (error.isCanceled) {
                            // Cancelling is not an error (iOS parity) - no-op.
                            continuation.resume(AuthActionResult.Cancelled)
                        } else {
                            Timber.e(error, "Auth0 login failed")
                            continuation.resume(AuthActionResult.Error(error.getDescription()))
                        }
                    }
                })
        } catch (e: Exception) {
            Timber.e(e, "Auth0 SDK initialization or start failed")
            continuation.resume(AuthActionResult.Error("Auth0 login failed: ${e.message ?: "Unknown error"}"))
        }
    }

    /**
     * Email login: POST /v1/auth/login/email
     * Body: { "method":"email", "email":"...", "password":"..." }
     * No Authorization header.
     */
    suspend fun loginWithEmailPassword(email: String, password: String): Result<Unit> {
        Timber.d("SessionManager: loginWithEmailPassword starting")
        val result = authRepository.emailLogin(email, password)
        return when (result) {
            is ApiResult.Success -> {
                if (parseAndStoreEmailToken(result.data)) {
                    Timber.d("SessionManager: token persisted, emitting Authenticated")
                    _authState.value = AuthState.Authenticated
                    // Fetch user profile in background so the UI updates immediately
                    scope.launch { bootstrap() }
                    // Sync legacy AuthSession so all production screens update
                    syncLegacyAuthSession()
                    Timber.d("SessionManager: login success returned to caller")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Invalid response from server"))
                }
            }
            is ApiResult.Failure -> Result.failure(Exception(result.error.message))
        }
    }

    /**
     * Email signup: POST /v1/auth/signup/email
     * Body: { email, firstName, lastName, password, dob:"1990-01-01", gender:"unspecified" }
     * No Authorization header.
     */
    suspend fun registerWithEmailPassword(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<Unit> {
        Timber.d("SessionManager: registerWithEmailPassword starting")
        val result = authRepository.emailSignup(email, firstName, lastName, password)
        return when (result) {
            is ApiResult.Success -> {
                if (parseAndStoreEmailToken(result.data)) {
                    Timber.d("SessionManager: token persisted, emitting Authenticated")
                    _authState.value = AuthState.Authenticated
                    // Fetch user profile in background so the UI updates immediately
                    scope.launch { bootstrap() }
                    // Sync legacy AuthSession so all production screens update
                    syncLegacyAuthSession()
                    Timber.d("SessionManager: registration success returned to caller")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Invalid response from server"))
                }
            }
            is ApiResult.Failure -> Result.failure(Exception(result.error.message))
        }
    }
    
    /**
     * Exchange Auth0 tokens for backend session
     * POST /v1/auth/auth0/callback
     */
    private suspend fun exchangeAuth0Tokens(accessToken: String, idToken: String): Boolean {
        // Store Auth0 tokens
        tokenStorage.auth0AccessToken = accessToken
        tokenStorage.auth0IdToken = idToken

        val result = authRepository.auth0Callback(
            auth0AccessToken = accessToken,
            auth0IdToken = idToken
        )
        return when (result) {
            is ApiResult.Success -> parseAndStoreTokens(result.data)
            is ApiResult.Failure -> {
                Timber.e("Auth0 callback failed: ${result.error.message}")
                false
            }
        }
    }
    
    /**
     * Sign out - revoke server-side session, then clear all local tokens and state.
     *
     * The backend call is fire-and-forget: local cleanup always happens even if
     * the network request fails, matching iOS behavior.
     */
    fun signOut() {
        Timber.d("SessionManager: signOut — starting")

        // Capture tokens before clearing so we can send them to the backend
        val refreshToken = tokenStorage.refreshToken

        // Fire-and-forget: revoke refresh token on the server
        scope.launch {
            try {
                val result = authRepository.logout(refreshToken)
                when (result) {
                    is ApiResult.Success -> Timber.d("SessionManager: backend logout successful")
                    is ApiResult.Failure -> Timber.w("SessionManager: backend logout failed: ${result.error.message}")
                }
            } catch (e: Exception) {
                Timber.w(e, "SessionManager: backend logout request failed (continuing)")
            }
        }

        // Clear local state immediately (don't wait for network)
        try {
            tokenStorage.clearAll()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear tokens during sign out")
        }
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
        // Sync legacy AuthSession so screens observing old system also update
        legacyAuthSession.signOut()
        Timber.d("SessionManager: signOut complete — both auth systems cleared")
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
     * Email login/signup token extraction:
     * token may be in: token, data.token, data.data.token, or jwt (tolerant parsing).
     */
    private fun parseAndStoreEmailToken(responseBody: String): Boolean {
        return try {
            val root = json.parseToJsonElement(responseBody)

            val token = extractStringByPaths(
                root,
                listOf(
                    listOf("token"),
                    listOf("jwt"),
                    listOf("accessToken"),
                    listOf("data", "token"),
                    listOf("data", "jwt"),
                    listOf("data", "accessToken"),
                    listOf("data", "data", "token"),
                    listOf("data", "data", "jwt"),
                    listOf("data", "data", "accessToken")
                )
            )

            val refreshToken = extractStringByPaths(
                root,
                listOf(
                    listOf("refreshToken"),
                    listOf("refresh_token"),
                    listOf("data", "refreshToken"),
                    listOf("data", "refresh_token"),
                    listOf("data", "data", "refreshToken"),
                    listOf("data", "data", "refresh_token")
                )
            )

            if (!tokenStorage.isValidJwtShape(token)) {
                Timber.e("Invalid access token shape from email auth")
                return false
            }

            tokenStorage.storeTokens(token, refreshToken)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse email auth response")
            false
        }
    }

    private fun extractStringByPaths(root: JsonElement, paths: List<List<String>>): String? {
        for (path in paths) {
            val value = root.navigate(path)
            // Avoid `jsonPrimitive` throwing    if this path points to a non-primitive (tolerant parsing).
            val text = (value as? JsonPrimitive)?.content
            if (!text.isNullOrBlank()) return text
        }
        return null
    }

    private fun JsonElement.navigate(path: List<String>): JsonElement? {
        var current: JsonElement? = this
        for (key in path) {
            val obj = (current as? JsonObject) ?: return null
            current = obj[key]
        }
        return current
    }
    
    /**
     * Extract user ID from JWT claims (iOS parity).
     * iOS: JWT.decodePayloadClaims → claims["userId"] ?? claims["user_id"] ?? "me"
     */
    private fun extractUserIdFromJwt(): String? {
        val token = tokenStorage.accessToken ?: return null
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            val payload = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
            val payloadJson = json.parseToJsonElement(String(payload)).jsonObject
            val id = payloadJson["userId"]?.jsonPrimitive?.content
                ?: payloadJson["user_id"]?.jsonPrimitive?.content
                ?: payloadJson["sub"]?.jsonPrimitive?.content
            if (!id.isNullOrBlank()) id else null
        } catch (e: Exception) {
            Timber.w(e, "Failed to extract user ID from JWT")
            null
        }
    }

    /**
     * Parse user from JSON response (tolerant parsing)
     * @param fromCache true when parsing cached data, to prevent infinite recursion
     */
    private fun parseAndSetUser(responseBody: String, fromCache: Boolean = false) {
        try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject

            // Find user data in common locations
            // /account/me returns { data: { profile: { firstName, ... } } }
            val dataObject = obj["data"]?.jsonObject
            val userData = dataObject?.get("profile")?.jsonObject
                ?: dataObject?.get("user")?.jsonObject
                ?: obj["user"]?.jsonObject
                ?: dataObject
                ?: obj

            // iOS parity: user ID from JWT claims first, then response body, then cached
            val responseId = userData["_id"]?.jsonPrimitive?.content
                ?: userData["id"]?.jsonPrimitive?.content
                ?: dataObject?.get("_id")?.jsonPrimitive?.content
                ?: dataObject?.get("id")?.jsonPrimitive?.content
                ?: obj["_id"]?.jsonPrimitive?.content
                ?: obj["id"]?.jsonPrimitive?.content
            val jwtId = extractUserIdFromJwt()
            val resolvedId = responseId?.takeIf { it.isNotBlank() }
                ?: jwtId
                ?: tokenStorage.userId?.takeIf { it.isNotBlank() }
                ?: ""

            val user = User(
                id = resolvedId,
                email = userData["email"]?.jsonPrimitive?.content,
                firstName = userData["firstName"]?.jsonPrimitive?.content
                    ?: userData["first_name"]?.jsonPrimitive?.content,
                lastName = userData["lastName"]?.jsonPrimitive?.content
                    ?: userData["last_name"]?.jsonPrimitive?.content,
                profileImage = userData["profileImage"]?.jsonPrimitive?.content
                    ?: userData["profile_image"]?.jsonPrimitive?.content
                    ?: userData["avatarUrl"]?.jsonPrimitive?.content
                    ?: userData["avatar"]?.jsonPrimitive?.content,
                phone = userData["phone"]?.jsonPrimitive?.content
            )

            if (user.id.isBlank()) {
                Timber.e("SessionManager: User ID is blank after parsing — JWT and response both missing ID")
            }

            _currentUser.value = user
            if (user.id.isNotBlank()) {
                tokenStorage.userId = user.id
            }
            tokenStorage.cachedUserSummary = responseBody

            Timber.d("SessionManager: User set: id=${user.id}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse user")
            if (!fromCache) {
                loadCachedUser()
            }
        }
    }

    /**
     * Update the current user's profile image URL after a successful upload.
     * This updates both the in-memory user and the cached summary.
     */
    fun updateUserProfileImage(imageUrl: String) {
        val current = _currentUser.value ?: return
        _currentUser.value = current.copy(profileImage = imageUrl)
        // Update cached summary so the new avatar persists across app restarts
        tokenStorage.cachedUserSummary?.let { cached ->
            try {
                val element = json.parseToJsonElement(cached)
                val mutableMap = element.jsonObject.toMutableMap()
                val dataObj = mutableMap["data"]?.jsonObject?.toMutableMap()
                if (dataObj != null) {
                    val profileObj = dataObj["profile"]?.jsonObject?.toMutableMap()
                    if (profileObj != null) {
                        profileObj["profileImage"] = JsonPrimitive(imageUrl)
                        profileObj["avatarUrl"] = JsonPrimitive(imageUrl)
                        dataObj["profile"] = JsonObject(profileObj)
                    } else {
                        dataObj["profileImage"] = JsonPrimitive(imageUrl)
                        dataObj["avatarUrl"] = JsonPrimitive(imageUrl)
                    }
                    mutableMap["data"] = JsonObject(dataObj)
                } else {
                    mutableMap["profileImage"] = JsonPrimitive(imageUrl)
                    mutableMap["avatarUrl"] = JsonPrimitive(imageUrl)
                }
                tokenStorage.cachedUserSummary = JsonObject(mutableMap).toString()
            } catch (e: Exception) {
                Timber.w(e, "Failed to update cached user summary with new avatar")
            }
        }
    }

    /**
     * Load cached user from storage
     */
    private fun loadCachedUser() {
        tokenStorage.cachedUserSummary?.let { cached ->
            try {
                parseAndSetUser(cached, fromCache = true)
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
 * Request models live in `AuthRepository`.
 */
