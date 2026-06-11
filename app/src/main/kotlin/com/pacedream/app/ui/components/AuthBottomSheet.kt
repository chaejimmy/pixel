package com.pacedream.app.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.pacedream.app.core.auth.Auth0Connection
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons

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
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = uiState.mode,
                transitionSpec = {
                    (fadeIn(tween(200, easing = FastOutSlowInEasing))
                        togetherWith fadeOut(tween(200, easing = FastOutSlowInEasing)))
                        .using(SizeTransform(clip = false))
                },
                label = "auth_flow_sheet_mode"
            ) { mode ->
                when (mode) {
                    AuthFlowMode.Chooser -> ChooserContent(
                        subtitle = subtitle,
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
                                viewModel.loginWithApple(activity)
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
                        isResetLoading = uiState.isResetLoading,
                        error = uiState.error,
                        info = uiState.info,
                        onEmailChange = viewModel::updateEmail,
                        onPasswordChange = viewModel::updatePassword,
                        onContinue = viewModel::loginWithEmail,
                        onForgotPassword = viewModel::sendPasswordReset,
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
    subtitle: String,
    isGoogleLoading: Boolean,
    isAppleLoading: Boolean,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onGoogle: () -> Unit,
    onApple: () -> Unit,
    onNotNow: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Compact heading - avoids repeating what the background already says
        Text(
            text = "Log in or sign up",
            style = PaceDreamTypography.Title1
        )

        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = PaceDreamTypography.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary CTA
        Button(
            onClick = onSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(PaceDreamRadius.LG)
        ) {
            Text(
                "Sign in",
                style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary CTA
        OutlinedButton(
            onClick = onSignUp,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(PaceDreamRadius.LG)
        ) {
            Text(
                "Create account",
                style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "  or  ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google sign-in
        OutlinedButton(
            onClick = onGoogle,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            enabled = !isGoogleLoading && !isAppleLoading
        ) {
            if (isGoogleLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                "Continue with Google",
                style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Apple sign-in (iOS parity: solid black with white content, 16dp radius)
        Button(
            onClick = onApple,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
                disabledContainerColor = Color.Black.copy(alpha = 0.6f),
                disabledContentColor = Color.White.copy(alpha = 0.8f)
            ),
            enabled = !isAppleLoading && !isGoogleLoading
        ) {
            if (isAppleLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                "Continue with Apple",
                style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quiet dismiss
        TextButton(
            onClick = onNotNow,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                "Not now",
                style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun SignInContent(
    email: String,
    password: String,
    isLoading: Boolean,
    isResetLoading: Boolean,
    error: String?,
    info: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onContinue: () -> Unit,
    onForgotPassword: () -> Unit,
    onSwitchToSignUp: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Sign in",
            style = PaceDreamTypography.Title1
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            shape = RoundedCornerShape(PaceDreamRadius.LG)
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(PaceDreamRadius.LG)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isResetLoading) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            TextButton(
                onClick = onForgotPassword,
                enabled = !isLoading && !isResetLoading
            ) {
                Text(
                    "Forgot password?",
                    style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        if (!error.isNullOrBlank()) {
            AuthInlineErrorBanner(message = error, modifier = Modifier.fillMaxWidth())
        }

        if (!info.isNullOrBlank()) {
            AuthInlineInfoBanner(message = info, modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
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
            Text(
                "Continue",
                style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.Bold)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = onSwitchToSignUp) {
                Text(
                    "Create an account",
                    style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.SemiBold)
                )
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Create account",
            style = PaceDreamTypography.Title1
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                label = { Text("First name") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                singleLine = true,
                shape = RoundedCornerShape(PaceDreamRadius.LG)
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                label = { Text("Last name") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                singleLine = true,
                shape = RoundedCornerShape(PaceDreamRadius.LG)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            shape = RoundedCornerShape(PaceDreamRadius.LG)
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(PaceDreamRadius.LG)
        )

        if (!error.isNullOrBlank()) {
            AuthInlineErrorBanner(message = error, modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
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
            Text(
                "Continue",
                style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.Bold)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = onSwitchToSignIn) {
                Text(
                    "Already have an account? Sign in",
                    style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun AuthInlineErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                RoundedCornerShape(PaceDreamRadius.MD),   // 14dp via token
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            PaceDreamIcons.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = message,
            style = PaceDreamTypography.Body,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AuthInlineInfoBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                RoundedCornerShape(PaceDreamRadius.MD),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            PaceDreamIcons.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = message,
            style = PaceDreamTypography.Body,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun PreviewAuthChooserContent() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ChooserContent(
                subtitle = "Save your favorites and book spaces.",
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
                isResetLoading = false,
                error = "Invalid email or password",
                info = null,
                onEmailChange = {},
                onPasswordChange = {},
                onContinue = {},
                onForgotPassword = {},
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
