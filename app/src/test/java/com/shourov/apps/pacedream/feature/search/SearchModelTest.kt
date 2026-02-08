package com.shourov.apps.pacedream.feature.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for SearchModels: SearchResultItem, SearchPage, AutocompleteSuggestion,
 * and SearchUiState/SearchPhase.
 */
class SearchModelTest {

    // ── SearchResultItem ────────────────────────────────────────────

    @Test
    fun `SearchResultItem holds all fields`() {
        val item = SearchResultItem(
            id = "abc",
            title = "Studio Apartment",
            location = "New York",
            imageUrl = "https://img.jpg",
            priceText = "$120/night",
            rating = 4.8
        )
        assertEquals("abc", item.id)
        assertEquals("Studio Apartment", item.title)
        assertEquals("New York", item.location)
        assertEquals("https://img.jpg", item.imageUrl)
        assertEquals("$120/night", item.priceText)
        assertEquals(4.8, item.rating!!, 0.01)
    }

    @Test
    fun `SearchResultItem allows null optional fields`() {
        val item = SearchResultItem(
            id = "1",
            title = "Minimal",
            location = null,
            imageUrl = null,
            priceText = null,
            rating = null
        )
        assertNull(item.location)
        assertNull(item.imageUrl)
        assertNull(item.priceText)
        assertNull(item.rating)
    }

    // ── SearchPage ──────────────────────────────────────────────────

    @Test
    fun `SearchPage empty with no more results`() {
        val page = SearchPage(items = emptyList(), hasMore = false)
        assertEquals(0, page.items.size)
        assertFalse(page.hasMore)
    }

    @Test
    fun `SearchPage with items and hasMore`() {
        val items = listOf(
            SearchResultItem("1", "A", null, null, null, null),
            SearchResultItem("2", "B", null, null, null, null)
        )
        val page = SearchPage(items = items, hasMore = true)
        assertEquals(2, page.items.size)
        assertTrue(page.hasMore)
    }

    // ── AutocompleteSuggestion ──────────────────────────────────────

    @Test
    fun `AutocompleteSuggestion holds value`() {
        val suggestion = AutocompleteSuggestion("New York City")
        assertEquals("New York City", suggestion.value)
    }

    // ── SearchPhase ─────────────────────────────────────────────────

    @Test
    fun `SearchPhase has all expected values`() {
        val phases = SearchPhase.values()
        assertEquals(6, phases.size)
        assertTrue(phases.contains(SearchPhase.Idle))
        assertTrue(phases.contains(SearchPhase.Loading))
        assertTrue(phases.contains(SearchPhase.LoadingMore))
        assertTrue(phases.contains(SearchPhase.Success))
        assertTrue(phases.contains(SearchPhase.Empty))
        assertTrue(phases.contains(SearchPhase.Error))
    }

    // ── SearchUiState ───────────────────────────────────────────────

    @Test
    fun `SearchUiState default values`() {
        val state = SearchUiState()
        assertEquals("", state.query)
        assertNull(state.city)
        assertNull(state.category)
        assertNull(state.sort)
        assertEquals(24, state.perPage)
        assertEquals(0, state.page0)
        assertTrue(state.items.isEmpty())
        assertFalse(state.hasMore)
        assertTrue(state.suggestions.isEmpty())
        assertEquals(SearchPhase.Idle, state.phase)
        assertNull(state.errorMessage)
        assertNull(state.shareType)
        assertNull(state.whatQuery)
        assertNull(state.startDate)
        assertNull(state.endDate)
    }

    @Test
    fun `SearchUiState with custom values`() {
        val state = SearchUiState(
            query = "studio",
            city = "NYC",
            category = "hourly",
            sort = "price",
            page0 = 2,
            hasMore = true,
            phase = SearchPhase.Success,
            shareType = "USE",
            startDate = "2024-06-01",
            endDate = "2024-06-15"
        )
        assertEquals("studio", state.query)
        assertEquals("NYC", state.city)
        assertEquals("hourly", state.category)
        assertEquals("price", state.sort)
        assertEquals(2, state.page0)
        assertTrue(state.hasMore)
        assertEquals(SearchPhase.Success, state.phase)
        assertEquals("USE", state.shareType)
        assertEquals("2024-06-01", state.startDate)
        assertEquals("2024-06-15", state.endDate)
    }

    @Test
    fun `SearchUiState error state`() {
        val state = SearchUiState(
            phase = SearchPhase.Error,
            errorMessage = "Network timeout"
        )
        assertEquals(SearchPhase.Error, state.phase)
        assertEquals("Network timeout", state.errorMessage)
    }
}
