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
import androidx.compose.foundation.layout.fillMaxSize
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
import com.shourov.apps.pacedream.designsystem.OnBrandSurface
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
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

const val DealCardCtaTestTag = "deal_card_cta"

/**
 * Horizontal deals carousel card with an inline "Rent Now" CTA.
 *
 * Two click surfaces, intentionally separate so callers can route them:
 *  - [onClick]    — outer card tap. Default destination: listing detail
 *                   (e.g. `navController.navigate(ListingRoutes.detail(roomModel.id))`).
 *  - [onCtaClick] — inline primary CTA tap ("Rent Now").
 *                   Default destination: same as [onClick] (listing detail),
 *                   so the two surfaces never disagree about where they go.
 *                   Callers may route the CTA to checkout pre-fill instead
 *                   if product wants the inline CTA to short-circuit detail;
 *                   keep that decision at the callsite, not in the card.
 *
 * Both parameters are required (no `{}` default) so a forgotten wire-up
 * surfaces at compile time rather than as a dead button at runtime.
 */
@Composable
fun DealsCard(
    roomModel: RoomModel,
    onClick: () -> Unit,
    onCtaClick: () -> Unit,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.LG))
                    .background(PaceDreamColors.Gray100),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(gallery.thumbnail)
                        .crossfade(200)
                        // 280x180dp deal card — bound the decode size.
                        .size(coil.size.Size(840, 540))
                        .build(),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
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
                                tint = OnBrandSurface,
                                modifier = Modifier.size(PaceDreamIconSize.XS)
                            )
                            Text(
                                text = firstPrice.discounts?.firstOrNull() ?: "DEAL",
                                color = OnBrandSurface,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DealCardCtaTestTag),
                    onClick = onCtaClick,
                )

            }

        }
    }
}

/**
 * Last-minute deals carousel card with discount badge and inline "Book Now" CTA.
 *
 *  - [onClick]    — outer card tap. Default destination: listing detail.
 *  - [onCtaClick] — inline primary CTA tap ("Book Now").
 *                   Default destination: same as [onClick]. Because last-minute
 *                   deals already imply urgency, callers may wire this to a
 *                   checkout pre-fill (skipping detail) when product asks for
 *                   the shorter funnel; do that at the callsite, not in the card.
 *
 * Both parameters are required so unwired CTAs fail at compile time.
 */
@Composable
fun LastMinuteDealCard(
    roomModel: RoomModel,
    discountPercent: Int,
    spotsLeft: Int? = null,
    onClick: () -> Unit,
    onCtaClick: () -> Unit,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.LG))
                    .background(PaceDreamColors.Gray100),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(gallery.thumbnail)
                        .crossfade(200)
                        // 280x180dp deal card — bound the decode size.
                        .size(coil.size.Size(840, 540))
                        .build(),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
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
                            tint = OnBrandSurface,
                            modifier = Modifier.size(PaceDreamIconSize.XS)
                        )
                        Text(
                            text = stringResource(R.string.feature_home_off, discountPercent),
                            color = OnBrandSurface,
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
                            color = OnBrandSurface,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DealCardCtaTestTag),
                    onClick = onCtaClick,
                )
            }
        }
    }
}

/**
 * Rented-gear deals carousel card with inline "Rent Now" CTA.
 *
 *  - [onClick]    — outer card tap. Default destination: gear listing detail
 *                   (`navController.navigate(ListingRoutes.detail(rentedGearModel.id))`).
 *  - [onCtaClick] — inline primary CTA tap ("Rent Now").
 *                   Default destination: same as [onClick]. Gear rentals are
 *                   typically detail-first because the renter needs to see
 *                   pickup terms before paying, so callers should route both
 *                   surfaces to detail unless explicitly told otherwise.
 *
 * Both parameters are required so unwired CTAs fail at compile time.
 */
@Composable
fun RentedGearDealsCard(
    rentedGearModel: RentedGearModel,
    onClick: () -> Unit,
    onCtaClick: () -> Unit,
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
                model = ImageRequest.Builder(LocalContext.current)
                    .data(if (images.isNullOrEmpty()) R.drawable.no_image else images?.get(0)
                        ?: R.drawable.no_image)
                    .crossfade(200)
                    // 280dp-wide deal card — bound the decode size.
                    .size(coil.size.Size(840, 540))
                    .build(),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DealCardCtaTestTag),
                    onClick = onCtaClick,
                )
            }
        }
    }
}
