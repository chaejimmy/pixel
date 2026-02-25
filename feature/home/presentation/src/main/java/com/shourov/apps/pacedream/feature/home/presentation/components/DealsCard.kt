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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pacedream.common.composables.VerticalSpacer
import com.pacedream.common.composables.buttons.ProcessButton
import com.pacedream.common.composables.texts.MediumTitleText
import com.pacedream.common.composables.texts.SmallTitleText
import com.pacedream.common.composables.theme.HeadlineColor
import com.pacedream.common.composables.theme.LargePadding
import com.pacedream.common.composables.theme.MediumPadding
import com.pacedream.common.composables.theme.NormalPadding
import com.pacedream.common.composables.theme.ViewAllColor
import com.pacedream.common.util.toCurrencySymbol
import com.shourov.apps.pacedream.feature.home.domain.models.RentedGearModel
import com.shourov.apps.pacedream.feature.home.domain.models.rooms.RoomModel
import com.shourov.apps.pacedream.feature.home.presentation.R

@Composable
fun DealsCard(
    roomModel: RoomModel,
    onClick: () -> Unit,
) {
    roomModel.apply {
        Column(
            modifier = Modifier
                .width(280.dp)
                .padding(NormalPadding)
                .background(Color.White, RoundedCornerShape(LargePadding))
                .clip(RoundedCornerShape(LargePadding))
                .clickable { onClick() },
        ) {
            Box {
                AsyncImage(
                    model = gallery.thumbnail,
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(LargePadding)),
                )
                // Discount badge (for last-minute deals)
                val priceList = roomModel.price
                val firstPrice = priceList?.firstOrNull()
                if (firstPrice != null && !firstPrice.discounts.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color(0xFFEF4444), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = firstPrice.discounts?.firstOrNull() ?: "DEAL",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(MediumPadding)) {
                VerticalSpacer(6)
                Text(
                    roomModel.title,
                    color = HeadlineColor,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                )
                VerticalSpacer(2)
                Text(
                    roomModel.summary,
                    color = ViewAllColor,
                    maxLines = 2,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                )
                VerticalSpacer(8)
                val price = roomModel.price?.get(0)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MediumTitleText(
                        text = "${
                            price?.currency?.ifBlank { "USD" }?.toCurrencySymbol()
                        }${price?.amount}",
                        color = HeadlineColor,
                    )
                    SmallTitleText(
                        text = "/ ${price?.frequency}",
                        color = HeadlineColor,
                    )
                }
                VerticalSpacer(12)
                ProcessButton(
                    text = stringResource(R.string.feature_home_rent_now),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {},
                )

            }

        }
    }
}

@Composable
fun LastMinuteDealCard(
    roomModel: RoomModel,
    discountPercent: Int,
    spotsLeft: Int? = null,
    onClick: () -> Unit,
) {
    roomModel.apply {
        Column(
            modifier = Modifier
                .width(280.dp)
                .padding(NormalPadding)
                .background(Color.White, RoundedCornerShape(LargePadding))
                .clip(RoundedCornerShape(LargePadding))
                .clickable { onClick() },
        ) {
            Box {
                AsyncImage(
                    model = gallery.thumbnail,
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(LargePadding)),
                )
                // Discount badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color(0xFFEF4444), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.feature_home_off, discountPercent),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                }
                // Spots left urgency indicator
                if (spotsLeft != null && spotsLeft <= 5) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.feature_home_spots_left, spotsLeft),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(MediumPadding)) {
                VerticalSpacer(6)
                Text(
                    roomModel.title,
                    color = HeadlineColor,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                )
                VerticalSpacer(2)
                Text(
                    roomModel.summary,
                    color = ViewAllColor,
                    maxLines = 2,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                )
                VerticalSpacer(8)
                val price = roomModel.price?.get(0)
                val originalAmount = price?.amount ?: 0
                val discountedAmount = (originalAmount * (100 - discountPercent)) / 100
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${price?.currency?.ifBlank { "USD" }?.toCurrencySymbol()}$originalAmount",
                        color = ViewAllColor,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        textDecoration = TextDecoration.LineThrough,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    MediumTitleText(
                        text = "${price?.currency?.ifBlank { "USD" }?.toCurrencySymbol()}$discountedAmount",
                        color = Color(0xFFEF4444),
                    )
                    SmallTitleText(
                        text = "/ ${price?.frequency}",
                        color = HeadlineColor,
                    )
                }
                VerticalSpacer(12)
                ProcessButton(
                    text = stringResource(R.string.feature_home_book_now),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {},
                )
            }
        }
    }
}

@Composable
fun RentedGearDealsCard(
    rentedGearModel: RentedGearModel,
    onClick: () -> Unit,
) {
    rentedGearModel.apply {
        Column(
            modifier = Modifier
                .width(280.dp)
                .padding(NormalPadding)
                .background(Color.White, RoundedCornerShape(LargePadding))
                .clip(RoundedCornerShape(LargePadding))
                .clickable { onClick() },
        ) {
            AsyncImage(
                model = if (images.isNullOrEmpty()) R.drawable.no_image else images?.get(0)
                    ?: R.drawable.no_image,
                contentDescription = "",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(LargePadding)),
            )
            Column(modifier = Modifier.padding(MediumPadding)) {
                VerticalSpacer(6)
                Text(
                    name,
                    color = HeadlineColor,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                )
                VerticalSpacer(2)
                Text(
                    description,
                    color = ViewAllColor,
                    maxLines = 2,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                )
                VerticalSpacer(8)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MediumTitleText(
                        text = "${
                            "USD".toCurrencySymbol()
                        }${hourlyRate}",
                        color = HeadlineColor,
                    )
                    SmallTitleText(
                        text = "/hour",
                        color = HeadlineColor,
                    )
                }
                VerticalSpacer(12)
                ProcessButton(
                    text = stringResource(R.string.feature_home_rent_now),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {},
                )

            }

        }
    }
}