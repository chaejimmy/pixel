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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shourov.apps.pacedream.core.ui.ShowDatePicker
import com.shourov.apps.pacedream.feature.signin.R
import com.pacedream.common.composables.buttons.CustomDateTimePickerButton
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

const val simpleDateFormatPattern = "EEE, MMM d yyyy"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDateOfBirth(
    modifier: Modifier = Modifier,
    onDateSelected: (Long) -> Unit = {},
    dateOfBirth: Long? = null,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat(simpleDateFormatPattern, Locale.getDefault())
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val dateString = dateOfBirth?.let { dateFormat.format(it) }
        ?: stringResource(id = R.string.feature_signin_ui_dob_label)

    var selectedDateString by remember{ mutableStateOf(dateString) }
    Column(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Text(
            text = "Date of Birth",
        )
        CustomDateTimePickerButton(
            text = selectedDateString,
            onClick = {
                showDatePicker = true
            },
            modifier = Modifier
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        // todo gender picker
    }

    AnimatedVisibility(
        visible = showDatePicker,
        label = "dob_picker_dialog_visibility",
    ) {
        ShowDatePicker(
            onDateSelected = {
                //dateString = dateFormat.format(it)
                selectedDateString = dateFormat.format(it)
                onDateSelected(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}