package com.pacedream.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.AuthSession
import com.pacedream.app.core.auth.AuthState
import com.pacedream.app.core.auth.TokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ProfileViewModel - Profile with Guest/Host mode
 * 
 * iOS Parity:
 * - Guest/Host mode toggle persisted to SharedPreferences
 * - User profile from AuthSession
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authSession: AuthSession,
    private val tokenStorage: TokenStorage
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            // Observe auth state
            authSession.authState.collect { state ->
                _uiState.update {
                    it.copy(isLoggedIn = state == AuthState.Authenticated)
                }
            }
        }
        
        viewModelScope.launch {
            // Observe current user
            authSession.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        userName = user?.displayName ?: "",
                        userEmail = user?.email,
                        userAvatar = user?.profileImage
                    )
                }
            }
        }
        
        // Load host mode preference
        _uiState.update { it.copy(isHostMode = tokenStorage.isHostMode) }
    }
    
    fun toggleHostMode() {
        val newMode = !_uiState.value.isHostMode
        tokenStorage.isHostMode = newMode
        _uiState.update { it.copy(isHostMode = newMode) }
    }
    
    fun logout() {
        authSession.signOut()
    }
}

/**
 * Profile UI State
 */
data class ProfileUiState(
    val isLoggedIn: Boolean = false,
    val isHostMode: Boolean = false,
    val userName: String = "",
    val userEmail: String? = null,
    val userAvatar: String? = null
)


