package com.shourov.apps.pacedream.signin.screens.otp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.buttons.PrimaryTextButton
import com.pacedream.common.composables.buttons.ProcessButton
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.core.ui.otp.OtpInputField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * OTP Verification Screen
 * User enters 6-digit OTP code received via SMS
 */
@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    viewModel: OtpVerificationViewModel = hiltViewModel(),
    onLoginSuccess: (com.shourov.apps.pacedream.model.response.otp.OtpUserData) -> Unit,
    onBackToPhone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var resendCountdown by remember { mutableStateOf(60) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Countdown timer
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000)
            resendCountdown--
        }
    }
    
    // Show error snackbar
    LaunchedEffect(uiState.otpError) {
        uiState.otpError?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PaceDreamColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(PaceDreamSpacing.MD),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "We sent a 6-digit code to",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.padding(bottom = PaceDreamSpacing.SM)
            )

            Text(
                text = maskPhone(phoneNumber),
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                modifier = Modifier.padding(bottom = PaceDreamSpacing.XXL)
            )

            Text(
                text = "Enter verification code",
                style = PaceDreamTypography.Subheadline,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.padding(bottom = PaceDreamSpacing.MD)
            )

            // OTP Input (6 individual digit boxes with auto-advance)
            OtpInputField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                otpText = uiState.otpCode,
                shouldShowCursor = true,
                shouldCursorBlink = true,
                onOtpModified = { text, isComplete ->
                    viewModel.updateOtpCode(text)
                    if (isComplete) {
                        viewModel.verifyAndLogin(
                            phoneNumber = phoneNumber,
                            onSuccess = { userData -> onLoginSuccess(userData) },
                            onError = { /* shown via snackbar */ }
                        )
                    }
                },
            )

            // Auto-focus the OTP input
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            // Verify Button — iOS primary action pattern
            ProcessButton(
                onClick = {
                    viewModel.verifyAndLogin(
                        phoneNumber = phoneNumber,
                        onSuccess = { userData ->
                            onLoginSuccess(userData)
                        },
                        onError = { error ->
                            // Error already shown via snackbar
                        }
                    )
                },
                isEnabled = uiState.otpCode.length == 6,
                isProcessing = uiState.isLoading,
                text = if (uiState.isLoading) "Verifying..." else "Verify",
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            Text(
                text = "Didn't receive the code?",
                style = PaceDreamTypography.Subheadline,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.padding(bottom = PaceDreamSpacing.SM)
            )

            // Resend Button — disabled during cooldown and while loading
            PrimaryTextButton(
                text = if (resendCountdown > 0) {
                    "Resend code in ${resendCountdown}s"
                } else {
                    "Resend code"
                },
                onClick = {
                    if (resendCountdown == 0 && !uiState.isLoading) {
                        viewModel.resendOTP(
                            phoneNumber = phoneNumber,
                            onSuccess = { cooldownSeconds ->
                                resendCountdown = cooldownSeconds
                                scope.launch {
                                    snackbarHostState.showSnackbar("New OTP sent successfully")
                                }
                            },
                            onError = { error ->
                                // Error already shown via snackbar
                            }
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Change phone number link
            PrimaryTextButton(
                text = "← Change phone number",
                onClick = onBackToPhone,
            )
        }
    }
}

/**
 * Mask phone number for display
 * Example: +12345678901 -> +1******8901
 */
private fun maskPhone(phone: String): String {
    if (phone.length <= 4) return "****"
    val prefix = phone.take(2)
    val suffix = phone.takeLast(4)
    return "$prefix****$suffix"
}
