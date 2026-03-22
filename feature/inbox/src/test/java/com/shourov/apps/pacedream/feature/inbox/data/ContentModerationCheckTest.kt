package com.shourov.apps.pacedream.feature.inbox.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentModerationCheckTest {

    // ── Safe words must NOT be flagged ──────────────────────────────────────

    @Test
    fun `hello is allowed`() {
        val result = ContentModerationCheck.check("hello")
        assertEquals(ContentModerationCheck.Status.ALLOW, result.status)
    }

    @Test
    fun `hello there is allowed`() {
        val result = ContentModerationCheck.check("hello there")
        assertEquals(ContentModerationCheck.Status.ALLOW, result.status)
    }

    @Test
    fun `shell is allowed`() {
        val result = ContentModerationCheck.check("shell")
        assertEquals(ContentModerationCheck.Status.ALLOW, result.status)
    }

    @Test
    fun `help is allowed`() {
        val result = ContentModerationCheck.check("help")
        assertEquals(ContentModerationCheck.Status.ALLOW, result.status)
    }

    @Test
    fun `assess is allowed - no false match on ass`() {
        val result = ContentModerationCheck.check("assess")
        assertEquals(ContentModerationCheck.Status.ALLOW, result.status)
    }

    @Test
    fun `assistant is allowed`() {
        val result = ContentModerationCheck.check("assistant")
        assertEquals(ContentModerationCheck.Status.ALLOW, result.status)
    }

    @Test
    fun `class is allowed`() {
        val result = ContentModerationCheck.check("class")
        assertEquals(ContentModerationCheck.Status.ALLOW, result.status)
    }

    @Test
    fun `dickens is allowed`() {
        val result = ContentModerationCheck.check("dickens")
        assertEquals(ContentModerationCheck.Status.ALLOW, result.status)
    }

    @Test
    fun `scrapbook is allowed`() {
        val result = ContentModerationCheck.check("scrapbook")
        assertEquals(ContentModerationCheck.Status.ALLOW, result.status)
    }

    // ── Actual profanity is still flagged ───────────────────────────────────

    @Test
    fun `standalone hell is warned`() {
        val result = ContentModerationCheck.check("go to hell")
        assertEquals(ContentModerationCheck.Status.WARN, result.status)
        assertTrue(result.categories.contains("profanity"))
    }

    @Test
    fun `shit is warned`() {
        val result = ContentModerationCheck.check("shit")
        assertEquals(ContentModerationCheck.Status.WARN, result.status)
    }

    @Test
    fun `standalone ass is warned`() {
        val result = ContentModerationCheck.check("that is ass")
        assertEquals(ContentModerationCheck.Status.WARN, result.status)
    }

    @Test
    fun `fuck is warned`() {
        val result = ContentModerationCheck.check("what the fuck")
        assertEquals(ContentModerationCheck.Status.WARN, result.status)
    }

    // ── Block-level content is blocked ──────────────────────────────────────

    @Test
    fun `threat is blocked`() {
        val result = ContentModerationCheck.check("i will kill you")
        assertEquals(ContentModerationCheck.Status.BLOCK, result.status)
        assertTrue(result.categories.contains("threat"))
    }

    @Test
    fun `slur is blocked`() {
        val result = ContentModerationCheck.check("nigger")
        assertEquals(ContentModerationCheck.Status.BLOCK, result.status)
        assertTrue(result.categories.contains("hate"))
    }

    @Test
    fun `scam is blocked`() {
        val result = ContentModerationCheck.check("send me your password")
        assertEquals(ContentModerationCheck.Status.BLOCK, result.status)
    }

    // ── Edge cases ──────────────────────────────────────────────────────────

    @Test
    fun `empty text is allowed`() {
        val result = ContentModerationCheck.check("")
        assertEquals(ContentModerationCheck.Status.ALLOW, result.status)
    }

    @Test
    fun `whitespace only is allowed`() {
        val result = ContentModerationCheck.check("   ")
        assertEquals(ContentModerationCheck.Status.ALLOW, result.status)
    }

    @Test
    fun `mixed case profanity is warned`() {
        val result = ContentModerationCheck.check("SHIT")
        assertEquals(ContentModerationCheck.Status.WARN, result.status)
    }
}
