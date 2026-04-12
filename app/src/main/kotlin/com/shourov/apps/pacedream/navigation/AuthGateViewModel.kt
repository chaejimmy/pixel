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

    /**
     * Sign out through the new SessionManager, which also cascades to the
     * legacy AuthSession. Must be called from the logout UI callback so that
     * both auth systems drop their cached state in the same pass (previously,
     * only the legacy system was cleared from ProfileTabViewModel, leaving
     * SessionManager.authState stuck on Authenticated until app restart).
     */
    fun signOut() {
        sessionManager.signOut()
    }
}
