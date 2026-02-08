package com.shourov.apps.pacedream.feature.webflow.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for BookingRepository parsing logic.
 * Since BookingRepository requires TokenStorage (needs Android Context),
 * we test the JSON parsing patterns and data models directly.
 *
 * Covers: checkout response parsing patterns, booking confirmation parsing patterns,
 * session ID extraction, field name variants used by the repository.
 */
class BookingRepositoryExtendedTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Session ID extraction from URL ──────────────────────────────

    @Test
    fun `extractSessionId from URL with session_id query param`() {
        val url = "https://checkout.stripe.com/c/pay/cs_test_a1b2?session_id=cs_test_a1b2#fidkdWxOYHw"
        val sessionId = extractSessionIdFromUrl(url)
        assertEquals("cs_test_a1b2", sessionId)
    }

    @Test
    fun `extractSessionId returns null when no session_id`() {
        val url = "https://checkout.stripe.com/c/pay/cs_test_abc"
        val sessionId = extractSessionIdFromUrl(url)
        assertNull(sessionId)
    }

    @Test
    fun `extractSessionId handles session_id as second param`() {
        val url = "https://example.com/checkout?foo=bar&session_id=ses_xyz"
        val sessionId = extractSessionIdFromUrl(url)
        assertEquals("ses_xyz", sessionId)
    }

    @Test
    fun `extractSessionId handles session_id with special chars`() {
        val url = "https://example.com?session_id=cs_live_a1B2c3D4e5F6"
        val sessionId = extractSessionIdFromUrl(url)
        assertEquals("cs_live_a1B2c3D4e5F6", sessionId)
    }

    // ── Checkout response parsing patterns ──────────────────────────

    @Test
    fun `parseCheckout handles data wrapper with checkoutUrl`() {
        val body = """
            {
              "status": true,
              "data": {
                "checkoutUrl": "https://checkout.stripe.com/pay/cs_test_123?session_id=cs_test_123"
              }
            }
        """.trimIndent()
        val result = parseCheckoutFromBody(body)
        assertNotNull(result.checkoutUrl)
        assertTrue(result.checkoutUrl!!.contains("stripe.com"))
        assertEquals("cs_test_123", result.sessionId)
    }

    @Test
    fun `parseCheckout handles checkout_url snake_case`() {
        val body = """{ "data": { "checkout_url": "https://stripe.com/pay/abc?session_id=abc" } }"""
        val result = parseCheckoutFromBody(body)
        assertEquals("https://stripe.com/pay/abc?session_id=abc", result.checkoutUrl)
    }

    @Test
    fun `parseCheckout handles url field`() {
        val body = """{ "data": { "url": "https://stripe.com/sessions/xyz" } }"""
        val result = parseCheckoutFromBody(body)
        assertEquals("https://stripe.com/sessions/xyz", result.checkoutUrl)
    }

    @Test
    fun `parseCheckout handles flat response without data wrapper`() {
        val body = """{ "checkoutUrl": "https://stripe.com/test" }"""
        val result = parseCheckoutFromBody(body)
        assertEquals("https://stripe.com/test", result.checkoutUrl)
    }

    @Test
    fun `parseCheckout handles missing URL fields`() {
        val body = """{ "data": { "bookingId": "bk_123" } }"""
        val result = parseCheckoutFromBody(body)
        assertNull(result.checkoutUrl)
        assertNull(result.sessionId)
    }

    // ── Booking confirmation parsing patterns ───────────────────────

    @Test
    fun `parseConfirmation handles bookingId`() {
        val body = """
            {
              "data": {
                "bookingId": "bk_abc123",
                "status": "confirmed",
                "message": "Booking confirmed!",
                "itemTitle": "Study Room A",
                "startDate": "2024-06-01T10:00:00Z",
                "endDate": "2024-06-01T12:00:00Z",
                "amount": "25.00"
              }
            }
        """.trimIndent()
        val result = parseConfirmationFromBody(body)
        assertEquals("bk_abc123", result.bookingId)
        assertEquals("confirmed", result.status)
        assertEquals("Booking confirmed!", result.message)
        assertEquals("Study Room A", result.itemTitle)
        assertEquals("2024-06-01T10:00:00Z", result.startDate)
        assertEquals("2024-06-01T12:00:00Z", result.endDate)
        assertEquals(25.0, result.amount!!, 0.01)
    }

    @Test
    fun `parseConfirmation handles _id variant`() {
        val body = """{ "data": { "_id": "bk_xyz" } }"""
        val result = parseConfirmationFromBody(body)
        assertEquals("bk_xyz", result.bookingId)
    }

    @Test
    fun `parseConfirmation handles id variant`() {
        val body = """{ "data": { "id": "bk_simple" } }"""
        val result = parseConfirmationFromBody(body)
        assertEquals("bk_simple", result.bookingId)
    }

    @Test
    fun `parseConfirmation handles start_date and end_date snake_case`() {
        val body = """{ "data": { "start_date": "2024-06-01", "end_date": "2024-06-02" } }"""
        val result = parseConfirmationFromBody(body)
        assertEquals("2024-06-01", result.startDate)
        assertEquals("2024-06-02", result.endDate)
    }

    @Test
    fun `parseConfirmation handles startTime and endTime as fallback`() {
        val body = """{ "data": { "startTime": "10:00", "endTime": "12:00" } }"""
        val result = parseConfirmationFromBody(body)
        assertEquals("10:00", result.startDate)
        assertEquals("12:00", result.endDate)
    }

    @Test
    fun `parseConfirmation handles total as amount fallback`() {
        val body = """{ "data": { "total": "45.00" } }"""
        val result = parseConfirmationFromBody(body)
        assertEquals(45.0, result.amount!!, 0.01)
    }

    @Test
    fun `parseConfirmation handles title variant`() {
        val body = """{ "data": { "title": "Desk Space" } }"""
        val result = parseConfirmationFromBody(body)
        assertEquals("Desk Space", result.itemTitle)
    }

    @Test
    fun `parseConfirmation handles listingTitle variant`() {
        val body = """{ "data": { "listingTitle": "Camera Kit" } }"""
        val result = parseConfirmationFromBody(body)
        assertEquals("Camera Kit", result.itemTitle)
    }

    @Test
    fun `parseConfirmation defaults status to confirmed`() {
        val body = """{ "data": {} }"""
        val result = parseConfirmationFromBody(body)
        assertEquals("confirmed", result.status)
    }

    @Test
    fun `parseConfirmation defaults message`() {
        val body = """{ "data": {} }"""
        val result = parseConfirmationFromBody(body)
        assertEquals("Booking confirmed successfully", result.message)
    }

    @Test
    fun `parseConfirmation defaults bookingId to empty`() {
        val body = """{ "data": {} }"""
        val result = parseConfirmationFromBody(body)
        assertEquals("", result.bookingId)
    }

    // ── Helpers (replicating repo parsing logic for testing) ────────

    private fun extractSessionIdFromUrl(url: String): String? {
        val regex = "[?&]session_id=([^&]+)".toRegex()
        return regex.find(url)?.groupValues?.getOrNull(1)
    }

    private fun parseCheckoutFromBody(responseBody: String): CheckoutResult {
        val jsonElement = json.parseToJsonElement(responseBody)
        val jsonObject = jsonElement.jsonObject
        val data = jsonObject["data"]?.jsonObject ?: jsonObject

        val checkoutUrl = data["checkoutUrl"]?.jsonPrimitive?.content
            ?: data["checkout_url"]?.jsonPrimitive?.content
            ?: data["url"]?.jsonPrimitive?.content

        val sessionId = checkoutUrl?.let { extractSessionIdFromUrl(it) }

        return CheckoutResult(
            checkoutUrl = checkoutUrl,
            sessionId = sessionId,
            bookingType = BookingType.TIME_BASED
        )
    }

    private fun parseConfirmationFromBody(responseBody: String): BookingConfirmation {
        val jsonElement = json.parseToJsonElement(responseBody)
        val jsonObject = jsonElement.jsonObject
        val data = jsonObject["data"]?.jsonObject ?: jsonObject

        return BookingConfirmation(
            bookingId = data["bookingId"]?.jsonPrimitive?.content
                ?: data["_id"]?.jsonPrimitive?.content
                ?: data["id"]?.jsonPrimitive?.content
                ?: "",
            bookingType = BookingType.TIME_BASED,
            status = data["status"]?.jsonPrimitive?.content ?: "confirmed",
            message = data["message"]?.jsonPrimitive?.content ?: "Booking confirmed successfully",
            itemTitle = data["itemTitle"]?.jsonPrimitive?.content
                ?: data["title"]?.jsonPrimitive?.content
                ?: data["listingTitle"]?.jsonPrimitive?.content,
            startDate = data["startDate"]?.jsonPrimitive?.content
                ?: data["start_date"]?.jsonPrimitive?.content
                ?: data["startTime"]?.jsonPrimitive?.content,
            endDate = data["endDate"]?.jsonPrimitive?.content
                ?: data["end_date"]?.jsonPrimitive?.content
                ?: data["endTime"]?.jsonPrimitive?.content,
            amount = data["amount"]?.jsonPrimitive?.content?.toDoubleOrNull()
                ?: data["total"]?.jsonPrimitive?.content?.toDoubleOrNull()
        )
    }
}
