package com.shourov.apps.pacedream.signin.screens.otp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

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
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var resendCountdown by remember { mutableStateOf(60) }
    
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "We sent a 6-digit code to",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = maskPhone(phoneNumber),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Text(
                text = "Enter verification code",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // OTP Input (single field with formatting)
            OutlinedTextField(
                value = uiState.otpCode,
                onValueChange = { viewModel.updateOtpCode(it) },
                label = { Text("Code") },
                placeholder = { Text("123456") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = uiState.otpError != null,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Verify Button
            Button(
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
                enabled = uiState.otpCode.length == 6 && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verifying...")
                } else {
                    Text("Verify")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Didn't receive the code?",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Resend Button
            TextButton(
                onClick = {
                    if (resendCountdown == 0) {
                        viewModel.resendOTP(
                            phoneNumber = phoneNumber,
                            onSuccess = {
                                resendCountdown = 60
                                snackbarHostState.showSnackbar("New OTP sent successfully")
                            },
                            onError = { error ->
                                // Error already shown via snackbar
                            }
                        )
                    }
                },
                enabled = resendCountdown == 0 && !uiState.isLoading
            ) {
                Text(
                    text = if (resendCountdown > 0) {
                        "Resend code in ${resendCountdown}s"
                    } else {
                        "Resend code"
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Change phone number link
            TextButton(onClick = onBackToPhone) {
                Text("← Change phone number")
            }
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
