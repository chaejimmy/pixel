package com.shourov.apps.pacedream.feature.homefeed

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.TokenProvider
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFeedRepositoryParsingTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val appConfig = AppConfig()
    private val apiClient = ApiClient(
        appConfig = appConfig,
        json = json,
        tokenProvider = object : TokenProvider {
            override fun getAccessToken(): String? = null
            override fun getRefreshToken(): String? = null
        }
    )

    private val repo = HomeFeedRepository(
        apiClient = apiClient,
        appConfig = appConfig,
        json = json
    )

    @Test
    fun `parseListingsToCards supports raw array`() {
        val body = """
            [
              {
                "_id": "1",
                "title": "Cool Space",
                "city": "NYC",
                "images": ["https://img/1.webp"],
                "rating": 4.8,
                "price": "10"
              }
            ]
        """.trimIndent()

        val cards = repo.parseListingsToCards(body)
        assertEquals(1, cards.size)
        assertEquals("1", cards.first().id)
        assertEquals("Cool Space", cards.first().title)
        assertEquals("NYC", cards.first().location)
        assertEquals("https://img/1.webp", cards.first().imageUrl)
        assertEquals("$10", cards.first().priceText)
    }

    @Test
    fun `parseListingsToCards supports nested data_listings`() {
        val body = """
            {
              "success": true,
              "data": {
                "listings": [
                  {
                    "id": "abc",
                    "name": "Loft",
                    "location": { "city": "LA" },
                    "imageUrl": "https://img/loft.webp",
                    "avgRating": 4.2,
                    "priceText": "$120"
                  }
                ]
              }
            }
        """.trimIndent()

        val cards = repo.parseListingsToCards(body)
        assertEquals(1, cards.size)
        assertEquals("abc", cards.first().id)
        assertEquals("Loft", cards.first().title)
        assertEquals("LA", cards.first().location)
        assertEquals("https://img/loft.webp", cards.first().imageUrl)
        assertEquals("$120", cards.first().priceText)
        assertTrue(cards.first().rating != null)
    }

    @Test
    fun `parseListingsToCards handles RentableItem format with gallery and price array`() {
        val body = """
            {
              "action": "/v1/properties/filter-rentable-items-by-group/time_based?item_type=room",
              "code": 200,
              "status": true,
              "data": [
                {
                  "_id": "rent1",
                  "title": "Cozy Meeting Room",
                  "item_type": "room",
                  "location": { "city": "Austin", "state": "TX", "country": "US" },
                  "gallery": {
                    "thumbnail": "https://img/thumb.webp",
                    "images": ["https://img/room1.webp", "https://img/room2.webp"]
                  },
                  "price": [
                    {
                      "pricing_type": "hourly",
                      "amount": 25,
                      "currency": "USD",
                      "frequency": "HOUR"
                    }
                  ],
                  "rating": 4.5,
                  "category": "time_based"
                }
              ],
              "message": "Rentable items filtered by group retrieved successfully"
            }
        """.trimIndent()

        val cards = repo.parseListingsToCards(body)
        assertEquals(1, cards.size)
        val card = cards.first()
        assertEquals("rent1", card.id)
        assertEquals("Cozy Meeting Room", card.title)
        assertEquals("Austin", card.location)
        assertEquals("https://img/room1.webp", card.imageUrl)
        assertNotNull(card.priceText)
        assertEquals("$25/hr", card.priceText)
        assertEquals(4.5, card.rating)
        assertEquals("room", card.subCategory)
    }

    @Test
    fun `parseListingsToCards does not crash on mixed field types`() {
        val body = """
            {
              "data": [
                {
                  "_id": "a1",
                  "title": "Space A",
                  "price": [{"amount": 10}],
                  "location": {"city": "Denver"},
                  "gallery": {"images": ["https://img/a.webp"]}
                },
                {
                  "_id": "a2",
                  "title": "Space B",
                  "price": "15",
                  "location": "Seattle",
                  "images": ["https://img/b.webp"]
                }
              ]
            }
        """.trimIndent()

        val cards = repo.parseListingsToCards(body)
        assertEquals(2, cards.size)
        assertEquals("Space A", cards[0].title)
        assertEquals("$10", cards[0].priceText)
        assertEquals("Space B", cards[1].title)
        assertEquals("$15", cards[1].priceText)
    }

    @Test
    fun `parseListingsToCards renders dynamic_price monthly as price per month`() {
        val body = """
            [
              {
                "_id": "m1",
                "title": "Hair Booth Rental in Fairfax Salon (${'$'}800/month)",
                "city": "Fairfax",
                "dynamic_price": [
                  {
                    "currency": "USD",
                    "hourly": null,
                    "daily": null,
                    "monthly": { "price": 800 }
                  }
                ]
              }
            ]
        """.trimIndent()

        val cards = repo.parseListingsToCards(body)
        assertEquals(1, cards.size)
        // Price lives in the badge, not the title — parenthesised suffix is stripped.
        assertEquals("Hair Booth Rental in Fairfax Salon", cards.first().title)
        assertEquals("$800/month", cards.first().priceText)
    }

    @Test
    fun `parseListingsToCards keeps hourly dynamic_price badge`() {
        val body = """
            [
              {
                "_id": "h1",
                "title": "Home Gym",
                "dynamic_price": [
                  { "hourly": { "price": 10 } }
                ]
              }
            ]
        """.trimIndent()

        val cards = repo.parseListingsToCards(body)
        assertEquals("$10/hr", cards.first().priceText)
    }

    @Test
    fun `structured dynamic_price monthly wins over unitless priceText from backend`() {
        // Backend currently returns priceText = "${'$'}800" (no unit) while the
        // structured dynamic_price carries the monthly period. Prefer the
        // structured inference so the badge still reads "${'$'}800/month".
        val body = """
            [
              {
                "_id": "m2",
                "title": "Hair Booth Rental in Fairfax Salon",
                "priceText": "${'$'}800",
                "dynamic_price": [
                  { "monthly": { "price": 800 } }
                ]
              }
            ]
        """.trimIndent()

        val cards = repo.parseListingsToCards(body)
        assertEquals("$800/month", cards.first().priceText)
    }
}

