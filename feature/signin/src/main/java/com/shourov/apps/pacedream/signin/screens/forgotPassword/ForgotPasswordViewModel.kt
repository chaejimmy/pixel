package com.shourov.apps.pacedream.signin.screens.forgotPassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authSession: AuthSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    /** Track attempts for abuse detection. */
    private var attemptCount = 0
    private var firstAttemptTimeMs = 0L

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun sendResetLink() {
        // Block during cooldown
        if (_uiState.value.cooldownSeconds > 0) return
        if (_uiState.value.isLoading) return

        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your email address")
            return
        }
        val emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex()
        if (!emailRegex.matches(email)) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid email address")
            return
        }

        // Abuse detection: 5+ attempts in 5 minutes
        val now = System.currentTimeMillis()
        if (firstAttemptTimeMs == 0L) firstAttemptTimeMs = now
        attemptCount++
        val elapsedMs = now - firstAttemptTimeMs
        if (attemptCount >= 5 && elapsedMs < 300_000) {
            Timber.w("[SECURITY] Forgot password abuse detected: $attemptCount attempts in ${elapsedMs / 1000}s for ${email.take(3)}***")
            startCooldown(300) // 5 minute penalty
            _uiState.value = _uiState.value.copy(
                error = "Too many attempts. Please try again in a few minutes."
            )
            return
        }
        // Reset window after 5 minutes
        if (elapsedMs > 300_000) {
            attemptCount = 1
            firstAttemptTimeMs = now
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = authSession.forgotPassword(email)
            result.fold(
                onSuccess = { _ ->
                    startCooldown(DEFAULT_COOLDOWN)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        // Uniform message: do not reveal whether the email exists
                        successMessage = UNIFORM_MESSAGE
                    )
                },
                onFailure = { error ->
                    // Start cooldown even on failure to prevent hammering
                    startCooldown(DEFAULT_COOLDOWN)

                    val message = when {
                        error.message?.contains("429", ignoreCase = true) == true ||
                        error.message?.contains("rate", ignoreCase = true) == true -> {
                            startCooldown(120) // Longer cooldown for rate limits
                            "Too many requests. Please try again later."
                        }
                        error.message?.contains("network", ignoreCase = true) == true ||
                        error.message?.contains("connection", ignoreCase = true) == true ->
                            "Something went wrong. Please check your connection and try again."
                        else -> {
                            // Return uniform message to prevent account enumeration
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isSuccess = true,
                                successMessage = UNIFORM_MESSAGE
                            )
                            return@launch
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = message
                    )
                }
            )
        }
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        _uiState.value = _uiState.value.copy(cooldownSeconds = seconds)
        cooldownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1_000)
                remaining--
                _uiState.value = _uiState.value.copy(cooldownSeconds = remaining)
            }
        }
    }

    companion object {
        const val DEFAULT_COOLDOWN = 60
        const val UNIFORM_MESSAGE =
            "If an account with this email exists, you'll receive a password reset link shortly."
    }
}

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val successMessage: String = "",
    val error: String? = null,
    /** Remaining cooldown seconds. 0 = no cooldown active. */
    val cooldownSeconds: Int = 0,
)
