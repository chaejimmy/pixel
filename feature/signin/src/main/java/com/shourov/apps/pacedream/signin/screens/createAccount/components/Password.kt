/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shourov.apps.pacedream.signin.screens.createAccount.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.inputfields.CustomInputField
import com.pacedream.common.composables.inputfields.InputType.PASSWORD
import com.shourov.apps.pacedream.core.ui.R
import com.shourov.apps.pacedream.core.ui.textFieldStates.LiveConfirmPasswordState
import com.shourov.apps.pacedream.core.ui.textFieldStates.PasswordValidationState
import com.pacedream.common.icon.PaceDreamIcons
import com.pacedream.common.composables.theme.PaceDreamTheme

@Composable
fun Password(
    onPasswordResponse: (String, Boolean) -> Unit = {_, _, ->},
) {

    val newPassword by remember {
        mutableStateOf(PasswordValidationState())
    }

    val confirmPassword by remember {
        mutableStateOf(LiveConfirmPasswordState { newPassword.text })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        val focusManager = LocalFocusManager.current
        CustomInputField(
            label = R.string.core_ui_password_label,
            value = newPassword.text,
            onValueChange = {
                newPassword.text = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    newPassword.onFocusChange(focusState.isFocused)
                    if (!focusState.isFocused && !confirmPassword.isValid) {
                        newPassword.enableShowErrors()
                    }
                },
            inputType = PASSWORD,
            onClear = {
                newPassword.text = ""
            },
            leadingIcon = {
                Icon(
                    imageVector = PaceDreamIcons.Password,
                    contentDescription = PaceDreamIcons.Password.name,
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                    newPassword.enableShowErrors()
                },
            ),
            showsErrors = newPassword.showErrors(),
            errorText = newPassword.getError() ?: "",
        )
        Spacer(modifier = Modifier.height(8.dp))
        CustomInputField(
            label = R.string.core_ui_confirm_password_label,
            value = confirmPassword.text,
            onValueChange = {
                confirmPassword.text = it
                onPasswordResponse(confirmPassword.text, confirmPassword.isValid)
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    confirmPassword.onFocusChange(focusState.isFocused)
                    if (!focusState.isFocused) {
                        confirmPassword.enableShowErrors()
                    }
                },
            inputType = PASSWORD,
            onClear = {
                confirmPassword.text = ""
            },
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
                onDone = {
                    focusManager.clearFocus()
                    confirmPassword.enableShowErrors()
                },
            ),
            enabled = newPassword.isValid,
            showsErrors = confirmPassword.showErrors(),
            errorText = confirmPassword.getError() ?: "",
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
fun PreviewPassword() {
    PaceDreamTheme {
        Password(
            onPasswordResponse = { _, _ -> },
        )
    }
}