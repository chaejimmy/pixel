package com.shourov.apps.pacedream.feature.inbox.data

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
 * Extended tests for InboxRepository parsing logic.
 * Covers: tolerant thread parsing, multiple message formats, unread count edge cases,
 * listing/user parsing, fallback response shapes, edge cases.
 */
class InboxRepositoryExtendedTest {

    private lateinit var repo: InboxRepository

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
        repo = InboxRepository(apiClient = apiClient, appConfig = appConfig, json = json)
    }

    // ── Thread parsing: multiple response shapes ────────────────────

    @Test
    fun `parseThreads handles data_items wrapper`() {
        val body = """
            {
              "data": {
                "items": [
                  { "_id": "t1", "participants": ["u1", "u2"], "unreadCount": 1 }
                ]
              }
            }
        """.trimIndent()
        val result = repo.parseThreadsResponseForTest(body)
        assertEquals(1, result.threads.size)
        assertEquals("t1", result.threads[0].id)
    }

    @Test
    fun `parseThreads handles flat threads array`() {
        val body = """
            {
              "threads": [
                { "id": "t2", "participants": ["a", "b"], "unread": 3 }
              ]
            }
        """.trimIndent()
        val result = repo.parseThreadsResponseForTest(body)
        assertEquals(1, result.threads.size)
        assertEquals("t2", result.threads[0].id)
        assertEquals(3, result.threads[0].unreadCount)
    }

    @Test
    fun `parseThreads with no nextCursor has hasMore false`() {
        val body = """{ "data": { "threads": [] } }"""
        val result = repo.parseThreadsResponseForTest(body)
        assertEquals(0, result.threads.size)
        assertNull(result.nextCursor)
        assertFalse(result.hasMore)
    }

    @Test
    fun `parseThreads with cursor key returns nextCursor`() {
        val body = """
            {
              "data": {
                "threads": [
                  { "_id": "t1", "participants": [] }
                ],
                "cursor": "abc123"
              }
            }
        """.trimIndent()
        val result = repo.parseThreadsResponseForTest(body)
        assertEquals("abc123", result.nextCursor)
        assertTrue(result.hasMore)
    }

    @Test
    fun `parseThreads with explicit hasMore field`() {
        val body = """
            {
              "data": {
                "threads": [
                  { "_id": "t1", "participants": [] }
                ],
                "hasMore": false,
                "nextCursor": "xyz"
              }
            }
        """.trimIndent()
        val result = repo.parseThreadsResponseForTest(body)
        assertEquals("xyz", result.nextCursor)
        assertFalse(result.hasMore)
    }

    // ── Thread field variants ───────────────────────────────────────

    @Test
    fun `parseThreads handles participant field as participant`() {
        val body = """
            {
              "data": {
                "threads": [
                  {
                    "_id": "t1",
                    "participants": ["u1"],
                    "participant": { "id": "u2", "name": "Bob", "avatar": "https://avatar.jpg" }
                  }
                ]
              }
            }
        """.trimIndent()
        val result = repo.parseThreadsResponseForTest(body)
        assertEquals("Bob", result.threads[0].opponent?.name)
        assertEquals("https://avatar.jpg", result.threads[0].opponent?.avatar)
    }

    @Test
    fun `parseThreads handles updated_at field`() {
        val body = """
            {
              "data": {
                "threads": [
                  { "_id": "t1", "participants": [], "updated_at": "2024-01-15T10:00:00Z" }
                ]
              }
            }
        """.trimIndent()
        val result = repo.parseThreadsResponseForTest(body)
        assertEquals("2024-01-15T10:00:00Z", result.threads[0].updatedAt)
    }

    @Test
    fun `parseThreads handles listing with name and thumbnail`() {
        val body = """
            {
              "data": {
                "threads": [
                  {
                    "_id": "t1",
                    "participants": [],
                    "listing": { "id": "l1", "name": "Studio", "thumbnail": "https://thumb.jpg" }
                  }
                ]
              }
            }
        """.trimIndent()
        val result = repo.parseThreadsResponseForTest(body)
        assertNotNull(result.threads[0].listing)
        assertEquals("l1", result.threads[0].listing?.id)
        assertEquals("Studio", result.threads[0].listing?.title)
        assertEquals("https://thumb.jpg", result.threads[0].listing?.imageUrl)
    }

    @Test
    fun `parseThreads handles opponent with firstName and lastName`() {
        val body = """
            {
              "data": {
                "threads": [
                  {
                    "_id": "t1",
                    "participants": [],
                    "opponent": { "_id": "u1", "firstName": "Jane", "lastName": "Doe" }
                  }
                ]
              }
            }
        """.trimIndent()
        val result = repo.parseThreadsResponseForTest(body)
        assertEquals("Jane Doe", result.threads[0].opponent?.name)
    }

    // ── Message parsing: multiple formats ───────────────────────────

    @Test
    fun `parseMessages handles data_messages wrapper`() {
        val body = """
            {
              "data": {
                "messages": [
                  { "_id": "m1", "text": "Hi there", "senderId": "u1", "createdAt": "2024-01-01T00:00:00Z" }
                ]
              }
            }
        """.trimIndent()
        val result = repo.parseMessagesResponseForTest(body)
        assertEquals(1, result.messages.size)
        assertEquals("m1", result.messages[0].id)
        assertEquals("Hi there", result.messages[0].text)
    }

    @Test
    fun `parseMessages handles content field instead of text`() {
        val body = """
            [
              { "id": "m1", "content": "Hello world", "from": "u1" }
            ]
        """.trimIndent()
        val result = repo.parseMessagesResponseForTest(body)
        assertEquals("Hello world", result.messages[0].text)
        assertEquals("u1", result.messages[0].senderId)
    }

    @Test
    fun `parseMessages handles body field instead of text`() {
        val body = """
            [
              { "id": "m1", "body": "Message body", "sender": "u1" }
            ]
        """.trimIndent()
        val result = repo.parseMessagesResponseForTest(body)
        assertEquals("Message body", result.messages[0].text)
    }

    @Test
    fun `parseMessages handles empty array`() {
        val body = "[]"
        val result = repo.parseMessagesResponseForTest(body)
        assertEquals(0, result.messages.size)
        assertFalse(result.hasMore)
    }

    @Test
    fun `parseMessages handles hasMore flag`() {
        val body = """
            {
              "data": [
                { "id": "m1", "text": "hi", "senderId": "u1" }
              ],
              "hasMore": true
            }
        """.trimIndent()
        val result = repo.parseMessagesResponseForTest(body)
        assertTrue(result.hasMore)
    }

    @Test
    fun `parseMessages handles read and isRead fields`() {
        val body = """
            [
              { "id": "m1", "text": "read msg", "senderId": "u1", "read": true },
              { "id": "m2", "text": "unread msg", "senderId": "u1", "isRead": false }
            ]
        """.trimIndent()
        val result = repo.parseMessagesResponseForTest(body)
        assertTrue(result.messages[0].isRead)
        assertFalse(result.messages[1].isRead)
    }

    @Test
    fun `parseMessages handles timestamp and created_at variants`() {
        val body = """
            [
              { "id": "m1", "text": "a", "senderId": "u1", "timestamp": "2024-06-01T12:00:00Z" },
              { "id": "m2", "text": "b", "senderId": "u1", "created_at": "2024-06-02T12:00:00Z" }
            ]
        """.trimIndent()
        val result = repo.parseMessagesResponseForTest(body)
        assertEquals("2024-06-01T12:00:00Z", result.messages[0].timestamp)
        assertEquals("2024-06-02T12:00:00Z", result.messages[1].timestamp)
    }

    @Test
    fun `parseMessages handles attachments array`() {
        val body = """
            [
              { "id": "m1", "text": "see pics", "senderId": "u1", "attachments": ["img1.jpg", "img2.jpg"] }
            ]
        """.trimIndent()
        val result = repo.parseMessagesResponseForTest(body)
        assertEquals(2, result.messages[0].attachments.size)
        assertEquals("img1.jpg", result.messages[0].attachments[0])
    }

    // ── Unread counts: edge cases ───────────────────────────────────

    @Test
    fun `parseUnreadCounts handles zero counts`() {
        val body = """{ "data": { "guestUnread": 0, "hostUnread": 0 } }"""
        val counts = repo.parseUnreadCountsForTest(body)
        assertEquals(0, counts.guestUnread)
        assertEquals(0, counts.hostUnread)
        assertEquals(0, counts.totalUnread)
    }

    @Test
    fun `parseUnreadCounts handles missing fields defaults to zero`() {
        val body = """{ "data": {} }"""
        val counts = repo.parseUnreadCountsForTest(body)
        assertEquals(0, counts.guestUnread)
        assertEquals(0, counts.hostUnread)
    }

    @Test
    fun `parseUnreadCounts handles flat response without data wrapper`() {
        val body = """{ "guestUnread": 7, "hostUnread": 3 }"""
        val counts = repo.parseUnreadCountsForTest(body)
        assertEquals(7, counts.guestUnread)
        assertEquals(3, counts.hostUnread)
        assertEquals(10, counts.totalUnread)
    }

    @Test
    fun `parseUnreadCounts handles mixed key formats`() {
        val body = """{ "data": { "guest": 2, "host": 4 } }"""
        val counts = repo.parseUnreadCountsForTest(body)
        assertEquals(2, counts.guestUnread)
        assertEquals(4, counts.hostUnread)
    }

    // ── Thread with lastMessage ─────────────────────────────────────

    @Test
    fun `parseThreads includes lastMessage with all fields`() {
        val body = """
            {
              "data": {
                "threads": [
                  {
                    "_id": "t1",
                    "participants": [],
                    "lastMessage": {
                      "_id": "m99",
                      "text": "See you tomorrow!",
                      "senderId": "u2",
                      "createdAt": "2024-03-15T14:30:00Z",
                      "read": true
                    }
                  }
                ]
              }
            }
        """.trimIndent()
        val result = repo.parseThreadsResponseForTest(body)
        val lastMsg = result.threads[0].lastMessage
        assertNotNull(lastMsg)
        assertEquals("m99", lastMsg!!.id)
        assertEquals("See you tomorrow!", lastMsg.text)
        assertEquals("u2", lastMsg.senderId)
        assertTrue(lastMsg.isRead)
    }

    // ── Multiple threads parsing ────────────────────────────────────

    @Test
    fun `parseThreads handles multiple threads`() {
        val body = """
            {
              "data": {
                "threads": [
                  { "_id": "t1", "participants": ["u1"], "unreadCount": 5 },
                  { "_id": "t2", "participants": ["u2"], "unreadCount": 0 },
                  { "_id": "t3", "participants": ["u3"], "unreadCount": 2 }
                ]
              }
            }
        """.trimIndent()
        val result = repo.parseThreadsResponseForTest(body)
        assertEquals(3, result.threads.size)
        assertEquals("t1", result.threads[0].id)
        assertEquals("t3", result.threads[2].id)
        assertEquals(5, result.threads[0].unreadCount)
        assertEquals(0, result.threads[1].unreadCount)
    }

    // ── Malformed thread is skipped ─────────────────────────────────

    @Test
    fun `parseThreads skips malformed threads gracefully`() {
        val body = """
            {
              "data": {
                "threads": [
                  { "_id": "t1", "participants": [] },
                  "not-a-thread",
                  { "_id": "t2", "participants": [] }
                ]
              }
            }
        """.trimIndent()
        val result = repo.parseThreadsResponseForTest(body)
        // Malformed entries are skipped via mapNotNull
        assertTrue(result.threads.size >= 2)
    }
}
