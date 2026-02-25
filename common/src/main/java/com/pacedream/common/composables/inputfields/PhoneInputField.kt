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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pacedream.common.R
import com.pacedream.common.composables.theme.stronglyDeemphasizedAlpha
import com.rejowan.ccpc.Country
import com.rejowan.ccpc.Country.UnitedStates
import com.rejowan.ccpc.CountryCodePicker
import com.rejowan.ccpc.PickerCustomization
import com.rejowan.ccpc.ViewCustomization

@Composable
fun PhoneInputField(
    modifier: Modifier = Modifier,
    onPhoneNumberChanged: (String) -> Unit,
    phoneNumber: String,
    onClear: () -> Unit = {},
    onDone: () -> Unit = {},
    showsErrors: Boolean = false,
    focusManager: FocusManager = LocalFocusManager.current,
    onCountrySelected: (Country) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            var country by remember {
                mutableStateOf(UnitedStates)
            }
            val background by animateColorAsState(
                targetValue = if (showsErrors) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(
                        stronglyDeemphasizedAlpha,
                    )
                },
                label = "phone_number_border_color",
            )

            CountryCodePicker(
                modifier = Modifier
                    .height(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            stronglyDeemphasizedAlpha,
                        ),
                        shape = MaterialTheme.shapes.medium,
                    ),
                selectedCountry = country,
                onCountrySelected = {
                    country = it
                    onCountrySelected(it)
                },
                viewCustomization = ViewCustomization(
                    showFlag = true,
                    showCountryIso = false,
                    showCountryName = false,
                    showCountryCode = true,
                    clipToFull = false,
                ),
                pickerCustomization = PickerCustomization(
                    showFlag = false,
                ),
                showSheet = false,
            )

            PhoneNumberInput(
                onPhoneNumberChanged = onPhoneNumberChanged,
                phoneNumber = phoneNumber,
                onClear = onClear,
                onDone = {
                    focusManager.clearFocus()
                    onDone()
                },
                hasError = showsErrors,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(
                        color = background,
                        shape = MaterialTheme.shapes.medium,
                    ),
                focusManager = focusManager,
            )
        }
        AnimatedVisibility(
            visible = showsErrors,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.padding(end = 8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.core_ui_phone_number_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun PhoneNumberInput(
    modifier: Modifier = Modifier,
    onPhoneNumberChanged: (String) -> Unit,
    phoneNumber: String,
    onClear: () -> Unit = {},
    onDone: () -> Unit = {},
    focusManager: FocusManager = LocalFocusManager.current,
    hasError: Boolean = false,
) {
    var hasFocus by remember {
        mutableStateOf(false)
    }
    val color by animateColorAsState(
        targetValue = if (hasError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "phone_number_text_color",
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChanged,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onDone()
                },
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = color,
            ),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        hasFocus = true
                    }
                },
            decorationBox = { innerTextField ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    when (phoneNumber.isEmpty() && !hasFocus) {
                        true -> Text(
                            text = stringResource(id = R.string.core_ui_phone_number_placehoolder),
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = stronglyDeemphasizedAlpha,
                            ),
                        )

                        false -> innerTextField()
                    }
                }
            },
        )
        Spacer(modifier = Modifier.width(8.dp))
        AnimatedVisibility(
            visible = phoneNumber.isNotEmpty(),
            enter = scaleIn(),
            exit = scaleOut(),
        ) {
            IconButton(
                onClick = onClear,
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Cancel,
                    contentDescription = "clear text",
                    modifier = Modifier.size(24.dp),
                    tint = color,
                )
            }
        }
    }
}