package com.shourov.apps.pacedream.feature.wishlist.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for wishlist data models: WishlistItem, WishlistItemType, WishlistFilter, WishlistUiState.
 */
class WishlistModelTest {

    // ── WishlistItemType.fromString ─────────────────────────────────

    @Test
    fun `fromString returns TIME_BASED for time-based`() {
        assertEquals(WishlistItemType.TIME_BASED, WishlistItemType.fromString("time-based"))
    }

    @Test
    fun `fromString returns TIME_BASED for use`() {
        assertEquals(WishlistItemType.TIME_BASED, WishlistItemType.fromString("use"))
    }

    @Test
    fun `fromString returns TIME_BASED for SHARE uppercase`() {
        assertEquals(WishlistItemType.TIME_BASED, WishlistItemType.fromString("SHARE"))
    }

    @Test
    fun `fromString returns TIME_BASED for short`() {
        assertEquals(WishlistItemType.TIME_BASED, WishlistItemType.fromString("short"))
    }

    @Test
    fun `fromString returns TIME_BASED for hourly`() {
        assertEquals(WishlistItemType.TIME_BASED, WishlistItemType.fromString("hourly"))
    }

    @Test
    fun `fromString returns HOURLY_GEAR for gear`() {
        assertEquals(WishlistItemType.HOURLY_GEAR, WishlistItemType.fromString("gear"))
    }

    @Test
    fun `fromString returns HOURLY_GEAR for borrow`() {
        assertEquals(WishlistItemType.HOURLY_GEAR, WishlistItemType.fromString("borrow"))
    }

    @Test
    fun `fromString returns HOURLY_GEAR for car`() {
        assertEquals(WishlistItemType.HOURLY_GEAR, WishlistItemType.fromString("car"))
    }

    @Test
    fun `fromString returns HOURLY_GEAR for vehicle`() {
        assertEquals(WishlistItemType.HOURLY_GEAR, WishlistItemType.fromString("vehicle"))
    }

    @Test
    fun `fromString returns HOURLY_GEAR for parking`() {
        assertEquals(WishlistItemType.HOURLY_GEAR, WishlistItemType.fromString("parking"))
    }

    @Test
    fun `fromString returns SPLIT_STAY for split`() {
        assertEquals(WishlistItemType.SPLIT_STAY, WishlistItemType.fromString("split"))
    }

    @Test
    fun `fromString returns SPLIT_STAY for room-stay`() {
        assertEquals(WishlistItemType.SPLIT_STAY, WishlistItemType.fromString("room-stay"))
    }

    @Test
    fun `fromString returns SPLIT_STAY for roommate`() {
        assertEquals(WishlistItemType.SPLIT_STAY, WishlistItemType.fromString("roommate"))
    }

    @Test
    fun `fromString returns null for plain room`() {
        // "room" alone is too generic; only "room-stay" should match SPLIT_STAY
        assertNull(WishlistItemType.fromString("room"))
    }

    @Test
    fun `fromString returns null for unknown type`() {
        assertNull(WishlistItemType.fromString("unknown"))
    }

    @Test
    fun `fromString returns null for null input`() {
        assertNull(WishlistItemType.fromString(null))
    }

    @Test
    fun `fromString is case insensitive`() {
        assertEquals(WishlistItemType.TIME_BASED, WishlistItemType.fromString("TIME-BASED"))
        assertEquals(WishlistItemType.HOURLY_GEAR, WishlistItemType.fromString("GEAR"))
        assertEquals(WishlistItemType.SPLIT_STAY, WishlistItemType.fromString("SPLIT"))
    }

    @Test
    fun `fromString handles whitespace`() {
        assertEquals(WishlistItemType.TIME_BASED, WishlistItemType.fromString("  use  "))
    }

    // ── WishlistItemType enum values ────────────────────────────────

    @Test
    fun `TIME_BASED has correct apiValue and displayName`() {
        assertEquals("time-based", WishlistItemType.TIME_BASED.apiValue)
        assertEquals("Spaces", WishlistItemType.TIME_BASED.displayName)
    }

    @Test
    fun `HOURLY_GEAR has correct apiValue and displayName`() {
        assertEquals("hourly-gear", WishlistItemType.HOURLY_GEAR.apiValue)
        assertEquals("Items", WishlistItemType.HOURLY_GEAR.displayName)
    }

    @Test
    fun `SPLIT_STAY has correct apiValue and displayName`() {
        assertEquals("room-stay", WishlistItemType.SPLIT_STAY.apiValue)
        assertEquals("Services", WishlistItemType.SPLIT_STAY.displayName)
    }

    // ── WishlistItem ────────────────────────────────────────────────

    @Test
    fun `WishlistItem formattedPrice with price`() {
        val item = createItem(price = 49.99)
        assertEquals("$49.99", item.formattedPrice)
    }

    @Test
    fun `WishlistItem formattedPrice with null price`() {
        val item = createItem(price = null)
        assertEquals("", item.formattedPrice)
    }

    @Test
    fun `WishlistItem formattedPrice with priceUnit`() {
        val item = createItem(price = 25.0, priceUnit = "hourly")
        assertEquals("$25/hr", item.formattedPrice)
    }

    @Test
    fun `WishlistItem formattedRating with rating`() {
        val item = createItem(rating = 4.5)
        assertEquals("4.5", item.formattedRating)
    }

    @Test
    fun `WishlistItem formattedRating with null rating`() {
        val item = createItem(rating = null)
        assertEquals("", item.formattedRating)
    }

    @Test
    fun `WishlistItem default type is TIME_BASED`() {
        val item = WishlistItem(id = "1", listingId = "1", title = "Test")
        assertEquals(WishlistItemType.TIME_BASED, item.itemType)
    }

    // ── WishlistFilter ──────────────────────────────────────────────

    @Test
    fun `WishlistFilter ALL matches all items`() {
        val timeBased = createItem(itemType = WishlistItemType.TIME_BASED)
        val gear = createItem(itemType = WishlistItemType.HOURLY_GEAR)
        val split = createItem(itemType = WishlistItemType.SPLIT_STAY)

        assertTrue(WishlistFilter.ALL.matches(timeBased))
        assertTrue(WishlistFilter.ALL.matches(gear))
        assertTrue(WishlistFilter.ALL.matches(split))
    }

    @Test
    fun `WishlistFilter SPACES matches only TIME_BASED`() {
        val timeBased = createItem(itemType = WishlistItemType.TIME_BASED)
        val gear = createItem(itemType = WishlistItemType.HOURLY_GEAR)

        assertTrue(WishlistFilter.SPACES.matches(timeBased))
        assertFalse(WishlistFilter.SPACES.matches(gear))
    }

    @Test
    fun `WishlistFilter ITEMS matches only HOURLY_GEAR`() {
        val gear = createItem(itemType = WishlistItemType.HOURLY_GEAR)
        val timeBased = createItem(itemType = WishlistItemType.TIME_BASED)

        assertTrue(WishlistFilter.ITEMS.matches(gear))
        assertFalse(WishlistFilter.ITEMS.matches(timeBased))
    }

    @Test
    fun `WishlistFilter SERVICES matches only SPLIT_STAY`() {
        val split = createItem(itemType = WishlistItemType.SPLIT_STAY)
        val gear = createItem(itemType = WishlistItemType.HOURLY_GEAR)

        assertTrue(WishlistFilter.SERVICES.matches(split))
        assertFalse(WishlistFilter.SERVICES.matches(gear))
    }

    // ── WishlistUiState ─────────────────────────────────────────────

    @Test
    fun `WishlistUiState Success filteredItems applies filter`() {
        val items = listOf(
            createItem(id = "1", itemType = WishlistItemType.TIME_BASED),
            createItem(id = "2", itemType = WishlistItemType.HOURLY_GEAR),
            createItem(id = "3", itemType = WishlistItemType.SPLIT_STAY)
        )
        val state = WishlistUiState.Success(items, WishlistFilter.ITEMS)
        assertEquals(1, state.filteredItems.size)
        assertEquals("2", state.filteredItems[0].id)
    }

    @Test
    fun `WishlistUiState Success isEmpty true for filtered empty`() {
        val items = listOf(createItem(itemType = WishlistItemType.TIME_BASED))
        val state = WishlistUiState.Success(items, WishlistFilter.ITEMS)
        assertTrue(state.isEmpty)
    }

    @Test
    fun `WishlistUiState Success isEmpty false when items match filter`() {
        val items = listOf(createItem(itemType = WishlistItemType.TIME_BASED))
        val state = WishlistUiState.Success(items, WishlistFilter.ALL)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `WishlistUiState Error has message`() {
        val state = WishlistUiState.Error("Connection failed")
        assertEquals("Connection failed", state.message)
    }

    // ── WishlistFilter display names ────────────────────────────────

    @Test
    fun `WishlistFilter has correct display names`() {
        assertEquals("All", WishlistFilter.ALL.displayName)
        assertEquals("Spaces", WishlistFilter.SPACES.displayName)
        assertEquals("Items", WishlistFilter.ITEMS.displayName)
        assertEquals("Services", WishlistFilter.SERVICES.displayName)
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private fun createItem(
        id: String = "item1",
        itemType: WishlistItemType = WishlistItemType.TIME_BASED,
        price: Double? = null,
        priceUnit: String? = null,
        rating: Double? = null
    ) = WishlistItem(
        id = id,
        listingId = id,
        title = "Test Item",
        description = null,
        imageUrl = null,
        price = price,
        priceUnit = priceUnit,
        itemType = itemType,
        location = null,
        rating = rating
    )
}
