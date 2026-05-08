package com.pacedream.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Pins down the contract that [com.shourov.apps.pacedream.feature.bookingdetail.BookingDetailViewModel]
 * relies on to keep raw IOException / "Server error 500: …" / null-payload
 * messages out of the user-facing error banner.  A regression here would
 * re-open the P1 risk where users saw raw network-layer text on booking
 * detail load/cancel failures.
 */
class UserFacingErrorMapperBookingTest {

    private val technicalIndicators = listOf(
        "Exception", "java.net.", "java.io.", "okhttp",
        "retrofit", "kotlinx.serialization", "stacktrace",
    )

    @Test
    fun `unknown host maps to friendly network message`() {
        val msg = UserFacingErrorMapper.forLoadBookings(
            UnknownHostException("Unable to resolve host \"api.pacedream.com\""),
        )
        assertEquals("Please check your internet connection and try again.", msg)
        assertNoTechLeaks(msg)
    }

    @Test
    fun `socket timeout maps to friendly timeout message`() {
        val msg = UserFacingErrorMapper.forLoadBookings(
            SocketTimeoutException("timeout"),
        )
        assertEquals("The connection timed out. Please try again.", msg)
        assertNoTechLeaks(msg)
    }

    @Test
    fun `500 response maps to generic server error`() {
        val msg = UserFacingErrorMapper.forLoadBookings(
            Exception("Server error 500: Internal Server Error"),
        )
        assertEquals("Something went wrong on our end. Please try again.", msg)
        assertNoTechLeaks(msg)
    }

    @Test
    fun `cancel-booking IOException maps via forBookingCancel fallback`() {
        val msg = UserFacingErrorMapper.forBookingCancel(IOException())
        // No message → falls back to the cancel-specific copy.
        assertEquals("We couldn't cancel this booking. Please try again.", msg)
        assertNoTechLeaks(msg)
    }

    @Test
    fun `raw json parse error never leaks to user`() {
        val msg = UserFacingErrorMapper.forLoadBookings(
            Exception("kotlinx.serialization.MissingFieldException: missing field 'id'"),
        )
        assertFalse(
            "MissingFieldException must not leak: was '$msg'",
            msg.contains("MissingFieldException"),
        )
        assertNoTechLeaks(msg)
    }

    private fun assertNoTechLeaks(msg: String) {
        technicalIndicators.forEach { indicator ->
            assertFalse(
                "Technical indicator '$indicator' leaked into user-facing copy: '$msg'",
                msg.contains(indicator),
            )
        }
        // Also pin the "looks user-friendly" heuristic — the message should
        // be a complete English sentence, not a stack-trace fragment.
        assertTrue("Mapped message should end with punctuation: '$msg'", msg.endsWith("."))
    }
}
