package com.shourov.apps.pacedream.signin.screens.forgotPassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.buttons.RoundIconButton
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onResetSent: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar(uiState.successMessage)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    RoundIconButton(
                        icon = PaceDreamIcons.ArrowBack,
                        onclick = onBackClick,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                ),
            )
        },
        containerColor = PaceDreamColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(PaceDreamSpacing.MD),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Reset Password",
                style = PaceDreamTypography.LargeTitle,
                color = PaceDreamColors.TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PaceDreamSpacing.SM),
            )

            Text(
                text = "Enter your email address and we'll send you a link to reset your password.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PaceDreamSpacing.XXL),
            )

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.updateEmail(it) },
                label = { Text("Email", style = PaceDreamTypography.Callout) },
                placeholder = { Text("you@example.com", style = PaceDreamTypography.Body) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = uiState.error != null,
                enabled = !uiState.isSuccess,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Border,
                ),
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            if (uiState.isSuccess) {
                Text(
                    text = uiState.successMessage,
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.Success,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

                Button(
                    onClick = onBackClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PaceDreamButtonHeight.MD),
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                    shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text("Back to Sign In", style = PaceDreamTypography.Button)
                }
            } else {
                Button(
                    onClick = { viewModel.sendResetLink() },
                    enabled = uiState.email.isNotBlank() && !uiState.isLoading,
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
                            color = PaceDreamColors.OnPrimary,
                        )
                    } else {
                        Text("Send Reset Link", style = PaceDreamTypography.Button)
                    }
                }
            }
        }
    }
}
