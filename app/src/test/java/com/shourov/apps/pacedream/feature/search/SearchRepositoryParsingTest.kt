package com.shourov.apps.pacedream.feature.search

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.TokenProvider
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRepositoryParsingTest {

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

    private val repo = SearchRepository(
        apiClient = apiClient,
        appConfig = appConfig,
        json = json
    )

    @Test
    fun `parseSearchPage supports items array`() {
        val body = """
            {
              "items": [
                { "id": "1", "title": "Apt", "city": "NYC", "images": ["https://img/a.webp"], "rating": 4.5, "price": "99" }
              ]
            }
        """.trimIndent()

        val page = repo.parseSearchPageForTest(body, perPage = 24)
        assertEquals(1, page.items.size)
        assertEquals("1", page.items.first().id)
        assertEquals("Apt", page.items.first().title)
        assertEquals("NYC", page.items.first().location)
        assertEquals("https://img/a.webp", page.items.first().imageUrl)
        assertTrue(page.hasMore.not()) // 1 < perPage => conservative false
    }

    @Test
    fun `parseSearchPage supports data_listings array`() {
        val body = """
            {
              "data": {
                "listings": [
                  { "_id": "x", "name": "Loft", "location": { "city": "LA" }, "imageUrl": "https://img/x.webp", "avgRating": 4.2, "priceText": "$120" }
                ]
              }
            }
        """.trimIndent()

        val page = repo.parseSearchPageForTest(body, perPage = 1)
        assertEquals(1, page.items.size)
        assertEquals("x", page.items.first().id)
        assertEquals("Loft", page.items.first().title)
        assertEquals("LA", page.items.first().location)
        assertEquals("https://img/x.webp", page.items.first().imageUrl)
        assertTrue(page.hasMore) // full page => assume more
    }
}

