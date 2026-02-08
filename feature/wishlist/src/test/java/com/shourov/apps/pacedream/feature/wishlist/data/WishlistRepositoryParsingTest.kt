package com.shourov.apps.pacedream.feature.wishlist.data

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.TokenProvider
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for WishlistRepository parsing logic.
 * Covers: parseWishlistsEndpointResponse (all response shapes),
 * parseToggleResponse, isSuccessResponse, field name variants.
 */
class WishlistRepositoryParsingTest {

    private lateinit var repo: WishlistRepository

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
        repo = WishlistRepository(apiClient = apiClient, appConfig = appConfig, json = json)
    }

    // ── Wishlist response shapes ────────────────────────────────────

    @Test
    fun `parseWishlistResponse handles raw array`() {
        val body = """
            [
              { "_id": "w1", "title": "Cool Space", "price": 50.0, "rating": 4.5 }
            ]
        """.trimIndent()
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(1, items.size)
        assertEquals("w1", items[0].id)
        assertEquals("Cool Space", items[0].title)
    }

    @Test
    fun `parseWishlistResponse handles data array`() {
        val body = """
            {
              "data": [
                { "id": "w2", "title": "Loft", "imageUrl": "https://img.jpg" }
              ]
            }
        """.trimIndent()
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(1, items.size)
        assertEquals("w2", items[0].id)
    }

    @Test
    fun `parseWishlistResponse handles wishlists array with nested properties`() {
        val body = """
            {
              "wishlists": [
                {
                  "id": "wl1",
                  "properties": [
                    { "_id": "p1", "title": "Studio A", "image": "https://a.jpg", "price": 30.0 },
                    { "_id": "p2", "title": "Studio B", "image": "https://b.jpg", "price": 40.0 }
                  ]
                }
              ]
            }
        """.trimIndent()
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(2, items.size)
        assertEquals("p1", items[0].id)
        assertEquals("p2", items[1].id)
    }

    @Test
    fun `parseWishlistResponse handles wishlists with items nested`() {
        val body = """
            {
              "wishlists": [
                {
                  "id": "wl1",
                  "items": [
                    { "id": "i1", "title": "Gear A", "type": "gear" }
                  ]
                }
              ]
            }
        """.trimIndent()
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(1, items.size)
        assertEquals("i1", items[0].id)
    }

    @Test
    fun `parseWishlistResponse handles wishlists with listings nested`() {
        val body = """
            {
              "wishlists": [
                {
                  "id": "wl1",
                  "listings": [
                    { "id": "l1", "title": "Room X" }
                  ]
                }
              ]
            }
        """.trimIndent()
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(1, items.size)
        assertEquals("l1", items[0].id)
    }

    @Test
    fun `parseWishlistResponse handles data_wishlists nested`() {
        val body = """
            {
              "data": {
                "wishlists": [
                  { "id": "w3", "title": "Saved Space" }
                ]
              }
            }
        """.trimIndent()
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(1, items.size)
        assertEquals("w3", items[0].id)
    }

    @Test
    fun `parseWishlistResponse handles items wrapper`() {
        val body = """
            {
              "items": [
                { "id": "w4", "title": "Item Space", "location": "NYC" }
              ]
            }
        """.trimIndent()
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(1, items.size)
        assertEquals("NYC", items[0].location)
    }

    @Test
    fun `parseWishlistResponse handles empty response`() {
        val body = """{ "data": [] }"""
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(0, items.size)
    }

    // ── Field name variants ─────────────────────────────────────────

    @Test
    fun `parseWishlistResponse handles name instead of title`() {
        val body = """[{ "id": "1", "name": "Named Item" }]"""
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals("Named Item", items[0].title)
    }

    @Test
    fun `parseWishlistResponse handles listing_id`() {
        val body = """[{ "id": "1", "listing_id": "lid1", "title": "T" }]"""
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals("lid1", items[0].listingId)
    }

    @Test
    fun `parseWishlistResponse handles listingId camelCase`() {
        val body = """[{ "id": "1", "listingId": "lid2", "title": "T" }]"""
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals("lid2", items[0].listingId)
    }

    @Test
    fun `parseWishlistResponse handles images array for imageUrl`() {
        val body = """[{ "id": "1", "title": "T", "images": ["https://first.jpg"] }]"""
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals("https://first.jpg", items[0].imageUrl)
    }

    @Test
    fun `parseWishlistResponse handles thumbnail for imageUrl`() {
        val body = """[{ "id": "1", "title": "T", "thumbnail": "https://thumb.jpg" }]"""
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals("https://thumb.jpg", items[0].imageUrl)
    }

    @Test
    fun `parseWishlistResponse handles amount for price`() {
        val body = """[{ "id": "1", "title": "T", "amount": 25.0 }]"""
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(25.0, items[0].price!!, 0.01)
    }

    @Test
    fun `parseWishlistResponse handles address for location`() {
        val body = """[{ "id": "1", "title": "T", "address": "123 Main St" }]"""
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals("123 Main St", items[0].location)
    }

    // ── Item type parsing ───────────────────────────────────────────

    @Test
    fun `parseWishlistResponse detects TIME_BASED from type field`() {
        val body = """[{ "id": "1", "title": "T", "type": "time-based" }]"""
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(com.shourov.apps.pacedream.feature.wishlist.model.WishlistItemType.TIME_BASED, items[0].itemType)
    }

    @Test
    fun `parseWishlistResponse detects HOURLY_GEAR from shareType borrow`() {
        val body = """[{ "id": "1", "title": "T", "shareType": "borrow" }]"""
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(com.shourov.apps.pacedream.feature.wishlist.model.WishlistItemType.HOURLY_GEAR, items[0].itemType)
    }

    @Test
    fun `parseWishlistResponse detects SPLIT_STAY from category split`() {
        val body = """[{ "id": "1", "title": "T", "category": "split" }]"""
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(com.shourov.apps.pacedream.feature.wishlist.model.WishlistItemType.SPLIT_STAY, items[0].itemType)
    }

    @Test
    fun `parseWishlistResponse defaults to TIME_BASED for unknown type`() {
        val body = """[{ "id": "1", "title": "T" }]"""
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(com.shourov.apps.pacedream.feature.wishlist.model.WishlistItemType.TIME_BASED, items[0].itemType)
    }

    // ── Toggle response parsing ─────────────────────────────────────

    @Test
    fun `parseToggleResponse success with liked true`() {
        val body = """{ "status": true, "data": { "liked": true, "message": "Added to wishlist" } }"""
        val result = repo.parseToggleResponseForTest(body)
        assertTrue(result.success)
        assertTrue(result.liked)
        assertEquals("Added to wishlist", result.message)
    }

    @Test
    fun `parseToggleResponse success with liked false`() {
        val body = """{ "success": true, "data": { "liked": false, "message": "Removed" } }"""
        val result = repo.parseToggleResponseForTest(body)
        assertTrue(result.success)
        assertFalse(result.liked)
        assertEquals("Removed", result.message)
    }

    @Test
    fun `parseToggleResponse handles ok field for liked`() {
        val body = """{ "status": true, "data": { "ok": true } }"""
        val result = repo.parseToggleResponseForTest(body)
        assertTrue(result.liked)
    }

    @Test
    fun `parseToggleResponse failure`() {
        val body = """{ "status": false, "data": {} }"""
        val result = repo.parseToggleResponseForTest(body)
        assertFalse(result.success)
    }

    @Test
    fun `parseToggleResponse with no data defaults liked to status`() {
        val body = """{ "status": true }"""
        val result = repo.parseToggleResponseForTest(body)
        assertTrue(result.success)
        assertTrue(result.liked)
        assertNull(result.message)
    }

    // ── Success response detection ──────────────────────────────────

    @Test
    fun `isSuccessResponse detects success true`() {
        assertTrue(repo.isSuccessResponseForTest("""{ "success": true }"""))
    }

    @Test
    fun `isSuccessResponse detects status true`() {
        assertTrue(repo.isSuccessResponseForTest("""{ "status": true }"""))
    }

    @Test
    fun `isSuccessResponse detects ok true`() {
        assertTrue(repo.isSuccessResponseForTest("""{ "ok": true }"""))
    }

    @Test
    fun `isSuccessResponse returns false for failure`() {
        assertFalse(repo.isSuccessResponseForTest("""{ "success": false }"""))
    }

    @Test
    fun `isSuccessResponse returns false for empty body`() {
        assertFalse(repo.isSuccessResponseForTest("{}"))
    }

    @Test
    fun `isSuccessResponse returns false for malformed JSON`() {
        assertFalse(repo.isSuccessResponseForTest("not-json"))
    }

    // ── Edge cases ──────────────────────────────────────────────────

    @Test
    fun `parseWishlistResponse skips malformed items`() {
        val body = """
            [
              { "id": "1", "title": "Good Item" },
              "not-an-object",
              { "id": "2", "title": "Another Good Item" }
            ]
        """.trimIndent()
        val items = repo.parseWishlistResponseForTest(body)
        assertTrue(items.size >= 2)
    }

    @Test
    fun `parseWishlistResponse handles empty array`() {
        val body = "[]"
        val items = repo.parseWishlistResponseForTest(body)
        assertEquals(0, items.size)
    }
}
