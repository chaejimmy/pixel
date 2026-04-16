package com.pacedream.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.core.auth.AuthState
import com.pacedream.app.core.auth.TokenStorage
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import timber.log.Timber
import javax.inject.Inject

/**
 * ProfileViewModel - iOS parity profile state
 *
 * Fetches user profile, bookings count, and wishlist count
 * to match iOS ProfileViewModel behavior.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val tokenStorage: TokenStorage,
    private val apiClient: ApiClient,
    private val appConfig: AppConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.authState.collect { state ->
                val isLoggedIn = state == AuthState.Authenticated
                _uiState.update { it.copy(isLoggedIn = isLoggedIn) }
                if (isLoggedIn) {
                    refresh()
                } else {
                    _uiState.update {
                        it.copy(
                            userName = "",
                            userEmail = null,
                            userAvatar = null,
                            bookingsCount = 0,
                            wishlistCount = 0,
                            identityStatus = null
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            sessionManager.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        userName = user?.displayName ?: "",
                        userEmail = user?.email,
                        userAvatar = user?.profileImage
                    )
                }
            }
        }

        // Read isHostMode off the main thread to avoid blocking on
        // EncryptedSharedPreferences initialisation (ANR prevention).
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val hostMode = tokenStorage.isHostMode
            _uiState.update { it.copy(isHostMode = hostMode) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            var wishlistFailed = false
            var bookingsFailed = false
            try {
                if (!fetchWishlistCount()) wishlistFailed = true
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch wishlist count")
                wishlistFailed = true
            }
            try {
                if (!fetchBookingsCount()) bookingsFailed = true
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch bookings count")
                bookingsFailed = true
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    // Distinguish "truly zero" from "couldn't load" so the UI
                    // can show a subtle hint (—) instead of "0".
                    wishlistCountFailed = wishlistFailed,
                    bookingsCountFailed = bookingsFailed
                )
            }
        }
    }

    /** Returns true on success, false on failure. */
    private suspend fun fetchWishlistCount(): Boolean {
        val url = appConfig.buildApiUrl("account", "wishlist")
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                val count = try {
                    val root = Json.parseToJsonElement(result.data).jsonObject
                    val data = root["data"]?.jsonObject
                    val items = data?.get("items")?.jsonArray
                        ?: data?.get("wishlist")?.jsonArray
                        ?: root["items"]?.jsonArray
                        ?: root["wishlist"]?.jsonArray
                    items?.size ?: 0
                } catch (_: Exception) { 0 }
                _uiState.update { it.copy(wishlistCount = count, wishlistCountFailed = false) }
                true
            }
            is ApiResult.Failure -> {
                Timber.w("Wishlist count fetch failed: ${result.error}")
                false
            }
        }
    }

    /** Returns true on success, false on failure. */
    private suspend fun fetchBookingsCount(): Boolean {
        val url = appConfig.buildApiUrl("bookings", "mine")
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                val count = try {
                    val root = Json.parseToJsonElement(result.data).jsonObject
                    val data = root["data"]?.jsonObject
                    val items = data?.get("items")?.jsonArray
                        ?: data?.get("bookings")?.jsonArray
                        ?: root["items"]?.jsonArray
                        ?: root["bookings"]?.jsonArray
                    items?.size ?: 0
                } catch (_: Exception) { 0 }
                _uiState.update { it.copy(bookingsCount = count, bookingsCountFailed = false) }
                true
            }
            is ApiResult.Failure -> {
                Timber.w("Bookings count fetch failed: ${result.error}")
                false
            }
        }
    }

    fun toggleHostMode() {
        val newMode = !_uiState.value.isHostMode
        tokenStorage.isHostMode = newMode
        _uiState.update { it.copy(isHostMode = newMode) }
    }

    fun logout() {
        sessionManager.signOut()
    }
}

/**
 * Profile UI State - iOS parity
 */
data class ProfileUiState(
    val isLoggedIn: Boolean = false,
    val isHostMode: Boolean = false,
    val isLoading: Boolean = false,
    val userName: String = "",
    val userEmail: String? = null,
    val userAvatar: String? = null,
    val bookingsCount: Int = 0,
    val wishlistCount: Int = 0,
    /**
     * True when the bookings count couldn't be fetched. The UI should
     * show a subtle hint ("—") instead of "0" so the user doesn't
     * mistakenly think they have zero bookings.
     */
    val bookingsCountFailed: Boolean = false,
    /**
     * True when the wishlist/favorites count couldn't be fetched.
     */
    val wishlistCountFailed: Boolean = false,
    val identityStatus: String? = null
)
