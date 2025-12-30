package com.shourov.apps.pacedream.navigation

import androidx.lifecycle.ViewModel
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    private val authSession: AuthSession
) : ViewModel() {
    val authState: StateFlow<AuthState> = authSession.authState
}

