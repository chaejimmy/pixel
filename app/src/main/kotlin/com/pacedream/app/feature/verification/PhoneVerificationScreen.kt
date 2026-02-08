package com.pacedream.app.feature.verification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PhoneVerificationContent(
    phoneNumber: String,
    onVerified: () -> Unit,
    viewModel: PhoneVerificationViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(VerificationStep.INPUT) }
    var otpCode by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            onVerified()
        }
    }
    
    LaunchedEffect(phoneNumber) {
        if (phoneNumber.isNotEmpty()) {
            viewModel.updatePhoneNumber(phoneNumber)
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (step) {
            VerificationStep.INPUT -> {
                PhoneInputView(
                    phoneNumber = uiState.phoneNumber.ifEmpty { phoneNumber },
                    onPhoneChanged = { viewModel.updatePhoneNumber(it) },
                    onSendCode = {
                        viewModel.sendCode()
                        step = VerificationStep.VERIFY
                    },
                    isLoading = uiState.isLoading,
                    cooldown = uiState.cooldown,
                    canSend = uiState.canSendCode
                )
            }
            VerificationStep.VERIFY -> {
                OTPInputView(
                    phoneNumber = uiState.phoneNumber.ifEmpty { phoneNumber },
                    otpCode = otpCode,
                    onOtpChanged = { 
                        otpCode = it
                        viewModel.updateOtpCode(it)
                    },
                    onVerify = {
                        viewModel.verifyCode()
                    },
                    onResend = {
                        viewModel.sendCode()
                    },
                    onChangePhone = {
                        step = VerificationStep.INPUT
                        otpCode = ""
                    },
                    isLoading = uiState.isLoading,
                    cooldown = uiState.cooldown
                )
            }
        }
        
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

enum class VerificationStep {
    INPUT, VERIFY
}

@Composable
fun PhoneInputView(
    phoneNumber: String,
    onPhoneChanged: (String) -> Unit,
    onSendCode: () -> Unit,
    isLoading: Boolean,
    cooldown: Int,
    canSend: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Phone Number",
            style = MaterialTheme.typography.titleMedium
        )
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone number with country code") },
            placeholder = { Text("+1234567890") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = null)
            },
            singleLine = true
        )
        
        Text(
            text = "Enter your phone number with country code (E.164 format)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onSendCode,
            enabled = canSend && !isLoading && cooldown == 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = if (cooldown > 0) "Wait ${cooldown}s" else "Send Verification Code"
                )
            }
        }
    }
}

@Composable
fun OTPInputView(
    phoneNumber: String,
    otpCode: String,
    onOtpChanged: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangePhone: () -> Unit,
    isLoading: Boolean,
    cooldown: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Enter Verification Code",
            style = MaterialTheme.typography.titleMedium
        )
        
        Text(
            text = "We sent a 6-digit code to $phoneNumber",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // OTP Input (6 digits)
        OutlinedTextField(
            value = otpCode,
            onValueChange = { newValue ->
                // Only allow digits, max 6
                val digits = newValue.filter { it.isDigit() }.take(6)
                onOtpChanged(digits)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Verification code") },
            placeholder = { Text("000000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                letterSpacing = 8.sp
            ),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null)
            }
        )
        
        Button(
            onClick = onVerify,
            enabled = otpCode.length == 6 && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Verify Code")
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onChangePhone) {
                Text("Change phone number")
            }
            
            TextButton(
                onClick = onResend,
                enabled = cooldown == 0 && !isLoading
            ) {
                Text(if (cooldown > 0) "Resend in ${cooldown}s" else "Resend Code")
            }
        }
    }
}
