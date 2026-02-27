package com.shourov.apps.pacedream.core.network.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for ApiError sealed class variants, default messages,
 * and the RateLimited.friendlyMessage() helper.
 */
class ApiErrorTest {

    // ── Default messages ────────────────────────────────────────────

    @Test
    fun `Unauthorized has default message`() {
        val error = ApiError.Unauthorized()
        assertEquals("Unauthorized", error.message)
    }

    @Test
    fun `Forbidden has default message`() {
        val error = ApiError.Forbidden()
        assertEquals("Access forbidden", error.message)
    }

    @Test
    fun `NotFound has default message`() {
        val error = ApiError.NotFound()
        assertEquals("Resource not found", error.message)
    }

    @Test
    fun `Timeout has default message`() {
        val error = ApiError.Timeout()
        assertEquals("Network timeout. Please try again.", error.message)
    }

    @Test
    fun `NetworkError has default message`() {
        val error = ApiError.NetworkError()
        assertEquals("Network connection error. Please check your connection.", error.message)
    }

    @Test
    fun `HtmlResponse has default message`() {
        val error = ApiError.HtmlResponse()
        assertEquals("Received unexpected response. Please try again later.", error.message)
    }

    @Test
    fun `ServiceUnavailable has default message`() {
        val error = ApiError.ServiceUnavailable()
        assertEquals("Service is temporarily unavailable. Please try again in a minute.", error.message)
    }

    @Test
    fun `ServerError has default message`() {
        val error = ApiError.ServerError()
        assertEquals("Something went wrong. Please try again.", error.message)
    }

    @Test
    fun `DecodingError has default message`() {
        val error = ApiError.DecodingError()
        assertEquals("Failed to process response.", error.message)
    }

    @Test
    fun `Unknown has default message`() {
        val error = ApiError.Unknown()
        assertEquals("An unexpected error occurred.", error.message)
    }

    // ── Custom messages ─────────────────────────────────────────────

    @Test
    fun `Unauthorized accepts custom message`() {
        val error = ApiError.Unauthorized("Token expired")
        assertEquals("Token expired", error.message)
    }

    @Test
    fun `ServerError accepts custom message`() {
        val error = ApiError.ServerError("Database error")
        assertEquals("Database error", error.message)
    }

    // ── RateLimited.friendlyMessage() ───────────────────────────────

    @Test
    fun `RateLimited friendlyMessage with retryAfterSeconds`() {
        val error = ApiError.RateLimited(retryAfterSeconds = 120)
        assertEquals("Too many requests. Please try again in 2 minutes.", error.friendlyMessage())
    }

    @Test
    fun `RateLimited friendlyMessage with small retryAfterSeconds rounds up to 1 minute`() {
        val error = ApiError.RateLimited(retryAfterSeconds = 10)
        assertEquals("Too many requests. Please try again in 1 minute.", error.friendlyMessage())
    }

    @Test
    fun `RateLimited friendlyMessage singular minute`() {
        val error = ApiError.RateLimited(retryAfterSeconds = 60)
        assertEquals("Too many requests. Please try again in 1 minute.", error.friendlyMessage())
    }

    @Test
    fun `RateLimited friendlyMessage without retryAfterSeconds uses default message`() {
        val error = ApiError.RateLimited()
        assertEquals("Too many requests. Please slow down.", error.friendlyMessage())
    }

    @Test
    fun `RateLimited friendlyMessage with null retryAfterSeconds uses default`() {
        val error = ApiError.RateLimited(retryAfterSeconds = null)
        assertEquals("Too many requests. Please slow down.", error.friendlyMessage())
    }

    // ── DecodingError underlyingCause ───────────────────────────────

    @Test
    fun `DecodingError preserves underlying cause`() {
        val cause = RuntimeException("parse error")
        val error = ApiError.DecodingError("bad json", cause)
        assertEquals(cause, error.underlyingCause)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `DecodingError without cause has null cause`() {
        val error = ApiError.DecodingError()
        assertNull(error.underlyingCause)
    }

    // ── Unknown extra fields ────────────────────────────────────────

    @Test
    fun `Unknown preserves statusCode and cause`() {
        val cause = IllegalStateException("oops")
        val error = ApiError.Unknown("server exploded", statusCode = 500, underlyingCause = cause)
        assertEquals(500, error.statusCode)
        assertEquals(cause, error.underlyingCause)
        assertEquals(cause, error.cause)
        assertEquals("server exploded", error.message)
    }

    // ── All errors extend Exception ─────────────────────────────────

    @Test
    fun `all ApiError types are Throwable`() {
        val errors: List<ApiError> = listOf(
            ApiError.Unauthorized(),
            ApiError.Forbidden(),
            ApiError.NotFound(),
            ApiError.RateLimited(),
            ApiError.ServiceUnavailable(),
            ApiError.Timeout(),
            ApiError.NetworkError(),
            ApiError.HtmlResponse(),
            ApiError.ServerError(),
            ApiError.DecodingError(),
            ApiError.Unknown()
        )
        errors.forEach { error ->
            assertTrue("${error::class.simpleName} should be Throwable", error is Throwable)
        }
    }

    // ── ErrorResponse.extractMessage() ──────────────────────────────

    @Test
    fun `ErrorResponse extracts message field`() {
        val response = ErrorResponse(message = "field required")
        assertEquals("field required", response.extractMessage())
    }

    @Test
    fun `ErrorResponse extracts error field when message is null`() {
        val response = ErrorResponse(error = "invalid request")
        assertEquals("invalid request", response.extractMessage())
    }

    @Test
    fun `ErrorResponse extracts from errors list when message and error are null`() {
        val response = ErrorResponse(errors = listOf("error1", "error2"))
        assertEquals("error1", response.extractMessage())
    }

    @Test
    fun `ErrorResponse extracts from data when all top-level are null`() {
        val response = ErrorResponse(
            data = ErrorResponse.ErrorData(message = "nested message")
        )
        assertEquals("nested message", response.extractMessage())
    }

    @Test
    fun `ErrorResponse returns null when everything is null`() {
        val response = ErrorResponse()
        assertNull(response.extractMessage())
    }

    // ── ApiEnvelope ─────────────────────────────────────────────────

    @Test
    fun `ApiEnvelope isSuccessful when success is true`() {
        val envelope = ApiEnvelope<String>(success = true)
        assertTrue(envelope.isSuccessful)
    }

    @Test
    fun `ApiEnvelope isSuccessful when status is true`() {
        val envelope = ApiEnvelope<String>(status = true)
        assertTrue(envelope.isSuccessful)
    }

    @Test
    fun `ApiEnvelope is not successful when both null`() {
        val envelope = ApiEnvelope<String>()
        assertFalse(envelope.isSuccessful)
    }
}
