package com.shourov.apps.pacedream.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the route string contract shared between the top-level NavHost
 * and the host-mode escape hatches. Prior to centralising these in
 * [Routes], host code navigated to `listing/$id` and `booking_details/$id`
 * patterns that were only registered inside nested NavHosts with
 * different conventions (uppercase enum names) — the calls were
 * swallowed by a try-catch and the tap looked like a no-op. Pinning the
 * strings here makes any future drift between callsite and registered
 * route visible in CI.
 */
class RoutesTest {

    @Test
    fun listing_helper_matches_registered_pattern() {
        // The pattern declares the placeholder; the helper substitutes a
        // concrete id. Both must use the same literal prefix or NavController
        // will throw IllegalArgumentException on navigation.
        assertEquals("listing/{listingId}", Routes.LISTING_PATTERN)
        assertEquals("listing/abc-123", Routes.listing("abc-123"))
    }

    @Test
    fun bookingDetail_helper_matches_registered_pattern() {
        assertEquals("booking_details/{bookingId}", Routes.BOOKING_DETAIL_PATTERN)
        assertEquals("booking_details/booking-42", Routes.bookingDetail("booking-42"))
    }

    @Test
    fun listing_helper_supports_uuid_like_ids() {
        // Real listing ids in the codebase are UUIDs; make sure dashes
        // and the standard hex grouping survive interpolation as-is
        // (no URL-encoding is applied here — they are valid path segments).
        val id = "550e8400-e29b-41d4-a716-446655440000"
        assertEquals("listing/$id", Routes.listing(id))
    }

    @Test
    fun bookingDetail_helper_supports_uuid_like_ids() {
        val id = "11111111-2222-3333-4444-555555555555"
        assertEquals("booking_details/$id", Routes.bookingDetail(id))
    }
}
