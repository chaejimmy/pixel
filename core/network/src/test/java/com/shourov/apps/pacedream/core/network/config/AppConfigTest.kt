package com.shourov.apps.pacedream.core.network.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for AppConfig URL normalization, building, and constants.
 */
class AppConfigTest {

    private val config = AppConfig()

    // ── apiBaseUrl normalization ─────────────────────────────────────

    @Test
    fun `apiBaseUrl ends with v1`() {
        val url = config.apiBaseUrl.toString()
        assertTrue("apiBaseUrl should contain /v1/: $url", url.contains("/v1/") || url.endsWith("/v1"))
    }

    @Test
    fun `apiBaseUrl uses https scheme`() {
        val url = config.apiBaseUrl
        assertEquals("https", url.scheme)
    }

    // ── frontendBaseUrl normalization ────────────────────────────────

    @Test
    fun `frontendBaseUrl does not contain v1`() {
        val url = config.frontendBaseUrl.toString()
        assertTrue("frontendBaseUrl should not contain /v1: $url", !url.contains("/v1"))
    }

    @Test
    fun `frontendBaseUrl uses https scheme`() {
        assertEquals("https", config.frontendBaseUrl.scheme)
    }

    // ── buildApiUrl ─────────────────────────────────────────────────

    @Test
    fun `buildApiUrl appends path segments`() {
        val url = config.buildApiUrl("auth", "refresh-token")
        val path = url.encodedPath
        assertTrue("Path should contain /v1/auth/refresh-token: $path",
            path.contains("/v1/auth/refresh-token"))
    }

    @Test
    fun `buildApiUrl handles single segment`() {
        val url = config.buildApiUrl("listings")
        val path = url.encodedPath
        assertTrue("Path should contain /v1/listings: $path",
            path.contains("/v1/listings"))
    }

    @Test
    fun `buildApiUrl handles segments with slashes`() {
        val url = config.buildApiUrl("inbox/threads")
        val path = url.encodedPath
        assertTrue("Path should contain /v1/inbox/threads: $path",
            path.contains("/v1/inbox/threads"))
    }

    @Test
    fun `buildApiUrl handles multiple deep segments`() {
        val url = config.buildApiUrl("properties", "bookings", "timebased", "success", "checkout")
        val path = url.encodedPath
        assertTrue("Path should contain booking checkout path: $path",
            path.contains("/v1/properties/bookings/timebased/success/checkout"))
    }

    // ── buildFrontendUrl ────────────────────────────────────────────

    @Test
    fun `buildFrontendUrl appends path without v1`() {
        val url = config.buildFrontendUrl("api", "search")
        val path = url.encodedPath
        assertTrue("Path should contain /api/search: $path", path.contains("/api/search"))
        assertTrue("Path should not contain /v1/: $path", !path.contains("/v1/"))
    }

    @Test
    fun `buildFrontendUrl proxy path works`() {
        val url = config.buildFrontendUrl("api", "proxy", "auth", "refresh-token")
        val path = url.encodedPath
        assertTrue("Path should contain /api/proxy/auth/refresh-token: $path",
            path.contains("/api/proxy/auth/refresh-token"))
    }

    // ── buildApiUrlWithQuery ────────────────────────────────────────

    @Test
    fun `buildApiUrlWithQuery adds query parameters`() {
        val url = config.buildApiUrlWithQuery(
            "search",
            queryParams = mapOf("q" to "studio", "city" to "NYC", "page" to "1")
        )
        assertEquals("studio", url.queryParameter("q"))
        assertEquals("NYC", url.queryParameter("city"))
        assertEquals("1", url.queryParameter("page"))
    }

    @Test
    fun `buildApiUrlWithQuery skips null values`() {
        val url = config.buildApiUrlWithQuery(
            "listings",
            queryParams = mapOf("shareType" to "USE", "category" to null)
        )
        assertEquals("USE", url.queryParameter("shareType"))
        assertEquals(null, url.queryParameter("category"))
    }

    @Test
    fun `buildApiUrlWithQuery with empty params produces clean URL`() {
        val url = config.buildApiUrlWithQuery("inbox", "threads", queryParams = emptyMap())
        val path = url.encodedPath
        assertTrue("Path should contain /v1/inbox/threads: $path",
            path.contains("/v1/inbox/threads"))
        assertTrue("Should have no query params", url.querySize == 0)
    }

    // ── Constants ───────────────────────────────────────────────────

    @Test
    fun `timeout constants match iOS parity`() {
        assertEquals(30L, AppConfig.REQUEST_TIMEOUT_SECONDS)
        assertEquals(60L, AppConfig.RESOURCE_TIMEOUT_SECONDS)
        assertEquals(60L, AppConfig.READ_TIMEOUT_SECONDS)
    }

    @Test
    fun `retry constants match iOS parity`() {
        assertEquals(2, AppConfig.MAX_RETRY_ATTEMPTS)
        assertEquals(listOf(500L, 1000L), AppConfig.RETRY_BACKOFF_DELAYS)
    }

    // ── Auth0 configuration ─────────────────────────────────────────

    @Test
    fun `auth0Scheme is pacedream`() {
        assertEquals("pacedream", config.auth0Scheme)
    }

    @Test
    fun `auth0Domain returns non-empty string`() {
        assertTrue("auth0Domain should not be blank", config.auth0Domain.isNotBlank())
    }
}
