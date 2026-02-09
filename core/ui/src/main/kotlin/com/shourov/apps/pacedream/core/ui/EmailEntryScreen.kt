
package com.shourov.apps.pacedream.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.inputfields.CustomInputField
import com.pacedream.common.composables.inputfields.InputType
import com.pacedream.common.composables.buttons.ProcessButton
import com.shourov.apps.pacedream.core.data.UserAuthPath
import com.shourov.apps.pacedream.core.ui.textFieldStates.EmailValidationState
import com.pacedream.common.icon.PaceDreamIcons
import com.pacedream.common.composables.theme.PaceDreamTheme
import com.pacedream.common.composables.theme.slightlyDeemphasizedAlpha

@Composable
fun EmailEntryScreen(
    modifier: Modifier = Modifier,
    onForgetPasswordClicked: () -> Unit,
    onContinueAccountSetup: () -> Unit,
    onNavigateToCreateAccount: () -> Unit,
    onVerifySignIn: () -> Unit,
    onNavigateToAccountSignIn: () -> Unit,
    userAuthPath: UserAuthPath,
    continueButtonText: String = stringResource(id = R.string.core_ui_continue_button),
    isProcessing: Boolean = false,
    emailState: String,
    onEmailChange: (String) -> Unit,
    passwordState: String,
    onPasswordChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var emailStateInternal by remember { mutableStateOf(EmailValidationState()) }
    var password by remember { mutableStateOf(passwordState) }

    Column(
        modifier = modifier.animateContentSize(),
        horizontalAlignment = Alignment.Start,
    ) {
        AccountCreationHeader(
            title = stringResource(id = R.string.core_ui_auth_header_title, "email"),
            userAuthPath = userAuthPath,
            onNavigateToCreateAccount = onNavigateToCreateAccount,
            onNavigateToAccountSignIn = onNavigateToAccountSignIn,
        )
        CustomInputField(
            label = R.string.core_ui_email_label,
            value = emailState,
            onValueChange = {
                onEmailChange(it)
                emailStateInternal.text = it
            },
            modifier = Modifier.fillMaxWidth(),
            inputType = InputType.TEXT,
            onClear = { onEmailChange("") },
            leadingIcon = {
                Icon(
                    imageVector = PaceDreamIcons.Email,
                    contentDescription = PaceDreamIcons.Email.name,
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = when (userAuthPath) {
                    UserAuthPath.NEW -> ImeAction.Next
                    UserAuthPath.EXISTING -> ImeAction.Done
                },
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { focusManager.clearFocus() },
            ),
        )
        // Show password field for both NEW and EXISTING users
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CustomInputField(
                label = R.string.core_ui_password_label,
                value = password,
                onValueChange = {
                    password = it
                    onPasswordChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                inputType = InputType.PASSWORD,
                onClear = { },
                leadingIcon = {
                    Icon(
                        imageVector = PaceDreamIcons.Password,
                        contentDescription = PaceDreamIcons.Password.name,
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
            )
            // Show forgot password link only for existing users signing in
            AnimatedVisibility(visible = userAuthPath == UserAuthPath.EXISTING) {
                Text(
                    text = stringResource(id = R.string.core_ui_forgot_password),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.clickable(onClick = onForgetPasswordClicked),
                )
            }
        }
        ProcessButton(
            onClick = {
                when (userAuthPath) {
                    UserAuthPath.NEW -> onContinueAccountSetup()
                    UserAuthPath.EXISTING -> onVerifySignIn()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isEnabled = emailStateInternal.isValid && password.isNotEmpty(),
            text = continueButtonText,
            isProcessing = isProcessing,
        )
        if (userAuthPath == UserAuthPath.NEW) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.core_ui_privacy_policy),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.outline.copy(slightlyDeemphasizedAlpha),
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NewEmailEntryScreenPreview() {
    PaceDreamTheme {
        EmailEntryScreen(
            onForgetPasswordClicked = {},
            onContinueAccountSetup = {},
            onNavigateToCreateAccount = {},
            onVerifySignIn = {},
            onNavigateToAccountSignIn = {},
            userAuthPath = UserAuthPath.NEW,
            emailState = "",
            onEmailChange = {},
            passwordState = "",
            onPasswordChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExistingEmailEntryScreenPreview() {
    PaceDreamTheme {
        EmailEntryScreen(
            onForgetPasswordClicked = {},
            onContinueAccountSetup = {},
            onNavigateToCreateAccount = {},
            onVerifySignIn = {},
            onNavigateToAccountSignIn = {},
            userAuthPath = UserAuthPath.EXISTING,
            emailState = "",
            onEmailChange = {},
            passwordState = "",
            onPasswordChange = {},
        )
    }
}
