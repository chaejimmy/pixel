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

package com.pacedream.common.composables.buttons

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pacedream.common.icon.PaceDreamIcons
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.stronglyDeemphasizedAlpha

@Composable
fun CustomDateTimePickerButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    leadingIcon: ImageVector = PaceDreamIcons.Calendar,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.outlineVariant.copy(
            stronglyDeemphasizedAlpha,
        ),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ),
) {
    Button(
        onClick = onClick,
        colors = colors,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = leadingIcon.name,
        )
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = PaceDreamSpacing.SM, horizontal = PaceDreamSpacing.SM2),
        )
        Icon(
            imageVector = PaceDreamIcons.ArrowForward,
            contentDescription = PaceDreamIcons.ArrowForward.name,
        )
    }
}