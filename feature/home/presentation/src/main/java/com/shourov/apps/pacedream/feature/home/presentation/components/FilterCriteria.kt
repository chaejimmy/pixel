/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package com.shourov.apps.pacedream.feature.home.presentation.components

/**
 * Captured state from the filter sheet. Mirrors Airbnb's filter taxonomy
 * (dates / guests / property type / price / rooms / amenities / instant-book)
 * so the search layer can translate this into backend query parameters.
 *
 * All fields are optional; null means "no constraint".
 */
data class FilterCriteria(
    val checkInEpochDay: Long? = null,
    val checkOutEpochDay: Long? = null,
    val adults: Int = 0,
    val children: Int = 0,
    val infants: Int = 0,
    val pets: Int = 0,
    val propertyType: String? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val bedrooms: Int? = null,
    val beds: Int? = null,
    val bathrooms: Int? = null,
    val amenities: Set<String> = emptySet(),
    /**
     * When true, restrict results to listings that the host has marked as
     * instant-book — Airbnb-parity toggle that lets users skip request flow.
     */
    val instantBookOnly: Boolean = false,
) {
    val totalGuests: Int get() = adults + children
    val isEmpty: Boolean
        get() = checkInEpochDay == null && checkOutEpochDay == null &&
            totalGuests == 0 && infants == 0 && pets == 0 &&
            propertyType.isNullOrBlank() &&
            minPrice == null && maxPrice == null &&
            bedrooms == null && beds == null && bathrooms == null &&
            amenities.isEmpty() && !instantBookOnly
}
