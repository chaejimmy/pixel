package com.shourov.apps.pacedream.signin.screens.otp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

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
                text = "Log in",
                style = PaceDreamTypography.LargeTitle,
                color = PaceDreamColors.TextPrimary,
                modifier = Modifier.padding(bottom = PaceDreamSpacing.SM)
            )

            Text(
                text = "Enter your phone number to receive a verification code.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.padding(bottom = PaceDreamSpacing.XXL)
            )

            // Phone Input - iOS 26 style
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = { viewModel.updatePhoneNumber(it) },
                label = { Text("Phone Number", style = PaceDreamTypography.Callout) },
                placeholder = { Text("+12345678901", style = PaceDreamTypography.Body) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                isError = uiState.phoneError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Border,
                ),
            )

            if (uiState.phoneError != null && !uiState.isLoading) {
                Text(
                    text = uiState.phoneError!!,
                    color = PaceDreamColors.Error,
                    style = PaceDreamTypography.Caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = PaceDreamSpacing.XS)
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            // Continue Button - iOS 26 style
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamButtonHeight.MD),
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = PaceDreamColors.OnPrimary
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text("Sending OTP...", style = PaceDreamTypography.Button)
                } else {
                    Text("Continue", style = PaceDreamTypography.Button)
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            // Divider
            HorizontalDivider(color = PaceDreamColors.Border)

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            // Social login buttons - iOS style
            OutlinedButton(
                onClick = onNavigateToEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamButtonHeight.MD),
                shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PaceDreamColors.TextPrimary,
                ),
            ) {
                Text("Continue with Email", style = PaceDreamTypography.Button)
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            OutlinedButton(
                onClick = onNavigateToGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamButtonHeight.MD),
                shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PaceDreamColors.TextPrimary,
                ),
            ) {
                Text("Continue with Google", style = PaceDreamTypography.Button)
            }
        }
    }
}
