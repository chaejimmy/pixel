package com.shourov.apps.pacedream.feature.homefeed

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.TokenProvider
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Extended tests for HomeFeedRepository.parseListingsToCards().
 * Covers: all response shapes, field name variants, image URL variants,
 * price normalization, edge cases, empty/malformed inputs.
 */
class HomeFeedRepositoryExtendedTest {

    private lateinit var repo: HomeFeedRepository

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
        repo = HomeFeedRepository(apiClient = apiClient, appConfig = appConfig, json = json)
    }

    // ── Response shapes ─────────────────────────────────────────────

    @Test
    fun `parseListingsToCards supports listings key`() {
        val body = """
            {
              "listings": [
                { "id": "a", "title": "Loft", "city": "LA", "price": "80" }
              ]
            }
        """.trimIndent()
        val cards = repo.parseListingsToCards(body)
        assertEquals(1, cards.size)
        assertEquals("a", cards[0].id)
    }

    @Test
    fun `parseListingsToCards supports items key`() {
        val body = """
            {
              "items": [
                { "_id": "b", "title": "Studio", "city": "NYC" }
              ]
            }
        """.trimIndent()
        val cards = repo.parseListingsToCards(body)
        assertEquals(1, cards.size)
        assertEquals("b", cards[0].id)
    }

    @Test
    fun `parseListingsToCards supports data as array`() {
        val body = """
            {
              "data": [
                { "id": "c", "title": "Room", "city": "SF" }
              ]
            }
        """.trimIndent()
        val cards = repo.parseListingsToCards(body)
        assertEquals(1, cards.size)
    }

    @Test
    fun `parseListingsToCards supports data_listings nested`() {
        val body = """
            {
              "data": {
                "listings": [
                  { "id": "d", "title": "Suite", "city": "Miami" }
                ]
              }
            }
        """.trimIndent()
        val cards = repo.parseListingsToCards(body)
        assertEquals(1, cards.size)
        assertEquals("d", cards[0].id)
    }

    @Test
    fun `parseListingsToCards supports data_items nested`() {
        val body = """
            {
              "data": {
                "items": [
                  { "id": "e", "title": "Pad", "city": "Boston" }
                ]
              }
            }
        """.trimIndent()
        val cards = repo.parseListingsToCards(body)
        assertEquals(1, cards.size)
    }

    @Test
    fun `parseListingsToCards supports deeply nested data_data array`() {
        val body = """
            {
              "data": {
                "data": [
                  { "id": "f", "title": "Deep", "city": "Austin" }
                ]
              }
            }
        """.trimIndent()
        val cards = repo.parseListingsToCards(body)
        assertEquals(1, cards.size)
    }

    // ── Field name variants ─────────────────────────────────────────

    @Test
    fun `parseListingsToCards handles name field instead of title`() {
        val body = """[{ "id": "1", "name": "Named Listing" }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("Named Listing", cards[0].title)
    }

    @Test
    fun `parseListingsToCards defaults title to Listing`() {
        val body = """[{ "id": "1" }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("Listing", cards[0].title)
    }

    @Test
    fun `parseListingsToCards handles nested location city`() {
        val body = """[{ "id": "1", "title": "T", "location": { "city": "Denver" } }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("Denver", cards[0].location)
    }

    @Test
    fun `parseListingsToCards handles location as string`() {
        val body = """[{ "id": "1", "title": "T", "location": "Portland" }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("Portland", cards[0].location)
    }

    @Test
    fun `parseListingsToCards handles address city`() {
        val body = """[{ "id": "1", "title": "T", "address": { "city": "Seattle" } }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("Seattle", cards[0].location)
    }

    // ── Image URL variants ──────────────────────────────────────────

    @Test
    fun `parseListingsToCards handles image field`() {
        val body = """[{ "id": "1", "title": "T", "image": "https://img.jpg" }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("https://img.jpg", cards[0].imageUrl)
    }

    @Test
    fun `parseListingsToCards handles imageUrl field`() {
        val body = """[{ "id": "1", "title": "T", "imageUrl": "https://url.jpg" }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("https://url.jpg", cards[0].imageUrl)
    }

    @Test
    fun `parseListingsToCards handles thumbnail field`() {
        val body = """[{ "id": "1", "title": "T", "thumbnail": "https://thumb.jpg" }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("https://thumb.jpg", cards[0].imageUrl)
    }

    @Test
    fun `parseListingsToCards handles images array first element`() {
        val body = """[{ "id": "1", "title": "T", "images": ["https://first.jpg", "https://second.jpg"] }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("https://first.jpg", cards[0].imageUrl)
    }

    @Test
    fun `parseListingsToCards handles gallery images array`() {
        val body = """[{ "id": "1", "title": "T", "gallery": { "images": ["https://gal.jpg"] } }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("https://gal.jpg", cards[0].imageUrl)
    }

    @Test
    fun `parseListingsToCards handles galleryImages array`() {
        val body = """[{ "id": "1", "title": "T", "galleryImages": ["https://galimg.jpg"] }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("https://galimg.jpg", cards[0].imageUrl)
    }

    // ── Rating variants ─────────────────────────────────────────────

    @Test
    fun `parseListingsToCards handles rating field`() {
        val body = """[{ "id": "1", "title": "T", "rating": 4.5 }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals(4.5, cards[0].rating!!, 0.01)
    }

    @Test
    fun `parseListingsToCards handles avgRating field`() {
        val body = """[{ "id": "1", "title": "T", "avgRating": 4.2 }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals(4.2, cards[0].rating!!, 0.01)
    }

    @Test
    fun `parseListingsToCards handles null rating`() {
        val body = """[{ "id": "1", "title": "T" }]"""
        val cards = repo.parseListingsToCards(body)
        assertNull(cards[0].rating)
    }

    // ── Price normalization ─────────────────────────────────────────

    @Test
    fun `parseListingsToCards normalizes numeric price`() {
        val body = """[{ "id": "1", "title": "T", "price": "75" }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("$75", cards[0].priceText)
    }

    @Test
    fun `parseListingsToCards preserves priceText as-is`() {
        val body = """[{ "id": "1", "title": "T", "priceText": "$50/hr" }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("$50/hr", cards[0].priceText)
    }

    @Test
    fun `parseListingsToCards handles nested price amount`() {
        val body = """[{ "id": "1", "title": "T", "price": { "amount": "99" } }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("$99", cards[0].priceText)
    }

    @Test
    fun `parseListingsToCards handles nested pricing price`() {
        val body = """[{ "id": "1", "title": "T", "pricing": { "price": "150" } }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals("$150", cards[0].priceText)
    }

    // ── Edge cases ──────────────────────────────────────────────────

    @Test
    fun `parseListingsToCards skips items without id`() {
        val body = """[{ "title": "No ID" }, { "id": "1", "title": "Has ID" }]"""
        val cards = repo.parseListingsToCards(body)
        assertEquals(1, cards.size)
        assertEquals("1", cards[0].id)
    }

    @Test
    fun `parseListingsToCards handles malformed JSON gracefully`() {
        val body = "not-json"
        val cards = repo.parseListingsToCards(body)
        assertEquals(0, cards.size)
    }

    @Test
    fun `parseListingsToCards handles empty array`() {
        val body = "[]"
        val cards = repo.parseListingsToCards(body)
        assertEquals(0, cards.size)
    }

    @Test
    fun `parseListingsToCards handles empty object`() {
        val body = "{}"
        val cards = repo.parseListingsToCards(body)
        assertEquals(0, cards.size)
    }

    @Test
    fun `parseListingsToCards handles large result set`() {
        val items = (1..50).map { """{ "id": "$it", "title": "Item $it", "city": "City$it" }""" }
        val body = "[${items.joinToString(",")}]"
        val cards = repo.parseListingsToCards(body)
        assertEquals(50, cards.size)
    }

    // ── HomeFeedModels ──────────────────────────────────────────────

    @Test
    fun `HomeCard holds all fields`() {
        val card = HomeCard(
            id = "1",
            title = "Test Space",
            location = "NYC",
            imageUrl = "https://img.jpg",
            priceText = "$50",
            rating = 4.5
        )
        assertEquals("1", card.id)
        assertEquals("Test Space", card.title)
        assertEquals("NYC", card.location)
        assertEquals("https://img.jpg", card.imageUrl)
        assertEquals("$50", card.priceText)
        assertEquals(4.5, card.rating!!, 0.01)
    }

    @Test
    fun `HomeSectionKey has correct properties`() {
        assertEquals("Hourly spaces", HomeSectionKey.HOURLY.displayTitle)
        assertEquals("USE", HomeSectionKey.HOURLY.shareType)

        assertEquals("Rent gear", HomeSectionKey.GEAR.displayTitle)
        assertEquals("BORROW", HomeSectionKey.GEAR.shareType)

        assertEquals("Split stays", HomeSectionKey.SPLIT.displayTitle)
        assertEquals("SPLIT", HomeSectionKey.SPLIT.shareType)
    }

    @Test
    fun `HomeFeedState default has three loading sections`() {
        val state = HomeFeedState()
        assertEquals(3, state.sections.size)
        assertTrue(state.sections.all { it.isLoading })
        assertTrue(state.sections.all { it.items.isEmpty() })
        assertNull(state.globalErrorMessage)
        assertEquals("Find your perfect stay", state.headerTitle)
    }
}
