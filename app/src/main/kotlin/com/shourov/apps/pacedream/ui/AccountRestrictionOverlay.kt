package com.shourov.apps.pacedream.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

/**
 * Surfaces [AuthState.Restricted] to the user. The backend can flag an
 * account as restricted (SecurityErrorHandler maps 403-style envelopes to
 * ApiError.AccountRestricted and AuthSession flips its state), but until
 * this overlay existed no UI observed that state — restricted users just
 * saw every action fail silently (MOBILE_PARITY_AUDIT Gap 9, P1).
 *
 * Mount once at the top of the app, above mode switches, so it covers
 * both guest and host UIs.
 */
@HiltViewModel
class AccountRestrictionViewModel @Inject constructor(
    legacyAuthSession: AuthSession,
    private val sessionManager: SessionManager,
) : ViewModel() {

    val restriction: StateFlow<AuthState.Restricted?> = legacyAuthSession.authState
        .map { it as? AuthState.Restricted }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun signOut() {
        Timber.i("Signing out from restricted-account dialog")
        sessionManager.signOut()
    }
}

@Composable
fun AccountRestrictionHost(
    viewModel: AccountRestrictionViewModel = hiltViewModel()
) {
    val restriction by viewModel.restriction.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    // Track which restriction instance the user dismissed so a dismissable
    // dialog doesn't immediately re-open, but a NEW restriction event
    // (different message/flags) shows again.
    var dismissedFor by remember { mutableStateOf<AuthState.Restricted?>(null) }

    val current = restriction ?: return
    if (!current.requiresLogout && current == dismissedFor) return

    AlertDialog(
        onDismissRequest = {
            // When the backend demands logout, the dialog is not dismissable —
            // the only ways out are Sign out or Contact support.
            if (!current.requiresLogout) dismissedFor = current
        },
        icon = {
            Icon(
                PaceDreamIcons.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text("Account restricted") },
        text = {
            val verificationHint = if (current.requiresVerification) {
                "\n\nPlease verify your account to restore access."
            } else {
                ""
            }
            Text(current.message + verificationHint)
        },
        confirmButton = {
            if (current.requiresLogout) {
                TextButton(onClick = viewModel::signOut) { Text("Sign out") }
            } else {
                TextButton(onClick = { dismissedFor = current }) { Text("OK") }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    try {
                        uriHandler.openUri("mailto:support@pacedream.com")
                    } catch (_: Exception) {
                        // No email app available.
                    }
                }
            ) {
                Text("Contact support")
            }
        },
    )
}
