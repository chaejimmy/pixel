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

package com.shourov.apps.pacedream.feature.home.data.dto.split_stays

import com.google.gson.annotations.SerializedName
import com.shourov.apps.pacedream.feature.home.domain.models.SplitStayModel

data class SplitStayResponse(
    val status: Boolean,
    val message: String? = null,
    val data: List<SplitStayDto> = emptyList()
)

data class SplitStayDto(
    @SerializedName("_id")
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val location: SplitStayLocationDto? = null,
    val price: Double? = null,
    @SerializedName("price_unit")
    val priceUnit: String? = null,
    val rating: Float? = null,
    @SerializedName("review_count")
    val reviewCount: Int? = null,
    val images: List<String>? = null,
    val amenities: List<String>? = null,
    @SerializedName("room_type")
    val roomType: String? = null,
    @SerializedName("max_guests")
    val maxGuests: Int? = null,
    @SerializedName("is_available")
    val isAvailable: Boolean? = null,
    @SerializedName("host_id")
    val hostId: String? = null,
    @SerializedName("host_name")
    val hostName: String? = null,
    @SerializedName("host_avatar")
    val hostAvatar: String? = null,
)

data class SplitStayLocationDto(
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val address: String? = null,
)

fun SplitStayDto.toSplitStayModel(): SplitStayModel {
    return SplitStayModel(
        _id = id,
        name = name,
        description = description,
        location = location?.address ?: "${location?.city ?: ""}, ${location?.state ?: ""}",
        city = location?.city,
        price = price,
        priceUnit = priceUnit ?: "per night",
        rating = rating,
        reviewCount = reviewCount,
        images = images,
        amenities = amenities,
        roomType = roomType,
        maxGuests = maxGuests,
        isAvailable = isAvailable ?: true,
        hostId = hostId,
        hostName = hostName,
        hostAvatar = hostAvatar,
    )
}


