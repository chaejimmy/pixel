package com.pacedream.app.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * AuthFlowSheet - iOS-parity authentication modal sheet.
 *
 * Modes:
 * - Chooser (default every time presented)
 * - Sign in (email + password)
 * - Sign up (first/last row + email + password)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthFlowSheet(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    viewModel: AuthFlowSheetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Always reset to chooser when presented
    LaunchedEffect(Unit) {
        viewModel.onPresented()
    }

    // Handle login success
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onDismiss()
            onSuccess()
            viewModel.consumeSuccess()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.onNotNow()
            onDismiss()
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header row: title/subtitle + Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                }
                TextButton(
                    onClick = {
                        viewModel.onNotNow()
                        onDismiss()
                    }
                ) {
                    Text("Done")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedContent(
                targetState = uiState.mode,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(140)) togetherWith fadeOut(animationSpec = tween(140)))
                        .using(SizeTransform(clip = false))
                },
                label = "auth_flow_sheet_mode"
            ) { mode ->
                when (mode) {
                    AuthFlowMode.Chooser -> ChooserContent(
                        isGoogleLoading = uiState.isGoogleLoading,
                        isAppleLoading = uiState.isAppleLoading,
                        onSignIn = viewModel::goToSignIn,
                        onSignUp = viewModel::goToSignUp,
                        onGoogle = {
                            (context as? Activity)?.let { activity ->
                                viewModel.loginWithAuth0(activity, Auth0Connection.Google)
                            }
                        },
                        onApple = {
                            (context as? Activity)?.let { activity ->
                                viewModel.loginWithAuth0(activity, Auth0Connection.Apple)
                            }
                        },
                        onNotNow = {
                            viewModel.onNotNow()
                            onDismiss()
                        }
                    )

                    AuthFlowMode.SignIn -> SignInContent(
                        email = uiState.email,
                        password = uiState.password,
                        isLoading = uiState.isEmailLoading,
                        error = uiState.error,
                        onEmailChange = viewModel::updateEmail,
                        onPasswordChange = viewModel::updatePassword,
                        onContinue = viewModel::loginWithEmail,
                        onSwitchToSignUp = viewModel::goToSignUp
                    )

                    AuthFlowMode.SignUp -> SignUpContent(
                        firstName = uiState.firstName,
                        lastName = uiState.lastName,
                        email = uiState.email,
                        password = uiState.password,
                        isLoading = uiState.isEmailLoading,
                        error = uiState.error,
                        onFirstNameChange = viewModel::updateFirstName,
                        onLastNameChange = viewModel::updateLastName,
                        onEmailChange = viewModel::updateEmail,
                        onPasswordChange = viewModel::updatePassword,
                        onContinue = viewModel::signUpWithEmail,
                        onSwitchToSignIn = viewModel::goToSignIn
                    )
                }
            }
        }
    }
}

@Composable
private fun ChooserContent(
    isGoogleLoading: Boolean,
    isAppleLoading: Boolean,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onGoogle: () -> Unit,
    onApple: () -> Unit,
    onNotNow: () -> Unit
) {
    val anySocialLoading = isGoogleLoading || isAppleLoading

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sign in") }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) { Text("Create account") }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "  or  ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedButton(
            onClick = onGoogle,
            modifier = Modifier.fillMaxWidth(),
            enabled = !anySocialLoading
        ) {
            if (isGoogleLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text("Continue with Google")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onApple,
            modifier = Modifier.fillMaxWidth(),
            enabled = !anySocialLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            if (isAppleLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text("Continue with Apple")
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick = onNotNow, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Not now")
        }
    }
}

@Composable
private fun SignInContent(
    email: String,
    password: String,
    isLoading: Boolean,
    error: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onContinue: () -> Unit,
    onSwitchToSignUp: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!error.isNullOrBlank()) {
            AuthInlineErrorBanner(message = error, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text("Continue")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = onSwitchToSignUp) {
                Text("Create an account")
            }
        }
    }
}

@Composable
private fun SignUpContent(
    firstName: String,
    lastName: String,
    email: String,
    password: String,
    isLoading: Boolean,
    error: String?,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onContinue: () -> Unit,
    onSwitchToSignIn: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                label = { Text("First name") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                singleLine = true
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                label = { Text("Last name") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!error.isNullOrBlank()) {
            AuthInlineErrorBanner(message = error, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading &&
                firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text("Continue")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = onSwitchToSignIn) {
                Text("Already have an account? Sign in")
            }
        }
    }
}

@Composable
private fun AuthInlineErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun PreviewAuthChooserContent() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ChooserContent(
                isGoogleLoading = false,
                isAppleLoading = false,
                onSignIn = {},
                onSignUp = {},
                onGoogle = {},
                onApple = {},
                onNotNow = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun PreviewAuthSignInContent() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SignInContent(
                email = "alex@pacedream.com",
                password = "password",
                isLoading = false,
                error = "Invalid email or password",
                onEmailChange = {},
                onPasswordChange = {},
                onContinue = {},
                onSwitchToSignUp = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun PreviewAuthSignUpContent() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SignUpContent(
                firstName = "Alex",
                lastName = "Kim",
                email = "alex@pacedream.com",
                password = "password",
                isLoading = false,
                error = null,
                onFirstNameChange = {},
                onLastNameChange = {},
                onEmailChange = {},
                onPasswordChange = {},
                onContinue = {},
                onSwitchToSignIn = {}
            )
        }
    }
}

