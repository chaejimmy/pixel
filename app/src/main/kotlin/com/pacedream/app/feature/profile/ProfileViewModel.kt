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

        _uiState.update { it.copy(isHostMode = tokenStorage.isHostMode) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                fetchWishlistCount()
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch wishlist count")
            }
            try {
                fetchBookingsCount()
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch bookings count")
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun fetchWishlistCount() {
        val url = appConfig.buildApiUrl("account", "wishlist")
        when (val result = apiClient.get(url, includeAuth = true)) {
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
                _uiState.update { it.copy(wishlistCount = count) }
            }
            is ApiResult.Failure -> {
                Timber.w("Wishlist count fetch failed: ${result.error}")
            }
        }
    }

    private suspend fun fetchBookingsCount() {
        val url = appConfig.buildApiUrl("bookings", "mine")
        when (val result = apiClient.get(url, includeAuth = true)) {
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
                _uiState.update { it.copy(bookingsCount = count) }
            }
            is ApiResult.Failure -> {
                Timber.w("Bookings count fetch failed: ${result.error}")
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
    val identityStatus: String? = null
)
