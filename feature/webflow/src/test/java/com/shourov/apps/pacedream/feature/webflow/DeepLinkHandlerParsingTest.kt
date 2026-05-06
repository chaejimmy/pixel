package com.shourov.apps.pacedream.feature.webflow

import com.shourov.apps.pacedream.feature.webflow.DeepLinkHandler.MatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [DeepLinkHandler.matchFromParts].
 *
 * Targets the pure-Kotlin URI matcher so we don't need Robolectric for
 * [android.net.Uri].  Pins the new external `/threads/{id}` and
 * `/bookings/{id}` parsing alongside the pre-existing
 * `/booking-success`, `/booking-cancelled`, listing, gear, and Stripe
 * Connect routes.
 */
class DeepLinkHandlerParsingTest {

    private fun match(
        path: String,
        segments: List<String> = path.split("/").filter { it.isNotBlank() },
        host: String = "www.pacedream.com",
        scheme: String? = "https",
        sessionId: String? = null,
    ): MatchResult? = DeepLinkHandler.matchFromParts(
        scheme = scheme,
        host = host,
        path = path,
        segments = segments,
        sessionIdQuery = sessionId,
    )

    // ── /bookings/{id} ─────────────────────────────────────────────

    @Test
    fun `bookings path with id returns BookingDetail`() {
        val result = match("/bookings/abc123")
        assertEquals(MatchResult.Direct(DeepLinkResult.BookingDetail("abc123")), result)
    }

    @Test
    fun `bookings path preserves original-case id`() {
        // Stripe session ids and Mongo ObjectIds can be mixed-case;
        // the matcher must not lowercase the value it hands back.
        val result = match(
            path = "/bookings/cs_testabcde",
            segments = listOf("bookings", "Cs_TestAbCdE"),
            host = "pacedream.com",
        )
        assertEquals(MatchResult.Direct(DeepLinkResult.BookingDetail("Cs_TestAbCdE")), result)
    }

    @Test
    fun `bookings path without id returns null`() {
        assertNull(match("/bookings"))
    }

    @Test
    fun `bookings path with invalid id characters returns null`() {
        // Punctuation other than - and _ must not slip through.
        val result = match(
            path = "/bookings/abc!@#",
            segments = listOf("bookings", "abc!@#"),
        )
        assertNull(result)
    }

    // ── /threads/{id} and /messages/{id} ───────────────────────────

    @Test
    fun `threads path with id returns Thread`() {
        val result = match("/threads/thr_42")
        assertEquals(MatchResult.Direct(DeepLinkResult.Thread("thr_42")), result)
    }

    @Test
    fun `messages path with id returns Thread`() {
        val result = match(path = "/messages/m-7", host = "pacedream.com")
        assertEquals(MatchResult.Direct(DeepLinkResult.Thread("m-7")), result)
    }

    @Test
    fun `threads path without id returns null`() {
        assertNull(match("/threads"))
    }

    // ── Booking-success / booking-cancelled stay intact ─────────────

    @Test
    fun `booking-success with session id returns Direct BookingSuccess`() {
        val result = match(path = "/booking-success", sessionId = "cs_test_abc")
        assertEquals(
            MatchResult.Direct(DeepLinkResult.BookingSuccess("cs_test_abc")),
            result,
        )
    }

    @Test
    fun `booking-success without session id signals stored-fallback`() {
        // The pure matcher returns the marker — parseUri layers the
        // checkStoredCheckout() side effect on top.  Pinning the marker
        // here means a regression that drops the fallback signal will
        // fail the test.
        val result = match(path = "/booking-success", sessionId = null)
        assertEquals(MatchResult.BookingSuccessFallback, result)
    }

    @Test
    fun `booking-cancelled returns BookingCancelled marker`() {
        val result = match("/booking-cancelled")
        assertEquals(MatchResult.BookingCancelled, result)
    }

    // ── Non-PaceDream hosts are rejected ────────────────────────────

    @Test
    fun `non pacedream host returns null`() {
        val result = match(
            path = "/bookings/abc123",
            host = "evil.example.com",
        )
        assertNull(result)
    }

    // ── Listing / gear / stripe-connect (regression coverage) ───────

    @Test
    fun `listing path returns ListingDetail`() {
        val result = match("/listings/abc123")
        assertEquals(MatchResult.Direct(DeepLinkResult.ListingDetail("abc123")), result)
    }

    @Test
    fun `gear path returns GearDetail`() {
        val result = match("/gear/g-1")
        assertEquals(MatchResult.Direct(DeepLinkResult.GearDetail("g-1")), result)
    }

    @Test
    fun `stripe-connect-return path returns StripeConnectReturn`() {
        val result = match("/stripe-connect-return")
        assertEquals(MatchResult.Direct(DeepLinkResult.StripeConnectReturn), result)
    }

    // ── Path-segment based matching avoids hyphenated false positives ──

    @Test
    fun `booking-success route does not collide with bookings route`() {
        // "/booking-success" must NOT match the /bookings/{id} branch even
        // though the path contains the substring "booking".
        val result = match("/booking-success", sessionId = "cs_x")
        assertEquals(
            MatchResult.Direct(DeepLinkResult.BookingSuccess("cs_x")),
            result,
        )
    }
}
