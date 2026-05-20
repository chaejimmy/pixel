package com.shourov.apps.pacedream.feature.search

import com.shourov.apps.pacedream.feature.home.presentation.components.FilterCriteria
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks in [SearchFiltersStore] semantics. The store is the single source
 * of truth shared between FilterScreen (writer) and SearchViewModel
 * (reader), so the contract has to hold:
 *
 *  1. Applying a non-default [FilterCriteria] is observable via the
 *     `criteria` flow — this is the mutation the view-model collector
 *     turns into a query reload.
 *  2. Clearing returns the slider (and every other field) to default,
 *     and that change is also observable.
 */
class SearchFiltersStoreTest {

    @Test
    fun `applying a filter mutates the criteria flow`() {
        val store = SearchFiltersStore()
        val applied = FilterCriteria(
            minPrice = 100,
            maxPrice = 500,
            propertyType = "Apartment",
            amenities = setOf("WiFi", "Pool"),
        )

        assertEquals(FilterCriteria(), store.criteria.value)
        store.update(applied)
        assertEquals(applied, store.criteria.value)
        assertNotEquals(FilterCriteria(), store.criteria.value)
    }

    @Test
    fun `clearing returns the slider to default`() {
        val store = SearchFiltersStore()
        store.update(
            FilterCriteria(
                minPrice = 250,
                maxPrice = 1500,
                amenities = setOf("Pool"),
            ),
        )
        // Sanity check: the price range fields are populated.
        assertEquals(250, store.criteria.value.minPrice)
        assertEquals(1500, store.criteria.value.maxPrice)

        store.clear()

        // Defaults — null on both ends of the range, no amenities, no
        // active filters at all. Equivalent to a fresh FilterCriteria(),
        // which is what the FilterScreen reads back on reopen.
        val cleared = store.criteria.value
        assertEquals(FilterCriteria(), cleared)
        assertEquals(null, cleared.minPrice)
        assertEquals(null, cleared.maxPrice)
        assertTrue(cleared.amenities.isEmpty())
        assertEquals(0, cleared.activeFilterCount())
    }

    @Test
    fun `update is idempotent`() {
        val store = SearchFiltersStore()
        val criteria = FilterCriteria(minPrice = 100, maxPrice = 200)
        store.update(criteria)
        store.update(criteria)
        assertEquals(criteria, store.criteria.value)
    }

    @Test
    fun `update overwrites previous criteria rather than merging`() {
        val store = SearchFiltersStore()
        store.update(FilterCriteria(amenities = setOf("WiFi"), instantBookOnly = true))
        // Subsequent update with a different shape replaces the whole
        // state — the store does not merge / accumulate, so an unset
        // amenity field on the new payload clears the previous one.
        store.update(FilterCriteria(minPrice = 50))
        val current = store.criteria.value
        assertEquals(50, current.minPrice)
        assertTrue(current.amenities.isEmpty())
        assertEquals(false, current.instantBookOnly)
    }
}
