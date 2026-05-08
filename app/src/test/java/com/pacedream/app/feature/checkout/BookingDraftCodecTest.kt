package com.pacedream.app.feature.checkout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Guards the contract that the BOOKING_FORM route relies on to decide
 * whether to show CheckoutScreen vs the missing-draft recovery surface.
 *
 * The route uses `runCatching { BookingDraftCodec.decode(it) }.getOrNull()`
 * — these tests pin down which inputs return null (recovery path) and
 * which return a real BookingDraft (proceed to native checkout).  A
 * regression here would re-open the P0 fake-success path that sent users
 * to "Booking Confirmed!" without payment.
 */
class BookingDraftCodecTest {

    @Test
    fun `roundtrip preserves listingId and times`() {
        val original = BookingDraft(
            listingId = "listing_abc",
            listingType = "time-based",
            date = "2026-05-10",
            startTimeISO = "2026-05-10T10:00:00",
            endTimeISO = "2026-05-10T11:00:00",
            guests = 2,
        )
        val encoded = BookingDraftCodec.encode(original)
        val decoded = BookingDraftCodec.decode(encoded)

        assertEquals(original.listingId, decoded.listingId)
        assertEquals(original.startTimeISO, decoded.startTimeISO)
        assertEquals(original.endTimeISO, decoded.endTimeISO)
        assertEquals(original.guests, decoded.guests)
    }

    @Test
    fun `decode throws on blank payload — recovery path engaged`() {
        // Mirrors the BOOKING_FORM route's `runCatching { decode(...) }`
        // — a blank string produces a null draft, which now triggers the
        // missing-draft recovery surface instead of legacy
        // BookingFormScreen.
        val draft = runCatching { BookingDraftCodec.decode("") }.getOrNull()
        assertNull("Blank payload must not yield a usable draft", draft)
    }

    @Test
    fun `decode throws on malformed JSON — recovery path engaged`() {
        val draft = runCatching { BookingDraftCodec.decode("not-json") }.getOrNull()
        assertNull("Malformed payload must not yield a usable draft", draft)
    }

    @Test
    fun `decode succeeds on minimal valid payload`() {
        val raw = """
            {"listingId":"abc","date":"2026-05-10",
             "startTimeISO":"2026-05-10T10:00:00",
             "endTimeISO":"2026-05-10T11:00:00",
             "guests":1}
        """.trimIndent()
        val draft = runCatching { BookingDraftCodec.decode(raw) }.getOrNull()
        assertNotNull(draft)
        assertEquals("abc", draft!!.listingId)
    }
}
