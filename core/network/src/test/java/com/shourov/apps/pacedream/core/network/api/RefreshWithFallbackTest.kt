package com.shourov.apps.pacedream.core.network.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshWithFallbackTest {

    @Test
    fun `refreshWithFallback tries primary then fallback`() = runBlocking {
        val calls = mutableListOf<String>()

        val ok = refreshWithFallback(
            primaryCall = {
                calls += "primary"
                """{"success":false}""" // parser will fail
            },
            fallbackCall = {
                calls += "fallback"
                """{"success":true}"""
            },
            parseAndStore = { body ->
                body.contains("success\":true")
            }
        )

        assertTrue(ok)
        assertEquals(listOf("primary", "fallback"), calls)
    }

    @Test
    fun `refreshWithFallback does not call fallback when primary succeeds`() = runBlocking {
        val calls = mutableListOf<String>()

        val ok = refreshWithFallback(
            primaryCall = {
                calls += "primary"
                """{"success":true}"""
            },
            fallbackCall = {
                calls += "fallback"
                """{"success":true}"""
            },
            parseAndStore = { body ->
                body.contains("success\":true")
            }
        )

        assertTrue(ok)
        assertEquals(listOf("primary"), calls)
    }

    @Test
    fun `refreshWithFallback fails when both fail`() = runBlocking {
        val calls = mutableListOf<String>()

        val ok = refreshWithFallback(
            primaryCall = {
                calls += "primary"
                null
            },
            fallbackCall = {
                calls += "fallback"
                """{"success":false}"""
            },
            parseAndStore = { false }
        )

        assertFalse(ok)
        assertEquals(listOf("primary", "fallback"), calls)
    }
}

