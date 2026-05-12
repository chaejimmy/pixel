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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.shourov.apps.pacedream.designsystem.OnBrandSurface
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.VerticalSpacer
import com.pacedream.common.composables.shimmerEffect
import com.pacedream.common.composables.theme.LargePadding
import com.pacedream.common.composables.theme.MediumPadding
import com.pacedream.common.composables.theme.NormalPadding

@Composable
fun DealCardShimmer() {
    Column(
        modifier = Modifier
            .width(280.dp)
            .padding(NormalPadding)
            .background(OnBrandSurface, RoundedCornerShape(LargePadding))
            .clip(RoundedCornerShape(LargePadding))
            .shimmerEffect(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(LargePadding))
                .shimmerEffect(),
        )
        Column(modifier = Modifier.padding(MediumPadding)) {
            VerticalSpacer(6)
            Box(
                modifier = Modifier.fillMaxWidth().height(20.dp).shimmerEffect(),
            )
            VerticalSpacer(2)
            Box(
                modifier = Modifier.fillMaxWidth().height(20.dp).shimmerEffect(),
            )
            VerticalSpacer(8)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.width(70.dp).height(20.dp).shimmerEffect(),
                )
            }
            VerticalSpacer(12)
            Box(
                modifier = Modifier.clip(RoundedCornerShape(LargePadding))
                    .fillMaxWidth()
                    .height(40.dp).shimmerEffect(),
            )

        }

    }
}