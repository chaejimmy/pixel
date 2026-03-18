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
import coil.compose.AsyncImage
import com.pacedream.common.composables.VerticalSpacer
import com.pacedream.common.composables.buttons.ProcessButton
import com.pacedream.common.composables.texts.MediumTitleText
import com.pacedream.common.composables.texts.SmallTitleText
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamIconSize
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
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
                .padding(PaceDreamSpacing.SM)
                .background(PaceDreamColors.Card, RoundedCornerShape(PaceDreamRadius.LG))
                .clip(RoundedCornerShape(PaceDreamRadius.LG))
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
                        .clip(RoundedCornerShape(PaceDreamRadius.LG)),
                )
                // Discount badge (for last-minute deals)
                val priceList = roomModel.price
                val firstPrice = priceList?.firstOrNull()
                if (firstPrice != null && !firstPrice.discounts.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(PaceDreamSpacing.SM)
                            .background(PaceDreamColors.Error, RoundedCornerShape(PaceDreamRadius.XS))
                            .padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(PaceDreamIconSize.XS)
                            )
                            Text(
                                text = firstPrice.discounts?.firstOrNull() ?: "DEAL",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = PaceDreamTypography.Caption2,
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                VerticalSpacer(6)
                Text(
                    roomModel.title,
                    color = PaceDreamColors.TextPrimary,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                )
                VerticalSpacer(2)
                Text(
                    roomModel.summary,
                    color = PaceDreamColors.TextTertiary,
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
                        color = PaceDreamColors.TextPrimary,
                    )
                    SmallTitleText(
                        text = "/ ${price?.frequency}",
                        color = PaceDreamColors.TextPrimary,
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
                .padding(PaceDreamSpacing.SM)
                .background(PaceDreamColors.Card, RoundedCornerShape(PaceDreamRadius.LG))
                .clip(RoundedCornerShape(PaceDreamRadius.LG))
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
                        .clip(RoundedCornerShape(PaceDreamRadius.LG)),
                )
                // Discount badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(PaceDreamSpacing.SM)
                        .background(PaceDreamColors.Error, RoundedCornerShape(PaceDreamRadius.XS))
                        .padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(PaceDreamIconSize.XS)
                        )
                        Text(
                            text = stringResource(R.string.feature_home_off, discountPercent),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = PaceDreamTypography.Caption2,
                        )
                    }
                }
                // Spots left urgency indicator
                if (spotsLeft != null && spotsLeft <= 5) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(PaceDreamSpacing.SM)
                            .background(PaceDreamColors.Warning, RoundedCornerShape(PaceDreamRadius.XS))
                            .padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS)
                    ) {
                        Text(
                            text = stringResource(R.string.feature_home_spots_left, spotsLeft),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = PaceDreamTypography.Caption2,
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                VerticalSpacer(6)
                Text(
                    roomModel.title,
                    color = PaceDreamColors.TextPrimary,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                )
                VerticalSpacer(2)
                Text(
                    roomModel.summary,
                    color = PaceDreamColors.TextTertiary,
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
                        color = PaceDreamColors.TextTertiary,
                        fontWeight = FontWeight.Normal,
                        style = PaceDreamTypography.Caption,
                        textDecoration = TextDecoration.LineThrough,
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                    MediumTitleText(
                        text = "${price?.currency?.ifBlank { "USD" }?.toCurrencySymbol()}$discountedAmount",
                        color = PaceDreamColors.Error,
                    )
                    SmallTitleText(
                        text = "/ ${price?.frequency}",
                        color = PaceDreamColors.TextPrimary,
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
                .padding(PaceDreamSpacing.SM)
                .background(PaceDreamColors.Card, RoundedCornerShape(PaceDreamRadius.LG))
                .clip(RoundedCornerShape(PaceDreamRadius.LG))
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
                    .clip(RoundedCornerShape(PaceDreamRadius.LG)),
            )
            Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                VerticalSpacer(6)
                Text(
                    name,
                    color = PaceDreamColors.TextPrimary,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                )
                VerticalSpacer(2)
                Text(
                    description,
                    color = PaceDreamColors.TextTertiary,
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
                        color = PaceDreamColors.TextPrimary,
                    )
                    SmallTitleText(
                        text = "/hour",
                        color = PaceDreamColors.TextPrimary,
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