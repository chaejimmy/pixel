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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.HorizontalSpacer
import com.pacedream.common.composables.texts.ViewAllText
import com.pacedream.common.composables.theme.BorderColor
import com.pacedream.common.composables.theme.BorderWidth
import com.pacedream.common.composables.theme.ExtraLargePadding
import com.pacedream.common.composables.theme.HeadlineColor
import com.pacedream.common.composables.theme.LargePadding
import com.pacedream.common.composables.theme.MediumPadding
import com.pacedream.common.composables.theme.NormalText
import com.pacedream.common.composables.theme.NotificationsBgColor
import com.shourov.apps.pacedream.feature.home.presentation.R

@Composable
fun RecentSearchCard(
    modifier: Modifier,
    location: String = "",
    address: String = "",
    onViewAllClick: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(LargePadding))
            .background(Color.White, RoundedCornerShape(LargePadding))
            .border(BorderWidth, BorderColor, RoundedCornerShape(LargePadding))
            .clickable {
                onViewAllClick()
            }
            .padding(vertical = ExtraLargePadding, horizontal = LargePadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .clip(CircleShape)
                .background(NotificationsBgColor).padding(MediumPadding),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_location),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(30.dp).clip(
                    CircleShape,
                ),
            )
        }

        HorizontalSpacer(20)

        val annotatedString = buildAnnotatedString {
            withStyle(style = SpanStyle(fontSize = NormalText, fontWeight = FontWeight.Bold)) {
                append(location)
            }
            appendLine()
            append(address)
        }

        Text(
            text = annotatedString,
            color = HeadlineColor,
            fontSize = NormalText,
            modifier = Modifier.weight(1F),
        )

        VerticalDivider(
            modifier = Modifier.height(50.dp),
            color = BorderColor,
            thickness = BorderWidth,
        )
        HorizontalSpacer(8)
        ViewAllText(color = HeadlineColor)

    }
}