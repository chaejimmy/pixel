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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.theme.*

/**
 * Auth Bottom Sheet - Modal overlay for authentication
 * This keeps the bottom tabs visible while presenting auth as a modal sheet
 * Matches iOS behavior where auth is presented modally
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBottomSheet(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PaceDreamColors.Background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            PaceDreamColors.Gray300,
                            RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }
        }
    ) {
        AuthSheetContent(
            uiState = uiState,
            onEmailChange = viewModel::updateEmail,
            onPasswordChange = viewModel::updatePassword,
            onFirstNameChange = viewModel::updateFirstName,
            onLastNameChange = viewModel::updateLastName,
            onConfirmPasswordChange = viewModel::updateConfirmPassword,
            onLoginClick = { 
                viewModel.login { 
                    onLoginSuccess()
                    onDismiss()
                } 
            },
            onRegisterClick = { 
                viewModel.register { 
                    onLoginSuccess()
                    onDismiss()
                } 
            },
            onSwitchToLogin = viewModel::switchToLogin,
            onSwitchToRegister = viewModel::switchToRegister,
            onAuth0Click = {
                viewModel.loginWithAuth0 {
                    onLoginSuccess()
                    onDismiss()
                }
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun AuthSheetContent(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onSwitchToLogin: () -> Unit,
    onSwitchToRegister: () -> Unit,
    onAuth0Click: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG)
            .padding(bottom = PaceDreamSpacing.XXXL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (uiState.isLoginMode) "Sign In" else "Create Account",
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = PaceDreamColors.TextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        Text(
            text = if (uiState.isLoginMode) 
                "Welcome back! Sign in to access your favorites and bookings." 
            else 
                "Join PaceDream to save your favorite spaces.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        
        // Auth0 / Social Login Button (Primary CTA)
        Button(
            onClick = onAuth0Click,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(
                    text = "Continue with Auth0",
                    style = PaceDreamTypography.Headline
                )
            }
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = PaceDreamColors.Gray200
            )
            Text(
                text = "  or  ",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = PaceDreamColors.Gray200
            )
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        // Email/Password Form
        if (uiState.isLoginMode) {
            // Login Form
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Email, 
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Gray300
                )
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Lock, 
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Gray300
                )
            )
        } else {
            // Register Form
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                OutlinedTextField(
                    value = uiState.firstName,
                    onValueChange = onFirstNameChange,
                    label = { Text("First Name") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Gray300
                    )
                )
                
                OutlinedTextField(
                    value = uiState.lastName,
                    onValueChange = onLastNameChange,
                    label = { Text("Last Name") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PaceDreamColors.Primary,
                        unfocusedBorderColor = PaceDreamColors.Gray300
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Email, 
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Gray300
                )
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Lock, 
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Gray300
                )
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm Password") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Lock, 
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PaceDreamColors.Primary,
                    unfocusedBorderColor = PaceDreamColors.Gray300
                )
            )
        }
        
        // Error Message
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = error,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.Error,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        
        // Submit Button
        OutlinedButton(
            onClick = if (uiState.isLoginMode) onLoginClick else onRegisterClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !uiState.isLoading,
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PaceDreamColors.Primary
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(PaceDreamColors.Primary)
            )
        ) {
            Text(
                text = if (uiState.isLoginMode) "Sign In with Email" else "Create Account",
                style = PaceDreamTypography.Headline
            )
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        // Switch mode
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (uiState.isLoginMode) "Don't have an account? " else "Already have an account? ",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary
            )
            TextButton(
                onClick = if (uiState.isLoginMode) onSwitchToRegister else onSwitchToLogin
            ) {
                Text(
                    text = if (uiState.isLoginMode) "Sign Up" else "Sign In",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


