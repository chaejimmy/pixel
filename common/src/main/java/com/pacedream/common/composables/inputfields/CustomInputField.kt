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

package com.pacedream.common.composables.inputfields

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pacedream.common.R
import com.pacedream.common.composables.texts.TextFieldError

enum class InputType {
    TEXT,
    PASSWORD,
    PHONE_NUMBER,
    MONEY
}

@Composable
fun CustomInputField(
    modifier: Modifier = Modifier,
    @StringRes label: Int? = null,
    customString: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    keyboardActions: KeyboardActions = KeyboardActions(),
    onValueChange: (String) -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null,
    value: String,
    onClear: () -> Unit = {},
    enabled: Boolean = true,
    inputType: InputType = InputType.TEXT,
    showsErrors: Boolean = false,
    errorText: String = "",
    messageText: String = "",
    colors: TextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.outlineVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.outlineVariant,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
        errorTextColor = MaterialTheme.colorScheme.error,
        errorLeadingIconColor = MaterialTheme.colorScheme.error,

        ),
    shape: Shape = MaterialTheme.shapes.medium,
) {
    var showPassword by remember { mutableStateOf(false) }
    val trailingIcon = if (showPassword && inputType == InputType.PASSWORD) {
        PaceDreamIcons.Visibility
    } else {
        PaceDreamIcons.VisibilityOff
    }

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            if (label != null) {
                Text(text = stringResource(id = label))
            } else {
                Text(text = customString)
            }
        },
        leadingIcon = leadingIcon,
        modifier = modifier,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = enabled,
        colors = colors,
        trailingIcon = {
            if (inputType == InputType.PASSWORD) {
                IconButton(
                    onClick = {
                        showPassword = !showPassword
                    },
                    enabled = value.isNotEmpty(),
                    modifier = Modifier.size(
                        ButtonDefaults.IconSize,
                    ),
                ) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = "show password",
                    )
                }
            } else {
                AnimatedVisibility(
                    visible = value.isNotEmpty() && enabled,
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = PaceDreamIcons.Clear,
                            contentDescription = "clear text",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        },
        placeholder = {
            if (inputType == InputType.PHONE_NUMBER) {
                Text(
                    text = stringResource(id = R.string.core_designsystem_phone_number_placehoolder),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                Unit
            }
        },
        visualTransformation = when (inputType) {
            InputType.TEXT, InputType.PHONE_NUMBER, InputType.MONEY -> VisualTransformation.None
            InputType.PASSWORD -> if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            }
        },
        supportingText = {
            TextFieldError(
                textError = errorText,
                isError = showsErrors,
                messageText = messageText,
            )
        },
        isError = showsErrors && enabled,
        shape = shape,
    )
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Prev(){
    CustomInputField(onValueChange = {}, value ="" )
}