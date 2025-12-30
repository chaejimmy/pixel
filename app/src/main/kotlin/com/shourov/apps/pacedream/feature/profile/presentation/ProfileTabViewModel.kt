package com.shourov.apps.pacedream.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileTabViewModel @Inject constructor(
    private val authSession: AuthSession
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

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

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                authSession.refreshProfile()
            } finally {
                _isRefreshing.value = false
            }
        }
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

