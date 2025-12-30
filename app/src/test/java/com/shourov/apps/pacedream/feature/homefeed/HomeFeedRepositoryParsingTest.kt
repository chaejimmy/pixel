package com.shourov.apps.pacedream.feature.homefeed

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.TokenProvider
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
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
}

