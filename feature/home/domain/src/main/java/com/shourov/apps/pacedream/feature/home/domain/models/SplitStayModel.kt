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

package com.shourov.apps.pacedream.feature.home.domain.models

/**
 * Domain model for Split Stays (Roommate Room Stay)
 * Used for shared accommodation / split cost stays
 */
data class SplitStayModel(
    val _id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val location: String? = null,
    val city: String? = null,
    val price: Double? = null,
    val priceUnit: String? = null, // "per night", "per month", etc.
    val rating: Float? = null,
    val reviewCount: Int? = null,
    val images: List<String>? = null,
    val amenities: List<String>? = null,
    val roomType: String? = null, // "private", "shared", etc.
    val maxGuests: Int? = null,
    val isAvailable: Boolean = true,
    val hostId: String? = null,
    val hostName: String? = null,
    val hostAvatar: String? = null,
)


