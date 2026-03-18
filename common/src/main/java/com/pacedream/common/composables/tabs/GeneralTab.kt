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

package com.pacedream.common.composables.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.pacedream.common.composables.VerticalSpacer
import com.pacedream.common.composables.theme.BorderColor
import com.pacedream.common.composables.theme.ExtraSmallPadding
import com.pacedream.common.composables.theme.HeadlineColor
import com.pacedream.common.composables.theme.LargerPadding
import com.pacedream.common.composables.theme.MediumPadding
import com.pacedream.common.composables.theme.NormalPadding
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.SmallPadding
import com.pacedream.common.composables.theme.SmallText

@Composable
fun GeneralTab(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    VerticalSpacer(15)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BorderColor, RoundedCornerShape(LargerPadding))
            .padding(horizontal = ExtraSmallPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        tabs.forEachIndexed { index, tab ->
            Box(
                modifier = Modifier
                    .padding(ExtraSmallPadding)
                    .clip(if (index == selectedTabIndex) CircleShape else MaterialTheme.shapes.small)
                    .background(if (index == selectedTabIndex) PaceDreamColors.Primary else Color.Transparent)
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab,
                    color = if (index == selectedTabIndex) Color.White else HeadlineColor,
                    fontSize = SmallText,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(
                        vertical = ExtraSmallPadding,
                        horizontal = SmallPadding,
                    ),
                )

            }
        }
    }
    VerticalSpacer(5)
}

@Preview
@Composable
fun PreviewCustomTabLayout() {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("ROOM", "PARKING", "EV PARKING", "RESTROOM")

    GeneralTab(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { index -> selectedTabIndex = index },
    )
}