package com.shourov.apps.pacedream.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import javax.inject.Inject

@HiltViewModel
class ProfileTabViewModel @Inject constructor(
    private val authSession: AuthSession,
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** iOS parity: bookings count displayed on profile card */
    private val _bookingsCount = MutableStateFlow(0)
    val bookingsCount: StateFlow<Int> = _bookingsCount.asStateFlow()

    /** iOS parity: wishlist count displayed on profile card */
    private val _wishlistCount = MutableStateFlow(0)
    val wishlistCount: StateFlow<Int> = _wishlistCount.asStateFlow()

    val uiState: StateFlow<ProfileTabUiState> = combine(
        authSession.authState,
        authSession.currentUser,
        _isRefreshing
    ) { authState, user, refreshing ->
        when (authState) {
            is AuthState.Unauthenticated -> ProfileTabUiState.Locked
            else -> {
                if (user == null) ProfileTabUiState.Loading(refreshing = refreshing)
                else ProfileTabUiState.Authenticated(user = user, refreshing = refreshing)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileTabUiState.Loading(refreshing = false))

    init {
        loadProfileStats()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                authSession.refreshProfile()
                loadProfileStats()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun loadProfileStats() {
        viewModelScope.launch {
            val bookingsDeferred = async { fetchBookingsCount() }
            val wishlistDeferred = async { fetchWishlistCount() }
            _bookingsCount.value = bookingsDeferred.await()
            _wishlistCount.value = wishlistDeferred.await()
        }
    }

    private suspend fun fetchBookingsCount(): Int {
        return try {
            val url = appConfig.buildApiUrlWithQuery(
                "bookings",
                queryParams = mapOf("limit" to "1", "page" to "1")
            )
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val root = json.parseToJsonElement(result.data).jsonObject
                    root["total"]?.jsonPrimitive?.intOrNull
                        ?: root["data"]?.let {
                            if (it is kotlinx.serialization.json.JsonArray) it.jsonArray.size else 0
                        }
                        ?: 0
                }
                else -> 0
            }
        } catch (_: Exception) { 0 }
    }

    private suspend fun fetchWishlistCount(): Int {
        return try {
            val url = appConfig.buildApiUrl("wishlists")
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val root = json.parseToJsonElement(result.data).jsonObject
                    root["total"]?.jsonPrimitive?.intOrNull
                        ?: root["data"]?.let {
                            if (it is kotlinx.serialization.json.JsonArray) it.jsonArray.size else 0
                        }
                        ?: 0
                }
                else -> 0
            }
        } catch (_: Exception) { 0 }
    }

    fun signOut() {
        authSession.signOut()
    }
}

sealed class ProfileTabUiState {
    data class Loading(val refreshing: Boolean) : ProfileTabUiState()
    data object Locked : ProfileTabUiState()
    data class Authenticated(
        val user: com.shourov.apps.pacedream.core.network.auth.User,
        val refreshing: Boolean
    ) : ProfileTabUiState()
}

