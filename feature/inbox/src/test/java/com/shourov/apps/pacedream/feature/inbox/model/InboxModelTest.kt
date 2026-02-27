package com.shourov.apps.pacedream.feature.inbox.model

import com.shourov.apps.pacedream.feature.inbox.data.ThreadListing
import com.shourov.apps.pacedream.feature.inbox.data.ThreadUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for inbox data models: Thread, Message, UnreadCounts, InboxMode,
 * InboxUiState, WishlistFilter.
 */
class InboxModelTest {

    // ── Thread computed properties ──────────────────────────────────

    @Test
    fun `Thread displayName returns opponent name`() {
        val thread = createThread(opponent = ThreadUser("u1", "Alice", null))
        assertEquals("Alice", thread.displayName)
    }

    @Test
    fun `Thread displayName returns Unknown when no opponent`() {
        val thread = createThread(opponent = null)
        assertEquals("Unknown", thread.displayName)
    }

    @Test
    fun `Thread avatarUrl returns opponent avatar`() {
        val thread = createThread(opponent = ThreadUser("u1", "Bob", "https://avatar.jpg"))
        assertEquals("https://avatar.jpg", thread.avatarUrl)
    }

    @Test
    fun `Thread lastMessagePreview truncates at 100 chars`() {
        val longText = "A".repeat(200)
        val msg = Message("m1", longText, "u1", null)
        val thread = createThread(lastMessage = msg)
        assertEquals(100, thread.lastMessagePreview.length)
    }

    @Test
    fun `Thread lastMessagePreview returns empty when no message`() {
        val thread = createThread(lastMessage = null)
        assertEquals("", thread.lastMessagePreview)
    }

    @Test
    fun `Thread hasUnread returns true when unreadCount greater than 0`() {
        val thread = createThread(unreadCount = 3)
        assertTrue(thread.hasUnread)
    }

    @Test
    fun `Thread hasUnread returns false when unreadCount is 0`() {
        val thread = createThread(unreadCount = 0)
        assertFalse(thread.hasUnread)
    }

    // ── Message computed properties ─────────────────────────────────

    @Test
    fun `Message hasAttachments returns true when attachments non-empty`() {
        val msg = Message("m1", "text", "u1", null, listOf("img.jpg"))
        assertTrue(msg.hasAttachments)
    }

    @Test
    fun `Message hasAttachments returns false when no attachments`() {
        val msg = Message("m1", "text", "u1", null)
        assertFalse(msg.hasAttachments)
    }

    @Test
    fun `Message formattedTime handles ISO timestamp`() {
        val msg = Message("m1", "hi", "u1", "2024-01-15T14:30:00Z")
        val formatted = msg.formattedTime
        // Should produce some formatted time string (e.g., "2:30 PM")
        assertTrue("formattedTime should not be empty for valid timestamp", formatted.isNotBlank())
    }

    @Test
    fun `Message formattedTime returns empty for null timestamp`() {
        val msg = Message("m1", "hi", "u1", null)
        assertEquals("", msg.formattedTime)
    }

    // ── UnreadCounts ────────────────────────────────────────────────

    @Test
    fun `UnreadCounts totalUnread sums both`() {
        val counts = UnreadCounts(5, 3)
        assertEquals(8, counts.totalUnread)
    }

    @Test
    fun `UnreadCounts totalUnread is zero when both zero`() {
        val counts = UnreadCounts(0, 0)
        assertEquals(0, counts.totalUnread)
    }

    // ── InboxMode ───────────────────────────────────────────────────

    @Test
    fun `InboxMode GUEST has correct apiValue`() {
        assertEquals("guest", InboxMode.GUEST.apiValue)
        assertEquals("Guest", InboxMode.GUEST.displayName)
    }

    @Test
    fun `InboxMode HOST has correct apiValue`() {
        assertEquals("host", InboxMode.HOST.apiValue)
        assertEquals("Host", InboxMode.HOST.displayName)
    }

    // ── InboxUiState ────────────────────────────────────────────────

    @Test
    fun `InboxUiState Success isEmpty returns true for empty threads`() {
        val state = InboxUiState.Success(
            threads = emptyList(),
            mode = InboxMode.GUEST,
            unreadCounts = UnreadCounts(0, 0)
        )
        assertTrue(state.isEmpty)
    }

    @Test
    fun `InboxUiState Success isEmpty returns false with threads`() {
        val thread = createThread()
        val state = InboxUiState.Success(
            threads = listOf(thread),
            mode = InboxMode.GUEST,
            unreadCounts = UnreadCounts(1, 0)
        )
        assertFalse(state.isEmpty)
    }

    @Test
    fun `InboxUiState Error has message`() {
        val state = InboxUiState.Error("Network error")
        assertEquals("Network error", state.message)
    }

    // ── InboxEvent ──────────────────────────────────────────────────

    @Test
    fun `InboxEvent ModeChanged carries mode`() {
        val event = InboxEvent.ModeChanged(InboxMode.HOST)
        assertEquals(InboxMode.HOST, event.mode)
    }

    @Test
    fun `InboxEvent ThreadClicked carries thread`() {
        val thread = createThread()
        val event = InboxEvent.ThreadClicked(thread)
        assertEquals(thread.id, event.thread.id)
    }

    // ── Helper ──────────────────────────────────────────────────────

    private fun createThread(
        id: String = "t1",
        participants: List<String> = emptyList(),
        lastMessage: Message? = null,
        unreadCount: Int = 0,
        updatedAt: String? = null,
        listing: ThreadListing? = null,
        opponent: ThreadUser? = null
    ) = Thread(
        id = id,
        participants = participants,
        lastMessage = lastMessage,
        unreadCount = unreadCount,
        updatedAt = updatedAt,
        listing = listing,
        opponent = opponent
    )
}
