package com.shourov.apps.pacedream.navigation

import androidx.lifecycle.ViewModel
import com.pacedream.app.core.auth.AuthState
import com.pacedream.app.core.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    val authState: StateFlow<AuthState> = sessionManager.authState
}
