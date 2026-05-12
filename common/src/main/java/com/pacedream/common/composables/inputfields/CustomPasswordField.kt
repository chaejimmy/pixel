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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.pacedream.common.R
import com.pacedream.common.composables.theme.stronglyDeemphasizedAlpha

@Composable
fun CustomPasswordField(
    modifier: Modifier = Modifier,
    value: String = "",
    onValueChange: (String) -> Unit,
    label: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    keyboardActions: KeyboardActions = KeyboardActions(),
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.outlineVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(
            stronglyDeemphasizedAlpha,
        ),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
        errorTextColor = MaterialTheme.colorScheme.error,
        errorLeadingIconColor = MaterialTheme.colorScheme.error,
        ),
    shape: Shape = MaterialTheme.shapes.medium,
    ){
    // showPassword is true local UI state — kept remembered.
    // M-11 fix: render directly from `value` rather than a remembered copy.
    // The previous `var text by remember { mutableStateOf(value) }` captured
    // only the initial value, so any external state change (e.g. clearing
    // the password after a failed login) was lost after the first
    // composition.  The field is now fully controlled by the caller.
    var showPassword by remember { mutableStateOf(false) }
    TextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        leadingIcon = leadingIcon,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = enabled,
        colors = colors,
        shape = shape,
        trailingIcon = {
            IconButton(
                onClick = { showPassword = !showPassword },
                enabled = value.isNotEmpty(),
                modifier = Modifier.size(
                    ButtonDefaults.IconSize,
                ).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                if (showPassword){
                    Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_hide_password), contentDescription = "hide_password")
                }else{
                    Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_show_password), contentDescription = "show_password")
                }
            }
        },
    )
}