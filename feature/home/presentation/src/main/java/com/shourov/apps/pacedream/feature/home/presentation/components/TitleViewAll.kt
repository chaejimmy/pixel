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

package com.shourov.apps.pacedream.feature.home.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pacedream.common.composables.VerticalSpacer
import com.pacedream.common.composables.texts.MediumTitleText
import com.pacedream.common.composables.texts.ViewAllText

@Composable
fun TitleViewAll(
    headline: String,
    isViewAll: Boolean,
    bottomVerticalSpacer: Int = 0,
    onViewAllClick: () -> Unit = {},
) {
    VerticalSpacer(20)
    Row(verticalAlignment = Alignment.CenterVertically) {
        MediumTitleText(modifier = Modifier.weight(1F), text = headline)
        if (isViewAll) ViewAllText(onClick = onViewAllClick)
    }
    VerticalSpacer(bottomVerticalSpacer)
}