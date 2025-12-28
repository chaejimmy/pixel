package com.shourov.apps.pacedream.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pacedream.common.composables.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PaceDreamColors.Primary.copy(alpha = 0.1f),
                            PaceDreamColors.Background
                        )
                    )
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamSpacing.LG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo and Welcome
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PaceDream",
                    style = PaceDreamTypography.LargeTitle,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                
                Text(
                    text = "Find your perfect space",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))
            
            // Auth Form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaceDreamSpacing.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(PaceDreamSpacing.LG)
                ) {
                    if (uiState.isLoginMode) {
                        LoginForm(
                            email = uiState.email,
                            password = uiState.password,
                            onEmailChange = viewModel::updateEmail,
                            onPasswordChange = viewModel::updatePassword,
                            onLoginClick = { viewModel.login { onLoginSuccess() } },
                            onSwitchToRegister = viewModel::switchToRegister,
                            isLoading = uiState.isLoading,
                            error = uiState.error
                        )
                    } else {
                        RegisterForm(
                            firstName = uiState.firstName,
                            lastName = uiState.lastName,
                            email = uiState.email,
                            password = uiState.password,
                            confirmPassword = uiState.confirmPassword,
                            onFirstNameChange = viewModel::updateFirstName,
                            onLastNameChange = viewModel::updateLastName,
                            onEmailChange = viewModel::updateEmail,
                            onPasswordChange = viewModel::updatePassword,
                            onConfirmPasswordChange = viewModel::updateConfirmPassword,
                            onRegisterClick = { viewModel.register { onLoginSuccess() } },
                            onSwitchToLogin = viewModel::switchToLogin,
                            isLoading = uiState.isLoading,
                            error = uiState.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginForm(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onSwitchToRegister: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column {
        Text(
            text = "Welcome Back",
            style = PaceDreamTypography.Title1,
            color = PaceDreamColors.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        Text(
            text = "Sign in to your account",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        
        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        
        // Error Message
        error?.let {
            Text(
                text = it,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.Error
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        }
        
        // Login Button
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White
                )
            } else {
                Text("Sign In")
            }
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        // Switch to Register
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary
            )
            TextButton(onClick = onSwitchToRegister) {
                Text(
                    text = "Sign Up",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.Primary
                )
            }
        }
    }
}

@Composable
fun RegisterForm(
    firstName: String,
    lastName: String,
    email: String,
    password: String,
    confirmPassword: String,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onSwitchToLogin: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column {
        Text(
            text = "Create Account",
            style = PaceDreamTypography.Title1,
            color = PaceDreamColors.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        Text(
            text = "Join PaceDream today",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        
        // Name Fields
        Row(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                label = { Text("First Name") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )
            
            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                label = { Text("Last Name") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        // Confirm Password Field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        
        // Error Message
        error?.let {
            Text(
                text = it,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.Error
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        }
        
        // Register Button
        Button(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White
                )
            } else {
                Text("Sign Up")
            }
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        // Switch to Login
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary
            )
            TextButton(onClick = onSwitchToLogin) {
                Text(
                    text = "Sign In",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.Primary
                )
            }
        }
    }
}
