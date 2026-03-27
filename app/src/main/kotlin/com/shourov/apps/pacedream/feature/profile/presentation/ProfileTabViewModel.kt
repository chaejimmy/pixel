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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import timber.log.Timber
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

    private val _bookingsCount = MutableStateFlow(0)
    val bookingsCount: StateFlow<Int> = _bookingsCount.asStateFlow()

    private val _wishlistCount = MutableStateFlow(0)
    val wishlistCount: StateFlow<Int> = _wishlistCount.asStateFlow()

    /** Website parity: verification status from /v1/users/verification */
    private val _verificationStatus = MutableStateFlow(VerificationStatus())
    val verificationStatus: StateFlow<VerificationStatus> = _verificationStatus.asStateFlow()

    /** Website parity: member since date from profile */
    private val _memberSince = MutableStateFlow("")
    val memberSince: StateFlow<String> = _memberSince.asStateFlow()

    val uiState: StateFlow<ProfileTabUiState> = combine(
        authSession.authState,
        authSession.currentUser,
        _isRefreshing
    ) { authState, user, refreshing ->
        when (authState) {
            is AuthState.Unauthenticated -> {
                Timber.d("[HostMode] ProfileTab: unauthenticated → Locked")
                ProfileTabUiState.Locked
            }
            else -> {
                if (user == null) {
                    Timber.d("[HostMode] ProfileTab: authenticated but user=null → Loading")
                    ProfileTabUiState.Loading(refreshing = refreshing)
                } else {
                    Timber.d("[HostMode] ProfileTab: authenticated user=${user.id}, isHost=${user.isHost}")
                    ProfileTabUiState.Authenticated(user = user, refreshing = refreshing)
                }
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
            try {
                val bookingsDeferred = async { fetchBookingsCount() }
                val wishlistDeferred = async { fetchWishlistCount() }
                val verificationDeferred = async { fetchVerificationStatus() }
                val memberSinceDeferred = async { fetchMemberSince() }
                _bookingsCount.value = bookingsDeferred.await()
                _wishlistCount.value = wishlistDeferred.await()
                _verificationStatus.value = verificationDeferred.await()
                _memberSince.value = memberSinceDeferred.await()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "loadProfileStats failed")
            }
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

    /** Website parity: fetch verification status from /v1/users/verification */
    private suspend fun fetchVerificationStatus(): VerificationStatus {
        return try {
            val url = appConfig.buildApiUrl("v1/users/verification")
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val root = json.parseToJsonElement(result.data).jsonObject
                    val data = root["data"]?.jsonObject ?: root
                    VerificationStatus(
                        phoneVerified = data["phoneVerified"]?.jsonPrimitive?.booleanOrNull ?: false,
                        identityVerified = data["identityVerified"]?.jsonPrimitive?.booleanOrNull ?: false,
                        verificationSubmitted = data["verification"]?.jsonObject
                            ?.get("status")?.jsonPrimitive?.contentOrNull?.let {
                                it == "pending" || it == "approved"
                            } ?: false
                    )
                }
                else -> VerificationStatus()
            }
        } catch (_: Exception) { VerificationStatus() }
    }

    /** Website parity: extract member since date from profile data */
    private suspend fun fetchMemberSince(): String {
        return try {
            val url = appConfig.buildApiUrl("users/get/profile")
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val root = json.parseToJsonElement(result.data).jsonObject
                    val data = root["data"]?.jsonObject ?: root
                    val createdAt = data["createdAt"]?.jsonPrimitive?.contentOrNull
                        ?: data["created_at"]?.jsonPrimitive?.contentOrNull
                        ?: return@try ""
                    formatMemberSince(createdAt)
                }
                else -> ""
            }
        } catch (_: Exception) { "" }
    }

    private fun formatMemberSince(isoDate: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            val date = sdf.parse(isoDate.take(19)) ?: return ""
            val outFmt = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.US)
            "Member since ${outFmt.format(date)}"
        } catch (_: Exception) { "" }
    }

    fun signOut() {
        authSession.signOut()
    }
}

data class VerificationStatus(
    val phoneVerified: Boolean = false,
    val identityVerified: Boolean = false,
    val verificationSubmitted: Boolean = false
) {
    val hasAnyVerification: Boolean get() = phoneVerified || identityVerified
}

sealed class ProfileTabUiState {
    data class Loading(val refreshing: Boolean) : ProfileTabUiState()
    data object Locked : ProfileTabUiState()
    data class Authenticated(
        val user: com.shourov.apps.pacedream.core.network.auth.User,
        val refreshing: Boolean
    ) : ProfileTabUiState()
}
