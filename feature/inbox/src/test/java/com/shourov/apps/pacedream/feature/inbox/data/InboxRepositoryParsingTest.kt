package com.shourov.apps.pacedream.feature.inbox.data

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.TokenProvider
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxRepositoryParsingTest {

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

    private val repo = InboxRepository(
        apiClient = apiClient,
        appConfig = appConfig,
        json = json
    )

    @Test
    fun `parseThreadsResponse supports data_threads wrapper and cursor`() {
        val body = """
            {
              "data": {
                "threads": [
                  {
                    "_id": "t1",
                    "participants": ["u1", "u2"],
                    "unreadCount": 2,
                    "updatedAt": "2024-01-01T00:00:00Z",
                    "opponent": { "id": "u2", "name": "Sam", "avatar": "https://a.webp" },
                    "lastMessage": { "id": "m1", "text": "hi", "sender": "u2", "createdAt": "2024-01-01T00:00:00Z" }
                  }
                ],
                "nextCursor": "next"
              }
            }
        """.trimIndent()

        val res = repo.parseThreadsResponseForTest(body)
        assertEquals(1, res.threads.size)
        assertEquals("t1", res.threads.first().id)
        assertEquals(2, res.threads.first().unreadCount)
        assertEquals("next", res.nextCursor)
        assertTrue(res.hasMore)
        assertEquals("Sam", res.threads.first().displayName)
        assertEquals("hi", res.threads.first().lastMessage?.text)
    }

    @Test
    fun `parseMessagesResponse supports raw array`() {
        val body = """
            [
              { "id": "m1", "content": "hello", "from": "u1", "timestamp": "2024-01-01T00:00:00Z" },
              { "_id": "m2", "text": "yo", "senderId": "u2", "createdAt": "2024-01-01T01:00:00Z" }
            ]
        """.trimIndent()

        val res = repo.parseMessagesResponseForTest(body)
        assertEquals(2, res.messages.size)
        assertEquals("m1", res.messages[0].id)
        assertEquals("hello", res.messages[0].text)
        assertEquals("u1", res.messages[0].senderId)
        assertFalse(res.hasMore)
    }

    @Test
    fun `parseUnreadCounts supports multiple key variants`() {
        val body = """{ "data": { "guest": 3, "hostUnread": 5 } }"""
        val counts = repo.parseUnreadCountsForTest(body)
        assertEquals(3, counts.guestUnread)
        assertEquals(5, counts.hostUnread)
        assertEquals(8, counts.totalUnread)
    }
}

