package com.shourov.apps.pacedream.feature.homefeed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for HomeFeedModels: HomeCard, HomeSectionKey, HomeSection, HomeFeedState.
 */
class HomeFeedModelTest {

    // ── HomeSection ─────────────────────────────────────────────────

    @Test
    fun `HomeSection loading state`() {
        val section = HomeSection(
            key = HomeSectionKey.HOURLY,
            items = emptyList(),
            isLoading = true,
            errorMessage = null
        )
        assertTrue(section.isLoading)
        assertTrue(section.items.isEmpty())
        assertNull(section.errorMessage)
    }

    @Test
    fun `HomeSection loaded with items`() {
        val cards = listOf(
            HomeCard("1", "Space A", "NYC", null, "$10", 4.0),
            HomeCard("2", "Space B", "LA", null, "$20", 3.5)
        )
        val section = HomeSection(
            key = HomeSectionKey.GEAR,
            items = cards,
            isLoading = false
        )
        assertFalse(section.isLoading)
        assertEquals(2, section.items.size)
        assertEquals(HomeSectionKey.GEAR, section.key)
    }

    @Test
    fun `HomeSection error state`() {
        val section = HomeSection(
            key = HomeSectionKey.SPLIT,
            items = emptyList(),
            isLoading = false,
            errorMessage = "Failed to load"
        )
        assertEquals("Failed to load", section.errorMessage)
    }

    // ── HomeFeedState ───────────────────────────────────────────────

    @Test
    fun `HomeFeedState default is not refreshing`() {
        val state = HomeFeedState()
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `HomeFeedState refreshing state`() {
        val state = HomeFeedState(isRefreshing = true)
        assertTrue(state.isRefreshing)
    }

    @Test
    fun `HomeFeedState with custom header`() {
        val state = HomeFeedState(headerTitle = "Welcome back!")
        assertEquals("Welcome back!", state.headerTitle)
    }

    @Test
    fun `HomeFeedState with global error`() {
        val state = HomeFeedState(globalErrorMessage = "Network failure")
        assertEquals("Network failure", state.globalErrorMessage)
    }

    // ── HomeSectionKey values ───────────────────────────────────────

    @Test
    fun `HomeSectionKey enum has exactly 3 values`() {
        assertEquals(3, HomeSectionKey.values().size)
    }

    @Test
    fun `HomeSectionKey HOURLY shareType is USE`() {
        assertEquals("USE", HomeSectionKey.HOURLY.shareType)
    }

    @Test
    fun `HomeSectionKey GEAR shareType is BORROW`() {
        assertEquals("BORROW", HomeSectionKey.GEAR.shareType)
    }

    @Test
    fun `HomeSectionKey SPLIT shareType is SPLIT`() {
        assertEquals("SPLIT", HomeSectionKey.SPLIT.shareType)
    }
}
