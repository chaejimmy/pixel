package com.shourov.apps.pacedream.feature.webflow.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for BookingRepository data models and parsing logic.
 * Covers: CheckoutResult, BookingConfirmation, BookingType, StoredCheckout.
 */
class BookingRepositoryParsingTest {

    // ── BookingType enum ────────────────────────────────────────────

    @Test
    fun `BookingType TIME_BASED has correct name`() {
        assertEquals("TIME_BASED", BookingType.TIME_BASED.name)
    }

    @Test
    fun `BookingType GEAR has correct name`() {
        assertEquals("GEAR", BookingType.GEAR.name)
    }

    @Test
    fun `BookingType valueOf round trips`() {
        assertEquals(BookingType.TIME_BASED, BookingType.valueOf("TIME_BASED"))
        assertEquals(BookingType.GEAR, BookingType.valueOf("GEAR"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `BookingType valueOf throws for unknown value`() {
        BookingType.valueOf("UNKNOWN")
    }

    // ── CheckoutResult ──────────────────────────────────────────────

    @Test
    fun `CheckoutResult holds checkout data`() {
        val result = CheckoutResult(
            checkoutUrl = "https://checkout.stripe.com/pay/cs_test_abc",
            sessionId = "cs_test_abc",
            bookingType = BookingType.TIME_BASED
        )
        assertEquals("https://checkout.stripe.com/pay/cs_test_abc", result.checkoutUrl)
        assertEquals("cs_test_abc", result.sessionId)
        assertEquals(BookingType.TIME_BASED, result.bookingType)
    }

    @Test
    fun `CheckoutResult allows null fields`() {
        val result = CheckoutResult(
            checkoutUrl = null,
            sessionId = null,
            bookingType = BookingType.GEAR
        )
        assertNull(result.checkoutUrl)
        assertNull(result.sessionId)
    }

    // ── BookingConfirmation ─────────────────────────────────────────

    @Test
    fun `BookingConfirmation holds all fields`() {
        val confirmation = BookingConfirmation(
            bookingId = "bk_123",
            bookingType = BookingType.GEAR,
            status = "confirmed",
            message = "Booking confirmed successfully",
            itemTitle = "Camera Kit",
            startDate = "2024-06-01",
            endDate = "2024-06-03",
            amount = 150.0
        )
        assertEquals("bk_123", confirmation.bookingId)
        assertEquals(BookingType.GEAR, confirmation.bookingType)
        assertEquals("confirmed", confirmation.status)
        assertEquals("Camera Kit", confirmation.itemTitle)
        assertEquals("2024-06-01", confirmation.startDate)
        assertEquals("2024-06-03", confirmation.endDate)
        assertEquals(150.0, confirmation.amount!!, 0.01)
    }

    @Test
    fun `BookingConfirmation formattedAmount with amount`() {
        val confirmation = BookingConfirmation(
            bookingId = "bk_1",
            bookingType = BookingType.TIME_BASED,
            status = "confirmed",
            message = "OK",
            itemTitle = null,
            startDate = null,
            endDate = null,
            amount = 99.50
        )
        assertEquals("$99.50", confirmation.formattedAmount)
    }

    @Test
    fun `BookingConfirmation formattedAmount with null amount`() {
        val confirmation = BookingConfirmation(
            bookingId = "bk_1",
            bookingType = BookingType.TIME_BASED,
            status = "confirmed",
            message = "OK",
            itemTitle = null,
            startDate = null,
            endDate = null,
            amount = null
        )
        assertEquals("", confirmation.formattedAmount)
    }

    @Test
    fun `BookingConfirmation formattedAmount with zero`() {
        val confirmation = BookingConfirmation(
            bookingId = "bk_1",
            bookingType = BookingType.GEAR,
            status = "confirmed",
            message = "OK",
            itemTitle = null,
            startDate = null,
            endDate = null,
            amount = 0.0
        )
        assertEquals("$0.00", confirmation.formattedAmount)
    }

    // ── StoredCheckout ──────────────────────────────────────────────

    @Test
    fun `StoredCheckout holds session data`() {
        val stored = StoredCheckout("cs_live_abc", BookingType.TIME_BASED)
        assertEquals("cs_live_abc", stored.sessionId)
        assertEquals(BookingType.TIME_BASED, stored.bookingType)
    }

    @Test
    fun `StoredCheckout for GEAR type`() {
        val stored = StoredCheckout("cs_gear_xyz", BookingType.GEAR)
        assertEquals(BookingType.GEAR, stored.bookingType)
    }
}
