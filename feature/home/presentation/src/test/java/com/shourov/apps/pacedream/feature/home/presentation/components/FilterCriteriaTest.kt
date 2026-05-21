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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins down [FilterCriteria] derived state. `activeFilterCount()` in
 * particular drives the FilterScreen's "Apply N filters" CTA label and
 * the badge on the search header's Filters chip — both must agree, so
 * the helper is locked down with explicit cases for each category.
 */
class FilterCriteriaTest {

    @Test
    fun `default criteria is empty`() {
        val criteria = FilterCriteria()
        assertTrue(criteria.isEmpty)
        assertEquals(0, criteria.activeFilterCount())
        assertEquals(0, criteria.totalGuests)
    }

    @Test
    fun `totalGuests sums adults and children but not infants or pets`() {
        val criteria = FilterCriteria(adults = 2, children = 1, infants = 1, pets = 1)
        assertEquals(3, criteria.totalGuests)
    }

    @Test
    fun `activeFilterCount counts each constrained category once`() {
        val criteria = FilterCriteria(
            checkInEpochDay = 100L,                 // dates
            adults = 2,                             // guests
            propertyType = "Apartment",             // property type
            minPrice = 50,                          // price
            bedrooms = 2,                           // bedrooms
            beds = 2,                               // beds
            bathrooms = 1,                          // bathrooms
            instantBookOnly = true,                 // instant book
            amenities = setOf("WiFi"),              // amenities
        )
        assertFalse(criteria.isEmpty)
        assertEquals(9, criteria.activeFilterCount())
    }

    @Test
    fun `price min OR max counts as one filter`() {
        assertEquals(1, FilterCriteria(minPrice = 50).activeFilterCount())
        assertEquals(1, FilterCriteria(maxPrice = 500).activeFilterCount())
        // Both set still counts as one — it's the same category.
        assertEquals(1, FilterCriteria(minPrice = 50, maxPrice = 500).activeFilterCount())
    }

    @Test
    fun `dates count as one filter regardless of check-in only or full range`() {
        assertEquals(1, FilterCriteria(checkInEpochDay = 100L).activeFilterCount())
        assertEquals(1, FilterCriteria(checkOutEpochDay = 200L).activeFilterCount())
        assertEquals(
            1,
            FilterCriteria(checkInEpochDay = 100L, checkOutEpochDay = 200L).activeFilterCount(),
        )
    }

    @Test
    fun `guests count as one filter whether adults, children, infants, or pets`() {
        assertEquals(1, FilterCriteria(adults = 2).activeFilterCount())
        assertEquals(1, FilterCriteria(children = 1).activeFilterCount())
        assertEquals(1, FilterCriteria(infants = 1).activeFilterCount())
        assertEquals(1, FilterCriteria(pets = 1).activeFilterCount())
        // Combined still one bucket.
        assertEquals(
            1,
            FilterCriteria(adults = 2, children = 1, infants = 1, pets = 1).activeFilterCount(),
        )
    }

    @Test
    fun `amenities count once regardless of size`() {
        assertEquals(1, FilterCriteria(amenities = setOf("WiFi")).activeFilterCount())
        assertEquals(
            1,
            FilterCriteria(amenities = setOf("WiFi", "Pool", "Gym")).activeFilterCount(),
        )
    }

    @Test
    fun `instantBookOnly false does not contribute`() {
        assertEquals(0, FilterCriteria(instantBookOnly = false).activeFilterCount())
        assertEquals(1, FilterCriteria(instantBookOnly = true).activeFilterCount())
    }

    @Test
    fun `blank propertyType does not contribute`() {
        assertEquals(0, FilterCriteria(propertyType = "").activeFilterCount())
        assertEquals(0, FilterCriteria(propertyType = "   ").activeFilterCount())
        assertEquals(1, FilterCriteria(propertyType = "House").activeFilterCount())
    }
}
