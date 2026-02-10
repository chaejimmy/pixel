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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.DashedDivider
import com.pacedream.common.composables.HorizontalSpacer
import com.pacedream.common.composables.VerticalSpacer
import com.pacedream.common.composables.inputfields.GeneralSearchBar
import com.pacedream.common.composables.theme.DashboardHeaderColor
import com.pacedream.common.composables.theme.ExtraLargePadding
import com.pacedream.common.composables.theme.LargerPadding
import com.pacedream.common.composables.theme.LargerText
import com.pacedream.common.composables.theme.MediumPadding
import com.pacedream.common.composables.theme.MediumText
import com.pacedream.common.composables.theme.NotificationsBgColor
import com.pacedream.common.composables.theme.WhiteTextColor
import com.shourov.apps.pacedream.feature.home.presentation.R

@Composable
fun DashboardHeader(
    userName: String = "",
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomEnd = LargerPadding, bottomStart = LargerPadding))
            .paint(
                painterResource(id = R.drawable.bg_dashboard_header),
                contentScale = ContentScale.FillBounds,
            ),
    ) {
        Surface(
            color = DashboardHeaderColor.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth().padding(
                        top = 60.dp,
                        start = ExtraLargePadding,
                        end = ExtraLargePadding,
                        bottom = ExtraLargePadding,
                    ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_dummy_user),
                        contentDescription = null,
                        modifier = Modifier.size(70.dp).clip(
                            CircleShape,
                        ),
                    )
                    val annotatedString = buildAnnotatedString {
                        append(stringResource(R.string.feature_home_good_morning))
                        if (userName.isNotBlank()) {
                            appendLine()
                            withStyle(style = SpanStyle(fontSize = MediumText)) {
                                append(userName)
                            }
                        }
                    }
                    HorizontalSpacer(20)
                    Text(
                        text = annotatedString,
                        color = WhiteTextColor,
                        modifier = Modifier.weight(1F),
                    )

                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(NotificationsBgColor).padding(MediumPadding),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_dashboard_notifications),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp).clip(
                                CircleShape,
                            ),
                        )
                    }
                }

                VerticalSpacer(20)
                DashedDivider()
                VerticalSpacer(20)

                Text(
                    stringResource(R.string.feature_home_find_your_perfect_stay),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = LargerText,
                    color = WhiteTextColor,
                )
                VerticalSpacer(20)
                Row {
                    GeneralSearchBar(
                        modifier = Modifier.weight(1F),
                        trailingIcon = {
                            IconButton(
                                onClick = { },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF8A2BE2), CircleShape),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = "Search",
                                    tint = Color.White,
                                )
                            }
                        },
                    )

                    HorizontalSpacer(16)
                    Image(painterResource(R.drawable.ic_filters), contentDescription = null)
                }
                VerticalSpacer(20)
            }
        }
    }
}
