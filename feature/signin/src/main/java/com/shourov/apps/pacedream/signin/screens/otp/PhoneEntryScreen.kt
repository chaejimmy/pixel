package com.shourov.apps.pacedream.signin.screens.otp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Phone Entry Screen
 * Collects phone number and sends OTP
 */
@Composable
fun PhoneEntryScreen(
    viewModel: PhoneEntryViewModel = hiltViewModel(),
    onOTPSent: (String) -> Unit,
    onNavigateToEmail: () -> Unit = {},
    onNavigateToGoogle: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error snackbar
    LaunchedEffect(uiState.phoneError) {
        uiState.phoneError?.let { error ->
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
                text = "Log in",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Phone Input
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = { viewModel.updatePhoneNumber(it) },
                label = { Text("Phone Number") },
                placeholder = { Text("+12345678901") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                isError = uiState.phoneError != null,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (uiState.phoneError != null && !uiState.isLoading) {
                Text(
                    text = uiState.phoneError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Continue Button
            Button(
                onClick = {
                    viewModel.sendOTP(
                        onSuccess = { phone ->
                            onOTPSent(phone)
                        },
                        onError = { error ->
                            // Error already shown via snackbar
                        }
                    )
                },
                enabled = uiState.isValidPhone && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sending OTP...")
                } else {
                    Text("Continue")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Or",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // More options buttons
            OutlinedButton(
                onClick = onNavigateToEmail,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue with Email")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onNavigateToGoogle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue with Google")
            }
        }
    }
}
