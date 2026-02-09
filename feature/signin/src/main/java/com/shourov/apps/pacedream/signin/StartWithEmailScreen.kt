package com.shourov.apps.pacedream.signin

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shourov.apps.pacedream.core.data.UserAuthPath
import com.shourov.apps.pacedream.core.ui.EmailEntryScreen
import com.shourov.apps.pacedream.core.ui.R
import com.shourov.apps.pacedream.core.ui.SignInButton
import com.pacedream.common.icon.PaceDreamIcons


@Composable
fun StartWithEmailScreen(
    onContinueWithGoogleClicked: () -> Unit,
    modifier: Modifier = Modifier,
    userAuthPath: UserAuthPath,
    onNavigateToPhoneEntry: (UserAuthPath) -> Unit,
    onForgotPasswordClicked: () -> Unit,
    onContinueAccountSetup: () -> Unit,
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToAccountSignIn: () -> Unit,
) {
    val viewModel: EmailSignInViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(state = rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {

            EmailEntryScreen(
                onForgetPasswordClicked = onForgotPasswordClicked,
                onContinueAccountSetup = {
                    // NEW user: register with email and password via backend API
                    viewModel.registerWithEmail(
                        email = email,
                        password = password,
                        onSuccess = { onContinueAccountSetup() },
                        onError = { error ->
                            errorMessage = error
                            showDialog = true
                        }
                    )
                },
                onNavigateToCreateAccount = onNavigateToCreateAccount,
                onVerifySignIn = {
                    // EXISTING user: login with email and password via backend API
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        viewModel.loginWithEmail(
                            email = email,
                            password = password,
                            onSuccess = { onContinueAccountSetup() },
                            onError = { error ->
                                errorMessage = error
                                showDialog = true
                            }
                        )
                    } else {
                        errorMessage = "Email and password cannot be empty"
                        showDialog = true
                    }
                },
                onNavigateToAccountSignIn = onNavigateToAccountSignIn,
                userAuthPath = userAuthPath,
                continueButtonText = stringResource(id = R.string.core_ui_continue_button),
                isProcessing = uiState.isLoading,
                modifier = modifier,
                emailState = email,
                onEmailChange = { email = it },
                passwordState = password,
                onPasswordChange = { password = it },
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                SignInButton(
                    icon = PaceDreamIcons.PhoneAndroid,
                    text = R.string.core_ui_continue_with_phone,
                    onClick = { onNavigateToPhoneEntry(userAuthPath) },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = false,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SignInButton(
                    logo = R.drawable.google_logo,
                    text = R.string.core_ui_continue_with_google,
                    onClick = {
                        viewModel.loginWithGoogle(
                            activity = activity,
                            onSuccess = { onContinueAccountSetup() },
                            onError = { error ->
                                errorMessage = error
                                showDialog = true
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = uiState.isGoogleLoading,
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "Sign-in Error") },
            text = { Text(text = errorMessage) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = "OK")
                }
            }
        )
    }
}
