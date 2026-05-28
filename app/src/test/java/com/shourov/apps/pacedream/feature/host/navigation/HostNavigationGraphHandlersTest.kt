package com.shourov.apps.pacedream.feature.host.navigation

import com.shourov.apps.pacedream.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the behaviour the user actually experiences when tapping a
 * property card from host context: the click escapes the host
 * NavController and resolves to the unified [Routes.listing]
 * destination. Before this wiring landed, the host NavController tried
 * to navigate to `listing/$id` — a route that was only registered
 * inside the nested dashboard host — and the resulting
 * IllegalArgumentException was swallowed, so the tap looked like a
 * no-op.
 *
 * The test exercises the extracted [hostListingClickHandler] helper
 * directly so the contract can be locked without standing up the full
 * Compose nav graph (which would otherwise require Hilt + Robolectric
 * setup for HostDashboardViewModel).
 */
class HostNavigationGraphHandlersTest {

    @Test
    fun property_card_tap_navigates_to_listing_detail_route() {
        val navigated = mutableListOf<String>()
        val handler = hostListingClickHandler { id ->
            navigated += Routes.listing(id)
        }

        handler("listing-123")

        // Acceptance criterion: host-mode property card tap opens
        // ListingDetail at the unified `listing/{listingId}` route.
        assertEquals(listOf("listing/listing-123"), navigated)
    }

    @Test
    fun property_card_tap_drops_blank_ids() {
        // Defensive: HostDashboardScreen surfaces Property models loaded
        // from the network; an empty id would route to `listing/` which
        // does not match the registered pattern. Dropping blanks here
        // prevents that mismatch from reaching the NavController.
        val navigated = mutableListOf<String>()
        val handler = hostListingClickHandler { id -> navigated += id }

        handler("")
        handler("   ")

        assertEquals(0, navigated.size)
        assertNull(navigated.firstOrNull())
    }

    @Test
    fun property_card_tap_forwards_only_non_blank_ids() {
        val navigated = mutableListOf<String>()
        val handler = hostListingClickHandler { id -> navigated += id }

        handler("")
        handler("abc")
        handler(" ")
        handler("xyz")

        assertEquals(listOf("abc", "xyz"), navigated)
    }
}
