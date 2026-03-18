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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pacedream.common.composables.theme.PaceDreamIconSize
import com.pacedream.common.composables.theme.PaceDreamSpacing

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String = "Back",
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onclick: () -> Unit = {}
) {
    IconButton(
        onClick = onclick,
    ) {
        Icon(
            modifier = Modifier
                .padding(PaceDreamSpacing.XXS)
                .size(PaceDreamIconSize.XL)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}
