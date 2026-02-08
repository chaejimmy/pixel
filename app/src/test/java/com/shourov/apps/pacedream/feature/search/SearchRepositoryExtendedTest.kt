package com.shourov.apps.pacedream.feature.search

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.TokenProvider
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Extended tests for SearchRepository parsing logic.
 * Covers: tolerant JSON parsing across multiple response shapes,
 * hasMore determination, field name variants, price normalization,
 * edge cases, empty/malformed inputs.
 */
class SearchRepositoryExtendedTest {

    private lateinit var repo: SearchRepository

    @Before
    fun setup() {
        val json = Json { ignoreUnknownKeys = true }
        val appConfig = AppConfig()
        val apiClient = ApiClient(
            appConfig = appConfig,
            json = json,
            tokenProvider = object : TokenProvider {
                override fun getAccessToken(): String? = null
                override fun getRefreshToken(): String? = null
            }
        )
        repo = SearchRepository(apiClient = apiClient, appConfig = appConfig, json = json)
    }

    // ── Response shapes ─────────────────────────────────────────────

    @Test
    fun `parseSearchPage supports raw array`() {
        val body = """
            [
              { "id": "1", "title": "Studio", "city": "NYC", "price": "50" }
            ]
        """.trimIndent()
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals(1, page.items.size)
        assertEquals("Studio", page.items[0].title)
    }

    @Test
    fun `parseSearchPage supports listings key`() {
        val body = """
            {
              "listings": [
                { "_id": "a", "title": "Loft", "city": "LA", "price": "100" }
              ]
            }
        """.trimIndent()
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals(1, page.items.size)
        assertEquals("a", page.items[0].id)
    }

    @Test
    fun `parseSearchPage supports data_items wrapper`() {
        val body = """
            {
              "data": {
                "items": [
                  { "id": "b", "title": "Apt", "city": "SF" }
                ]
              }
            }
        """.trimIndent()
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals(1, page.items.size)
        assertEquals("b", page.items[0].id)
    }

    @Test
    fun `parseSearchPage supports data as direct array`() {
        val body = """
            {
              "data": [
                { "id": "c", "title": "Room", "city": "Austin" }
              ]
            }
        """.trimIndent()
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals(1, page.items.size)
    }

    // ── hasMore determination ───────────────────────────────────────

    @Test
    fun `hasMore true when items count equals perPage`() {
        val items = (1..24).map { i ->
            """{ "id": "$i", "title": "Item $i", "city": "City" }"""
        }
        val body = """{ "items": [${items.joinToString(",")}] }"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals(24, page.items.size)
        assertTrue(page.hasMore)
    }

    @Test
    fun `hasMore false when items count less than perPage`() {
        val body = """
            { "items": [
              { "id": "1", "title": "Only one", "city": "NYC" }
            ]}
        """.trimIndent()
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertFalse(page.hasMore)
    }

    @Test
    fun `hasMore false for empty results`() {
        val body = """{ "items": [] }"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals(0, page.items.size)
        assertFalse(page.hasMore)
    }

    // ── Field name variants ─────────────────────────────────────────

    @Test
    fun `parseSearchPage handles _id field`() {
        val body = """[{ "_id": "x1", "title": "Test" }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("x1", page.items[0].id)
    }

    @Test
    fun `parseSearchPage handles listingId field`() {
        val body = """[{ "listingId": "lid1", "title": "Test" }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("lid1", page.items[0].id)
    }

    @Test
    fun `parseSearchPage handles name instead of title`() {
        val body = """[{ "id": "1", "name": "Named Listing" }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("Named Listing", page.items[0].title)
    }

    @Test
    fun `parseSearchPage defaults title to Listing when missing`() {
        val body = """[{ "id": "1" }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("Listing", page.items[0].title)
    }

    @Test
    fun `parseSearchPage handles nested location city`() {
        val body = """[{ "id": "1", "title": "T", "location": { "city": "Chicago" } }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("Chicago", page.items[0].location)
    }

    @Test
    fun `parseSearchPage handles location as string`() {
        val body = """[{ "id": "1", "title": "T", "location": "Denver" }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("Denver", page.items[0].location)
    }

    @Test
    fun `parseSearchPage handles address city`() {
        val body = """[{ "id": "1", "title": "T", "address": { "city": "Portland" } }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("Portland", page.items[0].location)
    }

    // ── Image URL variants ──────────────────────────────────────────

    @Test
    fun `parseSearchPage handles imageUrl field`() {
        val body = """[{ "id": "1", "title": "T", "imageUrl": "https://img/1.jpg" }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("https://img/1.jpg", page.items[0].imageUrl)
    }

    @Test
    fun `parseSearchPage handles thumbnail field`() {
        val body = """[{ "id": "1", "title": "T", "thumbnail": "https://thumb.jpg" }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("https://thumb.jpg", page.items[0].imageUrl)
    }

    @Test
    fun `parseSearchPage handles images array`() {
        val body = """[{ "id": "1", "title": "T", "images": ["https://first.jpg", "https://second.jpg"] }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("https://first.jpg", page.items[0].imageUrl)
    }

    @Test
    fun `parseSearchPage handles galleryImages array`() {
        val body = """[{ "id": "1", "title": "T", "galleryImages": ["https://gallery.jpg"] }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("https://gallery.jpg", page.items[0].imageUrl)
    }

    // ── Rating variants ─────────────────────────────────────────────

    @Test
    fun `parseSearchPage handles avgRating field`() {
        val body = """[{ "id": "1", "title": "T", "avgRating": 4.7 }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals(4.7, page.items[0].rating!!, 0.01)
    }

    @Test
    fun `parseSearchPage handles rating field`() {
        val body = """[{ "id": "1", "title": "T", "rating": 3.5 }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals(3.5, page.items[0].rating!!, 0.01)
    }

    @Test
    fun `parseSearchPage handles null rating`() {
        val body = """[{ "id": "1", "title": "T" }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertNull(page.items[0].rating)
    }

    // ── Price normalization ─────────────────────────────────────────

    @Test
    fun `parseSearchPage normalizes numeric price with dollar sign`() {
        val body = """[{ "id": "1", "title": "T", "price": "50" }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("$50", page.items[0].priceText)
    }

    @Test
    fun `parseSearchPage preserves priceText as-is`() {
        val body = """[{ "id": "1", "title": "T", "priceText": "$120/hr" }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("$120/hr", page.items[0].priceText)
    }

    @Test
    fun `parseSearchPage handles nested price amount`() {
        val body = """[{ "id": "1", "title": "T", "price": { "amount": "75" } }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("$75", page.items[0].priceText)
    }

    @Test
    fun `parseSearchPage handles nested pricing_price`() {
        val body = """[{ "id": "1", "title": "T", "pricing": { "price": "200" } }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals("$200", page.items[0].priceText)
    }

    // ── Edge cases ──────────────────────────────────────────────────

    @Test
    fun `parseSearchPage skips items without id`() {
        val body = """[{ "title": "No ID" }, { "id": "1", "title": "Has ID" }]"""
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals(1, page.items.size)
        assertEquals("1", page.items[0].id)
    }

    @Test
    fun `parseSearchPage handles malformed JSON gracefully`() {
        val body = "not valid json"
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals(0, page.items.size)
        assertFalse(page.hasMore)
    }

    @Test
    fun `parseSearchPage handles empty object`() {
        val body = "{}"
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals(0, page.items.size)
    }

    @Test
    fun `parseSearchPage handles multiple items with mixed fields`() {
        val body = """
            {
              "data": {
                "listings": [
                  { "_id": "1", "title": "Studio", "city": "NYC", "rating": 4.8, "price": "120" },
                  { "id": "2", "name": "Loft", "location": "LA", "avgRating": 4.2, "priceText": "$80/night" },
                  { "listingId": "3", "title": "Room", "address": { "city": "SF" }, "thumbnail": "https://t.jpg" }
                ]
              }
            }
        """.trimIndent()
        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals(3, page.items.size)
        assertEquals("1", page.items[0].id)
        assertEquals("2", page.items[1].id)
        assertEquals("3", page.items[2].id)
        assertEquals("NYC", page.items[0].location)
        assertEquals("LA", page.items[1].location)
        assertEquals("SF", page.items[2].location)
    }
}
