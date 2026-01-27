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

package com.pacedream.common.composables.texts

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.pacedream.common.composables.theme.HeadlineColor
import com.pacedream.common.composables.theme.NormalText
import com.pacedream.common.composables.theme.ViewAllColor

@Composable
fun TitleText(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

@Composable
fun MediumTitleText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = HeadlineColor,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = color,
        ),
    )
}

@Composable
fun SmallTitleText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = HeadlineColor,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = color,
        ),
    )
}

@Composable
fun NormalTitleText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = HeadlineColor,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = color,
        ),
    )
}

@Composable
fun ViewAllText(
    modifier: Modifier = Modifier,
    color: Color = ViewAllColor,
) {
    Text(
        modifier = modifier,
        text = "View All",
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Medium,
            color = color,
        ),
    )
}